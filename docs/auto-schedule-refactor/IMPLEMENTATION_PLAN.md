# LoraFilm Auto Schedule Refactor — Implementation Plan

## Goal and non-negotiable rules

This programme replaces the current `BALANCED_V1_S5` decision path with a demand-aware Google OR-Tools CP-SAT path while preserving immutable historical previews. The default admin flow is cinema + Tomorrow/3 days/7 days; movie versions and auditoriums are discovered automatically. All generated Showtimes remain `DRAFT`.

The backend owns the planning boundary. For a cinema timezone, `planningFrom` is always the cinema-local tomorrow. A legacy caller may still submit `scheduleFrom`, but it must equal that authoritative value. Today and past dates are rejected. Same-day adjustment is outside this workflow.

Hard constraints reject a candidate. A blocker stops before preview persistence. Soft constraints affect the CP-SAT objective. Business insights are shown only after a preview exists. Explicit advanced overrides are versioned inputs; they never bypass hard safety constraints.

## Audit baseline

- Current backend: Spring Boot 3.3.5 / Java 21 in `server/movie-service`.
- Current engine: S1–S5 culminates in `BALANCED_V1_S5`; S4 supplies coverage/fairness selection and S5 performs a deterministic per-day distribution pass.
- Current generation contract requires explicit date range, movie-version IDs, auditorium IDs, slot granularity, TTL and idempotency key.
- Current apply already uses preview optimistic/pessimistic locking, sorted auditorium locks, item reloading, complete revalidation, pricing resolution, ALL_OR_NOTHING transaction semantics and DRAFT Showtime creation.
- Current gaps: today is accepted; eligibility list performs an N+1 version query; no format compatibility rule exists; pricing is checked only after preview; no demand model; no CP-SAT; preview lacks demand/solver/fingerprint KPI snapshots; closure creation does not share the cinema lock used by apply; Quick Mode requires technical selections.
- Analytics Service already owns real payment-derived cinema/movie history, occupancy, refund and forecast data. Demand integration must consume these facts and use explicit cold-start priors when they are absent.
- API Gateway already routes `/api/admin/showtime-schedules/**`; it will also route `/api/admin/auto-schedules/**`.
- Baseline on 2026-08-06: frontend 111 test files / 443 tests passed. Movie Service produced 613 tests, 608 passed, 1 skipped group (44 skipped tests), with four pre-existing failures in `ShowtimeCommandServiceIntegrationTest` and `ShowtimeSchedulingConcurrencyIntegrationTest` caused by fixed release dates preceding the current clock.

## Phase plan and dependencies

### Phase 0 — business and architecture lock

Deliver the five programme documents, freeze the rules above, inventory contracts/schema/tests, and define additive compatibility. Done when architectural decisions and rollback steps are explicit and the audit is reproducible.

### Phase 1 — safety hardening

Make cinema-local tomorrow authoritative; revalidate the timezone snapshot on apply; add a shared lock order of preview → cinema → auditoriums; make closure creation lock the cinema and maintenance creation keep the auditorium lock; classify duplicate-key versus foreign-key/check/data-integrity failures; retain atomic idempotent apply. Depends on Phase 0. Done when boundary, race and idempotency tests pass.

### Phase 2 — Eligibility Engine and Preflight

Add `POST /api/admin/auto-schedules/preflight`. Batch-load eligible movies/versions/auditoriums, compatible pairs, operating windows, closures, maintenance, existing/frozen Showtimes and pricing facts. Return counts, blockers, authoritative dates, eligibility fingerprint and pricing fingerprint. No preview row or `ShowtimeCandidate` may be created on failure. Depends on Phase 1 locking/date rules.

### Phase 3 — candidate model and generation

Generate only from preflight-approved version/auditorium pairs and pricing-covered slots. Enrich candidates with service date, timezone, format/capacity, pricing snapshot, demand values, prime-time flag, risk flags and policy/model versions. Preserve the 10,000 unique-candidate guard. Depends on Phase 2.

### Phase 4 — automatic demand engine

