package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.enums.ReleaseReasonType;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CampaignEmergencyService {

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionReservationRepository reservationRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionReservationService reservationService;

    public CampaignEmergencyService(
            PromotionCampaignRepository campaignRepository,
            PromotionReservationRepository reservationRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionReservationService reservationService) {
        this.campaignRepository = campaignRepository;
        this.reservationRepository = reservationRepository;
        this.redemptionRepository = redemptionRepository;
        this.reservationService = reservationService;
    }

    @Transactional(readOnly = true)
    public ForceReleaseImpactResponse impact(String campaignPublicId) {
        requireCampaign(campaignPublicId);
        List<String> ids = reservationRepository.findIdsByCampaignAndStatus(
                campaignPublicId, ReservationStatus.ACTIVE);
        BigDecimal discount = redemptionRepository
                .sumActiveReservedDiscountByCampaign(campaignPublicId);
        return new ForceReleaseImpactResponse(campaignPublicId, ids.size(), ids.size(),
                discount, discount, ids.size(), 0);
    }

    public ForceReleaseImpactResponse forceRelease(
            String campaignPublicId, String campaignCode, String reason, String actor) {
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
        List<String> ids = reservationRepository.findIdsByCampaignAndStatus(
                campaignPublicId, ReservationStatus.ACTIVE);
        int released = 0;
        for (String reservationId : ids) {
            TransitionRequest request = new TransitionRequest(
                    ReleaseReasonType.CAMPAIGN_KILL_SWITCH, reason,
                    "PROMOTION_ADMIN", campaignPublicId, null);
            reservationService.release(reservationId, request,
                    "force-release:" + campaignPublicId + ":" + reservationId, actor);
            released++;
        }
        return new ForceReleaseImpactResponse(campaignPublicId,
                before.affectedReservationCount(), before.affectedBookingCount(),
                before.reservedDiscount(), before.budgetExposure(),
                before.bookingsRequiringRepriceOrCancel(), released);
    }

    private PromotionCampaign requireCampaign(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
    }
}
