package com.lorafilm.movie.showtime.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class BookingEmergencyClosureClient {

    private final RestClient restClient;

    public BookingEmergencyClosureClient(
            @Value("${services.booking-service.url:http://localhost:8083}") String baseUrl,
            @Value("${services.booking-service.timeout-millis:5000}") long timeoutMillis,
            @Value("${app.internal-token}") String internalToken) {
        Duration timeout = Duration.ofMillis(Math.max(500, timeoutMillis));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .requestFactory(requestFactory)
                .build();
    }

    public EmergencyClosureResult closeShowtime(String showtimePublicId, String reason) {
        ApiEnvelope<EmergencyClosureResult> response = restClient.post()
                .uri("/internal/bookings/showtimes/{showtimePublicId}/emergency-close", showtimePublicId)
                .body(Map.of("reason", reason))
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Booking Service did not confirm emergency closure handling");
        }
        return response.data();
    }

    public record EmergencyClosureResult(
            String showtimePublicId,
            int releasedUnlinkedSeatCount,
            List<String> cancelledPendingBookingPublicIds,
            List<PaidBooking> cancelledPendingBookings,
            List<PaidBooking> paidBookings) {
    }

    public record PaidBooking(
            String bookingPublicId,
            String bookingCode,
            Long userId,
            String bookingStatus,
            BigDecimal finalAmount,
            String currency,
            List<String> seatLabels) {
    }

    private record ApiEnvelope<T>(boolean success, String message, T data) {
    }
}
