CREATE DATABASE IF NOT EXISTS auth_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE auth_db;


-- =====================================================
-- TABLE: accounts
-- Quản lý tài khoản đăng nhập
-- =====================================================

CREATE TABLE accounts
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    email VARCHAR(255) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    status ENUM(
        'ACTIVE',
        'INACTIVE',
        'LOCKED',
        'DELETED'
    )
    NOT NULL DEFAULT 'INACTIVE',

    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,


    last_login_at TIMESTAMP NULL,


    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT NULL,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    updated_by BIGINT NULL,


    CONSTRAINT uk_accounts_email
        UNIQUE(email)

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_accounts_status
ON accounts(status);



CREATE INDEX idx_accounts_deleted
ON accounts(is_deleted);



-- =====================================================
-- TABLE: roles
-- Quản lý quyền theo vai trò
-- =====================================================

CREATE TABLE roles
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,


    code VARCHAR(50) NOT NULL,


    name VARCHAR(100) NOT NULL,


    description VARCHAR(255) NULL,


    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,


    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,


    CONSTRAINT uk_roles_code
        UNIQUE(code)

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_roles_name
ON roles(name);



-- =====================================================
-- TABLE: permissions
-- Quản lý quyền chi tiết
-- =====================================================

CREATE TABLE permissions
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,


    code VARCHAR(100) NOT NULL,


    name VARCHAR(150) NOT NULL,


    module VARCHAR(100) NOT NULL,


    description VARCHAR(255) NULL,


    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,


    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,


    CONSTRAINT uk_permissions_code
        UNIQUE(code)

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_permissions_module
ON permissions(module);


-- =====================================================
-- TABLE: access_profiles
-- Nhóm quyền nghiệp vụ dành cho tài khoản EMPLOYEE
-- =====================================================

