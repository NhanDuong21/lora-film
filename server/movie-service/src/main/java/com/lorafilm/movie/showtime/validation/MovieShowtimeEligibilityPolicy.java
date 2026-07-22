package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MovieShowtimeEligibilityPolicy {

    public static final String MOVIE_STATUS_NOT_ELIGIBLE = "MOVIE_STATUS_NOT_ELIGIBLE";
    public static final String MOVIE_DURATION_INVALID = "MOVIE_DURATION_INVALID";
    public static final String NO_ACTIVE_MOVIE_VERSION = "NO_ACTIVE_MOVIE_VERSION";
    public static final String OUTSIDE_RELEASE_WINDOW = "OUTSIDE_RELEASE_WINDOW";

    public void validateMovieAndVersion(Movie movie, MovieVersion version) {
        if (movie == null || movie.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }

        if (!hasSchedulableStatus(movie)) {
            throw new BusinessException(
                    ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING,
                    "Movie must be NOW_SHOWING or UPCOMING");
        }

        if (!hasValidDuration(movie)) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION, "Movie duration must be greater than zero");
        }

        if (version == null || version.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND);
        }

        if (version.getStatus() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_ACTIVE, "Movie version must be ACTIVE");
        }

        if (!sameMovie(version.getMovie(), movie)) {
            throw new BusinessException(
                    ErrorCode.MOVIE_VERSION_NOT_BELONG_TO_MOVIE,
                    "Movie version does not belong to the movie");
        }
    }

    public void validateReleaseWindow(Movie movie, Instant startTime, ZoneId cinemaZone) {
        Objects.requireNonNull(movie, "movie");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(cinemaZone, "cinemaZone");

        if (movie.getReleaseDate() != null) {
            Instant releaseStart = movie.getReleaseDate().atStartOfDay(cinemaZone).toInstant();
            if (startTime.isBefore(releaseStart)) {
                throw new BusinessException(
                        ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                        "Showtime cannot be scheduled before movie release date");
            }
        }

        if (movie.getEndDate() != null) {
            Instant endExclusive = movie.getEndDate().plusDays(1).atStartOfDay(cinemaZone).toInstant();
            if (!startTime.isBefore(endExclusive)) {
                throw new BusinessException(
                        ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                        "Showtime cannot start after movie end date");
            }
        }
    }

    public List<EligibilityIssue> evaluateRange(Movie movie,
                                                List<MovieVersion> versions,
                                                LocalDate fromDate,
                                                LocalDate toDate) {
        List<EligibilityIssue> issues = new ArrayList<>();

        if (!hasSchedulableStatus(movie)) {
            issues.add(new EligibilityIssue(MOVIE_STATUS_NOT_ELIGIBLE, "Movie must be NOW_SHOWING or UPCOMING"));
        }

        if (!hasValidDuration(movie)) {
            issues.add(new EligibilityIssue(MOVIE_DURATION_INVALID, "Movie duration is invalid"));
        }

        boolean hasActiveVersion = versions != null && versions.stream()
                .anyMatch(version -> version != null
                        && version.getDeletedAt() == null
                        && version.getStatus() == ActiveStatus.ACTIVE
                        && sameMovie(version.getMovie(), movie));
        if (!hasActiveVersion) {
            issues.add(new EligibilityIssue(NO_ACTIVE_MOVIE_VERSION, "Movie has no active version"));
        }

        boolean outsideRange = toDate != null
                && movie != null
                && movie.getReleaseDate() != null
                && movie.getReleaseDate().isAfter(toDate);
        outsideRange = outsideRange || (fromDate != null
                && movie != null
                && movie.getEndDate() != null
                && movie.getEndDate().isBefore(fromDate));
        if (outsideRange) {
            issues.add(new EligibilityIssue(OUTSIDE_RELEASE_WINDOW, "Schedule range is outside movie release window"));
        }

        return List.copyOf(issues);
    }

    private boolean hasSchedulableStatus(Movie movie) {
        return movie != null
                && movie.getDeletedAt() == null
                && (movie.getStatus() == MovieStatus.NOW_SHOWING || movie.getStatus() == MovieStatus.UPCOMING);
    }

    private boolean hasValidDuration(Movie movie) {
        return movie != null && movie.getDurationMinutes() != null && movie.getDurationMinutes() > 0;
    }

    private boolean sameMovie(Movie first, Movie second) {
        if (first == second) {
            return true;
        }
        return first != null
                && second != null
                && first.getId() != null
                && Objects.equals(first.getId(), second.getId());
    }

    public record EligibilityIssue(String code, String message) {
    }
}
