package com.lorafilm.booking.infrastructure.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;
import com.lorafilm.booking.infrastructure.enums.IdempotencyStatus;
import com.lorafilm.booking.infrastructure.repository.BookingIdempotencyKeyRepository;
import com.lorafilm.booking.infrastructure.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final BookingIdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyServiceImpl(
            BookingIdempotencyKeyRepository idempotencyKeyRepository,
            ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingIdempotencyKey> checkKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingIdempotencyKey> checkKey(
            String idempotencyKey, Long userId, String endpoint) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return idempotencyKeyRepository.findByUserIdAndEndpointAndIdempotencyKey(
                userId != null ? userId : 0L,
                endpoint != null ? endpoint : "",
                idempotencyKey);
    }

    @Override
    @Transactional
    public BookingIdempotencyKey startProcessing(
            String idempotencyKey, Long userId, String endpoint, String httpMethod, Object requestBody) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String requestHash = computeHashHex(requestBody);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS);

        BookingIdempotencyKey keyRecord = new BookingIdempotencyKey();
        keyRecord.setIdempotencyKey(idempotencyKey);
        keyRecord.setRequestHash(requestHash);
        keyRecord.setUserId(userId != null ? userId : 0L);
        keyRecord.setEndpoint(endpoint != null ? endpoint : "");
        keyRecord.setStatus(IdempotencyStatus.PROCESSING);
        keyRecord.setLockedUntil(now.plus(120, ChronoUnit.SECONDS));
        keyRecord.setExpiresAt(expiresAt);

        log.debug("Creating IdempotencyKey record with key: {}", idempotencyKey);
        BookingIdempotencyKey saved = idempotencyKeyRepository.save(keyRecord);
        // Flush the scoped unique claim before the reservation transaction
        // starts; this turns a concurrent claim into a deterministic conflict.
        idempotencyKeyRepository.flush();
        // A few lightweight repository doubles return null for save; the
        // managed entity is still the correct result.
        return saved != null ? saved : keyRecord;
    }

    @Override
    @Transactional
    public void completeProcessing(String idempotencyKey, int responseStatus, Object responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(keyRecord -> {
            keyRecord.setStatus(IdempotencyStatus.COMPLETED);
            keyRecord.setResponseStatus(responseStatus);
            try {
                keyRecord.setResponseBody(objectMapper.writeValueAsString(responseBody));
            } catch (Exception ex) {
                log.error("Failed to serialize response body for idempotency key {}: ", idempotencyKey, ex);
            }
            idempotencyKeyRepository.save(keyRecord);
            log.debug("Completed IdempotencyKey record with key: {}", idempotencyKey);
        });
    }

    @Override
    @Transactional
    public void completeProcessing(
            String idempotencyKey, Long userId, String endpoint,
            int responseStatus, Object responseBody) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        idempotencyKeyRepository.findByUserIdAndEndpointAndIdempotencyKey(
                userId != null ? userId : 0L,
                endpoint != null ? endpoint : "",
                idempotencyKey).ifPresent(keyRecord -> {
            keyRecord.setStatus(IdempotencyStatus.COMPLETED);
            keyRecord.setResponseStatus(responseStatus);
            keyRecord.setLockedUntil(null);
            try {
                keyRecord.setResponseBody(objectMapper.writeValueAsString(responseBody));
            } catch (Exception ex) {
                log.error("Failed to serialize response body for idempotency key {}: ", idempotencyKey, ex);
            }
            idempotencyKeyRepository.save(keyRecord);
        });
    }

    @Override
    @Transactional
    public void failProcessing(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(keyRecord -> {
            keyRecord.setStatus(IdempotencyStatus.FAILED);
            keyRecord.setLockedUntil(null);
            idempotencyKeyRepository.save(keyRecord);
            log.debug("Failed IdempotencyKey record with key: {}", idempotencyKey);
        });
    }

    @Override
    @Transactional
    public void failProcessing(String idempotencyKey, Long userId, String endpoint) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        idempotencyKeyRepository.findByUserIdAndEndpointAndIdempotencyKey(
                userId != null ? userId : 0L,
                endpoint != null ? endpoint : "",
                idempotencyKey).ifPresent(keyRecord -> {
            keyRecord.setStatus(IdempotencyStatus.FAILED);
            keyRecord.setLockedUntil(null);
            idempotencyKeyRepository.save(keyRecord);
        });
    }

    private String computeHashHex(Object obj) {
        try {
            String json = obj != null ? objectMapper.writeValueAsString(obj) : "";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            log.warn("Failed to compute SHA-256 hash for idempotency payload: ", ex);
            return "0000000000000000000000000000000000000000000000000000000000000000";
        }
    }
}
