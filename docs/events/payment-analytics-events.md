# Đặc Tả Hợp Đồng Event: Payment & Analytics (Payment Events Contract)

Tài liệu này quy định cấu trúc gói tin (Message Schema), định dạng phân phối và các nguyên tắc xử lý bất đồng bộ đối với luồng thanh toán và thống kê doanh thu giữa Payment Service và Analytics Service.

---

## 1. Kiến Trúc Luồng Đi Dữ Liệu (Message Topology Flow)

Payment Service đóng vai trò là Producer, phát hành các sự kiện liên quan đến giao dịch thanh toán thành công hoặc hoàn tiền. Analytics Service đóng vai trò là Consumer, lắng nghe các sự kiện này để cập nhật các thống kê: `daily_revenue_stats`, `movie_revenue_stats`, `movie_daily_revenue_stats` và `processed_analytics_events`.
Analytics Service tiêu thụ các sự kiện này để tổng hợp doanh thu theo thời gian thực mà không cần query trực tiếp Payment Database hay Booking Database. Mọi dữ liệu cần cho việc aggregate phải được cung cấp đầy đủ trong event.

### Giai đoạn 1: Thanh Toán Thành Công
- Payment Service xử lý thanh toán. Sau khi giao dịch thành công và lưu vào DB, phát hành sự kiện `PAYMENT_SUCCEEDED`.
- Analytics Service tiêu thụ sự kiện này để tăng doanh thu cho bộ phim tương ứng.

### Giai đoạn 2: Hoàn Tiền (Refund)
- Payment Service xử lý hoàn tiền (có thể là một phần hoặc toàn bộ).
- Phát hành sự kiện `PAYMENT_REFUNDED`.
- Analytics Service tiêu thụ sự kiện này để giảm trừ doanh thu. Có thể có nhiều sự kiện hoàn tiền cho cùng một thanh toán.

---

## 2. Thông Tin Cấu Hình Hàng Đợi Kafka (Kafka Stream Configurations)

| Thuộc tính | Sự kiện 1 (Thanh toán thành công) | Sự kiện 2 (Hoàn tiền) |
| :--- | :--- | :--- |
| **Topic** | `payment.payment-succeeded.v1` | `payment.payment-refunded.v1` |
| **DLQ Topic** | `payment.payment-succeeded.v1.dlq` | `payment.payment-refunded.v1.dlq` |
| **Loại Sự Kiện** | `PAYMENT_SUCCEEDED` | `PAYMENT_REFUNDED` |
| **Producer** | `payment-service` | `payment-service` |
| **Consumer** | `analytics-service` | `analytics-service` |
| **Consumer Group** | `analytics-service-payment-group` | `analytics-service-refund-group` |
| **Message Key** | `paymentId` | `paymentId` |

> **Lưu ý Message Key:** Cả hai topic đều sử dụng `paymentId` làm message key để đảm bảo tất cả các sự kiện liên quan đến cùng một giao dịch (thành công, hoàn tiền lần 1, hoàn tiền lần 2...) được điều phối vào cùng một Partition.
> * Ordering được bảo đảm trong cùng partition (cùng một Payment).
> * **Không bảo đảm ordering** giữa các payment khác nhau.

---

## 3. Đặc Tả Gói Tin Sự Kiện (Event Payload Specification)

Định dạng gói tin thống nhất (Event Envelope) sử dụng **payload wrapper object** (để đồng nhất với tiêu chuẩn hiện tại của repository như `account-created-event.md`).

**Event Envelope:**
Bao gồm các trường metadata chuẩn: `eventId`, `eventType`, `eventVersion`, `sourceService`, `occurredAt`, `correlationId`, `traceId`. Dữ liệu nghiệp vụ chi tiết được đặt trong object `data`.

### 3.1. Event: PAYMENT_SUCCEEDED

**Mô tả:** Được Payment Service phát hành sau khi giao dịch thanh toán thành công.

**Định Dạng Mẫu (JSON):**
```json
{
  "eventId": "PAYMENT-SUCCEEDED-3001",
  "eventType": "PAYMENT_SUCCEEDED",
  "eventVersion": "1.0",
  "sourceService": "payment-service",
  "occurredAt": "2026-06-24T20:12:00+07:00",
  "correlationId": "corr-12345",
  "traceId": "trace-67890",
  "data": {
    "paymentId": 3001,
    "bookingId": 1001,
    "paidAmount": 216000,
    "currency": "VND",
    "movieId": 101,
    "movieTitle": "Avengers",
    "ticketCount": 2
  }
}
```

**Định Nghĩa Trường Dữ Liệu (Field Definitions):**

