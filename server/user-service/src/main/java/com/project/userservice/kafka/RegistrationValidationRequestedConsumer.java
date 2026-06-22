package com.project.userservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.project.userservice.dto.RegistrationValidationRequestedEvent;
import com.project.userservice.dto.RegistrationValidationRequestedPayload;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.impl.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
public class RegistrationValidationRequestedConsumer {
    private static final Logger log = LoggerFactory.getLogger(RegistrationValidationRequestedConsumer.class);

    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final UserEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public RegistrationValidationRequestedConsumer(UserRepository userRepository, ReservationService reservationService, UserEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.registration-validation-requested}", groupId = "user-service-validation-group")
    public void consume(String message) {
        try {
            RegistrationValidationRequestedEvent event = objectMapper.readValue(message, RegistrationValidationRequestedEvent.class);
            RegistrationValidationRequestedPayload payload = event.getData();
            String requestId = payload.getRequestId();
            String phoneNumber = payload.getPhoneNumber();
            String cccd = payload.getCccd();

        log.info("Received REGISTRATION_VALIDATION_REQUESTED for requestId={}", requestId);

        // 1. Check DB for conflicts
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn("Validation failed: phoneNumber already exists in DB for requestId={}", requestId);
            eventPublisher.publishRegistrationValidationResult(requestId, "FAILED", "PHONE_NUMBER_ALREADY_EXISTS");
            return;
        }

        if (userRepository.existsByCccd(cccd)) {
            log.warn("Validation failed: CCCD already exists in DB for requestId={}", requestId);
            eventPublisher.publishRegistrationValidationResult(requestId, "FAILED", "CCCD_ALREADY_EXISTS");
            return;
        }

        // 2. Check and Reserve in Redis
        // TTL is 15 minutes (to match OTP expiry time)
        boolean reserved = reservationService.reserve(phoneNumber, cccd, Duration.ofMinutes(15));
        
        if (!reserved) {
            log.warn("Validation failed: phoneNumber or CCCD is already reserved in Redis for requestId={}", requestId);
            // We can return a generic conflict or be specific, but for simplicity, let's just say CONFLICT
            eventPublisher.publishRegistrationValidationResult(requestId, "FAILED", "REGISTRATION_CONFLICT");
            return;
        }

        // 3. Publish SUCCESS
        log.info("Validation successful, reservations made for requestId={}", requestId);
        eventPublisher.publishRegistrationValidationResult(requestId, "SUCCESS", null);
        } catch (Exception e) {
            log.error("Failed to parse REGISTRATION_VALIDATION_REQUESTED event", e);
        }
    }
}
