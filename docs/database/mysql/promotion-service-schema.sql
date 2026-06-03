CREATE TABLE `promotion_campaigns` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Campaign ID',
  `campaign_name` varchar(150) NOT NULL,
  `description` text,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `promotions` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Promotion/Voucher ID',
  `campaign_id` bigint NOT NULL COMMENT 'Foreign Key noi bo ket noi voi promotion_campaigns',
  `promotion_code` varchar(50) UNIQUE NOT NULL COMMENT 'Ma voucher khach hang nhap, e.g., LORAFILM2026',
  `description` text,
  `discount_type` varchar(20) NOT NULL COMMENT 'PERCENTAGE, FIXED_AMOUNT',
  `discount_value` decimal(10,2) NOT NULL COMMENT 'Gia tri giam, e.g., 10.00 cho percent hoac 20000.00 cho fixed',
  `max_discount_amount` decimal(10,2) COMMENT 'So tien giam toi da neu dung PERCENTAGE, Null neu dung FIXED_AMOUNT',
  `min_order_amount` decimal(10,2) DEFAULT 0 COMMENT 'Gia tri don hang toi thieu de duoc ap dung ma',
  `usage_limit` int NOT NULL COMMENT 'Tong so lan ma nay duoc phep su dung tren toan he thong',
  `used_count` int DEFAULT 0 COMMENT 'So lan ma nay da thuc te duoc dung, used_count <= usage_limit',
  `limit_per_user` int DEFAULT 1 COMMENT 'So lan toi da mot khach hang duoc dung ma nay',
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `promotion_usages` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `promotion_id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `booking_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang bookings.id cua Booking Service',
  `applied_at` timestamp DEFAULT (now())
);

ALTER TABLE `promotions` ADD FOREIGN KEY (`campaign_id`) REFERENCES `promotion_campaigns` (`id`) ON DELETE CASCADE;

ALTER TABLE `promotion_usages` ADD FOREIGN KEY (`promotion_id`) REFERENCES `promotions` (`id`) ON DELETE CASCADE;
