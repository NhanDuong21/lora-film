package com.project.movieservice.service;

import com.project.movieservice.dto.MovieDetailResponse;
import com.project.movieservice.dto.MoviePageResponse;
import com.project.movieservice.dto.MovieListItemResponse;
import com.project.movieservice.entity.Genre;
import com.project.movieservice.entity.Movie;
import com.project.movieservice.enumtype.AgeRating;
import com.project.movieservice.enumtype.MovieStatus;
import com.project.movieservice.dto.*;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.GenreRepository;
import com.project.movieservice.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    private Movie movie;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Avengers");
        movie.setDurationMinutes(180);
        movie.setReleaseDate(LocalDate.of(2026, 6, 20));
        movie.setEndDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setAgeRating(AgeRating.T16);
        
        Genre genre = new Genre(1, "Action");
        movie.setGenres(Set.of(genre));
    }

    @Test
    void getMovieDetail_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        MovieDetailResponse response = movieService.getMovieDetail("1");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Avengers", response.getTitle());
        assertEquals("NOW_SHOWING", response.getStatus());
        assertEquals(1, response.getGenres().size());
    }

    @Test
    void getMovieDetail_NotFound() {
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> movieService.getMovieDetail("1"));
    }

    @Test
    void getMovieDetail_Inactive() {
        movie.setStatus(MovieStatus.INACTIVE);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        assertThrows(BusinessException.class, () -> movieService.getMovieDetail("1"));
    }

    @Test
    void getMovieDetail_InvalidFormat() {
        assertThrows(BusinessException.class, () -> movieService.getMovieDetail("abc"));
    }

    @Test
    void getMovies_Success() {
        Page<Movie> page = new PageImpl<>(Collections.singletonList(movie));
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        MoviePageResponse<MovieListItemResponse> response = movieService.getMovies(
                "0", "10", null, null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Avengers", response.getContent().get(0).getTitle());
    }

    @Test
    void getMovies_InvalidPagination() {
        assertThrows(BusinessException.class, () -> movieService.getMovies(
                "-1", "10", null, null, null, null, null, null));
                
        assertThrows(BusinessException.class, () -> movieService.getMovies(
                "0", "51", null, null, null, null, null, null));
    }

    @Test
    void getMovies_GenreNotFound() {
        when(genreRepository.existsById(1)).thenReturn(false);

        assertThrows(BusinessException.class, () -> movieService.getMovies(
                "0", "10", null, null, "1", null, null, null));
    }

    @Test
    void getAdminMovieDetail_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        AdminMovieDetailResponse response = movieService.getAdminMovieDetail("1");
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getAdminMovieDetail_Inactive_Success() {
        movie.setStatus(MovieStatus.INACTIVE);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        AdminMovieDetailResponse response = movieService.getAdminMovieDetail("1");
        assertNotNull(response);
        assertEquals("INACTIVE", response.getStatus());
    }

    @Test
    void createMovie_Success() {
        MovieCreateRequest request = new MovieCreateRequest();
        request.setTitle("New Movie");
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDurationMinutes(120);
        request.setStatus("UPCOMING");
        request.setGenreIds(java.util.List.of(1));

        Genre genre = new Genre(1, "Action");
        when(genreRepository.findAllById(any())).thenReturn(java.util.List.of(genre));
        when(movieRepository.save(any(Movie.class))).thenAnswer(i -> {
            Movie m = i.getArgument(0);
            m.setId(2L);
            return m;
        });

        MovieCreatedResponse response = movieService.createMovie(request);
        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("New Movie", response.getTitle());
    }

    @Test
    void createMovie_InvalidDateRange() {
        MovieCreateRequest request = new MovieCreateRequest();
        request.setTitle("New Movie");
        request.setReleaseDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now());
        request.setDurationMinutes(120);
        request.setStatus("UPCOMING");
        request.setGenreIds(java.util.List.of(1));

        assertThrows(BusinessException.class, () -> movieService.createMovie(request));
    }

    @Test
    void updateMovieStatus_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie)); // NOW_SHOWING
        MovieStatusUpdateRequest request = new MovieStatusUpdateRequest();
        request.setStatus("ENDED");
        
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        MovieStatusResponse response = movieService.updateMovieStatus("1", request);
        assertNotNull(response);
        assertEquals("ENDED", response.getStatus());
    }

    @Test
    void updateMovieStatus_InvalidTransition() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie)); // NOW_SHOWING
        MovieStatusUpdateRequest request = new MovieStatusUpdateRequest();
        request.setStatus("UPCOMING"); // Invalid transition from NOW_SHOWING
        
        assertThrows(BusinessException.class, () -> movieService.updateMovieStatus("1", request));
    }
}
