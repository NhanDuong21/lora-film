package com.project.analyticsservice.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCancelledEvent(
        String eventId,
        String eventType,
        String eventVersion,
        String sourceService,
        Instant occurredAt,
        Long bookingId,
        String bookingPublicId,
        String previousStatus,
        String currentStatus,
        String reason
) {
}
