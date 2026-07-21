# Phase 0 Contract Matrix — Admin Movie Operations

## 1. Endpoint Inventory

| Endpoint | Method | Purpose | Verified in Source |
|---|---|---|---|
| `/api/admin/movies` | GET | List admin movies (paginated, filtered) | YES |
| `/api/admin/movies` | POST | Create movie | YES |
| `/api/admin/movies/{publicId}` | GET | View movie detail (includes genres, credits, companies) | YES |
| `/api/admin/movies/{publicId}` | PUT | Update core movie metadata | YES |
| `/api/admin/movies/{publicId}` | DELETE | Delete movie (soft delete) | YES |
| `/api/admin/movies/{publicId}/status` | PUT | Transition movie status | YES |
| `/api/admin/movies/{publicId}/genres` | POST/PUT | Assign/Append genres | YES |
| `/api/admin/movies/{publicId}/credits` | POST/PUT | Assign/Append credits | YES |
| `/api/admin/movies/{publicId}/production-companies` | POST/PUT | Assign/Append production companies | YES |
| `/api/admin/movies/{movieId}/versions` | GET/POST | View/Create movie versions | YES |
| `/api/admin/movie-versions/{versionId}` | PUT/DELETE | Update/Delete movie versions | YES |
| `/api/admin/movies/{movieId}/media` | GET/POST | View/Create movie media | YES |
| `/api/admin/movie-media/{mediaId}` | PUT/DELETE | Update/Delete movie media | YES |
| `/api/admin/tmdb/sync/{tmdbId}` | POST | Re-sync one movie | YES |
| `/api/admin/tmdb/sync/bulk/start` | POST | Start bulk sync | YES |
| `/api/admin/tmdb/sync/bulk/reset` | POST | Reset bulk sync | YES |
| `/api/admin/tmdb/approve` | POST | Preview movie from TMDB (Not used for Draft approval) | YES |

## 2. Use-Case Contract Matrix

| Use case | Current FE | Endpoint | Needed Response Fields | Source verified | Status | Notes |
|---|---|---|---|---|---|---|
| List admin movies | `getMovies` | `GET /api/admin/movies` | `publicId`, `title`, `status`, `releaseDate` | YES | READY | - |
| Filter by status | `getMovies` | `GET /api/admin/movies?status={status}` | same | YES | READY | - |
| Pagination | `getMovies` | `GET /api/admin/movies?page=X&size=Y` | same | YES | READY | - |
| View movie detail | `getMovieById` | `GET /api/admin/movies/{publicId}` | all metadata + relations | YES | READY | - |
| View genres | `getMovieById` | `GET /api/admin/movies/{publicId}` | `genres` | YES | READY | Data provided in detail DTO |
| View credits | `getMovieById` | `GET /api/admin/movies/{publicId}` | `credits`, `directors`, `actors`, etc. | YES | READY | Data provided in detail DTO |
| View companies | `getMovieById` | `GET /api/admin/movies/{publicId}` | `productionCompanies` | YES | READY | Data provided in detail DTO |
| Assign genres | `assignGenres` | `PUT /api/admin/movies/{publicId}/genres` | - | YES | READY | - |
| Assign credits | `assignCredits` | `PUT /api/admin/movies/{publicId}/credits` | - | YES | READY | - |
| Assign companies | `assignProductionCompanies`| `PUT /api/admin/movies/{publicId}/production-companies` | - | YES | READY | - |
| View versions | `getMovieVersions` | `GET /api/admin/movies/{movieId}/versions` | list of version DTOs | YES | READY | Path param is named `movieId` but accepts `publicId`. |
| Update metadata | `updateMovie` | `PUT /api/admin/movies/{publicId}` | `publicId` | YES | HAS_GAP | FE currently sends `status` field, bypassing full publish validation. (See MOVIE-GAP-006) |
| Transition status | `updateMovieStatus`| `PUT /api/admin/movies/{publicId}/status`| `publicId`, `status` | YES | READY | This is the required endpoint for lifecycle changes. |
| Re-sync movie | N/A (Stubbed)| `POST /api/admin/tmdb/sync/{tmdbId}` | - | YES | READY | FE relies on legacy manual-import stub. |
| Read bulk sync | - | - | - | NO | MISSING_CONTRACT | No endpoint found to check sync state. |

## 3. Identifier Audit Matrix

| Endpoint | Parameter name | Actual expected identifier type | Actual queried field in DB | FE currently sends | Verified Mismatch |
|---|---|---|---|---|---|
| `/api/admin/movies/{movieId}/versions` | `movieId` | String | `public_id` or `slug` | `publicId` | NO (Just poor param naming) |
| `/api/admin/movies/{movieId}/media` | `movieId` | String | `public_id` or `slug` | `publicId` | NO |
| `/api/admin/movie-versions/{versionId}` | `versionId` | String | `public_id` | `publicId` of version | NO |
| `/api/admin/movie-media/{mediaId}` | `mediaId` | String | `public_id` | `publicId` of media | NO |

## 4. DTO Field Matrix

| Field needed for UX | List DTO | Detail DTO | Database | Conclusion |
|---|---|---|---|---|
| `publicId` | YES | YES | YES | Present |
| `tmdbId` | NO | NO (Missing) | YES | Missing in DTOs (Phase 4 gap) |
| `tmdbLastUpdated` | NO | NO (Missing) | YES | Missing in DTOs (Phase 4 gap) |
| `title`, `slug`, `status`, `releaseDate`| YES | YES | YES | Present |
| `activeVersionCount`| NO | NO | NO | Missing. Needed for list readiness. |
| `mediaCount` | NO | NO | NO | Missing. Needed for list readiness. |
| `showtimeCount` | NO | NO | NO | Missing. Needed for list readiness. |
