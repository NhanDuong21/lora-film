package com.lorafilm.movie.autoschedule.service.impl;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleOptimizationResult;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Component
public class DemandAwareCpSatAutoScheduleGenerationStrategy implements AutoScheduleGenerationStrategy {

    public static final String SOLVER_VERSION = "OR_TOOLS_CP_SAT_9_12_4544_DEMAND_V1";
    private static final int MAX_MARGINAL_TIERS = 12;
    private static final long CLOSE_WINDOW_MINUTES = 120;

    static {
        Loader.loadNativeLibraries();
    }

    private final double timeoutSeconds;
    private final double relativeGapLimit;
    private final int randomSeed;
    private final CandidateSelectionResolverImpl selectionValidator;

    public DemandAwareCpSatAutoScheduleGenerationStrategy(
            @Value("${autoschedule.solver.timeout-seconds:0}") double timeoutSeconds,
            @Value("${autoschedule.solver.relative-gap-limit:0.02}") double relativeGapLimit,
            @Value("${autoschedule.solver.random-seed:20260806}") int randomSeed,
            CandidateSelectionResolverImpl selectionValidator) {
        this.timeoutSeconds = Math.max(0, timeoutSeconds);
        this.relativeGapLimit = Math.max(0, Math.min(1, relativeGapLimit));
        this.randomSeed = randomSeed;
        this.selectionValidator = selectionValidator;
    }

    @Override
    public String getStrategyVersion() {
        return AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1;
    }

    @Override
    public void scoreAndResolveDefaultSelection(List<ShowtimeCandidate> candidates,
                                                CandidateScoringContext scoringContext) {
        List<ShowtimeCandidate> valid = candidates.stream()
                .filter(candidate -> candidate.getValidationStatus() == PreviewItemValidationStatus.VALID)
                .sorted(candidateOrder())
                .toList();
        if (valid.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_NO_FEASIBLE_CANDIDATES);
        }

        Map<ShowtimeCandidate, Long> utilities = new IdentityHashMap<>();
        Map<LocalDate, List<ShowtimeCandidate>> candidatesByServiceDate = new LinkedHashMap<>();
        valid.forEach(candidate -> candidatesByServiceDate
                .computeIfAbsent(candidate.getServiceDate(), ignored -> new ArrayList<>())
                .add(candidate));

        AutoScheduleOptimizationResult.SolverStatus aggregateStatus =
                AutoScheduleOptimizationResult.SolverStatus.OPTIMAL;
        BigDecimal aggregateObjective = BigDecimal.ZERO;
        BigDecimal aggregateBound = BigDecimal.ZERO;
        long aggregateDurationMillis = 0;
        int selectedCount = 0;
        for (List<ShowtimeCandidate> serviceDateCandidates : candidatesByServiceDate.values()) {
            ScopeOptimization scope = solveServiceDate(serviceDateCandidates, utilities);
            if (scope.status() == AutoScheduleOptimizationResult.SolverStatus.FEASIBLE) {
                aggregateStatus = AutoScheduleOptimizationResult.SolverStatus.FEASIBLE;
            }
            aggregateObjective = aggregateObjective.add(scope.objectiveValue());
            aggregateBound = aggregateBound.add(scope.bestObjectiveBound());
            aggregateDurationMillis += scope.durationMillis();
            selectedCount += scope.selectedCount();
        }

        rankCandidates(candidates, utilities);
        selectionValidator.validateGlobalSelectionInvariant(candidates);

