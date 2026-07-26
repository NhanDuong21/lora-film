package com.project.promotionservice.integration.outbox;

import com.project.promotionservice.common.lock.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.scheduling.enable",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxEventPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisherScheduler.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    private static final String LOCK_KEY = "lock:scheduler:outbox";

    private final PromotionOutboxEventRepository outboxEventRepository;
    private final PromotionEventPublisher eventPublisher;
    private final RedisLockService redisLockService;
    private final String nodeOwnerId;

    public OutboxEventPublisherScheduler(
            PromotionOutboxEventRepository outboxEventRepository,
            PromotionEventPublisher eventPublisher,
            RedisLockService redisLockService) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.redisLockService = redisLockService;
        this.nodeOwnerId = "node-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Scheduled(fixedDelay = 5000) // Poll database every 5 seconds
    public void publishPendingEvents() {
        // Attempt to acquire distributed lock for 10 seconds to avoid multi-pod execution collision
        if (!redisLockService.acquireLock(LOCK_KEY, nodeOwnerId, 10)) {
            log.trace("OutboxEventPublisherScheduler: Lock busy, skipping.");
            return;
        }

        try {
            log.debug("OutboxEventPublisherScheduler starting polling...");
            Instant now = Instant.now();
            List<PromotionOutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(
                    OutboxStatus.PENDING, now, PageRequest.of(0, BATCH_SIZE));

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.info("Found {} pending outbox events to publish.", pendingEvents.size());

            for (PromotionOutboxEvent event : pendingEvents) {
                event.setPublishStatus(OutboxStatus.PROCESSING);
                outboxEventRepository.saveAndFlush(event);

                try {
                    eventPublisher.publish(event);
                    event.setPublishStatus(OutboxStatus.PUBLISHED);
                    event.setPublishedAt(Instant.now());
                    event.setErrorMessage(null);
                    outboxEventRepository.save(event);
                    log.info("Successfully published outbox event: {}", event.getPublicId());
                } catch (Exception e) {
                    int count = event.getRetryCount() + 1;
                    event.setRetryCount(count);
                    event.setErrorMessage(e.getMessage());
                    event.setUpdatedAt(Instant.now());

                    if (count >= MAX_RETRIES) {
                        event.setPublishStatus(OutboxStatus.FAILED);
                        log.error("Outbox event ID: {} failed and reached max retries. Marked as FAILED.", event.getPublicId());
                    } else {
                        event.setPublishStatus(OutboxStatus.PENDING);
                        // Exponential backoff: 10s, 20s, 40s...
                        long delaySeconds = (long) Math.pow(2, count) * 5L;
                        event.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
                        log.warn("Outbox event ID: {} failed to publish (attempt {}/{}). Next retry at: {}",
                                event.getPublicId(), count, MAX_RETRIES, event.getNextRetryAt());
                    }
                    outboxEventRepository.save(event);
                }
            }
        } finally {
            redisLockService.releaseLock(LOCK_KEY, nodeOwnerId);
        }
    }
}
