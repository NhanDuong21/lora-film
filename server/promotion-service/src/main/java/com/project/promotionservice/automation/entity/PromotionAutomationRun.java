package com.project.promotionservice.automation.entity;

import com.project.promotionservice.automation.enums.AutomationRunStatus;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_automation_runs")
public class PromotionAutomationRun extends BaseAuditableEntity {
    @Column(name = "playbook_public_id", nullable = false, length = 36)
    private String playbookPublicId;
    @Column(name = "campaign_public_id", nullable = false, length = 36)
    private String campaignPublicId;
    @Column(name = "promotion_public_id", nullable = false, length = 36)
    private String promotionPublicId;
    @Column(name = "test_data", nullable = false)
    private Boolean testData = false;
    @Column(name = "environment_tag", nullable = false, length = 30)
    private String environmentTag = "BUSINESS";
    @Column(name = "playbook_code", nullable = false, length = 80)
    private String playbookCode;
    @Column(name = "playbook_version", nullable = false)
    private Integer playbookVersion;
    @Column(name = "approved_config_hash", nullable = false, length = 64)
    private String approvedConfigHash;
    @Column(name = "config_snapshot_json", nullable = false, columnDefinition = "json")
    private String configSnapshotJson;
    @Column(name = "scope_snapshot_json", nullable = false, columnDefinition = "json")
    private String scopeSnapshotJson;
    @Column(name = "budget_snapshot", precision = 18, scale = 2)
    private BigDecimal budgetSnapshot;
    @Column(name = "quota_snapshot")
    private Integer quotaSnapshot;
    @Column(name = "estimated_unit_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedUnitCost = BigDecimal.ZERO;
    @Column(name = "trigger_type", nullable = false, length = 60)
    private String triggerType;
    @Column(name = "trigger_reference", length = 180)
    private String triggerReference;
    @Column(name = "trigger_source", nullable = false, length = 30)
    private String triggerSource = "SCHEDULE";
    @Column(name = "run_actor", nullable = false, length = 36)
    private String runActor = "SYSTEM";
    @Column(name = "authorized_by", nullable = false, length = 36)
    private String authorizedBy;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 180)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AutomationRunStatus status = AutomationRunStatus.PENDING;
    @Column(name = "audience_count", nullable = false)
    private Integer audienceCount = 0;
    @Column(name = "issued_count", nullable = false)
    private Integer issuedCount = 0;
    @Column(name = "skipped_count", nullable = false)
    private Integer skippedCount = 0;
    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    public String getPlaybookPublicId() { return playbookPublicId; }
    public void setPlaybookPublicId(String value) { playbookPublicId = value; }
    public String getCampaignPublicId() { return campaignPublicId; }
    public void setCampaignPublicId(String value) { campaignPublicId = value; }
    public String getPromotionPublicId() { return promotionPublicId; }
    public void setPromotionPublicId(String value) { promotionPublicId = value; }
    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
    public String getPlaybookCode() { return playbookCode; }
    public void setPlaybookCode(String value) { playbookCode = value; }
    public Integer getPlaybookVersion() { return playbookVersion; }
    public void setPlaybookVersion(Integer value) { playbookVersion = value; }
    public String getApprovedConfigHash() { return approvedConfigHash; }
    public void setApprovedConfigHash(String value) { approvedConfigHash = value; }
    public String getConfigSnapshotJson() { return configSnapshotJson; }
    public void setConfigSnapshotJson(String value) { configSnapshotJson = value; }
    public String getScopeSnapshotJson() { return scopeSnapshotJson; }
    public void setScopeSnapshotJson(String value) { scopeSnapshotJson = value; }
    public BigDecimal getBudgetSnapshot() { return budgetSnapshot; }
    public void setBudgetSnapshot(BigDecimal value) { budgetSnapshot = value; }
    public Integer getQuotaSnapshot() { return quotaSnapshot; }
    public void setQuotaSnapshot(Integer value) { quotaSnapshot = value; }
    public BigDecimal getEstimatedUnitCost() { return estimatedUnitCost; }
    public void setEstimatedUnitCost(BigDecimal value) { estimatedUnitCost = value; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String value) { triggerType = value; }
    public String getTriggerReference() { return triggerReference; }
    public void setTriggerReference(String value) { triggerReference = value; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String value) { triggerSource = value; }
    public String getRunActor() { return runActor; }
    public void setRunActor(String value) { runActor = value; }
    public String getAuthorizedBy() { return authorizedBy; }
    public void setAuthorizedBy(String value) { authorizedBy = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public AutomationRunStatus getStatus() { return status; }
    public void setStatus(AutomationRunStatus value) { status = value; }
    public Integer getAudienceCount() { return audienceCount; }
    public void setAudienceCount(Integer value) { audienceCount = value; }
    public Integer getIssuedCount() { return issuedCount; }
    public void setIssuedCount(Integer value) { issuedCount = value; }
    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer value) { skippedCount = value; }
    public Integer getFailedCount() { return failedCount; }
    public void setFailedCount(Integer value) { failedCount = value; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant value) { startedAt = value; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
}
