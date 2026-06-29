package com.project.scoreservice.dto;
 
import java.math.BigDecimal;
 
public class MembershipTierResponse {
    private Integer tierId;
    private String tierName;
    private Integer minPoints;
    private BigDecimal earningRate;
 
    public MembershipTierResponse() {
    }
 
    public MembershipTierResponse(Integer tierId, String tierName, Integer minPoints, BigDecimal earningRate) {
        this.tierId = tierId;
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.earningRate = earningRate;
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
}
