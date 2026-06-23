CREATE TABLE `promotion_campaigns` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `campaign_name` varchar(150) NOT NULL,
  `description` text,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `promotions` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `promotion_code` varchar(50) UNIQUE NOT NULL,
  `description` text,
  `discount_type` varchar(20) NOT NULL,
  `discount_value` decimal(10,2) NOT NULL,
  `max_discount_amount` decimal(10,2),
  `min_order_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `usage_limit` int NOT NULL,
  `used_count` int NOT NULL DEFAULT 0,
  `limit_per_user` int NOT NULL DEFAULT 1,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `promotion_usages` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `promotion_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `booking_id` bigint UNIQUE NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'RESERVED',
  `discount_snapshot` decimal(10,2) NOT NULL,
  `reserved_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_at` timestamp NULL DEFAULT NULL,
  `reverted_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE `promotions` 
  ADD CONSTRAINT `fk_promotions_campaign` 
  FOREIGN KEY (`campaign_id`) REFERENCES `promotion_campaigns` (`id`) ON DELETE RESTRICT;

ALTER TABLE `promotion_usages` 
  ADD CONSTRAINT `fk_usages_promotion` 
  FOREIGN KEY (`promotion_id`) REFERENCES `promotions` (`id`) ON DELETE RESTRICT;

CREATE INDEX `idx_usage_user_status_created` 
  ON `promotion_usages` (`user_id`, `status`, `created_at`);

CREATE INDEX `idx_usage_promotion_status_created` 
  ON `promotion_usages` (`promotion_id`, `status`, `created_at`);

CREATE INDEX `idx_usage_status_reserved` 
  ON `promotion_usages` (`status`, `reserved_at`);