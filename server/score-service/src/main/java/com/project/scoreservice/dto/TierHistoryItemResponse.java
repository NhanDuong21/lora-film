package com.project.scoreservice.dto;

import java.time.LocalDateTime;

public record TierHistoryItemResponse(
    Long id,
    String oldTierCode,
    String newTierCode,
    String reason,
    LocalDateTime createdAt
) {}
