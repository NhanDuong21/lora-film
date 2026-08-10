package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ShowtimeSchedulePreviewHistoryRow(
        String previewPublicId,
        Long version,
        String cinemaPublicId,
        String cinemaName,
        String timezoneSnapshot,
        LocalDate scheduleFrom,
        LocalDate scheduleTo,
        String strategyVersion,
        SchedulePreviewApplyMode applyMode,
        SchedulePreviewStatus persistedStatus,
        Integer totalCandidateCount,
        Integer validCandidateCount,
        Integer rejectedCandidateCount,
        Integer selectedCandidateCount,
        Instant createdAt,
        Instant expiresAt,
        Instant appliedAt
) {
}
