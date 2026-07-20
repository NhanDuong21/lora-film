# Phase 0 Frontend Architecture Map — Admin Movie Operations

## 1. Routes and Pages

| Route | Page File | Component Tree | Purpose |
|---|---|---|---|
| `/admin/movies` | `AdminMoviePage.jsx` | `MovieTable.jsx`, `MovieFormModal.jsx` | Orchestrates the movie catalog list, filters, search, and the create/edit modal. |
| `/admin/movies/:moviePublicId` | `AdminMovieDetailPage.jsx` | `MovieOverviewTab`, `MovieVersionTab`, `MovieMediaTab`, `MovieGenreTab`, `MovieCreditTab`, `MovieCompanyTab` | Dedicated workspace for reviewing movie details, relations, and statuses. |

## 2. Component and Data Flow Mapping

### AdminMoviePage (List View)
- **Hook**: `useAdminMovies` handles fetching list, pagination, filtering, and deletion.
- **Service**: `adminMovieService.getMovies` calls `GET /api/admin/movies`.
- **State Management**: React local state (`useState`) handles pagination (`currentPage`, `pageSize`), filters (`statusFilter`, `searchTerm`), and modal visibility (`isFormOpen`).
- **Response Unwrapping**: Returns basic fields (title, status, date) but lacks TMDB sync status or readiness counts.
- **Dead/Legacy Code Issue**: The create/edit flow is heavily prominent, suggesting manual movie creation. This conflicts with the DRAFT auto-sync workflow.

### MovieFormModal (Create/Edit Mutation)
- **Action**: Handles creating a new movie or editing basic metadata.
- **Service**: `adminMovieService.createMovie` (POST) or `updateMovie` (PUT).
- **Status Field Handling**: In Edit mode, this modal explicitly provides a `Select` dropdown for `status`, and sends the `status` field in the payload to `PUT /api/admin/movies/{publicId}`. This bypasses the lifecycle endpoint.

### AdminMovieDetailPage (Detail Workspace)
- **Hook**: `useAdminMovieDetail` handles fetching the detail DTO.
- **Service**: `adminMovieService.getMovieById` calls `GET /api/admin/movies/{publicId}`.
- **Identifiers**: Passes `moviePublicId` from the URL param.
- **Loading/Error States**: Wrapped in `<AsyncState>` component for unified loading/error handling.
- **Tabs**:
  - `MovieOverviewTab`: Read-only view of metadata.
  - `MovieVersionTab`: Manages versions. Passes `publicId` to version endpoints.
  - `MovieMediaTab`: Manages images/trailers. Passes `publicId`.

## 3. Dead Code & Legacy Methods
- `adminMovieService.js` contains `searchTmdbSuggestions`, `getTmdbMovieBundle`, `getTmdbMovieImages`, `getLatestTop20`.
- These methods are entirely stubbed, targeting non-existent endpoints (e.g., `/api/import/search/suggestions`).
- They represent an obsolete manual TMDB import flow and should be isolated or removed.

## 4. Proposed Refactor Boundaries (Phase 1)
- **AdminMoviePage**: Refactor to surface DRAFT review status. Lower the prominence of the "Create Manual" button. Hide unsupported count columns until the backend implements them. Avoid N+1 requests by only using available fields.
- **MovieFormModal**: Remove or disable the `status` dropdown from the generic update form to force users to use a proper lifecycle approval action (Phase 3).
