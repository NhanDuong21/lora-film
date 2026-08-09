-- Phân tách người lập yêu cầu hoàn tiền và người duyệt tại rạp.

ALTER TABLE payment_db.payment_refunds
    ADD COLUMN reviewed_by_account_id BIGINT NULL AFTER requested_by_account_id,
    ADD COLUMN reviewed_at DATETIME(6) NULL AFTER reviewed_by_account_id,
    ADD COLUMN review_note_sanitized TEXT NULL AFTER reviewed_at;

ALTER TABLE payment_db.payment_refunds
    DROP CHECK chk_payment_refunds_status;

ALTER TABLE payment_db.payment_refunds
    ADD CONSTRAINT chk_payment_refunds_status
      CHECK (status IN (
        'PENDING_APPROVAL', 'REQUESTED', 'PROCESSING', 'SUCCESS', 'FAILED',
        'REQUIRES_ACTION', 'CANCELLED', 'REJECTED'
      ));

CREATE INDEX idx_payment_refunds_review
    ON payment_db.payment_refunds(status, requested_at, reviewed_at);
