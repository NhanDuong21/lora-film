package com.project.scoreservice.dto;

public record ScoreRevokeRequest(
    Long userId,
    Long bookingId,
    Long originalEarnHistoryId,
    Integer pointsToRevoke,
    String reason,
    String eventId,
    String idempotencyKey
) {}
