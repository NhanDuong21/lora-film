# Auto Schedule Phase S4 - Quality and Fairness

## Implementation status

S4A is implemented and registered but not active. `BALANCED_V1_S4` is a
supported generation, persistence, fingerprint, and history value, while
`AutoScheduleStrategyVersions.CURRENT` remains `BALANCED_V1_S3`. The S3
strategy continues to delegate to the unchanged six-component scorer and exact
weighted-interval selector.

Implementation and activation are deliberately separate. This change does not
alter the default selections produced by current generation requests.

## Source-confirmed baseline

- Candidate generation, validation/rejection, authoritative service date,
  cleaning occupancy, manual selection, apply, pricing, and idempotent replay
  remain unchanged.
- S3 scores valid candidates with `base`, `primeTime`, `offPeak`, `earlySlot`,
  `auditoriumFit`, and `scheduleContinuity` at scale three.
- S3 selection is exact weighted interval scheduling over compatible occupancy
  intervals. Global ranking is display metadata and is not a selection order.
- S3 context loading continues to read existing Showtimes only for requested
  auditoriums and converts them to conflict and continuity indexes.
- S4 context loading adds one bounded cinema-wide projection and retains
  immutable existing counts by `(serviceDate,movieId)`.
- The history repository remains one content query plus one count query.

## Product Policy Decision Table

The S4A rows are explicitly approved for the dormant S4 strategy. Deferred
features remain excluded.

| Choice | Proposed value | Supported alternatives | Behavioral consequence | Counterexample | Correctness or tuning | Recommendation | Approval |
|---|---|---|---|---|---|---|---|
| Aggregate quality retention | 90% of S3 base-score sum per service date | 80%, 85%, 95%, absolute loss budget, none | Lower values trade more S3 quality for coverage | Nine 100-point selections plus one 10-point selection retain 91% of a 1000-point baseline | Product safety policy | Use in hybrid protection | APPROVED S4A |
| Coverage search weight | `+20.000` | +5, +10, +15, computed value, no steering | Determines when an uncovered movie changes the alternative WIS state | A=80 and B=74 may not move with +5; a large weight may displace much better intervals | Tuning | One coverage-only pass | APPROVED S4A |
| Distribution search weight | Not implemented | 0, +5, +10, configured value | Would encourage additional movie/daypart pairs | A owns Prime while X owns Morning; a weight may swap Morning ownership | Tuning | Defer | EXCLUDED |
| Saturation sequence | Not implemented | Fixed, capped linear, geometric, quota, none | Would apply pressure against repeats | A/A/A may become A/B/B without demand evidence | Tuning | Defer | EXCLUDED |
| Reweighting rounds | Exactly one additional coverage pass | 4, 8, 12, stable/cycle termination plus a cap | Bounds runtime and avoids cycle semantics | Uniform bonuses can alternate all-A/all-B | Correctness and policy | One pass | APPROVED S4A |
| Daypart boundaries | Not implemented | Two buckets, configured buckets, weekday rules, no dayparts | Would control distribution credit | A 22:30 show can reasonably be late Prime or Night | Required only for daypart behavior | Defer | EXCLUDED |
| Fairness unit | Underlying movie per authoritative service date, cinema-wide | Movie version, auditorium-local, whole requested range | Aggregates versions and resets coverage per operating day | 2D and IMAX count as one movie | Required to define fairness | Underlying movie/date | APPROVED S4A |
| Existing Showtime participation | Cinema-wide final-schedule-aware coverage | Preview-only, requested-auditorium-aware | Already scheduled movies begin covered | Existing A=5/B=0 must not give A new coverage priority | Required to define fairness | Final-schedule-aware | APPROVED S4A |
| Existing Showtime eligibility | Non-deleted DRAFT, OPEN_FOR_BOOKING, CLOSED, or FINISHED | Narrower configured status sets | Controls which existing rows count | CANCELLED and soft-deleted rows do not cover a movie | Product policy | Explicit four-status set | APPROVED S4A |
| Local opportunity-loss ceiling | `15.000` base-score points per anchor | 5, 10, 20, relative percentage, none | Prevents a locally destructive anchor hidden by unrelated high scores | A 95-point anchor displaces two 60-point shows, losing 25 | Product safety tuning | Combine with aggregate retention | APPROVED S4A |

No excluded objective is implied by the S4A name.

## Implemented Minimal S4A

The implemented strategy is:

1. Run unchanged S3 scoring and exact WIS to obtain a baseline state.
2. Determine eligible uncovered movie/service-date groups from approved
   existing-Showtime facts plus baseline selections.
3. Choose at most one deterministic, locally admissible coverage anchor per
   group and add `coverageSearchAdjustment` only to those anchors.
4. Run exact WIS once more.
5. Admit the alternative only when both per-date aggregate retention and the
   per-anchor opportunity-loss ceiling pass.
6. Choose between the two independently WIS-optimal states by coverage, base
   score, and canonical membership.

Distribution, saturation, daypart fairness, repeated reweighting, and exact
global fairness optimization remain deferred.

## Existing-Showtime-Aware Fairness Analysis

The current bounded blocking query reads non-deleted/non-cancelled Showtimes
overlapping the planning envelope for requested auditoriums. It fetches the
auditorium but not the lazy movie relation. Context construction then discards
movie identity and retains only conflict intervals and continuity endpoints.
It therefore cannot efficiently answer existing counts, represented dayparts,
or saturation by cinema/service-date/movie. Traversing the lazy relation would
risk N+1 reads and would still omit non-requested rooms.

