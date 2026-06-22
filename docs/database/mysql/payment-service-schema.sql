CREATE TABLE `payments` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Payment ID',
  `payment_transaction_code` varchar(100) UNIQUE NOT NULL COMMENT 'Ma giao dich noi bo tu sinh, e.g., PAY-LORAFILM-998877',
  `booking_id` bigint NOT NULL COMMENT 'Logical Ref sang bookings.id cua Booking Service',
  `amount` decimal(10,2) NOT NULL,
  `payment_method` varchar(30) NOT NULL COMMENT 'MOCK, CASH, VNPAY, MOMO',
  `external_transaction_id` varchar(100) UNIQUE COMMENT 'Ma giao dich tra ve tu phia Cong thanh toan (VNPAY/MOMO ID)',
  `provider_session_id` varchar(150),
  `payment_url` text,
  `status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED, EXPIRED, REFUNDED',
  `expires_at` timestamp NOT NULL,
  `raw_response_payload` text COMMENT 'Luu toan bo JSON/QueryString de doi soat. MUST SANITIZE: Khong luu PII, Card Number, CVV, OTP, Private Key, Token',
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_payments_booking_id (`booking_id`),
  INDEX idx_payments_status_expires_at (`status`, `expires_at`)
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
