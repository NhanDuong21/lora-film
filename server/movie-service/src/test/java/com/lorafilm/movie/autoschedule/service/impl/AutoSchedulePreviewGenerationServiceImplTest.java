package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewMapper;
import com.lorafilm.movie.autoschedule.mapper.ShowtimeSchedulePreviewPersistenceMapper;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerateRequestNormalizer;
import com.lorafilm.movie.autoschedule.service.AutoScheduleRequestFingerprintService;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
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
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewGenerationServiceImplTest {

    @Mock private AutoScheduleGenerateRequestNormalizer normalizer;
    @Mock private AutoScheduleRequestFingerprintService fingerprintService;
    @Mock private ShowtimeCandidateGenerator generator;
    @Mock private ShowtimeCandidateValidationService validationService;
    @Mock private BalancedCandidateScoringService scoringService;
    @Mock private CandidateSelectionResolver selectionResolver;
    @Mock private ShowtimeSchedulePreviewLifecycleService lifecycleService;
    @Mock private CinemaOperatingWindowResolver windowResolver;
    @Mock private CinemaRepository cinemaRepository;
    @Mock private AuditoriumRepository auditoriumRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock private ShowtimeSchedulePreviewItemRepository previewItemRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private ShowtimeSchedulePreviewPersistenceMapper persistenceMapper;
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
        when(fingerprintService.generateFingerprint(normalized)).thenReturn("fingerprint");
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema")).thenReturn(Optional.of(cinema));
        when(previewRepository.findByGenerateIdempotencyKey("generation-key")).thenReturn(Optional.empty());
        when(auditoriumRepository.findByPublicIdInAndDeletedAtIsNull(List.of("auditorium")))
                .thenReturn(List.of(auditorium));
        when(movieVersionRepository.findByPublicIdInWithMovieAndDeletedAtIsNull(List.of("version")))
                .thenReturn(List.of(version));
        when(lifecycleService.createGeneratingPreview(normalized, cinema, "fingerprint", 10L))
                .thenReturn(preview);
    }

    @Test
    void knownCandidateLimitErrorIsNotWrappedAndPreviewIsMarkedFailed() {
        BusinessException limitError = new BusinessException(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES);
        when(generator.generate(any())).thenThrow(limitError);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES, thrown.getErrorCode());
        verify(lifecycleService).markPreviewFailed(anyLong(), any());
    }

    @Test
    void unexpectedGenerationFailureIsWrappedAndPreviewIsMarkedFailed() {
        when(generator.generate(any())).thenThrow(new IllegalStateException("database unavailable"));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.generatePreview(request, 10L));

        assertEquals(ErrorCode.AUTO_SCHEDULE_GENERATION_FAILED, thrown.getErrorCode());
        verify(lifecycleService).markPreviewFailed(anyLong(), any());
    }
}
