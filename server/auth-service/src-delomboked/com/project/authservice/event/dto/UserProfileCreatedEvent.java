package com.project.authservice.event.dto;

import java.time.Instant;

public class UserProfileCreatedEvent {
    private String eventId;
    private String eventType = "USER_PROFILE_CREATED";
    private String eventVersion = "1.0";
    private String source = "user-service";
    private String occurredAt;
    private UserProfileCreatedEventData data;

    public UserProfileCreatedEvent() {
    }

    public UserProfileCreatedEvent(String eventId, String occurredAt, UserProfileCreatedEventData data) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.data = data;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    public UserProfileCreatedEventData getData() { return data; }
    public void setData(UserProfileCreatedEventData data) { this.data = data; }
}
