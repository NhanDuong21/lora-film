package com.project.promotionservice.service.impl;

import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.PromotionUsageStatus;
import com.project.promotionservice.repository.PromotionRepository;
import com.project.promotionservice.repository.PromotionUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class PromotionUsageReconciliationHelper {

    private static final Logger logger = LoggerFactory.getLogger(PromotionUsageReconciliationHelper.class);

    private final PromotionUsageRepository promotionUsageRepository;
    private final PromotionRepository promotionRepository;
    private final Clock clock;

    public PromotionUsageReconciliationHelper(PromotionUsageRepository promotionUsageRepository,
                                              PromotionRepository promotionRepository,
                                              Clock clock) {
        this.promotionUsageRepository = promotionUsageRepository;
        this.promotionRepository = promotionRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconcileExpiredUsage(Long usageId) {
        Objects.requireNonNull(usageId, "usageId must not be null");
        PromotionUsage usage = promotionUsageRepository.findById(usageId).orElse(null);
        if (usage == null) {
            logger.warn("Reconciliation worker: PromotionUsage {} not found. Skipping.", usageId);
            return;
        }

        PromotionUsageStatus oldStatus = usage.getStatus();
        if (oldStatus != PromotionUsageStatus.RESERVED) {
            logger.info("Reconciliation worker: PromotionUsage {} status was already changed to {}. Skipping.", usageId, oldStatus);
            return;
        }

        int affectedRows = promotionRepository.decrementUsedCountIfPositive(usage.getPromotion().getId());
        if (affectedRows == 0) {
            logger.warn("Reconciliation worker: Promotion usedCount could not be decremented (already 0). PromotionId: {}, UsageId: {}", 
                    usage.getPromotion().getId(), usageId);
        }

        usage.setStatus(PromotionUsageStatus.REVERTED);
        usage.setRevertedAt(LocalDateTime.now(clock));
        usage.setRevertReason("Reservation expired");
        usage.setConfirmedAt(null);

        PromotionUsage saved = promotionUsageRepository.saveAndFlush(usage);
        logger.info("Reconciliation worker: Reverted expired promotion usage. UsageId: {}, PromotionId: {}, BookingId: {}, oldStatus: {}, newStatus: {}, revertReason: {}",
                usageId, usage.getPromotion().getId(), usage.getBookingId(), oldStatus, saved.getStatus(), "Reservation expired");
    }
}
