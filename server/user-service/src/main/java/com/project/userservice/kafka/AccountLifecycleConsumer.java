package com.project.userservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.entity.User;
import com.project.userservice.entity.CustomerProfile;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.CustomerProfileRepository;
import com.project.userservice.service.UserAuditService;
import com.project.userservice.service.UserDomainEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.util.Map;
import java.time.LocalDate;

@Component
public class AccountLifecycleConsumer {
    private static final Logger log = LoggerFactory.getLogger(AccountLifecycleConsumer.class);

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService domainEventService;
    private final CustomerProfileRepository customerProfileRepository;

    public AccountLifecycleConsumer(ObjectMapper objectMapper,
                                    UserRepository userRepository,
                                    UserAuditService auditService,
                                    UserDomainEventService domainEventService,
                                    CustomerProfileRepository customerProfileRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.domainEventService = domainEventService;
        this.customerProfileRepository = customerProfileRepository;
    }

    @KafkaListener(topics = "${app.kafka.topic.account-lifecycle:auth.account.lifecycle.v1}",
            groupId = "user-service-account-lifecycle-consumer")
    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public void consume(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        String eventType = event.path("eventType").asText();
        JsonNode data = event.path("data");
        if (!eventType.startsWith("ACCOUNT_")) {
            log.debug("Ignoring non-account auth event {}", eventType);
            return;
        }
        if (!data.hasNonNull("accountId")) {
            throw new IllegalArgumentException("Account lifecycle event is missing data.accountId");
        }
        long accountId = data.path("accountId").asLong();
        if ("ACCOUNT_OAUTH_REGISTERED".equals(eventType)) {
            createOAuthCustomer(accountId, data);
            return;
        }
        User user = userRepository.findById(accountId).orElse(null);
        if (user == null) {
            throw new IllegalStateException("Profile does not exist for account lifecycle event");
        }

        UserStatus status = mapStatus(eventType, data.path("status").asText());
        if (status == null || user.getStatus() == status) {
            return;
        }
        user.setStatus(status);
        if ("ACCOUNT_DELETED".equals(eventType)) {
            user.setIsDeleted(true);
        }
        auditService.log(eventType, "USER", accountId, "Synchronized from auth-service");
        domainEventService.record("USER_STATUS_CHANGED", "USER", accountId,
                Map.of("accountId", accountId, "status", status.name()));
    }

    private void createOAuthCustomer(long accountId, JsonNode data) {
        if (!userRepository.existsById(accountId)) {
            User user = new User();
            user.setAccountId(accountId);
            String fullName = data.path("fullName").asText("Member").trim();
            if (fullName.isBlank()) {
                fullName = "Member";
            }
            user.setFullName(fullName.substring(0, Math.min(fullName.length(), 150)));
            String email = data.path("email").asText("").trim().toLowerCase(java.util.Locale.ROOT);
            user.setEmail(email.isBlank() ? null : email);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        if (!customerProfileRepository.existsByAccountId(accountId)) {
            CustomerProfile customer = new CustomerProfile();
            customer.setAccountId(accountId);
            customer.setCustomerCode("CUS" + String.format("%010d", accountId));
            customer.setJoinedAt(LocalDate.now());
            customerProfileRepository.save(customer);
        }
        auditService.log("CUSTOMER_PROFILE_CREATED", "CUSTOMER", accountId,
                "Customer profile created from OAuth account");
        domainEventService.record("CUSTOMER_CREATED", "CUSTOMER", accountId,
                Map.of("accountId", accountId, "provider", data.path("provider").asText()));
    }

    private UserStatus mapStatus(String eventType, String sourceStatus) {
        return switch (eventType) {
            case "ACCOUNT_DISABLED", "ACCOUNT_SUSPENDED" -> UserStatus.INACTIVE;
            case "ACCOUNT_LOCKED", "ACCOUNT_BLOCKED" -> UserStatus.BLOCKED;
            case "ACCOUNT_ACTIVATED", "ACCOUNT_UNLOCKED" -> UserStatus.ACTIVE;
            case "ACCOUNT_DELETED" -> UserStatus.INACTIVE;
            case "ACCOUNT_STATUS_CHANGED" -> switch (sourceStatus) {
                case "ACTIVE", "VERIFIED" -> UserStatus.ACTIVE;
                case "BLOCKED" -> UserStatus.BLOCKED;
                case "INACTIVE", "SUSPENDED" -> UserStatus.INACTIVE;
                default -> null;
            };
            default -> null;
        };
    }
}
