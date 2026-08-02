-- Promotion architecture v2: Campaign is a container, while every discount
-- definition lives in one Promotion catalogue. Legacy tables stay available
-- during this migration so data can be verified before the cleanup migration.

ALTER TABLE promotion_campaigns
    DROP COLUMN campaign_type;

CREATE TABLE promotions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL,
    campaign_public_id VARCHAR(36) NULL,
    promotion_type VARCHAR(30) NOT NULL,
    code VARCHAR(100) NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    priority INT NOT NULL DEFAULT 100,
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    conditions_json JSON NOT NULL,
    actions_json JSON NOT NULL,
    metadata_json JSON NULL,
    max_redemptions INT NULL,
    redemption_count INT NOT NULL DEFAULT 0,
    max_redemptions_per_user INT NOT NULL DEFAULT 1,
    valid_from DATETIME(6) NOT NULL,
    valid_to DATETIME(6) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(36) NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(36) NULL,
    CONSTRAINT uk_promotion_public UNIQUE (public_id),
    CONSTRAINT uk_promotion_campaign_type_code
        UNIQUE (campaign_public_id, promotion_type, code),
    CONSTRAINT chk_promotion_type CHECK (
        promotion_type IN ('AUTO', 'VOUCHER', 'COUPON')
    ),
    CONSTRAINT chk_promotion_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'DISABLED', 'EXPIRED')
    ),
    CONSTRAINT chk_promotion_visibility CHECK (
        (promotion_type = 'VOUCHER' AND code IS NOT NULL)
        OR (promotion_type = 'COUPON' AND code IS NOT NULL AND is_public = FALSE)
        OR (promotion_type = 'AUTO' AND is_public = FALSE)
    ),
    CONSTRAINT chk_promotion_period CHECK (valid_to > valid_from),
    CONSTRAINT chk_promotion_priority CHECK (priority >= 0),
    CONSTRAINT chk_promotion_counts CHECK (
        redemption_count >= 0
        AND max_redemptions_per_user > 0
        AND (max_redemptions IS NULL OR max_redemptions > 0)
    )
) COMMENT = 'Unified AUTO, VOUCHER and COUPON templates';

CREATE INDEX idx_promotion_campaign ON promotions (campaign_public_id);
CREATE INDEX idx_promotion_discovery
    ON promotions (promotion_type, status, is_public, valid_from, valid_to);
CREATE INDEX idx_promotion_priority ON promotions (priority);
CREATE INDEX idx_promotion_deleted ON promotions (deleted_at);

CREATE TABLE user_promotions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL,
    user_public_id VARCHAR(36) NOT NULL,
    promotion_public_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    claimed_at DATETIME(6) NOT NULL,
    valid_from DATETIME(6) NOT NULL,
    valid_to DATETIME(6) NOT NULL,
    usage_count INT NOT NULL DEFAULT 0,
    max_usage INT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(36) NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(36) NULL,
    CONSTRAINT uk_user_promotion_public UNIQUE (public_id),
    CONSTRAINT uk_user_promotion_owner_template
        UNIQUE (user_public_id, promotion_public_id),
    CONSTRAINT fk_user_promotion_template
        FOREIGN KEY (promotion_public_id) REFERENCES promotions (public_id),
    CONSTRAINT chk_user_promotion_status CHECK (
        status IN ('AVAILABLE', 'USED', 'EXPIRED', 'REVOKED')
    ),
    CONSTRAINT chk_user_promotion_period CHECK (valid_to > valid_from),
    CONSTRAINT chk_user_promotion_usage CHECK (
        usage_count >= 0 AND max_usage > 0 AND usage_count <= max_usage
    )
) COMMENT = 'Customer promotion wallet';

CREATE INDEX idx_user_promotion_wallet
    ON user_promotions (user_public_id, status, valid_to);
CREATE INDEX idx_user_promotion_template ON user_promotions (promotion_public_id);
CREATE INDEX idx_user_promotion_deleted ON user_promotions (deleted_at);

