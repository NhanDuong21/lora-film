package com.project.authservice.event.publisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.entity.Account;
import com.project.authservice.event.dto.AccountCreatedEvent;
import com.project.authservice.event.dto.AccountCreatedEventData;

/**
 * Publishes {@code ACCOUNT_CREATED} events to Kafka after a successful
 * account registration transaction.
 *
 * <p><strong>Security rules enforced here:</strong>
 * <ul>
 *   <li>Raw CCCD is NEVER written to any log statement.</li>
 *   <li>Password / password hash is NEVER included in the event payload.</li>
 * </ul>
 */
@Service
public class AuthAccountEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthAccountEventPublisher.class);

    private static final String EVENT_TYPE    = "ACCOUNT_CREATED";
    private static final String EVENT_VERSION = "1.0";
    private static final String EVENT_SOURCE  = "auth-service";

    private final KafkaTemplate<String, AccountCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.account-created}")
    private String accountCreatedTopic;

    public AuthAccountEventPublisher(KafkaTemplate<String, AccountCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Builds and publishes an {@link AccountCreatedEvent} to Kafka.
     *
     * <p>This method must be called <em>after</em> the database transaction has
     * committed successfully. If Kafka delivery fails, the account creation is
     * NOT rolled back – the error is logged and a {@link RuntimeException} is
     * thrown so callers can decide on compensating action.
     *
     * @param savedAccount  persisted {@link Account} entity (ID must be non-null)
     * @param request       original registration request (used for profile fields)
     * @param cccdInfo      CCCD validation result (provides masked value, province, gender, etc.)
     */
    public void publishAccountCreated(Account savedAccount,
                                      RegisterRequest request,
                                      CccdCheckClient.CccdInfo cccdInfo) {

        Long accountId    = savedAccount.getId();
        String email      = savedAccount.getEmail();
        String cccdMasked = cccdInfo.getCccdMasked();

        // ── Safe logging: masked CCCD only ──────────────────────────────────
        log.info("Building ACCOUNT_CREATED event for accountId={} email={} cccdMasked={}",
                accountId, email, cccdMasked);

        // ── Parse birthday to LocalDate ──────────────────────────────────────
        LocalDate birthday = LocalDate.parse(request.getBirthday().trim());

        // ── Build inner data payload ─────────────────────────────────────────
        AccountCreatedEventData data = AccountCreatedEventData.builder()
                .accountId(accountId)
                .email(email)
                .role(savedAccount.getRole().getRoleName())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .cccd(request.getCccd())            // raw CCCD – NEVER logged
                .cccdMasked(cccdMasked)
                .provinceCode(cccdInfo.getProvinceCode())
                .provinceName(cccdInfo.getProvinceName())
                .gender(cccdInfo.getGender())
                .birthday(birthday)
                .birthYear(cccdInfo.getBirthYear())
                .build();

        // ── Build event envelope ─────────────────────────────────────────────
        AccountCreatedEvent event = AccountCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .eventVersion(EVENT_VERSION)
                .source(EVENT_SOURCE)
                .occurredAt(Instant.now())
                .data(data)
                .build();

        // ── Publish with accountId as Kafka message key ──────────────────────
        String messageKey = String.valueOf(accountId);
        try {
            SendResult<String, AccountCreatedEvent> result =
                    kafkaTemplate.send(accountCreatedTopic, messageKey, event).get();

            log.info("Published ACCOUNT_CREATED event: topic={} partition={} offset={} "
                            + "accountId={} email={} cccdMasked={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    accountId, email, cccdMasked);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing ACCOUNT_CREATED event for accountId={} email={}",
                    accountId, email, ie);
            throw new RuntimeException(
                    "Kafka publish interrupted for accountId=" + accountId, ie);

        } catch (Exception ex) {
            log.error("Failed to publish ACCOUNT_CREATED event for accountId={} email={}: {}",
                    accountId, email, ex.getMessage(), ex);
            throw new RuntimeException(
                    "Kafka publish failed for accountId=" + accountId, ex);
        }
    }
}
