package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationContextLoader;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.autoschedule.service.CandidateCountEstimator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewGenerationServiceImplTest {

    @Mock private AutoScheduleGenerateRequestNormalizer normalizer;
    @Mock private AutoScheduleRequestFingerprintService fingerprintService;
    @Mock private AutoScheduleGenerationContextLoader contextLoader;
    @Mock private CandidateCountEstimator candidateCountEstimator;
    @Mock private AutoScheduleGenerationContext generationContext;
    @Mock private ShowtimeCandidateGenerator generator;
    @Mock private ShowtimeCandidateValidationService validationService;
    @Mock private AutoScheduleGenerationStrategyRegistry strategyRegistry;
    @Mock private AutoScheduleGenerationStrategy generationStrategy;
    @Mock private ShowtimeSchedulePreviewLifecycleService lifecycleService;
    @Mock private CinemaRepository cinemaRepository;
    @Mock private AuditoriumRepository auditoriumRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock private ShowtimeSchedulePreviewMapper responseMapper;
    @Mock private MovieShowtimeEligibilityPolicy eligibilityPolicy;

    @InjectMocks
    private AutoSchedulePreviewGenerationServiceImpl service;

    private GenerateShowtimeSchedulePreviewRequest request;
    private ShowtimeSchedulePreview preview;

    @BeforeEach
    void setUp() {
        request = new GenerateShowtimeSchedulePreviewRequest();
        LocalDate scheduleDate = LocalDate.now().plusDays(1);
        NormalizedGeneratePreviewRequest normalized = new NormalizedGeneratePreviewRequest(
                "cinema", scheduleDate, scheduleDate, List.of("version"), List.of("auditorium"),
                15, 60, "generation-key");

        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setTimezone("Asia/Ho_Chi_Minh");

        Auditorium auditorium = new Auditorium();
        auditorium.setId(2L);
        auditorium.setPublicId("auditorium");
        auditorium.setCinema(cinema);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);

        Movie movie = new Movie();
        movie.setId(3L);
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(90);

        MovieVersion version = new MovieVersion();
        version.setId(4L);
        version.setPublicId("version");
        version.setMovie(movie);
        version.setStatus(ActiveStatus.ACTIVE);

        preview = ShowtimeSchedulePreview.createGenerating(
                cinema, scheduleDate, scheduleDate, 15, 60,
                "generation-key", "fingerprint", 10L, Instant.now());
        ReflectionTestUtils.setField(preview, "id", 99L);
        ReflectionTestUtils.setField(preview, "publicId", "preview");

        when(normalizer.normalize(request)).thenReturn(normalized);
        when(strategyRegistry.getCurrent()).thenReturn(generationStrategy);
        when(generationStrategy.getStrategyVersion())
                .thenReturn(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3);
        lenient().when(fingerprintService.generateFingerprint(
                normalized, AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3))
                .thenReturn("fingerprint");
        lenient().when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema")).thenReturn(Optional.of(cinema));
        lenient().when(previewRepository.findByGenerateIdempotencyKey("generation-key")).thenReturn(Optional.empty());
        lenient().when(auditoriumRepository.findByPublicIdInAndDeletedAtIsNull(List.of("auditorium")))
                .thenReturn(List.of(auditorium));
        lenient().when(movieVersionRepository.findByPublicIdInWithMovieAndDeletedAtIsNull(List.of("version")))
                .thenReturn(List.of(version));
        lenient().when(lifecycleService.createGeneratingPreview(
                        normalized, cinema, AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3,
                        "fingerprint", 10L))
                .thenReturn(preview);
        lenient().when(contextLoader.load(
                        normalized, cinema, List.of(auditorium), List.of(version),
                        AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3))
                .thenReturn(generationContext);
    }

    @Test
    void knownCandidateLimitErrorIsNotWrappedAndPreviewIsMarkedFailed() {
        BusinessException limitError = new BusinessException(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES);
        when(candidateCountEstimator.estimate(generationContext)).thenThrow(limitError);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES, thrown.getErrorCode());
        verify(fingerprintService).generateFingerprint(
                any(), org.mockito.ArgumentMatchers.eq(
                        AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3));
        verify(lifecycleService).createGeneratingPreview(
                any(), any(), org.mockito.ArgumentMatchers.eq(
                        AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3), any(), anyLong());
        verify(contextLoader).load(
                any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(
                        AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3));
        verify(lifecycleService).markPreviewFailed(anyLong(), any());
    }

    @Test
    void unexpectedGenerationFailureIsWrappedAndPreviewIsMarkedFailed() {
        when(candidateCountEstimator.estimate(generationContext))
                .thenThrow(new IllegalStateException("database unavailable"));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED, thrown.getErrorCode());
        verify(lifecycleService).markPreviewFailed(anyLong(), any());
    }

    @Test
    void selectionInvariantFailureIsPreservedMarksPreviewFailedAndPersistsNoItems() {
        BusinessException invariantFailure =
                new BusinessException(ErrorCode.AUTO_SCHEDULE_SELECTION_INVARIANT_VIOLATION);
        when(candidateCountEstimator.estimate(generationContext)).thenReturn(0);
        when(generator.generate(any(), any())).thenReturn(0L);
        doThrow(invariantFailure).when(generationStrategy)
                .scoreAndResolveDefaultSelection(any(), any());

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_SELECTION_INVARIANT_VIOLATION, thrown.getErrorCode());
        verify(lifecycleService).markPreviewFailed(anyLong(), any());
        verify(lifecycleService, never()).persistGeneratedItemsAndMarkPreviewed(any(), any());
    }

    @Test
    void legacySameRequestReplayReturnsExistingPreviewWithoutRegenerationOrMutation() {
        preview.setStrategyVersion("BALANCED_V1");
        preview.setRequestFingerprint("legacy-fingerprint");
        preview.setTotalCandidateCount(7);
        when(previewRepository.findByGenerateIdempotencyKey("generation-key"))
                .thenReturn(Optional.of(preview));
        when(fingerprintService.generateFingerprint(any(), org.mockito.ArgumentMatchers.eq("BALANCED_V1")))
                .thenReturn("legacy-fingerprint");

        service.generatePreview(request, 10L);

        assertEquals("BALANCED_V1", preview.getStrategyVersion());
        assertEquals("legacy-fingerprint", preview.getRequestFingerprint());
        assertEquals(7, preview.getTotalCandidateCount());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
        verify(generator, never()).generate(any(), any());
        verify(lifecycleService, never()).createGeneratingPreview(any(), any(), any(), any(), anyLong());
    }

    @Test
    void legacyChangedRequestRejectsKeyReuseWithoutRegenerationOrMutation() {
        preview.setStrategyVersion("BALANCED_V1");
        preview.setRequestFingerprint("legacy-fingerprint");
        when(previewRepository.findByGenerateIdempotencyKey("generation-key"))
                .thenReturn(Optional.of(preview));
        when(fingerprintService.generateFingerprint(any(), org.mockito.ArgumentMatchers.eq("BALANCED_V1")))
                .thenReturn("changed-fingerprint");

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.IDEMPOTENCY_KEY_REUSED, thrown.getErrorCode());
        assertEquals("BALANCED_V1", preview.getStrategyVersion());
        assertEquals("legacy-fingerprint", preview.getRequestFingerprint());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
        verify(generator, never()).generate(any(), any());
    }

    @Test
    void phaseS2SameRequestReplayReturnsExistingPreviewWithoutRegeneration() {
        preview.setStrategyVersion(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2);
        preview.setRequestFingerprint("s2-fingerprint");
        preview.setSelectedCandidateCount(4);
        when(previewRepository.findByGenerateIdempotencyKey("generation-key"))
                .thenReturn(Optional.of(preview));
        when(fingerprintService.generateFingerprint(
                any(), org.mockito.ArgumentMatchers.eq(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2)))
                .thenReturn("s2-fingerprint");

        service.generatePreview(request, 10L);

        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2, preview.getStrategyVersion());
        assertEquals(4, preview.getSelectedCandidateCount());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
        verify(generator, never()).generate(any(), any());
    }

    @Test
    void phaseS2ChangedRequestRejectsKeyReuseWithoutMutation() {
        preview.setStrategyVersion(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2);
        preview.setRequestFingerprint("s2-fingerprint");
        when(previewRepository.findByGenerateIdempotencyKey("generation-key"))
                .thenReturn(Optional.of(preview));
        when(fingerprintService.generateFingerprint(
                any(), org.mockito.ArgumentMatchers.eq(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2)))
                .thenReturn("changed-fingerprint");

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.IDEMPOTENCY_KEY_REUSED, thrown.getErrorCode());
        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S2, preview.getStrategyVersion());
        assertEquals("s2-fingerprint", preview.getRequestFingerprint());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
        verify(generator, never()).generate(any(), any());
    }

    @Test
    void unknownStoredStrategyVersionFailsAsInconsistentWithoutRegeneration() {
        preview.setStrategyVersion("BALANCED_UNKNOWN");
        preview.setRequestFingerprint("stored-fingerprint");
        when(previewRepository.findByGenerateIdempotencyKey("generation-key"))
                .thenReturn(Optional.of(preview));
        when(fingerprintService.generateFingerprint(
                any(), org.mockito.ArgumentMatchers.eq("BALANCED_UNKNOWN")))
                .thenThrow(new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT, thrown.getErrorCode());
        assertEquals("BALANCED_UNKNOWN", preview.getStrategyVersion());
        assertEquals("stored-fingerprint", preview.getRequestFingerprint());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
        verify(generator, never()).generate(any(), any());
        verify(lifecycleService, never()).createGeneratingPreview(any(), any(), any(), any(), anyLong());
    }

    @Test
    void eightInclusiveServiceDatesAreRejectedBeforePersistence() {
        LocalDate scheduleFrom = LocalDate.now().plusDays(30);
        NormalizedGeneratePreviewRequest eightDayRequest = new NormalizedGeneratePreviewRequest(
                "cinema", scheduleFrom, scheduleFrom.plusDays(7), List.of("version"),
                List.of("auditorium"), 15, 60, "eight-day-key");
        when(normalizer.normalize(request)).thenReturn(eightDayRequest);
        when(fingerprintService.generateFingerprint(
                eightDayRequest, AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3))
                .thenReturn("eight-day-fingerprint");

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_DATE_RANGE_TOO_LARGE, thrown.getErrorCode());
        verify(lifecycleService, never()).createGeneratingPreview(any(), any(), any(), any(), anyLong());
    }

    @Test
    void sevenDayBatchStartingThirtyDaysAheadIsNotRejectedForFutureHorizon() {
        LocalDate scheduleFrom = LocalDate.now().plusDays(30);
        NormalizedGeneratePreviewRequest futureRequest = new NormalizedGeneratePreviewRequest(
                "cinema", scheduleFrom, scheduleFrom.plusDays(6), List.of("version"),
                List.of("auditorium"), 15, 60, "future-key");
        when(normalizer.normalize(request)).thenReturn(futureRequest);
        when(fingerprintService.generateFingerprint(
                futureRequest, AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3))
                .thenReturn("future-fingerprint");
        when(previewRepository.findByGenerateIdempotencyKey("future-key")).thenReturn(Optional.empty());
        when(lifecycleService.createGeneratingPreview(any(), any(), any(), any(), anyLong()))
                .thenReturn(preview);
        when(contextLoader.load(
                org.mockito.ArgumentMatchers.eq(futureRequest), any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3)))
                .thenReturn(generationContext);

        assertDoesNotThrow(() -> service.generatePreview(request, 10L));
        verify(lifecycleService).createGeneratingPreview(
                any(), any(), org.mockito.ArgumentMatchers.eq(
                        AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3), any(), anyLong());
    }

    @Test
    void ineligibleMovieIsRejectedBeforePreviewPersistence() {
        BusinessException statusError =
                new BusinessException(ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING);
        doThrow(statusError).when(eligibilityPolicy)
                .validateMovieAndVersion(any(Movie.class), any(MovieVersion.class));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING, thrown.getErrorCode());
        verify(lifecycleService, never()).createGeneratingPreview(any(), any(), any(), any(), anyLong());
        verify(contextLoader, never()).load(any(), any(), any(), any(), any());
    }
}
