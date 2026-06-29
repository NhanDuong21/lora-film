package com.project.scoreservice.dto;

public class ScoreEarnResponse {
    private Integer pointChange;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private Integer accumulatedBefore;
    private Integer accumulatedAfter;
    private String previousTier;
    private String currentTier;
    private Boolean tierChanged;
    private Boolean idempotent;

    public ScoreEarnResponse() {
    }

    public ScoreEarnResponse(Integer pointChange, Integer balanceBefore, Integer balanceAfter, 
                            Integer accumulatedBefore, Integer accumulatedAfter, 
                            String previousTier, String currentTier, Boolean tierChanged, Boolean idempotent) {
        this.pointChange = pointChange;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.accumulatedBefore = accumulatedBefore;
        this.accumulatedAfter = accumulatedAfter;
        this.previousTier = previousTier;
        this.currentTier = currentTier;
        this.tierChanged = tierChanged;
        this.idempotent = idempotent;
    }

    public Integer getPointChange() {
        return pointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
    }

    public Integer getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(Integer balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Integer getAccumulatedBefore() {
        return accumulatedBefore;
    }

    public void setAccumulatedBefore(Integer accumulatedBefore) {
        this.accumulatedBefore = accumulatedBefore;
    }

    public Integer getAccumulatedAfter() {
        return accumulatedAfter;
    }

    public void setAccumulatedAfter(Integer accumulatedAfter) {
        this.accumulatedAfter = accumulatedAfter;
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

    public Boolean getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Boolean idempotent) {
        this.idempotent = idempotent;
    }
}
