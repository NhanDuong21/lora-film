package com.lorafilm.movie.integration.tmdb.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;

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
        byte[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tmdb/export")
                        .queryParam("cursor", cursor)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(byte[].class);
        return response != null ? new String(response, StandardCharsets.UTF_8) : null;
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
        byte[] response = restClient.get()
                .uri("/api/tmdb/movies/latest")
                .retrieve()
                .body(byte[].class);
        return response != null ? new String(response, StandardCharsets.UTF_8) : null;
    }

    public String fetchUpdatedMovies(String lastUpdated) {
        byte[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tmdb/movies/updated")
                        .queryParam("lastUpdated", lastUpdated)
                        .build())
                .retrieve()
                .body(byte[].class);
        return response != null ? new String(response, StandardCharsets.UTF_8) : null;
    }

    public String fetchMovieDetails(Long tmdbId) {
        byte[] response = restClient.get()
                .uri("/api/tmdb/movies/{tmdbId}", tmdbId)
                .retrieve()
                .body(byte[].class);
        return response != null ? new String(response, StandardCharsets.UTF_8) : null;
    }

    public String fetchPersonDetails(Long tmdbPersonId) {
        byte[] response = restClient.get()
                .uri("/api/import/people/{tmdbPersonId}", tmdbPersonId)
                .retrieve()
                .body(byte[].class);
        return response != null ? new String(response, StandardCharsets.UTF_8) : null;
    }
}
