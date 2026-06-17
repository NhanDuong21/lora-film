package com.project.userservice.kafka;

import com.project.userservice.dto.AccountCreatedEvent;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.Gender;
import com.project.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class AccountCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountCreatedConsumer.class);
    private final UserRepository userRepository;

    public AccountCreatedConsumer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "auth.account.created.v1", groupId = "user-service-account-created-consumer")
    @Transactional
    public void consume(AccountCreatedEvent event, Acknowledgment acknowledgment) {
        try {
            if (!"ACCOUNT_CREATED".equals(event.getEventType())) {
                log.warn("Ignored event type: {}", event.getEventType());
                acknowledgment.acknowledge();
                return;
            }

            if (event.getData() == null || event.getData().getAccountId() == null) {
                log.error("Invalid event data: accountId is null for eventId {}", event.getEventId());
                throw new IllegalArgumentException("Invalid event data: accountId is null");
            }

            Long accountId = event.getData().getAccountId();
            
            // Duplicate event check (Idempotency)
            if (userRepository.existsById(accountId)) {
                log.warn("Duplicate event skipped. User profile already exists for accountId: {}. EventId: {}", accountId, event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            User user = new User();
            user.setAccountId(accountId);
            user.setFullName(event.getData().getFullName());
            user.setPhoneNumber(event.getData().getPhoneNumber());
            user.setCccd(event.getData().getCccd());
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

            user.setBirthYear(event.getData().getBirthYear());

            userRepository.save(user);

            log.info("Successfully created user profile for accountId: {}. Masked CCCD: {}", accountId, event.getData().getCccdMasked());
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing event: {}", event.getEventId(), e);
            throw e; // Rethrow to trigger retry and DLQ
        }
    }
}
