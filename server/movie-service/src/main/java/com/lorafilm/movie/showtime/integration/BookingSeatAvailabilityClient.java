package com.lorafilm.movie.showtime.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class BookingSeatAvailabilityClient {

    private static final Logger log = LoggerFactory.getLogger(BookingSeatAvailabilityClient.class);

    private final RestClient restClient;

    public BookingSeatAvailabilityClient(
            @Value("${services.booking-service.url:http://localhost:8083}") String baseUrl,
            @Value("${services.booking-service.timeout-millis:1500}") long timeoutMillis,
            @Value("${app.internal-token}") String internalToken) {
        Duration timeout = Duration.ofMillis(Math.max(100, timeoutMillis));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .requestFactory(requestFactory)
                .build();
    }

    public AvailabilityResult check(Long showtimeId, List<Long> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
            return new AvailabilityResult(true, List.of());
        }
        try {
            AvailabilityResponse response = restClient.get()
                    .uri(builder -> {
                        builder.path("/internal/seat-reservations/availability")
                                .queryParam("showtimeId", showtimeId);
                        seatIds.forEach(seatId -> builder.queryParam("seatIds", seatId));
                        return builder.build();
                    })
                    .retrieve()
                    .body(AvailabilityResponse.class);
            if (response == null) {
                return new AvailabilityResult(false, List.of());
            }
            return new AvailabilityResult(true,
                    response.unavailableSeats() == null ? List.of() : List.copyOf(response.unavailableSeats()));
        } catch (Exception exception) {
            log.warn("Cannot verify booking seat availability for showtime {}: {}",
                    showtimeId, exception.getClass().getSimpleName());
            return new AvailabilityResult(false, List.of());
        }
    }

    public record AvailabilityResult(boolean verified, List<Long> unavailableSeatIds) {
    }

    private record AvailabilityResponse(boolean available, List<Long> unavailableSeats) {
    }
}
