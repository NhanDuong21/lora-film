package com.project.promotionservice.common.monitoring;

import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionAdjustmentRepository;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionOperationsMonitoringServiceTest {

    private final PromotionReservationRepository reservationRepository =
            mock(PromotionReservationRepository.class);
    private final PromotionCampaignRepository campaignRepository =
            mock(PromotionCampaignRepository.class);
    private final PromotionRedemptionAdjustmentRepository adjustmentRepository =
            mock(PromotionRedemptionAdjustmentRepository.class);
    private final DatabaseTimeProvider databaseTimeProvider =
            mock(DatabaseTimeProvider.class);
    private final PromotionMetricsManager metricsManager =
            mock(PromotionMetricsManager.class);
    private final PromotionOperationsMonitoringService service =
            new PromotionOperationsMonitoringService(
                    reservationRepository,
                    campaignRepository,
                    adjustmentRepository,
                    databaseTimeProvider,
                    metricsManager,
                    100,
                    120,
                    10,
                    new BigDecimal("0.98"));

    @Test
    void summaryPublishesPersistentOperationalCountsAndActiveAlerts() {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        when(databaseTimeProvider.now()).thenReturn(now);
        when(reservationRepository.countExpirationBacklog(
                ReservationStatus.ACTIVE, now)).thenReturn(120L);
        when(reservationRepository.findOldestExpiredAt(
                ReservationStatus.ACTIVE, now))
                .thenReturn(Optional.of(now.minusSeconds(180)));
        when(adjustmentRepository.countDistinctReservationsByType("REVERSE"))
                .thenReturn(42L);
        when(adjustmentRepository.countDistinctReservationsByTypeSince(
                "REVERSE", now.minusSeconds(3600))).thenReturn(11L);
        when(campaignRepository.sumBudgetReservedByStatus(CampaignStatus.ACTIVE))
                .thenReturn(new BigDecimal("150000.00"));
        when(campaignRepository.sumBudgetExposureByStatus(CampaignStatus.ACTIVE))
                .thenReturn(new BigDecimal("900000.00"));
        when(campaignRepository.countCampaignsAtExposureThreshold(
                CampaignStatus.ACTIVE, new BigDecimal("0.98")))
                .thenReturn(2L);

        PromotionOperationsSummary summary = service.getSummary();

        assertThat(summary.expirationBacklog()).isEqualTo(120);
        assertThat(summary.oldestExpiredAgeSeconds()).isEqualTo(180);
        assertThat(summary.reversalCount()).isEqualTo(42);
        assertThat(summary.reversalsLastHour()).isEqualTo(11);
        assertThat(summary.activeBudgetReserved())
                .isEqualByComparingTo("150000.00");
        assertThat(summary.activeBudgetExposure())
                .isEqualByComparingTo("900000.00");
        assertThat(summary.campaignsAtExposureThreshold()).isEqualTo(2);
        assertThat(summary.activeAlerts()).containsExactly(
                PromotionOperationsMonitoringService.EXPIRATION_BACKLOG_HIGH,
                PromotionOperationsMonitoringService.EXPIRATION_OLDEST_AGE_HIGH,
                PromotionOperationsMonitoringService.REVERSAL_RATE_HIGH,
                PromotionOperationsMonitoringService.CAMPAIGN_BUDGET_EXPOSURE_HIGH);
        verify(metricsManager).updateOperations(summary);
    }
}
