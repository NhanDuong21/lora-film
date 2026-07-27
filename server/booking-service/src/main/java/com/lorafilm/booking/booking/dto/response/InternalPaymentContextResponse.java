package com.lorafilm.booking.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record InternalPaymentContextResponse(
        Long bookingId,
        String bookingPublicId,
        Long accountId,
        String bookingStatus,
        Boolean payable,
        BigDecimal amount,
        String currency,
        Instant amountLockedAt,
        Instant expiresAt,
        AnalyticsSnapshot analyticsSnapshot
) {
    public record AnalyticsSnapshot(Long movieId, String movieTitle, Integer ticketCount) {
    }
}
