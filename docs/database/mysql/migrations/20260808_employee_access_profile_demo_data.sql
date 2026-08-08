-- Dữ liệu demo để kiểm thử các nhóm nghiệp vụ nhân viên.
-- Chạy sau 20260808_employee_access_profiles.sql và có thể chạy lại an toàn.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

INSERT INTO user_db.positions (code, title, description, department_id, is_deleted)
SELECT 'TICKET_CHECKER', 'Nhân viên soát vé',
       'Kiểm tra và xác nhận vé hợp lệ tại cửa phòng chiếu.', d.id, FALSE
FROM user_db.departments d
WHERE d.code = 'OPS'
  AND NOT EXISTS (SELECT 1 FROM user_db.positions p WHERE p.code = 'TICKET_CHECKER');

UPDATE user_db.positions
SET title = 'Nhân viên soát vé',
    description = 'Kiểm tra và xác nhận vé hợp lệ tại cửa phòng chiếu.',
    is_deleted = FALSE
WHERE code = 'TICKET_CHECKER';

UPDATE auth_db.accounts a
JOIN auth_db.access_profiles ap ON ap.code = 'BOX_OFFICE'
SET a.access_profile_id = ap.id
WHERE a.email = 'nhannhinhanh63@gmail.com';

UPDATE auth_db.accounts a
JOIN auth_db.access_profiles ap ON ap.code = 'TICKET_CHECKER'
SET a.access_profile_id = ap.id
WHERE a.email = 'nhan15022022@gmail.com';

DELETE ar
FROM auth_db.account_roles ar
JOIN auth_db.accounts a ON a.id = ar.account_id
WHERE a.email = 'duongthanhphuong076@gmail.com';

INSERT INTO auth_db.account_roles (account_id, role_id, assigned_by)
SELECT a.id, r.id, root_account.id
FROM auth_db.accounts a
JOIN auth_db.roles r ON r.code = 'EMPLOYEE'
LEFT JOIN auth_db.accounts root_account ON root_account.email = 'admin@gmail.com'
WHERE a.email = 'duongthanhphuong076@gmail.com'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

UPDATE auth_db.accounts a
JOIN auth_db.access_profiles ap ON ap.code = 'ACCOUNTING'
SET a.access_profile_id = ap.id
WHERE a.email = 'duongthanhphuong076@gmail.com';

UPDATE user_db.users
SET account_type = 'WORKFORCE', status = 'ACTIVE', is_deleted = FALSE
WHERE email = 'duongthanhphuong076@gmail.com';

DELETE cp
FROM user_db.customer_profiles cp
JOIN user_db.users u ON u.account_id = cp.account_id
WHERE u.email = 'duongthanhphuong076@gmail.com';

INSERT INTO user_db.employees
    (account_id, employee_code, department_id, position_id, base_salary, hire_date, status, version, is_deleted)
SELECT u.account_id, CONCAT('EMP-', LPAD(u.account_id, 4, '0')), d.id, p.id,
       14000000.00, CURDATE(), 'ACTIVE', 0, FALSE
FROM user_db.users u
JOIN user_db.departments d ON d.code = 'FIN'
JOIN user_db.positions p ON p.department_id = d.id AND p.code = 'FINANCE_ADMIN'
WHERE u.email = 'duongthanhphuong076@gmail.com'
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    position_id = VALUES(position_id),
    base_salary = VALUES(base_salary),
    status = 'ACTIVE',
    is_deleted = FALSE;

UPDATE user_db.employees e
JOIN user_db.users u ON u.account_id = e.account_id AND u.email = 'nhannhinhanh63@gmail.com'
JOIN user_db.positions p ON p.code = 'BOX_OFFICE'
SET e.department_id = p.department_id, e.position_id = p.id, e.status = 'ACTIVE', e.is_deleted = FALSE;

UPDATE user_db.employees e
JOIN user_db.users u ON u.account_id = e.account_id AND u.email = 'nhan15022022@gmail.com'
JOIN user_db.positions p ON p.code = 'TICKET_CHECKER'
SET e.department_id = p.department_id, e.position_id = p.id, e.status = 'ACTIVE', e.is_deleted = FALSE;

COMMIT;
