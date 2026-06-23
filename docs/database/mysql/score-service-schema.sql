DROP TABLE IF EXISTS `score_expirations`;
DROP TABLE IF EXISTS `score_history`;
DROP TABLE IF EXISTS `user_scores`;
DROP TABLE IF EXISTS `membership_tiers`;



CREATE TABLE `membership_tiers` (

    `id` INT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Primary Key - Tier ID',

    `tier_name` VARCHAR(50) NOT NULL
        COMMENT 'SILVER, GOLD, DIAMOND',

    `min_accumulated_points` INT NOT NULL
        COMMENT 'Minimum accumulated points to achieve this tier',

    `earning_rate` DECIMAL(5,2) NOT NULL
        COMMENT 'Earn percentage, e.g. 3 = 3%, 5 = 5%',

    `description` TEXT NULL,

    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY `uk_membership_tier_name`
        (`tier_name`)
);



CREATE TABLE `user_scores` (

    `user_id` BIGINT PRIMARY KEY
        COMMENT 'Logical Ref sang User Service',

    `current_points` INT NOT NULL DEFAULT 0
        COMMENT 'Current available points',

    `accumulated_points` INT NOT NULL DEFAULT 0
        COMMENT 'Lifetime accumulated points',

    `current_tier_id` INT NOT NULL DEFAULT 1,

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
        COMMENT 'Logical Ref sang bookings.id cua Booking Service',

    `point_change` INT NOT NULL
        COMMENT 'Positive or negative point change',

    `transaction_type` VARCHAR(50) NOT NULL
        COMMENT 'EARN_BY_BOOKING, REDEEM_FOR_BOOKING, REFUND_REDEEM, REVOKE_EARN_BY_REFUND, MANUAL_ADD, MANUAL_DEDUCT, EXPIRED',

    `balance_before` INT NOT NULL,

    `balance_after` INT NOT NULL,

    `accumulated_before` INT NOT NULL,

    `accumulated_after` INT NOT NULL,

    `idempotency_key` VARCHAR(100) NOT NULL,

    `reference_history_id` BIGINT NULL
        COMMENT 'Reference to original history record',

    `created_by` BIGINT NULL
        COMMENT 'Logical Ref sang Admin/Employee account',

    `request_id` VARCHAR(100) NULL,

    `reason` VARCHAR(255) NULL,

    `description` TEXT NULL,

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

    UNIQUE KEY `uk_score_history_request_id`
        (`request_id`),

    INDEX `idx_score_history_user_created`
        (`user_id`, `created_at`),

    INDEX `idx_score_history_user_type_created`
        (`user_id`, `transaction_type`, `created_at`),

    INDEX `idx_score_history_booking`
        (`booking_id`)
);



CREATE TABLE `score_expirations` (

    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,

    `user_id` BIGINT NOT NULL,

    `source_history_id` BIGINT NOT NULL
        COMMENT 'Original earn history',

    `remaining_points` INT NOT NULL,

    `expired_at` DATETIME NOT NULL,

    `is_expired` BOOLEAN NOT NULL DEFAULT FALSE,

    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `fk_score_exp_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `user_scores` (`user_id`),

    CONSTRAINT `fk_score_exp_history`
        FOREIGN KEY (`source_history_id`)
        REFERENCES `score_history` (`id`)
);