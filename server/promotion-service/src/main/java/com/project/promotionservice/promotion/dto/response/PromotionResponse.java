package com.project.promotionservice.promotion.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;

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
        int priority,
        boolean stackable,
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
