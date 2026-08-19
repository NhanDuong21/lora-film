-- Promotion governance refinement (MySQL 8+).
-- Run against promotion_db before deploying the matching service build.

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_promotion_governance_20260819$$
CREATE PROCEDURE migrate_promotion_governance_20260819()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND column_name = 'release_reason_type') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN release_reason_type VARCHAR(50) NULL AFTER rollback_reason;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND column_name = 'released_at') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN released_at DATETIME(6) NULL AFTER release_reason_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND column_name = 'released_by') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN released_by VARCHAR(100) NULL AFTER released_at;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND column_name = 'source_service') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN source_service VARCHAR(100) NULL AFTER released_by;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND column_name = 'source_reference') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN source_reference VARCHAR(100) NULL AFTER source_service;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND column_name = 'reason_detail') THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN reason_detail VARCHAR(1000) NULL AFTER source_reference;
    END IF;

    -- Canonicalize all legacy cancellation representations to RELEASED.
    UPDATE promotion_reservations
       SET status = 'RELEASED',
           rollback_at = COALESCE(rollback_at, updated_at),
           rollback_reason = COALESCE(NULLIF(rollback_reason, ''), 'Legacy cancellation')
     WHERE status = 'CANCELLED';

    UPDATE promotion_reservations
       SET release_reason_type = COALESCE(release_reason_type, 'SYSTEM_COMPENSATION'),
           released_at = COALESCE(released_at, rollback_at, updated_at),
           released_by = COALESCE(NULLIF(released_by, ''), NULLIF(updated_by, ''), 'SYSTEM'),
           source_service = COALESCE(NULLIF(source_service, ''), 'MIGRATION'),
           reason_detail = COALESCE(NULLIF(reason_detail, ''), rollback_reason, 'Legacy release')
     WHERE status = 'RELEASED';

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND constraint_name = 'chk_reservation_release_reason_type') THEN
        ALTER TABLE promotion_reservations
            ADD CONSTRAINT chk_reservation_release_reason_type CHECK (
                release_reason_type IS NULL OR release_reason_type IN (
                    'PAYMENT_FAILED', 'PAYMENT_TIMEOUT',
                    'CUSTOMER_CANCELLED_BOOKING', 'STAFF_CANCELLED_BOOKING',
                    'BOOKING_EXPIRED', 'CAMPAIGN_PAUSED',
                    'CAMPAIGN_KILL_SWITCH', 'SYSTEM_COMPENSATION'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'promotion_reservations'
          AND constraint_name = 'chk_reservation_release_metadata') THEN
        ALTER TABLE promotion_reservations
            ADD CONSTRAINT chk_reservation_release_metadata CHECK (
                status <> 'RELEASED' OR (
                    release_reason_type IS NOT NULL AND released_at IS NOT NULL
                    AND released_by IS NOT NULL AND source_service IS NOT NULL));
    END IF;
END$$

CALL migrate_promotion_governance_20260819()$$
DROP PROCEDURE migrate_promotion_governance_20260819$$

DELIMITER ;
