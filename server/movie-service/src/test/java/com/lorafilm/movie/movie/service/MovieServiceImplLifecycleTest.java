package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplLifecycleTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieGenreRepository movieGenreRepository;
    @Mock private MovieMediaRepository movieMediaRepository;
    @Mock private MovieCreditRepository movieCreditRepository;
    @Mock private MovieProductionCompanyRepository movieProductionCompanyRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private MovieMapper movieMapper;
    @Mock private AdminMovieProjectionService projectionService;

    private MovieServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MovieServiceImpl(
                movieRepository, movieGenreRepository, movieMediaRepository, movieCreditRepository,
                movieProductionCompanyRepository, movieVersionRepository, movieMapper,
                new MovieReadinessEvaluator(), projectionService, new MovieLifecyclePolicy());
    }

    @Test
    void rejectsIllegalApiTransitionBeforeSaving() {
        Movie movie = movie(1L, MovieStatus.DRAFT, 120);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-1")).thenReturn(Optional.of(movie));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateMovieStatus("movie-1", MovieStatus.NOW_SHOWING));

        assertEquals(ErrorCode.INVALID_MOVIE_STATUS_TRANSITION, exception.getErrorCode());
        verify(movieRepository, never()).save(any());
    }

    @Test
    void blockedDraftCannotBeApproved() {
        Movie movie = movie(2L, MovieStatus.DRAFT, 120);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-2")).thenReturn(Optional.of(movie));
        when(movieGenreRepository.findByMovieId(2L)).thenReturn(List.of());
        when(movieVersionRepository.existsActiveVersion(2L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(2L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateMovieStatus("movie-2", MovieStatus.UPCOMING));

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
        verify(movieRepository, never()).save(any());
    }

    @Test
    void warningDraftCanBeApprovedWhenLifecycleAndRelationsAreValid() {
        Movie movie = movie(3L, MovieStatus.DRAFT, 10);
        MovieGenre genreLink = genreLink(movie);
        MovieDto expected = new MovieDto();
        expected.setStatus(MovieStatus.UPCOMING);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-3")).thenReturn(Optional.of(movie));
        when(movieGenreRepository.findByMovieId(3L)).thenReturn(List.of(genreLink));
        when(movieVersionRepository.existsActiveVersion(3L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(3L)).thenReturn(true);
        when(movieRepository.save(movie)).thenReturn(movie);
        when(movieMediaRepository.findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                any(), any(), any())).thenReturn(Optional.empty());
        when(movieMapper.toDto(any(), anyList(), isNull())).thenReturn(expected);

        MovieDto result = service.updateMovieStatus("movie-3", MovieStatus.UPCOMING);

        assertEquals(MovieStatus.UPCOMING, movie.getStatus());
        assertEquals(MovieStatus.UPCOMING, result.getStatus());
        verify(movieRepository).save(movie);
    }

    private Movie movie(Long id, MovieStatus status, int duration) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setPublicId("movie-" + id);
        movie.setStatus(status);
        movie.setTitle("Movie");
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        movie.setDurationMinutes(duration);
        movie.setAgeRating(AgeRating.P);
        return movie;
    }

    private MovieGenre genreLink(Movie movie) {
        Genre genre = new Genre();
        genre.setName("Action");
        MovieGenre link = new MovieGenre();
        link.setMovie(movie);
        link.setGenre(genre);
        return link;
    }
}
