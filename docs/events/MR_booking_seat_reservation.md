Tổng quan (Summary)
MR này triển khai các API quản lý Seat Reservation (Giữ chỗ) trong booking-service dựa trên Booking Service API Contract và database schema đã được phê duyệt.
Implementation xây dựng đầy đủ:

JPA Entity mapping cho bảng seat_reservations
DTO validation và response model
Redis Distributed Lock hỗ trợ multi-seat atomic
Cơ chế Idempotency chống duplicate request
Optimistic Locking bằng @Version
Swagger UI Integration
Client giả lập (MockMovieServiceClient) giao tiếp nội bộ

Đây là nền tảng cốt lõi cho các luồng:
* Book vé và Thanh toán
* Quản lý trạng thái ghế (Seat Status)
* Hủy vé (Release/Cancel)
* Auto Expiration (Worker xử lý vé hết hạn)

---

Related Issue
Closes #

---

Changes

1. Configuration & Dependencies
Thêm dependencies:
* `spring-boot-starter-validation`
* `spring-boot-starter-data-redis`
* `springdoc-openapi-starter-webmvc-ui`

Bổ sung cấu hình `application.properties`:
* Cấu hình database MySQL dialect và auto-create.
* Cấu hình kết nối Redis server mặc định.

2. Data Model & Entity
Tạo/Cập nhật entity: `SeatReservation`
Bao gồm:
* Trạng thái vé (`HELD`, `RELEASED`, `CONVERTED`)
* `@Version` cho optimistic locking.
* Tự động quản lý `createdAt`, `expiresAt`.

Tạo/Cập nhật entity: `IdempotencyRecord` để track payload.

3. DTO & Mapping Layer
Request DTOs:
* `CreateSeatReservationRequest`

Response DTOs:
* `CreateSeatReservationResponse`
* `SeatReservationResponse`
* `ReleaseSeatReservationResponse`
* `SeatInfo` / `ShowtimeInfo` (cho giao tiếp với Movie Service)

Validation:
* `@NotNull`
* `@Positive`
* `@NotEmpty`

4. Common Response & Exception Handling
Triển khai:
* `ApiResponse`
* `BusinessException`
* `GlobalExceptionHandler`

Các error code được hỗ trợ:
* `BOOKING_SHOWTIME_NOT_FOUND`
* `BOOKING_SEAT_NOT_FOUND`
* `BOOKING_SEAT_ALREADY_HELD`
* `SEAT_RESERVATION_NOT_FOUND`
* `SEAT_RESERVATION_ALREADY_CONVERTED`
* `BOOKING_IDEMPOTENCY_CONFLICT`
* `BOOKING_OPTIMISTIC_LOCK_CONFLICT`
* `FORBIDDEN`
* `VALIDATION_ERROR`
* `MISSING_HEADER`

5. Security & Swagger Integration
Triển khai:
* `CurrentUserProvider` (Abstraction tạm thời để lấy JWT userId = 15L theo đúng yêu cầu không chọc trực tiếp vào Auth JWT).
* `OpenApiConfig` để setup Swagger docs.

Swagger truy cập tại: `/swagger-ui/index.html`

6. Repository Layer
Triển khai:
* `SeatReservationRepository`
* Hỗ trợ tìm kiếm theo userId và Id.
* `IdempotencyRepository` track cache request cũ.

7. Service Layer
Triển khai:
* `SeatReservationServiceImpl`: Handle nghiệp vụ chính.
* `RedisSeatLockService`: Xử lý khóa Atomic bằng Redis `setIfAbsent` loop, tự động rollback lock các ghế trước nếu có ghế thất bại.
* `IdempotencyService`: Quản lý logic retry / chống duplicate.
* `BookingSeatValidator`: Hỗ trợ validate đầu vào với MovieService.

Business rules:
* multi-seat atomic locking.
* idempotent request caching.
* owner validation.
* optimistic locking update.

8. Controller Layer
Triển khai:
* `SeatReservationController`
Endpoints:
* `POST /api/bookings/seat-reservations`
* `GET /api/bookings/seat-reservations/{reservationId}`
* `DELETE /api/bookings/seat-reservations/{reservationId}`

---

