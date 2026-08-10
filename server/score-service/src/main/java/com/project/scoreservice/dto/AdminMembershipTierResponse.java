package com.project.scoreservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminMembershipTierResponse {
    private Integer tierId;
    private String tierCode;
    private String tierName;
    private Integer minAccumulatedPoints;
    private BigDecimal earningRate;
    private Integer priority;
    private Boolean isActive;
    private String description;
    private Long userCount;
    private Boolean recalculationRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminMembershipTierResponse() {
    }

    public AdminMembershipTierResponse(Integer tierId, String tierCode, String tierName, Integer minAccumulatedPoints, BigDecimal earningRate, Integer priority, Boolean isActive, String description, Long userCount, Boolean recalculationRequired, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.tierId = tierId;
        this.tierCode = tierCode;
        this.tierName = tierName;
        this.minAccumulatedPoints = minAccumulatedPoints;
        this.earningRate = earningRate;
        this.priority = priority;
        this.isActive = isActive;
        this.description = description;
        this.userCount = userCount;
        this.recalculationRequired = recalculationRequired;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Boolean getRecalculationRequired() {
        return recalculationRequired;
    }

    public void setRecalculationRequired(Boolean recalculationRequired) {
        this.recalculationRequired = recalculationRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
