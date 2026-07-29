package com.project.promotionservice.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.common.json.SensitiveDataSanitizer;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import java.util.UUID;

@Component
public class PromotionOutboxEnvelopeFactory {

    private final ObjectMapper objectMapper;
    private final SensitiveDataSanitizer sanitizer;

    public PromotionOutboxEnvelopeFactory(
            ObjectMapper objectMapper,
            SensitiveDataSanitizer sanitizer) {
        this.objectMapper = objectMapper;
        this.sanitizer = sanitizer;
    }

    public String create(PromotionOutboxEvent event, Object payload)
            throws JsonProcessingException {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("schemaVersion", "1.0");
        envelope.put("eventId", event.getPublicId());
        envelope.put("eventType", event.getEventType());
        envelope.put("aggregateType", event.getAggregateType());
        envelope.put("aggregatePublicId", event.getAggregatePublicId());
        // Correlation and trace identifiers are mandatory for consumers. The
        // HTTP filter supplies them for requests; scheduled/background work
        // gets a stable generated value instead of publishing an incomplete
        // envelope.
        envelope.put("correlationId", valueOrGenerated("correlationId"));
        envelope.put("traceId", valueOrGenerated("traceId"));
        envelope.put("occurredAt", event.getCreatedAt().toString());
        envelope.set("data", sanitizer.sanitize(objectMapper.valueToTree(payload)));
        return objectMapper.writeValueAsString(envelope);
    }

    private String valueOrGenerated(String key) {
        String value = MDC.get(key);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
