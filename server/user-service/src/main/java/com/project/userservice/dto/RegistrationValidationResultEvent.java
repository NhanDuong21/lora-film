package com.project.userservice.dto;

import java.time.Instant;

public class RegistrationValidationResultEvent {
    private final String eventId;
    private final String eventType = "REGISTRATION_VALIDATION_RESULT";
    private final String eventVersion = "1.0";
    private final String source = "user-service";
    private final String occurredAt;
    private final RegistrationValidationResultPayload data;

    public RegistrationValidationResultEvent(String eventId, Instant occurredAt, RegistrationValidationResultPayload data) {
        this.eventId = eventId;
        this.occurredAt = occurredAt != null ? occurredAt.toString() : null;
        this.data = data;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getEventVersion() { return eventVersion; }
    public String getSource() { return source; }
    public String getOccurredAt() { return occurredAt; }
    public RegistrationValidationResultPayload getData() { return data; }
}
