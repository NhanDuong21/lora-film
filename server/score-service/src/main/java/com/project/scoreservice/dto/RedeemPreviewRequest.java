package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RedeemPreviewRequest(
    Long bookingId,

    String bookingPublicId,

    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be positive")
    Integer points
) {
    /**
     * Keeps the original numeric-booking contract source compatible while new
     * checkout clients use the public Booking UUID.
     */
    public RedeemPreviewRequest(Long bookingId, Integer points) {
        this(bookingId, null, points);
    }

    public String bookingReference() {
        if (bookingPublicId != null && !bookingPublicId.isBlank()) {
            return bookingPublicId.trim();
        }
        return bookingId == null ? null : bookingId.toString();
    }
}
