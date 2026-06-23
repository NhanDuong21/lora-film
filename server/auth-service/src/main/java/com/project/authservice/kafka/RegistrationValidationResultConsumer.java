package com.project.authservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.project.authservice.event.dto.RegistrationValidationResultEvent;
import com.project.authservice.service.AuthService;
import com.project.authservice.dto.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RegistrationValidationResultConsumer {
    private static final Logger log = LoggerFactory.getLogger(RegistrationValidationResultConsumer.class);

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public RegistrationValidationResultConsumer(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.registration-validation-result}", groupId = "auth-service-registration-group")
    public void consume(String message) {
        try {
            RegistrationValidationResultEvent event = objectMapper.readValue(message, RegistrationValidationResultEvent.class);
            var payload = event.getData();
            String requestId = payload.getRequestId();
            log.info("Received REGISTRATION_VALIDATION_RESULT for requestId={} status={}", requestId, payload.getStatus());

            ValidationResult result = new ValidationResult(payload.getStatus(), payload.getErrorCode(), payload.getRetryAfterSeconds());
            authService.completeValidation(requestId, result);
        } catch (Exception e) {
            log.error("Failed to parse REGISTRATION_VALIDATION_RESULT event", e);
        }
    }
}
