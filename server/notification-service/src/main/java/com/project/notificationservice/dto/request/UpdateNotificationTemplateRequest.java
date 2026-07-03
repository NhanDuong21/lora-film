package com.project.notificationservice.dto.request;

import com.project.notificationservice.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateNotificationTemplateRequest {

    private String templateCode;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private org.springframework.web.multipart.MultipartFile htmlFile;

    private NotificationChannel channelType;

    private Boolean isActive;

    @NotNull(message = "Version is required")
    private Integer version;

    public UpdateNotificationTemplateRequest() {
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

    public org.springframework.web.multipart.MultipartFile getHtmlFile() {
        return htmlFile;
    }

    public void setHtmlFile(org.springframework.web.multipart.MultipartFile htmlFile) {
        this.htmlFile = htmlFile;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
