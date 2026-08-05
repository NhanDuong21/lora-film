package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.dto.UpdateMovieMediaRequest;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
public class MovieOperationalGuard {
    private final ShowtimeRepository showtimeRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final Clock clock;

    public MovieOperationalGuard(ShowtimeRepository showtimeRepository,
                                 MovieMediaRepository movieMediaRepository,
                                 Clock clock) {
        this.showtimeRepository = showtimeRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.clock = clock;
    }

    public void assertSchedulingFieldsEditable(Movie movie, MovieRequest request) {
        boolean schedulingChanged = !Objects.equals(movie.getDurationMinutes(), request.getDurationMinutes())
                || !Objects.equals(movie.getReleaseDate(), request.getReleaseDate())
                || !Objects.equals(movie.getEndDate(), request.getEndDate());
        if (!schedulingChanged) {
            return;
        }
        var impacted = showtimeRepository.findFutureOperationalPublicIdsByMovieId(
                movie.getId(), Instant.now(clock));
        if (!impacted.isEmpty()) {
            throw immutable("Duration or release window is already used by future showtimes.",
                    Map.of("impactedShowtimes", impacted));
        }
    }

    public void assertVersionEditable(MovieVersion version) {
        if (showtimeRepository.existsFutureOperationalByMovieVersionId(
                version.getId(), Instant.now(clock))) {
            throw immutable("This movie version is already used by a future showtime.",
                    Map.of("movieVersionPublicId", version.getPublicId()));
        }
    }

    public void assertPrimaryPosterPreserved(MovieMedia media, UpdateMovieMediaRequest request) {
        boolean remainsActivePrimaryPoster = request.getMediaType() == MovieMediaType.POSTER
                && Boolean.TRUE.equals(request.getIsPrimary())
                && (request.getStatus() == null || request.getStatus() == ActiveStatus.ACTIVE);
        if (!remainsActivePrimaryPoster) {
            assertPrimaryPosterCanBeRemoved(media);
        }
    }

    public void assertPrimaryPosterCanBeRemoved(MovieMedia media) {
        boolean currentPrimaryPoster = media.getMediaType() == MovieMediaType.POSTER
                && Boolean.TRUE.equals(media.getIsPrimary())
                && media.getStatus() == ActiveStatus.ACTIVE;
        if (!currentPrimaryPoster || !isPublic(media.getMovie().getStatus())) {
            return;
        }
        if (!movieMediaRepository.existsOtherActivePrimaryPoster(
                media.getMovie().getId(), media.getId())) {
            throw immutable("A published movie must keep an active primary poster.",
                    Map.of("mediaPublicId", media.getPublicId()));
        }
    }

    private boolean isPublic(MovieStatus status) {
        return status == MovieStatus.UPCOMING || status == MovieStatus.NOW_SHOWING;
    }

    private BusinessException immutable(String message, Object data) {
        return new BusinessException(ErrorCode.MOVIE_OPERATIONAL_DATA_IMMUTABLE, message, data);
    }
}
