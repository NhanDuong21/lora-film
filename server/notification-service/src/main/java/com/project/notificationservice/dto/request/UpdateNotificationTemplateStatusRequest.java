package com.project.notificationservice.dto.request;

import jakarta.validation.constraints.NotNull;

public class UpdateNotificationTemplateStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    @NotNull(message = "Version is required")
    private Integer version;

    public UpdateNotificationTemplateStatusRequest() {
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
