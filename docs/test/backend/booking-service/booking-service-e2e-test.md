# BÁO CÁO KIỂM THỬ E2E (END-TO-END TEST FLOW)

## 1. Thông tin chung
- **Service**: `booking-service`
- **Tính năng**: Core Booking, Seat Reservation & Background Expiration Engine
- **Mục tiêu**: Kiểm thử luồng nghiệp vụ từ lúc người dùng bắt đầu giữ chỗ, tạo đơn hàng cho đến khi hệ thống tự động dọn dẹp các đơn/ghế hết hạn.

---

## Hướng dẫn chung: Tạo Idempotency-Key

Các API tạo hoặc thay đổi dữ liệu (bao gồm cả API public và nội bộ) đều yêu cầu header `Idempotency-Key` để hỗ trợ cơ chế **Idempotency**.

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
> * Khi thực hiện các luồng test Idempotency (như Flow 6), nhớ sử dụng **cùng một `Idempotency-Key`** đã dùng ở request tạo mới trước đó.
> * Đối với mỗi yêu cầu tạo mới khác nhau, hãy tạo **một `Idempotency-Key` mới**.
> * Giá trị `Idempotency-Key` phải là **UUID Version 4 (UUID v4)** hợp lệ.

---

## 2. Các Luồng Kiểm Thử (Test Flows)

### Flow 1: Giữ chỗ ngồi (Hold Seat)
- **API**: `POST /api/bookings/seat-reservations`
- **Ngữ cảnh**: Người dùng chọn ghế trên giao diện và bấm giữ chỗ. Thời gian giữ là 5 phút.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
  - `Idempotency-Key`: Một UUID v4 mới.
