
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
-- TABLE: role_permissions
-- Quan hệ N-N giữa Role và Permission
-- =====================================================

CREATE TABLE role_permissions
(
    role_id BIGINT NOT NULL,

    permission_id BIGINT NOT NULL,

    granted_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    granted_by BIGINT NULL,

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


CREATE INDEX idx_role_permissions_permission
ON role_permissions(permission_id);





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
-- TABLE: login_histories
-- Lưu lịch sử đăng nhập
-- =====================================================

CREATE TABLE login_histories
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    account_id BIGINT NOT NULL,

    session_id BIGINT NULL,

    login_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    logout_at TIMESTAMP NULL,

    ip_address VARCHAR(45) NOT NULL,

    device_name VARCHAR(150) NULL,

    browser VARCHAR(100) NULL,

    operating_system VARCHAR(100) NULL,

    login_status ENUM(
        'SUCCESS',
        'FAILED'
    ) NOT NULL,

    failure_reason VARCHAR(255) NULL,

    CONSTRAINT fk_login_history_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_login_history_session
        FOREIGN KEY (session_id)
        REFERENCES sessions(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL

)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;



CREATE INDEX idx_login_history_account
ON login_histories(account_id);

CREATE INDEX idx_login_history_login
ON login_histories(login_at);

CREATE INDEX idx_login_history_status
ON login_histories(login_status);

CREATE INDEX idx_login_history_ip
ON login_histories(ip_address);





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

    ip_address VARCHAR(45) NULL,

    user_agent VARCHAR(500) NULL,

    created_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

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

    status ENUM(
        'PENDING',
        'PUBLISHED',
        'FAILED'
    ) NOT NULL DEFAULT 'PENDING',

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

