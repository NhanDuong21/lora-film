-- Query indexes used by session enforcement and outbox polling.

CREATE INDEX idx_user_sessions_account_agent_active
    ON user_sessions (account_id, user_agent, is_active);

CREATE INDEX idx_refresh_tokens_account_revoked
    ON refresh_tokens (account_id, is_revoked);

CREATE INDEX idx_audit_logs_created
    ON audit_logs (created_at);
