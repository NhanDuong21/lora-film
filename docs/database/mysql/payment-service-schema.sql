-- CURRENT STATUS: PROPOSED TARGET SCHEMA FOR ISSUE #157
-- MIGRATION TARGET: FLYWAY
-- NOT YET APPLIED TO RUNTIME DATABASE
-- DO NOT EXECUTE MANUALLY OUTSIDE THE APPROVED MIGRATION FLOW

-- ============================================================================
-- TABLE: booking_payment_guards
-- PURPOSE: Cung cấp cơ chế khóa an toàn và cấp phát attempt_number nhằm đảm bảo giới hạn truy cập đồng thời cho mỗi Booking.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: BR-001, ADR-002, DATA-005
-- FOREIGN KEY RATIONALE: Cột `active_payment_id` và `successful_payment_id` là Logical internal reference validated by Payment Service transaction logic.
-- RELATED ISSUE: #157
-- ============================================================================
CREATE TABLE `booking_payment_guards` (
  `booking_id` bigint PRIMARY KEY,
  `active_payment_id` bigint NULL COMMENT 'Logical internal reference validated by Payment Service transaction logic.',
  `successful_payment_id` bigint NULL COMMENT 'Logical internal reference validated by Payment Service transaction logic.',
  `next_attempt_number` int NOT NULL DEFAULT 1,
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================================
-- TABLE: payments
-- PURPOSE: Lưu trữ vòng đời và thông tin chi tiết của từng Payment Attempt. Hỗ trợ đa dạng phương thức và tích hợp dữ liệu Settlement Hold nhằm kiểm soát Race Condition từ Provider.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: BR-002, FR-010, DATA-001, DATA-004, ADR-013
-- RELATED ISSUE: #157
-- ============================================================================
CREATE TABLE `payments` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `payment_transaction_code` varchar(100) UNIQUE NOT NULL,
  `booking_id` bigint NOT NULL,
  `account_id` bigint NOT NULL COMMENT 'Owner snapshot',
  `attempt_number` int NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `currency` varchar(10) NOT NULL DEFAULT 'VND',
  `payment_method` varchar(30) NOT NULL,
  `provider_order_id` varchar(150),
  `provider_session_id` varchar(150),
  `external_transaction_id` varchar(150),
  `status` varchar(30) NOT NULL DEFAULT 'PENDING',
  `reconciliation_status` varchar(30) NOT NULL DEFAULT 'NONE' COMMENT 'Hỗ trợ logic Late Success / Reconciliation',
  `reconciliation_reason` varchar(255) NULL,
  `reconciliation_resolved_at` timestamp NULL,
  `settlement_hold_until` timestamp NULL COMMENT 'Thời gian khóa tạo attempt mới nếu đang PROCESSING online',
  `expires_at` timestamp NOT NULL,
  `failure_code` varchar(100),
  `failure_message_sanitized` text,
  `provider_response_code` varchar(100),
  `succeeded_at` timestamp NULL,
  `failed_at` timestamp NULL,
  `cancelled_at` timestamp NULL,
  `expired_at` timestamp NULL,
  `latest_provider_summary_sanitized` text,
  `version` int NOT NULL DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX idx_payments_booking_attempt (`booking_id`, `attempt_number`),
  UNIQUE INDEX idx_payments_method_ext_id (`payment_method`, `external_transaction_id`) COMMENT 'MySQL nullable unique behavior allows multiple NULLs safely',
  INDEX idx_payments_booking_id (`booking_id`, `created_at`),
  INDEX idx_payments_account_id (`account_id`, `created_at`),
  INDEX idx_payments_status_expires_at (`status`, `expires_at`),
  INDEX idx_payments_settlement_hold (`status`, `settlement_hold_until`),
  INDEX idx_payments_reconciliation (`reconciliation_status`, `updated_at`)
);

-- ============================================================================
-- TABLE: payment_logs
-- PURPOSE: Ghi nhận lịch sử trạng thái của từng Payment Attempt (Audit log). Cấu trúc Append-Only, Payment records are not hard-deleted in normal operations.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: NFR-006, SEC-011, DATA-003
-- RELATED ISSUE: #157
-- ============================================================================
CREATE TABLE `payment_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `source` varchar(50) NOT NULL,
  `actor_type` varchar(30) NOT NULL,
  `actor_account_id` bigint NULL,
  `previous_status` varchar(30),
  `current_status` varchar(30) NOT NULL,
  `message_sanitized` text,
  `metadata_sanitized` text,
  `correlation_id` varchar(100),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE RESTRICT,
  INDEX idx_payment_logs_payment_id (`payment_id`, `created_at`),
  INDEX idx_payment_logs_event_type (`event_type`, `created_at`)
);

-- ============================================================================
-- TABLE: cash_payment_details
-- PURPOSE: Lưu trữ các trường dữ liệu dành riêng cho thanh toán tại quầy bằng tiền mặt, giúp chuẩn hóa dữ liệu tài chính.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: BR-009, FR-020, DATA-002, ADR-005
-- RELATED ISSUE: #157, #160
-- ============================================================================
CREATE TABLE `cash_payment_details` (
  `payment_id` bigint PRIMARY KEY,
  `received_amount` decimal(12,2) NOT NULL,
  `change_amount` decimal(12,2) NOT NULL,
  `collected_by_account_id` bigint NOT NULL,
  `collected_at` timestamp NOT NULL,
  `note_sanitized` varchar(500),

  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_cash_payment_details_payment`
    FOREIGN KEY (`payment_id`)
    REFERENCES `payments` (`id`)
    ON DELETE RESTRICT,
  CONSTRAINT `chk_cash_received_amount`
    CHECK (`received_amount` >= 0),
  CONSTRAINT `chk_cash_change_amount`
    CHECK (`change_amount` >= 0)
);

