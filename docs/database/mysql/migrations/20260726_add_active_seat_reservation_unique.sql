-- Add generated column and unique constraint to prevent double-booking of seats
ALTER TABLE seat_reservations 
ADD COLUMN active_unique_key VARCHAR(255) 
GENERATED ALWAYS AS (IF(status IN ('HELD', 'BOOKED'), CONCAT(showtime_id, '_', seat_id), NULL)) STORED;

ALTER TABLE seat_reservations 
ADD CONSTRAINT uk_active_seat_reservation UNIQUE (active_unique_key);
