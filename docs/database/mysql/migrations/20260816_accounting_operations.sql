-- Hoàn thiện mô hình Kế toán vận hành: settlement, kiểm soát tiền mặt,
-- khóa kỳ, audit bất biến và phân tách quyền maker-checker.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE payment_db;

ALTER TABLE counter_cash_sessions
    ADD COLUMN verification_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SUBMITTED' AFTER variance_amount,
    ADD COLUMN verified_by_account_id BIGINT NULL AFTER verification_status,
    ADD COLUMN verified_at DATETIME(6) NULL AFTER verified_by_account_id,
    ADD COLUMN verification_note_sanitized VARCHAR(1000) NULL AFTER verified_at,
    ADD INDEX idx_counter_cash_sessions_verification (verification_status, closed_at);

UPDATE counter_cash_sessions
SET verification_status = CASE
    WHEN status = 'OPEN' THEN 'NOT_SUBMITTED'
    WHEN COALESCE(variance_amount, 0) = 0 THEN 'PENDING_VERIFICATION'
    ELSE 'DISCREPANCY_REVIEW'
END;

ALTER TABLE counter_cash_sessions
    ADD CONSTRAINT chk_counter_cash_sessions_verification_status CHECK (
        verification_status IN ('NOT_SUBMITTED','PENDING_VERIFICATION','DISCREPANCY_REVIEW','VERIFIED')
    );

