# Booking Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                        |
| -------------- | ----------------------------------------------- |
| Service        | `booking-service`                               |
| Feature        | Booking, Seat Reservation and Ticket Management |
| API liên quan  | Bookings, Seat Reservations, Tickets            |
| Contract Owner | Dương Thiện Nhân                                |
| Backend Owner  | Trần Hiển Vinh                                  |
| Reviewer       | Trần Hiển Vinh                                  |
| Trạng thái     | Updated after Owner Review / Ready for Re-review |
| Milestone      | Sprint 2 - Core Service API Foundation          |
| Ngày cập nhật  | 22/06/2026                                      |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API thuộc `booking-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Thống nhất contract giữa Frontend, Booking Service, API Gateway và các service liên quan.
* Làm cơ sở triển khai luồng giữ ghế, tạo booking và phát hành ticket.
* Xác định rõ booking lifecycle và seat reservation lifecycle.
* Phân định rõ trách nhiệm giữa Movie, Booking, Payment, Promotion và Notification Service.
* Chuẩn hóa endpoint, request, response, validation, authorization, HTTP status và error code.
* Làm cơ sở tách các implementation issue sau khi contract được duyệt.

---

## 3. Phạm Vi Booking Service

Booking Service sở hữu các bảng:

```txt
bookings
tickets
seat_reservations
```

Booking Service chịu trách nhiệm:

* Giữ ghế tạm thời theo user và showtime.
* Kiểm tra xung đột giữ ghế.
* Tạo booking từ các seat reservation hợp lệ.
* Quản lý booking lifecycle.
* Tạo các ticket thuộc booking.
* Lưu snapshot giá vé tại thời điểm tạo booking.
* Lấy lịch sử booking của người dùng.
* Hủy hoặc làm hết hạn booking theo business rule.

Booking Service không chịu trách nhiệm:

* Quản lý thông tin phim.
* Quản lý phòng, ghế vật lý hoặc suất chiếu.
* Quản lý tài khoản và hồ sơ người dùng.
* Xử lý giao dịch thanh toán.
* Quản lý mã khuyến mãi.
* Gửi email hoặc notification.
* Quản lý điểm thưởng.

---

## 4. Physical Schema Hiện Tại

### 4.1. Bảng `seat_reservations`

| Field       | Type        | Required | Mô tả                                   |
| ----------- | ----------- | -------: | --------------------------------------- |
| id          | bigint      |      Yes | Primary key                             |
| showtime_id | bigint      |      Yes | Logical reference tới Movie Service     |
| seat_id     | bigint      |      Yes | Logical reference tới Movie Service     |
| user_id     | bigint      |      Yes | Logical reference tới User/Auth Service |
| status      | varchar(20) |       No | Trạng thái reservation                  |
| expires_at  | timestamp   |      Yes | Thời điểm reservation hết hạn           |
| created_at  | timestamp   |       No | Thời điểm tạo                           |

### 4.2. Bảng `bookings`

| Field        | Type          | Required | Mô tả                               |
| ------------ | ------------- | -------: | ----------------------------------- |
| id           | bigint        |      Yes | Primary key                         |
| booking_code | varchar(50)   |      Yes | Mã booking unique                   |
| user_id      | bigint        |      Yes | Logical reference tới User Service  |
| showtime_id  | bigint        |      Yes | Logical reference tới Movie Service |
| total_amount | decimal(10,2) |      Yes | Tổng tiền snapshot                  |
| status       | varchar(30)   |       No | Trạng thái booking                  |
| created_at   | timestamp     |       No | Thời điểm tạo                       |
| updated_at   | timestamp     |       No | Thời điểm cập nhật                  |

### 4.3. Bảng `tickets`

| Field      | Type          | Required | Mô tả                                |
| ---------- | ------------- | -------: | ------------------------------------ |
| id         | bigint        |      Yes | Primary key                          |
| booking_id | bigint        |      Yes | Foreign key nội bộ tới `bookings.id` |
| seat_id    | bigint        |      Yes | Logical reference tới Movie Service  |
| price      | decimal(10,2) |      Yes | Giá vé snapshot                      |
| created_at | timestamp     |       No | Thời điểm tạo                        |

---

## 5. Schema Alignment Bắt Buộc Trước Implementation

Sau khi Booking Service Owner review contract, một số thay đổi schema được xác định là bắt buộc trước khi triển khai Backend.

### 5.1. Các field chưa có trong schema hiện tại

Schema hiện tại chưa có:

```txt
bookings.expires_at
bookings.version
seat_reservations.version

bookings.promotion_id
bookings.payment_id
bookings.discount_amount
bookings.final_amount

