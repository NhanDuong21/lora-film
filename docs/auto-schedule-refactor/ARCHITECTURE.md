# Demand-Aware Auto Schedule Architecture

## Runtime flow

```text
Admin Quick Mode
  → POST preflight (cinema + 1/3/7 days + optional Advanced filters)
  → Eligibility Engine (batch facts, compatible pairs, windows, pricing slots)
  → Candidate Generation (approved pairs only)
  → Demand Engine (Analytics history or explicit cold-start prior)
  → CP-SAT per service date
  → versioned Preview snapshot + KPI summary
  → Admin approval
  → Safety Gate (locks + fresh fingerprints + full revalidation)
  → one transaction creates DRAFT Showtimes and price snapshots
```

## Component boundaries

- `AutoSchedulePreflightService`: authoritative planning scope, blocker classification and reusable immutable preflight result.
- `AutoScheduleEligibilityService`: the reusable Admin eligibility/read model for movies and versions.
- `AutoSchedulePreflightService`: batch-loads eligible versions/auditoriums, format pairs, operating windows and pricing probes before candidate construction.
- `PricePolicyResolver`: batch-resolves active seat-type prices used by preflight and immutable candidate snapshots.
- `DemandHistoryProvider`: integration port. The Analytics implementation performs one batch read; a cold-start result is explicit data, not an exception-swallowing fake history.
- `DemandEngine`: maps a candidate plus real/cold-start history to attendance, occupancy, revenue, contribution, confidence and explanation.
- `DemandAwareCpSatAutoScheduleGenerationStrategy`: solver model and status/result metadata.
- Existing preview/apply lifecycle remains the transaction boundary and historical replay owner.
- `AutoScheduleMetrics`: bounded-cardinality Micrometer instrumentation and structured lifecycle logs.
- `AutoScheduleShadowComparisonService`: optional in-memory S5 comparison. It clones enriched candidates, never calls preview/showtime repositories, and cannot alter or replace CP-SAT output.

## Constraint ownership

Eligibility/preflight owns lifecycle, release range, active state, room/format compatibility, operating-hour existence, pricing completeness and whole-scope closure/maintenance blockers. Candidate generation owns exact slot containment and interval conflicts. CP-SAT owns conflicts between newly selected candidates plus objective constraints. Apply revalidates every hard constraint from authoritative current rows.

## Demand calculation

Available source facts are payment-derived bookings/tickets/revenue, cinema and movie occupancy, refund rate and generated forecasts in Analytics Service; Showtime metadata, price, capacity, release age, service day, time slot and format live in Movie Service. The first heuristic version combines those facts deterministically. New movies use cinema/time-slot priors and bounded exploration, with lower confidence. The interface is intentionally model-neutral so a forecasting service can replace the heuristic without changing CP-SAT.

## Preview immutability and staleness

A preview records timezone, policy, demand and solver versions; request, eligibility and pricing fingerprints; solver status/objective/duration; candidate demand/pricing snapshots; aggregate KPIs; created/expiry/audit/idempotency data. Apply recomputes mutable fingerprints under locks. Any mismatch is stale and the transaction creates nothing.

## Security and privacy

Admin routes require `ROLE_ADMIN`. Analytics history uses a service-to-service internal token and returns only aggregate operational facts—no customer identity or booking details. Logs exclude secrets and raw internal tokens.

## Rollout boundary

`cinemas.auto_schedule_engine` is the per-cinema feature flag. Its additive default is `CP_SAT`; `LEGACY` is an explicit compatibility/kill-switch choice and is included in the eligibility fingerprint. CP-SAT failures remain failures and never invoke S5. Optional S5 shadow is controlled by `autoschedule.shadow.s5-enabled` and is read-only.

The native dependency is pinned to OR-Tools 9.12.4544. Versions 9.14.6206 and 9.15.6755 crashed the project's Windows/JDK 21 process even for a one-variable solve. Any upgrade requires the native binding smoke test and optimizer determinism/non-overlap tests on all deployment operating systems.
