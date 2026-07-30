package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RedeemPreviewRequest(
    @NotNull(message = "Booking ID is required")
    Long bookingId,

    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be positive")
    Integer points
) {}
