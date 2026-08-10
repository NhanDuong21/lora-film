-- Production HR/payroll controls: timekeeping, leave, PII protection and reconciliation.
-- Apply after 20260808_customer_hr_operations.sql.

ALTER TABLE users
    DROP INDEX uk_users_phone,
    DROP INDEX uk_users_cccd,
    DROP INDEX idx_users_phone,
    MODIFY phone_number VARCHAR(512) NULL,
    MODIFY cccd VARCHAR(512) NULL,
    ADD COLUMN phone_hash CHAR(64) NULL AFTER phone_number,
    ADD COLUMN cccd_hash CHAR(64) NULL AFTER cccd,
    ADD COLUMN pii_key_version INT NOT NULL DEFAULT 0 AFTER account_type,
    ADD COLUMN pii_retention_until DATE NULL AFTER pii_key_version,
    ADD COLUMN pii_erased_at TIMESTAMP(6) NULL AFTER pii_retention_until,
    ADD CONSTRAINT uk_users_phone_hash UNIQUE(phone_hash),
    ADD CONSTRAINT uk_users_cccd_hash UNIQUE(cccd_hash);

CREATE TABLE work_shifts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    scheduled_start TIMESTAMP(6) NOT NULL,
    scheduled_end TIMESTAMP(6) NOT NULL,
    location VARCHAR(150) NULL,
    note VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_by BIGINT NOT NULL,
    cancelled_by BIGINT NULL,
    cancelled_at TIMESTAMP(6) NULL,
    cancellation_reason VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_work_shift_employee_start UNIQUE(employee_id, scheduled_start),
    CONSTRAINT fk_work_shift_employee FOREIGN KEY(employee_id)
        REFERENCES employees(account_id) ON DELETE RESTRICT,
    INDEX idx_work_shift_range (scheduled_start, scheduled_end),
    INDEX idx_work_shift_employee_range (employee_id, scheduled_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shift_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    check_in_at TIMESTAMP(6) NULL,
    check_out_at TIMESTAMP(6) NULL,
    status VARCHAR(20) NOT NULL,
    worked_minutes INT NOT NULL DEFAULT 0,
    overtime_minutes INT NOT NULL DEFAULT 0,
    source VARCHAR(30) NOT NULL DEFAULT 'SELF_SERVICE',
    corrected_by BIGINT NULL,
    correction_reason VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_attendance_shift UNIQUE(shift_id),
    CONSTRAINT fk_attendance_shift FOREIGN KEY(shift_id)
        REFERENCES work_shifts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_employee FOREIGN KEY(employee_id)
        REFERENCES employees(account_id) ON DELETE RESTRICT,
    INDEX idx_attendance_employee_created (employee_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP(6) NULL,
    review_note VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_leave_employee FOREIGN KEY(employee_id)
        REFERENCES employees(account_id) ON DELETE RESTRICT,
    INDEX idx_leave_employee_dates (employee_id, start_date, end_date),
    INDEX idx_leave_status_dates (status, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE payrolls
    MODIFY status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN bank_batch_reference VARCHAR(100) NULL AFTER payment_reference,
    ADD COLUMN accounting_reference VARCHAR(100) NULL AFTER bank_batch_reference,
    ADD COLUMN reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SUBMITTED' AFTER accounting_reference,
    ADD COLUMN reconciled_by BIGINT NULL AFTER reconciliation_status,
    ADD COLUMN reconciled_at TIMESTAMP(6) NULL AFTER reconciled_by,
    ADD COLUMN reconciliation_note VARCHAR(500) NULL AFTER reconciled_at,
    ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL_EXCEPTION' AFTER reconciliation_note,
    ADD COLUMN source_checksum CHAR(64) NULL AFTER source_type,
    ADD COLUMN scheduled_minutes INT NOT NULL DEFAULT 0 AFTER source_checksum,
    ADD COLUMN worked_minutes INT NOT NULL DEFAULT 0 AFTER scheduled_minutes,
    ADD COLUMN paid_leave_minutes INT NOT NULL DEFAULT 0 AFTER worked_minutes,
    ADD COLUMN overtime_minutes INT NOT NULL DEFAULT 0 AFTER paid_leave_minutes,
    ADD CONSTRAINT uk_payroll_payment_reference UNIQUE(payment_reference),
    ADD INDEX idx_payroll_bank_batch (bank_batch_reference),
    ADD INDEX idx_payroll_reconciliation (reconciliation_status);
