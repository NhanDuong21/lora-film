package com.project.notificationservice.provider.model;

import com.project.notificationservice.enums.NotificationChannel;

public class ProviderSendRequest {
    private String notificationId;
    private Long userId;
    private NotificationChannel channelType;
    private String recipient;
    private String title;
    private String content;
    private String templateCode;

    public ProviderSendRequest() {
    }

    public ProviderSendRequest(String notificationId, Long userId, NotificationChannel channelType,
                               String recipient, String title, String content, String templateCode) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.channelType = channelType;
        this.recipient = recipient;
        this.title = title;
        this.content = content;
        this.templateCode = templateCode;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public NotificationChannel getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannel channelType) {
        this.channelType = channelType;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String notificationId;
        private Long userId;
        private NotificationChannel channelType;
        private String recipient;
        private String title;
        private String content;
        private String templateCode;

        public Builder notificationId(String notificationId) {
            this.notificationId = notificationId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder channelType(NotificationChannel channelType) {
            this.channelType = channelType;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder templateCode(String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        public ProviderSendRequest build() {
            return new ProviderSendRequest(notificationId, userId, channelType, recipient, title, content, templateCode);
        }
    }
}
