package com.project.scoreservice.dto;

public record ScoreRedeemResponse(
    Long userId,
    Long bookingId,
    int redeemedPoints,
    int redeemValue,
    int currentPoints,
    int accumulatedPoints,
    boolean idempotent
) {}
