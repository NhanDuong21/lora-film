# Demand-Aware Auto Schedule — Final Progress and Evidence

## Final status

`COMPLETED_WITH_RUNTIME_VALIDATION_PENDING`

All Phase 0–10 work that can be implemented and verified in this repository is complete. Demand-aware CP-SAT is the current/default engine for new previews. Long-running production comparisons and forward forecast accuracy remain `RUNTIME_VALIDATION_PENDING`; their instrumentation and operating checklist are implemented.

## Phase 0 — audit and architecture lock

- Audited the S1–S5 pipeline, cinema timezone/hours/closures, maintenance and cleaning buffers, movie/version/format lifecycle, pricing, Showtime lifecycle, Booking/Payment/Analytics facts, API Gateway, Admin UI, schemas and tests.
- Chose additive migration and immutable legacy replay.
- Recorded implementation plan, ADRs, architecture, rollback and operational guidance under `docs/auto-schedule-refactor/`.

Result: complete. No destructive schema or data operation was used.

## Phase 1 — authoritative business invariants and safety

- Backend owns the first service date: `LocalDate.now(cinemaZone).plusDays(1)`.
- Only inclusive 1/3/7-day horizons are accepted. Supplying today or a mismatched legacy date range is rejected.
- Candidate intervals retain an authoritative service date and UTC instants.
- Apply lock order is preview → cinema → sorted auditoriums. Closure and maintenance writes take compatible resource locks.
- Cleaning buffers, operating windows, closures, maintenance and existing Showtime occupancy remain hard constraints.

Result: complete, including timezone-boundary, invalid-range and lock/concurrency coverage.

## Phase 2 — Eligibility and Preflight

- Added batch preflight before candidate creation or preview persistence.
- Eligibility covers active cinema, movies, versions and auditoriums; release windows; include/exclude filters; room ownership; format compatibility; operating hours; closures/maintenance/current schedules; active seat types and pricing coverage.
- Stable blockers include no eligible version/auditorium, no compatible pair, missing hours, incomplete/ambiguous pricing and fully blocked range, each with an Admin action path.
- Preflight returns authoritative dates, counts and eligibility/pricing/configuration fingerprints. A blocking result creates no candidate and no preview.

Result: complete. Pricing is a hard pre-generation gate.

## Phase 3 — real demand input pipeline

- Movie Service exposes capacity, auditorium, format and start-time context to Booking.
- Booking persists/passes the required price and showtime facts to Payment.
- Payment snapshots the facts into its analytics outbox payload.
- Analytics ingests payment-derived booking/revenue/refund facts and exposes a batch-only aggregate demand snapshot endpoint protected by `X-Internal-Token`.
- The endpoint returns aggregate operational facts only; no customer identity or raw booking data crosses the boundary.

Result: complete. Analytics security tests cover valid and invalid internal tokens.

## Phase 4 — automatic Demand Engine

- Added the model-neutral `DemandEngine` contract and deterministic `AutomaticDemandEngineV1`.
- Estimates attendance, occupancy, revenue, contribution and confidence from payment-derived history, local day/time/format, price/capacity and release age.
- New titles use bounded cinema/time priors and exploration with explicit cold-start risk flags; no manual hotness input or synthetic history exists.
- Demand model/version, explanation and risk flags are persisted per candidate.

Result: complete. Historical absolute occupancy error is instrumented; forward completed-showtime error remains runtime validation.

## Phase 5 — Demand-Aware CP-SAT

- Added Google OR-Tools CP-SAT with one Boolean decision per candidate.
- Hard constraints cover new-candidate auditorium overlap, release/eligibility, allowed slots, selected-set validity and bounded solve/model size.
- Integer objective combines expected contribution, occupancy/prime-time/utilization value, marginal demand and close-show cannibalisation penalties.
- Solver timeout, deterministic seed, objective/bound/duration and explicit `OPTIMAL`/`FEASIBLE`/failure statuses are retained.
- `DEMAND_CP_SAT_V1` is current. There is no implicit S5 fallback.
- OR-Tools is pinned to 9.12.4544 because 9.14.6206 and 9.15.6755 crashed the Windows/JDK 21 JVM in the native one-variable smoke test; 9.12 passes native, deterministic and non-overlap tests.

Result: complete.

## Phase 6 — immutable Preview and all-or-nothing Apply

- Preview stores immutable request scope, policy/demand/strategy/solver versions, solver evidence, three fingerprints, demand/pricing snapshots and KPI totals.
- Idempotency replay never regenerates an existing preview; legacy preview versions remain readable and immutable.
- Selection edits use optimistic versioning and must remain non-overlapping.
- Apply re-locks authoritative resources, checks expiry/version/status, recomputes configuration and prices, revalidates every selected item and creates only `DRAFT` Showtimes plus immutable price snapshots.
- Any stale, pricing, overlap, concurrent-write or integrity error rolls the transaction back; partial batches are not allowed.

