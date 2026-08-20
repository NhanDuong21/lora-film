package com.project.promotionservice.automation.entity;

import com.project.promotionservice.automation.enums.PlaybookStatus;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_playbooks")
public class PromotionPlaybook extends BaseAuditableEntity {
    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;
    @Column(name = "name", nullable = false, length = 180)
    private String name;
    @Column(name = "description", length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PlaybookStatus status = PlaybookStatus.DRAFT;
    @Column(name = "playbook_version", nullable = false)
    private Integer playbookVersion = 1;
    @Column(name = "trigger_type", nullable = false, length = 60)
    private String triggerType;
    @Column(name = "campaign_public_id", length = 36)
    private String campaignPublicId;
    @Column(name = "promotion_public_id", length = 36)
    private String promotionPublicId;
    @Column(name = "config_json", nullable = false, columnDefinition = "json")
    private String configJson = "{}";
    @Column(name = "scope_json", nullable = false, columnDefinition = "json")
    private String scopeJson = "{}";
    @Column(name = "budget_limit", precision = 18, scale = 2)
    private BigDecimal budgetLimit;
    @Column(name = "quota_limit")
    private Integer quotaLimit;
    @Column(name = "config_hash", length = 64)
    private String configHash;
    @Column(name = "submitted_playbook_version")
    private Integer submittedPlaybookVersion;
    @Column(name = "submitted_config_hash", length = 64)
    private String submittedConfigHash;
    @Column(name = "approved_playbook_version")
    private Integer approvedPlaybookVersion;
    @Column(name = "approved_config_hash", length = 64)
    private String approvedConfigHash;
    @Column(name = "budget_period_key", length = 7)
    private String budgetPeriodKey;
    @Column(name = "budget_committed", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetCommitted = BigDecimal.ZERO;
    @Column(name = "quota_committed", nullable = false)
    private Integer quotaCommitted = 0;
    @Column(name = "submitted_by", length = 36)
    private String submittedBy;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "approved_by", length = 36)
    private String approvedBy;
    @Column(name = "approved_at")
    private Instant approvedAt;

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public PlaybookStatus getStatus() { return status; }
    public void setStatus(PlaybookStatus value) { status = value; }
    public Integer getPlaybookVersion() { return playbookVersion; }
    public void setPlaybookVersion(Integer value) { playbookVersion = value; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String value) { triggerType = value; }
    public String getCampaignPublicId() { return campaignPublicId; }
    public void setCampaignPublicId(String value) { campaignPublicId = value; }
    public String getPromotionPublicId() { return promotionPublicId; }
    public void setPromotionPublicId(String value) { promotionPublicId = value; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String value) { configJson = value; }
    public String getScopeJson() { return scopeJson; }
    public void setScopeJson(String value) { scopeJson = value; }
    public BigDecimal getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(BigDecimal value) { budgetLimit = value; }
    public Integer getQuotaLimit() { return quotaLimit; }
    public void setQuotaLimit(Integer value) { quotaLimit = value; }
    public String getConfigHash() { return configHash; }
    public void setConfigHash(String value) { configHash = value; }
    public Integer getSubmittedPlaybookVersion() { return submittedPlaybookVersion; }
    public void setSubmittedPlaybookVersion(Integer value) { submittedPlaybookVersion = value; }
    public String getSubmittedConfigHash() { return submittedConfigHash; }
    public void setSubmittedConfigHash(String value) { submittedConfigHash = value; }
    public Integer getApprovedPlaybookVersion() { return approvedPlaybookVersion; }
    public void setApprovedPlaybookVersion(Integer value) { approvedPlaybookVersion = value; }
    public String getApprovedConfigHash() { return approvedConfigHash; }
    public void setApprovedConfigHash(String value) { approvedConfigHash = value; }
    public String getBudgetPeriodKey() { return budgetPeriodKey; }
    public void setBudgetPeriodKey(String value) { budgetPeriodKey = value; }
    public BigDecimal getBudgetCommitted() { return budgetCommitted; }
    public void setBudgetCommitted(BigDecimal value) { budgetCommitted = value; }
    public Integer getQuotaCommitted() { return quotaCommitted; }
    public void setQuotaCommitted(Integer value) { quotaCommitted = value; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String value) { submittedBy = value; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant value) { submittedAt = value; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String value) { approvedBy = value; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant value) { approvedAt = value; }
}