CREATE TABLE IF NOT EXISTS settlement_batches (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  provider_code VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  batch_code VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
  cinema_public_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  source_file_name VARCHAR(255) NULL,
  status VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'IMPORTED',
  entry_count INT NOT NULL DEFAULT 0,
  matched_count INT NOT NULL DEFAULT 0,
  mismatch_count INT NOT NULL DEFAULT 0,
  gross_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  provider_net_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  bank_credit_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  created_by_account_id BIGINT NOT NULL,
  locked_by_account_id BIGINT NULL,
  locked_at DATETIME(6) NULL,
  note_sanitized VARCHAR(1000) NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_settlement_batches_public_id (public_id),
  UNIQUE KEY uk_settlement_provider_batch (provider_code, batch_code),
  KEY idx_settlement_batch_queue (status, period_end),
  KEY idx_settlement_batch_cinema (cinema_public_id, period_end),
  CONSTRAINT chk_settlement_batch_status CHECK (status IN ('IMPORTED','NEEDS_REVIEW','RECONCILED','LOCKED')),
  CONSTRAINT chk_settlement_batch_range CHECK (period_start <= period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS settlement_entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  settlement_batch_id BIGINT NOT NULL,
  payment_id BIGINT NULL,
  payment_transaction_code VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  provider_transaction_id VARCHAR(150) COLLATE utf8mb4_bin NOT NULL,
  bank_credit_reference VARCHAR(150) COLLATE utf8mb4_bin NULL,
  provider_gross_amount DECIMAL(15,2) NOT NULL,
  provider_fee_amount DECIMAL(15,2) NOT NULL,
  provider_net_amount DECIMAL(15,2) NOT NULL,
  bank_credit_amount DECIMAL(15,2) NOT NULL,
  status VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  mismatch_reason_sanitized VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_settlement_entry_provider_transaction (settlement_batch_id, provider_transaction_id),
  KEY idx_settlement_entries_payment (payment_id),
  KEY idx_settlement_entries_status (status),
  CONSTRAINT fk_settlement_entries_batch FOREIGN KEY (settlement_batch_id)
      REFERENCES settlement_batches(id) ON DELETE RESTRICT,
  CONSTRAINT fk_settlement_entries_payment FOREIGN KEY (payment_id)
      REFERENCES payments(id) ON DELETE RESTRICT,
  CONSTRAINT chk_settlement_entry_status CHECK (status IN ('MATCHED','MISMATCH','UNMATCHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_periods (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  period_code VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  scope_key VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  cinema_public_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  status VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'OPEN',
  created_by_account_id BIGINT NOT NULL,
  reconciled_by_account_id BIGINT NULL,
  reconciled_at DATETIME(6) NULL,
  locked_by_account_id BIGINT NULL,
  locked_at DATETIME(6) NULL,
  note_sanitized VARCHAR(1000) NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_accounting_period_public_id (public_id),
  UNIQUE KEY uk_accounting_period_scope (period_code, scope_key),
  KEY idx_accounting_period_status (status, period_end),
  CONSTRAINT chk_accounting_period_status CHECK (status IN ('OPEN','RECONCILED','LOCKED','ADJUSTMENT')),
  CONSTRAINT chk_accounting_period_range CHECK (period_start <= period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_audit_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  action_code VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  aggregate_type VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  aggregate_public_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  actor_account_id BIGINT NOT NULL,
  detail_sanitized VARCHAR(2000) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_accounting_audit_aggregate (aggregate_type, aggregate_public_id, created_at),
  KEY idx_accounting_audit_actor (actor_account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

USE auth_db;

INSERT INTO permissions (code, name, module, description)
VALUES
  ('ACCOUNTING_VIEW_ALL_CINEMAS', 'View all cinema accounting data', 'Accounting Operations', 'View accounting data across the cinema chain'),
  ('SETTLEMENT_IMPORT', 'Import settlement batches', 'Accounting Operations', 'Import and automatically match provider and bank settlement data'),
  ('SETTLEMENT_LOCK', 'Lock reconciled settlement batches', 'Accounting Control', 'Independently lock a fully reconciled settlement batch'),
  ('CASH_CLOSE_VERIFY', 'Verify counter cash close', 'Accounting Operations', 'Verify submitted cash drawer sessions and discrepancies'),
  ('REFUND_REQUEST', 'Request payment refunds', 'Accounting Operations', 'Create a refund request without executing it directly'),
  ('REFUND_APPROVE', 'Approve payment refunds', 'Accounting Control', 'Approve or reject refund requests independently'),
  ('ACCOUNTING_PERIOD_VIEW', 'View accounting periods', 'Accounting Operations', 'View accounting period status and blockers'),
  ('ACCOUNTING_PERIOD_CREATE', 'Open accounting periods', 'Accounting Operations', 'Open a period for operational accounting work'),
  ('ACCOUNTING_PERIOD_RECONCILE', 'Reconcile accounting periods', 'Accounting Operations', 'Confirm that all operational accounting items have been reconciled'),
  ('ACCOUNTING_PERIOD_CLOSE', 'Reconcile and close accounting periods', 'Accounting Control', 'Reconcile and lock an accounting period'),
  ('ACCOUNTING_EXPORT', 'Export accounting evidence', 'Accounting Operations', 'Export accounting data with filters and audit metadata'),
  ('AUDIT_VIEW', 'View accounting audit trail', 'Accounting Operations', 'View immutable accounting operation history'),
  ('PAYROLL_SUBMIT_PAYMENT', 'Submit payroll bank batch', 'Payroll Management', 'Send an approved payroll into a bank batch'),
  ('PAYROLL_RECONCILE', 'Reconcile payroll payment', 'Payroll Management', 'Match payroll payment with bank and accounting references'),
  ('PAYROLL_CANCEL', 'Cancel pending payroll', 'Payroll Management', 'Cancel a payroll before approval with a reason')
ON DUPLICATE KEY UPDATE
  name = VALUES(name), module = VALUES(module), description = VALUES(description);

INSERT INTO access_profiles (code, name, description)
VALUES ('ACCOUNTING_CONTROL', 'Kế toán kiểm soát',
        'Duyệt độc lập, khóa đối soát, phê duyệt hoàn tiền và khóa kỳ kế toán.')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), is_active = TRUE;

DELETE app
FROM access_profile_permissions app
JOIN access_profiles ap ON ap.id = app.access_profile_id
WHERE ap.code IN ('ACCOUNTING', 'ACCOUNTING_CONTROL');

INSERT INTO access_profile_permissions (access_profile_id, permission_id)
SELECT ap.id, p.id
FROM access_profiles ap
JOIN permissions p ON (
  (ap.code = 'ACCOUNTING' AND p.code IN (
    'PAYMENT_VIEW','PAYMENT_RECONCILE','ANALYTICS_VIEW',
    'PAYROLL_VIEW','PAYROLL_CREATE','PAYROLL_UPDATE','PAYROLL_SUBMIT_PAYMENT',
    'PAYROLL_RECONCILE','PAYROLL_CANCEL','SETTLEMENT_IMPORT','CASH_CLOSE_VERIFY',
    'REFUND_REQUEST','ACCOUNTING_PERIOD_VIEW','ACCOUNTING_PERIOD_CREATE','ACCOUNTING_PERIOD_RECONCILE',
    'ACCOUNTING_EXPORT','AUDIT_VIEW',
    'ACCOUNTING_VIEW_ALL_CINEMAS'))
  OR
  (ap.code = 'ACCOUNTING_CONTROL' AND p.code IN (
    'PAYMENT_VIEW','PAYMENT_RECONCILE','ANALYTICS_VIEW','PAYROLL_VIEW','PAYROLL_APPROVE',
    'SETTLEMENT_LOCK','REFUND_APPROVE','ACCOUNTING_PERIOD_VIEW','ACCOUNTING_PERIOD_CLOSE',
    'ACCOUNTING_EXPORT','AUDIT_VIEW','ACCOUNTING_VIEW_ALL_CINEMAS'))
);

INSERT IGNORE INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';
