package com.project.scoreservice.dto;
 
import java.math.BigDecimal;
 
public class UserTierResponse {
    private Integer tierId;
    private String tierName;
    private Integer minPoints;
    private BigDecimal earningRate;
    private Integer accumulatedPoints;
    private NextTierResponse nextTier;
 
    public UserTierResponse() {
    }
 
    public UserTierResponse(Integer tierId, String tierName, Integer minPoints, BigDecimal earningRate, Integer accumulatedPoints, NextTierResponse nextTier) {
        this.tierId = tierId;
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.earningRate = earningRate;
        this.accumulatedPoints = accumulatedPoints;
        this.nextTier = nextTier;
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
 
    public Integer getAccumulatedPoints() {
        return accumulatedPoints;
    }
 
    public void setAccumulatedPoints(Integer accumulatedPoints) {
        this.accumulatedPoints = accumulatedPoints;
    }
 
    public NextTierResponse getNextTier() {
        return nextTier;
    }
 
    public void setNextTier(NextTierResponse nextTier) {
        this.nextTier = nextTier;
    }
}