tickets.ticket_code
tickets.qr_code
tickets.status
tickets.checked_in_at
```

### 5.2. Các thay đổi bắt buộc trong Sprint 2

Trước khi các Booking implementation issue được chuyển sang `Ready`, schema phải được cập nhật:

```txt
Add bookings.expires_at
Add bookings.version
Add seat_reservations.version
Drop unique index (showtime_id, seat_id)
Update BookingStatus comment/default
Update SeatReservationStatus comment/default
```

Booking status chính thức:

```txt
PENDING_PAYMENT
CONFIRMED
CANCELLED
EXPIRED
```

Seat reservation status chính thức:

```txt
HELD
RELEASED
EXPIRED
CONVERTED
```

Default của `seat_reservations.status` phải đổi từ:

```txt
RESERVED
```

thành:

```txt
HELD
```

### 5.3. Unique Index của Seat Reservation

Unique index hiện tại:

```txt
(showtime_id, seat_id)
```

phải được xóa.

Lý do:

- Reservation `RELEASED` và `EXPIRED` vẫn được giữ lại để audit.
- Nếu giữ unique index, một ghế đã từng được reservation sẽ không thể được giữ lại trong tương lai.
- Redis seat lock là lớp chính chống double booking real-time.
- Database transaction và locking tiếp tục bảo vệ tính nhất quán khi ghi dữ liệu.

Có thể thay bằng non-unique index:

```txt
(showtime_id, seat_id, status)
```

để tối ưu query trạng thái ghế.

### 5.4. Logical References

Booking Service không lưu trực tiếp:

```txt
paymentId
promotionId
```

Payment Service chịu trách nhiệm lưu `bookingId`.

Promotion Service chịu trách nhiệm lưu usage liên kết theo `bookingId`.

### 5.5. Ticket Scope

Ticket chỉ được tạo sau khi payment thành công và booking chuyển sang:

```txt
CONFIRMED
```

Không tạo ticket khi booking còn:

```txt
PENDING_PAYMENT
```

Các chức năng sau chưa nằm trong Sprint 2:

```txt
Ticket code
QR code
Ticket status lifecycle
Ticket check-in
```

### 5.6. Related Schema Issue

Các thay đổi bắt buộc được tracking trong issue:

```txt
[Database] Align Booking Schema with Booking API Contract
```

Booking implementation chưa được bắt đầu trước khi Schema Alignment MR được merge.

---

## 6. Database-per-Service và Logical Reference

Các field sau chỉ là logical references:

```txt
userId
showtimeId
seatId
```

Booking Service không tạo foreign key vật lý sang database khác.

### Source of truth

| Dữ liệu                              | Source of truth      |
| ------------------------------------ | -------------------- |
| User/account                         | User/Auth Service    |
| Movie, room, physical seat, showtime | Movie Service        |
| Seat reservation, booking, ticket    | Booking Service      |
| Payment transaction                  | Payment Service      |
| Promotion rule và usage              | Promotion Service    |
| Notification                         | Notification Service |

Booking Service không truy cập trực tiếp database của service khác.

---

## 7. API Gateway và Service URL

### 7.1. API Gateway

Frontend chỉ gọi:

```txt
http://localhost:8080
```

### 7.2. Booking Service Direct URL

Chỉ dùng để debug hoặc test nội bộ:

```txt
http://localhost:8083
```

Port chính thức phải lấy từ cấu hình project.

### 7.3. Request Flow

```txt
React Frontend
→ API Gateway
→ Booking Service
→ Booking Database
→ Redis seat lock nếu được enable
```

Frontend không gọi trực tiếp Booking Service port.

---

## 8. Quy Ước Chung

### 8.1. Headers

Protected API:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Các POST API tạo hoặc thay đổi dữ liệu quan trọng phải gửi:

```http
Idempotency-Key: <UUID>
```

Các endpoint bắt buộc sử dụng `Idempotency-Key`:

```txt
POST /api/bookings/seat-reservations
POST /api/bookings
POST /api/bookings/{bookingId}/cancel
```

Internal API:

```http
Content-Type: application/json
X-Internal-Token: <internal-token>
Idempotency-Key: <UUID>
```

`X-Internal-Token` được sử dụng trong Sprint 2 để xác thực giao tiếp nội bộ.

Trong các sprint sau, các endpoint cập nhật trạng thái thanh toán cần được nâng cấp sang cơ chế ký request bằng HMAC signature.

Payment provider như VNPay hoặc MoMo không được gọi trực tiếp Booking Service.

Flow đúng:

```txt
VNPay / MoMo
→ Payment Service xác minh chữ ký provider
→ Payment Service gọi Booking Internal API
```

### 8.2. Date and Time

Datetime sử dụng ISO-8601:

```txt
YYYY-MM-DDTHH:mm:ss
```

Timezone nghiệp vụ:

```txt
Asia/Ho_Chi_Minh
```

### 8.3. Currency

Đơn vị:

```txt
VND
```

Response trả amount dạng number:

```json
{
  "totalAmount": 240000
}
```

### 8.4. Pagination

- `page` bắt đầu từ `0`.
- `size` mặc định `10`.
- `size` tối đa `50`.

### 8.5. Idempotency Rules

`Idempotency-Key` phải là một UUID do caller tạo cho mỗi logical request.

Ví dụ:

```http
Idempotency-Key: 97605cf2-56c7-4d08-a5bc-c8910fb61239
```

Nếu cùng user hoặc internal service gửi lại cùng request với cùng `Idempotency-Key`:

```txt
Không tạo reservation lần hai
Không tạo booking lần hai
Không tạo ticket lần hai
Không thực hiện status transition lần hai
```

Service phải trả lại kết quả tương đương request đầu tiên.

Nếu cùng `Idempotency-Key` nhưng request payload khác:

```txt
Trả 409 BOOKING_IDEMPOTENCY_CONFLICT
```

Idempotency record có thể được quản lý bằng Redis hoặc database tùy implementation, nhưng phải có thời gian lưu đủ để bao phủ retry window.

---

## 9. Common Response Contract

### 9.1. Success

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

### 9.2. Error

```json
{
  "success": false,
  "message": "Operation failed",
  "errorCode": "ERROR_CODE",
  "data": null,
  "errors": null
}
```

### 9.3. Validation Error

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "data": null,
  "errors": [
    {
      "field": "seatIds",
      "message": "Seat list must not be empty"
    }
  ]
}
```

---

## 10. Enum Definitions

### 10.1. SeatReservationStatus

```txt
HELD
RELEASED
EXPIRED
CONVERTED
```

Ý nghĩa:

| Status    | Mô tả                                    |
| --------- | ---------------------------------------- |
| HELD      | Ghế đang được user giữ                   |
| RELEASED  | User hoặc hệ thống chủ động thả ghế      |
| EXPIRED   | Hết thời gian giữ                        |
| CONVERTED | Reservation đã được chuyển thành booking |

### 10.2. BookingStatus

```txt
PENDING_PAYMENT
CONFIRMED
CANCELLED
EXPIRED
```

Ý nghĩa:

| Status          | Mô tả                                                   |
| --------------- | ------------------------------------------------------- |
| PENDING_PAYMENT | Booking đã tạo, đang chờ thanh toán                     |
| CONFIRMED       | Payment đã thành công hoặc booking được xác nhận hợp lệ |
| CANCELLED       | Booking bị user/staff/system hủy                        |
| EXPIRED         | Booking hết thời gian thanh toán                        |

Schema hiện tại chưa có `DRAFT` riêng. Seat reservation đóng vai trò trạng thái tạm trước khi tạo booking.

### 10.3. Ticket Status

Schema hiện tại chưa có cột ticket status.

Sprint 2 không định nghĩa lifecycle ticket trong database.

Ticket được xem là bản ghi vé thuộc một booking. QR/check-in/status sẽ được bổ sung qua schema change issue sau.

---

## 11. Lifecycle Chính

### 11.1. Seat Reservation Lifecycle

```txt
HELD
→ CONVERTED khi tạo booking thành công

HELD
→ RELEASED khi user chủ động thả ghế

HELD
→ EXPIRED khi quá expiresAt
```

Không được chuyển:

```txt
EXPIRED → HELD
RELEASED → HELD
CONVERTED → HELD
```

Muốn giữ lại ghế phải tạo reservation mới.

### 11.2. Booking Lifecycle

```txt
PENDING_PAYMENT
→ CONFIRMED khi Payment Service xác nhận thanh toán thành công

PENDING_PAYMENT
→ CANCELLED khi customer hủy hợp lệ

PENDING_PAYMENT
→ EXPIRED khi quá bookings.expiresAt

CONFIRMED
→ CANCELLED chỉ bởi Admin/Employee thông qua nghiệp vụ đặc biệt
```

