package com.project.scoreservice.dto;

import com.project.scoreservice.enumtype.UserScoreStatus;

public class UserScoreResponse {
    private Long userId;
    private Integer currentPoints;
    private Integer heldPoints;
    private Integer accumulatedPoints;
    private MembershipTierResponse currentTier;
    private NextTierResponse nextTier;
    private UserScoreStatus status;
    private Integer outstandingPoints;

    public UserScoreResponse() {
    }

    public UserScoreResponse(Long userId, Integer currentPoints, Integer heldPoints, Integer accumulatedPoints, MembershipTierResponse currentTier, NextTierResponse nextTier, UserScoreStatus status, Integer outstandingPoints) {
        this.userId = userId;
        this.currentPoints = currentPoints != null ? currentPoints : 0;
        this.heldPoints = heldPoints != null ? heldPoints : 0;
        this.accumulatedPoints = accumulatedPoints != null ? accumulatedPoints : 0;
        this.currentTier = currentTier;
        this.nextTier = nextTier;
        this.status = status != null ? status : UserScoreStatus.ACTIVE;
        this.outstandingPoints = outstandingPoints != null ? outstandingPoints : 0;
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

    public Integer getHeldPoints() {
        return heldPoints;
    }

    public void setHeldPoints(Integer heldPoints) {
        this.heldPoints = heldPoints;
    }

    public Integer getAccumulatedPoints() {
        return accumulatedPoints;
    }

    public void setAccumulatedPoints(Integer accumulatedPoints) {
        this.accumulatedPoints = accumulatedPoints;
    }

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

    public UserScoreStatus getStatus() {
        return status;
    }

    public void setStatus(UserScoreStatus status) {
        this.status = status;
    }

    public Integer getOutstandingPoints() {
        return outstandingPoints;
    }

    public void setOutstandingPoints(Integer outstandingPoints) {
        this.outstandingPoints = outstandingPoints;
    }
}
