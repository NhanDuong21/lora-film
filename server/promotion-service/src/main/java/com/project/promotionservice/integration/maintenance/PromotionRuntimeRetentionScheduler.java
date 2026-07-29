package com.project.promotionservice.integration.maintenance;

import com.project.promotionservice.common.idempotency.PromotionIdempotencyKeyRepository;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(
        name = "app.scheduling.enable",
        havingValue = "true",
        matchIfMissing = true)
public class PromotionRuntimeRetentionScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PromotionRuntimeRetentionScheduler.class);

    private final PromotionIdempotencyKeyRepository idempotencyRepository;
    private final PromotionOutboxEventRepository outboxRepository;
    private final Duration publishedOutboxRetention;

    public PromotionRuntimeRetentionScheduler(
            PromotionIdempotencyKeyRepository idempotencyRepository,
            PromotionOutboxEventRepository outboxRepository,
            @Value("${promotion.outbox.published-retention-days:7}")
            long publishedOutboxRetentionDays) {
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.publishedOutboxRetention =
                Duration.ofDays(Math.max(1, publishedOutboxRetentionDays));
    }

    @Scheduled(fixedDelayString = "${promotion.runtime-retention-delay-ms:3600000}")
    @Transactional
    public void cleanRuntimeRecords() {
        Instant now = Instant.now();
        int idempotencyRecords = idempotencyRepository.deleteExpired(now);
        int outboxEvents = outboxRepository.deletePublishedBefore(
                OutboxStatus.PUBLISHED, now.minus(publishedOutboxRetention));
        if (idempotencyRecords > 0 || outboxEvents > 0) {
            log.info("Cleaned {} expired idempotency records and {} published outbox events",
                    idempotencyRecords, outboxEvents);
        }
    }
}
