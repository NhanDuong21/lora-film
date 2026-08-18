-- Adds encrypted, admin-managed SMTP sender credentials.
-- The table starts empty so the existing environment configuration remains active.
USE notification_db;

CREATE TABLE IF NOT EXISTS notification_email_provider_configs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(40) NOT NULL,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INT NOT NULL,
    sender_email VARCHAR(320) NOT NULL,
    app_password_encrypted VARCHAR(1000) NOT NULL,
    from_name VARCHAR(120) NOT NULL,
    smtp_auth_enabled BIT(1) NOT NULL DEFAULT b'1',
    starttls_enabled BIT(1) NOT NULL DEFAULT b'1',
    starttls_required BIT(1) NOT NULL DEFAULT b'1',
    connection_status VARCHAR(30) NOT NULL,
    last_tested_at DATETIME(6) NULL,
    updated_by VARCHAR(80) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_email_provider_config_key (config_key),
    CONSTRAINT ck_notification_email_provider_smtp_port
        CHECK (smtp_port BETWEEN 1 AND 65535)
) ENGINE=InnoDB;
