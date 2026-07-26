package com.project.scoreservice.dto;

public record ScoreCommitResponse(
    String holdCode,
    Long userId,
    Long bookingId,
    int pointsCommitted,
    int balanceBefore,
    int balanceAfter,
    int heldBefore,
    int heldAfter,
    String status,
    boolean idempotent
) {}
