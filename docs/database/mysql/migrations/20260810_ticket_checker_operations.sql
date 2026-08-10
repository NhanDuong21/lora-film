-- Ticket checker operations: atomic admission, immutable scan history and shift handoff.

ALTER TABLE booking_tickets
    ADD COLUMN used_by_account_id BIGINT NULL AFTER used_at,
    ADD COLUMN used_cinema_public_id VARCHAR(36) NULL AFTER used_by_account_id,
    ADD COLUMN used_gate_label VARCHAR(80) NULL AFTER used_cinema_public_id;

CREATE TABLE ticket_scan_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    ticket_id BIGINT NULL,
    entered_code VARCHAR(255) NOT NULL,
    employee_account_id BIGINT NOT NULL,
    cinema_public_id VARCHAR(36) NOT NULL,
    gate_label VARCHAR(80) NULL,
    result ENUM(
        'ADMITTED', 'ALREADY_USED', 'NOT_FOUND', 'WRONG_CINEMA',
        'TOO_EARLY', 'TOO_LATE', 'REFUNDED', 'CANCELLED',
        'NOT_PAID', 'INVALID_STATUS'
    ) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_message VARCHAR(500) NOT NULL,
    scanned_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ticket_scan_event_public UNIQUE(public_id),
    CONSTRAINT fk_ticket_scan_event_ticket
        FOREIGN KEY(ticket_id) REFERENCES booking_tickets(id),
    INDEX idx_ticket_scan_employee_time(employee_account_id, scanned_at),
    INDEX idx_ticket_scan_cinema_time(cinema_public_id, scanned_at),
    INDEX idx_ticket_scan_result_time(result, scanned_at),
    INDEX idx_ticket_scan_ticket(ticket_id)
) ENGINE=InnoDB
COMMENT='Immutable history of successful and rejected ticket scans';

CREATE TABLE ticket_gate_handoffs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    employee_account_id BIGINT NOT NULL,
    cinema_public_id VARCHAR(36) NOT NULL,
    shift_date DATE NOT NULL,
    gate_label VARCHAR(80) NULL,
    total_scans INT NOT NULL DEFAULT 0,
    successful_scans INT NOT NULL DEFAULT 0,
    rejected_scans INT NOT NULL DEFAULT 0,
    unresolved_incidents INT NOT NULL DEFAULT 0,
    note VARCHAR(1000) NULL,
    handed_off_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ticket_gate_handoff_public UNIQUE(public_id),
    CONSTRAINT uk_ticket_gate_handoff_employee_day
        UNIQUE(employee_account_id, shift_date),
    INDEX idx_ticket_gate_handoff_cinema_day(cinema_public_id, shift_date)
) ENGINE=InnoDB
COMMENT='Ticket gate handoff record per employee and operating day';
