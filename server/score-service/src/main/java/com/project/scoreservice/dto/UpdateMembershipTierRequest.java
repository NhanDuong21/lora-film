package com.project.scoreservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class UpdateMembershipTierRequest {
    @Size(max = 30, message = "Tier code cannot exceed 30 characters")
    private String tierCode;

    @Size(max = 100, message = "Tier name cannot exceed 100 characters")
    private String tierName;

    @Min(value = 0, message = "Minimum points cannot be negative")
    private Integer minAccumulatedPoints;

    @DecimalMin(value = "0.0", inclusive = false, message = "Earning rate must be greater than zero")
    @DecimalMax(value = "1.0", inclusive = true, message = "Earning rate must be less than or equal to one")
    private BigDecimal earningRate;

    private Integer priority;

    private Boolean isActive;

    private String description;

    public UpdateMembershipTierRequest() {
    }

    public String getTierCode() {
        return tierCode;
    }

    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Integer getMinAccumulatedPoints() {
        return minAccumulatedPoints;
    }

    public void setMinAccumulatedPoints(Integer minAccumulatedPoints) {
        this.minAccumulatedPoints = minAccumulatedPoints;
    }

    public Integer getMinPoints() {
        return minAccumulatedPoints;
    }

    public void setMinPoints(Integer minPoints) {
        this.minAccumulatedPoints = minPoints;
    }

    public BigDecimal getEarningRate() {
        return earningRate;
    }

    public void setEarningRate(BigDecimal earningRate) {
        this.earningRate = earningRate;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
