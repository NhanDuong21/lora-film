# Payment Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                                                       |
| -------------- | ------------------------------------------------------------------------------ |
| Service        | `payment-service`                                                              |
| Feature        | Payment Transaction and Payment Log Management                                 |
| API liên quan  | Create Payment, Payment Session, Payment Query, Callback/Webhook, Payment Logs |
| Contract Owner | Dương Thiện Nhân                                                               |
| Backend Owner  | Trần Hiển Vinh                                                                 |
| Reviewer       | Trần Hiển Vinh                                                                 |
| Trạng thái     | Draft / Ready for Review                                                       |
| Milestone      | Sprint 2 - Core Service API Foundation                                         |
| Ngày cập nhật  | 22/06/2026                                                                     |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API thuộc `payment-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Thống nhất API Contract giữa Frontend, API Gateway, Booking Service và Payment Service.
* Làm cơ sở implement payment foundation trong Sprint 2.
* Chuẩn hóa quy trình tạo payment, tạo mock/sandbox payment session, xử lý callback và cập nhật trạng thái.
* Xác định rõ payment lifecycle, idempotency và audit log.
* Phân định rõ trách nhiệm giữa Booking Service và Payment Service.
* Không để Frontend tự quyết định số tiền thanh toán.
* Không lưu thông tin thẻ hoặc dữ liệu thanh toán nhạy cảm.
* Làm cơ sở tách implementation issues sau khi contract được review.

---

## 3. Phạm Vi Payment Service

Payment Service chịu trách nhiệm:

* Tạo giao dịch thanh toán theo booking.
* Sinh mã giao dịch nội bộ.
* Quản lý trạng thái payment.
* Lưu phương thức thanh toán.
* Tạo mock/sandbox payment session hoặc payment URL.
* Tiếp nhận callback/webhook từ payment provider.
* Xác minh callback hoặc signature.
* Lưu external transaction ID.
* Lưu raw response phục vụ đối soát trong phạm vi an toàn.
* Ghi log mỗi lần payment thay đổi trạng thái.
* Truy vấn payment theo `paymentId`, `bookingId` hoặc transaction code.
* Thông báo kết quả payment cho Booking Service.

Payment Service không chịu trách nhiệm:

* Tạo hoặc quản lý booking.
* Tính giá vé từ Movie Service.
* Quản lý ghế hoặc ticket.
* Quản lý mã promotion.
* Quản lý điểm thưởng.
* Lưu thông tin thẻ ngân hàng.
* Gửi email hoặc notification.
* Tự thay đổi dữ liệu trực tiếp trong Booking database.

---

## 4. Physical Schema Sprint 0

### 4.1. Bảng `payments`

```sql
CREATE TABLE `payments` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Payment ID',
  `payment_transaction_code` varchar(100) UNIQUE NOT NULL COMMENT 'Ma giao dich noi bo tu sinh, e.g., PAY-LORAFILM-998877',
  `booking_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang bookings.id cua Booking Service',
  `amount` decimal(10,2) NOT NULL,
  `payment_method` varchar(30) NOT NULL COMMENT 'VNPAY, MOMO, CASH',
  `external_transaction_id` varchar(100) COMMENT 'Ma giao dich tra ve tu phia Cong thanh toan (VNPAY/MOMO ID)',
  `status` varchar(30) DEFAULT 'PENDING' COMMENT 'PENDING, SUCCESS, FAILED, REFUNDED',
  `raw_response_payload` text COMMENT 'Luu toan bo du lieu JSON/QueryString ma cong thanh toan tra ve de doi soat khi can',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);
```

### 4.2. Bảng `payment_logs`

