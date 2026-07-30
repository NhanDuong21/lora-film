package com.lorafilm.booking.infrastructure.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.service.BookingEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "booking.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaBookingEventPublisher implements BookingEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaBookingEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${booking.kafka.domain-topic:booking.domain.events.v1}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(BookingOutboxEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    event.getPayload(), new TypeReference<>() {
                    });
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.getEventId());
            envelope.put("eventType", event.getEventType().toLowerCase(Locale.ROOT).replace('_', '.'));
            envelope.put("eventVersion", event.getEventVersion());
            envelope.put("occurredAt", event.getCreatedAt() == null ? Instant.now() : event.getCreatedAt());
            envelope.put("source", "booking-service");
            envelope.put("correlationId", value(payload, "correlationId", event.getEventId()));
            envelope.put("causationId", value(payload, "causationId", null));
            envelope.put("aggregateId", value(payload, "bookingPublicId",
                    event.getAggregatePublicId() == null
                            ? String.valueOf(event.getAggregateId()) : event.getAggregatePublicId()));
            envelope.put("userPublicId", value(payload, "userPublicId", null));
            envelope.put("locale", value(payload, "locale", "vi-VN"));
            envelope.put("payload", payload);
            kafkaTemplate.send(topic, String.valueOf(envelope.get("aggregateId")),
                    objectMapper.writeValueAsString(envelope)).get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to publish booking domain event", exception);
        }
    }

    private Object value(Map<String, Object> payload, String key, Object fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : value;
    }
}
