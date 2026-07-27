package com.lorafilm.booking.booking.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShowtimeBookingContext(
        Long showtimeId,
        String showtimePublicId,
        Long movieId,
        Long cinemaId,
        Long auditoriumId,
        String status,
        Instant startsAt,
        Instant endsAt,
        Instant paymentExpiresAt,
        BigDecimal ticketAmount,
        BigDecimal serviceFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency,
        String movieTitle,
        String moviePosterUrl,
        String cinemaName,
        String auditoriumName,
        List<SeatContext> seats) {

    public record SeatContext(Long seatId, String seatPublicId, String seatLabel, String seatType,
                               BigDecimal price, String currency) {
        public SeatContext(Long seatId, String seatLabel, String seatType, BigDecimal price, String currency) {
            this(seatId, null, seatLabel, seatType, price, currency);
        }

        public SeatContext(Long seatId, String seatLabel, String seatType, BigDecimal price) {
            this(seatId, null, seatLabel, seatType, price, null);
        }
    }
}
