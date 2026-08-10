# Ticket notification flow

1. Payment confirmation completes.
2. Booking service issues and persists all tickets.
3. In the same application transaction, booking service writes one `TICKET_ISSUED`
   outbox event with a stable event ID and the existing booking user reference.
4. The Kafka outbox publisher emits a versioned envelope to `booking.domain.events.v1`.
5. Notification service inserts `(source_service, source_event_id)` into its inbox. A duplicate is acknowledged without another request.
6. The consumer creates one transactional `TICKET_PURCHASED` request with an idempotency key derived from the event ID.
7. Delivery workers load only the published `TICKET_PURCHASED` template, validate payload variables strictly, render safely, and dispatch.
8. Provider attempts, retry timing, template commit/version, final state, and dead-letter state remain auditable.

The booking payload includes the existing booking user reference, booking/payment
identifiers, movie and cinema details, showtime, seats/ticket codes, food and
beverage items, promotion, price totals, secure ticket reference URL, locale,
timestamps, correlation, and causation identifiers. It does not require a schema
change in `booking_db`.

Marketing preferences do not suppress this flow because ticket delivery is transactional.
