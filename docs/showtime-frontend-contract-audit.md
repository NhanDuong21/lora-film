# Showtime Frontend Contract Audit

This audit reflects the current controller, DTO, service, and frontend route source. Backend API contracts and browser routes are listed separately.

## 1. Confirmed Backend API Contracts

| Method | Endpoint | Request or query | Response data |
|---|---|---|---|
| GET | `/api/admin/showtimes` | Optional `cinemaSlug`, `movieSlug`, `status`, `date`, `batchId`, `source`; `page`, `size` | `PageResponse<AdminShowtimeResponse>` |
| GET | `/api/admin/showtimes/{showtimePublicId}` | - | `AdminShowtimeResponse` |
| POST | `/api/admin/showtimes` | `CreateShowtimeRequest` | `AdminShowtimeResponse` |
| PUT | `/api/admin/showtimes/{showtimePublicId}` | `UpdateShowtimeRequest` | `AdminShowtimeResponse` |
| PUT | `/api/admin/showtimes/{showtimePublicId}/status` | `UpdateShowtimeStatusRequest` | `AdminShowtimeResponse` |
| GET | `/api/admin/showtimes/{showtimePublicId}/status-history` | - | `List<ShowtimeStatusHistoryResponse>` |
| GET | `/api/admin/showtimes/{showtimeId}/prices` | `{showtimeId}` is semantically the Showtime public ID | `ShowtimePricesResponse` |
| PUT | `/api/admin/showtimes/{showtimeId}/prices` | `{showtimeId}` is semantically the Showtime public ID; `UpdateShowtimePricesRequest` | `ShowtimePricesResponse` |
| GET | `/api/admin/showtime-schedules` | Optional history filters; zero-based `page`, `size`, allowlisted `sort` | `PageResponse<AutoSchedulePreviewHistoryItemResponse>` |
| POST | `/api/admin/showtime-schedules/generate-preview` | `GenerateShowtimeSchedulePreviewRequest` | `ShowtimeSchedulePreviewSummaryResponse` |
| GET | `/api/admin/showtime-schedules/{previewPublicId}` | `page`, `size`, and preview-item filters | `ShowtimeSchedulePreviewPageResponse` |
| PUT | `/api/admin/showtime-schedules/{previewPublicId}/items` | `UpdatePreviewItemSelectionsRequest` | `ShowtimeSchedulePreviewSummaryResponse` |
| POST | `/api/admin/showtime-schedules/{previewPublicId}/apply` | `ApplyShowtimeSchedulePreviewRequest` | `ApplyShowtimeSchedulePreviewResponse` |
| GET | `/api/admin/showtime-schedules/eligible-movies` | Optional `fromDate`, `toDate` | `List<EligibleMovieResponse>` |

`GET /api/admin/showtimes` is implemented by `AdminShowtimeQueryController`; it is not a missing contract.

## 2. Preview History Contract

Root `GET /api/admin/showtime-schedules` is implemented for read-only preview history. It uses the admin-list `PageResponse` shape (`data`, `pageNo`, `pageSize`, `totalElements`, `totalPages`, `last`), while preview detail retains its existing item-page response shape.

The history endpoint accepts `cinemaPublicId`, persisted `status`, `strategyVersion`, schedule overlap bounds, `[createdFrom, createdTo)` instant bounds, `page`, `size`, and one allowlisted sort pair. It returns no preview items, internal IDs, actor IDs, idempotency data, fingerprint, or raw failure reason. `cinemaName` is the current cinema name; `timezoneSnapshot` remains the historical timezone snapshot.

History derives an overdue persisted `PREVIEWED` row to display status `EXPIRED` without saving it. The backend-provided `displayStatus`, `editable`, and `applicable` values are authoritative for the history UI. Detail, edit, expiration normalization, generation, and apply behavior are unchanged.

## 3. Identifier Mapping

- Showtimes use `showtimePublicId` in query, command, status, and history endpoints.
- The pricing controller names its path variable `{showtimeId}`, but `ShowtimePricingServiceImpl` passes that string to `ShowtimeRepository.findByPublicIdAndDeletedAtIsNull`; callers must supply the Showtime public ID, not the internal numeric ID.
- Movies use `moviePublicId`.
- Movie versions use `movieVersionPublicId`.
- Cinemas use `cinemaPublicId`.
- Auditoriums use `auditoriumPublicId`.
- Schedule previews use `previewPublicId`.

## 4. Datetime and Timezone Mapping

- `startTime` in `CreateShowtimeRequest` and Showtime responses is an `Instant`; API values use UTC timestamps such as `2026-07-20T19:30:00Z`.
- `endTime` is calculated by the backend from movie duration and represents film end.
- Candidate occupancy is `[startTime, endTime + cleaningBuffer)`. Existing-Showtime, cinema-closure, and maintenance conflicts use occupancy; operating-hour containment uses `[startTime, endTime)`.
- Movie release and end dates are cinema-local calendar dates. Release validation is start-based.
- `GET /api/admin/cinemas` returns `PageResponse<CinemaResponse>` and `CinemaResponse.timezone`; this is the value read by the manual and auto-schedule create pages through `selectedCinema.timezone`.
- `GET /api/admin/cinemas/{cinemaPublicId}` returns `CinemaDetailDto`, which inherits `timezone` from `CinemaDto`.
- Admin Showtime list/detail responses expose `AdminShowtimeResponse.CinemaSummary.timezone`.

