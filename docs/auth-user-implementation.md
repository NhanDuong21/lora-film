# Auth and User Services Implementation Guide

Date: 2026-07-26

This guide describes the completed auth-service and user-service slice, including its frontend integration, persistence, events, cache behavior, security controls, and local deployment.

## Architecture

- `auth-service` owns accounts, authentication, access/refresh tokens, sessions, roles, permissions, OAuth identities, password recovery, login history, security audit, and auth events.
- `user-service` owns user/customer profiles, employees, departments, positions, payroll, avatars, dashboard projections, user audit, and user-domain events.
- The API gateway validates JWTs and routes auth administration paths to auth-service and `/api/users/**` to user-service.
- MySQL is the source of truth. Redis stores short-lived security state and selected read caches. Kafka carries cross-service integration events.
- Both services use a transactional outbox for domain events: the business transaction persists an envelope first and a scheduled publisher sends it after commit with bounded exponential backoff.

## Auth capabilities

Public authentication APIs are under `/api/auth`:

- `POST /register`
- `POST /verify` and the design-compatible alias `/verify-email`
- `POST /send-otp`
- `POST /login`
- `POST /refresh-token` and the design-compatible alias `/refresh`
- `POST /forgot-password`
- `POST /reset-password`

Authenticated APIs include `/logout`, `/logout-all`, `/change-password`, `/me`, and `/sessions`. Administrative APIs are exposed at `/api/accounts`, `/api/roles`, `/api/permissions`, and `/api/audits`.

Access tokens expire after 15 minutes. Refresh tokens expire after 7 days, are stored only as SHA-256 hashes, rotate on every refresh, and are linked to a device session. Logout blacklists the access-token hash for its remaining lifetime and revokes the linked refresh token. Password changes, password resets, account restrictions, and role changes revoke existing credentials.

Login records IP address, raw user agent, a derived device/browser name, last activity, and expiry. Repeated failed logins are limited with `login_attempt:<sha256-email>`. OTPs use `otp:<email>`, reset state uses `password_reset:<sha256-token>`, and access-token revocation uses `blacklist:<sha256-jwt>`.

OAuth login supports Google, GitHub, and Facebook. Provider subject identifiers are stored in `account_providers`, multiple providers can link to one account, and successful login returns tokens to the frontend callback in the URL fragment rather than the query string.

Lifecycle events use the explicit contract names `ACCOUNT_CREATED`,
`ACCOUNT_VERIFIED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`, `ACCOUNT_DISABLED`,
`PASSWORD_CHANGED`, `PASSWORD_RESET`, `ROLE_ASSIGNED`, `ROLE_REMOVED`, and
`PERMISSION_UPDATED` where their corresponding operations occur.

## User capabilities

The user API is under `/api/users`:

- Profile: `GET|PUT /profile`, avatar upload/delete/download
- Customers: pageable search, detail, block, and unblock under `/customers`
- Employees: pageable search, detail, create/update, suspend/activate/resign, and department/position transfer under `/employees`
- Employee documents: upload, active list, metadata history, authenticated download, and audited soft delete under `/employees/{accountId}/documents`
- Departments and positions: list/search/create/update/delete with reference checks
- Payroll: pageable search, employee self-history, detail, create/update, approve, mark paid, and cancel
- Dashboard: aggregate customer, employee, and payroll metrics under `/dashboard`

Payroll totals are calculated as:

```text
total = basicSalary + allowance + bonus - deduction
```

One payroll is allowed per employee and month. Approved and paid payrolls are immutable; paid payrolls cannot be cancelled. Database uniqueness and optimistic version fields protect these rules under concurrency.

Avatar uploads accept JPEG, PNG, and WebP, verify both declared media type and magic bytes, enforce a 5 MB limit, use generated file names, and retain database upload history. Employee documents accept PDF, DOCX, JPEG, and PNG up to 10 MB; DOCX packages and file signatures are inspected, paths and stored names are generated, and physical deletion occurs only after the metadata transaction commits. File paths are normalized before access.

## Database schema

- `docs/database/mysql/auth-service-schema.sql`
- `docs/database/mysql/user-service-schema.sql`

