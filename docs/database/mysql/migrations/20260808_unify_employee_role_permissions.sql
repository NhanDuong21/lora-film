-- Unify the workforce identity on EMPLOYEE and move employee features to permissions.
-- Apply to auth_db before deploying the matching auth/user/payment services.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO auth_db.permissions (code, name, module, description)
VALUES
    ('EMPLOYEE_DASHBOARD_VIEW', 'View employee dashboard', 'Employee Self Service',
     'View the employee workspace dashboard'),
    ('EMPLOYEE_SCHEDULE_VIEW', 'View own work schedule and leave requests', 'Employee Self Service',
     'View assigned shifts and own leave request history'),
    ('EMPLOYEE_LEAVE_CREATE', 'Create and cancel own leave requests', 'Employee Self Service',
     'Create and cancel leave requests owned by the authenticated employee'),
    ('EMPLOYEE_ATTENDANCE_VIEW', 'View own attendance', 'Employee Self Service',
     'View attendance records owned by the authenticated employee'),
    ('EMPLOYEE_ATTENDANCE_UPDATE', 'Check in and check out own shifts', 'Employee Self Service',
     'Record check-in and check-out for assigned shifts'),
    ('EMPLOYEE_PAYROLL_VIEW', 'View own payroll', 'Employee Self Service',
     'View payroll records owned by the authenticated employee'),
    ('PAYMENT_CASH_COLLECT', 'Collect cash payments at the counter', 'Payment Operations',
     'Look up bookings and create, collect or cancel counter cash payments')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    module = VALUES(module),
    description = VALUES(description);

INSERT INTO auth_db.roles (code, name, description)
SELECT 'EMPLOYEE', 'EMPLOYEE', 'Employee'
WHERE NOT EXISTS (
    SELECT 1 FROM auth_db.roles WHERE code = 'EMPLOYEE'
);

SET @employee_role_id = (
    SELECT id FROM auth_db.roles WHERE code = 'EMPLOYEE' LIMIT 1
);
SET @staff_role_id = (
    SELECT id FROM auth_db.roles WHERE code = 'STAFF' LIMIT 1
);

-- Preserve permissions and account assignments from the retired STAFF role.
INSERT IGNORE INTO auth_db.roles_permissions (role_id, permission_id)
SELECT @employee_role_id, rp.permission_id
FROM auth_db.roles_permissions rp
WHERE rp.role_id = @staff_role_id
  AND @staff_role_id IS NOT NULL;

INSERT IGNORE INTO auth_db.account_roles (account_id, role_id)
SELECT ar.account_id, @employee_role_id
FROM auth_db.account_roles ar
WHERE ar.role_id = @staff_role_id
  AND @staff_role_id IS NOT NULL;

DELETE FROM auth_db.account_roles
WHERE role_id = @staff_role_id
  AND @staff_role_id IS NOT NULL;

DELETE FROM auth_db.roles
WHERE id = @staff_role_id
  AND @staff_role_id IS NOT NULL;

-- Give the canonical EMPLOYEE role a working self-service baseline. These
-- assignments remain editable from the role administration screen afterwards.
INSERT IGNORE INTO auth_db.roles_permissions (role_id, permission_id)
SELECT @employee_role_id, p.id
FROM auth_db.permissions p
WHERE p.code IN (
    'EMPLOYEE_DASHBOARD_VIEW',
    'EMPLOYEE_SCHEDULE_VIEW',
    'EMPLOYEE_LEAVE_CREATE',
    'EMPLOYEE_ATTENDANCE_VIEW',
    'EMPLOYEE_ATTENDANCE_UPDATE',
    'EMPLOYEE_PAYROLL_VIEW',
    'PAYMENT_CASH_COLLECT'
);

-- Keep the built-in administrator complete even when it predates these permissions.
INSERT IGNORE INTO auth_db.roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth_db.roles r
JOIN auth_db.permissions p ON p.code IN (
    'EMPLOYEE_DASHBOARD_VIEW',
    'EMPLOYEE_SCHEDULE_VIEW',
    'EMPLOYEE_LEAVE_CREATE',
    'EMPLOYEE_ATTENDANCE_VIEW',
    'EMPLOYEE_ATTENDANCE_UPDATE',
    'EMPLOYEE_PAYROLL_VIEW',
    'PAYMENT_CASH_COLLECT'
)
WHERE r.code = 'ADMIN';
