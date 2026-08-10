package com.project.promotionservice.integration.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class PromotionEventPublisherImpl implements PromotionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long acknowledgementTimeoutSeconds;
    private final ObjectMapper objectMapper;

    @Autowired
    public PromotionEventPublisherImpl(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${promotion.outbox.kafka-ack-timeout-seconds:10}")
            long acknowledgementTimeoutSeconds,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.acknowledgementTimeoutSeconds = Math.max(1, acknowledgementTimeoutSeconds);
        this.objectMapper = objectMapper;
    }

    /**
     * Kept for small unit tests and embedders that instantiate the publisher
     * directly rather than through Spring.
     */
    public PromotionEventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate,
                                       long acknowledgementTimeoutSeconds) {
        this(kafkaTemplate, acknowledgementTimeoutSeconds, new ObjectMapper());
    }

    @Override
    public void publish(PromotionOutboxEvent event) {
        String topic = event.getTopicName();
        String key = event.getEventKey();
        String value = event.getPayload();

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        // Standard trace and correlation headers propagation to Kafka
        if (event.getPublicId() != null) {
            record.headers().add("X-Event-ID", event.getPublicId().getBytes(StandardCharsets.UTF_8));
        }
        record.headers().add("X-Event-Type", event.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Event-Schema-Version", "1.0".getBytes(StandardCharsets.UTF_8));
        addEnvelopeHeader(record, "correlationId", "X-Correlation-ID", value);
        addEnvelopeHeader(record, "traceId", "X-Trace-ID", value);

        try {
            kafkaTemplate.send(record).get(acknowledgementTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Kafka acknowledgement", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka broker did not acknowledge promotion event", exception);
        }
    }

    private void addEnvelopeHeader(ProducerRecord<String, String> record,
                                   String field, String header, String payload) {
        try {
            JsonNode value = objectMapper.readTree(payload).get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                record.headers().add(header, value.asText().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {
            // The outbox payload is validated before persistence. Header
            // propagation is best-effort and must not prevent publication.
        }
    }
}
