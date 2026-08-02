package com.lorafilm.booking.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PromotionQuoteResponse(
        boolean eligible,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String currency,
        List<AppliedPromotion> appliedPromotions,
        List<PromotionEvaluation> promotionEvaluations,
        List<String> warnings) {

    public PromotionQuoteResponse(
            boolean eligible,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String currency,
            List<AppliedPromotion> appliedPromotions,
            List<String> warnings) {
        this(eligible, originalAmount, discountAmount, finalAmount, currency,
                appliedPromotions, List.of(), warnings);
    }

    public record AppliedPromotion(
            String promotionPublicId,
            String userPromotionPublicId,
            String campaignPublicId,
            String promotionType,
            String code,
            String name,
            BigDecimal discountAmount,
            Integer priority,
            boolean stackable) {
    }

    public record PromotionEvaluation(
            String promotionPublicId,
            String userPromotionPublicId,
            String promotionType,
            boolean eligible,
            BigDecimal discountAmount,
            String reasonCode,
            String reason) {
    }
}
