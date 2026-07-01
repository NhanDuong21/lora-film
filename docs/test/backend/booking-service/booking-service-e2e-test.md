# BÁO CÁO KIỂM THỬ E2E (END-TO-END TEST FLOW)

## 1. Thông tin chung
- **Service**: `booking-service`
- **Tính năng**: Core Booking, Seat Reservation & Background Expiration Engine
- **Mục tiêu**: Kiểm thử luồng nghiệp vụ từ lúc người dùng bắt đầu giữ chỗ, tạo đơn hàng cho đến khi hệ thống tự động dọn dẹp các đơn/ghế hết hạn.

---

## Hướng dẫn chung: Tạo Idempotency-Key

Các API nội bộ yêu cầu header `Idempotency-Key` để hỗ trợ cơ chế **Idempotency**.

### Cách tạo

1. Truy cập: **https://www.uuidgenerator.net/version4**
2. Sao chép một **UUID Version 4** được tạo trên trang web.
3. Dán giá trị đó vào header `Idempotency-Key` trong Swagger UI.

Ví dụ:

```text
Idempotency-Key: f8c72b64-b2d8-4e4d-9bad-b7f62f248b55
```

> **Lưu ý:**
>
> * Khi kiểm thử **Test Case 2 – Idempotency Replay**, sử dụng **cùng một `Idempotency-Key`** với Test Case 1.
> * Đối với mỗi yêu cầu thanh toán mới, hãy tạo **một `Idempotency-Key` mới**.
> * Giá trị `Idempotency-Key` phải là **UUID Version 4 (UUID v4)** hợp lệ.

---

## 2. Các Luồng Kiểm Thử (Test Flows)

### Flow 1: Giữ chỗ ngồi (Hold Seat) thành công
- **Ngữ cảnh**: Người dùng chọn ghế trên giao diện và bấm giữ chỗ.
- **Các bước thực hiện**:
  1. Gửi request `POST /api/bookings/seat-reservations`
  2. Gửi kèm Headers:
     - `Authorization`: `Bearer <user-jwt-token>`
  3. Gửi kèm Request Body:
     ```json
     {
       "showtimeId": 120,
       "seatIds": [10, 11]
     }
     ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về 201 Created cùng ID của nhóm giữ chỗ (Reservation Group ID).
    - Database chèn các bản ghi vào bảng `seat_reservations` với trạng thái `HELD` và `expires_at` là 5 phút tính từ hiện tại.
    - Redis set các key khóa ghế `booking:seat-lock:{showtimeId}:{seatId}` với TTL là 5 phút.
  - **Negative Case 1**: Gửi request chọn ghế đang bị người khác giữ -> API trả về `409 Conflict` (Seat already locked).
  - **Negative Case 2**: `showtimeId` không tồn tại -> API trả về `404 Not Found`.
  - **Negative Case 3**: Payload rỗng hoặc sai kiểu dữ liệu -> API trả về `400 Bad Request` (Validation error).
- **Kết quả thực tế**: PASS toàn bộ các case trên.

### Flow 2: Xung đột giữ chỗ (Concurrency Lock)
- **Ngữ cảnh**: Hai người dùng cùng lúc bấm chọn giữ cùng một ghế.
- **Các bước thực hiện**:
  1. Người dùng A gửi request giữ chỗ thành công cho ghế số 10.
  2. Người dùng B lập tức gửi request giữ chỗ cho ghế số 10 đó.
- **Kết quả mong đợi**:
  - Request của người dùng B bị từ chối với lỗi `409 Conflict` (Seat already locked).
  - Không có dữ liệu rác nào của người dùng B bị lọt vào Database.
- **Kết quả thực tế**: PASS.

### Flow 3: Tạo Booking (Đặt vé) thành công
- **Ngữ cảnh**: Người dùng xác nhận thanh toán rổ hàng đang giữ chỗ.
- **Các bước thực hiện**:
  1. Gửi request `POST /api/bookings`
  2. Gửi kèm Headers bắt buộc:
     - `Authorization`: `Bearer <user-jwt-token>`
     - `Idempotency-Key`: UUID (vd: `f47ac10b-58cc-4372-a567-0e02b2c3d479`)
  3. Gửi kèm Request Body:
     ```json
     {
       "reservationIds": [501, 502]
     }
     ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về 201 Created với `bookingCode` và tổng tiền.
    - Bảng `bookings` thêm bản ghi mới với trạng thái `PENDING_PAYMENT` (thời hạn 15 phút).
    - Các bản ghi trong `seat_reservations` chuyển trạng thái từ `HELD` sang `CONVERTED` và được gán `bookingId` tương ứng.
  - **Negative Case 1**: Các `reservationIds` truyền vào đã quá hạn 5 phút (trạng thái EXPIRED) -> API trả về `409 Conflict` (Reservation Expired).
  - **Negative Case 2**: Ghế đã thuộc về booking khác -> API trả về `409 Conflict` (Seat already booked).
  - **Negative Case 3**: Thiếu header `Idempotency-Key` -> API trả về `400 Bad Request`.
