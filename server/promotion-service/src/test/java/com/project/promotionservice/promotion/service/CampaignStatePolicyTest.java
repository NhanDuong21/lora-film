package com.project.promotionservice.promotion.service;

import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignAvailabilityStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignStatePolicyTest {

    private final CampaignStatePolicy policy = new CampaignStatePolicy();

    @Test
    void exhaustedCampaignCannotRenderOperationalActions() {
        CampaignResponse campaign = activeCampaign();
        campaign.setMaxRedemptions(1);
        campaign.setRedemptionCount(1);
        UserPrincipal operator = principal("99", List.of(
                "PROMOTION_VIEW", "PROMOTION_OPERATE"));

        policy.decorate(campaign, operator);

        assertThat(campaign.getAvailabilityStatus())
                .isEqualTo(CampaignAvailabilityStatus.EXHAUSTED);
        assertThat(campaign.getBlockedReasons())
                .contains("PROMOTION_QUOTA_EXHAUSTED");
        assertThat(campaign.getAllowedActions()).contains("VIEW", "PAUSE");
    }

    @Test
    void creatorNeverReceivesNormalApproveActionEvenWithCapability() {
        CampaignResponse campaign = activeCampaign();
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setCreatedBy("42");
        campaign.setApprovalStatus(CampaignApprovalStatus.PENDING);
        UserPrincipal creator = principal("42", List.of(
                "PROMOTION_VIEW", "PROMOTION_APPROVE_STANDARD",
                "PROMOTION_OVERRIDE"));

        policy.decorate(campaign, creator);

        assertThat(campaign.getAllowedActions()).doesNotContain("APPROVE", "REJECT");
        assertThat(campaign.getAllowedActions()).contains("OVERRIDE_APPROVE");
    }

    @Test
    void legalReviewOnlyAppearsAfterBusinessApproval() {
        CampaignResponse campaign = activeCampaign();
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setApprovalStatus(CampaignApprovalStatus.DRAFT);
        campaign.setLegalStatus(LegalStatus.PENDING);
        UserPrincipal legalReviewer = principal("99", List.of(
                "PROMOTION_VIEW", "PROMOTION_LEGAL_REVIEW"));

        policy.decorate(campaign, legalReviewer);
        assertThat(campaign.getAllowedActions()).doesNotContain("LEGAL_REVIEW");

        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        policy.decorate(campaign, legalReviewer);
        assertThat(campaign.getAllowedActions()).contains("LEGAL_REVIEW");
    }

    @Test
    void killedCampaignCreatesAnOperationalHoldMonitoringTask() {
        CampaignResponse campaign = activeCampaign();
        campaign.setStatus(CampaignStatus.KILLED);
        campaign.setKillSwitch(true);

        policy.decorate(campaign, principal("99", List.of("PROMOTION_VIEW")));

        assertThat(campaign.getPendingTasks()).contains("MONITOR_ACTIVE_HOLDS");
        assertThat(campaign.getAvailabilityStatus())
                .isEqualTo(CampaignAvailabilityStatus.CAMPAIGN_BLOCKED);
    }

    private CampaignResponse activeCampaign() {
        CampaignResponse campaign = new CampaignResponse();
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setLegalStatus(LegalStatus.PASSED);
        campaign.setStartAt(Instant.now().minusSeconds(60));
        campaign.setEndAt(Instant.now().plusSeconds(3600));
        campaign.setBudgetAmount(new BigDecimal("1000000"));
        campaign.setBudgetRemaining(new BigDecimal("500000"));
        campaign.setRedemptionCount(0);
        return campaign;
    }

    private UserPrincipal principal(String id, List<String> permissions) {
        return new UserPrincipal(Long.valueOf(id), id, null,
                List.of("MANAGER"), permissions, List.of());
    }
}
