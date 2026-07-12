# Showtime Refactor Decisions

## 1. Service Splitting Strategy
- `ShowtimeServiceImpl` will be deprecated in favor of two new services: `ShowtimeQueryServiceImpl` and `ShowtimeBookingContextServiceImpl`.
- We will NOT keep the old `ShowtimeService` interface forever, but for this refactoring phase (Phase 1), we will follow the "Cách ưu tiên" (Preferred Way):
  - We will migrate `ShowtimeController` to use `ShowtimeQueryService`.
  - We will migrate `InternalShowtimeController` to use `ShowtimeBookingContextService`.
  - Then we will safely delete `ShowtimeService` and `ShowtimeServiceImpl` once we ensure tests pass and there are no lingering bean dependencies.

## 2. Validation Foundation
- The validation foundation will reside in the `com.lorafilm.movie.showtime.validation` package.
- `ShowtimeValidationContext` will wrap fully resolved entity instances (`Movie`, `MovieVersion`, `Cinema`, `Auditorium`, etc.) instead of HTTP DTOs, decoupling validation logic from presentation layers.
- The `ShowtimeValidationService` will provide atomic checks ensuring they follow the exact order specified in the design lock.

## 3. Contracts
- Existing customer visibility rules (only `OPEN_FOR_BOOKING` showtimes are shown) remain completely intact.
- JSON response payload structure and request mappings stay unmodified.
- Error codes for validation failures will strictly adhere to the prompt's provided list in English.
