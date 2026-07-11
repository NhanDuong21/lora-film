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

    @Column(name = "tier_name", nullable = false, unique = true, length = 50)
    private String tierName;

    @Column(name = "min_points", nullable = false)
    private Integer minPoints;

    @Column(name = "earning_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal earningRate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public MembershipTier() {
    }

    public MembershipTier(Integer id, String tierName, Integer minPoints, BigDecimal earningRate, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.earningRate = earningRate;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Integer getMinPoints() {
        return minPoints;
    }

    public void setMinPoints(Integer minPoints) {
        this.minPoints = minPoints;
    }

    public BigDecimal getEarningRate() {
        return earningRate;
    }

    public void setEarningRate(BigDecimal earningRate) {
        this.earningRate = earningRate;
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

    // Builder pattern
    public static MembershipTierBuilder builder() {
        return new MembershipTierBuilder();
    }

    public static class MembershipTierBuilder {
        private Integer id;
        private String tierName;
        private Integer minPoints;
        private BigDecimal earningRate;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public MembershipTierBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public MembershipTierBuilder tierName(String tierName) {
            this.tierName = tierName;
            return this;
        }

        public MembershipTierBuilder minPoints(Integer minPoints) {
            this.minPoints = minPoints;
            return this;
        }

        public MembershipTierBuilder earningRate(BigDecimal earningRate) {
            this.earningRate = earningRate;
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
            return new MembershipTier(id, tierName, minPoints, earningRate, description, createdAt, updatedAt);
        }
    }
}
