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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MovieOperationalGuardTest {
    private ShowtimeRepository showtimeRepository;
    private MovieMediaRepository mediaRepository;
    private MovieOperationalGuard guard;
    private Instant now;

    @BeforeEach
    void setUp() {
        showtimeRepository = mock(ShowtimeRepository.class);
        mediaRepository = mock(MovieMediaRepository.class);
        now = Instant.parse("2026-08-04T03:00:00Z");
        guard = new MovieOperationalGuard(
                showtimeRepository, mediaRepository, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void rejectsSchedulingFieldChangeWhenFutureShowtimeUsesMovie() {
        Movie movie = movie();
        MovieRequest request = request(130);
        when(showtimeRepository.findFutureOperationalPublicIdsByMovieId(1L, now))
                .thenReturn(List.of("showtime-1"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.assertSchedulingFieldsEditable(movie, request));

        assertEquals(ErrorCode.MOVIE_OPERATIONAL_DATA_IMMUTABLE, exception.getErrorCode());
    }

    @Test
    void rejectsVersionMutationWhenReferencedByFutureShowtime() {
        MovieVersion version = new MovieVersion();
        version.setId(2L);
        version.setPublicId("version-1");
        when(showtimeRepository.existsFutureOperationalByMovieVersionId(2L, now))
                .thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.assertVersionEditable(version));

        assertEquals(ErrorCode.MOVIE_OPERATIONAL_DATA_IMMUTABLE, exception.getErrorCode());
    }

    @Test
    void rejectsRemovingLastPrimaryPosterFromPublishedMovie() {
        Movie movie = movie();
        movie.setStatus(MovieStatus.NOW_SHOWING);
        MovieMedia media = new MovieMedia();
        media.setId(3L);
        media.setPublicId("media-1");
        media.setMovie(movie);
        media.setMediaType(MovieMediaType.POSTER);
        media.setIsPrimary(true);
        media.setStatus(ActiveStatus.ACTIVE);
        UpdateMovieMediaRequest request = new UpdateMovieMediaRequest(
                MovieMediaType.POSTER, "https://example/poster.jpg", "Poster",
                0, false, ActiveStatus.ACTIVE);
        when(mediaRepository.existsOtherActivePrimaryPoster(1L, 3L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> guard.assertPrimaryPosterPreserved(media, request));

        assertEquals(ErrorCode.MOVIE_OPERATIONAL_DATA_IMMUTABLE, exception.getErrorCode());
    }

    private Movie movie() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2026, 8, 1));
        movie.setEndDate(LocalDate.of(2026, 8, 31));
        movie.setStatus(MovieStatus.DRAFT);
        return movie;
    }

    private MovieRequest request(int duration) {
        MovieRequest request = new MovieRequest();
        request.setTitle("Movie");
        request.setDurationMinutes(duration);
        request.setReleaseDate(LocalDate.of(2026, 8, 1));
        request.setEndDate(LocalDate.of(2026, 8, 31));
        return request;
    }
}
