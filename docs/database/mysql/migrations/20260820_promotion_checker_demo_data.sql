-- UAT-only maker/checker assignment for Smart Promotion.
-- The checker profile deliberately has no PROMOTION_AUTHOR capability.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;

START TRANSACTION;

INSERT INTO access_profiles (code, name, description, is_active)
VALUES (
    'PROMOTION_CHECKER',
    'Kiểm soát khuyến mãi',
    'Phê duyệt, kiểm tra pháp lý, phát hành và giám sát khuyến mãi; không được tạo hoặc gửi duyệt.',
    TRUE
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    is_active = TRUE;

DELETE app
FROM access_profile_permissions app
JOIN access_profiles profile ON profile.id = app.access_profile_id
WHERE profile.code = 'PROMOTION_CHECKER';

INSERT INTO access_profile_permissions (access_profile_id, permission_id)
SELECT profile.id, permission.id
FROM access_profiles profile
JOIN permissions permission ON permission.code IN (
    'PROMOTION_VIEW',
    'PROMOTION_APPROVE_STANDARD',
    'PROMOTION_APPROVE_HIGH_BUDGET',
    'PROMOTION_LEGAL_REVIEW',
    'PROMOTION_PUBLISH',
    'PROMOTION_OPERATE',
    'PROMOTION_AUDIT_VIEW'
)
WHERE profile.code = 'PROMOTION_CHECKER';

-- This is the existing local UAT manager account. Production environments
-- must assign the profile through account administration instead.
UPDATE accounts account
JOIN account_roles account_role ON account_role.account_id = account.id
JOIN roles role ON role.id = account_role.role_id AND role.code = 'MANAGER'
JOIN access_profiles profile ON profile.code = 'PROMOTION_CHECKER'
SET account.access_profile_id = profile.id
WHERE account.email = 'nhandt.ce190741@gmail.com'
  AND account.is_deleted = FALSE;

COMMIT;
