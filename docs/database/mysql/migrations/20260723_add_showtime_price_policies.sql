-- Showtime Pricing Management V1
-- Preflight: showtime-pricing-migration-preflight.md
-- Run against the Movie Service database before deploying the new Movie Service.

CREATE TABLE price_policies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    cinema_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    priority INT NOT NULL DEFAULT 0,
    supersedes_policy_id BIGINT NULL,
    activated_at TIMESTAMP NULL,
    activated_by BIGINT NULL,
    deactivated_at TIMESTAMP NULL,
    deactivated_by BIGINT NULL,
    deactivation_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_price_policies_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas (id) ON DELETE RESTRICT,
    CONSTRAINT fk_price_policies_supersedes FOREIGN KEY (supersedes_policy_id) REFERENCES price_policies (id) ON DELETE RESTRICT,
    CONSTRAINT chk_price_policies_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT chk_price_policies_currency CHECK (currency = 'VND'),
    INDEX idx_price_policies_resolution (cinema_id, status, effective_from, effective_to, priority),
    INDEX idx_price_policies_supersedes (supersedes_policy_id)
);

CREATE TABLE price_policy_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL UNIQUE,
    policy_id BIGINT NOT NULL,
    seat_type_id BIGINT NOT NULL,
    auditorium_id BIGINT NULL,
    screen_type VARCHAR(30) NULL,
    day_type VARCHAR(20) NOT NULL DEFAULT 'ALL_DAYS',
    time_band_start TIME NULL,
    time_band_end TIME NULL,
    price DECIMAL(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    deleted_by BIGINT NULL,
    CONSTRAINT fk_price_policy_rules_policy FOREIGN KEY (policy_id) REFERENCES price_policies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_price_policy_rules_seat_type FOREIGN KEY (seat_type_id) REFERENCES seat_types (id) ON DELETE RESTRICT,
    CONSTRAINT fk_price_policy_rules_auditorium FOREIGN KEY (auditorium_id) REFERENCES auditoriums (id) ON DELETE RESTRICT,
    CONSTRAINT chk_price_policy_rules_scope CHECK (auditorium_id IS NULL OR screen_type IS NULL),
    CONSTRAINT chk_price_policy_rules_band CHECK (
        (time_band_start IS NULL AND time_band_end IS NULL)
        OR (time_band_start IS NOT NULL AND time_band_end IS NOT NULL AND time_band_start <> time_band_end)
    ),
    CONSTRAINT chk_price_policy_rules_price CHECK (price > 0),
    INDEX idx_price_policy_rules_resolution (policy_id, active, seat_type_id),
    INDEX idx_price_policy_rules_auditorium (auditorium_id)
);

ALTER TABLE showtimes
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER source;

ALTER TABLE showtime_prices
    ADD COLUMN seat_type_name_snapshot VARCHAR(80) NULL AFTER currency,
    ADD COLUMN seat_type_code_snapshot VARCHAR(30) NULL AFTER seat_type_name_snapshot,
    ADD COLUMN pricing_source VARCHAR(30) NULL AFTER seat_type_code_snapshot,
    ADD COLUMN source_policy_id BIGINT NULL AFTER pricing_source,
    ADD COLUMN source_rule_id BIGINT NULL AFTER source_policy_id,
    ADD COLUMN resolved_at TIMESTAMP NULL AFTER source_rule_id,
    ADD COLUMN resolution_timezone VARCHAR(80) NULL AFTER resolved_at;

UPDATE showtime_prices sp
JOIN seat_types st ON st.id = sp.seat_type_id
JOIN showtimes s ON s.id = sp.showtime_id
JOIN cinemas c ON c.id = s.cinema_id
SET sp.seat_type_name_snapshot = st.name,
    sp.seat_type_code_snapshot = st.code,
    sp.pricing_source = 'LEGACY',
    sp.resolved_at = COALESCE(sp.updated_at, sp.created_at, CURRENT_TIMESTAMP),
    sp.resolution_timezone = c.timezone;

ALTER TABLE showtime_prices
    MODIFY COLUMN seat_type_name_snapshot VARCHAR(80) NOT NULL,
    MODIFY COLUMN seat_type_code_snapshot VARCHAR(30) NOT NULL,
    MODIFY COLUMN pricing_source VARCHAR(30) NOT NULL,
    MODIFY COLUMN resolved_at TIMESTAMP NOT NULL,
    MODIFY COLUMN resolution_timezone VARCHAR(80) NOT NULL,
    ADD CONSTRAINT fk_showtime_prices_source_policy
        FOREIGN KEY (source_policy_id) REFERENCES price_policies (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_showtime_prices_source_rule
        FOREIGN KEY (source_rule_id) REFERENCES price_policy_rules (id) ON DELETE RESTRICT,
    ADD INDEX idx_showtime_prices_source_policy (source_policy_id),
    ADD INDEX idx_showtime_prices_source_rule (source_rule_id);

ALTER TABLE showtime_prices
    DROP CHECK chk_showtime_prices_amount,
    ADD CONSTRAINT chk_showtime_prices_amount CHECK (price > 0);

INSERT INTO price_policies (
    public_id, name, cinema_id, effective_from, effective_to, status,
    currency, priority, activated_at, version, created_at, updated_at
)
SELECT UUID(), 'Legacy Default', c.id, '1970-01-01', NULL, 'ACTIVE',
       'VND', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cinemas c
WHERE c.deleted_at IS NULL;

INSERT INTO price_policy_rules (
    public_id, policy_id, seat_type_id, auditorium_id, screen_type,
    day_type, time_band_start, time_band_end, price, active,
    created_at, updated_at
)
SELECT UUID(), p.id, st.id, NULL, NULL, 'ALL_DAYS', NULL, NULL,
       CASE
           WHEN st.code = 'VIP' THEN 90000.00
           WHEN st.code = 'COUPLE' THEN 120000.00
           ELSE 75000.00
       END,
       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM price_policies p
CROSS JOIN seat_types st
WHERE p.name = 'Legacy Default'
  AND p.status = 'ACTIVE'
  AND p.priority = 0
  AND st.status = 'ACTIVE'
  AND st.deleted_at IS NULL;
