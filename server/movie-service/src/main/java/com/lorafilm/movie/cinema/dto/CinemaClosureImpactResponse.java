package com.lorafilm.movie.cinema.dto;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;

import java.time.Instant;
import java.util.List;

public record CinemaClosureImpactResponse(
        String cinemaPublicId,
        String cinemaName,
        Instant startTime,
        Instant endTime,
        int affectedShowtimeCount,
        int openForBookingCount,
        int draftShowtimeCount,
        int occupiedSeatCount,
        boolean bookingDataComplete,
        List<AffectedShowtime> showtimes) {

    public record AffectedShowtime(
            String showtimePublicId,
            String auditoriumName,
            String movieTitle,
            Instant startTime,
            Instant endTime,
            ShowtimeStatus status,
            int occupiedSeatCount,
            boolean bookingDataAvailable) {
    }
}
