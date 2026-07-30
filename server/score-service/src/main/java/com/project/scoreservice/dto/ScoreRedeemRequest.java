package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreRedeemRequest(
    @NotNull(message = "User ID is required")
    Long userId,
    @NotNull(message = "Booking ID is required")
    Long bookingId,
    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be positive")
    Integer points,
    String eventId,
    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {}
