package com.project.scoreservice.dto;

import com.project.scoreservice.enumtype.UserScoreStatus;
import java.math.BigDecimal;

public class InternalUserScoreResponse {
    private Long userId;
    private Integer currentPoints;
    private Integer heldPoints;
    private Integer accumulatedPoints;
    private String tierCode;
    private String tierName;
    private BigDecimal earningRate;
    private UserScoreStatus status;

    public InternalUserScoreResponse() {
    }

    public InternalUserScoreResponse(Long userId, Integer currentPoints, Integer heldPoints, Integer accumulatedPoints, String tierCode, String tierName, BigDecimal earningRate, UserScoreStatus status) {
        this.userId = userId;
        this.currentPoints = currentPoints != null ? currentPoints : 0;
        this.heldPoints = heldPoints != null ? heldPoints : 0;
        this.accumulatedPoints = accumulatedPoints != null ? accumulatedPoints : 0;
        this.tierCode = tierCode;
        this.tierName = tierName;
        this.earningRate = earningRate;
        this.status = status != null ? status : UserScoreStatus.ACTIVE;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(Integer currentPoints) {
        this.currentPoints = currentPoints;
    }

    public Integer getHeldPoints() {
        return heldPoints;
    }

    public void setHeldPoints(Integer heldPoints) {
        this.heldPoints = heldPoints;
    }

    public Integer getAccumulatedPoints() {
        return accumulatedPoints;
    }

    public void setAccumulatedPoints(Integer accumulatedPoints) {
        this.accumulatedPoints = accumulatedPoints;
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

    public BigDecimal getEarningRate() {
        return earningRate;
    }

    public void setEarningRate(BigDecimal earningRate) {
        this.earningRate = earningRate;
    }

    public UserScoreStatus getStatus() {
        return status;
    }

    public void setStatus(UserScoreStatus status) {
        this.status = status;
    }
}
