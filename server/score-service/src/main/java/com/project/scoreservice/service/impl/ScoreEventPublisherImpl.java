package com.project.scoreservice.service.impl;

import com.project.scoreservice.entity.OutboxEvent;
import com.project.scoreservice.service.ScoreEventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class ScoreEventPublisherImpl implements ScoreEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScoreEventPublisherImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ScoreEventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(OutboxEvent event) {
        String topic = getTopicForEventType(event.getEventType());
        String key = event.getAggregateId();
        String value = event.getPayload();

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        if (event.getEventId() != null) {
            record.headers().add("X-Event-ID", event.getEventId().getBytes(StandardCharsets.UTF_8));
        }
        if (event.getCorrelationId() != null) {
            record.headers().add("X-Correlation-ID", event.getCorrelationId().getBytes(StandardCharsets.UTF_8));
        }
        record.headers().add("X-Event-Type", event.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Schema-Version", String.valueOf(event.getEventVersion()).getBytes(StandardCharsets.UTF_8));

        log.debug("Publishing outbox event [{}] of type [{}] to topic [{}] with key [{}]",
                event.getEventId(), event.getEventType(), topic, key);

        kafkaTemplate.send(record);
    }

    private String getTopicForEventType(String eventType) {
        if (eventType == null) return "score.events";
        return switch (eventType.toUpperCase()) {
            case "POINT_EARNED" -> "score.earned";
            case "POINT_REDEEMED" -> "score.redeemed";
            case "POINT_HELD" -> "score.held";
            case "POINT_COMMITTED" -> "score.committed";
            case "POINT_RELEASED" -> "score.released";
            case "POINT_REFUNDED" -> "score.refunded";
            case "POINT_REVOKED" -> "score.revoked";
            case "POINT_EXPIRED" -> "score.expired";
            case "POINT_ADJUSTED" -> "score.adjusted";
            case "TIER_UPGRADED" -> "score.tier.upgraded";
            case "TIER_DOWNGRADED" -> "score.tier.downgraded";
            default -> "score.events";
        };
    }
}
