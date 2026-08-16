package com.lorafilm.booking.infrastructure.client.impl;

import com.lorafilm.booking.infrastructure.client.MovieServiceClient;
import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
public class MovieServiceClientImpl implements MovieServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String movieServiceUrl;
    private final String internalToken;

    public MovieServiceClientImpl(
            RestTemplate restTemplate,
            @Value("${movie.service.url:http://movie-service}") String movieServiceUrl,
            @Value("${services.movie-service.internal-token:${BOOKING_TO_MOVIE_INTERNAL_TOKEN:${APP_INTERNAL_TOKEN:${INTERNAL_NOTIFICATION_TOKEN:8f0a00f11a51ad253c9560e55236b464bab6b20e57642c01a9c896a98ff061ff}}}}")
            String internalToken) {
        this.restTemplate = restTemplate;
        this.movieServiceUrl = movieServiceUrl;
        this.internalToken = internalToken;
    }

    @Override
    public ShowtimeSeatLayoutResponse getShowtimeSeatLayout(Long showtimeId) {
        if (showtimeId == null) {
            return null;
        }
        return fetchSeatLayout(showtimeId.toString(), "/internal/showtimes/" + showtimeId + "/seat-layout");
    }

    @Override
    public ShowtimeSeatLayoutResponse getShowtimeSeatLayoutByPublicId(String showtimePublicId) {
        if (showtimePublicId == null || showtimePublicId.isBlank()) {
            return null;
        }
        return fetchSeatLayout(showtimePublicId, "/api/showtimes/" + showtimePublicId + "/seat-layout");
    }

    private ShowtimeSeatLayoutResponse fetchSeatLayout(String identifier, String path) {
        try {
            String url = movieServiceUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<MovieServiceApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MovieServiceApiResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MovieServiceApiResponse wrappedResponse = response.getBody();
                if (wrappedResponse.getData() != null) {
                    return wrappedResponse.getData();
                }
            }

            // Fallback for direct un-wrapped response if needed
            ResponseEntity<ShowtimeSeatLayoutResponse> directResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ShowtimeSeatLayoutResponse.class
            );
            if (directResponse.getStatusCode().is2xxSuccessful() && directResponse.getBody() != null) {
                return directResponse.getBody();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch showtime seat layout from movie-service for identifier {}: {}", identifier, ex.getMessage());
        }
        return null;
    }

    public static class MovieServiceApiResponse {
        private boolean success;
        private ShowtimeSeatLayoutResponse data;

        public MovieServiceApiResponse() {
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public ShowtimeSeatLayoutResponse getData() {
            return data;
        }

        public void setData(ShowtimeSeatLayoutResponse data) {
            this.data = data;
        }
    }
}
