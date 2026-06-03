CREATE TABLE `membership_tiers` (
  `id` int PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Tier ID',
  `tier_name` varchar(50) UNIQUE NOT NULL COMMENT 'SILVER, GOLD, DIAMOND',
  `min_points` int NOT NULL COMMENT 'So diem toi thieu de dat duoc hang nay, e.g., 0, 200, 500',
  `earning_rate` decimal(5,2) NOT NULL DEFAULT 0.05 COMMENT 'Ty le tich diem, e.g., hang Vang duoc tich 7% gia tri don hang',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `user_scores` (
  `user_id` bigint PRIMARY KEY COMMENT 'Shared Primary Key - Logical Ref sang users.account_id cua User Service',
  `current_points` int NOT NULL DEFAULT 0 COMMENT 'So diem kha dung hien tai de doi qua',
  `accumulated_points` int NOT NULL DEFAULT 0 COMMENT 'Tong diem da tich luy trong doi de xet hang thanh vien',
  `current_tier_id` int NOT NULL DEFAULT 1,
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `score_history` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `booking_id` bigint COMMENT 'Nullable neu la doi qua, Logical Ref sang bookings.id cua Booking Service',
  `point_change` int NOT NULL COMMENT 'Gia tri diem bien dong, e.g., +15 hoac -50',
  `transaction_type` varchar(30) NOT NULL COMMENT 'EARN_BY_BOOKING, SPEND_FOR_REWARD, EXPIRED',
  `description` text COMMENT 'Chi tiet su kien, e.g., Tich diem tu don hang LORAFILM-123',
  `created_at` timestamp DEFAULT (now())
);

ALTER TABLE `user_scores` ADD FOREIGN KEY (`current_tier_id`) REFERENCES `membership_tiers` (`id`);

ALTER TABLE `score_history` ADD FOREIGN KEY (`user_id`) REFERENCES `user_scores` (`user_id`) ON DELETE CASCADE;
