package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.integration.client.CampaignEmergencyDependencyClient;
import com.project.promotionservice.promotion.dto.response.ForceReleaseBookingImpact;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.ForceReleaseDisposition;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReleaseReasonType;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CampaignEmergencyService {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionReservationRepository reservationRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionReservationService reservationService;
    private final CampaignEmergencyDependencyClient dependencyClient;

    public CampaignEmergencyService(
            PromotionCampaignRepository campaignRepository,
            PromotionReservationRepository reservationRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionReservationService reservationService,
            CampaignEmergencyDependencyClient dependencyClient) {
        this.campaignRepository = campaignRepository;
        this.reservationRepository = reservationRepository;
        this.redemptionRepository = redemptionRepository;
        this.reservationService = reservationService;
        this.dependencyClient = dependencyClient;
    }

    @Transactional(readOnly = true)
    public ForceReleaseImpactResponse impact(String campaignPublicId) {
        PromotionCampaign campaign = requireCampaign(campaignPublicId);
        List<PromotionReservation> reservations = reservationRepository.findByCampaignAndStatus(
                campaignPublicId, ReservationStatus.ACTIVE);
        BigDecimal discount = redemptionRepository
                .sumActiveReservedDiscountByCampaign(campaignPublicId);
        List<String> bookingIds = reservations.stream()
                .map(PromotionReservation::getBookingPublicId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        CampaignEmergencyDependencyClient.PaymentSnapshot payments = null;
        String paymentError = null;
        try {
            payments = dependencyClient.payments(bookingIds);
        } catch (CampaignEmergencyDependencyClient.DependencyUnavailableException exception) {
            paymentError = exception.getMessage();
        }

        Map<String, CampaignEmergencyDependencyClient.BookingSnapshot> bookings = new HashMap<>();
        Map<String, String> bookingErrors = new HashMap<>();
        for (String bookingId : bookingIds) {
            try {
                bookings.put(bookingId, dependencyClient.booking(bookingId));
            } catch (CampaignEmergencyDependencyClient.DependencyUnavailableException exception) {
                bookingErrors.put(bookingId, exception.getMessage());
            }
        }

        List<ForceReleaseBookingImpact> items = new ArrayList<>();
        for (PromotionReservation reservation : reservations) {
            items.add(classify(reservation, bookings, bookingErrors, payments, paymentError));
        }
        items.sort(Comparator.comparing(ForceReleaseBookingImpact::reservationPublicId));
        long safe = count(items, ForceReleaseDisposition.SAFE_TO_RELEASE);
        long reprice = count(items, ForceReleaseDisposition.REPRICE_REQUIRED);
        long blocked = items.size() - safe - reprice;
        String impactToken = impactToken(campaign, items);
        return new ForceReleaseImpactResponse(
                campaignPublicId, reservations.size(), bookingIds.size(),
                discount, discount, reprice + blocked, 0,
                campaign.getVersion(), impactToken, Instant.now(), safe, reprice, blocked,
                reprice == 0 && blocked == 0, List.copyOf(items));
    }

    @Transactional
    public ForceReleaseImpactResponse forceRelease(
            String campaignPublicId, String campaignCode, String reason,
            String expectedImpactToken, Integer expectedCampaignVersion,
            String idempotencyKey, String actor) {
        PromotionCampaign campaign = requireCampaign(campaignPublicId);
        if (campaign.getStatus() != CampaignStatus.KILLED
                && !Boolean.TRUE.equals(campaign.getKillSwitch())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Force release is only available after kill switch", HttpStatus.CONFLICT);
        }
        if (!campaign.getCode().equals(campaignCode)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign code confirmation does not match", HttpStatus.BAD_REQUEST);
        }
        ForceReleaseImpactResponse before = impact(campaignPublicId);
        if (!java.util.Objects.equals(campaign.getVersion(), expectedCampaignVersion)
                || !MessageDigest.isEqual(
                before.impactToken().getBytes(StandardCharsets.UTF_8),
                expectedImpactToken.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Impact snapshot changed; refresh impact before confirming",
                    HttpStatus.CONFLICT);
        }
        if (!before.executable()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Release blocked: dependent bookings must be repriced/cancelled and payments must be terminal",
                    HttpStatus.CONFLICT);
        }
        int released = 0;
        for (ForceReleaseBookingImpact item : before.bookings()) {
            if (item.disposition() != ForceReleaseDisposition.SAFE_TO_RELEASE) continue;
            TransitionRequest request = new TransitionRequest(
                    ReleaseReasonType.CAMPAIGN_KILL_SWITCH, reason,
                    "PROMOTION_ADMIN", campaignPublicId, null);
            reservationService.release(item.reservationPublicId(), request,
                    "force-release:" + campaignPublicId + ":" + idempotencyKey
                            + ":" + item.reservationPublicId(), actor);
            released++;
        }
        return new ForceReleaseImpactResponse(campaignPublicId,
                before.affectedReservationCount(), before.affectedBookingCount(),
                before.reservedDiscount(), before.budgetExposure(),
                before.bookingsRequiringRepriceOrCancel(), released,
                before.campaignVersion(), before.impactToken(),
                before.generatedAt(),
                before.safeToReleaseCount(), before.repriceRequiredCount(),
                before.blockedCount(), before.executable(), before.bookings());
    }

    private ForceReleaseBookingImpact classify(
            PromotionReservation reservation,
            Map<String, CampaignEmergencyDependencyClient.BookingSnapshot> bookings,
            Map<String, String> bookingErrors,
            CampaignEmergencyDependencyClient.PaymentSnapshot payments,
            String paymentError) {
        String bookingId = reservation.getBookingPublicId();
        if (bookingId == null || bookingId.isBlank()) {
            return item(reservation, null,
                    ForceReleaseDisposition.BLOCKED_MISSING_BOOKING_REFERENCE,
                    "Reservation has no stable Booking reference");
        }
        if (paymentError != null) {
            return item(reservation, null,
                    ForceReleaseDisposition.BLOCKED_DEPENDENCY_UNAVAILABLE,
                    paymentError);
        }
        String bookingError = bookingErrors.get(bookingId);
        if (bookingError != null) {
            return item(reservation, null,
                    ForceReleaseDisposition.BLOCKED_DEPENDENCY_UNAVAILABLE,
                    bookingError);
        }
        CampaignEmergencyDependencyClient.BookingSnapshot booking = bookings.get(bookingId);
        if (booking == null || booking.bookingStatus() == null) {
            return item(reservation, null,
                    ForceReleaseDisposition.BLOCKED_DEPENDENCY_UNAVAILABLE,
                    "Booking status is unavailable");
        }
        if (payments.successfulPaymentBookingPublicIds().contains(bookingId)) {
            return item(reservation, booking.bookingStatus(),
                    ForceReleaseDisposition.BLOCKED_PAYMENT_SUCCESSFUL,
                    "A successful payment exists for this booking");
        }
        if (payments.activePaymentBookingPublicIds().contains(bookingId)) {
            return item(reservation, booking.bookingStatus(),
                    ForceReleaseDisposition.BLOCKED_PAYMENT_IN_PROGRESS,
                    "An active payment attempt is still in progress");
        }
        return switch (booking.bookingStatus()) {
            case "CANCELLED", "EXPIRED" -> item(reservation, booking.bookingStatus(),
                    ForceReleaseDisposition.SAFE_TO_RELEASE,
                    "Booking is terminal without a successful payment");
            case "PENDING_PAYMENT" -> item(reservation, booking.bookingStatus(),
                    ForceReleaseDisposition.REPRICE_REQUIRED,
                    "Booking total must be repriced or booking cancelled first");
            case "CONFIRMED", "COMPLETED", "REFUNDED" -> item(
                    reservation, booking.bookingStatus(),
                    ForceReleaseDisposition.BLOCKED_BOOKING_TERMINAL,
                    "Confirmed or completed booking cannot lose its promotion hold");
            default -> item(reservation, booking.bookingStatus(),
                    ForceReleaseDisposition.BLOCKED_DEPENDENCY_UNAVAILABLE,
                    "Unknown Booking status");
        };
    }

    private ForceReleaseBookingImpact item(
            PromotionReservation reservation, String bookingStatus,
            ForceReleaseDisposition disposition, String reason) {
        return new ForceReleaseBookingImpact(
                reservation.getPublicId(), reservation.getBookingPublicId(),
                bookingStatus, disposition, reason);
    }

    private long count(
            List<ForceReleaseBookingImpact> items,
            ForceReleaseDisposition disposition) {
        return items.stream().filter(item -> item.disposition() == disposition).count();
    }

    private String impactToken(
            PromotionCampaign campaign, List<ForceReleaseBookingImpact> items) {
        StringBuilder canonical = new StringBuilder()
                .append(campaign.getPublicId()).append('|')
                .append(campaign.getVersion()).append('\n');
        items.forEach(item -> canonical.append(item.reservationPublicId()).append('|')
                .append(item.bookingPublicId()).append('|')
                .append(item.bookingStatus()).append('|')
                .append(item.disposition()).append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private PromotionCampaign requireCampaign(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
    }
}
