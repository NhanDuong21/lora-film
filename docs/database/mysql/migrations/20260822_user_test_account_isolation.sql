-- Explicit UAT recipient classification (MySQL 8+).
-- Run against user_db before deploying the matching User Service build.
-- The migration intentionally marks no account as test; that is an explicit
-- operational decision per environment.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_user_test_account_isolation_20260822$$
CREATE PROCEDURE migrate_user_test_account_isolation_20260822()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'users' AND column_name = 'is_test_account') THEN
        ALTER TABLE users
            ADD COLUMN is_test_account BOOLEAN NOT NULL DEFAULT FALSE AFTER is_deleted;
    END IF;
END$$
CALL migrate_user_test_account_isolation_20260822()$$
DROP PROCEDURE migrate_user_test_account_isolation_20260822$$
DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS index_user_test_account_isolation_20260822$$
CREATE PROCEDURE index_user_test_account_isolation_20260822()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
        AND table_name = 'users' AND index_name = 'idx_users_test_account') THEN
        CREATE INDEX idx_users_test_account
            ON users(is_test_account, status, account_type);
    END IF;
END$$
CALL index_user_test_account_isolation_20260822()$$
DROP PROCEDURE index_user_test_account_isolation_20260822$$
DELIMITER ;
