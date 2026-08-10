# Showtime Pricing Migration Preflight

Run this checklist before
`20260723_add_showtime_price_policies.sql` and
`20260723_add_booking_price_snapshot_unique.sql`.

## Required deployment order

1. Stop policy/snapshot writers or place the affected admin operations in
   maintenance mode.
2. Back up the Movie and Booking databases.
3. Record the preflight queries below and retain their output with deployment
   evidence.
4. Run the Movie migration and its verification queries.
5. Run the Booking migration and its verification queries.
6. Deploy Movie Service, Booking Service, Payment Service, API Gateway, then the
   frontend.
7. Exercise manual create, Auto Schedule apply, open, Booking creation, and a
   payment-result callback.

Deploying the new Movie binary before the SQL migration is unsupported because
production uses `spring.jpa.hibernate.ddl-auto=validate`.

## Preflight queries

```sql
SELECT COUNT(*) AS showtime_price_rows FROM showtime_prices;
SELECT COUNT(*) AS showtime_rows FROM showtimes;
SELECT COUNT(*) AS cinema_rows FROM cinemas;
SELECT COUNT(*) AS seat_type_rows FROM seat_types;
SELECT COUNT(*) AS non_positive_showtime_prices
FROM showtime_prices
WHERE price <= 0;

SELECT COUNT(*) AS cinemas_without_timezone
FROM cinemas
WHERE timezone IS NULL OR TRIM(timezone) = '';

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('price_policies', 'price_policy_rules');

SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'showtime_prices'
  AND COLUMN_NAME IN (
    'seat_type_name_snapshot',
    'seat_type_code_snapshot',
    'pricing_source',
    'source_policy_id',
    'source_rule_id',
    'resolved_at',
    'resolution_timezone'
  );

SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'showtimes'
  AND COLUMN_NAME = 'version';

SELECT COUNT(*) AS duplicate_booking_snapshots
FROM (
  SELECT booking_id
  FROM booking_price_snapshots
  GROUP BY booking_id
  HAVING COUNT(*) > 1
) duplicates;
```

The target policy tables, snapshot columns, and Showtime version column must
not exist. The non-positive price, missing timezone, and Booking duplicate
counts must all be zero. Repair those rows before continuing; the migration
intentionally strengthens the price constraint and does not invent timezone
data.

## Post-migration verification

```sql
SELECT COUNT(*) AS legacy_rows
FROM showtime_prices
WHERE pricing_source = 'LEGACY';

SELECT COUNT(*) AS invalid_snapshot_rows
FROM showtime_prices
WHERE seat_type_name_snapshot IS NULL
   OR seat_type_code_snapshot IS NULL
   OR resolved_at IS NULL
   OR resolution_timezone IS NULL
   OR pricing_source IS NULL;

SELECT COUNT(*) AS non_positive_showtime_prices
FROM showtime_prices
WHERE price <= 0;

SELECT COUNT(*) AS policy_count FROM price_policies;
SELECT COUNT(*) AS active_legacy_policy_count
FROM price_policies
WHERE name = 'Legacy Default' AND status = 'ACTIVE' AND priority = 0;

SELECT COUNT(*) AS orphan_policy_rules
FROM price_policy_rules r
LEFT JOIN price_policies p ON p.id = r.policy_id
WHERE p.id IS NULL;
```

`legacy_rows` must equal the recorded preflight Showtime price row count.
`invalid_snapshot_rows`, `non_positive_showtime_prices`, and
`orphan_policy_rules` must be zero. There must be one active Legacy Default
policy per Cinema.

## Rollback boundary

Application rollback keeps the additive tables and columns and deploys the
previous binaries. A physical SQL rollback is allowed only before any new policy
or snapshot writes and after restoring the old Auto Schedule hardcoded behavior.
After writes begin, restore from backup instead of dropping provenance data.
