# Auto Schedule Phase S5 - Per-day Movie Distribution

## Why S5 exists

S4 guarantees minimum movie coverage per service date, but its remaining
selection objective is still weighted interval scheduling. A shorter movie or
a deterministic tie-break winner can therefore occupy nearly every remaining
slot after all eligible movies have appeared once.

S5 is the current strategy for newly generated previews. Persisted S4 previews
remain immutable and are never regenerated as S5.

## Selection contract

1. Run the complete S4 scoring, non-overlap optimization, coverage guard, and
   quality checks.
2. For each authoritative service date, count selected showtimes by underlying
   movie, not by movie version.
3. Build a deterministic balanced alternative from the complete valid
   candidate universe for that date, choosing the least represented movie and
   then the earliest compatible occupancy end.
4. Accept the rebuilt alternative only when it improves distribution while:
   - retaining at least 90% of the S4 average base candidate score; and
   - retaining at least 90% of the S4 occupied-room seconds.
5. Continue with local substitutions for any remaining safe improvement. An
   unselected candidate may replace one or more overlapping selected
   candidates in the same auditorium only when:
   - it strictly reduces the sum of pairwise movie-count differences;
   - every removed movie is currently overrepresented relative to the
     replacement movie;
   - the local fallback retains its bounded base-score budget; and
   - the global auditorium non-overlap invariant still holds.
6. Choose between valid replacements deterministically by resulting imbalance,
   score loss, removed count, start-time proximity, and canonical business
   identity.
7. Stop when counts are balanced, no safe improvement exists, or the bounded
   pass limit is reached.

The result is intentionally quality-guarded rather than blindly equal. Movies
with substantially longer occupancy or materially worse candidate scores may
retain a small count difference when an equal split would violate the daily
quality or occupied-room floor. Occupancy-normalized guards avoid treating a
short movie as intrinsically higher quality merely because more screenings fit
inside the same operating window.

## Runtime and persistence

- No schema change is required.
- S5 uses the same bounded existing-showtime coverage read as S4.
- The persisted strategy version and idempotency fingerprint are
  `BALANCED_V1_S5`.
- Score breakdown remains the S4 breakdown. S5 changes only selected flags and
  does not claim a synthetic score bonus for a distribution replacement.
- Manual edits and apply-time validation remain authoritative after generation.

## Regression evidence

- Equal six-slot alternatives starting at `6-0-0` become `2-2-2`.
- A lower-score alternative stops exactly at the 90% daily quality floor.
- A longer replacement may displace two overlapping dominant slots without
  violating the final non-overlap invariant.
- A realistic three-room universe with 105/120/135-minute occupancies cannot
  leave the shortest movie above 60% of selected showtimes.
- S3 and S4 remain resolvable for immutable history replay.