Customer không được tự hủy booking `CONFIRMED` trong Sprint 2 vì chưa có refund flow.

Ticket chỉ được tạo tại transition:

```txt
PENDING_PAYMENT
→ CONFIRMED
```

Nếu booking chuyển từ `PENDING_PAYMENT` sang `CANCELLED` hoặc `EXPIRED`:

- Không tạo ticket.
- Redis seat lock được release nếu còn tồn tại.
- Các reservation `CONVERTED` không còn làm ghế unavailable vì booking liên kết đã kết thúc.

---

## 12. Reservation và Booking Timeout

### 12.1. Seat Reservation TTL

Thời gian giữ ghế:

```txt
5 phút
```

Cấu hình:

```properties
booking.seat-reservation-ttl-minutes=5
```

`seat_reservations.expires_at` được tính tại thời điểm tạo reservation:

```txt
expiresAt = createdAt + 5 phút
```

Redis seat lock sử dụng TTL tương ứng.

### 12.2. Booking Payment Timeout

Thời gian chờ thanh toán:

```txt
15 phút
```

Cấu hình:

```properties
booking.payment-timeout-minutes=15
```

Khi tạo booking:

```txt
bookings.expires_at = createdAt + 15 phút
```

`bookings.expires_at` là field bắt buộc phải được bổ sung trong Schema Alignment Issue.

Không tính expiry on-the-fly từ `createdAt` khi query booking.

### 12.3. System Worker

Booking Service phải có background worker hoặc scheduled job để chủ động xử lý record hết hạn.

Reservation expiration:

```txt
status = HELD
AND expiresAt < now
→ status = EXPIRED
→ release Redis lock
```

Booking expiration:

```txt
status = PENDING_PAYMENT
AND expiresAt < now
→ status = EXPIRED
→ release Redis lock liên quan nếu còn tồn tại
→ ghế thuộc reservation CONVERTED được xem là available trở lại
```

Worker phải idempotent.

Chạy lại worker không được:

- Expire cùng booking nhiều lần.
- Release cùng lock gây lỗi nghiệp vụ.
- Tạo duplicate event.
- Thay đổi booking đã `CONFIRMED`, `CANCELLED` hoặc `EXPIRED`.

### 12.4. Redis TTL và Database Worker

Redis TTL chịu trách nhiệm:

```txt
Giải phóng lock real-time
```

Database worker chịu trách nhiệm:

```txt
Cập nhật trạng thái nghiệp vụ lâu dài
```

Không chỉ dựa vào Redis TTL để xác định trạng thái booking hoặc reservation trong database.

---

## 13. API Classification

### 13.1. Protected Customer APIs

```txt
POST   /api/bookings/seat-reservations
GET    /api/bookings/seat-reservations/{reservationId}
DELETE /api/bookings/seat-reservations/{reservationId}

POST   /api/bookings
GET    /api/bookings/{bookingId}
GET    /api/bookings/me
POST   /api/bookings/{bookingId}/cancel

GET    /api/bookings/{bookingId}/tickets
GET    /api/tickets/{ticketId}
```

### 13.2. Admin/Employee APIs

```txt
GET   /api/admin/bookings
GET   /api/admin/bookings/{bookingId}
PATCH /api/admin/bookings/{bookingId}/status
```

### 13.3. Internal APIs

```txt
GET /internal/bookings/{bookingId}/payment-context
POST /internal/bookings/{bookingId}/payment-results
```

Internal API không được expose công khai qua API Gateway.

---

## 14. Endpoint Summary

| Method | Endpoint                                  | Access         | Mục đích                          |
| ------ | ----------------------------------------- | -------------- | --------------------------------- |
| POST   | `/api/bookings/seat-reservations`         | Protected      | Giữ nhiều ghế                     |
| GET    | `/api/bookings/seat-reservations/{id}`    | Protected      | Xem reservation                   |
| DELETE | `/api/bookings/seat-reservations/{id}`    | Protected      | Thả reservation                   |
| POST   | `/api/bookings`                           | Protected      | Tạo booking từ reservation        |
| GET    | `/api/bookings/{id}`                      | Protected      | Xem booking detail                |
| GET    | `/api/bookings/me`                        | Protected      | Lịch sử booking của user hiện tại |
| POST   | `/api/bookings/{id}/cancel`               | Protected      | Hủy booking                       |
| GET    | `/api/bookings/{id}/tickets`              | Protected      | Lấy ticket theo booking           |
| GET    | `/api/tickets/{id}`                       | Protected      | Lấy ticket detail                 |
| GET    | `/api/admin/bookings`                     | Admin/Employee | Danh sách booking                 |
| GET    | `/api/admin/bookings/{id}`                | Admin/Employee | Chi tiết booking                  |
| PATCH  | `/api/admin/bookings/{id}/status`         | Admin/Employee | Điều chỉnh status                 |
| GET    | `/internal/bookings/{bookingId}/payment-context` | Internal       | Lấy snapshot booking để thanh toán |
| POST   | `/internal/bookings/{bookingId}/payment-results` | Internal       | Xử lý kết quả thanh toán           |
---

# 15. Seat Reservation APIs

## 15.1. Create Seat Reservations

### Endpoint

```http
POST /api/bookings/seat-reservations
```

### Request Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
Idempotency-Key: <UUID>
```

`Idempotency-Key` là bắt buộc.

### Request Body

```json
{
  "showtimeId": 10,
  "seatIds": [101, 102]
}
```

Không nhận `userId` từ request body.

Backend lấy `userId/accountId` từ JWT.

### Field Definitions

| Field | Type | Required | Validation |
|---|---|---:|---|
| showtimeId | number | Yes | > 0 |
| seatIds | array<number> | Yes | Không rỗng, không trùng |
| seatIds[] | number | Yes | > 0 |

### Processing Flow

```txt
Validate request và Idempotency-Key
→ Resolve authenticated userId
→ Check idempotency result
→ Validate showtime exists and is bookable
→ Validate all seats belong to showtime room
→ Check physical seats are active
→ Acquire all Redis locks atomically
→ Create seat_reservations với status HELD
→ Store idempotency result
→ Return reservation group
```

### Atomic Rule

Giữ nhiều ghế là một operation atomic:

```txt
Nếu một ghế không giữ được
→ không giữ bất kỳ ghế nào trong request
```

### Idempotency Behavior

Nếu cùng request được gửi lại với cùng `Idempotency-Key`:

- Không tạo reservation mới.
- Không acquire lock mới.
- Trả lại reservation group đã được tạo ở request đầu tiên.

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Seats reserved successfully",
  "data": {
    "showtimeId": 10,
    "userId": 15,
    "status": "HELD",
    "expiresAt": "2026-06-21T20:05:00",
    "reservations": [
      {
        "reservationId": 501,
        "seatId": 101
      },
      {
        "reservationId": 502,
        "seatId": 102
      }
    ]
  }
}
```

