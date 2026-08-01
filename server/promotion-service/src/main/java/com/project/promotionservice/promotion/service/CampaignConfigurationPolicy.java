package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CampaignConfigurationPolicy {

    public void requireEditable(PromotionCampaign campaign) {
        if (!isEditable(campaign)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign configuration is locked after submission; reject it before editing",
                    HttpStatus.CONFLICT);
        }
    }

    public boolean isEditable(PromotionCampaign campaign) {
        boolean editableApproval = campaign.getApprovalStatus() == CampaignApprovalStatus.DRAFT
                || campaign.getApprovalStatus() == CampaignApprovalStatus.REJECTED;
        return campaign.getStatus() == CampaignStatus.DRAFT && editableApproval;
    }

    public void markConfigurationChanged(PromotionCampaign campaign, String actor) {
        campaign.setApprovalStatus(CampaignApprovalStatus.DRAFT);
        campaign.setLegalStatus(LegalStatus.PENDING);
        campaign.setApprovedAt(null);
        campaign.setApprovedBy(null);
        campaign.setLegalNotificationRef(null);
        campaign.setUpdatedBy(actor);
    }
}
