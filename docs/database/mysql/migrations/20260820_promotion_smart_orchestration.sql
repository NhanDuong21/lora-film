-- Smart promotion orchestration foundation (MySQL 8+).
-- Run against promotion_db before deploying the matching service build.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_promotion_smart_orchestration_20260820$$
CREATE PROCEDURE migrate_promotion_smart_orchestration_20260820()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'legal_notification_required') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN legal_notification_required BOOLEAN NOT NULL DEFAULT TRUE AFTER legal_notification_ref;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'compliance_status') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN compliance_status VARCHAR(30) NOT NULL DEFAULT 'REVIEW_REQUIRED' AFTER legal_notification_required;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'compliance_reason') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN compliance_reason VARCHAR(500) NULL AFTER compliance_status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'compliance_policy_version') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN compliance_policy_version VARCHAR(50) NULL AFTER compliance_reason;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'compliance_verified_by') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN compliance_verified_by VARCHAR(36) NULL AFTER compliance_policy_version;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'compliance_verified_at') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN compliance_verified_at DATETIME(6) NULL AFTER compliance_verified_by;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND column_name = 'automation_run_public_id') THEN
        ALTER TABLE user_promotions ADD COLUMN automation_run_public_id VARCHAR(36) NULL AFTER max_usage;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND column_name = 'audience_member_public_id') THEN
        ALTER TABLE user_promotions ADD COLUMN audience_member_public_id VARCHAR(36) NULL AFTER automation_run_public_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND column_name = 'issuance_key') THEN
        ALTER TABLE user_promotions ADD COLUMN issuance_key VARCHAR(180) NULL AFTER audience_member_public_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND index_name = 'uk_user_promotion_issuance') THEN
        ALTER TABLE user_promotions ADD UNIQUE KEY uk_user_promotion_issuance (issuance_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND column_name = 'revocation_pending') THEN
        ALTER TABLE user_promotions ADD COLUMN revocation_pending BOOLEAN NOT NULL DEFAULT FALSE AFTER issuance_key;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'user_promotions' AND column_name = 'revocation_reason') THEN
        ALTER TABLE user_promotions ADD COLUMN revocation_reason VARCHAR(100) NULL AFTER revocation_pending;
    END IF;
END$$
CALL migrate_promotion_smart_orchestration_20260820()$$
DROP PROCEDURE migrate_promotion_smart_orchestration_20260820$$
DELIMITER ;

-- Existing published campaigns were accepted under the previous policy and
-- cannot be sent back through a draft-only legal review. Grandfather them
-- explicitly instead of pretending a missing notification reference exists.
UPDATE promotion_campaigns
SET legal_notification_required = FALSE,
    compliance_status = 'NOT_REQUIRED',
    compliance_reason = 'Campaign đã phát hành trước chính sách 2026-08-v1 và được giữ nguyên trạng thái',
    compliance_policy_version = 'legacy-pre-2026-08'
WHERE status <> 'DRAFT'
  AND legal_status = 'PASSED'
  AND compliance_policy_version IS NULL;

UPDATE promotion_campaigns
SET compliance_reason = 'Campaign đã phát hành trước chính sách 2026-08-v1 và được giữ nguyên trạng thái'
WHERE compliance_policy_version = 'legacy-pre-2026-08'
  AND compliance_reason = 'Legacy published campaign accepted before policy 2026-08-v1';

