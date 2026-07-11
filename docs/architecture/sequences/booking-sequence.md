# Sequence Diagrams

## Booking Flow

> Đây là draft flow cho **Booking module** trong hệ thống đặt vé xem phim online.
> Flow này mô tả quá trình user chọn phim, suất chiếu, ghế, giữ ghế tạm thời, thanh toán, lưu booking và gửi thông báo sau khi booking thành công.

## Diagram liên quan

```text
docs/architecture/diagrams/booking-sequence.puml
docs/architecture/diagrams/booking-sequence.drawio
docs/architecture/diagrams/booking-sequence.png
```

## Mục đích

Tài liệu này mô tả luồng đặt vé chính của hệ thống, bao gồm:

* User chọn phim, suất chiếu và ghế
* Frontend gửi request giữ ghế đến API Gateway
* Booking Service kiểm tra trạng thái ghế
* Redis giữ ghế tạm thời
* Payment Service xử lý thanh toán
* Booking Service lưu booking vào MySQL
* Kafka publish event booking confirmed
* Notification Service nhận event và gửi thông báo

## Mô tả flow

### Bước 1: User chọn phim, suất chiếu và ghế

User thao tác trên React Frontend để chọn phim, suất chiếu và ghế muốn đặt.

### Bước 2: Frontend gửi request giữ ghế đến API Gateway

Sau khi user chọn ghế, React Frontend gửi request đến API Gateway để yêu cầu giữ ghế tạm thời.

Ví dụ endpoint:

```text
POST /api/bookings/hold-seats
```

Request có thể gồm:

```json
{
  "userId": "USER_ID",
  "movieId": "MOVIE_ID",
  "showtimeId": "SHOWTIME_ID",
  "seatIds": ["A1", "A2"]
}
```

### Bước 3: API Gateway route request đến Booking Service

API Gateway nhận request từ frontend và chuyển tiếp request đến Booking Service.

Booking Service chịu trách nhiệm xử lý nghiệp vụ đặt vé và trạng thái ghế.

### Bước 4: Booking Service kiểm tra trạng thái ghế

Booking Service kiểm tra các ghế được chọn có còn trống không.

Các điều kiện cần kiểm tra:

* Ghế có tồn tại trong phòng chiếu không
* Ghế có thuộc đúng suất chiếu không
* Ghế đã được đặt chưa
* Ghế có đang bị user khác giữ tạm thời không

### Bước 5: Redis giữ ghế tạm thời

Nếu ghế còn trống, Booking Service lưu trạng thái giữ ghế tạm thời vào Redis.

Redis có thể dùng TTL để tự động hết hạn giữ ghế.

Ví dụ:

```text
seat_hold:showtimeId:A1 -> userId
TTL: 5 minutes
```

Nếu user không thanh toán trong thời gian giữ ghế, ghế sẽ tự động được mở lại.

### Bước 6: User xác nhận thanh toán

Sau khi ghế được giữ thành công, user xác nhận thanh toán trên frontend.

Frontend gửi request thanh toán đến API Gateway.

Ví dụ endpoint:

```text
POST /api/bookings/confirm
```

### Bước 7: Payment Service xử lý thanh toán

Booking Service gửi yêu cầu thanh toán sang Payment Service.

Payment Service xử lý thanh toán dựa trên tổng tiền booking.

Nếu thanh toán thất bại, hệ thống trả lỗi về frontend và có thể release ghế đang giữ.

Nếu thanh toán thành công, Booking Service tiếp tục lưu booking chính thức.

### Bước 8: Booking Service lưu booking vào MySQL

Sau khi thanh toán thành công, Booking Service lưu thông tin booking vào MySQL.

Dữ liệu booking có thể gồm:

```json
{
  "bookingId": "BOOKING_ID",
  "userId": "USER_ID",
  "movieId": "MOVIE_ID",
  "showtimeId": "SHOWTIME_ID",
  "seatIds": ["A1", "A2"],
  "totalAmount": 200000,
  "paymentStatus": "PAID",
  "bookingStatus": "CONFIRMED"
}
```

### Bước 9: Kafka publish event booking confirmed

Sau khi booking được lưu thành công, Booking Service publish event `BookingConfirmed` vào Kafka.

Ví dụ event:

```json
{
  "eventName": "BookingConfirmed",
  "bookingId": "BOOKING_ID",
  "userId": "USER_ID",
  "showtimeId": "SHOWTIME_ID",
  "seatIds": ["A1", "A2"],
  "totalAmount": 200000,
  "confirmedAt": "2026-06-04T10:00:00"
}
```

### Bước 10: Notification Service nhận event và gửi thông báo

Notification Service consume event `BookingConfirmed` từ Kafka.

Sau đó Notification Service gửi thông báo cho user, ví dụ:

* Email xác nhận đặt vé
* Thông báo trong hệ thống
* Push notification nếu sau này có mobile app

## Ghi chú

* Đây là draft flow cho Booking module.
* Redis được dùng để giữ ghế tạm thời và tránh nhiều user đặt cùng một ghế.
* MySQL được dùng để lưu booking chính thức sau khi thanh toán thành công.
* Kafka được dùng để publish event sau khi booking confirmed.
* Notification Service xử lý gửi thông báo bất đồng bộ sau khi nhận event từ Kafka.
* Code PlantUML để vẽ diagram nằm trong file `booking-sequence.puml`.
