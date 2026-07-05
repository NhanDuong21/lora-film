package com.project.authservice.service.impl;

import java.security.SecureRandom;
import java.time.Duration;

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

    private String getRedisKey(String email) {
        return "otp:" + email;
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
    public com.project.authservice.dto.response.SendOtpResponse sendOtp(SendOtpRequest request) {
        String email = request.getEmail();
        
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(AccountNotFoundException::new);
        Long accountId = account.getId();
        
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
        newData.setCreatedAt(existingData != null && existingData.getCreatedAt() > 0 ? existingData.getCreatedAt() : System.currentTimeMillis());
        newData.setLastSentAt(System.currentTimeMillis());

        saveRedisOtpData(key, newData);

        System.out.println("\n==================================");
        System.out.println("OTP GENERATED (Do not use in production)");
        System.out.println("Email: " + email);
        System.out.println("OTP: " + otp);
        System.out.println("==================================\n");

        log.info("OTP generated for email={}", email);
        
        return new com.project.authservice.dto.response.SendOtpResponse(accountId, 300L);
    }

    @Override
    @Transactional
    public void verify(VerifyRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();
        
        if (email != null) {
            if (!accountRepository.existsByEmail(email)) {
                throw new AccountNotFoundException();
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
                log.warn("OTP max failed attempts reached for email={}. Key deleted.", email);
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
        log.info("OTP verified successfully for email={}", email);

        // Account status activation should be handled by the caller or Saga pattern, not here.
    }
}

