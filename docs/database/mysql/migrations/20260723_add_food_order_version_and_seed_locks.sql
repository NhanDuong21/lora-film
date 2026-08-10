-- Migration: Add version column to booking_food_orders, optimize booking expires index, and seed scheduler locks

-- 1. Optimizing booking expires index
ALTER TABLE bookings DROP INDEX idx_booking_expires;
ALTER TABLE bookings ADD INDEX idx_booking_status_expires(booking_status, expires_at);

-- 2. Add version column to booking_food_orders
ALTER TABLE booking_food_orders ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT 'Phiên bản bản ghi (Dùng cho Optimistic Locking / Khóa lạc quan)';

-- 3. Update scheduler lock seeds
DELETE FROM booking_scheduler_locks WHERE scheduler_name IN ('BOOKING_EXPIRE', 'OUTBOX_PUBLISHER', 'RETRY_SCHEDULER');
INSERT INTO booking_scheduler_locks (scheduler_name, status)
VALUES
    ('BookingExpirationScheduler', 'RELEASED'),
    ('OutboxEventPublisherScheduler', 'RELEASED'),
    ('RetryTaskScheduler', 'RELEASED'),
    ('ReservationExpirationScheduler', 'RELEASED')
ON DUPLICATE KEY UPDATE status = VALUES(status);
