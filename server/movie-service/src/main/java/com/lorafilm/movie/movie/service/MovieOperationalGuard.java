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
import java.time.LocalDate;
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
        assertReleaseWindowEditable(movie, request.getReleaseDate(), request.getEndDate());
    }

    public void assertReleaseWindowEditable(Movie movie, LocalDate releaseDate, LocalDate endDate) {
        var impacted = showtimeRepository.findFutureOperationalPublicIdsByMovieId(
                movie.getId(), Instant.now(clock));
        if (!impacted.isEmpty()) {
            throw immutable("Không thể đổi thời gian khai thác vì phim đã có suất chiếu trong tương lai.",
                    Map.of("impactedShowtimes", impacted));
        }
    }

    public void assertVersionEditable(MovieVersion version) {
        if (showtimeRepository.existsFutureOperationalByMovieVersionId(
                version.getId(), Instant.now(clock))) {
            throw immutable("Không thể sửa bản chiếu vì đang được dùng cho một suất chiếu trong tương lai.",
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
            throw immutable("Phim đang phục vụ khách hàng phải có một poster chính đang hoạt động.",
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
