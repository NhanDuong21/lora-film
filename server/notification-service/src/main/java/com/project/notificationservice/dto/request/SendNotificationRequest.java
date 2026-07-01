package com.project.notificationservice.dto.request;

import com.project.notificationservice.enums.NotificationChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class SendNotificationRequest {

    @Size(max = 150, message = "eventId must not exceed 150 characters")
    private String eventId;

    @Size(max = 150, message = "idempotencyKey must not exceed 150 characters")
    private String idempotencyKey;

    @NotBlank(message = "requestSource is required")
    @Size(max = 100, message = "requestSource must not exceed 100 characters")
    private String requestSource;

    @Size(max = 100, message = "templateCode must not exceed 100 characters")
    private String templateCode;

    @NotNull(message = "userId is required")
    @Min(value = 1, message = "userId must be greater than 0")
    private Long userId;

    @Size(max = 150, message = "recipient must not exceed 150 characters")
    private String recipient;

    @NotNull(message = "channelType is required")
    private NotificationChannel channelType;

    private Map<String, Object> variables;

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private String content;

    @Valid
    private ReferenceDto reference;

    public SendNotificationRequest() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestSource() {
        return requestSource;
    }

    public void setRequestSource(String requestSource) {
        this.requestSource = requestSource;
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

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public NotificationChannel getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannel channelType) {
        this.channelType = channelType;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
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

    public ReferenceDto getReference() {
        return reference;
    }

    public void setReference(ReferenceDto reference) {
        this.reference = reference;
    }
}
