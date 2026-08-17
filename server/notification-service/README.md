# Notification service

Spring Boot notification runtime for email, in-app, SMS, and Web Push delivery. Operational state lives in MySQL; notification templates live only in a separate Git repository.

This service is intentionally **not packaged into Docker**. The root `docker-compose.yml` starts infrastructure only (MySQL, Redis, Kafka, and Zookeeper). Run this application as a local Java process.

## Local prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8, Redis 7, and Kafka reachable locally
- A private Git repository with a protected `main` branch
- A Base64-encoded 32-byte AES key for recipient encryption
- A shared internal-service token and JWT secret

Create the schema by reviewing and executing
[`docs/database/mysql/notification-service-schema.sql`](../../docs/database/mysql/notification-service-schema.sql).
It replaces the old notification database and is destructive.

Copy the safe Spring configuration template, set its referenced environment
variables, then run:

```powershell
Copy-Item src/main/resources/application.example.properties src/main/resources/application.properties
mvn test
mvn spring-boot:run
```

The application validates the schema with `spring.jpa.hibernate.ddl-auto=validate`; it never creates or mutates tables.

## Template registry

The default published template source is
[`NhanDuong21/template-mail`](https://github.com/NhanDuong21/template-mail.git). Its
`email/{language}/{domain}/*.html` layout is supported directly for delivery;
native `templates/**/manifest.json` templates take precedence when both formats
contain the same key/channel/locale. Template keys follow the uppercase file
name, for example `BOOKING_CONFIRMED` resolves to `booking_confirmed.html`.
When a requested locale is absent, the registry tries the repository's `vi`
then `en` version. IN_APP and WEB_PUSH content is derived from the title, first
heading, and first paragraph of the same HTML file and Git revision. Auxiliary
directories such as `email/_archive`, `email/assets`, and `email/preview-data`
are not exposed as published templates.

Set `NOTIFICATION_TEMPLATE_GIT_URI`, `NOTIFICATION_TEMPLATE_GIT_WORKDIR`, and
`NOTIFICATION_TEMPLATE_GIT_BRANCH` to override the source. Public read-only
delivery does not need credentials; admin draft/publish/rollback operations need
`NOTIFICATION_TEMPLATE_GIT_USERNAME` and `NOTIFICATION_TEMPLATE_GIT_TOKEN` with
write access. The work directory is runtime data and must stay outside this
repository and the application artifact. Draft branches use optimistic SHA
checks. Publishing creates a merge commit on protected `main` and an immutable
version tag; rollback creates another commit and tag.

The default work directory is `~/.lorafilm/template-mail-nhanduong21`. It is
deliberately different from the previous registry cache so changing the default
remote cannot leave an existing checkout attached to the old Git origin.

Published templates are refreshed automatically from `origin/main`. The first
check runs after 5 seconds and subsequent checks run every 30 seconds by
default. Configure this with `NOTIFICATION_TEMPLATE_AUTO_REFRESH_ENABLED`,
`NOTIFICATION_TEMPLATE_REFRESH_INITIAL_DELAY_MS`, and
`NOTIFICATION_TEMPLATE_REFRESH_INTERVAL_MS`. Refreshes are fast-forward only;
the service never overwrites local changes or resolves a diverged branch
automatically.

Template content is never seeded into MySQL, packaged under `src/main/resources`, or embedded in a Docker image.

Detailed design and runbooks are under [`docs/notification`](../../docs/notification/architecture.md).
