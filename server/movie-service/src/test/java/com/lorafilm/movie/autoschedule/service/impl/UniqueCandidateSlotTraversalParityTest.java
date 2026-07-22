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
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniqueCandidateSlotTraversalParityTest {

    private final UniqueCandidateSlotTraversalImpl traversal = new UniqueCandidateSlotTraversalImpl();
    private final CandidateCountEstimatorImpl estimator = new CandidateCountEstimatorImpl(traversal);
    private final ShowtimeCandidateGeneratorImpl generator = new ShowtimeCandidateGeneratorImpl(traversal);

    @Test
    void matrixEstimateEqualsGeneratedCount() {
        int[] granularities = {1, 7, 15, 30};
        int[] durations = {1, 59, 60, 121};
        int[] buffers = {0, 15};
        int[] windowMinutes = {0, 30, 60, 125, 240};
        Instant base = Instant.parse("2026-07-22T00:00:00Z");

        for (int granularity : granularities) {
            for (int duration : durations) {
                for (int buffer : buffers) {
                    for (int length : windowMinutes) {
                        OperatingWindow window = new OperatingWindow(
                                LocalDate.of(2026, 7, 22), base,
                                base.plus(length, ChronoUnit.MINUTES));
                        assertParity(context(ZoneId.of("UTC"), granularity, 10_000,
                                List.of(window), 1, List.of(duration), buffer));
                    }
                }
            }
        }
    }

    @Test
    void seededRandomizedOverlappingWindowParity() {
        Random random = new Random(0x5_2_2026L);
        Instant base = Instant.parse("2026-10-01T00:00:00Z");
        int[] granularities = {1, 5, 7, 15, 30};

        for (int sample = 0; sample < 200; sample++) {
            int granularity = granularities[random.nextInt(granularities.length)];
            int windowCount = 1 + random.nextInt(3);
            List<OperatingWindow> windows = new ArrayList<>();
            for (int w = 0; w < windowCount; w++) {
                int openOffset = random.nextInt(181);
                int length = 30 + random.nextInt(331);
                windows.add(new OperatingWindow(LocalDate.of(2026, 10, 1),
                        base.plus(openOffset, ChronoUnit.MINUTES),
                        base.plus(openOffset + length, ChronoUnit.MINUTES)));
            }
            List<Integer> durations = new ArrayList<>();
            int versionCount = 1 + random.nextInt(3);
            for (int v = 0; v < versionCount; v++) {
                durations.add(1 + random.nextInt(180));
            }
            assertParity(context(ZoneId.of("UTC"), granularity, 10_000, windows,
                    1 + random.nextInt(3), durations, random.nextInt(31)));
        }
    }

    @Test
    void overlappingWindowsAreDeduplicatedAndEarliestWindowOwnsSlot() {
        Instant base = Instant.parse("2026-07-22T08:00:00Z");
        OperatingWindow earliest = new OperatingWindow(LocalDate.of(2026, 7, 22),
                base, base.plus(4, ChronoUnit.HOURS));
        OperatingWindow overlapping = new OperatingWindow(LocalDate.of(2026, 7, 22),
                base.plus(1, ChronoUnit.HOURS), base.plus(3, ChronoUnit.HOURS));
        AutoScheduleGenerationContext context = context(ZoneId.of("UTC"), 30, 10_000,
                List.of(overlapping, earliest), 1, List.of(60), 0);

        List<ShowtimeCandidate> candidates = generator.generate(context);

        assertEquals(7, estimator.estimate(context));
        assertEquals(7, candidates.size());
        candidates.forEach(candidate -> assertSame(earliest, candidate.getOperatingWindow()));
    }

    @Test
    void exactTenThousandIsAcceptedAndTenThousandOneStopsBeforeMaterializingExtraSlot() {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        AutoScheduleGenerationContext exact = context(ZoneId.of("UTC"), 1, 10_000,
                List.of(new OperatingWindow(LocalDate.of(2026, 1, 1), base,
                        base.plus(10_000, ChronoUnit.MINUTES))), 1, List.of(1), 0);
        assertEquals(10_000, estimator.estimate(exact));
        assertEquals(10_000, generator.generate(exact).size());

        AutoScheduleGenerationContext tooLarge = context(ZoneId.of("UTC"), 1, 10_000,
                List.of(new OperatingWindow(LocalDate.of(2026, 1, 1), base,
                        base.plus(10_001, ChronoUnit.MINUTES))), 1, List.of(1), 0);
        BusinessException error = assertThrows(BusinessException.class,
                () -> estimator.estimate(tooLarge));
        assertEquals(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES, error.getErrorCode());

        AtomicInteger emittedObjects = new AtomicInteger();
        assertEquals(10_001, traversal.traverse(tooLarge, 10_000,
                ignored -> emittedObjects.incrementAndGet()));
        assertEquals(10_000, emittedObjects.get(), "slot 10,001 must be detected before callback materialization");
    }

    @Test
    void nonDivisibleGranularityAndDstUseResolvedInstantBoundaries() {
        Instant open = Instant.parse("2026-07-22T08:00:00Z");
        AutoScheduleGenerationContext nonDivisible = context(ZoneId.of("UTC"), 30, 10_000,
                List.of(new OperatingWindow(LocalDate.of(2026, 7, 22), open,
                        open.plus(125, ChronoUnit.MINUTES))), 1, List.of(60), 0);
        assertEquals(3, estimator.estimate(nonDivisible));
        assertParity(nonDivisible);

        ZoneId newYork = ZoneId.of("America/New_York");
        ZonedDateTime dstOpen = LocalDate.of(2026, 3, 8).atTime(0, 0).atZone(newYork);
        ZonedDateTime dstClose = LocalDate.of(2026, 3, 8).atTime(4, 0).atZone(newYork);
        AutoScheduleGenerationContext dst = context(newYork, 60, 10_000,
                List.of(new OperatingWindow(LocalDate.of(2026, 3, 8),
                        dstOpen.toInstant(), dstClose.toInstant())), 1, List.of(60), 0);
        assertEquals(3, estimator.estimate(dst));
        assertParity(dst);
    }

    private void assertParity(AutoScheduleGenerationContext context) {
        assertEquals(estimator.estimate(context), generator.generate(context).size());
    }

    private AutoScheduleGenerationContext context(ZoneId zoneId,
                                                  int granularity,
                                                  int limit,
                                                  List<OperatingWindow> windows,
                                                  int auditoriumCount,
                                                  List<Integer> durations,
                                                  int buffer) {
        AutoScheduleGenerationContext.CinemaSnapshot cinema =
                new AutoScheduleGenerationContext.CinemaSnapshot(
                        1L, "cinema", "Cinema", zoneId, CinemaStatus.ACTIVE, false);
        List<AutoScheduleGenerationContext.AuditoriumSnapshot> auditoriums = new ArrayList<>();
        for (int i = 0; i < auditoriumCount; i++) {
            auditoriums.add(new AutoScheduleGenerationContext.AuditoriumSnapshot(
                    10L + i, "aud-" + i, 1L, "Room " + i, 100, buffer,
                    AuditoriumStatus.ACTIVE, false));
        }
        List<AutoScheduleGenerationContext.MovieVersionSnapshot> versions = new ArrayList<>();
        for (int i = 0; i < durations.size(); i++) {
            AutoScheduleGenerationContext.MovieSnapshot movie =
                    new AutoScheduleGenerationContext.MovieSnapshot(
                            100L + i, "movie-" + i, "Movie " + i, durations.get(i),
                            null, null, MovieStatus.NOW_SHOWING, false);
            versions.add(new AutoScheduleGenerationContext.MovieVersionSnapshot(
                    200L + i, "version-" + i, movie.id(), ActiveStatus.ACTIVE, false, movie));
        }
        Instant planningStart = windows.stream().map(OperatingWindow::getOpenInstant)
                .min(Instant::compareTo).orElse(null);
        Instant planningEnd = windows.stream().map(OperatingWindow::getCloseInstant)
                .max(Instant::compareTo).orElse(null);
        return new AutoScheduleGenerationContext(
                cinema, windows.isEmpty() ? LocalDate.of(2026, 1, 1) : windows.get(0).getServiceDate(),
                windows.isEmpty() ? LocalDate.of(2026, 1, 1) : windows.get(0).getServiceDate(),
                granularity, limit, AutoScheduleStrategy.BALANCED, AutoScheduleStrategyVersions.CURRENT,
                auditoriums, versions, windows, Set.of(1, 2, 3, 4, 5, 6, 7),
                ImmutableIntervalIndex.empty(), Map.of(), Map.of(), Map.of(),
                planningStart, planningEnd);
    }
}
