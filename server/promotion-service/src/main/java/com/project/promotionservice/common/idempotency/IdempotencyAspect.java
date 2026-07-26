package com.project.promotionservice.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

@Aspect
@Component
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    private final PromotionIdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(PromotionIdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
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

        if (key == null || key.isBlank()) {
            throw new BusinessException("ERR_IDEMPOTENCY_KEY_MISSING", "Header 'X-Idempotency-Key' is required for this request.", HttpStatus.BAD_REQUEST);
        }

        String apiName = idempotent.apiName().isEmpty() ? joinPoint.getSignature().toShortString() : idempotent.apiName();
        String httpMethod = request.getMethod();
        String requestUri = request.getRequestURI();

        // Calculate hash of request body arguments
        String bodyString = "";
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            try {
                bodyString = objectMapper.writeValueAsString(args[0]);
            } catch (Exception e) {
                log.warn("Failed to serialize request arguments for hashing", e);
            }
        }
        String requestHash = sha256(bodyString);

        // Get User ID if authenticated
        String userPublicId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userPublicId = principal.getUserId() != null ? principal.getUserId().toString() : null;
        }

        Optional<PromotionIdempotencyKey> existingKeyOpt = repository.findByIdempotencyKey(key);

        if (existingKeyOpt.isPresent()) {
            PromotionIdempotencyKey existingRecord = existingKeyOpt.get();

            // Check processing status
            if ("PROCESSING".equals(existingRecord.getProcessingStatus())) {
                throw new BusinessException("ERR_CONCURRENT_REQUEST", "Another request with the same idempotency key is already in progress.", HttpStatus.CONFLICT);
            }

            if ("COMPLETED".equals(existingRecord.getProcessingStatus())) {
                log.info("Replaying response for idempotency key: {}", key);
                // Validate request hash to make sure body didn't change for the same key
                if (!existingRecord.getRequestHash().equals(requestHash)) {
                    throw new BusinessException("ERR_IDEMPOTENCY_HASH_MISMATCH", "Request body does not match the original request for this idempotency key.", HttpStatus.BAD_REQUEST);
                }

                // Replay the response
                Class<?> returnType = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getReturnType();
                if (returnType == void.class) {
                    return null;
                }
                return objectMapper.readValue(existingRecord.getResponseBody(), returnType);
            }
        }

        // Key doesn't exist, or previous attempt failed, create a new record
        PromotionIdempotencyKey idempotencyRecord = new PromotionIdempotencyKey();
        idempotencyRecord.setIdempotencyKey(key);
        idempotencyRecord.setRequestHash(requestHash);
        idempotencyRecord.setApiName(apiName);
        idempotencyRecord.setHttpMethod(httpMethod);
        idempotencyRecord.setRequestUri(requestUri);
        idempotencyRecord.setRequestBody(bodyString);
        idempotencyRecord.setUserPublicId(userPublicId);
        idempotencyRecord.setProcessingStatus("PROCESSING");
        idempotencyRecord.setFirstRequestAt(Instant.now());
        idempotencyRecord.setExpiredAt(Instant.now().plus(java.time.Duration.ofDays(1))); // Default TTL 1 day

        idempotencyRecord = repository.saveAndFlush(idempotencyRecord);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            // Processing failed, set status to FAILED so it can be retried
            idempotencyRecord.setProcessingStatus("FAILED");
            idempotencyRecord.setCompletedAt(Instant.now());
            repository.saveAndFlush(idempotencyRecord);
            throw throwable;
        }

        // Success, store response
        try {
            String responseBody = objectMapper.writeValueAsString(result);
            idempotencyRecord.setResponseBody(responseBody);
            idempotencyRecord.setResponseStatus(HttpStatus.OK.value());
            idempotencyRecord.setProcessingStatus("COMPLETED");
            idempotencyRecord.setCompletedAt(Instant.now());
            repository.saveAndFlush(idempotencyRecord);
        } catch (Exception ex) {
            log.error("Failed to cache response in idempotency record", ex);
        }

        return result;
    }

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
