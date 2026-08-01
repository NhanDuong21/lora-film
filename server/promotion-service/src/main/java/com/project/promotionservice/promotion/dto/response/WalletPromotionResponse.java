package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.UserPromotionStatus;

import java.time.Instant;

public record WalletPromotionResponse(
        String publicId,
        String userPublicId,
        UserPromotionStatus status,
        Instant claimedAt,
        Instant validFrom,
        Instant validTo,
        int usageCount,
        int maxUsage,
        PromotionResponse promotion) {
}
