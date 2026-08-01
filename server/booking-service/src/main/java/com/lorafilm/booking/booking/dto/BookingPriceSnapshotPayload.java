package com.lorafilm.booking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingPriceSnapshotPayload(
        Long showtimeId,
        String showtimePublicId,
        Instant capturedAt,
        String currency,
        Long movieId,
        String moviePublicId,
        String movieTitle,
        String cinemaPublicId,
        String format,
        String roomType,
        String channel,
        BigDecimal authoritativeTicketTotal,
        List<SeatPriceLine> seats
) {
    /** Compatibility constructor for snapshots without promotion context dimensions. */
    public BookingPriceSnapshotPayload(
            Long showtimeId,
            String showtimePublicId,
            Instant capturedAt,
            String currency,
            Long movieId,
            String moviePublicId,
            String movieTitle,
            String cinemaPublicId,
            BigDecimal authoritativeTicketTotal,
            List<SeatPriceLine> seats) {
        this(showtimeId, showtimePublicId, capturedAt, currency, movieId,
                moviePublicId, movieTitle, cinemaPublicId, null, null, "WEB",
                authoritativeTicketTotal, seats);
    }

    /** Compatibility constructor for snapshots without public Movie identities. */
    public BookingPriceSnapshotPayload(
            Long showtimeId,
            String showtimePublicId,
            Instant capturedAt,
            String currency,
            Long movieId,
            String movieTitle,
            BigDecimal authoritativeTicketTotal,
            List<SeatPriceLine> seats) {
        this(showtimeId, showtimePublicId, capturedAt, currency, movieId, null,
                movieTitle, null, null, null, "WEB", authoritativeTicketTotal, seats);
    }

    public record SeatPriceLine(
            Long seatId,
            String seatLabel,
            String seatType,
            BigDecimal unitPrice,
            String seatPublicId
    ) {
        /** Compatibility constructor for historical snapshot callers. */
        public SeatPriceLine(Long seatId, String seatLabel, String seatType, BigDecimal unitPrice) {
            this(seatId, seatLabel, seatType, unitPrice, null);
        }
    }
}
