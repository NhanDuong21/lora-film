package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ShowtimeLifecycleSchedulerTest {
    @Test
    void closesStartedShowtimesAndFinishesEndedShowtimesWithHistory() {
        ShowtimeRepository repository = mock(ShowtimeRepository.class);
        ShowtimeStatusHistoryService history = mock(ShowtimeStatusHistoryService.class);
        Instant now = Instant.parse("2026-08-04T03:00:00Z");
        Showtime open = showtime(1L, ShowtimeStatus.OPEN_FOR_BOOKING);
        Showtime closed = showtime(2L, ShowtimeStatus.CLOSED);
        when(repository.findByStatusAndStartTimeLessThanEqualAndDeletedAtIsNullOrderByIdAsc(
                ShowtimeStatus.OPEN_FOR_BOOKING, now)).thenReturn(List.of(open));
        when(repository.findByStatusAndEndTimeLessThanEqualAndDeletedAtIsNullOrderByIdAsc(
                ShowtimeStatus.CLOSED, now)).thenReturn(List.of(closed));
        when(repository.save(any(Showtime.class))).thenAnswer(call -> call.getArgument(0));

        new ShowtimeLifecycleScheduler(repository, history, Clock.fixed(now, ZoneOffset.UTC))
                .reconcile();

        assertEquals(ShowtimeStatus.CLOSED, open.getStatus());
        assertEquals(now, open.getBookingCloseTime());
        assertEquals(ShowtimeStatus.FINISHED, closed.getStatus());
        verify(history).recordTransitionHistory(
                open, ShowtimeStatus.OPEN_FOR_BOOKING, ShowtimeStatus.CLOSED,
                "Automatically closed when the showtime started", null, now);
        verify(history).recordTransitionHistory(
                closed, ShowtimeStatus.CLOSED, ShowtimeStatus.FINISHED,
                "Automatically finished when the showtime ended", null, now);
    }

    private Showtime showtime(Long id, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setId(id);
        showtime.setStatus(status);
        return showtime;
    }
}
