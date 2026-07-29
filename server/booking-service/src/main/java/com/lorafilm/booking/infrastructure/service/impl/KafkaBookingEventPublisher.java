package com.lorafilm.booking.infrastructure.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.service.BookingEventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@Profile("!mock-booking-events")
public class KafkaBookingEventPublisher implements BookingEventPublisher {
    private static final Set<String> SUPPORTED_REASONS = Set.of(
            "USER_CANCELLED", "PAYMENT_TIMEOUT", "ADMIN_CANCELLED",
            "SYSTEM_CANCELLED", "PAYMENT_FAILED");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String bookingEventsTopic;
    private final String cancelledTopic;

    public KafkaBookingEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${booking.kafka.events-topic:booking.events.v1}") String bookingEventsTopic,
            @Value("${booking.kafka.cancelled-topic:booking.booking-cancelled.v1}")
            String cancelledTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.bookingEventsTopic = bookingEventsTopic;
        this.cancelledTopic = cancelledTopic;
    }

    @Override
    public void publish(BookingOutboxEvent event) {
        try {
            boolean cancellation = "BOOKING_CANCELLED".equals(event.getEventType())
                    || "BOOKING_EXPIRED".equals(event.getEventType());
            String topic = cancellation ? cancelledTopic : bookingEventsTopic;
            String payload = cancellation
                    ? cancellationEnvelope(event) : event.getPayload();
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topic, key(event), payload);
            record.headers().add(
                    "event-id", event.getEventId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            kafkaTemplate.send(record).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot publish booking outbox event", exception);
        }
    }

    private String cancellationEnvelope(BookingOutboxEvent event) throws Exception {
        JsonNode booking = objectMapper.readTree(event.getPayload());
        boolean expired = "BOOKING_EXPIRED".equals(event.getEventType());
        String reason = expired
                ? "PAYMENT_TIMEOUT"
                : normalizedReason(text(booking, "cancelReasonCode"));
        Instant occurredAt = instant(
                booking, expired ? "expiredAt" : "cancelledAt");

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
        return objectMapper.writeValueAsString(envelope);
    }

    private String normalizedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "SYSTEM_CANCELLED";
        }
        String normalized = reason.trim().toUpperCase();
        return SUPPORTED_REASONS.contains(normalized)
                ? normalized : "SYSTEM_CANCELLED";
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
}
