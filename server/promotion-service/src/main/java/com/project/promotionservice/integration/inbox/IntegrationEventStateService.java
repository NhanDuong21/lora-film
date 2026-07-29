package com.project.promotionservice.integration.inbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IntegrationEventStateService {
    private final PromotionIntegrationEventRepository repository;
    private final int maxRetries;
    private final long processingLeaseSeconds;

    public IntegrationEventStateService(PromotionIntegrationEventRepository repository,
                                        @Value("${promotion.integration.max-retries:5}") int maxRetries,
                                        @Value("${promotion.integration.processing-lease-seconds:120}")
                                        long processingLeaseSeconds) {
        this.repository = repository;
        this.maxRetries = Math.max(1, maxRetries);
        this.processingLeaseSeconds = Math.max(30, processingLeaseSeconds);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessing(String publicId) {
        PromotionIntegrationEvent event = repository.findByPublicIdForUpdate(publicId).orElse(null);
        if (event == null
                || event.getProcessingStatus() == IntegrationEventStatus.COMPLETED
                || event.getProcessingStatus() == IntegrationEventStatus.IGNORED
                || event.getProcessingStatus() == IntegrationEventStatus.DEAD_LETTER) {
            return false;
        }
        Instant now = Instant.now();
        if (event.getProcessingStatus() == IntegrationEventStatus.PROCESSING
                && event.getUpdatedAt() != null
                && event.getUpdatedAt().isAfter(now.minusSeconds(processingLeaseSeconds))) {
            return false;
        }
        if (event.getProcessingStatus() != IntegrationEventStatus.RECEIVED
                && event.getProcessingStatus() != IntegrationEventStatus.RETRY
                && event.getProcessingStatus() != IntegrationEventStatus.PROCESSING) {
            return false;
        }
        event.setProcessingStatus(IntegrationEventStatus.PROCESSING);
        event.setUpdatedAt(now);
        repository.save(event);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String publicId, boolean ignored) {
        repository.findByPublicIdForUpdate(publicId).ifPresent(event -> {
            event.setProcessingStatus(ignored ? IntegrationEventStatus.IGNORED : IntegrationEventStatus.COMPLETED);
            event.setProcessedAt(Instant.now());
            event.setNextRetryAt(null);
            event.setLastError(null);
            event.setUpdatedAt(Instant.now());
            repository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String publicId, String error) {
        repository.findByPublicIdForUpdate(publicId).ifPresent(event -> {
            int count = event.getRetryCount() + 1;
            event.setRetryCount(count);
            event.setLastError(limit(error));
            event.setUpdatedAt(Instant.now());
            if (count >= maxRetries) {
                event.setProcessingStatus(IntegrationEventStatus.DEAD_LETTER);
                event.setNextRetryAt(null);
            } else {
                event.setProcessingStatus(IntegrationEventStatus.RETRY);
                event.setNextRetryAt(Instant.now().plusSeconds(Math.min(900, 5L << Math.min(count, 8))));
            }
            repository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetForReprocess(String publicId) {
        repository.findByPublicIdForUpdate(publicId).ifPresent(event -> {
            event.setProcessingStatus(IntegrationEventStatus.RECEIVED);
            event.setRetryCount(0);
            event.setNextRetryAt(null);
            event.setLastError(null);
            event.setProcessedAt(null);
            event.setUpdatedAt(Instant.now());
            repository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeadLetter(String publicId, String error) {
        repository.findByPublicIdForUpdate(publicId).ifPresent(event -> {
            event.setProcessingStatus(IntegrationEventStatus.DEAD_LETTER);
            event.setLastError(limit(error));
            event.setNextRetryAt(null);
            event.setUpdatedAt(Instant.now());
            repository.save(event);
        });
    }

    private String limit(String message) {
        if (message == null) return "Integration event processing failed";
        return message.length() <= 4000 ? message : message.substring(0, 4000);
    }
}
