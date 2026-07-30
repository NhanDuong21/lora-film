# Notification operations runbook

Start infrastructure with `docker compose up -d`, apply
`deployment/database/notification-schema.sql`, verify the external Git registry,
then run notification service locally with `mvn spring-boot:run`.

Readiness checks:

- MySQL schema validates at startup.
- Kafka and Redis endpoints are reachable.
- `/actuator/health` reports the template registry as available and shows a `main` commit.
- The admin dashboard loads without exposing secrets.

For failures, search by notification/delivery public ID or correlation ID. Inspect the last attempt category and provider code. Retry only after correcting a permanent/template/payload fault; transient failures already retry automatically. Dead-letter reprocessing resets a delivery through the audited admin endpoint.

If Git is down, do not publish or render new deliveries. Restore repository access, check branch protection and credentials, then confirm readiness. Do not fall back to database or packaged templates.

Back up MySQL operational tables and the external Git repository independently. Restore Git with tags intact because commit SHA and version references are audit evidence.

No notification service Docker image or container exists. Docker Compose lifecycle commands affect infrastructure only.
