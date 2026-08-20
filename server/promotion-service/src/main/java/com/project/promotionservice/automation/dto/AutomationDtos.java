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
            String configJson, String scopeJson, BigDecimal budgetLimit,
            Integer quotaLimit, String submittedBy, Instant submittedAt,
            String approvedBy, Instant approvedAt, String configHash,
            Integer submittedPlaybookVersion, String submittedConfigHash,
            Integer approvedPlaybookVersion, String approvedConfigHash,
            String budgetPeriodKey, BigDecimal budgetCommitted,
            BigDecimal budgetRemaining, Integer quotaCommitted,
            String updatedBy, Instant updatedAt) { }

    public record IssueJobView(
            String publicId, IssueJobStatus status, Integer batchSize,
            Integer processedCount, Integer issuedCount, Integer skippedCount,
            Integer failedCount, String lastError, Instant startedAt,
            Instant completedAt) { }

    public record RunView(
            String publicId, String playbookCode, Integer playbookVersion,
            String triggerType, String triggerReference, String runActor,
            String authorizedBy, String idempotencyKey, AutomationRunStatus status,
            Integer audienceCount, Integer issuedCount, Integer skippedCount,
            Integer failedCount, Instant startedAt, Instant completedAt,
            String approvedConfigHash, BigDecimal estimatedUnitCost,
            String configSnapshotJson, List<IssueJobView> jobs) { }

    public record OpportunityView(
            String code, String title, String insight, String reason,
            Integer relatedCount, Integer excludedCount, BigDecimal expectedCost,
            BigDecimal monthlyBudget, BigDecimal budgetRemaining,
            String state, String actionLabel, String actionTarget,
            PlaybookView playbook) { }
}
