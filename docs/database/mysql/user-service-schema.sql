

CREATE DATABASE IF NOT EXISTS user_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE user_db;

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

    phone_number VARCHAR(15) NOT NULL,

    gender ENUM
    (
        'MALE',
        'FEMALE',
        'OTHER'
    ) NULL,

    birthday DATE NULL,

    birth_year SMALLINT NULL,

    avatar_url VARCHAR(500) NULL,

    -- ==========================
    -- CCCD
    -- ==========================
    cccd VARCHAR(12) NULL,

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

    -- ==========================
    -- Audit
    -- ==========================
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
    CONSTRAINT uk_users_phone
        UNIQUE(phone_number),

    CONSTRAINT uk_users_cccd
        UNIQUE(cccd),

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

CREATE INDEX idx_users_phone
ON users(phone_number);

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

    user_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    file_url VARCHAR(500) NOT NULL,

    file_size BIGINT NULL,

    mime_type VARCHAR(100) NULL,

    uploaded_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avatar_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_avatar_user
ON avatars(user_id);

USE user_db;

-- =====================================================
-- TABLE: customer_profiles
-- Thông tin khách hàng
-- =====================================================

CREATE TABLE customer_profiles
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

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
        UNIQUE(user_id),

    CONSTRAINT fk_customer_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
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

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

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
ON departments(is_active);


-- =====================================================
-- TABLE: positions
-- Danh mục chức vụ
-- =====================================================

CREATE TABLE positions
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    code VARCHAR(30) NOT NULL,

    name VARCHAR(100) NOT NULL,

    description VARCHAR(255) NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_position_code
        UNIQUE(code),

    CONSTRAINT uk_position_name
        UNIQUE(name)

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_position_active
ON positions(is_active);


-- =====================================================
-- TABLE: employee_profiles
-- Thông tin nhân sự
-- =====================================================

CREATE TABLE employee_profiles
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    employee_code VARCHAR(30) NOT NULL,

    department_id BIGINT NOT NULL,

    position_id BIGINT NOT NULL,

    hire_date DATE NOT NULL,

    resign_date DATE NULL,

    employment_type ENUM(
        'FULL_TIME',
        'PART_TIME',
        'CONTRACT',
        'INTERN'
    ) NOT NULL DEFAULT 'FULL_TIME',

    status ENUM(
        'ACTIVE',
        'ON_LEAVE',
        'SUSPENDED',
        'RESIGNED'
    ) NOT NULL DEFAULT 'ACTIVE',

    emergency_contact_name VARCHAR(150) NULL,

    emergency_contact_phone VARCHAR(20) NULL,

    note TEXT NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_user
        UNIQUE(user_id),

    CONSTRAINT uk_employee_code
        UNIQUE(employee_code),

    CONSTRAINT fk_employee_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
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
ON employee_profiles(department_id);

CREATE INDEX idx_employee_position
ON employee_profiles(position_id);

CREATE INDEX idx_employee_status
ON employee_profiles(status);

CREATE INDEX idx_employee_hire_date
ON employee_profiles(hire_date);





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

    file_url VARCHAR(500) NOT NULL,

    file_size BIGINT NULL,

    mime_type VARCHAR(100) NULL,

    issued_date DATE NULL,

    expired_date DATE NULL,

    uploaded_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_employee
        FOREIGN KEY(employee_id)
        REFERENCES employee_profiles(id)
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

    payroll_month DATE NOT NULL,

    basic_salary DECIMAL(15,2) NOT NULL DEFAULT 0,

    allowance DECIMAL(15,2) NOT NULL DEFAULT 0,

    bonus DECIMAL(15,2) NOT NULL DEFAULT 0,

    deduction DECIMAL(15,2) NOT NULL DEFAULT 0,

    total_salary DECIMAL(15,2) NOT NULL DEFAULT 0,

    status ENUM(
        'DRAFT',
        'PENDING_APPROVAL',
        'APPROVED',
        'PAID',
        'CANCELLED'
    ) NOT NULL DEFAULT 'DRAFT',

    approved_by BIGINT NULL,

    approved_at TIMESTAMP NULL,

    paid_at TIMESTAMP NULL,

    note TEXT NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_employee_payroll
        UNIQUE(employee_id, payroll_month),

    CONSTRAINT fk_payroll_employee
        FOREIGN KEY(employee_id)
        REFERENCES employee_profiles(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_payroll_employee
ON payrolls(employee_id);

CREATE INDEX idx_payroll_month
ON payrolls(payroll_month);

CREATE INDEX idx_payroll_status
ON payrolls(status);





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

    user_id BIGINT NULL,

    action VARCHAR(100) NOT NULL,

    resource VARCHAR(100) NOT NULL,

    resource_id BIGINT NULL,

    description TEXT NULL,

    ip_address VARCHAR(45) NULL,

    user_agent VARCHAR(500) NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_audit
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_audit_user
ON audit_logs(user_id);

CREATE INDEX idx_audit_action
ON audit_logs(action);

CREATE INDEX idx_audit_resource
ON audit_logs(resource);

CREATE INDEX idx_audit_created
ON audit_logs(created_at);





-- =====================================================
-- TABLE: outbox_events
-- Transactional Outbox Pattern
-- =====================================================

CREATE TABLE outbox_events
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id BIGINT NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload JSON NOT NULL,

    status ENUM
    (
        'PENDING',
        'PUBLISHED',
        'FAILED'
    )
    NOT NULL DEFAULT 'PENDING',

    retry_count INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    published_at TIMESTAMP NULL

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_outbox_status
ON outbox_events(status);

CREATE INDEX idx_outbox_created
ON outbox_events(created_at);

CREATE INDEX idx_outbox_event_type
ON outbox_events(event_type);