Result: complete, including MySQL idempotency, concurrent generation/apply and atomic rollback tests.

## Phase 7 — Admin API and Quick Mode UI

- Added `POST /api/admin/auto-schedules/preflight`; retained compatible showtime-schedule endpoints and routed Admin Auto Schedule paths through API Gateway.
- Quick Mode asks only for cinema and a 1/3/7-day preset. Backend preflight supplies the actual dates and eligibility.
- Advanced include/exclude filters are isolated from Quick Mode and cannot bypass safety.
- Removed manual hotness and mandatory movie/version/auditorium selection from the normal flow.
- Preview shows expected attendance/occupancy/revenue/contribution, solver state, explanations/risk flags, timeline and the apply action.
- Client idempotency fingerprint includes planning horizon and Advanced filters.

Result: complete. Frontend test count remains 443; no tests were removed or weakened.

## Phase 8 — comprehensive testing

Final executed evidence on 2026-08-06:

- Movie Service: `mvn -q "-Ddebug=false" "-Dlogging.level.org.hibernate.SQL=OFF" test` — PASS, 117 suites / 616 tests / 0 failures / 0 errors / 0 skipped.
- Booking Service: `mvn -q test` — PASS, 36 suites / 165 tests / 0 failures / 0 errors / 0 skipped.
- Payment Service: `mvn -q test` — PASS, 21 suites / 97 tests / 0 failures / 0 errors / 0 skipped.
- Analytics Service: `mvn -q test` — PASS, 11 suites / 26 tests / 0 failures / 0 errors / 0 skipped.
- Client: `npm test -- --run --reporter=dot` — PASS, 111 files / 443 tests.
- Total: 1,347 passing tests, no failures/errors/skips in the final reports.
- Production packages: Movie, Booking, Payment, Analytics and API Gateway `mvn -q -DskipTests package` — PASS.
- Client: `npm run build` — PASS, 1,975 modules transformed.
- Testcontainers was pinned to 1.21.4 for tests because Spring Boot's managed 1.19.8 used Docker API 1.32, which Docker Engine 29 rejects. The final Movie run executed all 44 MySQL/Testcontainers cases instead of auto-skipping them.
- In-app browser verification was attempted against a running local Vite server. The browser runtime reported no installed/available browser (`agent.browsers.list() = []`), so visual browser QA is not claimed as passed. Component tests and production build passed.

Result: complete for repository-executable validation; visual browser runtime unavailable is explicitly recorded.

## Phase 9 — shadow comparison and rollout

- `cinemas.auto_schedule_engine` is an additive per-cinema `CP_SAT | LEGACY` flag with `CP_SAT` default.
- Legacy S5 remains available only as an explicit incident/compatibility selection and historical replay strategy. CP-SAT errors never fall back to it.
- Optional S5 shadow comparison clones enriched candidates in memory and performs no preview-item or Showtime writes.
- Shadow metrics compare selected volume, contribution, occupancy, prime-time allocation, auditorium use, duration and violations.

Result: implementation complete. Multi-day production comparison is `RUNTIME_VALIDATION_PENDING`.

## Phase 10 — observability and operations

- Added Actuator/Prometheus support and bounded-cardinality metrics for preflight, blockers, candidates, eligible pairs, solver duration/status/timeouts, expected/actual occupancy, forecast error, stale/apply conflicts, pricing/format/closure/maintenance blockers, admin modifications, cancellations and shadow deltas.
- Added structured lifecycle logging without token/customer data or unbounded metric labels.
- Completed architecture, API, additive migration, compatibility, rollback, feature-flag, runtime-validation, troubleshooting and Demand Engine replacement guidance.

Result: complete. Production SLOs, forward forecast error, cancellation outcome and admin-acceptance cohorts are `RUNTIME_VALIDATION_PENDING`.

## Migration and compatibility evidence

- Additive migration: `docs/database/migrations/2026-08-06-demand-aware-auto-schedule.sql`.
- Canonical Movie, Payment and Analytics schema documents were updated.
- New preview/item and analytics columns are nullable or defaulted; old rows are not rewritten.
- Legacy strategy versions, URLs and request fields remain readable. Old explicit selection lists act as Advanced filters.
- Existing preview strategy/version/fingerprint data is never reinterpreted under the current engine.

## Remaining runtime validation

- Compare CP-SAT and read-only S5 shadow over representative cinema/business-day cohorts.
- Measure actual occupancy and forward forecast error after showtimes finish and payment/refund/cancellation facts settle.
- Establish p95 generation/solver SLOs and approved timeout/blocker thresholds under production load.
- Review admin modification/acceptance and cancellation rates by cohort.
- Run visual Admin-flow browser QA when a browser runtime is available.

No source mock flow or TODO placeholder remains in the Auto Schedule scope. No commit, push, pull, rebase, reset, amend, force checkout, destructive migration, database drop or secret change was performed.
