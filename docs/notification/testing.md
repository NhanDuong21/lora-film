# Notification testing

Backend:

```powershell
cd server/notification-service
mvn test
```

Tests cover strict variables and HTML sanitization, transactional preference bypass, marketing opt-out, idempotent request acceptance, Kafka ticket mapping, duplicate inbox handling, payment-event non-trigger behavior, Git draft isolation, publish tags, stale SHA conflicts, and rollback without history rewriting.

Booking integration:

```powershell
cd server/booking-service
mvn test
```

Frontend:

```powershell
cd client
npm run lint
npm run build
```

Repository checks should confirm no Lombok, Flyway, Liquibase, template entity/table/content, notification Dockerfile, or notification Compose service exists inside the notification implementation.

For an end-to-end smoke test, publish a valid template in the external registry, emit a `TICKET_ISSUED` event twice, and verify one request, one inbox record, channel deliveries, and the captured commit SHA/version.