- **Kết quả thực tế**: PASS toàn bộ các case trên.

### Flow 4: Hủy tự động ghế bị giữ quá hạn (Seat Expiration Worker)
- **Ngữ cảnh**: Người dùng giữ ghế nhưng thoát trang không đi tiếp, quá 5 phút.
- **Các bước thực hiện**:
  1. Mô phỏng: Sửa trường `expires_at` của một ghế đang `HELD` thành 10 phút trước.
  2. Đợi `ReservationExpirationWorker` chạy ngầm.
- **Kết quả mong đợi**:
  - Worker quét thấy bản ghi và cập nhật trạng thái ghế thành `EXPIRED`.
  - Redis tự động bị ép xóa key tương ứng thông qua `SeatLockManager.evictLocks` để trả ghế lại cho người khác đặt.
- **Kết quả thực tế**: PASS. Hệ thống xử lý qua Sub-transaction an toàn.

### Flow 5: Hủy tự động Booking chưa thanh toán (Booking Expiration Worker)
- **Ngữ cảnh**: Người dùng đã tạo Booking thành công nhưng không chịu thanh toán quá 15 phút.
- **Các bước thực hiện**:
  1. Mô phỏng: Sửa trường `expires_at` của một booking đang `PENDING_PAYMENT` thành 20 phút trước.
  2. Đợi `BookingExpirationWorker` chạy ngầm.
- **Kết quả mong đợi**:
  - Worker quét thấy booking và cập nhật trạng thái booking thành `EXPIRED`.
  - Đệ quy tìm tất cả các `seat_reservations` có liên kết (đang ở trạng thái `CONVERTED`), chuyển chúng sang `EXPIRED`.
  - Ép xóa các key khóa ghế liên quan trên Redis.
- **Kết quả thực tế**: PASS. Batch hoạt động trơn tru.

### Flow 6: Chống trùng lặp (Idempotency)
- **Ngữ cảnh**: Network chập chờn, client retry tạo booking 2 lần.
- **Các bước thực hiện**:
  1. Gửi 2 request `POST /api/bookings` y hệt nhau, cùng Body và cùng một `Idempotency-Key` (vd: `f47ac10b-58cc-4372-a567-0e02b2c3d479`).
- **Kết quả mong đợi**:
  - Lần 1: Trả về 201 Created (Sinh mã booking và lưu DB).
  - Lần 2: Trả về kết quả lấy từ Cache (với cùng HTTP Status Code và Response Body), tuyệt đối không tạo thêm Booking thứ hai trong DB.
- **Kết quả thực tế**: PASS.

