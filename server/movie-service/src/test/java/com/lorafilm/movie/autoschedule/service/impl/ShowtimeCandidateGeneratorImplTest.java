package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.ImmutableIntervalIndex;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowtimeCandidateGeneratorImplTest {

    private final ShowtimeCandidateGeneratorImpl generator =
            new ShowtimeCandidateGeneratorImpl(new UniqueCandidateSlotTraversalImpl());

    @Test
    void generateOnlyCreatesStartsWhoseFilmEndsInsideWindow() {
        Instant open = Instant.parse("2026-07-22T08:00:00Z");
        AutoScheduleGenerationContext context = context(
                30, 90, 15, List.of(new OperatingWindow(LocalDate.of(2026, 7, 22),
                        open, open.plus(2, ChronoUnit.HOURS))));

        List<ShowtimeCandidate> candidates = generator.generate(context);

        assertEquals(2, candidates.size());
        assertEquals(open, candidates.get(0).getStartTime());
        assertEquals(open.plus(90, ChronoUnit.MINUTES), candidates.get(0).getEndTime());
        assertEquals(open.plus(105, ChronoUnit.MINUTES), candidates.get(0).getOccupancyEndTime());
        assertEquals(open.plus(30, ChronoUnit.MINUTES), candidates.get(1).getStartTime());
        assertEquals(LocalDate.of(2026, 7, 22),
                candidates.get(0).getOperatingWindow().getServiceDate());
    }

    @Test
    void closingEqualityIsIncludedAndCleaningMayFinishAfterClose() {
        Instant open = Instant.parse("2026-07-22T08:00:00Z");
        AutoScheduleGenerationContext context = context(
                15, 60, 25, List.of(new OperatingWindow(LocalDate.of(2026, 7, 22),
                        open, open.plus(60, ChronoUnit.MINUTES))));

        List<ShowtimeCandidate> candidates = generator.generate(context);

        assertEquals(1, candidates.size());
        assertEquals(open.plus(60, ChronoUnit.MINUTES), candidates.get(0).getEndTime());
        assertEquals(open.plus(85, ChronoUnit.MINUTES), candidates.get(0).getOccupancyEndTime());
    }

    @Test
    void afterMidnightCandidateRetainsPriorOperatingServiceDate() {
        LocalDate serviceDate = LocalDate.of(2026, 7, 24);
        Instant open = Instant.parse("2026-07-24T20:00:00Z");
        AutoScheduleGenerationContext context = context(
                30, 60, 0, List.of(new OperatingWindow(
                        serviceDate, open, Instant.parse("2026-07-25T02:00:00Z"))));

        ShowtimeCandidate afterMidnight = generator.generate(context).stream()
                .filter(candidate -> candidate.getStartTime().equals(
                        Instant.parse("2026-07-25T00:30:00Z")))
                .findFirst()
                .orElseThrow();

        assertEquals(serviceDate, afterMidnight.getOperatingWindow().getServiceDate());
    }

    @Test
    void candidatesFromMultipleWindowsOnSameServiceDateRetainThatDate() {
        LocalDate serviceDate = LocalDate.of(2026, 7, 24);
        Instant morning = Instant.parse("2026-07-24T08:00:00Z");
        Instant evening = Instant.parse("2026-07-24T20:00:00Z");
        AutoScheduleGenerationContext context = context(
                60, 60, 0, List.of(
                        new OperatingWindow(serviceDate, morning, morning.plus(2, ChronoUnit.HOURS)),
                        new OperatingWindow(serviceDate, evening, evening.plus(2, ChronoUnit.HOURS))));

        List<ShowtimeCandidate> candidates = generator.generate(context);

        assertEquals(4, candidates.size());
        candidates.forEach(candidate -> assertEquals(
                serviceDate, candidate.getOperatingWindow().getServiceDate()));
    }

    static AutoScheduleGenerationContext context(int granularity,
                                                 int duration,
                                                 int cleaningBuffer,
                                                 List<OperatingWindow> windows) {
        AutoScheduleGenerationContext.CinemaSnapshot cinema =
                new AutoScheduleGenerationContext.CinemaSnapshot(
                        1L, "cinema", "Cinema", ZoneId.of("UTC"), CinemaStatus.ACTIVE, false);
        AutoScheduleGenerationContext.AuditoriumSnapshot auditorium =
                new AutoScheduleGenerationContext.AuditoriumSnapshot(
                        2L, "auditorium", 1L, "Room", 100, cleaningBuffer,
                        AuditoriumStatus.ACTIVE, false);
        AutoScheduleGenerationContext.MovieSnapshot movie =
                new AutoScheduleGenerationContext.MovieSnapshot(
                        3L, "movie", "Movie", duration, null, null,
                        MovieStatus.NOW_SHOWING, false);
        AutoScheduleGenerationContext.MovieVersionSnapshot version =
                new AutoScheduleGenerationContext.MovieVersionSnapshot(
                        4L, "version", 3L, ActiveStatus.ACTIVE, false, movie);
        Instant planningStart = windows.isEmpty() ? null : windows.get(0).getOpenInstant();
        Instant planningEnd = windows.isEmpty() ? null : windows.get(windows.size() - 1).getCloseInstant();
        return new AutoScheduleGenerationContext(
                cinema, LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 22),
                granularity, 10_000, AutoScheduleStrategy.BALANCED,
                AutoScheduleStrategyVersions.CURRENT, List.of(auditorium), List.of(version), windows,
                Set.of(3), ImmutableIntervalIndex.empty(), Map.of(), Map.of(), Map.of(),
                planningStart, planningEnd);
    }
}
