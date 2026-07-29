-- Manual migration. Do not use Flyway/Liquibase.
USE `movie_db`;

CREATE TABLE showtime_refund_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    showtime_public_id CHAR(36) NOT NULL,
    cancellation_reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    locked_by VARCHAR(100) NULL,
    locked_until DATETIME(6) NULL,
    last_error VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    CONSTRAINT pk_showtime_refund_outbox PRIMARY KEY (id),
    CONSTRAINT uk_showtime_refund_outbox_event UNIQUE (event_id),
    CONSTRAINT chk_showtime_refund_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'FAILED', 'PUBLISHED', 'DEAD_LETTER')),
    INDEX idx_showtime_refund_outbox_delivery (status, next_attempt_at),
    INDEX idx_showtime_refund_outbox_showtime (showtime_public_id)
);