CREATE TABLE promotion_redemptions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL,
    reservation_public_id VARCHAR(36) NULL,
    user_public_id VARCHAR(36) NOT NULL,
    customer_phone VARCHAR(20) NULL,
    promotion_public_id VARCHAR(36) NOT NULL,
    user_promotion_public_id VARCHAR(36) NULL,
    booking_public_id VARCHAR(36) NULL,
    order_public_id VARCHAR(36) NULL,
    payment_public_id VARCHAR(36) NULL,
    status VARCHAR(30) NOT NULL,
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    original_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    confirmed_at DATETIME(6) NULL,
    rollback_at DATETIME(6) NULL,
    rollback_reason VARCHAR(255) NULL,
    metadata_json JSON NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(36) NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(36) NULL,
    CONSTRAINT uk_promotion_redemption_public UNIQUE (public_id),
    CONSTRAINT uk_promotion_redemption_reservation_item
        UNIQUE (reservation_public_id, promotion_public_id),
    CONSTRAINT fk_promotion_redemption_template
        FOREIGN KEY (promotion_public_id) REFERENCES promotions (public_id),
    CONSTRAINT fk_promotion_redemption_wallet
        FOREIGN KEY (user_promotion_public_id) REFERENCES user_promotions (public_id),
    CONSTRAINT chk_promotion_redemption_status CHECK (
        status IN ('RESERVED', 'CONFIRMED', 'ROLLBACKED')
    ),
    CONSTRAINT chk_promotion_redemption_amount CHECK (
        original_amount >= 0
        AND discount_amount >= 0
        AND final_amount >= 0
        AND final_amount = original_amount - discount_amount
    )
) COMMENT = 'Unified promotion reservation and redemption ledger';

CREATE INDEX idx_promotion_redemption_reservation
    ON promotion_redemptions (reservation_public_id);
CREATE INDEX idx_promotion_redemption_template
    ON promotion_redemptions (promotion_public_id, status);
CREATE INDEX idx_promotion_redemption_wallet
    ON promotion_redemptions (user_promotion_public_id, status);
CREATE INDEX idx_promotion_redemption_user
    ON promotion_redemptions (user_public_id, status, created_at);
CREATE INDEX idx_promotion_redemption_booking
    ON promotion_redemptions (booking_public_id);
CREATE INDEX idx_promotion_redemption_order
    ON promotion_redemptions (order_public_id);

