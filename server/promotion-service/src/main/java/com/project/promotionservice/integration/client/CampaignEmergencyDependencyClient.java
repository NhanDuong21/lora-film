package com.project.promotionservice.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Read-only Booking/Payment snapshots used by the force-release impact gate. */
@Component
public class CampaignEmergencyDependencyClient {

    private final RestClient bookingClient;
    private final RestClient paymentClient;

    public CampaignEmergencyDependencyClient(
            RestClient.Builder builder,
            @Value("${promotion.booking-service.url:http://localhost:8083}")
            String bookingServiceUrl,
            @Value("${promotion.booking-service.internal-token:}")
            String bookingInternalToken,
            @Value("${promotion.payment-service.url:http://localhost:8084}")
            String paymentServiceUrl,
            @Value("${promotion.payment-service.internal-token:}")
            String paymentInternalToken) {
        requireConfigured(bookingInternalToken,
                "promotion.booking-service.internal-token");
        requireConfigured(paymentInternalToken,
                "promotion.payment-service.internal-token");
        this.bookingClient = builder.clone().baseUrl(bookingServiceUrl)
                .defaultHeader("X-Internal-Token", bookingInternalToken)
                .build();
        this.paymentClient = builder.clone().baseUrl(paymentServiceUrl)
                .defaultHeader("X-Internal-Token", paymentInternalToken)
                .build();
    }

    private void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "CRITICAL SECURITY FAILURE: '" + property
                            + "' must be configured");
        }
    }

    public BookingSnapshot booking(String bookingPublicId) {
        try {
            JsonNode response = bookingClient.get()
                    .uri("/internal/bookings/{id}/lifecycle-context", bookingPublicId)
                    .retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || data.isMissingNode() || data.isNull()) {
                throw new DependencyUnavailableException("Booking Service returned no booking context");
            }
            return new BookingSnapshot(bookingPublicId,
                    data.path("bookingStatus").asText(null));
        } catch (DependencyUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException(
                    "Booking Service is unavailable", exception);
        }
    }

    public PaymentSnapshot payments(List<String> bookingPublicIds) {
        if (bookingPublicIds == null || bookingPublicIds.isEmpty()) {
            return new PaymentSnapshot(Set.of(), Set.of());
        }
        try {
            JsonNode response = paymentClient.post()
                    .uri("/internal/payments/emergency/assess")
                    .body(java.util.Map.of(
                            "bookingPublicIds", bookingPublicIds,
                            "reason", "Promotion force-release impact assessment"))
                    .retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || data.isMissingNode() || data.isNull()) {
                throw new DependencyUnavailableException("Payment Service returned no assessment");
            }
            return new PaymentSnapshot(
                    textSet(data.path("activePaymentBookingPublicIds")),
                    textSet(data.path("successfulPaymentBookingPublicIds")));
        } catch (DependencyUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException(
                    "Payment Service is unavailable", exception);
        }
    }

    private Set<String> textSet(JsonNode node) {
        Set<String> result = new LinkedHashSet<>();
        if (node != null && node.isArray()) {
            node.forEach(value -> {
                if (value.isTextual() && !value.asText().isBlank()) {
                    result.add(value.asText());
                }
            });
        }
        return Set.copyOf(result);
    }

    public record BookingSnapshot(String bookingPublicId, String bookingStatus) { }

    public record PaymentSnapshot(
            Set<String> activePaymentBookingPublicIds,
            Set<String> successfulPaymentBookingPublicIds) { }

    public static class DependencyUnavailableException extends RuntimeException {
        public DependencyUnavailableException(String message) { super(message); }
        public DependencyUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
