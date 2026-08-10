package com.lorafilm.movie.autoschedule.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CandidatePricingSnapshot(
        String currency,
        String timezone,
        Instant resolvedAt,
        BigDecimal weightedAverageTicketPrice,
        List<PriceLine> prices,
        String fingerprint) {

    public record PriceLine(
            String seatTypePublicId,
            String seatTypeCode,
            BigDecimal price,
            long seatCount,
            String policyPublicId,
            String rulePublicId) {
    }
}
