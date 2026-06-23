package com.project.userservice.dto;

public class AccountVerifiedEvent {
    private String eventId;
    private String eventType;
    private String eventVersion;
    private String source;
    private String occurredAt;
    private AccountCreatedPayload data;

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
    public AccountCreatedPayload getData() { return data; }
    public void setData(AccountCreatedPayload data) { this.data = data; }
}
