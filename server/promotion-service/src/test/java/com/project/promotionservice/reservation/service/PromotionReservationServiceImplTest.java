package com.project.promotionservice.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.monitoring.PromotionMetricsManager;
import com.project.promotionservice.common.time.DatabaseTimeProvider;
import com.project.promotionservice.configuration.domain.ConfigurationService;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.entity.PromotionRedemptionAdjustment;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionAdjustmentRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.promotion.service.PromotionCatalogEventService;
import com.project.promotionservice.promotion.service.PromotionEngineService;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.CompensateRequest;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.impl.PromotionReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionReservationServiceImplTest {

    @Mock private PromotionReservationRepository reservationRepository;
    @Mock private PromotionRedemptionRepository redemptionRepository;
    @Mock private PromotionRedemptionAdjustmentRepository adjustmentRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private UserPromotionRepository walletRepository;
    @Mock private PromotionCampaignRepository campaignRepository;
    @Mock private PromotionEngineService engineService;
    @Mock private ReservationLockManager lockManager;
    @Mock private ConfigurationService configurationService;
    @Mock private DatabaseTimeProvider databaseTimeProvider;
    @Mock private PromotionCatalogEventService eventService;
    @Mock private PromotionMetricsManager metricsManager;
    @Mock private PlatformTransactionManager transactionManager;

    private PromotionReservationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromotionReservationServiceImpl(
                reservationRepository, redemptionRepository, adjustmentRepository,
                promotionRepository,
                walletRepository, campaignRepository, engineService, lockManager,
                configurationService, databaseTimeProvider, eventService,
                metricsManager, new ObjectMapper(), transactionManager);
    }

    @Test
    void reverseConfirmedRestoresWalletBudgetAndOrderCapacity() {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        PromotionReservation reservation = new PromotionReservation();
        reservation.setPublicId("11111111-1111-4111-8111-111111111111");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setUserPublicId("1001");
        reservation.setOriginalAmount(new BigDecimal("100000.00"));
        reservation.setDiscountAmount(new BigDecimal("20000.00"));
        reservation.setFinalAmount(new BigDecimal("80000.00"));
        reservation.setCurrency("VND");
        reservation.setPaymentPublicId("22222222-2222-4222-8222-222222222222");
        reservation.setConfirmedAt(now.minusSeconds(60));

        PromotionRedemption redemption = new PromotionRedemption();
        redemption.setPublicId("33333333-3333-4333-8333-333333333333");
        redemption.setReservationPublicId(reservation.getPublicId());
        redemption.setUserPublicId("1001");
        redemption.setPromotionPublicId("44444444-4444-4444-8444-444444444444");
        redemption.setCampaignPublicId("55555555-5555-4555-8555-555555555555");
        redemption.setPromotionType(PromotionType.VOUCHER);
        redemption.setPromotionCode("EVENT20");
        redemption.setPromotionName("Event voucher");
        redemption.setPromotionPriority(10);
        redemption.setPromotionStackable(true);
        redemption.setConditionsSnapshotJson("{}");
        redemption.setActionsSnapshotJson(
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":20000}");
        redemption.setSequenceNo(1);
        redemption.setUserPromotionPublicId("66666666-6666-4666-8666-666666666666");
        redemption.setStatus(PromotionRedemptionStatus.CONFIRMED);
        redemption.setOriginalAmount(new BigDecimal("100000.00"));
        redemption.setDiscountAmount(new BigDecimal("20000.00"));
        redemption.setFinalAmount(new BigDecimal("80000.00"));

        Promotion promotion = new Promotion();
        promotion.setPublicId(redemption.getPromotionPublicId());
        promotion.setRedemptionCount(1);
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId(redemption.getCampaignPublicId());
        campaign.setBudgetAmount(new BigDecimal("1000000.00"));
        campaign.setBudgetUsed(new BigDecimal("20000.00"));
        campaign.setBudgetRemaining(new BigDecimal("980000.00"));
        campaign.setRedemptionCount(1);
        UserPromotion wallet = new UserPromotion();
        wallet.setPublicId(redemption.getUserPromotionPublicId());
        wallet.setUserPublicId("1001");
        wallet.setStatus(UserPromotionStatus.USED);
        wallet.setUsageCount(1);
        wallet.setMaxUsage(1);
        wallet.setValidTo(now.plusSeconds(3600));

        when(databaseTimeProvider.now()).thenReturn(now);
        when(reservationRepository.findByPublicIdForUpdate(reservation.getPublicId()))
                .thenReturn(Optional.of(reservation));
        when(redemptionRepository.findByReservationPublicIdForUpdate(
                reservation.getPublicId())).thenReturn(List.of(redemption));
        when(redemptionRepository.findByReservationPublicIdAndDeletedAtIsNull(
                reservation.getPublicId())).thenReturn(List.of(redemption));
        when(promotionRepository.findByPublicIdForUpdate(promotion.getPublicId()))
                .thenReturn(Optional.of(promotion));
        when(campaignRepository.findByPublicIdForUpdate(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));
        when(walletRepository.findByPublicIdForUpdate(wallet.getPublicId()))
                .thenReturn(Optional.of(wallet));

        var response = service.reverseConfirmed(
                reservation.getPublicId(),
                new CompensateRequest("PAYMENT_REVERSED", "Full refund"),
                "refund-idempotency", "BOOKING_SERVICE");

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.REVERSED);
        assertThat(redemption.getStatus()).isEqualTo(PromotionRedemptionStatus.REVERSED);
        assertThat(wallet.getStatus()).isEqualTo(UserPromotionStatus.AVAILABLE);
        assertThat(wallet.getUsageCount()).isZero();
        assertThat(promotion.getRedemptionCount()).isZero();
        assertThat(campaign.getRedemptionCount()).isZero();
        assertThat(campaign.getBudgetUsed()).isEqualByComparingTo("0.00");
        assertThat(campaign.getBudgetRemaining()).isEqualByComparingTo("1000000.00");
        ArgumentCaptor<PromotionRedemptionAdjustment> adjustmentCaptor =
                ArgumentCaptor.forClass(PromotionRedemptionAdjustment.class);
        verify(adjustmentRepository).save(adjustmentCaptor.capture());
        PromotionRedemptionAdjustment adjustment = adjustmentCaptor.getValue();
        assertThat(adjustment.getRedemptionPublicId()).isEqualTo(redemption.getPublicId());
        assertThat(adjustment.getReservationPublicId()).isEqualTo(reservation.getPublicId());
        assertThat(adjustment.getAdjustmentType()).isEqualTo("REVERSE");
        assertThat(adjustment.getDiscountAmount()).isEqualByComparingTo("20000.00");
        assertThat(adjustment.getReasonCode()).isEqualTo("PAYMENT_REVERSED");
        assertThat(adjustment.getReason()).isEqualTo("Full refund");
    }
}
