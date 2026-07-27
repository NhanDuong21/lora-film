package com.project.scoreservice.dto;

public record ScoreRefundRequest(
    Long userId,
    Long bookingId,
    Integer pointsToRefund,
    String reason,
    String eventId,
    String idempotencyKey
) {}
