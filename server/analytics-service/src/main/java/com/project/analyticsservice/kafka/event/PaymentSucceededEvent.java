package com.project.analyticsservice.kafka.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentSucceededEvent(
        String eventId,
        String schemaVersion,
        String paymentPublicId,
        String bookingPublicId,
        String provider,
        BigDecimal amount,
        String currency,
        Instant succeededAt,
        Long movieId,
        String moviePublicId,
        String movieTitle,
        String showtimePublicId,
        String cinemaPublicId,
        String cinemaName,
        String auditoriumPublicId,
        Integer ticketCount,
        Integer availableSeats,
        BigDecimal ticketAmount,
        BigDecimal foodAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        @JsonAlias("userId") String userPublicId,
        @JsonAlias("promotionId") String promotionPublicId,
        String promotionName,
        String membershipTier,
        String paymentMethod,
        Instant showtimeStartsAt,
        String format
) {
}
