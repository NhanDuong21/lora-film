package com.project.scoreservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminMembershipTierResponse {
    private Integer tierId;
    private String tierName;
    private Integer minPoints;
    private BigDecimal earningRate;
    private String description;
    private Long userCount;
    private Boolean recalculationRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminMembershipTierResponse() {
    }

    public AdminMembershipTierResponse(Integer tierId, String tierName, Integer minPoints, BigDecimal earningRate, String description, Long userCount, Boolean recalculationRequired, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.tierId = tierId;
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.earningRate = earningRate;
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
