package com.project.scoreservice.dto;

public class ScoreAdjustmentResponse {
    private Long userId;
    private ScoreAdjustmentType adjustmentType;
    private Integer pointChange;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private String previousTier;
    private String currentTier;
    private Boolean tierChanged;
    private Long historyId;
    private String requestId;
    private Boolean idempotent;

    public ScoreAdjustmentResponse() {
    }

    public ScoreAdjustmentResponse(Long userId, ScoreAdjustmentType adjustmentType, Integer pointChange, Integer currentPoints, Integer accumulatedPoints, String previousTier, String currentTier, Boolean tierChanged, Long historyId, String requestId, Boolean idempotent) {
        this.userId = userId;
        this.adjustmentType = adjustmentType;
        this.pointChange = pointChange;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
        this.previousTier = previousTier;
        this.currentTier = currentTier;
        this.tierChanged = tierChanged;
        this.historyId = historyId;
        this.requestId = requestId;
        this.idempotent = idempotent;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ScoreAdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(ScoreAdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Integer getPointChange() {
        return pointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
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

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Boolean getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Boolean idempotent) {
        this.idempotent = idempotent;
    }
}
