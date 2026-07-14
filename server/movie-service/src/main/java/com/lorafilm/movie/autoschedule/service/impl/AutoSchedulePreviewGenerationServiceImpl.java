package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewPersistenceMapper;
import com.lorafilm.movie.autoschedule.model.CandidateGenerationContext;
import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewGenerationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AutoSchedulePreviewGenerationServiceImpl implements AutoSchedulePreviewGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AutoSchedulePreviewGenerationServiceImpl.class);

    private final AutoScheduleGenerateRequestNormalizer normalizer;
    private final AutoScheduleRequestFingerprintService fingerprintService;
    private final ShowtimeCandidateGenerator generator;
    private final ShowtimeCandidateValidationService validationService;
    private final BalancedCandidateScoringService scoringService;
    private final CandidateSelectionResolver selectionResolver;
    private final ShowtimeSchedulePreviewLifecycleService lifecycleService;
    private final CinemaOperatingWindowResolver windowResolver;

    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository previewItemRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSchedulePreviewPersistenceMapper mapper;
    private final com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper responseMapper;

    public AutoSchedulePreviewGenerationServiceImpl(AutoScheduleGenerateRequestNormalizer normalizer,
                                                    AutoScheduleRequestFingerprintService fingerprintService,
                                                    ShowtimeCandidateGenerator generator,
                                                    ShowtimeCandidateValidationService validationService,
                                                    BalancedCandidateScoringService scoringService,
                                                    CandidateSelectionResolver selectionResolver,
                                                    ShowtimeSchedulePreviewLifecycleService lifecycleService,
                                                    CinemaOperatingWindowResolver windowResolver,
                                                    CinemaRepository cinemaRepository,
                                                    AuditoriumRepository auditoriumRepository,
                                                    MovieVersionRepository movieVersionRepository,
                                                    ShowtimeSchedulePreviewRepository previewRepository,
                                                    ShowtimeSchedulePreviewItemRepository previewItemRepository,
                                                    ShowtimeRepository showtimeRepository,
                                                    ShowtimeSchedulePreviewPersistenceMapper mapper,
                                                    com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper responseMapper) {
        this.normalizer = normalizer;
        this.fingerprintService = fingerprintService;
        this.generator = generator;
        this.validationService = validationService;
        this.scoringService = scoringService;
        this.selectionResolver = selectionResolver;
        this.lifecycleService = lifecycleService;
        this.windowResolver = windowResolver;
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.previewRepository = previewRepository;
        this.previewItemRepository = previewItemRepository;
        this.showtimeRepository = showtimeRepository;
        this.mapper = mapper;
        this.responseMapper = responseMapper;
    }

    @Override
    public ShowtimeSchedulePreviewResponse generatePreview(GenerateShowtimeSchedulePreviewRequest request, Long adminUserId) {
        // 1. Normalize and Fingerprint
        NormalizedGeneratePreviewRequest normalizedRequest = normalizer.normalize(request);
        String fingerprint = fingerprintService.generateFingerprint(normalizedRequest);

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
            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            log.info("Idempotency replay detected. Returning existing preview: {}", existing.getPublicId());
            return responseMapper.toResponse(existing, previewItemRepository.findDetailedItemsByPreviewId(existing.getId()));
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
            if (version.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_ACTIVE);
            }
            com.lorafilm.movie.movie.domain.entity.Movie movie = version.getMovie();
            if (movie == null || movie.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
            }
            if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION);
            }
        }

        ShowtimeSchedulePreview preview;
        try {
            preview = lifecycleService.createGeneratingPreview(normalizedRequest, cinema, fingerprint, adminUserId);
        } catch (DataIntegrityViolationException e) {
            // Concurrent same key check
            existingOpt = previewRepository.findByGenerateIdempotencyKeyWithCinema(normalizedRequest.getIdempotencyKey());
            if (existingOpt.isPresent()) {
                ShowtimeSchedulePreview existing = existingOpt.get();
                if (!existing.getRequestFingerprint().equals(fingerprint)) {
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
                }
                return responseMapper.toResponse(existing, previewItemRepository.findDetailedItemsByPreviewId(existing.getId()));
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Concurrency issue occurred");
        }

        try {
            CandidateGenerationContext context = new CandidateGenerationContext(normalizedRequest, cinema, auditoriums, movieVersions);
            List<ShowtimeCandidate> candidates = generator.generate(context);

            List<OperatingWindow> operatingWindows = windowResolver.resolve(cinema, normalizedRequest.getScheduleFrom(), normalizedRequest.getScheduleTo());
            
            // For continuity bonus, we need existing real showtimes within this period
            Instant searchStart = operatingWindows.isEmpty() ? Instant.now() : operatingWindows.get(0).getOpenInstant();
            Instant searchEnd = operatingWindows.isEmpty() ? Instant.now() : operatingWindows.get(operatingWindows.size() - 1).getCloseInstant();
            
            List<Long> auditoriumIds = auditoriums.stream().map(Auditorium::getId).collect(Collectors.toList());
            List<Showtime> existingShowtimes = showtimeRepository.findByAuditoriumIdInAndStartTimeBetween(auditoriumIds, searchStart, searchEnd);

            CandidateScoringContext scoringContext = new CandidateScoringContext(cinema, operatingWindows, existingShowtimes);

            for (ShowtimeCandidate candidate : candidates) {
                CandidateValidationResult valResult = validationService.validate(candidate);
                if (valResult.isValid()) {
                    candidate.setValidationStatus(com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus.VALID);
                } else {
                    candidate.setValidationStatus(com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus.REJECTED);
                    candidate.setRejectionCode(valResult.getRejectionCode());
                    candidate.setRejectionReason(valResult.getRejectionReason());
                }

                CandidateScoreResult scoreResult = scoringService.score(candidate, scoringContext);
                candidate.setScore(scoreResult.getScore());
                candidate.setScoreBreakdown(scoreResult.getScoreBreakdown());
            }

            selectionResolver.resolveDefaultSelection(candidates);

            lifecycleService.persistGeneratedItemsAndMarkPreviewed(preview, candidates);

            return responseMapper.toResponse(preview, previewItemRepository.findDetailedItemsByPreviewId(preview.getId()));

        } catch (Exception e) {
            log.error("Generation failed for preview {}", preview.getPublicId(), e);
            lifecycleService.markPreviewFailed(preview.getId(), sanitizeFailureReason(e));
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED);
        }
    }

    private String sanitizeFailureReason(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "Unknown error";
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
