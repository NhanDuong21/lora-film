package com.lorafilm.booking.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;
import com.lorafilm.booking.infrastructure.enums.IdempotencyStatus;
import com.lorafilm.booking.infrastructure.repository.BookingIdempotencyKeyRepository;
import com.lorafilm.booking.security.service.SecurityContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final long PROCESSING_LEASE_SECONDS = 120;

    private final BookingIdempotencyKeyRepository idempotencyKeyRepository;
    private final SecurityContextService securityContextService;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(
            BookingIdempotencyKeyRepository idempotencyKeyRepository,
            SecurityContextService securityContextService,
            ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.securityContextService = securityContextService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String key = request.getHeader(IDEMPOTENCY_HEADER);

        // If no idempotency key is provided, we proceed without enforcing it or log it.
        // As a production-ready system, we only enforce if the header is present, otherwise we bypass.
        if (key == null || key.trim().isEmpty()) {
            return joinPoint.proceed();
        }

        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
            return joinPoint.proceed();
        }

        Long userId = 0L;
        try {
            userId = securityContextService.getCurrentUserId();
        } catch (Exception e) {
            // Anonymous or unauthenticated user
        }

        String endpoint = request.getRequestURI();
        String requestHash = computeRequestHash(joinPoint.getArgs());

        // Check database
        Instant now = Instant.now();
        Optional<BookingIdempotencyKey> existingOpt =
                idempotencyKeyRepository.findByUserIdAndEndpointAndIdempotencyKey(userId, endpoint, key);
        if (existingOpt.isPresent()) {
            BookingIdempotencyKey record = existingOpt.get();
            if (record.getExpiresAt().isBefore(now)) {
                idempotencyKeyRepository.delete(record);
            } else {
                if (!requestHash.equals(record.getRequestHash())) {
                    throw new BusinessException("IDEMPOTENCY_PAYLOAD_CONFLICT",
                            "The idempotency key was reused with a different request payload",
                            HttpStatus.CONFLICT);
                }
                if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                    if (record.getLockedUntil() == null || record.getLockedUntil().isAfter(now)) {
                        throw new BusinessException("DUPLICATE_REQUEST", "Request is already in progress");
                    }
                    // A crashed request may be retried only after its processing lease
                    // expires. The scoped DB unique key still arbitrates concurrent claims.
                    idempotencyKeyRepository.delete(record);
                    idempotencyKeyRepository.flush();
                } else if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                    log.info("Idempotency match found for key: {}. Returning cached response.", key);
                    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                    Class<?> returnType = signature.getReturnType();

                    if (ResponseEntity.class.isAssignableFrom(returnType)) {
                        // Reconstruct ResponseEntity
                        Object body = null;
                        if (record.getResponseBody() != null) {
                            java.lang.reflect.Type genericType = ((MethodSignature) joinPoint.getSignature()).getMethod().getGenericReturnType();
                            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                                java.lang.reflect.Type actualType = ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments()[0];
                                body = objectMapper.readValue(record.getResponseBody(), objectMapper.getTypeFactory().constructType(actualType));
                            } else {
                                body = objectMapper.readValue(record.getResponseBody(), Object.class);
                            }
                        }
                        return ResponseEntity.status(record.getResponseStatus()).body(body);
                    } else {
                        return objectMapper.readValue(record.getResponseBody(), returnType);
                    }
                } else {
                    // If status is FAILED, we let it retry by deleting it.
                    idempotencyKeyRepository.delete(record);
                }
            }
        }

        // Insert new record as PROCESSING
        BookingIdempotencyKey newRecord = new BookingIdempotencyKey();
        newRecord.setIdempotencyKey(key);
        newRecord.setRequestHash(requestHash);
        newRecord.setUserId(userId);
        newRecord.setEndpoint(endpoint);
        newRecord.setStatus(IdempotencyStatus.PROCESSING);
        newRecord.setLockedUntil(now.plusSeconds(PROCESSING_LEASE_SECONDS));
        newRecord.setExpiresAt(now.plusSeconds(idempotent.expireInSeconds()));
        try {
            newRecord = idempotencyKeyRepository.saveAndFlush(newRecord);
        } catch (DataIntegrityViolationException concurrentClaim) {
            throw new BusinessException(
                    "DUPLICATE_REQUEST",
                    "Another request already claimed this scoped idempotency key",
                    HttpStatus.CONFLICT);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            try {
                idempotencyKeyRepository.delete(newRecord);
            } catch (Exception ex) {
                log.error("Failed to clean up failed idempotency key", ex);
            }
            throw t;
        }

        // Save result
        try {
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                newRecord.setResponseStatus(responseEntity.getStatusCode().value());
                if (responseEntity.getBody() != null) {
                    newRecord.setResponseBody(objectMapper.writeValueAsString(responseEntity.getBody()));
                }
            } else if (result != null) {
                newRecord.setResponseStatus(HttpStatus.OK.value());
                newRecord.setResponseBody(objectMapper.writeValueAsString(result));
            } else {
                newRecord.setResponseStatus(HttpStatus.NO_CONTENT.value());
            }
            newRecord.setStatus(IdempotencyStatus.COMPLETED);
            newRecord.setLockedUntil(null);
            newRecord.setResourcePublicId(extractResourcePublicId(result));
            idempotencyKeyRepository.save(newRecord);
        } catch (Exception ex) {
            log.error("Failed to update idempotency key status to COMPLETED", ex);
        }

        return result;
    }

    private String extractResourcePublicId(Object result) {
        Object body = result instanceof ResponseEntity<?> responseEntity
                ? responseEntity.getBody() : result;
        if (body instanceof com.lorafilm.booking.common.response.ApiResponse<?> apiResponse) {
            body = apiResponse.getData();
        }
        if (body == null) {
            return null;
        }
        try {
            Object value = body.getClass().getMethod("publicId").invoke(body);
            return value instanceof String publicId ? publicId : null;
        } catch (ReflectiveOperationException ignored) {
            try {
                Object value = body.getClass().getMethod("getPublicId").invoke(body);
                return value instanceof String publicId ? publicId : null;
            } catch (ReflectiveOperationException noPublicIdentity) {
                return null;
            }
        }
    }

    private String computeRequestHash(Object[] args) {
        if (args == null || args.length == 0) {
            return "EMPTY";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg != null) {
                    sb.append(objectMapper.writeValueAsString(arg));
                }
            }
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("Failed to compute request hash, returning fallback hash", e);
            return "HASH_ERROR_" + UUID.randomUUID().toString();
        }
    }
}
