CREATE DATABASE IF NOT EXISTS auth_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE auth_db;

DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính role',

    name VARCHAR(50) NOT NULL COMMENT 'Tên quyền: ADMIN, STAFF, USER',

    description VARCHAR(255) NULL COMMENT 'Mô tả quyền',

    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng phân quyền tài khoản';


CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính account, dùng làm account_id cho service khác',

	email VARCHAR(150) NOT NULL COMMENT 'Email đăng nhập, không được trùng',

    password VARCHAR(255) NOT NULL COMMENT 'Mật khẩu đã hash, không lưu plain text',

    role_id BIGINT NOT NULL COMMENT 'Khóa ngoại tới bảng roles',

    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED', 'BANNED')
        NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'Trạng thái tài khoản',

	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo tài khoản',

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật tài khoản',

    deleted_at DATETIME NULL COMMENT 'Soft delete tài khoản',



    CONSTRAINT uk_accounts_email UNIQUE (email),

    CONSTRAINT fk_accounts_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng tài khoản phục vụ đăng ký, đăng nhập và phân quyền';


CREATE DATABASE IF NOT EXISTS user_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE user_db;

DROP TABLE IF EXISTS user_profiles;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính profile',

    account_id BIGINT NOT NULL COMMENT 'ID account từ Authentication Service, không tạo foreign key chéo database',

    full_name VARCHAR(150) NOT NULL COMMENT 'Họ tên đầy đủ',

    phone_number VARCHAR(20) NULL COMMENT 'Số điện thoại',

    identity_card VARCHAR(20) NULL COMMENT 'CCCD/CMND, chỉ lưu nếu thật sự cần',

    date_of_birth DATE NULL COMMENT 'Ngày sinh',

    gender ENUM('MALE', 'FEMALE', 'OTHER') NULL COMMENT 'Giới tính',

    avatar_url VARCHAR(500) NULL COMMENT 'Đường dẫn ảnh đại diện',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo profile',

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật profile',

    deleted_at DATETIME NULL COMMENT 'Soft delete profile',

    CONSTRAINT uk_user_profiles_account_id UNIQUE (account_id),

    CONSTRAINT uk_user_profiles_phone UNIQUE (phone_number),

    CONSTRAINT uk_user_profiles_identity_card UNIQUE (identity_card)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng hồ sơ người dùng thuộc User Service';