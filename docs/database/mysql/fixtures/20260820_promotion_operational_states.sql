-- Optional local/demo fixtures for Promotion Center operational walkthroughs.
-- Do not run in production. Idempotent by stable public_id values.

USE promotion_db;
START TRANSACTION;

INSERT INTO promotion_campaigns
    (public_id, code, name, slug, description, status, approval_status,
     legal_status, start_at, end_at, budget_amount, budget_used,
     budget_reserved, budget_remaining, max_redemptions, redemption_count,
     created_by, updated_by)
SELECT * FROM (
    SELECT 'f1000000-0000-4000-8000-000000000001' AS public_id,
           'FIX-DRAFT' AS code, '[Fixture] Bản nháp' AS name,
           'fixture-draft' AS slug, 'Campaign để kiểm tra author flow' AS description,
           'DRAFT' AS status, 'DRAFT' AS approval_status,
           'PENDING' AS legal_status, NOW(6) - INTERVAL 1 DAY AS start_at,
           NOW(6) + INTERVAL 30 DAY AS end_at, 1000000.00 AS budget_amount,
           0.00 AS budget_used, 0.00 AS budget_reserved,
           1000000.00 AS budget_remaining, 100 AS max_redemptions,
           0 AS redemption_count, 'fixture-loader' AS created_by,
           'fixture-loader' AS updated_by
    UNION ALL
    SELECT 'f1000000-0000-4000-8000-000000000002', 'FIX-PENDING',
           '[Fixture] Chờ duyệt', 'fixture-pending', 'Campaign maker-checker pending',
           'DRAFT', 'PENDING', 'PENDING', NOW(6) - INTERVAL 1 DAY,
           NOW(6) + INTERVAL 30 DAY, 2000000.00, 0.00, 0.00, 2000000.00,
           200, 0, 'fixture-author', 'fixture-author'
    UNION ALL
    SELECT 'f1000000-0000-4000-8000-000000000003', 'FIX-ACTIVE',
           '[Fixture] Đang hoạt động', 'fixture-active', 'Campaign có active hold',
           'ACTIVE', 'APPROVED', 'PASSED', NOW(6) - INTERVAL 1 DAY,
           NOW(6) + INTERVAL 30 DAY, 3000000.00, 0.00, 10000.00, 3000000.00,
           300, 0, 'fixture-author', 'fixture-approver'
    UNION ALL
    SELECT 'f1000000-0000-4000-8000-000000000004', 'FIX-PAUSED',
           '[Fixture] Tạm dừng', 'fixture-paused', 'Campaign có thể resume',
           'PAUSED', 'APPROVED', 'PASSED', NOW(6) - INTERVAL 1 DAY,
           NOW(6) + INTERVAL 30 DAY, 1000000.00, 150000.00, 0.00, 850000.00,
           100, 5, 'fixture-author', 'fixture-operator'
    UNION ALL
    SELECT 'f1000000-0000-4000-8000-000000000005', 'FIX-EXHAUSTED',
           '[Fixture] Hết hạn mức đơn', 'fixture-exhausted', 'Campaign đạt quota đơn',
           'ACTIVE', 'APPROVED', 'PASSED', NOW(6) - INTERVAL 1 DAY,
           NOW(6) + INTERVAL 30 DAY, 1000000.00, 250000.00, 0.00, 750000.00,
           10, 10, 'fixture-author', 'fixture-runtime'
    UNION ALL
    SELECT 'f1000000-0000-4000-8000-000000000006', 'FIX-BUDGET',
           '[Fixture] Hết ngân sách', 'fixture-budget', 'Campaign đạt budget ceiling',
           'ACTIVE', 'APPROVED', 'PASSED', NOW(6) - INTERVAL 1 DAY,
           NOW(6) + INTERVAL 30 DAY, 100000.00, 100000.00, 0.00, 0.00,
           100, 4, 'fixture-author', 'fixture-runtime'
    UNION ALL
    SELECT 'f1000000-0000-4000-8000-000000000007', 'FIX-KILLED',
           '[Fixture] Dừng khẩn cấp', 'fixture-killed', 'Campaign kill switch',
           'KILLED', 'APPROVED', 'PASSED', NOW(6) - INTERVAL 1 DAY,
           NOW(6) + INTERVAL 30 DAY, 1000000.00, 0.00, 0.00, 1000000.00,
           100, 0, 'fixture-author', 'fixture-operator'
) AS fixture
WHERE NOT EXISTS (
    SELECT 1 FROM promotion_campaigns existing
    WHERE existing.public_id = fixture.public_id
);

