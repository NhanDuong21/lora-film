-- Align existing Booking databases with the canonical schema and the
-- Booking.cinemaPublicId JPA mapping. Safe for nullable existing values.
USE booking_db;

ALTER TABLE bookings
    MODIFY COLUMN cinema_public_id VARCHAR(36)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NULL
        COMMENT 'UUID rạp dùng để khóa phạm vi vận hành của Manager';
