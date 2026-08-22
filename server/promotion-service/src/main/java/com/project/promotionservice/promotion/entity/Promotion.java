package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "promotions")
public class Promotion extends BaseAuditableEntity {

    @Column(name = "campaign_public_id", length = 36)
    private String campaignPublicId;

    @Column(name = "cloned_from_public_id", length = 36)
    private String clonedFromPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", length = 30, nullable = false)
    private PromotionType promotionType;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PromotionStatus status = PromotionStatus.DRAFT;

    @Column(name = "is_public", nullable = false)
    private Boolean publicVisible = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_mode", length = 30, nullable = false)
    private PromotionDistributionMode distributionMode = PromotionDistributionMode.ASSIGNED_WALLET;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "stackable", nullable = false)
    private Boolean stackable = false;

    @Column(name = "conditions_json", nullable = false, columnDefinition = "json")
    private String conditionsJson;

    @Column(name = "actions_json", nullable = false, columnDefinition = "json")
    private String actionsJson;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "redemption_count", nullable = false)
    private Integer redemptionCount = 0;

    @Column(name = "max_redemptions_per_user", nullable = false)
    private Integer maxRedemptionsPerUser = 1;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
    }

    public String getClonedFromPublicId() {
        return clonedFromPublicId;
    }

    public void setClonedFromPublicId(String clonedFromPublicId) {
        this.clonedFromPublicId = clonedFromPublicId;
    }

    public PromotionType getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public void setStatus(PromotionStatus status) {
        this.status = status;
    }

    public Boolean getPublicVisible() {
        return publicVisible;
    }

    public void setPublicVisible(Boolean publicVisible) {
        this.publicVisible = publicVisible;
    }

    public PromotionDistributionMode getDistributionMode() {
        return distributionMode;
    }

    public void setDistributionMode(PromotionDistributionMode distributionMode) {
        this.distributionMode = distributionMode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getStackable() {
        return stackable;
    }

    public void setStackable(Boolean stackable) {
        this.stackable = stackable;
    }

    public String getConditionsJson() {
        return conditionsJson;
    }

    public void setConditionsJson(String conditionsJson) {
        this.conditionsJson = conditionsJson;
    }

    public String getActionsJson() {
        return actionsJson;
    }

    public void setActionsJson(String actionsJson) {
        this.actionsJson = actionsJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Integer getMaxRedemptions() {
        return maxRedemptions;
    }

    public void setMaxRedemptions(Integer maxRedemptions) {
        this.maxRedemptions = maxRedemptions;
    }

    public Integer getRedemptionCount() {
        return redemptionCount;
    }

    public void setRedemptionCount(Integer redemptionCount) {
        this.redemptionCount = redemptionCount;
    }

    public Integer getMaxRedemptionsPerUser() {
        return maxRedemptionsPerUser;
    }

    public void setMaxRedemptionsPerUser(Integer maxRedemptionsPerUser) {
        this.maxRedemptionsPerUser = maxRedemptionsPerUser;
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
}
