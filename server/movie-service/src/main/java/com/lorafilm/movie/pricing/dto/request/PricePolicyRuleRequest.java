package com.lorafilm.movie.pricing.dto.request;

import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PricePolicyRuleRequest(
        @NotBlank String seatTypeId,
        String auditoriumId,
        ScreenType screenType,
        @NotNull PriceDayType dayType,
        LocalTime timeBandStart,
        LocalTime timeBandEnd,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        Boolean active
) {
}
