-- Booking authoritative price snapshot uniqueness.
-- Preflight must confirm no duplicate booking_id values before this is run.

ALTER TABLE booking_price_snapshots
    ADD CONSTRAINT uk_price_snapshot_booking UNIQUE (booking_id);
