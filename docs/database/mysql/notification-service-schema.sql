CREATE TABLE `notification_templates` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `template_code` varchar(100) UNIQUE NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `channel_type` varchar(30) NOT NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `notification_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `template_code` varchar(100) NULL,
  `user_id` bigint NOT NULL,
  `recipient` varchar(150) NULL,
  `channel_type` varchar(30) NOT NULL,
  `actual_title` varchar(255),
  `actual_content` text NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `error_message` text,
  
  `event_id` varchar(150) UNIQUE NULL,
  `idempotency_key` varchar(150) UNIQUE NULL,
  
  `retry_count` int NOT NULL DEFAULT 0,
  `max_retries` int NOT NULL DEFAULT 3,
  `next_retry_at` timestamp NULL DEFAULT NULL,
  
  `provider_name` varchar(50) NULL,
  `provider_message_id` varchar(150) NULL,
  
  `business_reference` varchar(150) NULL,
  
  `is_read` boolean NOT NULL DEFAULT false,
  `read_at` timestamp NULL DEFAULT NULL,
  
  `version` int NOT NULL DEFAULT 0,
  
  `sent_at` timestamp NULL DEFAULT NULL,
  `delivered_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE `notification_logs` 
  ADD CONSTRAINT `fk_logs_template` 
  FOREIGN KEY (`template_code`) REFERENCES `notification_templates` (`template_code`) ON DELETE RESTRICT;

CREATE INDEX `idx_notification_user_channel_status` 
  ON `notification_logs` (`user_id`, `channel_type`, `status`);

CREATE INDEX `idx_notification_retry_worker` 
  ON `notification_logs` (`status`, `next_retry_at`);

CREATE INDEX `idx_notification_provider_tracking` 
  ON `notification_logs` (`provider_name`, `provider_message_id`);

CREATE INDEX `idx_notification_business_reference` 
  ON `notification_logs` (`business_reference`);

CREATE INDEX `idx_notification_status_created` 
  ON `notification_logs` (`status`, `created_at`);

CREATE INDEX `idx_notification_template_created` 
  ON `notification_logs` (`template_code`, `created_at`);