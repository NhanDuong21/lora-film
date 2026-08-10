# Authoritative Auto-Schedule Service Date

## Definition and persistence

For every generated candidate, the authoritative operating date is exactly
`OperatingWindow.serviceDate`. An after-midnight start in an overnight window remains owned by
the date on which that window opened. The date is not derived from `startTime`, a timezone,
opening-hour heuristics, or current cinema configuration.

The frozen schema has one deliberate domain-correctness exception:
`showtime_schedule_preview_items.service_date DATE NULL`. The column is nullable only because
existing rows cannot be reconstructed exactly. The generation persistence mapper rejects any
new candidate without a non-null originating service date. No other schema change or index is
part of this work.

The manual one-time migration is
`docs/database/mysql/migrations/20260722_add_showtime_schedule_preview_item_service_date.sql`.
Spring does not execute it: the service has no Flyway or Liquibase integration, and production
uses Hibernate schema validation. Deployment order is database migration and verification,
backend, then frontend.

The migration is intentionally one-time rather than idempotent. The operator must run this
pre-check and proceed only when it returns `0`:

```sql
SELECT COUNT(*) AS service_date_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'showtime_schedule_preview_items'
  AND column_name = 'service_date';
```

Before migration, record the legacy row count:

```sql
SELECT COUNT(*) AS preview_item_count_before
FROM showtime_schedule_preview_items;
```

After migration, verify the column and legacy-row state:

```sql
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'showtime_schedule_preview_items'
  AND column_name = 'service_date';

SELECT
    COUNT(*) AS preview_item_count_after,
    COALESCE(SUM(service_date IS NULL), 0) AS null_service_date_rows,
    COALESCE(SUM(service_date IS NOT NULL), 0) AS non_null_service_date_rows
FROM showtime_schedule_preview_items;
```

The before/after counts must match, `null_service_date_rows` must equal the original row count,
and `non_null_service_date_rows` must be zero. The emergency rollback is:

```sql
ALTER TABLE showtime_schedule_preview_items
    DROP COLUMN service_date;
```

Dropping the column after new writes begin loses authoritative metadata, so normal application
rollback leaves the nullable column in place.

## REST and legacy behavior

`ShowtimeSchedulePreviewItemResponse.serviceDate` is a nullable `LocalDate`, serialized as
`YYYY-MM-DD`. Legacy `BALANCED_V1`, `BALANCED_V1_S2`, and pre-migration `BALANCED_V1_S3` items
return null. They are never backfilled, regenerated, or rewritten during GET, replay, selection,
or apply.

The existing preview-detail `date` query retains its prior meaning: cinema-local calendar date
of `startTime`. The current frontend does not send that filter; it loads every item page and
performs authoritative service-date filtering locally. No backend service-date filter or index
is introduced. An index requires a later concrete query plus representative MySQL
`EXPLAIN ANALYZE` evidence.

Preview history is unchanged: summary-only, read-only, two queries, and zero item collection
fetches.

## Frontend behavior

Known items are grouped, filtered, headed, and assigned to timeline sections using the plain
`serviceDate` calendar string. The value is validated and formatted without JavaScript `Date`.
`preview.timezoneSnapshot` remains responsible only for instant-based clock rendering,
tooltips, and timeline positioning.

Unknown legacy items remain visible and filterable under `Không xác định ngày vận hành`. No
timeline row is invented for them; Timeline mode directs the administrator to the full List
view. The existing 08:00–24:00 axis is preserved, including clipping and the outside-range
notice for fully after-midnight items.

Selection conflicts remain global by auditorium and absolute `[startTime, occupancyEndTime)`
intervals, independent of service-date groups. Selected flags, quick selection, request
payloads, expected versions, scoring, ranking, weighted interval scheduling, locking, apply,
pricing, and idempotency are unchanged.
