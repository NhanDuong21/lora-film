package com.project.promotionservice.promotion.manager;

import com.project.promotionservice.automation.enums.AnomalyCaseStatus;
import com.project.promotionservice.automation.enums.AutomationRunStatus;
import com.project.promotionservice.automation.enums.PlaybookStatus;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;

import java.time.Instant;
import java.util.List;

/** Safe, cinema-scoped read models for the manager workspace. */
public final class ManagerPromotionDtos {
    private ManagerPromotionDtos() { }

    public record Capabilities(
            boolean canViewCinemaPromotions,
            boolean canLaunchApprovedTemplate,
            boolean canDistributeLocalBenefit,
            boolean canViewLocalIncidents,
            boolean canProposeCampaign) { }

    public record Workspace(
            String cinemaPublicId,
            Capabilities capabilities,
            long activeCampaignCount,
            long upcomingCampaignCount,
            long lowQuotaBenefitCount,
            long openIncidentCount,
            List<Task> tasks) { }

    public record Task(
            String key,
            String type,
            String title,
            String description,
            String targetView,
            Instant dueAt) { }

    public record Campaign(
            String publicId,
            String code,
            String name,
            String description,
            CampaignStatus status,
            CampaignApprovalStatus approvalStatus,
            String source,
            boolean readOnly,
            Instant startAt,
            Instant endAt,
            Integer quota,
            Integer used,
            Integer remaining,
            List<Benefit> benefits) { }

    public record Benefit(
            String publicId,
            String campaignPublicId,
            String campaignName,
            String name,
            String description,
            PromotionType promotionType,
            PromotionStatus status,
            PromotionDistributionMode distributionMode,
            Instant validFrom,
            Instant validTo,
            Integer quota,
            Integer used,
            Integer remaining,
            boolean canDistribute,
            String staffGuidance) { }

    public record Automation(
            String publicId,
            String code,
            String name,
            String description,
            PlaybookStatus status,
            String triggerType,
            AutomationRunStatus latestRunStatus,
            Instant latestRunAt,
            boolean readOnly) { }

    public record Incident(
            String publicId,
            String businessName,
            String summary,
            AnomalyCaseStatus status,
            String assignedTo,
            Instant createdAt) { }
}
