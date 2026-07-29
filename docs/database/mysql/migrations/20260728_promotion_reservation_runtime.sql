-- Production hardening for promotion reservation runtime.
-- MySQL 8+, safe for both the original schema and the current canonical schema.

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_promotion_runtime_20260728$$
CREATE PROCEDURE migrate_promotion_runtime_20260728()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND column_name = 'customer_phone'
    ) THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN customer_phone VARCHAR(20) NULL
                COMMENT 'Verified E.164 phone used for coupon quota'
                AFTER user_public_id;
    END IF;

    -- Backfill the legacy JSON field before canonicalizing the phone.
    UPDATE promotion_reservations
    SET customer_phone = NULLIF(
            JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.customerPhone')),
            'null'
        )
    WHERE customer_phone IS NULL
      AND metadata_json IS NOT NULL
      AND JSON_TYPE(metadata_json) = 'OBJECT';

    UPDATE promotion_reservations
    SET customer_phone = NULLIF(
            JSON_UNQUOTE(
                JSON_EXTRACT(
                    CAST(JSON_UNQUOTE(metadata_json) AS JSON),
                    '$.customerPhone'
                )
            ),
            'null'
        )
    WHERE customer_phone IS NULL
      AND metadata_json IS NOT NULL
      AND JSON_TYPE(metadata_json) = 'STRING'
      AND JSON_VALID(JSON_UNQUOTE(metadata_json));

    UPDATE promotion_reservations
    SET customer_phone = REGEXP_REPLACE(customer_phone, '[[:space:]().-]', '')
    WHERE customer_phone IS NOT NULL;

    UPDATE promotion_reservations
    SET customer_phone = CASE
        WHEN customer_phone REGEXP '^0[0-9]{9}$'
            THEN CONCAT('+84', SUBSTRING(customer_phone, 2))
        WHEN customer_phone REGEXP '^84[0-9]{9}$'
            THEN CONCAT('+', customer_phone)
        WHEN customer_phone REGEXP '^00[1-9][0-9]{7,14}$'
            THEN CONCAT('+', SUBSTRING(customer_phone, 3))
        WHEN customer_phone REGEXP '^\\+[1-9][0-9]{7,14}$'
            THEN customer_phone
        ELSE NULL
    END
    WHERE customer_phone IS NOT NULL;

    -- Normalize legacy terminal rows before lifecycle checks are installed.
    UPDATE promotion_reservations
    SET status = 'RELEASED',
        rollback_at = COALESCE(rollback_at, cancelled_at, updated_at),
        rollback_reason = COALESCE(
            NULLIF(rollback_reason, ''),
            NULLIF(cancelled_reason, ''),
            'Legacy reservation release'
        ),
        cancelled_at = NULL,
        cancelled_reason = NULL
    WHERE status = 'CANCELLED'
      AND rollback_at IS NOT NULL;

    UPDATE promotion_reservations
    SET cancelled_at = COALESCE(cancelled_at, updated_at),
        cancelled_reason = COALESCE(
            NULLIF(cancelled_reason, ''),
            'Legacy reservation cancellation'
        ),
        rollback_at = NULL,
        rollback_reason = NULL
    WHERE status = 'CANCELLED';

    UPDATE promotion_reservations
    SET confirmed_at = NULL,
        cancelled_at = NULL,
        cancelled_reason = NULL,
        rollback_at = NULL,
        rollback_reason = NULL
    WHERE status IN ('ACTIVE', 'EXPIRED');

    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND constraint_name = 'chk_reservation_single_benefit'
    ) THEN
        ALTER TABLE promotion_reservations
            DROP CHECK chk_reservation_single_benefit;
    END IF;

    ALTER TABLE promotion_reservations
        ADD CONSTRAINT chk_reservation_single_benefit CHECK (
            (
                reservation_type = 'COUPON'
                AND coupon_public_id IS NOT NULL
                AND voucher_public_id IS NULL
            )
            OR (
                reservation_type = 'VOUCHER'
                AND coupon_public_id IS NULL
                AND voucher_public_id IS NOT NULL
            )
        );

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND constraint_name = 'chk_reservation_status'
    ) THEN
        ALTER TABLE promotion_reservations
            ADD CONSTRAINT chk_reservation_status CHECK (
                status IN ('ACTIVE', 'COMPLETED', 'RELEASED', 'CANCELLED', 'EXPIRED')
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND constraint_name = 'chk_reservation_lifecycle'
    ) THEN
        ALTER TABLE promotion_reservations
            ADD CONSTRAINT chk_reservation_lifecycle CHECK (
                (
                    status = 'ACTIVE'
                    AND confirmed_at IS NULL
                    AND cancelled_at IS NULL
                    AND rollback_at IS NULL
                )
                OR (
                    status = 'COMPLETED'
                    AND confirmed_at IS NOT NULL
                    AND payment_public_id IS NOT NULL
                    AND cancelled_at IS NULL
                    AND rollback_at IS NULL
                )
                OR (
                    status = 'RELEASED'
                    AND confirmed_at IS NULL
                    AND cancelled_at IS NULL
                    AND rollback_at IS NOT NULL
                    AND rollback_reason IS NOT NULL
                )
                OR (
                    status = 'CANCELLED'
                    AND confirmed_at IS NULL
                    AND cancelled_at IS NOT NULL
                    AND cancelled_reason IS NOT NULL
                    AND rollback_at IS NULL
                )
                OR (
                    status = 'EXPIRED'
                    AND confirmed_at IS NULL
                    AND cancelled_at IS NULL
                    AND rollback_at IS NULL
                )
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND index_name = 'idx_reservation_coupon_user_active'
    ) THEN
        CREATE INDEX idx_reservation_coupon_user_active
            ON promotion_reservations (
                coupon_public_id, user_public_id, status, reservation_expired_at
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND index_name = 'idx_reservation_coupon_phone_active'
    ) THEN
        CREATE INDEX idx_reservation_coupon_phone_active
            ON promotion_reservations (
                coupon_public_id, customer_phone, status, reservation_expired_at
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND index_name = 'idx_reservation_history'
    ) THEN
        CREATE INDEX idx_reservation_history
            ON promotion_reservations (status, reservation_type, created_at);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND index_name = 'idx_reservation_created'
    ) THEN
        CREATE INDEX idx_reservation_created
            ON promotion_reservations (created_at);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND column_name = 'reservation_scope_key'
    ) THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN reservation_scope_key VARCHAR(80) NULL
                COMMENT 'One effective benefit per order/booking scope'
                AFTER customer_phone;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND column_name = 'expiration_attempts'
    ) THEN
        ALTER TABLE promotion_reservations
            ADD COLUMN expiration_attempts INT NOT NULL DEFAULT 0
                AFTER metadata_json,
            ADD COLUMN expiration_last_attempt_at DATETIME(6) NULL
                AFTER expiration_attempts,
            ADD COLUMN expiration_next_attempt_at DATETIME(6) NULL
                AFTER expiration_last_attempt_at,
            ADD COLUMN expiration_error VARCHAR(1000) NULL
                AFTER expiration_next_attempt_at;
    END IF;

    -- Keep one canonical historical scope. Older duplicate completed rows remain
    -- immutable history, while the canonical row prevents a new benefit on that checkout.
    UPDATE promotion_reservations target
    JOIN (
        SELECT public_id,
               CONCAT(
                   CASE WHEN order_public_id IS NOT NULL THEN 'ORDER:' ELSE 'BOOKING:' END,
                   COALESCE(order_public_id, booking_public_id)
               ) AS scope_key
        FROM (
            SELECT public_id,
                   order_public_id,
                   booking_public_id,
                   ROW_NUMBER() OVER (
                       PARTITION BY COALESCE(
                           CONCAT('ORDER:', order_public_id),
                           CONCAT('BOOKING:', booking_public_id)
                       )
                       ORDER BY
                           CASE WHEN status = 'COMPLETED' THEN 0 ELSE 1 END,
                           created_at DESC,
                           id DESC
                   ) AS row_number_in_scope
            FROM promotion_reservations
            WHERE status IN ('ACTIVE', 'COMPLETED')
              AND (order_public_id IS NOT NULL OR booking_public_id IS NOT NULL)
        ) ranked
        WHERE row_number_in_scope = 1
    ) canonical ON canonical.public_id = target.public_id
    SET target.reservation_scope_key = canonical.scope_key
    WHERE target.reservation_scope_key IS NULL;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND index_name = 'uk_promotion_reservation_scope'
    ) THEN
        CREATE UNIQUE INDEX uk_promotion_reservation_scope
            ON promotion_reservations (reservation_scope_key);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_reservations'
          AND index_name = 'idx_reservation_expiration_due'
    ) THEN
        CREATE INDEX idx_reservation_expiration_due
            ON promotion_reservations (
                status, reservation_expired_at, expiration_next_attempt_at
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'outbox_events'
          AND column_name = 'processing_started_at'
    ) THEN
        ALTER TABLE outbox_events
            ADD COLUMN processing_started_at DATETIME(6) NULL
                AFTER next_retry_at,
            ADD COLUMN processing_owner VARCHAR(100) NULL
                AFTER processing_started_at;
    END IF;

    UPDATE outbox_events
    SET publish_status = 'PENDING',
        processing_started_at = NULL,
        processing_owner = NULL,
        next_retry_at = COALESCE(next_retry_at, CURRENT_TIMESTAMP(6))
    WHERE publish_status = 'PROCESSING';

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'outbox_events'
          AND index_name = 'idx_outbox_claim'
    ) THEN
        CREATE INDEX idx_outbox_claim
            ON outbox_events (
                publish_status, next_retry_at, processing_started_at, created_at
            );
    END IF;

    UPDATE promotion_idempotency_keys
    SET client_id = 'LEGACY'
    WHERE client_id IS NULL OR TRIM(client_id) = '';

    ALTER TABLE promotion_idempotency_keys
        MODIFY COLUMN client_id VARCHAR(100) NOT NULL
            COMMENT 'Authenticated calling service';

    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_idempotency_keys'
          AND index_name = 'uk_idempotency_key'
    ) THEN
        ALTER TABLE promotion_idempotency_keys
            DROP INDEX uk_idempotency_key;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'promotion_idempotency_keys'
          AND index_name = 'uk_idempotency_scope'
    ) THEN
        CREATE UNIQUE INDEX uk_idempotency_scope
            ON promotion_idempotency_keys (client_id, api_name, idempotency_key);
    END IF;
END$$

CALL migrate_promotion_runtime_20260728()$$
DROP PROCEDURE migrate_promotion_runtime_20260728$$

DELIMITER ;
