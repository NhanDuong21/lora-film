package com.project.scoreservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScoreHoldResponse(
    String holdCode,
    Long userId,
    Long bookingId,
    int pointsHeld,
    int availablePointsAfter,
    LocalDateTime expiredAt,
    String status,
    boolean idempotent,
    BigDecimal discountAmount,
    BigDecimal valuePerPoint
) {
    public ScoreHoldResponse(
            String holdCode,
            Long userId,
            Long bookingId,
            int pointsHeld,
            int availablePointsAfter,
            LocalDateTime expiredAt,
            String status,
            boolean idempotent) {
        this(holdCode, userId, bookingId, pointsHeld, availablePointsAfter,
                expiredAt, status, idempotent, null, null);
    }
}
