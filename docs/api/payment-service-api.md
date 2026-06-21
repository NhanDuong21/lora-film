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
| Ngày cập nhật  | 21/06/2026                                                                     |

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

## 5. Phân Tích Schema Hiện Tại

### 5.1. Những nghiệp vụ schema hiện tại hỗ trợ

Schema hiện tại hỗ trợ:

* Một payment record cho một booking.
* Sinh mã giao dịch nội bộ.
* Lưu amount snapshot.
* Lưu payment method.
* Lưu external transaction ID.
* Lưu trạng thái payment.
* Lưu provider response.
* Lưu lịch sử chuyển trạng thái.

### 5.2. Giới hạn hiện tại

Schema hiện tại chưa có:

```txt
provider
payment_url
payment_session_id
expired_at
paid_at
failed_at
cancelled_at
refunded_at
refund_amount
failure_code
failure_reason
callback_received_at
signature_verified
currency
created_by
```

### 5.3. Điểm cần reviewer xác nhận

`booking_id` hiện đang là:

```txt
UNIQUE NOT NULL
```

Điều này có nghĩa:

```txt
Một booking chỉ có tối đa một payment record.
```

Nếu payment thất bại và user thử lại:

```txt
Không tạo payment row mới.
Payment Service cập nhật lại record hiện có và ghi thêm payment_logs.
```

Nếu team muốn:

```txt
Một booking có nhiều payment attempts riêng biệt
```

thì phải:

* Gỡ unique khỏi `payments.booking_id`.
* Thêm `attempt_number` hoặc thiết kế payment attempt table.
* Tạo schema alignment issue riêng.
* Cập nhật contract trước implementation.

Trong Sprint 2, contract này mặc định sử dụng:

```txt
Một booking = một payment record
Nhiều lần thử = nhiều payment log trên cùng payment
```

---

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

### 8.3. Internal API Header

```http
X-Internal-Token: <internal-token>
Content-Type: application/json
```

Cơ chế internal token có thể thay đổi theo security design chính thức.

### 8.4. Callback/Webhook Header

Tùy provider, ví dụ:

```http
X-Signature: <provider-signature>
Content-Type: application/json
```

Provider callback không dùng JWT của user.

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

Amount trả dưới dạng number:

```json
{
  "amount": 240000
}
```

Không gửi chuỗi:

```json
{
  "amount": "240.000 VND"
}
```

---

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

### 11.1. PaymentStatus đề xuất

```txt
PENDING
PROCESSING
SUCCESS
FAILED
CANCELLED
REFUNDED
```

Schema comment hiện chỉ có:

```txt
PENDING
SUCCESS
FAILED
REFUNDED
```

`PROCESSING` và `CANCELLED` là status đề xuất cần Vinh review.

Nếu không muốn đổi schema comment, Sprint 2 có thể dùng tối thiểu:

```txt
PENDING
SUCCESS
FAILED
REFUNDED
```

### 11.2. Allowed Transitions

| Current    | Allowed Next                           |
| ---------- | -------------------------------------- |
| PENDING    | PROCESSING, SUCCESS, FAILED, CANCELLED |
| PROCESSING | SUCCESS, FAILED, CANCELLED             |
| FAILED     | PENDING, PROCESSING                    |
| SUCCESS    | REFUNDED                               |
| CANCELLED  | Không có                               |
| REFUNDED   | Không có                               |

### 11.3. Transition Rules

Không cho:

```txt
SUCCESS → PENDING
SUCCESS → FAILED
REFUNDED → SUCCESS
CANCELLED → SUCCESS
```

Callback lặp lại với cùng trạng thái `SUCCESS` phải được xử lý idempotent.

---

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
```

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
```

### Field Definitions

| Field         | Type   | Required | Validation         |
| ------------- | ------ | -------: | ------------------ |
| bookingId     | number |      Yes | > 0                |
| paymentMethod | string |      Yes | PaymentMethod enum |

### Processing Flow

