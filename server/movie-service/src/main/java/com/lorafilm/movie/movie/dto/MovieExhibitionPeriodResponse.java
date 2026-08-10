package com.lorafilm.movie.movie.dto;

import java.time.Instant;
import java.time.LocalDate;

public record MovieExhibitionPeriodResponse(
        String publicId,
        LocalDate startDate,
        LocalDate endDate,
        String periodState,
        String note,
        Instant createdAt) {
}
