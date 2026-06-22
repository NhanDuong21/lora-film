DROP TABLE IF EXISTS promotion_usages;
DROP TABLE IF EXISTS promotions;
DROP TABLE IF EXISTS promotion_campaigns;

CREATE TABLE promotion_campaigns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Campaign ID',

    campaign_name VARCHAR(150) NOT NULL,
    description TEXT,

    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,

    version INT NOT NULL DEFAULT 0
        COMMENT 'Optimistic locking version',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE promotions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Primary Key - Promotion/Voucher ID',

    campaign_id BIGINT NOT NULL
        COMMENT 'Foreign Key noi bo ket noi voi promotion_campaigns',

    promotion_code VARCHAR(50) UNIQUE NOT NULL
        COMMENT 'Ma voucher khach hang nhap, e.g., LORAFILM2026',

    description TEXT,

    discount_type VARCHAR(20) NOT NULL
        COMMENT 'PERCENTAGE, FIXED_AMOUNT',

    discount_value DECIMAL(10,2) NOT NULL
        COMMENT 'Gia tri giam, e.g., 10.00 cho percent hoac 20000.00 cho fixed',

    max_discount_amount DECIMAL(10,2)
        COMMENT 'So tien giam toi da neu dung PERCENTAGE, Null neu dung FIXED_AMOUNT',

    min_order_amount DECIMAL(10,2) DEFAULT 0
        COMMENT 'Gia tri don hang toi thieu de duoc ap dung ma',

    usage_limit INT NOT NULL
        COMMENT 'Tong so lan ma nay duoc phep su dung tren toan he thong',

    used_count INT DEFAULT 0
        COMMENT 'So lan ma nay da thuc te duoc dung, used_count <= usage_limit',

    limit_per_user INT DEFAULT 1
        COMMENT 'So lan toi da mot khach hang duoc dung ma nay',

    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,

    version INT NOT NULL DEFAULT 0
        COMMENT 'Optimistic locking version',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE promotion_usages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    promotion_id BIGINT NOT NULL,

    user_id BIGINT NOT NULL
        COMMENT 'Logical Ref sang users.account_id cua User Service',

    booking_id BIGINT UNIQUE NOT NULL
        COMMENT 'Logical Ref sang bookings.id cua Booking Service',

    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
        COMMENT 'RESERVED, APPLIED, REVERTED',

    original_amount DECIMAL(10,2) NOT NULL
        COMMENT 'Booking amount truoc discount tai thoi diem apply',

    discount_amount DECIMAL(10,2) NOT NULL
        COMMENT 'Discount snapshot tai thoi diem apply',

    final_amount DECIMAL(10,2) NOT NULL
        COMMENT 'Final amount sau discount tai thoi diem apply',

    expires_at TIMESTAMP NOT NULL
        COMMENT 'Snapshot booking expiry dung cho reconciliation',

    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    reverted_at TIMESTAMP NULL,

    revert_reason VARCHAR(255) NULL,

    version INT NOT NULL DEFAULT 0
        COMMENT 'Optimistic locking version',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE promotions
ADD CONSTRAINT fk_promotions_campaign
FOREIGN KEY (campaign_id)
REFERENCES promotion_campaigns(id)
ON DELETE RESTRICT
ON UPDATE CASCADE;

ALTER TABLE promotion_usages
ADD CONSTRAINT fk_promotion_usages_promotion
FOREIGN KEY (promotion_id)
REFERENCES promotions(id)
ON DELETE RESTRICT
ON UPDATE CASCADE;

CREATE INDEX idx_promotion_usage_user_limit
ON promotion_usages (promotion_id, user_id, status);

CREATE INDEX idx_promotion_usage_expiration
ON promotion_usages (status, expires_at);

CREATE INDEX idx_promotion_code
ON promotions (promotion_code);

CREATE INDEX idx_promotion_active_period
ON promotions (is_active, start_date, end_date);

CREATE INDEX idx_campaign_active_period
ON promotion_campaigns (is_active, start_date, end_date);