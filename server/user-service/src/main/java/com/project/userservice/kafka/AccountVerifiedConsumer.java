package com.project.userservice.kafka;

import com.project.userservice.dto.AccountVerifiedEvent;
import com.project.userservice.dto.AccountVerifiedPayload;
import com.project.userservice.entity.User;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.enumtype.Gender;
import com.project.userservice.enumtype.AccountType;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.service.UserDomainEventService;
import com.project.userservice.service.UserAuditService;
import com.project.userservice.service.impl.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AccountVerifiedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountVerifiedConsumer.class);
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserDomainEventService eventService;
    private final UserAuditService auditService;

    public AccountVerifiedConsumer(UserRepository userRepository,
                                   ReservationService reservationService,
                                   ObjectMapper objectMapper,
                                   CustomerProfileRepository customerProfileRepository,
                                   UserDomainEventService eventService,
                                   UserAuditService auditService) {
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
        this.customerProfileRepository = customerProfileRepository;
        this.eventService = eventService;
        this.auditService = auditService;
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

            AccountVerifiedPayload payload = event.getData();
            if (payload == null || payload.getAccountId() == null) {
                log.error("Invalid event data: accountId is null for eventId {}", event.getEventId());
                throw new IllegalArgumentException("Invalid event data: accountId is null");
            }

            Long accountId = event.getData().getAccountId();
            String phoneNumber = event.getData().getPhoneNumber();
            String cccd = event.getData().getCccd();
            String requestId = event.getData().getRequestId();
            String reservationOwner = payload.getEmail() == null
                    ? requestId
                    : payload.getEmail().trim().toLowerCase(java.util.Locale.ROOT);

            // Duplicate event check (Idempotency)
            User existingUser = userRepository.findById(accountId).orElse(null);
            if (existingUser != null) {
                boolean profileChanged = false;
                if ((existingUser.getEmail() == null || existingUser.getEmail().isBlank())
                        && payload.getEmail() != null && !payload.getEmail().isBlank()) {
                    existingUser.setEmail(payload.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
                    profileChanged = true;
                }
                if ((existingUser.getFullName() == null || existingUser.getFullName().isBlank())
                        && payload.getFullName() != null && !payload.getFullName().isBlank()) {
                    existingUser.setFullName(payload.getFullName().trim());
                    profileChanged = true;
                }
                if ((existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank())
                        && payload.getAvatarUrl() != null && !payload.getAvatarUrl().isBlank()) {
                    existingUser.setAvatarUrl(payload.getAvatarUrl().trim());
                    profileChanged = true;
                }
                if (existingUser.getAccountType() == null) {
                    existingUser.setAccountType(accountType(payload.getRole()));
                    profileChanged = true;
                }
                if (profileChanged) {
                    userRepository.save(existingUser);
                    log.info("Backfilled OAuth profile fields for existing accountId: {}", accountId);
                }
                ensureCustomerProfile(accountId, payload.getRole());
                recordProfileCreated(accountId, payload.getEmail(), requestId, "SUCCESS");
                releaseReservationAfterCommit(phoneNumber, cccd, reservationOwner);
                log.info("Idempotently handled existing user profile for accountId={}", accountId);
                acknowledgment.acknowledge();
                return;
            }

            if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
                recordProfileCreated(accountId, payload.getEmail(), requestId, "FAILED");
                releaseReservationAfterCommit(phoneNumber, cccd, reservationOwner);
                acknowledgment.acknowledge();
                return;
            }

            if (cccd != null && userRepository.existsByCccd(cccd)) {
                recordProfileCreated(accountId, payload.getEmail(), requestId, "FAILED");
                releaseReservationAfterCommit(phoneNumber, cccd, reservationOwner);
                acknowledgment.acknowledge();
                return;
            }

            User user = new User();
            user.setAccountId(accountId);
            user.setFullName(resolveFullName(payload));
            if (event.getData().getEmail() != null) {
                user.setEmail(event.getData().getEmail().trim().toLowerCase(java.util.Locale.ROOT));
            }
            if (payload.getAvatarUrl() != null && !payload.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(payload.getAvatarUrl().trim());
            }
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
            user.setStatus(initialStatus(payload));
            user.setAccountType(accountType(payload.getRole()));

            userRepository.save(user);
            boolean customerProfileCreated = ensureCustomerProfile(accountId, payload.getRole());
            auditService.log(customerProfileCreated ? "CUSTOMER_PROFILE_CREATED" : "USER_PROFILE_CREATED",
                    customerProfileCreated ? "CUSTOMER" : "USER", accountId,
                    customerProfileCreated
                            ? "Created from verified customer account"
                            : "Created from verified workforce account");

            recordProfileCreated(accountId, event.getData().getEmail(), requestId, "SUCCESS");
            releaseReservationAfterCommit(phoneNumber, cccd, reservationOwner);

            log.info("Successfully created user profile for accountId: {}. Masked CCCD: {}", accountId, event.getData().getCccdMasked());
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing ACCOUNT_VERIFIED event", e);
            throw new IllegalStateException("Account verified event processing failed", e);
        }
    }

    private boolean ensureCustomerProfile(Long accountId, String role) {
        if (!isCustomerRole(role)) {
            log.info("Skipping customer profile for workforce accountId={} role={}", accountId, role);
            return false;
        }
        if (customerProfileRepository.existsByAccountId(accountId)) {
            return true;
        }
        CustomerProfile customer = new CustomerProfile();
        customer.setAccountId(accountId);
        customer.setCustomerCode("CUS" + String.format("%010d", accountId));
        customer.setJoinedAt(LocalDate.now());
        customerProfileRepository.save(customer);
        return true;
    }

    private boolean isCustomerRole(String role) {
        if (role == null || role.isBlank()) {
            // Backwards compatibility for older ACCOUNT_VERIFIED producers.
            return true;
        }
        String normalized = role.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.equals("CUSTOMER") || normalized.equals("MEMBER") || normalized.equals("USER");
    }

    private AccountType accountType(String role) {
        return isCustomerRole(role) ? AccountType.CUSTOMER : AccountType.WORKFORCE;
    }

    private UserStatus initialStatus(AccountVerifiedPayload payload) {
        if (accountType(payload.getRole()) == AccountType.WORKFORCE
                && "INACTIVE".equalsIgnoreCase(payload.getAccountStatus())) {
            return UserStatus.INACTIVE;
        }
        return UserStatus.ACTIVE;
    }

    private String resolveFullName(AccountVerifiedPayload payload) {
        if (payload.getFullName() != null && !payload.getFullName().isBlank()) {
            return payload.getFullName().trim();
        }
        String email = payload.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "User";
    }

    private void recordProfileCreated(Long accountId, String email, String requestId, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountId", accountId);
        data.put("email", email);
        data.put("requestId", requestId);
        data.put("createdAt", Instant.now().toString());
        data.put("status", status);
        eventService.record("USER_PROFILE_CREATED", "USER", accountId, data);
    }

    private void releaseReservationAfterCommit(String phone, String cccd, String requestId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            reservationService.release(phone, cccd, requestId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reservationService.release(phone, cccd, requestId);
            }
        });
    }
}
