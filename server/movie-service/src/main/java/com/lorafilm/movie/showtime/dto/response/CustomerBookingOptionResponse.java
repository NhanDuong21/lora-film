package com.lorafilm.movie.showtime.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CustomerBookingOptionResponse(
        String showtimePublicId,
        LocalDate serviceDate,
        Instant startTime,
        Instant endTime,
        LocalDateTime localStartTime,
        LocalDateTime localEndTime,
        String cinemaPublicId,
        String cinemaSlug,
        String cinemaName,
        String cinemaAddress,
        String cinemaCity,
        String cinemaTimezone,
        String auditoriumPublicId,
        String auditoriumName,
        String screenType,
        String soundType,
        String movieVersionPublicId,
        String versionName,
        String format,
        String audioLanguage,
        String subtitleLanguage,
        String status,
        BigDecimal priceFrom,
        String currency) {
}