CREATE TABLE access_profiles
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_access_profiles_code UNIQUE(code)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE TABLE access_profile_permissions
(
    access_profile_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (access_profile_id, permission_id),
    CONSTRAINT fk_access_profile_permissions_profile
        FOREIGN KEY (access_profile_id) REFERENCES access_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_profile_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

ALTER TABLE accounts
    ADD COLUMN access_profile_id BIGINT NULL AFTER is_deleted,
    ADD INDEX idx_accounts_access_profile (access_profile_id),
    ADD CONSTRAINT fk_accounts_access_profile
        FOREIGN KEY (access_profile_id) REFERENCES access_profiles(id);


-- =====================================================
-- TABLE: account_roles
-- Quan hệ N-N giữa Account và Role
-- =====================================================

CREATE TABLE account_roles
(
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    assigned_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    assigned_by BIGINT NULL,

    PRIMARY KEY (account_id, role_id),

    CONSTRAINT fk_account_roles_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_account_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_account_roles_role
ON account_roles(role_id);


-- =====================================================
-- TABLE: manager_cinema_assignments
-- Phạm vi rạp mà mỗi tài khoản MANAGER được phép điều hành
-- =====================================================

CREATE TABLE manager_cinema_assignments
(
    account_id BIGINT NOT NULL,
    cinema_public_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (account_id, cinema_public_id),

    CONSTRAINT fk_manager_cinema_assignments_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_manager_cinema_assignments_cinema
ON manager_cinema_assignments(cinema_public_id);





-- =====================================================
-- TABLE: roles_permissions
-- Quan hệ N-N giữa Role và Permission
-- =====================================================

CREATE TABLE roles_permissions
(
    role_id BIGINT NOT NULL,

    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_roles_permissions_permission
ON roles_permissions(permission_id);





-- =====================================================
-- TABLE: refresh_tokens
-- Refresh Token Rotation
-- Chỉ lưu HASH của Refresh Token
-- =====================================================

CREATE TABLE refresh_tokens
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    token_hash CHAR(64) NOT NULL,

    device_id VARCHAR(120) NULL,

    expires_at TIMESTAMP NOT NULL,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    revoked_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_refresh_token_hash
        UNIQUE(token_hash),

    CONSTRAINT fk_refresh_token_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_refresh_account
ON refresh_tokens(account_id);


CREATE INDEX idx_refresh_expired
ON refresh_tokens(expires_at);


CREATE INDEX idx_refresh_revoked
ON refresh_tokens(revoked);





-- =====================================================
-- TABLE: sessions
-- Quản lý phiên đăng nhập
-- =====================================================

CREATE TABLE sessions
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    refresh_token_id BIGINT NULL,

    device_name VARCHAR(150) NULL,

    device_type VARCHAR(50) NULL,

    browser VARCHAR(100) NULL,

    operating_system VARCHAR(100) NULL,

    ip_address VARCHAR(45) NOT NULL,

    user_agent VARCHAR(500) NULL,

    login_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    last_active_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    expired_at TIMESTAMP NULL,

    logout_at TIMESTAMP NULL,

    is_online BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_sessions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_sessions_refresh
        FOREIGN KEY (refresh_token_id)
        REFERENCES refresh_tokens(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_sessions_account
ON sessions(account_id);


CREATE INDEX idx_sessions_online
ON sessions(is_online);


CREATE INDEX idx_sessions_last_active
ON sessions(last_active_at);


CREATE INDEX idx_sessions_ip
ON sessions(ip_address);


-- =====================================================
-- TABLE: oauth_accounts
-- Liên kết tài khoản OAuth2
-- =====================================================

CREATE TABLE oauth_accounts
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    provider ENUM(
        'GOOGLE',
        'FACEBOOK',
        'GITHUB',
        'MICROSOFT'
    ) NOT NULL,

    provider_user_id VARCHAR(255) NOT NULL,

    provider_email VARCHAR(255) NULL,

    linked_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_provider_user
        UNIQUE(provider, provider_user_id),

    CONSTRAINT fk_oauth_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_oauth_account
ON oauth_accounts(account_id);

CREATE INDEX idx_oauth_provider
ON oauth_accounts(provider);





-- =====================================================
-- TABLE: email_verifications
-- OTP xác thực Email
-- =====================================================

CREATE TABLE email_verifications
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    otp_code CHAR(6) NOT NULL,

    expired_at TIMESTAMP NOT NULL,

    verified_at TIMESTAMP NULL,

    attempts INT NOT NULL DEFAULT 0,

    is_verified BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_email_verification_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_email_verification_account
ON email_verifications(account_id);

CREATE INDEX idx_email_verification_expired
ON email_verifications(expired_at);

CREATE INDEX idx_email_verification_verified
ON email_verifications(is_verified);





-- =====================================================
-- TABLE: password_reset_tokens
-- OTP đặt lại mật khẩu
-- =====================================================

CREATE TABLE password_reset_tokens
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    otp_code CHAR(6) NOT NULL,

    expired_at TIMESTAMP NOT NULL,

    used_at TIMESTAMP NULL,

    attempts INT NOT NULL DEFAULT 0,

    is_used BOOLEAN NOT NULL DEFAULT FALSE,

    purpose VARCHAR(30) NOT NULL DEFAULT 'PASSWORD_RESET',

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_password_reset_account
ON password_reset_tokens(account_id);

CREATE INDEX idx_password_reset_expired
ON password_reset_tokens(expired_at);

CREATE INDEX idx_password_reset_used
ON password_reset_tokens(is_used);

-- =====================================================
-- TABLE: login_history
-- Lưu lịch sử đăng nhập
-- =====================================================

CREATE TABLE login_history
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    login_time TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    ip_address VARCHAR(45) NULL,

    user_agent VARCHAR(255) NULL,

    status VARCHAR(20) NULL,

    CONSTRAINT fk_login_history_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_login_history_account
ON login_history(account_id);

CREATE INDEX idx_login_history_login
ON login_history(login_time);

CREATE INDEX idx_login_history_status
ON login_history(status);

CREATE INDEX idx_login_history_ip
ON login_history(ip_address);





-- =====================================================
-- TABLE: audit_logs
-- Nhật ký Audit
-- =====================================================

CREATE TABLE audit_logs
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NULL,

    action VARCHAR(100) NOT NULL,

    resource VARCHAR(100) NOT NULL,

    resource_id VARCHAR(100) NULL,

    description TEXT NULL,

    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',

    severity VARCHAR(20) NOT NULL DEFAULT 'NORMAL',

    review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',

    reviewed_by BIGINT NULL,

    review_note VARCHAR(500) NULL,

    reviewed_at TIMESTAMP NULL,

    ip_address VARCHAR(45) NULL,

    user_agent VARCHAR(500) NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    created_by BIGINT NULL,

    updated_at TIMESTAMP NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    updated_by BIGINT NULL,

    CONSTRAINT fk_audit_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_audit_account
ON audit_logs(account_id);

CREATE INDEX idx_audit_action
ON audit_logs(action);

CREATE INDEX idx_audit_resource
ON audit_logs(resource);

CREATE INDEX idx_audit_created
ON audit_logs(created_at);





-- =====================================================
-- TABLE: outbox_messages
-- Transactional Outbox Pattern
-- =====================================================

CREATE TABLE outbox_messages
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,

    aggregate_id VARCHAR(100) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    processed BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_outbox_status
ON outbox_messages(processed);

CREATE INDEX idx_outbox_created
ON outbox_messages(created_at);

CREATE INDEX idx_outbox_event_type
ON outbox_messages(event_type);

