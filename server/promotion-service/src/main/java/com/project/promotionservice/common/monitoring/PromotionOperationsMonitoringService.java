package com.project.promotionservice.common.monitoring;

import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionAdjustmentRepository;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PromotionOperationsMonitoringService {

    public static final String EXPIRATION_BACKLOG_HIGH = "EXPIRATION_BACKLOG_HIGH";
    public static final String EXPIRATION_OLDEST_AGE_HIGH = "EXPIRATION_OLDEST_AGE_HIGH";
    public static final String REVERSAL_RATE_HIGH = "REVERSAL_RATE_HIGH";
    public static final String CAMPAIGN_BUDGET_EXPOSURE_HIGH =
            "CAMPAIGN_BUDGET_EXPOSURE_HIGH";

    private static final Logger log =
            LoggerFactory.getLogger(PromotionOperationsMonitoringService.class);
    private static final String REVERSE_ADJUSTMENT = "REVERSE";

    private final PromotionReservationRepository reservationRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRedemptionAdjustmentRepository adjustmentRepository;
    private final DatabaseTimeProvider databaseTimeProvider;
    private final PromotionMetricsManager metricsManager;
    private final long expirationBacklogAlertThreshold;
    private final long oldestExpiredAgeAlertSeconds;
    private final long reversalHourlyAlertThreshold;
    private final BigDecimal budgetExposureAlertRatio;
    private volatile Set<String> previousAlerts = Set.of();

    public PromotionOperationsMonitoringService(
            PromotionReservationRepository reservationRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionRedemptionAdjustmentRepository adjustmentRepository,
            DatabaseTimeProvider databaseTimeProvider,
            PromotionMetricsManager metricsManager,
            @Value("${promotion.monitoring.expiration-backlog-alert-threshold:100}")
            long expirationBacklogAlertThreshold,
            @Value("${promotion.monitoring.oldest-expired-age-alert-seconds:120}")
            long oldestExpiredAgeAlertSeconds,
            @Value("${promotion.monitoring.reversal-hourly-alert-threshold:10}")
            long reversalHourlyAlertThreshold,
            @Value("${promotion.monitoring.budget-exposure-alert-ratio:0.98}")
            BigDecimal budgetExposureAlertRatio) {
        this.reservationRepository = reservationRepository;
        this.campaignRepository = campaignRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.databaseTimeProvider = databaseTimeProvider;
        this.metricsManager = metricsManager;
        this.expirationBacklogAlertThreshold = expirationBacklogAlertThreshold;
        this.oldestExpiredAgeAlertSeconds = oldestExpiredAgeAlertSeconds;
        this.reversalHourlyAlertThreshold = reversalHourlyAlertThreshold;
        this.budgetExposureAlertRatio = budgetExposureAlertRatio;
    }

    @Transactional(readOnly = true)
    public PromotionOperationsSummary getSummary() {
        return collect();
    }

    @Transactional(readOnly = true)
    public void refreshMetricsAndAlerts() {
        PromotionOperationsSummary summary = collect();
        Set<String> currentAlerts = Set.copyOf(summary.activeAlerts());
        Set<String> activated = new HashSet<>(currentAlerts);
        activated.removeAll(previousAlerts);
        Set<String> resolved = new HashSet<>(previousAlerts);
        resolved.removeAll(currentAlerts);
        if (!activated.isEmpty()) {
            log.warn("Promotion operations alerts activated: {}", activated);
        }
        if (!resolved.isEmpty()) {
            log.info("Promotion operations alerts resolved: {}", resolved);
        }
        previousAlerts = currentAlerts;
    }

    private PromotionOperationsSummary collect() {
        Instant now = databaseTimeProvider.now();
        long expirationBacklog = reservationRepository.countExpirationBacklog(
                ReservationStatus.ACTIVE, now);
        long oldestExpiredAgeSeconds = reservationRepository
                .findOldestExpiredAt(ReservationStatus.ACTIVE, now)
                .map(oldest -> Math.max(0, Duration.between(oldest, now).getSeconds()))
                .orElse(0L);
        long reversalCount = adjustmentRepository
                .countDistinctReservationsByType(REVERSE_ADJUSTMENT);
        long reversalsLastHour = adjustmentRepository
                .countDistinctReservationsByTypeSince(
                        REVERSE_ADJUSTMENT, now.minus(Duration.ofHours(1)));
        BigDecimal budgetReserved = nonNull(campaignRepository
                .sumBudgetReservedByStatus(CampaignStatus.ACTIVE));
        BigDecimal budgetExposure = nonNull(campaignRepository
                .sumBudgetExposureByStatus(CampaignStatus.ACTIVE));
        long campaignsAtThreshold = campaignRepository
                .countCampaignsAtExposureThreshold(
                        CampaignStatus.ACTIVE, budgetExposureAlertRatio);

        List<String> alerts = new ArrayList<>();
        if (expirationBacklog >= expirationBacklogAlertThreshold) {
            alerts.add(EXPIRATION_BACKLOG_HIGH);
        }
        if (oldestExpiredAgeSeconds >= oldestExpiredAgeAlertSeconds) {
            alerts.add(EXPIRATION_OLDEST_AGE_HIGH);
        }
        if (reversalsLastHour >= reversalHourlyAlertThreshold) {
            alerts.add(REVERSAL_RATE_HIGH);
        }
        if (campaignsAtThreshold > 0) {
            alerts.add(CAMPAIGN_BUDGET_EXPOSURE_HIGH);
        }
        PromotionOperationsSummary summary = new PromotionOperationsSummary(
                expirationBacklog,
                oldestExpiredAgeSeconds,
                reversalCount,
                reversalsLastHour,
                budgetReserved,
                budgetExposure,
                campaignsAtThreshold,
                List.copyOf(alerts),
                now);
        metricsManager.updateOperations(summary);
        return summary;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
