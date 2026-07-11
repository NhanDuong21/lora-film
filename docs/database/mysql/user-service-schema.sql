CREATE TABLE `users` (
  `account_id` bigint PRIMARY KEY COMMENT 'Shared Primary Key - Logical Ref từ accounts.id của Auth Service',
  `full_name` varchar(100) NOT NULL,
  `phone_number` varchar(15) UNIQUE NOT NULL,
  `gender` varchar(10) COMMENT 'MALE, FEMALE, OTHER',
  `birthday` date,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now()),
  `cccd` varchar(12) UNIQUE,
  `cccd_masked` varchar(20),
  `province_code` varchar(10),
  `province_name` varchar(100),
  `birth_year` int
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

ALTER TABLE `employee_profiles`
ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`account_id`) ON DELETE CASCADE;
