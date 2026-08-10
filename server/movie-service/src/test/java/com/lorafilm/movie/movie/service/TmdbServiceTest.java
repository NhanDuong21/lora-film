package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.integration.tmdb.dto.TmdbImportOutcome;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImportResult;
import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbServiceTest {

    @Mock private TmdbImportService tmdbImportService;
    @Mock private MovieRepository movieRepository;
    @Mock private MovieService movieService;

    @Test
    void legacyApproveDelegatesToCanonicalImportAndReturnsDraftMovie() {
        TmdbService service = new TmdbService(tmdbImportService, movieRepository, movieService);
        Movie movie = new Movie();
        movie.setPublicId("public-movie-id");
        movie.setTmdbId(1L);
        movie.setStatus(MovieStatus.DRAFT);
        MovieDetailDto detail = new MovieDetailDto();
        detail.setPublicId(movie.getPublicId());
        detail.setTmdbId(1L);
        detail.setStatus(MovieStatus.DRAFT);

        when(tmdbImportService.importMovieById(1L)).thenReturn(
                new TmdbImportResult(1L, TmdbImportOutcome.CREATED, movie.getPublicId(), "Movie imported as DRAFT"));
        when(movieRepository.findByTmdbId(1L)).thenReturn(Optional.of(movie));
        when(movieService.getMovieByIdentifier(movie.getPublicId())).thenReturn(detail);

        MovieDto result = service.approveTmdbMovie(1);

        assertEquals(MovieStatus.DRAFT, result.getStatus());
        assertEquals(1L, result.getTmdbId());
        verify(tmdbImportService).importMovieById(1L);
        verify(movieService).getMovieByIdentifier("public-movie-id");
    }
}
