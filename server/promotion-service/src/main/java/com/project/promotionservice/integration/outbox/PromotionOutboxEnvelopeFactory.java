package com.project.promotionservice.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.common.json.SensitiveDataSanitizer;
import org.springframework.stereotype.Component;

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
        envelope.put("occurredAt", event.getCreatedAt().toString());
        envelope.set("data", sanitizer.sanitize(objectMapper.valueToTree(payload)));
        return objectMapper.writeValueAsString(envelope);
    }
}
