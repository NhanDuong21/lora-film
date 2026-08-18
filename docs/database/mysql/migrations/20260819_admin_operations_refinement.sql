-- Operational account lifecycle and reviewable audit metadata.
-- Run once after the base auth-service and user-service schemas.

USE auth_db;

ALTER TABLE password_reset_tokens
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'PASSWORD_RESET' AFTER is_used;

ALTER TABLE audit_logs
    ADD COLUMN result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' AFTER description,
    ADD COLUMN severity VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER result,
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED' AFTER severity,
    ADD COLUMN reviewed_by BIGINT NULL AFTER review_status,
    ADD COLUMN review_note VARCHAR(500) NULL AFTER reviewed_by,
    ADD COLUMN reviewed_at TIMESTAMP NULL AFTER review_note,
    ADD INDEX idx_auth_audit_attention (severity, review_status, created_at);

UPDATE audit_logs
SET result = 'FAILED'
WHERE action LIKE '%FAILED%' OR action LIKE '%REJECTED%'
   OR action LIKE '%BLOCKED%' OR action LIKE '%ERROR%';

UPDATE audit_logs
SET severity = 'REVIEW', review_status = 'UNREVIEWED'
WHERE action IN ('LOGIN_FAILED_INVALID_PASSWORD', 'UPDATE_ACCOUNT_ROLE',
                 'UPDATE_ACCOUNT_ACCESS_PROFILE', 'UPDATE_MANAGER_CINEMA_ASSIGNMENTS',
                 'UPDATE_ROLE', 'DELETE_ROLE', 'CREATE_PERMISSION',
                 'UPDATE_PERMISSION', 'DELETE_PERMISSION', 'ADMIN_REVOKED_ALL_SESSIONS');

UPDATE access_profiles
SET name = 'Kế toán kiểm soát',
    description = 'Duyệt độc lập, khóa đối soát, phê duyệt hoàn tiền và khóa kỳ kế toán.'
WHERE code = 'ACCOUNTING_CONTROL';

USE user_db;

ALTER TABLE audit_logs
    ADD COLUMN result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' AFTER details,
    ADD COLUMN severity VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER result,
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED' AFTER severity,
    ADD COLUMN reviewed_by BIGINT NULL AFTER review_status,
    ADD COLUMN review_note VARCHAR(500) NULL AFTER reviewed_by,
    ADD COLUMN reviewed_at TIMESTAMP NULL AFTER review_note,
    ADD INDEX idx_user_audit_attention (severity, review_status, created_at);

UPDATE audit_logs
SET result = 'FAILED'
WHERE action LIKE '%FAILED%' OR action LIKE '%REJECTED%'
   OR action LIKE '%BLOCKED%' OR action LIKE '%ERROR%';

UPDATE audit_logs
SET severity = 'REVIEW', review_status = 'UNREVIEWED'
WHERE action IN ('EMPLOYEE_RESIGNED', 'PII_ERASED',
                 'EMPLOYEE_DOCUMENT_DELETED', 'PAYROLL_CANCELLED');