### Error: Showtime Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Showtime not found",
  "errorCode": "BOOKING_SHOWTIME_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Showtime Not Bookable

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Showtime is not available for booking",
  "errorCode": "BOOKING_SHOWTIME_NOT_AVAILABLE",
  "data": null,
  "errors": null
}
```

### Error: Seat Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "One or more seats were not found",
  "errorCode": "BOOKING_SEAT_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Seat Does Not Belong to Room

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "One or more seats do not belong to the showtime room",
  "errorCode": "BOOKING_SEAT_ROOM_MISMATCH",
  "data": null,
  "errors": null
}
```

### Error: Seat Already Held

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "One or more seats are no longer available",
  "errorCode": "BOOKING_SEAT_ALREADY_HELD",
  "data": {
    "unavailableSeatIds": [102]
  },
  "errors": null
}
```

### Error: Seat Already Booked

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "One or more seats have already been booked",
  "errorCode": "BOOKING_SEAT_ALREADY_BOOKED",
  "data": {
    "unavailableSeatIds": [101]
  },
  "errors": null
}
```

---

## 15.2. Get Seat Reservation

### Endpoint

```http
GET /api/bookings/seat-reservations/{reservationId}
```

User chỉ được xem reservation của chính mình.

### Response Success

```json
{
  "success": true,
  "message": "Seat reservation retrieved successfully",
  "data": {
    "reservationId": 501,
    "showtimeId": 10,
    "seatId": 101,
    "userId": 15,
    "status": "HELD",
    "expiresAt": "2026-06-21T20:05:00",
    "createdAt": "2026-06-21T20:00:00"
  }
}
```

### Error: Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Seat reservation not found",
  "errorCode": "SEAT_RESERVATION_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Forbidden

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "You cannot access this reservation",
  "errorCode": "FORBIDDEN",
  "data": null,
  "errors": null
}
```

---

## 15.3. Release Seat Reservation

### Endpoint

```http
DELETE /api/bookings/seat-reservations/{reservationId}
```

### Business Rules

Chỉ cho release reservation:

```txt
status = HELD
reservation thuộc user hiện tại
reservation chưa converted
```

### Response Success

```json
{
  "success": true,
  "message": "Seat reservation released successfully",
  "data": {
    "reservationId": 501,
    "status": "RELEASED"
  }
}
```

### Error: Already Expired

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Seat reservation has already expired",
  "errorCode": "SEAT_RESERVATION_EXPIRED",
  "data": null,
  "errors": null
}
```

### Error: Already Converted

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Seat reservation has already been converted to a booking",
  "errorCode": "SEAT_RESERVATION_ALREADY_CONVERTED",
  "data": null,
  "errors": null
}
```

---

## 15.4. Extend Reservation

Sprint 2 không hỗ trợ API gia hạn reservation.

Lý do:

* Dễ bị lạm dụng để giữ ghế vô hạn.
* Làm tăng độ phức tạp concurrency.
* User phải tạo reservation mới nếu reservation cũ hết hạn.

---

# 16. Booking APIs

## 16.1. Create Booking

### Endpoint

```http
POST /api/bookings
```

### Request Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
Idempotency-Key: <UUID>
```

`Idempotency-Key` là bắt buộc.

### Request Body

```json
{
  "reservationIds": [501, 502]
}
```

Không nhận:

```txt
userId
totalAmount
ticketPrice
bookingCode
status
expiresAt
```

Các giá trị trên do Backend xác định.

### Field Definitions

| Field | Type | Required | Validation |
|---|---|---:|---|
| reservationIds | array<number> | Yes | Không rỗng, không trùng |
| reservationIds[] | number | Yes | > 0 |

### Processing Flow

```txt
Validate request và Idempotency-Key
→ Resolve authenticated userId
→ Check idempotency result
→ Load all reservations
→ Lock hoặc version-check reservations
→ Ensure all reservations belong to user
→ Ensure all reservations belong to same showtime
→ Ensure all status = HELD
→ Ensure all reservations are not expired
→ Resolve showtime and base ticket price from Movie Service
→ Calculate totalAmount
→ Generate unique bookingCode
→ Calculate expiresAt = now + 15 phút
→ Create booking với status PENDING_PAYMENT
→ Mark reservations as CONVERTED
→ Store idempotency result
→ Commit transaction
→ Return booking without tickets
```

### Important Ticket Rule

Không tạo record trong bảng `tickets` tại bước này.

Trong trạng thái:

```txt
PENDING_PAYMENT
```

ghế của booking được liên kết thông qua:

```txt
seat_reservations.status = CONVERTED
```

Ticket chỉ được tạo sau khi payment thành công tại:

```txt
POST /internal/bookings/{bookingId}/payment-results (khi nhận SUCCESS)
```

### Seat Availability While Waiting for Payment

Ghế được xem là unavailable khi:

```txt
seat_reservation.status = CONVERTED
AND booking.status IN (PENDING_PAYMENT, CONFIRMED)
```

Ghế được xem là available trở lại khi booking liên kết chuyển sang:

```txt
CANCELLED
EXPIRED
```

### Price Source of Truth

Frontend không được gửi giá.

Backend lấy giá vé từ Movie Service:

```txt
showtime.ticketPrice
```

Sau đó lưu tổng tiền snapshot:

```txt
bookings.totalAmount
```

Sprint 2 giả định các ghế trong cùng showtime có cùng `ticketPrice`.

Khi payment thành công, `tickets.price` được tạo từ snapshot:

```txt
ticketPrice = booking.totalAmount / numberOfSeats
```

Nếu sau này hỗ trợ nhiều loại giá ghế trong cùng booking, phải bổ sung booking item/seat price snapshot trước khi tạo ticket.

Công thức Sprint 2:

```txt
totalAmount = showtime.ticketPrice × numberOfSeats
```

