CREATE TABLE `payments` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Payment ID',
  `payment_transaction_code` varchar(100) UNIQUE NOT NULL COMMENT 'Ma giao dich noi bo tu sinh, e.g., PAY-LORAFILM-998877',
  `booking_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang bookings.id cua Booking Service',
  `amount` decimal(10,2) NOT NULL,
  `payment_method` varchar(30) NOT NULL COMMENT 'VNPAY, MOMO, CASH',
  `external_transaction_id` varchar(100) COMMENT 'Ma giao dich tra ve tu phia Cong thanh toan (VNPAY/MOMO ID)',
  `status` varchar(30) DEFAULT 'PENDING' COMMENT 'PENDING, SUCCESS, FAILED, REFUNDED',
  `raw_response_payload` text COMMENT 'Luu toan bo du lieu JSON/QueryString ma cong thanh toan tra ve de doi soat khi can',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `payment_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  `previous_status` varchar(30),
  `current_status` varchar(30) NOT NULL,
  `log_message` text COMMENT 'Chi tiet dien bien, e.g., Create payment link, Received Webhook success',
  `created_at` timestamp DEFAULT (now())
);

ALTER TABLE `payment_logs` ADD FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE CASCADE;
