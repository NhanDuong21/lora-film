-- Promotion automation operational read-model hardening (MySQL 8+).
-- Run against promotion_db before deploying the matching service build.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_promotion_read_model_hardening_20260821$$
CREATE PROCEDURE migrate_promotion_read_model_hardening_20260821()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'test_data') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN test_data BOOLEAN NOT NULL DEFAULT FALSE AFTER remarks;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'environment_tag') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN environment_tag VARCHAR(30) NOT NULL DEFAULT 'BUSINESS' AFTER test_data;
    END IF;
END$$
CALL migrate_promotion_read_model_hardening_20260821()$$
DROP PROCEDURE migrate_promotion_read_model_hardening_20260821$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS promotion_automation_suppressions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    playbook_code VARCHAR(80) NOT NULL,
    trigger_reference VARCHAR(180) NOT NULL,
    reason_code VARCHAR(100) NOT NULL,
    observed_at DATETIME(6) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by VARCHAR(36) NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(36) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_automation_suppression_public (public_id),
    UNIQUE KEY uq_automation_suppression_trigger (playbook_code, trigger_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- One-time classification of legacy local fixtures. Runtime code relies only
-- on the persisted fields above and never infers UAT data from the campaign name.
UPDATE promotion_campaigns
SET test_data = TRUE,
    environment_tag = 'UAT'
WHERE code LIKE 'UAT\_%';
