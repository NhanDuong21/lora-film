package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.PromotionType;

import java.math.BigDecimal;

public record PromotionEligibilityResponse(
        String promotionPublicId,
        String userPromotionPublicId,
        PromotionType promotionType,
        boolean eligible,
        BigDecimal discountAmount,
        String reasonCode,
        String reason) {
}
