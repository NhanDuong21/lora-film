CREATE TABLE `bookings` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Booking ID',
  `booking_code` varchar(50) UNIQUE NOT NULL COMMENT 'Ma don hang hien thi cho khach hang, e.g., LORAFILM-123456',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `showtime_id` bigint NOT NULL COMMENT 'Logical Ref sang showtimes.id cua Movie Service',
  `total_amount` decimal(10,2) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT 'PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED',
  `expires_at` timestamp NOT NULL COMMENT 'Booking expiration time used for payment timeout',
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `tickets` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL COMMENT 'Logical Ref sang seats.id cua Movie Service',
  `price` decimal(10,2) NOT NULL COMMENT 'Gia snapshot tai thoi diem tao booking',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `seat_reservations` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `showtime_id` bigint NOT NULL COMMENT 'Logical Ref sang showtimes.id cua Movie Service',
  `seat_id` bigint NOT NULL COMMENT 'Logical Ref sang seats.id cua Movie Service',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `booking_id` bigint COMMENT 'Ref sang bookings.id khi reservation duoc convert',
  `status` varchar(20) NOT NULL DEFAULT 'HELD' COMMENT 'HELD, RELEASED, EXPIRED, CONVERTED',
  `expires_at` timestamp NOT NULL COMMENT 'Thoi gian het han giu ghe, thuong la created_at + 5 phut',
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX `idx_booking_expiration` ON `bookings` (`status`, `expires_at`);

CREATE INDEX `idx_seat_reservation_lookup` ON `seat_reservations` (`showtime_id`, `seat_id`, `status`);
CREATE INDEX `idx_seat_reservation_expiration` ON `seat_reservations` (`status`, `expires_at`);

ALTER TABLE `tickets` ADD FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE;
ALTER TABLE `seat_reservations` ADD FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE SET NULL;
