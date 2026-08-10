-- Gán rạp mẫu cho tài khoản MANAGER dùng trong môi trường demo/local.
-- Chạy sau 20260808_manager_cinema_scope.sql và có thể chạy lại an toàn.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

INSERT IGNORE INTO auth_db.manager_cinema_assignments (account_id, cinema_public_id)
SELECT account.id, cinema.public_id
FROM auth_db.accounts account
JOIN auth_db.account_roles account_role ON account_role.account_id = account.id
JOIN auth_db.roles role ON role.id = account_role.role_id AND role.code = 'MANAGER'
JOIN movie_db.cinemas cinema
    ON cinema.slug = 'lorafilm-landmark-81'
   AND cinema.deleted_at IS NULL
WHERE account.email = 'nhandt.ce190741@gmail.com';

COMMIT;

SELECT account.email,
       role.code AS role_code,
       assignment.cinema_public_id,
       cinema.name AS cinema_name
FROM auth_db.accounts account
JOIN auth_db.account_roles account_role ON account_role.account_id = account.id
JOIN auth_db.roles role ON role.id = account_role.role_id
LEFT JOIN auth_db.manager_cinema_assignments assignment ON assignment.account_id = account.id
LEFT JOIN movie_db.cinemas cinema ON cinema.public_id = assignment.cinema_public_id
WHERE account.email = 'nhandt.ce190741@gmail.com';
