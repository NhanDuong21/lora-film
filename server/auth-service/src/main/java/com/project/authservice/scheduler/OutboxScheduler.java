package com.project.authservice.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.OutboxMessage;
import com.project.authservice.repository.OutboxMessageRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, Object> eventKafkaTemplate; // Assuming this bean exists
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> messages = outboxMessageRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();
        if (messages.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox messages", messages.size());

        for (OutboxMessage message : messages) {
            try {
                // Determine topic name from eventType
                String topicName = determineTopic(message.getEventType());
                if (topicName != null) {
                    JsonNode payloadNode = objectMapper.readTree(message.getPayload());
                    eventKafkaTemplate.send(topicName, message.getAggregateId(), payloadNode).get();
                    log.info("Published outbox message id={} to topic={}", message.getId(), topicName);
                } else {
                    log.warn("Unknown event type: {}", message.getEventType());
                }
                
                message.setProcessed(true);
            } catch (Exception e) {
                log.error("Failed to process outbox message {}", message.getId(), e);
            }
        }
    }
    
    private String determineTopic(String eventType) {
        // Map eventType to topic based on naming conventions
        if ("ACCOUNT_VERIFIED".equals(eventType)) {
            return "account-verified";
        }
        if ("REGISTRATION_VALIDATION_REQUESTED".equals(eventType)) {
            return "registration-validation-requested";
        }
        // Fallback to lowercase hyphen-separated
        return eventType.toLowerCase().replace('_', '-');
    }
    public OutboxScheduler(OutboxMessageRepository outboxMessageRepository, org.springframework.kafka.core.KafkaTemplate<String, Object> eventKafkaTemplate, ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.eventKafkaTemplate = eventKafkaTemplate;
        this.objectMapper = objectMapper;
    }
}
