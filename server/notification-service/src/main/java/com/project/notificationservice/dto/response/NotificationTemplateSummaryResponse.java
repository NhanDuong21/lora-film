package com.project.notificationservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.notificationservice.enums.NotificationChannel;
import java.time.LocalDateTime;

public class NotificationTemplateSummaryResponse {

    private Integer templateId;
    private String templateCode;
    private String title;
    private NotificationChannel channelType;
    private Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public NotificationTemplateSummaryResponse() {
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NotificationChannel getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannel channelType) {
        this.channelType = channelType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}
