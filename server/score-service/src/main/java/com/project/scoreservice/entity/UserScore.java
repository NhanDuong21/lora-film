package com.project.scoreservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_scores")
public class UserScore {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "current_points", nullable = false)
    private Integer currentPoints = 0;

    @Column(name = "accumulated_points", nullable = false)
    private Integer accumulatedPoints = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_tier_id", nullable = false)
    private MembershipTier currentTier;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserScore() {
    }

    public UserScore(Long userId, Integer currentPoints, Integer accumulatedPoints, MembershipTier currentTier, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.currentPoints = currentPoints != null ? currentPoints : 0;
        this.accumulatedPoints = accumulatedPoints != null ? accumulatedPoints : 0;
        this.currentTier = currentTier;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
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

    public MembershipTier getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(MembershipTier currentTier) {
        this.currentTier = currentTier;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static UserScoreBuilder builder() {
        return new UserScoreBuilder();
    }

    public static class UserScoreBuilder {
        private Long userId;
        private Integer currentPoints = 0;
        private Integer accumulatedPoints = 0;
        private MembershipTier currentTier;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserScoreBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserScoreBuilder currentPoints(Integer currentPoints) {
            this.currentPoints = currentPoints;
            return this;
        }

        public UserScoreBuilder accumulatedPoints(Integer accumulatedPoints) {
            this.accumulatedPoints = accumulatedPoints;
            return this;
        }

        public UserScoreBuilder currentTier(MembershipTier currentTier) {
            this.currentTier = currentTier;
            return this;
        }

        public UserScoreBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserScoreBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserScore build() {
            return new UserScore(userId, currentPoints, accumulatedPoints, currentTier, createdAt, updatedAt);
        }
    }
}