```txt
Resolve authenticated user
→ Validate booking exists
→ Validate booking belongs to current user
→ Validate booking status = PENDING_PAYMENT
→ Validate booking is not expired
→ Get totalAmount from Booking Service
→ Check existing payment for bookingId
→ Generate paymentTransactionCode
→ Create payment with PENDING
→ Create initial payment log
→ Create mock/sandbox session if required
→ Return payment response
```

### Amount Source of Truth

Payment Service lấy amount từ Booking Service:

```txt
booking.totalAmount
```

Không tin:

```txt
amount do Frontend gửi
```

### Internal Transaction Code

Format đề xuất:

```txt
PAY-LORAFILM-<timestamp-or-random>
```

Ví dụ:

```txt
PAY-LORAFILM-20260621-998877
```

Mã phải unique.

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Payment created successfully",
  "data": {
    "paymentId": 3001,
    "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
    "bookingId": 1001,
    "amount": 240000,
    "currency": "VND",
    "paymentMethod": "MOCK",
    "status": "PENDING",
    "paymentSession": {
      "paymentUrl": "http://localhost:8080/api/payments/mock/3001",
      "expiresAt": "2026-06-21T20:20:00"
    },
    "createdAt": "2026-06-21T20:10:00"
  }
}
```

`paymentUrl` và `expiresAt` là response-derived fields, chưa được lưu trong schema hiện tại.

### Existing Payment Behavior

Vì `booking_id` đang unique:

Nếu booking đã có payment `PENDING`, API trả payment hiện tại:

```txt
200 OK
```

Không tạo row mới.

Nếu payment hiện tại `FAILED`, service có thể:

* Đặt lại status thành `PENDING`.
* Cập nhật payment method.
* Tạo log retry.
* Sinh payment session mới.

Nếu payment đã `SUCCESS`, không tạo hoặc retry payment.

### Error: Booking Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Booking not found",
  "errorCode": "PAYMENT_BOOKING_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Error: Booking Ownership Mismatch

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "You cannot create payment for this booking",
  "errorCode": "PAYMENT_BOOKING_OWNERSHIP_MISMATCH",
  "data": null,
  "errors": null
}
```

### Error: Booking Not Payable

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking is not available for payment",
  "errorCode": "PAYMENT_BOOKING_NOT_PAYABLE",
  "data": null,
  "errors": null
}
```

### Error: Booking Expired

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking payment period has expired",
  "errorCode": "PAYMENT_BOOKING_EXPIRED",
  "data": null,
  "errors": null
}
```

### Error: Payment Already Successful

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking has already been paid successfully",
  "errorCode": "PAYMENT_ALREADY_SUCCESSFUL",
  "data": null,
  "errors": null
}
```

### Error: Booking Service Unavailable

Status: `503 Service Unavailable`

```json
{
  "success": false,
  "message": "Booking information is temporarily unavailable",
  "errorCode": "BOOKING_SERVICE_UNAVAILABLE",
  "data": null,
  "errors": null
}
```

Không tạo payment khi chưa xác minh được amount hợp lệ.

---

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

## 14.3. Get Payment by Booking

### Endpoint

```http
GET /api/payments/booking/{bookingId}
```

### Response Success

Response giống Payment Detail.

### Response Error

* `404 PAYMENT_NOT_FOUND`
* `403 FORBIDDEN`

Do `booking_id` đang unique, response chỉ trả một payment object, không trả danh sách.

Nếu schema sau này hỗ trợ nhiều attempt, endpoint này phải đổi thành list hoặc có endpoint `/attempts`.

---

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

Hoặc theo provider requirement có thể dùng:

```http
GET /api/payments/callback/vnpay
```

Endpoint cuối cùng phải đồng bộ với tài liệu VNPay khi tích hợp thật.

## 16.2. MoMo Callback

```http
POST /api/payments/callback/momo
```

## 16.3. Callback Rules

Callback phải:

* Xác minh signature.
* Xác minh transaction code.
* Xác minh amount.
* Xác minh external transaction ID.
* Kiểm tra payment tồn tại.
* Kiểm tra callback không bị xử lý trùng.
* Không tin status chỉ dựa trên request body.
* Lưu log kết quả xác minh.
* Không expose secret key trong log hoặc response.

## 16.4. Invalid Signature

Status: `401 Unauthorized`

```json
{
  "success": false,
  "message": "Invalid payment callback signature",
  "errorCode": "PAYMENT_INVALID_SIGNATURE",
  "data": null,
  "errors": null
}
```

## 16.5. Amount Mismatch

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Callback amount does not match payment amount",
  "errorCode": "PAYMENT_AMOUNT_MISMATCH",
  "data": null,
  "errors": null
}
```

