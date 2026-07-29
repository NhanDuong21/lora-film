package com.project.promotionservice.configuration.domain;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "promotion_configurations")
public class PromotionConfiguration extends BaseAuditableEntity {

    @Column(name = "config_key", length = 150, nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT", nullable = false)
    private String configValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 30, nullable = false)
    private ConfigurationValueType valueType;

    @Column(name = "category", length = 100, nullable = false)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "editable", nullable = false)
    private Boolean editable = true;

    @Column(name = "requires_restart", nullable = false)
    private Boolean requiresRestart = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ConfigurationStatus status = ConfigurationStatus.ACTIVE;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public ConfigurationValueType getValueType() { return valueType; }
    public void setValueType(ConfigurationValueType valueType) { this.valueType = valueType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public Boolean getRequiresRestart() { return requiresRestart; }
    public void setRequiresRestart(Boolean requiresRestart) { this.requiresRestart = requiresRestart; }
    public ConfigurationStatus getStatus() { return status; }
    public void setStatus(ConfigurationStatus status) { this.status = status; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
