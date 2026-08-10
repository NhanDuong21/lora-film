package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreflightResponse;
import com.lorafilm.movie.autoschedule.model.AutoScheduleOptimizationResult;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class AutoScheduleMetrics {

    private final MeterRegistry registry;

    public AutoScheduleMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public static AutoScheduleMetrics noop() {
        return new AutoScheduleMetrics(null);
    }

    public void recordPreflight(Duration duration, AutoSchedulePreflightResponse response) {
        if (registry == null) return;
        Timer.builder("autoschedule.preflight.duration")
                .tag("outcome", response.canGenerate() ? "ready" : "blocked")
                .register(registry).record(duration);
        summary("autoschedule.eligible.pairs", response.compatiblePairCount());
        response.blockers().forEach(blocker -> registry.counter(
                "autoschedule.preflight.blockers", "code", blocker.code()).increment());
    }

    public void recordCandidateGeneration(Duration duration, long candidateCount) {
        if (registry == null) return;
        registry.timer("autoschedule.candidate.generation.duration").record(duration);
        summary("autoschedule.candidates", candidateCount);
    }

    public void recordGeneration(String engine,
                                 Duration endToEndDuration,
                                 AutoScheduleOptimizationResult optimization,
                                 BigDecimal expectedContribution,
                                 BigDecimal expectedOccupancy) {
        if (registry == null) return;
        String boundedEngine = boundedEngine(engine);
        Timer.builder("autoschedule.generation.duration").tag("engine", boundedEngine)
                .register(registry).record(endToEndDuration);
        Timer.builder("autoschedule.solver.duration")
                .tag("engine", boundedEngine)
                .tag("status", optimization.status().name())
                .register(registry).record(Duration.ofMillis(optimization.durationMillis()));
        registry.counter("autoschedule.solver.runs", "engine", boundedEngine,
                "status", optimization.status().name()).increment();
        if (optimization.status() == AutoScheduleOptimizationResult.SolverStatus.TIMEOUT) {
            registry.counter("autoschedule.solver.timeouts", "engine", boundedEngine).increment();
        }
        summary("autoschedule.selected.showtimes", optimization.selectedCount());
        summary("autoschedule.expected.contribution", decimal(expectedContribution));
        summary("autoschedule.expected.occupancy", decimal(expectedOccupancy));
    }

    public void recordGenerationFailure(String engine, Duration duration, String outcome) {
        if (registry == null) return;
        Timer.builder("autoschedule.generation.duration")
                .tag("engine", boundedEngine(engine)).tag("outcome", boundedOutcome(outcome))
                .register(registry).record(duration);
    }

    public void recordApply(String outcome) {
        if (registry == null) return;
        String bounded = boundedOutcome(outcome);
        registry.counter("autoschedule.apply", "outcome", bounded).increment();
        if (bounded.equals("stale")) registry.counter("autoschedule.preview.stale").increment();
        if (bounded.equals("conflict")) registry.counter("autoschedule.apply.conflicts").increment();
    }

    public void recordAdminModification(int changedItems) {
        if (registry == null || changedItems <= 0) return;
        registry.counter("autoschedule.admin.modifications").increment();
        summary("autoschedule.admin.modified.items", changedItems);
    }

    public void recordPreviewCancellation() {
        if (registry != null) registry.counter("autoschedule.preview.cancellations").increment();
    }

    public void recordDemandHistory(BigDecimal actualOccupancy, BigDecimal cancellationRate) {
        if (registry == null) return;
        summary("autoschedule.actual.occupancy", decimal(actualOccupancy));
        summary("autoschedule.history.cancellation.rate", decimal(cancellationRate));
    }

    public void recordHistoricalForecastError(BigDecimal predictedOccupancy,
                                              BigDecimal observedOccupancy) {
        if (registry == null || predictedOccupancy == null || observedOccupancy == null) return;
        summary("autoschedule.forecast.error",
                predictedOccupancy.subtract(observedOccupancy).abs().doubleValue());
    }

    public void recordShadow(String outcome, int selectedDelta,
                             BigDecimal contributionDelta, BigDecimal occupancyDelta,
                             long primeTimeDelta, long auditoriumUseDelta,
                             Duration shadowDuration, int constraintViolations) {
        if (registry == null) return;
        registry.counter("autoschedule.shadow.runs", "engine", "legacy_s5",
                "outcome", boundedOutcome(outcome)).increment();
        summary("autoschedule.shadow.selected.delta", Math.abs(selectedDelta));
        summary("autoschedule.shadow.contribution.delta", Math.abs(decimal(contributionDelta)));
        summary("autoschedule.shadow.occupancy.delta", Math.abs(decimal(occupancyDelta)));
        summary("autoschedule.shadow.prime_time.delta", Math.abs(primeTimeDelta));
        summary("autoschedule.shadow.auditorium_use.delta", Math.abs(auditoriumUseDelta));
        Timer.builder("autoschedule.shadow.duration").tag("engine", "legacy_s5")
                .register(registry).record(shadowDuration);
        summary("autoschedule.shadow.constraint.violations", constraintViolations);
    }

    private void summary(String name, double value) {
        if (registry == null || !Double.isFinite(value)) return;
        DistributionSummary.builder(name).register(registry).record(Math.max(0D, value));
    }

    private double decimal(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    private String boundedEngine(String value) {
        return value != null && value.contains("CP_SAT") ? "cp_sat" : "legacy_s5";
    }

    private String boundedOutcome(String value) {
        if (value == null) return "failed";
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("stale") || normalized.contains("expired")) return "stale";
        if (normalized.contains("conflict") || normalized.contains("version")) return "conflict";
        if (normalized.contains("timeout")) return "timeout";
        if (normalized.contains("idempot")) return "idempotent_replay";
        if (normalized.contains("success")) return "success";
        if (normalized.contains("blocked")) return "blocked";
        return "failed";
    }
}
