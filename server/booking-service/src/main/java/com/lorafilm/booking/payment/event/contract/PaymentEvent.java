package com.lorafilm.booking.payment.event.contract;

import java.time.Instant;

public record PaymentEvent(
    String eventId,
    String eventType,
    String aggregateId,
    String aggregateType,
    Integer eventVersion,
    Instant occurredAt,
    String producer,
    String schemaVersion,
    String correlationId,
    PaymentEventPayload payload
) {}
