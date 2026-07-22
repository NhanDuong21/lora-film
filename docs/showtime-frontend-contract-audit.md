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
| POST | `/api/admin/showtime-schedules/generate-preview` | `GenerateShowtimeSchedulePreviewRequest` | `ShowtimeSchedulePreviewSummaryResponse` |
| GET | `/api/admin/showtime-schedules/{previewPublicId}` | `page`, `size`, and preview-item filters | `ShowtimeSchedulePreviewPageResponse` |
| PUT | `/api/admin/showtime-schedules/{previewPublicId}/items` | `UpdatePreviewItemSelectionsRequest` | `ShowtimeSchedulePreviewSummaryResponse` |
| POST | `/api/admin/showtime-schedules/{previewPublicId}/apply` | `ApplyShowtimeSchedulePreviewRequest` | `ApplyShowtimeSchedulePreviewResponse` |
| GET | `/api/admin/showtime-schedules/eligible-movies` | Optional `fromDate`, `toDate` | `List<EligibleMovieResponse>` |

`GET /api/admin/showtimes` is implemented by `AdminShowtimeQueryController`; it is not a missing contract.

## 2. Confirmed Backend Contract Gap

There is no root `GET /api/admin/showtime-schedules` controller method for preview history. `AdminShowtimeScheduleController` provides preview detail, generation, selection update, apply, and eligible-movie endpoints only. The frontend service currently declares `getPreviewHistory`, but no registered frontend page calls it and no backend endpoint serves it.

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
- Optimizer service-date ownership is a separate backend concept: it comes from the candidate's originating operating window and can differ from the candidate start's calendar date after midnight. `ShowtimeSchedulePreviewItemResponse` does not expose that authoritative service date, so it cannot be reconstructed reliably from `startTime` alone.

### Current preview frontend compliance

- `useAutoSchedulePreview` retains the preview summary, including `timezoneSnapshot`, but `AdminAutoSchedulePreviewPage` and `AutoScheduleTimeline` do not read `preview.timezoneSnapshot`.
- Preview date grouping and filtering construct `new Date(item.startTime)` and use `getFullYear`, `getMonth`, and `getDate`. Time labels and timeline positioning use `toLocaleTimeString`, `getHours`, and `getMinutes` without a `timeZone` option. The resulting grouping and rendering therefore use the browser's local timezone, not the authoritative snapshot.
- The frontend does not interpret authoritative service-date ownership. It groups by the browser-local calendar date of `startTime`, so an after-midnight candidate owned by the prior operating service date can appear under the next browser-local date.
- Manual-selection conflict hints and the page's re-selection helper compare `[startTime, endTime)`, where `endTime` is film end. They do not use the available `occupancyEndTime`, so cleaning-buffer conflicts can be missed in the UI. Backend selection and apply-time validation remain occupancy-authoritative.
- These are known frontend gaps. Phase S3 does not change frontend source and does not claim current UI compliance with the backend timezone or occupancy contracts.

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
| `/admin/showtime-schedules/create` | `AdminAutoScheduleCreatePage` | Implemented |
| `/admin/showtime-schedules/:id` | `AdminAutoSchedulePreviewPage` | Implemented; `id` carries `previewPublicId` |

There is no registered preview-history route corresponding to root `/admin/showtime-schedules`.
