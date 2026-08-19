package com.project.promotionservice.promotion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Schema(description = "Request body to create a new promotion campaign")
public class CampaignCreateRequest {

    @NotBlank(message = "code is required")
    @Size(min = 2, max = 100, message = "code must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "code must contain only alphanumeric characters, underscores, or hyphens")
    private String code;

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 255, message = "name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotNull(message = "priority is required")
    @Min(value = 0, message = "priority must be >= 0")
    private Integer priority = 100;

    @NotNull(message = "stackable is required")
    private Boolean stackable = false;

    @NotNull(message = "exclusiveCampaign is required")
    private Boolean exclusiveCampaign = false;

    @NotNull(message = "autoActivate is required")
    private Boolean autoActivate = true;

    @NotNull(message = "autoComplete is required")
    private Boolean autoComplete = true;

    @NotNull(message = "autoPauseWhenBudgetExceeded is required")
    private Boolean autoPauseWhenBudgetExceeded = true;

    @NotBlank(message = "timezone is required")
    @Size(max = 60, message = "timezone must be <= 60 characters")
    private String timezone = "Asia/Ho_Chi_Minh";

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
    private Integer maxRedemptionsPerUser = 1;

    @Size(max = 150, message = "legalNotificationRef must be <= 150 characters")
    private String legalNotificationRef;

    private String remarks;

    private CampaignScopeType scopeType = CampaignScopeType.GLOBAL;

    private Set<@Size(max = 36) String> cinemaPublicIds = new LinkedHashSet<>();

    public CampaignCreateRequest() {
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

    public CampaignScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(CampaignScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public Set<String> getCinemaPublicIds() {
        return cinemaPublicIds;
    }

    public void setCinemaPublicIds(Set<String> cinemaPublicIds) {
        this.cinemaPublicIds = cinemaPublicIds == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(cinemaPublicIds);
    }
}
