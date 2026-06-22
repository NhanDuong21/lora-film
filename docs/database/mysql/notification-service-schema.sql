CREATE TABLE `notification_templates` (
  `id` int PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Template ID',
  `template_code` varchar(100) UNIQUE NOT NULL COMMENT 'e.g., EMAIL_VERIFICATION, TICKET_CONFIRMATION',
  `title` varchar(255) NOT NULL COMMENT 'Tieu de email hoac tieu de thong bao push',
  `content` text NOT NULL COMMENT 'Noi dung mau co chua cac bien cho, e.g., Kính gui {name}, ma ve cua ban la {code}',
  `channel_type` varchar(30) NOT NULL COMMENT 'EMAIL, SMS, PUSH_NOTIFICATION, IN_APP',
  `is_active` boolean DEFAULT true,
  `version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `notification_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Log ID',
  `template_code` varchar(100) NULL COMMENT 'Co the null neu la thong bao tu do khong dung mau',
  `event_id` varchar(150) UNIQUE NULL COMMENT 'Business idempotency key tu Kafka event de chong trung',
  `idempotency_key` varchar(150) NULL COMMENT 'Idempotency key cho internal caller timeout retry',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `recipient` varchar(150) NULL COMMENT 'EMAIL/SMS/PUSH bat buoc. IN_APP co the NULL neu da co user_id',
  `channel_type` varchar(30) NOT NULL COMMENT 'EMAIL, SMS, PUSH_NOTIFICATION, IN_APP',
  `provider` varchar(100) NULL COMMENT 'Ten nha cung cap: e.g., AWS_SES, TWILIO, FIREBASE',
  `provider_message_id` varchar(255) NULL COMMENT 'ID tin nhan tu phia Provider phan hoi de doi soat',
  `request_source` varchar(100) NULL COMMENT 'He thong yeu cau: e.g., booking-service, payment-service',
  `reference_type` varchar(50) NULL COMMENT 'Loai nghiep vu goc: e.g., BOOKING, PAYMENT',
  `reference_id` varchar(100) NULL COMMENT 'ID nghiep vu goc de trace log',
  `actual_title` varchar(255) NULL COMMENT 'Tieu de thuc te da truyen bien hoan thien',
  `actual_content` text NOT NULL COMMENT 'Noi dung thuc te da duoc truyen bien hoan thien de gui di',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, SENT, FAILED, RETRYING, CANCELLED',
  `failure_code` varchar(100) NULL COMMENT 'Ma phan loai loi mang tinh chat thong ke/he thong',
  `error_message` text NULL COMMENT 'Luu loi da duoc sanitize (Khong luu credential, token, OTP)',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT 'So lan da hanh dong thu lai',
  `max_retry` int NOT NULL DEFAULT 3 COMMENT 'So lan thu lai toi da',
  `last_retry_at` timestamp NULL COMMENT 'Thoi gian thuc hien retry gan nhat',
  `next_retry_at` timestamp NULL COMMENT 'Thoi gian schedule cho lan retry tiep theo',
  `sent_at` timestamp NULL COMMENT 'Thoi gian Provider chap nhan request gui',
  `delivered_at` timestamp NULL COMMENT 'Thoi gian Provider xac nhan da den tay nguoi dung',
  `is_read` boolean NOT NULL DEFAULT false COMMENT 'Trang thai doc cho thong bao IN_APP',
  `read_at` timestamp NULL COMMENT 'Thoi gian nguoi dung bam doc thong bao',
  `version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version de tranh xung dot giua cac worker',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

---
--- KHÓA NGOẠI (FOREIGN KEY CONSTRAINTS)
--- Khuyển nghị Sprint 2: Không hard delete template, chan hanh dong xoa neu da co log
---
ALTER TABLE `notification_logs` 
ADD CONSTRAINT `fk_notification_logs_template_code`
FOREIGN KEY (`template_code`) 
REFERENCES `notification_templates` (`template_code`)
ON UPDATE RESTRICT 
ON DELETE RESTRICT;

---
--- CÁC INDEX BỔ SUNG (REQUIRED INDEXES)
---
-- 1. Index cho Provider tracking & doi soat
CREATE INDEX `idx_notification_provider_message` 
ON `notification_logs` (`provider`, `provider_message_id`);

-- 2. Index cho Retry Worker quet cac thong bao can gui lai
CREATE INDEX `idx_notification_retry_worker` 
ON `notification_logs` (`status`, `next_retry_at`);

-- 3. Index cho Audit & Debug nghiep vu theo Booking/Payment
CREATE INDEX `idx_notification_reference` 
ON `notification_logs` (`reference_type`, `reference_id`);

-- 4. Index ho tro truy van Notification Center (In-App) cho User
CREATE INDEX `idx_notification_user_read` 
ON `notification_logs` (`user_id`, `is_read`, `created_at`);