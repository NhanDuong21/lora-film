package com.project.analyticsservice.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentRefundedEvent(
        String eventId,
        String schemaVersion,
        String eventType,
        String sourceService,
        String paymentPublicId,
        String bookingPublicId,
        BigDecimal refundAmount,
        String currency,
        Instant refundedAt
) {
}
