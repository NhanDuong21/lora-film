package com.project.scoreservice.dto;

public class NextTierResponse {
    private Integer tierId;
    private String tierCode;
    private String tierName;
    private Integer minAccumulatedPoints;
    private Integer pointsRequired;

    public NextTierResponse() {
    }

    public NextTierResponse(Integer tierId, String tierCode, String tierName, Integer minAccumulatedPoints, Integer pointsRequired) {
        this.tierId = tierId;
        this.tierCode = tierCode;
        this.tierName = tierName;
        this.minAccumulatedPoints = minAccumulatedPoints;
        this.pointsRequired = pointsRequired;
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

    public Integer getPointsRequired() {
        return pointsRequired;
    }

    public void setPointsRequired(Integer pointsRequired) {
        this.pointsRequired = pointsRequired;
    }
}
