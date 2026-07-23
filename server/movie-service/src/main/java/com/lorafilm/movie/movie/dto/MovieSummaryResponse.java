package com.lorafilm.movie.movie.dto;

public record MovieSummaryResponse(
        long total,
        long draft,
        long upcoming,
        long nowShowing,
        long ended,
        long inactive,
        long ready,
        long warning,
        long blocked,
        long missingPrimaryPoster,
        long missingActiveVersion,
        long withoutShowtime) {
}
