package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbProviderMovieServiceTest {

    @Mock private TmdbClient tmdbClient;
    private TmdbProviderMovieService service;

    @BeforeEach
    void setUp() {
        service = new TmdbProviderMovieService(tmdbClient, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void parsesAndValidatesProviderIdentity() {
        when(tmdbClient.fetchMovieDetails(10L)).thenReturn("""
                {"success":true,"data":{"tmdbId":10,"movie":{"tmdbId":10,"title":"Movie"}}}
                """);

        TmdbMovieWrapperDto result = service.fetchMovie(10L);

        assertEquals(10L, result.getTmdbId());
        assertEquals("Movie", result.getMovie().getTitle());
    }

    @Test
    void rejectsMismatchedNestedIdentity() {
        when(tmdbClient.fetchMovieDetails(10L)).thenReturn("""
                {"success":true,"data":{"tmdbId":10,"movie":{"tmdbId":11,"title":"Movie"}}}
                """);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.fetchMovie(10L));

        assertEquals(ErrorCode.TMDB_IMPORT_INVALID_PAYLOAD, exception.getErrorCode());
    }

    @Test
    void mapsMalformedAndFailedResponsesToProviderErrors() {
        when(tmdbClient.fetchMovieDetails(20L)).thenReturn("not-json");
        BusinessException malformed = assertThrows(BusinessException.class, () -> service.fetchMovie(20L));
        assertEquals(ErrorCode.TMDB_PROVIDER_RESPONSE_INVALID, malformed.getErrorCode());

        when(tmdbClient.fetchMovieDetails(21L)).thenReturn("{\"success\":false,\"message\":\"down\"}");
        BusinessException failed = assertThrows(BusinessException.class, () -> service.fetchMovie(21L));
        assertEquals(ErrorCode.TMDB_PROVIDER_UNAVAILABLE, failed.getErrorCode());
    }
}
