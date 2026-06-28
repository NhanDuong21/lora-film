package com.project.scoreservice.dto;
 
import java.math.BigDecimal;
 
public class InternalUserScoreResponse {
    private Long userId;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private String tierName;
    private BigDecimal earningRate;
 
    public InternalUserScoreResponse() {
    }
 
    public InternalUserScoreResponse(Long userId, Integer currentPoints, Integer accumulatedPoints, String tierName, BigDecimal earningRate) {
        this.userId = userId;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
        this.tierName = tierName;
        this.earningRate = earningRate;
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
 
    public Integer getAccumulatedPoints() {
        return accumulatedPoints;
    }
 
    public void setAccumulatedPoints(Integer accumulatedPoints) {
        this.accumulatedPoints = accumulatedPoints;
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
}
