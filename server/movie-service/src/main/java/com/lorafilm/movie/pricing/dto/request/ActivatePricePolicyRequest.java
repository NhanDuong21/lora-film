package com.lorafilm.movie.pricing.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActivatePricePolicyRequest(@NotNull Long expectedVersion) {
}
