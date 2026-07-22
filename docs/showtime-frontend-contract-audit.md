# Showtime Frontend Contract Audit

## 1. Confirmed Contracts

| Method | Endpoint | Request Schema | Response Schema | Confirmed |
|---|---|---|---|---|
| POST | `/api/admin/showtimes` | `CreateShowtimeRequest` | `AdminShowtimeResponse` | Yes |
| PUT | `/api/admin/showtimes/{showtimePublicId}` | `UpdateShowtimeRequest` | `AdminShowtimeResponse` | Yes |
| PUT | `/api/admin/showtimes/{showtimePublicId}/status` | `UpdateShowtimeStatusRequest` | `AdminShowtimeResponse` | Yes |
| GET | `/api/admin/showtimes/{showtimePublicId}/status-history` | - | `List<ShowtimeStatusHistoryResponse>` | Yes |
| GET | `/api/admin/showtimes/{showtimeId}/prices` | - | `ShowtimePricesResponse` | Yes |
| PUT | `/api/admin/showtimes/{showtimeId}/prices` | `UpdateShowtimePricesRequest` | `ShowtimePricesResponse` | Yes |
| POST | `/api/admin/showtime-schedules/generate-preview` | `GenerateShowtimeSchedulePreviewRequest` | `ShowtimeSchedulePreviewSummaryResponse` | Yes |
| GET | `/api/admin/showtime-schedules/{previewPublicId}` | Query: page, size | `ShowtimeSchedulePreviewPageResponse` | Yes |
| PUT | `/api/admin/showtime-schedules/{previewPublicId}/items` | `UpdatePreviewItemSelectionsRequest` | `ShowtimeSchedulePreviewSummaryResponse` | Yes |
| POST | `/api/admin/showtime-schedules/{previewPublicId}/apply` | `ApplyShowtimeSchedulePreviewRequest` | `ApplyShowtimeSchedulePreviewResponse` | Yes |

## 2. Missing Contracts

1. **Admin Showtime List API**: There is no `GET /api/admin/showtimes` endpoint with filters for all statuses (DRAFT, OPEN_FOR_BOOKING, CANCELLED, etc.), cinema, auditorium, movie, date range. The `GET /api/showtimes` is customer-facing and only returns `OPEN_FOR_BOOKING`.
2. **Admin Auto-Schedule History API**: There is no `GET /api/admin/showtime-schedules` endpoint to list previously generated previews.

## 3. Identifier Mapping

- **Showtimes**: `showtimePublicId` (UUID string) is used in create response, update, status update, and history. The pricing endpoint uses `{showtimeId}` in path but the schema suggests it expects `String`, likely the `showtimePublicId`.
- **Movies**: `moviePublicId`
- **Movie Versions**: `movieVersionPublicId`
- **Cinemas**: `cinemaPublicId`
- **Auditoriums**: `auditoriumPublicId`
- **Previews**: `previewPublicId`

## 4. Datetime/Timezone Mapping

- `startTime` in requests (`CreateShowtimeRequest`) and responses is an `Instant`. The backend expects UTC format (e.g. `2026-07-20T19:30:00Z`).
- The `endTime` is not sent in `CreateShowtimeRequest`; the backend calculates it from movie duration only, so it is the film end.
- The auditorium occupancy interval is `[startTime, endTime + cleaningBuffer)`. Existing-showtime, cinema-closure, and auditorium-maintenance conflicts use this occupancy interval; operating hours use `[startTime, endTime)`.
- Movie release and end dates are interpreted as cinema-local calendar dates. Scheduling is start-based: a film may finish after the end-date boundary when it starts before the next local midnight.
- `CinemaSummary` includes a `timezone` (e.g., `Asia/Ho_Chi_Minh`). The frontend MUST use this timezone to display dates/times in the browser, rather than relying on the user's local timezone.

## 5. Auto-Schedule Generation Errors

- `AUTO_SCHEDULE_TOO_MANY_CANDIDATES` returns HTTP 422 when generation attempts candidate 10,001. Exactly 10,000 candidates are allowed.
- Known domain errors retain their own HTTP status and code. Unexpected generation failures return `AUTO_SCHEDULE_GENERATION_FAILED` with HTTP 500.

## 6. Route Proposal

- `/admin/showtimes` - Showtime list/calendar view
- `/admin/showtimes/create` - Manual create showtime
- `/admin/showtimes/:showtimePublicId` - Showtime details, pricing, history
- `/admin/showtime-schedules/create` - Wizard for auto schedule preview configuration
- `/admin/showtime-schedules/:previewPublicId` - Auto schedule preview summary and item selection

## 7. First Blocker

**Admin Showtime List API Missing**: The `/api/admin/showtimes` endpoint does not exist. We cannot build Phase 1 (Showtime Management Foundation) without a backend contract to retrieve the list of showtimes with all statuses.