```sql
CREATE TABLE `payment_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  `previous_status` varchar(30),
  `current_status` varchar(30) NOT NULL,
  `log_message` text COMMENT 'Chi tiet dien bien, e.g., Create payment link, Received Webhook success',
  `created_at` timestamp DEFAULT (now())
);
```

### 4.3. Quan hệ nội bộ

```sql
ALTER TABLE `payment_logs`
ADD FOREIGN KEY (`payment_id`)
REFERENCES `payments` (`id`)
ON DELETE CASCADE;
```

---

## 5. Schema Alignment Bắt Buộc Trước Implementation

Sau khi Payment Service Owner review contract, các thay đổi schema sau được xác định là blocker và phải hoàn thành trước khi triển khai Backend.

### 5.1. Mô hình nhiều payment attempts

Một booking có thể có nhiều payment attempts.

Mỗi attempt phải là một payment record riêng với:

```txt
payment_transaction_code riêng
external_transaction_id riêng
provider_session_id riêng
payment_url riêng
status riêng
payment_logs riêng
```

Không được reset hoặc ghi đè payment attempt cũ khi user retry.

Unique constraint hiện tại:

```txt
payments.booking_id UNIQUE
```

phải được xóa và thay bằng non-unique index:

```txt
INDEX idx_payments_booking_id (booking_id)
```

### 5.2. Các thay đổi schema bắt buộc

Schema phải bổ sung:

```txt
payments.expires_at
payments.payment_url
payments.provider_session_id
payments.version
```

Schema phải bổ sung unique constraint:

```txt
payments.external_transaction_id UNIQUE
```

Constraint cho `external_transaction_id` chỉ áp dụng khi giá trị khác `NULL` theo behavior của MySQL.

### 5.3. Payment status chính thức

```txt
PENDING
PROCESSING
SUCCESS
FAILED
CANCELLED
EXPIRED
REFUNDED
```

SQL comment, Backend enum và contract phải dùng cùng bộ status.

### 5.4. Payment session timeout

Payment session timeout trong Sprint 2:

```txt
15 phút
```

`payments.expires_at` được lưu trực tiếp trong database để worker/cronjob xử lý payment hết hạn.

### 5.5. Persist payment session

Các field sau phải được persist:

```txt
payment_url
provider_session_id
expires_at
```

Không xem các field này là response-derived-only.

### 5.6. Optimistic Locking

Bảng `payments` phải có:

```txt
version INT NOT NULL DEFAULT 0
```

Entity tương ứng sử dụng `@Version` để chống race condition giữa callback, CASH confirmation, cancel và expiry worker.

### 5.7. Raw response payload

`raw_response_payload` tiếp tục được lưu để audit và reconciliation, nhưng phải sanitize trước khi insert.

Không được lưu:

```txt
full card number
CVV
OTP
password
provider secret
private key
access token
authorization header
```

### 5.8. Related Schema Issue

Các thay đổi được tracking trong issue:

```txt
[Database] Align Payment Schema with Payment API Contract
```

Payment implementation chưa được bắt đầu trước khi Schema Alignment MR được merge.

## 6. Database-per-Service và Logical Reference

`bookingId` là logical reference tới Booking Service.

Payment Service:

* Không tạo foreign key vật lý sang Booking database.
* Không truy cập trực tiếp Booking database.
* Không tự thay đổi booking status bằng SQL.
* Phải gọi Internal API hoặc publish event cho Booking Service.

### Source of truth

| Dữ liệu                       | Source of truth   |
| ----------------------------- | ----------------- |
| Booking status, booking total | Booking Service   |
| Payment transaction và status | Payment Service   |
| Movie, seat, showtime         | Movie Service     |
| Promotion rule                | Promotion Service |
| Payment log                   | Payment Service   |

---

## 7. API Gateway và Service URL

### 7.1. API Gateway URL

Frontend chỉ gọi:

```txt
http://localhost:8080
```

### 7.2. Payment Service Direct URL

Chỉ dùng cho debug hoặc backend integration:

```txt
http://localhost:8084
```

Port chính thức lấy từ cấu hình project.

### 7.3. Request Flow

```txt
React Frontend
→ API Gateway
→ Payment Service
→ Payment Database
```

### 7.4. Booking Validation Flow

```txt
Payment Service
→ Booking Service Internal API
→ Validate booking
→ Get booking total and status
→ Create/update payment
```

Payment Service không tin trực tiếp dữ liệu amount do Frontend gửi.

---

## 8. Quy Ước Chung

### 8.1. Content Type

```http
Content-Type: application/json
```

### 8.2. Protected API Header

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Các POST API tạo hoặc thay đổi dữ liệu quan trọng phải gửi:

```http
Idempotency-Key: <UUID>
```

Bắt buộc cho:

```txt
POST /api/payments
POST /api/payments/{paymentId}/cancel
POST /api/admin/payments/{paymentId}/confirm-cash
POST /api/admin/payments/{paymentId}/mark-failed
```

### 8.3. Internal API Header

```http
X-Internal-Token: <internal-token>
Idempotency-Key: <UUID>
Content-Type: application/json
```

Internal API chỉ được gọi qua network/gateway nội bộ được bảo vệ.

### 8.4. Callback/Webhook Header

Tùy provider, ví dụ:

```http
X-Signature: <provider-signature>
Content-Type: application/json
```

Provider callback không dùng JWT của user.

Callback phải xác minh HMAC/RSA hoặc signature mechanism đúng theo provider specification.

### 8.5. Datetime Format

```txt
ISO-8601
YYYY-MM-DDTHH:mm:ss
```

### 8.6. Timezone

```txt
Asia/Ho_Chi_Minh
```

### 8.7. Currency

Sprint 2 chỉ hỗ trợ:

```txt
VND
```

Amount trả dưới dạng number.

### 8.8. Idempotency Rules

`Idempotency-Key` phải là UUID do caller tạo cho mỗi logical request.

Nếu cùng user gửi lại cùng payload với cùng key:

```txt
Không tạo payment attempt lần hai
Trả lại kết quả của request đầu tiên
```

Nếu cùng key nhưng payload khác:

```txt
409 PAYMENT_IDEMPOTENCY_CONFLICT
```

Create Payment có thể dùng Redis lock:

```txt
payment:create:{userId}:{idempotencyKey}
```

Idempotency không được dựa vào unique constraint của `booking_id`.

## 9. Common Response Contract

### 9.1. Success Response

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

### 9.2. Error Response

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
      "field": "paymentMethod",
      "message": "Payment method is invalid"
    }
  ]
}
```

---

## 10. Payment Method

### 10.1. PaymentMethod

Schema Sprint 0 định hướng:

```txt
VNPAY
MOMO
CASH
```

Trong Sprint 2:

```txt
MOCK
CASH
```

có thể được dùng để test foundation nếu VNPay/MoMo sandbox chưa được tích hợp.

Bộ enum đề xuất:

```txt
MOCK
VNPAY
MOMO
CASH
```

### 10.2. Method Rules

| Method | Behavior                                                      |
| ------ | ------------------------------------------------------------- |
| MOCK   | Tạo mock payment session để test                              |
| VNPAY  | Tạo payment URL theo VNPay sandbox/production                 |
| MOMO   | Tạo payment URL theo MoMo sandbox/production                  |
| CASH   | Xác nhận bởi nhân viên tại quầy, không tự success từ Frontend |

Frontend không được tự gọi API confirm payment thành công.

---

## 11. Payment Status Lifecycle

### 11.1. PaymentStatus chính thức

```txt
PENDING
PROCESSING
SUCCESS
FAILED
CANCELLED
EXPIRED
REFUNDED
```

### 11.2. Allowed Transitions

| Current | Allowed Next |
|---|---|
| PENDING | PROCESSING, SUCCESS, FAILED, CANCELLED, EXPIRED |
| PROCESSING | SUCCESS, FAILED, CANCELLED, EXPIRED |
| SUCCESS | REFUNDED |
| FAILED | Không chuyển lại; retry tạo payment record mới |
| CANCELLED | Không chuyển lại; retry tạo payment record mới |
| EXPIRED | Không chuyển lại; retry tạo payment record mới |
| REFUNDED | Không có |

### 11.3. Transition Rules

Không cho:

```txt
SUCCESS → PENDING
SUCCESS → FAILED
FAILED → PENDING
CANCELLED → PENDING
EXPIRED → PENDING
REFUNDED → SUCCESS
```

Callback lặp lại cùng kết quả phải được xử lý idempotent.

### 11.4. Terminal Status

Frontend dừng polling khi payment có một trong các status:

```txt
SUCCESS
FAILED
CANCELLED
EXPIRED
REFUNDED
```

`REFUNDED` được giữ trong enum để mở rộng nhưng refund API hoàn chỉnh nằm ngoài Sprint 2.

