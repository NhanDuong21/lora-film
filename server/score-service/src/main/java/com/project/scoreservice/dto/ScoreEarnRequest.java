package com.project.scoreservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ScoreEarnRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Booking ID is required")
    Long bookingId,

    @NotNull(message = "Eligible amount is required")
    @DecimalMin(value = "0.0001", message = "Eligible amount must be positive")
    BigDecimal eligibleAmount,

    @NotBlank(message = "Event ID is required")
    String eventId,

    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey
) {}
