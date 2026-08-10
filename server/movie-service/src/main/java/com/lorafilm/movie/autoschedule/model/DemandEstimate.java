package com.lorafilm.movie.autoschedule.model;

import java.math.BigDecimal;
import java.util.List;

public record DemandEstimate(
        BigDecimal expectedAttendance,
        BigDecimal expectedOccupancy,
        BigDecimal expectedRevenue,
        BigDecimal expectedContribution,
        BigDecimal confidence,
        String explanation,
        String demandModelVersion,
        boolean primeTime,
        List<String> riskFlags) {
}
