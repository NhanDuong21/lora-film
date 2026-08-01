package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FinalizeCheckoutRequest(
        @Min(value = 0, message = "scorePoints must not be negative")
        Integer scorePoints,

        @Size(max = 100, message = "scoreIdempotencyKey must not exceed 100 characters")
        String scoreIdempotencyKey,

        @Size(max = 10)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        selectedUserPromotionPublicIds,

        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$")
        String couponCode
) {
    public FinalizeCheckoutRequest(Integer scorePoints, String scoreIdempotencyKey) {
        this(scorePoints, scoreIdempotencyKey, List.of(), null);
    }

    public int normalizedScorePoints() {
        return scorePoints == null ? 0 : scorePoints;
    }

    public PromotionSelectionRequest promotionSelection() {
        return new PromotionSelectionRequest(selectedUserPromotionPublicIds, couponCode);
    }
}
