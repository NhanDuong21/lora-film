package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.AutoScheduleExistingShowtimeFact;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import com.lorafilm.movie.showtime.validation.ShowtimeSchedulingRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoScheduleGenerationContextLoaderImplTest {

    @Mock private CinemaOperatingHourRepository operatingHourRepository;
    @Mock private CinemaClosurePeriodRepository closureRepository;
    @Mock private AuditoriumMaintenanceWindowRepository maintenanceRepository;
    @Mock private ShowtimeRepository showtimeRepository;

    private AutoScheduleGenerationContextLoaderImpl loader;
    private Cinema cinema;
    private Auditorium auditorium;
    private MovieVersion movieVersion;
    private NormalizedGeneratePreviewRequest request;

    @BeforeEach
    void setUp() {
        loader = new AutoScheduleGenerationContextLoaderImpl(
                operatingHourRepository, closureRepository, maintenanceRepository, showtimeRepository,
                new CinemaOperatingWindowResolver(operatingHourRepository),
                new ExistingShowtimeServiceDateClassifier());
        cinema = cinema();
        auditorium = auditorium(2L, "aud-1", 20);
        movieVersion = movieVersion();
        LocalDate date = LocalDate.of(2026, 7, 22);
        request = new NormalizedGeneratePreviewRequest(
                "cinema", date, date, List.of("version"), List.of("aud-1"),
                30, 60, "key");
        CinemaOperatingHour hour = new CinemaOperatingHour();
        hour.setDayOfWeek(date.getDayOfWeek().getValue());
        hour.setOpenTime(LocalTime.of(8, 0));
        hour.setCloseTime(LocalTime.of(12, 0));
        hour.setIsClosed(false);
        when(operatingHourRepository.findByCinemaId(1L)).thenReturn(List.of(hour));
        when(closureRepository.findOverlappingClosures(eq(1L), any(), any())).thenReturn(List.of());
        when(maintenanceRepository.findActiveOverlapsForAutoSchedule(
                anyList(), eq(ActionStatus.ACTIVE), any(), any())).thenReturn(List.of());
        when(showtimeRepository.findBlockingFactsForAutoSchedule(anyList(), any(), any())).thenReturn(List.of());
    }

    @Test
    void currentS5LoadsEveryContextRepositoryCategoryOnceAndCandidateLoopsStayRepositoryFree() {
        AutoScheduleGenerationContext context = loader.load(
                request, cinema, List.of(auditorium), List.of(movieVersion));

        ShowtimeCandidateGeneratorImpl generator = new ShowtimeCandidateGeneratorImpl(
                new UniqueCandidateSlotTraversalImpl());
        ShowtimeCandidateValidationServiceImpl validator = new ShowtimeCandidateValidationServiceImpl(
                new MovieShowtimeEligibilityPolicy(), new ShowtimeSchedulingRules());
        BalancedCandidateScoringServiceImpl scorer = new BalancedCandidateScoringServiceImpl();
        CandidateScoringContext scoringContext = new CandidateScoringContext(context);
        List<ShowtimeCandidate> candidates = generator.generate(context);
        for (ShowtimeCandidate candidate : candidates) {
            CandidateValidationResult result = validator.validate(candidate, context);
            candidate.setValidationStatus(result.isValid()
                    ? PreviewItemValidationStatus.VALID : PreviewItemValidationStatus.REJECTED);
            scorer.score(candidate, scoringContext);
        }

        assertEquals(7, candidates.size());
        verify(operatingHourRepository, times(1)).findByCinemaId(1L);
        verify(closureRepository, times(1)).findOverlappingClosures(eq(1L), any(), any());
        verify(maintenanceRepository, times(1)).findActiveOverlapsForAutoSchedule(
                anyList(), eq(ActionStatus.ACTIVE), any(), any());
        verify(showtimeRepository, times(1)).findBlockingFactsForAutoSchedule(anyList(), any(), any());
        verify(showtimeRepository, times(1)).findCoverageFactsForAutoSchedule(
                eq(1L), eq(List.of(movieVersion.getMovie().getId())), anyList(), any(), any());
        verifyNoMoreInteractions(operatingHourRepository, closureRepository,
                maintenanceRepository, showtimeRepository);
    }

    @Test
    void existingOccupancyUsesMatchingAuditoriumBufferAndContinuityFactsRemainIndividual() {
        Auditorium noBufferAuditorium = auditorium(3L, "aud-2", 0);
        Showtime bufferedFirst = showtime(auditorium,
                "2026-07-22T09:00:00Z", "2026-07-22T10:00:00Z");
        Showtime bufferedSecond = showtime(auditorium,
                "2026-07-22T09:30:00Z", "2026-07-22T10:05:00Z");
        Showtime unbuffered = showtime(noBufferAuditorium,
                "2026-07-22T09:00:00Z", "2026-07-22T10:00:00Z");
        when(showtimeRepository.findBlockingFactsForAutoSchedule(anyList(), any(), any()))
                .thenReturn(List.of(bufferedFirst, bufferedSecond, unbuffered));

        AutoScheduleGenerationContext context = loader.load(
                request, cinema, List.of(auditorium, noBufferAuditorium), List.of(movieVersion));
        Instant tenTen = Instant.parse("2026-07-22T10:10:00Z");

        assertTrue(context.showtimeConflictsFor(2L).overlaps(
                tenTen, tenTen.plusSeconds(60)));
        assertFalse(context.showtimeConflictsFor(3L).overlaps(
                tenTen, tenTen.plusSeconds(60)));
        assertEquals(1, context.showtimeConflictsFor(2L).intervals().size(),
                "overlapping conflict ranges may be merged");
        assertEquals(2, context.continuityFor(2L).occupancyEnds().size(),
                "continuity must retain each occupancy end");
        assertEquals(Instant.parse("2026-07-22T10:20:00Z"),
                context.continuityFor(2L).occupancyEnds().get(0));
        assertEquals(Instant.parse("2026-07-22T10:25:00Z"),
                context.continuityFor(2L).occupancyEnds().get(1));
        assertEquals(Instant.parse("2026-07-22T10:00:00Z"),
                context.continuityFor(3L).occupancyEnds().get(0));
    }

    @Test
    void retainsTheExplicitHistoricalS3StrategyVersionWhileS5RemainsCurrent() {
        AutoScheduleGenerationContext context = loader.load(
                request, cinema, List.of(auditorium), List.of(movieVersion),
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3);

        assertEquals(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3,
                context.getStrategyVersion());
        assertEquals(AutoScheduleStrategyVersions.BALANCED_V1_S5,
                AutoScheduleStrategyVersions.CURRENT);
    }

    @Test
    void s5LoadsOneBoundedCinemaWideCoverageProjectionAndClassifiesCounts() {
        AutoScheduleExistingShowtimeFact first = fairnessFact(
                4L, Instant.parse("2026-07-22T08:30:00Z"));
        AutoScheduleExistingShowtimeFact second = fairnessFact(
                4L, Instant.parse("2026-07-22T09:30:00Z"));
        when(showtimeRepository.findCoverageFactsForAutoSchedule(
                eq(1L), eq(List.of(4L)), eq(List.of(
                        ShowtimeStatus.DRAFT,
                        ShowtimeStatus.OPEN_FOR_BOOKING,
                        ShowtimeStatus.CLOSED,
                        ShowtimeStatus.FINISHED)),
                eq(Instant.parse("2026-07-22T08:00:00Z")),
                eq(Instant.parse("2026-07-22T12:00:00Z"))))
                .thenReturn(List.of(first, second));

        AutoScheduleGenerationContext context = loader.load(
                request, cinema, List.of(auditorium), List.of(movieVersion),
                AutoScheduleStrategyVersions.BALANCED_V1_S5);

        assertEquals(2, context.existingShowtimeCount(
                LocalDate.of(2026, 7, 22), 4L));
        verify(showtimeRepository).findCoverageFactsForAutoSchedule(
                eq(1L), eq(List.of(4L)), anyList(), any(), any());
    }

    private AutoScheduleExistingShowtimeFact fairnessFact(
            Long movieId, Instant startTime) {
        AutoScheduleExistingShowtimeFact fact = mock(AutoScheduleExistingShowtimeFact.class);
        when(fact.getMovieId()).thenReturn(movieId);
        when(fact.getStartTime()).thenReturn(startTime);
        return fact;
    }

    private Cinema cinema() {
        Cinema value = new Cinema();
        value.setId(1L);
        value.setPublicId("cinema");
        value.setName("Cinema");
        value.setTimezone("UTC");
        value.setStatus(CinemaStatus.ACTIVE);
        return value;
    }

    private Auditorium auditorium(Long id, String publicId, int buffer) {
        Auditorium value = new Auditorium();
        value.setId(id);
        value.setPublicId(publicId);
        value.setCinema(cinema);
        value.setName(publicId);
        value.setCapacity(100);
        value.setCleaningBufferMinutes(buffer);
        value.setStatus(AuditoriumStatus.ACTIVE);
        return value;
    }

    private MovieVersion movieVersion() {
        Movie movie = new Movie();
        movie.setId(4L);
        movie.setPublicId("movie");
        movie.setTitle("Movie");
        movie.setDurationMinutes(60);
        movie.setStatus(MovieStatus.NOW_SHOWING);
        MovieVersion version = new MovieVersion();
        version.setId(5L);
        version.setPublicId("version");
        version.setMovie(movie);
        version.setStatus(ActiveStatus.ACTIVE);
        return version;
    }

    private Showtime showtime(Auditorium room, String start, String end) {
        Showtime value = new Showtime();
        value.setAuditorium(room);
        value.setStartTime(Instant.parse(start));
        value.setEndTime(Instant.parse(end));
        return value;
    }
}
