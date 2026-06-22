CREATE TABLE `bookings` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Booking ID',
  `booking_code` varchar(50) UNIQUE NOT NULL COMMENT 'Ma don hang hien thi cho khach hang, e.g., LORAFILM-123456',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `showtime_id` bigint NOT NULL COMMENT 'Logical Ref sang showtimes.id cua Movie Service',
  `total_amount` decimal(10,2) NOT NULL,
  `status` varchar(30) DEFAULT 'PENDING_PAYMENT' COMMENT 'PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED',
  `expires_at` timestamp NOT NULL COMMENT 'Booking expiration time used for payment timeout',
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `tickets` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL COMMENT 'Logical Ref sang seats.id cua Movie Service',
  `price` decimal(10,2) NOT NULL COMMENT 'Gia thuc te cua ve sau khi ap dung khuyen mai (neu co)',
  `created_at` timestamp DEFAULT (now())
);

CREATE TABLE `seat_reservations` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `showtime_id` bigint NOT NULL COMMENT 'Logical Ref sang showtimes.id cua Movie Service',
  `seat_id` bigint NOT NULL COMMENT 'Logical Ref sang seats.id cua Movie Service',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `status` varchar(20) DEFAULT 'HELD' COMMENT 'HELD, RELEASED, EXPIRED, CONVERTED',
  `expires_at` timestamp NOT NULL COMMENT 'Thoi gian het han giu ghe, thuong la created_at + 5/10 phut',
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT (now())
);

CREATE INDEX `idx_seat_reservation_lookup` ON `seat_reservations` (`showtime_id`, `seat_id`, `status`);

ALTER TABLE `tickets` ADD FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE;
