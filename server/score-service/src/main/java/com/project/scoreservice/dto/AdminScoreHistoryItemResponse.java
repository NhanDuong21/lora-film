package com.project.scoreservice.dto;

import java.time.LocalDateTime;

public class AdminScoreHistoryItemResponse {
    private Long historyId;
    private String eventId;
    private Long bookingId;
    private Integer pointChange;
    private Integer requestedPointChange;
    private Integer outstandingPoints;
    private String reconciliationStatus;
    private String transactionType;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private Integer accumulatedBefore;
    private Integer accumulatedAfter;
    private Long referenceHistoryId;
    private Long createdBy;
    private String requestId;
    private String reason;
    private String description;
    private LocalDateTime createdAt;

    public AdminScoreHistoryItemResponse() {
    }

    public AdminScoreHistoryItemResponse(Long historyId, String eventId, Long bookingId, Integer pointChange, Integer requestedPointChange, Integer outstandingPoints, String reconciliationStatus, String transactionType, Integer balanceBefore, Integer balanceAfter, Integer accumulatedBefore, Integer accumulatedAfter, Long referenceHistoryId, Long createdBy, String requestId, String reason, String description, LocalDateTime createdAt) {
        this.historyId = historyId;
        this.eventId = eventId;
        this.bookingId = bookingId;
        this.pointChange = pointChange;
        this.requestedPointChange = requestedPointChange;
        this.outstandingPoints = outstandingPoints;
        this.reconciliationStatus = reconciliationStatus;
        this.transactionType = transactionType;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.accumulatedBefore = accumulatedBefore;
        this.accumulatedAfter = accumulatedAfter;
        this.referenceHistoryId = referenceHistoryId;
        this.createdBy = createdBy;
        this.requestId = requestId;
        this.reason = reason;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getPointChange() {
        return pointChange;
    }

    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
    }

    public Integer getRequestedPointChange() {
        return requestedPointChange;
    }

    public void setRequestedPointChange(Integer requestedPointChange) {
        this.requestedPointChange = requestedPointChange;
    }

    public Integer getOutstandingPoints() {
        return outstandingPoints;
    }

    public void setOutstandingPoints(Integer outstandingPoints) {
        this.outstandingPoints = outstandingPoints;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
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

    public Long getReferenceHistoryId() {
        return referenceHistoryId;
    }

    public void setReferenceHistoryId(Long referenceHistoryId) {
        this.referenceHistoryId = referenceHistoryId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
