# Đặc Tả Hợp Đồng Event Hủy Đặt Vé (Booking Cancelled Event Contract)

Tài liệu này quy định cấu trúc gói tin (Message Schema), định dạng phân phối và các nguyên tắc xử lý bất đồng bộ đối với luồng sự kiện hủy đặt vé (`BOOKING_CANCELLED`) giữa Booking Service và Analytics Service.

---

## 1. Kiến Trúc Luồng Đi Dữ Liệu (Message Topology Flow)

- **Giai đoạn:** Sau khi Booking Service lưu thành công trạng thái hủy vé vào cơ sở dữ liệu.
- **Mục đích của Analytics Service:** Chỉ sử dụng sự kiện này để cập nhật số lượng vé đã hủy (`daily_revenue_stats.cancelled_bookings_count += 1`).

**Những việc Analytics Service KHÔNG ĐƯỢC LÀM:**
* Truy vấn lại Booking Database.
* Giải phóng ghế (Release Seats).
* Xử lý hoàn tiền (Process Refunds).
* Trừ doanh thu hoặc giảm số vé/giao dịch thành công. (Việc điều chỉnh hoàn tiền phải đến từ các sự kiện của Payment Service như `PAYMENT_REFUNDED`).
* Sửa đổi doanh thu của bộ phim.

---

## 2. Thông Tin Cấu Hình Hàng Đợi Kafka (Kafka Stream Configurations)

| Thuộc tính | Chi tiết |
| :--- | :--- |
| **Topic** | `booking.booking-cancelled.v1` |
| **Mục đích Topic** | Truyền tải sự kiện hủy vé từ Booking Service sang các service khác. |
| **Quyền sở hữu (Owner)** | `booking-service` |
| **Loại Sự Kiện** | `BOOKING_CANCELLED` |
| **Producer** | `booking-service` |
| **Consumer** | `analytics-service` |
| **Phiên bản (Event Version)** | `1.0` |
| **Message Key** | `bookingId` |

**Quy tắc về Phiên bản (Versioning Strategy):**
* Bất kỳ thay đổi phá vỡ cấu trúc (breaking changes) nào cũng yêu cầu tạo topic mới và version mới.
* Các version hiện tại phải giữ nguyên tính ổn định.
* Không được âm thầm thay đổi payload.

**Quy tắc về Message Key:**
* Key được sử dụng là `bookingId` nhằm đảm bảo tất cả các sự kiện của cùng một booking sẽ được đẩy vào cùng một partition.
* Thứ tự (ordering) chỉ được đảm bảo trong phạm vi một partition.
* Không sử dụng các ID ngẫu nhiên làm Kafka key.

---

## 3. Đặc Tả Gói Tin Sự Kiện (Event Payload Specification)

### 3.1. Required Payload (Trường Bắt Buộc)

```json
{
  "eventId": "BOOKING-CANCELLED-1002",
  "eventType": "BOOKING_CANCELLED",
  "eventVersion": "1.0",
  "sourceService": "booking-service",
  "occurredAt": "2026-06-24T20:30:00+07:00",
  "bookingId": 1002,
  "previousStatus": "PENDING_PAYMENT",
  "currentStatus": "CANCELLED",
  "reason": "USER_CANCELLED"
}
```

### 3.2. Optional Fields (Trường Tùy Chọn)

```json
{
  "cancelledBy": "USER",
  "correlationId": "uuid",
  "traceId": "uuid",
  "cancelType": "MANUAL",
  "metadata": {}
}
```

---

## 4. Định Nghĩa Các Trường (Field Definitions)

### Các trường bắt buộc

* **`eventId`**: Định danh duy nhất cho sự kiện hủy nghiệp vụ.
  * Phải ổn định qua các lần retry.
  * Không được tạo mới khi Kafka gửi lại bản tin.
  * Được sử dụng bởi cơ chế Idempotency của Analytics Service.
* **`eventType`**: Giá trị cố định: `BOOKING_CANCELLED`.
* **`eventVersion`**: Phiên bản schema (hiện tại `1.0`).
* **`sourceService`**: Giá trị cố định: `booking-service`.
* **`occurredAt`**: Thời điểm Booking Service commit thành công trạng thái hủy.
  * Analytics **phải sử dụng** trường này để gom cụm (aggregation) theo ngày.
  * Analytics **KHÔNG ĐƯỢC DÙNG** thời gian nhận bản tin của consumer.
  * Múi giờ: `Asia/Ho_Chi_Minh`.
