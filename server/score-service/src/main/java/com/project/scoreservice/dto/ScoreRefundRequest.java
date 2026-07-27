package com.project.scoreservice.dto;

public record ScoreRefundRequest(
    Long userId,
    Long bookingId,
    Integer pointsToRefund,
    String originalRedeemEventId,
    String eventId,
    String idempotencyKey,
    String reason
) {
    public ScoreRefundRequest(Long userId, Long bookingId, Integer pointsToRefund, String reason, String eventId, String idempotencyKey) {
        this(userId, bookingId, pointsToRefund, null, eventId, idempotencyKey, reason);
    }
}

