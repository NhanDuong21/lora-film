package com.lorafilm.movie.integration.tmdb.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;

import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;

@Component
public class TmdbClient {
    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);
    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbClient(TmdbProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-api-key", properties.getApiKey())
                .requestFactory(requestFactory)
                .build();
    }

    public String fetchMoviesExport(String cursor, int limit) {
        return fetchMoviesExport(cursor, limit, null, null);
    }

    public String fetchMoviesExport(String cursor, int limit, LocalDate releaseDateFrom, LocalDate releaseDateTo) {
        log.info("[TmdbClient] Fetching movies export from TMDB API (cursor={}, limit={}, from={}, to={})",
                cursor, limit, releaseDateFrom, releaseDateTo);
        try {
            byte[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/tmdb/export")
                            .queryParam("cursor", cursor)
                            .queryParam("limit", limit)
                            .queryParamIfPresent("releaseDateFrom", java.util.Optional.ofNullable(releaseDateFrom))
                            .queryParamIfPresent("releaseDateTo", java.util.Optional.ofNullable(releaseDateTo))
                            .build())
                    .retrieve()
                    .body(byte[].class);
            String result = response != null ? new String(response, StandardCharsets.UTF_8) : null;
            log.info("[TmdbClient] Successfully fetched movies export for cursor={}", cursor);
            return result;
        } catch (Exception e) {
            log.error("[TmdbClient] Error fetching movies export (cursor={}, limit={}): {}", cursor, limit, e.getMessage());
            throw e;
        }
    }

    public void triggerDownloadExport() {
        log.info("[TmdbClient] Triggering download export on TMDB API");
        try {
            restClient.post()
                    .uri("/api/tmdb/download-export")
                    .retrieve()
                    .body(String.class);
            log.info("[TmdbClient] Download export trigger completed");
        } catch (Exception e) {
            log.warn("[TmdbClient] Could not trigger export download (may already be running or downloaded): {}", e.getMessage());
        }
    }

    public String fetchLatestMovies() {
        log.info("[TmdbClient] Fetching latest movies from TMDB API");
        try {
            byte[] response = restClient.get()
                    .uri("/api/tmdb/movies/latest")
                    .retrieve()
                    .body(byte[].class);
            String result = response != null ? new String(response, StandardCharsets.UTF_8) : null;
            log.info("[TmdbClient] Successfully fetched latest movies");
            return result;
        } catch (Exception e) {
            log.error("[TmdbClient] Error fetching latest movies: {}", e.getMessage());
            throw e;
        }
    }

    public String fetchUpdatedMovies(String lastUpdated) {
        log.info("[TmdbClient] Fetching updated movies from TMDB API (lastUpdated={})", lastUpdated);
        try {
            byte[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/tmdb/movies/updated")
                            .queryParam("lastUpdated", lastUpdated)
                            .build())
                    .retrieve()
                    .body(byte[].class);
            String result = response != null ? new String(response, StandardCharsets.UTF_8) : null;
            log.info("[TmdbClient] Successfully fetched updated movies for lastUpdated={}", lastUpdated);
            return result;
        } catch (Exception e) {
            log.error("[TmdbClient] Error fetching updated movies (lastUpdated={}): {}", lastUpdated, e.getMessage());
            throw e;
        }
    }

    public String fetchMovieDetails(Long tmdbId) {
        log.info("[TmdbClient] Fetching movie details from TMDB API (tmdbId={})", tmdbId);
        try {
            byte[] response = restClient.get()
                    .uri("/api/tmdb/movies/{tmdbId}", tmdbId)
                    .retrieve()
                    .body(byte[].class);
            String result = response != null ? new String(response, StandardCharsets.UTF_8) : null;
            log.info("[TmdbClient] Successfully fetched movie details for tmdbId={}", tmdbId);
            return result;
        } catch (Exception e) {
            log.error("[TmdbClient] Error fetching movie details for tmdbId={}: {}", tmdbId, e.getMessage());
            throw e;
        }
    }

    public String searchMovies(String query, int limit) {
        log.info("[TmdbClient] Searching TMDB movies (query={}, limit={})", query, limit);
        try {
            byte[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/tmdb/search")
                            .queryParam("query", query)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(byte[].class);
            return response != null ? new String(response, StandardCharsets.UTF_8) : null;
        } catch (Exception exception) {
            log.error("[TmdbClient] Error searching TMDB movies: {}", exception.getMessage());
            throw exception;
        }
    }

    public String fetchPersonDetails(Long tmdbPersonId) {
        log.info("[TmdbClient] Fetching person details from TMDB API (tmdbPersonId={})", tmdbPersonId);
        try {
            byte[] response = restClient.get()
                    .uri("/api/import/people/{tmdbPersonId}", tmdbPersonId)
                    .retrieve()
                    .body(byte[].class);
            String result = response != null ? new String(response, StandardCharsets.UTF_8) : null;
            log.info("[TmdbClient] Successfully fetched person details for tmdbPersonId={}", tmdbPersonId);
            return result;
        } catch (Exception e) {
            log.error("[TmdbClient] Error fetching person details for tmdbPersonId={}: {}", tmdbPersonId, e.getMessage());
            throw e;
        }
    }
}
