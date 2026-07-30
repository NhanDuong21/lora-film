package com.project.promotionservice.integration.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
public class PromotionLifecycleScheduler {
    private static final Logger log = LoggerFactory.getLogger(PromotionLifecycleScheduler.class);
    private final PromotionJobRunner jobs;
    public PromotionLifecycleScheduler(PromotionJobRunner jobs) { this.jobs = jobs; }

    @Scheduled(fixedDelayString = "${promotion.scheduler.lifecycle-delay-ms:300000}")
    public void progressCampaignLifecycle() {
        safeRun("campaigns-activate");
        safeRun("campaigns-expire");
    }

    @Scheduled(fixedDelayString = "${promotion.scheduler.benefit-expiration-delay-ms:3600000}")
    public void expireBenefits() {
        safeRun("coupons-expire");
        safeRun("vouchers-expire");
    }

    @Scheduled(fixedDelayString = "${promotion.integration.retry-delay-ms:30000}")
    public void retryIntegration() { safeRun("integration-retry"); }

    private void safeRun(String name) {
        try {
            jobs.run(name, "SCHEDULED", "PROMOTION_SCHEDULER");
        } catch (RuntimeException failure) {
            log.error("Scheduled promotion job {} failed; execution is persisted for retry/inspection",
                    name, failure);
        }
    }
}