Promotion và Score chưa được đưa vào công thức Booking Sprint 2.

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "bookingId": 1001,
    "bookingCode": "LORA-20260621-0001",
    "userId": 15,
    "showtimeId": 10,
    "totalAmount": 240000,
    "status": "PENDING_PAYMENT",
    "paymentExpiresAt": "2026-06-21T20:20:00",
    "reservedSeats": [
      {
        "reservationId": 501,
        "seatId": 101
      },
      {
        "reservationId": 502,
        "seatId": 102
      }
    ],
    "tickets": [],
    "createdAt": "2026-06-21T20:05:00"
  }
}
```

`paymentExpiresAt` được lấy trực tiếp từ:

```txt
bookings.expires_at
```

### Idempotency Behavior

Nếu cùng request được gửi lại với cùng `Idempotency-Key`:

- Không tạo booking mới.
- Không convert reservation lần hai.
- Không thay đổi expiry.
- Trả lại booking đã tạo ở request đầu tiên.

### Error: Idempotency Conflict

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Idempotency key was already used with a different request",
  "errorCode": "BOOKING_IDEMPOTENCY_CONFLICT",
  "data": null,
  "errors": null
}
```

### Error: Reservation Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "One or more seat reservations were not found",
  "errorCode": "SEAT_RESERVATION_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Reservation Expired

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "One or more seat reservations have expired",
  "errorCode": "SEAT_RESERVATION_EXPIRED",
  "data": null,
  "errors": null
}
```

### Error: Reservation Ownership

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "One or more reservations do not belong to the current user",
  "errorCode": "SEAT_RESERVATION_OWNERSHIP_MISMATCH",
  "data": null,
  "errors": null
}
```

### Error: Different Showtimes

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "All reservations must belong to the same showtime",
  "errorCode": "BOOKING_MULTIPLE_SHOWTIMES_NOT_ALLOWED",
  "data": null,
  "errors": null
}
```

### Error: Duplicate Booking

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Reservations have already been converted to a booking",
  "errorCode": "BOOKING_ALREADY_CREATED",
  "data": null,
  "errors": null
}
```

### Error: Movie Service Unavailable

Status: `503 Service Unavailable`

```json
{
  "success": false,
  "message": "Showtime information is temporarily unavailable",
  "errorCode": "MOVIE_SERVICE_UNAVAILABLE",
  "data": null,
  "errors": null
}
```

Không tạo booking nếu chưa xác định được snapshot giá hợp lệ.

---

## 16.2. Get Booking Detail

### Endpoint

```http
GET /api/bookings/{bookingId}
```

Customer chỉ được xem booking của mình.

Admin/Employee xem qua Admin API.

### Response Success — Pending Payment

```json
{
  "success": true,
  "message": "Booking retrieved successfully",
  "data": {
    "bookingId": 1001,
    "bookingCode": "LORA-20260621-0001",
    "userId": 15,
    "showtimeId": 10,
    "totalAmount": 240000,
    "status": "PENDING_PAYMENT",
    "paymentExpiresAt": "2026-06-21T20:20:00",
    "reservedSeats": [
      {
        "reservationId": 501,
        "seatId": 101
      },
      {
        "reservationId": 502,
        "seatId": 102
      }
    ],
    "tickets": [],
    "createdAt": "2026-06-21T20:05:00",
    "updatedAt": "2026-06-21T20:05:00"
  }
}
```

Nếu booking đang `PENDING_PAYMENT`, response chưa có ticket.

Nếu booking đã `CONFIRMED`, response có thể trả danh sách ticket được tạo sau payment success.

