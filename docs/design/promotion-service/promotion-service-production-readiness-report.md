# Promotion Service – production-readiness audit

Audit date: 2026-07-29  
Audited revision: merge commit `3344cb3a` plus the working-tree Promotion
changes visible after the latest branch pull.

## Executive conclusion

The Promotion service now has a coherent production core for campaign,
coupon, voucher, rule, compensation, reservation, partner settlement,
configuration, audit, outbox, and scheduler operations. Database migrations
are executable on MySQL 8.4 and Hibernate validation passes against the
migrated schema.

It is not yet an end-to-end production feature in the whole system. Booking
and Payment do not call Promotion, the gateway does not route most Promotion
paths, and the cross-service event contracts are not connected. Those gaps are
outside the requested edit scope, so no files in those services were changed.
Promotion must be released with the integration checklist below, otherwise the
new APIs will be unreachable or discounts will not be applied to a booking.

## What was audited

- Promotion source, controllers, DTO validation, state machines, repositories,
  locking/idempotency, outbox/inbox, scheduled jobs, security, configuration,
  Flyway migrations, and tests.
- `docs/api/promotion-service-api.md`,
  `docs/design/promotion-service/promotion-service-plan.md`,
  `docs/design/promotion-service/promotion-service-questions-and-business-rules.md`,
  `docs/design/promotion-service/specifications-promotion-sql.md`, and the
  canonical MySQL schema.
- Other related services were read-only reviewed: Auth, API Gateway, User,
  Movie, Score, Booking, Payment, and Client.

## Promotion APIs currently implemented

| Area | Implemented contract |
|---|---|
| Campaign | CRUD, search, approval, legal review, reject, status transitions, approval history, scheduled activation/expiry, kill switch |
| Rules | CRUD, search, clone, schema preview; supported conditions fail closed |
| Coupon | CRUD, generation, CSV import/export, search, validation, redemption ledger |
| Voucher | Issue/batch issue, update, revoke, extend, search, customer wallet, validation, redemption ledger |
| Reservation | Runtime validate; atomic reserve; detail; confirm; release; cancel; refresh; expiry and admin history |
| Compensation | Issue/update/search/detail with an auditable approval history |
| Partner | Partner CRUD and settlement lifecycle |
| Configuration | Versioned dynamic configuration, cache refresh, deprecation |
| Integration | Transactional outbox, broker acknowledgement/retry/DLQ, inbox deduplication (disabled by default until event contracts are agreed) |
| Operations | Job execution history, DB lease locks, internal scheduler hooks |

The current checkout policy is deliberately one benefit per order/booking:
one coupon or one voucher. Automatic discovery, multi-benefit stacking, and
Score point hold/commit/release remain documented roadmap/integration work; the
runtime does not pretend to support them.

## Changes made in this pass

- Accepted current Auth/User/Score identifiers: positive numeric account IDs
  or legacy UUIDs for `userPublicId`/`ownerPublicId`.
- Added automatic activation for approved, legally-passed, scheduled campaigns
  and a reasoned `KILL_SWITCH` transition. Ended campaigns can no longer be
  republished or activated.
- Tightened terminal campaign transitions and made repeated cancellation
  idempotent.
- Added method/class-level RBAC to every `/api/admin/**` controller and made
  the global admin matcher authentication-only so non-ADMIN business roles can
  reach their explicitly authorized APIs.
- Applied the configured Jackson mapper to 401/403 responses and made the JWT
  filter cover protected Swagger/Actuator endpoints.
- Aligned token validation with the current Auth contract: Promotion accepts
  only signed, non-expired `access` tokens that contain a subject and a
  positive numeric `userId`; refresh tokens can no longer authenticate API
  requests.
- Added a lease guard to inbound-event processing so two workers cannot apply
  the same event concurrently; expired leases are recoverable and manual DLQ
  reprocessing resets its retry budget.
- Completed the financial settlement state machine: a disputed settlement can
  be resolved back to approved, and a paid settlement can be closed as
  completed without reopening financial fields.