* **`bookingId`**: Identifier số dương, tham chiếu bất biến đến booking.
* **`previousStatus`**: Trạng thái ngay trước khi hủy. Ví dụ: `PENDING_PAYMENT`, `CONFIRMED`, `AWAITING_PAYMENT` (Theo đúng enum của Booking Service).
* **`currentStatus`**: Giá trị chính thức được chuẩn hóa là: `CANCELLED`.
  * *Tất cả các trường hợp hủy (bao gồm cả timeout/expired) đều phải được Producer chuẩn hóa (normalize) thành `CANCELLED` trong event payload này. Consumer chỉ chấp nhận duy nhất giá trị `CANCELLED` để đảm bảo tính nhất quán.*
* **`reason`**: Lý do hủy. Bắt buộc dùng các giá trị Enum chính thức sau, không sử dụng chuỗi tự do:
  * `USER_CANCELLED`
  * `PAYMENT_TIMEOUT`
  * `ADMIN_CANCELLED`
  * `SYSTEM_CANCELLED`
  * `PAYMENT_FAILED`

---

## 5. Phạm Vi Đếm Số Lượng Hủy (Cancellation Counting Scope)

Analytics Service sẽ ghi nhận `cancelled_bookings_count += 1` cho tất cả các lý do sau:
* `USER_CANCELLED`
* `PAYMENT_TIMEOUT`
* `PAYMENT_FAILED`
* `ADMIN_CANCELLED`
* `SYSTEM_CANCELLED`

**Lưu ý:**
* Analytics sẽ đếm mọi vé bị hủy (kể cả vé chưa thanh toán, vé quá hạn thanh toán, vé thanh toán lỗi, hủy bởi admin/system).
* **Vé đã hoàn tiền (Refunded bookings):** Nếu booking bị hủy và có hoàn tiền, Booking Service vẫn gửi `BOOKING_CANCELLED`. Việc điều chỉnh doanh thu/hoàn tiền được xử lý riêng rẽ qua sự kiện hoàn tiền của Payment.

---

## 6. Điều Kiện Phát Hành (Publish Conditions)

* Sự kiện `BOOKING_CANCELLED` **CHỈ ĐƯỢC PHÁT HÀNH SAU KHI** transaction cập nhật trạng thái booking thành công (commit thành công).
* Không bao giờ publish trước khi commit.
* Một nghiệp vụ hủy duy nhất sinh ra đúng một logical event.
* Nếu retry publish, phải sử dụng lại đúng `eventId` cũ.
* Các request hủy trùng lặp (idempotent duplicate requests) không được sinh ra event business mới.

---

## 7. Các Nguyên Tắc Idempotency (Idempotency Rules)

* **Một nghiệp vụ hủy -> 1 eventId ổn định.**
* Analytics Service **phải sử dụng** trường `processed_analytics_events.event_id` để đảm bảo: `exactly-once business aggregation` (chỉ cộng 1 lần duy nhất).
* Việc chỉ dựa vào Kafka offset là không đủ để đảm bảo idempotency vì có thể xảy ra at-least-once delivery dẫn đến trùng bản tin.

---

## 8. Nguyên Tắc Về Thứ Tự (Ordering Rules)

* **Thứ tự chỉ được đảm bảo trong cùng một partition.**
* Các sự kiện của cùng một `bookingId` sẽ giữ đúng thứ tự.
* Việc sắp xếp thứ tự chéo giữa các partition không được đảm bảo.
* **Xử lý sự kiện lệch thứ tự (Out-of-order events):** Nếu Consumer nhận được `BOOKING_CANCELLED` trước khi nhận các sự kiện tạo booking khác, hệ thống phải xử lý khéo léo (ví dụ: tạo bản ghi trống chờ cập nhật sau hoặc lưu DLQ).

---

## 9. Chiến Lược Retry & Khôi Phục Lỗi (Retry & DLQ Strategy)

### 9.1 Lỗi Có Thể Retry (Retryable Errors)
* Database không khả dụng.
* Kafka broker tạm thời không phản hồi.
* Lỗi mạng tạm thời.
* Transaction deadlock.
* Lỗi hạ tầng khác.

