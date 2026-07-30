# Notification service

Spring Boot notification runtime for email, in-app, SMS, and Web Push delivery. Operational state lives in MySQL; notification templates live only in a separate private Git repository.

This service is intentionally **not packaged into Docker**. The root `docker-compose.yml` starts infrastructure only (MySQL, Redis, Kafka, and Zookeeper). Run this application as a local Java process.

## Local prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8, Redis 7, and Kafka reachable locally
- A private Git repository with a protected `main` branch
- A Base64-encoded 32-byte AES key for recipient encryption
- A shared internal-service token and JWT secret

Create the schema by reviewing and executing the single
`deployment/database/notification-schema.sql` file. It replaces the old
notification database and is destructive.

Copy the safe Spring configuration template, set its referenced environment
variables, then run:

```powershell
Copy-Item src/main/resources/application.example.properties src/main/resources/application.properties
mvn test
mvn spring-boot:run
```

The application validates the schema with `spring.jpa.hibernate.ddl-auto=validate`; it never creates or mutates tables.

## Template registry

Set `NOTIFICATION_TEMPLATE_GIT_URI`, `NOTIFICATION_TEMPLATE_GIT_WORKDIR`, and Git credentials. The work directory is runtime data and must stay outside this repository and the application artifact. Draft branches use optimistic SHA checks. Publishing creates a merge commit on protected `main` and an immutable version tag; rollback creates another commit and tag.

Template content is never seeded into MySQL, packaged under `src/main/resources`, or embedded in a Docker image.

Detailed design and runbooks are under [`docs/notification`](../../docs/notification/architecture.md).
