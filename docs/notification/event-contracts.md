# Event contracts

Events are JSON envelopes:

```json
{
  "eventId": "uuid",
  "eventType": "TICKET_ISSUED",
  "eventVersion": 1,
  "source": "booking-service",
  "occurredAt": "2026-07-29T12:00:00Z",
  "correlationId": "booking-public-id",
  "causationId": "payment-reference",
  "userPublicId": "customer-public-id",
  "locale": "vi-VN",
  "payload": {}
}
```

Required fields are `eventId`, `eventType`, and a positive `eventVersion`. Notification inbox uniqueness is `(source_service, source_event_id)`. Consumer compatibility normalizes dots and hyphens in event types.

Topics:

- `booking.domain.events.v1`: consumes `TICKET_ISSUED`.
- `payment.domain.events.v1`: inbox/audit only; it does not issue ticket notifications.
- `user.domain.events.v1`: reserved for user lifecycle notification contracts.

New incompatible payloads require a new positive `eventVersion` and a backward-compatible consumer rollout.
