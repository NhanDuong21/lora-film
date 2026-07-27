package com.project.scoreservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreReleaseRequest(
    @NotNull(message = "Booking ID is required")
    Long bookingId,

    String holdCode,

    String reason,

    String eventId,

    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {}
