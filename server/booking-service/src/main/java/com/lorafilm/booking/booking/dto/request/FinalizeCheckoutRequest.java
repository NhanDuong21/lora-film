package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record FinalizeCheckoutRequest(
        @Min(value = 0, message = "scorePoints must not be negative")
        Integer scorePoints,

        @Size(max = 100, message = "scoreIdempotencyKey must not exceed 100 characters")
        String scoreIdempotencyKey
) {
    public int normalizedScorePoints() {
        return scorePoints == null ? 0 : scorePoints;
    }
}
