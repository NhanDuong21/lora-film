package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieShowtimeEligibilityPolicyTest {

    private final MovieShowtimeEligibilityPolicy policy = new MovieShowtimeEligibilityPolicy();
    private Movie movie;
    private MovieVersion version;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId(1L);
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setDurationMinutes(1);
        movie.setReleaseDate(LocalDate.of(2026, 3, 1));
        movie.setEndDate(LocalDate.of(2026, 11, 1));

        version = new MovieVersion();
        version.setId(2L);
        version.setMovie(movie);
        version.setStatus(ActiveStatus.ACTIVE);
    }

    @Test
    void durationOneIsEligibleInHelperAndSchedulingAuthority() {
        var issues = policy.evaluateRange(
                movie, List.of(version), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));

        assertTrue(issues.isEmpty());
        assertDoesNotThrow(() -> policy.validateMovieAndVersion(movie, version));
    }

    @Test
    void durationZeroIsRejectedInHelperAndSchedulingAuthority() {
        movie.setDurationMinutes(0);

        var issues = policy.evaluateRange(movie, List.of(version), null, null);
        assertTrue(issues.stream().anyMatch(issue ->
                MovieShowtimeEligibilityPolicy.MOVIE_DURATION_INVALID.equals(issue.code())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> policy.validateMovieAndVersion(movie, version));
        assertEquals(ErrorCode.INVALID_MOVIE_DURATION, ex.getErrorCode());
    }

    @Test
    void draftMovieCannotHaveShowtimesScheduled() {
        movie.setStatus(MovieStatus.DRAFT);

        var issues = policy.evaluateRange(movie, List.of(version), null, null);
        assertTrue(issues.stream().anyMatch(issue ->
                MovieShowtimeEligibilityPolicy.MOVIE_STATUS_NOT_ELIGIBLE.equals(issue.code())));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> policy.validateMovieAndVersion(movie, version));

        assertEquals(ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING, ex.getErrorCode());
    }

    @Test
    void upcomingAndNowShowingMoviesCanHaveShowtimesScheduled() {
        movie.setStatus(MovieStatus.UPCOMING);
        assertDoesNotThrow(() -> policy.validateMovieAndVersion(movie, version));

        movie.setStatus(MovieStatus.NOW_SHOWING);
        assertDoesNotThrow(() -> policy.validateMovieAndVersion(movie, version));
    }

    @Test
    void draftMovieShowtimeCannotOpenForBookingBeforeApproval() {
        movie.setStatus(MovieStatus.DRAFT);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> policy.validateMovieCanOpenForBooking(movie));

        assertEquals(ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING, ex.getErrorCode());
    }

    @Test
    void approvedMovieShowtimeCanOpenForBooking() {
        movie.setStatus(MovieStatus.UPCOMING);
        assertDoesNotThrow(() -> policy.validateMovieCanOpenForBooking(movie));

        movie.setStatus(MovieStatus.NOW_SHOWING);
        assertDoesNotThrow(() -> policy.validateMovieCanOpenForBooking(movie));
    }

    @Test
    void inactiveVersionIsRejectedInHelperAndSchedulingAuthority() {
        version.setStatus(ActiveStatus.INACTIVE);

        var issues = policy.evaluateRange(movie, List.of(version), null, null);
        assertTrue(issues.stream().anyMatch(issue ->
                MovieShowtimeEligibilityPolicy.NO_ACTIVE_MOVIE_VERSION.equals(issue.code())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> policy.validateMovieAndVersion(movie, version));
        assertEquals(ErrorCode.MOVIE_VERSION_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void deletedVersionIsNotEligibleAndIsRejectedSpecifically() {
        version.setDeletedAt(java.time.Instant.parse("2026-07-01T00:00:00Z"));

        var issues = policy.evaluateRange(movie, List.of(version), null, null);
        assertTrue(issues.stream().anyMatch(issue ->
                MovieShowtimeEligibilityPolicy.NO_ACTIVE_MOVIE_VERSION.equals(issue.code())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> policy.validateMovieAndVersion(movie, version));
        assertEquals(ErrorCode.MOVIE_VERSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void releaseBoundariesUseSuppliedCinemaZoneAcrossDstZone() {
        ZoneId zone = ZoneId.of("America/New_York");
        LocalDate releaseDate = LocalDate.of(2026, 3, 8);
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(releaseDate);

        var releaseStart = releaseDate.atStartOfDay(zone).toInstant();
        var endExclusive = releaseDate.plusDays(1).atStartOfDay(zone).toInstant();

        assertDoesNotThrow(() -> policy.validateReleaseWindow(movie, releaseStart, zone));
        assertDoesNotThrow(() -> policy.validateReleaseWindow(movie, endExclusive.minusMillis(1), zone));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                assertThrows(BusinessException.class,
                        () -> policy.validateReleaseWindow(movie, releaseStart.minusMillis(1), zone)).getErrorCode());
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                assertThrows(BusinessException.class,
                        () -> policy.validateReleaseWindow(movie, endExclusive, zone)).getErrorCode());
    }

    @Test
    void releaseBoundariesUseHoChiMinhLocalMidnight() {
        assertReleaseBoundariesInZone(ZoneId.of("Asia/Ho_Chi_Minh"), LocalDate.of(2026, 7, 22));
    }

    @Test
    void releaseBoundariesUseUtcMidnight() {
        assertReleaseBoundariesInZone(ZoneId.of("UTC"), LocalDate.of(2026, 7, 22));
    }

    @Test
    void inclusiveDateRangeIntersectionMatchesReleaseWindow() {
        var finalDayIssues = policy.evaluateRange(
                movie, List.of(version), movie.getEndDate(), movie.getEndDate());
        var afterEndIssues = policy.evaluateRange(
                movie, List.of(version), movie.getEndDate().plusDays(1), movie.getEndDate().plusDays(2));

        assertFalse(finalDayIssues.stream().anyMatch(issue ->
                MovieShowtimeEligibilityPolicy.OUTSIDE_RELEASE_WINDOW.equals(issue.code())));
        assertTrue(afterEndIssues.stream().anyMatch(issue ->
                MovieShowtimeEligibilityPolicy.OUTSIDE_RELEASE_WINDOW.equals(issue.code())));
    }

    private void assertReleaseBoundariesInZone(ZoneId zone, LocalDate date) {
        movie.setReleaseDate(date);
        movie.setEndDate(date);

        var releaseStart = date.atStartOfDay(zone).toInstant();
        var endExclusive = date.plusDays(1).atStartOfDay(zone).toInstant();

        assertDoesNotThrow(() -> policy.validateReleaseWindow(movie, releaseStart, zone));
        assertDoesNotThrow(() -> policy.validateReleaseWindow(movie, releaseStart.plusSeconds(1800), zone));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                assertThrows(BusinessException.class,
                        () -> policy.validateReleaseWindow(movie, releaseStart.minusMillis(1), zone)).getErrorCode());
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                assertThrows(BusinessException.class,
                        () -> policy.validateReleaseWindow(movie, endExclusive, zone)).getErrorCode());
    }
}
