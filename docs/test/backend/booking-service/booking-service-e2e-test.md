# BÁO CÁO KIỂM THỬ E2E (END-TO-END TEST FLOW)

## 1. Thông tin chung
- **Service**: `booking-service`
- **Tính năng**: Core Booking, Seat Reservation & Background Expiration Engine
- **Mục tiêu**: Kiểm thử luồng nghiệp vụ từ lúc người dùng bắt đầu giữ chỗ, tạo đơn hàng cho đến khi hệ thống tự động dọn dẹp các đơn/ghế hết hạn.

---

## 2. Các Luồng Kiểm Thử (Test Flows)

### Flow 1: Giữ chỗ ngồi (Hold Seat) thành công
- **Ngữ cảnh**: Người dùng chọn ghế trên giao diện và bấm giữ chỗ.
- **Các bước thực hiện**:
  1. Gửi request `POST /api/bookings/seat-reservations` với thông tin `showtimeId` và danh sách `seatIds`.
  2. Xác thực JWT hợp lệ.
- **Kết quả mong đợi**:
  - API trả về 201 Created cùng ID của nhóm giữ chỗ.
  - Database chèn các bản ghi vào bảng `seat_reservations` với trạng thái `HELD` và `expires_at` là 5 phút tính từ hiện tại.
  - Redis set các key khóa ghế `booking:seat-lock:{showtimeId}:{seatId}` với TTL là 5 phút.
- **Kết quả thực tế**: PASS.

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
  1. Gửi request `POST /api/bookings` kèm theo danh sách các ID ghế đã giữ trước đó, sử dụng `Idempotency-Key` trên header.
- **Kết quả mong đợi**:
  - API trả về 201 Created với `bookingCode` và tổng tiền.
  - Bảng `bookings` thêm bản ghi mới với trạng thái `PENDING_PAYMENT` (thời hạn 15 phút).
  - Các bản ghi trong `seat_reservations` chuyển trạng thái từ `HELD` sang `CONVERTED` và được gán `bookingId` tương ứng.
- **Kết quả thực tế**: PASS.

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
  1. Gửi 2 request `POST /api/bookings` y hệt nhau với cùng một `Idempotency-Key`.
- **Kết quả mong đợi**:
  - Lần 1: Trả về 201 Created.
  - Lần 2: Trả về kết quả lấy từ Cache, không tạo thêm Booking thứ hai trong DB.
- **Kết quả thực tế**: PASS.

---

## 3. Tổng hợp kết quả tự động (Automated Test Metrics)
- **Môi trường chạy**: Local Integration Tests với H2 / MySQL & Redis giả lập.
- **Lệnh thực thi**: `mvn clean verify -Dnet.bytebuddy.experimental=true`
- **Tổng số Test Cases**: 32 (bao gồm cả Unit và Integration Tests)
  - Core Booking & Idempotency: 25 tests
  - Background Expiration Workers: 7 tests
- **Tỉ lệ Pass**: 100% (0 Failures, 0 Errors, 0 Skipped).
- **Đánh giá**: Tất cả các luồng nghiệp vụ (E2E flows) đều hoạt động chuẩn xác theo thiết kế. Sẵn sàng tích hợp.
