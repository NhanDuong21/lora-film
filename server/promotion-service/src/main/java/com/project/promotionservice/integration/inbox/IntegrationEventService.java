package com.project.promotionservice.integration.inbox;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class IntegrationEventService {
    private final PromotionIntegrationEventRepository repository;
    private final IntegrationEventStateService state;
    private final IntegrationEventProcessor processor;

    public IntegrationEventService(PromotionIntegrationEventRepository repository,
                                   IntegrationEventStateService state,
                                   IntegrationEventProcessor processor) {
        this.repository = repository;
        this.state = state;
        this.processor = processor;
    }

    @Transactional
    public String receive(String source, String eventId, String eventType, String schemaVersion,
                          String correlationId, String traceId, String payload) {
        PromotionIntegrationEvent existing = repository
                .findBySourceServiceAndEventId(source, eventId).orElse(null);
        if (existing != null) return existing.getPublicId();
        PromotionIntegrationEvent event = new PromotionIntegrationEvent();
        event.setSourceService(source);
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setSchemaVersion(schemaVersion);
        event.setCorrelationId(correlationId);
        event.setTraceId(traceId);
        event.setPayload(payload);
        try {
            return repository.saveAndFlush(event).getPublicId();
        } catch (DataIntegrityViolationException duplicate) {
            return repository.findBySourceServiceAndEventId(source, eventId)
                    .map(PromotionIntegrationEvent::getPublicId)
                    .orElseThrow(() -> duplicate);
        }
    }

    public void process(String publicId) {
        PromotionIntegrationEvent event = repository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Integration event not found"));
        if (event.getProcessingStatus() == IntegrationEventStatus.COMPLETED
                || event.getProcessingStatus() == IntegrationEventStatus.IGNORED
                || event.getProcessingStatus() == IntegrationEventStatus.DEAD_LETTER) return;
        if (!state.markProcessing(publicId)) return;
        try {
            boolean ignored = processor.process(event);
            state.markCompleted(publicId, ignored);
        } catch (RuntimeException failure) {
            state.markFailed(publicId, failure.getMessage());
            throw failure;
        }
    }

    public void reprocess(String publicId) {
        state.resetForReprocess(publicId);
        process(publicId);
    }

    public int retryDue() {
        List<PromotionIntegrationEvent> events = repository.findDueRetries(
                Instant.now(), PageRequest.of(0, 100));
        int processed = 0;
        for (PromotionIntegrationEvent event : events) {
            try {
                process(event.getPublicId());
                processed++;
            } catch (RuntimeException ignored) {
                // State is persisted; next scheduler run will retry or DLQ.
            }
        }
        return processed;
    }
}
