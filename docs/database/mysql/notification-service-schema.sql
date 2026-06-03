CREATE TABLE `notification_templates` (
  `id` int PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Template ID',
  `template_code` varchar(100) UNIQUE NOT NULL COMMENT 'e.g., EMAIL_VERIFICATION, TICKET_CONFIRMATION',
  `title` varchar(255) NOT NULL COMMENT 'Tieu de email hoac tieu de thong bao push',
  `content` text NOT NULL COMMENT 'Noi dung mau co chua cac bien cho, e.g., Kính gui {name}, ma ve cua ban la {code}',
  `channel_type` varchar(30) NOT NULL COMMENT 'EMAIL, SMS, PUSH_NOTIFICATION',
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `notification_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Log ID',
  `template_code` varchar(100) COMMENT 'Co the null neu la thong bao tu do khong dung mau',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `recipient` varchar(150) NOT NULL COMMENT 'Dia chi nhan thuc te: email hoac so dien thoai',
  `channel_type` varchar(30) NOT NULL COMMENT 'EMAIL, SMS, PUSH',
  `actual_title` varchar(255) COMMENT 'Tieu de thuc te da truyen bien hoan thien',
  `actual_content` text NOT NULL COMMENT 'Noi dung thuc te da duoc truyen bien hoan thien de gui di',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED',
  `error_message` text COMMENT 'Luu loi tra ve tu các nha cung cap SMTP, Firebase, Twilio neu gui that bai',
  `sent_at` timestamp COMMENT 'Thoi gian thuc te tin nhan roi khoi he thong',
  `created_at` timestamp DEFAULT (now())
);

ALTER TABLE `notification_logs` ADD FOREIGN KEY (`template_code`) REFERENCES `notification_templates` (`template_code`);
