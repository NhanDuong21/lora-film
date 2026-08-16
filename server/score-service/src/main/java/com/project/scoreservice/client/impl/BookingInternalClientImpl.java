package com.project.scoreservice.client.impl;

import com.project.scoreservice.client.BookingContext;
import com.project.scoreservice.client.BookingInternalClient;
import com.project.scoreservice.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class BookingInternalClientImpl implements BookingInternalClient {

    private final RestClient restClient;
    private final String internalToken;

    public BookingInternalClientImpl(
            RestClient.Builder restClientBuilder,
            @Value("${clients.booking-service.base-url:http://localhost:8083}") String baseUrl,
            @Value("${clients.booking-service.internal-token:${SCORE_TO_BOOKING_INTERNAL_TOKEN:${APP_INTERNAL_TOKEN:8f0a00f11a51ad253c9560e55236b464bab6b20e57642c01a9c896a98ff061ff}}}") String internalToken) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public BookingContext getBookingContext(String bookingReference) {
        if (bookingReference == null || bookingReference.isBlank()) {
            throw invalidReference();
        }
        return fetch(bookingReference.trim());
    }

    private BookingContext fetch(String reference) {
        try {
            BookingApiResponse response = restClient.get()
                    .uri("/internal/bookings/{reference}/score-redemption-context", reference)
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .body(BookingApiResponse.class);
            BookingPaymentContext data = response == null ? null : response.data();
            if (data == null) {
                throw new BusinessException(
                        "Booking Service returned an empty payment context",
                        "SCORE_BOOKING_CONTEXT_UNAVAILABLE",
                        HttpStatus.BAD_GATEWAY);
            }
            return new BookingContext(
                    data.bookingId(),
                    data.bookingPublicId(),
                    data.accountId(),
                    data.bookingStatus(),
                    data.expiresAt(),
                    Boolean.TRUE.equals(data.payable()),
                    data.amount());
        } catch (RestClientResponseException exception) {
            HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                throw new BusinessException(
                        "Booking was not found",
                        "SCORE_BOOKING_NOT_FOUND",
                        HttpStatus.NOT_FOUND);
            }
            if (status == HttpStatus.CONFLICT || status == HttpStatus.GONE) {
                throw new BusinessException(
                        "Booking is not eligible for point redemption",
                        "SCORE_BOOKING_NOT_REDEEMABLE",
                        HttpStatus.CONFLICT);
            }
            throw new BusinessException(
                    "Cannot verify Booking for point redemption",
                    "SCORE_BOOKING_CONTEXT_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY);
        } catch (RestClientException exception) {
            throw new BusinessException(
                    "Cannot connect to Booking Service to verify point redemption",
                    "SCORE_BOOKING_CONTEXT_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private BusinessException invalidReference() {
        return new BusinessException(
                "Booking ID or public ID is required",
                "SCORE_BOOKING_REFERENCE_REQUIRED",
                HttpStatus.BAD_REQUEST);
    }

    private record BookingApiResponse(boolean success, BookingPaymentContext data) {
    }

    private record BookingPaymentContext(
            Long bookingId,
            String bookingPublicId,
            Long accountId,
            String bookingStatus,
            Boolean payable,
            BigDecimal amount,
            String currency,
            Instant amountLockedAt,
            Instant expiresAt) {
    }
}