### Flow 7: Xác nhận thanh toán (Confirm Payment) qua Internal API
- **Ngữ cảnh**: Hệ thống thanh toán (payment-service) gọi sang booking-service để xác nhận giao dịch thành công, yêu cầu chốt đơn và sinh vé.
- **Các bước thực hiện**:
  1. Gửi request `POST /internal/bookings/{bookingId}/confirm-payment`
  2. Gửi kèm Headers bắt buộc:
     - `X-Internal-Token`: (Chuỗi bí mật chia sẻ giữa các microservice, ví dụ `default-internal-secret`)
     - `Idempotency-Key`: UUID để chống trùng lặp (vd: `550e8400-e29b-41d4-a716-446655440000`)
  3. Gửi kèm Request Body:
     ```json
     {
       "paidAmount": 240000,
       "transactionId": "TXN_123456",
       "paymentMethod": "MOMO"
     }
     ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về 200 OK cùng thông tin chi tiết các vé (`tickets`) vừa được tạo.
    - Bảng `bookings` cập nhật trạng thái từ `PENDING_PAYMENT` sang `CONFIRMED`.
    - Các bản ghi trong `tickets` được tạo tương ứng với từng ghế trong đơn hàng, kế thừa giá tiền từ tổng hóa đơn.
    - Các khóa ghế trên Redis được gỡ bỏ hoàn toàn.
  - **Negative Case 1**: Thiếu hoặc sai `X-Internal-Token` -> API trả về `401 Unauthorized`.
  - **Negative Case 2**: Số tiền `paidAmount` không khớp với tổng hóa đơn -> API trả về `409 Conflict`.
  - **Negative Case 3**: Booking đã quá hạn thanh toán (hơn 15 phút) -> API trả về `409 Conflict` (Booking Expired).
  - **Negative Case 4**: Gọi lại cùng `Idempotency-Key` (Idempotency Replay) -> API trả về dữ liệu cache an toàn (200 OK) mà không sinh thêm vé mới.
  - **Negative Case 5**: Hai request đồng thời gửi tới (Concurrency) -> Một request trả về 200 OK, request còn lại bị từ chối với `409 Conflict` (nhờ Optimistic Locking).
- **Kết quả thực tế**: PASS toàn bộ các case trên.

### Flow 8: Ghi nhận thanh toán thất bại (Fail Payment) qua Internal API
- **Ngữ cảnh**: Hệ thống thanh toán báo về booking-service rằng giao dịch đã lỗi hoặc bị người dùng hủy bỏ.
- **Các bước thực hiện**:
  1. Gửi request `POST /internal/bookings/{bookingId}/fail-payment`
  2. Gửi kèm Headers: `X-Internal-Token` và `Idempotency-Key`
  3. Gửi kèm Request Body:
     ```json
     {
       "reason": "USER_CANCELLED",
       "transactionId": "TXN_789012"
     }
     ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về 200 OK ghi nhận sự cố thành công.
    - Cực kỳ quan trọng: Trạng thái booking **vẫn giữ nguyên** là `PENDING_PAYMENT` (không tự động hủy, cho phép người dùng retry lại thanh toán trong khoảng thời gian 15 phút còn lại).
  - **Negative Case 1**: Booking không tồn tại -> API trả về `404 Not Found`.
  - **Negative Case 2**: Gửi sai hoặc thiếu Token -> API trả về `401 Unauthorized`.
- **Kết quả thực tế**: PASS toàn bộ các case trên.

---

## 3. Tổng hợp kết quả tự động (Automated Test Metrics)
- **Môi trường chạy**: Local Integration Tests với H2 / MySQL & Redis giả lập.
- **Lệnh thực thi**: `mvn clean verify -Dnet.bytebuddy.experimental=true`
- **Tổng số Test Cases**: 42 (bao gồm cả Unit và Integration Tests)
  - Core Booking & Idempotency: 25 tests
  - Background Expiration Workers: 7 tests
  - Internal Payment APIs (Concurrency, Token, Idempotency): 10 tests
- **Tỉ lệ Pass**: 100% (0 Failures, 0 Errors, 0 Skipped).
- **Đánh giá**: Tất cả các luồng nghiệp vụ (E2E flows) đều hoạt động chuẩn xác theo thiết kế. Các contract của Internal APIs được đảm bảo 100%. Sẵn sàng tích hợp.
