package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.repository.GenreRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.service.AdminMovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminMovieServiceTest {

    @Mock
    private MovieRepository movieRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private MovieGenreRepository movieGenreRepository;
    @Mock
    private MovieMapper movieMapper;

    @InjectMocks
    private AdminMovieService adminMovieService;

    private MovieRequest validRequest;
    private Movie existingMovie;

    @BeforeEach
    void setUp() {
        validRequest = new MovieRequest();
        validRequest.setTitle("Test Movie");
        validRequest.setDurationMinutes(120);
        validRequest.setAgeRating(AgeRating.T13);
        validRequest.setReleaseDate(LocalDate.now());
        
        existingMovie = new Movie();
        existingMovie.setId(1L);
        existingMovie.setPublicId("public-id");
        existingMovie.setTitle("Old Title");
        existingMovie.setStatus(MovieStatus.DRAFT);
    }

    @Test
    void testCreateMovie_InvalidDates() {
        validRequest.setEndDate(LocalDate.now().minusDays(1));
        
        BusinessException exception = assertThrows(BusinessException.class, () -> adminMovieService.createMovie(validRequest));
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void testUpdateMovie_PublishWithoutGenres_ThrowsException() {
        validRequest.setStatus(MovieStatus.NOW_SHOWING);
        
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("public-id")).thenReturn(Optional.of(existingMovie));
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(Collections.emptyList());
        
        BusinessException exception = assertThrows(BusinessException.class, () -> adminMovieService.updateMovie("public-id", validRequest));
        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
    }
}
