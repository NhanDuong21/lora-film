package com.project.promotionservice.integration.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "promotion.integration.consumers-enabled",
        havingValue = "true", matchIfMissing = false)
public class IntegrationKafkaConsumers {
    private final ObjectMapper objectMapper;
    private final IntegrationEventService events;
    private final IntegrationEventStateService state;

    public IntegrationKafkaConsumers(ObjectMapper objectMapper,
                                     IntegrationEventService events,
                                     IntegrationEventStateService state) {
        this.objectMapper = objectMapper;
        this.events = events;
        this.state = state;
    }

    @KafkaListener(
            topics = "#{'${promotion.integration.consume-topics:booking.events.v1,payment.events,notification.events,score.events,movie.events}'.split(',')}",
            groupId = "${spring.kafka.consumer.group-id:promotion-service-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record) {
        EventHeaders headers;
        try {
            headers = parse(record);
        } catch (RuntimeException malformed) {
            // Persist malformed envelopes as DLQ records before acknowledging
            // the Kafka offset. This keeps the event inspectable and prevents a
            // poison message from being silently discarded.
            String eventId = valueOrGenerated(header(record, "X-Event-ID"),
                    record.topic() + ":" + record.partition() + ":" + record.offset());
            String publicId = events.receive(source(record.topic()), eventId, "INVALID_EVENT",
                    valueOrGenerated(header(record, "X-Event-Schema-Version"), "1.0"),
                    valueOrGenerated(header(record, "X-Correlation-ID"), eventId),
                    valueOrGenerated(header(record, "X-Trace-ID"), eventId),
                    safePayload(record.value()));
            state.markDeadLetter(publicId, malformed.getMessage());
            return;
        }
        String publicId = events.receive(source(record.topic()), headers.eventId(), headers.eventType(),
                headers.schemaVersion(), headers.correlationId(), headers.traceId(),
                safePayload(record.value()));
        events.process(publicId);
    }

    @KafkaListener(
            topics = "#{'${promotion.integration.dlq-topics:booking.events.dlq,payment.events.dlq,notification.events.dlq,score.events.dlq,movie.events.dlq}'.split(',')}",
            groupId = "${spring.kafka.consumer.dlq-group-id:promotion-service-dlq-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeDeadLetter(ConsumerRecord<String, String> record) {
        String eventId = header(record, "X-Event-ID");
        if (eventId == null || eventId.isBlank()) eventId = UUID.randomUUID().toString();
        String eventType = header(record, "X-Event-Type");
        if (eventType == null || eventType.isBlank()) eventType = "UNKNOWN";
        String schema = header(record, "X-Event-Schema-Version");
        if (schema == null || schema.isBlank()) schema = "1.0";
        String publicId = events.receive("KAFKA_DLQ", eventId, eventType, schema,
                valueOrGenerated(header(record, "X-Correlation-ID"), eventId),
                valueOrGenerated(header(record, "X-Trace-ID"), eventId), safePayload(record.value()));
        state.markDeadLetter(publicId, "Kafka dead-letter topic: " + record.topic());
    }

    private EventHeaders parse(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String eventId = valueOr(root, "eventId", header(record, "X-Event-ID"));
            String eventType = valueOr(root, "eventType", header(record, "X-Event-Type"));
            String schema = valueOr(root, "schemaVersion", header(record, "X-Event-Schema-Version"));
            String correlation = valueOr(root, "correlationId", header(record, "X-Correlation-ID"));
            String trace = valueOr(root, "traceId", header(record, "X-Trace-ID"));
            if (blank(eventId) || blank(eventType) || blank(schema)
                    || blank(correlation) || blank(trace)) {
                throw new IllegalArgumentException("Event requires id, type, schemaVersion, correlationId and traceId");
            }
            if (!"1.0".equals(schema) && !"v1".equalsIgnoreCase(schema)) {
                throw new IllegalArgumentException("Unsupported event schema version: " + schema);
            }
            return new EventHeaders(eventId, eventType, schema, correlation, trace);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Kafka event envelope", exception);
        }
    }

    private String source(String topic) {
        String normalized = topic == null ? "UNKNOWN" : topic.toUpperCase();
        if (normalized.contains("BOOKING")) return "BOOKING_SERVICE";
        if (normalized.contains("PAYMENT")) return "PAYMENT_SERVICE";
        if (normalized.contains("NOTIFICATION")) return "NOTIFICATION_SERVICE";
        if (normalized.contains("SCORE")) return "SCORE_SERVICE";
        if (normalized.contains("MOVIE")) return "MOVIE_SERVICE";
        return "UNKNOWN_SERVICE";
    }

    private String valueOr(JsonNode root, String field, String fallback) {
        JsonNode node = root == null ? null : root.get(field);
        return node != null && !node.isNull() ? node.asText() : fallback;
    }

    private String valueOrGenerated(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String safePayload(String payload) {
        if (payload == null || payload.isBlank()) return "{}";
        try {
            objectMapper.readTree(payload);
            return payload;
        } catch (Exception invalidJson) {
            try {
                return objectMapper.createObjectNode()
                        .put("rawPayload", payload.length() > 10000
                                ? payload.substring(0, 10000) : payload)
                        .toString();
            } catch (RuntimeException ignored) {
                return "{}";
            }
        }
    }
    private record EventHeaders(String eventId, String eventType, String schemaVersion,
                                String correlationId, String traceId) {}
}
