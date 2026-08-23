package com.project.promotionservice.automation.dto;

import com.project.promotionservice.automation.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AutomationDtos {
    private AutomationDtos() { }

    public record PlaybookRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9_]{3,80}$") String code,
            @NotBlank @Size(max = 180) String name,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 60) String triggerType,
            @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String campaignPublicId,
            @Pattern(regexp = "^[a-fA-F0-9-]{36}$") String promotionPublicId,
            @NotBlank String configJson,
            @NotBlank String scopeJson,
            @DecimalMin("0.01") BigDecimal budgetLimit,
            @Min(1) Integer quotaLimit) { }

    public record PlaybookView(
            String publicId, Integer version, String code, String name,
            String description, PlaybookStatus status, Integer playbookVersion,
            String triggerType, String campaignPublicId, String promotionPublicId,
            Boolean testData, String environmentTag,
            String configJson, String scopeJson, BigDecimal budgetLimit,
            Integer quotaLimit, String submittedBy, Instant submittedAt,
            String approvedBy, Instant approvedAt, String configHash,
            Integer submittedPlaybookVersion, String submittedConfigHash,
            Integer approvedPlaybookVersion, String approvedConfigHash,
            String budgetPeriodKey, BigDecimal budgetCommitted,
            BigDecimal budgetRemaining, Integer quotaCommitted,
            String submittedByDisplayName, String approvedByDisplayName,
            QuotaView effectiveQuota, EntitlementSummary entitlements,
            String updatedBy, Instant updatedAt) { }

    public record QuotaView(
            BigDecimal estimatedUnitCost, Integer requestedQuota,
            Integer playbookIssued, Integer playbookQuotaRemaining,
            Integer budgetQuotaRemaining, Integer campaignIssued,
            Integer campaignQuotaRemaining, Integer promotionQuotaRemaining,
            Integer effectiveQuota, String limitingFactor) { }

    public record EntitlementSummary(
            Integer issued, Integer unredeemed, Integer reserved,
            Integer used, Integer expiredOrRevoked,
            BigDecimal walletIssuedCommitted, BigDecimal orderReserved,
            BigDecimal usedAmount, BigDecimal releasedAmount,
            BigDecimal remainingCapacity) { }

    public record IssueJobView(
            String publicId, IssueJobStatus status, Integer batchSize,
            Integer processedCount, Integer issuedCount, Integer skippedCount,
            Integer failedCount, String lastError, Instant startedAt,
            Instant completedAt) { }

    public record RunView(
            String publicId, String playbookCode, Integer playbookVersion,
            String triggerType, String triggerReference, String triggerSource,
            String runActor,
            String runActorDisplayName, String authorizedBy,
            String authorizedByDisplayName, String idempotencyKey,
            Boolean testData, String environmentTag,
            AutomationRunStatus status,
            Integer audienceCount, Integer issuedCount, Integer skippedCount,
            Integer failedCount, Instant startedAt, Instant completedAt,
            String approvedConfigHash, BigDecimal estimatedUnitCost,
            String configSnapshotJson, String snapshotPublicId,
            Integer eligibleCount, Integer excludedCount, Integer retryingCount,
            BigDecimal committedCost, List<ReasonCount> exclusionReasons,
            Long openAnomalyCount, List<AudienceMemberView> members,
            List<IssueJobView> jobs) { }

    public record ReasonCount(String reasonCode, Long count) { }

    public record AudienceMemberView(
            String publicId, String customerPublicId, AudienceMemberStatus status,
            String reasonCode, Integer attemptCount, String walletPublicId,
            String issuanceKey, BigDecimal committedAmount,
            Boolean testData, String environmentTag) { }

    public record AnomalyCaseView(
            String publicId, String runPublicId, String audienceMemberPublicId,
            String playbookCode, String businessName, String customerPublicId,
            String sourceReference, String summary, String technicalReasonCode,
            BigDecimal costAmount, Boolean testData, String environmentTag,
            AnomalyCaseStatus status, String assignedTo,
            String assignedToDisplayName, AnomalyResolution resolution,
            String resolutionNote, String resolvedBy,
            String resolvedByDisplayName, Instant resolvedAt, Instant createdAt) { }

    public record ResolveAnomalyRequest(
            @NotNull AnomalyResolution resolution,
            @NotBlank @Size(max = 1000) String resolutionNote) { }

    public record CampaignAutomationView(
            List<PlaybookView> playbooks, RunView latestRun,
            EntitlementSummary entitlements) { }

    public record OpportunityView(
            String code, String title, String insight, String reason,
            Integer relatedCount, Integer excludedCount, BigDecimal expectedCost,
            BigDecimal monthlyBudget, BigDecimal budgetRemaining,
            String state, String actionLabel, String actionTarget,
            PlaybookView playbook) { }
}
