package com.project.scoreservice.dto;

public class RecalculateTierResponse {
    private Long userId;
    private Integer accumulatedPoints;
    private String previousTier;
    private String currentTier;
    private Boolean tierChanged;

    public RecalculateTierResponse() {
    }

    public RecalculateTierResponse(Long userId, Integer accumulatedPoints, String previousTier, String currentTier, Boolean tierChanged) {
        this.userId = userId;
        this.accumulatedPoints = accumulatedPoints;
        this.previousTier = previousTier;
        this.currentTier = currentTier;
        this.tierChanged = tierChanged;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getAccumulatedPoints() {
        return accumulatedPoints;
    }

    public void setAccumulatedPoints(Integer accumulatedPoints) {
        this.accumulatedPoints = accumulatedPoints;
    }

    public String getPreviousTier() {
        return previousTier;
    }

    public void setPreviousTier(String previousTier) {
        this.previousTier = previousTier;
    }

    public String getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(String currentTier) {
        this.currentTier = currentTier;
    }

    public Boolean getTierChanged() {
        return tierChanged;
    }

    public void setTierChanged(Boolean tierChanged) {
        this.tierChanged = tierChanged;
    }
}
