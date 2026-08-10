package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreflightResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoSchedulePreflightResult;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleCandidateEnrichmentService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationContextLoader;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreflightService;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.autoschedule.service.CandidateCountEstimator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewGenerationServiceImplTest {

    @Mock private AutoScheduleGenerateRequestNormalizer normalizer;
    @Mock private AutoScheduleRequestFingerprintService fingerprintService;
    @Mock private AutoScheduleGenerationContextLoader contextLoader;
    @Mock private CandidateCountEstimator candidateCountEstimator;
    @Mock private ShowtimeCandidateGenerator generator;
    @Mock private ShowtimeCandidateValidationService validationService;
    @Mock private AutoSchedulePreflightService preflightService;
    @Mock private AutoScheduleCandidateEnrichmentService enrichmentService;
    @Mock private AutoScheduleGenerationStrategyRegistry strategyRegistry;
    @Mock private AutoScheduleGenerationStrategy strategy;
    @Mock private ShowtimeSchedulePreviewLifecycleService lifecycleService;
    @Mock private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock private ShowtimeSchedulePreviewMapper mapper;
    @Mock private AutoScheduleGenerationContext context;

    private AutoSchedulePreviewGenerationServiceImpl service;
    private GenerateShowtimeSchedulePreviewRequest request;
    private NormalizedGeneratePreviewRequest normalized;
    private Cinema cinema;
    private Auditorium auditorium;
    private MovieVersion version;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        service = new AutoSchedulePreviewGenerationServiceImpl(
                normalizer, fingerprintService, contextLoader, candidateCountEstimator,
                generator, validationService, preflightService, enrichmentService,
                strategyRegistry, lifecycleService, previewRepository, mapper);
        tomorrow = LocalDate.of(2026, 8, 7);
        request = new GenerateShowtimeSchedulePreviewRequest();
        request.setCinemaPublicId("cinema-1");
        request.setPlanningDays(1);
        request.setIdempotencyKey("key-1");
        normalized = new NormalizedGeneratePreviewRequest(
                "cinema-1", tomorrow, tomorrow, List.of("version-1"), List.of("aud-1"),
                15, 60, "key-1");
        cinema = new Cinema();
        cinema.setPublicId("cinema-1");
        auditorium = new Auditorium();
        auditorium.setPublicId("aud-1");
        version = new MovieVersion();
        version.setPublicId("version-1");
        org.mockito.Mockito.lenient().when(strategyRegistry.getForCinema(any())).thenReturn(strategy);
        org.mockito.Mockito.lenient().when(strategy.getStrategyVersion())
                .thenReturn(AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1);
    }

    @Test
    void blockedPreflightStopsBeforeNormalizationOrCandidateQueries() {
        when(preflightService.prepare(any())).thenReturn(preflight(false));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_PREFLIGHT_BLOCKED, error.getErrorCode());
        verify(normalizer, never()).normalize(any());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
    }

    @Test
    void clientSuppliedRangeCannotOverrideAuthoritativeTomorrowScope() {
        request.setScheduleFrom(tomorrow.plusDays(1));
        request.setScheduleTo(tomorrow.plusDays(1));
        when(preflightService.prepare(any())).thenReturn(preflight(true));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE, error.getErrorCode());
        verify(normalizer, never()).normalize(any());
    }

    @Test
    void sameIdempotentRequestReturnsImmutableExistingPreview() {
        ShowtimeSchedulePreview existing = org.mockito.Mockito.mock(ShowtimeSchedulePreview.class);
        ShowtimeSchedulePreviewSummaryResponse response = new ShowtimeSchedulePreviewSummaryResponse();
        when(preflightService.prepare(any())).thenReturn(preflight(true));
        when(normalizer.normalize(any())).thenReturn(normalized);
        when(fingerprintService.generateFingerprint(normalized, AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1))
                .thenReturn("fingerprint");
        when(previewRepository.findByGenerateIdempotencyKey("key-1")).thenReturn(Optional.of(existing));
        when(existing.getStrategyVersion()).thenReturn(AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1);
        when(existing.getRequestFingerprint()).thenReturn("fingerprint");
        when(mapper.toSummaryResponse(existing)).thenReturn(response);

        assertSame(response, service.generatePreview(request, 10L));
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
        verify(lifecycleService, never()).createOptimizedPreview(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void knownCandidateLimitErrorIsPreservedAndNoPartialPreviewIsPublished() {
        BusinessException limit = new BusinessException(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES);
        when(preflightService.prepare(any())).thenReturn(preflight(true));
        when(normalizer.normalize(any())).thenReturn(normalized);
        when(fingerprintService.generateFingerprint(normalized, AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1))
                .thenReturn("fingerprint");
        when(previewRepository.findByGenerateIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(contextLoader.load(eq(normalized), eq(cinema), any(), any(),
                eq(AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1))).thenReturn(context);
        when(candidateCountEstimator.estimate(context)).thenThrow(limit);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertSame(limit, error);
        verify(lifecycleService, never()).createOptimizedPreview(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    private AutoSchedulePreflightResult preflight(boolean ready) {
        AutoSchedulePreflightResponse response = new AutoSchedulePreflightResponse(
                ready, tomorrow, tomorrow, "Asia/Ho_Chi_Minh", 1, 1, 1, 1,
                ready ? List.of() : List.of(new AutoSchedulePreflightResponse.Blocker(
                        "PRICING_INCOMPLETE", "Missing price", "/admin/pricing")),
                "eligibility", "pricing", "configuration",
                List.of("version-1"), List.of("aud-1"));
        return new AutoSchedulePreflightResult(
                response, cinema, List.of(auditorium), List.of(version), Set.of());
    }
}
