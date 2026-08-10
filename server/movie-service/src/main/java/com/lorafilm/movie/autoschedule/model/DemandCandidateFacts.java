package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.movie.domain.enums.MovieFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record DemandCandidateFacts(
        String moviePublicId,
        MovieFormat format,
        int auditoriumCapacity,
        Instant startTime,
        LocalDate serviceDate,
        LocalDate releaseDate,
        ZoneId cinemaZone,
        BigDecimal ticketPrice,
        int existingSameMovieShowtimes) {
}
