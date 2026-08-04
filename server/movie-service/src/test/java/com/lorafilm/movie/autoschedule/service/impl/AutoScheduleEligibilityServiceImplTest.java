package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.dto.response.EligibleMovieResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoScheduleEligibilityServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieVersionRepository movieVersionRepository;

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @Mock
    private MovieShowtimeEligibilityPolicy eligibilityPolicy;

    @Test
    void draftMovieIsReturnedAsEligibleForSchedulePreparation() {
        Movie movie = new Movie();
        movie.setId(8L);
        movie.setPublicId("draft-tmdb-movie");
        movie.setTitle("Phim TMDB chờ duyệt");
        movie.setSlug("phim-tmdb-cho-duyet");
        movie.setDurationMinutes(110);
        movie.setReleaseDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.DRAFT);

        MovieVersion version = new MovieVersion();
        version.setPublicId("draft-movie-version");
        version.setMovie(movie);
        version.setVersionName("2D Phụ đề Việt");
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("EN");
        version.setSubtitleLanguage("VI");
        version.setStatus(ActiveStatus.ACTIVE);

        LocalDate fromDate = LocalDate.of(2026, 7, 25);
        LocalDate toDate = LocalDate.of(2026, 7, 31);
        when(movieRepository.findAll()).thenReturn(List.of(movie));
        when(movieVersionRepository.findByMovieIdAndDeletedAtIsNull(8L)).thenReturn(List.of(version));
        when(eligibilityPolicy.evaluateRange(movie, List.of(version), fromDate, toDate)).thenReturn(List.of());
        when(movieMediaRepository.findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                List.of(8L),
                MovieMediaType.POSTER,
                ActiveStatus.ACTIVE)).thenReturn(List.of());

        AutoScheduleEligibilityServiceImpl service = new AutoScheduleEligibilityServiceImpl(
                movieRepository,
                movieVersionRepository,
                movieMediaRepository,
                eligibilityPolicy);

        List<EligibleMovieResponse> result = service.getEligibleMovies(fromDate, toDate);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getMoviePublicId()).isEqualTo("draft-tmdb-movie");
        assertThat(result.getFirst().getStatus()).isEqualTo(MovieStatus.DRAFT);
        assertThat(result.getFirst().isEligible()).isTrue();
        assertThat(result.getFirst().getVersions()).hasSize(1);
        verify(eligibilityPolicy).evaluateRange(movie, List.of(version), fromDate, toDate);
    }

    @Test
    void eligibleMoviesIncludeActivePrimaryPosterFromBatchLookup() {
        Movie movie = new Movie();
        movie.setId(7L);
        movie.setPublicId("movie-public-id");
        movie.setTitle("Phim có poster");
        movie.setSlug("phim-co-poster");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.NOW_SHOWING);

        MovieVersion version = new MovieVersion();
        version.setPublicId("version-public-id");
        version.setMovie(movie);
        version.setVersionName("2D Phụ đề Việt");
        version.setFormat(MovieFormat.TWO_D);
        version.setAudioLanguage("EN");
        version.setSubtitleLanguage("VI");
        version.setStatus(ActiveStatus.ACTIVE);

        MovieMedia poster = new MovieMedia();
        poster.setMovie(movie);
        poster.setUrl("https://cdn.example.test/poster.jpg");
        poster.setMediaType(MovieMediaType.POSTER);
        poster.setIsPrimary(true);
        poster.setStatus(ActiveStatus.ACTIVE);

        when(movieRepository.findAll()).thenReturn(List.of(movie));
        when(movieVersionRepository.findByMovieIdAndDeletedAtIsNull(7L)).thenReturn(List.of(version));
        when(eligibilityPolicy.evaluateRange(
                movie,
                List.of(version),
                LocalDate.of(2026, 7, 25),
                LocalDate.of(2026, 7, 31))).thenReturn(List.of());
        when(movieMediaRepository.findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                List.of(7L),
                MovieMediaType.POSTER,
                ActiveStatus.ACTIVE)).thenReturn(List.of(poster));

        AutoScheduleEligibilityServiceImpl service = new AutoScheduleEligibilityServiceImpl(
                movieRepository,
                movieVersionRepository,
                movieMediaRepository,
                eligibilityPolicy);

        List<EligibleMovieResponse> result = service.getEligibleMovies(
                LocalDate.of(2026, 7, 25),
                LocalDate.of(2026, 7, 31));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPrimaryPoster()).isEqualTo("https://cdn.example.test/poster.jpg");
        assertThat(result.getFirst().getVersions()).hasSize(1);
        verify(movieMediaRepository)
                .findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        List.of(7L),
                        MovieMediaType.POSTER,
                        ActiveStatus.ACTIVE);
    }
}
