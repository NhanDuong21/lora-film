# Auth/User Part 8 Readiness Audit

Date: 2026-07-27  
Scope: `server/auth-service`, `server/user-service`, API gateway integration, in-scope frontend, schemas, and local development

## Decision

**GO for auth/user integration testing, with external credentials and live infrastructure treated as deployment prerequisites.**

The database, backend, frontend, event, cache, security, and observability implementation required to begin the testing phase is present. Automated module tests and production frontend compilation pass.

## Gate results

| Gate | Result | Evidence |
| --- | --- | --- |
| Database schema | Pass | The canonical auth and user SQL scripts contain the complete MySQL 8 schema and seed data; Hibernate validates the resulting tables without changing them. |
| Auth backend | Pass | Registration/OTP, login, rotating refresh, logout/revocation, sessions, password flows, account/role/permission administration, OAuth providers, audit, rate limiting, Redis security state, and transactional events are implemented. |
| User backend | Pass | Profile/customer/employee/employee-document/department/position/payroll/avatar/dashboard APIs, validation, role authorization, audit, caching, consumers, and transactional events are implemented. |
| Frontend | Pass | Auth recovery/session pages, 401/403/404/500 handling, and real profile/avatar, customer, staff/document, department, position, payroll, dashboard, account, role, permission, and audit integrations are present. |
| Local runtime | Pass | Auth and user services build and run directly with Maven once MySQL, Redis, Kafka, and Eureka are available. |
| Auth tests | Pass | 16 tests, 0 failures/errors with `server/auth-service: mvn test -q` |
| User tests | Pass | 12 tests, 0 failures/errors with `server/user-service: mvn test -q`, including payroll rules, document storage/signature/path/date/soft-delete, and security coverage |
| Gateway tests | Pass | `api-gateway: mvn test -q` |
| OpenAPI | Pass | Auth and user services expose OpenAPI documents and Swagger UI through their configured Springdoc endpoints. |
| Frontend tests | Pass | 37 files and 173 tests pass with `npm test -- --run` |
| Frontend production build | Pass | `npm run build` |
| In-scope lint | Pass | Auth/user files introduced or changed by this implementation have no ESLint errors. |
| Repository-wide lint | Existing debt | 42 errors remain only in unrelated booking, concessions, facilities, and finance files. |

## Implemented reliability and security controls

- Access JWTs expire in 15 minutes; refresh credentials expire in 7 days and rotate on every use.
- JWT, refresh token, reset token, and blacklist database/cache lookups use hashes where persistence is required.
- Account restrictions, password changes/resets, role changes, session revocation, and logout revoke credentials.
- OAuth callback tokens are returned in the URL fragment.
- OTP attempts and login attempts are rate-limited in Redis.
- Upload media type, magic bytes, size, generated name, and normalized path are validated.
- Employee document metadata is retained for history after deletion; file cleanup is deferred until its database transaction commits.
- Domain events are committed to outbox tables with asynchronous publication, retry counters, next-attempt timestamps, and error capture.
- Kafka consumers use record acknowledgements and bounded retries before publishing failures to per-topic dead-letter topics.
- Redis caches are limited to department, position, and dashboard projections with explicit TTL and eviction.
- Security rules deny by default and method authorization protects administration operations.

## External prerequisites for live end-to-end validation

These are environment inputs, not missing source implementation:

1. Put strong local values in `.env`, especially `JWT_SECRET`, MySQL credentials, and `INTERNAL_NOTIFICATION_TOKEN`.
2. Configure OAuth provider applications and redirect URIs before testing social login.
3. Ensure the notification service and the configured CCCD validation dependency are reachable when exercising registration and password email delivery.
4. Apply the two canonical schema scripts to clean databases, or review the changes carefully before applying them to pre-existing non-empty schemas.
5. Run full browser, OAuth, notification-delivery, Kafka failure/recovery, and concurrency suites in the target deployment environment.

Detailed operation and API notes are in
[`docs/auth-user-implementation.md`](auth-user-implementation.md).
