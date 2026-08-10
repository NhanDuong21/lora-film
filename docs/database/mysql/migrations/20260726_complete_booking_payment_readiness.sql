-- Booking production lifecycle/payment readiness.
-- Manual migration; no Redis state is persisted.

ALTER TABLE bookings
    ADD COLUMN showtime_public_id VARCHAR(36) NULL AFTER showtime_id,
    ADD COLUMN amount_locked_at DATETIME NULL AFTER expires_at,
    ADD INDEX idx_booking_showtime_public (showtime_public_id),
    ADD INDEX idx_booking_payment_ready (booking_status, amount_locked_at, expires_at);

UPDATE bookings b
JOIN booking_price_snapshots ps ON ps.booking_id = b.id
SET b.showtime_public_id =
    JSON_UNQUOTE(JSON_EXTRACT(ps.pricing_breakdown_json, '$.showtimePublicId'))
WHERE b.showtime_public_id IS NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(
      ps.pricing_breakdown_json, '$.showtimePublicId')) IS NOT NULL;

UPDATE bookings
SET amount_locked_at = COALESCE(confirmed_at, created_at)
WHERE amount_locked_at IS NULL
  AND booking_status IN ('CONFIRMED', 'COMPLETED', 'REFUNDED');

ALTER TABLE seat_reservations
    ADD COLUMN showtime_public_id VARCHAR(36) NULL AFTER showtime_id,
    ADD COLUMN seat_public_id VARCHAR(36) NULL AFTER seat_id,
    ADD INDEX idx_reservation_booking (booking_id),
    ADD INDEX idx_reservation_showtime_public_status
        (showtime_public_id, status, expires_at),
    ADD INDEX idx_reservation_seat_public (seat_public_id);

ALTER TABLE booking_tickets
    ADD COLUMN seat_public_id VARCHAR(36) NULL AFTER seat_id,
    ADD INDEX idx_ticket_seat_public (seat_public_id);

ALTER TABLE booking_idempotency_keys
    ADD COLUMN locked_until DATETIME NULL AFTER status,
    ADD COLUMN resource_public_id VARCHAR(36) NULL AFTER response_body,
    ADD COLUMN updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        AFTER created_at,
    DROP INDEX uk_idempotency_key,
    ADD CONSTRAINT uk_idempotency_scope
        UNIQUE (user_id, endpoint, idempotency_key),
    ADD INDEX idx_idempotency_status_expire (status, expires_at),
    ADD INDEX idx_idempotency_resource (resource_public_id);

ALTER TABLE booking_outbox_events
    ADD COLUMN aggregate_public_id VARCHAR(36) NULL AFTER aggregate_id,
    ADD INDEX idx_outbox_aggregate_public
        (aggregate_type, aggregate_public_id);

-- The JPA event identifiers are String(length=36), so keep the canonical
-- schema and migrated deployments on VARCHAR rather than CHAR.
ALTER TABLE booking_outbox_events
    MODIFY COLUMN event_id VARCHAR(36) NOT NULL;

ALTER TABLE booking_inbox_events
    MODIFY COLUMN event_id VARCHAR(36) NOT NULL;

ALTER TABLE booking_dead_letter_events
    MODIFY COLUMN event_id VARCHAR(36) NOT NULL;

ALTER TABLE booking_scheduler_locks
    ADD COLUMN created_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP AFTER id;

UPDATE booking_outbox_events o
JOIN bookings b ON o.aggregate_type = 'BOOKING'
               AND o.aggregate_id = b.id
SET o.aggregate_public_id = b.public_id
WHERE o.aggregate_public_id IS NULL;

CREATE OR REPLACE VIEW vw_booking_summary AS
SELECT
    b.id,
    b.public_id,
    b.booking_code,
    b.user_id,
    b.showtime_id,
    b.showtime_public_id,
    b.final_amount,
    b.currency,
    b.booking_status,
    b.payment_status,
    b.amount_locked_at,
    b.expires_at,
    b.created_at,
    (SELECT COUNT(*) FROM booking_tickets bt WHERE bt.booking_id = b.id) AS total_ticket,
    COALESCE((SELECT total_quantity FROM booking_food_orders fo
              WHERE fo.booking_id = b.id LIMIT 1), 0) AS total_food
FROM bookings b;

CREATE OR REPLACE VIEW vw_booking_admin AS
SELECT
    booking_code,
    user_id,
    showtime_id,
    showtime_public_id,
    booking_status,
    payment_status,
    ticket_amount,
    food_amount,
    promotion_discount,
    final_amount,
    currency,
    expires_at,
    amount_locked_at,
    created_at
FROM bookings;
