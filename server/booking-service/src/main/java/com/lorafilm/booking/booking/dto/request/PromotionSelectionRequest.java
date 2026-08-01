package com.lorafilm.booking.booking.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

public record PromotionSelectionRequest(
        @Size(max = 10)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        selectedUserPromotionPublicIds,

        @Size(max = 10)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        selectedPromotionPublicIds,

        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$")
        String couponCode,

        @Size(max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$")
        String paymentMethod,

        @Size(max = 300)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        evaluationUserPromotionPublicIds,

        @Size(max = 300)
        List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String>
        evaluationPromotionPublicIds) {

    public PromotionSelectionRequest(
            List<String> selectedUserPromotionPublicIds,
            List<String> selectedPromotionPublicIds,
            String couponCode,
            List<String> evaluationUserPromotionPublicIds,
            List<String> evaluationPromotionPublicIds) {
        this(selectedUserPromotionPublicIds, selectedPromotionPublicIds, couponCode,
                null, evaluationUserPromotionPublicIds, evaluationPromotionPublicIds);
    }

    public List<String> normalizedWalletIds() {
        return selectedUserPromotionPublicIds == null
                ? List.of()
                : selectedUserPromotionPublicIds.stream().distinct().toList();
    }

    public List<String> normalizedPromotionIds() {
        return selectedPromotionPublicIds == null
                ? List.of()
                : selectedPromotionPublicIds.stream().distinct().toList();
    }

    public String normalizedCouponCode() {
        return couponCode == null || couponCode.isBlank()
                ? null
                : couponCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public String normalizedPaymentMethod() {
        return paymentMethod == null || paymentMethod.isBlank()
                ? null
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    public List<String> normalizedEvaluationWalletIds() {
        return evaluationUserPromotionPublicIds == null
                ? List.of()
                : evaluationUserPromotionPublicIds.stream().distinct().toList();
    }

    public List<String> normalizedEvaluationPromotionIds() {
        return evaluationPromotionPublicIds == null
                ? List.of()
                : evaluationPromotionPublicIds.stream().distinct().toList();
    }
}
