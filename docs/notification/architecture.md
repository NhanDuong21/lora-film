# Notification architecture

The service accepts versioned domain events from Kafka and authenticated internal commands. It writes request, recipient, delivery, attempt, inbox, outbox, preference, in-app, audit, scheduling, and dead-letter state to MySQL. It uses Redis only for best-effort published-template cache invalidation.

```text
booking-service outbox -> Kafka booking.domain.events.v1
                                 |
                                 v
                         notification inbox
                                 |
                    idempotent request + deliveries
                                 |
                  published template from external Git
                                 |
             email | in-app | mock SMS | mock Web Push
                                 |
                 attempts, retry, outbox, dead letter
```

`payment.succeeded` never creates `TICKET_PURCHASED`; only `ticket.issued` does. This prevents sending a ticket before issuance has actually completed.

The API gateway exposes customer and administrator paths. `/api/v1/internal/notifications/**` is deliberately not routed publicly and requires `X-Internal-Token`.

The notification application is a local Java process. Docker Compose contains infrastructure only and no notification image, build, container, or service.