API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/bookings/seat-reservations` | Tạo giữ chỗ mới |
| GET | `/api/bookings/seat-reservations/{reservationId}` | Xem chi tiết giữ chỗ của user |
| DELETE | `/api/bookings/seat-reservations/{reservationId}` | Hủy giữ chỗ và nhả khóa ghế |

---

Business Rules
* Header `Idempotency-Key` là bắt buộc cho POST.
* `seatIds` không được rỗng và không được chứa ID trùng lặp.
* Showtime phải tồn tại bên Movie DB.
* Các ghế phải tồn tại và có trong phòng chiếu tương ứng.
* Giữ nhiều ghế phải Atomic (1 ghế xịt -> Rollback toàn bộ ghế khác trong mảng).
* Chỉ User owner mới được xem (GET) hoặc hủy (DELETE) vé của mình.
* Không được phép xóa vé đã ở trạng thái `CONVERTED`.
* Hỗ trợ optimistic locking chống race condition ghi đè DB.

---

How To Test

Manual Test via Swagger
1. Truy cập Swagger UI:
Mở `http://localhost:8083/swagger-ui/index.html`
2. Test các API trực tiếp trên UI. (Lưu ý: Hệ thống hiện tại đang mock User ID = 15).

Test Scenarios
* Create Reservation:
  * Đặt thành công (Happy path).
  * Lỗi trùng lặp ghế (Validation error).
  * Lỗi ghế đã có người đặt (Conflict).
* Idempotency:
  * Bắn 2 request giống hệt nhau -> Trả về y hệt, không tạo vé thứ 2.
* Query Reservation:
  * Xem vé hợp lệ.
  * Xem vé không tồn tại (404).
  * Lỗi 403 nếu truy cập vé của user khác.
* Release Reservation:
  * Xóa thành công.
  * Thử xóa vé đã `CONVERTED` (400).
* Optimistic Lock:
  * Sửa cột version trong DB rồi test update/delete đồng thời.

---

Seat Reservation APIs - Input & Output

Base URL
`/api/bookings/seat-reservations`

1. Create Seat Reservation
Endpoint
`POST /api/bookings/seat-reservations`

Headers
`Idempotency-Key`: `uuid-12345`

Request Body
```json
{ "showtimeId": 10, "seatIds": [101, 102] }
```

Success Response (201 Created)
```json
{
  "success": true,
  "errorCode": null,
  "message": "Success",
  "data": {
    "reservationId": 1,
    "status": "HELD"
  },
  "errors": null
}
```

Duplicate/Idempotent Conflict Response (409 Conflict)
```json
{
  "success": false,
  "errorCode": "BOOKING_IDEMPOTENCY_CONFLICT",
  "message": "Idempotency conflict",
  "data": null,
  "errors": null
}
```

2. Get Seat Reservation Detail
Endpoint
`GET /api/bookings/seat-reservations/{reservationId}`

Success Response (200 OK)
```json
{
  "success": true,
  "errorCode": null,
  "message": "Success",
  "data": {
    "id": 1,
    "showtimeId": 10,
    "seatId": 101,
    "userId": 15,
    "status": "HELD",
    "expiresAt": "2026-06-26T10:05:00",
    "createdAt": "2026-06-26T10:00:00"
  },
  "errors": null
}
```

Forbidden Response (403)
```json
{
  "success": false,
  "errorCode": "FORBIDDEN",
  "message": "You are not the owner of this reservation",
  "data": null,
  "errors": null
}
```

3. Release Seat Reservation
Endpoint
`DELETE /api/bookings/seat-reservations/{reservationId}`

Success Response (200 OK)
```json
{
  "success": true,
  "errorCode": null,
  "message": "Success",
  "data": {
    "reservationId": 1,
    "status": "RELEASED"
  },
  "errors": null
}
```

Converted/Invalid Response (400 Bad Request)
```json
{
  "success": false,
  "errorCode": "SEAT_RESERVATION_ALREADY_CONVERTED",
  "message": "Reservation is already converted to booking",
  "data": null,
  "errors": null
}
```

---

Summary

| API | Input | Output |
|---|---|---|
| POST `/seat-reservations` | Header `Idempotency-Key`, `showtimeId`, `seatIds` | `CreateSeatReservationResponse` |
| GET `/seat-reservations/{id}` | `reservationId` | `SeatReservationResponse` |
| DELETE `/seat-reservations/{id}` | `reservationId` | `ReleaseSeatReservationResponse` |

---

Verification Results
* Implementation compiles successfully.
* APIs conform to the approved API contract.
* Multi-seat atomic and Idempotency logic functionally verified.
* Notification/Movie service boundaries remain isolated via abstractions.
* *(Note: Automated Unit/Integration tests are deferred/pending based on pipeline requirements).*
