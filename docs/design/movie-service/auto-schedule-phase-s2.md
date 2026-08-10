# Auto Schedule Phase S2

## Candidate universe

New previews use strategy version `BALANCED_V1_S2`. A candidate exists only when its film interval fits completely inside an operating window:

```text
start >= open
filmEnd = start + duration
filmEnd <= close
```

The auditorium cleaning buffer may finish after closing. Release-window, cinema-closure, maintenance, and existing-Showtime conflicts remain generated items with `REJECTED` validation status. Film-end-after-close starts and duplicate `(auditoriumId, movieVersionId, startTime)` keys are never materialized or persisted.

`totalCandidateCount` is the unique fit-only universe and equals the number of persisted preview items. `validCandidateCount + rejectedCandidateCount` therefore always equals `totalCandidateCount`.

## Shared traversal and limit

Estimation and generation consume the same `UniqueCandidateSlotTraversal`. It owns service-date and window ordering, auditorium/version ordering, fit boundaries, minute stepping, duplicate suppression, and overlapping-window ownership. The earliest deterministic service-date/window traversal owns a duplicate.

The estimator uses only slot keys. It creates no `ShowtimeCandidate` or preview-item objects and stops before invoking the materialization callback for unique key 10,001. Exactly 10,000 is accepted; 10,001 produces `AUTO_SCHEDULE_TOO_MANY_CANDIDATES`. Matrix, DST, overlap, boundary, and 200-seed randomized tests assert estimator/generator parity.

## Immutable generation context

Generation first bulk-loads repository facts and maps them into `AutoScheduleGenerationContext`. The context contains no repository reference and exposes immutable cinema, auditorium, movie/version, operating-window, and planning snapshots.

Conflict indexes are separate from continuity facts:

- Cinema closures use a merged half-open interval index.
- Maintenance uses a merged half-open index per auditorium.
- Existing-Showtime conflicts use a merged half-open occupancy index per auditorium.
- Scoring continuity retains every individual occupancy end and uses binary search for the closest prior fact.

Existing-Showtime occupancy is calculated with the cleaning buffer from the matching auditorium snapshot. Candidate generation, validation, and scoring perform no repository access. The database-backed manual/apply validator remains authoritative and shares the same pure movie and scheduling rule primitives; apply still reloads current state.

## Query behavior

The stable contract is behavioral: cinema, auditorium, movie/version, operating-hour, closure, maintenance, and existing-Showtime context categories are each invoked at most once on a normal non-empty request; the idempotency lookup is separate. Query growth is independent of candidate count. Preview and identity item inserts remain candidate-dependent.

The H2 `test` profile query-budget fixture measured the following pre-persistence path. The lifecycle persistence service was replaced only for this measurement so statement counts do not mix reads with identity inserts.

| Fixture | Dimensions | Candidates | Repository reads | Context reads | Hibernate prepared statements | Wall time | Observed heap delta |
|---|---|---:|---:|---:|---:|---:|---:|
| Small | 1 room, 1 version, 1-minute film, 15-minute granularity, 00:00-23:48 | 96 | 8 | 7 | 8 | 47.029 ms | +2,097,152 B |
| Medium | 4 rooms, 1 version, 1-minute film, 1-minute granularity, 00:00-23:48 | 5,712 | 8 | 7 | 8 | 284.135 ms | -68,491,872 B |
| Near limit | 7 rooms, 1 version, 1-minute film, 1-minute granularity, 00:00-23:48 | 9,996 | 8 | 7 | 8 | 648.418 ms | +17,837,632 B |

Environment: Oracle Java 21.0.5, Windows 11, H2 in-memory database, Spring `test` profile. Warm-up consisted of Spring context initialization and fixture persistence only. Wall time and heap deltas are observations, not correctness gates; the negative heap delta reflects normal garbage collection. No timing or heap assertion is present in the suite.

## Persistence and compatibility

All preview-item inserts, counter updates, and the `PREVIEWED` transition remain in one transaction. There are no chunk commits, premature counter writes, or persistence-context clears. An integration test forces the second identity insert to violate the slot uniqueness constraint and confirms the first insert and preview transition both roll back.

Idempotency fingerprint verification uses the preview's stored strategy version. A legacy `BALANCED_V1` preview is replayed with the V1 canonical fingerprint and is never regenerated or rewritten. A changed request with the same legacy key returns `IDEMPOTENCY_KEY_REUSED`. A new key creates `BALANCED_V1_S2`.

## Unchanged behavior

The REST envelope, frontend contract, score weights and breakdown keys, global comparator/ranking, greedy selection behavior, apply services, auditorium locks, `ALL_OR_NOTHING`, apply-time revalidation, schema, pricing, and S3/S4 concerns are unchanged.