CREATE TABLE IF NOT EXISTS promotion_playbooks (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, public_id VARCHAR(36) NOT NULL,
  code VARCHAR(80) NOT NULL, name VARCHAR(180) NOT NULL, description VARCHAR(500),
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', playbook_version INT NOT NULL DEFAULT 1,
  trigger_type VARCHAR(60) NOT NULL, campaign_public_id VARCHAR(36), promotion_public_id VARCHAR(36),
  config_json JSON NOT NULL, scope_json JSON NOT NULL, budget_limit DECIMAL(18,2), quota_limit INT,
  config_hash VARCHAR(64), submitted_playbook_version INT, submitted_config_hash VARCHAR(64),
  approved_playbook_version INT, approved_config_hash VARCHAR(64), budget_period_key VARCHAR(7),
  budget_committed DECIMAL(18,2) NOT NULL DEFAULT 0, quota_committed INT NOT NULL DEFAULT 0,
  submitted_by VARCHAR(36), submitted_at DATETIME(6), approved_by VARCHAR(36), approved_at DATETIME(6),
  version INT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by VARCHAR(36), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(36), deleted_at DATETIME(6), deleted_by VARCHAR(36), PRIMARY KEY (id),
  UNIQUE KEY uk_playbook_public (public_id), UNIQUE KEY uk_playbook_code (code), KEY idx_playbook_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion_automation_runs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, public_id VARCHAR(36) NOT NULL,
  playbook_public_id VARCHAR(36) NOT NULL, campaign_public_id VARCHAR(36) NOT NULL,
  promotion_public_id VARCHAR(36) NOT NULL, playbook_code VARCHAR(80) NOT NULL,
  playbook_version INT NOT NULL, approved_config_hash VARCHAR(64) NOT NULL,
  config_snapshot_json JSON NOT NULL, scope_snapshot_json JSON NOT NULL,
  budget_snapshot DECIMAL(18,2), quota_snapshot INT,
  estimated_unit_cost DECIMAL(18,2) NOT NULL DEFAULT 0, trigger_type VARCHAR(60) NOT NULL,
  trigger_reference VARCHAR(180), run_actor VARCHAR(36) NOT NULL DEFAULT 'SYSTEM',
  authorized_by VARCHAR(36) NOT NULL, idempotency_key VARCHAR(180) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING', audience_count INT NOT NULL DEFAULT 0,
  issued_count INT NOT NULL DEFAULT 0, skipped_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0, started_at DATETIME(6), completed_at DATETIME(6),
  version INT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by VARCHAR(36), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(36), deleted_at DATETIME(6), deleted_by VARCHAR(36), PRIMARY KEY (id),
  UNIQUE KEY uk_automation_run_public (public_id), UNIQUE KEY uk_automation_run_idempotency (idempotency_key),
  KEY idx_automation_run_playbook (playbook_public_id, created_at), KEY idx_automation_run_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion_audience_snapshots (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, public_id VARCHAR(36) NOT NULL,
  run_public_id VARCHAR(36) NOT NULL, audience_rule_snapshot_json JSON NOT NULL,
  total_count INT NOT NULL DEFAULT 0, eligible_count INT NOT NULL DEFAULT 0,
  excluded_count INT NOT NULL DEFAULT 0, captured_at DATETIME(6) NOT NULL,
  version INT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by VARCHAR(36), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(36), deleted_at DATETIME(6), deleted_by VARCHAR(36), PRIMARY KEY (id),
  UNIQUE KEY uk_audience_snapshot_public (public_id), UNIQUE KEY uk_audience_snapshot_run (run_public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion_audience_members (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, public_id VARCHAR(36) NOT NULL,
  snapshot_public_id VARCHAR(36) NOT NULL, run_public_id VARCHAR(36) NOT NULL,
  customer_public_id VARCHAR(36) NOT NULL, issuance_key VARCHAR(180) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING', reason_code VARCHAR(80),
  attempt_count INT NOT NULL DEFAULT 0, wallet_public_id VARCHAR(36),
  budget_reserved_amount DECIMAL(18,2) NOT NULL DEFAULT 0, budget_period_key VARCHAR(7),
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), created_by VARCHAR(36),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(36), deleted_at DATETIME(6), deleted_by VARCHAR(36), PRIMARY KEY (id),
  UNIQUE KEY uk_audience_member_public (public_id),
  UNIQUE KEY uq_audience_snapshot_customer (snapshot_public_id, customer_public_id),
  UNIQUE KEY uq_audience_run_customer (run_public_id, customer_public_id),
  KEY idx_audience_member_work (run_public_id, status, id), KEY idx_audience_member_issuance (issuance_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion_issue_jobs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, public_id VARCHAR(36) NOT NULL,
  run_public_id VARCHAR(36) NOT NULL, snapshot_public_id VARCHAR(36) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING', batch_size INT NOT NULL DEFAULT 200,
  processed_count INT NOT NULL DEFAULT 0, issued_count INT NOT NULL DEFAULT 0,
  skipped_count INT NOT NULL DEFAULT 0, failed_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500), started_at DATETIME(6), completed_at DATETIME(6),
  version INT NOT NULL DEFAULT 0, created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by VARCHAR(36), updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(36), deleted_at DATETIME(6), deleted_by VARCHAR(36), PRIMARY KEY (id),
  UNIQUE KEY uk_issue_job_public (public_id), KEY idx_issue_job_run (run_public_id, created_at),
  KEY idx_issue_job_work (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS harden_promotion_automation_20260820$$
CREATE PROCEDURE harden_promotion_automation_20260820()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_playbooks' AND column_name = 'config_hash') THEN
        ALTER TABLE promotion_playbooks
          ADD COLUMN config_hash VARCHAR(64) NULL AFTER quota_limit,
          ADD COLUMN submitted_playbook_version INT NULL AFTER config_hash,
          ADD COLUMN submitted_config_hash VARCHAR(64) NULL AFTER submitted_playbook_version,
          ADD COLUMN approved_playbook_version INT NULL AFTER submitted_config_hash,
          ADD COLUMN approved_config_hash VARCHAR(64) NULL AFTER approved_playbook_version,
          ADD COLUMN budget_period_key VARCHAR(7) NULL AFTER approved_config_hash,
          ADD COLUMN budget_committed DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER budget_period_key,
          ADD COLUMN quota_committed INT NOT NULL DEFAULT 0 AFTER budget_committed;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_automation_runs' AND column_name = 'approved_config_hash') THEN
        ALTER TABLE promotion_automation_runs
          ADD COLUMN approved_config_hash VARCHAR(64) NOT NULL DEFAULT 'LEGACY_UNVERIFIED' AFTER playbook_version,
          ADD COLUMN estimated_unit_cost DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER quota_snapshot;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_audience_members' AND column_name = 'budget_reserved_amount') THEN
        ALTER TABLE promotion_audience_members
          ADD COLUMN budget_reserved_amount DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER wallet_public_id,
          ADD COLUMN budget_period_key VARCHAR(7) NULL AFTER budget_reserved_amount;
    END IF;
END$$
CALL harden_promotion_automation_20260820()$$
DROP PROCEDURE harden_promotion_automation_20260820$$
DELIMITER ;

-- Versions approved before config hashing are intentionally sent back for
-- review. This is fail-closed and never fabricates an approval hash.
UPDATE promotion_playbooks
SET status = 'DRAFT', submitted_by = NULL, submitted_at = NULL,
    approved_by = NULL, approved_at = NULL, updated_by = 'SYSTEM'
WHERE status IN ('PENDING_APPROVAL', 'ACTIVE')
  AND approved_config_hash IS NULL;

INSERT INTO promotion_playbooks (
  public_id, code, name, description, status, playbook_version, trigger_type,
  config_json, scope_json, budget_limit, quota_limit, version, created_by, updated_by)
SELECT UUID(), 'BIRTHDAY_REWARD', 'Quà sinh nhật',
  'Tự tìm thành viên đủ điều kiện và cấp quà sinh nhật một lần mỗi năm.',
  'DRAFT', 1, 'DAILY_SCHEDULE',
  JSON_OBJECT('match','EXACT_DAY','timezone','Asia/Ho_Chi_Minh','feb29Policy','FEB_28_NON_LEAP_YEAR',
    'validityDays',30,'annualPeriod','CALENDAR_YEAR','lateRegistrationCatchup',false,
    'accountStatus','ACTIVE','stackable',false),
  JSON_OBJECT('type','GLOBAL'), 20000000.00, 100000, 0, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM promotion_playbooks WHERE code = 'BIRTHDAY_REWARD');

INSERT INTO promotion_playbooks (
  public_id, code, name, description, status, playbook_version, trigger_type,
  config_json, scope_json, budget_limit, quota_limit, version, created_by, updated_by)
SELECT UUID(), 'SECOND_BOOKING_INCENTIVE', 'Khuyến khích booking lần hai',
  'Cấp một ưu đãi sau booking hợp lệ đầu tiên đã thanh toán và phát hành vé.',
  'DRAFT', 1, 'BOOKING_CONFIRMED',
  JSON_OBJECT('paidOnly',true,'ticketIssuedRequired',true,'firstValidBookingOnly',true,
    'excludeAnonymousCounter',true,'excludeTestBooking',true,'validityDays',21,'oncePerCustomer',true),
  JSON_OBJECT('type','GLOBAL'), 20000000.00, 100000, 0, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM promotion_playbooks WHERE code = 'SECOND_BOOKING_INCENTIVE');

-- Repair seed labels when an earlier mysql client session used latin1. This is
-- deliberately limited to untouched system drafts so operator edits are never overwritten.
UPDATE promotion_playbooks
SET name = 'Quà sinh nhật',
    description = 'Tự tìm thành viên đủ điều kiện và cấp quà sinh nhật một lần mỗi năm.'
WHERE code = 'BIRTHDAY_REWARD'
  AND status = 'DRAFT'
  AND campaign_public_id IS NULL
  AND promotion_public_id IS NULL
  AND created_by = 'SYSTEM';

UPDATE promotion_playbooks
SET name = 'Khuyến khích booking lần hai',
    description = 'Cấp một ưu đãi sau booking hợp lệ đầu tiên đã thanh toán và phát hành vé.'
WHERE code = 'SECOND_BOOKING_INCENTIVE'
  AND status = 'DRAFT'
  AND campaign_public_id IS NULL
  AND promotion_public_id IS NULL
  AND created_by = 'SYSTEM';

-- Correct the misleading demo semantics without changing any issued entitlement.
UPDATE promotion_campaigns c
JOIN promotions p ON p.campaign_public_id = c.public_id AND p.deleted_at IS NULL
SET c.name = 'Voucher giảm 10% – cấp thủ công',
    c.slug = 'voucher-giam-10-cap-thu-cong',
    p.name = 'Voucher giảm 10% – cấp thủ công'
WHERE c.name = 'Sinh nhật vui vẻ'
  AND (p.conditions_json = JSON_OBJECT() OR JSON_LENGTH(p.conditions_json) = 0);
