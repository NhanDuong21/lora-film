package com.project.notificationservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.notificationservice.enums.NotificationChannel;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendNotificationResponse {

    private Long notificationId;
    private String eventId;
    private String templateCode;
    private Long userId;
    private NotificationChannel channelType;
    private String recipient;
    private String status;
    private LocalDateTime sentAt;
    private Boolean idempotent;

    public SendNotificationResponse() {
    }

    public SendNotificationResponse(Long notificationId, String eventId, String templateCode, Long userId,
                                    NotificationChannel channelType, String recipient, String status,
                                    LocalDateTime sentAt, Boolean idempotent) {
        this.notificationId = notificationId;
        this.eventId = eventId;
        this.templateCode = templateCode;
        this.userId = userId;
        this.channelType = channelType;
        this.recipient = recipient;
        this.status = status;
        this.sentAt = sentAt;
        this.idempotent = idempotent;
    }

    public static SendNotificationResponse idempotent(Long notificationId, String status) {
        SendNotificationResponse response = new SendNotificationResponse();
        response.setNotificationId(notificationId);
        response.setStatus(status);
        response.setIdempotent(true);
        return response;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getIdempotent() {
        return idempotent;
    }

    public void setIdempotent(Boolean idempotent) {
        this.idempotent = idempotent;
    }
}
