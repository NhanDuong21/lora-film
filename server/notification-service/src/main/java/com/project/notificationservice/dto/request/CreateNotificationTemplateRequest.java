package com.project.notificationservice.dto.request;

import com.project.notificationservice.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateNotificationTemplateRequest {

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must not exceed 100 characters")
    @Pattern(regexp = "^\\s*[A-Za-z0-9_]+\\s*$", message = "Template code must only contain alphanumeric characters and underscores")
    private String templateCode;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotNull(message = "HTML file is required")
    private org.springframework.web.multipart.MultipartFile htmlFile;

    @NotNull(message = "Channel type is required")
    private NotificationChannel channelType;

    private Boolean isActive = true;

    public CreateNotificationTemplateRequest() {
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
}
