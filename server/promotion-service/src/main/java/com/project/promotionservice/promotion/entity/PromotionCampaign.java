package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.enums.LegalStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_campaigns")
public class PromotionCampaign extends BaseAuditableEntity {

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "slug", length = 255, nullable = false)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30, nullable = false)
    private CampaignApprovalStatus approvalStatus = CampaignApprovalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", length = 30, nullable = false)
    private LegalStatus legalStatus = LegalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 30, nullable = false)
    private CampaignScopeType scopeType = CampaignScopeType.GLOBAL;

    @Column(name = "cinema_scope_json", columnDefinition = "json")
    private String cinemaScopeJson;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "stackable", nullable = false)
    private Boolean stackable = false;

    @Column(name = "exclusive_campaign", nullable = false)
    private Boolean exclusiveCampaign = false;

    @Column(name = "auto_activate", nullable = false)
    private Boolean autoActivate = true;

    @Column(name = "auto_complete", nullable = false)
    private Boolean autoComplete = true;

    @Column(name = "auto_pause_when_budget_exceeded", nullable = false)
    private Boolean autoPauseWhenBudgetExceeded = true;

    @Column(name = "kill_switch", nullable = false)
    private Boolean killSwitch = false;

    @Column(name = "timezone", length = 60, nullable = false)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "approval_threshold_applied", precision = 18, scale = 2)
    private BigDecimal approvalThresholdApplied;

    @Column(name = "approval_policy_version", length = 50)
    private String approvalPolicyVersion;

    @Column(name = "required_approval_capability", length = 100)
    private String requiredApprovalCapability;

    @Column(name = "budget_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(name = "budget_used", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetUsed = BigDecimal.ZERO;

    @Column(name = "budget_reserved", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetReserved = BigDecimal.ZERO;

    @Column(name = "budget_remaining", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetRemaining = BigDecimal.ZERO;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count", nullable = false)
    private Integer redemptionCount = 0;

    @Column(name = "max_redemptions_per_user", nullable = false)
    private Integer maxRedemptionsPerUser = 1;

    @Column(name = "legal_notification_ref", length = 150)
    private String legalNotificationRef;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    public PromotionCampaign() {
    }

    public PromotionCampaign(Long id, String publicId, Integer version,
                             Instant createdAt, String createdBy, Instant updatedAt,
                             String updatedBy, Instant deletedAt, String deletedBy,
                             String code, String name, String slug, String description,
                             CampaignStatus status, CampaignApprovalStatus approvalStatus,
                             LegalStatus legalStatus, Integer priority, Boolean stackable,
                             Boolean exclusiveCampaign, Boolean autoActivate, Boolean autoComplete,
                             Boolean autoPauseWhenBudgetExceeded, Boolean killSwitch, String timezone,
                             Instant startAt, Instant endAt, Instant publishedAt, Instant approvedAt,
                             String approvedBy, BigDecimal budgetAmount, BigDecimal budgetUsed,
                             BigDecimal budgetReserved, BigDecimal budgetRemaining, Integer maxRedemptions,
                             Integer redemptionCount, Integer maxRedemptionsPerUser,
                             String legalNotificationRef, String remarks) {
        super(id, publicId, version, createdAt, createdBy, updatedAt, updatedBy, deletedAt, deletedBy);
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.status = status;
        this.approvalStatus = approvalStatus;
        this.legalStatus = legalStatus;
        this.priority = priority;
        this.stackable = stackable;
        this.exclusiveCampaign = exclusiveCampaign;
        this.autoActivate = autoActivate;
        this.autoComplete = autoComplete;
        this.autoPauseWhenBudgetExceeded = autoPauseWhenBudgetExceeded;
        this.killSwitch = killSwitch;
        this.timezone = timezone;
        this.startAt = startAt;
        this.endAt = endAt;
        this.publishedAt = publishedAt;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
        this.budgetAmount = budgetAmount;
        this.budgetUsed = budgetUsed;
        this.budgetReserved = budgetReserved;
        this.budgetRemaining = budgetRemaining;
        this.maxRedemptions = maxRedemptions;
        this.redemptionCount = redemptionCount;
        this.maxRedemptionsPerUser = maxRedemptionsPerUser;
        this.legalNotificationRef = legalNotificationRef;
        this.remarks = remarks;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public CampaignApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(CampaignApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public LegalStatus getLegalStatus() {
        return legalStatus;
    }

    public CampaignScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(CampaignScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public String getCinemaScopeJson() {
        return cinemaScopeJson;
    }

    public void setCinemaScopeJson(String cinemaScopeJson) {
        this.cinemaScopeJson = cinemaScopeJson;
    }

    public void setLegalStatus(LegalStatus legalStatus) {
        this.legalStatus = legalStatus;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getStackable() {
        return stackable;
    }

    public void setStackable(Boolean stackable) {
        this.stackable = stackable;
    }

    public Boolean getExclusiveCampaign() {
        return exclusiveCampaign;
    }

    public void setExclusiveCampaign(Boolean exclusiveCampaign) {
        this.exclusiveCampaign = exclusiveCampaign;
    }

    public Boolean getAutoActivate() {
        return autoActivate;
    }

    public void setAutoActivate(Boolean autoActivate) {
        this.autoActivate = autoActivate;
    }

    public Boolean getAutoComplete() {
        return autoComplete;
    }

    public void setAutoComplete(Boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public Boolean getAutoPauseWhenBudgetExceeded() {
        return autoPauseWhenBudgetExceeded;
    }

    public void setAutoPauseWhenBudgetExceeded(Boolean autoPauseWhenBudgetExceeded) {
        this.autoPauseWhenBudgetExceeded = autoPauseWhenBudgetExceeded;
    }

    public Boolean getKillSwitch() {
        return killSwitch;
    }

    public void setKillSwitch(Boolean killSwitch) {
        this.killSwitch = killSwitch;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public BigDecimal getApprovalThresholdApplied() { return approvalThresholdApplied; }
    public void setApprovalThresholdApplied(BigDecimal value) { this.approvalThresholdApplied = value; }
    public String getApprovalPolicyVersion() { return approvalPolicyVersion; }
    public void setApprovalPolicyVersion(String value) { this.approvalPolicyVersion = value; }
    public String getRequiredApprovalCapability() { return requiredApprovalCapability; }
    public void setRequiredApprovalCapability(String value) { this.requiredApprovalCapability = value; }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getBudgetUsed() {
        return budgetUsed;
    }

    public void setBudgetUsed(BigDecimal budgetUsed) {
        this.budgetUsed = budgetUsed;
    }

    public BigDecimal getBudgetReserved() {
        return budgetReserved;
    }

    public void setBudgetReserved(BigDecimal budgetReserved) {
        this.budgetReserved = budgetReserved;
    }

    public BigDecimal getBudgetRemaining() {
        return budgetRemaining;
    }

    public void setBudgetRemaining(BigDecimal budgetRemaining) {
        this.budgetRemaining = budgetRemaining;
    }

    public Integer getMaxRedemptions() {
        return maxRedemptions;
    }

    public void setMaxRedemptions(Integer maxRedemptions) {
        this.maxRedemptions = maxRedemptions;
    }

    public Integer getRedemptionCount() {
        return redemptionCount;
    }

    public void setRedemptionCount(Integer redemptionCount) {
        this.redemptionCount = redemptionCount;
    }

    public Integer getMaxRedemptionsPerUser() {
        return maxRedemptionsPerUser;
    }

    public void setMaxRedemptionsPerUser(Integer maxRedemptionsPerUser) {
        this.maxRedemptionsPerUser = maxRedemptionsPerUser;
    }

    public String getLegalNotificationRef() {
        return legalNotificationRef;
    }

    public void setLegalNotificationRef(String legalNotificationRef) {
        this.legalNotificationRef = legalNotificationRef;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
