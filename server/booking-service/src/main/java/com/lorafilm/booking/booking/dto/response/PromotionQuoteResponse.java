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
        List<String> warnings) {

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
}