-- Keep fixture labels ASCII-safe so the file behaves consistently through
-- PowerShell, cmd, Docker stdin and different MySQL client code pages.
UPDATE promotion_campaigns
SET name = CASE code
        WHEN 'FIX-DRAFT' THEN '[Fixture] Ban nhap'
        WHEN 'FIX-PENDING' THEN '[Fixture] Cho duyet'
        WHEN 'FIX-ACTIVE' THEN '[Fixture] Dang hoat dong'
        WHEN 'FIX-PAUSED' THEN '[Fixture] Tam dung'
        WHEN 'FIX-EXHAUSTED' THEN '[Fixture] Het han muc don'
        WHEN 'FIX-BUDGET' THEN '[Fixture] Het ngan sach'
        WHEN 'FIX-KILLED' THEN '[Fixture] Dung khan cap'
        ELSE name
    END,
    description = CASE code
        WHEN 'FIX-DRAFT' THEN 'Campaign kiem tra author flow'
        WHEN 'FIX-PENDING' THEN 'Campaign maker-checker pending'
        WHEN 'FIX-ACTIVE' THEN 'Campaign co active hold'
        WHEN 'FIX-PAUSED' THEN 'Campaign co the resume'
        WHEN 'FIX-EXHAUSTED' THEN 'Campaign dat quota don'
        WHEN 'FIX-BUDGET' THEN 'Campaign dat budget ceiling'
        WHEN 'FIX-KILLED' THEN 'Campaign kill switch'
        ELSE description
    END
WHERE code LIKE 'FIX-%';

UPDATE promotion_campaigns
SET kill_switch = TRUE
WHERE public_id = 'f1000000-0000-4000-8000-000000000007';

INSERT INTO promotions
    (public_id, campaign_public_id, promotion_type, code, name, description,
     status, is_public, priority, stackable, conditions_json, actions_json,
     max_redemptions, redemption_count, max_redemptions_per_user,
     valid_from, valid_to, created_by, updated_by)
SELECT 'f2000000-0000-4000-8000-000000000001',
       'f1000000-0000-4000-8000-000000000003', 'AUTO', NULL,
       '[Fixture] AUTO giảm 10.000đ', 'Benefit cho ledger walkthrough',
       'ACTIVE', FALSE, 10, FALSE, JSON_OBJECT(),
       JSON_OBJECT('discountType', 'FIXED_AMOUNT', 'discountValue', 10000),
       300, 0, 1, NOW(6) - INTERVAL 1 DAY, NOW(6) + INTERVAL 30 DAY,
       'fixture-loader', 'fixture-loader'
WHERE NOT EXISTS (
    SELECT 1 FROM promotions
    WHERE public_id = 'f2000000-0000-4000-8000-000000000001'
);

INSERT INTO promotion_reservations
    (public_id, reservation_code, booking_public_id, user_public_id,
     reservation_scope_key, status, original_amount, discount_amount,
     final_amount, reservation_started_at, reservation_expired_at,
     rollback_at, rollback_reason, release_reason_type, released_at,
     released_by, source_service, source_reference, reason_detail,
     created_by, updated_by)
SELECT * FROM (
    SELECT 'f3000000-0000-4000-8000-000000000001' AS public_id,
           'FIX-HOLD-ACTIVE' AS reservation_code,
           'f4000000-0000-4000-8000-000000000001' AS booking_public_id,
           'f5000000-0000-4000-8000-000000000001' AS user_public_id,
           'FIXTURE:ACTIVE' AS reservation_scope_key, 'ACTIVE' AS status,
           100000.00 AS original_amount, 10000.00 AS discount_amount,
           90000.00 AS final_amount,
           NOW(6) - INTERVAL 2 MINUTE AS reservation_started_at,
           NOW(6) + INTERVAL 20 MINUTE AS reservation_expired_at,
           NULL AS rollback_at, NULL AS rollback_reason,
           NULL AS release_reason_type, NULL AS released_at,
           NULL AS released_by, NULL AS source_service,
           NULL AS source_reference, NULL AS reason_detail,
           'fixture-loader' AS created_by, 'fixture-loader' AS updated_by
    UNION ALL
    SELECT 'f3000000-0000-4000-8000-000000000002', 'FIX-HOLD-RELEASED',
           'f4000000-0000-4000-8000-000000000002',
           'f5000000-0000-4000-8000-000000000002', NULL,
           'RELEASED', 100000.00, 10000.00, 90000.00,
           NOW(6) - INTERVAL 20 MINUTE, NOW(6) - INTERVAL 5 MINUTE,
           NOW(6) - INTERVAL 10 MINUTE, 'Thanh toán thất bại',
           'PAYMENT_FAILED', NOW(6) - INTERVAL 10 MINUTE,
           'fixture-operator', 'payment-service', 'FIX-PAYMENT-FAILED',
           'Fixture kiểm tra taxonomy release', 'fixture-loader', 'fixture-operator'
) AS fixture
WHERE NOT EXISTS (
    SELECT 1 FROM promotion_reservations existing
    WHERE existing.public_id = fixture.public_id
);

