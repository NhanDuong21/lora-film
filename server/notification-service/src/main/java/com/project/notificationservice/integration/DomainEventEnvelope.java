package com.project.notificationservice.integration;

import java.time.Instant;
import java.util.Map;

public record DomainEventEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String source,
        String correlationId,
        String causationId,
        String aggregateId,
        String userPublicId,
        String locale,
        Map<String, Object> payload) {
}
