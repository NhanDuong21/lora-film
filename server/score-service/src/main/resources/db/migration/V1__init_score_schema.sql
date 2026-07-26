-- =====================================================
-- MEMBERSHIP TIERS
-- =====================================================

CREATE TABLE `membership_tiers`
(
    `id` INT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Tier ID',

    `tier_code` VARCHAR(30) NOT NULL
        COMMENT 'SILVER, GOLD, DIAMOND',

    `tier_name` VARCHAR(100) NOT NULL
        COMMENT 'Display name',

    `min_accumulated_points` INT NOT NULL
        COMMENT 'Minimum lifetime points',

    `earning_rate` DECIMAL(5,2) NOT NULL
        COMMENT '0.05 = 5%',

    `priority` INT NOT NULL
        COMMENT 'Higher value = higher tier',

    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,

    `description` TEXT NULL,

    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_membership_tier_code`
        (`tier_code`)
);

-- =====================================================
-- USER SCORE BALANCE (Projection)
-- =====================================================

CREATE TABLE `user_scores`
(
    `user_id` BIGINT PRIMARY KEY
        COMMENT 'Reference User Service',

    `current_points` INT NOT NULL DEFAULT 0
        COMMENT 'Actual balance',

    `held_points` INT NOT NULL DEFAULT 0
        COMMENT 'Reserved by Hold',

    `accumulated_points` INT NOT NULL DEFAULT 0
        COMMENT 'Lifetime point',

    `current_tier_id` INT NOT NULL,

    `status`
        ENUM(
            'ACTIVE',
            'LOCKED',
            'INACTIVE',
            'MERGED'
        )
        NOT NULL DEFAULT 'ACTIVE',

    `outstanding_points`
        INT NOT NULL DEFAULT 0
        COMMENT 'Pending revoke amount',

    `last_earn_at`
        DATETIME NULL,

    `last_redeem_at`
        DATETIME NULL,

    `last_expire_at`
        DATETIME NULL,

    `version`
        BIGINT NOT NULL DEFAULT 0
        COMMENT 'Optimistic Lock',

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `updated_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT `fk_user_scores_tier`
        FOREIGN KEY (`current_tier_id`)
        REFERENCES `membership_tiers` (`id`),

    INDEX `idx_user_scores_tier`
        (`current_tier_id`),

    INDEX `idx_user_scores_status`
        (`status`)
);

-- =====================================================
-- POINT HOLD
-- =====================================================

