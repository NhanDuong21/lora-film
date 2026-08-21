package com.project.promotionservice.promotion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Request body to update an existing campaign")
public class CampaignUpdateRequest {

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 255, message = "name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotNull(message = "priority is required")
    @Min(value = 0, message = "priority must be >= 0")
    private Integer priority;

    @NotNull(message = "stackable is required")
    private Boolean stackable;

    @NotNull(message = "exclusiveCampaign is required")
    private Boolean exclusiveCampaign;

    @NotNull(message = "autoActivate is required")
    private Boolean autoActivate;

    @NotNull(message = "autoComplete is required")
    private Boolean autoComplete;

    @NotNull(message = "autoPauseWhenBudgetExceeded is required")
    private Boolean autoPauseWhenBudgetExceeded;

    @NotBlank(message = "timezone is required")
    @Size(max = 60, message = "timezone must be <= 60 characters")
    private String timezone;

    @NotNull(message = "startAt is required")
    private Instant startAt;

    @NotNull(message = "endAt is required")
    private Instant endAt;

    @NotNull(message = "budgetAmount is required")
    @DecimalMin(value = "0.01", message = "budgetAmount must be greater than 0")
    private BigDecimal budgetAmount;

    @Min(value = 1, message = "maxRedemptions must be >= 1")
    private Integer maxRedemptions;

    @NotNull(message = "maxRedemptionsPerUser is required")
    @Min(value = 1, message = "maxRedemptionsPerUser must be >= 1")
    private Integer maxRedemptionsPerUser;

    @Size(max = 150, message = "legalNotificationRef must be <= 150 characters")
    private String legalNotificationRef;

    private String remarks;

    private Boolean testData;

    @Size(max = 30, message = "environmentTag must be <= 30 characters")
    private String environmentTag;

    public CampaignUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public Integer getMaxRedemptions() {
        return maxRedemptions;
    }

    public void setMaxRedemptions(Integer maxRedemptions) {
        this.maxRedemptions = maxRedemptions;
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

    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
}
