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
        boolean runtimeAvailable,
        String unavailableReasonCode,
        String unavailableReason,
        PromotionResponse promotion) {

    public WalletPromotionResponse(
            String publicId,
            String userPublicId,
            UserPromotionStatus status,
            Instant claimedAt,
            Instant validFrom,
            Instant validTo,
            int usageCount,
            int maxUsage,
            PromotionResponse promotion) {
        this(publicId, userPublicId, status, claimedAt, validFrom, validTo,
                usageCount, maxUsage, true, null, null, promotion);
    }
}
