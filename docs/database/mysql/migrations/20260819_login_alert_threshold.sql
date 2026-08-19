-- Preserve historic failed-login events while removing one-event-one-alert noise.
-- New incidents are promoted by auth-service only after the configured threshold.
UPDATE audit_logs
SET severity = 'NORMAL',
    review_status = 'NOT_REQUIRED',
    reviewed_by = NULL,
    review_note = NULL,
    reviewed_at = NULL
WHERE action = 'LOGIN_FAILED_INVALID_PASSWORD'
  AND review_status = 'UNREVIEWED';
