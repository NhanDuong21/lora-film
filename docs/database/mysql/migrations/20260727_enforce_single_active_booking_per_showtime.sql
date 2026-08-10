-- Enforce one PENDING_PAYMENT Booking per customer and Showtime.
-- MySQL 8+ manual migration.
--
-- Existing-data policy:
--   * expired PENDING_PAYMENT rows are moved to EXPIRED;
--   * when duplicate live rows already exist, the earliest row is retained;
--   * later duplicate rows are CANCELLED and their HELD seats are RELEASED.
--
-- Run this migration after 20260726_complete_booking_payment_readiness.sql.

START TRANSACTION;

INSERT INTO booking_status_histories (
    booking_id,
    from_status,
    to_status,
    reason,
    source,
    changed_by,
    created_at
)
SELECT
    b.id,
    'PENDING_PAYMENT',
    'EXPIRED',
    'Deadline elapsed before single-active-booking migration',
    'DATABASE_MIGRATION',
    '20260727_single_active_booking',
    CURRENT_TIMESTAMP
FROM bookings b
WHERE b.booking_status = 'PENDING_PAYMENT'
  AND b.is_deleted = FALSE
  AND b.expires_at <= CURRENT_TIMESTAMP;

UPDATE seat_reservations sr
JOIN bookings b ON b.id = sr.booking_id
SET sr.status = 'EXPIRED',
    sr.expired_reason = 'Owning Booking expired during single-active-booking migration',
    sr.version = sr.version + 1,
    sr.updated_at = CURRENT_TIMESTAMP
WHERE b.booking_status = 'PENDING_PAYMENT'
  AND b.is_deleted = FALSE
  AND b.expires_at <= CURRENT_TIMESTAMP
  AND sr.status = 'HELD';

UPDATE booking_food_orders fo
JOIN bookings b ON b.id = fo.booking_id
SET fo.status = 'CANCELLED',
    fo.version = fo.version + 1,
    fo.updated_at = CURRENT_TIMESTAMP
WHERE b.booking_status = 'PENDING_PAYMENT'
  AND b.is_deleted = FALSE
  AND b.expires_at <= CURRENT_TIMESTAMP
  AND fo.status = 'PENDING';

UPDATE bookings
SET booking_status = 'EXPIRED',
    expired_at = COALESCE(expired_at, CURRENT_TIMESTAMP),
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = '20260727_single_active_booking'
WHERE booking_status = 'PENDING_PAYMENT'
  AND is_deleted = FALSE
  AND expires_at <= CURRENT_TIMESTAMP;

CREATE TEMPORARY TABLE duplicate_active_bookings (
    booking_id BIGINT PRIMARY KEY
);

INSERT INTO duplicate_active_bookings (booking_id)
SELECT ranked.id
FROM (
    SELECT
        b.id,
        ROW_NUMBER() OVER (
            PARTITION BY b.user_id, b.showtime_id
            ORDER BY b.created_at ASC, b.id ASC
        ) AS active_rank
    FROM bookings b
    WHERE b.booking_status = 'PENDING_PAYMENT'
      AND b.is_deleted = FALSE
      AND b.expires_at > CURRENT_TIMESTAMP
) ranked
WHERE ranked.active_rank > 1;

INSERT INTO booking_status_histories (
    booking_id,
    from_status,
    to_status,
    reason,
    source,
    changed_by,
    created_at
)
SELECT
    d.booking_id,
    'PENDING_PAYMENT',
    'CANCELLED',
    'Duplicate active Booking removed; earliest order retained',
    'DATABASE_MIGRATION',
    '20260727_single_active_booking',
    CURRENT_TIMESTAMP
FROM duplicate_active_bookings d;

UPDATE seat_reservations sr
JOIN duplicate_active_bookings d ON d.booking_id = sr.booking_id
SET sr.status = 'RELEASED',
    sr.expired_reason = 'Duplicate active Booking cancelled during migration',
    sr.version = sr.version + 1,
    sr.updated_at = CURRENT_TIMESTAMP
WHERE sr.status = 'HELD';

UPDATE booking_food_orders fo
JOIN duplicate_active_bookings d ON d.booking_id = fo.booking_id
SET fo.status = 'CANCELLED',
    fo.version = fo.version + 1,
    fo.updated_at = CURRENT_TIMESTAMP
WHERE fo.status = 'PENDING';

UPDATE bookings b
JOIN duplicate_active_bookings d ON d.booking_id = b.id
SET b.booking_status = 'CANCELLED',
    b.cancelled_at = COALESCE(b.cancelled_at, CURRENT_TIMESTAMP),
    b.cancel_reason_code = 'DUPLICATE_ACTIVE_BOOKING',
    b.cancel_reason_detail = 'Migration retained the earliest active Booking for this Showtime',
    b.version = b.version + 1,
    b.updated_at = CURRENT_TIMESTAMP,
    b.updated_by = '20260727_single_active_booking';

DROP TEMPORARY TABLE duplicate_active_bookings;

COMMIT;

ALTER TABLE bookings
    ADD COLUMN active_customer_showtime_key VARCHAR(100)
        GENERATED ALWAYS AS (
            IF(
                booking_status = 'PENDING_PAYMENT' AND is_deleted = FALSE,
                CONCAT(user_id, '_', showtime_id),
                NULL
            )
        ) STORED
        COMMENT 'Database guard: one active pending Booking per customer and Showtime'
        AFTER is_deleted,
    ADD CONSTRAINT uk_active_customer_showtime_booking
        UNIQUE (active_customer_showtime_key);
