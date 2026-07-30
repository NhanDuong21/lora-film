package com.project.scoreservice.dto;

public record ScoreRefundResponse(
    Long userId,
    Long bookingId,
    int refundedPoints,
    int currentPoints,
    int accumulatedPoints,
    Long originalHistoryId,
    boolean idempotent
) {}
