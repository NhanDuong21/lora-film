package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PromotionSelectionRequest(
        @Size(max = 10)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        selectedUserPromotionPublicIds,

        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$")
        String couponCode) {

    public List<String> normalizedWalletIds() {
        return selectedUserPromotionPublicIds == null
                ? List.of()
                : selectedUserPromotionPublicIds.stream().distinct().toList();
    }

    public String normalizedCouponCode() {
        return couponCode == null || couponCode.isBlank()
                ? null
                : couponCode.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