*   **`eventId`** (String): Định danh duy nhất của sự kiện. Sinh ra một lần và cố định cho dù có retry.
*   **`eventType`** (String): Luôn là `PAYMENT_SUCCEEDED`.
*   **`eventVersion`** (String): Phiên bản cấu trúc sự kiện (hiện tại `1.0`).
*   **`sourceService`** (String): Nguồn phát hành, luôn là `payment-service`.
*   **`occurredAt`** (String - ISO-8601): Thời điểm Payment chuyển sang trạng thái thành công. Phải dùng ISO-8601 và có timezone/offset rõ ràng. Analytics dùng field này để xác định `statDate` khi aggregate.
*   **`correlationId`, `traceId`** (String): Hỗ trợ truy vết log liên dịch vụ (Distributed Tracing).
*   **`data.paymentId`** (Long): ID giao dịch thanh toán. Ý nghĩa nghiệp vụ: Xác định duy nhất giao dịch thanh toán.
*   **`data.bookingId`** (Long): ID của đơn đặt vé liên kết.
*   **`data.paidAmount`** (Long/BigDecimal): Số tiền cuối cùng Payment Service xác nhận đã thanh toán thành công. Số tiền này có thể đã phản ánh các khoản giảm giá như Promotion discount, Membership discount, Score Redeem... Analytics không tự tính lại.
*   **`data.currency`** (String): Đơn vị tiền tệ, cố định là `VND`.
*   **`data.movieId`** (Long): ID của bộ phim.
*   **`data.movieTitle`** (String): Tên bộ phim, giúp Analytics Service không cần query Movie Service.
*   **`data.ticketCount`** (Integer): Số lượng vé đã mua trong giao dịch này.

### 3.2. Event: PAYMENT_REFUNDED

**Mô tả:** Được Payment Service phát hành khi tiến hành hoàn tiền cho người dùng.

**Hành vi hỗ trợ hoàn tiền:**
*   **Full & Partial Refund:** Hỗ trợ hoàn tiền toàn bộ hoặc một phần.
*   **Multiple Refund Events:** Cho phép nhiều sự kiện hoàn tiền trên cùng một `paymentId` (ví dụ: hoàn tiền làm nhiều đợt).
*   **Refund Event Semantics (Quan trọng):**
    *   Mỗi `PAYMENT_REFUNDED` event đại diện cho một refund transaction độc lập.
    *   `refundAmount` và `refundedTicketCount` là giá trị của lần refund đó, **không phải** giá trị cộng dồn (cumulative) của toàn bộ payment.
*   **Ordering:** Đảm bảo `PAYMENT_REFUNDED` luôn được consume sau `PAYMENT_SUCCEEDED` đối với cùng một `paymentId` bằng cách sử dụng chung Message Key.

**Định Dạng Mẫu (JSON):**
```json
{
  "eventId": "PAYMENT-REFUNDED-3001-1",
  "eventType": "PAYMENT_REFUNDED",
  "eventVersion": "1.0",
  "sourceService": "payment-service",
  "occurredAt": "2026-06-24T21:00:00+07:00",
  "correlationId": "corr-12345",
  "traceId": "trace-67890",
  "data": {
    "paymentId": 3001,
    "bookingId": 1001,
    "refundAmount": 216000,
    "currency": "VND",
    "movieId": 101,
    "movieTitle": "Avengers",
    "refundedTicketCount": 2
  }
}
```

> **Lưu ý `eventId` Generation Strategy:** Vì có thể có nhiều Refund cho 1 Payment, `eventId` của Refund nên kết hợp giữa ID của Payment và ID của Refund Transaction (hoặc sequence), ví dụ: `PAYMENT-REFUNDED-<paymentId>-<refundSeq>`.

---

## 4. Ràng Buộc Dữ Liệu (Validation Rules)

### 4.1. Phía Producer (Payment Service)
Trước khi gửi thông điệp, Payment Service phải đảm bảo:
*   Event được publish **chỉ sau khi** giao dịch database của nghiệp vụ đã commit thành công (sử dụng Transactional Outbox Pattern nếu có).
*   Bổ sung đầy đủ thông tin phim (`movieId`, `movieTitle`) vào Payload, tránh việc Analytics phải gọi các API nội bộ khác.

### 4.2. Phía Consumer (Analytics Service)
**PAYMENT_SUCCEEDED:**
*   `eventId` không được rỗng (not blank).
*   `paymentId`, `bookingId`, `movieId`, `ticketCount` > 0.
*   `paidAmount` >= 0.
*   `currency` = "VND".
*   `movieTitle` không được rỗng.
*   `occurredAt` đúng định dạng ISO-8601.

