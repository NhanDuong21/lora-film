# Auto Schedule Phase S3

## Selection objective

New previews use strategy version `BALANCED_V1_S3`. Phase S3 preserves the S2 candidate universe, validation, scores, score breakdown, and global ranking. It changes only the automatic selected flags.

For each independent optimization component, the selector maximizes the exact `BigDecimal` sum of scores for `VALID` candidates subject to non-overlapping half-open occupancy intervals:

```text
[startTime, occupancyEndTime)
```

Rejected candidates never contribute. Exact adjacency is compatible. Show count, occupied minutes, fairness, diversity, utilization, demand, and revenue are not secondary objectives.

## Greedy behavior replaced

The S2 selector ranked candidates globally by validation status, descending score, start, auditorium public ID, movie-version public ID, and rejection code. It then greedily selected each valid candidate that did not overlap an already-selected candidate in the same auditorium. Its conflict scan was `O(C^2)` in the worst case.

A source-compatible counterexample is one long candidate with score `85.000` and occupancy `[18:00, 20:45)` versus two candidates with scores `60.000 + 60.000` and occupancy `[18:00, 19:15)` and `[19:15, 20:30)`. Greedy keeps 85; S3 keeps the compatible pair for 120.

Global `rankingPosition` still uses the exact S2 comparator. Ranking is display order, not optimizer decision order.

## Service dates and safe components

The logical partition key is auditorium plus the authoritative service date on the candidate's originating `OperatingWindow`. A start after midnight remains owned by the prior service date when it came from an overnight window. The selector never derives ownership from the start's local calendar date.

Because cleaning occupancy may pass closing time and adjacent operating windows are not guaranteed disjoint, service-date groups are coalesced per auditorium when their occupancy hulls overlap. Hull equality remains independent because intervals are half-open. This conservative merge produces components that cannot conflict with one another and does not reduce the score optimum.

After reconstruction, the selector sorts every auditorium's selected intervals and enforces a global non-overlap invariant. A violation raises `AUTO_SCHEDULE_SELECTION_INVARIANT_VIOLATION`; generation persists no items and the existing lifecycle marks the preview `FAILED`.

## Weighted interval scheduling

Each safe component is sorted by:

1. occupancy end;
2. start;
3. auditorium public ID;
4. movie-version public ID;
5. authoritative service date.

For each zero-based candidate `i`, binary search finds the largest predecessor `p(i)` whose occupancy end is at or before `i.startTime`. With `best[k]` representing the first `k` candidates, the recurrence is:

```text
include = score[i] + best[p(i) + 1]
exclude = best[i]
best[i + 1] = max(include, exclude)
```

Equal totals choose `exclude`. This lexicographically minimizes the reverse canonical membership vector, with unselected before selected, and makes the selected set independent of input order without introducing a business-policy tie objective.

DP state is limited to predecessor indexes, exact best-score values, and take/exclude bits. Reconstruction walks those arrays backward. No DP cell stores or copies a selected schedule.

For `C` candidates, total sorting and predecessor work is `O(C log C)`; DP, reconstruction, and the final invariant are `O(C)`. Auxiliary memory is `O(C)`.

## Compatibility and persistence

- `BALANCED_V1` and `BALANCED_V1_S2` fingerprints remain supported for immutable idempotent replay.
- Same-request legacy replay returns the existing preview; changed-request reuse returns `IDEMPOTENCY_KEY_REUSED`.
- Unknown stored versions fail with `AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT`.
- The schema default remains `BALANCED_V1`; no schema or REST DTO shape changes are required.
- Automatic selection keeps `selectedAt` and `selectedBy` null. Manual edits retain their timestamps, actor, optimistic versioning, and selected-count recalculation.
- Manual selection can still create an overlapping set; authoritative apply-time occupancy revalidation rejects it atomically. That pre-existing editing policy is outside S3.

## Correctness and performance evidence

Selector tests include a 250-seed brute-force oracle for small partitions, the known greedy counterexample, adjacency and positive-overlap boundaries, rejected candidates, auditorium independence, cross-midnight coalescing, 100 input shuffles, equal-total ties, malformed data, and the defensive invariant.

The canonical diagnostic run used 10,000 candidates across 50 logical partitions and 50 safe components, with a largest component of 200. On Oracle Java 21.0.5 and Windows 11 it observed:

| Phase | Time |
|---|---:|
| Global ranking | 20.154 ms |
| Partition construction/coalescing | 5.435 ms |
| WIS sorts | 3.671 ms |
| Predecessors | 1.281 ms |
| DP | 0.316 ms |
| Reconstruction | 0.015 ms |
| Global invariant | 1.558 ms |
| Total measured selector | 34.861 ms |

The individually attributed phase values sum to `32.430 ms`. The `2.431 ms` difference from the `34.861 ms` total is measured but unattributed orchestration and diagnostic overhead: candidate precondition validation, per-component loop and timer bookkeeping outside the phase boundaries, boundary timer reads, and the diagnostic selected-count scan. The total timestamp is captured before the diagnostics record constructor, so record construction is not included.

The run selected 850 candidates and the external test sampling observed a `+1,050,624 B` heap delta. No GC is requested, so this coarse delta can include ambient JVM effects and is evidence rather than a memory-allocation guarantee. The public production selector path performs no timing calls, heap sampling, diagnostic selected-count pass, or diagnostics object construction. Instrumentation is reachable only through the package-private test diagnostic entry point; both entry points use the same selection helpers. Times and heap deltas are environment-specific evidence, not test thresholds.

## Deferred work

Phase S3 does not change scoring weights, candidate generation, query loading, fairness, quotas, movie diversity, saturation, demand/revenue inputs, pricing, screen-format compatibility, apply locking, frontend pages, or solver technology. Those remain deferred beyond S3.
