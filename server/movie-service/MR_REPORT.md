# MR Description: [QA] Implement Movie Service Business Rule Test Matrix

##  Summary of Changes

* **Branch**: `test/movie-service-business-rules`
* **Target Module**: `server/movie-service`
* **Java Version**: Java 21 (`<java.version>21</java.version>`)
* **Goal**: Validate and audit Movie Service core business rules using automated unit & integration test suites.

---

##  Acceptance Criteria Verification

- [x] **Success Cases**: Tested happy path scenarios for all domain operations.
- [x] **Failure / Business Rule Cases**: Tested validation exceptions, error codes, and edge-case violations.
- [x] **Maven Build Compatibility**: All test suites compile and pass successfully via Maven using Java 21 (`mvn test`).
- [x] **Clear Business Scenario Test Names**: Standardized test descriptions with `@DisplayName` for readable test reports.
- [x] **Comprehensive Coverage**: Verified business rules across Service, Repository, and Security layers (not just controller happy path).
- [x] **Test Results Report**: QA test matrix and execution results included in this MR.

---

##  Business Rule Test Matrix (19 Scenarios)

| # | Business Rule Group | Main Test Class | Business Scenario & Rule Verified | Status |
|---|---|---|---|---|
| 1 | **Movie publish validation** | `MoviePublishValidationTest` | Publish requires active movie version & primary poster | `PASSED` |
| 2 | **Movie customer visibility** | `CustomerMovieServiceTest`, `CustomerMovieControllerTest` | Only `PUBLISHED` movies visible to customers (`DRAFT`/`ARCHIVED`/`INACTIVE` hidden) | `PASSED` |
| 3 | **Movie version/media validation** | `MovieVersionServiceTest`, `MovieMediaServiceTest` | Duplicate version formats & invalid media type/URL rejected | `PASSED` |
| 4 | **Cinema status visibility** | `CinemaServiceImplTest` | Customers only see `ACTIVE` cinemas; inactive/closed filtered | `PASSED` |
| 5 | **Cinema closure period** | `CinemaStatusClosureAutomationIntegrationTest` | Temporary closure period auto-updates status and restricts operations | `PASSED` |
| 6 | **Auditorium maintenance window** | `MaintenanceWindowIntegrationTest`, `AuditoriumServiceTest` | Maintenance windows block scheduling and auditorium usage | `PASSED` |
| 7 | **Seat duplicate code/position** | `BulkSeatValidationTest`, `SeatRepositoryTest` | Duplicate seat code or identical (row, col) position in auditorium rejected | `PASSED` |
| 8 | **Showtime overlap** | `ShowtimeValidationServiceImplTest` | Overlapping showtimes in the same auditorium rejected (including turnaround buffer) | `PASSED` |
| 9 | **Showtime outside release window** | `ShowtimeValidationServiceImplTest` | Showtime before movie release date or after end release date rejected | `PASSED` |
| 10 | **Showtime outside operating hours** | `ShowtimeValidationServiceImplTest` | Showtime starting before open time or ending after close time rejected | `PASSED` |
| 11 | **Showtime during cinema closure** | `ShowtimeValidationServiceImplTest` | Showtime scheduled during cinema closure window rejected | `PASSED` |
| 12 | **Showtime during auditorium maintenance** | `ShowtimeValidationServiceImplTest` | Showtime scheduled during auditorium maintenance window rejected | `PASSED` |
| 13 | **Showtime invalid status transition** | `ShowtimeStatusTransitionServiceImplTest` | Enforces status state machine (`DRAFT -> OPEN -> CLOSED -> FINISHED/CANCELLED`) | `PASSED` |
| 14 | **Showtime cancel reason/history** | `ShowtimeStatusHistoryIntegrationTest` | Showtime cancellation requires reason and records immutable status history | `PASSED` |
| 15 | **Showtime missing price** | `ShowtimePricingServiceImplTest` | Showtime publishing blocked if pricing missing for any active seat category | `PASSED` |
| 16 | **Negative price** | `ShowtimePricingServiceImplTest`, `AdminShowtimePricingControllerTest` | Ticket prices <= 0 rejected at validation and controller layer | `PASSED` |
| 17 | **Customer hidden draft/inactive/cancelled data** | `ShowtimeQueryServiceImplTest`, `CustomerSeatLayoutIntegrationTest` | Customer APIs filter out draft/cancelled showtimes and inactive auditoriums | `PASSED` |
| 18 | **Admin/customer security boundary** | `SecurityInterceptorTest` | Unauthenticated -> 401 Unauthorized; Customer role -> 403 Forbidden on Admin endpoints | `PASSED` |
| 19 | **Audit created_by/updated_by** | `AuditIntegrationTest` | JPA Auditing auto-populates `created_by`, `created_at`, `updated_by`, `updated_at` | `PASSED` |

---

##  Test Execution Results

```bash
$ env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
$ mvn test -f server/movie-service/pom.xml

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Results:
[INFO] 
[INFO] Tests run: 99, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 01:26 min
[INFO] Finished at: 2026-07-20T22:26:57+07:00
```

- **Total Test Cases Executed**: `99`
- **Failures**: `0`
- **Errors**: `0`
- **Skipped**: `0`
- **Build Result**: `SUCCESS`
