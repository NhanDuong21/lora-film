package com.project.userservice.kafka;

import com.project.userservice.dto.AccountVerifiedEvent;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.Gender;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.impl.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class AccountVerifiedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountVerifiedConsumer.class);
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public AccountVerifiedConsumer(UserRepository userRepository, ReservationService reservationService, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.account-verified}", groupId = "user-service-account-verified-consumer")
    @Transactional
    public void consume(String message, Acknowledgment acknowledgment) {
        try {
            AccountVerifiedEvent event = objectMapper.readValue(message, AccountVerifiedEvent.class);
            if (!"ACCOUNT_VERIFIED".equals(event.getEventType())) {
                log.warn("Ignored event type: {}", event.getEventType());
                acknowledgment.acknowledge();
                return;
            }

            if (event.getData() == null || event.getData().getAccountId() == null) {
                log.error("Invalid event data: accountId is null for eventId {}", event.getEventId());
                throw new IllegalArgumentException("Invalid event data: accountId is null");
            }

            Long accountId = event.getData().getAccountId();
            String phoneNumber = event.getData().getPhoneNumber();
            String cccd = event.getData().getCccd();

            // Duplicate event check (Idempotency)
            if (userRepository.existsById(accountId)) {
                log.warn("Duplicate event skipped. User profile already exists for accountId: {}. EventId: {}", accountId, event.getEventId());
                reservationService.release(phoneNumber, cccd);
                acknowledgment.acknowledge();
                return;
            }

            if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
                log.warn("Phone number already exists. Skipping event for accountId: {}. EventId: {}", accountId, event.getEventId());
                reservationService.release(phoneNumber, cccd);
                acknowledgment.acknowledge();
                return;
            }

            if (cccd != null && userRepository.existsByCccd(cccd)) {
                log.warn("CCCD already exists. Skipping event for accountId: {}. EventId: {}", accountId, event.getEventId());
                reservationService.release(phoneNumber, cccd);
                acknowledgment.acknowledge();
                return;
            }

            User user = new User();
            user.setAccountId(accountId);
            user.setFullName(event.getData().getFullName());
            user.setPhoneNumber(phoneNumber);
            user.setCccd(cccd);
            user.setCccdMasked(event.getData().getCccdMasked());
            user.setProvinceCode(event.getData().getProvinceCode());
            user.setProvinceName(event.getData().getProvinceName());
            
            if (event.getData().getGender() != null) {
                try {
                    user.setGender(Gender.valueOf(event.getData().getGender().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender value: {}", event.getData().getGender());
                    user.setGender(Gender.OTHER);
                }
            }

            if (event.getData().getBirthday() != null && !event.getData().getBirthday().trim().isEmpty()) {
                try {
                    user.setBirthday(LocalDate.parse(event.getData().getBirthday()));
                } catch (Exception e) {
                    log.warn("Invalid birthday format: {}", event.getData().getBirthday());
                }
            }

            if (event.getData().getBirthYear() != null) {
                user.setBirthYear(event.getData().getBirthYear());
            }

            userRepository.save(user);

            // Clean up Redis reservations
            reservationService.release(phoneNumber, cccd);

            log.info("Successfully created user profile for accountId: {}. Masked CCCD: {}", accountId, event.getData().getCccdMasked());
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing event message: {}", message, e);
            throw new RuntimeException(e); // Rethrow to trigger retry and DLQ
        }
    }
}
