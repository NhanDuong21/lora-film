
CREATE DATABASE IF NOT EXISTS user_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE user_db;

DROP TABLE IF EXISTS outbox_messages;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS payroll_details;
DROP TABLE IF EXISTS payrolls;
DROP TABLE IF EXISTS employee_documents;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS positions;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS customer_profiles;
DROP TABLE IF EXISTS avatars;
DROP TABLE IF EXISTS users;

-- =====================================================
-- TABLE: users
-- Thực thể gốc của User Service
-- Liên kết với auth_db.accounts thông qua account_id
-- =====================================================

-- =====================================================
-- TABLE: users
-- Shared Primary Key với Auth Service
-- =====================================================

CREATE TABLE users
(
    -- Shared PK từ auth-service
    account_id BIGINT PRIMARY KEY
        COMMENT 'Logical reference tới auth_db.accounts.id',

    -- ==========================
    -- Thông tin cá nhân
    -- ==========================
    full_name VARCHAR(150) NOT NULL,

    email VARCHAR(100) NULL,

    -- OAuth providers such as Google do not return a phone number.
    -- The user can add it later from the profile page.
    phone_number VARCHAR(512) NULL COMMENT 'AES-256-GCM encrypted at application boundary',

    phone_hash CHAR(64) NULL COMMENT 'HMAC-SHA256 exact-search token',

    gender ENUM
    (
        'MALE',
        'FEMALE',
        'OTHER'
    ) NULL,

    birthday DATE NULL,

    birth_year INT NULL,

    avatar_url VARCHAR(500) NULL,

    -- ==========================
    -- CCCD
    -- ==========================
    cccd VARCHAR(512) NULL COMMENT 'AES-256-GCM encrypted at application boundary',

    cccd_hash CHAR(64) NULL COMMENT 'HMAC-SHA256 exact-search token',

    cccd_masked VARCHAR(20) NULL,

    -- ==========================
    -- Quê quán (không phải địa chỉ hiện tại)
    -- ==========================
    province_code VARCHAR(20) NULL,

    province_name VARCHAR(150) NULL,

    -- ==========================
    -- Trạng thái hồ sơ
    -- ==========================
    status ENUM
    (
        'ACTIVE',
        'INACTIVE',
        'BLOCKED'
    )
    NOT NULL DEFAULT 'ACTIVE',

    account_type ENUM('CUSTOMER', 'WORKFORCE')
        NOT NULL DEFAULT 'CUSTOMER',

    pii_key_version INT NOT NULL DEFAULT 1,

    pii_retention_until DATE NULL,

    pii_erased_at TIMESTAMP(6) NULL,

    -- ==========================
    -- Audit
    -- ==========================
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT NULL,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    updated_by BIGINT NULL,

    deleted_at TIMESTAMP NULL,

    deleted_by BIGINT NULL,

    version INT NOT NULL DEFAULT 0,

    -- ==========================
    -- Constraint
    -- ==========================
    CONSTRAINT uk_users_phone_hash
        UNIQUE(phone_hash),

    CONSTRAINT uk_users_cccd_hash
        UNIQUE(cccd_hash),

    CONSTRAINT chk_birth_year
        CHECK (
            birth_year IS NULL
            OR birth_year >= 1900
        )

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


-- ==========================
-- INDEX
-- ==========================

CREATE INDEX idx_users_name
ON users(full_name);

CREATE INDEX idx_users_status
ON users(status);

CREATE INDEX idx_users_birth_year
ON users(birth_year);

CREATE INDEX idx_users_province
ON users(province_code);

CREATE INDEX idx_users_created
ON users(created_at);


-- =====================================================
-- TABLE: avatars
-- Lưu lịch sử Avatar
-- =====================================================

CREATE TABLE avatars
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    file_url VARCHAR(500) NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    file_size BIGINT NOT NULL,

    uploaded_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avatar_user
        FOREIGN KEY(account_id)
        REFERENCES users(account_id)
        ON UPDATE CASCADE   
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_avatar_user
ON avatars(account_id);

USE user_db;

-- =====================================================
-- TABLE: customer_profiles
-- Thông tin khách hàng
-- =====================================================

CREATE TABLE customer_profiles
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    customer_code VARCHAR(30) NOT NULL,

    joined_at DATE NOT NULL,

    note TEXT NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_customer_code
        UNIQUE(customer_code),

    CONSTRAINT uk_customer_user
        UNIQUE(account_id),

    CONSTRAINT fk_customer_user
        FOREIGN KEY(account_id)
        REFERENCES users(account_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_customer_joined
ON customer_profiles(joined_at);





-- =====================================================
-- TABLE: departments
-- Danh mục phòng ban
-- =====================================================

CREATE TABLE departments
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    code VARCHAR(30) NOT NULL,

    name VARCHAR(100) NOT NULL,

    description VARCHAR(255) NULL,

    department_id BIGINT NOT NULL,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_department_code
        UNIQUE(code),

    CONSTRAINT uk_department_name
        UNIQUE(name)

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_department_active
ON departments(is_deleted);


-- =====================================================
-- TABLE: positions
-- Danh mục chức vụ
-- =====================================================

CREATE TABLE positions
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    code VARCHAR(30) NOT NULL,

    title VARCHAR(100) NOT NULL,

    description VARCHAR(255) NULL,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_position_code
        UNIQUE(code),

    CONSTRAINT fk_position_department
        FOREIGN KEY(department_id)
        REFERENCES departments(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_position_active
ON positions(is_deleted);

CREATE INDEX idx_position_department_active
ON positions(department_id, is_deleted);


-- =====================================================
-- TABLE: employee_profiles
-- Thông tin nhân sự
-- =====================================================

CREATE TABLE employees
(
    account_id BIGINT PRIMARY KEY,

    employee_code VARCHAR(50) NOT NULL,

    department_id BIGINT NULL,

    position_id BIGINT NULL,

    base_salary DECIMAL(15,2) NULL,

    hire_date DATE NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version INT NOT NULL DEFAULT 0,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_code
        UNIQUE(employee_code),

    CONSTRAINT fk_employee_account
        FOREIGN KEY(account_id)
        REFERENCES users(account_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_employee_department
        FOREIGN KEY(department_id)
        REFERENCES departments(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_position
        FOREIGN KEY(position_id)
        REFERENCES positions(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_employee_department
ON employees(department_id);

CREATE INDEX idx_employee_position
ON employees(position_id);

CREATE INDEX idx_employee_status
ON employees(status);

CREATE INDEX idx_employee_hire_date
ON employees(hire_date);

-- =====================================================
-- TABLE: employment_actions
-- Lịch sử vòng đời nhân sự có snapshot trước/sau
-- =====================================================

CREATE TABLE employment_actions
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_account_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    effective_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NULL,
    previous_department_id BIGINT NULL,
    new_department_id BIGINT NULL,
    previous_position_id BIGINT NULL,
    new_position_id BIGINT NULL,
    previous_base_salary DECIMAL(15,2) NULL,
    new_base_salary DECIMAL(15,2) NULL,
    performed_by BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_employment_action_employee FOREIGN KEY(employee_account_id)
        REFERENCES employees(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_employment_action_previous_department FOREIGN KEY(previous_department_id)
        REFERENCES departments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employment_action_new_department FOREIGN KEY(new_department_id)
        REFERENCES departments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employment_action_previous_position FOREIGN KEY(previous_position_id)
        REFERENCES positions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employment_action_new_position FOREIGN KEY(new_position_id)
        REFERENCES positions(id) ON DELETE RESTRICT
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_employment_action_employee_created
ON employment_actions(employee_account_id, created_at);

CREATE INDEX idx_employment_action_effective_date
ON employment_actions(effective_date);






-- =====================================================
-- TABLE: work_shifts
-- Ca làm đã phân công
-- =====================================================

CREATE TABLE work_shifts
(
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
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: attendance_records
-- Dữ liệu chấm công có nguồn và lịch sử hiệu chỉnh
-- =====================================================

CREATE TABLE attendance_records
(
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
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: leave_requests
-- Nghỉ phép có maker-checker
-- =====================================================

CREATE TABLE leave_requests
(
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
)
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: employee_documents
-- Hồ sơ nhân sự
-- =====================================================

CREATE TABLE employee_documents
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    document_type ENUM(
        'IDENTITY_CARD',
        'PASSPORT',
        'LABOR_CONTRACT',
        'CERTIFICATE',
        'DIPLOMA',
        'OTHER'
    ) NOT NULL,

    document_name VARCHAR(255) NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    file_url VARCHAR(500) NOT NULL,

    file_size BIGINT NOT NULL,

    mime_type VARCHAR(100) NOT NULL,

    issued_date DATE NULL,

    expired_date DATE NULL,

    uploaded_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    uploaded_by BIGINT NULL,

    deleted_at TIMESTAMP NULL,

    deleted_by BIGINT NULL,

    CONSTRAINT uk_document_file_name
        UNIQUE(file_name),

    CONSTRAINT fk_document_employee
        FOREIGN KEY(employee_id)
        REFERENCES employees(account_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_document_employee
ON employee_documents(employee_id);

CREATE INDEX idx_document_type
ON employee_documents(document_type);



-- =====================================================
-- TABLE: payrolls
-- Bảng lương nhân viên
-- =====================================================

CREATE TABLE payrolls
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    salary_month DATE NOT NULL,

    basic_salary DECIMAL(15,2) NOT NULL DEFAULT 0,

    allowance DECIMAL(15,2) NOT NULL DEFAULT 0,

    bonus DECIMAL(15,2) NOT NULL DEFAULT 0,

    deduction DECIMAL(15,2) NOT NULL DEFAULT 0,

    total_salary DECIMAL(15,2) NOT NULL DEFAULT 0,

    status ENUM(
        'DRAFT',
        'PENDING_APPROVAL',
        'APPROVED',
        'PAYMENT_PENDING',
        'PAID',
        'CANCELLED'
    ) NOT NULL DEFAULT 'DRAFT',

    created_by BIGINT NULL,

    approved_by BIGINT NULL,

    approved_at TIMESTAMP NULL,

    paid_by BIGINT NULL,

    payment_reference VARCHAR(100) NULL,

    bank_batch_reference VARCHAR(100) NULL,

    accounting_reference VARCHAR(100) NULL,

    reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SUBMITTED',

    reconciled_by BIGINT NULL,

    reconciled_at TIMESTAMP(6) NULL,

    reconciliation_note VARCHAR(500) NULL,

    source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL_EXCEPTION',

    source_checksum CHAR(64) NULL,

    scheduled_minutes INT NOT NULL DEFAULT 0,

    worked_minutes INT NOT NULL DEFAULT 0,

    paid_leave_minutes INT NOT NULL DEFAULT 0,

    overtime_minutes INT NOT NULL DEFAULT 0,

    paid_at TIMESTAMP NULL,

    cancelled_by BIGINT NULL,

    cancellation_reason VARCHAR(500) NULL,

    note TEXT NULL,

    version INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_payroll
        UNIQUE(employee_id, salary_month),

    CONSTRAINT uk_payroll_payment_reference
        UNIQUE(payment_reference),

    CONSTRAINT fk_payroll_employee
        FOREIGN KEY(employee_id)
        REFERENCES employees(account_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_payroll_employee
ON payrolls(employee_id);

CREATE INDEX idx_payroll_month
ON payrolls(salary_month);

CREATE INDEX idx_payroll_status
ON payrolls(status);

CREATE INDEX idx_payroll_bank_batch
ON payrolls(bank_batch_reference);

CREATE INDEX idx_payroll_reconciliation
ON payrolls(reconciliation_status);





-- =====================================================
-- TABLE: payroll_details
-- Chi tiết từng khoản lương
-- =====================================================

CREATE TABLE payroll_details
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    payroll_id BIGINT NOT NULL,

    type ENUM(
        'ALLOWANCE',
        'BONUS',
        'DEDUCTION'
    ) NOT NULL,

    description VARCHAR(255) NOT NULL,

    amount DECIMAL(15,2) NOT NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payroll_detail
        FOREIGN KEY(payroll_id)
        REFERENCES payrolls(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_payroll_detail_payroll
ON payroll_details(payroll_id);

CREATE INDEX idx_payroll_detail_type
ON payroll_details(type);
CREATE TABLE audit_logs
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    actor_account_id BIGINT NULL,

    action VARCHAR(100) NOT NULL,

    target_type VARCHAR(50) NOT NULL,

    target_id VARCHAR(100) NULL,

    details VARCHAR(1000) NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_audit
        FOREIGN KEY (actor_account_id)
        REFERENCES users(account_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_audit_user
ON audit_logs(actor_account_id);

CREATE INDEX idx_audit_action
ON audit_logs(action);

CREATE INDEX idx_audit_target
ON audit_logs(target_type);

CREATE INDEX idx_audit_created
ON audit_logs(created_at);





-- =====================================================
-- TABLE: outbox_events
-- Transactional Outbox Pattern
-- =====================================================

CREATE TABLE outbox_messages
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    last_error VARCHAR(1000) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_outbox_processed
ON outbox_messages(processed);

CREATE INDEX idx_outbox_created
ON outbox_messages(created_at);

CREATE INDEX idx_outbox_event_type
ON outbox_messages(event_type);


