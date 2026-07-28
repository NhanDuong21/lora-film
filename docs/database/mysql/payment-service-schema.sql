-- ============================================================================
-- LORAFILM PAYMENT SERVICE - CANONICAL FRESH-INSTALL SCHEMA
-- ============================================================================
-- Database engine : MySQL 8.0.16+ (CHECK constraints must be enforced)
-- Schema owner    : Payment Service
-- Installation    : Manual SQL execution only
-- ORM policy      : spring.jpa.hibernate.ddl-auto=validate
--
-- IMPORTANT:
--   1. This file is the canonical schema for a NEW, EMPTY Payment database.
--   2. Flyway, Liquibase and Lombok are not used by this project.
--   3. Do not run this file over an existing Payment database.
--   4. Do not add DROP statements here. Database recreation is an explicit
--      operator action outside this canonical schema.
--   5. Application and database sessions must use UTC.
--   6. Provider secrets, raw card data, CVV, access tokens and unsanitized
--      provider payloads must never be persisted in these tables.
--
-- Refund provider execution is intentionally outside Release 1. A dedicated
-- refund aggregate must be designed before automatic refunds are introduced.
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================================
-- TABLE: payments
-- PURPOSE:
--   Stores one immutable financial snapshot and lifecycle for each Payment
--   attempt. Booking Service remains authoritative for amount, currency and the
--   original payment deadline.
-- ============================================================================
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `payment_transaction_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

  `booking_public_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `booking_id` bigint NULL
      COMMENT 'Deprecated numeric Booking compatibility identifier; never expose publicly.',
  `account_id` bigint NOT NULL
      COMMENT 'Immutable owner snapshot returned by Booking Service.',
  `attempt_number` int NOT NULL,

  `amount` decimal(12,2) NOT NULL
      COMMENT 'Immutable amount locked by Booking Service.',
  `currency` char(3) CHARACTER SET ascii COLLATE ascii_bin
      NOT NULL DEFAULT 'VND',
  `booking_amount_locked_at` datetime(6) NOT NULL,
  `booking_expires_at` datetime(6) NOT NULL
      COMMENT 'Original Booking deadline. Retry and provider activity never extend it.',

  `payment_method` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL
      COMMENT 'Normalized method such as ONLINE or CASH.',
  `provider_code` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL
      COMMENT 'Provider/channel such as VNPAY, MOMO, CASH or MOCK.',
  `provider_order_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `provider_session_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `provider_session_expires_at` datetime(6) NULL
      COMMENT 'Effective provider session expiry, capped by booking_expires_at.',
  `external_transaction_id` varchar(150) COLLATE utf8mb4_bin NULL,

  `status` varchar(30) CHARACTER SET ascii COLLATE ascii_bin
      NOT NULL DEFAULT 'PENDING',
  `reconciliation_status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NONE',
  `reconciliation_reason` varchar(500) NULL,
  `reconciliation_resolution_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `reconciliation_note_sanitized` text NULL,
  `reconciliation_resolved_by_account_id` bigint NULL,
  `reconciliation_resolved_at` datetime(6) NULL,

  `settlement_hold_until` datetime(6) NULL
      COMMENT 'Short retry-suppression window for an uncertain provider result.',
  `failure_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `failure_message_sanitized` text NULL,
  `provider_response_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `latest_provider_summary_sanitized` json NULL,

  `succeeded_at` datetime(6) NULL,
  `failed_at` datetime(6) NULL,
  `cancelled_at` datetime(6) NULL,
  `expired_at` datetime(6) NULL,

  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_payments` PRIMARY KEY (`id`),
  CONSTRAINT `uk_payments_public_id` UNIQUE (`public_id`),
  CONSTRAINT `uk_payments_transaction_code`
      UNIQUE (`payment_transaction_code`),
  CONSTRAINT `uk_payments_booking_attempt`
      UNIQUE (`booking_public_id`, `attempt_number`),
  CONSTRAINT `uk_payments_provider_order`
      UNIQUE (`provider_code`, `provider_order_id`),
  CONSTRAINT `uk_payments_provider_session`
      UNIQUE (`provider_code`, `provider_session_id`),
  CONSTRAINT `uk_payments_provider_external_transaction`
      UNIQUE (`provider_code`, `external_transaction_id`),

  CONSTRAINT `chk_payments_attempt_number`
      CHECK (`attempt_number` > 0),
  CONSTRAINT `chk_payments_amount`
      CHECK (`amount` > 0),
  CONSTRAINT `chk_payments_currency`
      CHECK (
        CHAR_LENGTH(`currency`) = 3
        AND BINARY `currency` = BINARY UPPER(`currency`)
      ),
  CONSTRAINT `chk_payments_status`
      CHECK (`status` IN (
        'PENDING', 'PROCESSING', 'SUCCESS',
        'FAILED', 'CANCELLED', 'EXPIRED'
      )),
  CONSTRAINT `chk_payments_reconciliation_status`
      CHECK (`reconciliation_status` IN (
        'NONE', 'REQUIRED', 'IN_REVIEW', 'RESOLVED'
      )),
  CONSTRAINT `chk_payments_provider_session_deadline`
      CHECK (
        `provider_session_expires_at` IS NULL
        OR `provider_session_expires_at` <= `booking_expires_at`
      ),
  CONSTRAINT `chk_payments_terminal_timestamp`
      CHECK (
        (`status` <> 'SUCCESS' OR `succeeded_at` IS NOT NULL)
        AND (`status` <> 'FAILED' OR `failed_at` IS NOT NULL)
        AND (`status` <> 'CANCELLED' OR `cancelled_at` IS NOT NULL)
        AND (`status` <> 'EXPIRED' OR `expired_at` IS NOT NULL)
      ),

  INDEX `idx_payments_booking_created`
      (`booking_public_id`, `created_at`),
  INDEX `idx_payments_booking_id_compat`
      (`booking_id`, `created_at`),
  INDEX `idx_payments_account_created`
      (`account_id`, `created_at`),
  INDEX `idx_payments_status_booking_expiry`
      (`status`, `booking_expires_at`),
  INDEX `idx_payments_settlement_hold`
      (`status`, `settlement_hold_until`),
  INDEX `idx_payments_reconciliation`
      (`reconciliation_status`, `updated_at`),
  INDEX `idx_payments_provider_response`
      (`provider_code`, `provider_response_code`, `updated_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: booking_payment_guards
