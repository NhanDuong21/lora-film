package com.project.authservice.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.OutboxMessage;
import com.project.authservice.repository.OutboxMessageRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;

@Component
public class OutboxScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxScheduler.class);
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, Object> eventKafkaTemplate; // Assuming this bean exists
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.registration-validation-requested}")
    private String registrationValidationRequestedTopic;

    @Value("${app.kafka.topic.account-verified}")
    private String accountVerifiedTopic;

    @Value("${app.kafka.topic.account-lifecycle:auth.account.lifecycle.v1}")
    private String accountLifecycleTopic;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxMessages() {
        List<OutboxMessage> messages =
                outboxMessageRepository.findReady(LocalDateTime.now(), PageRequest.of(0, 100));
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
                    throw new IllegalArgumentException("Unsupported outbox event type " + message.getEventType());
                }
                
                message.setProcessed(true);
                message.setProcessedAt(LocalDateTime.now());
                message.setLastError(null);
            } catch (Exception e) {
                int attempt = message.getAttemptCount() + 1;
                message.setAttemptCount(attempt);
                message.setLastError(truncate(e.getMessage()));
                if (attempt >= MAX_ATTEMPTS) {
                    deadLetter(message);
                } else {
                    message.setNextAttemptAt(LocalDateTime.now().plusSeconds(1L << attempt));
                    log.warn("Outbox message id={} failed on attempt {}", message.getId(), attempt);
                }
            }
        }
    }
    
    private String determineTopic(String eventType) {
        // Map eventType to topic based on naming conventions
        if ("ACCOUNT_VERIFIED".equals(eventType)) {
            return accountVerifiedTopic;
        }
        if ("REGISTRATION_VALIDATION_REQUESTED".equals(eventType)) {
            return registrationValidationRequestedTopic;
        }
        if (eventType.startsWith("ACCOUNT_") || eventType.startsWith("ROLE_")
                || eventType.startsWith("PASSWORD_")) {
            return accountLifecycleTopic;
        }
        return null;
    }

    private void deadLetter(OutboxMessage message) {
        try {
            String topic = determineTopic(message.getEventType());
            if (topic != null) {
                eventKafkaTemplate.send(topic + ".DLT", message.getAggregateId(),
                        objectMapper.readTree(message.getPayload())).get();
            }
        } catch (Exception exception) {
            log.error("Unable to dead-letter outbox message id={}", message.getId(), exception);
        }
        message.setProcessed(true);
        message.setProcessedAt(LocalDateTime.now());
    }

    private String truncate(String value) {
        if (value == null) {
            return "Unknown publish failure";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
    public OutboxScheduler(OutboxMessageRepository outboxMessageRepository, ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }
}
