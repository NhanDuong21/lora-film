package com.project.scoreservice.dto;
 
public class NextTierResponse {
    private Integer tierId;
    private String tierName;
    private Integer minPoints;
    private Integer pointsRequired;
 
    public NextTierResponse() {
    }
 
    public NextTierResponse(Integer tierId, String tierName, Integer minPoints, Integer pointsRequired) {
        this.tierId = tierId;
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.pointsRequired = pointsRequired;
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
 
    public Integer getPointsRequired() {
        return pointsRequired;
    }
 
    public void setPointsRequired(Integer pointsRequired) {
        this.pointsRequired = pointsRequired;
    }
}
