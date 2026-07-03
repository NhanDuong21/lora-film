package com.project.scoreservice.dto;

public class ScoreRevokeResponse {
    private Long userId;
    private Long bookingId;
    private Integer requestedPoints;
    private Integer deductedPoints;
    private Integer outstandingPoints;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private String previousTier;
    private String currentTier;
    private Boolean tierChanged;
    private Long historyId;
    private String reconciliationStatus;
    private Boolean requiresManualReconciliation;
    private Boolean idempotent;

    public ScoreRevokeResponse() {
    }

    public ScoreRevokeResponse(Long userId, Long bookingId, Integer requestedPoints, Integer deductedPoints, Integer outstandingPoints, Integer currentPoints, Integer accumulatedPoints, String previousTier, String currentTier, Boolean tierChanged, Long historyId, String reconciliationStatus, Boolean requiresManualReconciliation, Boolean idempotent) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.requestedPoints = requestedPoints;
        this.deductedPoints = deductedPoints;
        this.outstandingPoints = outstandingPoints;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
        this.previousTier = previousTier;
        this.currentTier = currentTier;
        this.tierChanged = tierChanged;
        this.historyId = historyId;
        this.reconciliationStatus = reconciliationStatus;
        this.requiresManualReconciliation = requiresManualReconciliation;
        this.idempotent = idempotent;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getRequestedPoints() {
        return requestedPoints;
    }

    public void setRequestedPoints(Integer requestedPoints) {
        this.requestedPoints = requestedPoints;
    }

    public Integer getDeductedPoints() {
        return deductedPoints;
    }

    public void setDeductedPoints(Integer deductedPoints) {
        this.deductedPoints = deductedPoints;
    }

    public Integer getOutstandingPoints() {
        return outstandingPoints;
    }

    public void setOutstandingPoints(Integer outstandingPoints) {
        this.outstandingPoints = outstandingPoints;
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

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public Boolean getRequiresManualReconciliation() {
        return requiresManualReconciliation;
    }

    public void setRequiresManualReconciliation(Boolean requiresManualReconciliation) {
        this.requiresManualReconciliation = requiresManualReconciliation;
    }

    public Boolean getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Boolean idempotent) {
        this.idempotent = idempotent;
    }
}
