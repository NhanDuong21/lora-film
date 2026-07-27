package com.project.scoreservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreAdjustmentRequest(
    Long userId,
    @JsonAlias({"type", "adjustmentType"}) ScoreAdjustmentType type,
    Integer points,
    @JsonAlias({"allowNegative", "affectAccumulatedPoints"}) Boolean allowNegative,
    @JsonAlias({"affectAccumulatedPoints", "allowNegative"}) Boolean affectAccumulatedPoints,
    String reason,
    String requestId
) {
    // 5-arg constructor for backwards compatibility with existing integration tests
    public ScoreAdjustmentRequest(ScoreAdjustmentType type, Integer points, Boolean allowNegative, String reason, String requestId) {
        this(null, type, points, allowNegative, allowNegative, reason, requestId);
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