CREATE TABLE `score_holds`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `hold_code`
        VARCHAR(80) NOT NULL,

    `user_id`
        BIGINT NOT NULL,

    `booking_id`
        BIGINT NOT NULL,

    `points`
        INT NOT NULL,

    `status`
        ENUM
        (
            'ACTIVE',
            'COMMITTED',
            'RELEASED',
            'EXPIRED'
        )
        NOT NULL DEFAULT 'ACTIVE',

    `expired_at`
        DATETIME NOT NULL,

    `committed_at`
        DATETIME NULL,

    `released_at`
        DATETIME NULL,

    `event_id`
        VARCHAR(150) NULL,

    `idempotency_key`
        VARCHAR(100) NOT NULL,

    `request_id`
        VARCHAR(100) NULL,

    `source_service`
        VARCHAR(50) NOT NULL,

    `correlation_id`
        VARCHAR(100) NULL,

    `metadata`
        JSON NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `fk_score_hold_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `user_scores` (`user_id`),

    UNIQUE KEY `uk_hold_code`
        (`hold_code`),

    UNIQUE KEY `uk_hold_booking`
        (`booking_id`),

    UNIQUE KEY `uk_hold_idempotency`
        (`idempotency_key`),

    INDEX `idx_hold_status_expire`
        (`status`,`expired_at`),

    INDEX `idx_hold_user`
        (`user_id`)
);
-- =====================================================
-- POINT EXPIRATION BUCKET
-- FIFO Source Of Truth For Expiration
-- =====================================================

CREATE TABLE `point_expiration_buckets`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `user_id`
        BIGINT NOT NULL,

    `history_id`
        BIGINT NOT NULL
        COMMENT 'Earn transaction that created this bucket',

    `booking_id`
        BIGINT NULL,

    `earned_points`
        INT NOT NULL
        COMMENT 'Original earned point',

    `remaining_points`
        INT NOT NULL
        COMMENT 'Remaining point after redeem/revoke',

    `expired_points`
        INT NOT NULL DEFAULT 0,

    `consumed_points`
        INT NOT NULL DEFAULT 0
        COMMENT 'Redeemed or revoked point',

    `expiration_date`
        DATE NOT NULL,

    `status`
        ENUM
        (
            'ACTIVE',
            'PARTIAL',
            'EXPIRED',
            'CONSUMED'
        )
        NOT NULL DEFAULT 'ACTIVE',

    `tier_snapshot`
        VARCHAR(30) NULL
        COMMENT 'Tier when earning',

    `event_id`
        VARCHAR(150) NULL,

    `metadata`
        JSON NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `updated_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT `fk_bucket_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `user_scores` (`user_id`),

    UNIQUE KEY `uk_bucket_history`
        (`history_id`),

    INDEX `idx_bucket_expiration`
        (`expiration_date`,`status`),

    INDEX `idx_bucket_user_expire`
        (`user_id`,`expiration_date`),

    INDEX `idx_bucket_status`
        (`status`)
);
-- =====================================================
-- SCORE HISTORY (Immutable Ledger)
-- =====================================================

CREATE TABLE `score_history`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `transaction_uuid`
        CHAR(36) NOT NULL
        COMMENT 'Business UUID',

    `user_id`
        BIGINT NOT NULL,

    `booking_id`
        BIGINT NULL,

    `hold_id`
        BIGINT NULL,

    `reference_history_id`
        BIGINT NULL
        COMMENT 'Reverse/Revoke reference',

    `event_id`
        VARCHAR(150) NULL,

    `idempotency_key`
        VARCHAR(120) NOT NULL,

    `request_id`
        VARCHAR(120) NULL,

    `correlation_id`
        VARCHAR(120) NULL,

    `source_service`
        VARCHAR(50) NOT NULL
        COMMENT 'BOOKING / PROMOTION / ADMIN / SCHEDULER',

    `transaction_type`
        ENUM
        (
            'EARN',
            'HOLD',
            'COMMIT',
            'RELEASE',
            'REDEEM',
            'REFUND_REDEEM',
            'REVOKE_EARN',
            'EXPIRED',
            'MANUAL_ADD',
            'MANUAL_DEDUCT',
            'REVERSE_ADJUSTMENT'
        )
        NOT NULL,

    `requested_point_change`
        INT NULL,

    `actual_point_change`
        INT NOT NULL,

    `balance_before`
        INT NOT NULL,

    `balance_after`
        INT NOT NULL,

    `held_before`
        INT NOT NULL DEFAULT 0,

    `held_after`
        INT NOT NULL DEFAULT 0,

    `accumulated_before`
        INT NOT NULL,

    `accumulated_after`
        INT NOT NULL,

    `outstanding_before`
        INT NOT NULL DEFAULT 0,

    `outstanding_after`
        INT NOT NULL DEFAULT 0,

    `tier_snapshot`
        VARCHAR(30) NOT NULL,

    `earning_rate_snapshot`
        DECIMAL(5,2) NULL,

    `redeem_rate_snapshot`
        DECIMAL(10,2) NULL,

    `reason`
        VARCHAR(255) NULL,

    `description`
        TEXT NULL,

    `operator_id`
        BIGINT NULL
        COMMENT 'Admin ID',

    `approval_id`
        BIGINT NULL,

    `case_id`
        VARCHAR(100) NULL,

    `batch_id`
        VARCHAR(100) NULL
        COMMENT 'Expire/Reconciliation batch',

    `reconciliation_status`
        ENUM
        (
            'NONE',
            'PENDING',
            'RESOLVED'
        )
        NOT NULL DEFAULT 'NONE',

    `metadata`
        JSON NULL,

    `occurred_at`
        DATETIME NOT NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `fk_history_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `user_scores` (`user_id`),

    CONSTRAINT `fk_history_hold`
        FOREIGN KEY (`hold_id`)
        REFERENCES `score_holds` (`id`),

    CONSTRAINT `fk_history_reference`
        FOREIGN KEY (`reference_history_id`)
        REFERENCES `score_history` (`id`)
        ON DELETE RESTRICT,

    UNIQUE KEY `uk_transaction_uuid`
        (`transaction_uuid`),

    UNIQUE KEY `uk_history_event`
        (`event_id`),

    UNIQUE KEY `uk_history_idempotency`
        (`idempotency_key`),

    UNIQUE KEY `uk_history_request`
        (`request_id`),

    INDEX `idx_history_user_created`
        (`user_id`,`created_at`),

    INDEX `idx_history_booking`
        (`booking_id`),

    INDEX `idx_history_hold`
        (`hold_id`),

    INDEX `idx_history_event`
        (`event_id`),

    INDEX `idx_history_type_created`
        (`transaction_type`,`created_at`),

    INDEX `idx_history_source`
        (`source_service`),

    INDEX `idx_history_reconciliation`
        (`reconciliation_status`,`created_at`)
);
-- =====================================================
-- RECONCILIATION RUN
-- =====================================================

