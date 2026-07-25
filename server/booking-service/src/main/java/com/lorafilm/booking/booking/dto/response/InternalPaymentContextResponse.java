package com.lorafilm.booking.booking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InternalPaymentContextResponse(
        Long bookingId,
        Long accountId,
        String bookingStatus,
        Boolean payable,
        BigDecimal amount,
        String currency,
        LocalDateTime expiresAt,
        AnalyticsSnapshot analyticsSnapshot
) {
    public record AnalyticsSnapshot(Long movieId, String movieTitle, Integer ticketCount) {
    }
}
