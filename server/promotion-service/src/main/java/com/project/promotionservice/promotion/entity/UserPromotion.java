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

    @Column(name = "automation_run_public_id", length = 36)
    private String automationRunPublicId;

    @Column(name = "audience_member_public_id", length = 36)
    private String audienceMemberPublicId;

    @Column(name = "issuance_key", length = 180, unique = true)
    private String issuanceKey;
    @Column(name = "test_data", nullable = false)
    private Boolean testData = false;
    @Column(name = "environment_tag", nullable = false, length = 30)
    private String environmentTag = "BUSINESS";
    @Column(name = "revocation_pending", nullable = false)
    private Boolean revocationPending = false;
    @Column(name = "revocation_reason", length = 100)
    private String revocationReason;

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

    public String getAutomationRunPublicId() { return automationRunPublicId; }
    public void setAutomationRunPublicId(String value) { automationRunPublicId = value; }
    public String getAudienceMemberPublicId() { return audienceMemberPublicId; }
    public void setAudienceMemberPublicId(String value) { audienceMemberPublicId = value; }
    public String getIssuanceKey() { return issuanceKey; }
    public void setIssuanceKey(String value) { issuanceKey = value; }
    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
    public Boolean getRevocationPending() { return revocationPending; }
    public void setRevocationPending(Boolean value) { revocationPending = value; }
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String value) { revocationReason = value; }
}
