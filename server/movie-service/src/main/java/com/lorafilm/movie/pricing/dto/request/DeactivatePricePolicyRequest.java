package com.lorafilm.movie.pricing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeactivatePricePolicyRequest(
        @NotNull Long expectedVersion,
        @NotBlank @Size(max = 500) String reason
) {
}
