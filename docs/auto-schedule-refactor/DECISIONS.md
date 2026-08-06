# Auto Schedule Refactor — Decisions

## ADR-001: tomorrow is backend-owned

The authoritative first service date is `LocalDate.now(cinemaZone).plusDays(1)`. Presets are inclusive 1/3/7-day horizons. Legacy explicit dates cannot move the boundary and a legacy request for today is rejected.

## ADR-002: additive compatibility

Historical `BALANCED_V1`, `BALANCED_V1_S2`, `BALANCED_V1_S3`, `BALANCED_V1_S4` and `BALANCED_V1_S5` rows are immutable replay data. `DEMAND_CP_SAT_V1` is current. Existing URLs and JSON fields stay valid; new fields are additive and old explicit selection lists are interpreted as Advanced include filters.

## ADR-003: format compatibility

The repository has `MovieFormat` and `ScreenType` but no independent 3D-capability attribute. Therefore specialised formats are strict (`IMAX→IMAX`, `FOUR_DX→FOUR_DX`, `SCREENX→SCREENX`) while `TWO_D` and `THREE_D` may use any active screen. This avoids inventing a non-existent capability flag. A future auditorium-capability table can replace this policy behind the Eligibility Engine.

## ADR-004: pricing is a preflight hard constraint

Pricing coverage is evaluated from batch-loaded active policies, rules and active auditorium seat types across discrete candidate start slots. It produces pricing snapshots without constructing `ShowtimeCandidate` objects. Missing or equal-rank ambiguous prices block preview generation.

## ADR-005: real analytics with explicit cold start

Movie Service does not own booking history. A batch-only internal Analytics endpoint exposes existing payment-derived cinema/movie aggregates. The Demand Engine consumes it when reachable. Absence is represented as cold-start data with lower confidence and deterministic cinema/time priors; no synthetic bookings are created.

## ADR-006: CP-SAT model

Use Google OR-Tools Java 9.12.4544. Each candidate is a Boolean variable. Segment constraints enforce auditorium non-overlap. Marginal selection variables and close-show penalties encode diminishing value and cannibalisation. Monetary values are converted to bounded integer objective units because CP-SAT is integer-only. The version is pinned because, on the project's Windows/JDK 21 runtime, even a one-variable model crashed the JVM in native code on 9.15.6755 and 9.14.6206; the same binding smoke test passes on 9.12.4544. Any upgrade must pass the native smoke test plus deterministic/non-overlap optimizer tests on every supported OS.

## ADR-007: failure semantics

No fallback from CP-SAT to S5 is permitted. Invalid/infeasible/unknown/timeout solve outcomes are explicit failures and record solver status. An explicitly configured `LEGACY` engine is a rollout choice, not a fallback. Shadow S5 is read-only.

## ADR-008: lock order

Apply locks preview, cinema, then sorted auditoriums. Closure creation locks cinema; maintenance creation locks its auditorium. This serialises competing writes on the same resource and avoids applying a schedule against concurrently inserted closure/maintenance facts.

## ADR-009: observability cardinality

Metrics use bounded tags such as engine/status/blocker code, never preview, movie, cinema or candidate IDs. Correlation IDs and public preview IDs may appear in structured logs, not metric labels.

## ADR-010: Testcontainers compatibility

Movie Service test dependencies pin Testcontainers 1.21.4. Spring Boot 3.3.5 otherwise manages 1.19.8, whose Docker client negotiates API 1.32; Docker Engine 29 requires at least API 1.40 and returns HTTP 400. Version 1.21.4 retains the 1.x module/API surface and supports recent Docker Engine changes. The final suite executes all MySQL integration tests with zero skips.
