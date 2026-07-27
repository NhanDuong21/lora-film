# Payment Release 1 — Sandbox và kiểm thử tay

## 1. Chuẩn bị

1. Chạy thủ công `docs/database/mysql/payment-service-schema.sql` trên schema
   Payment rỗng.
2. Payment Service dùng `spring.jpa.hibernate.ddl-auto=validate`; không dùng
   Flyway/Liquibase và không tự sửa DDL.
3. Sao chép các key trong
   `server/payment-service/src/main/resources/application.example.properties`
   sang `application.properties` local hoặc khai báo environment variables.
4. Khởi động MySQL, Kafka, Booking Service, Payment Service, Gateway và client.
5. Booking Service phải có internal token trùng với token Payment cấu hình.

Không đưa credential sandbox, JWT secret hoặc internal token thật vào Git.

## 2. Biến môi trường chính

- `PAYMENT_BOOKING_SERVICE_URL`
- `PAYMENT_BOOKING_INTERNAL_TOKEN`
- `PAYMENT_FRONTEND_RETURN_URL`
- `PAYMENT_ANALYTICS_TOPIC`
- `VNPAY_ENABLED`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL`
- `MOMO_ENABLED`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY`,
  `MOMO_REDIRECT_URL`, `MOMO_IPN_URL`
- `PAYMENT_MOCK_ENABLED` chỉ dùng local/test.

Callback sandbox phải là URL HTTPS công khai trỏ đúng:

- VNPay: `/api/payments/callback/vnpay`
- MoMo: `/api/payments/callback/momo`

## 3. Kịch bản customer

1. Tạo Booking, chọn bắp nước và finalize checkout.
2. Chọn VNPay hoặc MoMo.
3. Xác nhận request `POST /api/payments` chỉ gửi
   `bookingPublicId`, `paymentMethod` và `Idempotency-Key`.
4. Refresh/retry mạng phải trả cùng `paymentPublicId`.
5. Hoàn tất/thất bại/hủy trên sandbox.
6. Return page phải polling Payment; Return URL một mình không được xác nhận đơn.
7. Với SUCCESS, kiểm tra Payment SUCCESS, Booking CONFIRMED, reservations BOOKED
   và vé được phát hành.
8. Gửi lại callback giống hệt: không tạo side effect thứ hai.

## 4. Kịch bản CASH

1. Đăng nhập EMPLOYEE/SUPERVISOR/ADMIN.
2. Mở “Thu tiền tại quầy”, tra theo Booking UUID hoặc mã đơn.
3. Xác nhận số tiền hiển thị đúng Booking đã finalization.
4. Tạo CASH attempt.
5. Nhập thiếu tiền: phải bị từ chối.
6. Nhập đủ/dư: Payment SUCCESS và server tính tiền thừa.
7. Collect lại cùng key: trả cùng kết quả, không tạo giao dịch mới.

## 5. Kịch bản vận hành

- Tắt Kafka, tạo Payment SUCCESS và để Booking chấp nhận: Analytics outbox còn
  giữ lại; bật Kafka để worker gửi lại.
- Cho Booking trả conflict/reconciliation: Booking outbox hoàn thành, tạo case
  đối soát và không có Analytics revenue.
- Replay webhook chữ ký sai phải bị cấm.
- Replay outbox giữ nguyên event ID.
- ACCOUNTANT xem/lọc/export được nhưng không replay/assign/resolve.
- ADMIN assign và resolve reconciliation với mã xử lý + ghi chú bắt buộc.
- Restart Payment giữa provider request/callback không được tạo attempt trùng.

## 6. Giới hạn Release 1

Provider refund chưa triển khai. Trạng thái hoàn tiền chỉ được xử lý ở release
sau khi có hợp đồng settlement và API provider tương ứng.
