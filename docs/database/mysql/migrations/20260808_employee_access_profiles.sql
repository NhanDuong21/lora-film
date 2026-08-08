-- Phân quyền nhân viên theo nhóm nghiệp vụ, không tạo thêm role hệ thống.
-- Áp dụng cho auth_db trước khi triển khai auth-service tương ứng.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;

INSERT INTO auth_db.permissions (code, name, module, description)
VALUES
    ('CUSTOMER_VIEW', 'View customers', 'Customer Management', 'Read customer profiles'),
    ('PAYROLL_VIEW', 'View payroll', 'Payroll Management', 'Read employee payroll records'),
    ('PAYROLL_CREATE', 'Create payroll', 'Payroll Management', 'Create employee payroll records'),
    ('PAYROLL_UPDATE', 'Update payroll', 'Payroll Management', 'Update payroll before payment'),
    ('PAYROLL_APPROVE', 'Approve payroll', 'Payroll Management', 'Approve payroll for payment')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description);

CREATE TABLE IF NOT EXISTS auth_db.access_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_access_profiles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_db.access_profile_permissions (
    access_profile_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (access_profile_id, permission_id),
    CONSTRAINT fk_access_profile_permissions_profile
        FOREIGN KEY (access_profile_id) REFERENCES auth_db.access_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_profile_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES auth_db.permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_access_profile_column = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'auth_db'
      AND TABLE_NAME = 'accounts'
      AND COLUMN_NAME = 'access_profile_id'
);
SET @access_profile_ddl = IF(
    @has_access_profile_column = 0,
    'ALTER TABLE auth_db.accounts ADD COLUMN access_profile_id BIGINT NULL AFTER is_deleted, ADD INDEX idx_accounts_access_profile (access_profile_id), ADD CONSTRAINT fk_accounts_access_profile FOREIGN KEY (access_profile_id) REFERENCES auth_db.access_profiles(id)',
    'SELECT 1'
);
PREPARE access_profile_statement FROM @access_profile_ddl;
EXECUTE access_profile_statement;
DEALLOCATE PREPARE access_profile_statement;

INSERT INTO auth_db.access_profiles (code, name, description)
VALUES
    ('GENERAL_STAFF', 'Chưa phân nhóm nghiệp vụ',
     'Tài khoản nhân viên mới hoặc đang chờ xác định công việc. Chỉ có quyền cá nhân dùng chung.'),
    ('BOX_OFFICE', 'Nhân viên bán vé',
     'Tìm suất chiếu, tạo đơn đặt vé và thu tiền mặt tại quầy.'),
    ('TICKET_CHECKER', 'Nhân viên soát vé',
     'Tra cứu đặt vé và quét vé hợp lệ tại cửa phòng chiếu.'),
    ('ACCOUNTING', 'Nhân viên kế toán',
     'Theo dõi thanh toán, đối soát doanh thu và xử lý bảng lương.'),
    ('CUSTOMER_SERVICE', 'Nhân viên chăm sóc khách hàng',
     'Tra cứu khách hàng, đặt vé và hỗ trợ điểm thưởng.'),
    ('CINEMA_OPERATIONS', 'Nhân viên vận hành rạp',
     'Theo dõi phòng chiếu, suất chiếu và tình hình vận hành trong ngày.')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    is_active = TRUE;

DELETE app
FROM auth_db.access_profile_permissions app
JOIN auth_db.access_profiles ap ON ap.id = app.access_profile_id
WHERE ap.code IN ('GENERAL_STAFF', 'BOX_OFFICE', 'TICKET_CHECKER', 'ACCOUNTING', 'CUSTOMER_SERVICE', 'CINEMA_OPERATIONS');

INSERT INTO auth_db.access_profile_permissions (access_profile_id, permission_id)
SELECT ap.id, p.id
FROM auth_db.access_profiles ap
JOIN auth_db.permissions p ON (
       (ap.code = 'BOX_OFFICE' AND p.code IN (
           'MOVIE_VIEW', 'BOOKING_VIEW', 'BOOKING_MANAGE', 'PAYMENT_VIEW', 'PAYMENT_CASH_COLLECT', 'USER_VIEW'))
    OR (ap.code = 'TICKET_CHECKER' AND p.code IN (
           'MOVIE_VIEW', 'BOOKING_VIEW', 'TICKET_SCAN'))
    OR (ap.code = 'ACCOUNTING' AND p.code IN (
           'PAYMENT_VIEW', 'PAYMENT_RECONCILE', 'ANALYTICS_VIEW',
           'PAYROLL_VIEW', 'PAYROLL_CREATE', 'PAYROLL_UPDATE', 'PAYROLL_APPROVE'))
    OR (ap.code = 'CUSTOMER_SERVICE' AND p.code IN (
           'USER_VIEW', 'CUSTOMER_VIEW', 'BOOKING_VIEW', 'SCORE_MANAGE'))
    OR (ap.code = 'CINEMA_OPERATIONS' AND p.code IN (
           'MOVIE_VIEW', 'CINEMA_MANAGE', 'SHOWTIME_MANAGE', 'BOOKING_VIEW', 'ANALYTICS_VIEW'))
);

-- EMPLOYEE chỉ giữ quyền cá nhân dùng chung. Quyền công việc đến từ nhóm nghiệp vụ.
DELETE rp
FROM auth_db.roles_permissions rp
JOIN auth_db.roles r ON r.id = rp.role_id AND r.code = 'EMPLOYEE'
JOIN auth_db.permissions p ON p.id = rp.permission_id
WHERE p.code NOT IN (
    'EMPLOYEE_DASHBOARD_VIEW',
    'EMPLOYEE_SCHEDULE_VIEW',
    'EMPLOYEE_LEAVE_CREATE',
    'EMPLOYEE_ATTENDANCE_VIEW',
    'EMPLOYEE_ATTENDANCE_UPDATE',
    'EMPLOYEE_PAYROLL_VIEW'
);

INSERT IGNORE INTO auth_db.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth_db.roles r
JOIN auth_db.permissions p ON p.code IN (
    'EMPLOYEE_DASHBOARD_VIEW',
    'EMPLOYEE_SCHEDULE_VIEW',
    'EMPLOYEE_LEAVE_CREATE',
    'EMPLOYEE_ATTENDANCE_VIEW',
    'EMPLOYEE_ATTENDANCE_UPDATE',
    'EMPLOYEE_PAYROLL_VIEW'
)
WHERE r.code = 'EMPLOYEE';

-- Không đoán công việc của tài khoản cũ: đưa về nhóm chờ phân loại để admin rà soát.
UPDATE auth_db.accounts a
JOIN auth_db.account_roles ar ON ar.account_id = a.id
JOIN auth_db.roles r ON r.id = ar.role_id AND r.code = 'EMPLOYEE'
JOIN auth_db.access_profiles ap ON ap.code = 'GENERAL_STAFF'
SET a.access_profile_id = ap.id
WHERE a.access_profile_id IS NULL;

-- Quản trị hệ thống luôn có toàn bộ quyền, kể cả quyền được bổ sung sau này.
INSERT IGNORE INTO auth_db.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth_db.roles r
CROSS JOIN auth_db.permissions p
WHERE r.code = 'ADMIN';
