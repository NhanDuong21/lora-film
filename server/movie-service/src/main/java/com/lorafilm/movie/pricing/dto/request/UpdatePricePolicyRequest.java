package com.lorafilm.movie.pricing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdatePricePolicyRequest(
        @NotNull Long expectedVersion,
        @NotBlank @Size(max = 120) String name,
        @NotBlank String cinemaId,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Integer priority,
        @NotNull @Valid List<PricePolicyRuleRequest> rules
) {
}
