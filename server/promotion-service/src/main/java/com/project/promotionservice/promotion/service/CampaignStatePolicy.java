package com.project.promotionservice.promotion.service;

import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignAvailabilityStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Single source of truth for admin campaign actions and availability. */
@Component
public final class CampaignStatePolicy {

    private BigDecimal highBudgetThreshold = new BigDecimal("50000000.00");

    @Value("${promotion.approval.high-budget-threshold:50000000.00}")
    void setHighBudgetThreshold(BigDecimal highBudgetThreshold) {
        this.highBudgetThreshold = highBudgetThreshold;
    }

    public CampaignResponse decorate(CampaignResponse campaign, UserPrincipal principal) {
        if (campaign == null) return null;
        Set<String> permissions = principal == null
                ? Set.of() : Set.copyOf(principal.getPermissions());
        String actor = principal == null || principal.getId() == null
                ? null : principal.getId().toString();
        CampaignAvailabilityStatus availability = availability(campaign, Instant.now());
        List<String> blocked = blockedReasons(campaign, availability);
        List<String> tasks = pendingTasks(campaign);
        List<String> actions = allowedActions(campaign, permissions, actor);
        campaign.setBusinessStatus(campaign.getStatus());
        campaign.setAvailabilityStatus(availability);
        campaign.setBlockedReasons(blocked);
        campaign.setPendingTasks(tasks);
        campaign.setAllowedActions(actions);
        return campaign;
    }

