-- Resource ownership and approval-policy snapshots for promotions (MySQL 8+).
-- Existing campaigns stay GLOBAL and remain invisible to cinema managers until
-- an administrator explicitly creates or migrates a cinema-scoped campaign.

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_promotion_manager_scope_20260820$$
CREATE PROCEDURE migrate_promotion_manager_scope_20260820()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_campaigns'
          AND column_name = 'scope_type') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN scope_type VARCHAR(30) NOT NULL DEFAULT 'GLOBAL'
            AFTER legal_status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_campaigns'
          AND column_name = 'cinema_scope_json') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN cinema_scope_json JSON NULL AFTER scope_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_campaigns'
          AND column_name = 'approval_threshold_applied') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN approval_threshold_applied DECIMAL(18,2) NULL
            AFTER approved_by;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_campaigns'
          AND column_name = 'approval_policy_version') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN approval_policy_version VARCHAR(50) NULL
            AFTER approval_threshold_applied;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'promotion_campaigns'
          AND column_name = 'required_approval_capability') THEN
        ALTER TABLE promotion_campaigns
            ADD COLUMN required_approval_capability VARCHAR(100) NULL
            AFTER approval_policy_version;
    END IF;

    UPDATE promotion_campaigns
       SET scope_type = 'GLOBAL'
     WHERE scope_type IS NULL OR scope_type = '';
END$$

CALL migrate_promotion_manager_scope_20260820()$$
DROP PROCEDURE migrate_promotion_manager_scope_20260820$$

DELIMITER ;
