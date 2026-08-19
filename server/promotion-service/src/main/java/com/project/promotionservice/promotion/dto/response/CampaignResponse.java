package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignAvailabilityStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class CampaignResponse {

    private String publicId;
    private String code;
    private String name;
    private String slug;
    private String description;
    private CampaignStatus status;
    private CampaignApprovalStatus approvalStatus;
    private LegalStatus legalStatus;
    private Integer priority;
    private Boolean stackable;
    private Boolean exclusiveCampaign;
    private Boolean autoActivate;
    private Boolean autoComplete;
    private Boolean autoPauseWhenBudgetExceeded;
    private Boolean killSwitch;
    private String timezone;
    private Instant startAt;
    private Instant endAt;
    private Instant publishedAt;
    private Instant approvedAt;
    private String approvedBy;
    private BigDecimal budgetAmount;
    private BigDecimal budgetUsed;
    private BigDecimal budgetReserved;
    private BigDecimal budgetRemaining;
    private Integer maxRedemptions;
    private Integer redemptionCount;
    private Integer maxRedemptionsPerUser;
    private String legalNotificationRef;
    private String remarks;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private CampaignStatus businessStatus;
    private CampaignAvailabilityStatus availabilityStatus;
    private List<String> allowedActions = List.of();
    private List<String> blockedReasons = List.of();
    private List<String> pendingTasks = List.of();

    public CampaignResponse() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public CampaignStatus getBusinessStatus() { return businessStatus; }
    public void setBusinessStatus(CampaignStatus value) { this.businessStatus = value; }
    public CampaignAvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(CampaignAvailabilityStatus value) { this.availabilityStatus = value; }
    public List<String> getAllowedActions() { return allowedActions; }
    public void setAllowedActions(List<String> value) { this.allowedActions = value == null ? List.of() : value; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public void setBlockedReasons(List<String> value) { this.blockedReasons = value == null ? List.of() : value; }
    public List<String> getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(List<String> value) { this.pendingTasks = value == null ? List.of() : value; }
}
