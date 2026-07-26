package com.project.scoreservice.dto;

import java.time.LocalDateTime;

public record ScoreHoldResponse(
    String holdCode,
    Long userId,
    Long bookingId,
    int pointsHeld,
    int availablePointsAfter,
    LocalDateTime expiredAt,
    String status,
    boolean idempotent
) {}