    CampaignAvailabilityStatus availability(CampaignResponse campaign, Instant now) {
        if (campaign.getStatus() == CampaignStatus.KILLED
                || campaign.getStatus() == CampaignStatus.CANCELLED
                || Boolean.TRUE.equals(campaign.getKillSwitch())) {
            return CampaignAvailabilityStatus.CAMPAIGN_BLOCKED;
        }
        if (campaign.getStatus() == CampaignStatus.PAUSED) {
            return CampaignAvailabilityStatus.PAUSED;
        }
        if (campaign.getEndAt() != null && !now.isBefore(campaign.getEndAt())) {
            return CampaignAvailabilityStatus.EXPIRED;
        }
        if (campaign.getMaxRedemptions() != null && campaign.getMaxRedemptions() > 0
                && campaign.getRedemptionCount() != null
                && campaign.getRedemptionCount() >= campaign.getMaxRedemptions()) {
            return CampaignAvailabilityStatus.EXHAUSTED;
        }
        BigDecimal remaining = campaign.getBudgetRemaining();
        if (campaign.getBudgetAmount() != null && campaign.getBudgetAmount().signum() > 0
                && (remaining == null || remaining.signum() <= 0)) {
            return CampaignAvailabilityStatus.BUDGET_EXHAUSTED;
        }
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || (campaign.getStartAt() != null && now.isBefore(campaign.getStartAt()))) {
            return CampaignAvailabilityStatus.NOT_STARTED;
        }
        return CampaignAvailabilityStatus.AVAILABLE;
    }

    private List<String> blockedReasons(
            CampaignResponse campaign, CampaignAvailabilityStatus availability) {
        List<String> reasons = new ArrayList<>();
        if (campaign.getApprovalStatus() != CampaignApprovalStatus.APPROVED) {
            reasons.add("CAMPAIGN_APPROVAL_REQUIRED");
        }
        if (campaign.getLegalStatus() != LegalStatus.PASSED) {
            reasons.add("CAMPAIGN_LEGAL_REVIEW_REQUIRED");
        }
        switch (availability) {
            case EXHAUSTED -> reasons.add("PROMOTION_QUOTA_EXHAUSTED");
            case BUDGET_EXHAUSTED -> reasons.add("CAMPAIGN_BUDGET_EXHAUSTED");
            case PAUSED -> reasons.add("CAMPAIGN_PAUSED");
            case CAMPAIGN_BLOCKED -> reasons.add(Boolean.TRUE.equals(campaign.getKillSwitch())
                    || campaign.getStatus() == CampaignStatus.KILLED
                    ? "CAMPAIGN_KILL_SWITCHED" : "CAMPAIGN_CANCELLED");
            case EXPIRED -> reasons.add("CAMPAIGN_EXPIRED");
            default -> { }
        }
        return List.copyOf(reasons);
    }

    private List<String> pendingTasks(CampaignResponse campaign) {
        List<String> tasks = new ArrayList<>();
        if (campaign.getStatus() == CampaignStatus.KILLED
                || Boolean.TRUE.equals(campaign.getKillSwitch())) {
            tasks.add("MONITOR_ACTIVE_HOLDS");
        }
        if (campaign.getApprovalStatus() == CampaignApprovalStatus.PENDING) {
            tasks.add("APPROVAL_DECISION");
        }
        if (campaign.getApprovalStatus() == CampaignApprovalStatus.APPROVED
                && campaign.getLegalStatus() == LegalStatus.PENDING) {
            tasks.add("LEGAL_REVIEW");
        }
        if (campaign.getApprovalStatus() == CampaignApprovalStatus.APPROVED
                && campaign.getLegalStatus() == LegalStatus.PASSED
                && campaign.getStatus() == CampaignStatus.DRAFT) {
            tasks.add("PUBLISH_CAMPAIGN");
        }
        return List.copyOf(tasks);
    }

    private List<String> allowedActions(
            CampaignResponse campaign, Set<String> permissions, String actor) {
        List<String> actions = new ArrayList<>();
        if (permissions.contains("PROMOTION_VIEW")) actions.add("VIEW");
        if (permissions.contains("PROMOTION_AUTHOR")) actions.add("CLONE");
        if (campaign.getStatus() == CampaignStatus.DRAFT) {
            if (permissions.contains("PROMOTION_AUTHOR")
                    && campaign.getApprovalStatus() != CampaignApprovalStatus.PENDING) {
                actions.add("EDIT");
                actions.add("SUBMIT");
                actions.add("DELETE");
            }
            boolean selfApproval = actor != null && actor.equalsIgnoreCase(campaign.getCreatedBy());
            if (campaign.getApprovalStatus() == CampaignApprovalStatus.PENDING && !selfApproval) {
                String approvalCapability = campaign.getRequiredApprovalCapability();
                if (approvalCapability == null || approvalCapability.isBlank()) {
                    approvalCapability = campaign.getBudgetAmount() != null
                            && campaign.getBudgetAmount().compareTo(highBudgetThreshold) > 0
                            ? "PROMOTION_APPROVE_HIGH_BUDGET"
                            : "PROMOTION_APPROVE_STANDARD";
                }
                if (permissions.contains(approvalCapability)) {
                    actions.add("APPROVE");
                    actions.add("REJECT");
                }
            }
            if (permissions.contains("PROMOTION_LEGAL_REVIEW")
                    && campaign.getApprovalStatus() == CampaignApprovalStatus.APPROVED) {
                actions.add("LEGAL_REVIEW");
            }
            if (permissions.contains("PROMOTION_PUBLISH")
                    && campaign.getApprovalStatus() == CampaignApprovalStatus.APPROVED
                    && campaign.getLegalStatus() == LegalStatus.PASSED) actions.add("PUBLISH");
        }
        if (permissions.contains("PROMOTION_OPERATE")) {
            if (campaign.getStatus() == CampaignStatus.SCHEDULED) actions.add("PAUSE");
            if (campaign.getStatus() == CampaignStatus.ACTIVE) actions.add("PAUSE");
            if (campaign.getStatus() == CampaignStatus.PAUSED
                    && !Boolean.TRUE.equals(campaign.getKillSwitch())
                    && (campaign.getEndAt() == null
                    || Instant.now().isBefore(campaign.getEndAt()))) actions.add("RESUME");
            if (campaign.getStatus() == CampaignStatus.SCHEDULED
                    || campaign.getStatus() == CampaignStatus.ACTIVE
                    || campaign.getStatus() == CampaignStatus.PAUSED) actions.add("CANCEL");
        }
        if (permissions.contains("PROMOTION_EMERGENCY_STOP")
                && (campaign.getStatus() == CampaignStatus.SCHEDULED
                || campaign.getStatus() == CampaignStatus.ACTIVE
                || campaign.getStatus() == CampaignStatus.PAUSED)) {
            actions.add("KILL_SWITCH");
        }
        if (permissions.contains("PROMOTION_FORCE_RELEASE")
                && (campaign.getStatus() == CampaignStatus.KILLED
                || Boolean.TRUE.equals(campaign.getKillSwitch()))) {
            actions.add("FORCE_RELEASE_HOLDS");
        }
        if (permissions.contains("PROMOTION_OVERRIDE")
                && campaign.getStatus() == CampaignStatus.DRAFT
                && campaign.getApprovalStatus() == CampaignApprovalStatus.PENDING) {
            actions.add("OVERRIDE_APPROVE");
        }
        return List.copyOf(actions);
    }
}
