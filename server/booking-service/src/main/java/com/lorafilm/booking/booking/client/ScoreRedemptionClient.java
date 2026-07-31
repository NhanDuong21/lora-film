package com.lorafilm.booking.booking.client;

import java.math.BigDecimal;

public interface ScoreRedemptionClient {

    ScoreHoldResult hold(
            Long userId,
            Long bookingId,
            int points,
            int ttlSeconds,
            BigDecimal bookingAmount,
            String eventId,
            String idempotencyKey);

    void commit(Long bookingId, String holdCode, String eventId, String idempotencyKey);

    void release(
            Long bookingId,
            String holdCode,
            String reason,
            String eventId,
            String idempotencyKey);

    void refund(
            Long userId,
            Long bookingId,
            int points,
            String reason,
            String eventId,
            String idempotencyKey);

    record ScoreHoldResult(
            String holdCode,
            int pointsHeld,
            String status,
            BigDecimal discountAmount,
            BigDecimal valuePerPoint,
            boolean idempotent) {
    }
}
