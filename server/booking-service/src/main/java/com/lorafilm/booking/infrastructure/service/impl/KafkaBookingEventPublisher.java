package com.lorafilm.booking.infrastructure.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.service.BookingEventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
    private final String domainTopic;
    private final String bookingEventsTopic;
    private final String cancelledTopic;

    public KafkaBookingEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${booking.kafka.domain-topic:booking.domain.events.v1}") String domainTopic,
            @Value("${booking.kafka.events-topic:booking.events.v1}") String bookingEventsTopic,
            @Value("${booking.kafka.cancelled-topic:booking.booking-cancelled.v1}")
            String cancelledTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.domainTopic = domainTopic;
        this.bookingEventsTopic = bookingEventsTopic;
        this.cancelledTopic = cancelledTopic;
    }

    @Override
    public void publish(BookingOutboxEvent event) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    event.getPayload(), new TypeReference<>() {
                    });
            String eventKey = key(event);

            publishDomainEnvelope(event, payload, eventKey);
            if (isCancellation(event)) {
                publishCancellationEnvelope(event, eventKey);
            } else {
                publishLegacyBookingEvent(event, eventKey);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to publish booking outbox event", exception);
        }
    }

    private void publishDomainEnvelope(
            BookingOutboxEvent event,
            Map<String, Object> payload,
            String eventKey) throws Exception {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getEventId());
        envelope.put("eventType",
                event.getEventType().toLowerCase(Locale.ROOT).replace('_', '.'));
        envelope.put("eventVersion",
                event.getEventVersion() == null ? 1 : event.getEventVersion());
        envelope.put("occurredAt",
                event.getCreatedAt() == null ? Instant.now() : event.getCreatedAt());
        envelope.put("source", "booking-service");
        envelope.put("correlationId", value(payload, "correlationId", event.getEventId()));
        envelope.put("causationId", value(payload, "causationId", null));
        envelope.put("aggregateId", value(payload, "bookingPublicId", eventKey));
        envelope.put("userPublicId", value(payload, "userPublicId", null));
        envelope.put("locale", value(payload, "locale", "vi-VN"));
        envelope.put("payload", payload);

        kafkaTemplate.send(
                domainTopic,
                String.valueOf(envelope.get("aggregateId")),
                objectMapper.writeValueAsString(envelope))
                .get(10, TimeUnit.SECONDS);
    }

    private void publishLegacyBookingEvent(
            BookingOutboxEvent event,
            String eventKey) throws Exception {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(bookingEventsTopic, eventKey, event.getPayload());
        record.headers().add("event-id", event.getEventId().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
    }

    private void publishCancellationEnvelope(
            BookingOutboxEvent event,
            String eventKey) throws Exception {
        JsonNode booking = objectMapper.readTree(event.getPayload());
        boolean expired = "BOOKING_EXPIRED".equals(event.getEventType());
        String reason = expired
                ? "PAYMENT_TIMEOUT"
                : normalizedCancellationReason(text(booking, "cancelReasonCode"));
        Instant occurredAt = instant(booking, expired ? "expiredAt" : "cancelledAt");

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getEventId());
        envelope.put("eventType", "BOOKING_CANCELLED");
        envelope.put("eventVersion", "1.0");
        envelope.put("sourceService", "booking-service");
        envelope.put("occurredAt", occurredAt);
        envelope.put("bookingId", event.getAggregateId());
        envelope.put("bookingPublicId", firstText(
                event.getAggregatePublicId(), text(booking, "publicId")));
        envelope.put("previousStatus", expired ? "PENDING_PAYMENT" : "UNKNOWN");
        envelope.put("currentStatus", "CANCELLED");
        envelope.put("reason", reason);

        kafkaTemplate.send(
                cancelledTopic,
                eventKey,
                objectMapper.writeValueAsString(envelope))
                .get(10, TimeUnit.SECONDS);
    }

    private boolean isCancellation(BookingOutboxEvent event) {
        return "BOOKING_CANCELLED".equals(event.getEventType())
                || "BOOKING_EXPIRED".equals(event.getEventType());
    }

    private String normalizedCancellationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "SYSTEM_CANCELLED";
        }
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "USER_CANCEL", "USER_CANCELLED" -> "USER_CANCELLED";
            case "ADMIN_CANCEL", "ADMIN_CANCELLED" -> "ADMIN_CANCELLED";
            case "PAYMENT_TIMEOUT" -> "PAYMENT_TIMEOUT";
            case "PAYMENT_FAILED" -> "PAYMENT_FAILED";
            case "SYSTEM_CANCEL", "SYSTEM_CANCELLED" -> "SYSTEM_CANCELLED";
            default -> "SYSTEM_CANCELLED";
        };
    }

    private Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? Instant.now() : Instant.parse(value);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstText(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String key(BookingOutboxEvent event) {
        return event.getAggregatePublicId() == null
                ? String.valueOf(event.getAggregateId())
                : event.getAggregatePublicId();
    }

    private Object value(Map<String, Object> payload, String key, Object fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : value;
    }
}