-- Legacy automatic rules become AUTO templates. Their public IDs are retained
-- so audit references and operational links remain stable.
INSERT INTO promotions (
    public_id, campaign_public_id, promotion_type, code, name, description,
    status, is_public, priority, stackable, conditions_json, actions_json,
    metadata_json, max_redemptions, redemption_count,
    max_redemptions_per_user, valid_from, valid_to, version,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    r.public_id, r.campaign_public_id, 'AUTO', r.code, r.name, r.description,
    IF(r.enabled, 'ACTIVE', 'DISABLED'), FALSE, r.priority, r.stackable,
    r.conditions_json, r.actions_json, r.metadata_json,
    c.max_redemptions, 0, c.max_redemptions_per_user,
    r.effective_from, COALESCE(r.effective_to, c.end_at), r.version,
    r.created_at, r.created_by, r.updated_at, r.updated_by,
    r.deleted_at, r.deleted_by
FROM promotion_rules r
JOIN promotion_campaigns c ON c.public_id = r.campaign_public_id;

INSERT INTO promotions (
    public_id, campaign_public_id, promotion_type, code, name, description,
    status, is_public, priority, stackable, conditions_json, actions_json,
    metadata_json, max_redemptions, redemption_count,
    max_redemptions_per_user, valid_from, valid_to, version,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    public_id, campaign_public_id, 'COUPON', code, name, description,
    CASE
        WHEN status = 'ACTIVE' THEN 'ACTIVE'
        WHEN status = 'DRAFT' THEN 'DRAFT'
        WHEN status = 'EXPIRED' THEN 'EXPIRED'
        ELSE 'DISABLED'
    END,
    FALSE, priority, stackable, conditions_json, actions_json, metadata_json,
    max_redemptions, redemption_count, max_redemptions_per_user,
    valid_from, valid_to, version, created_at, created_by, updated_at,
    updated_by, deleted_at, deleted_by
FROM coupons;

-- Every legacy Voucher was already issued to one owner. It becomes one private
-- template plus one wallet instance, preserving the exact validity and usage.
INSERT INTO promotions (
    public_id, campaign_public_id, promotion_type, code, name, description,
    status, is_public, priority, stackable, conditions_json, actions_json,
    metadata_json, max_redemptions, redemption_count,
    max_redemptions_per_user, valid_from, valid_to, version,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    public_id, campaign_public_id, 'VOUCHER', code, name, description,
    CASE
        WHEN status IN ('ISSUED', 'ACTIVE') THEN 'ACTIVE'
        WHEN status = 'EXPIRED' THEN 'EXPIRED'
        ELSE 'DISABLED'
    END,
    FALSE, 100, stackable, conditions_json, actions_json, metadata_json,
    max_usage, usage_count, max_usage, valid_from, valid_to, version,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
FROM vouchers;

INSERT INTO user_promotions (
    public_id, user_public_id, promotion_public_id, status, claimed_at,
    valid_from, valid_to, usage_count, max_usage, version,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    UUID(), owner_public_id, public_id,
    CASE
        WHEN usage_count >= max_usage OR status = 'USED' THEN 'USED'
        WHEN status = 'EXPIRED' OR valid_to <= CURRENT_TIMESTAMP(6) THEN 'EXPIRED'
        WHEN status IN ('REVOKED', 'CANCELLED', 'LOCKED') THEN 'REVOKED'
        ELSE 'AVAILABLE'
    END,
    issued_at, valid_from, valid_to, usage_count, max_usage, version,
    created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
FROM vouchers;

INSERT INTO promotion_redemptions (
    public_id, reservation_public_id, user_public_id, customer_phone,
    promotion_public_id, user_promotion_public_id, booking_public_id,
    order_public_id, payment_public_id, status, discount_amount,
    original_amount, final_amount, confirmed_at, rollback_at,
    rollback_reason, metadata_json, created_at, created_by,
    updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    cr.public_id, cr.reservation_public_id, cr.user_public_id,
    cr.customer_phone, cr.coupon_public_id, NULL, cr.booking_public_id,
    cr.order_public_id, cr.payment_public_id,
    CASE
        WHEN cr.status IN ('SUCCESS', 'CONFIRMED') THEN 'CONFIRMED'
        ELSE 'ROLLBACKED'
    END,
    cr.discount_amount, cr.original_amount, cr.final_amount,
    cr.confirmed_at, cr.rollback_at, cr.rollback_reason, cr.metadata_json,
    cr.created_at, cr.created_by, cr.updated_at, cr.updated_by,
    cr.deleted_at, cr.deleted_by
FROM coupon_redemptions cr;

INSERT INTO promotion_redemptions (
    public_id, reservation_public_id, user_public_id, customer_phone,
    promotion_public_id, user_promotion_public_id, booking_public_id,
    order_public_id, payment_public_id, status, discount_amount,
    original_amount, final_amount, confirmed_at, rollback_at,
    rollback_reason, metadata_json, created_at, created_by,
    updated_at, updated_by, deleted_at, deleted_by
)
SELECT
    vr.public_id, vr.reservation_public_id, vr.redeemed_by, NULL,
    vr.voucher_public_id, up.public_id, vr.booking_public_id,
    vr.order_public_id, vr.payment_public_id,
    CASE
        WHEN vr.status IN ('SUCCESS', 'CONFIRMED') THEN 'CONFIRMED'
        ELSE 'ROLLBACKED'
    END,
    vr.discount_amount, vr.original_amount, vr.final_amount,
    vr.confirmed_at, vr.rollback_at, vr.rollback_reason, vr.metadata_json,
    vr.created_at, vr.created_by, vr.updated_at, vr.updated_by,
    vr.deleted_at, vr.deleted_by
FROM voucher_redemptions vr
JOIN user_promotions up
  ON up.promotion_public_id = vr.voucher_public_id
 AND up.user_public_id = vr.owner_public_id;

UPDATE approval_histories
SET target_type = 'PROMOTION'
WHERE target_type = 'RULE';
