package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_promotions")
public class UserPromotion extends BaseAuditableEntity {

    @Column(name = "user_public_id", length = 36, nullable = false)
    private String userPublicId;

    @Column(name = "promotion_public_id", length = 36, nullable = false)
    private String promotionPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private UserPromotionStatus status = UserPromotionStatus.AVAILABLE;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "max_usage", nullable = false)
    private Integer maxUsage = 1;

    public String getUserPublicId() {
        return userPublicId;
    }

    public void setUserPublicId(String userPublicId) {
        this.userPublicId = userPublicId;
    }

    public String getPromotionPublicId() {
        return promotionPublicId;
    }

    public void setPromotionPublicId(String promotionPublicId) {
        this.promotionPublicId = promotionPublicId;
    }

    public UserPromotionStatus getStatus() {
        return status;
    }

    public void setStatus(UserPromotionStatus status) {
        this.status = status;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Integer getMaxUsage() {
        return maxUsage;
    }

    public void setMaxUsage(Integer maxUsage) {
        this.maxUsage = maxUsage;
    }
}
