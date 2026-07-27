-- Booking Payment Readiness contract v1.
-- Payment Service owns provider attempts and settlement. Booking stores only
-- normalized receipts, Booking lifecycle state, and reconciliation work.

ALTER TABLE booking_payment_events
    ADD COLUMN payment_public_id VARCHAR(36) NULL AFTER payment_id,
    ADD COLUMN schema_version VARCHAR(20) NOT NULL DEFAULT '1.0' AFTER payment_public_id,
    ADD COLUMN payload_hash VARCHAR(64) NULL AFTER response_payload,
    ADD COLUMN processing_outcome VARCHAR(40) NOT NULL DEFAULT 'ACCEPTED' AFTER payload_hash,
    ADD COLUMN processing_error_code VARCHAR(100) NULL AFTER processing_outcome,
    ADD COLUMN reconciliation_task_public_id VARCHAR(36) NULL AFTER processing_error_code,
    ADD INDEX idx_payment_public (payment_public_id),
    ADD INDEX idx_payment_processing (processing_outcome, created_at);

ALTER TABLE booking_reconciliation_tasks
    ADD COLUMN payment_event_id BIGINT NULL AFTER booking_id,
    ADD COLUMN expected_currency VARCHAR(10) NULL AFTER actual_amount,
    ADD COLUMN actual_currency VARCHAR(10) NULL AFTER expected_currency,
    ADD CONSTRAINT uk_reconciliation_payment_event UNIQUE (payment_event_id),
    ADD CONSTRAINT fk_reconciliation_payment_event
        FOREIGN KEY (payment_event_id) REFERENCES booking_payment_events(id);

-- Historical Payment events have no canonical payload hash or public Payment
-- identity. The application uses a strict legacy field comparison for those
-- rows; every newly received event persists the full v1 hash.
