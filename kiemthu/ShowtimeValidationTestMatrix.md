# Showtime Validation Test Matrix

## Movie & Version Rules (ValidateMovie)
| Scenario | Condition | Expected Result | Error Code |
| :--- | :--- | :--- | :--- |
| Valid | Movie is NOW_SHOWING, Version is ACTIVE, Version belongs to Movie, within release window | Pass | N/A |
| Valid | Movie is UPCOMING, Version is ACTIVE, Version belongs to Movie, within release window | Pass | N/A |
| Invalid | Movie is DRAFT | Exception | MOVIE_NOT_AVAILABLE_FOR_SCHEDULING |
| Invalid | Movie is ENDED | Exception | MOVIE_NOT_AVAILABLE_FOR_SCHEDULING |
| Invalid | Movie is INACTIVE | Exception | MOVIE_NOT_AVAILABLE_FOR_SCHEDULING |
| Invalid | Version is INACTIVE | Exception | MOVIE_VERSION_NOT_ACTIVE |
| Invalid | Version belongs to a different Movie ID | Exception | MOVIE_VERSION_NOT_BELONG_TO_MOVIE |
| Invalid | Start time < Movie Release Date (if set) | Exception | SHOWTIME_OUTSIDE_RELEASE_WINDOW |
| Invalid | Start time > Movie End Date + 1 day (if set) | Exception | SHOWTIME_OUTSIDE_RELEASE_WINDOW |

## Cinema & Auditorium Rules (ValidateCinemaAndAuditorium)
| Scenario | Condition | Expected Result | Error Code |
| :--- | :--- | :--- | :--- |
| Valid | Cinema is ACTIVE, Auditorium is ACTIVE, Auditorium belongs to Cinema | Pass | N/A |
| Invalid | Cinema is DRAFT / INACTIVE | Exception | CINEMA_NOT_ACTIVE |
| Invalid | Auditorium is INACTIVE | Exception | AUDITORIUM_NOT_ACTIVE |
| Invalid | Auditorium belongs to a different Cinema ID | Exception | AUDITORIUM_NOT_BELONG_TO_CINEMA |

## Time & Duration Rules (ValidateTimeAndDuration)
| Scenario | Condition | Expected Result | Error Code |
| :--- | :--- | :--- | :--- |
| Valid | Duration >= 30 mins, Valid Timezone, Inside Op Hours | Pass | N/A |
| Invalid | Duration < 30 mins | Exception | INVALID_MOVIE_DURATION |
| Invalid | Invalid Cinema Timezone string | Exception | INVALID_CINEMA_TIMEZONE |
| Invalid | Operating Hours: isClosed = true for that DayOfWeek | Exception | SHOWTIME_OUTSIDE_OPERATING_HOURS |
| Invalid | Start time before Open Time | Exception | SHOWTIME_OUTSIDE_OPERATING_HOURS |
| Invalid | End time after Close Time | Exception | SHOWTIME_OUTSIDE_OPERATING_HOURS |
| Valid | Fallback Open/Close (08:00 - 23:59) if no OpHour config | Pass | N/A |

## Overlaps Rules (ValidateOverlaps)
| Scenario | Condition | Expected Result | Error Code |
| :--- | :--- | :--- | :--- |
| Valid | No overlaps with closure, maintenance, or showtimes | Pass | N/A |
| Invalid | Overlaps with Cinema Closure Period (ACTIVE) | Exception | SHOWTIME_OVERLAPS_CINEMA_CLOSURE |
| Invalid | Overlaps with Auditorium Maintenance Window (ACTIVE) | Exception | SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE |
| Invalid | Overlaps with another Showtime | Exception | SHOWTIME_OVERLAP |
| Valid | Overlaps with the SAME Showtime (when excludeShowtimeId is provided) | Pass | N/A |