CREATE TABLE `reconciliation_runs`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `batch_code`
        VARCHAR(100) NOT NULL
        COMMENT 'Unique reconciliation batch',

    `status`
        ENUM
        (
            'RUNNING',
            'COMPLETED',
            'FAILED'
        )
        NOT NULL DEFAULT 'RUNNING',

    `started_at`
        DATETIME NOT NULL,

    `finished_at`
        DATETIME NULL,

    `total_users`
        INT NOT NULL DEFAULT 0,

    `matched_users`
        INT NOT NULL DEFAULT 0,

    `mismatched_users`
        INT NOT NULL DEFAULT 0,

    `total_adjustments`
        INT NOT NULL DEFAULT 0,

    `executed_by`
        BIGINT NULL
        COMMENT 'Scheduler/Admin',

    `remark`
        VARCHAR(500) NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_reconciliation_batch`
        (`batch_code`),

    INDEX `idx_reconciliation_status`
        (`status`)
);

-- =====================================================
-- RECONCILIATION DETAIL
-- =====================================================

CREATE TABLE `reconciliation_details`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `run_id`
        BIGINT NOT NULL,

    `user_id`
        BIGINT NOT NULL,

    `current_balance`
        INT NOT NULL,

    `ledger_balance`
        INT NOT NULL,

    `balance_difference`
        INT NOT NULL,

    `current_held_points`
        INT NOT NULL,

    `ledger_held_points`
        INT NOT NULL,

    `held_difference`
        INT NOT NULL,

    `current_accumulated`
        INT NOT NULL,

    `ledger_accumulated`
        INT NOT NULL,

    `accumulated_difference`
        INT NOT NULL,

    `status`
        ENUM
        (
            'MATCHED',
            'MISMATCH',
            'ADJUSTED',
            'IGNORED'
        )
        NOT NULL,

    `adjustment_history_id`
        BIGINT NULL
        COMMENT 'Adjustment transaction if fixed',

    `remark`
        VARCHAR(500) NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `fk_recon_detail_run`
        FOREIGN KEY (`run_id`)
        REFERENCES `reconciliation_runs` (`id`)
        ON DELETE CASCADE,

    CONSTRAINT `fk_recon_detail_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `user_scores` (`user_id`),

    CONSTRAINT `fk_recon_adjustment`
        FOREIGN KEY (`adjustment_history_id`)
        REFERENCES `score_history` (`id`),

    INDEX `idx_recon_run`
        (`run_id`),

    INDEX `idx_recon_user`
        (`user_id`),

    INDEX `idx_recon_status`
        (`status`)
);
INSERT INTO membership_tiers
(
    tier_code,
    tier_name,
    min_accumulated_points,
    earning_rate,
    priority
)
VALUES
('SILVER','Silver',0,0.05,1),
('GOLD','Gold',400,0.07,2),
('DIAMOND','Diamond',1000,0.10,3);
-- =====================================================
-- OUTBOX EVENTS
-- Reliable Event Publishing (Outbox Pattern)
-- =====================================================

CREATE TABLE `outbox_events`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `aggregate_type`
        VARCHAR(50) NOT NULL
        COMMENT 'USER_SCORE, HOLD, LEDGER',

    `aggregate_id`
        VARCHAR(100) NOT NULL
        COMMENT 'Business aggregate id',

    `event_type`
        VARCHAR(100) NOT NULL
        COMMENT 'POINT_EARNED, POINT_REDEEMED...',

    `event_version`
        INT NOT NULL DEFAULT 1,

    `event_id`
        VARCHAR(150) NOT NULL,

    `correlation_id`
        VARCHAR(120) NULL,

    `payload`
        JSON NOT NULL,

    `status`
        ENUM
        (
            'PENDING',
            'PUBLISHED',
            'FAILED'
        )
        NOT NULL DEFAULT 'PENDING',

    `retry_count`
        INT NOT NULL DEFAULT 0,

    `next_retry_at`
        DATETIME NULL,

    `published_at`
        DATETIME NULL,

    `error_message`
        VARCHAR(1000) NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_outbox_event`
        (`event_id`),

    INDEX `idx_outbox_status`
        (`status`,`created_at`),

    INDEX `idx_outbox_retry`
        (`next_retry_at`)
);
-- =====================================================
-- AUDIT LOGS
-- =====================================================

CREATE TABLE `audit_logs`
(
    `id`
        BIGINT PRIMARY KEY AUTO_INCREMENT,

    `transaction_uuid`
        CHAR(36) NULL,

    `history_id`
        BIGINT NULL,

    `user_id`
        BIGINT NULL,

    `operator_id`
        BIGINT NULL,

    `action`
        VARCHAR(100) NOT NULL,

    `resource`
        VARCHAR(100) NOT NULL,

    `http_method`
        VARCHAR(10) NULL,

    `request_uri`
        VARCHAR(300) NULL,

    `http_status`
        INT NULL,

    `client_ip`
        VARCHAR(45) NULL,

    `user_agent`
        VARCHAR(500) NULL,

    `device_id`
        VARCHAR(120) NULL,

    `correlation_id`
        VARCHAR(120) NULL,

    `request_payload`
        JSON NULL,

    `response_payload`
        JSON NULL,

    `metadata`
        JSON NULL,

    `duration_ms`
        BIGINT NULL,

    `created_at`
        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `fk_audit_history`
        FOREIGN KEY (`history_id`)
        REFERENCES `score_history` (`id`),

    INDEX `idx_audit_user`
        (`user_id`),

    INDEX `idx_audit_operator`
        (`operator_id`),

    INDEX `idx_audit_created`
        (`created_at`),

    INDEX `idx_audit_action`
        (`action`)
);