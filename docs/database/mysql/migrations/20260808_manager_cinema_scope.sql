-- Giới hạn tài khoản MANAGER theo đúng rạp được quản trị viên phân công.
-- Bảng lưu public ID của rạp để auth-service không phụ thuộc khóa nội bộ của movie-service.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;

CREATE TABLE IF NOT EXISTS manager_cinema_assignments (
    account_id BIGINT NOT NULL,
    cinema_public_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, cinema_public_id),
    CONSTRAINT fk_manager_cinema_assignments_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    INDEX idx_manager_cinema_assignments_cinema (cinema_public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Đồng bộ kiểu dữ liệu với ánh xạ JPA nếu migration từng được chạy bằng bản cũ dùng CHAR(36).
ALTER TABLE manager_cinema_assignments
    MODIFY COLUMN cinema_public_id VARCHAR(36) NOT NULL;

-- Phòng trường hợp tài khoản đã đổi khỏi MANAGER trước khi migration được chạy lại.
DELETE assignment
FROM manager_cinema_assignments assignment
JOIN accounts account ON account.id = assignment.account_id
LEFT JOIN account_roles account_role ON account_role.account_id = account.id
LEFT JOIN roles role ON role.id = account_role.role_id AND role.code = 'MANAGER'
WHERE role.id IS NULL;
