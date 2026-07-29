package com.project.promotionservice.promotion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request body to update an existing promotion rule")
public class RuleUpdateRequest {

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 255, message = "name must be between 2 and 255 characters")
    private String name;

    private String description;

    @NotNull(message = "priority is required")
    @Min(value = 0, message = "priority must be >= 0")
    private Integer priority;

    @NotNull(message = "executionOrder is required")
    @Min(value = 1, message = "executionOrder must be >= 1")
    private Integer executionOrder;

    @NotNull(message = "stackable is required")
    private Boolean stackable;

    @NotNull(message = "stopFurtherRules is required")
    private Boolean stopFurtherRules;

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    @NotBlank(message = "conditionsJson is required")
    private String conditionsJson;

    @NotBlank(message = "actionsJson is required")
    private String actionsJson;

    private String metadataJson;

    @NotNull(message = "effectiveFrom is required")
    private Instant effectiveFrom;

    private Instant effectiveTo;

    public RuleUpdateRequest() {
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
