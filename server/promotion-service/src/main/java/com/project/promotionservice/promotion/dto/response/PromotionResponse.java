package com.project.promotionservice.promotion.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionStackingBlockedReason;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;

import java.time.Instant;

public record PromotionResponse(
        String publicId,
        String campaignPublicId,
        String clonedFromPublicId,
        PromotionType promotionType,
        String code,
        String name,
        String description,
        PromotionStatus status,
        boolean publicVisible,
        PromotionDistributionMode distributionMode,
        boolean testData,
        String environmentTag,
        int priority,
        boolean stackable,
        boolean campaignStackable,
        boolean effectiveStackable,
        PromotionStackingBlockedReason stackingBlockedReason,
        JsonNode conditionsJson,
        JsonNode actionsJson,
        JsonNode metadataJson,
        Integer maxRedemptions,
        int redemptionCount,
        int maxRedemptionsPerUser,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt) {
}