## 12. API Classification

### 12.1. Protected Customer APIs

```txt
POST /api/payments
GET  /api/payments/{paymentId}
GET  /api/payments/booking/{bookingId}
GET  /api/payments/{paymentId}/status
GET  /api/payments/{paymentId}/logs
POST /api/payments/{paymentId}/cancel
```

### 12.2. Admin/Employee APIs

```txt
GET  /api/admin/payments
GET  /api/admin/payments/{paymentId}
GET  /api/admin/payments/{paymentId}/logs
POST /api/admin/payments/{paymentId}/confirm-cash
POST /api/admin/payments/{paymentId}/mark-failed
```

### 12.3. Callback/Webhook APIs

```txt
POST /api/payments/callback/mock
POST /api/payments/callback/vnpay
POST /api/payments/callback/momo
```

Callback endpoint được expose có kiểm soát và phải xác minh signature/provider data.

### 12.4. Internal APIs

```txt
POST /internal/payments/{paymentId}/confirm
POST /internal/payments/{paymentId}/fail
```

Các endpoint này chỉ dùng nếu flow backend-to-backend cần thiết.

Không expose `/internal/**` qua Gateway.

---

## 13. Endpoint Summary

| Method | Endpoint                                       | Access         | Mục đích                  |
| ------ | ---------------------------------------------- | -------------- | ------------------------- |
| POST   | `/api/payments`                                | Protected      | Tạo payment theo booking  |
| GET    | `/api/payments/{paymentId}`                    | Protected      | Lấy payment detail        |
| GET    | `/api/payments/booking/{bookingId}`            | Protected      | Lấy payment theo booking  |
| GET    | `/api/payments/{paymentId}/status`             | Protected      | Query trạng thái          |
| GET    | `/api/payments/{paymentId}/logs`               | Protected      | Lịch sử trạng thái        |
| POST   | `/api/payments/{paymentId}/cancel`             | Protected      | Hủy payment chưa hoàn tất |
| POST   | `/api/payments/callback/mock`                  | Callback       | Mock callback             |
| POST   | `/api/payments/callback/vnpay`                 | Callback       | VNPay callback            |
| POST   | `/api/payments/callback/momo`                  | Callback       | MoMo callback             |
| GET    | `/api/admin/payments`                          | Admin/Employee | Danh sách payment         |
| GET    | `/api/admin/payments/{paymentId}`              | Admin/Employee | Payment detail quản trị   |
| GET    | `/api/admin/payments/{paymentId}/logs`         | Admin/Employee | Log quản trị              |
| POST   | `/api/admin/payments/{paymentId}/confirm-cash` | Admin/Employee | Xác nhận tiền mặt         |
| POST   | `/api/admin/payments/{paymentId}/mark-failed`  | Admin/Employee | Ghi nhận thất bại         |
| POST   | `/internal/payments/{paymentId}/confirm`       | Internal       | Internal confirm          |
| POST   | `/internal/payments/{paymentId}/fail`          | Internal       | Internal fail             |

---

# 14. Payment Core APIs

## 14.1. Create Payment

### Endpoint

```http
POST /api/payments
```

### Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
Idempotency-Key: <UUID>
```

`Idempotency-Key` là bắt buộc.

### Request Body

```json
{
  "bookingId": 1001,
  "paymentMethod": "MOCK"
}
```

Frontend không gửi:

```txt
amount
status
paymentTransactionCode
externalTransactionId
paymentUrl
providerSessionId
expiresAt
```

### Field Definitions

| Field | Type | Required | Validation |
|---|---|---:|---|
| bookingId | number | Yes | > 0 |
| paymentMethod | string | Yes | PaymentMethod enum |

### Processing Flow

```txt
Validate Idempotency-Key
→ Resolve authenticated user
→ Check idempotency result
→ Acquire Redis lock cho logical request
→ Validate booking exists
→ Validate booking belongs to current user
→ Validate booking status = PENDING_PAYMENT
→ Validate booking is not expired
→ Get totalAmount from Booking Service
→ Check booking chưa có payment SUCCESS
→ Check active attempt PENDING/PROCESSING chưa hết hạn
→ Generate paymentTransactionCode mới
→ Create payment record mới với PENDING
→ Calculate expiresAt = now + 15 phút
→ Create provider/mock session
→ Persist paymentUrl và providerSessionId
→ Create initial payment log
→ Store idempotency result
→ Return payment response
```

### Amount Source of Truth

Payment Service lấy amount từ:

```txt
booking.totalAmount
```

Không tin amount do Frontend gửi.

### Retry Behavior

Nếu attempt trước có status:

```txt
FAILED
CANCELLED
EXPIRED
```

user retry sẽ tạo payment record mới với:

```txt
paymentId mới
paymentTransactionCode mới
providerSessionId mới
paymentUrl mới
expiresAt mới
payment log mới
```

Không reset attempt cũ về `PENDING`.

Nếu booking đã có payment `SUCCESS`:

```txt
Không tạo attempt mới
Trả 409 PAYMENT_ALREADY_SUCCESSFUL
```

Nếu booking đang có attempt `PENDING` hoặc `PROCESSING` chưa hết hạn:

```txt
Trả attempt active hiện tại nếu cùng Idempotency-Key
Nếu là logical request khác, trả 409 PAYMENT_ACTIVE_ATTEMPT_EXISTS
```

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Payment created successfully",
  "data": {
    "paymentId": 3002,
    "paymentTransactionCode": "PAY-LORAFILM-20260622-002",
    "bookingId": 1001,
    "amount": 240000,
    "currency": "VND",
    "paymentMethod": "MOCK",
    "status": "PENDING",
    "paymentSession": {
      "providerSessionId": "MOCK-SESSION-3002",
      "paymentUrl": "http://localhost:8080/api/payments/mock/3002",
      "expiresAt": "2026-06-22T10:30:00"
    },
    "createdAt": "2026-06-22T10:15:00"
  }
}
```

Các field session được persist tại:

```txt
payments.provider_session_id
payments.payment_url
payments.expires_at
```

