# Showtime Validation Design

## Overview
This document outlines the design for the reusable showtime scheduling validation foundation, ensuring consistency across manual, automated, and bulk scheduling flows.

## 1. Validation Context
`ShowtimeValidationContext` will be constructed by resolving necessary entities upfront. It will contain:
- `Movie`
- `MovieVersion`
- `Cinema`
- `Auditorium`
- `Instant startTime`
- `Instant endTime`
- `Integer cleaningBufferMinutes`
- `Long excludedShowtimeId` (Optional, used during updates)

## 2. Validation Order
1. Movie exists
2. Movie status valid (UPCOMING, NOW_SHOWING)
3. MovieVersion exists
4. MovieVersion ACTIVE
5. MovieVersion belongs to Movie
6. Cinema exists
7. Cinema ACTIVE
8. Cinema timezone valid
9. Auditorium exists
10. Auditorium ACTIVE
11. Auditorium belongs to Cinema
12. Duration > 0
13. endTime calculation (`startTime + durationMinutes`)
14. Cleaning buffer valid (from Auditorium)
15. Release window valid
16. Operating hours valid
17. Cinema closure overlap valid
18. Auditorium maintenance overlap valid
19. Showtime overlap valid

## 3. Data Sources
- **Duration**: Extracted exclusively from `Movie.durationMinutes`. End time must be calculated internally and not accepted from client requests.
- **Cleaning Buffer**: Extracted exclusively from `Auditorium.cleaningBufferMinutes`. Must be non-null and non-negative.

## 4. Timezone Handling
- Timezone is derived from `Cinema.timezone`.
- Validation must aggressively reject null, blank, or invalid timezones without falling back to the server's default.
- Storage in DB remains as `Instant`. Conversions to local dates and times happen on-the-fly using the Cinema's timezone during validation.

## 5. Temporal Validations
### Release Window
- Local start date must be `>= movie.releaseDate`.
- Local end date must be `<= movie.endDate` (if present).

### Operating Hours
- Supports 1-7 day index matching.
- Capable of validating overnight schedules correctly by evaluating the effective timespan across midnight.
- Reject if the day is configured as closed (`isClosed = true`).

### Closure & Maintenance Overlap
- Overlap evaluated using half-open intervals `[start, end)`.
- Validations will only check active closures and maintenance periods.

### Showtime Overlap
- Candidate interval encompasses `[startTime, endTime + cleaningBufferMinutes)`.
- Exclusion of self (`excludedShowtimeId`) and ignored statuses (`CANCELLED`, soft deletes) implemented safely.

## 6. Limitations on Concurrency
Current validation foundation prevents logical overlap in sequential requests.
Concurrent scheduling race protection will be finalized in the Admin Command phase.
