package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.ImmutableIntervalIndex;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import com.lorafilm.movie.showtime.validation.ShowtimeSchedulingRules;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowtimeCandidateValidationServiceImplTest {

    @Test
    void validateMapsCanonicalClosureErrorToPreviewRejection() {
        Instant open = Instant.parse("2026-07-22T08:00:00Z");
        AutoScheduleGenerationContext base = ShowtimeCandidateGeneratorImplTest.context(
                15, 90, 15, List.of(new OperatingWindow(LocalDate.of(2026, 7, 22),
                        open, open.plus(3, ChronoUnit.HOURS))));
        AutoScheduleGenerationContext context = new AutoScheduleGenerationContext(
                base.getCinema(), base.getScheduleFrom(), base.getScheduleTo(),
                base.getSlotGranularityMinutes(), base.getCandidateLimit(), base.getStrategy(),
                base.getStrategyVersion(), base.getAuditoriums(), base.getMovieVersions(),
                base.getOperatingWindows(), base.getConfiguredOperatingDays(),
                ImmutableIntervalIndex.of(List.of(new ImmutableIntervalIndex.Interval(
                        open.plus(30, ChronoUnit.MINUTES), open.plus(2, ChronoUnit.HOURS)))),
                Map.of(), Map.of(), Map.of(), base.getPlanningStart(), base.getPlanningEnd());
        ShowtimeCandidate candidate = new ShowtimeCandidateGeneratorImpl(
                new UniqueCandidateSlotTraversalImpl()).generate(context).get(0);
        ShowtimeCandidateValidationServiceImpl service = new ShowtimeCandidateValidationServiceImpl(
                new MovieShowtimeEligibilityPolicy(), new ShowtimeSchedulingRules());

        CandidateValidationResult result = service.validate(candidate, context);

        assertFalse(result.isValid());
        assertEquals(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE.name(), result.getRejectionCode());
    }

    @Test
    void maintenanceAndExistingShowtimeConflictsUseCanonicalCodes() {
        AutoScheduleGenerationContext base = baseContext(15);
        ShowtimeCandidate candidate = candidate(base);
        ImmutableIntervalIndex conflict = ImmutableIntervalIndex.of(List.of(
                new ImmutableIntervalIndex.Interval(candidate.getStartTime().plusSeconds(1),
                        candidate.getOccupancyEndTime())));

        CandidateValidationResult maintenance = validator().validate(candidate,
                copy(base, ImmutableIntervalIndex.empty(), Map.of(2L, conflict), Map.of()));
        CandidateValidationResult showtime = validator().validate(candidate,
                copy(base, ImmutableIntervalIndex.empty(), Map.of(), Map.of(2L, conflict)));

        assertEquals(ErrorCode.SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE.name(),
                maintenance.getRejectionCode());
        assertEquals(ErrorCode.SHOWTIME_OVERLAP_CONFLICT.name(), showtime.getRejectionCode());
    }

    @Test
    void halfOpenAdjacencyAndZeroBufferRemainValid() {
        AutoScheduleGenerationContext base = baseContext(0);
        ShowtimeCandidate candidate = candidate(base);
        ImmutableIntervalIndex adjacent = ImmutableIntervalIndex.of(List.of(
                new ImmutableIntervalIndex.Interval(candidate.getStartTime().minusSeconds(60),
                        candidate.getStartTime()),
                new ImmutableIntervalIndex.Interval(candidate.getOccupancyEndTime(),
                        candidate.getOccupancyEndTime().plusSeconds(60))));
        AutoScheduleGenerationContext context = copy(
                base, adjacent, Map.of(2L, adjacent), Map.of(2L, adjacent));

        CandidateValidationResult result = validator().validate(candidate, context);

        assertTrue(result.isValid());
        assertEquals(candidate.getEndTime(), candidate.getOccupancyEndTime());
    }

    @Test
    void releaseViolationIsPersistableCandidateRejectionRatherThanUniversePruning() {
        AutoScheduleGenerationContext base = baseContext(15);
        AutoScheduleGenerationContext.MovieVersionSnapshot original = base.getMovieVersions().get(0);
        AutoScheduleGenerationContext.MovieSnapshot movie = original.movie();
        AutoScheduleGenerationContext.MovieSnapshot unreleased =
                new AutoScheduleGenerationContext.MovieSnapshot(
                        movie.id(), movie.publicId(), movie.title(), movie.durationMinutes(),
                        LocalDate.of(2026, 7, 23), movie.endDate(), movie.status(), movie.deleted());
        AutoScheduleGenerationContext.MovieVersionSnapshot version =
                new AutoScheduleGenerationContext.MovieVersionSnapshot(
                        original.id(), original.publicId(), original.movieId(), original.status(),
                        original.deleted(), unreleased);
        AutoScheduleGenerationContext context = new AutoScheduleGenerationContext(
                base.getCinema(), base.getScheduleFrom(), base.getScheduleTo(),
                base.getSlotGranularityMinutes(), base.getCandidateLimit(), base.getStrategy(),
                base.getStrategyVersion(), base.getAuditoriums(), List.of(version),
                base.getOperatingWindows(), base.getConfiguredOperatingDays(),
                ImmutableIntervalIndex.empty(), Map.of(), Map.of(), Map.of(),
                base.getPlanningStart(), base.getPlanningEnd());

        List<ShowtimeCandidate> generated = new ShowtimeCandidateGeneratorImpl(
                new UniqueCandidateSlotTraversalImpl()).generate(context);
        CandidateValidationResult result = validator().validate(generated.get(0), context);

        assertFalse(generated.isEmpty());
        assertFalse(result.isValid());
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW.name(), result.getRejectionCode());
    }

    private AutoScheduleGenerationContext baseContext(int buffer) {
        Instant open = Instant.parse("2026-07-22T08:00:00Z");
        return ShowtimeCandidateGeneratorImplTest.context(
                15, 90, buffer, List.of(new OperatingWindow(LocalDate.of(2026, 7, 22),
                        open, open.plus(3, ChronoUnit.HOURS))));
    }

    private ShowtimeCandidate candidate(AutoScheduleGenerationContext context) {
        return new ShowtimeCandidateGeneratorImpl(
                new UniqueCandidateSlotTraversalImpl()).generate(context).get(0);
    }

    private ShowtimeCandidateValidationServiceImpl validator() {
        return new ShowtimeCandidateValidationServiceImpl(
                new MovieShowtimeEligibilityPolicy(), new ShowtimeSchedulingRules());
    }

    private AutoScheduleGenerationContext copy(
            AutoScheduleGenerationContext base,
            ImmutableIntervalIndex closures,
            Map<Long, ImmutableIntervalIndex> maintenance,
            Map<Long, ImmutableIntervalIndex> showtimes) {
        return new AutoScheduleGenerationContext(
                base.getCinema(), base.getScheduleFrom(), base.getScheduleTo(),
                base.getSlotGranularityMinutes(), base.getCandidateLimit(), base.getStrategy(),
                base.getStrategyVersion(), base.getAuditoriums(), base.getMovieVersions(),
                base.getOperatingWindows(), base.getConfiguredOperatingDays(), closures,
                maintenance, showtimes, Map.of(), base.getPlanningStart(), base.getPlanningEnd());
    }
}
