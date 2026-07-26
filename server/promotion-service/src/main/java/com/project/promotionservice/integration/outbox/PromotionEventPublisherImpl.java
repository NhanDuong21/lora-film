package com.project.promotionservice.integration.outbox;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class PromotionEventPublisherImpl implements PromotionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PromotionEventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
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

        kafkaTemplate.send(record);
    }
}
