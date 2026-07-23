package com.lorafilm.movie.pricing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PriceResolutionPreviewResponse(
        boolean complete,
        String currency,
        String timezone,
        Instant resolvedAt,
        List<ResolvedLine> prices,
        List<PriceSeatTypeDiagnosticDto> missingSeatTypes,
        List<PriceSeatTypeDiagnosticDto> ambiguousSeatTypes
) {
    public record ResolvedLine(
            String seatTypeId,
            String seatTypeCode,
            String seatTypeName,
            BigDecimal price,
            String policyId,
            String policyName,
            String ruleId
    ) {
    }
}
