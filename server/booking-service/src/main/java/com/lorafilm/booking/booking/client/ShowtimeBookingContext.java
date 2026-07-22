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
        String cinemaName,
        String auditoriumName,
        List<SeatContext> seats) {

    public record SeatContext(Long seatId, String seatLabel, String seatType, BigDecimal price) {
    }
}
