package com.project.authservice.event.dto;

import java.time.Instant;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class RegistrationValidationResultEvent {
    private String eventId;
    private String eventType;
    private String eventVersion;
    private String source;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Instant occurredAt;
    
    private RegistrationValidationResultEventData data;

    public RegistrationValidationResultEvent() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public RegistrationValidationResultEventData getData() { return data; }
    public void setData(RegistrationValidationResultEventData data) { this.data = data; }
}
