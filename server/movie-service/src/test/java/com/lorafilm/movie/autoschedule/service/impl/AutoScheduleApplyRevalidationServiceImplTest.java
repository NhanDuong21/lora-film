package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AutoScheduleApplyRevalidationServiceImplTest {

    @Test
    void validateAll_usesSharedValidatorOnceAndPreservesSpecificError() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);

        ShowtimeSchedulePreviewItem item = validItem();

        doThrow(new BusinessException(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE))
                .when(validationService).validateScheduling(org.mockito.ArgumentMatchers.any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateAll(null, List.of(item), Instant.now()));

        assertEquals(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE, ex.getErrorCode());
        verify(validationService).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_rejectsCurrentDurationChangeBeforeCreatingAnything() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem item = validItem();
        item.getMovie().setDurationMinutes(91);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateAll(null, List.of(item), Instant.now()));

        assertEquals(ErrorCode.AUTO_SCHEDULE_CANDIDATE_CHANGED, ex.getErrorCode());
        verify(validationService, never()).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_rejectsCurrentCleaningBufferChangeBeforeCreatingAnything() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem item = validItem();
        item.getAuditorium().setCleaningBufferMinutes(30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateAll(null, List.of(item), Instant.now()));

        assertEquals(ErrorCode.AUTO_SCHEDULE_CANDIDATE_CHANGED, ex.getErrorCode());
        verify(validationService, never()).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_rejectsOverlappingSelectedItemsBeforeSharedValidation() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem first = validItem();
        ShowtimeSchedulePreviewItem overlapping = validItem();
        overlapping.setPublicId("overlapping-item");
        overlapping.setStartTime(Instant.parse("2026-07-22T11:30:00Z"));
        overlapping.setEndTime(Instant.parse("2026-07-22T13:00:00Z"));
        overlapping.setOccupancyEndTime(Instant.parse("2026-07-22T13:15:00Z"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateAll(null, List.of(first, overlapping), Instant.now()));

        assertEquals(ErrorCode.AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP, ex.getErrorCode());
        verify(validationService, never()).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    private ShowtimeSchedulePreviewItem validItem() {
        Movie movie = new Movie();
        movie.setDurationMinutes(90);
        Cinema cinema = new Cinema();
        Auditorium auditorium = new Auditorium();
        auditorium.setId(1L);
        auditorium.setCleaningBufferMinutes(15);
        MovieVersion version = new MovieVersion();

        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        item.setPublicId("item");
        item.setMovie(movie);
        item.setMovieVersion(version);
        item.setCinema(cinema);
        item.setAuditorium(auditorium);
        item.setStartTime(Instant.parse("2026-07-22T10:00:00Z"));
        item.setEndTime(Instant.parse("2026-07-22T11:30:00Z"));
        item.setOccupancyEndTime(Instant.parse("2026-07-22T11:45:00Z"));
        return item;
    }
}
