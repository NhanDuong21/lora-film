package com.project.scoreservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreAdjustmentRequest(
    Long userId,
    @NotNull(message = "Adjustment type is required")
    @JsonAlias({"type", "adjustmentType"}) ScoreAdjustmentType type,
    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be at least 1")
    Integer points,
    @JsonAlias({"allowNegative", "affectAccumulatedPoints"}) Boolean allowNegative,
    @JsonAlias({"affectAccumulatedPoints", "allowNegative"}) Boolean affectAccumulatedPoints,
    @NotBlank(message = "Reason is required")
    String reason,
    String requestId,
    String caseId
) {
    public ScoreAdjustmentRequest(Long userId, ScoreAdjustmentType type, Integer points,
                                  Boolean allowNegative, Boolean affectAccumulatedPoints,
                                  String reason, String requestId) {
        this(userId, type, points, allowNegative, affectAccumulatedPoints, reason, requestId, null);
    }

    // 5-arg constructor for backwards compatibility with existing integration tests
    public ScoreAdjustmentRequest(ScoreAdjustmentType type, Integer points, Boolean allowNegative, String reason, String requestId) {
        this(null, type, points, allowNegative, allowNegative, reason, requestId, null);
    }

    public ScoreAdjustmentType getEffectiveType() {
        return type;
    }

    public boolean getEffectiveAffectAccumulatedPoints() {
        if (affectAccumulatedPoints != null) return affectAccumulatedPoints;
        if (allowNegative != null) return allowNegative;
        return false;
    }

    public boolean getEffectiveAllowNegative() {
        if (allowNegative != null) return allowNegative;
        if (affectAccumulatedPoints != null) return affectAccumulatedPoints;
        return false;
    }
}
