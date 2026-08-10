package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import java.time.Instant;

public record MovieStatusHistoryResponse(
        MovieStatus previousStatus,
        MovieStatus newStatus,
        String reason,
        Instant changedAt,
        Long changedBy) {
}
