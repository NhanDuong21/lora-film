package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;

import java.time.Instant;
import java.util.List;

public record MaintenanceImpactResponse(
        String auditoriumPublicId,
        String auditoriumName,
        Instant startTime,
        Instant endTime,
        int affectedShowtimeCount,
        int openForBookingCount,
        int draftShowtimeCount,
        int occupiedSeatCount,
        boolean bookingDataComplete,
        List<AffectedShowtime> showtimes
) {
    public record AffectedShowtime(
            String showtimePublicId,
            String movieTitle,
            Instant startTime,
            Instant endTime,
            ShowtimeStatus status,
            int occupiedSeatCount,
            boolean bookingDataAvailable
    ) {
    }
}
