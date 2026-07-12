# Showtime Refactor Audit

## Existing endpoints
| Endpoint | Controller | Service method | Repository/specification | DTO | Visibility rule |
|---|---|---|---|---|---|
| `GET /api/showtimes` | `ShowtimeController` | `getShowtimes` | `ShowtimeRepository` with `ShowtimeSpecification` | `PageResponse<ShowtimeDto>` | Only `OPEN_FOR_BOOKING` and not deleted |
| `GET /api/showtimes/{showtimePublicId}` | `ShowtimeController` | `getShowtimeByPublicId` | `ShowtimeRepository` | `ShowtimeDto` | Only `OPEN_FOR_BOOKING` and not deleted |
| `GET /api/showtimes/{showtimePublicId}/seat-layout` | `ShowtimeController` | `getSeatLayout` | `ShowtimeRepository`, `SeatService`, `ShowtimePriceRepository`, `ShowtimeBlockedSeatRepository` | `SeatLayoutDto` | Only `OPEN_FOR_BOOKING` and not deleted |
| `POST /internal/showtimes/{showtimeId}/booking-context` | `InternalShowtimeController` | `getBookingContext` | `ShowtimeRepository`, `SeatService`, `ShowtimePriceRepository` | `BookingContextResponse` | Only `OPEN_FOR_BOOKING` and not deleted, validates seats belong to auditorium, inactive seats, missing price |

## Existing responsibilities
| Responsibility | Current class | Target class |
|---|---|---|
| Customer search (list showtimes) | `ShowtimeServiceImpl` | `ShowtimeQueryServiceImpl` |
| Customer detail (get showtime by ID) | `ShowtimeServiceImpl` | `ShowtimeQueryServiceImpl` |
| Customer seat layout | `ShowtimeServiceImpl` | `ShowtimeQueryServiceImpl` |
| Internal booking context | `ShowtimeServiceImpl` | `ShowtimeBookingContextServiceImpl` |

## Existing call sites
| Caller | Method | Compatibility risk |
|---|---|---|
| `ShowtimeController` | `getShowtimes`, `getShowtimeByPublicId`, `getSeatLayout` | Low - switch dependency to `ShowtimeQueryService` as contract is unchanged |
| `InternalShowtimeController` | `getBookingContext` | Low - switch dependency to `ShowtimeBookingContextService` as contract is unchanged |
| `InternalShowtimeControllerTest` | `getBookingContext` | Needs mock update |
| `ShowtimeServiceImplTest` | Various | Needs to be split into `ShowtimeQueryServiceImplTest` and `ShowtimeBookingContextServiceImplTest` |

## Existing tests
| Test class | Covered behavior | Missing behavior |
|---|---|---|
| `ShowtimeServiceImplTest` | Context validation, seat validation (duplicate, not found, inactive, different auditorium), missing price, not open status | Might miss explicit mapping tests for some edge cases, timezone checking edge cases |
| `InternalShowtimeControllerTest` | Security (missing token, invalid token), empty seat list | N/A |
| (Missing Customer Controller Test?) | Not found explicitly in the same directory but behavior must be protected | Ensure we add or keep regression tests. |

## Risks
- **Contract risk**: Very low if we strictly move logic.
- **Transaction risk**: `ShowtimeServiceImpl` currently doesn't use explicit `@Transactional`. Moving might keep it the same, but we should be aware.
- **Lazy loading risk**: OSIV dependency exists if no `@Transactional(readOnly = true)`.
- **Security risk**: Keep unchanged.
- **Mapping risk**: `ShowtimeMapper` continues to be used.
- **Timezone risk**: Retain the existing `ZoneId.of(showtime.getCinema().getTimezone())`.
