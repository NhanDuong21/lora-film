package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovieLifecyclePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 22);
    private final MovieLifecyclePolicy policy = new MovieLifecyclePolicy(
            Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void enforcesTheCanonicalTransitionGraph() {
        Movie movie = movie(MovieStatus.DRAFT, TODAY.plusDays(1), null);
        assertDoesNotThrow(() -> policy.validateTransition(movie, MovieStatus.UPCOMING));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policy.validateTransition(movie, MovieStatus.NOW_SHOWING));
        assertEquals(ErrorCode.INVALID_MOVIE_STATUS_TRANSITION, exception.getErrorCode());

        Movie releasedDraft = movie(MovieStatus.DRAFT, TODAY, null);
        assertThrows(BusinessException.class,
                () -> policy.validateTransition(releasedDraft, MovieStatus.NOW_SHOWING));
    }

    @Test
    void upcomingApprovalRequiresAFutureLocalReleaseDate() {
        Movie movie = movie(MovieStatus.DRAFT, TODAY, null);

        assertFalse(policy.getTransitionViolations(movie, MovieStatus.UPCOMING).isEmpty());
        assertThrows(BusinessException.class, () -> policy.validateTransition(movie, MovieStatus.UPCOMING));
    }

    @Test
    void nowShowingAndEndedUseTheirTargetDateRules() {
        Movie upcoming = movie(MovieStatus.UPCOMING, TODAY, TODAY.plusDays(1));
        assertDoesNotThrow(() -> policy.validateTransition(upcoming, MovieStatus.NOW_SHOWING));

        Movie showing = movie(MovieStatus.NOW_SHOWING, TODAY.minusDays(5), TODAY);
        assertDoesNotThrow(() -> policy.validateTransition(showing, MovieStatus.ENDED));

        showing.setEndDate(TODAY.plusDays(1));
        assertThrows(BusinessException.class, () -> policy.validateTransition(showing, MovieStatus.ENDED));
    }

    @Test
    void inactiveCanOnlyReturnToDraft() {
        Movie movie = movie(MovieStatus.INACTIVE, TODAY, null);
        assertDoesNotThrow(() -> policy.validateTransition(movie, MovieStatus.DRAFT));
        assertThrows(BusinessException.class, () -> policy.validateTransition(movie, MovieStatus.UPCOMING));
    }

    @Test
    void deactivationIsNeverBlockedByReleaseWindowData() {
        Movie movie = movie(MovieStatus.DRAFT, TODAY.plusDays(10), TODAY.minusDays(10));

        assertDoesNotThrow(() -> policy.validateTransition(movie, MovieStatus.INACTIVE));
    }

    private Movie movie(MovieStatus status, LocalDate releaseDate, LocalDate endDate) {
        Movie movie = new Movie();
        movie.setStatus(status);
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(endDate);
        return movie;
    }
}
