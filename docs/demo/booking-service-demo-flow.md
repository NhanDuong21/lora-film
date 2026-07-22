# Booking Service Demo Flow

## Demo 1

### Tạo Booking

Input

```text
Movie

↓

Showtime

↓

Seat

↓

Promotion
```

Kết quả

```
Booking Created
```

---

## Demo 2

Thanh toán thành công

```
Booking

↓

PENDING_PAYMENT

↓

CONFIRMED
```

Hiển thị:

- Ticket
- History
- Audit
- Outbox

---

## Demo 3

Thanh toán thất bại

```
PENDING_PAYMENT

↓

CANCELLED
```

Hiển thị:

- History

- Audit

- Event

---

## Demo 4

Booking hết hạn

Đợi:

```
10 phút
```

Scheduler

↓

Booking

↓

EXPIRED

---

## Demo 5

Refund

```
CONFIRMED

↓

REFUNDED
```

Hiển thị:

- Refund Event

- History

- Audit

---

## Demo 6

Duplicate Request

Gửi:

```
POST /bookings
```

hai lần.

Kết quả.

Chỉ sinh:

```
1 Booking
```

---

## Demo 7

Redis Lock

User A

↓

Seat A01

↓

SUCCESS

User B

↓

Seat A01

↓

FAIL

---

## Demo 8

Outbox

Booking Created

↓

Outbox

↓

Publisher

↓

Kafka

↓

Notification

---

## Demo 9

Retry

Kafka Stop

↓

Retry

↓

Retry

↓

Kafka Start

↓

Publish Success

---

## Demo 10

Reconciliation

Booking

```
PENDING_PAYMENT
```

Payment

```
SUCCESS
```

↓

Scheduler

↓

Repair

↓

CONFIRMED

---

## Demo 11

Food & Beverage

Booking

```
PENDING_PAYMENT
```

↓

Thêm đồ ăn (Popcorn, Combo)

↓

Validate qua Food Catalog Client

↓

Tính toán lại Tổng tiền (Final Amount)

↓

Lưu Snapshot (Giá, Tên, Hình ảnh) vào DB

---

## Demo Checklist

Trong buổi demo nên trình diễn lần lượt:

- ✅ Đăng nhập và xác thực JWT.
- ✅ Chọn suất chiếu và ghế.
- ✅ Tạo Booking.
- ✅ Sinh Booking Code.
- ✅ Sinh Ticket.
- ✅ Thêm đồ ăn (Food & Beverage) & Kiểm tra tổng tiền.
- ✅ Lưu Snapshot giá của đồ ăn.
- ✅ Thanh toán thành công.
- ✅ Booking chuyển sang `CONFIRMED`.
- ✅ Outbox Event được tạo.
- ✅ Notification/Analytics nhận Event (nếu có).
- ✅ Hủy Booking hợp lệ.
- ✅ Booking hết hạn bằng Scheduler.
- ✅ Duplicate Request với `Idempotency-Key`.
- ✅ Redis Lock ngăn hai người đặt cùng một ghế.
- ✅ Kiểm tra Audit Log và Status History.
- ✅ Swagger hiển thị đầy đủ API.
- ✅ Kiểm tra Health Check (`/actuator/health`).

---
