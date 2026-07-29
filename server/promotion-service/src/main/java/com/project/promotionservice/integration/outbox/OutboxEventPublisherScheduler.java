package com.project.promotionservice.integration.outbox;

import com.project.promotionservice.common.monitoring.PromotionMetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxEventPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisherScheduler.class);
    private final PromotionOutboxEventRepository outboxEventRepository;
    private final PromotionEventPublisher eventPublisher;
    private final OutboxDeliveryStateService stateService;
    private final String nodeOwnerId;
    private final int batchSize;
    private final PromotionMetricsManager metricsManager;
    private final boolean schedulingEnabled;

    public OutboxEventPublisherScheduler(
            PromotionOutboxEventRepository outboxEventRepository,
            PromotionEventPublisher eventPublisher,
            OutboxDeliveryStateService stateService,
            PromotionMetricsManager metricsManager,
            @Value("${promotion.outbox.batch-size:50}") int batchSize,
            @Value("${app.scheduling.enable:true}") boolean schedulingEnabled) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.stateService = stateService;
        this.metricsManager = metricsManager;
        this.nodeOwnerId = "node-" + UUID.randomUUID().toString().substring(0, 8);
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.schedulingEnabled = schedulingEnabled;
    }

    @Scheduled(fixedDelay = 5000) // Poll database every 5 seconds
    public void publishPendingEvents() {
        if (!schedulingEnabled) {
            return;
        }
        Instant now = Instant.now();
        List<Long> claimableIds = outboxEventRepository.findClaimableIds(
                now,
                now.minus(stateService.leaseDuration()),
                PageRequest.of(0, batchSize));
        for (Long eventId : claimableIds) {
            PromotionOutboxEvent event = stateService
                    .claim(eventId, nodeOwnerId, Instant.now())
                    .orElse(null);
            if (event == null) {
                continue;
            }
            try {
                eventPublisher.publish(event);
                stateService.markPublished(event.getId(), nodeOwnerId, Instant.now());
                metricsManager.incrementOutboxDelivery("published");
                log.debug("Published outbox event {}", event.getPublicId());
            } catch (Exception exception) {
                metricsManager.incrementOutboxDelivery("failed");
                log.warn("Unable to publish outbox event {}: {}",
                        event.getPublicId(), rootMessage(exception));
                try {
                    stateService.markFailed(
                            event.getId(), nodeOwnerId, rootMessage(exception), Instant.now());
                } catch (RuntimeException stateFailure) {
                    log.error("Unable to persist failed outbox delivery state for event {}",
                            event.getPublicId(), stateFailure);
                }
            }
        }
    }

    private String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }
}
