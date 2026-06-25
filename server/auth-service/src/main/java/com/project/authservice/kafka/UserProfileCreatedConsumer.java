package com.project.authservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.Account;
import com.project.authservice.event.dto.UserProfileCreatedEvent;
import com.project.authservice.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserProfileCreatedConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserProfileCreatedConsumer.class);

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public UserProfileCreatedConsumer(AccountRepository accountRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.user-profile-created:user.profile.created.v1}", groupId = "auth-service-user-profile-created-consumer")
    @Transactional
    public void consume(String message) {
        try {
            UserProfileCreatedEvent event = objectMapper.readValue(message, UserProfileCreatedEvent.class);
            if (!"USER_PROFILE_CREATED".equals(event.getEventType())) {
                log.warn("Ignored event type: {}", event.getEventType());
                return;
            }

            if (event.getData() == null || event.getData().getAccountId() == null) {
                log.error("Invalid event data: accountId is null for eventId {}", event.getEventId());
                throw new IllegalArgumentException("Invalid event data: accountId is null");
            }

            Long accountId = event.getData().getAccountId();
            
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                log.warn("Account not found for accountId {}. Cannot activate.", accountId);
                return;
            }

            if (account.getRegistrationCompleted() != null && account.getRegistrationCompleted() == 1) {
                if (account.getIsActive() != null && account.getIsActive() == 1) {
                    log.info("Account {} is already active.", accountId);
                } else {
                    account.setIsActive(1);
                    accountRepository.save(account);
                    log.info("Account {} successfully activated.", accountId);
                }
            } else {
                log.warn("Account {} registration is not completed yet. Throwing exception to trigger retry.", accountId);
                throw new RuntimeException("Account " + accountId + " registration is not completed. Retrying...");
            }

            String pendingKey = "pending_registration:" + account.getEmail();
            redisTemplate.delete(pendingKey);

        } catch (Exception e) {
            log.error("Error processing USER_PROFILE_CREATED event message: {}", message, e);
            throw new RuntimeException(e);
        }
    }
}
