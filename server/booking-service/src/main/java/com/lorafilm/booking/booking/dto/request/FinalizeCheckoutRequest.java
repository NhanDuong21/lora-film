package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FinalizeCheckoutRequest(
        @Min(value = 0, message = "scorePoints must not be negative")
        Integer scorePoints,

        @Size(max = 100, message = "scoreIdempotencyKey must not exceed 100 characters")
        String scoreIdempotencyKey,

        @Size(max = 1)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        selectedUserPromotionPublicIds,

        @Size(max = 1)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        selectedPromotionPublicIds,

        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$")
        String couponCode,

        @Size(max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$")
        String paymentMethod
) {
    public FinalizeCheckoutRequest(Integer scorePoints, String scoreIdempotencyKey) {
        this(scorePoints, scoreIdempotencyKey, List.of(), List.of(), null, null);
    }

    public int normalizedScorePoints() {
        return scorePoints == null ? 0 : scorePoints;
    }

    @AssertTrue(message = "Only one voucher or coupon can be selected per booking")
    public boolean isSingleManualSelection() {
        return promotionSelection().isSingleManualSelection();
    }

    public PromotionSelectionRequest promotionSelection() {
        return new PromotionSelectionRequest(
                selectedUserPromotionPublicIds, selectedPromotionPublicIds, couponCode,
                paymentMethod, List.of(), List.of());
    }
}
