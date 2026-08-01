-- Preserve the exact promotion definition used by each ledger entry. Runtime
-- history and compensation must not depend on a mutable catalog template.
ALTER TABLE promotion_redemptions
    DROP CHECK chk_promotion_redemption_status,
    DROP CHECK chk_promotion_redemption_amount,
    ADD COLUMN campaign_public_id VARCHAR(36) NULL AFTER promotion_public_id,
    ADD COLUMN promotion_type VARCHAR(30) NULL AFTER campaign_public_id,
    ADD COLUMN promotion_code VARCHAR(100) NULL AFTER promotion_type,
    ADD COLUMN promotion_name VARCHAR(255) NULL AFTER promotion_code,
    ADD COLUMN promotion_priority INT NULL AFTER promotion_name,
    ADD COLUMN promotion_stackable BOOLEAN NULL AFTER promotion_priority,
    ADD COLUMN conditions_snapshot_json JSON NULL AFTER promotion_stackable,
    ADD COLUMN actions_snapshot_json JSON NULL AFTER conditions_snapshot_json,
    ADD COLUMN sequence_no INT NULL AFTER actions_snapshot_json;

UPDATE promotion_redemptions redemption
JOIN promotions promotion
  ON promotion.public_id = redemption.promotion_public_id
SET redemption.campaign_public_id = promotion.campaign_public_id,
    redemption.promotion_type = promotion.promotion_type,
    redemption.promotion_code = promotion.code,
    redemption.promotion_name = promotion.name,
    redemption.promotion_priority = promotion.priority,
    redemption.promotion_stackable = promotion.stackable,
    redemption.conditions_snapshot_json = promotion.conditions_json,
    redemption.actions_snapshot_json = promotion.actions_json;

-- Legacy rows recorded the checkout amount on every redemption. Rewrite them
-- as a sequential ledger so every row satisfies before - discount = after.
CREATE TEMPORARY TABLE promotion_redemption_steps AS
SELECT redemption.id,
       ROW_NUMBER() OVER (
           PARTITION BY COALESCE(redemption.reservation_public_id, redemption.public_id)
           ORDER BY redemption.created_at, redemption.id
       ) AS sequence_no,
       COALESCE(reservation.original_amount, redemption.original_amount)
         - COALESCE(SUM(redemption.discount_amount) OVER (
             PARTITION BY COALESCE(redemption.reservation_public_id, redemption.public_id)
             ORDER BY redemption.created_at, redemption.id
             ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
           ), 0) AS step_original_amount
FROM promotion_redemptions redemption
LEFT JOIN promotion_reservations reservation
  ON reservation.public_id = redemption.reservation_public_id;

UPDATE promotion_redemptions redemption
JOIN promotion_redemption_steps step ON step.id = redemption.id
SET redemption.sequence_no = step.sequence_no,
    redemption.original_amount = step.step_original_amount,
    redemption.final_amount = step.step_original_amount - redemption.discount_amount;

DROP TEMPORARY TABLE promotion_redemption_steps;

ALTER TABLE promotion_redemptions
    MODIFY COLUMN campaign_public_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN promotion_type VARCHAR(30) NOT NULL,
    MODIFY COLUMN promotion_name VARCHAR(255) NOT NULL,
    MODIFY COLUMN promotion_priority INT NOT NULL,
    MODIFY COLUMN promotion_stackable BOOLEAN NOT NULL,
    MODIFY COLUMN conditions_snapshot_json JSON NOT NULL,
    MODIFY COLUMN actions_snapshot_json JSON NOT NULL,
    MODIFY COLUMN sequence_no INT NOT NULL,
    ADD CONSTRAINT chk_promotion_redemption_status_v2 CHECK (
        status IN ('RESERVED', 'CONFIRMED', 'REVERSED', 'ROLLBACKED')
    ),
    ADD CONSTRAINT chk_promotion_redemption_amount_v2 CHECK (
        original_amount >= 0
        AND discount_amount >= 0
        AND final_amount >= 0
        AND final_amount = original_amount - discount_amount
    );

CREATE INDEX idx_promotion_redemption_campaign
    ON promotion_redemptions (campaign_public_id, status);
CREATE INDEX idx_promotion_redemption_sequence
    ON promotion_redemptions (reservation_public_id, sequence_no);

CREATE TABLE promotion_redemption_adjustments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL,
    redemption_public_id VARCHAR(36) NOT NULL,
    reservation_public_id VARCHAR(36) NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    discount_amount DECIMAL(18, 2) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(36) NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(36) NULL,
    CONSTRAINT uk_promotion_adjustment_public UNIQUE (public_id),
    CONSTRAINT uk_promotion_adjustment_reverse
        UNIQUE (redemption_public_id, adjustment_type),
    CONSTRAINT fk_promotion_adjustment_redemption
        FOREIGN KEY (redemption_public_id)
        REFERENCES promotion_redemptions (public_id),
    CONSTRAINT chk_promotion_adjustment_type CHECK (
        adjustment_type IN ('REVERSE')
    ),
    CONSTRAINT chk_promotion_adjustment_amount CHECK (
        discount_amount >= 0
    )
) COMMENT = 'Append-only promotion redemption compensation ledger';

CREATE INDEX idx_promotion_adjustment_reservation
    ON promotion_redemption_adjustments (reservation_public_id, occurred_at);

-- Administrative reissue creates a new wallet grant after the previous one is
-- consumed/revoked/expired. The public_id remains the stable grant identity.
ALTER TABLE user_promotions
    DROP INDEX uk_user_promotion_owner_template;
CREATE INDEX idx_user_promotion_owner_template
    ON user_promotions (user_public_id, promotion_public_id, created_at);

ALTER TABLE promotion_reservations
    DROP CHECK chk_reservation_status_v2,
    DROP CHECK chk_reservation_lifecycle_v2,
    ADD CONSTRAINT chk_reservation_status_v3 CHECK (
        status IN ('ACTIVE', 'CONFIRMED', 'REVERSED', 'RELEASED', 'EXPIRED')
    ),
    ADD CONSTRAINT chk_reservation_lifecycle_v3 CHECK (
        (
            status = 'ACTIVE'
            AND confirmed_at IS NULL
            AND rollback_at IS NULL
        )
        OR (
            status = 'CONFIRMED'
            AND confirmed_at IS NOT NULL
            AND payment_public_id IS NOT NULL
            AND rollback_at IS NULL
        )
        OR (
            status = 'REVERSED'
            AND confirmed_at IS NOT NULL
            AND payment_public_id IS NOT NULL
            AND rollback_at IS NOT NULL
            AND rollback_reason IS NOT NULL
        )
        OR (
            status = 'RELEASED'
            AND confirmed_at IS NULL
            AND rollback_at IS NOT NULL
            AND rollback_reason IS NOT NULL
        )
        OR (
            status = 'EXPIRED'
            AND confirmed_at IS NULL
            AND rollback_at IS NULL
        )
    );
