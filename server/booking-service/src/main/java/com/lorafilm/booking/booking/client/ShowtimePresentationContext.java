package com.lorafilm.booking.booking.client;

public record ShowtimePresentationContext(
        String movieTitle,
        String moviePosterUrl,
        Integer durationMinutes,
        String ageRating) {
}
