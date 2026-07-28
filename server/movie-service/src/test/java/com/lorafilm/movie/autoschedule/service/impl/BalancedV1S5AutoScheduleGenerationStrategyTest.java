package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class BalancedV1S5AutoScheduleGenerationStrategyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 28);
    private static final Instant DAY_START = Instant.parse("2026-07-28T02:00:00Z");

    @Test
    void currentVersionIsS5() {
        assertEquals(AutoScheduleStrategyVersions.BALANCED_V1_S5,
                strategy(List.of(), Map.of()).getStrategyVersion());
    }

    @Test
    void sixEqualSlotsAreDistributedEvenlyAcrossThreeEligibleMovies() {
        List<ShowtimeCandidate> candidates = slotAlternatives(6, Map.of(
                "movie-a", "80.000",
                "movie-b", "80.000",
                "movie-c", "80.000"));

        BalancedV1S5AutoScheduleGenerationStrategy.S5Diagnostics diagnostics =
                strategy(candidates, Map.of()).scoreAndResolveWithDiagnostics(
                        candidates, mock(CandidateScoringContext.class));

        assertEquals(Map.of("movie-a", 2L, "movie-b", 2L, "movie-c", 2L),
                selectedCounts(candidates));
        assertEquals(4, diagnostics.swapCount());
        assertEquals(12, diagnostics.imbalanceBefore().get(DATE));
        assertEquals(0, diagnostics.imbalanceAfter().get(DATE));
        assertEquals(6, candidates.stream().filter(ShowtimeCandidate::isSelected).count());
    }

    @Test
    void realS4CoverageResultIsRebalancedInsteadOfOnlyReceivingMinimumCoverage() {
        List<ShowtimeCandidate> candidates = slotAlternatives(6, Map.of(
                "movie-a", "80.000",
                "movie-b", "80.000",
                "movie-c", "80.000"));
        BalancedV1S5AutoScheduleGenerationStrategy strategy = realStrategy(candidates);

        BalancedV1S5AutoScheduleGenerationStrategy.S5Diagnostics diagnostics =
                strategy.scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates));

        assertEquals(Map.of("movie-a", 2L, "movie-b", 2L, "movie-c", 2L),
                selectedCounts(candidates));
        assertTrue(diagnostics.swapCount() > 0);
        assertEquals(0, diagnostics.imbalanceAfter().get(DATE));
        new CandidateSelectionResolverImpl().validateGlobalSelectionInvariant(candidates);
    }

    @Test
    void shorterMovieCannotRetainNinetyPercentOfARealisticThreeRoomSchedule() {
        List<ShowtimeCandidate> candidates = realisticThreeRoomUniverse();
        BalancedV1S5AutoScheduleGenerationStrategy strategy = realStrategy(candidates);

        BalancedV1S5AutoScheduleGenerationStrategy.S5Diagnostics diagnostics =
                strategy.scoreAndResolveWithDiagnostics(candidates, scoringContext(candidates));

        Map<String, Long> counts = selectedCounts(candidates);
        long selectedTotal = counts.values().stream().mapToLong(Long::longValue).sum();
        long largestMovieCount = counts.values().stream().mapToLong(Long::longValue)
                .max().orElseThrow();
        assertEquals(3, counts.size());
        assertTrue(largestMovieCount * 100L <= selectedTotal * 60L,
                () -> "largest movie share remained above 60%: " + counts);
        assertTrue(diagnostics.swapCount() > 0);
        diagnostics.imbalanceAfter().forEach((date, imbalance) ->
                assertTrue(imbalance < diagnostics.imbalanceBefore().get(date)));
        new CandidateSelectionResolverImpl().validateGlobalSelectionInvariant(candidates);
    }

    @Test
    void qualityFloorStopsDistributionBeforeDailyScoreFallsBelowNinetyPercent() {
        List<ShowtimeCandidate> candidates = slotAlternatives(10, Map.of(
                "movie-a", "100.000",
                "movie-b", "50.000",
                "movie-c", "50.000"));

        BalancedV1S5AutoScheduleGenerationStrategy.S5Diagnostics diagnostics =
                strategy(candidates, Map.of()).scoreAndResolveWithDiagnostics(
                        candidates, mock(CandidateScoringContext.class));

        BigDecimal selectedScore = candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .map(ShowtimeCandidate::getScore)
                .reduce(new BigDecimal("0.000"), BigDecimal::add);
        assertEquals(new BigDecimal("900.000"), selectedScore);
        assertEquals(Map.of("movie-a", 8L, "movie-b", 1L, "movie-c", 1L),
                selectedCounts(candidates));
        assertEquals(2, diagnostics.swapCount());
        assertTrue(diagnostics.imbalanceAfter().get(DATE)
                < diagnostics.imbalanceBefore().get(DATE));
    }

    @Test
    void longerReplacementMayRemoveTwoDominantSlotsOnlyWhenQualityFloorAllowsIt() {
        List<ShowtimeCandidate> candidates = slotAlternatives(10, Map.of(
                "movie-a", "80.000"));
        ShowtimeCandidate longMovie = candidate(
                20L, "movie-b", 201L, "movie-b-long",
                DAY_START, DAY_START.plus(120, ChronoUnit.MINUTES), "150.000", 1000);
        candidates.add(longMovie);

        BalancedV1S5AutoScheduleGenerationStrategy.S5Diagnostics diagnostics =
                strategy(candidates, Map.of()).scoreAndResolveWithDiagnostics(
                        candidates, mock(CandidateScoringContext.class));

        assertTrue(longMovie.isSelected());
        assertEquals(9, candidates.stream().filter(ShowtimeCandidate::isSelected).count());
        assertEquals(1, diagnostics.swapCount());
        new CandidateSelectionResolverImpl().validateGlobalSelectionInvariant(candidates);
    }

    private BalancedV1S5AutoScheduleGenerationStrategy strategy(
            List<ShowtimeCandidate> candidates,
            Map<String, String> selectedScoreOverrides) {
        BalancedV1S4AutoScheduleGenerationStrategy coverage =
                mock(BalancedV1S4AutoScheduleGenerationStrategy.class);
        doAnswer(invocation -> {
            int ranking = 1;
            for (ShowtimeCandidate candidate : candidates) {
                String override = selectedScoreOverrides.get(
                        candidate.getMovieVersionSnapshot().movie().publicId());
                if (override != null) {
                    candidate.setScore(new BigDecimal(override));
                }
                candidate.setRankingPosition(ranking++);
                candidate.setSelected(candidate.getMovieVersionSnapshot().movie().publicId()
                        .equals("movie-a"));
            }
            return null;
        }).when(coverage).scoreAndResolveDefaultSelection(any(), any());
        return new BalancedV1S5AutoScheduleGenerationStrategy(
                coverage, new CandidateSelectionResolverImpl());
    }

    private BalancedV1S5AutoScheduleGenerationStrategy realStrategy(
            List<ShowtimeCandidate> candidates) {
        Map<ShowtimeCandidate, BigDecimal> baseScores = new IdentityHashMap<>();
        candidates.forEach(candidate -> baseScores.put(candidate, candidate.getScore()));
        BalancedCandidateScoringService scorer = (candidate, ignored) -> {
            LinkedHashMap<String, BigDecimal> breakdown = new LinkedHashMap<>();
            breakdown.put("base", baseScores.get(candidate));
            breakdown.put("primeTime", new BigDecimal("0.000"));
            breakdown.put("offPeak", new BigDecimal("0.000"));
            breakdown.put("earlySlot", new BigDecimal("0.000"));
            breakdown.put("auditoriumFit", new BigDecimal("0.000"));
            breakdown.put("scheduleContinuity", new BigDecimal("0.000"));
            return new CandidateScoreResult(baseScores.get(candidate), breakdown);
        };
        CandidateSelectionResolverImpl resolver = new CandidateSelectionResolverImpl();
        BalancedV1S4AutoScheduleGenerationStrategy coverage =
                new BalancedV1S4AutoScheduleGenerationStrategy(scorer, resolver);
        return new BalancedV1S5AutoScheduleGenerationStrategy(coverage, resolver);
    }

    private CandidateScoringContext scoringContext(List<ShowtimeCandidate> candidates) {
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
                .toList();
        AutoScheduleGenerationContext context = new AutoScheduleGenerationContext(
                candidates.getFirst().getCinemaSnapshot(),
                DATE,
                DATE,
                15,
                10_000,
                AutoScheduleStrategy.BALANCED,
                AutoScheduleStrategyVersions.BALANCED_V1_S5,
                auditoriums,
                versions,
                windows,
                Set.of(),
                ImmutableIntervalIndex.empty(),
                Map.of(),
                Map.of(),
                Map.<Long, ContinuityIndex>of(),
                Map.of(),
                DAY_START,
                DAY_START.plus(24, ChronoUnit.HOURS));
        return new CandidateScoringContext(context);
    }

    private List<ShowtimeCandidate> slotAlternatives(
            int slotCount,
            Map<String, String> movieScores) {
        List<ShowtimeCandidate> candidates = new ArrayList<>();
        int version = 1;
        for (int slot = 0; slot < slotCount; slot++) {
            Instant start = DAY_START.plus(slot * 60L, ChronoUnit.MINUTES);
            Instant end = start.plus(60, ChronoUnit.MINUTES);
            int movieIndex = 0;
            for (Map.Entry<String, String> movie : movieScores.entrySet()) {
                candidates.add(candidate(
                        10L + movieIndex,
                        movie.getKey(),
                        (long) version,
                        movie.getKey() + "-" + slot,
                        start,
                        end,
                        movie.getValue(),
                        version));
                version++;
                movieIndex++;
            }
        }
        return candidates;
    }

    private List<ShowtimeCandidate> realisticThreeRoomUniverse() {
        List<ShowtimeCandidate> candidates = new ArrayList<>();
        int[] occupancyMinutes = {105, 120, 135};
        long versionId = 1L;
        for (int room = 0; room < 3; room++) {
            for (int offset = 0; offset <= 15 * 60; offset += 15) {
                Instant start = DAY_START.plus(offset, ChronoUnit.MINUTES);
                for (int movie = 0; movie < occupancyMinutes.length; movie++) {
                    Instant occupancyEnd = start.plus(
                            occupancyMinutes[movie], ChronoUnit.MINUTES);
                    if (occupancyEnd.isAfter(DAY_START.plus(15, ChronoUnit.HOURS))) {
                        continue;
                    }
                    String moviePublicId = "movie-" + (char) ('a' + movie);
                    ShowtimeCandidate candidate = candidate(
                            10L + movie,
                            moviePublicId,
                            versionId,
                            moviePublicId + "-" + room + "-" + offset,
                            start,
                            occupancyEnd,
                            "80.000",
                            Math.toIntExact(versionId));
                    candidate.setAuditoriumSnapshot(
                            new AutoScheduleGenerationContext.AuditoriumSnapshot(
                                    1L + room, "aud-" + room, 1L, "Room " + room,
                                    100, 15, AuditoriumStatus.ACTIVE, false));
                    candidates.add(candidate);
                    versionId++;
                }
            }
        }
        return candidates;
    }

    private ShowtimeCandidate candidate(Long movieId,
                                        String moviePublicId,
                                        Long versionId,
                                        String versionPublicId,
                                        Instant start,
                                        Instant occupancyEnd,
                                        String score,
                                        int ranking) {
        AutoScheduleGenerationContext.CinemaSnapshot cinema =
                new AutoScheduleGenerationContext.CinemaSnapshot(
                        1L, "cinema", "Cinema", ZoneId.of("UTC"),
                        CinemaStatus.ACTIVE, false);
        AutoScheduleGenerationContext.AuditoriumSnapshot auditorium =
                new AutoScheduleGenerationContext.AuditoriumSnapshot(
                        1L, "aud-1", 1L, "Room 1", 100, 0,
                        AuditoriumStatus.ACTIVE, false);
        AutoScheduleGenerationContext.MovieSnapshot movie =
                new AutoScheduleGenerationContext.MovieSnapshot(
                        movieId, moviePublicId, moviePublicId, 60,
                        DATE.minusDays(1), null, MovieStatus.NOW_SHOWING, false);
        AutoScheduleGenerationContext.MovieVersionSnapshot version =
                new AutoScheduleGenerationContext.MovieVersionSnapshot(
                        versionId, versionPublicId, movieId,
                        ActiveStatus.ACTIVE, false, movie);

        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setCinemaSnapshot(cinema);
        candidate.setAuditoriumSnapshot(auditorium);
        candidate.setMovieVersionSnapshot(version);
        candidate.setOperatingWindow(new OperatingWindow(
                DATE, DAY_START, DAY_START.plus(24, ChronoUnit.HOURS)));
        candidate.setStartTime(start);
        candidate.setEndTime(occupancyEnd);
        candidate.setOccupancyEndTime(occupancyEnd);
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setScore(new BigDecimal(score));
        candidate.setRankingPosition(ranking);
        return candidate;
    }

    private Map<String, Long> selectedCounts(List<ShowtimeCandidate> candidates) {
        Map<String, Long> result = new HashMap<>();
        candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .forEach(candidate -> result.merge(
                        candidate.getMovieVersionSnapshot().movie().publicId(), 1L, Long::sum));
        return result;
    }
}
