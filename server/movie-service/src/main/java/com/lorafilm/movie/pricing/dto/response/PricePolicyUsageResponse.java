package com.lorafilm.movie.pricing.dto.response;

import java.time.Instant;
import java.util.List;

public record PricePolicyUsageResponse(
        long snapshotShowtimeCount,
        long futureDraftShowtimeCount,
        List<AffectedShowtime> affectedFutureShowtimes,
        int affectedPage,
        int affectedPageSize,
        int affectedTotalPages,
        boolean affectedLast
) {
    public record AffectedShowtime(
            String showtimeId,
            String auditoriumId,
            String auditoriumName,
            Instant startTime
    ) {
    }
}
