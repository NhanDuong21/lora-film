# Auto Schedule Refactor — Rollback Guide

## Safety principles

Rollback is configuration-first and non-destructive. Never drop the additive columns/tables while a deployed binary may read them, and never delete previews or created Showtimes to hide a failure.

## Operational rollback

1. Set `cinemas.auto_schedule_engine='LEGACY'` only for an affected cinema through an approved configuration migration, then restart/reload Movie Service through the normal deployment process. There is no silent global fallback.
2. Keep Analytics and the new schema in place; old code ignores additive columns and the internal aggregate endpoint is read-only.
3. Stop applying unreviewed CP-SAT previews. Existing `PREVIEWED` CP-SAT previews may be cancelled; do not mutate or reinterpret them as S5.
4. Showtimes already applied are ordinary `DRAFT` rows. Review/cancel them through the existing Showtime lifecycle; do not run direct SQL deletion.
5. Preserve logs and solver/preview metadata for incident analysis.

## Application rollback order

Frontend can roll back first because existing Movie Service contracts remain. Movie Service can roll back next with the feature flag on `LEGACY`. Analytics can roll back last because its new endpoint is additive. Schema rollback is intentionally not part of the emergency path.

## Data migration rollback

The migration is additive. Leave new nullable/defaulted columns and indexes present. A later cleanup migration may be designed only after all deployed versions and retained previews no longer use them; that future destructive action requires separate approval.

## Verification after rollback

- New previews for overridden cinemas report the explicit legacy strategy, never an implicit fallback.
- Shadow execution writes no preview items or Showtimes.
- Apply remains ALL_OR_NOTHING and creates only DRAFT Showtimes.
- Existing S1–S5 and CP-SAT preview history remains readable.
- Restoring `CP_SAT` creates only new CP-SAT previews; no stored preview is reinterpreted.
