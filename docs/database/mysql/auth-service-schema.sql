CREATE TABLE `accounts` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Logical Ref to User Service',
  `email` varchar(100) UNIQUE NOT NULL COMMENT 'Dùng làm tên đăng nhập chính',
  `password_hash` varchar(255) NOT NULL,
  `role_id` int NOT NULL,
  `account_status` varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING, ACTIVE, SUSPENDED, BLOCKED',
  `version` int DEFAULT 0 COMMENT 'For optimistic locking',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `roles` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `role_name` varchar(50) UNIQUE NOT NULL COMMENT 'CUSTOMER, STAFF, ADMIN',
  `description` varchar(255),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `permissions` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `permission_code` varchar(100) UNIQUE NOT NULL,
  `description` varchar(255),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `roles_permissions` (
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`)
);

CREATE TABLE `refresh_tokens` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `token_hash` varchar(255) UNIQUE NOT NULL,
  `expiry_date` timestamp NOT NULL,
  `is_revoked` boolean DEFAULT false,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `audit_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint,
  `action` varchar(100) NOT NULL,
  `ip_address` varchar(45),
  `user_agent` varchar(255),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `account_providers` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `provider_name` varchar(50) NOT NULL COMMENT 'google, facebook, apple, github',
  `provider_account_id` varchar(255) NOT NULL COMMENT 'Provider specific user ID (e.g. sub in JWT)',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint,
  UNIQUE KEY `uk_provider_account` (`provider_name`, `provider_account_id`)
);

ALTER TABLE `accounts` ADD CONSTRAINT `fk_accounts_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT;

ALTER TABLE `roles_permissions` ADD CONSTRAINT `fk_roles_permissions_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `roles_permissions` ADD CONSTRAINT `fk_roles_permissions_permissions` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE;

ALTER TABLE `refresh_tokens` ADD CONSTRAINT `fk_refresh_tokens_accounts` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;

ALTER TABLE `account_providers` ADD CONSTRAINT `fk_account_providers_accounts` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;

CREATE INDEX `idx_accounts_email` ON `accounts` (`email`);
CREATE INDEX `idx_accounts_role_id` ON `accounts` (`role_id`);
CREATE INDEX `idx_refresh_tokens_account_id` ON `refresh_tokens` (`account_id`);
CREATE INDEX `idx_audit_logs_account_id` ON `audit_logs` (`account_id`);
CREATE INDEX `idx_audit_logs_action` ON `audit_logs` (`action`);
CREATE INDEX `idx_account_providers_account_id` ON `account_providers` (`account_id`);
