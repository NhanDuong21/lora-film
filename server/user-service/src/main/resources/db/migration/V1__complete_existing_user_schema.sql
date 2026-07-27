-- Additive compatibility migration for the existing user_db schema.
-- Existing tables and data are preserved; no destructive operation is used.

ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER birth_year,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER avatar_url;

ALTER TABLE departments
    ADD COLUMN code VARCHAR(20) NULL AFTER id,
    ADD COLUMN description VARCHAR(255) NULL AFTER name,
    ADD COLUMN created_by BIGINT NULL AFTER created_at,
    ADD COLUMN updated_by BIGINT NULL AFTER updated_at;

UPDATE departments
SET code = CONCAT('DEPT_', id)
WHERE code IS NULL OR TRIM(code) = '';

ALTER TABLE departments
    MODIFY COLUMN code VARCHAR(20) NOT NULL,
    ADD CONSTRAINT uk_departments_code UNIQUE (code);

ALTER TABLE positions
    ADD COLUMN code VARCHAR(30) NULL AFTER id,
    ADD COLUMN description VARCHAR(255) NULL AFTER title,
    ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6);

UPDATE positions
SET code = CONCAT('POS_', id)
WHERE code IS NULL OR TRIM(code) = '';

ALTER TABLE positions
    MODIFY COLUMN code VARCHAR(30) NOT NULL,
    ADD CONSTRAINT uk_positions_code UNIQUE (code);

ALTER TABLE employees
    ADD COLUMN hire_date DATE NULL AFTER base_salary,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER hire_date,
    ADD COLUMN created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE outbox_messages
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER processed,
    ADD COLUMN next_attempt_at DATETIME(6) NULL AFTER attempt_count,
    ADD COLUMN processed_at DATETIME(6) NULL AFTER next_attempt_at,
    ADD COLUMN last_error VARCHAR(1000) NULL AFTER processed_at;

CREATE INDEX idx_user_outbox_ready
    ON outbox_messages (processed, next_attempt_at, created_at);

CREATE TABLE IF NOT EXISTS avatars
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_avatars_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer_profiles
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    customer_code VARCHAR(30) NOT NULL,
    joined_at DATE NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_profiles_account UNIQUE (account_id),
    CONSTRAINT uk_customer_profiles_code UNIQUE (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employee_documents
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    issued_date DATE NULL,
    expired_date DATE NULL,
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    uploaded_by BIGINT NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_documents_file_name UNIQUE (file_name),
    CONSTRAINT fk_employee_documents_employee
        FOREIGN KEY (employee_id) REFERENCES employees (account_id),
    INDEX idx_employee_documents_employee (employee_id, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payrolls
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    salary_month DATE NOT NULL,
    basic_salary DECIMAL(15,2) NOT NULL,
    allowance DECIMAL(15,2) NOT NULL DEFAULT 0,
    bonus DECIMAL(15,2) NOT NULL DEFAULT 0,
    deduction DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_salary DECIMAL(15,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    approved_by BIGINT NULL,
    approved_at DATETIME(6) NULL,
    paid_at DATETIME(6) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_employee_month UNIQUE (employee_id, salary_month),
    CONSTRAINT fk_payroll_employee
        FOREIGN KEY (employee_id) REFERENCES employees (account_id),
    CONSTRAINT chk_payroll_total_non_negative CHECK (total_salary >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payroll_details
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    payroll_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payroll_details_payroll
        FOREIGN KEY (payroll_id) REFERENCES payrolls (id) ON DELETE CASCADE,
    INDEX idx_payroll_details_payroll (payroll_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_account_id BIGINT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100) NULL,
    action VARCHAR(100) NOT NULL,
    details VARCHAR(1000) NULL,
    created_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_user_audit_actor_created (actor_account_id, created_at),
    INDEX idx_user_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
