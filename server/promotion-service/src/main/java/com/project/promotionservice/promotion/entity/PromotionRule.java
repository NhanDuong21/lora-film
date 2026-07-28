package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.promotion.enums.RuleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "promotion_rules")
public class PromotionRule extends BaseAuditableEntity {

    @Column(name = "campaign_public_id", length = 36, nullable = false)
    private String campaignPublicId;

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = 50, nullable = false)
    private RuleType ruleType;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder = 1;

    @Column(name = "stackable", nullable = false)
    private Boolean stackable = false;

    @Column(name = "stop_further_rules", nullable = false)
    private Boolean stopFurtherRules = false;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "conditions_json", columnDefinition = "JSON", nullable = false)
    private String conditionsJson;

    @Column(name = "actions_json", columnDefinition = "JSON", nullable = false)
    private String actionsJson;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    public PromotionRule() {
    }

    public PromotionRule(Long id, String publicId, Integer version,
                         Instant createdAt, String createdBy, Instant updatedAt,
                         String updatedBy, Instant deletedAt, String deletedBy,
                         String campaignPublicId, String code, String name, String description,
                         RuleType ruleType, Integer priority, Integer executionOrder,
                         Boolean stackable, Boolean stopFurtherRules, Boolean enabled,
                         String conditionsJson, String actionsJson, String metadataJson,
                         Instant effectiveFrom, Instant effectiveTo) {
        super(id, publicId, version, createdAt, createdBy, updatedAt, updatedBy, deletedAt, deletedBy);
        this.campaignPublicId = campaignPublicId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.priority = priority;
        this.executionOrder = executionOrder;
        this.stackable = stackable;
        this.stopFurtherRules = stopFurtherRules;
        this.enabled = enabled;
        this.conditionsJson = conditionsJson;
        this.actionsJson = actionsJson;
        this.metadataJson = metadataJson;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
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

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(Integer executionOrder) {
        this.executionOrder = executionOrder;
    }

    public Boolean getStackable() {
        return stackable;
    }

    public void setStackable(Boolean stackable) {
        this.stackable = stackable;
    }

    public Boolean getStopFurtherRules() {
        return stopFurtherRules;
    }

    public void setStopFurtherRules(Boolean stopFurtherRules) {
        this.stopFurtherRules = stopFurtherRules;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}
