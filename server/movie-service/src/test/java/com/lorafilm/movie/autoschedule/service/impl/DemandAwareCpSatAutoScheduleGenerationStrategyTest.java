package com.lorafilm.movie.autoschedule.service.impl;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleOptimizationResult;
import com.lorafilm.movie.autoschedule.model.CandidatePricingSnapshot;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandAwareCpSatAutoScheduleGenerationStrategyTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 8);
    private static final Instant OPEN = Instant.parse("2026-08-08T02:00:00Z");

    @Test
    void nativeBindingSolvesMinimalModel() {
        Loader.loadNativeLibraries();
        CpModel model = new CpModel();
        var selected = model.newBoolVar("selected");
        model.addEquality(selected, 1);
        assertEquals(CpSolverStatus.OPTIMAL, new CpSolver().solve(model));
    }

    @Test
    void selectsHigherValueCandidateAndNeverOverlapsAnAuditorium() {
        List<ShowtimeCandidate> candidates = candidates();
        var strategy = new DemandAwareCpSatAutoScheduleGenerationStrategy(
                2.0, 20260806, new CandidateSelectionResolverImpl());
        CandidateScoringContext context = new CandidateScoringContext(null, List.of(), List.of());

        strategy.scoreAndResolveDefaultSelection(candidates, context);

        ShowtimeCandidate high = byVersion(candidates, "version-high");
        ShowtimeCandidate low = byVersion(candidates, "version-low");
        ShowtimeCandidate later = byVersion(candidates, "version-later");
        assertTrue(high.isSelected());
        assertFalse(low.isSelected());
        assertTrue(later.isSelected());
        assertFalse(high.getStartTime().isBefore(later.getOccupancyEndTime())
                && high.getOccupancyEndTime().isAfter(later.getStartTime()));
        assertNotNull(context.getOptimizationResult());
        assertEquals(2, context.getOptimizationResult().selectedCount());
    }

    @Test
    void fixedSeedAndSingleWorkerProduceStableSelection() {
        var strategy = new DemandAwareCpSatAutoScheduleGenerationStrategy(
                2.0, 20260806, new CandidateSelectionResolverImpl());
        List<ShowtimeCandidate> first = candidates();
        List<ShowtimeCandidate> second = candidates();

        strategy.scoreAndResolveDefaultSelection(first,
                new CandidateScoringContext(null, List.of(), List.of()));
        strategy.scoreAndResolveDefaultSelection(second,
                new CandidateScoringContext(null, List.of(), List.of()));

        assertEquals(selectedVersions(first), selectedVersions(second));
    }

    @Test
    void mapsUnknownAndInfeasibleStatusesToExplicitFailureSemantics() {
        var strategy = new DemandAwareCpSatAutoScheduleGenerationStrategy(
                2.0, 20260806, new CandidateSelectionResolverImpl());

        assertEquals(AutoScheduleOptimizationResult.SolverStatus.TIMEOUT,
                strategy.mapStatus(CpSolverStatus.UNKNOWN));
        assertEquals(AutoScheduleOptimizationResult.SolverStatus.INFEASIBLE,
                strategy.mapStatus(CpSolverStatus.INFEASIBLE));
        assertEquals(AutoScheduleOptimizationResult.SolverStatus.MODEL_INVALID,
                strategy.mapStatus(CpSolverStatus.MODEL_INVALID));
    }

    private List<ShowtimeCandidate> candidates() {
        List<ShowtimeCandidate> result = new ArrayList<>();
        result.add(candidate(1L, "version-high", OPEN, OPEN.plus(120, ChronoUnit.MINUTES), "9000000"));
        result.add(candidate(2L, "version-low", OPEN.plus(15, ChronoUnit.MINUTES),
                OPEN.plus(120, ChronoUnit.MINUTES), "3000000"));
        result.add(candidate(3L, "version-later", OPEN.plus(120, ChronoUnit.MINUTES),
                OPEN.plus(240, ChronoUnit.MINUTES), "6000000"));
        return result;
    }

    private ShowtimeCandidate candidate(long movieId, String versionPublicId,
                                         Instant start, Instant occupancyEnd,
                                         String contribution) {
        var cinema = new AutoScheduleGenerationContext.CinemaSnapshot(
                1L, "cinema-1", "Cinema", ZoneId.of("UTC"), CinemaStatus.ACTIVE, false);
        var auditorium = new AutoScheduleGenerationContext.AuditoriumSnapshot(
                10L, "aud-1", 1L, "Room 1", 100, 15, AuditoriumStatus.ACTIVE, false);
        var movie = new AutoScheduleGenerationContext.MovieSnapshot(
                movieId, "movie-" + movieId, "Movie " + movieId, 105,
                DATE.minusDays(7), null, MovieStatus.NOW_SHOWING, false);
        var version = new AutoScheduleGenerationContext.MovieVersionSnapshot(
                movieId, versionPublicId, movieId, ActiveStatus.ACTIVE, false, movie,
                MovieFormat.TWO_D);
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setCinemaSnapshot(cinema);
        candidate.setAuditoriumSnapshot(auditorium);
        candidate.setMovieVersionSnapshot(version);
        candidate.setOperatingWindow(new OperatingWindow(DATE, OPEN,
                OPEN.plus(12, ChronoUnit.HOURS)));
        candidate.setStartTime(start);
        candidate.setEndTime(occupancyEnd.minus(15, ChronoUnit.MINUTES));
        candidate.setOccupancyEndTime(occupancyEnd);
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setExpectedAttendance(new BigDecimal("60"));
        candidate.setExpectedOccupancy(new BigDecimal("0.60"));
        candidate.setExpectedRevenue(new BigDecimal(contribution).add(new BigDecimal("500000")));
        candidate.setExpectedContribution(new BigDecimal(contribution));
        candidate.setDemandConfidence(new BigDecimal("0.80"));
        candidate.setPricingSnapshot(new CandidatePricingSnapshot(
                "VND", "UTC", OPEN, new BigDecimal("100000"), List.of(), "price-fp"));
        return candidate;
    }

    private ShowtimeCandidate byVersion(List<ShowtimeCandidate> candidates, String publicId) {
        return candidates.stream().filter(item -> publicId.equals(item.getMovieVersionPublicId()))
                .findFirst().orElseThrow();
    }

    private List<String> selectedVersions(List<ShowtimeCandidate> candidates) {
        return candidates.stream().filter(ShowtimeCandidate::isSelected)
                .map(ShowtimeCandidate::getMovieVersionPublicId).sorted().toList();
    }
}
