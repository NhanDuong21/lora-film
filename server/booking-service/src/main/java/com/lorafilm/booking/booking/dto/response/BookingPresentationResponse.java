package com.lorafilm.booking.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingPresentationResponse(
        String movieTitle,
        String moviePosterUrl,
        Instant showtimeStart,
        Instant showtimeEnd,
        String cinemaName,
        String auditoriumName,
        List<SeatLine> seats) {

    public record SeatLine(
            String seatPublicId,
            String label,
            String type,
            BigDecimal price) {
    }
}
