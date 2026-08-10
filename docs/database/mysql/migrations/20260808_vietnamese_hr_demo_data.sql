-- Việt hóa dữ liệu mẫu cho khu vực Nhân sự.
-- Script chỉ cập nhật các bản ghi demo theo khóa ổn định và có thể chạy lại an toàn.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE user_db.users
SET full_name = CASE account_id
    WHEN 1 THEN 'Nguyễn Minh Anh'
    WHEN 2 THEN 'Trần Quốc Huy'
    WHEN 3 THEN 'Lê Thu Hà'
    WHEN 7 THEN 'Võ Ngọc Mai'
    WHEN 8 THEN 'Đỗ Minh Quân'
    ELSE full_name
END
WHERE account_id IN (1, 2, 3, 7, 8)
  AND account_type = 'WORKFORCE';

UPDATE user_db.departments
SET name = CASE code
        WHEN 'OPS' THEN 'Vận hành rạp'
        WHEN 'CS' THEN 'Chăm sóc khách hàng'
        WHEN 'FIN' THEN 'Tài chính - Kế toán'
        ELSE name
    END,
    description = CASE code
        WHEN 'OPS' THEN 'Điều phối hoạt động rạp, quầy vé và chất lượng phục vụ.'
        WHEN 'CS' THEN 'Tiếp nhận và xử lý yêu cầu hỗ trợ khách hàng.'
        WHEN 'FIN' THEN 'Quản lý thu chi, bảng lương và đối soát thanh toán.'
        ELSE description
    END
WHERE code IN ('OPS', 'CS', 'FIN');

UPDATE user_db.positions
SET title = CASE code
        WHEN 'OPS_MANAGER' THEN 'Quản lý vận hành'
        WHEN 'BOX_OFFICE' THEN 'Nhân viên quầy vé'
        WHEN 'CUSTOMER_CARE' THEN 'Chuyên viên chăm sóc khách hàng'
        WHEN 'FINANCE_ADMIN' THEN 'Chuyên viên tài chính'
        ELSE title
    END,
    description = CASE code
        WHEN 'OPS_MANAGER' THEN 'Điều phối nhân sự và hoạt động vận hành tại rạp.'
        WHEN 'BOX_OFFICE' THEN 'Bán vé, hướng dẫn và hỗ trợ khách tại quầy.'
        WHEN 'CUSTOMER_CARE' THEN 'Tiếp nhận và giải quyết yêu cầu của khách hàng.'
        WHEN 'FINANCE_ADMIN' THEN 'Theo dõi thu chi, bảng lương và đối soát.'
        ELSE description
    END
WHERE code IN ('OPS_MANAGER', 'BOX_OFFICE', 'CUSTOMER_CARE', 'FINANCE_ADMIN');

-- Giữ nguyên ID, quyền và mật khẩu của hai tài khoản E2E; chỉ đổi tên/email hiển thị.
UPDATE auth_db.accounts
SET email = CASE id
    WHEN 7 THEN 'ngoc.mai@lorafilm.local'
    WHEN 8 THEN 'minh.quan@lorafilm.local'
    ELSE email
END
WHERE (id = 7 AND email IN ('e2e.approver.20260808@lorafilm.local', 'ngoc.mai@lorafilm.local'))
   OR (id = 8 AND email IN ('e2e.staff.20260808@lorafilm.local', 'minh.quan@lorafilm.local'));

UPDATE user_db.users
SET email = CASE account_id
    WHEN 7 THEN 'ngoc.mai@lorafilm.local'
    WHEN 8 THEN 'minh.quan@lorafilm.local'
    ELSE email
END
WHERE (account_id = 7 AND email IN ('e2e.approver.20260808@lorafilm.local', 'ngoc.mai@lorafilm.local'))
   OR (account_id = 8 AND email IN ('e2e.staff.20260808@lorafilm.local', 'minh.quan@lorafilm.local'));
