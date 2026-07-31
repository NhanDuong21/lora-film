-- Partner, dynamic configuration and inbound integration hardening.
-- All changes are additive and preserve existing promotion data.

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_promotion_partner_configuration_20260729$$
CREATE PROCEDURE migrate_promotion_partner_configuration_20260729()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'partners' AND column_name = 'version') THEN
        ALTER TABLE partners ADD COLUMN version INT NOT NULL DEFAULT 1 AFTER settlement_cycle;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'partner_settlements' AND column_name = 'version') THEN
        ALTER TABLE partner_settlements ADD COLUMN version INT NOT NULL DEFAULT 1 AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'partner_settlements' AND column_name = 'settlement_rule') THEN
        ALTER TABLE partner_settlements
            ADD COLUMN settlement_rule VARCHAR(50) NOT NULL DEFAULT 'PERCENTAGE_OF_DISCOUNT' AFTER currency,
            ADD COLUMN partner_percentage DECIMAL(5,2) NOT NULL DEFAULT 0 AFTER settlement_rule,
            ADD COLUMN fixed_amount_per_redemption DECIMAL(18,2) NULL AFTER partner_percentage;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'partner_settlements' AND index_name = 'idx_settlement_period') THEN
        CREATE INDEX idx_settlement_period ON partner_settlements
            (partner_public_id, settlement_period_from, settlement_period_to);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_configurations' AND column_name = 'version') THEN
        ALTER TABLE promotion_configurations ADD COLUMN version INT NOT NULL DEFAULT 1 AFTER editable;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_configurations' AND column_name = 'requires_restart') THEN
        ALTER TABLE promotion_configurations
            ADD COLUMN requires_restart BOOLEAN NOT NULL DEFAULT FALSE AFTER editable,
            ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER requires_restart,
            ADD COLUMN metadata_json JSON NULL AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'promotion_configurations' AND index_name = 'idx_configuration_status') THEN
        CREATE INDEX idx_configuration_status ON promotion_configurations (status);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'promotion_campaigns' AND column_name = 'partner_public_id') THEN
        ALTER TABLE promotion_campaigns ADD COLUMN partner_public_id CHAR(36) NULL AFTER funding_source;
        CREATE INDEX idx_campaign_partner ON promotion_campaigns (partner_public_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'coupons' AND column_name = 'partner_public_id') THEN
        ALTER TABLE coupons ADD COLUMN partner_public_id CHAR(36) NULL AFTER campaign_public_id;
        CREATE INDEX idx_coupon_partner ON coupons (partner_public_id);
    END IF;
    UPDATE coupons c
    INNER JOIN promotion_campaigns p ON p.public_id = c.campaign_public_id
    SET c.partner_public_id = p.partner_public_id
    WHERE c.partner_public_id IS NULL AND p.partner_public_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'vouchers' AND column_name = 'partner_public_id') THEN
        ALTER TABLE vouchers ADD COLUMN partner_public_id CHAR(36) NULL AFTER campaign_public_id;
        CREATE INDEX idx_voucher_partner ON vouchers (partner_public_id);
    END IF;
    UPDATE vouchers v
    INNER JOIN promotion_campaigns p ON p.public_id = v.campaign_public_id
    SET v.partner_public_id = p.partner_public_id
    WHERE v.partner_public_id IS NULL AND p.partner_public_id IS NOT NULL;

    CREATE TABLE IF NOT EXISTS promotion_integration_events (
        id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        public_id CHAR(36) NOT NULL,
        source_service VARCHAR(60) NOT NULL,
        event_id VARCHAR(150) NOT NULL,
        event_type VARCHAR(150) NOT NULL,
        schema_version VARCHAR(30) NOT NULL,
        correlation_id VARCHAR(100) NULL,
        trace_id VARCHAR(100) NULL,
        payload JSON NOT NULL,
        processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
        retry_count INT NOT NULL DEFAULT 0,
        next_retry_at DATETIME(6) NULL,
        last_error VARCHAR(4000) NULL,
        processed_at DATETIME(6) NULL,
        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
        CONSTRAINT uk_integration_event_public UNIQUE (public_id),
        CONSTRAINT uk_integration_event_source_id UNIQUE (source_service, event_id)
    );
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'promotion_integration_events' AND index_name = 'idx_integration_event_status') THEN
        CREATE INDEX idx_integration_event_status ON promotion_integration_events
            (processing_status, next_retry_at, created_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'promotion_integration_events' AND index_name = 'idx_integration_event_type') THEN
        CREATE INDEX idx_integration_event_type ON promotion_integration_events (event_type, created_at);
    END IF;

    CREATE TABLE IF NOT EXISTS promotion_scheduler_job_executions (
        id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
        public_id CHAR(36) NOT NULL,
        job_name VARCHAR(100) NOT NULL,
        trigger_type VARCHAR(30) NOT NULL,
        status VARCHAR(30) NOT NULL,
        instance_id VARCHAR(100) NOT NULL,
        started_at DATETIME(6) NOT NULL,
        finished_at DATETIME(6) NULL,
        processed_count INT NOT NULL DEFAULT 0,
        error_message VARCHAR(4000) NULL,
        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        CONSTRAINT uk_scheduler_execution_public UNIQUE (public_id)
    );
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'promotion_scheduler_job_executions' AND index_name = 'idx_scheduler_execution_job') THEN
        CREATE INDEX idx_scheduler_execution_job ON promotion_scheduler_job_executions
            (job_name, started_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'promotion_scheduler_job_executions' AND index_name = 'idx_scheduler_execution_status') THEN
        CREATE INDEX idx_scheduler_execution_status ON promotion_scheduler_job_executions
            (status, started_at);
    END IF;

    CREATE TABLE IF NOT EXISTS promotion_scheduler_locks (
        job_name VARCHAR(100) PRIMARY KEY,
        owner VARCHAR(100) NOT NULL,
        locked_until DATETIME(6) NOT NULL,
        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
    );

    -- Seed the known lock rows so the first scheduler invocation on a
    -- multi-instance deployment never races on an INSERT.
    INSERT IGNORE INTO promotion_scheduler_locks (job_name, owner, locked_until)
    VALUES
        ('campaigns-expire', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('coupons-expire', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('vouchers-expire', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('reservations-expire', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('outbox-publish', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('outbox-retry', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('integration-retry', 'MIGRATION', '1970-01-01 00:00:00.000000'),
        ('cache-refresh', 'MIGRATION', '1970-01-01 00:00:00.000000');
END$$

CALL migrate_promotion_partner_configuration_20260729()$$
DROP PROCEDURE migrate_promotion_partner_configuration_20260729$$

DELIMITER ;
