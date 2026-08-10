-- LoraFilm notification-service schema for MySQL 8.
-- DESTRUCTIVE: this intentionally replaces the legacy notification_db.
-- Template content is never stored in this database.

DROP DATABASE IF EXISTS notification_db;
CREATE DATABASE notification_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE notification_db;

CREATE TABLE notification_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    source_service VARCHAR(80) NOT NULL,
    source_event_id VARCHAR(80) NULL,
    event_type VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(80) NULL,
    causation_id VARCHAR(80) NULL,
    template_key VARCHAR(100) NOT NULL,
    template_commit_sha VARCHAR(64) NULL,
    template_version VARCHAR(40) NULL,
    locale VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    scheduled_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    status VARCHAR(30) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    is_test BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_request_public_id (public_id),
    UNIQUE KEY uk_notification_request_idempotency (idempotency_key),
    KEY ix_notification_request_source_event (source_service, source_event_id),
    KEY ix_notification_request_status_schedule (status, scheduled_at),
    KEY ix_notification_request_correlation (correlation_id),
    KEY ix_notification_request_template_created (template_key, created_at),
    CONSTRAINT ck_notification_request_status CHECK (status IN (
        'ACCEPTED', 'PROCESSING', 'COMPLETED', 'PARTIALLY_FAILED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT ck_notification_request_payload_json CHECK (JSON_VALID(payload_json)),
    CONSTRAINT ck_notification_request_locale CHECK (locale REGEXP '^[a-z]{2}-[A-Z]{2}$')
) ENGINE=InnoDB;

CREATE TABLE notification_recipients (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_request_id BIGINT UNSIGNED NOT NULL,
    user_public_id VARCHAR(80) NULL,
    recipient_type VARCHAR(30) NOT NULL,
    email_encrypted VARCHAR(1000) NULL,
    phone_encrypted VARCHAR(1000) NULL,
    web_push_subscription_encrypted VARCHAR(4000) NULL,
    locale VARCHAR(20) NOT NULL,
    timezone VARCHAR(60) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_notification_recipient_request (notification_request_id),
    KEY ix_notification_recipient_user (user_public_id),
    CONSTRAINT fk_notification_recipient_request
        FOREIGN KEY (notification_request_id) REFERENCES notification_requests (id)
) ENGINE=InnoDB;

CREATE TABLE notification_deliveries (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    notification_request_id BIGINT UNSIGNED NOT NULL,
    notification_recipient_id BIGINT UNSIGNED NOT NULL,
    channel VARCHAR(20) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_message_id VARCHAR(200) NULL,
    failure_category VARCHAR(40) NULL,
    failure_code VARCHAR(80) NULL,
    failure_message VARCHAR(500) NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    delivered_at DATETIME(6) NULL,
    failed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_delivery_public_id (public_id),
    KEY ix_notification_delivery_request (notification_request_id),
    KEY ix_notification_delivery_recipient (notification_recipient_id),
    KEY ix_notification_delivery_worker (status, next_retry_at, created_at),
    KEY ix_notification_delivery_provider_status (provider, status),
    CONSTRAINT fk_notification_delivery_request
        FOREIGN KEY (notification_request_id) REFERENCES notification_requests (id),
    CONSTRAINT fk_notification_delivery_recipient
        FOREIGN KEY (notification_recipient_id) REFERENCES notification_recipients (id),
    CONSTRAINT ck_notification_delivery_attempt_count CHECK (attempt_count >= 0)
) ENGINE=InnoDB;

CREATE TABLE notification_delivery_attempts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_delivery_id BIGINT UNSIGNED NOT NULL,
    attempt_number INT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    failure_category VARCHAR(40) NULL,
    failure_code VARCHAR(80) NULL,
    failure_message VARCHAR(500) NULL,
    duration_ms BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_attempt_number
        (notification_delivery_id, attempt_number),
    KEY ix_notification_attempt_created (created_at),
    CONSTRAINT fk_notification_attempt_delivery
        FOREIGN KEY (notification_delivery_id) REFERENCES notification_deliveries (id),
    CONSTRAINT ck_notification_attempt_number CHECK (attempt_number > 0),
    CONSTRAINT ck_notification_attempt_duration CHECK (duration_ms >= 0)
) ENGINE=InnoDB;

