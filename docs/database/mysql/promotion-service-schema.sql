CREATE TABLE `promotion_campaigns` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT
    COMMENT 'Primary Key - Campaign ID',

  `campaign_name` VARCHAR(150) NOT NULL,

  `description` TEXT,

  `start_date` TIMESTAMP NOT NULL,

  `end_date` TIMESTAMP NOT NULL,

  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  `updated_at` TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `promotions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT
    COMMENT 'Primary Key - Promotion/Voucher ID',

  `campaign_id` BIGINT NOT NULL
    COMMENT 'Foreign Key noi bo ket noi voi promotion_campaigns',

  `promotion_code` VARCHAR(50) NOT NULL UNIQUE
    COMMENT 'Ma voucher khach hang nhap, e.g., LORAFILM2026',

  `description` TEXT,

  `discount_type` VARCHAR(20) NOT NULL
    COMMENT 'PERCENTAGE, FIXED_AMOUNT',

  `discount_value` DECIMAL(10,2) NOT NULL
    COMMENT 'Gia tri giam',

  `max_discount_amount` DECIMAL(10,2)
    COMMENT 'Chi dung voi PERCENTAGE',

  `min_order_amount` DECIMAL(10,2)
    NOT NULL DEFAULT 0,

  `usage_limit` INT NOT NULL,

  `used_count` INT NOT NULL DEFAULT 0,

  `limit_per_user` INT NOT NULL DEFAULT 1,

  `start_date` TIMESTAMP NOT NULL,

  `end_date` TIMESTAMP NOT NULL,

  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,

  `version` INT NOT NULL DEFAULT 0,

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  `updated_at` TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `promotion_usages` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,

  `promotion_id` BIGINT NOT NULL,

  `user_id` BIGINT NOT NULL
    COMMENT 'Logical Ref sang users.account_id cua User Service',

  `booking_id` BIGINT NOT NULL UNIQUE
    COMMENT 'Logical Ref sang bookings.id cua Booking Service',

  `status` VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
    COMMENT 'RESERVED, APPLIED, REVERTED',

  `original_amount` DECIMAL(10,2) NOT NULL
    COMMENT 'Booking amount truoc discount tai thoi diem apply',

  `discount_amount` DECIMAL(10,2) NOT NULL
    COMMENT 'Discount snapshot tai thoi diem apply',

  `final_amount` DECIMAL(10,2) NOT NULL
    COMMENT 'Final amount sau discount tai thoi diem apply',

  `expires_at` TIMESTAMP NOT NULL
    COMMENT 'Snapshot booking expiry dung cho reconciliation',

  `reserved_at` TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
    COMMENT 'Thoi diem tao usage o trang thai RESERVED',

  `confirmed_at` TIMESTAMP NULL
    COMMENT 'Thoi diem chuyen sang APPLIED',

  `reverted_at` TIMESTAMP NULL
    COMMENT 'Thoi diem chuyen sang REVERTED',

  `revert_reason` VARCHAR(255) NULL
    COMMENT 'Ly do revert usage',

  `version` INT NOT NULL DEFAULT 0,

  `updated_at` TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE `promotions`
ADD CONSTRAINT `fk_promotions_campaign`
FOREIGN KEY (`campaign_id`)
REFERENCES `promotion_campaigns` (`id`)
ON DELETE RESTRICT
ON UPDATE RESTRICT;

ALTER TABLE `promotion_usages`
ADD CONSTRAINT `fk_promotion_usages_promotion`
FOREIGN KEY (`promotion_id`)
REFERENCES `promotions` (`id`)
ON DELETE RESTRICT
ON UPDATE RESTRICT;

CREATE INDEX `idx_promotion_usage_user_limit`
ON `promotion_usages`
(`promotion_id`, `user_id`, `status`);

CREATE INDEX `idx_promotion_usage_expiration`
ON `promotion_usages`
(`status`, `expires_at`);

CREATE INDEX `idx_promotion_usage_customer_history`
ON `promotion_usages`
(`user_id`, `status`, `reserved_at`);

CREATE INDEX `idx_promotion_usage_admin_list`
ON `promotion_usages`
(`status`, `reserved_at`);