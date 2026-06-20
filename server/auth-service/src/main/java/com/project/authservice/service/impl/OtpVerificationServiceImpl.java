package com.project.authservice.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.authservice.dto.request.ResendOtpRequest;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.entity.Account;
import com.project.authservice.entity.RedisOtpData;
import com.project.authservice.exception.AccountAlreadyVerifiedException;
import com.project.authservice.exception.AccountNotFoundException;
import com.project.authservice.exception.InvalidOtpException;
import com.project.authservice.exception.OtpRateLimitException;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.service.VerificationService;

@Service
public class OtpVerificationServiceImpl implements VerificationService {
    private static final Logger log = LoggerFactory.getLogger(OtpVerificationServiceImpl.class);

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpVerificationServiceImpl(AccountRepository accountRepository, StringRedisTemplate redisTemplate, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String getRedisKey(String purpose, String email) {
        return "otp:" + purpose + ":" + email;
    }

    private RedisOtpData getRedisOtpData(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
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
    public com.project.authservice.dto.response.ResendOtpResponse sendOtp(SendOtpRequest request) {
        String email = request.getEmail();
        String purpose = request.getPurpose();
        
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(AccountNotFoundException::new);
        Long accountId = account.getId();
        
        String key = getRedisKey(purpose, email);

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
        newData.setCreatedAt(existingData != null && existingData.getCreatedAt() > 0 ? existingData.getCreatedAt() : System.currentTimeMillis());
        newData.setLastSentAt(System.currentTimeMillis());

        saveRedisOtpData(key, newData);

        System.out.println("\n==================================");
        System.out.println("OTP GENERATED (Do not use in production)");
        System.out.println("AccountId: " + accountId);
        System.out.println("Email: " + email);
        System.out.println("Purpose: " + purpose);
        System.out.println("OTP: " + otp);
        System.out.println("==================================\n");

        log.info("OTP generated for email={} purpose={}", email, purpose);
        
        return new com.project.authservice.dto.response.ResendOtpResponse(accountId, 300L, 60L);
    }

    @Override
    public com.project.authservice.dto.response.ResendOtpResponse resendOtp(ResendOtpRequest request) {
        String email = request.getEmail();
        String purpose = request.getPurpose();

        Account account = accountRepository.findByEmail(email).orElseThrow(AccountNotFoundException::new);
        
        if ("REGISTRATION".equals(purpose)) {
            if (account.getRegistrationCompleted() != null && account.getRegistrationCompleted() == 1) {
                throw new AccountAlreadyVerifiedException();
            }
        }

        return sendOtp(new SendOtpRequest(email, purpose));
    }

    @Override
    @Transactional
    public void verify(VerifyRequest request) {
        Long accountId = request.getAccountId();
        String otp = request.getOtp();
        String purpose = request.getPurpose();
        
        Account account = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        String email = account.getEmail();
        String key = getRedisKey(purpose, email);

        RedisOtpData existingData = getRedisOtpData(key);
        if (existingData == null) {
            throw new InvalidOtpException();
        }

        if (!passwordEncoder.matches(otp, existingData.getOtpHash())) {
            int attempts = existingData.getFailedAttempts() + 1;
            if (attempts >= 5) {
                redisTemplate.delete(key);
                log.warn("OTP max failed attempts reached for email={} purpose={}. Key deleted.", email, purpose);
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
        log.info("OTP verified successfully for email={} purpose={}", email, purpose);

        // Activate account
        account.setRegistrationCompleted(1);
        account.setIsActive(1);
        accountRepository.save(account);
        log.info("Account {} activated successfully.", email);
    }
}
