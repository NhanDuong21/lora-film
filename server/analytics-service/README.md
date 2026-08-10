# Analytics Service

Production-oriented business intelligence for LoraFilm. The service owns no booking,
movie, promotion, or customer master data. It consumes immutable Kafka snapshots,
stores facts, calculates decision-support snapshots on a schedule, and exposes
read APIs plus controlled alert/recommendation workflow actions.

## Architecture

```text
HTTP Controller
  -> Application Service
    -> Domain Service
      -> CRUD Repository
        -> analytics_db

Kafka Consumer
  -> Event Ingestion Application Service
    -> Validation + Fact Ingestion Domain Service
      -> Fact Repository

Scheduler
  -> KPI Pipeline Application Service
    -> independent stateless calculators
```

The ordered calculation pipeline is:

```text
Daily -> Cinema -> Movie -> Promotion -> Customer Segment -> Data Quality
      -> Forecast + Backtest -> Health Score -> Anomaly Detection
      -> Insight + Root Cause -> Recommendation -> Alert
```

Architecture constraints are executable in `CleanArchitectureTest`:

- controllers cannot depend on domain, entity, or repository packages;
- application services cannot depend on entity or repository packages;
- repositories contain no `@Query`, native SQL, or KPI calculation;
- entities contain persistence state only;
- every KPI formula lives in a service-layer calculator.

## Canonical formulas

All ratios are stored as fractions (`0.125000` means `12.5%`).

```text
Net Revenue              = Gross Revenue - Discount - Refund
Average Booking Value    = Net Revenue / Booking Count
Refund Rate              = Refund Booking Count / Booking Count
Promotion ROI            = Generated Revenue / Discount Cost
Occupancy                 = Sold Seats / Available Seats
Customer Lifetime Value  = Total Spending / Customer Count
```

Division by zero returns zero. Money uses `BigDecimal` and scale 2. Ratios use
scale 6 with `HALF_UP`.

## Kafka input

| Topic | Owner | Fact |
|---|---|---|
| `payment-success.v1` | payment-service | successful booking/payment snapshot |
| `payment-refunded.v1` | payment-service | refund snapshot |
| `booking.booking-cancelled.v1` | booking-service | cancellation snapshot |

Delivery is at-least-once. `processed_analytics_events.event_id` and a database
unique constraint provide business-level idempotency. Invalid payloads are not
retried; retryable infrastructure failures use exponential backoff and then go to
`<source-topic>.dlq`.

Analytics does not call another service or connect to another service database.
Missing optional enrichment (`userPublicId`, `membershipTier`, `cinemaName`,
`promotionPublicId`, `availableSeats`) lowers `dataCompleteness`; dependent KPI
values remain zero instead of being inferred from another database.

The current payment event v1 fields remain supported. Producers may add the
optional enrichment fields without breaking v1.

## Scheduling

The completed-day schedule runs at `01:10 Asia/Ho_Chi_Minh` and recalculates the
previous three business days so late events are incorporated. A second stale-aware
job recalculates today every minute only when a new source event has arrived.
Calculators are idempotent upserts keyed by dimension and date. An Admin-only,
audited asynchronous rebuild API supports recovery/backfill without changing facts.

Key configuration:

```properties
analytics.scheduler.cron=0 10 1 * * *
analytics.scheduler.lookback-days=3
analytics.scheduler.today-delay-ms=60000
analytics.forecast.horizon-days=7
analytics.zone-id=Asia/Ho_Chi_Minh
```

## APIs

All APIs require Admin, Manager, Accountant, `PERM_VIEW_FINANCE`, or
`DASHBOARD_VIEW`.

| Method | Path |
|---|---|
| GET | `/api/analytics/dashboard` |
| GET | `/api/analytics/cinemas` |
| PATCH | `/api/analytics/alerts/{id}/acknowledge` |
| PATCH | `/api/analytics/recommendations/{id}/status` |
| POST | `/api/analytics/jobs/rebuild` (Admin/`ANALYTICS_REBUILD`) |
| GET | `/api/analytics/jobs` (Admin/`ANALYTICS_REBUILD`) |
| GET | `/api/analytics/movies/**` (legacy-compatible report contract) |

`GET /api/analytics/dashboard` accepts the optional `cinemaKey` query parameter.
Without it, the response has system-wide KPIs. With it, revenue, bookings,
refunds, occupancy, movies, promotions, and cinema-linked insights are scoped to
that cinema; system-wide forecasts and health scores are intentionally omitted.

OpenAPI is available at `/swagger-ui.html`; health and Prometheus metrics are
available at `/actuator/health` and `/actuator/prometheus`.

## Local run

```bash
copy src\main\resources\application.example.properties src\main\resources\application.properties
mvn test
mvn spring-boot:run
```

Start MySQL, Kafka, and Eureka first, then apply
`docs/database/mysql/analytics-service-schema.sql` manually. This service does
not use Flyway. Hibernate runs with `ddl-auto=validate`, so the application
validates the manually managed schema and never silently changes tables.

## Frontend

The complete admin surface is `/admin/analytics`. It is lazy-loaded and uses the
same auth, gateway client, layout, permission guard, and Tailwind design system as
the rest of the project. Its Modern Minimal flow answers four questions in order:
what happened, why it happened, what will happen, and what action should be taken.
Authorized managers can acknowledge alerts and track recommendations without
directly mutating source-service business data.
