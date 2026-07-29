package com.project.authservice.event.publisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.project.authservice.client.CccdCheckClient;
import com.project.authservice.dto.request.RegisterRequest;
import com.project.authservice.entity.Account;
import com.project.authservice.event.dto.AccountVerifiedEventData;

import static com.project.authservice.util.SensitiveDataMasker.maskEmail;


@Service
public class AuthAccountEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthAccountEventPublisher.class);

    @Value("${app.kafka.topic.registration-validation-requested}")
    private String registrationValidationRequestedTopic;

    @Value("${app.kafka.topic.account-verified}")
    private String accountVerifiedTopic;



    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuthAccountEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void publishRegistrationValidationRequested(RegisterRequest request, String requestId) {
        log.info("Building REGISTRATION_VALIDATION_REQUESTED event for requestId={} email={}",
                requestId, maskEmail(request.getEmail()));

        com.project.authservice.event.dto.RegistrationValidationRequestedEventData data = 
            com.project.authservice.event.dto.RegistrationValidationRequestedEventData.builder()
                .requestId(requestId)
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .cccd(request.getCccd())
                .build();

        com.project.authservice.event.dto.RegistrationValidationRequestedEvent event = 
            com.project.authservice.event.dto.RegistrationValidationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .data(data)
                .build();

        try {
            kafkaTemplate.send(registrationValidationRequestedTopic, requestId, event).get();
            log.info("Published REGISTRATION_VALIDATION_REQUESTED event for requestId={}", requestId);
        } catch (Exception ex) {
            log.error("Failed to publish REGISTRATION_VALIDATION_REQUESTED event for requestId={}", requestId, ex);
            throw new RuntimeException("Kafka publish failed for requestId=" + requestId, ex);
        }
    }

    public void publishAccountVerified(Account savedAccount, RegisterRequest request, CccdCheckClient.CccdInfo cccdInfo) {
        Long accountId = savedAccount.getId();
        String email = savedAccount.getEmail();
        String cccdMasked = cccdInfo.getCccdMasked();

        log.info("Building ACCOUNT_VERIFIED event for accountId={} email={} cccdMasked={}",
                accountId, maskEmail(email), cccdMasked);

        LocalDate birthday = LocalDate.parse(request.getBirthday().trim());

        AccountVerifiedEventData data = AccountVerifiedEventData.builder()
                .requestId(UUID.randomUUID().toString())
                .accountId(accountId)
                .email(email)
                .role(savedAccount.getRole().getRoleName())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .cccd(request.getCccd())
                .cccdMasked(cccdMasked)
                .provinceCode(cccdInfo.getProvinceCode())
                .provinceName(cccdInfo.getProvinceName())
                .gender(cccdInfo.getGender())
                .birthday(birthday)
                .birthYear(cccdInfo.getBirthYear())
                .build();

        com.project.authservice.event.dto.AccountVerifiedEvent event = 
            com.project.authservice.event.dto.AccountVerifiedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .data(data)
                .build();

        try {
            kafkaTemplate.send(accountVerifiedTopic, String.valueOf(accountId), event).get();
            log.info("Published ACCOUNT_VERIFIED event for accountId={}", accountId);
        } catch (Exception ex) {
            log.error("Failed to publish ACCOUNT_VERIFIED event for accountId={}", accountId, ex);
            throw new RuntimeException("Kafka publish failed for accountId=" + accountId, ex);
        }
    }
}
