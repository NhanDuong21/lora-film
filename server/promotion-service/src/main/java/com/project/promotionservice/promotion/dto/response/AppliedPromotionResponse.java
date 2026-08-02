package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.PromotionType;

import java.math.BigDecimal;

public record AppliedPromotionResponse(
        String promotionPublicId,
        String userPromotionPublicId,
        String campaignPublicId,
        PromotionType promotionType,
        String code,
        String name,
        BigDecimal discountAmount,
        int priority,
        boolean stackable) {
}
