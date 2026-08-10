package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleOptimizationResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AutoScheduleMetricsTest {

    @Test
    void emitsBoundedEngineAndStatusTagsWithoutBusinessIdentifiers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AutoScheduleMetrics metrics = new AutoScheduleMetrics(registry);
        var result = new AutoScheduleOptimizationResult(
                AutoScheduleOptimizationResult.SolverStatus.OPTIMAL,
                "solver", BigDecimal.TEN, BigDecimal.TEN, 12, 3, "ok");

        metrics.recordGeneration("DEMAND_CP_SAT_V1", Duration.ofMillis(30), result,
                new BigDecimal("12000000"), new BigDecimal("0.65"));

        assertEquals(1D, registry.get("autoschedule.solver.runs")
                .tag("engine", "cp_sat").tag("status", "OPTIMAL").counter().count());
        assertNotNull(registry.find("autoschedule.solver.duration")
                .tag("engine", "cp_sat").timer());
        assertEquals(0, registry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .filter(tag -> tag.getKey().contains("cinema")
                        || tag.getKey().contains("preview")
                        || tag.getKey().contains("movie"))
                .count());
    }
}
