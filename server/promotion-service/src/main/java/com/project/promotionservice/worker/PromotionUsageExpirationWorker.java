package com.project.promotionservice.worker;

import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.repository.PromotionUsageRepository;
import com.project.promotionservice.service.impl.PromotionUsageReconciliationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class PromotionUsageExpirationWorker {

    private static final Logger logger = LoggerFactory.getLogger(PromotionUsageExpirationWorker.class);

    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionUsageReconciliationHelper reconciliationHelper;
    private final Clock clock;

    private final boolean enabled;
    private final int batchSize;

    public PromotionUsageExpirationWorker(PromotionUsageRepository promotionUsageRepository,
                                          PromotionUsageReconciliationHelper reconciliationHelper,
                                          Clock clock,
                                          @Value("${promotion.usage.reconciliation.enabled:true}") boolean enabled,
                                          @Value("${promotion.usage.reconciliation.batch-size:50}") int batchSize) {
        this.promotionUsageRepository = promotionUsageRepository;
        this.reconciliationHelper = reconciliationHelper;
        this.clock = clock;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${promotion.usage.reconciliation.fixed-delay-ms:60000}")
    public void runReconciliation() {
        if (!enabled) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now(clock);
            Pageable pageable = PageRequest.of(0, batchSize);
            Page<PromotionUsage> page = promotionUsageRepository.findByStatusAndExpiresAtBeforeOrderByExpiresAtAscIdAsc(
                    PromotionUsageStatus.RESERVED, now, pageable);

            if (page.isEmpty()) {
                return;
            }

            long startTime = System.currentTimeMillis();
            int processedCount = 0;
            int skippedCount = 0;
            int failedCount = 0;

            for (PromotionUsage usage : page.getContent()) {
                try {
                    reconciliationHelper.reconcileExpiredUsage(usage.getId());
                    processedCount++;
                } catch (ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ole) {
                    skippedCount++;
                    logger.warn("Reconciliation worker: Optimistic locking conflict processing usage {}. Skipping.", usage.getId());
                } catch (Exception ex) {
                    failedCount++;
                    logger.error("Reconciliation worker: Unexpected error processing usage {}.", usage.getId(), ex);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Reconciliation worker batch processed. BatchSize: {}, Processed: {}, Skipped: {}, Failed: {}, Duration: {}ms",
                    page.getNumberOfElements(), processedCount, skippedCount, failedCount, duration);

        } catch (Exception e) {
            logger.error("Reconciliation worker failed to run: {}", e.getMessage(), e);
        }
    }
}
