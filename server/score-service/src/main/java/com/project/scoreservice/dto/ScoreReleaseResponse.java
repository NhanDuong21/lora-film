package com.project.scoreservice.dto;

public record ScoreReleaseResponse(
    String holdCode,
    Long userId,
    Long bookingId,
    int pointsReleased,
    int availablePointsAfter,
    int heldBefore,
    int heldAfter,
    String status,
    boolean idempotent
) {}
