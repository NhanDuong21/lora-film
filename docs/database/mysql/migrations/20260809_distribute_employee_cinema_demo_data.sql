-- Phân bổ dữ liệu demo sang nhiều rạp để kiểm tra bộ lọc và phạm vi quản lý nhân sự.
-- Có thể chạy lại an toàn sau 20260808_manager_workforce_scope.sql.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE user_db.employees employee
JOIN user_db.users user_account ON user_account.account_id = employee.account_id
JOIN movie_db.cinemas cinema ON cinema.slug = 'lorafilm-landmark-81' AND cinema.deleted_at IS NULL
SET employee.cinema_public_id = cinema.public_id
WHERE user_account.email IN (
    'nhandt.ce190741@gmail.com',
    'nhannhinhanh63@gmail.com'
);

UPDATE user_db.employees employee
JOIN user_db.users user_account ON user_account.account_id = employee.account_id
JOIN movie_db.cinemas cinema ON cinema.slug = 'lorafilm-crescent-mall' AND cinema.deleted_at IS NULL
SET employee.cinema_public_id = cinema.public_id
WHERE user_account.email = 'nhan15022022@gmail.com';

UPDATE user_db.employees employee
JOIN user_db.users user_account ON user_account.account_id = employee.account_id
JOIN movie_db.cinemas cinema ON cinema.slug = 'lorafilm-thu-duc-central' AND cinema.deleted_at IS NULL
SET employee.cinema_public_id = cinema.public_id
WHERE user_account.email = 'duongthanhphuong076@gmail.com';

SELECT employee.employee_code,
       user_account.email,
       cinema.name AS cinema_name
FROM user_db.employees employee
JOIN user_db.users user_account ON user_account.account_id = employee.account_id
LEFT JOIN movie_db.cinemas cinema ON cinema.public_id = employee.cinema_public_id
ORDER BY employee.employee_code;
