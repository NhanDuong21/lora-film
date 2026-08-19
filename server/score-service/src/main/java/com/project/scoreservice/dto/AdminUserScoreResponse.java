package com.project.scoreservice.dto;

import com.project.scoreservice.enumtype.UserScoreStatus;

import java.time.LocalDateTime;

public class AdminUserScoreResponse {
    private Long userId;
    private Integer currentPoints;
    private Integer heldPoints;
    private Integer availablePoints;
    private Integer accumulatedPoints;
    private Integer outstandingPoints;
    private UserScoreStatus status;
    private MembershipTierResponse currentTier;
    private NextTierResponse nextTier;
    private LocalDateTime lastEarnAt;
    private LocalDateTime lastRedeemAt;
    private LocalDateTime lastExpireAt;
    private LocalDateTime updatedAt;

    public AdminUserScoreResponse() {
    }

    public AdminUserScoreResponse(Long userId, Integer currentPoints, Integer heldPoints,
                                  Integer accumulatedPoints, Integer outstandingPoints,
                                  UserScoreStatus status, MembershipTierResponse currentTier,
                                  NextTierResponse nextTier, LocalDateTime lastEarnAt,
                                  LocalDateTime lastRedeemAt, LocalDateTime lastExpireAt,
                                  LocalDateTime updatedAt) {
        this.userId = userId;
        this.currentPoints = currentPoints;
        this.heldPoints = heldPoints != null ? heldPoints : 0;
        this.availablePoints = Math.max(0, (currentPoints != null ? currentPoints : 0) - this.heldPoints);
        this.accumulatedPoints = accumulatedPoints;
        this.outstandingPoints = outstandingPoints != null ? outstandingPoints : 0;
        this.status = status;
        this.currentTier = currentTier;
        this.nextTier = nextTier;
        this.lastEarnAt = lastEarnAt;
        this.lastRedeemAt = lastRedeemAt;
        this.lastExpireAt = lastExpireAt;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(Integer currentPoints) {
        this.currentPoints = currentPoints;
    }

    public Integer getHeldPoints() { return heldPoints; }
    public void setHeldPoints(Integer heldPoints) { this.heldPoints = heldPoints; }

    public Integer getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Integer availablePoints) { this.availablePoints = availablePoints; }

    public Integer getAccumulatedPoints() {
        return accumulatedPoints;
    }

    public void setAccumulatedPoints(Integer accumulatedPoints) {
        this.accumulatedPoints = accumulatedPoints;
    }

    public Integer getOutstandingPoints() { return outstandingPoints; }
    public void setOutstandingPoints(Integer outstandingPoints) { this.outstandingPoints = outstandingPoints; }

    public UserScoreStatus getStatus() { return status; }
    public void setStatus(UserScoreStatus status) { this.status = status; }

    public MembershipTierResponse getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(MembershipTierResponse currentTier) {
        this.currentTier = currentTier;
    }

    public NextTierResponse getNextTier() {
        return nextTier;
    }

    public void setNextTier(NextTierResponse nextTier) {
        this.nextTier = nextTier;
    }

    public LocalDateTime getLastEarnAt() { return lastEarnAt; }
    public void setLastEarnAt(LocalDateTime lastEarnAt) { this.lastEarnAt = lastEarnAt; }

    public LocalDateTime getLastRedeemAt() { return lastRedeemAt; }
    public void setLastRedeemAt(LocalDateTime lastRedeemAt) { this.lastRedeemAt = lastRedeemAt; }

    public LocalDateTime getLastExpireAt() { return lastExpireAt; }
    public void setLastExpireAt(LocalDateTime lastExpireAt) { this.lastExpireAt = lastExpireAt; }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
