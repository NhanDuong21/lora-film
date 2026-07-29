package com.project.promotionservice.integration.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class OutboxDeliveryStateService {

    private final EntityManager entityManager;
    private final PromotionOutboxEventRepository repository;
    private final int maxRetries;
    private final Duration leaseDuration;

    public OutboxDeliveryStateService(
            EntityManager entityManager,
            PromotionOutboxEventRepository repository,
            @Value("${promotion.outbox.max-retries:8}") int maxRetries,
            @Value("${promotion.outbox.lease-seconds:60}") long leaseSeconds) {
        this.entityManager = entityManager;
        this.repository = repository;
        this.maxRetries = Math.max(1, maxRetries);
        this.leaseDuration = Duration.ofSeconds(Math.max(15, leaseSeconds));
    }

    public Duration leaseDuration() {
        return leaseDuration;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<PromotionOutboxEvent> claim(Long id, String owner, Instant now) {
        PromotionOutboxEvent event = entityManager.find(
                PromotionOutboxEvent.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (event == null || event.getDeletedAt() != null) {
            return Optional.empty();
        }
        boolean pendingAndDue = event.getPublishStatus() == OutboxStatus.PENDING
                && (event.getNextRetryAt() == null || !event.getNextRetryAt().isAfter(now));
        boolean staleProcessing = event.getPublishStatus() == OutboxStatus.PROCESSING
                && (event.getProcessingStartedAt() == null
                || !event.getProcessingStartedAt().isAfter(now.minus(leaseDuration)));
        if (!pendingAndDue && !staleProcessing) {
            return Optional.empty();
        }
        event.setPublishStatus(OutboxStatus.PROCESSING);
        event.setProcessingOwner(owner);
        event.setProcessingStartedAt(now);
        event.setUpdatedAt(now);
        repository.saveAndFlush(event);
        entityManager.detach(event);
        return Optional.of(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(Long id, String owner, Instant now) {
        PromotionOutboxEvent event = requireOwned(id, owner);
        event.setPublishStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setErrorMessage(null);
        event.setNextRetryAt(null);
        event.setProcessingOwner(null);
        event.setProcessingStartedAt(null);
        event.setUpdatedAt(now);
        repository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String owner, String errorMessage, Instant now) {
        PromotionOutboxEvent event = requireOwned(id, owner);
        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);
        event.setErrorMessage(limit(errorMessage, 4000));
        event.setProcessingOwner(null);
        event.setProcessingStartedAt(null);
        event.setUpdatedAt(now);
        if (retryCount >= maxRetries) {
            event.setPublishStatus(OutboxStatus.FAILED);
            event.setNextRetryAt(null);
        } else {
            event.setPublishStatus(OutboxStatus.PENDING);
            long delaySeconds = Math.min(900L, 5L * (1L << Math.min(retryCount, 8)));
            event.setNextRetryAt(now.plusSeconds(delaySeconds));
        }
        repository.save(event);
    }

    private PromotionOutboxEvent requireOwned(Long id, String owner) {
        PromotionOutboxEvent event = entityManager.find(
                PromotionOutboxEvent.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (event == null
                || event.getPublishStatus() != OutboxStatus.PROCESSING
                || !owner.equals(event.getProcessingOwner())) {
            throw new IllegalStateException("Outbox lease is no longer owned by this worker");
        }
        return event;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
