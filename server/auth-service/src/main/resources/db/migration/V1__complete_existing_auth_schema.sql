-- Additive compatibility migration for the existing auth_db schema.
-- Existing tables and data are preserved; no destructive operation is used.

CREATE TABLE IF NOT EXISTS account_providers
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_account_providers_identity UNIQUE (provider, provider_user_id),
    CONSTRAINT uk_account_providers_account_provider UNIQUE (account_id, provider),
    CONSTRAINT fk_account_providers_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE user_sessions
    ADD COLUMN refresh_token_id BIGINT NULL AFTER account_id,
    ADD COLUMN device_name VARCHAR(150) NULL AFTER user_agent,
    ADD COLUMN last_active_at DATETIME(6) NULL AFTER device_name,
    ADD CONSTRAINT fk_user_sessions_refresh_token
        FOREIGN KEY (refresh_token_id) REFERENCES refresh_tokens (id) ON DELETE SET NULL;

CREATE INDEX idx_user_sessions_account_active
    ON user_sessions (account_id, is_active);

CREATE INDEX idx_user_sessions_access_token_hash
    ON user_sessions (access_token_hash);

ALTER TABLE outbox_messages
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER processed,
    ADD COLUMN next_attempt_at DATETIME(6) NULL AFTER attempt_count,
    ADD COLUMN processed_at DATETIME(6) NULL AFTER next_attempt_at,
    ADD COLUMN last_error VARCHAR(1000) NULL AFTER processed_at;

CREATE INDEX idx_auth_outbox_ready
    ON outbox_messages (processed, next_attempt_at, created_at);
