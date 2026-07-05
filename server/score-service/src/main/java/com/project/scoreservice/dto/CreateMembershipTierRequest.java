package com.project.scoreservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateMembershipTierRequest {
    @NotBlank(message = "Tier name is required")
    @Size(max = 50, message = "Tier name cannot exceed 50 characters")
    private String tierName;

    @NotNull(message = "Minimum points is required")
    @Min(value = 0, message = "Minimum points cannot be negative")
    private Integer minPoints;

    @NotNull(message = "Earning rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Earning rate must be greater than zero")
    @DecimalMax(value = "1.0", inclusive = true, message = "Earning rate must be less than or equal to one")
    private BigDecimal earningRate;

    private String description;

    public CreateMembershipTierRequest() {
    }

    public CreateMembershipTierRequest(String tierName, Integer minPoints, BigDecimal earningRate, String description) {
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.earningRate = earningRate;
        this.description = description;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Integer getMinPoints() {
        return minPoints;
    }

    public void setMinPoints(Integer minPoints) {
        this.minPoints = minPoints;
    }

    public BigDecimal getEarningRate() {
        return earningRate;
    }

    public void setEarningRate(BigDecimal earningRate) {
        this.earningRate = earningRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
