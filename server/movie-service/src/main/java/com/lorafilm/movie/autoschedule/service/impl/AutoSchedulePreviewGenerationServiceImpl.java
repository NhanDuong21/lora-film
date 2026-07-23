package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationContextLoader;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewGenerationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.autoschedule.service.CandidateCountEstimator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class AutoSchedulePreviewGenerationServiceImpl implements AutoSchedulePreviewGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AutoSchedulePreviewGenerationServiceImpl.class);

    private final AutoScheduleGenerateRequestNormalizer normalizer;
    private final AutoScheduleRequestFingerprintService fingerprintService;
    private final AutoScheduleGenerationContextLoader contextLoader;
    private final CandidateCountEstimator candidateCountEstimator;
    private final ShowtimeCandidateGenerator generator;
    private final ShowtimeCandidateValidationService validationService;
    private final AutoScheduleGenerationStrategyRegistry strategyRegistry;
    private final ShowtimeSchedulePreviewLifecycleService lifecycleService;
    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper responseMapper;
    private final MovieShowtimeEligibilityPolicy movieEligibilityPolicy;

    public AutoSchedulePreviewGenerationServiceImpl(AutoScheduleGenerateRequestNormalizer normalizer,
                                                    AutoScheduleRequestFingerprintService fingerprintService,
                                                    AutoScheduleGenerationContextLoader contextLoader,
                                                    CandidateCountEstimator candidateCountEstimator,
                                                    ShowtimeCandidateGenerator generator,
                                                    ShowtimeCandidateValidationService validationService,
                                                    AutoScheduleGenerationStrategyRegistry strategyRegistry,
                                                    ShowtimeSchedulePreviewLifecycleService lifecycleService,
                                                    CinemaRepository cinemaRepository,
                                                    AuditoriumRepository auditoriumRepository,
                                                    MovieVersionRepository movieVersionRepository,
                                                    ShowtimeSchedulePreviewRepository previewRepository,
                                                    com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper responseMapper,
                                                    MovieShowtimeEligibilityPolicy movieEligibilityPolicy) {
        this.normalizer = normalizer;
        this.fingerprintService = fingerprintService;
        this.contextLoader = contextLoader;
        this.candidateCountEstimator = candidateCountEstimator;
        this.generator = generator;
        this.validationService = validationService;
        this.strategyRegistry = strategyRegistry;
        this.lifecycleService = lifecycleService;
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.previewRepository = previewRepository;
        this.responseMapper = responseMapper;
        this.movieEligibilityPolicy = movieEligibilityPolicy;
    }

    @Override
    public ShowtimeSchedulePreviewSummaryResponse generatePreview(GenerateShowtimeSchedulePreviewRequest request, Long adminUserId) {
        // 1. Normalize and Fingerprint
        NormalizedGeneratePreviewRequest normalizedRequest = normalizer.normalize(request);
        AutoScheduleGenerationStrategy generationStrategy = strategyRegistry.getCurrent();
        String strategyVersion = generationStrategy.getStrategyVersion();
        String fingerprint = fingerprintService.generateFingerprint(normalizedRequest, strategyVersion);

        // Date range validation
        long inclusiveDays = ChronoUnit.DAYS.between(normalizedRequest.getScheduleFrom(), normalizedRequest.getScheduleTo()) + 1;
        if (inclusiveDays <= 0) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE);
        }
        if (inclusiveDays > 7) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_DATE_RANGE_TOO_LARGE);
        }

        // Resolve Cinema
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(normalizedRequest.getCinemaPublicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));

        if (cinema.getStatus() != com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_ACTIVE);
        }

        ZoneId cinemaZone;
        try {
            cinemaZone = ZoneId.of(cinema.getTimezone());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE);
        }

        if (normalizedRequest.getScheduleFrom().isBefore(Instant.now().atZone(cinemaZone).toLocalDate())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE, "Cannot schedule in the past");
        }

        // Idempotency check
        Optional<ShowtimeSchedulePreview> existingOpt = previewRepository.findByGenerateIdempotencyKey(normalizedRequest.getIdempotencyKey());
        if (existingOpt.isPresent()) {
            ShowtimeSchedulePreview existing = existingOpt.get();
            if (!matchesStoredFingerprint(normalizedRequest, existing)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            log.info("Idempotency replay detected. Returning existing preview: {}", existing.getPublicId());
            return responseMapper.toSummaryResponse(existing);
        }

        // Resolve Auditoriums
        List<Auditorium> auditoriums = auditoriumRepository.findByPublicIdInAndDeletedAtIsNull(normalizedRequest.getAuditoriumPublicIds());
        if (auditoriums.size() != normalizedRequest.getAuditoriumPublicIds().size()) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND);
        }
        for (Auditorium aud : auditoriums) {
            if (aud.getStatus() != com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.AUDITORIUM_NOT_ACTIVE);
            }
            if (aud.getCleaningBufferMinutes() == null || aud.getCleaningBufferMinutes() < 0) {
                throw new BusinessException(ErrorCode.INVALID_CLEANING_BUFFER);
            }
            if (!aud.getCinema().getId().equals(cinema.getId())) {
                throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA);
            }
        }

        // Resolve Movie Versions
        List<MovieVersion> movieVersions = movieVersionRepository.findByPublicIdInWithMovieAndDeletedAtIsNull(normalizedRequest.getMovieVersionPublicIds());
        if (movieVersions.size() != normalizedRequest.getMovieVersionPublicIds().size()) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND);
        }
        for (MovieVersion version : movieVersions) {
            com.lorafilm.movie.movie.domain.entity.Movie movie = version.getMovie();
            movieEligibilityPolicy.validateMovieAndVersion(movie, version);
        }

        ShowtimeSchedulePreview preview;
        try {
            preview = lifecycleService.createGeneratingPreview(
                    normalizedRequest, cinema, strategyVersion, fingerprint, adminUserId);
        } catch (DataIntegrityViolationException e) {
            // Concurrent same key check
            existingOpt = previewRepository.findByGenerateIdempotencyKeyWithCinema(normalizedRequest.getIdempotencyKey());
            if (existingOpt.isPresent()) {
                ShowtimeSchedulePreview existing = existingOpt.get();
                if (!matchesStoredFingerprint(normalizedRequest, existing)) {
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
                }
                return responseMapper.toSummaryResponse(existing);
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Concurrency issue occurred");
        }

        try {
            AutoScheduleGenerationContext context = contextLoader.load(
                    normalizedRequest, cinema, auditoriums, movieVersions, strategyVersion);
            int estimatedCandidateCount = candidateCountEstimator.estimate(context);
            List<ShowtimeCandidate> candidates = new ArrayList<>(estimatedCandidateCount);
            CandidateScoringContext scoringContext = new CandidateScoringContext(context);

            long generatedCandidateCount = generator.generate(context, candidate -> {
                CandidateValidationResult valResult = validationService.validate(candidate, context);
                if (valResult.isValid()) {
                    candidate.setValidationStatus(com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus.VALID);
                } else {
                    candidate.setValidationStatus(com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus.REJECTED);
                    candidate.setRejectionCode(valResult.getRejectionCode());
                    candidate.setRejectionReason(valResult.getRejectionReason());
                }

                candidates.add(candidate);
            });

            if (generatedCandidateCount != estimatedCandidateCount
                    || candidates.size() != estimatedCandidateCount) {
                throw new IllegalStateException("Candidate estimation and generation diverged");
            }

            generationStrategy.scoreAndResolveDefaultSelection(candidates, scoringContext);
            attachPersistenceReferences(candidates, cinema, auditoriums, movieVersions);

            lifecycleService.persistGeneratedItemsAndMarkPreviewed(preview, candidates);

            return responseMapper.toSummaryResponse(preview);

        } catch (BusinessException e) {
            log.warn("Generation rejected for preview {} with error {}", preview.getPublicId(), e.getErrorCode());
            markPreviewFailedSafely(preview.getId(), sanitizeFailureReason(e));
            throw e;
        } catch (Exception e) {
            log.error("Generation failed for preview {}", preview.getPublicId(), e);
            markPreviewFailedSafely(preview.getId(), sanitizeFailureReason(e));
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED);
        }
    }

    private boolean matchesStoredFingerprint(NormalizedGeneratePreviewRequest request,
                                             ShowtimeSchedulePreview existing) {
        String expected = fingerprintService.generateFingerprint(request, existing.getStrategyVersion());
        return Objects.equals(existing.getRequestFingerprint(), expected);
    }

    private void attachPersistenceReferences(List<ShowtimeCandidate> candidates,
                                             Cinema cinema,
                                             List<Auditorium> auditoriums,
                                             List<MovieVersion> movieVersions) {
        Map<Long, Auditorium> auditoriumsById = new HashMap<>();
        auditoriums.forEach(auditorium -> auditoriumsById.put(auditorium.getId(), auditorium));
        Map<Long, MovieVersion> versionsById = new HashMap<>();
        movieVersions.forEach(version -> versionsById.put(version.getId(), version));

        for (ShowtimeCandidate candidate : candidates) {
            Auditorium auditorium = auditoriumsById.get(candidate.getAuditoriumSnapshot().id());
            MovieVersion version = versionsById.get(candidate.getMovieVersionSnapshot().id());
            if (auditorium == null || version == null) {
                throw new IllegalStateException("Candidate snapshot no longer matches loaded scheduling facts");
            }
            candidate.setCinema(cinema);
            candidate.setAuditorium(auditorium);
            candidate.setMovieVersion(version);
            candidate.setMovie(version.getMovie());
        }
    }

    private void markPreviewFailedSafely(Long previewId, String failureReason) {
        try {
            lifecycleService.markPreviewFailed(previewId, failureReason);
        } catch (Exception failurePersistenceException) {
            log.error("Could not mark auto schedule preview {} as FAILED", previewId, failurePersistenceException);
        }
    }

    private String sanitizeFailureReason(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "Unknown error";
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
