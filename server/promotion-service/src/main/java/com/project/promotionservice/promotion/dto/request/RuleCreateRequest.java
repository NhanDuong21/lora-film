package com.project.promotionservice.promotion.dto.request;

import com.project.promotionservice.common.constant.ValidationConstants;
import com.project.promotionservice.promotion.enums.RuleType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request body to create a new promotion rule")
public class RuleCreateRequest {

    @NotBlank(message = "campaignPublicId is required")
    @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "campaignPublicId must be a valid UUID")
    private String campaignPublicId;

    @NotBlank(message = "code is required")
    @Size(min = 2, max = 100, message = "code must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "code must contain only alphanumeric characters, underscores, or hyphens")
    private String code;

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 255, message = "name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotNull(message = "ruleType is required")
    private RuleType ruleType;

    @Min(value = 0, message = "priority must be >= 0")
    private Integer priority = 100;

    @Min(value = 1, message = "executionOrder must be >= 1")
    private Integer executionOrder = 1;

    private Boolean stackable = false;

    private Boolean stopFurtherRules = false;

    private Boolean enabled = true;

    @NotBlank(message = "conditionsJson is required")
    private String conditionsJson;

    @NotBlank(message = "actionsJson is required")
    private String actionsJson;

    private String metadataJson;

    @NotNull(message = "effectiveFrom is required")
    private Instant effectiveFrom;

    private Instant effectiveTo;

    public RuleCreateRequest() {
    }

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
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

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public Boolean getStackable() {
        return stackable;
    }

    public void setStackable(Boolean stackable) {
        this.stackable = stackable;
    }

    public Boolean getStopFurtherRules() {
        return stopFurtherRules;
    }

    public void setStopFurtherRules(Boolean stopFurtherRules) {
        this.stopFurtherRules = stopFurtherRules;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getConditionsJson() {
        return conditionsJson;
    }

    public void setConditionsJson(String conditionsJson) {
        this.conditionsJson = conditionsJson;
    }

    public String getActionsJson() {
        return actionsJson;
    }

    public void setActionsJson(String actionsJson) {
        this.actionsJson = actionsJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}
