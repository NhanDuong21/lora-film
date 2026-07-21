# Phase 0 Gap Register — Admin Movie Operations

## MOVIE-GAP-001
**Admin movie list DTO lacks tmdbId**

- **Layer**: Backend contract
- **Evidence**: `MovieDto` does not include `tmdbId` or `tmdbLastUpdated`.
- **Current behavior**: Frontend cannot display TMDB ID in the list or easily distinguish synced movies.
- **Expected capability**: Include `tmdbId` and `tmdbLastUpdated` in the `MovieDto`.
- **Impact**: Blocks advanced sync visibility in the UI.
- **Severity**: MEDIUM
- **Blocks**: Does not block Phase 1 core. Blocks advanced sync visibility in Phase 4.
- **Recommended resolution**: Add `tmdbId` to `MovieDto`.

## MOVIE-GAP-002
**Admin movie list DTO lacks relation counts**

- **Layer**: Backend contract
- **Evidence**: `MovieServiceImpl.getMovies` only returns base properties. It doesn't project active version count, media count, or showtime count.
- **Current behavior**: Frontend cannot show readiness state on the catalog row without fetching detail for each (N+1).
- **Expected capability**: Include `activeVersionCount`, `mediaCount`, and `showtimeCount` in `MovieDto`.
- **Impact**: Causes N+1 requests if frontend tries to display aggregate readiness status in list view.
- **Severity**: MEDIUM
- **Blocks**: Does not block Phase 1 core. Blocks aggregate readiness/count columns in Phase 1. Can be deferred to Phase 4 or a small backend enhancement phase.
- **Recommended resolution**: Add count fields to `MovieDto` and use a DTO projection query in repository.

## MOVIE-GAP-003
**Legacy frontend TMDB search/import service methods conflict with the confirmed auto-sync workflow**

- **Layer**: Frontend architecture
- **Evidence**: `adminMovieService.js` contains stubbed/manual-import methods (`searchTmdbSuggestions`, etc.) targeting endpoints that do not exist.
- **Current behavior**: Dead or incomplete frontend code suggests an obsolete manual TMDB search/import workflow.
- **Expected capability**: Admin movie operations must consume movies already synchronized into Movie Service as DRAFT.
- **Impact**: Creates architectural confusion and risks implementing the wrong UX.
- **Severity**: MEDIUM
- **Blocks**: Does not block Phase 1, Phase 2, or Phase 3. Must be handled during frontend cleanup/refactor.
- **Recommended resolution**: Remove, isolate, or clearly mark legacy TMDB search/import methods unless another verified use case requires them. (Do not implement `/api/admin/tmdb/search`).

## MOVIE-GAP-005
**TMDB Approve endpoint semantic mismatch**

- **Layer**: Backend contract
- **Evidence**: `POST /api/admin/tmdb/approve` fetches from TMDB and maps to a preview `MovieDto`, but does not save.
- **Current behavior**: The name implies approving a draft, but it actually generates a preview DTO.
- **Expected capability**: The draft lifecycle should only be managed via the movie status transition endpoint.
- **Impact**: Confusion in frontend implementation.
- **Severity**: MEDIUM
- **Blocks**: Does not block Draft Review & Lifecycle because movie approval is performed through `PUT /api/admin/movies/{publicId}/status`.
- **Risk**: Frontend developers may accidentally call the wrong endpoint due to misleading naming.
- **Recommended resolution**: Do not use this endpoint in the Draft Review workflow. Rename or deprecate later if it has no remaining use case.

## MOVIE-GAP-006
**Movie status can be changed through both metadata update and lifecycle endpoint**

- **Layer**: Backend contract & Frontend implementation
- **Evidence**: The generic `PUT /api/admin/movies/{publicId}` endpoint accepts a `status` field in `MovieRequest`. The backend validation `AdminMovieService.updateMovie` -> `validatePublishStatus` only checks for the presence of genres. It bypasses the strict `hasActiveVersion` and `hasPrimaryPoster` checks that exist in the proper lifecycle endpoint (`PUT /api/admin/movies/{publicId}/status`). Additionally, the frontend `MovieFormModal.jsx` includes a `status` dropdown and sends this field during edits.
- **Current behavior**: An admin can accidentally (or intentionally) bypass strict publication requirements by updating the status via the generic edit modal rather than the dedicated lifecycle approval flow.
- **Expected capability**: Status transitions should only happen through the strict lifecycle endpoint.
- **Impact**: Frontend or external client may bypass the intended lifecycle action flow, and business validation differs between the two paths.
- **Severity**: HIGH
- **Blocks**: Blocks safe rollout of Phase 3 (Draft Review).
- **Recommended resolution**: Remove the `status` dropdown from the frontend `MovieFormModal` (Phase 1/2). Later, remove `status` from `MovieRequest` in the backend.
