DROP TABLE IF EXISTS `score_history`;
DROP TABLE IF EXISTS `user_scores`;
DROP TABLE IF EXISTS `membership_tiers`;

CREATE TABLE `membership_tiers` (

    `id` INT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Primary Key - Tier ID',

    `tier_name` VARCHAR(50) NOT NULL
        COMMENT 'SILVER, GOLD, DIAMOND',

    `min_points` INT NOT NULL
        COMMENT 'Minimum accumulated points required to reach this tier',

    `earning_rate` DECIMAL(5,2) NOT NULL
        COMMENT 'Fractional earning rate, e.g. 0.05 = 5%, 0.07 = 7%, 0.10 = 10%',

    `description` TEXT NULL,

    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_membership_tier_name` (`tier_name`)
);

CREATE TABLE `user_scores` (

    `user_id` BIGINT PRIMARY KEY
        COMMENT 'Logical Ref to User Service',

    `current_points` INT NOT NULL DEFAULT 0
        COMMENT 'Available points balance',

    `accumulated_points` INT NOT NULL DEFAULT 0
        COMMENT 'Lifetime accumulated points for tier calculation',

    `current_tier_id` INT NOT NULL
        COMMENT 'Current membership tier',

    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT `fk_user_scores_tier`
        FOREIGN KEY (`current_tier_id`)
        REFERENCES `membership_tiers` (`id`)
);

CREATE TABLE `score_history` (

    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,

    `user_id` BIGINT NOT NULL,

    `booking_id` BIGINT NULL
        COMMENT 'Logical Ref to Booking Service',

    `event_id` VARCHAR(150) NULL
        COMMENT 'Business event identifier received from upstream service',

    `point_change` INT NOT NULL
        COMMENT 'Actual applied point change (+/-)',

    `transaction_type` VARCHAR(50) NOT NULL
        COMMENT 'EARN_BY_BOOKING, REDEEM_FOR_BOOKING, REFUND_REDEEM, REVOKE_EARN_BY_REFUND, MANUAL_ADD, MANUAL_DEDUCT, EXPIRED',

    `balance_before` INT NOT NULL,
    `balance_after` INT NOT NULL,

    `accumulated_before` INT NOT NULL,
    `accumulated_after` INT NOT NULL,

    `idempotency_key` VARCHAR(100) NOT NULL,

    `reference_history_id` BIGINT NULL
        COMMENT 'Self reference to original transaction',

    `created_by` BIGINT NULL
        COMMENT 'Admin/Employee ID if manual action',

    `request_id` VARCHAR(100) NULL,

    `reason` VARCHAR(255) NULL,

    `description` TEXT NULL,

    `requested_point_change` INT NULL
        COMMENT 'Original requested change before partial apply',

    `outstanding_points` INT NOT NULL DEFAULT 0
        COMMENT 'Unprocessed points due to insufficient balance',

    `reconciliation_status` VARCHAR(30) NOT NULL DEFAULT 'NONE'
        COMMENT 'NONE, PENDING, RESOLVED',

    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `fk_score_history_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `user_scores` (`user_id`),

    CONSTRAINT `fk_score_history_reference`
        FOREIGN KEY (`reference_history_id`)
        REFERENCES `score_history` (`id`)
        ON DELETE RESTRICT,

    UNIQUE KEY `uk_score_history_idempotency`
        (`idempotency_key`),

    UNIQUE KEY `uk_score_history_event_id`
        (`event_id`),

    UNIQUE KEY `uk_score_history_request_id`
        (`request_id`),

    INDEX `idx_score_history_user_created`
        (`user_id`, `created_at`),

    INDEX `idx_score_history_user_type_created`
        (`user_id`, `transaction_type`, `created_at`),

    INDEX `idx_score_history_booking`
        (`booking_id`),

    INDEX `idx_score_history_reconciliation`
        (`reconciliation_status`, `created_at`)
);