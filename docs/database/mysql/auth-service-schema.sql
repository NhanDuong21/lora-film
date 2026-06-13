CREATE TABLE `accounts` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Logical Ref to User Service',
  `email` varchar(100) UNIQUE NOT NULL COMMENT 'Dùng làm tên đăng nhập chính',
  `password_hash` varchar(255) NOT NULL,
  `role_id` int NOT NULL,
  `is_active` int DEFAULT 1,
  `registration_completed` int DEFAULT 0,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `roles` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `role_name` varchar(50) UNIQUE NOT NULL COMMENT 'CUSTOMER, STAFF, ADMIN',
  `description` varchar(255)
);

CREATE TABLE `permissions` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `permission_code` varchar(100) UNIQUE NOT NULL,
  `description` varchar(255)
);

CREATE TABLE `roles_permissions` (
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`)
);

CREATE TABLE `refresh_tokens` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `token` varchar(255) UNIQUE NOT NULL,
  `expiry_date` timestamp NOT NULL,
  `is_revoked` boolean DEFAULT false,
  `created_at` timestamp DEFAULT (now())
);

CREATE TABLE `audit_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint,
  `action` varchar(100) NOT NULL,
  `ip_address` varchar(45),
  `user_agent` varchar(255),
  `created_at` timestamp DEFAULT (now())
);

ALTER TABLE `accounts` ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `roles_permissions` ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `roles_permissions` ADD FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE;

ALTER TABLE `refresh_tokens` ADD FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;
