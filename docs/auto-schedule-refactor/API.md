# Demand-Aware Auto Schedule API

All Admin endpoints require `ROLE_ADMIN`, use the existing API envelope, and are routed by API Gateway. Dates are cinema-local service dates; persisted instants remain UTC.

## Preflight

`POST /api/admin/auto-schedules/preflight`

Minimal Quick Mode body:

```json
{
  "cinemaPublicId": "<cinema-public-id>",
  "planningDays": 3
}
```

`planningDays` is one of `1`, `3`, or `7`; omission means `1`. The backend always computes `planningFrom` as tomorrow in the cinema timezone. Advanced clients may send include/exclude version and auditorium public-ID lists. Includes and excludes are filters only and never bypass safety.

The response returns authoritative dates/timezone, eligible movie/version/auditorium/pair counts, blocker objects (`code`, `message`, `actionPath`), and eligibility/pricing/configuration fingerprints. A blocked response creates no candidate and no preview.

Stable blocker codes include `CINEMA_NOT_ACTIVE`, `NO_ELIGIBLE_VERSIONS`, `NO_ELIGIBLE_AUDITORIUMS`, `NO_COMPATIBLE_PAIRS`, `MISSING_OPERATING_HOURS`, `PRICING_INCOMPLETE`, `PRICING_AMBIGUOUS`, and `PLANNING_RANGE_FULLY_BLOCKED`.

## Generate preview

`POST /api/admin/showtime-schedules/generate-preview`

Quick Mode sends cinema, planning-days preset and an idempotency key. Legacy `scheduleFrom`/`scheduleTo` remain additive-compatible but, when supplied, must exactly equal cinema-local tomorrow and the selected 1/3/7-day horizon. Omitted version/auditorium lists mean all eligible values. Preflight runs before normalization and candidate generation.

The preview summary includes the immutable request scope; policy, demand-model, strategy and solver versions; solver status/objective/bound/duration; eligibility/pricing/configuration fingerprints; expected attendance, occupancy, revenue and contribution; selected count and expiry/version data.

## Review and apply

- `GET /api/admin/showtime-schedules/{previewPublicId}` returns the paged candidate timeline and per-item demand/pricing/explanation/risk snapshots.
- `PUT /api/admin/showtime-schedules/{previewPublicId}/items` atomically updates selections with `expectedVersion`; the final selected occupancy set must remain non-overlapping.
- `POST /api/admin/showtime-schedules/{previewPublicId}/apply` requires an idempotency key and expected preview version.

Apply locks preview, cinema and sorted auditoriums; recomputes timezone/configuration/pricing facts; validates all selected items; and creates only `DRAFT` Showtimes in one transaction. Any stale, price, conflict or integrity failure creates none.

## Internal Analytics contract

`POST /internal/analytics/demand-snapshot` is not Gateway-routed. It requires `X-Internal-Token`, accepts one cinema, bounded history range, timezone and movie public IDs, and returns aggregate cinema/movie/time-slot/format facts. No customer identity or raw booking record is exposed. Movie Service uses a bounded connect/read timeout and an explicit low-confidence cold-start snapshot if Analytics is unavailable.
