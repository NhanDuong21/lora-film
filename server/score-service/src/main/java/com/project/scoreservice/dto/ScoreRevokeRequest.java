package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreRevokeRequest(
    @NotNull(message = "User ID is required")
    Long userId,
    @NotNull(message = "Booking ID is required")
    Long bookingId,
    Long originalEarnHistoryId,
    @NotNull(message = "Points to revoke is required")
    @Min(value = 1, message = "Points to revoke must be positive")
    Integer pointsToRevoke,
    String originalEarnEventId,
    String eventId,
    @NotBlank(message = "Idempotency key is required")
    String idempotencyKey,
    String reason
) {
    public ScoreRevokeRequest(Long userId, Long bookingId, Long originalEarnHistoryId, Integer pointsToRevoke, String reason, String eventId, String idempotencyKey) {
        this(userId, bookingId, originalEarnHistoryId, pointsToRevoke, null, eventId, idempotencyKey, reason);
    }

    public ScoreRevokeRequest(Long userId, Long bookingId, Integer pointsToRevoke, String originalEarnEventId, String eventId, String idempotencyKey, String reason) {
        this(userId, bookingId, null, pointsToRevoke, originalEarnEventId, eventId, idempotencyKey, reason);
    }
}
