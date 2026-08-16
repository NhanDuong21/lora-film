-- Demo assignment for the independent accounting controller.
-- The access-profile model allows one active business profile per employee account.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;

START TRANSACTION;

UPDATE accounts a
JOIN account_roles ar ON ar.account_id = a.id
JOIN roles r ON r.id = ar.role_id AND r.code = 'EMPLOYEE'
JOIN access_profiles ap ON ap.code = 'ACCOUNTING_CONTROL' AND ap.is_active = TRUE
SET a.access_profile_id = ap.id
WHERE a.email = 'nhan15022022@gmail.com'
  AND a.status = 'ACTIVE'
  AND a.is_deleted = FALSE;

COMMIT;