### Backend authoritative timezone contract

- Preview generation returns `ShowtimeSchedulePreviewSummaryResponse.timezoneSnapshot`. Preview detail returns the same summary under `ShowtimeSchedulePreviewPageResponse.preview.timezoneSnapshot`. That snapshot, rather than the browser timezone, is the authoritative cinema timezone for preview clock rendering and cinema-local calendar grouping.
- Preview history returns the same `timezoneSnapshot` independently from the joined current `cinemaName`. Schedule bounds are plain cinema-local `LocalDate` values; history creation bounds and timestamps are instants.
- Optimizer service-date ownership is a separate backend concept: it comes from the candidate's originating operating window and can differ from the candidate start's calendar date after midnight. `ShowtimeSchedulePreviewItemResponse` does not expose that authoritative service date, so it cannot be reconstructed reliably from `startTime` alone.

### Current preview frontend compliance

- `useAutoSchedulePreview` retains the preview summary, and the preview page and timeline now use `preview.timezoneSnapshot` explicitly through `Intl.DateTimeFormat`. Time labels, cinema-local calendar grouping, date filtering, and timeline positioning are therefore independent of the administrator's browser timezone. A malformed timezone is shown visibly and falls back deterministically to UTC.
- The timeline retains its existing `08:00–24:00` visual contract. Positions and midnight clipping use cinema-local time parts; candidates entirely outside that visual window are counted and directed to the complete table view instead of being drawn misleadingly at `08:00`.
- The frontend still does not interpret authoritative service-date ownership. It groups by the cinema-local calendar date of `startTime`, so an after-midnight candidate owned by the prior operating service date can appear under the next cinema-local calendar date. Cinema-local calendar grouping is not operating service-date grouping.
- Manual-selection conflict hints, disabled states, final local guards, and the explicit quick re-selection helper use `[startTime, occupancyEndTime)` with half-open adjacency. They compare against all selected items in the same auditorium, not only the visible filtered group. Missing occupancy data is never replaced by `endTime`.
- The quick action is an explicit earliest-start greedy non-overlap helper and is not equivalent to the `BALANCED_V1_S3` weighted interval selection. It can replace the backend-selected flags only after an administrator invokes it.
- Frontend checks remain assistance only. Backend selection versioning and apply-time revalidation remain authoritative.

## 5. Phase S3 Auto-Schedule Contract

- New previews use `BALANCED_V1_S3`; `BALANCED_V1` and `BALANCED_V1_S2` remain immutable replay versions.
- The candidate universe contains unique `(auditoriumId, movieVersionId, startTime)` slots whose film end fits inside an operating window. Cleaning may finish after closing.
- Film-end-after-close slots and duplicate keys are not materialized or persisted. Release and operational conflicts remain persisted `REJECTED` items.
- `totalCandidateCount` is the fit-only unique universe and equals persisted item count. `validCandidateCount + rejectedCandidateCount = totalCandidateCount`.
- Exactly 10,000 candidates are accepted. Discovery of unique key 10,001 stops before candidate materialization and returns `AUTO_SCHEDULE_TOO_MANY_CANDIDATES` with HTTP 422.
- Default selected flags maximize total score with deterministic weighted interval scheduling over occupancy intervals. Global `rankingPosition` remains the S2 score-first display order and is not optimizer decision order.
- Legacy previews are fingerprinted using their stored supported strategy version. Same-request replay returns them unchanged; changed-request reuse returns `IDEMPOTENCY_KEY_REUSED`; replays never regenerate legacy items.
- The REST response envelope and frontend request/response DTO shapes are unchanged by S3.

## 6. Implemented Frontend Routes

These routes are registered under the admin layout by `client/src/features/scheduling/admin/routes.jsx`; they are implemented routes, not proposals.

| Browser route | Component | Status |
|---|---|---|
| `/admin/showtimes` | `AdminShowtimePage` | Implemented |
| `/admin/showtimes/create` | `AdminShowtimeCreatePage` | Implemented |
| `/admin/showtimes/:id` | `AdminShowtimeDetailPage` | Implemented; `id` carries `showtimePublicId` |
| `/admin/showtime-schedules` | `AdminAutoScheduleHistoryPage` | Implemented; URL-backed filters and pagination |
| `/admin/showtime-schedules/create` | `AdminAutoScheduleCreatePage` | Implemented |
| `/admin/showtime-schedules/:id` | `AdminAutoSchedulePreviewPage` | Implemented; `id` carries `previewPublicId` |
