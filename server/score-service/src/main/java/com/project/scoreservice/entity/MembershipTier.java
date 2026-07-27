package com.project.scoreservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_tiers")
public class MembershipTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tier_code", nullable = false, unique = true, length = 30)
    private String tierCode;

    @Column(name = "tier_name", nullable = false, length = 100)
    private String tierName;

    @Column(name = "min_accumulated_points", nullable = false)
    private Integer minAccumulatedPoints;

    @Column(name = "earning_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal earningRate;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MembershipTier() {
    }

    public MembershipTier(Integer id, String tierCode, String tierName, Integer minAccumulatedPoints, BigDecimal earningRate, Integer priority, Boolean isActive, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tierCode = tierCode;
        this.tierName = tierName;
        this.minAccumulatedPoints = minAccumulatedPoints;
        this.earningRate = earningRate;
        this.priority = priority;
        this.isActive = isActive != null ? isActive : true;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTierCode() {
        return tierCode;
    }

    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Integer getMinAccumulatedPoints() {
        return minAccumulatedPoints;
    }

    public void setMinAccumulatedPoints(Integer minAccumulatedPoints) {
        this.minAccumulatedPoints = minAccumulatedPoints;
    }

    public BigDecimal getEarningRate() {
        return earningRate;
    }

    public void setEarningRate(BigDecimal earningRate) {
        this.earningRate = earningRate;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static MembershipTierBuilder builder() {
        return new MembershipTierBuilder();
    }

    public static class MembershipTierBuilder {
        private Integer id;
        private String tierCode;
        private String tierName;
        private Integer minAccumulatedPoints;
        private BigDecimal earningRate;
        private Integer priority;
        private Boolean isActive = true;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public MembershipTierBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public MembershipTierBuilder tierCode(String tierCode) {
            this.tierCode = tierCode;
            return this;
        }

        public MembershipTierBuilder tierName(String tierName) {
            this.tierName = tierName;
            return this;
        }

        public MembershipTierBuilder minAccumulatedPoints(Integer minAccumulatedPoints) {
            this.minAccumulatedPoints = minAccumulatedPoints;
            return this;
        }

        public MembershipTierBuilder earningRate(BigDecimal earningRate) {
            this.earningRate = earningRate;
            return this;
        }

        public MembershipTierBuilder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        public MembershipTierBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public MembershipTierBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MembershipTierBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MembershipTierBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public MembershipTier build() {
            return new MembershipTier(id, tierCode, tierName, minAccumulatedPoints, earningRate, priority, isActive, description, createdAt, updatedAt);
        }
    }
}