### 9.2 Lỗi Không Thể Retry (Non-Retryable Errors)
* `eventId` bị trống (blank).
* `eventType` không hợp lệ.
* `bookingId` không hợp lệ (ví dụ: số âm).
* `occurredAt` sai định dạng.
* Payload lỗi cấu trúc JSON (Malformed payload).
* Phiên bản không được hỗ trợ.
* `currentStatus` không hợp lệ.
=> Các lỗi này nên được đẩy thẳng vào DLQ hoặc bỏ qua (và log error).

### 9.3 Dead Letter Queue (DLQ)
* **Topic gợi ý:** `booking.booking-cancelled.v1.dlq`.
* **Khuyến nghị:** Cấu hình retry số lần cụ thể (ví dụ: 3-5 lần) với cơ chế Exponential backoff trước khi đẩy vào DLQ.
* Tin nhắn rơi vào DLQ sẽ được rà soát lại thủ công hoặc chạy lại tự động. Thao tác chạy lại từ DLQ phải đảm bảo idempotent để tránh duplicate behavior.
* Không được retry vô hạn.
* Acknowledge: Consumer phải gửi ack cho Kafka dù thành công hay đẩy sang DLQ thành công để tránh kẹt offset.

---

## 10. Nguyên Tắc Validation (Validation Rules)

Analytics Service phải kiểm tra hợp lệ ít nhất các yếu tố sau:
1. `eventId` không rỗng.
2. `eventType` đúng là `BOOKING_CANCELLED`.
3. `sourceService` đúng là `booking-service`.
4. `occurredAt` đúng chuẩn định dạng thời gian.
5. `bookingId` > 0.
6. `previousStatus` không rỗng.
7. `currentStatus` bắt buộc phải là `CANCELLED` (Nếu là EXPIRED, Producer phải normalize thành CANCELLED).
8. `reason` hợp lệ thuộc các Enum cho phép.

---

## 11. Trách Nhiệm Các Bên

### Trách Nhiệm của Producer (Booking Service)
* Chỉ phát hành sự kiện sau khi commit DB thành công.
* Tránh phát hành sự kiện trùng lặp do lỗi logic.
* Giữ nguyên `eventId` khi retry publish.
* Phát hành đúng phiên bản schema, đúng trạng thái và đúng lý do.
* KHÔNG BẮT BUỘC Analytics phải gọi lại Booking DB để lấy thêm thông tin (trừ khi cần thiết qua API, nhưng ở đây Analytics không được truy vấn DB).
* KHÔNG ĐƯA logic hoàn tiền vào sự kiện hủy vé.

### Trách Nhiệm của Consumer (Analytics Service)
* Validate sự kiện đến.
* Từ chối (reject) các phiên bản không được hỗ trợ.
* Sử dụng `eventId` để đảm bảo idempotency.
* Dùng `occurredAt` để tính toán ngày thống kê (statDate).
* Cộng dồn số lượng hủy đúng 1 lần.
* **Không được** truy vấn trực tiếp Booking DB.
* **Không được** tự ý trừ doanh thu.
* **Không được** giảm số lượng vé (ticket count).
* **Không được** xử lý quy trình hoàn tiền.

---

## 12. Tính Nhất Quán Giữa Các Service (Cross-Service Consistency)

* Đảm bảo sự nhất quán với Hợp đồng API của Booking, Booking Status Enum, luồng Payment Refund, và các Hợp đồng Analytics khác.
* **Chuẩn hóa đặt tên:** Không cho phép sử dụng lẫn lộn các giá trị chưa thống nhất như `CANCELLED`, `CANCELED`, `TIMEOUT`, `EXPIRED` trừ khi có tài liệu rõ ràng. Giá trị chính thức luôn sử dụng chuẩn tiếng Anh đã định (VD: `CANCELLED`).

---

## 13. Ngoài Phạm Vi Bàn Giao (Out Of Scope)

Tài liệu hợp đồng này **KHÔNG BAO GỒM** việc triển khai các tính năng sau (sẽ được xử lý ở các issue/task riêng biệt):
* Triển khai API hủy đặt vé (Booking Cancellation API).
* Logic chuyển đổi trạng thái của Booking (Booking Status Transition Logic).
* Code thực thi Kafka Producer & Kafka Consumer.
* Logic nhả ghế (Seat Release).
* Xử lý hoàn tiền (Refund Processing).
* Thu hồi vé (Ticket Revocation).
* Triển khai Outbox Pattern.
* Triển khai Retry & DLQ trong code.
