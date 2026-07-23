package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.service.MovieServiceImpl;
import com.lorafilm.movie.movie.service.AdminMovieProjectionService;
import com.lorafilm.movie.movie.service.MovieHealthFacts;
import com.lorafilm.movie.movie.service.MovieLifecyclePolicy;
import com.lorafilm.movie.movie.service.MovieReadinessEvaluator;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieReadinessDto;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.dto.MovieMapper;
import java.util.Optional;
import java.util.List;
import java.util.Collections;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Rule 1: Movie Publish Validation Business Rules")
class MoviePublishValidationTest {

    @Mock
    private MovieVersionRepository movieVersionRepository;

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @Mock
    private MovieGenreRepository movieGenreRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieMapper movieMapper;

    @Spy
    private MovieReadinessEvaluator readinessEvaluator = new MovieReadinessEvaluator();

    @Spy
    private MovieLifecyclePolicy lifecyclePolicy = new MovieLifecyclePolicy();

    @Mock
    private AdminMovieProjectionService projectionService;

    @InjectMocks
    private MovieServiceImpl movieService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should publish successfully when active version and primary poster exist")
    void validatePublishConditions_Success() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        assertDoesNotThrow(() -> movieService.validatePublishConditions(1L));

        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
        verify(movieGenreRepository, times(1)).findByMovieId(1L);
    }

    @Test
    @DisplayName("Should throw exception when active version is missing during publish validation")
    void validatePublishConditions_MissingVersion() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    @DisplayName("Should throw exception when primary poster is missing during publish validation")
    void validatePublishConditions_MissingPoster() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    @DisplayName("Should throw exception when both active version and primary poster are missing")
    void validatePublishConditions_MissingBoth() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }
    
    @Test
    void validatePublishConditions_MissingGenre() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
        verify(movieGenreRepository, times(1)).findByMovieId(1L);
    }

    @Test
    void updateMovieStatus_EndedToUpcoming_MissingVersion() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setPublicId("movie123");
        movie.setStatus(MovieStatus.ENDED);
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie123")).thenReturn(Optional.of(movie));
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.updateMovieStatus("movie123", MovieStatus.UPCOMING)
        );

        assertEquals(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, exception.getErrorCode());
    }

    @Test
    void updateMovieStatus_EndedToUpcoming_MissingPoster() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setPublicId("movie123");
        movie.setStatus(MovieStatus.ENDED);
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie123")).thenReturn(Optional.of(movie));
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.updateMovieStatus("movie123", MovieStatus.UPCOMING)
        );

        assertEquals(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, exception.getErrorCode());
    }

    @Test
    void updateMovieStatus_EndedToUpcoming_Success() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setPublicId("movie123");
        movie.setStatus(MovieStatus.ENDED);
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        
        MovieGenre mg = new MovieGenre();
        Genre g = new Genre();
        g.setName("Action");
        mg.setGenre(g);
        
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie123")).thenReturn(Optional.of(movie));
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(mg));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        when(movieMapper.toDto(any(), any(), any())).thenReturn(new MovieDto());

        assertDoesNotThrow(() -> movieService.updateMovieStatus("movie123", MovieStatus.UPCOMING));
        assertEquals(MovieStatus.UPCOMING, movie.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = MovieStatus.class, names = "UPCOMING")
    void draftPublishTransitionsRejectMissingGenre(MovieStatus targetStatus) {
        Movie movie = draftMovie();
        setEligibleReleaseDate(movie, targetStatus);
        stubMovieForStatusUpdate(movie);
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> movieService.updateMovieStatus("movie123", targetStatus));

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
    }

    @ParameterizedTest
    @EnumSource(value = MovieStatus.class, names = "UPCOMING")
    void draftPublishTransitionsRejectMissingActiveVersion(MovieStatus targetStatus) {
        Movie movie = draftMovie();
        setEligibleReleaseDate(movie, targetStatus);
        stubMovieForStatusUpdate(movie);
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> movieService.updateMovieStatus("movie123", targetStatus));

        assertEquals(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, exception.getErrorCode());
    }

    @ParameterizedTest
    @EnumSource(value = MovieStatus.class, names = "UPCOMING")
    void draftPublishTransitionsRejectMissingPrimaryPoster(MovieStatus targetStatus) {
        Movie movie = draftMovie();
        setEligibleReleaseDate(movie, targetStatus);
        stubMovieForStatusUpdate(movie);
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> movieService.updateMovieStatus("movie123", targetStatus));

        assertEquals(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, exception.getErrorCode());
    }

    @Test
    void publishValidationDelegatesTheSameFactsToCanonicalEvaluator() {
        Movie movie = draftMovie();
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        assertThrows(BusinessException.class, () -> movieService.validatePublishConditions(1L));

        ArgumentCaptor<MovieHealthFacts> factsCaptor = ArgumentCaptor.forClass(MovieHealthFacts.class);
        verify(readinessEvaluator).validatePublishConditions(factsCaptor.capture());
        MovieHealthFacts delegatedFacts = factsCaptor.getValue();
        MovieReadinessDto readiness = readinessEvaluator.evaluate(delegatedFacts);

        assertTrue(delegatedFacts.hasGenres());
        assertFalse(delegatedFacts.hasActiveVersion());
        assertTrue(delegatedFacts.hasPrimaryPoster());
        assertEquals(MovieHealthStatus.BLOCKED, readiness.getHealthStatus());
        assertEquals("INCOMPLETE", readiness.getClassification());
        assertEquals(MovieReadinessEvaluator.NO_ACTIVE_VERSION, readiness.getBlockers().getFirst().getCode());
    }

    private Movie draftMovie() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setPublicId("movie123");
        movie.setStatus(MovieStatus.DRAFT);
        return movie;
    }

    private void stubMovieForStatusUpdate(Movie movie) {
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie123"))
                .thenReturn(Optional.of(movie));
    }

    private void setEligibleReleaseDate(Movie movie, MovieStatus targetStatus) {
        movie.setReleaseDate(targetStatus == MovieStatus.UPCOMING
                ? LocalDate.now().plusDays(1)
                : LocalDate.now().minusDays(1));
    }
}

