-- UAT read-model isolation and minimal anomaly review lifecycle (MySQL 8+).
-- Run against promotion_db before deploying the matching Promotion Service build.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_promotion_operation_isolation_20260823$$
CREATE PROCEDURE migrate_promotion_operation_isolation_20260823()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_playbooks' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_playbooks
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER promotion_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_playbook_environment (test_data, status, updated_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_automation_runs' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_automation_runs
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER promotion_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_automation_run_environment (test_data, status, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_audience_snapshots' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_audience_snapshots
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER run_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_audience_members' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_audience_members
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER customer_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_audience_member_environment (test_data, status, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND column_name = 'test_data') THEN
        ALTER TABLE user_promotions
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER issuance_key,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_user_promotion_environment (test_data, status, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_reservations' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER user_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_reservation_environment (test_data, status, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_redemptions' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_redemptions
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER campaign_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_redemption_environment (test_data, status, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_redemption_adjustments' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_redemption_adjustments
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER reservation_public_id,
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data,
            ADD INDEX idx_adjustment_environment (test_data, adjustment_type, occurred_at);
    END IF;
END$$
CALL migrate_promotion_operation_isolation_20260823()$$
DROP PROCEDURE migrate_promotion_operation_isolation_20260823$$
DELIMITER ;

UPDATE promotion_playbooks pb
LEFT JOIN promotion_campaigns c ON c.public_id = pb.campaign_public_id
SET pb.test_data = COALESCE(c.test_data, FALSE),
    pb.environment_tag = COALESCE(NULLIF(c.environment_tag, ''),
        CASE WHEN c.test_data = TRUE THEN 'UAT' ELSE 'BUSINESS' END);

UPDATE promotion_automation_runs run
JOIN promotion_campaigns c ON c.public_id = run.campaign_public_id
SET run.test_data = c.test_data,
    run.environment_tag = COALESCE(NULLIF(c.environment_tag, ''),
        CASE WHEN c.test_data = TRUE THEN 'UAT' ELSE 'BUSINESS' END);

UPDATE promotion_audience_snapshots snapshot
JOIN promotion_automation_runs run ON run.public_id = snapshot.run_public_id
SET snapshot.test_data = run.test_data,
    snapshot.environment_tag = run.environment_tag;

UPDATE promotion_audience_members member
JOIN promotion_automation_runs run ON run.public_id = member.run_public_id
SET member.test_data = run.test_data,
    member.environment_tag = run.environment_tag;

UPDATE user_promotions wallet
JOIN promotions promotion ON promotion.public_id = wallet.promotion_public_id
JOIN promotion_campaigns campaign ON campaign.public_id = promotion.campaign_public_id
SET wallet.test_data = campaign.test_data,
    wallet.environment_tag = COALESCE(NULLIF(campaign.environment_tag, ''),
        CASE WHEN campaign.test_data = TRUE THEN 'UAT' ELSE 'BUSINESS' END);

UPDATE promotion_redemptions redemption
JOIN promotion_campaigns campaign ON campaign.public_id = redemption.campaign_public_id
SET redemption.test_data = campaign.test_data,
    redemption.environment_tag = COALESCE(NULLIF(campaign.environment_tag, ''),
        CASE WHEN campaign.test_data = TRUE THEN 'UAT' ELSE 'BUSINESS' END);

UPDATE promotion_reservations reservation
JOIN (
    SELECT reservation_public_id,
           MIN(test_data) AS all_test_data,
           CASE WHEN MIN(environment_tag) = MAX(environment_tag)
                THEN MAX(environment_tag)
                WHEN MIN(test_data) = TRUE THEN 'UAT'
                ELSE 'BUSINESS' END AS resolved_environment_tag
    FROM promotion_redemptions
    WHERE deleted_at IS NULL
    GROUP BY reservation_public_id
) marker ON marker.reservation_public_id = reservation.public_id
SET reservation.test_data = marker.all_test_data,
    reservation.environment_tag = marker.resolved_environment_tag;

UPDATE promotion_redemption_adjustments adjustment
JOIN promotion_redemptions redemption
  ON redemption.public_id = adjustment.redemption_public_id
SET adjustment.test_data = redemption.test_data,
    adjustment.environment_tag = redemption.environment_tag;

-- In the local integrated deployment, test accounts can exercise a production
-- campaign. Their wallet and operational ledger still belong to UAT. Keep this
-- block optional so the promotion schema can also be migrated independently.
DELIMITER $$
DROP PROCEDURE IF EXISTS backfill_promotion_test_accounts_20260823$$
CREATE PROCEDURE backfill_promotion_test_accounts_20260823()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'user_db' AND table_name = 'users')
       AND EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'user_db' AND table_name = 'users'
          AND column_name = 'is_test_account') THEN
        UPDATE user_promotions wallet
        JOIN user_db.users user_account
          ON CAST(user_account.account_id AS CHAR) = wallet.user_public_id
        SET wallet.test_data = TRUE,
            wallet.environment_tag = 'UAT'
        WHERE user_account.is_test_account = TRUE;

        UPDATE promotion_reservations reservation
        JOIN user_db.users user_account
          ON CAST(user_account.account_id AS CHAR) = reservation.user_public_id
        SET reservation.test_data = TRUE,
            reservation.environment_tag = 'UAT'
        WHERE user_account.is_test_account = TRUE;
    END IF;
END$$
CALL backfill_promotion_test_accounts_20260823()$$
DROP PROCEDURE backfill_promotion_test_accounts_20260823$$
DELIMITER ;

UPDATE promotion_redemptions redemption
JOIN promotion_reservations reservation
  ON reservation.public_id = redemption.reservation_public_id
SET redemption.test_data = reservation.test_data,
    redemption.environment_tag = reservation.environment_tag;

UPDATE promotion_redemption_adjustments adjustment
JOIN promotion_redemptions redemption
  ON redemption.public_id = adjustment.redemption_public_id
SET adjustment.test_data = redemption.test_data,
    adjustment.environment_tag = redemption.environment_tag;

CREATE TABLE IF NOT EXISTS promotion_anomaly_cases (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    run_public_id VARCHAR(36) NOT NULL,
    audience_member_public_id VARCHAR(36) NOT NULL,
    playbook_code VARCHAR(80) NOT NULL,
    customer_public_id VARCHAR(36) NOT NULL,
    source_reference VARCHAR(180) DEFAULT NULL,
    reason_code VARCHAR(100) NOT NULL,
    cost_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    test_data BOOLEAN NOT NULL DEFAULT FALSE,
    environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS',
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    assigned_to VARCHAR(36) DEFAULT NULL,
    resolution VARCHAR(40) DEFAULT NULL,
    resolution_note VARCHAR(1000) DEFAULT NULL,
    resolved_by VARCHAR(36) DEFAULT NULL,
    resolved_at DATETIME(6) DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36) DEFAULT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(36) DEFAULT NULL,
    deleted_at DATETIME(6) DEFAULT NULL,
    deleted_by VARCHAR(36) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_promotion_anomaly_public (public_id),
    UNIQUE KEY uq_promotion_anomaly_member (audience_member_public_id),
    KEY idx_promotion_anomaly_work (test_data, status, created_at),
    KEY idx_promotion_anomaly_run (run_public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO promotion_anomaly_cases (
    public_id, run_public_id, audience_member_public_id, playbook_code,
    customer_public_id, source_reference, reason_code, cost_amount,
    test_data, environment_tag, status, created_at, created_by,
    updated_at, updated_by, version)
SELECT UUID(), run.public_id, member.public_id, run.playbook_code,
       member.customer_public_id, run.trigger_reference, member.reason_code,
       member.budget_reserved_amount, run.test_data, run.environment_tag,
       'OPEN', COALESCE(member.updated_at, CURRENT_TIMESTAMP(6)), 'SYSTEM',
       COALESCE(member.updated_at, CURRENT_TIMESTAMP(6)), 'SYSTEM', 0
FROM promotion_audience_members member
JOIN promotion_automation_runs run ON run.public_id = member.run_public_id
WHERE member.status = 'ANOMALY_REVIEW_REQUIRED'
  AND NOT EXISTS (
      SELECT 1 FROM promotion_anomaly_cases anomaly
      WHERE anomaly.audience_member_public_id = member.public_id
  );

UPDATE promotion_playbooks
SET name = 'Ưu đãi cho lần đặt vé thứ hai'
WHERE code = 'SECOND_BOOKING_INCENTIVE';
