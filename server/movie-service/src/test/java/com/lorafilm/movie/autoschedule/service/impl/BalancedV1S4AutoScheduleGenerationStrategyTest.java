package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ContinuityIndex;
import com.lorafilm.movie.autoschedule.model.ImmutableIntervalIndex;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalancedV1S4AutoScheduleGenerationStrategyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 24);
    private static final Instant DAY_START = Instant.parse("2026-07-24T00:00:00Z");

    @Test
    void oneCoverageRoundReplacesOneLowLossRepeatAndPersistsHonestBreakdown() {
        List<ShowtimeCandidate> candidates = threeSlotAlternatives();
        BalancedV1S4AutoScheduleGenerationStrategy strategy = strategy(candidates);

        strategy.scoreAndResolveDefaultSelection(candidates, scoringContext(candidates, Map.of()));

        assertEquals(Set.of("a-2", "a-3", "b-1"), selectedVersionKeys(candidates));
        assertEquals(1, candidates.stream()
                .filter(candidate -> candidate.getScoreBreakdown().get(
                        "coverageSearchAdjustment").compareTo(new BigDecimal("20.000")) == 0)
                .count());
        for (ShowtimeCandidate candidate : candidates) {
            assertEquals(BalancedV1S4AutoScheduleGenerationStrategy.BREAKDOWN_KEYS,
                    new ArrayList<>(candidate.getScoreBreakdown().keySet()));
            BigDecimal sum = candidate.getScoreBreakdown().values().stream()
                    .reduce(new BigDecimal("0.000"), BigDecimal::add);
            assertEquals(candidate.getScore(), sum);
            assertEquals(3, candidate.getScore().scale());
        }
        assertExactPersistedWisOptimum(candidates);
    }

    @Test
    void existingCinemaWideCoveragePreventsAlreadyCoveredMovieFromReceivingAnchor() {
        ShowtimeCandidate existingMovie = candidate(
                1L, "aud-1", 10L, "movie-a", 101L, "a", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "80.000");
        ShowtimeCandidate uncoveredMovie = candidate(
                1L, "aud-1", 20L, "movie-b", 201L, "b", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "85.000");
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(existingMovie, uncoveredMovie));
        Map<AutoScheduleGenerationContext.MovieServiceDateKey, Integer> existing = Map.of(
                new AutoScheduleGenerationContext.MovieServiceDateKey(DATE, 10L), 5);

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, existing));

        assertEquals(1, diagnostics.existingCoveredGroupCount());
        assertEquals(0, diagnostics.anchorCount());
        assertTrue(uncoveredMovie.isSelected());
        assertEquals(new BigDecimal("0.000"),
                existingMovie.getScoreBreakdown().get("coverageSearchAdjustment"));
    }

    @Test
    void versionsOfTheSameUnderlyingMovieFormOneEligibleGroup() {
        ShowtimeCandidate firstVersion = candidate(
                1L, "aud-1", 10L, "movie-a", 101L, "a-2d", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "80.000");
        ShowtimeCandidate secondVersion = candidate(
                1L, "aud-1", 10L, "movie-a", 102L, "a-imax", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "75.000");
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(firstVersion, secondVersion));

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertEquals(1, diagnostics.eligibleGroupCount());
        assertEquals(0, diagnostics.anchorCount());
        assertTrue(firstVersion.isSelected());
    }

    @Test
    void localOpportunityLossAboveFifteenBlocksDestructiveCoverageAnchor() {
        ShowtimeCandidate first = candidate(1L, "aud-1", 10L, "movie-a", 101L, "a-1", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "60.000");
        ShowtimeCandidate second = candidate(1L, "aud-1", 10L, "movie-a", 102L, "a-2", DATE,
                DAY_START.plus(60, ChronoUnit.MINUTES),
                DAY_START.plus(120, ChronoUnit.MINUTES), "60.000");
        ShowtimeCandidate destructive = candidate(1L, "aud-1", 20L, "movie-b", 201L, "b", DATE,
                DAY_START, DAY_START.plus(120, ChronoUnit.MINUTES), "95.000");
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(destructive, second, first));

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertEquals(0, diagnostics.anchorCount());
        assertFalse(destructive.isSelected());
        assertEquals(Set.of("a-1", "a-2"), selectedVersionKeys(candidates));
        assertAllValidAdjustmentsZero(candidates);
    }

    @Test
    void aggregateQualityFloorRejectsCumulativeIndividuallyAllowedLosses() {
        List<ShowtimeCandidate> candidates = twoSlotQualityFixture("86.000", "86.000");

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertEquals(2, diagnostics.anchorCount());
        assertFalse(diagnostics.coverageRoundAdmissible());
        assertFalse(diagnostics.coverageRoundWon());
        assertEquals(Set.of("a-1", "a-2"), selectedVersionKeys(candidates));
        assertAllValidAdjustmentsZero(candidates);
    }

    @Test
    void exactNinetyPercentAndFifteenPointBoundariesAreAdmissible() {
        List<ShowtimeCandidate> candidates = twoSlotQualityFixture("85.000", "95.000");

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertTrue(diagnostics.coverageRoundAdmissible());
        assertTrue(diagnostics.coverageRoundWon());
        assertEquals(Set.of("b", "c"), selectedVersionKeys(candidates));
        assertExactPersistedWisOptimum(candidates);
    }

    @Test
    void qualityRetentionIsEnforcedPerAuthoritativeServiceDate() {
        LocalDate nextDate = DATE.plusDays(1);
        ShowtimeCandidate firstDate = candidate(
                1L, "aud-1", 10L, "movie-a", 101L, "a", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "100.000");
        ShowtimeCandidate secondDateBaseline = candidate(
                1L, "aud-1", 20L, "movie-b", 201L, "b", nextDate,
                DAY_START.plus(1, ChronoUnit.DAYS),
                DAY_START.plus(1, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES), "100.000");
        ShowtimeCandidate secondDateCoverage = candidate(
                1L, "aud-1", 30L, "movie-c", 301L, "c", nextDate,
                DAY_START.plus(1, ChronoUnit.DAYS),
                DAY_START.plus(1, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES), "86.000");
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(
                secondDateCoverage, firstDate, secondDateBaseline));

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertFalse(diagnostics.coverageRoundAdmissible(),
                "93% across the range must not hide 86% retention on the second service date");
        assertEquals(Set.of("a", "b"), selectedVersionKeys(candidates));
        assertAllValidAdjustmentsZero(candidates);
    }

    @Test
    void outerComparatorUsesCoverageThenBaseScoreNotEffectiveScoreSum() {
        ShowtimeCandidate a = candidate(1L, "aud-1", 10L, "movie-a", 101L, "a", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "100.000");
        ShowtimeCandidate b = candidate(1L, "aud-1", 20L, "movie-b", 201L, "b", DATE,
                DAY_START.plus(60, ChronoUnit.MINUTES),
                DAY_START.plus(120, ChronoUnit.MINUTES), "100.000");
        ShowtimeCandidate c = candidate(1L, "aud-1", 30L, "movie-c", 301L, "c", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "85.000");
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(c, b, a));

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertTrue(diagnostics.coverageRoundAdmissible());
        assertEquals(2, diagnostics.baselineCoveredGroupCount());
        assertEquals(2, diagnostics.coverageRoundCoveredGroupCount());
        assertFalse(diagnostics.coverageRoundWon(),
                "the 205 effective-score coverage state must lose to the 200 base-score baseline");
        assertEquals(Set.of("a", "b"), selectedVersionKeys(candidates));
        assertAllValidAdjustmentsZero(candidates);
        assertExactPersistedWisOptimum(candidates);
    }

    @Test
    void rejectedCandidateKeepsZeroScoreAndEmptyBreakdown() {
        ShowtimeCandidate valid = candidate(1L, "aud-1", 10L, "movie-a", 101L, "a", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "80.000");
        ShowtimeCandidate rejected = candidate(1L, "aud-1", 20L, "movie-b", 201L, "b", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "999.000");
        rejected.setValidationStatus(PreviewItemValidationStatus.REJECTED);
        List<ShowtimeCandidate> candidates = new ArrayList<>(List.of(rejected, valid));

        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = strategy(candidates)
                .scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates, Map.of()));

        assertEquals(1, diagnostics.eligibleGroupCount());
        assertEquals(new BigDecimal("0.000"), rejected.getScore());
        assertTrue(rejected.getScoreBreakdown().isEmpty());
        assertFalse(rejected.isSelected());
    }

    @Test
    void repeatedAndShuffledRunsAreIdentical() {
        List<String> expected = null;
        for (int seed = 0; seed < 50; seed++) {
            List<ShowtimeCandidate> candidates = threeSlotAlternatives();
            Collections.shuffle(candidates, new Random(seed));
            strategy(candidates).scoreAndResolveDefaultSelection(
                    candidates, scoringContext(candidates, Map.of()));
            List<String> actual = candidates.stream()
                    .map(candidate -> candidate.getMovieVersionPublicId()
                            + "|" + candidate.getStartTime()
                            + "|" + candidate.getScore()
                            + "|" + candidate.getScoreBreakdown().get("coverageSearchAdjustment")
                            + "|" + candidate.isSelected()
                            + "|" + candidate.getRankingPosition())
                    .toList();
            if (expected == null) {
                expected = actual;
            } else {
                assertEquals(expected, actual, "shuffle seed " + seed);
            }
        }
    }

    @Test
    void tenThousandCandidateDiagnosticIsStableAcrossRepeatedShuffledRuns() {
        Runtime runtime = Runtime.getRuntime();
        long heapBefore = runtime.totalMemory() - runtime.freeMemory();
        List<BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics> measured = new ArrayList<>();
        List<String> expectedResult = null;
        BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics diagnostics = null;
        for (int run = 0; run < 4; run++) {
            List<ShowtimeCandidate> candidates = tenThousandCandidates();
            Collections.shuffle(candidates, new Random(0x54_0000L + run));
            diagnostics = strategy(candidates).scoreAndResolveWithDiagnostics(
                    candidates, scoringContext(candidates, Map.of()));
            List<String> result = deterministicResult(candidates);
            if (expectedResult == null) {
                expectedResult = result;
            } else {
                assertEquals(expectedResult, result, "10,000-candidate run " + run);
                measured.add(diagnostics);
            }
        }
        long heapAfter = runtime.totalMemory() - runtime.freeMemory();
        assertEquals(10_000, diagnostics.candidateCount());
        assertEquals(10, diagnostics.eligibleGroupCount());
        assertTrue(diagnostics.baselineWisNanos() > 0);
        assertTrue(diagnostics.coverageWisNanos() > 0);
        assertTrue(diagnostics.totalNanos() > diagnostics.baselineWisNanos());
        System.out.printf(Locale.ROOT,
                "S4A_SELECTION_BENCHMARK candidates=%d eligibleGroups=%d existingCovered=%d "
                        + "baselineCovered=%d anchors=%d coverageCovered=%d admissible=%s won=%s "
                        + "measuredRuns=%d baselineWisMedianMs=%.3f anchorMedianMs=%.3f "
                        + "coverageWisMedianMs=%.3f comparatorMedianMs=%.3f totalMedianMs=%.3f "
                        + "totalUpperMs=%.3f observedHeapDeltaBytes=%d jdk=%s os=%s%n",
                diagnostics.candidateCount(), diagnostics.eligibleGroupCount(),
                diagnostics.existingCoveredGroupCount(), diagnostics.baselineCoveredGroupCount(),
                diagnostics.anchorCount(), diagnostics.coverageRoundCoveredGroupCount(),
                diagnostics.coverageRoundAdmissible(), diagnostics.coverageRoundWon(),
                measured.size(), medianMillis(measured.stream()
                        .map(BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics::baselineWisNanos).toList()),
                medianMillis(measured.stream()
                        .map(BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics::anchorNanos).toList()),
                medianMillis(measured.stream()
                        .map(BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics::coverageWisNanos).toList()),
                medianMillis(measured.stream()
                        .map(BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics::comparatorNanos).toList()),
                medianMillis(measured.stream()
                        .map(BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics::totalNanos).toList()),
                millis(measured.stream()
                        .mapToLong(BalancedV1S4AutoScheduleGenerationStrategy.S4Diagnostics::totalNanos)
                        .max().orElseThrow()),
                heapAfter - heapBefore,
                System.getProperty("java.version"), System.getProperty("os.name"));
    }

    private BalancedV1S4AutoScheduleGenerationStrategy strategy(List<ShowtimeCandidate> candidates) {
        Map<ShowtimeCandidate, BigDecimal> baseScores = new IdentityHashMap<>();
        candidates.forEach(candidate -> baseScores.put(candidate, candidate.getScore()));
        BalancedCandidateScoringService scorer = (candidate, ignored) -> {
            if (candidate.getValidationStatus() == PreviewItemValidationStatus.REJECTED) {
                return new CandidateScoreResult(new BigDecimal("0.000"), Map.of());
            }
            LinkedHashMap<String, BigDecimal> breakdown = new LinkedHashMap<>();
            breakdown.put("base", baseScores.get(candidate));
            breakdown.put("primeTime", new BigDecimal("0.000"));
            breakdown.put("offPeak", new BigDecimal("0.000"));
            breakdown.put("earlySlot", new BigDecimal("0.000"));
            breakdown.put("auditoriumFit", new BigDecimal("0.000"));
            breakdown.put("scheduleContinuity", new BigDecimal("0.000"));
            return new CandidateScoreResult(baseScores.get(candidate), breakdown);
        };
        return new BalancedV1S4AutoScheduleGenerationStrategy(
                scorer, new CandidateSelectionResolverImpl());
    }

    private CandidateScoringContext scoringContext(
            List<ShowtimeCandidate> candidates,
            Map<AutoScheduleGenerationContext.MovieServiceDateKey, Integer> existing) {
        List<AutoScheduleGenerationContext.AuditoriumSnapshot> auditoriums = candidates.stream()
                .map(ShowtimeCandidate::getAuditoriumSnapshot)
                .distinct()
                .toList();
        List<AutoScheduleGenerationContext.MovieVersionSnapshot> versions = candidates.stream()
                .map(ShowtimeCandidate::getMovieVersionSnapshot)
                .distinct()
                .toList();
        List<OperatingWindow> windows = candidates.stream()
                .map(ShowtimeCandidate::getOperatingWindow)
                .distinct()
                .toList();
        AutoScheduleGenerationContext.CinemaSnapshot cinema =
                candidates.getFirst().getCinemaSnapshot();
        AutoScheduleGenerationContext context = new AutoScheduleGenerationContext(
                cinema, DATE, DATE.plusDays(4), 5, 10_000,
                AutoScheduleStrategy.BALANCED, AutoScheduleStrategyVersions.BALANCED_V1_S4,
                auditoriums, versions, windows, Set.of(), ImmutableIntervalIndex.empty(),
                Map.of(), Map.of(), Map.<Long, ContinuityIndex>of(), existing,
                windows.stream().map(OperatingWindow::getOpenInstant).min(Instant::compareTo).orElseThrow(),
                windows.stream().map(OperatingWindow::getCloseInstant).max(Instant::compareTo).orElseThrow());
        return new CandidateScoringContext(context);
    }

    private List<ShowtimeCandidate> threeSlotAlternatives() {
        List<ShowtimeCandidate> candidates = new ArrayList<>();
        for (int slot = 0; slot < 3; slot++) {
            Instant start = DAY_START.plus(slot * 60L, ChronoUnit.MINUTES);
            Instant end = start.plus(60, ChronoUnit.MINUTES);
            candidates.add(candidate(1L, "aud-1", 10L, "movie-a", 100L + slot,
                    "a-" + (slot + 1), DATE, start, end, "80.000"));
            candidates.add(candidate(1L, "aud-1", 20L, "movie-b", 200L + slot,
                    "b-" + (slot + 1), DATE, start, end, "74.000"));
        }
        return candidates;
    }

    private List<ShowtimeCandidate> twoSlotQualityFixture(String firstAlternative,
                                                           String secondAlternative) {
        ShowtimeCandidate a1 = candidate(1L, "aud-1", 10L, "movie-a", 101L, "a-1", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), "100.000");
        ShowtimeCandidate a2 = candidate(1L, "aud-1", 10L, "movie-a", 102L, "a-2", DATE,
                DAY_START.plus(60, ChronoUnit.MINUTES),
                DAY_START.plus(120, ChronoUnit.MINUTES), "100.000");
        ShowtimeCandidate b = candidate(1L, "aud-1", 20L, "movie-b", 201L, "b", DATE,
                DAY_START, DAY_START.plus(60, ChronoUnit.MINUTES), firstAlternative);
        ShowtimeCandidate c = candidate(1L, "aud-1", 30L, "movie-c", 301L, "c", DATE,
                DAY_START.plus(60, ChronoUnit.MINUTES),
                DAY_START.plus(120, ChronoUnit.MINUTES), secondAlternative);
        return new ArrayList<>(List.of(c, a2, b, a1));
    }

    private List<ShowtimeCandidate> tenThousandCandidates() {
        List<ShowtimeCandidate> candidates = new ArrayList<>(10_000);
        long versionId = 1L;
        for (int auditorium = 0; auditorium < 10; auditorium++) {
            for (int day = 0; day < 5; day++) {
                LocalDate serviceDate = DATE.plusDays(day);
                Instant dayStart = DAY_START.plus(day, ChronoUnit.DAYS);
                for (int slot = 0; slot < 100; slot++) {
                    Instant start = dayStart.plus(slot * 10L, ChronoUnit.MINUTES);
                    Instant end = start.plus(20, ChronoUnit.MINUTES);
                    candidates.add(candidate((long) auditorium + 1, "aud-" + auditorium,
                            10L, "movie-a", versionId++, "a-" + versionId,
                            serviceDate, start, end, "80.000"));
                    candidates.add(candidate((long) auditorium + 1, "aud-" + auditorium,
                            20L, "movie-b", versionId++, "b-" + versionId,
                            serviceDate, start, end, "74.000"));
                }
            }
        }
        return candidates;
    }

    private ShowtimeCandidate candidate(Long auditoriumId,
                                        String auditoriumPublicId,
                                        Long movieId,
                                        String moviePublicId,
                                        Long versionId,
                                        String versionPublicId,
                                        LocalDate serviceDate,
                                        Instant start,
                                        Instant occupancyEnd,
                                        String baseScore) {
        AutoScheduleGenerationContext.CinemaSnapshot cinema =
                new AutoScheduleGenerationContext.CinemaSnapshot(
                        1L, "cinema", "Cinema", ZoneId.of("UTC"), CinemaStatus.ACTIVE, false);
        AutoScheduleGenerationContext.AuditoriumSnapshot auditorium =
                new AutoScheduleGenerationContext.AuditoriumSnapshot(
                        auditoriumId, auditoriumPublicId, 1L, auditoriumPublicId,
                        100, 0, AuditoriumStatus.ACTIVE, false);
        AutoScheduleGenerationContext.MovieSnapshot movie =
                new AutoScheduleGenerationContext.MovieSnapshot(
                        movieId, moviePublicId, moviePublicId, 60,
                        serviceDate.minusDays(1), null, MovieStatus.NOW_SHOWING, false);
        AutoScheduleGenerationContext.MovieVersionSnapshot version =
                new AutoScheduleGenerationContext.MovieVersionSnapshot(
                        versionId, versionPublicId, movieId, ActiveStatus.ACTIVE, false, movie);
        Instant open = serviceDate.atStartOfDay(ZoneId.of("UTC")).toInstant();

        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setCinemaSnapshot(cinema);
        candidate.setAuditoriumSnapshot(auditorium);
        candidate.setMovieVersionSnapshot(version);
        candidate.setOperatingWindow(new OperatingWindow(
                serviceDate, open, open.plus(24, ChronoUnit.HOURS)));
        candidate.setStartTime(start);
        candidate.setEndTime(occupancyEnd);
        candidate.setOccupancyEndTime(occupancyEnd);
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setScore(new BigDecimal(baseScore).setScale(3, RoundingMode.HALF_UP));
        return candidate;
    }

    private void assertExactPersistedWisOptimum(List<ShowtimeCandidate> candidates) {
        Set<String> before = selectedCandidateKeys(candidates);
        new CandidateSelectionResolverImpl().resolveDefaultSelection(candidates);
        assertEquals(before, selectedCandidateKeys(candidates));

        Map<String, List<ShowtimeCandidate>> partitions = new HashMap<>();
        candidates.stream()
                .filter(candidate -> candidate.getValidationStatus() == PreviewItemValidationStatus.VALID)
                .forEach(candidate -> partitions.computeIfAbsent(
                        candidate.getAuditoriumPublicId() + "|"
                                + candidate.getOperatingWindow().getServiceDate(),
                        ignored -> new ArrayList<>()).add(candidate));
        for (List<ShowtimeCandidate> partition : partitions.values()) {
            if (partition.size() <= 20) {
                BigDecimal selected = partition.stream().filter(ShowtimeCandidate::isSelected)
                        .map(ShowtimeCandidate::getScore)
                        .reduce(new BigDecimal("0.000"), BigDecimal::add);
                assertEquals(bruteForceBestScore(partition), selected);
            }
        }
    }

    private BigDecimal bruteForceBestScore(List<ShowtimeCandidate> candidates) {
        BigDecimal best = new BigDecimal("0.000");
        long subsets = 1L << candidates.size();
        for (long mask = 0; mask < subsets; mask++) {
            BigDecimal score = new BigDecimal("0.000");
            boolean feasible = true;
            for (int i = 0; i < candidates.size() && feasible; i++) {
                if ((mask & (1L << i)) == 0) {
                    continue;
                }
                score = score.add(candidates.get(i).getScore());
                for (int j = i + 1; j < candidates.size(); j++) {
                    if ((mask & (1L << j)) != 0
                            && overlaps(candidates.get(i), candidates.get(j))) {
                        feasible = false;
                        break;
                    }
                }
            }
            if (feasible && score.compareTo(best) > 0) {
                best = score;
            }
        }
        return best;
    }

    private boolean overlaps(ShowtimeCandidate first, ShowtimeCandidate second) {
        return first.getStartTime().isBefore(second.getOccupancyEndTime())
                && second.getStartTime().isBefore(first.getOccupancyEndTime());
    }

    private void assertAllValidAdjustmentsZero(List<ShowtimeCandidate> candidates) {
        candidates.stream()
                .filter(candidate -> candidate.getValidationStatus() == PreviewItemValidationStatus.VALID)
                .forEach(candidate -> assertEquals(new BigDecimal("0.000"),
                        candidate.getScoreBreakdown().get("coverageSearchAdjustment")));
    }

    private Set<String> selectedVersionKeys(List<ShowtimeCandidate> candidates) {
        Set<String> keys = new TreeSet<>();
        candidates.stream().filter(ShowtimeCandidate::isSelected)
                .map(ShowtimeCandidate::getMovieVersionPublicId).forEach(keys::add);
        return keys;
    }

    private Set<String> selectedCandidateKeys(List<ShowtimeCandidate> candidates) {
        Set<String> keys = new TreeSet<>();
        candidates.stream().filter(ShowtimeCandidate::isSelected)
                .map(candidate -> candidate.getAuditoriumPublicId() + "|"
                        + candidate.getStartTime() + "|" + candidate.getMovieVersionPublicId())
                .forEach(keys::add);
        return keys;
    }

    private List<String> deterministicResult(List<ShowtimeCandidate> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparing(ShowtimeCandidate::getAuditoriumPublicId)
                        .thenComparing(ShowtimeCandidate::getStartTime)
                        .thenComparing(ShowtimeCandidate::getMovieVersionPublicId))
                .map(candidate -> candidate.getAuditoriumPublicId() + "|"
                        + candidate.getStartTime() + "|"
                        + candidate.getMovieVersionPublicId() + "|"
                        + candidate.getScore() + "|"
                        + candidate.getScoreBreakdown() + "|"
                        + candidate.isSelected() + "|"
                        + candidate.getRankingPosition())
                .toList();
    }

    private double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private double medianMillis(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        return millis(sorted.get(sorted.size() / 2));
    }
}