**PAYMENT_REFUNDED:**
*   `eventId` không được rỗng (not blank).
*   `paymentId`, `bookingId`, `movieId`, `refundedTicketCount` > 0.
*   `refundAmount` > 0.
*   `currency` = "VND".
*   `movieTitle` không được rỗng.
*   `occurredAt` đúng định dạng ISO-8601.

---

## 5. Cơ Chế Xử Lý Trùng Lặp & Tính Cố Vị (Idempotency)

Mạng Kafka cung cấp cơ chế At-Least-Once Delivery, có thể dẫn đến việc nhận 1 gói tin nhiều lần (Duplicate Events).

*   **Một Business Event = Một Event ID ổn định:** `eventId` sinh ra phải cố định theo nghiệp vụ, không thay đổi ngay cả khi Producer retry gửi.
*   **Consumer Deduplication:** Analytics Service BẮT BUỘC lưu `eventId` vào bảng `processed_analytics_events.event_id` trong **cùng một Transaction** của bước cộng/trừ doanh thu. 
*   Trước khi xử lý, Analytics Service phải kiểm tra `eventId` đã tồn tại trong bảng này chưa. Nếu có rồi -> Bỏ qua Event (Duplicate handling) và Ack Kafka bình thường. Tuyệt đối không phụ thuộc vào Kafka offsets cho Idempotency.

---

## 6. Lỗi & Chiến Lược Khôi Phục (Retry & DLQ)

### 6.1. Xử lý Consumer Lỗi (Retry Behavior)
*   **Lỗi Retryable (Có thể thử lại):** Database unavailable, Kafka transient failure, Transaction deadlock, Temporary infrastructure error.
    *   *Chiến lược:* Consumer throw Exception để Kafka poll lại message.
    *   *Retry count & Backoff:* Cấu hình Backoff theo hướng tăng dần (ví dụ 1s, 2s, 5s) và giới hạn số lần Retry (ví dụ max 3 lần). Duplicate event không được retry vô hạn.
    *   *Chuyển DLQ:* Khi vượt quá số lần Retry count tối đa, thông điệp sẽ bị chuyển sang DLQ.
    *   *Acknowledge behavior:* Consumer chỉ acknowledge (commit offset) khi xử lý thành công hoặc sau khi đã ném thành công vào DLQ.
*   **Lỗi Non-Retryable (Không thể thử lại):** Sai cấu trúc JSON, thiếu trường bắt buộc (`invalid payload`), phiên bản event không hỗ trợ, sai kiểu dữ liệu (`invalid amount`, `invalid currency`).
    *   *Chiến lược:* Consumer bắt lỗi, in cảnh báo, không retry và ném thẳng Message vào **DLQ (Dead Letter Queue)**. Không ảnh hưởng đến các Message tiếp theo.

### 6.2. DLQ (Dead Letter Queue)
Sử dụng 2 topic tương ứng:
*   `payment.payment-succeeded.v1.dlq`
*   `payment.payment-refunded.v1.dlq`

Những event quá số lần Retry tối đa (Retry Exhausted) hoặc dính lỗi Non-Retryable sẽ được lưu vào DLQ để đội ngũ vận hành kiểm tra (Manual Intervention) mà không gây tắc nghẽn luồng xử lý chính.

---

## 7. Trách Nhiệm Phân Hệ (Responsibilities)

### 7.1. Payment Service (Producer)
*   Publish event sau khi transaction nghiệp vụ đã hoàn thành (nếu dùng Outbox Pattern, phải mô tả direction trong implementation issue riêng).
*   Không publish success event khi payment chưa thực sự thành công.
*   Không publish refund event khi refund chưa thực sự thành công.
*   Giữ `eventId` ổn định qua các lần retry.
*   Publish payload đúng Event Version đã thoả thuận.
*   Cung cấp đủ enriched fields (từ Booking/Movie) để Analytics không cần query ngược lại.

### 7.2. Analytics Service (Consumer)
*   Phải tự thực hiện validate payload đầu vào.
*   Từ chối và không xử lý các bản tin có `eventVersion` chưa được hỗ trợ (đẩy sang DLQ).
*   Dùng `eventId` cho idempotency, lưu vào bảng `processed_analytics_events` để chống aggregate trùng.
*   Dùng `occurredAt` để xác định ngày aggregate (`statDate`).
*   Thực hiện Aggregate trong một transaction duy nhất.
*   Không gọi API ngược lại Payment/Booking/Movie Service trong aggregate transaction.
*   Không tự tính lại discount hoặc payment amount, chỉ sử dụng field cung cấp trong event.
*   Xử lý phòng thủ (Defensive Consumer Handling): Consumer vẫn phải xử lý phòng thủ nếu event đến sai thứ tự (ví dụ Refund event đến trước Succeeded event).