These two canonical scripts are applied directly to MySQL before the services start.
They create foreign keys, uniqueness constraints, check constraints, audit columns,
lifecycle indexes, outbox retry fields, and idempotent reference data. Hibernate
uses `ddl-auto=validate` so the entity mappings are checked without changing the
database.

For an existing database, take a backup and review the schema changes before
applying the scripts. For a clean installation, create `auth_db` and `user_db`,
run the corresponding script, and then start each service.

## Kafka contracts

Auth topics:

- `auth.registration.validation.requested.v1`
- `auth.registration.validation.result.v1`
- `auth.account.verified.v1`
- `auth.account.lifecycle.v1`
- `auth.domain.events.v1`

User topics:

- `user.profile.created.v1`
- `user.domain.events.v1`

Every outbox envelope contains `eventId`, `eventType`, `eventVersion`, `source`, `occurredAt`, and `data`. Auth publishes registration, verification, account lifecycle, password, role, and permission events. User publishes customer, employee, payroll, avatar/profile, and synchronized-status events. Account creation consumers are idempotent by aggregate identity and unique database constraints.

Consumers acknowledge records only after successful processing. Failures are retried three times with a fixed backoff and then routed to the source topic's `.dlq` dead-letter topic; handlers rethrow processing failures so offsets are not silently committed.

## Redis cache policy

User-service cache keys are prefixed with `user-service:cache:`:

| Cache | TTL | Invalidation |
| --- | ---: | --- |
| `departments` | 6 hours | Department mutations |
| `positions` | 6 hours | Position mutations |
| `userDashboard` | 5 minutes | Customer, employee, payroll, and account lifecycle mutations |

Profiles and payroll calculations are intentionally not cached.

## Security and observability

- Every endpoint is denied by default except explicitly listed auth, OAuth, Swagger, avatar-file, and health paths.
- Controller authorization uses roles and JWT permission authorities.
- CORS origin patterns are supplied through `ALLOWED_ORIGIN_PATTERNS`.
- Passwords use BCrypt. Password policy validation requires upper/lowercase letters, a digit, a symbol, and at least eight characters.
- Reset credentials, JWTs, refresh tokens, passwords, and raw OTPs are not written to application logs.
- Actuator health, liveness/readiness groups, metrics, and Prometheus output are enabled.
- Swagger UI is available at each service's `/swagger-ui/index.html`; OpenAPI JSON is at `/v3/api-docs`.
- The frontend includes dedicated 401, 403, 404, and 500 states; role guards route forbidden access to 403 and a top-level error boundary handles unexpected render failures.

## Configuration

Copy `.env.example` to `.env` and replace all `change-me` values. Required security settings include:

- `JWT_SECRET` — a strong shared HMAC key
- `INTERNAL_NOTIFICATION_TOKEN`
- `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, and `MYSQL_PASSWORD`
- `ALLOWED_ORIGIN_PATTERNS` for the frontend origins allowed to call the services
- provider client ID/secret pairs for any OAuth provider that should be enabled

OAuth redirect URIs must point to:

```text
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/github
http://localhost:8080/login/oauth2/code/facebook
```

## Local development

```bash
mysql -u root -p < docs/database/mysql/auth-service-schema.sql
mysql -u root -p < docs/database/mysql/user-service-schema.sql
cd server/auth-service && mvn spring-boot:run
cd server/user-service && mvn spring-boot:run
```

MySQL, Redis, Kafka, and Eureka must be reachable through the values configured
in each service's `application.properties`. The services are run directly with
Maven; no service-level Dockerfile is required.

Endpoints:

- Frontend: `http://localhost:5173`
- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- Auth health: `http://localhost:8081/actuator/health`
- User health: `http://localhost:8086/actuator/health`

## Verification commands

```bash
cd server/auth-service && mvn test
cd server/user-service && mvn test
cd api-gateway && mvn test
cd client && npm test -- --run
cd client && npm run build
```

The auth/user frontend scope is lint-clean. The repository-wide lint command still reports pre-existing errors in booking, concessions, facilities, and an unrelated finance page; those files are outside this implementation slice.

The final automated results were 16 passing auth tests, 12 passing user tests,
and 173 passing frontend tests across 37 files.
