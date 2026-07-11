package com.project.scoreservice.dto;

public class ScoreRedeemResponse {
    private Long userId;
    private Long bookingId;
    private Integer redeemedPoints;
    private Integer redeemValue;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private Long historyId;
    private Boolean idempotent;

    public ScoreRedeemResponse() {
    }

    public ScoreRedeemResponse(Long userId, Long bookingId, Integer redeemedPoints, Integer redeemValue, Integer currentPoints, Integer accumulatedPoints, Long historyId, Boolean idempotent) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.redeemedPoints = redeemedPoints;
        this.redeemValue = redeemValue;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
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

    public Integer getRedeemedPoints() {
        return redeemedPoints;
    }

    public void setRedeemedPoints(Integer redeemedPoints) {
        this.redeemedPoints = redeemedPoints;
    }

    public Integer getRedeemValue() {
        return redeemValue;
    }

    public void setRedeemValue(Integer redeemValue) {
        this.redeemValue = redeemValue;
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
