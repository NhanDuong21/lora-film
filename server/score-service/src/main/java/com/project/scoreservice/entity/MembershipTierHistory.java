package com.project.scoreservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "membership_tier_histories")
public class MembershipTierHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserScore userScore;

    @Column(name = "old_tier_code", length = 30)
    private String oldTierCode;

    @Column(name = "new_tier_code", nullable = false, length = 30)
    private String newTierCode;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MembershipTierHistory() {}

    public MembershipTierHistory(Long id, UserScore userScore, String oldTierCode, String newTierCode, String reason, LocalDateTime createdAt) {
        this.id = id;
        this.userScore = userScore;
        this.oldTierCode = oldTierCode;
        this.newTierCode = newTierCode;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static MembershipTierHistoryBuilder builder() {
        return new MembershipTierHistoryBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserScore getUserScore() { return userScore; }
    public void setUserScore(UserScore userScore) { this.userScore = userScore; }

    public String getOldTierCode() { return oldTierCode; }
    public void setOldTierCode(String oldTierCode) { this.oldTierCode = oldTierCode; }

    public String getNewTierCode() { return newTierCode; }
    public void setNewTierCode(String newTierCode) { this.newTierCode = newTierCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class MembershipTierHistoryBuilder {
        private Long id;
        private UserScore userScore;
        private String oldTierCode;
        private String newTierCode;
        private String reason;
        private LocalDateTime createdAt;

        public MembershipTierHistoryBuilder id(Long id) { this.id = id; return this; }
        public MembershipTierHistoryBuilder userScore(UserScore userScore) { this.userScore = userScore; return this; }
        public MembershipTierHistoryBuilder oldTierCode(String oldTierCode) { this.oldTierCode = oldTierCode; return this; }
        public MembershipTierHistoryBuilder newTierCode(String newTierCode) { this.newTierCode = newTierCode; return this; }
        public MembershipTierHistoryBuilder reason(String reason) { this.reason = reason; return this; }
        public MembershipTierHistoryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public MembershipTierHistory build() {
            return new MembershipTierHistory(id, userScore, oldTierCode, newTierCode, reason, createdAt);
        }
    }
}
