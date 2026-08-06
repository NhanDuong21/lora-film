package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.DemandCandidateFacts;
import com.lorafilm.movie.autoschedule.model.DemandHistorySnapshot;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticDemandEngineV1Test {

    private final AutomaticDemandEngineV1 engine = new AutomaticDemandEngineV1();

    @Test
    void unavailableHistoryUsesDeterministicColdStartWithExplicitRisk() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        DemandCandidateFacts candidate = new DemandCandidateFacts(
                "movie-1", MovieFormat.TWO_D, 100,
                Instant.parse("2026-08-08T12:00:00Z"), date, date.minusDays(2),
                ZoneId.of("Asia/Ho_Chi_Minh"), new BigDecimal("100000"), 0);
        DemandHistorySnapshot unavailable = DemandHistorySnapshot.unavailable(
                date.minusDays(30), date.minusDays(1), Instant.parse("2026-08-06T00:00:00Z"));

        var first = engine.estimate(candidate, unavailable);
        var second = engine.estimate(candidate, unavailable);

        assertEquals(first, second);
        assertEquals(new BigDecimal("0.20"), first.confidence());
        assertTrue(first.expectedAttendance().signum() > 0);
        assertTrue(first.riskFlags().contains("ANALYTICS_SOURCE_UNAVAILABLE"));
        assertTrue(first.riskFlags().contains("COLD_START_EXPLORATION"));
    }

    @Test
    void observedMovieHistoryRaisesConfidenceAndAppliesLossRates() {
        LocalDate date = LocalDate.of(2026, 8, 8);
        var observed = new DemandHistorySnapshot.Aggregate(
                20, 6, 240, new BigDecimal("0.60"), new BigDecimal("8"),
                new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("0.10"),
                new BigDecimal("0.05"), new BigDecimal("100000"), true);
        DemandHistorySnapshot history = new DemandHistorySnapshot(
                true, "v1", Instant.parse("2026-08-06T00:00:00Z"),
                date.minusDays(30), date.minusDays(1), 20, 20, observed,
                List.of(new DemandHistorySnapshot.MovieHistory("movie-1", observed)),
                List.of(), List.of());
        DemandCandidateFacts candidate = new DemandCandidateFacts(
                "movie-1", MovieFormat.TWO_D, 100,
                Instant.parse("2026-08-08T12:00:00Z"), date, date.minusDays(10),
                ZoneId.of("Asia/Ho_Chi_Minh"), new BigDecimal("100000"), 1);

        var estimate = engine.estimate(candidate, history);

        assertTrue(estimate.confidence().compareTo(new BigDecimal("0.50")) > 0);
        assertTrue(estimate.expectedContribution().compareTo(estimate.expectedRevenue()) < 0);
        assertTrue(estimate.riskFlags().isEmpty());
        assertEquals(AutomaticDemandEngineV1.MODEL_VERSION, estimate.demandModelVersion());
    }
}
