package com.project.scoreservice.dto;

public record ScoreRevokeRequest(
    Long userId,
    Long bookingId,
    Long originalEarnHistoryId,
    Integer pointsToRevoke,
    String originalEarnEventId,
    String eventId,
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
