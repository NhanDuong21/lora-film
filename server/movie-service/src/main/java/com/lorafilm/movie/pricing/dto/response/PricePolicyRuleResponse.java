package com.lorafilm.movie.pricing.dto.response;

import com.lorafilm.movie.pricing.domain.enums.PriceDayType;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PricePolicyRuleResponse(
        String publicId,
        String seatTypeId,
        String seatTypeCode,
        String seatTypeName,
        String auditoriumId,
        String auditoriumName,
        String screenType,
        PriceDayType dayType,
        LocalTime timeBandStart,
        LocalTime timeBandEnd,
        BigDecimal price,
        boolean active
) {
}