## 16.6. Transaction Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Payment transaction not found",
  "errorCode": "PAYMENT_TRANSACTION_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

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

Request giả định:

```json
{
  "paymentId": 3001,
  "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
  "paidAmount": 240000,
  "paidAt": "2026-06-21T20:12:00"
}
```

### Expected Booking Response

```json
{
  "success": true,
  "message": "Booking confirmed successfully",
  "data": {
    "bookingId": 1001,
    "status": "CONFIRMED"
  }
}
```

## 18.2. Payment Failed

Payment failure không bắt buộc hủy Booking ngay.

Flow Sprint 2:

```txt
Payment FAILED
→ Booking vẫn PENDING_PAYMENT
→ user có thể retry payment nếu booking chưa hết hạn
```

## 18.3. Booking Confirmation Failure

Tình huống:

```txt
Payment đã SUCCESS
nhưng Booking Service tạm thời không khả dụng
```

Payment không được rollback từ `SUCCESS` về `FAILED`.

Payment Service phải:

* Giữ `SUCCESS`.
* Ghi log booking confirmation failed.
* Retry thông báo Booking Service.
* Không yêu cầu user thanh toán lại.

Error log ví dụ:

```txt
Payment succeeded but booking confirmation is pending retry.
```

Trong hệ thống production, nên dùng event/outbox; Sprint 2 có thể dùng internal REST + retry foundation.

---

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

Hai request đồng thời cho cùng `bookingId`:

```txt
Chỉ được tạo một payment record.
```

Dựa trên:

```txt
UNIQUE payments.booking_id
```

Request còn lại:

* Trả payment hiện có.
* Hoặc trả `409 PAYMENT_ALREADY_EXISTS`.

Contract Sprint 2 ưu tiên trả payment hiện có nếu trạng thái còn có thể sử dụng.

## 23.2. Callback

Hai callback đồng thời:

* Phải lock payment record hoặc dùng optimistic locking.
* Chỉ một transition được thực hiện.
* Không tạo duplicate log/event ngoài ý muốn.

## 23.3. Cash Confirmation và Callback

Nếu cash confirm và provider callback xảy ra trên cùng payment:

* Payment method validation phải ngăn flow không hợp lệ.
* Không được xác nhận payment hai lần.

---

# 24. Idempotency Rules

### Create Payment

Idempotency nghiệp vụ dựa trên:

```txt
bookingId
```

Do `booking_id UNIQUE`.

### Callback

Idempotency dựa trên:

```txt
paymentTransactionCode
externalTransactionId
callback result
```

### Booking Confirmation

Payment Service không được gửi duplicate effect sang Booking Service.

Booking confirmation request phải mang:

```txt
paymentId
paymentTransactionCode
```

Booking Service cũng phải xử lý idempotent.

---

# 25. Retry Rules

Payment Service có thể retry:

* Booking validation tạm thời lỗi.
* Booking confirmation sau payment success.
* Provider query status nếu callback bị thiếu.

Không retry vô hạn.

Đề xuất:

```txt
Maximum attempts: 3
Backoff: 5s, 15s, 30s
```

Nếu vẫn thất bại:

* Ghi payment log.
* Đánh dấu cần manual reconciliation.
* Không đổi `SUCCESS` thành `FAILED`.

Schema hiện chưa có `reconciliation_status`; nếu cần tracking rõ phải schema change.

---

# 26. Security Rules

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

# 27. Error Code Catalog

