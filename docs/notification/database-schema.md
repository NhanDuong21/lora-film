# Notification database schema

The canonical notification SQL file is
[`notification-service-schema.sql`](../database/mysql/notification-service-schema.sql).
It drops and recreates `notification_db`, creates the least-privilege application
user, all operational tables, indexes, foreign keys/checks, and development
preferences. Review the credential placeholder before executing it. The reset is
destructive.

Templates, subjects, schemas, samples, and rendered bodies are forbidden in
MySQL. `notification_requests` stores the template key plus the exact commit
SHA/version selected during delivery for traceability.

Recipient email, phone, and Web Push subscription data are AES-GCM encrypted by
the service. The schema file is explicit and manually managed; Hibernate runs
with `ddl-auto=validate`. Flyway and Liquibase are not used. No SQL file for
another service is created or changed as part of this feature.
