# Auto Schedule Preview History

## Scope

Preview history adds read-only discovery and navigation for persisted auto-schedule previews. It does not change schema, generation, validation, scoring, selection, expiration normalization, detail editing, locking, idempotency, pricing, or apply behavior.

All endpoints and browser routes remain admin-only. Every administrator can see all cinemas because the movie service has no tenant or cinema-manager scope.

## HTTP contract

```http
GET /api/admin/showtime-schedules
```

Accepted query parameters:

| Parameter | Meaning |
|---|---|
| `cinemaPublicId` | Exact current cinema public ID. Unknown well-formed values return an empty page. |
| `status` | Exact persisted `SchedulePreviewStatus`. |
| `strategyVersion` | `BALANCED_V1`, `BALANCED_V1_S2`, or `BALANCED_V1_S3`. |
| `scheduleFrom` | Inclusive lower overlap boundary as `YYYY-MM-DD`. |
| `scheduleTo` | Inclusive upper overlap boundary as `YYYY-MM-DD`. |
| `createdFrom` | Inclusive creation instant with `Z` or an explicit offset. |
| `createdTo` | Exclusive creation instant with `Z` or an explicit offset. |
| `page` | Zero-based page, default `0`. |
| `size` | Page size `1..100`, default `10`. |
| `sort` | One allowlisted `field,direction` pair, default `createdAt,desc`. |

Schedule matching is inclusive overlap:

```text
preview.scheduleTo >= scheduleFrom
preview.scheduleFrom <= scheduleTo
```

If both schedule bounds exist, `scheduleFrom <= scheduleTo`. If both creation bounds exist, `createdFrom < createdTo`. Malformed types, reversed ranges, unsupported enum/strategy/sort, negative pages, and sizes outside `1..100` return the standard HTTP 400 envelope.

Sort fields are `createdAt`, `scheduleFrom`, `scheduleTo`, `status`, `cinemaName`, `totalCandidateCount`, `selectedCandidateCount`, and `appliedAt`. `status` means persisted status, cinema names sort case-insensitively, and null application times sort last in either direction. Every order has an internal `id DESC` stable tie-break; `id` itself is not public or accepted as a sort field.

## Response

The response type is:

```text
ApiResponse<PageResponse<AutoSchedulePreviewHistoryItemResponse>>
```

The page is the admin-list shape:

```text
data, pageNo, pageSize, totalElements, totalPages, last
```

Each row exposes:

```text
previewPublicId, version
cinemaPublicId, cinemaName, timezoneSnapshot
scheduleFrom, scheduleTo
strategyVersion, applyMode
persistedStatus, displayStatus, editable, applicable
totalCandidateCount, validCandidateCount, rejectedCandidateCount
selectedCandidateCount, appliedShowtimeCount
createdAt, expiresAt, appliedAt, failureReasonSafe
```

Counters are persisted non-null values. `appliedShowtimeCount` equals `selectedCandidateCount` only for `APPLIED`, otherwise it is null. `appliedAt` is null until apply completes. Failed rows expose only the generic `AUTO_SCHEDULE_GENERATION_FAILED` message; raw stored failure text is not selected.

The response omits numeric preview/cinema IDs, numeric actor IDs, idempotency keys, fingerprints, preview items, item metadata, and raw exceptions. `cinemaName` is the current joined name and can change after a rename. `timezoneSnapshot` is stored with the preview and remains historical.

## Persisted and display lifecycle

One `Clock` instant is captured per request:

```text
displayStatus = EXPIRED
  when persistedStatus == PREVIEWED and now >= expiresAt
  otherwise persistedStatus

editable = persistedStatus == PREVIEWED and now < expiresAt
applicable = editable
  and applyMode == ALL_OR_NOTHING
  and selectedCandidateCount > 0
```

No other overdue status is derived. A persisted-status `PREVIEWED` filter can therefore contain a row whose badge is `EXPIRED`. History never invokes the expiry service and never saves, locks, increments a version, or initializes items. Existing detail GET continues its current persisted expiration normalization.

## Query design and performance

The repository uses two Criteria queries with a shared predicate builder:

1. one paginated flat constructor projection joined once to cinema;
2. one count query.

It never selects the preview entity or joins `ShowtimeSchedulePreview.items`. Integration coverage asserts two executed queries, zero collection fetches, stable ordering, boundary behavior, and visibility through a soft-deleted cinema. No cinema soft-delete predicate is applied because the preview is historical and has no soft-delete lifecycle.

## Admin browser contract

`/admin/showtime-schedules` renders `AdminAutoScheduleHistoryPage`. Canonical URL state contains the same seven filters plus `page`, `size`, and `sort`. Malformed values normalize to defaults. Filter, size, and sort changes reset the page; page-only changes preserve filters. An out-of-range page redirects to the final valid page.

Schedule dates are sent unchanged and are never converted through browser `Date`. Creation controls use `datetime-local`, label the administrator device timezone, and store normalized ISO instants in the URL. Reversed ranges show inline validation and suppress requests.

The hook strictly parses the `ApiResponse` and admin page envelope, preserves rows during refresh, rejects stale responses with monotonic request IDs, and loads all cinema option pages separately with `showDeleted=true`. Cinema-option failure does not replace history data.

The page supports initial loading/error, empty history, filtered empty, populated table, background refresh, retry, page sizes 10/20/50, and explicit detail navigation. It renders backend `displayStatus`, `editable`, and `applicable` directly and performs no retry, regeneration, apply, edit, or lifecycle mutation.

## Known limitations

- Creator filtering/display remains unavailable because previews only store a logical numeric `generatedBy` and the service has no public user selector.
- Cinema names are not snapshotted.
- Applied count relies on current all-or-nothing atomic apply semantics.
- Strategy filtering has no dedicated index and this work adds no migration.
- Stale `GENERATING` and `APPLYING` records retain those statuses.
- Cleanup jobs, exports, analytics, bulk deletion, retry/regeneration, and authoritative service-date exposure remain deferred.
