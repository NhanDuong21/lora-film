package com.project.scoreservice.dto;

public class AdminAdjustmentResponse {
    private Long userId;
    private String adjustmentType;
    private Integer pointChange;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private String currentTier;
    private String previousTier;
    private Boolean tierChanged;
    private Long historyId;
    private Boolean idempotent;

    public AdminAdjustmentResponse() {
    }

    public AdminAdjustmentResponse(Long userId, String adjustmentType, Integer pointChange, Integer currentPoints, Integer accumulatedPoints, String currentTier, String previousTier, Boolean tierChanged, Long historyId, Boolean idempotent) {
        this.userId = userId;
        this.adjustmentType = adjustmentType;
        this.pointChange = pointChange;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
        this.currentTier = currentTier;
        this.previousTier = previousTier;
        this.tierChanged = tierChanged != null ? tierChanged : false;
        this.historyId = historyId;
        this.idempotent = idempotent != null ? idempotent : false;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public Integer getPointChange() { return pointChange; }
    public void setPointChange(Integer pointChange) { this.pointChange = pointChange; }

    public Integer getCurrentPoints() { return currentPoints; }
    public void setCurrentPoints(Integer currentPoints) { this.currentPoints = currentPoints; }

    public Integer getAccumulatedPoints() { return accumulatedPoints; }
    public void setAccumulatedPoints(Integer accumulatedPoints) { this.accumulatedPoints = accumulatedPoints; }

    public String getCurrentTier() { return currentTier; }
    public void setCurrentTier(String currentTier) { this.currentTier = currentTier; }

    public String getPreviousTier() { return previousTier; }
    public void setPreviousTier(String previousTier) { this.previousTier = previousTier; }

    public Boolean getTierChanged() { return tierChanged; }
    public void setTierChanged(Boolean tierChanged) { this.tierChanged = tierChanged; }

    public Long getHistoryId() { return historyId; }
    public void setHistoryId(Long historyId) { this.historyId = historyId; }

    public Boolean getIdempotent() { return idempotent; }
    public void setIdempotent(Boolean idempotent) { this.idempotent = idempotent; }
}
