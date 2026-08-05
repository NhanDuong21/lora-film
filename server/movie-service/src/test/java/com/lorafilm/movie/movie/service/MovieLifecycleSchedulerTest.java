package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MovieLifecycleSchedulerTest {
    @Test
    void startsOnlyOperationalReleasedMoviesAndEndsExpiredMovies() {
        MovieRepository repository = mock(MovieRepository.class);
        MovieApprovalPolicy approval = mock(MovieApprovalPolicy.class);
        MovieStatusHistoryService history = mock(MovieStatusHistoryService.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-04T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate today = LocalDate.of(2026, 8, 4);
        Movie ready = movie(1L, MovieStatus.UPCOMING);
        Movie blocked = movie(2L, MovieStatus.UPCOMING);
        Movie expired = movie(3L, MovieStatus.NOW_SHOWING);
        when(repository.findReleasedByStatusForUpdate(MovieStatus.UPCOMING, today))
                .thenReturn(List.of(ready, blocked));
        when(approval.hasOperationalShowtime(1L)).thenReturn(true);
        when(approval.hasOperationalShowtime(2L)).thenReturn(false);
        when(repository.findEndedByStatusForUpdate(MovieStatus.NOW_SHOWING, today))
                .thenReturn(List.of(expired));
        when(repository.save(any(Movie.class))).thenAnswer(call -> call.getArgument(0));

        new MovieLifecycleScheduler(repository, approval, history, clock).reconcile();

        assertEquals(MovieStatus.NOW_SHOWING, ready.getStatus());
        assertEquals(MovieStatus.UPCOMING, blocked.getStatus());
        assertEquals(MovieStatus.ENDED, expired.getStatus());
        verify(history).record(ready, MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING,
                "Automatically started on the local release date", null);
        verify(history).record(expired, MovieStatus.NOW_SHOWING, MovieStatus.ENDED,
                "Automatically ended after the local release window", null);
    }

    private Movie movie(Long id, MovieStatus status) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setStatus(status);
        return movie;
    }
}
