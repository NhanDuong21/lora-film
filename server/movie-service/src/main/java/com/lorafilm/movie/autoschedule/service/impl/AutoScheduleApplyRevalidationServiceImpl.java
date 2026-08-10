package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.service.AutoScheduleApplyRevalidationService;
import com.lorafilm.movie.autoschedule.validation.OccupancyInterval;
import com.lorafilm.movie.autoschedule.validation.OccupancyOverlapValidator;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationContext;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AutoScheduleApplyRevalidationServiceImpl implements AutoScheduleApplyRevalidationService {

    private final ShowtimeValidationService showtimeValidationService;

    public AutoScheduleApplyRevalidationServiceImpl(ShowtimeValidationService showtimeValidationService) {
        this.showtimeValidationService = showtimeValidationService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void validateAll(ShowtimeSchedulePreview preview, List<ShowtimeSchedulePreviewItem> selectedItems, Instant now) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        validateTimezoneSnapshot(preview);

        // Check candidate-candidate overlap first
        validateCandidateCandidateOverlaps(selectedItems);

        for (ShowtimeSchedulePreviewItem item : selectedItems) {
            revalidateItem(item);
        }
    }

    private void validateTimezoneSnapshot(ShowtimeSchedulePreview preview) {
        if (preview == null || preview.getCinema() == null
                || preview.getTimezoneSnapshot() == null
                || !preview.getTimezoneSnapshot().equals(preview.getCinema().getTimezone())) {
            throw new BusinessException(
                    ErrorCode.AUTO_SCHEDULE_PREVIEW_STALE,
                    "Cinema timezone changed after preview generation");
        }
        try {
            ZoneId.of(preview.getTimezoneSnapshot());
        } catch (RuntimeException invalidZone) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE);
        }
    }

    private void validateCandidateCandidateOverlaps(List<ShowtimeSchedulePreviewItem> items) {
        List<OccupancyInterval> intervals = items.stream()
                .map(this::toCanonicalInterval)
                .toList();

        OccupancyOverlapValidator.findConflict(intervals).ifPresent(conflict -> {
            String auditoriumName = items.stream()
                    .filter(item -> item.getAuditorium().getId().equals(conflict.auditoriumId()))
                    .map(item -> String.valueOf(item.getAuditorium().getName()))
                    .findFirst()
                    .orElse("null");
            throw new BusinessException(
                    ErrorCode.AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP,
                    "Selected candidates overlap in auditorium " + auditoriumName);
        });
    }

    private OccupancyInterval toCanonicalInterval(ShowtimeSchedulePreviewItem item) {
        if (item == null
                || item.getAuditorium() == null
                || item.getAuditorium().getId() == null
                || item.getPublicId() == null
                || item.getPublicId().isBlank()
                || !hasValidInterval(item)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT);
        }
        return new OccupancyInterval(
                item.getAuditorium().getId(),
                item.getStartTime(),
                item.getOccupancyEndTime(),
                item.getPublicId());
    }

    private boolean hasValidInterval(ShowtimeSchedulePreviewItem item) {
        return item.getStartTime() != null
                && item.getEndTime() != null
                && item.getOccupancyEndTime() != null
                && item.getStartTime().isBefore(item.getEndTime())
                && !item.getEndTime().isAfter(item.getOccupancyEndTime());
    }

    private void revalidateItem(ShowtimeSchedulePreviewItem item) {
        Movie movie = item.getMovie();
        Auditorium auditorium = item.getAuditorium();

        if (movie == null || movie.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }
        if (auditorium == null || auditorium.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND);
        }

        if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION, "Invalid movie duration");
        }

        // Integrity check: duration hasn't changed
        Instant expectedEndTime = item.getStartTime().plus(movie.getDurationMinutes(), ChronoUnit.MINUTES);
        if (!expectedEndTime.equals(item.getEndTime())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_CANDIDATE_CHANGED, "Movie duration has changed since preview generation");
        }

        // Integrity check: cleaning buffer hasn't changed
        Instant expectedOccupancyEnd = item.getEndTime().plus(auditorium.getCleaningBufferMinutes(), ChronoUnit.MINUTES);
        if (!expectedOccupancyEnd.equals(item.getOccupancyEndTime())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_CANDIDATE_CHANGED, "Auditorium cleaning buffer has changed since preview generation");
        }

        // Re-use manual validation for things like active status, closure, maintenance, etc.
        ShowtimeValidationContext context = ShowtimeValidationContext.builder()
                .movie(item.getMovie())
                .movieVersion(item.getMovieVersion())
                .cinema(item.getCinema())
                .auditorium(item.getAuditorium())
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .excludeShowtimeId(null)
                .build();

        showtimeValidationService.validateScheduling(context);
    }
}
