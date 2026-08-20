package com.project.promotionservice.promotion.service;

import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Versioned internal policy; campaign authors cannot toggle this decision. */
@Service
public class CampaignCompliancePolicy {

    private final PromotionRepository promotionRepository;
    private final BigDecimal notificationBudgetThreshold;
    private final String policyVersion;

    public CampaignCompliancePolicy(
            PromotionRepository promotionRepository,
            @Value("${promotion.compliance.notification-budget-threshold:50000000.00}")
            BigDecimal notificationBudgetThreshold,
            @Value("${promotion.compliance.policy-version:2026-08-v1}")
            String policyVersion) {
        this.promotionRepository = promotionRepository;
        this.notificationBudgetThreshold = notificationBudgetThreshold;
        this.policyVersion = policyVersion;
    }

    public Decision evaluate(PromotionCampaign campaign) {
        boolean globalScope = campaign.getScopeType() == null
                || campaign.getScopeType() == CampaignScopeType.GLOBAL;
        boolean publicBenefit = promotionRepository
                .existsByCampaignPublicIdAndPublicVisibleTrueAndDeletedAtIsNull(
                        campaign.getPublicId());
        boolean highBudget = campaign.getBudgetAmount() != null
                && campaign.getBudgetAmount().compareTo(notificationBudgetThreshold) >= 0;
        boolean required = globalScope || publicBenefit || highBudget;
        String reason = required
                ? "Chính sách nội bộ " + policyVersion
                    + ": cần rà soát với campaign toàn hệ thống, công khai hoặc ngân sách cao"
                : "Chính sách nội bộ " + policyVersion
                    + ": quyền lợi riêng theo rạp đang dưới ngưỡng phải thông báo";
        return new Decision(required, reason, policyVersion);
    }

    public record Decision(
            boolean legalNotificationRequired,
            String reason,
            String policyVersion) {
    }
}
