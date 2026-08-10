package com.project.scoreservice.dto;

public record ReverseAdjustmentRequest(
        Long historyId,
        String reason,
        String requestId
) {}
