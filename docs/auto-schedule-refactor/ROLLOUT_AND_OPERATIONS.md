# Demand-Aware Auto Schedule Rollout and Operations

## Deployment order

1. Back up each service schema and apply `docs/database/migrations/2026-08-06-demand-aware-auto-schedule.sql` to Movie, Payment and Analytics databases. The migration is additive; do not drop legacy fields or rewrite preview history.
2. Deploy Payment and Booking event-context additions, then Analytics demand aggregation/internal-token protection.
3. Deploy Movie Service with the pinned OR-Tools native smoke test passing on the deployment OS.
4. Deploy API Gateway and Admin UI Quick Mode.
5. Keep `cinemas.auto_schedule_engine='CP_SAT'` for new previews. Use `LEGACY` only as an explicit per-cinema incident switch. Never reinterpret an existing preview under another engine.
6. Optionally enable read-only S5 comparison with `AUTO_SCHEDULE_S5_SHADOW_ENABLED=true`; it clones candidates in memory and writes neither preview items nor Showtimes.

## Canary gates

- Preflight blocker mix and p95 duration are stable.
- CP-SAT status is `OPTIMAL`/`FEASIBLE`; timeout and model-invalid counters are zero or within the approved budget.
- Candidate count stays below the 10,000 guard and p95 generation duration meets the service SLO.
- Preview stale/apply conflict rates do not regress.
- Admin modification rate, selected volume, expected contribution/occupancy, prime-time allocation and auditorium use are compared with the read-only S5 shadow.
- Applied rows are `DRAFT`, apply is idempotent, and no partial batch exists.

Production-day conclusions for forecast accuracy, cancellation impact, admin acceptance and multi-day contribution are **RUNTIME_VALIDATION_PENDING** until enough real showtimes finish. Instrumentation is implemented; do not convert absence of alerts into acceptance evidence.

## Feature flags

- Per cinema: `cinemas.auto_schedule_engine = 'CP_SAT' | 'LEGACY'`. Default and current engine are `CP_SAT`.
- Shadow: `autoschedule.shadow.s5-enabled` / `AUTO_SCHEDULE_S5_SHADOW_ENABLED`, default `false`.
- Demand history: `autoschedule.demand.analytics-enabled`, default `true`; disabling it produces explicit cold-start risk flags.
- Solver controls: `autoschedule.solver.timeout-seconds` and fixed `autoschedule.solver.random-seed`.

Changing the cinema flag changes the eligibility/configuration fingerprint, so an already-generated preview becomes stale rather than changing engine in place.

## Metrics and alerts

Prometheus is exposed through Actuator. Metric tags are limited to engine, solver status, outcome and known blocker code; cinema/movie/preview IDs never appear as labels.

| Area | Metrics / evidence | Suggested alert |
|---|---|---|
| Preflight | `autoschedule.preflight.duration`, `autoschedule.preflight.blockers`, `autoschedule.eligible.pairs` | p95 latency or blocker spike |
| Candidates | `autoschedule.candidate.generation.duration`, `autoschedule.candidates` | p95 latency; count near 10,000 |
| Solver | `autoschedule.solver.duration`, `autoschedule.solver.runs`, `autoschedule.solver.timeouts` | timeout/model-invalid > approved budget |
| Forecast | expected/actual occupancy, contribution, forecast-error and history-cancellation distributions | material backtest/forward drift |
| Preview/apply | stale, apply-conflict, admin-modification and preview-cancellation counters | rate regression against requests |
| Shadow | selected/contribution/occupancy/prime-time/auditorium-use deltas, duration and constraint-violation distributions | unexplained sustained delta |

`autoschedule.forecast.error` is a historical backtest absolute occupancy error at generation time. Forward error for completed production showtimes must be calculated by the dashboard from the persisted preview-item forecast, created Showtime link and payment-derived actuals; that production validation remains pending.

Structured logs contain engine/status and safe public correlation identifiers where needed, never tokens or customer facts. Rates are derived in PromQL from counters; they are not stored as mutable application state.

## Runtime validation checklist

- Compare CP-SAT and S5 shadow for showtime count, expected contribution/occupancy, prime-time allocation, auditorium utilization and duration over multiple business days.
- Measure actual occupancy and absolute forecast error only after showtimes finish and payment/cancellation/refund events settle.
- Review admin selection changes and cancellation rate by cohort.
- Exercise cinema-local midnight/DST boundaries for every deployed timezone.
- Confirm Analytics outage produces cold-start risk labels without blocking safe generation.
- Confirm solver timeout/infeasible/model-invalid remains an explicit failed request with no preview.
- Record sample window, query/dashboard version and approval owner before changing `RUNTIME_VALIDATION_PENDING`.

## Troubleshooting

- `PRICING_INCOMPLETE`/`PRICING_AMBIGUOUS`: follow the blocker action path; correct seat-type coverage or precedence, rerun preflight, then generate a new preview.
- `PLANNING_RANGE_FULLY_BLOCKED`: inspect closures, maintenance and existing Showtimes; never bypass the hard constraint.
- Configuration-stale or price mismatch: cancel the preview and regenerate after authoritative changes.
- Solver timeout: inspect candidate count and solver metrics; adjust the bounded timeout through configuration only after load testing. Do not fall back inside the request.
- Analytics unavailable: verify service URL/token/network. Cold-start is safe but lower-confidence; do not fabricate history.
- Windows native crash after an OR-Tools upgrade: restore 9.12.4544, run the one-variable native smoke test, and keep the additive data schema. Versions 9.14/9.15 failed on the development Windows/JDK 21 runtime.
- Docker Engine 29 returns HTTP 400 before Testcontainers starts: verify Movie Service resolves Testcontainers 1.21.4 or newer compatible 1.x. Version 1.19.8 negotiates the removed Docker API 1.32 and can make `disabledWithoutDocker` tests appear skipped even while Docker CLI works.
- Concurrent apply: retry only with the same idempotency key. A different key against an applied preview is a business conflict.

## Replacing the Demand Engine

Implement the `DemandEngine` interface behind a new immutable model version. Preserve output units and bounds (attendance, occupancy 0–1, currency-valued revenue/contribution, confidence 0–1, explanation and risk flags). Train only from time-correct data, retain cold-start behavior, add deterministic golden/backtest tests, version the feature/label snapshot, and run shadow comparison before activation. The optimizer consumes the contract and must not depend on model internals. Old preview model versions remain immutable and readable.
