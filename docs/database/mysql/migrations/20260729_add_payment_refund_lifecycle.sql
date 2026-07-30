-- Manual upgrade for an existing Payment database.
-- Do not run this after a fresh execution of payment-service-schema.sql.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE `payment_refunds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `refund_code` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `payment_id` bigint NOT NULL,
  `request_key` varchar(180) COLLATE utf8mb4_bin NOT NULL,
  `provider_code` varchar(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `refund_type` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `refund_component` varchar(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `reason_code` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `reason_detail_sanitized` text NULL,
  `requested_amount` decimal(12,2) NOT NULL,
  `currency` char(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `automatic` boolean NOT NULL DEFAULT FALSE,
  `requested_by_actor` varchar(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `requested_by_account_id` bigint NULL,
  `status` varchar(30) CHARACTER SET ascii COLLATE ascii_bin
      NOT NULL DEFAULT 'REQUESTED',
  `provider_order_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `provider_request_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `provider_refund_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `provider_response_code` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NULL,
  `provider_summary_sanitized` json NULL,
  `failure_code` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NULL,
  `failure_message_sanitized` text NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_attempt_at` datetime(6) NULL,
  `locked_by` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NULL,
  `locked_at` datetime(6) NULL,
  `locked_until` datetime(6) NULL,
  `requested_at` datetime(6) NOT NULL,
  `submitted_at` datetime(6) NULL,
  `succeeded_at` datetime(6) NULL,
  `failed_at` datetime(6) NULL,
  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT `pk_payment_refunds` PRIMARY KEY (`id`),
  CONSTRAINT `uk_payment_refunds_public_id` UNIQUE (`public_id`),
  CONSTRAINT `uk_payment_refunds_code` UNIQUE (`refund_code`),
  CONSTRAINT `uk_payment_refunds_request` UNIQUE (`payment_id`, `request_key`),
  CONSTRAINT `uk_payment_refunds_provider_order`
      UNIQUE (`provider_code`, `provider_order_id`),
  CONSTRAINT `uk_payment_refunds_provider_request`
      UNIQUE (`provider_code`, `provider_request_id`),
  CONSTRAINT `uk_payment_refunds_provider_refund`
      UNIQUE (`provider_code`, `provider_refund_id`),
  CONSTRAINT `fk_payment_refunds_payment`
      FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_payment_refunds_amount` CHECK (`requested_amount` > 0),
  CONSTRAINT `chk_payment_refunds_type`
      CHECK (`refund_type` IN ('FULL', 'PARTIAL')),
  CONSTRAINT `chk_payment_refunds_component`
      CHECK (`refund_component` IN (
        'FULL_ORDER', 'CONCESSION', 'PRICE_DIFFERENCE',
        'OPERATIONAL_ADJUSTMENT'
      )),
  CONSTRAINT `chk_payment_refunds_type_component`
      CHECK (
        (`refund_type` = 'FULL' AND `refund_component` = 'FULL_ORDER')
        OR `refund_type` = 'PARTIAL'
      ),
  CONSTRAINT `chk_payment_refunds_status`
      CHECK (`status` IN (
        'REQUESTED', 'PROCESSING', 'SUCCESS', 'FAILED',
        'REQUIRES_ACTION', 'CANCELLED'
      )),
  CONSTRAINT `chk_payment_refunds_retry_count` CHECK (`retry_count` >= 0),
  CONSTRAINT `chk_payment_refunds_terminal_timestamp`
      CHECK (
        (`status` <> 'SUCCESS' OR `succeeded_at` IS NOT NULL)
        AND (`status` <> 'FAILED' OR `failed_at` IS NOT NULL)
      ),
  CONSTRAINT `chk_payment_refunds_lease`
      CHECK (
        (`locked_at` IS NULL AND `locked_until` IS NULL AND `locked_by` IS NULL)
        OR (`locked_at` IS NOT NULL AND `locked_until` IS NOT NULL
          AND `locked_by` IS NOT NULL AND `locked_until` > `locked_at`)
      ),
  INDEX `idx_payment_refunds_payment_created` (`payment_id`, `created_at`),
  INDEX `idx_payment_refunds_queue` (`status`, `next_attempt_at`),
  INDEX `idx_payment_refunds_reason` (`reason_code`, `created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
