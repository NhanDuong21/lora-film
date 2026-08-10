-- Migration: Add status column to booking_idempotency_keys table
ALTER TABLE booking_idempotency_keys ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING' COMMENT 'Trạng thái xử lý (PROCESSING, COMPLETED, FAILED)' AFTER endpoint;
