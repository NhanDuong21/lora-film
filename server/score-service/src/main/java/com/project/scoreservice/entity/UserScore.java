package com.project.scoreservice.entity;

import com.project.scoreservice.enumtype.UserScoreStatus;
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

    @Column(name = "held_points", nullable = false)
    private Integer heldPoints = 0;

    @Column(name = "accumulated_points", nullable = false)
    private Integer accumulatedPoints = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_tier_id", nullable = false)
    private MembershipTier currentTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserScoreStatus status = UserScoreStatus.ACTIVE;

    @Column(name = "outstanding_points", nullable = false)
    private Integer outstandingPoints = 0;

    @Column(name = "last_earn_at")
    private LocalDateTime lastEarnAt;

    @Column(name = "last_redeem_at")
    private LocalDateTime lastRedeemAt;

    @Column(name = "last_expire_at")
    private LocalDateTime lastExpireAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserScore() {
    }

    public UserScore(Long userId, Integer currentPoints, Integer heldPoints, Integer accumulatedPoints, MembershipTier currentTier, UserScoreStatus status, Integer outstandingPoints, LocalDateTime lastEarnAt, LocalDateTime lastRedeemAt, LocalDateTime lastExpireAt, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.currentPoints = currentPoints != null ? currentPoints : 0;
        this.heldPoints = heldPoints != null ? heldPoints : 0;
        this.accumulatedPoints = accumulatedPoints != null ? accumulatedPoints : 0;
        this.currentTier = currentTier;
        this.status = status != null ? status : UserScoreStatus.ACTIVE;
        this.outstandingPoints = outstandingPoints != null ? outstandingPoints : 0;
        this.lastEarnAt = lastEarnAt;
        this.lastRedeemAt = lastRedeemAt;
        this.lastExpireAt = lastExpireAt;
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt;
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

    public MembershipTier getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(MembershipTier currentTier) {
        this.currentTier = currentTier;
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

    public LocalDateTime getLastEarnAt() {
        return lastEarnAt;
    }

    public void setLastEarnAt(LocalDateTime lastEarnAt) {
        this.lastEarnAt = lastEarnAt;
    }

    public LocalDateTime getLastRedeemAt() {
        return lastRedeemAt;
    }

    public void setLastRedeemAt(LocalDateTime lastRedeemAt) {
        this.lastRedeemAt = lastRedeemAt;
    }

    public LocalDateTime getLastExpireAt() {
        return lastExpireAt;
    }

    public void setLastExpireAt(LocalDateTime lastExpireAt) {
        this.lastExpireAt = lastExpireAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public static UserScoreBuilder builder() {
        return new UserScoreBuilder();
    }

    public static class UserScoreBuilder {
        private Long userId;
        private Integer currentPoints = 0;
        private Integer heldPoints = 0;
        private Integer accumulatedPoints = 0;
        private MembershipTier currentTier;
        private UserScoreStatus status = UserScoreStatus.ACTIVE;
        private Integer outstandingPoints = 0;
        private LocalDateTime lastEarnAt;
        private LocalDateTime lastRedeemAt;
        private LocalDateTime lastExpireAt;
        private Long version = 0L;
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

        public UserScoreBuilder heldPoints(Integer heldPoints) {
            this.heldPoints = heldPoints;
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

        public UserScoreBuilder status(UserScoreStatus status) {
            this.status = status;
            return this;
        }

        public UserScoreBuilder outstandingPoints(Integer outstandingPoints) {
            this.outstandingPoints = outstandingPoints;
            return this;
        }

        public UserScoreBuilder lastEarnAt(LocalDateTime lastEarnAt) {
            this.lastEarnAt = lastEarnAt;
            return this;
        }

        public UserScoreBuilder lastRedeemAt(LocalDateTime lastRedeemAt) {
            this.lastRedeemAt = lastRedeemAt;
            return this;
        }

        public UserScoreBuilder lastExpireAt(LocalDateTime lastExpireAt) {
            this.lastExpireAt = lastExpireAt;
            return this;
        }

        public UserScoreBuilder version(Long version) {
            this.version = version;
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
            return new UserScore(userId, currentPoints, heldPoints, accumulatedPoints, currentTier, status, outstandingPoints, lastEarnAt, lastRedeemAt, lastExpireAt, version, createdAt, updatedAt);
        }
    }
}