- Added Flyway V4 to normalize legacy `CHAR(n)` references to `VARCHAR(n)`;
  this aligns JPA and permits numeric account references without padding.
- Added MySQL/JPA schema validation, lifecycle, request-validation, and
  security integration tests.

## Related-service findings (read-only)

| Service | Finding | Effect |
|---|---|---|
| Auth | JWT now carries `userId: Long`, one role, permissions, and session ID; Promotion accepts the numeric identity. Bootstrap currently seeds only `ADMIN`, `EMPLOYEE`, and `CUSTOMER`. | Token format is compatible, but Promotion business roles must be provisioned before rollout. |
| API Gateway | Current routes cover only campaign paths. Coupon, voucher, rule, partner, configuration, customer wallet, reservation, event, and scheduler paths are absent. | Public/admin APIs return gateway 404 until routes are added. |
| Booking | No Promotion HTTP client or reservation-id propagation; booking event publisher is a logging/mock publisher. | Discounts cannot be reserved/confirmed/cancelled in a real checkout. |
| Payment | No Promotion client and no Promotion payment event; payment success is sent to an analytics topic. | Promotion confirmation/release is not triggered by payment. |
| Score | Internal hold/commit/release endpoints exist but no Promotion caller or event integration. | Loyalty-point promotions are not available. |
| Movie | Only booking-context/seat-layout internal endpoints; no Promotion client/basic profile contract. | Movie/cinema eligibility must be supplied by a trusted caller. |
| User | Profile APIs exist but no Promotion client or service-to-service profile contract. | Promotion must not call User synchronously until an authenticated contract exists. |
| Client | No Promotion API usage/UI; only booking discount placeholders. | No customer-facing discovery or wallet experience. |

## Mock-data decision

Promotion must not seed mock campaigns, coupons, vouchers, users, or
redemptions in a production database. The service starts with empty business
data and admins create real records through the protected APIs. Fixtures are
appropriate only for unit/contract tests and local development. The inbound
event consumer remains disabled by default until Booking/Payment publish the
agreed envelope; enabling it against the current mock/logging publishers would
silently lose lifecycle events.

## Go-live blockers outside Promotion scope

1. Add gateway routes for all public Promotion paths and keep `/internal/**`
   reachable only from trusted service-to-service networks.
2. Provision and assign the Promotion roles used by RBAC (`MARKETING_MANAGER`,
   `MARKETING_STAFF`, `FINANCE_DIRECTOR`, `CSKH_AGENT`,
   `LEGAL_COMPLIANCE`, `OPERATIONS_MANAGER`, and configuration/partner roles);
   Auth bootstrap does not create them.
3. Implement Booking → Promotion validate/reserve/refresh/cancel and carry the
   Promotion reservation ID through the booking/payment flow.
4. Implement Payment → Promotion confirm/release with idempotency and a shared
   event envelope/correlation ID.
5. Replace Booking's mock event publisher and configure a real Kafka broker,
   topic names, ACLs, retry/DLQ, and observability.
6. Decide whether Score points are part of checkout; if yes, implement the
   saga and compensation semantics before enabling point benefits.
7. Add customer-facing Client calls for wallet/discovery and define the
   User/Movie profile contracts needed for eligibility.

## Verification

From `server/promotion-service`:

```text
mvn -q "-Dtest=PromotionSecurityIntegrationTest" test
mvn -q "-Dtest=BenefitRequestValidationTest,ReservationRequestValidationTest,PromotionLifecycleServiceTest" test
mvn -q "-Dtest=PromotionMySqlJpaSchemaValidationTest" test
mvn -q "-Dtest=IntegrationEventStateServiceTest" test
mvn -q "-Dtest=PartnerSettlementServiceImplTest" test
mvn -q test
```

The MySQL test runs Flyway V1–V4 against MySQL 8.4 with
`hibernate.ddl-auto=validate`; it is the release gate for schema drift. The
final full suite completed with 68 tests, 0 failures, 0 errors, and 0 skipped.
