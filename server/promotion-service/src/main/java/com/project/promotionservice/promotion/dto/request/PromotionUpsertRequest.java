package com.project.promotionservice.promotion.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.promotion.enums.PromotionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record PromotionUpsertRequest(
        @NotBlank @Size(max = 36) String campaignPublicId,
        @NotNull PromotionType promotionType,
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$",
                message = "code must contain only letters, numbers, underscores, or hyphens")
        String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull Boolean publicVisible,
        @NotNull @Min(0) Integer priority,
        @NotNull Boolean stackable,
        @NotNull JsonNode conditionsJson,
        @NotNull JsonNode actionsJson,
        JsonNode metadataJson,
        @Min(1) Integer maxRedemptions,
        @NotNull @Min(1) Integer maxRedemptionsPerUser,
        @NotNull Instant validFrom,
        @NotNull Instant validTo,
        @Size(max = 36) String clonedFromPublicId) {
}
