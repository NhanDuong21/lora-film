package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.service.AutoScheduleApplyRevalidationService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationContext;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutoScheduleApplyRevalidationServiceImpl implements AutoScheduleApplyRevalidationService {

    private final ShowtimeValidationService showtimeValidationService;
    private final ShowtimeRepository showtimeRepository;

    public AutoScheduleApplyRevalidationServiceImpl(ShowtimeValidationService showtimeValidationService,
                                                    ShowtimeRepository showtimeRepository) {
        this.showtimeValidationService = showtimeValidationService;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void validateAll(ShowtimeSchedulePreview preview, List<ShowtimeSchedulePreviewItem> selectedItems, Instant now) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        // Check candidate-candidate overlap first
        validateCandidateCandidateOverlaps(selectedItems);

        List<Long> auditoriumIds = selectedItems.stream()
                .map(item -> item.getAuditorium().getId())
                .distinct()
                .collect(Collectors.toList());

        for (ShowtimeSchedulePreviewItem item : selectedItems) {
            try {
                revalidateItem(item);
            } catch (BusinessException e) {
                // In ALL_OR_NOTHING, one failure rolls back the whole thing.
                // We map this to a specific apply failure code.
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_APPLY_REVALIDATION_FAILED, "Validation failed for item " + item.getPublicId());
            }
        }
    }

    private void validateCandidateCandidateOverlaps(List<ShowtimeSchedulePreviewItem> items) {
        // Group by auditorium
        var itemsByAuditorium = items.stream()
                .collect(Collectors.groupingBy(item -> item.getAuditorium().getId()));

        for (var entry : itemsByAuditorium.entrySet()) {
            List<ShowtimeSchedulePreviewItem> audItems = entry.getValue();
            audItems.sort(Comparator.comparing(ShowtimeSchedulePreviewItem::getStartTime));

            for (int i = 0; i < audItems.size() - 1; i++) {
                ShowtimeSchedulePreviewItem current = audItems.get(i);
                ShowtimeSchedulePreviewItem next = audItems.get(i + 1);

                if (next.getStartTime().isBefore(current.getOccupancyEndTime())) {
                    throw new BusinessException(ErrorCode.AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP, 
                            "Selected candidates overlap in auditorium " + current.getAuditorium().getName());
                }
            }
        }
    }

    private void revalidateItem(ShowtimeSchedulePreviewItem item) {
        Movie movie = item.getMovie();
        Auditorium auditorium = item.getAuditorium();

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

        // Explicitly check overlaps with existing real showtimes considering cleaning buffers
        // We use exact semantics matching manual flow, minus CANCELLED or soft-deleted items.
        Instant candidateStartMinusBuffer = item.getStartTime().minus(auditorium.getCleaningBufferMinutes(), ChronoUnit.MINUTES);
        List<Showtime> overlappingShowtimes = showtimeRepository.findBlockingOverlapsForScheduling(
                auditorium.getId(),
                candidateStartMinusBuffer,
                item.getOccupancyEndTime()
        );
        
        if (!overlappingShowtimes.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAP_CONFLICT, "Showtime overlaps with an existing schedule");
        }
    }
}
