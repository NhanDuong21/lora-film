-- A checkout reservation owns one or more unified redemption rows. Benefit and
-- campaign references therefore belong to promotion_redemptions, not to the
-- reservation header.

ALTER TABLE promotion_reservations
    DROP CHECK chk_reservation_single_benefit,
    DROP CHECK chk_reservation_status,
    DROP CHECK chk_reservation_lifecycle;

-- Preserve any legacy reservation that had not produced an old redemption row
-- when V6 copied the historical ledgers.
INSERT INTO promotion_redemptions (
    public_id, reservation_public_id, user_public_id, customer_phone,
    promotion_public_id, user_promotion_public_id, booking_public_id,
    order_public_id, payment_public_id, status, discount_amount,
    original_amount, final_amount, confirmed_at, rollback_at,
    rollback_reason, metadata_json, created_at, created_by,
    updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    UUID(), r.public_id, r.user_public_id, r.customer_phone,
    r.coupon_public_id, NULL, r.booking_public_id, r.order_public_id,
    r.payment_public_id,
    CASE
        WHEN r.status = 'ACTIVE' THEN 'RESERVED'
        WHEN r.status = 'COMPLETED' THEN 'CONFIRMED'
        ELSE 'ROLLBACKED'
    END,
    r.discount_amount, r.original_amount, r.final_amount, r.confirmed_at,
    r.rollback_at,
    COALESCE(r.rollback_reason, r.cancelled_reason,
             IF(r.status = 'EXPIRED', 'Legacy reservation expired', NULL)),
    r.metadata_json, r.created_at, r.created_by, r.updated_at, r.updated_by,
    r.deleted_at, r.deleted_by
FROM promotion_reservations r
WHERE r.coupon_public_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM promotion_redemptions pr
      WHERE pr.reservation_public_id = r.public_id
        AND pr.promotion_public_id = r.coupon_public_id
  );

INSERT INTO promotion_redemptions (
    public_id, reservation_public_id, user_public_id, customer_phone,
    promotion_public_id, user_promotion_public_id, booking_public_id,
    order_public_id, payment_public_id, status, discount_amount,
    original_amount, final_amount, confirmed_at, rollback_at,
    rollback_reason, metadata_json, created_at, created_by,
    updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    UUID(), r.public_id, r.user_public_id, r.customer_phone,
    r.voucher_public_id, up.public_id, r.booking_public_id, r.order_public_id,
    r.payment_public_id,
    CASE
        WHEN r.status = 'ACTIVE' THEN 'RESERVED'
        WHEN r.status = 'COMPLETED' THEN 'CONFIRMED'
        ELSE 'ROLLBACKED'
    END,
    r.discount_amount, r.original_amount, r.final_amount, r.confirmed_at,
    r.rollback_at,
    COALESCE(r.rollback_reason, r.cancelled_reason,
             IF(r.status = 'EXPIRED', 'Legacy reservation expired', NULL)),
    r.metadata_json, r.created_at, r.created_by, r.updated_at, r.updated_by,
    r.deleted_at, r.deleted_by
FROM promotion_reservations r
JOIN user_promotions up
  ON up.promotion_public_id = r.voucher_public_id
 AND up.user_public_id = r.user_public_id
WHERE r.voucher_public_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM promotion_redemptions pr
      WHERE pr.reservation_public_id = r.public_id
        AND pr.promotion_public_id = r.voucher_public_id
  );

UPDATE promotion_reservations
SET status = 'CONFIRMED'
WHERE status = 'COMPLETED';

UPDATE promotion_reservations
SET status = 'RELEASED',
    rollback_at = COALESCE(rollback_at, cancelled_at, updated_at),
    rollback_reason = COALESCE(
        rollback_reason, cancelled_reason, 'Legacy reservation cancelled')
WHERE status = 'CANCELLED';

DROP INDEX idx_reservation_campaign ON promotion_reservations;
DROP INDEX idx_reservation_coupon ON promotion_reservations;
DROP INDEX idx_reservation_voucher ON promotion_reservations;
DROP INDEX idx_reservation_coupon_active ON promotion_reservations;
DROP INDEX idx_reservation_coupon_user_active ON promotion_reservations;
DROP INDEX idx_reservation_coupon_phone_active ON promotion_reservations;
DROP INDEX idx_reservation_voucher_active ON promotion_reservations;
DROP INDEX idx_reservation_campaign_active ON promotion_reservations;
DROP INDEX idx_reservation_history ON promotion_reservations;

ALTER TABLE promotion_reservations
    DROP COLUMN campaign_public_id,
    DROP COLUMN coupon_public_id,
    DROP COLUMN voucher_public_id,
    DROP COLUMN reservation_type,
    DROP COLUMN cancelled_at,
    DROP COLUMN cancelled_reason,
    ADD CONSTRAINT chk_reservation_status_v2 CHECK (
        status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED')
    ),
    ADD CONSTRAINT chk_reservation_lifecycle_v2 CHECK (
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

CREATE INDEX idx_reservation_history_v2
    ON promotion_reservations (status, created_at);
