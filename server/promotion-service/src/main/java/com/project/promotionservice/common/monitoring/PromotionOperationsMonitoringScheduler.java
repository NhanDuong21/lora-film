package com.project.promotionservice.common.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.scheduling.enable",
        havingValue = "true",
        matchIfMissing = true)
public class PromotionOperationsMonitoringScheduler {

    private final PromotionOperationsMonitoringService monitoringService;

    public PromotionOperationsMonitoringScheduler(
            PromotionOperationsMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Scheduled(fixedDelayString =
            "${promotion.monitoring.refresh-delay-ms:60000}")
    public void refresh() {
        monitoringService.refreshMetricsAndAlerts();
    }
}