- **Request Body (Happy Case)**:
  ```json
  {
    "showtimeId": 120,
    "seatIds": [10, 11]
  }
  ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `201 Created` cùng ID của nhóm giữ chỗ (`reservationIds`). (Lưu lại `reservationIds` để dùng cho Flow 3).
    - Database chèn vào `seat_reservations` với trạng thái `HELD`.
    - Redis khóa ghế trong 5 phút.
  - **Negative Case 1 (Lỗi trùng ghế)**: Truyền lại `seatIds` bằng `[10, 11]` (ghế vừa bị giữ) -> API trả về `409 Conflict` (Mã lỗi `BOOKING_SEAT_ALREADY_HELD`). Payload: `{"showtimeId": 120, "seatIds": [10, 11]}`
  - **Negative Case 2 (Lỗi sai lịch chiếu)**: Đổi `showtimeId` thành `99999` (không tồn tại) -> API trả về `404 Not Found` (Mã lỗi `BOOKING_SHOWTIME_NOT_FOUND`). Payload: `{"showtimeId": 99999, "seatIds": [10, 11]}`
  - **Negative Case 3 (Lỗi Validation)**: Đổi `seatIds` thành `[]` rỗng -> API trả về `400 Bad Request` (Mã lỗi `VALIDATION_ERROR`). Payload: `{"showtimeId": 120, "seatIds": []}`
  - **Negative Case 4 (Ghế không tồn tại)**: Truyền ID ghế ảo -> API trả về `404 Not Found` (Mã lỗi `BOOKING_SEAT_NOT_FOUND`). Payload: `{"showtimeId": 120, "seatIds": [9999]}`
  - **Negative Case 5 (Ghế không hoạt động)**: Truyền ghế đang bảo trì -> API trả về `400 Bad Request` (Mã lỗi `BOOKING_SEAT_NOT_ACTIVE`). Payload: `{"showtimeId": 120, "seatIds": [99]}` (giả sử 99 bảo trì)
  - **Negative Case 6 (Ghế sai phòng)**: Truyền ghế thuộc phòng khác của lịch chiếu -> API trả về `400 Bad Request` (Mã lỗi `BOOKING_SEAT_ROOM_MISMATCH`). Payload: `{"showtimeId": 120, "seatIds": [300]}`
  - **Negative Case 7 (Lỗi kết nối Movie Service)**: Service bị sập -> API trả về `503 Service Unavailable` (Mã lỗi `MOVIE_SERVICE_UNAVAILABLE`). Payload: `{"showtimeId": 120, "seatIds": [10, 11]}`
  - **Negative Case 8 (Ghế đã bán)**: Ghế đã có người thanh toán xong -> API trả về `409 Conflict` (Mã lỗi `BOOKING_SEAT_ALREADY_BOOKED`). Payload: `{"showtimeId": 120, "seatIds": [20, 21]}`
  - **Negative Case 9 (Lịch chiếu chưa mở bán)**: Suất chiếu bị khóa/chưa mở bán giá vé -> API trả về `409 Conflict` (Mã lỗi `BOOKING_SHOWTIME_NOT_AVAILABLE`). Payload: `{"showtimeId": 125, "seatIds": [10]}`
  - **Negative Case 10 (Chưa đăng nhập)**: Không truyền header Authorization -> API trả về `401 Unauthorized` (Mã lỗi `UNAUTHORIZED`).

### Flow 2: Xung đột giữ chỗ (Concurrency Lock)
- **API**: `POST /api/bookings/seat-reservations`
- **Ngữ cảnh**: Hai người dùng cùng lúc bấm chọn giữ cùng một ghế.
- **Cách thực hiện**:
  1. Lấy 2 JWT Token của 2 User (User A và User B).
  2. Bắn 2 request bằng Postman/JMeter lên cùng API với cùng payload:
     ```json
     {
       "showtimeId": 120,
       "seatIds": [15]
     }
     ```
- **Kết quả mong đợi**:
  - Request nào đến trước (hoặc lấy được Redis lock trước) -> API trả về `201 Created`.
  - Request đến sau bị từ chối với lỗi `409 Conflict` (Mã lỗi `BOOKING_SEAT_ALREADY_HELD`).
  - Không có dữ liệu rác nào của User thua cuộc lọt vào Database.

### Flow 3: Tạo Booking (Đặt vé)
- **API**: `POST /api/bookings`
- **Ngữ cảnh**: Người dùng xác nhận thanh toán rổ hàng đang giữ chỗ để tạo đơn hàng. Thời hạn thanh toán là 15 phút.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
  - `Idempotency-Key`: Một UUID v4 mới.
- **Request Body (Happy Case)**:
  ```json
  {
    "reservationIds": [501, 502]
  }
  ```
  *(Thay `[501, 502]` bằng mảng trả về thành công ở Flow 1)*
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `201 Created` với `bookingId` và `totalAmount`. (Lưu lại để test Flow 7, 8).
    - Database tạo `bookings` với trạng thái `PENDING_PAYMENT`.
    - Các ghế trong `seat_reservations` đổi từ `HELD` sang `CONVERTED`.
  - **Negative Case 1 (Lỗi quá hạn giữ chỗ)**: Truyền `reservationIds` của các ghế đã giữ quá 5 phút (trạng thái `EXPIRED`) -> API trả về `409 Conflict` (Mã lỗi `SEAT_RESERVATION_EXPIRED`). Payload: `{"reservationIds": [991]}`
  - **Negative Case 2 (Lỗi không phải chủ sở hữu)**: Truyền `reservationIds` mà User khác tạo -> API trả về `403 Forbidden` (Mã lỗi `SEAT_RESERVATION_OWNERSHIP_MISMATCH`). Payload: `{"reservationIds": [501]}`
  - **Negative Case 3 (Trạng thái ghế không hợp lệ)**: Truyền ghế đã bị hủy/release thay vì `HELD` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_INVALID_STATUS`). Payload: `{"reservationIds": [992]}`
  - **Negative Case 4 (Nhiều suất chiếu)**: Đặt các ghế thuộc nhiều suất chiếu khác nhau -> API trả về `400 Bad Request` (Mã lỗi `BOOKING_MULTIPLE_SHOWTIMES_NOT_ALLOWED`). Payload: `{"reservationIds": [501, 801]}`
  - **Negative Case 5 (Ghế đã đặt rồi)**: Truyền ghế đã `CONVERTED` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_ALREADY_CREATED` hoặc `SEAT_RESERVATION_ALREADY_CONVERTED`). Payload: `{"reservationIds": [501]}`
  - **Negative Case 6 (Lỗi kết nối Movie Service)**: Service sập khi lấy giá vé -> API trả về `503 Service Unavailable` (Mã lỗi `MOVIE_SERVICE_UNAVAILABLE`). Payload: `{"reservationIds": [501, 502]}`
  - **Negative Case 7 (Ghế giữ chỗ không tồn tại)**: Truyền ID giữ chỗ ảo -> API trả về `404 Not Found` (Mã lỗi `SEAT_RESERVATION_NOT_FOUND`). Payload: `{"reservationIds": [999999]}`
  - **Negative Case 8 (Xung đột Idempotency)**: Truyền lại `Idempotency-Key` cũ nhưng thay đổi Payload khác Lần 1 -> API trả về `409 Conflict` (Mã lỗi `BOOKING_IDEMPOTENCY_CONFLICT`). Payload: `{"reservationIds": [901]}`

### Flow 4: Hủy tự động ghế bị giữ quá hạn (Seat Expiration Worker)
- **Ngữ cảnh**: Người dùng giữ ghế nhưng thoát trang không tạo booking (quá 5 phút).
- **Cách thực hiện**:
  1. Chạy **Flow 1** để có bản ghi `HELD`.
  2. Vào Database sửa cột `expires_at` lùi lại 10 phút trước.
  3. Đợi `ReservationExpirationWorker` chạy ngầm.
- **Kết quả mong đợi**:
  - Worker quét thấy bản ghi và cập nhật trạng thái ghế thành `EXPIRED`.
  - Redis tự động ép xóa key khóa ghế để người khác có thể đặt.

### Flow 5: Hủy tự động Booking chưa thanh toán (Booking Expiration Worker)
- **Ngữ cảnh**: Người dùng tạo Booking nhưng không chịu thanh toán quá 15 phút.
- **Cách thực hiện**:
  1. Chạy **Flow 3** để có bản ghi booking `PENDING_PAYMENT`.
  2. Vào Database sửa cột `expires_at` của booking đó thành 20 phút trước.
  3. Đợi `BookingExpirationWorker` chạy ngầm.
- **Kết quả mong đợi**:
  - Worker quét thấy booking và cập nhật trạng thái thành `EXPIRED`.
  - Đệ quy tìm tất cả `seat_reservations` đang `CONVERTED` thuộc về booking đó, chuyển thành `EXPIRED`.
  - Xóa nốt các key Redis (nếu còn).

### Flow 6: Chống trùng lặp (Idempotency)
- **API**: Bất kỳ POST API nào (Ví dụ: `POST /api/bookings`)
- **Ngữ cảnh**: Network chập chờn, client retry thao tác 2 lần.
- **Cách thực hiện**:
  1. Gửi request tạo booking y hệt Flow 3. Nhớ chính xác chuỗi `Idempotency-Key` (Vd: `f47ac10b-58cc...`).
  2. Gửi lại y chang Request đó thêm 1 lần nữa.
- **Kết quả mong đợi**:
  - Lần 1: Trả về `201 Created` (Lưu DB).
  - Lần 2: Trả về kết quả lấy từ Cache (giống hệt response Lần 1), API không văng lỗi và **tuyệt đối không tạo thêm dữ liệu thừa** trong DB.

### Flow 7: Xác nhận thanh toán (Confirm Payment - Internal)
- **API**: `POST /internal/bookings/{bookingId}/payment-results`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `X-Internal-Token: secret-internal-token`
- **Payload**:
  ```json
  {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "schemaVersion": "1.0",
    "paymentId": 3001,
    "paymentTransactionCode": "PAY-20260621-0001",
    "paymentMethod": "VNPAY",
    "result": "SUCCESS",
    "amount": 240000,
    "currency": "VND",
    "occurredAt": "2026-06-21T20:10:00Z",
    "reconciliationStatus": "NONE"
  }
  ```
  *(Thay `{bookingId}` trên URL. Đảm bảo `paidAmount` bằng đúng `totalAmount` đã tạo ở Flow 3).*
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK` kèm danh sách vé (`tickets`) vừa được tạo.
    - Booking đổi trạng thái sang `CONFIRMED`.
    - Redis nhả khóa ghế hoàn toàn.
  - **Negative Case 1 (Lỗi sai Token)**: Đổi `X-Internal-Token` thành `wrong-secret` -> API trả về `401 Unauthorized` (Mã lỗi `INTERNAL_TOKEN_INVALID`).
  - **Negative Case 2 (Lỗi thiếu tiền)**: Sửa `paidAmount` thành `10000` -> API trả về `409 Conflict` (Mã lỗi `PAYMENT_AMOUNT_MISMATCH`).
  - **Negative Case 3 (Lỗi sai loại tiền tệ)**: Sửa `currency` thành `USD` -> API trả về `409 Conflict` (Mã lỗi `PAYMENT_CURRENCY_MISMATCH`).
  - **Negative Case 4 (Lỗi quá hạn thanh toán)**: Gọi trên booking đã chuyển sang `EXPIRED` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_CANNOT_BE_CANCELLED` hoặc `BOOKING_INVALID_STATUS_TRANSITION`).
  - **Negative Case 5 (Không cho phép thanh toán)**: Booking trạng thái CANCELLED -> API trả về `409 Conflict` (Mã lỗi `BOOKING_NOT_PAYABLE`).

### Flow 8: Ghi nhận thanh toán thất bại (Fail Payment - Internal)
- **API**: `POST /internal/bookings/{bookingId}/payment-results`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `X-Internal-Token: secret-internal-token`
- **Payload**:
  ```json
  {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "schemaVersion": "1.0",
    "paymentId": 3002,
    "paymentTransactionCode": "PAY-20260621-0002",
    "paymentMethod": "VNPAY",
    "result": "FAILED",
    "amount": 240000,
    "currency": "VND",
    "occurredAt": "2026-06-21T20:15:00Z",
    "reconciliationStatus": "NONE"
  }
  ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK` ghi nhận sự cố.
    - Trạng thái Booking **VẪN LÀ `PENDING_PAYMENT`** để User có thể thử thanh toán lại trong giới hạn 15 phút.
  - **Negative Case 1 (Lỗi Booking không tồn tại)**: Thay `{bookingId}` bằng ID không có thực -> API trả về `404 Not Found` (Mã lỗi `BOOKING_NOT_FOUND`).
  - **Negative Case 2 (Lỗi sai Token)**: Đổi `X-Internal-Token` thành chuỗi rác -> API trả về `401 Unauthorized`.

### Flow 9: Hủy đặt vé (Cancel Booking - Mở rộng cho User)
- **API**: `POST /api/bookings/{bookingId}/cancel`
- **Ngữ cảnh**: Người dùng chủ động ấn nút "Hủy đơn hàng" trước khi hết hạn 15 phút.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
  - `Idempotency-Key`: UUID v4 mới.
- **Request Body**:
  ```json
  {}
  ```
  *(Payload rỗng)*
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK`.
    - Booking đổi trạng thái thành `CANCELLED`.
    - Gọi lại API Confirm Payment (Flow 7) cho booking này sẽ bị từ chối.
  - **Negative Case 1 (Lỗi Booking không tồn tại)**: Thay `{bookingId}` bằng ID ảo -> API trả về `404 Not Found` (Mã lỗi `BOOKING_NOT_FOUND`).
  - **Negative Case 2 (Lỗi sai người dùng)**: Dùng Token của User B để cố hủy Booking của User A -> API trả về `403 Forbidden` (Mã lỗi `FORBIDDEN`).
  - **Negative Case 3 (Lỗi đã thanh toán xong)**: Gọi trên booking trạng thái `CONFIRMED` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_CANNOT_BE_CANCELLED`).
  - **Negative Case 4 (Lỗi đã hết hạn)**: Gọi trên booking trạng thái `EXPIRED` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_CANNOT_BE_CANCELLED`).
  - **Negative Case 5 (Lỗi đã hủy từ trước)**: Gọi trên booking trạng thái `CANCELLED` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_CANNOT_BE_CANCELLED`).
  - **Negative Case 6 (Lỗi sai vòng đời)**: Nếu booking không ở trạng thái `PENDING_PAYMENT` -> API trả về `409 Conflict` (Mã lỗi `BOOKING_INVALID_STATUS_TRANSITION`).

### Flow 10: Truy vấn Vé của Booking (Get Tickets)
- **API**: `GET /api/bookings/{bookingId}/tickets`
- **Ngữ cảnh**: Người dùng xem danh sách vé sau khi thanh toán thành công.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
- **Kết quả mong đợi**:
  - **Happy Case**: API trả về `200 OK` cùng mảng vé (nếu đã `CONFIRMED`) hoặc rỗng (nếu chưa).
  - **Negative Case 1 (Lỗi Booking không tồn tại)**: Trả về `404 Not Found` (Mã lỗi `BOOKING_NOT_FOUND`).
  - **Negative Case 2 (Lỗi sai người dùng)**: Dùng Token của người khác -> `403 Forbidden` (Mã lỗi `FORBIDDEN`).

### Flow 11: Xem Chi tiết Vé (Get Ticket Detail)
- **API**: `GET /api/tickets/{ticketId}`
- **Ngữ cảnh**: Người dùng xem chi tiết 1 vé cụ thể.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
- **Kết quả mong đợi**:
  - **Happy Case**: API trả về `200 OK` cùng đối tượng vé.
  - **Negative Case 1 (Lỗi Vé không tồn tại)**: Trả về `404 Not Found` (Mã lỗi `TICKET_NOT_FOUND`).
  - **Negative Case 2 (Lỗi sai người dùng)**: Người khác xem -> `403 Forbidden` (Mã lỗi `FORBIDDEN`).

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
