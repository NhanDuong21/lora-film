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
import com.project.movieservice.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

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

        Object response = movieService.getMovieDetail("1", false);

        assertNotNull(response);
        assertTrue(response instanceof MovieDetailResponse);
        MovieDetailResponse dto = (MovieDetailResponse) response;
        assertEquals(1L, dto.getId());
        assertEquals("Avengers", dto.getTitle());
        assertEquals("NOW_SHOWING", dto.getStatus());
        assertEquals(1, dto.getGenres().size());
    }

    @Test
    void getMovieDetail_NotFound() {
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> movieService.getMovieDetail("1", false));
    }

    @Test
    void getMovieDetail_Inactive() {
        movie.setStatus(MovieStatus.INACTIVE);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        assertThrows(BusinessException.class, () -> movieService.getMovieDetail("1", false));
    }

    @Test
    void getMovieDetail_InvalidFormat() {
        assertThrows(BusinessException.class, () -> movieService.getMovieDetail("abc", false));
    }

    @Test
    void getMovies_Success() {
        Page<Movie> page = new PageImpl<>(Collections.singletonList(movie));
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        MoviePageResponse<?> response = movieService.getMovies(
                "0", "10", null, null, null, null, null, null, false);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertTrue(response.getContent().get(0) instanceof MovieListItemResponse);
        assertEquals("Avengers", ((MovieListItemResponse)response.getContent().get(0)).getTitle());
    }

    @Test
    void getMovies_InvalidPagination() {
        assertThrows(BusinessException.class, () -> movieService.getMovies(
                "-1", "10", null, null, null, null, null, null, false));
                
        assertThrows(BusinessException.class, () -> movieService.getMovies(
                "0", "51", null, null, null, null, null, null, false));
    }

    @Test
    void getMovies_GenreNotFound() {
        when(genreRepository.existsById(1)).thenReturn(false);

        assertThrows(BusinessException.class, () -> movieService.getMovies(
                "0", "10", null, null, "1", null, null, null, false));
    }

    @Test
    void getAdminMovieDetail_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        Object response = movieService.getMovieDetail("1", true);
        assertNotNull(response);
        assertTrue(response instanceof AdminMovieDetailResponse);
        assertEquals(1L, ((AdminMovieDetailResponse)response).getId());
    }

    @Test
    void getAdminMovieDetail_Inactive_Success() {
        movie.setStatus(MovieStatus.INACTIVE);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        Object response = movieService.getMovieDetail("1", true);
        assertNotNull(response);
        assertTrue(response instanceof AdminMovieDetailResponse);
        assertEquals("INACTIVE", ((AdminMovieDetailResponse)response).getStatus());
    }

    @Test
    void createMovie_Success() {
        MovieCreateRequest request = new MovieCreateRequest();
        request.setTitle("New Movie");
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDurationMinutes(120);
        request.setStatus("UPCOMING");
        request.setGenreIds(java.util.Set.of(1));

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
        request.setGenreIds(java.util.Set.of(1));

        assertThrows(BusinessException.class, () -> movieService.createMovie(request));
    }

    @Test
    void softDeleteMovie_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie)); // NOW_SHOWING
        when(showtimeRepository.existsByMovieIdAndEndTimeAfter(anyLong(), any())).thenReturn(false);
        
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        movieService.softDeleteMovie("1");
        
        assertEquals(MovieStatus.INACTIVE, movie.getStatus());
    }

    @Test
    void softDeleteMovie_Failed_WhenFutureShowtimes() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie)); // NOW_SHOWING
        when(showtimeRepository.existsByMovieIdAndEndTimeAfter(anyLong(), any())).thenReturn(true);
        
        assertThrows(BusinessException.class, () -> movieService.softDeleteMovie("1"));
    }

    @Test
    void updateMovie_InvalidTransition() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie)); // NOW_SHOWING
        MovieUpdateRequest request = new MovieUpdateRequest();
        request.setTitle("Avengers");
        request.setReleaseDate(LocalDate.now().minusDays(10));
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDurationMinutes(180); 
        request.setStatus("UPCOMING"); // Invalid transition from NOW_SHOWING
        request.setGenreIds(java.util.Set.of(1));
        
        assertThrows(BusinessException.class, () -> movieService.updateMovie("1", request));
    }

    @Test
    void updateMovie_DurationChangeBlocked() {
        // Phim đang chiếu (khác UPCOMING)
        movie.setStatus(MovieStatus.NOW_SHOWING);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        
        MovieUpdateRequest request = new MovieUpdateRequest();
        request.setTitle("Avengers");
        request.setReleaseDate(LocalDate.now().minusDays(10));
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDurationMinutes(200); // Cố tình đổi thời lượng khác 180
        request.setStatus("NOW_SHOWING");
        request.setGenreIds(java.util.Set.of(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> movieService.updateMovie("1", request));
        assertEquals("MOVIE_HAS_FUTURE_SHOWTIMES", exception.getErrorCode());
    }
}
