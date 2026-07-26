package com.project.scoreservice.dto;

import jakarta.validation.constraints.NotNull;

public record RedeemPreviewRequest(
    @NotNull(message = "Booking ID is required")
    Long bookingId,

    @NotNull(message = "Points is required")
    Integer points
) {}
