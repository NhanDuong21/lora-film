-- Create the replacement ticket-checker account after Trần Minh Nhân
-- moves to the independent accounting-control profile.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

-- Keep the controller's HR assignment aligned with the accounting profile.
UPDATE user_db.employees e
JOIN user_db.positions p ON p.code = 'FINANCE_ADMIN' AND p.is_deleted = FALSE
SET e.department_id = p.department_id,
    e.position_id = p.id,
    e.cinema_public_id = NULL,
    e.version = e.version + 1
WHERE e.account_id = (
    SELECT id FROM auth_db.accounts WHERE email = 'nhan15022022@gmail.com'
);

-- Reuse a hash belonging to an account with the shared local-demo password.
-- The plaintext password is never stored in this migration.
INSERT INTO auth_db.accounts (
    email, password_hash, status, is_enabled, is_deleted, access_profile_id
)
SELECT
    'lamthoaithanh@gmail.com', source.password_hash, 'ACTIVE', TRUE, FALSE, profile.id
FROM auth_db.accounts source
JOIN auth_db.access_profiles profile
    ON profile.code = 'TICKET_CHECKER' AND profile.is_active = TRUE
WHERE source.email = 'nhan15022022@gmail.com'
LIMIT 1
ON DUPLICATE KEY UPDATE
    status = 'ACTIVE',
    is_enabled = TRUE,
    is_deleted = FALSE,
    access_profile_id = VALUES(access_profile_id);

SET @lam_account_id = (
    SELECT id FROM auth_db.accounts WHERE email = 'lamthoaithanh@gmail.com'
);

INSERT INTO auth_db.account_roles (account_id, role_id)
SELECT @lam_account_id, r.id
FROM auth_db.roles r
WHERE r.code = 'EMPLOYEE'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_db.users (
    account_id, full_name, email, status, account_type, is_deleted
)
VALUES (
    @lam_account_id,
    CONVERT(0x4CC3A26D2054686FE1BAA169205468616E68 USING utf8mb4),
    'lamthoaithanh@gmail.com',
    'ACTIVE', 'WORKFORCE', FALSE
)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    status = 'ACTIVE',
    account_type = 'WORKFORCE',
    is_deleted = FALSE;

INSERT INTO user_db.employees (
    account_id, employee_code, department_id, position_id,
    base_salary, hire_date, cinema_public_id, status, is_deleted
)
SELECT
    @lam_account_id,
    CONCAT('EMP-', LPAD(@lam_account_id, 4, '0')),
    p.department_id,
    p.id,
    10500000.00,
    '2026-08-16',
    'b1575c2d-9081-11f1-bf65-0ebab02bf6f5',
    'ACTIVE',
    FALSE
FROM user_db.positions p
WHERE p.code = 'TICKET_CHECKER' AND p.is_deleted = FALSE
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    position_id = VALUES(position_id),
    cinema_public_id = VALUES(cinema_public_id),
    status = 'ACTIVE',
    is_deleted = FALSE,
    version = version + 1;

COMMIT;