-- PURPOSE:
--   Serializes attempt-number allocation and guarantees at most one active and
--   one successful Payment pointer for each Booking.
-- ============================================================================
CREATE TABLE `booking_payment_guards` (
  `booking_public_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `booking_id` bigint NULL
      COMMENT 'Deprecated numeric Booking compatibility identifier.',
  `active_payment_id` bigint NULL,
  `successful_payment_id` bigint NULL,
  `next_attempt_number` int NOT NULL DEFAULT 1,
  `version` int NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_booking_payment_guards`
      PRIMARY KEY (`booking_public_id`),
  CONSTRAINT `uk_booking_payment_guards_booking_id`
      UNIQUE (`booking_id`),
  CONSTRAINT `fk_booking_payment_guards_active_payment`
      FOREIGN KEY (`active_payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `fk_booking_payment_guards_successful_payment`
      FOREIGN KEY (`successful_payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `chk_booking_payment_guards_next_attempt`
      CHECK (`next_attempt_number` > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: payment_logs
-- PURPOSE:
--   Append-only lifecycle and security audit for a Payment attempt.
-- ============================================================================
CREATE TABLE `payment_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  `event_type` varchar(50)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `source` varchar(50)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor_type` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor_account_id` bigint NULL,
  `previous_status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `current_status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `message_sanitized` text NULL,
  `metadata_sanitized` json NULL,
  `correlation_id` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `trace_id` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_payment_logs` PRIMARY KEY (`id`),
  CONSTRAINT `fk_payment_logs_payment`
      FOREIGN KEY (`payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,

  INDEX `idx_payment_logs_payment_created`
      (`payment_id`, `created_at`),
  INDEX `idx_payment_logs_event_created`
      (`event_type`, `created_at`),
  INDEX `idx_payment_logs_correlation`
      (`correlation_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: cash_payment_details
-- PURPOSE:
--   CASH-only financial and collector snapshot. There is at most one successful
--   collection detail for a Payment attempt.
-- ============================================================================
CREATE TABLE `cash_payment_details` (
  `payment_id` bigint NOT NULL,
  `received_amount` decimal(12,2) NOT NULL,
  `change_amount` decimal(12,2) NOT NULL,
  `collected_by_account_id` bigint NOT NULL,
  `collected_at` datetime(6) NOT NULL,
  `note_sanitized` varchar(500) NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_cash_payment_details` PRIMARY KEY (`payment_id`),
  CONSTRAINT `fk_cash_payment_details_payment`
      FOREIGN KEY (`payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `chk_cash_payment_received_amount`
      CHECK (`received_amount` > 0),
  CONSTRAINT `chk_cash_payment_change_amount`
      CHECK (`change_amount` >= 0),
  CONSTRAINT `chk_cash_payment_received_vs_change`
      CHECK (`received_amount` >= `change_amount`),

  INDEX `idx_cash_payment_collector`
      (`collected_by_account_id`, `collected_at`),
  INDEX `idx_cash_payment_collected_at`
      (`collected_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: payment_analytics_snapshots
-- PURPOSE:
--   Immutable Booking-owned business dimensions used to publish a complete
--   Payment success fact to Analytics without querying Movie Service.
-- ============================================================================
CREATE TABLE `payment_analytics_snapshots` (
  `payment_id` bigint NOT NULL,
  `movie_id` bigint NULL
      COMMENT 'Deprecated numeric Movie compatibility identifier.',
  `movie_public_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `movie_title` varchar(255) NOT NULL,
  `showtime_public_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `cinema_public_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `ticket_count` int NOT NULL,
  `ticket_amount` decimal(12,2) NOT NULL DEFAULT 0,
  `food_amount` decimal(12,2) NOT NULL DEFAULT 0,
  `discount_amount` decimal(12,2) NOT NULL DEFAULT 0,
  `total_amount` decimal(12,2) NOT NULL,
  `currency` char(3) CHARACTER SET ascii COLLATE ascii_bin
      NOT NULL DEFAULT 'VND',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_payment_analytics_snapshots`
      PRIMARY KEY (`payment_id`),
  CONSTRAINT `fk_payment_analytics_snapshots_payment`
      FOREIGN KEY (`payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `chk_payment_analytics_ticket_count`
      CHECK (`ticket_count` > 0),
  CONSTRAINT `chk_payment_analytics_amounts`
      CHECK (
        `ticket_amount` >= 0
        AND `food_amount` >= 0
        AND `discount_amount` >= 0
        AND `total_amount` > 0
      )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: payment_idempotency_records
-- PURPOSE:
--   Persistent idempotency for authenticated mutating APIs. An owner token and
--   finite lease prevent a crashed request from blocking the key forever.
-- ============================================================================
CREATE TABLE `payment_idempotency_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `operation` varchar(50)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `idempotency_key` varchar(100) COLLATE utf8mb4_bin NOT NULL,
  `request_hash` char(64)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `processing_status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PROCESSING',
  `payment_id` bigint NULL,
  `response_status` int NULL,
  `response_body_sanitized` json NULL,
  `error_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `last_error_sanitized` text NULL,
  `locked_by` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `locked_at` datetime(6) NULL,
  `locked_until` datetime(6) NULL,
  `expires_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_payment_idempotency_records` PRIMARY KEY (`id`),
  CONSTRAINT `uk_payment_idempotency_scope`
      UNIQUE (`account_id`, `operation`, `idempotency_key`),
  CONSTRAINT `fk_payment_idempotency_records_payment`
      FOREIGN KEY (`payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `chk_payment_idempotency_status`
      CHECK (`processing_status` IN ('PROCESSING', 'COMPLETED', 'FAILED')),
  CONSTRAINT `chk_payment_idempotency_lease`
      CHECK (
        (`locked_at` IS NULL AND `locked_until` IS NULL AND `locked_by` IS NULL)
        OR
        (`locked_at` IS NOT NULL
          AND `locked_until` IS NOT NULL
          AND `locked_by` IS NOT NULL
          AND `locked_until` > `locked_at`)
      ),

  INDEX `idx_payment_idempotency_status_lease`
      (`processing_status`, `locked_until`),
  INDEX `idx_payment_idempotency_expires`
      (`expires_at`),
  INDEX `idx_payment_idempotency_payment`
      (`payment_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: payment_webhook_events
-- PURPOSE:
--   Provider-scoped inbox and deduplication authority for verified callback
--   processing. Browser return URLs never update Payment state.
-- ============================================================================
CREATE TABLE `payment_webhook_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_code` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `provider_event_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `deduplication_key` varchar(150) COLLATE utf8mb4_bin NOT NULL,
  `payment_id` bigint NULL,
  `payment_transaction_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `provider_order_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `external_transaction_id` varchar(150) COLLATE utf8mb4_bin NULL,
  `event_type` varchar(50)
      CHARACTER SET ascii COLLATE ascii_bin NULL,

  `raw_body_hash` char(64)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `sanitized_payload` json NOT NULL,
  `signature_valid` boolean NOT NULL DEFAULT FALSE,
  `signature_algorithm` varchar(50)
      CHARACTER SET ascii COLLATE ascii_bin NULL,

  `processing_status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime(6) NULL,
  `last_error_sanitized` text NULL,
  `locked_by` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `locked_at` datetime(6) NULL,
  `locked_until` datetime(6) NULL,

  `received_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `processed_at` datetime(6) NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_payment_webhook_events` PRIMARY KEY (`id`),
  CONSTRAINT `uk_payment_webhook_dedup`
      UNIQUE (`provider_code`, `deduplication_key`),
  CONSTRAINT `fk_payment_webhook_events_payment`
      FOREIGN KEY (`payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `chk_payment_webhook_status`
      CHECK (`processing_status` IN (
        'PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'
      )),
  CONSTRAINT `chk_payment_webhook_retry_count`
      CHECK (`retry_count` >= 0),
  CONSTRAINT `chk_payment_webhook_lease`
      CHECK (
        (`locked_at` IS NULL AND `locked_until` IS NULL AND `locked_by` IS NULL)
        OR
        (`locked_at` IS NOT NULL
          AND `locked_until` IS NOT NULL
          AND `locked_by` IS NOT NULL
          AND `locked_until` > `locked_at`)
      ),

  INDEX `idx_payment_webhook_payment_received`
      (`payment_id`, `received_at`),
  INDEX `idx_payment_webhook_status_retry`
      (`processing_status`, `next_retry_at`),
  INDEX `idx_payment_webhook_provider_external`
      (`provider_code`, `external_transaction_id`),
  INDEX `idx_payment_webhook_provider_order`
      (`provider_code`, `provider_order_id`),
  INDEX `idx_payment_webhook_transaction_code`
      (`payment_transaction_code`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: payment_reconciliation_cases
-- PURPOSE:
--   Operational work queue for late success, provider conflicts, Booking result
--   rejection and other cases that require explicit human resolution.
-- ============================================================================
CREATE TABLE `payment_reconciliation_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `public_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `payment_id` bigint NOT NULL,
  `webhook_event_id` bigint NULL,
  `reason_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `source_reference` varchar(150) COLLATE utf8mb4_bin NULL,
  `status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'OPEN',
  `detail_sanitized` text NULL,
  `assigned_to_account_id` bigint NULL,
  `resolution_code` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `resolution_note_sanitized` text NULL,
  `resolved_by_account_id` bigint NULL,
  `opened_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `resolved_at` datetime(6) NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),

  CONSTRAINT `pk_payment_reconciliation_cases` PRIMARY KEY (`id`),
  CONSTRAINT `uk_payment_reconciliation_public_id`
      UNIQUE (`public_id`),
  CONSTRAINT `uk_payment_reconciliation_source`
      UNIQUE (`payment_id`, `reason_code`, `source_reference`),
  CONSTRAINT `fk_payment_reconciliation_cases_payment`
      FOREIGN KEY (`payment_id`)
      REFERENCES `payments` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `fk_payment_reconciliation_cases_webhook`
      FOREIGN KEY (`webhook_event_id`)
      REFERENCES `payment_webhook_events` (`id`)
      ON DELETE RESTRICT,
  CONSTRAINT `chk_payment_reconciliation_case_status`
      CHECK (`status` IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'IGNORED')),
  CONSTRAINT `chk_payment_reconciliation_case_resolution`
      CHECK (
        (`status` NOT IN ('RESOLVED', 'IGNORED'))
        OR (`resolution_code` IS NOT NULL
          AND `resolved_by_account_id` IS NOT NULL
          AND `resolved_at` IS NOT NULL)
      ),

  INDEX `idx_payment_reconciliation_case_queue`
      (`status`, `opened_at`),
  INDEX `idx_payment_reconciliation_case_payment`
      (`payment_id`, `created_at`),
  INDEX `idx_payment_reconciliation_case_assignee`
      (`assigned_to_account_id`, `status`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TABLE: payment_outbox_events
-- PURPOSE:
--   Atomic durable delivery to Booking Service REST and Analytics Kafka.
--   `aggregate_id` is the Payment public ID, never the numeric database ID.
-- ============================================================================
CREATE TABLE `payment_outbox_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` char(36)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `aggregate_type` varchar(50)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `aggregate_id` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `event_type` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `schema_version` varchar(20)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `destination` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `payload` json NOT NULL,

  `status` varchar(30)
      CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
  `attempt_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime(6) NULL,
  `last_error_sanitized` text NULL,
  `correlation_id` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `trace_id` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `locked_by` varchar(100)
      CHARACTER SET ascii COLLATE ascii_bin NULL,
  `locked_at` datetime(6) NULL,
  `locked_until` datetime(6) NULL,

  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),
  `published_at` datetime(6) NULL,

  CONSTRAINT `pk_payment_outbox_events` PRIMARY KEY (`id`),
  CONSTRAINT `uk_payment_outbox_event_id` UNIQUE (`event_id`),
  CONSTRAINT `chk_payment_outbox_status`
      CHECK (`status` IN (
        'PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DEAD_LETTER'
      )),
  CONSTRAINT `chk_payment_outbox_attempt_count`
      CHECK (`attempt_count` >= 0),
  CONSTRAINT `chk_payment_outbox_lease`
      CHECK (
        (`locked_at` IS NULL AND `locked_until` IS NULL AND `locked_by` IS NULL)
        OR
        (`locked_at` IS NOT NULL
          AND `locked_until` IS NOT NULL
          AND `locked_by` IS NOT NULL
          AND `locked_until` > `locked_at`)
      ),

  INDEX `idx_payment_outbox_status_retry`
      (`status`, `next_retry_at`),
  INDEX `idx_payment_outbox_lock_expiry`
      (`status`, `locked_until`),
  INDEX `idx_payment_outbox_aggregate`
      (`aggregate_type`, `aggregate_id`),
  INDEX `idx_payment_outbox_correlation`
      (`correlation_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
