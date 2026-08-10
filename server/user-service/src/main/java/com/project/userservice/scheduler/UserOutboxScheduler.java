package com.project.userservice.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.entity.OutboxMessage;
import com.project.userservice.repository.OutboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class UserOutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(UserOutboxScheduler.class);
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxMessageRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String domainTopic;
    private final String userProfileCreatedTopic;

    public UserOutboxScheduler(OutboxMessageRepository repository,
                               KafkaTemplate<String, Object> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${app.kafka.topic.user-domain-events:user.domain.events.v1}") String domainTopic,
                               @Value("${app.kafka.topic.user-profile-created:user.profile.created.v1}")
                               String userProfileCreatedTopic) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.domainTopic = domainTopic;
        this.userProfileCreatedTopic = userProfileCreatedTopic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void publishReadyMessages() {
        List<OutboxMessage> messages = repository.findReady(LocalDateTime.now(), PageRequest.of(0, 100));
        for (OutboxMessage message : messages) {
            publish(message);
        }
    }

    private void publish(OutboxMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String topic = "USER_PROFILE_CREATED".equals(message.getEventType())
                    ? userProfileCreatedTopic
                    : domainTopic;
            kafkaTemplate.send(topic, message.getAggregateId(), payload).get(10, TimeUnit.SECONDS);
            message.setProcessed(true);
            message.setProcessedAt(LocalDateTime.now());
            message.setLastError(null);
        } catch (Exception exception) {
            int attempt = message.getAttemptCount() + 1;
            if (attempt >= MAX_ATTEMPTS && publishToDeadLetter(message, exception)) {
                return;
            }
            message.setAttemptCount(attempt);
            message.setLastError(truncate(exception.getMessage()));
            long backoffSeconds = Math.min(300L, 1L << Math.min(attempt, 8));
            message.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            if (attempt >= MAX_ATTEMPTS) {
                log.error("Outbox message {} reached {} failed delivery attempts", message.getId(), attempt, exception);
            } else {
                log.warn("Outbox message {} delivery failed; retry {} scheduled in {} seconds",
                        message.getId(), attempt, backoffSeconds);
            }
        }
    }

    private boolean publishToDeadLetter(OutboxMessage message, Exception originalFailure) {
        try {
            kafkaTemplate.send(
                    ("USER_PROFILE_CREATED".equals(message.getEventType())
                            ? userProfileCreatedTopic : domainTopic) + ".dlq",
                    message.getAggregateId(),
                    message.getPayload()).get(10, TimeUnit.SECONDS);
            message.setAttemptCount(MAX_ATTEMPTS);
            message.setProcessed(true);
            message.setProcessedAt(LocalDateTime.now());
            message.setLastError(truncate("Dead-lettered after " + MAX_ATTEMPTS
                    + " attempts: " + originalFailure.getMessage()));
            log.error("Outbox message {} moved to dead-letter topic after {} attempts",
                    message.getId(), MAX_ATTEMPTS, originalFailure);
            return true;
        } catch (Exception deadLetterFailure) {
            log.error("Unable to dead-letter outbox message {}", message.getId(), deadLetterFailure);
            return false;
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "Unknown Kafka publishing failure";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
