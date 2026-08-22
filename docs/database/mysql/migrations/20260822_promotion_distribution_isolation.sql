-- Canonical distribution modes and terminal automation run semantics (MySQL 8+).
-- Run against promotion_db before deploying the matching Promotion Service build.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_promotion_distribution_isolation_20260822$$
CREATE PROCEDURE migrate_promotion_distribution_isolation_20260822()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotions' AND column_name = 'distribution_mode') THEN
        ALTER TABLE promotions
            ADD COLUMN distribution_mode VARCHAR(30) NOT NULL
                DEFAULT 'ASSIGNED_WALLET' AFTER is_public;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_automation_runs' AND column_name = 'trigger_source') THEN
        ALTER TABLE promotion_automation_runs
            ADD COLUMN trigger_source VARCHAR(30) NOT NULL
                DEFAULT 'LEGACY_UNKNOWN' AFTER trigger_reference;
    END IF;
END$$
CALL migrate_promotion_distribution_isolation_20260822()$$
DROP PROCEDURE migrate_promotion_distribution_isolation_20260822$$
DELIMITER ;

UPDATE promotions
SET distribution_mode = CASE
    WHEN promotion_type = 'AUTO' THEN 'AUTO_APPLY'
    WHEN promotion_type = 'COUPON' THEN 'PERSONAL_CODE'
    WHEN is_public = TRUE THEN 'CLAIMABLE_WALLET'
    ELSE 'ASSIGNED_WALLET'
END;

-- A playbook owns its configured benefit. Its underlying template shape no
-- longer creates a second manual issuance path.
UPDATE promotions p
JOIN promotion_playbooks pb ON pb.promotion_public_id = p.public_id
SET p.distribution_mode = 'AUTOMATION_ONLY'
WHERE pb.deleted_at IS NULL;

UPDATE promotion_automation_runs
SET status = 'COMPLETED_NO_AUDIENCE',
    completed_at = COALESCE(completed_at, started_at, updated_at)
WHERE audience_count = 0
  AND issued_count = 0
  AND skipped_count = 0
  AND failed_count = 0
  AND status IN ('AUDIENCE_READY', 'COMPLETED');

UPDATE promotion_campaigns
SET compliance_reason = 'Được phê duyệt theo quy trình quản trị cũ. Chương trình được phát hành trước khi chính sách maker–checker có hiệu lực; mọi thay đổi mới phải gửi phê duyệt lại.'
WHERE compliance_policy_version = 'legacy-pre-2026-08';
