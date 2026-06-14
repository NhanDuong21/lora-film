package com.project.authservice.event.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
 * <p>No setters are provided; use the {@link Builder} for construction.
 */
@Getter
@Builder
@AllArgsConstructor
public class AccountCreatedEvent {

    /** Unique identifier for this specific event instance (UUID v4). */
    private final String eventId;

    /** Discriminator constant – always {@code ACCOUNT_CREATED}. */
    private final String eventType;

    /** Schema version – always {@code 1.0}. */
    private final String eventVersion;

    /** Originating service – always {@code auth-service}. */
    private final String source;

    /** Wall-clock time when the event was created (UTC). */
    private final Instant occurredAt;

    /** Business payload. */
    private final AccountCreatedEventData data;
}