INSERT INTO promotion_redemptions
    (public_id, reservation_public_id, user_public_id, promotion_public_id,
     campaign_public_id, promotion_type, promotion_name, promotion_priority,
     promotion_stackable, conditions_snapshot_json, actions_snapshot_json,
     sequence_no, booking_public_id, status, discount_amount, original_amount,
     final_amount, rollback_at, rollback_reason, created_by, updated_by)
SELECT * FROM (
    SELECT 'f6000000-0000-4000-8000-000000000001' AS public_id,
           'f3000000-0000-4000-8000-000000000001' AS reservation_public_id,
           'f5000000-0000-4000-8000-000000000001' AS user_public_id,
           'f2000000-0000-4000-8000-000000000001' AS promotion_public_id,
           'f1000000-0000-4000-8000-000000000003' AS campaign_public_id,
           'AUTO' AS promotion_type, '[Fixture] AUTO giảm 10.000đ' AS promotion_name,
           10 AS promotion_priority, FALSE AS promotion_stackable,
           JSON_OBJECT() AS conditions_snapshot_json,
           JSON_OBJECT('discountType', 'FIXED_AMOUNT', 'discountValue', 10000)
               AS actions_snapshot_json,
           1 AS sequence_no,
           'f4000000-0000-4000-8000-000000000001' AS booking_public_id,
           'RESERVED' AS status, 10000.00 AS discount_amount,
           100000.00 AS original_amount, 90000.00 AS final_amount,
           NULL AS rollback_at, NULL AS rollback_reason,
           'fixture-loader' AS created_by, 'fixture-loader' AS updated_by
    UNION ALL
    SELECT 'f6000000-0000-4000-8000-000000000002',
           'f3000000-0000-4000-8000-000000000002',
           'f5000000-0000-4000-8000-000000000002',
           'f2000000-0000-4000-8000-000000000001',
           'f1000000-0000-4000-8000-000000000003', 'AUTO',
           '[Fixture] AUTO giảm 10.000đ', 10, FALSE, JSON_OBJECT(),
           JSON_OBJECT('discountType', 'FIXED_AMOUNT', 'discountValue', 10000),
           1, 'f4000000-0000-4000-8000-000000000002', 'ROLLBACKED',
           10000.00, 100000.00, 90000.00, NOW(6) - INTERVAL 10 MINUTE,
           'PAYMENT_FAILED: Thanh toán thất bại', 'fixture-loader', 'fixture-operator'
) AS fixture
WHERE NOT EXISTS (
    SELECT 1 FROM promotion_redemptions existing
    WHERE existing.public_id = fixture.public_id
);

-- Keep fixture labels ASCII-safe when the script is piped through Windows
-- shells whose active code page is not UTF-8. Application-owned records are
-- intentionally left untouched.
UPDATE promotions
SET name = '[Fixture] AUTO giam 10.000d',
    description = 'Benefit cho ledger walkthrough'
WHERE public_id = 'f2000000-0000-4000-8000-000000000001';

UPDATE promotion_reservations
SET rollback_reason = 'Thanh toan that bai',
    reason_detail = 'Fixture kiem tra taxonomy release'
WHERE public_id = 'f3000000-0000-4000-8000-000000000002';

-- Reset the fixture-owned live hold on every run so the walkthrough remains
-- deterministic even when the local expiration scheduler has processed it.
UPDATE promotion_campaigns
SET budget_reserved = 10000.00
WHERE public_id = 'f1000000-0000-4000-8000-000000000003';

UPDATE promotion_reservations
SET status = 'ACTIVE',
    reservation_started_at = NOW(6) - INTERVAL 2 MINUTE,
    reservation_expired_at = NOW(6) + INTERVAL 20 MINUTE,
    rollback_at = NULL,
    rollback_reason = NULL,
    release_reason_type = NULL,
    released_at = NULL,
    released_by = NULL,
    source_service = NULL,
    source_reference = NULL,
    reason_detail = NULL,
    expiration_attempts = 0,
    expiration_error = NULL,
    expiration_last_attempt_at = NULL,
    expiration_next_attempt_at = NULL
WHERE public_id = 'f3000000-0000-4000-8000-000000000001';

UPDATE promotion_redemptions
SET promotion_name = '[Fixture] AUTO giam 10.000d',
    status = CASE
        WHEN public_id = 'f6000000-0000-4000-8000-000000000001'
            THEN 'RESERVED'
        ELSE status
    END,
    rollback_at = CASE
        WHEN public_id = 'f6000000-0000-4000-8000-000000000001'
            THEN NULL
        ELSE rollback_at
    END,
    rollback_reason = CASE
        WHEN public_id = 'f6000000-0000-4000-8000-000000000001'
            THEN NULL
        WHEN public_id = 'f6000000-0000-4000-8000-000000000002'
            THEN 'PAYMENT_FAILED: Thanh toan that bai'
        ELSE rollback_reason
    END
WHERE public_id IN (
    'f6000000-0000-4000-8000-000000000001',
    'f6000000-0000-4000-8000-000000000002'
);

COMMIT;
