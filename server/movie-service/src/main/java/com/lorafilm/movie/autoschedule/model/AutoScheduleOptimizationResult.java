package com.lorafilm.movie.autoschedule.model;

import java.math.BigDecimal;

public record AutoScheduleOptimizationResult(
        SolverStatus status,
        String solverVersion,
        BigDecimal objectiveValue,
        BigDecimal bestBound,
        long durationMillis,
        int selectedCount,
        String explanation) {

    public enum SolverStatus {
        OPTIMAL,
        FEASIBLE,
        INFEASIBLE,
        TIMEOUT,
        MODEL_INVALID
    }
}
