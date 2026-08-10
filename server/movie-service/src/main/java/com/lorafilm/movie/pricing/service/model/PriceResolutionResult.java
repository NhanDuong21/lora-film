package com.lorafilm.movie.pricing.service.model;

import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.seat.domain.entity.SeatType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PriceResolutionResult(
        String currency,
        String timezone,
        Instant resolvedAt,
        List<ResolvedPrice> resolvedPrices,
        List<SeatTypeDiagnostic> missingSeatTypes,
        List<SeatTypeDiagnostic> ambiguousSeatTypes
) {
    public boolean isComplete() {
        return missingSeatTypes.isEmpty() && ambiguousSeatTypes.isEmpty();
    }

    public record ResolvedPrice(
            SeatType seatType,
            BigDecimal price,
            PricePolicy policy,
            PricePolicyRule rule
    ) {
    }

    public record SeatTypeDiagnostic(
            String seatTypeId,
            String seatTypeCode,
            String seatTypeName,
            List<String> candidateRuleIds
    ) {
    }
}