| Error Code                              | HTTP | Ý nghĩa                       |
| --------------------------------------- | ---: | ----------------------------- |
| `PAYMENT_NOT_FOUND`                     |  404 | Không tìm thấy payment        |
| `PAYMENT_TRANSACTION_NOT_FOUND`         |  404 | Không tìm thấy transaction    |
| `PAYMENT_ALREADY_EXISTS`                |  409 | Booking đã có payment         |
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

# 28. Schema Alignment Notes

Các điểm contract có thể mismatch với schema Sprint 0:

## 28.1. Payment statuses

Contract đề xuất thêm:

```txt
PROCESSING
CANCELLED
```

Schema comment hiện chưa liệt kê hai trạng thái này.

## 28.2. External transaction uniqueness

Contract yêu cầu:

```txt
external_transaction_id unique khi không null
```

Schema hiện chưa có unique constraint.

## 28.3. Payment expiry

Contract trả:

```txt
paymentSession.expiresAt
```

nhưng schema chưa có `expires_at`.

Có thể dùng derived value trong Sprint 2 hoặc thêm column nếu cần persist.

## 28.4. Retry attempts

Schema chỉ cho một payment record mỗi booking.

Nếu muốn nhiều payment attempts riêng:

```txt
Phải refactor booking_id UNIQUE.
```

## 28.5. Refund

Schema chưa đủ field theo dõi refund chi tiết.

## 28.6. Reconciliation

Schema chưa có trạng thái đối soát/manual review.

## 28.7. Raw payload

Cần xác định sanitization và maximum length.

### Required Review Result

Reviewer phải phân loại từng mismatch:

```txt
Có thể xử lý bằng derived field
Out of Scope Sprint 2
Bắt buộc refactor schema trước implementation
```

Nếu có mục bắt buộc, tạo issue:

```txt
[Database] Align Payment Schema with Payment API Contract
```

---

# 29. Out of Scope

* Production VNPay integration nếu chưa có account/config.
* Production MoMo integration.
* Lưu card data.
* Full refund flow.
* Partial refund.
* Chargeback.
* Settlement automation.
* Multi-currency.
* Multiple payment attempts table.
* Payment dispute.
* PCI DSS card processing.
* Advanced reconciliation dashboard.
* Kafka/outbox production implementation.
* Payment provider secret management production.
* Automatic retry không giới hạn.
* Direct modification Booking database.

---

# 30. Implementation Issue Direction

Sau khi contract được review và schema alignment hoàn thành nếu cần, có thể tách:

```txt
[Backend] Implement Payment Core and Query APIs

[Backend] Implement Mock Payment Session and Callback Flow

[Backend] Implement Payment Logs and Admin Management APIs

[Backend] Implement Booking Confirmation and Payment Retry Flow
```

Nếu giảm scope Sprint 2:

```txt
Issue 1: Create/Get Payment
Issue 2: Mock Success/Failed Callback
Issue 3: Payment Log and Booking Confirmation
```

Mọi thay đổi endpoint, request, response hoặc business rule phải cập nhật contract trong cùng MR.

---

# 31. Acceptance Criteria

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

# 32. Các Điểm Reviewer Cần Xác Nhận

Vinh cần xác nhận:

1. Payment Service port chính thức.
2. Sprint 2 sử dụng `MOCK`, `CASH` hay provider sandbox nào.
3. Có thêm status `PROCESSING` không.
4. Có thêm status `CANCELLED` không.
5. Một booking có một payment record hay nhiều attempts.
6. Có giữ `booking_id UNIQUE` không.
7. Retry failed payment có reuse record hiện tại không.
8. `external_transaction_id` có cần unique constraint không.
9. Có cần lưu `expires_at` không.
10. Payment URL/session có cần persist không.
11. Payment success thông báo Booking bằng REST hay Kafka.
12. Có dùng `/internal/bookings/{id}/confirm-payment` đúng như Booking Contract không.
13. Cách bảo vệ callback endpoint.
14. Cách bảo vệ internal endpoint.
15. Có lưu raw provider payload không.
16. Raw payload cần sanitize field nào.
17. Có implement CASH trong Sprint 2 không.
18. Có cho customer cancel payment không.
19. Refund có hoàn toàn Out of Scope không.
20. Có cần schema change trước implementation không.