### Error: Active Attempt Exists

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "An active payment attempt already exists for this booking",
  "errorCode": "PAYMENT_ACTIVE_ATTEMPT_EXISTS",
  "data": null,
  "errors": null
}
```

### Error: Idempotency Conflict

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Idempotency key was already used with a different request",
  "errorCode": "PAYMENT_IDEMPOTENCY_CONFLICT",
  "data": null,
  "errors": null
}
```

Các error case Booking Not Found, Ownership Mismatch, Booking Not Payable, Booking Expired, Payment Already Successful và Booking Service Unavailable giữ nguyên như contract hiện tại.

## 14.2. Get Payment Detail

### Endpoint

```http
GET /api/payments/{paymentId}
```

Customer chỉ được xem payment thuộc booking của mình.

### Response Success

```json
{
  "success": true,
  "message": "Payment retrieved successfully",
  "data": {
    "paymentId": 3001,
    "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
    "bookingId": 1001,
    "amount": 240000,
    "currency": "VND",
    "paymentMethod": "MOCK",
    "externalTransactionId": "MOCK-TXN-3001",
    "status": "SUCCESS",
    "createdAt": "2026-06-21T20:10:00",
    "updatedAt": "2026-06-21T20:12:00"
  }
}
```

Không trả `rawResponsePayload` cho Customer API.