### Error: Booking Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Booking not found",
  "errorCode": "BOOKING_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Forbidden

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "You cannot access this booking",
  "errorCode": "FORBIDDEN",
  "data": null,
  "errors": null
}
```

---

## 16.3. Get Current User Booking History

### Endpoint

```http
GET /api/bookings/me
```

Không nhận `userId` từ Frontend. Backend lấy user từ JWT.

### Query Parameters

| Parameter | Type          | Required | Validation      |
| --------- | ------------- | -------: | --------------- |
| page      | integer       |       No | >= 0            |
| size      | integer       |       No | 1–50            |
| status    | BookingStatus |       No | Enum hợp lệ     |
| from      | datetime      |       No | ISO-8601        |
| to        | datetime      |       No | ISO-8601        |
| sort      | string        |       No | field,direction |

### Response Success

```json
{
  "success": true,
  "message": "Bookings retrieved successfully",
  "data": {
    "content": [
      {
        "bookingId": 1001,
        "bookingCode": "LORA-20260621-0001",
        "showtimeId": 10,
        "totalAmount": 240000,
        "status": "CONFIRMED",
        "ticketCount": 2,
        "createdAt": "2026-06-21T20:05:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### Empty Result

```json
{
  "success": true,
  "message": "Bookings retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

### Error: Invalid Query

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Invalid booking query parameters",
  "errorCode": "BOOKING_INVALID_QUERY",
  "data": null,
  "errors": null
}
```

---

## 16.4. Cancel Booking

### Endpoint

```http
POST /api/bookings/{bookingId}/cancel
```

### Request Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
Idempotency-Key: <UUID>
```

### Request Body

```json
{
  "reason": "User cancelled booking"
}
```

`reason` là optional trong Sprint 2 vì schema hiện tại chưa có cột lưu lý do.

### Allowed Status

Customer được cancel khi:

```txt
PENDING_PAYMENT
```

Customer không tự cancel booking `CONFIRMED` trong Sprint 2.

### Response Success

```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "data": {
    "bookingId": 1001,
    "status": "CANCELLED"
  }
}
```

### Error: Invalid Status

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking cannot be cancelled in its current status",
  "errorCode": "BOOKING_CANNOT_BE_CANCELLED",
  "data": null,
  "errors": null
}
```

### Side Effects

Khi customer cancel booking `PENDING_PAYMENT`:

- Booking chuyển sang `CANCELLED`.
- Không tạo ticket.
- Reservation liên quan vẫn giữ trạng thái lịch sử `CONVERTED`.
- Ghế không còn bị xem là unavailable vì booking đã `CANCELLED`.
- Redis seat locks liên quan được release nếu còn tồn tại.
- Payment refund không được xử lý vì customer không thể cancel booking `CONFIRMED`.
- Có thể publish `BOOKING_CANCELLED` ở sprint sau.

Operation phải idempotent theo `Idempotency-Key`.

Nếu booking đã `CANCELLED` bởi cùng request:

```txt
Trả 200 OK với status CANCELLED
```

---

# 17. Ticket APIs

Ticket chỉ tồn tại đối với booking đã:

```txt
CONFIRMED
```

Booking `PENDING_PAYMENT`, `CANCELLED` hoặc `EXPIRED` không được tạo ticket.

Nếu truy vấn ticket của booking `PENDING_PAYMENT`:

```json
{
  "success": true,
  "message": "Tickets retrieved successfully",
  "data": []
}
```

## 17.1. Get Tickets by Booking

### Endpoint

```http
GET /api/bookings/{bookingId}/tickets
```

### Response Success

```json
{
  "success": true,
  "message": "Tickets retrieved successfully",
  "data": [
    {
      "ticketId": 2001,
      "bookingId": 1001,
      "seatId": 101,
      "price": 120000,
      "createdAt": "2026-06-21T20:05:00"
    }
  ]
}
```

### Empty Result

```json
{
  "success": true,
  "message": "Tickets retrieved successfully",
  "data": []
}
```

### Error

* `404 BOOKING_NOT_FOUND`
* `403 FORBIDDEN`

---

## 17.2. Get Ticket Detail

### Endpoint

```http
GET /api/tickets/{ticketId}
```

### Response Success

```json
{
  "success": true,
  "message": "Ticket retrieved successfully",
  "data": {
    "ticketId": 2001,
    "bookingId": 1001,
    "seatId": 101,
    "price": 120000,
    "createdAt": "2026-06-21T20:05:00"
  }
}
```

### Error: Ticket Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Ticket not found",
  "errorCode": "TICKET_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Ticket Visibility Rule

Customer chỉ được xem ticket thuộc booking của chính mình.

---

## 17.3. Ticket Verify/Check-in

Không implement trong Sprint 2 vì schema hiện tại chưa có:

```txt
ticketCode
qrCode
ticketStatus
checkedInAt
checkedInBy
```

Cần tạo issue riêng:

```txt
[Database] Extend Ticket Schema for QR and Check-in Flow
```

Sau schema change mới đặc tả:

```txt
POST /api/admin/tickets/{ticketId}/check-in
POST /api/admin/tickets/verify
```

---

# 18. Admin Booking APIs

## 18.1. Get Booking List

### Endpoint

```http
GET /api/admin/bookings
```

### Query Parameters

| Parameter   | Type          | Required |
| ----------- | ------------- | -------: |
| page        | integer       |       No |
| size        | integer       |       No |
| bookingCode | string        |       No |
| userId      | number        |       No |
| showtimeId  | number        |       No |
| status      | BookingStatus |       No |
| from        | datetime      |       No |
| to          | datetime      |       No |
| sort        | string        |       No |

### Response Success

Trả pagination tương tự `/api/bookings/me`.

### Errors

* `400 BOOKING_INVALID_QUERY`
* `401 UNAUTHORIZED`
* `403 FORBIDDEN`

---

## 18.2. Get Admin Booking Detail

```http
GET /api/admin/bookings/{bookingId}
```

Admin/Employee có quyền được xem booking của mọi user.

---

## 18.3. Update Booking Status

### Endpoint

```http
PATCH /api/admin/bookings/{bookingId}/status
```

### Request

```json
{
  "status": "CANCELLED"
}
```

### Allowed Direction

Admin/Employee không được tự đặt:

```txt
CONFIRMED
```

nếu chưa có payment success hoặc business authorization hợp lệ.

Status adjustment phải tuân thủ transition table.

### Error: Invalid Transition

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Invalid booking status transition",
  "errorCode": "BOOKING_INVALID_STATUS_TRANSITION",
  "data": null,
  "errors": null
}
```

---

# 19. Internal Payment Integration APIs

## 19.1. Get Payment Context

### Endpoint

```http
GET /internal/bookings/{bookingId}/payment-context
```

### Request Headers

```http
X-Internal-Token: <internal-token>
```

### Response Success

```json
{
  "success": true,
  "message": "Booking payment context retrieved successfully",
  "errorCode": null,
  "data": {
    "bookingId": 1001,
    "accountId": 15,
    "bookingStatus": "PENDING_PAYMENT",
    "payable": true,
    "amount": 250000.00,
    "currency": "VND",
    "expiresAt": "2026-07-03T10:15:00Z",
    "analyticsSnapshot": {
      "movieId": 99,
      "movieTitle": "Avengers",
      "ticketCount": 2
    }
  },
  "errors": null
}
```

---

## 19.2. Process Payment Result

### Endpoint

```http
POST /internal/bookings/{bookingId}/payment-results
```

### Request Headers

```http
Content-Type: application/json
X-Internal-Token: <internal-token>
```

### Request

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "schemaVersion": "1.0",
  "paymentId": 123,
  "paymentTransactionCode": "PAY-1001-XYZ",
  "paymentMethod": "VNPAY",
  "result": "SUCCESS",
  "amount": 250000.00,
  "currency": "VND",
  "occurredAt": "2026-07-03T10:05:00Z",
  "externalTransactionId": "EXT-999",
  "reconciliationStatus": "NONE"
}
```

### Deterministic Acknowledgement Responses

**Duplicate Event (200 OK)**
```json
{
  "success": true,
  "data": {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "applied": false,
    "duplicate": true,
    "result": "ALREADY_PROCESSED"
  }
}
```

**Already Confirmed (200 OK)**
```json
{
  "success": true,
  "data": {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "applied": false,
    "duplicate": false,
    "result": "ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT"
  }
}
```

**Confirmed Successfully (200 OK)**
```json
{
  "success": true,
  "data": {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "applied": true,
    "duplicate": false,
    "result": "BOOKING_CONFIRMED"
  }
}
```

---

# 20. Redis Seat Lock Direction

## 20.1. Redis Responsibility

Redis là source of truth tạm thời cho lock real-time:

```txt
showtimeId + seatId
```

Key đề xuất:

```txt
booking:seat-lock:{showtimeId}:{seatId}
```

Value:

```json
{
  "userId": 15,
  "reservationId": 501,
  "expiresAt": "2026-06-21T20:05:00"
}
```

TTL:

```txt
5 phút
```

## 20.2. Database Responsibility

`seat_reservations` lưu trạng thái nghiệp vụ và audit cơ bản.

Redis đảm bảo cạnh tranh real-time.

Database đảm bảo truy vết reservation.

## 20.3. Lock Rule

Dùng atomic operation:

```txt
SET key value NX EX <ttl>
```

Không dùng flow:

```txt
GET
→ nếu chưa có
→ SET
```

vì có race condition.

## 20.4. Partial Lock

Nếu request giữ nhiều ghế và một ghế lock thất bại:

```txt
Rollback/release toàn bộ lock vừa tạo trong request
Không tạo reservation DB
Trả 409
```

## 20.5. Redis Failure

Nếu Redis được bật làm seat lock chính nhưng không hoạt động:

```txt
Không tiếp tục tạo reservation chỉ bằng DB
Trả 503 để tránh double booking
```

Response:

```json
{
  "success": false,
  "message": "Seat reservation service is temporarily unavailable",
  "errorCode": "SEAT_LOCK_SERVICE_UNAVAILABLE",
  "data": null,
  "errors": null
}
```

---

# 21. Expiration Worker

Booking Service phải triển khai scheduled worker hoặc background worker trong Sprint 2.

## 21.1. Reservation Expiration

Query:

```txt
status = HELD
AND expires_at < now
```

Xử lý:

```txt
HELD
→ EXPIRED
→ release Redis seat lock
```

## 21.2. Booking Expiration

Query:

```txt
status = PENDING_PAYMENT
AND expires_at < now
```

Xử lý:

```txt
PENDING_PAYMENT
→ EXPIRED
→ release Redis seat lock nếu còn tồn tại
→ ghế thuộc reservations CONVERTED được xem là available trở lại
```

Không tạo ticket cho booking hết hạn.

## 21.3. Worker Requirements

Worker phải:

- Chạy idempotent.
- Có thể chạy lại an toàn.
- Không expire booking đã `CONFIRMED`.
- Không thay đổi booking đã `CANCELLED` hoặc `EXPIRED`.
- Không tạo duplicate event.
- Xử lý theo batch nếu số lượng record lớn.
- Ghi log số record đã xử lý và số record lỗi.

Redis TTL giải phóng lock real-time, còn worker đồng bộ trạng thái nghiệp vụ trong database.

---

# 22. Concurrency Rules

### Seat Reservation

Một cặp:

```txt
showtimeId + seatId
```

chỉ có một active Redis lock tại một thời điểm.

### Booking Creation

Một reservation chỉ được convert một lần.

Create booking phải chạy trong transaction:

```txt
Validate và lock/version-check reservations
→ Create booking PENDING_PAYMENT
→ Mark reservations CONVERTED
→ Store idempotency result
→ Commit
```

Không tạo ticket trong transaction Create Booking.

Nếu bất kỳ bước nào lỗi:

```txt
Rollback toàn bộ transaction
```

### Payment Confirmation

Confirm payment phải chạy trong transaction:

```txt
Validate callback
→ Lock/version-check booking
→ Change booking to CONFIRMED
→ Create tickets
→ Store idempotency result
→ Commit
```

Payment confirmation phải idempotent.

Không được:

- Confirm booking hai lần.
- Tạo ticket hai lần.
- Publish event hai lần cho cùng payment.

### Optimistic Locking

Schema phải bổ sung:

```txt
bookings.version
seat_reservations.version
```

Entity tương ứng sử dụng `@Version`.

Optimistic locking được dùng để phát hiện concurrent update.

Redis vẫn là cơ chế chính chống giữ trùng ghế real-time.

---

# 23. Security và Ownership Rules

## Customer

Customer chỉ được:

* Xem reservation của mình.
* Release reservation của mình.
* Tạo booking từ reservation của mình.
* Xem booking của mình.
* Xem ticket của mình.
* Cancel booking của mình nếu status cho phép.

## Admin/Employee

Có thể:

* Xem booking toàn hệ thống.
* Tra cứu theo booking code.
* Xem booking detail.
* Điều chỉnh status theo permission và transition hợp lệ.

## Internal Service

Internal API:

```txt
/internal/bookings/**
```

không expose public qua Gateway.

---

# 24. Cross-Service Contract Assumptions

## 24.1. Movie Service

Booking Service cần xác nhận:

```txt
showtime exists
showtime is bookable
showtime startTime
showtime ticketPrice
roomId
seat belongs to room
seat physical status
```

Các dữ liệu này có thể lấy qua internal REST API hoặc contract stub trong Sprint 2.

## 24.2. User/Auth Service

`userId` lấy từ JWT claim:

```txt
accountId
```

Booking Service không nhận user ID tùy ý từ customer request.

## 24.3. Payment Service

Payment Service lưu:

```txt
bookingId
```

Booking Service không cần lưu `paymentId` theo schema hiện tại.

## 24.4. Promotion Service

Schema Booking hiện tại không lưu:

```txt
promotionId
discountAmount
```

Sprint 2 Booking Contract chưa apply promotion vào total.

Khi tích hợp Promotion cần schema/contract update riêng.

## 24.5. Notification Service

Sau này Booking Service có thể publish:

```txt
BOOKING_CREATED
BOOKING_CONFIRMED
BOOKING_CANCELLED
BOOKING_EXPIRED
```

Kafka event implementation không nằm trong issue contract này.

---

# 25. Snapshot Rules

Booking phải giữ dữ liệu tài chính không đổi sau khi tạo.

Hiện schema lưu được:

```txt
bookings.total_amount
tickets.price
```

Nếu giá showtime thay đổi sau khi booking được tạo:

```txt
Booking cũ vẫn giữ totalAmount và ticket.price cũ
```

Schema chưa lưu snapshot:

```txt
movieTitle
roomName
seatLabel
showtimeStartTime
```

Response hiện chỉ trả logical ID.

Nếu muốn lịch sử booking hiển thị đầy đủ ngay cả khi Movie Service thay đổi dữ liệu, cần schema snapshot issue riêng.

---

# 26. Delete Policy

Không hard delete:

```txt
bookings
tickets
seat_reservations
```

Sử dụng status:

```txt
CANCELLED
EXPIRED
RELEASED
```

Lý do:

* Giữ lịch sử.
* Phục vụ audit.
* Tránh phá Payment/Analytics reference.
* Hỗ trợ đối soát.

---

# 27. Error Code Catalog

| Error Code                               | HTTP | Ý nghĩa                      |
| ---------------------------------------- | ---: | ---------------------------- |
| `BOOKING_NOT_FOUND`                      |  404 | Không tìm thấy booking       |
| `BOOKING_INVALID_QUERY`                  |  400 | Query không hợp lệ           |
| `BOOKING_INVALID_STATUS`                 |  400 | Status không hợp lệ          |
| `BOOKING_INVALID_STATUS_TRANSITION`      |  409 | Transition không hợp lệ      |
| `BOOKING_CANNOT_BE_CANCELLED`            |  409 | Không thể cancel             |
| `BOOKING_ALREADY_CREATED`                |  409 | Reservation đã tạo booking   |
| `BOOKING_MULTIPLE_SHOWTIMES_NOT_ALLOWED` |  400 | Reservation khác showtime    |
| `BOOKING_SHOWTIME_NOT_FOUND`             |  404 | Không tìm thấy showtime      |
| `BOOKING_SHOWTIME_NOT_AVAILABLE`         |  409 | Showtime không book được     |
| `BOOKING_SEAT_NOT_FOUND`                 |  404 | Không tìm thấy seat          |
| `BOOKING_SEAT_ROOM_MISMATCH`             |  400 | Seat không thuộc room        |
| `BOOKING_SEAT_ALREADY_HELD`              |  409 | Seat đang được giữ           |
| `BOOKING_SEAT_ALREADY_BOOKED`            |  409 | Seat đã được book            |
| `BOOKING_PAYMENT_AMOUNT_MISMATCH`        |  409 | Số tiền không khớp           |
| `BOOKING_PAYMENT_EXPIRED`                |  409 | Booking hết hạn thanh toán   |
| `SEAT_RESERVATION_NOT_FOUND`             |  404 | Không tìm thấy reservation   |
| `SEAT_RESERVATION_EXPIRED`               |  409 | Reservation hết hạn          |
| `SEAT_RESERVATION_ALREADY_CONVERTED`     |  409 | Reservation đã tạo booking   |
| `SEAT_RESERVATION_OWNERSHIP_MISMATCH`    |  403 | Reservation không thuộc user |
| `SEAT_LOCK_SERVICE_UNAVAILABLE`          |  503 | Redis seat lock lỗi          |
| `TICKET_NOT_FOUND`                       |  404 | Không tìm thấy ticket        |
| `MOVIE_SERVICE_UNAVAILABLE`              |  503 | Movie Service không khả dụng |
| `BOOKING_IDEMPOTENCY_KEY_REQUIRED`      |  400 | Thiếu Idempotency-Key        |
| `BOOKING_IDEMPOTENCY_CONFLICT`          |  409 | Key đã dùng với payload khác |
| `BOOKING_PAYMENT_CONFIRMATION_CONFLICT` |  409 | Booking đã confirm bởi payment khác |
| `BOOKING_OPTIMISTIC_LOCK_CONFLICT`      |  409 | Dữ liệu bị cập nhật đồng thời |
| `VALIDATION_ERROR`                       |  400 | Request validation lỗi       |
| `UNAUTHORIZED`                           |  401 | Chưa đăng nhập               |
| `FORBIDDEN`                              |  403 | Không có quyền               |
| `INTERNAL_SERVER_ERROR`                  |  500 | Lỗi hệ thống                 |

---

# 28. Out of Scope

Không nằm trong Sprint 2 contract hiện tại:

* Promotion apply flow.
* Score redeem flow.
* Payment gateway implementation.
* Refund flow.
* QR ticket.
* Ticket check-in.
* Ticket status lifecycle.
* WebSocket real-time seat broadcast.
* Kafka event implementation.
* Distributed transaction/Saga hoàn chỉnh.
* Snapshot movie title/room/seat label.
* Multiple showtimes trong một booking.
* Reservation extension.
* Guest booking không đăng nhập.
* Direct counter ticket sale.
* Booking transfer.
* Hard delete.

---

# 29. Implementation Issue Direction

Contract implementation chỉ bắt đầu sau khi:

```txt
Booking Contract MR được merge
+
Booking Schema Alignment MR được merge
+
SQL, ERD và entity đã đồng bộ
```

Các implementation issue đề xuất:

```txt
[Backend] Implement Seat Reservation and Redis Lock APIs

[Backend] Implement Booking Core APIs

[Backend] Implement Payment Confirmation and Ticket Creation

[Backend] Implement Ticket Query APIs

[Backend] Implement Booking and Reservation Expiration Worker
```

Thứ tự implementation:

```txt
Schema Alignment
→ Redis Seat Reservation
→ Booking Core
→ Payment Confirmation + Ticket Creation
→ Expiration Worker
→ Ticket Query
```

Mọi thay đổi endpoint, request, response hoặc business rule phải cập nhật contract trong cùng MR.

---

# 30. Acceptance Criteria

Contract hoàn thành khi:

- [x] Có endpoint summary.
- [x] Có Protected/Admin/Internal classification.
- [x] Có request headers.
- [x] Có `Idempotency-Key` rules.
- [x] Có request/response mẫu.
- [x] Có field definitions.
- [x] Có success/error response.
- [x] Có booking lifecycle.
- [x] Có seat reservation lifecycle.
- [x] Có timeout rules.
- [x] Có expiration worker direction.
- [x] Có Redis seat lock direction.
- [x] Có atomic multi-seat reservation rule.
- [x] Có concurrency rule.
- [x] Có Optimistic Locking direction.
- [x] Create Booking không tạo ticket.
- [x] Confirm Payment tạo ticket trong transaction.
- [x] Có payment confirmation idempotency.
- [x] Có ownership/authorization rule.
- [x] Có logical reference notes.
- [x] Có snapshot price rule.
- [x] Có delete policy.
- [x] Có schema alignment requirements.
- [x] Không giả định `promotionId` hoặc `paymentId` đã được lưu.
- [x] Không giả định ticket đã có QR/status.
- [x] Vinh đã review feasibility và cung cấp design decisions.
- [ ] Vinh xác nhận bản contract đã cập nhật.
- [ ] Schema Alignment Issue đã được tạo và liên kết.
- [ ] Tài liệu đủ rõ để tách implementation issues.
- [x] MR target vào `develop`.

---

# 31. Review Decisions

Booking Service Owner đã review và xác nhận:

1. Booking status:

   ```txt
   PENDING_PAYMENT
   CONFIRMED
   CANCELLED
   EXPIRED
   ```

2. Reservation status:

   ```txt
   HELD
   RELEASED
   EXPIRED
   CONVERTED
   ```

3. Reservation TTL: `5 phút`.

4. Booking payment timeout: `15 phút`.

5. Redis seat lock được triển khai trong Sprint 2.

6. Ghế có reservation `CONVERTED` được xem là unavailable khi booking còn:

   ```txt
   PENDING_PAYMENT
   CONFIRMED
   ```

7. Unique index `(showtime_id, seat_id)` phải được xóa.

8. Ticket chỉ được tạo sau payment success.

9. Customer chỉ được cancel booking `PENDING_PAYMENT`.

10. Payment Service lưu `bookingId`; Booking Service không lưu `paymentId`.

11. Internal API dùng `X-Internal-Token` trong Sprint 2.

12. `Idempotency-Key` bắt buộc cho POST API quan trọng.

13. HMAC signature là hướng nâng cấp bảo mật cho sprint sau.

14. Schema phải bổ sung:

   ```txt
   bookings.expires_at
   bookings.version
   seat_reservations.version
   ```

15. Booking Service phải có expiration worker.

16. Schema Alignment Issue phải được merge trước Backend implementation.

---

# 32. Lịch Sử Chỉnh Sửa

| Ngày | Nội dung | Người thực hiện |
|---|---|---|
| 21/06/2026 | Khởi tạo Booking Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật contract theo review của Booking Service Owner: đổi thời điểm tạo ticket, bổ sung Idempotency-Key, expiration worker, schema alignment, Optimistic Locking và webhook security direction | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận là hoàn tất tại tài liệu sau khi Schema Alignment MR tương ứng được merge.
