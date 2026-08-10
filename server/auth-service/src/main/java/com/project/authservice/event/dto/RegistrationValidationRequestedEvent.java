package com.project.authservice.event.dto;

import java.time.Instant;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class RegistrationValidationRequestedEvent {
    private final String eventId;
    private final String eventType = "REGISTRATION_VALIDATION_REQUESTED";
    private final String eventVersion = "1.0";
    private final String source = "auth-service";
    
    @JsonSerialize(using = ToStringSerializer.class)
    private final Instant occurredAt;
    
    private final RegistrationValidationRequestedEventData data;

    public RegistrationValidationRequestedEvent(String eventId, Instant occurredAt, RegistrationValidationRequestedEventData data) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.data = data;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getEventVersion() { return eventVersion; }
    public String getSource() { return source; }
    public Instant getOccurredAt() { return occurredAt; }
    public RegistrationValidationRequestedEventData getData() { return data; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String eventId;
        private Instant occurredAt;
        private RegistrationValidationRequestedEventData data;

        private Builder() {}

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder data(RegistrationValidationRequestedEventData data) { this.data = data; return this; }

        public RegistrationValidationRequestedEvent build() {
            return new RegistrationValidationRequestedEvent(eventId, occurredAt, data);
        }
    }
}
