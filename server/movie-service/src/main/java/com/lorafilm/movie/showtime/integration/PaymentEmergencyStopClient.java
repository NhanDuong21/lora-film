package com.lorafilm.movie.showtime.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class PaymentEmergencyStopClient {

    private final RestClient restClient;

    public PaymentEmergencyStopClient(
            @Value("${payment.service.base-url:http://localhost:8084}") String baseUrl,
            @Value("${payment.service.connect-timeout-millis:5000}") long connectTimeoutMillis,
            @Value("${payment.service.read-timeout-millis:10000}") long readTimeoutMillis,
            @Value("${payment.service.internal-token:}") String internalToken) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, connectTimeoutMillis)))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(500, readTimeoutMillis)));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .requestFactory(requestFactory)
                .build();
    }

    public EmergencyPaymentStopResult stopPendingPayments(
            List<String> bookingPublicIds,
            String reason) {
        if (bookingPublicIds == null || bookingPublicIds.isEmpty()) {
            return new EmergencyPaymentStopResult(0, List.of());
        }
        ApiEnvelope<EmergencyPaymentStopResult> response = restClient.post()
                .uri("/internal/payments/emergency/stop")
                .body(Map.of("bookingPublicIds", bookingPublicIds, "reason", reason))
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Payment Service did not confirm pending-payment stop");
        }
        return response.data();
    }

    public record EmergencyPaymentStopResult(
            int stoppedPaymentAttemptCount,
            List<String> alreadySuccessfulBookingPublicIds) {
    }

    private record ApiEnvelope<T>(boolean success, String message, T data) {
    }
}
