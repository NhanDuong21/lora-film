package com.lorafilm.movie.integration.tmdb.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;

@Component
public class TmdbClient {
    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbClient(TmdbProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("x-api-key", properties.getApiKey())
                .build();
    }

    public String fetchMoviesExport(String cursor, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tmdb/export")
                        .queryParam("cursor", cursor)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(String.class);
    }

    public void triggerDownloadExport() {
        try {
            restClient.post()
                    .uri("/api/tmdb/download-export")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            // Log and ignore, maybe node job already ran
        }
    }

    public String fetchLatestMovies() {
        return restClient.get()
                .uri("/api/tmdb/movies/latest")
                .retrieve()
                .body(String.class);
    }

    public String fetchUpdatedMovies(String lastUpdated) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tmdb/movies/updated")
                        .queryParam("lastUpdated", lastUpdated)
                        .build())
                .retrieve()
                .body(String.class);
    }

    public String fetchMovieDetails(Long tmdbId) {
        return restClient.get()
                .uri("/api/tmdb/movies/{tmdbId}", tmdbId)
                .retrieve()
                .body(String.class);
    }
}
