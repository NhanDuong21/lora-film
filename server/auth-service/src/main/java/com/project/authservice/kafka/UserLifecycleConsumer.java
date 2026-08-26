package com.project.authservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.Account;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.RoleRepository;
import com.project.authservice.service.CredentialRevocationService;
import com.project.authservice.service.EmployeeInvitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserLifecycleConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserLifecycleConsumer.class);

    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;
    private final CredentialRevocationService revocationService;
    private final RoleRepository roleRepository;
    private final EmployeeInvitationService invitationService;

    public UserLifecycleConsumer(ObjectMapper objectMapper,
                                 AccountRepository accountRepository,
                                 CredentialRevocationService revocationService,
                                 RoleRepository roleRepository,
                                 EmployeeInvitationService invitationService) {
        this.objectMapper = objectMapper;
        this.accountRepository = accountRepository;
        this.revocationService = revocationService;
        this.roleRepository = roleRepository;
        this.invitationService = invitationService;
    }

    @KafkaListener(topics = "${app.kafka.topic.user-domain-events:user.domain.events.v1}",
            groupId = "auth-service-user-lifecycle-consumer")
    @Transactional
    public void consume(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        String eventType = event.path("eventType").asText();
        JsonNode data = event.path("data");
        if (!isCredentialLifecycleEvent(eventType)) {
            return;
        }
        if (!data.hasNonNull("accountId")) {
            throw new IllegalArgumentException("User lifecycle event is missing data.accountId");
        }
        long accountId = data.path("accountId").asLong();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Account does not exist for user lifecycle event"));
        if ("EMPLOYEE_CREATED".equals(eventType)) {
            var employeeRole = roleRepository.findByCode("EMPLOYEE")
                    .orElseThrow(() -> new IllegalStateException("EMPLOYEE role is not configured"));
            account.setRole(employeeRole);
            accountRepository.save(account);
            revocationService.revokeAll(accountId);
            log.info("Assigned EMPLOYEE role to accountId={} from employee lifecycle event", accountId);
            return;
        }
        if ("EMPLOYEE_ONBOARDING_CANCELLED".equals(eventType)) {
            account.setAccountStatus(AccountStatus.INACTIVE);
            account.setIsEnabled(false);
            accountRepository.save(account);
            invitationService.invalidate(accountId);
            revocationService.revokeAll(accountId);
            log.info("Cancelled employee invitation for accountId={}", accountId);
            return;
        }
        if ("EMPLOYEE_ONBOARDING_REOPENED".equals(eventType)) {
            account.setAccountStatus(AccountStatus.INACTIVE);
            account.setIsEnabled(false);
            accountRepository.save(account);
            revocationService.revokeAll(accountId);
            invitationService.issue(account, null);
            log.info("Reopened employee invitation for accountId={}", accountId);
            return;
        }
        AccountStatus target = targetStatus(eventType, data.path("status").asText(), account.getAccountStatus());
        if (target == null || target == account.getAccountStatus()) {
            return;
        }
        account.setAccountStatus(target);
        accountRepository.save(account);
        if (target != AccountStatus.ACTIVE) {
            revocationService.revokeAll(accountId);
        }
        log.info("Synchronized accountId={} to status={} from user lifecycle event {}",
                accountId, target, eventType);
    }

    private boolean isCredentialLifecycleEvent(String eventType) {
        return "CUSTOMER_BLOCKED".equals(eventType)
                || "CUSTOMER_UNBLOCKED".equals(eventType)
                || "EMPLOYEE_CREATED".equals(eventType)
                || "EMPLOYEE_ONBOARDING_CANCELLED".equals(eventType)
                || "EMPLOYEE_ONBOARDING_REOPENED".equals(eventType)
                || "EMPLOYEE_RESIGNED".equals(eventType)
                || "EMPLOYEE_UPDATED".equals(eventType);
    }

    private AccountStatus targetStatus(String eventType, String sourceStatus, AccountStatus current) {
        return switch (eventType) {
            case "CUSTOMER_BLOCKED" -> AccountStatus.LOCKED;
            case "CUSTOMER_UNBLOCKED" -> current == AccountStatus.LOCKED ? AccountStatus.ACTIVE : null;
            case "EMPLOYEE_RESIGNED" -> AccountStatus.INACTIVE;
            case "EMPLOYEE_UPDATED" -> switch (sourceStatus) {
                case "SUSPENDED" -> AccountStatus.LOCKED;
                case "ACTIVE" -> current == AccountStatus.LOCKED ? AccountStatus.ACTIVE : null;
                default -> null;
            };
            default -> null;
        };
    }
}