        String explanation = "CP-SAT selected " + selectedCount + " showtimes across "
                + candidatesByServiceDate.size() + " service-date scopes by maximizing expected "
                + "contribution with capacity/risk penalties, diminishing movie value, demand coverage "
                + "and bounded cold-start exploration.";
        scoringContext.setOptimizationResult(new AutoScheduleOptimizationResult(
                aggregateStatus,
                SOLVER_VERSION,
                aggregateObjective.setScale(3, RoundingMode.HALF_UP),
                aggregateBound.setScale(3, RoundingMode.HALF_UP),
                aggregateDurationMillis,
                selectedCount,
                explanation));
    }

    private ScopeOptimization solveServiceDate(
            List<ShowtimeCandidate> valid,
            Map<ShowtimeCandidate, Long> aggregateUtilities) {
        CpModel model = new CpModel();
        Map<ShowtimeCandidate, BoolVar> variables = new IdentityHashMap<>();
        Map<ShowtimeCandidate, Long> utilities = new IdentityHashMap<>();
        List<LinearArgument> objectiveTerms = new ArrayList<>();
        List<Long> objectiveCoefficients = new ArrayList<>();

        for (int index = 0; index < valid.size(); index++) {
            ShowtimeCandidate candidate = valid.get(index);
            BoolVar variable = model.newBoolVar("candidate_" + index);
            long utility = candidateUtility(candidate);
            variables.put(candidate, variable);
            utilities.put(candidate, utility);
            aggregateUtilities.put(candidate, utility);
            objectiveTerms.add(variable);
            objectiveCoefficients.add(utility);
            candidate.setScore(BigDecimal.valueOf(utility).setScale(3));
            candidate.setScoreBreakdown(scoreBreakdown(candidate, utility));
            candidate.setSelected(false);
        }

        addAuditoriumNoOverlapConstraints(model, valid, variables);
        addDiminishingMarginalValue(model, valid, variables, utilities,
                objectiveTerms, objectiveCoefficients);
        addCloseShowtimeCannibalization(model, valid, variables, utilities,
                objectiveTerms, objectiveCoefficients);
        model.addGreaterOrEqual(LinearExpr.sum(valid.stream()
                .map(variables::get).toArray(LinearArgument[]::new)), 1);
        addGreedySolutionHint(model, valid, variables, utilities);
        model.maximize(LinearExpr.weightedSum(
                objectiveTerms.toArray(LinearArgument[]::new),
                objectiveCoefficients.stream().mapToLong(Long::longValue).toArray()));

        CpSolver solver = new CpSolver();
        configureSolver(solver);
        long started = System.nanoTime();
        CpSolverStatus rawStatus = solver.solve(model);
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        AutoScheduleOptimizationResult.SolverStatus status = mapStatus(rawStatus);
        if (status == AutoScheduleOptimizationResult.SolverStatus.INFEASIBLE) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_SOLVER_INFEASIBLE);
        }
        if (status == AutoScheduleOptimizationResult.SolverStatus.MODEL_INVALID) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_SOLVER_MODEL_INVALID);
        }
        if (status == AutoScheduleOptimizationResult.SolverStatus.TIMEOUT) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_SOLVER_TIMEOUT);
        }

        int selectedCount = 0;
        for (ShowtimeCandidate candidate : valid) {
            boolean selected = solver.booleanValue(variables.get(candidate));
            candidate.setSelected(selected);
            if (selected) selectedCount++;
        }
        if (selectedCount == 0) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_SOLVER_INFEASIBLE);
        }
        return new ScopeOptimization(
                status,
                BigDecimal.valueOf(solver.objectiveValue()).setScale(3, RoundingMode.HALF_UP),
                BigDecimal.valueOf(solver.bestObjectiveBound()).setScale(3, RoundingMode.HALF_UP),
                durationMillis,
                selectedCount);
    }

    private void addAuditoriumNoOverlapConstraints(
            CpModel model,
            List<ShowtimeCandidate> candidates,
            Map<ShowtimeCandidate, BoolVar> variables) {
        Map<Long, List<ShowtimeCandidate>> byAuditorium = new HashMap<>();
        candidates.forEach(candidate -> byAuditorium
                .computeIfAbsent(candidate.getAuditoriumId(), ignored -> new ArrayList<>())
                .add(candidate));
        for (List<ShowtimeCandidate> roomCandidates : byAuditorium.values()) {
            TreeSet<Instant> endpoints = new TreeSet<>();
            roomCandidates.forEach(candidate -> {
                endpoints.add(candidate.getStartTime());
                endpoints.add(candidate.getOccupancyEndTime());
            });
            List<Instant> points = List.copyOf(endpoints);
            for (int point = 0; point + 1 < points.size(); point++) {
                Instant segmentStart = points.get(point);
                Instant segmentEnd = points.get(point + 1);
                LinearArgument[] active = roomCandidates.stream()
                        .filter(candidate -> candidate.getStartTime().isBefore(segmentEnd)
                                && candidate.getOccupancyEndTime().isAfter(segmentStart))
                        .map(variables::get)
                        .toArray(LinearArgument[]::new);
                if (active.length > 1) {
                    model.addLessOrEqual(LinearExpr.sum(active), 1);
                }
            }
        }
    }

    private void addDiminishingMarginalValue(
            CpModel model,
            List<ShowtimeCandidate> candidates,
            Map<ShowtimeCandidate, BoolVar> variables,
            Map<ShowtimeCandidate, Long> utilities,
            List<LinearArgument> objectiveTerms,
            List<Long> objectiveCoefficients) {
        Map<MovieDateKey, List<ShowtimeCandidate>> grouped = new LinkedHashMap<>();
        candidates.forEach(candidate -> grouped.computeIfAbsent(
                new MovieDateKey(movieId(candidate), candidate.getServiceDate()),
                ignored -> new ArrayList<>()).add(candidate));
        int groupIndex = 0;
        for (List<ShowtimeCandidate> group : grouped.values()) {
            LinearArgument[] groupVariables = group.stream().map(variables::get)
                    .toArray(LinearArgument[]::new);
            IntVar count = model.newIntVar(0, group.size(), "movie_count_" + groupIndex);
            model.addEquality(count, LinearExpr.sum(groupVariables));
            long reference = medianPositiveUtility(group, utilities);
            BoolVar first = threshold(model, count, 1, group.size(),
                    "movie_coverage_" + groupIndex);
            objectiveTerms.add(first);
            objectiveCoefficients.add(Math.max(1L, Math.round(reference * 0.15)));
            int tiers = Math.min(MAX_MARGINAL_TIERS, group.size());
            for (int tier = 2; tier <= tiers; tier++) {
                BoolVar threshold = threshold(model, count, tier, group.size(),
                        "movie_tier_" + groupIndex + "_" + tier);
                objectiveTerms.add(threshold);
                objectiveCoefficients.add(-Math.max(1L,
                        Math.round(reference * 0.12 * (tier - 1L))));
            }
            if (group.stream().anyMatch(candidate -> candidate.getRiskFlags()
                    .contains("COLD_START_EXPLORATION"))) {
                objectiveTerms.add(first);
                objectiveCoefficients.add(Math.max(1L, Math.round(reference * 0.05)));
            }
            groupIndex++;
        }
    }

    private void addCloseShowtimeCannibalization(
            CpModel model,
            List<ShowtimeCandidate> candidates,
            Map<ShowtimeCandidate, BoolVar> variables,
            Map<ShowtimeCandidate, Long> utilities,
            List<LinearArgument> objectiveTerms,
            List<Long> objectiveCoefficients) {
        for (long offset : List.of(0L, CLOSE_WINDOW_MINUTES / 2L)) {
            Map<CloseWindowKey, List<ShowtimeCandidate>> groups = new LinkedHashMap<>();
            for (ShowtimeCandidate candidate : candidates) {
                long minute = candidate.getStartTime().getEpochSecond() / 60L;
                long bucket = Math.floorDiv(minute + offset, CLOSE_WINDOW_MINUTES);
                groups.computeIfAbsent(new CloseWindowKey(
                        movieId(candidate), candidate.getServiceDate(), bucket, offset),
                        ignored -> new ArrayList<>()).add(candidate);
            }
            int index = 0;
            for (List<ShowtimeCandidate> group : groups.values()) {
                if (group.size() < 2) continue;
                IntVar count = model.newIntVar(0, group.size(),
                        "close_count_" + offset + "_" + index);
                model.addEquality(count, LinearExpr.sum(group.stream().map(variables::get)
                        .toArray(LinearArgument[]::new)));
                BoolVar repeated = threshold(model, count, 2, group.size(),
                        "close_repeat_" + offset + "_" + index);
                long reference = medianPositiveUtility(group, utilities);
                objectiveTerms.add(repeated);
                objectiveCoefficients.add(-Math.max(1L, Math.round(reference * 0.04)));
                index++;
            }
        }
    }

    private BoolVar threshold(CpModel model, IntVar count, int threshold,
                              int maximum, String name) {
        BoolVar flag = model.newBoolVar(name);
        model.addGreaterOrEqual(count, threshold).onlyEnforceIf(flag);
        model.addLessOrEqual(count, threshold - 1L).onlyEnforceIf(flag.not());
        return flag;
    }

    private long candidateUtility(ShowtimeCandidate candidate) {
        BigDecimal contribution = candidate.getExpectedContribution();
        BigDecimal emptySeats = BigDecimal.valueOf(candidate.getAuditoriumCapacity())
                .subtract(candidate.getExpectedAttendance()).max(BigDecimal.ZERO);
        BigDecimal excessCapacityCost = emptySeats
                .multiply(candidate.getPricingSnapshot().weightedAverageTicketPrice())
                .multiply(new BigDecimal("0.02"));
        BigDecimal riskPenalty = contribution
                .multiply(BigDecimal.ONE.subtract(candidate.getDemandConfidence()))
                .multiply(new BigDecimal("0.08"));
        BigDecimal demandCoverage = candidate.getExpectedRevenue().multiply(new BigDecimal("0.03"));
        BigDecimal utility = contribution.subtract(excessCapacityCost).subtract(riskPenalty)
                .add(demandCoverage);
        return Math.max(1L, utility.setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    private void addGreedySolutionHint(
            CpModel model,
            List<ShowtimeCandidate> candidates,
            Map<ShowtimeCandidate, BoolVar> variables,
            Map<ShowtimeCandidate, Long> utilities) {
        Map<Long, List<ShowtimeCandidate>> selectedByAuditorium = new HashMap<>();
        List<ShowtimeCandidate> greedyOrder = new ArrayList<>(candidates);
        greedyOrder.sort(Comparator
                .comparing((ShowtimeCandidate candidate) -> utilities.get(candidate),
                        Comparator.reverseOrder())
                .thenComparing(candidateOrder()));
        for (ShowtimeCandidate candidate : greedyOrder) {
            List<ShowtimeCandidate> selected = selectedByAuditorium
                    .computeIfAbsent(candidate.getAuditoriumId(), ignored -> new ArrayList<>());
            boolean overlaps = selected.stream().anyMatch(existing ->
                    candidate.getStartTime().isBefore(existing.getOccupancyEndTime())
                            && candidate.getOccupancyEndTime().isAfter(existing.getStartTime()));
            model.addHint(variables.get(candidate), overlaps ? 0 : 1);
            if (!overlaps) {
                selected.add(candidate);
            }
        }
    }

    private Map<String, BigDecimal> scoreBreakdown(ShowtimeCandidate candidate, long utility) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("expectedContribution", candidate.getExpectedContribution());
        result.put("expectedRevenue", candidate.getExpectedRevenue());
        result.put("expectedOccupancy", candidate.getExpectedOccupancy());
        result.put("demandConfidence", candidate.getDemandConfidence());
        result.put("cpSatBaseUtility", BigDecimal.valueOf(utility));
        return Map.copyOf(result);
    }

    private long medianPositiveUtility(List<ShowtimeCandidate> group,
                                       Map<ShowtimeCandidate, Long> utilities) {
        long[] values = group.stream().mapToLong(candidate -> Math.max(1L, utilities.get(candidate)))
                .sorted().toArray();
        return values[values.length / 2];
    }

    private void rankCandidates(List<ShowtimeCandidate> candidates,
                                Map<ShowtimeCandidate, Long> utilities) {
        List<ShowtimeCandidate> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator
                .comparing(ShowtimeCandidate::isSelected).reversed()
                .thenComparing(candidate -> utilities.getOrDefault(candidate, Long.MIN_VALUE),
                        Comparator.reverseOrder())
                .thenComparing(candidateOrder()));
        int rank = 1;
        for (ShowtimeCandidate candidate : ranked) candidate.setRankingPosition(rank++);
        candidates.sort(Comparator.comparing(ShowtimeCandidate::getRankingPosition));
    }

    private Comparator<ShowtimeCandidate> candidateOrder() {
        return Comparator.comparing(ShowtimeCandidate::getServiceDate)
                .thenComparing(ShowtimeCandidate::getStartTime)
                .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
                .thenComparing(ShowtimeCandidate::getMovieVersionPublicId);
    }

    private Long movieId(ShowtimeCandidate candidate) {
        if (candidate.getMovieVersionSnapshot() != null
                && candidate.getMovieVersionSnapshot().movie() != null) {
            return candidate.getMovieVersionSnapshot().movie().id();
        }
        return candidate.getMovie() == null ? null : candidate.getMovie().getId();
    }

    AutoScheduleOptimizationResult.SolverStatus mapStatus(CpSolverStatus status) {
        return switch (status) {
            case OPTIMAL -> AutoScheduleOptimizationResult.SolverStatus.OPTIMAL;
            case FEASIBLE -> AutoScheduleOptimizationResult.SolverStatus.FEASIBLE;
            case INFEASIBLE -> AutoScheduleOptimizationResult.SolverStatus.INFEASIBLE;
            case MODEL_INVALID -> AutoScheduleOptimizationResult.SolverStatus.MODEL_INVALID;
            case UNKNOWN -> AutoScheduleOptimizationResult.SolverStatus.TIMEOUT;
            default -> AutoScheduleOptimizationResult.SolverStatus.MODEL_INVALID;
        };
    }

    void configureSolver(CpSolver solver) {
        if (timeoutSeconds > 0) {
            solver.getParameters().setMaxTimeInSeconds(timeoutSeconds);
        }
        if (relativeGapLimit > 0) {
            solver.getParameters().setRelativeGapLimit(relativeGapLimit);
        }
        solver.getParameters().setNumSearchWorkers(1);
        solver.getParameters().setRandomSeed(randomSeed);
    }

    boolean hasSolverTimeLimit() {
        return timeoutSeconds > 0;
    }

    private record MovieDateKey(Long movieId, LocalDate serviceDate) {
    }

    private record CloseWindowKey(Long movieId, LocalDate serviceDate, long bucket, long offset) {
    }

    private record ScopeOptimization(
            AutoScheduleOptimizationResult.SolverStatus status,
            BigDecimal objectiveValue,
            BigDecimal bestObjectiveBound,
            long durationMillis,
            int selectedCount) {
    }
}
