package com.project.authservice.service.impl;

import java.security.SecureRandom;
import java.time.Duration;

import com.project.authservice.client.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.RedisOtpData;
import com.project.authservice.exception.AccountAlreadyVerifiedException;
import com.project.authservice.exception.AccountNotFoundException;
import com.project.authservice.exception.InvalidOtpException;
import com.project.authservice.exception.OtpRateLimitException;
import com.project.authservice.exception.OtpDeliveryPendingException;
import com.project.authservice.exception.RegistrationExpiredException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.service.VerificationService;

import static com.project.authservice.util.SensitiveDataMasker.maskEmail;

@Service
public class OtpVerificationServiceImpl implements VerificationService {
    private static final Logger log = LoggerFactory.getLogger(OtpVerificationServiceImpl.class);

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final NotificationClient notificationClient;

    public OtpVerificationServiceImpl(AccountRepository accountRepository,
            StringRedisTemplate redisTemplate,
            PasswordEncoder passwordEncoder,
            NotificationClient notificationClient) {
        this.accountRepository = accountRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.notificationClient = notificationClient;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String getRedisKey(String email) {
        return "otp:" + normalizeEmail(email);
    }

    private RedisOtpData getRedisOtpData(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, RedisOtpData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse RedisOtpData from JSON", e);
            return null;
        }
    }

    private void saveRedisOtpData(String key, RedisOtpData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            log.error("Failed to write RedisOtpData to JSON", e);
        }
    }

    private String generateSecureOtp() {
        int number = secureRandom.nextInt(1000000);
        return String.format("%06d", number);
    }

    @Override
    public com.project.authservice.dto.response.SendOtpResponse sendOtp(SendOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(AccountNotFoundException::new);
        if (account.getAccountStatus() != com.project.authservice.enums.AccountStatus.INACTIVE) {
            throw new AccountAlreadyVerifiedException();
        }
        Long accountId = account.getId();

        String pendingKey = "pending_registration:" + email;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(pendingKey))) {
            throw new RegistrationExpiredException();
        }

        String key = getRedisKey(email);

        RedisOtpData existingData = getRedisOtpData(key);
        if (existingData != null) {
            long lastSentAt = existingData.getLastSentAt();
            if (lastSentAt > 0) {
                long elapsedSeconds = (System.currentTimeMillis() - lastSentAt) / 1000;
                if (elapsedSeconds < 60) {
                    throw new OtpRateLimitException(60 - elapsedSeconds);
                }
            }
        }

        String otp = generateSecureOtp();
        String otpHash = passwordEncoder.encode(otp);

        RedisOtpData newData = new RedisOtpData();
        newData.setOtpHash(otpHash);
        newData.setFailedAttempts(0);
        newData.setCreatedAt(existingData != null && existingData.getCreatedAt() > 0 ? existingData.getCreatedAt()
                : System.currentTimeMillis());
        newData.setLastSentAt(System.currentTimeMillis());

        saveRedisOtpData(key, newData);

        log.info("OTP generated for email={}", maskEmail(email));
        if (account.getAccountStatus() == com.project.authservice.enums.AccountStatus.INACTIVE) {
            String name = "Khách hàng";
            String pendingJson = redisTemplate.opsForValue().get(pendingKey);
            if (pendingJson != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(pendingJson);
                    com.fasterxml.jackson.databind.JsonNode requestNode = rootNode.path("request");
                    if (!requestNode.isMissingNode() && requestNode.has("fullName")) {
                        name = requestNode.path("fullName").asText();
                    }
                } catch (Exception e) {
                    log.error("Failed to parse PendingRegistrationData for email {}: {}",
                            maskEmail(email), e.getMessage());
                }
            }

            try {
                notificationClient.sendRegistrationOtp(accountId, email, name, otp);
                log.info("OTP registration email delivered to SMTP provider for email={}", maskEmail(email));
            } catch (OtpDeliveryPendingException exception) {
                log.warn("OTP delivery status is still pending for email={}", maskEmail(email));
                throw exception;
            } catch (RuntimeException exception) {
                redisTemplate.delete(key);
                log.warn("OTP delivery failed; removed OTP and resend cooldown for email={} causeType={}",
                        maskEmail(email), exception.getClass().getSimpleName());
                throw exception;
            }
        }

        return new com.project.authservice.dto.response.SendOtpResponse(accountId, 300L, 60L);
    }

    @Override
    @Transactional
    public void verify(VerifyRequest request) {
        String email = normalizeEmail(request.getEmail());
        String otp = request.getOtp();

        if (email != null) {
            if (!accountRepository.existsByEmail(email)) {
                throw new AccountNotFoundException();
            }
            Account account = accountRepository.findByEmail(email).orElseThrow(AccountNotFoundException::new);
            if (account.getAccountStatus() != com.project.authservice.enums.AccountStatus.INACTIVE) {
                throw new AccountAlreadyVerifiedException();
            }
        } else {
            throw new AccountNotFoundException();
        }

        String key = getRedisKey(email);

        RedisOtpData existingData = getRedisOtpData(key);
        if (existingData == null) {
            throw new InvalidOtpException();
        }

        if (!passwordEncoder.matches(otp, existingData.getOtpHash())) {
            int attempts = existingData.getFailedAttempts() + 1;
            if (attempts >= 5) {
                redisTemplate.delete(key);
                log.warn("OTP max failed attempts reached for email={}. Key deleted.", maskEmail(email));
            } else {
                existingData.setFailedAttempts(attempts);
                // Preserve remaining TTL when updating attempts
                Long expireSecs = redisTemplate.getExpire(key);
                if (expireSecs != null && expireSecs > 0) {
                    try {
                        String json = objectMapper.writeValueAsString(existingData);
                        redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(expireSecs));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to write RedisOtpData to JSON", e);
                    }
                }
            }
            throw new InvalidOtpException();
        }

        // Success
        redisTemplate.delete(key);
        log.info("OTP verified successfully for email={}", maskEmail(email));

        // Account status activation should be handled by the caller or Saga pattern,
        // not here.
    }

    @Override
    public void sendForgotPasswordEmail(Long accountId, String email, String otp) {
        notificationClient.sendForgotPasswordOtp(accountId, email, otp);
        log.info("Forgot password OTP email delivered to SMTP provider for email={}", maskEmail(email));
    }

    @Override
    public void sendChangeEmailOtp(Long accountId, String currentEmail, String newEmail, String otp) {
        notificationClient.sendChangeEmailOtp(accountId, currentEmail, newEmail, otp);
        log.info("Change email OTP delivered to SMTP provider for email={}", maskEmail(currentEmail));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
