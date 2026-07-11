package com.project.scoreservice.dto;

public class ScoreRefundResponse {
    private Long userId;
    private Long bookingId;
    private Integer refundedPoints;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private Long originalHistoryId;
    private Long historyId;
    private Boolean idempotent;

    public ScoreRefundResponse() {
    }

    public ScoreRefundResponse(Long userId, Long bookingId, Integer refundedPoints, Integer currentPoints, Integer accumulatedPoints, Long originalHistoryId, Long historyId, Boolean idempotent) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.refundedPoints = refundedPoints;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
        this.originalHistoryId = originalHistoryId;
        this.historyId = historyId;
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

    public Integer getRefundedPoints() {
        return refundedPoints;
    }

    public void setRefundedPoints(Integer refundedPoints) {
        this.refundedPoints = refundedPoints;
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

    public Long getOriginalHistoryId() {
        return originalHistoryId;
    }

    public void setOriginalHistoryId(Long originalHistoryId) {
        this.originalHistoryId = originalHistoryId;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Boolean getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Boolean idempotent) {
        this.idempotent = idempotent;
    }
}
