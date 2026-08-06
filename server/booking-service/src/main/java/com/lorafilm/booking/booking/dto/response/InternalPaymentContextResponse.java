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
        String lockedPaymentProvider,
        AnalyticsSnapshot analyticsSnapshot
) {
    public InternalPaymentContextResponse(
            Long bookingId,
            String bookingPublicId,
            Long accountId,
            String bookingStatus,
            Boolean payable,
            BigDecimal amount,
            String currency,
            Instant amountLockedAt,
            Instant expiresAt,
            AnalyticsSnapshot analyticsSnapshot) {
        this(bookingId, bookingPublicId, accountId, bookingStatus, payable,
                amount, currency, amountLockedAt, expiresAt, null, analyticsSnapshot);
    }
    public record AnalyticsSnapshot(
            Long movieId,
            String moviePublicId,
            String movieTitle,
            String showtimePublicId,
            String cinemaPublicId,
            Integer ticketCount,
            BigDecimal ticketAmount,
            BigDecimal foodAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String currency,
            Instant showtimeStartsAt,
            String auditoriumPublicId,
            Integer auditoriumCapacity,
            String format) {
        /**
         * Compatibility constructor for existing Booking controller/service tests and
         * one-release numeric Payment adapters. New runtime responses use the complete
         * immutable analytics snapshot above.
         */
        public AnalyticsSnapshot(Long movieId, String movieTitle, Integer ticketCount) {
            this(
                    movieId,
                    null,
                    movieTitle,
                    null,
                    null,
                    ticketCount,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
    }
}
