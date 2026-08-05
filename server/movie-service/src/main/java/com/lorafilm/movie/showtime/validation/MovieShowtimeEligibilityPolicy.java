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
        MovieFacts movieFacts = movie == null ? MovieFacts.absent() : new MovieFacts(
                movie.getId(), movie.getDeletedAt() != null, movie.getStatus(), movie.getDurationMinutes(),
                movie.getReleaseDate(), movie.getEndDate());
        VersionFacts versionFacts = version == null ? VersionFacts.absent() : new VersionFacts(
                false, version.getDeletedAt() != null, version.getStatus(), sameMovie(version.getMovie(), movie));
        validateMovieAndVersion(movieFacts, versionFacts);
    }

    public void validateMovieAndVersion(MovieFacts movie, VersionFacts version) {
        if (movie == null || movie.missing() || movie.deleted()) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }

        if (!hasSchedulableStatus(movie)) {
            throw new BusinessException(
                    ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING,
                    "Movie must be UPCOMING or NOW_SHOWING");
        }

        if (!hasValidDuration(movie)) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION, "Movie duration must be greater than zero");
        }

        if (version == null || version.missing() || version.deleted()) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_FOUND);
        }

        if (version.status() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_ACTIVE, "Movie version must be ACTIVE");
        }

        if (!version.belongsToMovie()) {
            throw new BusinessException(
                    ErrorCode.MOVIE_VERSION_NOT_BELONG_TO_MOVIE,
                    "Movie version does not belong to the movie");
        }
    }

    public void validateMovieCanOpenForBooking(Movie movie) {
        if (movie == null
                || movie.getDeletedAt() != null
                || (movie.getStatus() != MovieStatus.UPCOMING
                    && movie.getStatus() != MovieStatus.NOW_SHOWING)) {
            throw new BusinessException(
                    ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING,
                    "Movie must be UPCOMING or NOW_SHOWING before its showtime can open for booking");
        }
    }

    public void validateReleaseWindow(Movie movie, Instant startTime, ZoneId cinemaZone) {
        Objects.requireNonNull(movie, "movie");
        validateReleaseWindow(new MovieFacts(
                movie.getId(), movie.getDeletedAt() != null, movie.getStatus(), movie.getDurationMinutes(),
                movie.getReleaseDate(), movie.getEndDate()), startTime, cinemaZone);
    }

    public void validateReleaseWindow(MovieFacts movie, Instant startTime, ZoneId cinemaZone) {
        Objects.requireNonNull(movie, "movie");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(cinemaZone, "cinemaZone");

        if (movie.releaseDate() != null) {
            Instant releaseStart = movie.releaseDate().atStartOfDay(cinemaZone).toInstant();
            if (startTime.isBefore(releaseStart)) {
                throw new BusinessException(
                        ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW,
                        "Showtime cannot be scheduled before movie release date");
            }
        }

        if (movie.endDate() != null) {
            Instant endExclusive = movie.endDate().plusDays(1).atStartOfDay(cinemaZone).toInstant();
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
            issues.add(new EligibilityIssue(
                    MOVIE_STATUS_NOT_ELIGIBLE,
                    "Movie must be UPCOMING or NOW_SHOWING"));
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
                && (movie.getStatus() == MovieStatus.UPCOMING
                    || movie.getStatus() == MovieStatus.NOW_SHOWING);
    }

    private boolean hasValidDuration(Movie movie) {
        return movie != null && movie.getDurationMinutes() != null && movie.getDurationMinutes() > 0;
    }

    private boolean hasSchedulableStatus(MovieFacts movie) {
        return movie != null
                && !movie.missing()
                && !movie.deleted()
                && (movie.status() == MovieStatus.UPCOMING
                    || movie.status() == MovieStatus.NOW_SHOWING);
    }

    private boolean hasValidDuration(MovieFacts movie) {
        return movie != null && movie.durationMinutes() != null && movie.durationMinutes() > 0;
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

    public record MovieFacts(Long id,
                             boolean deleted,
                             MovieStatus status,
                             Integer durationMinutes,
                             LocalDate releaseDate,
                             LocalDate endDate) {
        public static MovieFacts absent() {
            return new MovieFacts(null, false, null, null, null, null);
        }

        public boolean missing() {
            return id == null && status == null && durationMinutes == null
                    && releaseDate == null && endDate == null && !deleted;
        }
    }

    public record VersionFacts(boolean missing,
                               boolean deleted,
                               ActiveStatus status,
                               boolean belongsToMovie) {
        public static VersionFacts absent() {
            return new VersionFacts(true, false, null, false);
        }
    }
}
