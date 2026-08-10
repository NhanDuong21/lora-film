package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
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
import static org.mockito.Mockito.times;

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
                () -> service.validateAll(previewFor(item), List.of(item), Instant.now()));

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
                () -> service.validateAll(previewFor(item), List.of(item), Instant.now()));

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
                () -> service.validateAll(previewFor(item), List.of(item), Instant.now()));

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
                () -> service.validateAll(previewFor(first), List.of(first, overlapping), Instant.now()));

        assertEquals(ErrorCode.AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP, ex.getErrorCode());
        verify(validationService, never()).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_rejectsNestedIntervalsUsingMaximumPriorOccupancyEnd() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem outer = item(
                "outer", 1L, "10:00:00", "13:45:00", "14:00:00", 225, 15);
        ShowtimeSchedulePreviewItem nested = item(
                "nested", 1L, "11:00:00", "11:45:00", "12:00:00", 45, 15);
        ShowtimeSchedulePreviewItem later = item(
                "later", 1L, "13:00:00", "14:45:00", "15:00:00", 105, 15);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateAll(previewFor(outer), List.of(outer, nested, later), Instant.now()));

        assertEquals(ErrorCode.AUTO_SCHEDULE_SELECTED_ITEMS_OVERLAP, ex.getErrorCode());
        verify(validationService, never()).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_acceptsExactOccupancyAdjacency() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem first = item(
                "first", 1L, "10:00:00", "11:00:00", "11:15:00", 60, 15);
        ShowtimeSchedulePreviewItem adjacent = item(
                "adjacent", 1L, "11:15:00", "12:15:00", "12:30:00", 60, 15);

        service.validateAll(previewFor(first), List.of(first, adjacent), Instant.now());

        verify(validationService, times(2)).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_acceptsSameTimesInDifferentAuditoriums() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem first = item(
                "first", 1L, "10:00:00", "11:00:00", "11:15:00", 60, 15);
        ShowtimeSchedulePreviewItem otherAuditorium = item(
                "other", 2L, "10:00:00", "11:00:00", "11:15:00", 60, 15);

        service.validateAll(previewFor(first), List.of(first, otherAuditorium), Instant.now());

        verify(validationService, times(2)).validateScheduling(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateAll_rejectsCleaningOnlyOverlap() {
        ShowtimeValidationService validationService = mock(ShowtimeValidationService.class);
        AutoScheduleApplyRevalidationServiceImpl service =
                new AutoScheduleApplyRevalidationServiceImpl(validationService);
        ShowtimeSchedulePreviewItem first = item(
                "first", 1L, "10:00:00", "11:00:00", "11:15:00", 60, 15);
        ShowtimeSchedulePreviewItem cleaningOverlap = item(
                "cleaning-overlap", 1L, "11:05:00", "12:05:00", "12:20:00", 60, 15);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateAll(previewFor(first), List.of(first, cleaningOverlap), Instant.now()));

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

    private ShowtimeSchedulePreview previewFor(ShowtimeSchedulePreviewItem item) {
        item.getCinema().setTimezone("UTC");
        ShowtimeSchedulePreview preview = mock(ShowtimeSchedulePreview.class);
        org.mockito.Mockito.when(preview.getCinema()).thenReturn(item.getCinema());
        org.mockito.Mockito.when(preview.getTimezoneSnapshot()).thenReturn("UTC");
        return preview;
    }

    private ShowtimeSchedulePreviewItem item(String publicId,
                                             Long auditoriumId,
                                             String start,
                                             String end,
                                             String occupancyEnd,
                                             int durationMinutes,
                                             int cleaningMinutes) {
        Movie movie = new Movie();
        movie.setDurationMinutes(durationMinutes);
        Cinema cinema = new Cinema();
        Auditorium auditorium = new Auditorium();
        auditorium.setId(auditoriumId);
        auditorium.setCleaningBufferMinutes(cleaningMinutes);

        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        item.setPublicId(publicId);
        item.setMovie(movie);
        item.setMovieVersion(new MovieVersion());
        item.setCinema(cinema);
        item.setAuditorium(auditorium);
        item.setStartTime(Instant.parse("2026-07-22T" + start + "Z"));
        item.setEndTime(Instant.parse("2026-07-22T" + end + "Z"));
        item.setOccupancyEndTime(Instant.parse("2026-07-22T" + occupancyEnd + "Z"));
        return item;
    }
}
