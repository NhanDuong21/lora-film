package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieApprovalPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 4);

    @Mock private ShowtimeRepository showtimeRepository;

    private MovieApprovalPolicy approvalPolicy;

    @BeforeEach
    void setUp() {
        MovieLifecyclePolicy lifecyclePolicy = new MovieLifecyclePolicy(
                Clock.fixed(Instant.parse("2026-08-04T02:00:00Z"), ZoneOffset.UTC));
        approvalPolicy = new MovieApprovalPolicy(lifecyclePolicy, showtimeRepository);
    }

    @Test
    void futureDraftTargetsUpcomingWithoutRequiringAShowtime() {
        Movie movie = movie(TODAY.plusDays(1));

        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);

        assertEquals(MovieStatus.UPCOMING, decision.targetStatus());
        assertTrue(decision.blockers().isEmpty());
        verifyNoInteractions(showtimeRepository);
    }

    @Test
    void releasedDraftTargetsNowShowingWhenAnOperationalShowtimeExists() {
        Movie movie = movie(TODAY);
        when(showtimeRepository.existsByMovieIdAndStatusInAndEndTimeAfterAndDeletedAtIsNull(
                eq(1L), any(), any())).thenReturn(true);

        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);

        assertEquals(MovieStatus.NOW_SHOWING, decision.targetStatus());
        assertTrue(decision.blockers().isEmpty());
    }

    @Test
    void releasedDraftRemainsBlockedWithoutAnOperationalShowtime() {
        Movie movie = movie(TODAY.minusDays(10));
        when(showtimeRepository.existsByMovieIdAndStatusInAndEndTimeAfterAndDeletedAtIsNull(
                eq(1L), any(), any())).thenReturn(false);

        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);

        assertEquals(MovieStatus.NOW_SHOWING, decision.targetStatus());
        assertFalse(decision.blockers().isEmpty());
        assertTrue(decision.blockers().getFirst().contains("showtime"));
    }

    private Movie movie(LocalDate releaseDate) {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setStatus(MovieStatus.DRAFT);
        movie.setReleaseDate(releaseDate);
        return movie;
    }
}