CREATE TABLE notification_event_inbox (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_service VARCHAR(80) NOT NULL,
    source_event_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INT NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    failure_message VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_inbox_source_event
        (source_service, source_event_id),
    KEY ix_notification_inbox_status_received (status, received_at),
    CONSTRAINT ck_notification_inbox_payload_json CHECK (JSON_VALID(payload_json)),
    CONSTRAINT ck_notification_inbox_version CHECK (event_version > 0)
) ENGINE=InnoDB;

CREATE TABLE notification_outbox (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    aggregate_public_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_outbox_event_id (event_id),
    KEY ix_notification_outbox_worker (status, next_retry_at, created_at),
    CONSTRAINT ck_notification_outbox_payload_json CHECK (JSON_VALID(payload_json))
) ENGINE=InnoDB;

CREATE TABLE notification_preferences (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_public_id VARCHAR(80) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_preference_user_channel_category
        (user_public_id, channel, category)
) ENGINE=InnoDB;

CREATE TABLE notification_suppressions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    destination_hash CHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    reason VARCHAR(80) NOT NULL,
    source VARCHAR(80) NOT NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_suppression_destination
        (destination_hash, channel),
    KEY ix_notification_suppression_expiry (expires_at)
) ENGINE=InnoDB;

CREATE TABLE in_app_notifications (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    notification_delivery_id BIGINT UNSIGNED NOT NULL,
    user_public_id VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    deep_link VARCHAR(500) NULL,
    read_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_app_public_id (public_id),
    UNIQUE KEY uk_in_app_delivery (notification_delivery_id),
    KEY ix_in_app_user_created (user_public_id, created_at),
    KEY ix_in_app_user_unread (user_public_id, read_at),
    CONSTRAINT fk_in_app_delivery
        FOREIGN KEY (notification_delivery_id) REFERENCES notification_deliveries (id)
) ENGINE=InnoDB;

CREATE TABLE web_push_subscriptions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    user_public_id VARCHAR(80) NOT NULL,
    endpoint_encrypted VARCHAR(3000) NOT NULL,
    p256dh_encrypted VARCHAR(1000) NOT NULL,
    auth_encrypted VARCHAR(1000) NOT NULL,
    user_agent_hash CHAR(64) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_web_push_public_id (public_id),
    KEY ix_web_push_user_active (user_public_id, active)
) ENGINE=InnoDB;

CREATE TABLE notification_audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_public_id VARCHAR(80) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_public_id VARCHAR(150) NOT NULL,
    metadata_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_notification_audit_target
        (target_type, target_public_id, created_at),
    KEY ix_notification_audit_actor (actor_public_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE notification_scheduled_jobs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    notification_request_id BIGINT UNSIGNED NULL,
    job_type VARCHAR(80) NOT NULL,
    schedule_expression VARCHAR(120) NULL,
    run_at DATETIME(6) NULL,
    status VARCHAR(30) NOT NULL,
    lock_owner VARCHAR(100) NULL,
    lock_until DATETIME(6) NULL,
    last_run_at DATETIME(6) NULL,
    next_run_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_scheduled_job_public_id (public_id),
    KEY ix_notification_scheduled_job_worker (status, next_run_at, lock_until),
    CONSTRAINT fk_notification_job_request
        FOREIGN KEY (notification_request_id) REFERENCES notification_requests (id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE notification_dead_letters (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_delivery_id BIGINT UNSIGNED NOT NULL,
    reason VARCHAR(80) NOT NULL,
    failure_message VARCHAR(500) NULL,
    reprocess_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    reprocessed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_dead_letter_delivery (notification_delivery_id),
    KEY ix_notification_dead_letter_created (created_at),
    CONSTRAINT fk_notification_dead_letter_delivery
        FOREIGN KEY (notification_delivery_id) REFERENCES notification_deliveries (id),
    CONSTRAINT ck_notification_dead_letter_reprocess_count
        CHECK (reprocess_count >= 0)
) ENGINE=InnoDB;

-- Operational development data only; no template bodies, subjects, schemas, or samples.
INSERT INTO notification_preferences (
    user_public_id, channel, category, enabled, updated_at
) VALUES
    ('dev-customer-marketing-opt-out', 'EMAIL', 'MARKETING', b'0', UTC_TIMESTAMP(6)),
    ('dev-customer-marketing-opt-out', 'IN_APP', 'MARKETING', b'0', UTC_TIMESTAMP(6)),
    ('dev-customer-transactional', 'EMAIL', 'MARKETING', b'1', UTC_TIMESTAMP(6));

-- Database users and credentials are provisioned by Docker Compose or the
-- local database operator, never by this schema file.
