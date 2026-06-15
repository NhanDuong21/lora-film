package com.project.authservice.event.dto;

import java.time.Instant;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Top-level Kafka event envelope published to {@code auth.account.created.v1}.
 *
 * <p>Fixed metadata:
 * <ul>
 *   <li>{@code eventType}    = {@code ACCOUNT_CREATED}</li>
 *   <li>{@code eventVersion} = {@code 1.0}</li>
 *   <li>{@code source}       = {@code auth-service}</li>
 * </ul>
 *
 * <p>Generated per-event:
 * <ul>
 *   <li>{@code eventId}    = {@link java.util.UUID#randomUUID()}</li>
 *   <li>{@code occurredAt} = current UTC {@link Instant}</li>
 * </ul>
 *
 * <p>No setters are provided; use {@link Builder} for construction.
 */
public class AccountCreatedEvent {

    /** Unique identifier for this specific event instance (UUID v4). */
    private final String eventId;

    /** Discriminator constant – always {@code ACCOUNT_CREATED}. */
    private final String eventType;

    /** Schema version – always {@code 1.0}. */
    private final String eventVersion;

    /** Originating service – always {@code auth-service}. */
    private final String source;

    /**
     * Wall-clock time when the event was created (UTC).
     * Serialized as an ISO-8601 string (e.g. {@code 2026-06-12T10:30:00Z}),
     * never as a numeric epoch timestamp.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private final Instant occurredAt;

    /** Business payload. */
    private final AccountCreatedEventData data;

    /** All-args constructor used by {@link Builder}. */
    public AccountCreatedEvent(String eventId,
                               String eventType,
                               String eventVersion,
                               String source,
                               Instant occurredAt,
                               AccountCreatedEventData data) {
        this.eventId       = eventId;
        this.eventType     = eventType;
        this.eventVersion  = eventVersion;
        this.source        = source;
        this.occurredAt    = occurredAt;
        this.data          = data;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getEventId()      { return eventId; }
    public String getEventType()    { return eventType; }
    public String getEventVersion() { return eventVersion; }
    public String getSource()       { return source; }
    public Instant getOccurredAt()  { return occurredAt; }
    public AccountCreatedEventData getData() { return data; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String eventId;
        private String eventType;
        private String eventVersion;
        private String source;
        private Instant occurredAt;
        private AccountCreatedEventData data;

        private Builder() {}

        public Builder eventId(String eventId)           { this.eventId = eventId;             return this; }
        public Builder eventType(String eventType)       { this.eventType = eventType;         return this; }
        public Builder eventVersion(String eventVersion) { this.eventVersion = eventVersion;   return this; }
        public Builder source(String source)             { this.source = source;               return this; }
        public Builder occurredAt(Instant occurredAt)    { this.occurredAt = occurredAt;       return this; }
        public Builder data(AccountCreatedEventData data){ this.data = data;                   return this; }

        public AccountCreatedEvent build() {
            return new AccountCreatedEvent(eventId, eventType, eventVersion, source, occurredAt, data);
        }
    }
}
