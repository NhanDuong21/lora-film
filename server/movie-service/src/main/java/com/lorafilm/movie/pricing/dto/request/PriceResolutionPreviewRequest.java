package com.lorafilm.movie.pricing.dto.request;

import java.time.Instant;

public record PriceResolutionPreviewRequest(
        String showtimeId,
        String cinemaId,
        String auditoriumId,
        Instant startTime
) {
}
