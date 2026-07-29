package com.project.promotionservice.integration.maintenance;

import com.project.promotionservice.common.idempotency.PromotionIdempotencyKeyRepository;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.integration.inbox.IntegrationEventStatus;
import com.project.promotionservice.integration.inbox.PromotionIntegrationEventRepository;
import com.project.promotionservice.integration.job.SchedulerJobExecutionRepository;
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
    private final PromotionIntegrationEventRepository integrationRepository;
    private final SchedulerJobExecutionRepository jobExecutionRepository;
    private final Duration processedEventRetention;
    private final Duration jobExecutionRetention;

    public PromotionRuntimeRetentionScheduler(
            PromotionIdempotencyKeyRepository idempotencyRepository,
            PromotionOutboxEventRepository outboxRepository,
            @Value("${promotion.outbox.published-retention-days:7}")
            long publishedOutboxRetentionDays,
            PromotionIntegrationEventRepository integrationRepository,
            SchedulerJobExecutionRepository jobExecutionRepository,
            @Value("${promotion.integration.event-retention-days:30}")
            long processedEventRetentionDays,
            @Value("${promotion.scheduler.job-retention-days:30}")
            long jobExecutionRetentionDays) {
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.publishedOutboxRetention =
                Duration.ofDays(Math.max(1, publishedOutboxRetentionDays));
        this.integrationRepository = integrationRepository;
        this.jobExecutionRepository = jobExecutionRepository;
        this.processedEventRetention = Duration.ofDays(Math.max(1, processedEventRetentionDays));
        this.jobExecutionRetention = Duration.ofDays(Math.max(1, jobExecutionRetentionDays));
    }

    @Scheduled(fixedDelayString = "${promotion.runtime-retention-delay-ms:3600000}")
    @Transactional
    public void cleanRuntimeRecords() {
        Instant now = Instant.now();
        int idempotencyRecords = idempotencyRepository.deleteExpired(now);
        int outboxEvents = outboxRepository.deletePublishedBefore(
                OutboxStatus.PUBLISHED, now.minus(publishedOutboxRetention));
        int processedEvents = integrationRepository.deleteProcessedBefore(
                java.util.EnumSet.of(IntegrationEventStatus.COMPLETED, IntegrationEventStatus.IGNORED),
                now.minus(processedEventRetention));
        int jobExecutions = jobExecutionRepository.deleteStartedBefore(
                now.minus(jobExecutionRetention));
        if (idempotencyRecords > 0 || outboxEvents > 0 || processedEvents > 0 || jobExecutions > 0) {
            log.info("Cleaned {} idempotency, {} published outbox, {} processed integration and {} scheduler records",
                    idempotencyRecords, outboxEvents, processedEvents, jobExecutions);
        }
    }
}
