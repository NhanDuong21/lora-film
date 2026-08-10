package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateSelectionResolverImplTest {

    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 7, 24);
    private static final Instant BASE = Instant.parse("2026-07-24T00:00:00Z");

    private static final Comparator<ShowtimeCandidate> CANONICAL_ORDER = Comparator
            .comparing(ShowtimeCandidate::getOccupancyEndTime)
            .thenComparing(ShowtimeCandidate::getStartTime)
            .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
            .thenComparing(ShowtimeCandidate::getMovieVersionPublicId)
            .thenComparing(candidate -> candidate.getOperatingWindow().getServiceDate());

    private CandidateSelectionResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new CandidateSelectionResolverImpl();
    }

    @Test
    void weightedSchedulingBeatsKnownGreedyCounterexampleAndPreservesRanking() {
        ShowtimeCandidate longCandidate = candidate(1L, "aud-1", 1L, "mv-long", SERVICE_DATE,
                at(18, 0), at(20, 30), at(20, 45), "85.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate shortA = candidate(1L, "aud-1", 2L, "mv-short-a", SERVICE_DATE,
                at(18, 0), at(19, 0), at(19, 15), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate shortB = candidate(1L, "aud-1", 3L, "mv-short-b", SERVICE_DATE,
                at(19, 15), at(20, 15), at(20, 30), "60.000", PreviewItemValidationStatus.VALID);
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(shortB, shortA, longCandidate));

        resolver.resolveDefaultSelection(candidates);

        assertEquals(List.of(longCandidate, shortA, shortB), candidates);
        assertEquals(List.of(1, 2, 3), candidates.stream().map(ShowtimeCandidate::getRankingPosition).toList());
        assertFalse(longCandidate.isSelected());
        assertTrue(shortA.isSelected());
        assertTrue(shortB.isSelected());
        assertEquals(new BigDecimal("120.000"), selectedScore(candidates));
    }

    @Test
    void equalTotalUsesCanonicalMembershipTieBreak() {
        ShowtimeCandidate laterKey = candidate(1L, "aud-1", 2L, "mv-b", SERVICE_DATE,
                at(10, 0), at(11, 0), at(11, 15), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate earlierKey = candidate(1L, "aud-1", 1L, "mv-a", SERVICE_DATE,
                at(10, 0), at(11, 0), at(11, 15), "60.0", PreviewItemValidationStatus.VALID);

        resolver.resolveDefaultSelection(new ArrayList<>(List.of(laterKey, earlierKey)));

        assertTrue(earlierKey.isSelected());
        assertFalse(laterKey.isSelected());
    }

    @Test
    void rejectedHighScoreNeverContributes() {
        ShowtimeCandidate valid = candidate(1L, "aud-1", 1L, "mv-valid", SERVICE_DATE,
                at(10, 0), at(11, 0), at(11, 15), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate rejected = candidate(1L, "aud-1", 2L, "mv-rejected", SERVICE_DATE,
                at(10, 0), at(12, 0), at(12, 15), "9999999.999", PreviewItemValidationStatus.REJECTED);
        rejected.setRejectionCode("REJECTED_FOR_TEST");

        resolver.resolveDefaultSelection(new ArrayList<>(List.of(rejected, valid)));

        assertTrue(valid.isSelected());
        assertFalse(rejected.isSelected());
        assertEquals(PreviewItemValidationStatus.VALID,
                List.of(valid, rejected).stream().filter(ShowtimeCandidate::isSelected).findFirst().orElseThrow()
                        .getValidationStatus());
    }

    @Test
    void positiveOverlapConflictsButExactOccupancyAdjacencyIsAllowed() {
        ShowtimeCandidate first = candidate(1L, "aud-1", 1L, "mv-a", SERVICE_DATE,
                at(9, 0), at(10, 0), at(10, 15), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate adjacent = candidate(1L, "aud-1", 2L, "mv-b", SERVICE_DATE,
                at(10, 15), at(11, 15), at(11, 30), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate overlapping = candidate(1L, "aud-1", 3L, "mv-c", SERVICE_DATE,
                at(10, 14), at(11, 14), at(11, 29), "1.000", PreviewItemValidationStatus.VALID);

        resolver.resolveDefaultSelection(new ArrayList<>(List.of(overlapping, adjacent, first)));

        assertTrue(first.isSelected());
        assertTrue(adjacent.isSelected());
        assertFalse(overlapping.isSelected());
    }

    @Test
    void differentAuditoriumsAreIndependent() {
        ShowtimeCandidate auditoriumA = candidate(1L, "aud-a", 1L, "mv-a", SERVICE_DATE,
                at(10, 0), at(11, 0), at(11, 15), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate auditoriumB = candidate(2L, "aud-b", 1L, "mv-a", SERVICE_DATE,
                at(10, 0), at(11, 0), at(11, 15), "60.000", PreviewItemValidationStatus.VALID);

        resolver.resolveDefaultSelection(new ArrayList<>(List.of(auditoriumB, auditoriumA)));

        assertTrue(auditoriumA.isSelected());
        assertTrue(auditoriumB.isSelected());
    }

    @Test
    void crossMidnightOwnershipCoalescesOverlappingServiceDates() {
        LocalDate firstServiceDate = LocalDate.of(2026, 7, 24);
        LocalDate secondServiceDate = firstServiceDate.plusDays(1);
        ShowtimeCandidate overnight = candidate(1L, "aud-1", 1L, "mv-overnight", firstServiceDate,
                Instant.parse("2026-07-25T00:30:00Z"), Instant.parse("2026-07-25T01:30:00Z"),
                Instant.parse("2026-07-25T01:45:00Z"), "80.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate nextServiceDate = candidate(1L, "aud-1", 2L, "mv-next", secondServiceDate,
                Instant.parse("2026-07-25T01:00:00Z"), Instant.parse("2026-07-25T02:00:00Z"),
                Instant.parse("2026-07-25T02:15:00Z"), "90.000", PreviewItemValidationStatus.VALID);

        resolver.resolveDefaultSelection(new ArrayList<>(List.of(overnight, nextServiceDate)));

        assertEquals(firstServiceDate, overnight.getOperatingWindow().getServiceDate());
        assertFalse(overnight.isSelected());
        assertTrue(nextServiceDate.isSelected());
    }

    @Test
    void inputOrderDoesNotChangeSelectedBusinessKeys() {
        List<ShowtimeCandidate> source = List.of(
                candidate(1L, "aud-1", 1L, "mv-a", SERVICE_DATE,
                        at(8, 0), at(9, 0), at(9, 15), "60.000", PreviewItemValidationStatus.VALID),
                candidate(1L, "aud-1", 2L, "mv-b", SERVICE_DATE,
                        at(8, 30), at(9, 30), at(9, 45), "75.000", PreviewItemValidationStatus.VALID),
                candidate(1L, "aud-1", 3L, "mv-c", SERVICE_DATE,
                        at(9, 15), at(10, 15), at(10, 30), "60.000", PreviewItemValidationStatus.VALID),
                candidate(1L, "aud-1", 4L, "mv-d", SERVICE_DATE,
                        at(10, 30), at(11, 30), at(11, 45), "60.000", PreviewItemValidationStatus.VALID));
        Set<String> expected = null;

        for (int seed = 0; seed < 100; seed++) {
            List<ShowtimeCandidate> shuffled = new ArrayList<>(source);
            Collections.shuffle(shuffled, new Random(seed));
            resolver.resolveDefaultSelection(shuffled);
            Set<String> actual = selectedKeys(shuffled);
            if (expected == null) {
                expected = actual;
            } else {
                assertEquals(expected, actual, "selection changed for shuffle seed " + seed);
            }
        }
    }

    @Test
    void randomizedSmallPartitionsMatchBruteForceOracle() {
        for (int seed = 0; seed < 250; seed++) {
            Random random = new Random(0x53_0000L + seed);
            int size = 1 + random.nextInt(12);
            List<ShowtimeCandidate> candidates = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int startSlot = random.nextInt(16);
                int occupancySlots = 1 + random.nextInt(5);
                Instant start = BASE.plus(startSlot * 15L, ChronoUnit.MINUTES);
                Instant occupancyEnd = start.plus(occupancySlots * 15L, ChronoUnit.MINUTES);
                Instant end = occupancyEnd.minus(5, ChronoUnit.MINUTES);
                String score = BigDecimal.valueOf(random.nextInt(8) * 10L)
                        .setScale(3, RoundingMode.UNNECESSARY).toPlainString();
                candidates.add(candidate(1L, "aud-1", (long) i + 1, "mv-" + String.format("%02d", i),
                        SERVICE_DATE, start, end, occupancyEnd, score, PreviewItemValidationStatus.VALID));
            }
            Set<String> expected = bruteForceOptimalKeys(candidates);
            Collections.shuffle(candidates, random);

            resolver.resolveDefaultSelection(candidates);

            assertEquals(expected, selectedKeys(candidates), "oracle mismatch for seed " + seed);
        }
    }

    @Test
    void emptySingleZeroAndNegativeInputsAreHandled() {
        List<ShowtimeCandidate> empty = new ArrayList<>();
        resolver.resolveDefaultSelection(empty);
        assertTrue(empty.isEmpty());

        ShowtimeCandidate positive = candidate(1L, "aud-1", 1L, "mv-positive", SERVICE_DATE,
                at(8, 0), at(9, 0), at(9, 15), "1.000", PreviewItemValidationStatus.VALID);
        resolver.resolveDefaultSelection(new ArrayList<>(List.of(positive)));
        assertTrue(positive.isSelected());

        ShowtimeCandidate zero = candidate(1L, "aud-1", 2L, "mv-zero", SERVICE_DATE,
                at(9, 15), at(10, 15), at(10, 30), "0.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate negative = candidate(1L, "aud-1", 3L, "mv-negative", SERVICE_DATE,
                at(10, 30), at(11, 30), at(11, 45), "-1.000", PreviewItemValidationStatus.VALID);
        resolver.resolveDefaultSelection(new ArrayList<>(List.of(zero, negative)));
        assertFalse(zero.isSelected());
        assertFalse(negative.isSelected());
    }

    @Test
    void malformedScoreOrServiceDateFailsFast() {
        ShowtimeCandidate missingScore = candidate(1L, "aud-1", 1L, "mv-a", SERVICE_DATE,
                at(8, 0), at(9, 0), at(9, 15), "60.000", PreviewItemValidationStatus.VALID);
        missingScore.setScore(null);
        assertThrows(IllegalStateException.class,
                () -> resolver.resolveDefaultSelection(new ArrayList<>(List.of(missingScore))));

        ShowtimeCandidate missingServiceDate = candidate(1L, "aud-1", 2L, "mv-b", SERVICE_DATE,
                at(9, 15), at(10, 15), at(10, 30), "60.000", PreviewItemValidationStatus.VALID);
        missingServiceDate.setOperatingWindow(new OperatingWindow(null, BASE, BASE.plus(1, ChronoUnit.DAYS)));
        assertThrows(IllegalStateException.class,
                () -> resolver.resolveDefaultSelection(new ArrayList<>(List.of(missingServiceDate))));
    }

    @Test
    void defensiveInvariantRejectsOverlappingSelectedIntervals() {
        ShowtimeCandidate first = candidate(1L, "aud-1", 1L, "mv-a", SERVICE_DATE,
                at(8, 0), at(9, 0), at(9, 15), "60.000", PreviewItemValidationStatus.VALID);
        ShowtimeCandidate second = candidate(1L, "aud-1", 2L, "mv-b", SERVICE_DATE,
                at(9, 0), at(10, 0), at(10, 15), "60.000", PreviewItemValidationStatus.VALID);
        first.setSelected(true);
        second.setSelected(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> resolver.validateGlobalSelectionInvariant(List.of(first, second)));

        assertEquals(ErrorCode.AUTO_SCHEDULE_SELECTION_INVARIANT_VIOLATION, error.getErrorCode());
    }

    @Test
    void nearLimitDiagnosticsRemainBoundedAndReportSelectionPhases() {
        List<ShowtimeCandidate> candidates = new ArrayList<>(10_000);
        for (int auditorium = 0; auditorium < 10; auditorium++) {
            for (int day = 0; day < 5; day++) {
                LocalDate serviceDate = SERVICE_DATE.plusDays(day);
                Instant dayStart = BASE.plus(day, ChronoUnit.DAYS);
                for (int slot = 0; slot < 200; slot++) {
                    Instant start = dayStart.plus(slot * 5L, ChronoUnit.MINUTES);
                    candidates.add(candidate((long) auditorium + 1, "aud-" + auditorium,
                            1L, "mv-benchmark", serviceDate, start,
                            start.plus(45, ChronoUnit.MINUTES), start.plus(60, ChronoUnit.MINUTES),
                            BigDecimal.valueOf(60L + slot % 10).setScale(3).toPlainString(),
                            PreviewItemValidationStatus.VALID));
                }
            }
        }
        Runtime runtime = Runtime.getRuntime();
        long heapBefore = runtime.totalMemory() - runtime.freeMemory();

        CandidateSelectionResolverImpl.SelectionDiagnostics diagnostics =
                resolver.resolveDefaultSelectionWithDiagnostics(candidates);

        long heapAfter = runtime.totalMemory() - runtime.freeMemory();
        assertEquals(10_000, diagnostics.candidateCount());
        assertEquals(50, diagnostics.logicalPartitionCount());
        assertEquals(50, diagnostics.optimizationComponentCount());
        assertEquals(200, diagnostics.largestComponentSize());
        assertTrue(diagnostics.selectedCount() > 0);
        assertTrue(diagnostics.totalNanos() > 0);
        assertNotNull(selectedKeys(candidates));
        System.out.printf(Locale.ROOT,
                "S3_SELECTION_BENCHMARK candidates=%d logicalPartitions=%d components=%d largest=%d " +
                        "rankingMs=%.3f partitionMs=%.3f wisSortMs=%.3f predecessorMs=%.3f " +
                        "dpMs=%.3f reconstructionMs=%.3f invariantMs=%.3f totalMs=%.3f " +
                        "selected=%d observedHeapDeltaBytes=%d jdk=%s os=%s%n",
                diagnostics.candidateCount(), diagnostics.logicalPartitionCount(),
                diagnostics.optimizationComponentCount(), diagnostics.largestComponentSize(),
                millis(diagnostics.rankingNanos()), millis(diagnostics.partitioningNanos()),
                millis(diagnostics.wisSortNanos()), millis(diagnostics.predecessorNanos()),
                millis(diagnostics.dpNanos()), millis(diagnostics.reconstructionNanos()),
                millis(diagnostics.invariantNanos()), millis(diagnostics.totalNanos()),
                diagnostics.selectedCount(), heapAfter - heapBefore,
                System.getProperty("java.version"), System.getProperty("os.name"));
    }

    private Set<String> bruteForceOptimalKeys(List<ShowtimeCandidate> source) {
        List<ShowtimeCandidate> canonical = new ArrayList<>(source);
        canonical.sort(CANONICAL_ORDER);
        BigDecimal bestScore = null;
        long bestMask = Long.MAX_VALUE;
        long subsetCount = 1L << canonical.size();
        for (long mask = 0; mask < subsetCount; mask++) {
            if (!isFeasible(canonical, mask)) {
                continue;
            }
            BigDecimal score = BigDecimal.ZERO.setScale(3);
            for (int i = 0; i < canonical.size(); i++) {
                if ((mask & (1L << i)) != 0) {
                    score = score.add(canonical.get(i).getScore());
                }
            }
            if (bestScore == null || score.compareTo(bestScore) > 0
                    || (score.compareTo(bestScore) == 0 && mask < bestMask)) {
                bestScore = score;
                bestMask = mask;
            }
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < canonical.size(); i++) {
            if ((bestMask & (1L << i)) != 0) {
                keys.add(canonical.get(i).getMovieVersionPublicId());
            }
        }
        return keys;
    }

    private boolean isFeasible(List<ShowtimeCandidate> candidates, long mask) {
        for (int i = 0; i < candidates.size(); i++) {
            if ((mask & (1L << i)) == 0) {
                continue;
            }
            for (int j = i + 1; j < candidates.size(); j++) {
                if ((mask & (1L << j)) == 0) {
                    continue;
                }
                ShowtimeCandidate first = candidates.get(i);
                ShowtimeCandidate second = candidates.get(j);
                if (first.getStartTime().isBefore(second.getOccupancyEndTime())
                        && first.getOccupancyEndTime().isAfter(second.getStartTime())) {
                    return false;
                }
            }
        }
        return true;
    }

    private ShowtimeCandidate candidate(Long auditoriumId,
                                        String auditoriumPublicId,
                                        Long movieVersionId,
                                        String movieVersionPublicId,
                                        LocalDate serviceDate,
                                        Instant start,
                                        Instant end,
                                        Instant occupancyEnd,
                                        String score,
                                        PreviewItemValidationStatus status) {
        AutoScheduleGenerationContext.CinemaSnapshot cinema = new AutoScheduleGenerationContext.CinemaSnapshot(
                1L, "cinema", "Cinema", ZoneId.of("UTC"), CinemaStatus.ACTIVE, false);
        AutoScheduleGenerationContext.AuditoriumSnapshot auditorium =
                new AutoScheduleGenerationContext.AuditoriumSnapshot(
                        auditoriumId, auditoriumPublicId, 1L, auditoriumPublicId,
                        100, 15, AuditoriumStatus.ACTIVE, false);
        AutoScheduleGenerationContext.MovieSnapshot movie = new AutoScheduleGenerationContext.MovieSnapshot(
                movieVersionId, "movie-" + movieVersionPublicId, movieVersionPublicId,
                Math.toIntExact(ChronoUnit.MINUTES.between(start, end)), serviceDate.minusDays(1), null,
                MovieStatus.NOW_SHOWING, false);
        AutoScheduleGenerationContext.MovieVersionSnapshot version =
                new AutoScheduleGenerationContext.MovieVersionSnapshot(
                        movieVersionId, movieVersionPublicId, movie.id(), ActiveStatus.ACTIVE, false, movie);
        Instant windowOpen = serviceDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant windowClose = windowOpen.plus(2, ChronoUnit.DAYS);

        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setCinemaSnapshot(cinema);
        candidate.setAuditoriumSnapshot(auditorium);
        candidate.setMovieVersionSnapshot(version);
        candidate.setOperatingWindow(new OperatingWindow(serviceDate, windowOpen, windowClose));
        candidate.setStartTime(start);
        candidate.setEndTime(end);
        candidate.setOccupancyEndTime(occupancyEnd);
        candidate.setScore(new BigDecimal(score));
        candidate.setValidationStatus(status);
        return candidate;
    }

    private Instant at(int hour, int minute) {
        return BASE.plus(hour, ChronoUnit.HOURS).plus(minute, ChronoUnit.MINUTES);
    }

    private BigDecimal selectedScore(List<ShowtimeCandidate> candidates) {
        return candidates.stream().filter(ShowtimeCandidate::isSelected)
                .map(ShowtimeCandidate::getScore)
                .reduce(BigDecimal.ZERO.setScale(3), BigDecimal::add);
    }

    private Set<String> selectedKeys(List<ShowtimeCandidate> candidates) {
        Set<String> keys = new HashSet<>();
        candidates.stream().filter(ShowtimeCandidate::isSelected)
                .map(ShowtimeCandidate::getMovieVersionPublicId).forEach(keys::add);
        return keys;
    }

    private double millis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
