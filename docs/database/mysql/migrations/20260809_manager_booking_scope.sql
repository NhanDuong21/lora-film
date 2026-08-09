-- Bổ sung UUID rạp trực tiếp trên Booking để các API Manager luôn lọc ở DB.
-- Chạy sau khi movie_db và booking_db đã có dữ liệu rạp.

ALTER TABLE booking_db.bookings
    ADD COLUMN cinema_public_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL
        AFTER cinema_id,
    ADD INDEX idx_bookings_cinema_public_created (cinema_public_id, created_at);

UPDATE booking_db.bookings booking
JOIN movie_db.cinemas cinema ON cinema.id = booking.cinema_id
SET booking.cinema_public_id = LOWER(cinema.public_id)
WHERE booking.cinema_public_id IS NULL;
