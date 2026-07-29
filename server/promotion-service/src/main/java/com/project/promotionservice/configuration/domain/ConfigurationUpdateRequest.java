package com.project.promotionservice.configuration.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ConfigurationUpdateRequest {

    @NotBlank
    @Size(max = 10000)
    private String configValue;

    @NotNull
    private ConfigurationValueType valueType;

    @NotNull
    @Size(max = 100)
    private String category;

    @Size(max = 500)
    private String description;

    @NotNull
    private Boolean editable;

    @NotNull
    private Boolean requiresRestart;

    private ConfigurationStatus status;

    @Size(max = 10000)
    private String metadataJson;

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
