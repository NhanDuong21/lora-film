package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ScoreHoldRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Booking ID is required")
    Long bookingId,

    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be positive")
    Integer points,

    Integer ttlSeconds,

    String eventId,

    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey,

    BigDecimal bookingAmount
) {
    public ScoreHoldRequest(
            Long userId,
            Long bookingId,
            Integer points,
            Integer ttlSeconds,
            String eventId,
            String idempotencyKey) {
        this(userId, bookingId, points, ttlSeconds, eventId, idempotencyKey, null);
    }
}
