package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreRefundRequest(
    @NotNull(message = "User ID is required")
    Long userId,
    @NotNull(message = "Booking ID is required")
    Long bookingId,
    @NotNull(message = "Points to refund is required")
    @Min(value = 1, message = "Points to refund must be positive")
    Integer pointsToRefund,
    String originalRedeemEventId,
    String eventId,
    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey,
    String reason
) {
    public ScoreRefundRequest(Long userId, Long bookingId, Integer pointsToRefund, String reason, String eventId, String idempotencyKey) {
        this(userId, bookingId, pointsToRefund, null, eventId, idempotencyKey, reason);
    }
}