### Error: Payment Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Payment not found",
  "errorCode": "PAYMENT_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Forbidden

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "You cannot access this payment",
  "errorCode": "FORBIDDEN",
  "data": null,
  "errors": null
}
```

---

## 14.3. Get Payment Attempts by Booking

### Endpoint

```http
GET /api/payments/booking/{bookingId}
```

Customer chỉ được xem attempts thuộc booking của mình.

### Query Parameters

| Parameter | Type | Required | Validation |
|---|---|---:|---|
| page | integer | No | >= 0 |
| size | integer | No | 1–50 |
| status | PaymentStatus | No | Enum hợp lệ |
| sort | string | No | Mặc định `createdAt,desc` |

### Response Success

```json
{
  "success": true,
  "message": "Payment attempts retrieved successfully",
  "data": {
    "content": [
      {
        "paymentId": 3002,
        "paymentTransactionCode": "PAY-LORAFILM-20260622-002",
        "bookingId": 1001,
        "paymentMethod": "MOCK",
        "amount": 240000,
        "status": "PENDING",
        "providerSessionId": "MOCK-SESSION-3002",
        "paymentUrl": "http://localhost:8080/api/payments/mock/3002",
        "expiresAt": "2026-06-22T10:30:00",
        "createdAt": "2026-06-22T10:15:00"
      },
      {
        "paymentId": 3001,
        "paymentTransactionCode": "PAY-LORAFILM-20260622-001",
        "bookingId": 1001,
        "paymentMethod": "MOCK",
        "amount": 240000,
        "status": "FAILED",
        "expiresAt": "2026-06-22T10:10:00",
        "createdAt": "2026-06-22T09:55:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

### Latest Attempt Endpoint

```http
GET /api/payments/booking/{bookingId}/latest
```

Endpoint này trả payment attempt mới nhất theo `createdAt DESC`.

### Response Error

- `404 PAYMENT_NOT_FOUND`
- `403 FORBIDDEN`

## 14.4. Query Payment Status

### Endpoint

```http
GET /api/payments/{paymentId}/status
```

### Response Success

```json
{
  "success": true,
  "message": "Payment status retrieved successfully",
  "data": {
    "paymentId": 3001,
    "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
    "status": "SUCCESS",
    "updatedAt": "2026-06-21T20:12:00"
  }
}
```

Endpoint này có thể dùng để Frontend polling trong mock/sandbox flow.

Polling interval đề xuất:

```txt
3–5 giây
```

Frontend phải dừng polling khi status là:

```txt
SUCCESS
FAILED
CANCELLED
EXPIRED
REFUNDED
```

---

## 14.5. Cancel Payment

### Endpoint

```http
POST /api/payments/{paymentId}/cancel
```

### Request Body

```json
{
  "reason": "User cancelled payment"
}
```

`reason` chưa được lưu riêng trong `payments`, nhưng có thể lưu vào `payment_logs.log_message`.

### Allowed Status

Chỉ cho cancel khi payment:

```txt
PENDING
PROCESSING
```

### Response Success

```json
{
  "success": true,
  "message": "Payment cancelled successfully",
  "data": {
    "paymentId": 3001,
    "status": "CANCELLED"
  }
}
```

### Error: Cannot Cancel

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Payment cannot be cancelled in its current status",
  "errorCode": "PAYMENT_CANNOT_BE_CANCELLED",
  "data": null,
  "errors": null
}
```

Nếu schema/status không bổ sung `CANCELLED`, endpoint này phải bỏ khỏi Sprint 2 hoặc map sang `FAILED`. Khuyến nghị bổ sung `CANCELLED` để phân biệt user cancel với provider failure.

---

# 15. Mock Payment Flow

## 15.1. Mục Tiêu

Mock payment hỗ trợ test Sprint 2 mà chưa cần tích hợp provider production.

Flow:

```txt
Create Payment
→ status PENDING
→ return mock payment URL
→ user mở mock payment page
→ chọn Success hoặc Failed
→ callback vào Payment Service
→ update status
→ ghi payment log
→ notify Booking Service
```

## 15.2. Mock Payment Success Callback

### Endpoint

```http
POST /api/payments/callback/mock
```

### Request Body

```json
{
  "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
  "result": "SUCCESS",
  "mockTransactionId": "MOCK-TXN-3001"
}
```

### Response Success

```json
{
  "success": true,
  "message": "Mock payment processed successfully",
  "data": {
    "paymentId": 3001,
    "status": "SUCCESS"
  }
}
```

### Processing

```txt
Find payment by transaction code
→ verify current status
→ validate callback
→ update externalTransactionId
→ set SUCCESS
→ store raw response
→ create payment log
→ notify Booking Service
```

## 15.3. Mock Payment Failure

Request:

```json
{
  "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
  "result": "FAILED",
  "mockTransactionId": "MOCK-TXN-3001",
  "message": "User rejected mock payment"
}
```

Payment chuyển:

```txt
PENDING/PROCESSING → FAILED
```

Booking vẫn có thể ở `PENDING_PAYMENT` nếu chưa hết payment timeout.

---

# 16. Provider Callback/Webhook Direction

## 16.1. VNPay Callback

```http
POST /api/payments/callback/vnpay
```

Hoặc theo provider requirement có thể dùng `GET`.

Endpoint cuối cùng phải đồng bộ với tài liệu VNPay chính thức khi tích hợp.

## 16.2. MoMo Callback

```http
POST /api/payments/callback/momo
```

## 16.3. Callback Rules

Callback phải:

- Xác minh HMAC/RSA hoặc signature đúng theo provider.
- Xác minh transaction code.
- Xác minh amount.
- Xác minh external transaction ID.
- Kiểm tra payment tồn tại.
- Kiểm tra session chưa hết hạn nếu provider rule yêu cầu.
- Kiểm tra callback không bị xử lý trùng.
- Dùng optimistic locking qua `payments.version`.
- Sanitize raw payload trước khi lưu.
- Tạo payment log.
- Không expose provider secret trong log hoặc response.

## 16.4. Provider Response Format

Provider callback response **không tuân theo Common Response Contract của LoraFilm**.

Response phải đúng HTTP status, header và body mà provider yêu cầu.

Ví dụ hướng VNPay:

```json
{
  "RspCode": "00",
  "Message": "Confirm Success"
}
```

MoMo phải trả đúng format theo tài liệu MoMo.

Common response dạng:

```json
{
  "success": true,
  "message": "..."
}
```

chỉ áp dụng cho:

- Customer API
- Admin API
- Internal API của LoraFilm
- Mock callback do LoraFilm kiểm soát

Không áp dụng mặc định cho callback thật từ provider.

## 16.5. Invalid Signature

Payment Service phải ghi log verification failure và trả response đúng provider specification.

Không nhất thiết trả Common Error DTO.

## 16.6. Amount Mismatch

Không cập nhật payment sang `SUCCESS`.

Ghi audit log và trả provider acknowledgement/error đúng provider specification.

## 16.7. Transaction Not Found

Không tạo payment mới từ callback.

Ghi audit log và trả response đúng provider specification.

# 17. Callback Idempotency

Callback có thể được provider gửi nhiều lần.

Idempotency key nghiệp vụ:

```txt
paymentTransactionCode
+
externalTransactionId
+
provider result
```

### Callback SUCCESS lặp

Nếu payment đã `SUCCESS` và callback hợp lệ trùng lặp:

```txt
Trả 200 OK
Không update status lần nữa
Không tạo duplicate booking confirmation
Có thể ghi log duplicate callback hoặc bỏ qua
```

Response:

```json
{
  "success": true,
  "message": "Payment callback already processed",
  "data": {
    "paymentId": 3001,
    "status": "SUCCESS",
    "idempotent": true
  }
}
```

### Callback conflict

Nếu payment đã `SUCCESS` nhưng callback mới báo `FAILED`:

```txt
Không chuyển SUCCESS → FAILED
Ghi audit log
Trả lỗi conflict hoặc provider acknowledgement phù hợp
```

Error code:

```txt
PAYMENT_CALLBACK_STATUS_CONFLICT
```

---

# 18. Booking Service Integration

## 18.1. Payment Success

Sau khi payment chuyển `SUCCESS`, Payment Service gọi:

```http
POST /internal/bookings/{bookingId}/confirm-payment
```

Headers:

```http
X-Internal-Token: <internal-token>
Idempotency-Key: <UUID>
Content-Type: application/json
```

Request:

```json
{
  "paymentId": 3002,
  "paymentTransactionCode": "PAY-LORAFILM-20260622-002",
  "paidAmount": 240000,
  "paidAt": "2026-06-22T10:20:00"
}
```

Booking Service xử lý idempotent và tạo ticket khi booking chuyển sang `CONFIRMED`.

## 18.2. Payment Failed

Khi một attempt chuyển `FAILED`, Payment Service có thể gọi:

```http
POST /internal/bookings/{bookingId}/fail-payment
```

Request:

```json
{
  "paymentId": 3001,
  "paymentTransactionCode": "PAY-LORAFILM-20260622-001",
  "status": "FAILED",
  "reason": "Provider rejected transaction"
}
```

`FAILED` của một payment attempt không tự động làm booking thất bại.

Booking vẫn có thể ở `PENDING_PAYMENT` để user tạo attempt mới nếu chưa hết `bookings.expires_at`.

## 18.3. Payment Expired hoặc Cancelled

Payment Service có thể thông báo Booking Service để phục vụ audit/UI, nhưng Booking không bắt buộc chuyển trạng thái ngay nếu vẫn còn payment window.

Canonical booking expiration vẫn do Booking Service worker quản lý theo `bookings.expires_at`.

## 18.4. Endpoint Consistency Decision

Sprint 2 giữ hai endpoint đã có trong Booking Contract:

```txt
/internal/bookings/{bookingId}/confirm-payment
/internal/bookings/{bookingId}/fail-payment
```

Không dùng endpoint generic `/update-payment-status` trừ khi cả Booking Contract và Payment Contract cùng được cập nhật trong một đợt review.

## 18.5. Booking Confirmation Failure

Nếu payment đã `SUCCESS` nhưng Booking Service tạm thời không khả dụng:

- Payment giữ nguyên `SUCCESS`.
- Ghi payment log.
- Retry internal REST request.
- Không yêu cầu user thanh toán lại.
- Không rollback payment.

Sprint 2 dùng internal REST + retry; Kafka/outbox là hướng mở rộng.

# 19. Payment Log Contract

## 19.1. Log Creation Rule

Mỗi hành động quan trọng phải tạo `payment_logs`:

* Payment created.
* Payment session generated.
* Payment retry.
* Callback received.
* Signature verified.
* Payment success.
* Payment failed.
* Payment cancelled.
* Booking confirmation requested.
* Booking confirmation failed.
* Refund started.
* Refund completed.

### Example Logs

```txt
previous_status: null
current_status: PENDING
log_message: Payment created for booking 1001

previous_status: PENDING
current_status: SUCCESS
log_message: Mock callback received and verified
```

## 19.2. Get Payment Logs

### Endpoint

```http
GET /api/payments/{paymentId}/logs
```

Customer chỉ được xem log rút gọn, không chứa payload hoặc thông tin kỹ thuật nhạy cảm.

### Response Success

```json
{
  "success": true,
  "message": "Payment logs retrieved successfully",
  "data": [
    {
      "logId": 7001,
      "previousStatus": null,
      "currentStatus": "PENDING",
      "message": "Payment created",
      "createdAt": "2026-06-21T20:10:00"
    },
    {
      "logId": 7002,
      "previousStatus": "PENDING",
      "currentStatus": "SUCCESS",
      "message": "Payment completed successfully",
      "createdAt": "2026-06-21T20:12:00"
    }
  ]
}
```

## 19.3. Admin Payment Logs

```http
GET /api/admin/payments/{paymentId}/logs
```

Admin response có thể chi tiết hơn, nhưng không trả secret/signing key/card data.

---

# 20. Admin Payment APIs

## 20.1. Get Payment List

### Endpoint

```http
GET /api/admin/payments
```

### Query Parameters

| Parameter             | Type          | Required | Validation      |
| --------------------- | ------------- | -------: | --------------- |
| page                  | integer       |       No | >= 0            |
| size                  | integer       |       No | 1–50            |
| bookingId             | number        |       No | > 0             |
| transactionCode       | string        |       No | Tối đa 100      |
| externalTransactionId | string        |       No | Tối đa 100      |
| method                | PaymentMethod |       No | Enum hợp lệ     |
| status                | PaymentStatus |       No | Enum hợp lệ     |
| from                  | datetime      |       No | ISO-8601        |
| to                    | datetime      |       No | ISO-8601        |
| sort                  | string        |       No | field,direction |

### Response Success

```json
{
  "success": true,
  "message": "Payments retrieved successfully",
  "data": {
    "content": [
      {
        "paymentId": 3001,
        "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
        "bookingId": 1001,
        "amount": 240000,
        "paymentMethod": "MOCK",
        "externalTransactionId": "MOCK-TXN-3001",
        "status": "SUCCESS",
        "createdAt": "2026-06-21T20:10:00",
        "updatedAt": "2026-06-21T20:12:00"
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

### Error: Invalid Query

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Invalid payment query parameters",
  "errorCode": "PAYMENT_INVALID_QUERY",
  "data": null,
  "errors": null
}
```

---

## 20.2. Get Admin Payment Detail

```http
GET /api/admin/payments/{paymentId}
```

Admin có thể xem:

* Payment detail.
* Raw response payload đã sanitized.
* Payment logs.

Không trả:

* Secret key.
* Signature secret.
* Card number.
* CVV.
* Access token của provider.

---

## 20.3. Confirm Cash Payment

### Endpoint

```http
POST /api/admin/payments/{paymentId}/confirm-cash
```

Chỉ áp dụng:

```txt
paymentMethod = CASH
status = PENDING
```

### Request

```json
{
  "note": "Cash received at counter 01"
}
```

### Response Success

```json
{
  "success": true,
  "message": "Cash payment confirmed successfully",
  "data": {
    "paymentId": 3001,
    "status": "SUCCESS"
  }
}
```

### Error

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Payment is not a pending cash payment",
  "errorCode": "PAYMENT_CASH_CONFIRMATION_NOT_ALLOWED",
  "data": null,
  "errors": null
}
```

---

## 20.4. Mark Payment Failed

### Endpoint

```http
POST /api/admin/payments/{paymentId}/mark-failed
```

### Request

```json
{
  "reason": "Manual reconciliation detected failed transaction"
}
```

Không cho mark failed nếu payment đã:

```txt
SUCCESS
REFUNDED
CANCELLED
```

---

# 21. Refund Direction

Schema có status:

```txt
REFUNDED
```

nhưng chưa có:

```txt
refund_amount
refund_transaction_id
refunded_at
refund_reason
```

Vì vậy Sprint 2:

* Không implement refund API hoàn chỉnh.
* Chỉ giữ `REFUNDED` như hướng mở rộng.
* Không chuyển payment sang `REFUNDED` nếu chưa có provider refund confirmation.
* Cần schema change issue trước khi implement.

Issue đề xuất sau này:

```txt
[Database] Extend Payment Schema for Refund Tracking
```

---

# 22. Raw Response Payload Security

`raw_response_payload` chỉ dùng cho:

* Audit.
* Debug provider callback.
* Reconciliation.

Không được lưu:

```txt
full card number
CVV
OTP
password
provider secret
private key
access token nhạy cảm
```

Trước khi lưu phải:

* Sanitize payload.
* Mask thông tin nhạy cảm.
* Giới hạn kích thước.
* Không trả field này qua Customer API.

Admin API cũng chỉ trả payload đã sanitized nếu thật sự cần.

---

# 23. Concurrency Rules

## 23.1. Create Payment

Hai request đồng thời không được tạo duplicate attempt cho cùng logical request.

Cơ chế:

```txt
Idempotency-Key
+
Redis lock
+
Database transaction
```

Không dùng `UNIQUE(booking_id)` để chống duplicate request vì một booking được phép có nhiều attempts.

## 23.2. Callback

Hai callback đồng thời phải:

- Lock payment record hoặc dùng optimistic locking.
- Kiểm tra `external_transaction_id` unique.
- Chỉ thực hiện một status transition.
- Không tạo duplicate log hoặc duplicate effect sang Booking Service.

## 23.3. Cash Confirmation, Callback và Expiry Worker

Admin confirm CASH, provider callback, customer cancel và expiry worker có thể chạy đồng thời.

Bảng `payments` phải có:

```txt
version INT NOT NULL DEFAULT 0
```

Entity sử dụng:

```java
@Version
```

Optimistic lock conflict trả:

```txt
409 PAYMENT_OPTIMISTIC_LOCK_CONFLICT
```

Không được ghi đè status mới hơn bằng dữ liệu stale.

# 24. Idempotency Rules

## 24.1. Create Payment

Create Payment bắt buộc có:

```http
Idempotency-Key: <UUID>
```

Idempotency identity:

```txt
userId + Idempotency-Key
```

Cùng key và cùng payload:

```txt
Trả kết quả request đầu tiên
Không tạo payment record mới
```

Cùng key nhưng payload khác:

```txt
409 PAYMENT_IDEMPOTENCY_CONFLICT
```

## 24.2. Callback

Callback idempotency dựa trên:

```txt
paymentTransactionCode
externalTransactionId
provider result
```

`external_transaction_id` phải unique khi khác `NULL`.

## 24.3. Booking Notification

Payment Service không được gửi duplicate effect sang Booking Service.

Internal request phải có:

```http
Idempotency-Key: <UUID>
```

Booking Service cũng phải xử lý idempotent.

## 24.4. Payment Attempts

Một booking có nhiều attempts không đồng nghĩa mọi request đều tạo attempt mới.

Attempt mới chỉ được tạo khi:

```txt
Logical request mới
+
Không có payment SUCCESS
+
Attempt trước FAILED/CANCELLED/EXPIRED hoặc không còn active
```

Duplicate HTTP retry của cùng logical request phải trả attempt đã tạo trước đó.

# 25. Retry Rules

Payment Service có thể retry:

- Booking validation lỗi tạm thời.
- Booking notification sau payment success.
- Provider status query khi callback bị thiếu.

Không retry vô hạn.

Đề xuất:

```txt
Maximum attempts: 3
Backoff: 5s, 15s, 30s
```

Nếu vẫn thất bại:

- Ghi payment log.
- Đánh dấu cần manual reconciliation.
- Không đổi `SUCCESS` thành `FAILED`.

Schema chưa có `reconciliation_status`; nếu cần tracking rõ phải có schema change riêng.

# 26. Payment Expiration Worker

Payment Service phải triển khai scheduled worker hoặc background worker trong Sprint 2.

## 26.1. Expiration Query

```txt
status IN (PENDING, PROCESSING)
AND expires_at < now
```

## 26.2. Processing

```txt
PENDING / PROCESSING
→ EXPIRED
→ tạo payment log
→ không xóa payment attempt
→ không reuse payment record
```

## 26.3. Worker Requirements

Worker phải:

- Idempotent.
- Có thể chạy lại an toàn.
- Không thay đổi `SUCCESS`, `FAILED`, `CANCELLED`, `EXPIRED`, `REFUNDED`.
- Dùng optimistic locking qua `version`.
- Không tạo duplicate log/event.
- Có thể xử lý theo batch.
- Ghi log số record thành công và thất bại.

Payment `EXPIRED` được phân biệt với `FAILED`:

```txt
EXPIRED = user không hoàn tất trước timeout
FAILED = provider hoặc nghiệp vụ từ chối attempt
```

# 27. Security Rules

* Customer phải authenticated khi tạo hoặc xem payment.
* Customer chỉ xem payment thuộc booking của mình.
* Admin/Employee cần permission phù hợp.
* Callback phải xác minh signature.
* Internal API không expose qua Gateway.
* Secret/payment key chỉ nằm ở environment variable hoặc secret manager.
* Không commit provider secret vào Git.
* Không trả raw provider secret cho Frontend.
* Không lưu card data.
* Không log OTP hoặc credential.
* Không tin callback amount nếu chưa đối chiếu payment record.
* Không tin booking amount do Frontend gửi.

Permission đề xuất:

```txt
PAYMENT_READ
PAYMENT_MANAGE
PAYMENT_RECONCILE
PAYMENT_CASH_CONFIRM
```

---

# 28. Error Code Catalog

| Error Code                              | HTTP | Ý nghĩa                       |
| --------------------------------------- | ---: | ----------------------------- |
| `PAYMENT_NOT_FOUND`                     |  404 | Không tìm thấy payment        |
| `PAYMENT_TRANSACTION_NOT_FOUND`         |  404 | Không tìm thấy transaction    |
| `PAYMENT_ALREADY_EXISTS`                |  409 | Booking đã có payment         |
| `PAYMENT_ACTIVE_ATTEMPT_EXISTS`          |  409 | Đang có payment attempt active |
| `PAYMENT_IDEMPOTENCY_KEY_REQUIRED`       |  400 | Thiếu Idempotency-Key         |
| `PAYMENT_IDEMPOTENCY_CONFLICT`           |  409 | Key đã dùng với payload khác  |
| `PAYMENT_OPTIMISTIC_LOCK_CONFLICT`       |  409 | Payment bị cập nhật đồng thời |
| `PAYMENT_EXPIRED`                        |  409 | Payment session đã hết hạn    |
| `PAYMENT_ALREADY_SUCCESSFUL`            |  409 | Booking đã thanh toán         |
| `PAYMENT_BOOKING_NOT_FOUND`             |  404 | Không tìm thấy booking        |
| `PAYMENT_BOOKING_NOT_PAYABLE`           |  409 | Booking không được thanh toán |
| `PAYMENT_BOOKING_EXPIRED`               |  409 | Booking hết hạn               |
| `PAYMENT_BOOKING_OWNERSHIP_MISMATCH`    |  403 | Booking không thuộc user      |
| `PAYMENT_INVALID_METHOD`                |  400 | Method không hợp lệ           |
| `PAYMENT_INVALID_STATUS`                |  400 | Status không hợp lệ           |
| `PAYMENT_INVALID_STATUS_TRANSITION`     |  409 | Transition không hợp lệ       |
| `PAYMENT_CANNOT_BE_CANCELLED`           |  409 | Không thể cancel              |
| `PAYMENT_AMOUNT_MISMATCH`               |  409 | Amount không khớp             |
| `PAYMENT_INVALID_SIGNATURE`             |  401 | Callback signature sai        |
| `PAYMENT_CALLBACK_STATUS_CONFLICT`      |  409 | Callback mâu thuẫn trạng thái |
| `PAYMENT_PROVIDER_UNAVAILABLE`          |  503 | Provider không khả dụng       |
| `PAYMENT_SESSION_CREATION_FAILED`       |  502 | Không tạo được session        |
| `PAYMENT_CASH_CONFIRMATION_NOT_ALLOWED` |  409 | Không được confirm cash       |
| `PAYMENT_INVALID_QUERY`                 |  400 | Query không hợp lệ            |
| `BOOKING_SERVICE_UNAVAILABLE`           |  503 | Booking Service lỗi           |
| `VALIDATION_ERROR`                      |  400 | Validation lỗi                |
| `UNAUTHORIZED`                          |  401 | Chưa đăng nhập                |
| `FORBIDDEN`                             |  403 | Không có quyền                |
| `INTERNAL_SERVER_ERROR`                 |  500 | Lỗi hệ thống                  |

---

# 29. Schema Alignment Notes

Các thay đổi sau là bắt buộc trước implementation:

## 29.1. Multiple Payment Attempts

- Drop unique constraint của `booking_id`.
- Thêm non-unique index cho `booking_id`.
- Mỗi retry tạo payment row mới.

## 29.2. External Transaction Uniqueness

Thêm unique constraint cho:

```txt
external_transaction_id
```

## 29.3. Payment Session Fields

Thêm:

```txt
expires_at
payment_url
provider_session_id
```

## 29.4. Optimistic Locking

Thêm:

```txt
version INT NOT NULL DEFAULT 0
```

## 29.5. Status Synchronization

Đồng bộ comment/enum:

```txt
PENDING
PROCESSING
SUCCESS
FAILED
CANCELLED
EXPIRED
REFUNDED
```

## 29.6. Raw Payload Sanitization

Raw payload được giữ nhưng phải sanitize trước khi lưu.

## 29.7. Refund

Refund nằm ngoài Sprint 2 và cần schema mở rộng riêng.

## 29.8. Required Schema Issue

```txt
[Database] Align Payment Schema with Payment API Contract
```

Backend implementation chỉ bắt đầu sau khi Schema Alignment MR được merge.

# 30. Out of Scope

* Production VNPay integration nếu chưa có account/config.
* Production MoMo integration.
* Lưu card data.
* Full refund flow.
* Partial refund.
* Chargeback.
* Settlement automation.
* Multi-currency.
* Refund implementation đầy đủ.
* Payment dispute.
* PCI DSS card processing.
* Advanced reconciliation dashboard.
* Kafka/outbox production implementation.
* Payment provider secret management production.
* Automatic retry không giới hạn.
* Direct modification Booking database.

---

# 31. Implementation Issue Direction

Implementation chỉ bắt đầu sau khi:

```txt
Payment Contract MR được merge
+
Payment Schema Alignment MR được merge
+
SQL, ERD và entity đã đồng bộ
```

Các implementation issue đề xuất:

```txt
[Backend] Implement Payment Core and Multiple Attempt APIs

[Backend] Implement Mock Payment Session and Callback Flow

[Backend] Implement CASH Payment and Customer Cancel Flow

[Backend] Implement Payment Expiration Worker

[Backend] Implement Booking Notification and Retry Flow

[Backend] Implement Payment Logs and Admin Query APIs
```

Thứ tự đề xuất:

```txt
Schema Alignment
→ Payment Core + Idempotency
→ Mock/CASH Session
→ Callback + Signature Validation
→ Booking Notification
→ Expiration Worker
→ Logs/Admin Query
```

Refund và provider production integration nằm ngoài Sprint 2.

# 32. Acceptance Criteria

Contract hoàn thành khi:

* [ ] Có Physical Schema baseline.
* [ ] Có schema limitation notes.
* [ ] Có endpoint summary.
* [ ] Có Protected/Admin/Internal/Callback classification.
* [ ] Có request headers.
* [ ] Có request/response mẫu.
* [ ] Có field definitions.
* [ ] Có payment method rules.
* [ ] Có payment status lifecycle.
* [ ] Có allowed status transitions.
* [ ] Có amount source-of-truth rule.
* [ ] Có callback/webhook direction.
* [ ] Có signature validation direction.
* [ ] Có callback idempotency.
* [ ] Có create payment idempotency.
* [ ] Có concurrency rules.
* [ ] Có payment logs behavior.
* [ ] Có Booking Service integration direction.
* [ ] Có security notes.
* [ ] Có raw payload security rule.
* [ ] Có retry behavior.
* [ ] Có error code catalog.
* [ ] Có Out of Scope.
* [ ] Vinh xác nhận feasibility.
* [ ] Schema bắt buộc đã được align trước implementation.
* [ ] Tài liệu đủ rõ để tách implementation issues.
* [ ] MR target `develop`.

---

# 33. Review Decisions

Payment Service Owner đã review và xác nhận:

1. Payment status chính thức:

   ```txt
   PENDING
   PROCESSING
   SUCCESS
   FAILED
   CANCELLED
   EXPIRED
   REFUNDED
   ```

2. Một booking có nhiều payment attempts.

3. Không reuse hoặc reset payment record khi retry.

4. Retry tạo payment record và `paymentTransactionCode` mới.

5. Drop unique constraint của `booking_id`.

6. Thêm unique constraint cho `external_transaction_id` khi khác `NULL`.

7. Payment session timeout là `15 phút`.

8. Persist:

   ```txt
   expires_at
   payment_url
   provider_session_id
   ```

9. Thêm `version` và dùng Optimistic Locking.

10. Sprint 2 triển khai:

    ```txt
    MOCK
    CASH
    Customer Cancel
    ```

11. Refund hoàn toàn Out of Scope Sprint 2.

12. VNPay/MoMo Sandbox chỉ thực hiện nếu còn thời gian.

13. Payment success thông báo Booking Service bằng Internal REST + retry.

14. Sprint 2 giữ hai Booking endpoint:

    ```txt
    /internal/bookings/{bookingId}/confirm-payment
    /internal/bookings/{bookingId}/fail-payment
    ```

15. Callback thật phải xác minh signature HMAC/RSA theo provider.

16. Payment Service port `8084` không được expose trực tiếp ngoài Gateway/network nội bộ.

17. Raw provider payload được lưu sau khi sanitize.

18. Provider webhook response phải đúng provider specification, không dùng Common Response DTO.

19. Payment expiration worker phải được triển khai.

20. Schema Alignment MR phải merge trước Backend implementation.

---

# 34. Lịch Sử Chỉnh Sửa

| Ngày | Nội dung | Người thực hiện |
|---|---|---|
| 21/06/2026 | Khởi tạo Payment Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật contract theo review của Payment Service Owner: multiple attempts, idempotency, EXPIRED, persisted session, webhook response, optimistic locking và expiration worker | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận là đã hoàn tất sau khi Schema Alignment MR tương ứng được merge.
