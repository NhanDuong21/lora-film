package com.project.promotionservice.reservation.idempotency;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.idempotency.PromotionIdempotencyKey;
import com.project.promotionservice.common.idempotency.PromotionIdempotencyKeyRepository;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class ReservationIdempotencyStore {

    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";

    private final PromotionIdempotencyKeyRepository repository;
    private final Duration processingLease;
    private final Duration retention;

    public ReservationIdempotencyStore(
            PromotionIdempotencyKeyRepository repository,
            @Value("${promotion.idempotency.processing-lease-seconds:120}") long processingLeaseSeconds,
            @Value("${promotion.idempotency.retention-hours:24}") long retentionHours) {
        this.repository = repository;
        this.processingLease = Duration.ofSeconds(Math.max(30, processingLeaseSeconds));
        this.retention = Duration.ofHours(Math.max(1, retentionHours));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim claim(
            String clientId,
            String apiName,
            String idempotencyKey,
            String requestHash,
            String requestUri,
            String httpMethod) {
        Instant now = Instant.now();
        PromotionIdempotencyKey existing = repository
                .findForUpdate(clientId, apiName, idempotencyKey)
                .orElse(null);
        if (existing == null) {
            PromotionIdempotencyKey created = new PromotionIdempotencyKey();
            created.setClientId(clientId);
            created.setApiName(apiName);
            created.setIdempotencyKey(idempotencyKey);
            created.setRequestHash(requestHash);
            created.setRequestUri(requestUri);
            created.setHttpMethod(httpMethod);
            created.setProcessingStatus(PROCESSING);
            created.setFirstRequestAt(now);
            created.setExpiredAt(now.plus(retention));
            created.setUpdatedAt(now);
            repository.saveAndFlush(created);
            return Claim.acquiredClaim();
        }

        if (!existing.getRequestHash().equals(requestHash)) {
            throw conflict("Idempotency key was already bound to another request payload");
        }
        if (COMPLETED.equals(existing.getProcessingStatus())
                && now.isBefore(existing.getExpiredAt())) {
            return Claim.replay(existing.getResponseBody(), existing.getResponseStatus());
        }

        Instant heartbeat = existing.getUpdatedAt() != null
                ? existing.getUpdatedAt() : existing.getFirstRequestAt();
        if (PROCESSING.equals(existing.getProcessingStatus())
                && heartbeat != null
                && now.isBefore(heartbeat.plus(processingLease))) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Another request with this idempotency key is still processing",
                    HttpStatus.CONFLICT);
        }

        existing.setProcessingStatus(PROCESSING);
        existing.setResponseBody(null);
        existing.setResponseStatus(null);
        existing.setCompletedAt(null);
        existing.setUpdatedAt(now);
        existing.setExpiredAt(now.plus(retention));
        repository.save(existing);
        return Claim.acquiredClaim();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(
            String clientId,
            String apiName,
            String idempotencyKey,
            String responseBody,
            int responseStatus,
            String reservationPublicId) {
        PromotionIdempotencyKey record = requireForUpdate(clientId, apiName, idempotencyKey);
        record.setResponseBody(responseBody);
        record.setResponseStatus(responseStatus);
        record.setReservationPublicId(reservationPublicId);
        record.setProcessingStatus(COMPLETED);
        record.setCompletedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String clientId, String apiName, String idempotencyKey) {
        PromotionIdempotencyKey record = repository
                .findForUpdate(clientId, apiName, idempotencyKey)
                .orElse(null);
        if (record == null || COMPLETED.equals(record.getProcessingStatus())) {
            return;
        }
        record.setProcessingStatus(FAILED);
        record.setCompletedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        repository.save(record);
    }

    private PromotionIdempotencyKey requireForUpdate(
            String clientId, String apiName, String idempotencyKey) {
        return repository.findForUpdate(clientId, apiName, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "Idempotency claim disappeared before completion"));
    }

    private BusinessException conflict(String message) {
        return new BusinessException(
                ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                message,
                HttpStatus.CONFLICT);
    }

    public record Claim(boolean acquired, String responseBody, Integer responseStatus) {
        public static Claim acquiredClaim() {
            return new Claim(true, null, null);
        }

        public static Claim replay(String responseBody, Integer responseStatus) {
            return new Claim(false, responseBody, responseStatus);
        }
    }
}
