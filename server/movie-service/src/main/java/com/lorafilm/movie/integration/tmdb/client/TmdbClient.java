package com.lorafilm.movie.integration.tmdb.client;

import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {
    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbClient(TmdbProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();
    }

    public String fetchMovies(String cursor, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tmdb/export")
                        .queryParam("cursor", cursor)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(String.class); // Simplified for demonstration
    }
}
