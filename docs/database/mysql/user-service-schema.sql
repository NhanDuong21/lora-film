CREATE TABLE `users` (
  `account_id` bigint PRIMARY KEY COMMENT 'Shared Primary Key - Logical Ref từ accounts.id của Auth Service',
  `full_name` varchar(100) NOT NULL,
  `phone_number` varchar(15) UNIQUE NOT NULL,
  `gender` varchar(10) COMMENT 'MALE, FEMALE, OTHER',
  `birthday` date,
  `is_verified_phone` boolean DEFAULT false,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `employee_profiles` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint UNIQUE NOT NULL COMMENT 'Foreign Key nội bộ tới users.account_id',
  `employee_code` varchar(20) UNIQUE NOT NULL COMMENT 'Mã nhân viên, e.g., NV001',
  `hire_date` date NOT NULL,
  `work_status` varchar(30) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, LEAVE',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

ALTER TABLE `users` ADD FOREIGN KEY (`account_id`) REFERENCES `employee_profiles` (`user_id`) ON DELETE CASCADE;
