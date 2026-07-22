package com.lorafilm.booking.infrastructure.client.impl;

import com.lorafilm.booking.infrastructure.client.MovieServiceClient;
import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MovieServiceClientImpl implements MovieServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String movieServiceUrl;

    public MovieServiceClientImpl(
            RestTemplate restTemplate,
            @Value("${movie.service.url:http://movie-service}") String movieServiceUrl) {
        this.restTemplate = restTemplate;
        this.movieServiceUrl = movieServiceUrl;
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
            MovieServiceApiResponse wrappedResponse = restTemplate.getForObject(url, MovieServiceApiResponse.class);
            if (wrappedResponse != null && wrappedResponse.getData() != null) {
                return wrappedResponse.getData();
            }
            // Fallback for direct un-wrapped response if needed
            ShowtimeSeatLayoutResponse directResponse = restTemplate.getForObject(url, ShowtimeSeatLayoutResponse.class);
            if (directResponse != null) {
                return directResponse;
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
