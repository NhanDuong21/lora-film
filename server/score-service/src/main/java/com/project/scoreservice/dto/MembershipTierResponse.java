package com.project.scoreservice.dto;

import java.math.BigDecimal;

public class MembershipTierResponse {
    private Integer tierId;
    private String tierCode;
    private String tierName;
    private Integer minAccumulatedPoints;
    private BigDecimal earningRate;
    private Integer priority;

    public MembershipTierResponse() {
    }

    public MembershipTierResponse(Integer tierId, String tierCode, String tierName, Integer minAccumulatedPoints, BigDecimal earningRate, Integer priority) {
        this.tierId = tierId;
        this.tierCode = tierCode;
        this.tierName = tierName;
        this.minAccumulatedPoints = minAccumulatedPoints;
        this.earningRate = earningRate;
        this.priority = priority;
    }

    public Integer getTierId() {
        return tierId;
    }

    public void setTierId(Integer tierId) {
        this.tierId = tierId;
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

    // Alias for backward compatibility
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
}