Introduce a replaceable `DemandEngine` interface and deterministic heuristic implementation. Add an internal Analytics Service batch endpoint backed by real cinema/movie aggregate history. Use cinema/time priors and a bounded exploration value for cold starts; lower confidence when history is absent. Never ask the admin for hotness. Depends on Phase 3 inputs and Analytics facts.

### Phase 5 — demand-aware CP-SAT

Use pinned `com.google.ortools:ortools-java:9.12.4544`. One Boolean variable represents each valid candidate. Enforce room non-overlap with exact time-segment constraints; all other hard constraints are proven by the preflight/candidate pipeline and rechecked before solving. Maximise integer-scaled expected contribution plus coverage/exploration value minus capacity/risk/cannibalisation and increasing marginal-show penalties. Use a bounded timeout, fixed seed and one worker for deterministic tests. `UNKNOWN`, timeout, invalid and infeasible results fail explicitly; there is no S5 fallback. Depends on Phases 2–4.

### Phase 6 — versioned preview and safe apply

Add non-destructive columns for policy/demand/solver versions, solver status/duration/objective, eligibility/pricing fingerprints and forecast KPIs; add matching item snapshots. Reject stale fingerprints/timezone/rules at apply, then create all DRAFT Showtimes in one transaction. Historical S1–S5 rows remain readable with nullable/default columns. Depends on Phase 5.

### Phase 7 — minimal Admin UI

Replace the default wizard with cinema + Tomorrow/3/7-day presets, preflight summary/blockers, short preview KPIs/timeline and one approval action. Put include/exclude and restrictions in a separate Advanced drawer. Do not expose S5, CP-SAT, weights, candidate volume or slot granularity in Quick Mode. Depends on new contracts from Phases 2 and 6.

### Phase 8 — comprehensive verification

Cover today rejection, timezone boundary, pricing gaps, every empty eligibility class, incompatible formats, partial maintenance, closure/apply race, concurrent apply, retry key, stale preview, cold start, prime-time competition, diminishing demand, timeout/infeasible, no-preview-on-preflight-fail, Quick Mode defaults and DRAFT-only apply. Run targeted tests after each checkpoint, full backend/frontend suites, production builds, Testcontainers when Docker is available, and browser verification when the runtime can start. Depends on all implementation phases.

### Phase 9 — shadow comparison and rollout

Default `AUTO_SCHEDULE_ENGINE` to `CP_SAT`; allow per-cinema `CP_SAT|LEGACY` compatibility overrides. Retain S5 only for historical replay, explicit legacy rollout and read-only shadow comparison. Persist/emit comparison metrics without writing shadow Showtimes or silently falling back. Production-day acceptance criteria remain `RUNTIME_VALIDATION_PENDING`. Depends on Phase 8.

### Phase 10 — observability and operations

Add timers/counters/distributions for preflight, candidates, pairs, solver outcome, selected volume, forecasts, stale/conflict rates and blockers. Document API, migration, feature flags, troubleshooting, replacement of the Demand Engine, rollback and runtime validation. Depends on Phase 9 instrumentation.

## Migration and compatibility

- Only additive nullable/defaulted columns and new indexes/tables are allowed. No destructive migration or data rewrite is required.
- Persisted `BALANCED_V1*` previews remain readable and applyable under their historical contract if still fresh.
- The existing generation endpoint remains available. Its old explicit lists become Advanced include filters; omitted lists mean all eligible data. A supplied legacy `scheduleFrom` is accepted only when it equals cinema-local tomorrow.
- Existing preview/detail/history/apply routes and response fields remain; new response fields are additive.
- New engine version is `DEMAND_CP_SAT_V1`; policy and demand versions are independent.

## Test and rollout gates

Each phase requires compile, targeted tests, diff review and regression review. Final gates are Movie Service package/build, Analytics Service package/build, frontend test/build, API contract tests, solver determinism/timeout tests and available browser verification. A failure caused by missing Docker/runtime/credentials is reported as unverified, never as passed.

Rollout order: schema → Analytics internal demand endpoint → Movie Service with CP-SAT disabled for selected canary cinemas if needed → frontend Quick Mode → enable CP-SAT per cinema → observe shadow metrics → expand. Rollback never deletes preview or Showtime data.
