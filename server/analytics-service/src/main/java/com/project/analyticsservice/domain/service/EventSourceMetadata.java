package com.project.analyticsservice.domain.service;

import java.time.Instant;

public record EventSourceMetadata(
        String topic,
        Integer partition,
        Long offset,
        String correlationId,
        String traceId,
        Instant receivedAt) {

    public static EventSourceMetadata unknown() {
        return new EventSourceMetadata(null, null, null, null, null, Instant.now());
    }
}
