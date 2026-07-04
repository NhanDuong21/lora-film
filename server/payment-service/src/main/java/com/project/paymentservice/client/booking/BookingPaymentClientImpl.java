package com.project.paymentservice.client.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class BookingPaymentClientImpl implements BookingPaymentClient {

    private static final Logger logger = LoggerFactory.getLogger(BookingPaymentClientImpl.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String internalToken;

    public BookingPaymentClientImpl(
            ObjectMapper objectMapper,
            @Value("${booking.service.base-url:http://localhost:8084}") String baseUrl,
            @Value("${booking.service.internal-token:}") String internalToken,
            @Value("${booking.service.connect-timeout:5000}") int connectTimeout,
            @Value("${booking.service.read-timeout:10000}") int readTimeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
        
        if (this.internalToken == null || this.internalToken.isBlank()) {
            throw new IllegalStateException("booking.service.internal-token must be configured when Booking Service integration is enabled.");
        }
    }

    @Override
    public BookingPaymentContext getPaymentContext(Long bookingId) {
        String url = baseUrl + "/internal/bookings/" + bookingId + "/payment-context";

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofMillis(10000))
                    .header("Content-Type", "application/json");

            if (internalToken != null && !internalToken.isBlank()) {
                requestBuilder.header("X-Internal-Token", internalToken);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode == 404) {
                throw new BusinessException("BOOKING_NOT_FOUND",
                        "Booking not found: " + bookingId, HttpStatus.NOT_FOUND);
            }
            if (statusCode == 409) {
                throw new BusinessException("BOOKING_NOT_PAYABLE",
                        "Booking is not payable: " + bookingId, HttpStatus.CONFLICT);
            }
            if (statusCode == 401) {
                throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                        "Internal authentication failed with Booking Service",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            if (statusCode != 200) {
                throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                        "Booking Service returned unexpected status: " + statusCode,
                        HttpStatus.SERVICE_UNAVAILABLE);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataNode = root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                        "Booking Service returned empty data", HttpStatus.SERVICE_UNAVAILABLE);
            }

            BookingPaymentContext context = objectMapper.treeToValue(dataNode, BookingPaymentContext.class);
            validateContext(context, bookingId);
            return context;

        } catch (BusinessException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            logger.error("Booking Service timeout for bookingId={}", bookingId);
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service timed out", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (java.io.IOException | InterruptedException e) {
            logger.error("Booking Service communication error for bookingId={}", bookingId, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void validateContext(BookingPaymentContext context, Long requestedBookingId) {
        if (context.getBookingId() == null || !context.getBookingId().equals(requestedBookingId)) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service returned mismatched bookingId", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (context.getAccountId() == null || context.getAccountId() <= 0) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service returned invalid accountId", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (context.getAmount() == null || context.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service returned invalid amount", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (context.getCurrency() == null || context.getCurrency().isBlank()) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service returned invalid currency", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(context.getPayable())) {
            throw new BusinessException("BOOKING_NOT_PAYABLE",
                    "Booking is not payable", HttpStatus.CONFLICT);
        }
        if (context.getExpiresAt() == null || context.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("BOOKING_NOT_PAYABLE",
                    "Booking has expired", HttpStatus.CONFLICT);
        }
        BookingPaymentContext.AnalyticsSnapshotData snapshot = context.getAnalyticsSnapshot();
        if (snapshot == null || snapshot.getMovieId() == null || snapshot.getMovieTitle() == null
                || snapshot.getMovieTitle().isBlank() || snapshot.getTicketCount() == null
                || snapshot.getTicketCount() <= 0) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE",
                    "Booking Service returned incomplete analytics snapshot",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
