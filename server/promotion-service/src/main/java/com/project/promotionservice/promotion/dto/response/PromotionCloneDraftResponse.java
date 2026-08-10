package com.project.promotionservice.promotion.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.promotion.enums.PromotionType;

import java.time.Instant;

public record PromotionCloneDraftResponse(
        String sourcePublicId,
        String sourceName,
        String suggestedCampaignPublicId,
        boolean sourceCampaignEditable,
        PromotionType promotionType,
        String suggestedCode,
        String suggestedName,
        String description,
        Boolean publicVisible,
        Integer priority,
        Boolean stackable,
        JsonNode conditionsJson,
        JsonNode actionsJson,
        JsonNode metadataJson,
        Integer maxRedemptions,
        Integer maxRedemptionsPerUser,
        Instant suggestedValidFrom,
        Instant suggestedValidTo,
        boolean validityWindowShifted) {
}
