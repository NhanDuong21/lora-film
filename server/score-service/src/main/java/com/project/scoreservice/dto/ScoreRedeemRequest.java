package com.project.scoreservice.dto;

public record ScoreRedeemRequest(
    Long userId,
    Long bookingId,
    Integer points,
    String eventId,
    String idempotencyKey
) {}
