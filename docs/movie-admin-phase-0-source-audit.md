# Phase 0 Source Audit — Admin Movie Operations

## 1. Executive Summary
This document provides a comprehensive source code audit of the Movie Service backend and frontend to prepare for the Admin Movie Operations refactoring. It traces the TMDB synchronization flow, identifies the backend aggregate creation rules, audits the movie lifecycle, and evaluates the security and identifier semantics. The backend correctly auto-syncs movies as DRAFT, which confirms the Draft Review workflow.

## 2. Repository/Module Map
- **Backend Module**: `server/movie-service/`
  - Integration: `com.lorafilm.movie.integration.tmdb` (TMDB synchronization logic)
  - Core Business: `com.lorafilm.movie.movie` (Movie core CRUD and domain logic)
  - Entities: `Movie`, `MovieTranslation`, `Genre`, `Person`, `ProductionCompany`, `MovieCredit`, `MovieMedia`, `MovieVersion`, `TmdbSyncState`
- **Frontend Module**: `client/`
  - Target Feature: `client/src/features/catalog/admin/` (Routes, Hooks, Services, Pages)

## 3. TMDB Sync Source Trace
**Trigger mechanisms**:
1. **Manual Endpoint**: `POST /api/admin/tmdb/sync/{tmdbId}` calls `TmdbImportService.importMovieById`.
2. **Bulk Endpoint**: `POST /api/admin/tmdb/sync/bulk/start` starts a background thread running `TmdbImportService.runBulkSync()`.
3. **Reset Bulk Sync**: `POST /api/admin/tmdb/sync/bulk/reset` resets the cursor to "0" and triggers a new bulk sync.
4. **Schedulers** (`TmdbSyncScheduler.java`):
   - Bulk Sync (`@Scheduled` based on `fixedDelayString`, defaults to 1 hour).
   - Daily Latest (`cron` at 01:00 AM).
   - Daily Updated (`cron` at 02:00 AM).

**Sync execution flow**:
1. `TmdbImportService` fetches data using `TmdbClient` (which calls a Node.js TMDB worker).
2. Uses `TmdbMovieMapper` to extract titles, slugs, overviews, and maps fields.
3. Automatically sets `status = MovieStatus.DRAFT`.
4. Saves the `Movie` entity.
5. Saves associated relations.
6. Sync state is tracked in the `tmdb_sync_state` table with cursor-based pagination.
7. Supports updating existing movies gracefully.

## 4. Aggregate Creation Matrix
| Aggregate | Created Automatically | Updated Automatically | Runtime Verified | Notes |
|---|---|---|---|---|
| `Movie` | AUTO-CREATED | AUTO-UPDATED | NO | Status defaults to `DRAFT`. Slugs are uniquely generated. |
| `MovieTranslation` | AUTO-CREATED | AUTO-UPDATED | NO | Database schema locale convention needs verification (e.g., `vi-VN` vs `vi`). Falls back to `vi-VN`. |
| `Genre` / `MovieGenre` | AUTO-CREATED | AUTO-UPDATED | NO | Re-associates on update. |
| `Person` / `MovieCredit` | AUTO-CREATED | AUTO-UPDATED | NO | `MovieCredit` relations are replaced during movie resync. Existing `Person` master records are reused/upserted based on ID/Name. |
| `ProductionCompany` | AUTO-CREATED | AUTO-UPDATED | NO | `MovieProductionCompany` relations are replaced. Existing `ProductionCompany` master records are reused/upserted. |
| `MovieMedia` | AUTO-CREATED | AUTO-UPDATED | NO | Old media is deleted and new media is inserted during resync. |
| `MovieVersion` | CONDITIONALLY CREATED | NOT UPDATED | NO | Only created if it does not already exist (Default 2D version). |

## 5. Movie Lifecycle Source Audit
- **Primary Endpoint**: `PUT /api/admin/movies/{publicId}/status` (via `MovieServiceImpl.updateMovieStatus`)
- **Lifecycle Rules**:
  - Requires `ROLE_ADMIN`.
  - Enforces `validatePublishConditions` if moving to published statuses (UPCOMING, NOW_SHOWING, ENDED):
    - Must have at least 1 Active Version (`movieVersionRepository.existsActiveVersion`).
    - Must have at least 1 Primary Poster (`movieMediaRepository.existsPrimaryPoster`).
- **Conflict Discovered**:
  - The generic update endpoint `PUT /api/admin/movies/{publicId}` via `AdminMovieService.updateMovie()` also accepts a `status` field in `MovieRequest`.
  - It runs `validatePublishStatus(movieId, newStatus)` which **only checks for the presence of at least 1 genre**. It DOES NOT check for active versions or primary posters, effectively bypassing the strict `validatePublishConditions` of the main lifecycle endpoint.
  - The frontend `MovieFormModal.jsx` currently sends the `status` field in the payload when editing a movie.

## 6. Identifier Audit
- The source code in `MovieVersionController.java` and `MovieMediaController.java` uses the parameter names `{movieId}`, `{versionId}`, and `{mediaId}` typed as `String`.
- The corresponding services (`MovieVersionServiceImpl`, etc.) use `findByPublicIdAndDeletedAtIsNull` (and fallback to slug) for these parameters.
- **Conclusion**: There is no actual identifier mismatch at the backend layer. The endpoints correctly expect `publicId`, but they are named confusingly in the path parameters (e.g. `movieId` instead of `moviePublicId`). The frontend currently passes `publicId`, which is correct.

## 7. Security Audit
- Endpoint `AdminMovieController`, `AdminTmdbController`, `TmdbAdminController` methods are protected with `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.
- Frontend calls include authorization headers.

## 8. Source Conflicts & Legacy Code
- **Legacy Frontend Methods**: `adminMovieService.js` contains stubbed methods like `searchTmdbSuggestions` and `getTmdbMovieBundle`. These point to non-existent backend endpoints (`/api/import/search/suggestions`). They are legacy remnants of a manual TMDB search/import workflow that conflicts with the confirmed DRAFT auto-sync workflow.
- **Misnamed Approve Endpoint**: `POST /api/admin/tmdb/approve` uses `TmdbService.approveTmdbMovie`, which fetches from TMDB and maps to `MovieDto` but *does not save* it. It functions as a mapping preview. It is not the approval mechanism for the DRAFT lifecycle.

## 9. Verified Conclusions
- Backend automatically handles full aggregate creation during TMDB sync, producing DRAFT movies.
- Admin does not need to manually fill DRAFT metadata from scratch, but rather review.
- The `status` field in generic movie update bypasses the intended strict lifecycle validation.

## 10. Unverified Items
- Runtime behavior could not be fully verified because the backend application was not started.
