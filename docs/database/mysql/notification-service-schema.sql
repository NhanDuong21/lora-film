CREATE TABLE `notification_templates` (
  `id` INT PRIMARY KEY AUTO_INCREMENT
    COMMENT 'Primary Key - Template ID',

  `template_code` VARCHAR(100) UNIQUE NOT NULL
    COMMENT 'e.g., EMAIL_VERIFICATION, TICKET_CONFIRMATION',

  `title` VARCHAR(255) NOT NULL
    COMMENT 'Tieu de email hoac tieu de thong bao push',

  `content` TEXT NOT NULL
    COMMENT 'Noi dung mau co chua cac bien cho',

  `channel_type` VARCHAR(30) NOT NULL
    COMMENT 'EMAIL, SMS, PUSH_NOTIFICATION, IN_APP',

  `is_active` BOOLEAN NOT NULL DEFAULT TRUE,

  `version` INT NOT NULL DEFAULT 0
    COMMENT 'Optimistic locking version',

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  `updated_at` TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `notification_logs` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT
    COMMENT 'Primary Key - Log ID',

  `template_code` VARCHAR(100) NULL
    COMMENT 'Co the null neu la thong bao tu do khong dung mau',

  `event_id` VARCHAR(150) NULL
    COMMENT 'Idempotency key cho Kafka/event redelivery',

  `idempotency_key` VARCHAR(150) NULL
    COMMENT 'Idempotency key cho Internal REST retry',

  `user_id` BIGINT NOT NULL
    COMMENT 'Logical Ref sang users.account_id cua User Service',

  `recipient` VARCHAR(150) NULL
    COMMENT 'Email, phone number, device token. NULL chi duoc phep voi IN_APP',

  `channel_type` VARCHAR(30) NOT NULL
    COMMENT 'EMAIL, SMS, PUSH_NOTIFICATION, IN_APP',

  `provider_name` VARCHAR(100) NULL
    COMMENT 'SMTP, Firebase, Twilio',

  `provider_message_id` VARCHAR(255) NULL
    COMMENT 'Message ID tra ve tu provider',

  `request_source` VARCHAR(100) NULL
    COMMENT 'Service gui request',

  `reference_type` VARCHAR(50) NULL
    COMMENT 'BOOKING, PAYMENT, ORDER',

  `reference_id` VARCHAR(100) NULL
    COMMENT 'ID cua nghiep vu',

  `actual_title` VARCHAR(255) NULL
    COMMENT 'Tieu de thuc te sau khi render template',

  `actual_content` TEXT NOT NULL
    COMMENT 'Noi dung thuc te sau khi render template',

  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING, PROCESSING, SENT, FAILED, RETRYING, CANCELLED',

  `failure_code` VARCHAR(100) NULL
    COMMENT 'Ma loi phan loai retryable/non-retryable',

  `error_message` TEXT NULL
    COMMENT 'Chi tiet loi tra ve tu provider',

  `retry_count` INT NOT NULL DEFAULT 0,

  `max_retries` INT NOT NULL DEFAULT 3,

  `last_retry_at` TIMESTAMP NULL,

  `next_retry_at` TIMESTAMP NULL,

  `sent_at` TIMESTAMP NULL
    COMMENT 'Provider accepted send request',

  `delivered_at` TIMESTAMP NULL
    COMMENT 'Provider confirmed delivery',

  `is_read` BOOLEAN NOT NULL DEFAULT FALSE
    COMMENT 'Chi ap dung cho IN_APP',

  `read_at` TIMESTAMP NULL
    COMMENT 'Chi ap dung cho IN_APP',

  `version` INT NOT NULL DEFAULT 0
    COMMENT 'Optimistic locking version',

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  `updated_at` TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT `fk_notification_template`
    FOREIGN KEY (`template_code`)
    REFERENCES `notification_templates` (`template_code`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,

  CONSTRAINT `uk_notification_logs_event_id`
    UNIQUE (`event_id`),

  CONSTRAINT `uk_notification_logs_idempotency_key`
    UNIQUE (`idempotency_key`)
);

CREATE INDEX `idx_notification_retry`
ON `notification_logs`
(`status`, `next_retry_at`);

CREATE INDEX `idx_notification_provider`
ON `notification_logs`
(`provider_name`, `provider_message_id`);

CREATE INDEX `idx_notification_reference`
ON `notification_logs`
(`reference_type`, `reference_id`);

CREATE INDEX `idx_notification_reference_source`
ON `notification_logs`
(`request_source`, `reference_type`, `reference_id`);

CREATE INDEX `idx_notification_user_channel_status_created`
ON `notification_logs`
(`user_id`, `channel_type`, `status`, `created_at`);

CREATE INDEX `idx_notification_user_read`
ON `notification_logs`
(`user_id`, `is_read`, `created_at`);

CREATE INDEX `idx_notification_status_created`
ON `notification_logs`
(`status`, `created_at`);

CREATE INDEX `idx_notification_template_created`
ON `notification_logs`
(`template_code`, `created_at`);