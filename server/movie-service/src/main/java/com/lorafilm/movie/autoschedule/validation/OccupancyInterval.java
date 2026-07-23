package com.lorafilm.movie.autoschedule.validation;

import java.time.Instant;

/**
 * Repository-independent occupancy interval used by authoritative backend overlap checks.
 */
public record OccupancyInterval(
        Long auditoriumId,
        Instant startTime,
        Instant occupancyEndTime,
        String itemPublicId) {
}