CREATE INDEX `idx_cash_payment_collector`
ON `cash_payment_details` (`collected_by_account_id`, `collected_at`);

CREATE INDEX `idx_cash_payment_collected_at`
ON `cash_payment_details` (`collected_at`);

-- ============================================================================
-- TABLE: payment_analytics_snapshots
-- PURPOSE: Lưu trữ thông tin tĩnh snapshot từ Booking phục vụ việc phát hành Analytics Event.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: FR-080, DATA-004, ADR-006
-- RELATED ISSUE: #157, #166
-- ============================================================================
CREATE TABLE `payment_analytics_snapshots` (
  `payment_id` bigint PRIMARY KEY,
  `movie_id` bigint NOT NULL,
  `movie_title` varchar(255) NOT NULL,
  `ticket_count` int NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE RESTRICT,
  CHECK (`ticket_count` > 0)
);

-- ============================================================================
-- TABLE: payment_idempotency_records
-- PURPOSE: Bảng điều khiển cơ chế Persistent Idempotency cho toàn bộ API thay đổi trạng thái, sử dụng thuật toán Hash Canonical SHA-256.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: NFR-002, DATA-014, ADR-004
-- FOREIGN KEY RATIONALE: Cột `payment_id` là Logical internal reference validated by Payment Service transaction logic.
-- RELATED ISSUE: #157, #159
-- ============================================================================
CREATE TABLE `payment_idempotency_records` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `operation` varchar(50) NOT NULL,
  `idempotency_key` varchar(100) NOT NULL,
  `request_hash` varchar(255) NOT NULL COMMENT 'Canonical SHA-256 of request',
  `processing_status` varchar(30) NOT NULL DEFAULT 'PROCESSING',
  `payment_id` bigint NULL COMMENT 'Logical internal reference validated by Payment Service transaction logic.',
  `response_status` int NULL,
  `response_body_sanitized` text NULL,
  `error_code` varchar(100) NULL,
  `last_error` text NULL,
  `locked_at` timestamp NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL,
  UNIQUE INDEX idx_idempotency_acc_op_key (`account_id`, `operation`, `idempotency_key`),
  INDEX idx_idempotency_status_locked (`processing_status`, `locked_at`),
  INDEX idx_idempotency_expires_at (`expires_at`),
  INDEX idx_idempotency_payment_id (`payment_id`)
);

-- ============================================================================
-- TABLE: payment_webhook_events
-- PURPOSE: Tiếp nhận và kiểm soát trùng lặp các IPN Webhook callback từ provider.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: FR-030, DATA-005, ADR-007
-- FOREIGN KEY RATIONALE: Cột `payment_id` là Logical internal reference validated by Payment Service transaction logic.
-- RELATED ISSUE: #157, #161
-- ============================================================================
CREATE TABLE `payment_webhook_events` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `provider` varchar(30) NOT NULL,
  `provider_event_id` varchar(150) NULL,
  `deduplication_key` varchar(150) NOT NULL COMMENT 'Provider-scoped unique criteria',
  `payment_id` bigint NULL COMMENT 'Logical internal reference validated by Payment Service transaction logic.',
  `payment_transaction_code` varchar(100) NULL,
  `provider_order_id` varchar(150) NULL,
  `external_transaction_id` varchar(150) NULL,
  `event_type` varchar(50) NULL,
  `payload_hash` varchar(255) NOT NULL,
  `sanitized_payload` text NOT NULL,
  `signature_valid` boolean NOT NULL DEFAULT FALSE,
  `processing_status` varchar(30) NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `last_error` text NULL,
  `received_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `processed_at` timestamp NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX idx_webhook_dedup (`provider`, `deduplication_key`),
  INDEX idx_webhook_payment_id (`payment_id`, `received_at`),
  INDEX idx_webhook_processing_status (`processing_status`, `received_at`),
  INDEX idx_webhook_provider_ext_tx (`provider`, `external_transaction_id`)
);

-- ============================================================================
-- TABLE: payment_outbox_events
-- PURPOSE: Đảm bảo phát hành Rest API/Kafka cho Booking Service và Analytics một cách Atomic.
-- OWNERSHIP: Payment Service
-- RELATED REQUIREMENTS: NFR-001, FR-070, ADR-008
-- RELATED ISSUE: #157, #161, #164, #166
-- ============================================================================
CREATE TABLE `payment_outbox_events` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(100) UNIQUE NOT NULL,
  `aggregate_type` varchar(50) NOT NULL,
  `aggregate_id` varchar(50) NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `schema_version` varchar(10) NOT NULL,
  `destination` varchar(100) NOT NULL,
  `payload` text NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'PENDING',
  `attempt_count` int NOT NULL DEFAULT 0,
  `next_retry_at` timestamp NULL,
  `last_error` text NULL,
  `correlation_id` varchar(100) NULL,
  `trace_id` varchar(100) NULL,
  `locked_by` varchar(100) NULL,
  `locked_at` timestamp NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `published_at` timestamp NULL,
  INDEX idx_outbox_status_retry (`status`, `next_retry_at`),
  INDEX idx_outbox_locked_at (`locked_at`),
  INDEX idx_outbox_aggregate (`aggregate_type`, `aggregate_id`)
);
