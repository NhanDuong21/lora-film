package com.lorafilm.movie.pricing.dto.response;

import java.util.List;

public record PriceSeatTypeDiagnosticDto(
        String seatTypeId,
        String seatTypeCode,
        String seatTypeName,
        List<String> candidateRuleIds
) {
}
