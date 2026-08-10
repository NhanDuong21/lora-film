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
import com.lorafilm.movie.movie.service.MovieOperationalGuard;
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
    @Mock
    private com.lorafilm.movie.showtime.repository.ShowtimeRepository showtimeRepository;
    @Mock
    private MovieOperationalGuard operationalGuard;

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
    void testUpdateMovie_ShouldNotChangeStatus() {
        // generic update payload doesn't contain status anymore
        
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("public-id")).thenReturn(Optional.of(existingMovie));
        when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArguments()[0]);
        com.lorafilm.movie.movie.dto.MovieDto dto = new com.lorafilm.movie.movie.dto.MovieDto();
        dto.setTitle("Test Movie");
        dto.setStatus(MovieStatus.DRAFT);
        when(movieMapper.toDto(any(Movie.class), any(), any())).thenReturn(dto);

        com.lorafilm.movie.movie.dto.MovieDto response = adminMovieService.updateMovie("public-id", validRequest);
        
        assertEquals(MovieStatus.DRAFT, existingMovie.getStatus());
        assertEquals(MovieStatus.DRAFT, response.getStatus());
    }

    @Test
    void testCreateMovie_Success() {
        Movie savedMovie = new Movie();
        savedMovie.setId(1L);
        savedMovie.setTitle("Test Movie");
        savedMovie.setStatus(MovieStatus.DRAFT);
        
        when(movieRepository.save(any(Movie.class))).thenReturn(savedMovie);
        com.lorafilm.movie.movie.dto.MovieDto dto = new com.lorafilm.movie.movie.dto.MovieDto();
        dto.setTitle("Test Movie");
        dto.setStatus(MovieStatus.DRAFT);
        when(movieMapper.toDto(eq(savedMovie), any(), any())).thenReturn(dto);

        com.lorafilm.movie.movie.dto.MovieDto response = adminMovieService.createMovie(validRequest);
        
        org.mockito.ArgumentCaptor<Movie> movieCaptor = org.mockito.ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie capturedMovie = movieCaptor.getValue();
        
        assertNotNull(response);
        assertEquals(MovieStatus.DRAFT, capturedMovie.getStatus());
        assertEquals("Test Movie", response.getTitle());
    }

    @Test
    void testUpdateMovie_Success() {
        validRequest.setTitle("Updated Title");
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("public-id")).thenReturn(Optional.of(existingMovie));
        when(movieRepository.save(any(Movie.class))).thenReturn(existingMovie);
        
        com.lorafilm.movie.movie.dto.MovieDto dto = new com.lorafilm.movie.movie.dto.MovieDto();
        dto.setTitle("Updated Title");
        when(movieMapper.toDto(eq(existingMovie), any(), any())).thenReturn(dto);

        com.lorafilm.movie.movie.dto.MovieDto response = adminMovieService.updateMovie("public-id", validRequest);
        
        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
    }

    @Test
    void testAssignGenres_Success() {
        com.lorafilm.movie.movie.domain.entity.Genre genre = new com.lorafilm.movie.movie.domain.entity.Genre();
        genre.setId(1L);
        
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("public-id")).thenReturn(Optional.of(existingMovie));
        when(genreRepository.findByPublicIdInAndDeletedAtIsNull(any())).thenReturn(java.util.List.of(genre));
        
        assertDoesNotThrow(() -> adminMovieService.assignGenres("public-id", java.util.List.of("genre-1")));
        
        verify(movieGenreRepository).deleteByMovieId(1L);
        verify(movieGenreRepository).save(any(com.lorafilm.movie.movie.domain.entity.MovieGenre.class));
    }

    @Test
    void testAssignGenres_PublishedMovieEmptyGenres_ThrowsException() {
        existingMovie.setStatus(MovieStatus.NOW_SHOWING);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("public-id")).thenReturn(Optional.of(existingMovie));
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> adminMovieService.assignGenres("public-id", Collections.emptyList()));
            
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }
}
