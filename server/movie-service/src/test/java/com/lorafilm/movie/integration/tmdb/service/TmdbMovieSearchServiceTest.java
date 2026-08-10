package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbMovieSearchServiceTest {

    @Mock private TmdbClient tmdbClient;
    @Mock private MovieRepository movieRepository;

    private TmdbMovieSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new TmdbMovieSearchService(tmdbClient, new ObjectMapper(), movieRepository);
    }

    @Test
    void returnsVietnameseFriendlySuggestionsAndMarksExistingMovie() {
        when(tmdbClient.searchMovies("Avatar", 8)).thenReturn("""
                {
                  "results": [
                    {
                      "tmdbId": 19995,
                      "title": "Avatar",
                      "originalTitle": "Avatar",
                      "releaseDate": "2009-12-18",
                      "posterPath": "/avatar.jpg",
                      "overview": "Một bộ phim khoa học viễn tưởng."
                    }
                  ]
                }
                """);
        Movie existing = new Movie();
        existing.setTmdbId(19995L);
        existing.setPublicId("movie-public-id");
        existing.setStatus(MovieStatus.DRAFT);
        when(movieRepository.findByTmdbIdIn(List.of(19995L))).thenReturn(List.of(existing));

        var results = searchService.search(" Avatar ", 8);

        assertEquals(1, results.size());
        assertEquals(19995L, results.get(0).tmdbId());
        assertEquals(LocalDate.of(2009, 12, 18), results.get(0).originalReleaseDate());
        assertEquals("https://image.tmdb.org/t/p/w185/avatar.jpg", results.get(0).posterUrl());
        assertTrue(results.get(0).alreadyImported());
        assertEquals("DRAFT", results.get(0).localMovieStatus());
    }

    @Test
    void rejectsQueryThatIsTooShort() {
        assertThrows(BusinessException.class, () -> searchService.search("A", 8));

        verify(tmdbClient, never()).searchMovies(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void explainsWhenSeparateTmdbSourceDoesNotSupportSearchYet() {
        when(tmdbClient.searchMovies("Avatar", 8))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.search("Avatar", 8));

        assertTrue(exception.getMessage().contains("chưa hỗ trợ tìm kiếm phim theo tên"));
    }
}
