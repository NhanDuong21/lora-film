package com.project.promotionservice.integration.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class PromotionEventPublisherImpl implements PromotionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long acknowledgementTimeoutSeconds;

    public PromotionEventPublisherImpl(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${promotion.outbox.kafka-ack-timeout-seconds:10}")
            long acknowledgementTimeoutSeconds) {
        this.kafkaTemplate = kafkaTemplate;
        this.acknowledgementTimeoutSeconds = Math.max(1, acknowledgementTimeoutSeconds);
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

        try {
            kafkaTemplate.send(record).get(acknowledgementTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Kafka acknowledgement", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka broker did not acknowledge promotion event", exception);
        }
    }
}
