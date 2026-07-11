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

    @Value("${app.kafka.topic.user-profile-created:user.profile.created.v1}")
    private String userProfileCreatedTopic;

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

    public void publishUserProfileCreated(Long accountId, String email, String requestId, String createdAt, String status) {
        log.info("Building USER_PROFILE_CREATED event for accountId={} with status={}", accountId, status);

        com.project.userservice.dto.UserProfileCreatedEventPayload payload = new com.project.userservice.dto.UserProfileCreatedEventPayload(accountId, email, requestId, createdAt, status);
        com.project.userservice.dto.UserProfileCreatedEvent event = new com.project.userservice.dto.UserProfileCreatedEvent(
                UUID.randomUUID().toString(),
                Instant.now().toString(),
                payload
        );

        try {
            kafkaTemplate.send(userProfileCreatedTopic, String.valueOf(accountId), event).get();
            log.info("Published USER_PROFILE_CREATED event for accountId={}", accountId);
        } catch (Exception ex) {
            log.error("Failed to publish USER_PROFILE_CREATED event for accountId={}", accountId, ex);
            throw new RuntimeException("Kafka publish failed for accountId=" + accountId, ex);
        }
    }
}
