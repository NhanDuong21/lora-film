package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.dto.tmdb.TmdbGenreDto;
import com.lorafilm.movie.movie.dto.tmdb.TmdbMovieDetailResponse;
import com.lorafilm.movie.movie.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TmdbServiceTest {

    @Mock
    private AdminMovieService adminMovieService;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    @Spy
    private TmdbService tmdbService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tmdbService, "tmdbApiUrl", "http://localhost");
        ReflectionTestUtils.setField(tmdbService, "tmdbApiKey", "test-key");
    }

    @Test
    void testApproveTmdbMovie() {
        TmdbMovieDetailResponse mockResponse = new TmdbMovieDetailResponse();
        mockResponse.setTitle("Deadpool");
        mockResponse.setAdult(true);
        mockResponse.setRuntime(108);
        
        TmdbGenreDto genre = new TmdbGenreDto();
        genre.setName("Action");
        mockResponse.setGenres(Collections.singletonList(genre));

        doReturn(mockResponse).when(tmdbService).getMovieDetail(1);

        MovieDto createdDto = new MovieDto();
        createdDto.setPublicId("public-movie-id");
        when(adminMovieService.createMovie(any(MovieRequest.class))).thenReturn(createdDto);

        Genre existingGenre = new Genre();
        existingGenre.setPublicId("public-genre-id");
        when(genreRepository.findByActiveSlugAndDeletedAtIsNull("action")).thenReturn(Optional.of(existingGenre));

        tmdbService.approveTmdbMovie(1);

        ArgumentCaptor<MovieRequest> requestCaptor = ArgumentCaptor.forClass(MovieRequest.class);
        verify(adminMovieService).createMovie(requestCaptor.capture());

        MovieRequest captured = requestCaptor.getValue();
        assertEquals("Deadpool", captured.getTitle());
        assertEquals(AgeRating.T18, captured.getAgeRating());
        assertEquals(108, captured.getDurationMinutes());
        
        verify(adminMovieService).assignGenres(eq("public-movie-id"), anyList());
    }
}