Final-schedule-aware coverage is loaded without a migration by one projection
query returning movie identity and start instant,
bounded by requested cinema, distinct requested underlying movies, and at most
seven service dates. In-memory operating-window resolution assigns authoritative
service dates. The existing `idx_showtimes_movie_cinema_start` index supports
the range. S3 generation remains at eight repository reads; S4 generation uses
nine. Candidate loops remain repository-free and history remains one content
query plus one count query.

- Contract A, preview-only: only proposed selections count. With existing
  A=5/B=0, A and B both begin uncovered and a tie may leave the final schedule
  at 6:0.
- Contract B, final-schedule-aware: eligible existing Showtimes plus selections
  count. In the same example A begins covered, so only B receives coverage
  assistance. A showing in a non-requested room still counts under cinema-wide
  scope.

Contract B is the approved and implemented contract. DRAFT, OPEN_FOR_BOOKING,
CLOSED, and FINISHED rows count; CANCELLED and soft-deleted rows do not. An
existing start is converted with the cinema timezone and assigned only when it
belongs to a resolved `[open,close)` operating window. Overnight starts retain
the prior service date. A fact in a gap or outside every resolved window is
ignored: this is generation-time fairness classification, not historical
service-date reconstruction. Coverage-only S4A removes an already covered
movie's priority but does not penalize its count; saturation remains excluded.

## Minimal S4 vs Full S4 Comparison

| Dimension | Minimal S4A | Full iterative S4 |
|---|---|---|
| Correctness | Two independently exact WIS states; unchanged occupancy invariants | Exact inner WIS, but more interacting set objectives and cycle behavior |
| Explainability | One coverage search weight and one outer comparison | Three changing weights, reference states, and repeated rounds |
| Implementation risk | Moderate: registry, bounded facts, coverage pass | High: repeated-state management, distribution, saturation, cycles |
| Runtime | Two WIS executions, `O(C log C)` with a fixed multiplier | Up to `O(R x C log C)` |
| Production usefulness | Addresses low-loss omission and existing coverage | Richer diversity, but semantics lack business evidence |
| Extensibility | Registry and immutable facts permit later objectives | More policy becomes costly to revise once persisted |

Minimal S4A is recommended. Complexity is not added merely to label the output
as fair or diverse.

## Score Explainability Contract

For S4 previews, persisted item `score` means the winning round's
effective search score. Its breakdown will contain the six S3 components plus
`coverageSearchAdjustment`; the values must sum exactly to `score`.

The adjustment is a search-steering weight. It is not demand, revenue,
attendance, popularity, or commercial value. A selected set must be the exact
WIS optimum for the persisted effective scores of its originating round. A
separate set-level comparator chooses the winning round, so the winning round
need not have the greatest score sum among all evaluated rounds. An individual
unselected candidate may also score higher than a selected candidate when a
compatible set has the greater total.

Tests must re-run WIS from the persisted winning-round scores and reproduce the
selected flags, with a brute-force oracle for small fixtures.

## Quality Protection Alternatives

- Aggregate per-date floor: limits total loss, but unrelated high scores can
  hide one poor replacement. Nine 100s plus one 10 still retain 91% of 1000.
- Per-anchor opportunity-loss ceiling: compare an anchor with the S3-baseline
  base-score sum it positively overlaps in its auditorium. It prevents a bad
  local substitution, but many small losses can accumulate.
- Candidate-level minimum: easy to state but ignores replacements. A 75 can
  displace two 95s, while a 74 can be rejected despite replacing only a 76.
- Hybrid: require both aggregate per-date retention and per-anchor local-loss
  limits, always keeping the S3 baseline admissible. Use exact `BigDecimal`
  multiplication rather than percentage division.

The implemented hybrid contract uses exactly 90% and `15.000`. The baseline is
always admissible, and no candidate-level minimum is used.

## Implementation verification evidence

The dormant S4A implementation was measured on Java 21.0.5 / Windows 11 with
10,000 generated candidates. After one warm-up, three repeated shuffled runs
produced identical scores, ordered breakdowns, ranks, anchors, and selected
flags. Median timings were 61.919 ms for baseline WIS, 15.439 ms for anchor
calculation, 23.001 ms for coverage WIS, 0.040 ms for the outer comparator, and
236.331 ms total; the greatest measured total was 291.056 ms. The observed
coarse heap delta was 98,235,392 bytes. These measurements are diagnostic
evidence, not a production latency threshold.

The S4 context benchmark performed five context repository reads, including
exactly one fairness projection, for nine total generation reads. Its measured
projection/context path was 242.300 ms under the H2 test profile. The current
S3 path remains at eight total generation reads, and the history query budget is
unchanged.

The final focused and regression backend verification ran 162 tests with no failures. The full
backend suite ran 527 tests with three pre-existing bulk-seat failures, no
errors, and one skip; no S4 test failed. Frontend verification ran 39 tests and
the production build successfully. Full-project lint retains the existing 14
errors and five warnings in unrelated files; scoped lint for the changed
history files is clean.

## S4 Activation Checkpoint

Implementation and activation are separate changes. S4 activation requires:

- approved and documented active policy constants;
- focused S4 scoring, coverage, quality, persistence, and existing-fact tests;
- green V1, V1_S2, and V1_S3 replay tests;
- persisted-score WIS-optimality proof tests;
- identical repeated and shuffled runs;
- reviewed 10,000-candidate performance and query-budget evidence;
- no candidate-universe, validation, overlap, manual-selection, apply, pricing,
  schema, or history-query-budget regression.

Only a separately reviewed activation change may switch `CURRENT` from
`BALANCED_V1_S3` to `BALANCED_V1_S4`. That change must contain no algorithm
implementation or unrelated cleanup.
