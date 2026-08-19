package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.integration.client.CampaignEmergencyDependencyClient;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.ForceReleaseDisposition;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignEmergencyServiceTest {

    @Mock PromotionCampaignRepository campaignRepository;
    @Mock PromotionReservationRepository reservationRepository;
    @Mock PromotionRedemptionRepository redemptionRepository;
    @Mock PromotionReservationService reservationService;
    @Mock CampaignEmergencyDependencyClient dependencyClient;

    private CampaignEmergencyService service;
    private PromotionCampaign campaign;
    private PromotionReservation reservation;

    @BeforeEach
    void setUp() {
        service = new CampaignEmergencyService(campaignRepository,
                reservationRepository, redemptionRepository,
                reservationService, dependencyClient);
        campaign = new PromotionCampaign();
        campaign.setPublicId("9cc9f0aa-e17b-4eec-ae80-5bf6b3062f62");
        campaign.setVersion(7);
        campaign.setCode("EMERGENCY-7");
        campaign.setStatus(CampaignStatus.KILLED);
        campaign.setKillSwitch(true);
        reservation = new PromotionReservation();
        reservation.setPublicId("938afbc0-8dc8-4e8d-b8c5-57ac7fae2dcf");
        reservation.setBookingPublicId("bbf7ffbe-cbd6-4036-b5c5-ed45910a108b");
        reservation.setStatus(ReservationStatus.ACTIVE);
        when(campaignRepository.findByPublicId(campaign.getPublicId()))
                .thenReturn(Optional.of(campaign));
        when(reservationRepository.findByCampaignAndStatus(
                campaign.getPublicId(), ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));
        when(redemptionRepository.sumActiveReservedDiscountByCampaign(
                campaign.getPublicId())).thenReturn(new BigDecimal("25000"));
    }

    @Test
    void impactFailsClosedWhenPaymentServiceIsUnavailable() {
        when(dependencyClient.payments(any())).thenThrow(
                new CampaignEmergencyDependencyClient.DependencyUnavailableException(
                        "Payment Service is unavailable"));
        when(dependencyClient.booking(reservation.getBookingPublicId()))
                .thenReturn(new CampaignEmergencyDependencyClient.BookingSnapshot(
                        reservation.getBookingPublicId(), "CANCELLED"));

        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());

        assertThat(impact.executable()).isFalse();
        assertThat(impact.blockedCount()).isEqualTo(1);
        assertThat(impact.bookings().getFirst().disposition())
                .isEqualTo(ForceReleaseDisposition.BLOCKED_DEPENDENCY_UNAVAILABLE);
    }

    @Test
    void pendingBookingRequiresRepriceAndCannotBeReleased() {
        stubPayments(Set.of(), Set.of());
        stubBooking("PENDING_PAYMENT");
        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());

        assertThat(impact.executable()).isFalse();
        assertThat(impact.repriceRequiredCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                impact.impactToken(), impact.campaignVersion(), "command-1", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("repriced/cancelled");
        verify(reservationService, never()).release(
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void activePaymentBlocksReleaseEvenWhenBookingIsCancelled() {
        stubPayments(Set.of(reservation.getBookingPublicId()), Set.of());
        stubBooking("CANCELLED");

        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());

        assertThat(impact.executable()).isFalse();
        assertThat(impact.bookings().getFirst().disposition())
                .isEqualTo(ForceReleaseDisposition.BLOCKED_PAYMENT_IN_PROGRESS);
    }

    @Test
    void cancelledUnpaidBookingCanReleaseWithMatchingSnapshot() {
        stubPayments(Set.of(), Set.of());
        stubBooking("CANCELLED");
        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());

        ForceReleaseImpactResponse result = service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                impact.impactToken(), impact.campaignVersion(), "command-2", "123");

        assertThat(result.executable()).isTrue();
        assertThat(result.releasedCount()).isEqualTo(1);
        verify(reservationService).release(
                org.mockito.ArgumentMatchers.eq(reservation.getPublicId()),
                any(), org.mockito.ArgumentMatchers.contains("command-2"),
                org.mockito.ArgumentMatchers.eq("123"));
    }

    @Test
    void staleImpactTokenIsRejectedBeforeMutation() {
        stubPayments(Set.of(), Set.of());
        stubBooking("CANCELLED");

        assertThatThrownBy(() -> service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                "0".repeat(64), campaign.getVersion(), "command-3", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot changed");
        verify(reservationService, never()).release(
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void executeRevalidatesAndBlocksWhenPaymentStartsAfterSafeImpact() {
        when(dependencyClient.payments(any())).thenReturn(
                new CampaignEmergencyDependencyClient.PaymentSnapshot(Set.of(), Set.of()),
                new CampaignEmergencyDependencyClient.PaymentSnapshot(
                        Set.of(reservation.getBookingPublicId()), Set.of()));
        stubBooking("CANCELLED");
        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());
        assertThat(impact.executable()).isTrue();

        assertThatThrownBy(() -> service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                impact.impactToken(), impact.campaignVersion(), "command-race-payment", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot changed");

        verify(reservationService, never()).release(
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void executeRevalidatesAndBlocksWhenBookingLifecycleChangesAfterImpact() {
        stubPayments(Set.of(), Set.of());
        when(dependencyClient.booking(reservation.getBookingPublicId())).thenReturn(
                new CampaignEmergencyDependencyClient.BookingSnapshot(
                        reservation.getBookingPublicId(), "CANCELLED"),
                new CampaignEmergencyDependencyClient.BookingSnapshot(
                        reservation.getBookingPublicId(), "PENDING_PAYMENT"));
        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());
        assertThat(impact.executable()).isTrue();

        assertThatThrownBy(() -> service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                impact.impactToken(), impact.campaignVersion(), "command-race-booking", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot changed");

        verify(reservationService, never()).release(
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void executeFailsClosedWhenPaymentAssessmentTimesOutAfterSafeImpact() {
        when(dependencyClient.payments(any()))
                .thenReturn(new CampaignEmergencyDependencyClient.PaymentSnapshot(
                        Set.of(), Set.of()))
                .thenThrow(new CampaignEmergencyDependencyClient.DependencyUnavailableException(
                        "Payment Service timed out"));
        stubBooking("CANCELLED");
        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());
        assertThat(impact.executable()).isTrue();

        assertThatThrownBy(() -> service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                impact.impactToken(), impact.campaignVersion(), "command-timeout-payment", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot changed");

        verify(reservationService, never()).release(
                anyString(), any(), anyString(), anyString());
    }

    @Test
    void executeFailsClosedWhenBookingAssessmentTimesOutAfterSafeImpact() {
        stubPayments(Set.of(), Set.of());
        when(dependencyClient.booking(reservation.getBookingPublicId()))
                .thenReturn(new CampaignEmergencyDependencyClient.BookingSnapshot(
                        reservation.getBookingPublicId(), "CANCELLED"))
                .thenThrow(new CampaignEmergencyDependencyClient.DependencyUnavailableException(
                        "Booking Service timed out"));
        ForceReleaseImpactResponse impact = service.impact(campaign.getPublicId());
        assertThat(impact.executable()).isTrue();

        assertThatThrownBy(() -> service.forceRelease(
                campaign.getPublicId(), campaign.getCode(), "Kill switch",
                impact.impactToken(), impact.campaignVersion(), "command-timeout-booking", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("snapshot changed");

        verify(reservationService, never()).release(
                anyString(), any(), anyString(), anyString());
    }

    private void stubBooking(String status) {
        when(dependencyClient.booking(reservation.getBookingPublicId()))
                .thenReturn(new CampaignEmergencyDependencyClient.BookingSnapshot(
                        reservation.getBookingPublicId(), status));
    }

    private void stubPayments(Set<String> active, Set<String> successful) {
        when(dependencyClient.payments(any())).thenReturn(
                new CampaignEmergencyDependencyClient.PaymentSnapshot(
                        active, successful));
    }
}
