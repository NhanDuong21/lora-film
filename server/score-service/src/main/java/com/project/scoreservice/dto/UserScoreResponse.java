package com.project.scoreservice.dto;
 
import java.time.LocalDateTime;
 
public class UserScoreResponse {
    private Long userId;
    private Integer currentPoints;
    private Integer accumulatedPoints;
    private MembershipTierResponse currentTier;
    private NextTierResponse nextTier;
    private LocalDateTime updatedAt;
 
    public UserScoreResponse() {
    }
 
    public UserScoreResponse(Long userId, Integer currentPoints, Integer accumulatedPoints, MembershipTierResponse currentTier, NextTierResponse nextTier, LocalDateTime updatedAt) {
        this.userId = userId;
        this.currentPoints = currentPoints;
        this.accumulatedPoints = accumulatedPoints;
        this.currentTier = currentTier;
        this.nextTier = nextTier;
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
 
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
 
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
