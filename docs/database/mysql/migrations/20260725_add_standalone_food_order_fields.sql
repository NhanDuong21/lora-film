-- 20260725_add_standalone_food_order_fields.sql

ALTER TABLE booking_food_orders MODIFY booking_id BIGINT NULL COMMENT 'Mã đơn đặt vé liên kết (FK) (NULL nếu là đơn rời)';
ALTER TABLE booking_food_orders ADD COLUMN user_id BIGINT NULL COMMENT 'Mã khách hàng (Dùng khi mua rời không qua booking)' AFTER booking_id;
ALTER TABLE booking_food_orders ADD COLUMN payment_status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING' COMMENT 'Trạng thái giao dịch thanh toán của đơn đồ ăn' AFTER status;
ALTER TABLE booking_food_orders ADD COLUMN payment_method_snapshot VARCHAR(50) COMMENT 'Phương thức thanh toán (Ví dụ: CREDIT_CARD, MOMO)' AFTER payment_status;
ALTER TABLE booking_food_orders ADD COLUMN payment_provider VARCHAR(50) COMMENT 'Đơn vị cung cấp cổng thanh toán (Ví dụ: Stripe, MoMo)' AFTER payment_method_snapshot;
ALTER TABLE booking_food_orders ADD COLUMN payment_reference VARCHAR(100) COMMENT 'Mã giao dịch tham chiếu từ phía Cổng thanh toán' AFTER payment_provider;
