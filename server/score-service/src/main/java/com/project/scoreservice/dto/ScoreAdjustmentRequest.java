package com.project.scoreservice.dto;

public record ScoreAdjustmentRequest(
    ScoreAdjustmentType type,
    Integer points,
    Boolean allowNegative,
    String reason,
    String requestId
) {}
