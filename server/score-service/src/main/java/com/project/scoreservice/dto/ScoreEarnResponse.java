package com.project.scoreservice.dto;

public record ScoreEarnResponse(
    int pointChange,
    int balanceBefore,
    int balanceAfter,
    int accumulatedBefore,
    int accumulatedAfter,
    String previousTier,
    String currentTier,
    boolean tierChanged,
    boolean idempotent
) {}
