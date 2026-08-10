package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreflightRequest;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleOptimizationResult;
import com.lorafilm.movie.autoschedule.model.AutoSchedulePreflightResult;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoScheduleCandidateEnrichmentService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationContextLoader;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreflightService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewGenerationService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.autoschedule.service.CandidateCountEstimator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
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
    private final AutoSchedulePreflightService preflightService;
    private final AutoScheduleCandidateEnrichmentService enrichmentService;
    private final AutoScheduleGenerationStrategyRegistry strategyRegistry;
    private final ShowtimeSchedulePreviewLifecycleService lifecycleService;
    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper responseMapper;
    private AutoScheduleMetrics metrics = AutoScheduleMetrics.noop();
    private AutoScheduleShadowComparisonService shadowComparisonService;

    public AutoSchedulePreviewGenerationServiceImpl(AutoScheduleGenerateRequestNormalizer normalizer,
                                                    AutoScheduleRequestFingerprintService fingerprintService,
                                                    AutoScheduleGenerationContextLoader contextLoader,
                                                    CandidateCountEstimator candidateCountEstimator,
                                                    ShowtimeCandidateGenerator generator,
                                                    ShowtimeCandidateValidationService validationService,
                                                    AutoSchedulePreflightService preflightService,
                                                    AutoScheduleCandidateEnrichmentService enrichmentService,
                                                    AutoScheduleGenerationStrategyRegistry strategyRegistry,
                                                    ShowtimeSchedulePreviewLifecycleService lifecycleService,
                                                    ShowtimeSchedulePreviewRepository previewRepository,
                                                    com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper responseMapper) {
        this.normalizer = normalizer;
        this.fingerprintService = fingerprintService;
        this.contextLoader = contextLoader;
        this.candidateCountEstimator = candidateCountEstimator;
        this.generator = generator;
        this.validationService = validationService;
        this.preflightService = preflightService;
        this.enrichmentService = enrichmentService;
        this.strategyRegistry = strategyRegistry;
        this.lifecycleService = lifecycleService;
        this.previewRepository = previewRepository;
        this.responseMapper = responseMapper;
    }

    @Autowired
    void setOperationalDependencies(AutoScheduleMetrics metrics,
                                    AutoScheduleShadowComparisonService shadowComparisonService) {
        this.metrics = metrics;
        this.shadowComparisonService = shadowComparisonService;
    }

    @Override
    public ShowtimeSchedulePreviewSummaryResponse generatePreview(GenerateShowtimeSchedulePreviewRequest request, Long adminUserId) {
        long generationStarted = System.nanoTime();
        String observedStrategy = AutoScheduleStrategyVersions.CURRENT;
        AutoSchedulePreflightResult preflight = preflightService.prepare(toPreflightRequest(request));
        validateAuthoritativeRange(request, preflight.response().planningFrom(), preflight.response().planningTo());
        if (!preflight.response().canGenerate()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREFLIGHT_BLOCKED, preflight.response());
        }

        NormalizedGeneratePreviewRequest normalizedRequest = normalizer.normalize(
                toEffectiveRequest(request, preflight));
        Cinema cinema = preflight.cinema();
        AutoScheduleGenerationStrategy generationStrategy = strategyRegistry.getForCinema(cinema);
        String strategyVersion = generationStrategy.getStrategyVersion();
        observedStrategy = strategyVersion;
        String fingerprint = fingerprintService.generateFingerprint(normalizedRequest, strategyVersion);

        // Date range validation
        long inclusiveDays = ChronoUnit.DAYS.between(normalizedRequest.getScheduleFrom(), normalizedRequest.getScheduleTo()) + 1;
        if (inclusiveDays <= 0) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE);
        }
        if (inclusiveDays > 7) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_DATE_RANGE_TOO_LARGE);
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

        List<Auditorium> auditoriums = preflight.auditoriums();
        List<MovieVersion> movieVersions = preflight.movieVersions();

        try {
            AutoScheduleGenerationContext context = contextLoader.load(
                    normalizedRequest, cinema, auditoriums, movieVersions, strategyVersion);
            int estimatedCandidateCount = candidateCountEstimator.estimate(context);
            List<ShowtimeCandidate> candidates = new ArrayList<>(estimatedCandidateCount);
            CandidateScoringContext scoringContext = new CandidateScoringContext(context);

            long candidateGenerationStarted = System.nanoTime();
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
            metrics.recordCandidateGeneration(
                    Duration.ofNanos(System.nanoTime() - candidateGenerationStarted),
                    generatedCandidateCount);

            if (generatedCandidateCount != estimatedCandidateCount
                    || candidates.size() != estimatedCandidateCount) {
                throw new IllegalStateException("Candidate estimation and generation diverged");
            }

            attachPersistenceReferences(candidates, cinema, auditoriums, movieVersions);
            enrichmentService.enrich(candidates, context);
            long optimizationStarted = System.nanoTime();
            generationStrategy.scoreAndResolveDefaultSelection(candidates, scoringContext);

            if (scoringContext.getOptimizationResult() == null) {
                if (AutoScheduleStrategyVersions.BALANCED_V1_S5.equals(strategyVersion)) {
                    scoringContext.setOptimizationResult(legacyOptimizationResult(
                            candidates, System.nanoTime() - optimizationStarted));
                } else {
                    throw new IllegalStateException("Current demand-aware strategy did not provide solver metadata");
                }
            }
            if (shadowComparisonService != null) {
                shadowComparisonService.compareIfEnabled(strategyVersion, candidates, context);
            }
            ShowtimeSchedulePreview preview;
            try {
                preview = lifecycleService.createOptimizedPreview(
                        normalizedRequest, cinema, strategyVersion, fingerprint, adminUserId,
                        preflight.response(), scoringContext.getOptimizationResult(),
                        candidates.stream().filter(ShowtimeCandidate::isSelected)
                                .map(ShowtimeCandidate::getDemandModelVersion)
                                .filter(Objects::nonNull).findFirst()
                                .orElseThrow(() -> new IllegalStateException("Demand model version is missing")));
            } catch (DataIntegrityViolationException exception) {
                Optional<ShowtimeSchedulePreview> concurrent = previewRepository
                        .findByGenerateIdempotencyKeyWithCinema(normalizedRequest.getIdempotencyKey());
                if (concurrent.isPresent()) {
                    ShowtimeSchedulePreview existing = concurrent.get();
                    if (!matchesStoredFingerprint(normalizedRequest, existing)) {
                        throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
                    }
                    return responseMapper.toSummaryResponse(existing);
                }
                throw exception;
            }

            lifecycleService.persistGeneratedItemsAndMarkPreviewed(preview, candidates);

            List<ShowtimeCandidate> selected = candidates.stream()
                    .filter(ShowtimeCandidate::isSelected).toList();
            BigDecimal expectedContribution = selected.stream()
                    .map(ShowtimeCandidate::getExpectedContribution)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expectedOccupancy = selected.isEmpty() ? BigDecimal.ZERO
                    : selected.stream().map(ShowtimeCandidate::getExpectedOccupancy)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(selected.size()), 6, RoundingMode.HALF_UP);
            metrics.recordGeneration(strategyVersion,
                    Duration.ofNanos(System.nanoTime() - generationStarted),
                    scoringContext.getOptimizationResult(), expectedContribution, expectedOccupancy);

            return responseMapper.toSummaryResponse(preview);

        } catch (BusinessException e) {
            metrics.recordGenerationFailure(observedStrategy,
                    Duration.ofNanos(System.nanoTime() - generationStarted),
                    e.getErrorCode().name());
            log.warn("Auto schedule generation rejected with error {}", e.getErrorCode());
            throw e;
        } catch (DataIntegrityViolationException e) {
            metrics.recordGenerationFailure(observedStrategy,
                    Duration.ofNanos(System.nanoTime() - generationStarted), "conflict");
            throw e;
        } catch (Exception e) {
            metrics.recordGenerationFailure(observedStrategy,
                    Duration.ofNanos(System.nanoTime() - generationStarted), "failed");
            log.error("Auto schedule generation failed before a preview could be published", e);
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED);
        }
    }

    private AutoScheduleOptimizationResult legacyOptimizationResult(
            List<ShowtimeCandidate> candidates, long durationNanos) {
        List<ShowtimeCandidate> selected = candidates.stream()
                .filter(ShowtimeCandidate::isSelected).toList();
        BigDecimal score = selected.stream().map(ShowtimeCandidate::getScore)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AutoScheduleOptimizationResult(
                AutoScheduleOptimizationResult.SolverStatus.FEASIBLE,
                "LEGACY_S5_NO_SOLVER",
                score,
                score,
                Duration.ofNanos(durationNanos).toMillis(),
                selected.size(),
                "Explicit per-cinema LEGACY feature flag selected the retained S5 strategy; no fallback occurred.");
    }

    private AutoSchedulePreflightRequest toPreflightRequest(GenerateShowtimeSchedulePreviewRequest request) {
        AutoSchedulePreflightRequest preflight = new AutoSchedulePreflightRequest();
        preflight.setCinemaPublicId(request.getCinemaPublicId());
        preflight.setPlanningDays(resolvePlanningDays(request));
        preflight.setIncludeMovieVersionPublicIds(request.getMovieVersionPublicIds());
        preflight.setIncludeAuditoriumPublicIds(request.getAuditoriumPublicIds());
        preflight.setExcludeMovieVersionPublicIds(request.getExcludeMovieVersionPublicIds());
        preflight.setExcludeAuditoriumPublicIds(request.getExcludeAuditoriumPublicIds());
        return preflight;
    }

    private int resolvePlanningDays(GenerateShowtimeSchedulePreviewRequest request) {
        if (request.getPlanningDays() != null) return request.getPlanningDays();
        if (request.getScheduleFrom() == null && request.getScheduleTo() == null) return 1;
        if (request.getScheduleFrom() == null || request.getScheduleTo() == null) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE,
                    "scheduleFrom and scheduleTo must both be supplied or both omitted");
        }
        long days = ChronoUnit.DAYS.between(request.getScheduleFrom(), request.getScheduleTo()) + 1;
        if (days != 1 && days != 3 && days != 7) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE,
                    "Planning range must contain exactly 1, 3, or 7 days");
        }
        return (int) days;
    }

    private void validateAuthoritativeRange(GenerateShowtimeSchedulePreviewRequest request,
                                            LocalDate authoritativeFrom,
                                            LocalDate authoritativeTo) {
        if ((request.getScheduleFrom() != null && !request.getScheduleFrom().equals(authoritativeFrom))
                || (request.getScheduleTo() != null && !request.getScheduleTo().equals(authoritativeTo))) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE,
                    "Planning range must be cinema-local tomorrow through " + authoritativeTo);
        }
    }

    private GenerateShowtimeSchedulePreviewRequest toEffectiveRequest(
            GenerateShowtimeSchedulePreviewRequest source,
            AutoSchedulePreflightResult preflight) {
        GenerateShowtimeSchedulePreviewRequest effective = new GenerateShowtimeSchedulePreviewRequest();
        effective.setCinemaPublicId(preflight.cinema().getPublicId());
        effective.setScheduleFrom(preflight.response().planningFrom());
        effective.setScheduleTo(preflight.response().planningTo());
        effective.setMovieVersionPublicIds(preflight.response().eligibleMovieVersionPublicIds());
        effective.setAuditoriumPublicIds(preflight.response().eligibleAuditoriumPublicIds());
        effective.setSlotGranularityMinutes(source.getSlotGranularityMinutes());
        effective.setPreviewTtlMinutes(source.getPreviewTtlMinutes());
        effective.setIdempotencyKey(source.getIdempotencyKey());
        return effective;
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

}
