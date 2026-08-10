package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import java.time.Instant;
import java.util.List;

public record MovieLaunchReadinessResponse(
        String moviePublicId,
        MovieStatus movieStatus,
        boolean contentReady,
        boolean publishable,
        boolean bookable,
        long futureShowtimeCount,
        long draftShowtimeCount,
        long openShowtimeCount,
        long openableDraftShowtimeCount,
        long blockedDraftShowtimeCount,
        List<LaunchIssue> blockers,
        List<LaunchIssue> warnings,
        List<ShowtimeReadiness> showtimes) {

    public record LaunchIssue(String code, String message, String action, String showtimePublicId) {}

    public record ShowtimeReadiness(
            String showtimePublicId,
            String status,
            Instant startTime,
            boolean openable,
            List<LaunchIssue> blockers) {}
}
