package com.project.promotionservice.promotion.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PromotionCheckoutResponse(
        boolean eligible,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String currency,
        List<AppliedPromotionResponse> appliedPromotions,
        List<PromotionEligibilityResponse> promotionEvaluations,
        List<String> warnings,
        boolean manualSelectionReplaced,
        BigDecimal additionalSavings) {

    public PromotionCheckoutResponse(
            boolean eligible,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String currency,
            List<AppliedPromotionResponse> appliedPromotions,
            List<String> warnings) {
        this(eligible, originalAmount, discountAmount, finalAmount, currency,
                appliedPromotions, List.of(), warnings, false, BigDecimal.ZERO);
    }
}
