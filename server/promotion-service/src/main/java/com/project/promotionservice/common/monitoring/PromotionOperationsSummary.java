package com.project.promotionservice.common.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionOperationsSummary(
        long expirationBacklog,
        long oldestExpiredAgeSeconds,
        long reversalCount,
        long reversalsLastHour,
        BigDecimal activeBudgetReserved,
        BigDecimal activeBudgetExposure,
        long campaignsAtExposureThreshold,
        List<String> activeAlerts,
        Instant observedAt) {
}
