package com.project.userservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.project.userservice.dto.RegistrationValidationResultEvent;
import com.project.userservice.dto.RegistrationValidationResultPayload;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.registration-validation-result}")
    private String registrationValidationResultTopic;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRegistrationValidationResult(String requestId, String status, String errorCode, Long retryAfterSeconds) {
        log.info("Building REGISTRATION_VALIDATION_RESULT event for requestId={} status={}", requestId, status);

        RegistrationValidationResultPayload payload = new RegistrationValidationResultPayload(requestId, status, errorCode, retryAfterSeconds);
        RegistrationValidationResultEvent event = new RegistrationValidationResultEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                payload
        );

        try {
            kafkaTemplate.send(registrationValidationResultTopic, requestId, event).get();
            log.info("Published REGISTRATION_VALIDATION_RESULT event for requestId={}", requestId);
        } catch (Exception ex) {
            log.error("Failed to publish REGISTRATION_VALIDATION_RESULT event for requestId={}", requestId, ex);
            throw new RuntimeException("Kafka publish failed for requestId=" + requestId, ex);
        }
    }
}
