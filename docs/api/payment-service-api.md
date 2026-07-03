# LoraFilm Payment Service
# REST API & Internal Integration Contract

## 1. Document Control
| Field | Value |
|---|---|
| Project | LoraFilm |
| Service | payment-service |
| Document Type | API Contract |
| Implementation Owner | Booking Service Owner / Payment Service Owner |
| Contract Requester/Reviewer | Dương Thiện Nhân — Payment Service Owner |
| Related Issue | #156 |
| Related Roadmap | #155 |
| Reviewed Branch | docs/payment-architecture-contracts |
| Reviewed Commit | 5878fd088a935405da303ff28a7329fa796c54c1 |
| Release | Product Release 1 |
| Status | PROPOSED — READY FOR REVIEW |
| Last Updated | 2026-07-03 |
| Language | Vietnamese |
| Source of Truth | latest develop source + approved contracts |

## 2. Table of Contents
- [1. Document Control](#1-document-control)
- [2. Table of Contents](#2-table-of-contents)
- [3. Purpose](#3-purpose)
- [4. Scope](#4-scope)
- [5. Actors and roles](#5-actors-and-roles)
- [6. Common conventions](#6-common-conventions)
- [7. Response wrapper](#7-response-wrapper)
- [8. Authentication](#8-authentication)
- [9. Idempotency](#9-idempotency)
- [10. Error conventions](#10-error-conventions)
- [11. Customer Endpoints](#11-customer-endpoints)
- [12. Employee CASH Endpoints](#12-employee-cash-endpoints)
- [13. Provider Callback Contract](#13-provider-callback-contract)
- [14. Booking Internal Contract](#14-booking-internal-contract)
- [15. Operations Contract](#15-operations-contract)
- [16. Error Catalog](#16-error-catalog)
- [17. Security Matrix](#17-security-matrix)
- [18. Requirement Traceability](#18-requirement-traceability)
- [19. Deferred Endpoints](#19-deferred-endpoints)

## 3. Purpose
Payment Service cung cấp khả năng xử lý, ghi nhận và xác minh các giao dịch thanh toán. Không quản lý ghế, huỷ vé, hoặc tự xác định số tiền. Hỗ trợ cơ chế đa attempt nhưng chỉ 1 attempt active, và đảm bảo mọi xác thực cuối cùng thông qua Booking Service bằng Transactional Outbox. Việc khách hàng quay về (Browser return) từ cổng thanh toán là Non-authoritative (Không có tính quyết định).

## 4. Scope
- MOCK (Dev/Test only, Disabled in Production)
- CASH (Thu tại quầy)
- VNPay Sandbox
- MoMo Sandbox
- Persistent idempotency & Payment guard
- Webhook inbox & Transactional outbox
- Booking result delivery & Analytics event
- Customer checkout & Employee counter flow
- Operations and reconciliation

## 5. Actors and roles
- `CUSTOMER`: Khách hàng khởi tạo thanh toán trực tuyến.
- `EMPLOYEE` / `ADMIN`: Quản lý giao dịch tại quầy (CASH), tra cứu đối soát.
- `Booking Service`: Service sở hữu thông tin giỏ hàng và nhận thông điệp báo vé.
- `Analytics Service`: Service tiêu thụ Kafka Event.
- `Provider`: Đối tác cung cấp dịch vụ thanh toán.

## 6. Common conventions
- Base URL qua Gateway: `/api/payments`.
- Định dạng DateTime: Chuẩn ISO-8601 UTC (VD: `2026-07-03T10:00:00Z`).
- Số tiền: Kiểu `Decimal` / `BigDecimal` (VND).
- Pagination: Dùng `page`, `size` cho kết quả danh sách.
- Ownership: Snapshot `accountId` từ lúc tạo attempt.
- **Browser Return**: Quá trình chuyển hướng trình duyệt từ Provider quay về Frontend là Non-authoritative. Frontend bắt buộc gọi `GET /api/payments/{paymentId}/status` để kiểm tra tiến trình. Không thực hiện xử lý nghiệp vụ hay thay đổi trạng thái phụ thuộc vào tham số URL Browser Return (trừ khi tài liệu chính hãng Provider ép buộc phải dùng Web Backend Return Endpoint).

## 7. Response wrapper
Chuẩn `ApiResponse` của LoraFilm dành cho REST API thông thường:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "errorCode": null,
  "data": {},
  "errors": null
}
```
*(Lưu ý: Không sử dụng `ApiResponse` để phản hồi lại Webhook của Provider).*

## 8. Authentication
- Xác thực User thông qua JWT Token lấy `userId`.
- Xác thực Internal Backend qua Header: `X-Internal-Token: <internal-token>`.

## 9. Idempotency
- Áp dụng trên toàn bộ lệnh POST thay đổi trạng thái.
- Header bắt buộc: `Idempotency-Key` (UUID).
- Dữ liệu idempotency được khóa trên bảng `payment_idempotency_records`.
- Thuật toán Hash: **Canonical SHA-256** (Dựa trên operation, accountId, bookingId và normalized request body, không bao gồm khoảng trắng rỗng hoặc headers bí mật).
- **Quy trình xử lý lỗi tại Idempotency Phase**:
  - *Booking business rejection (Ví dụ hết hạn)*: Cập nhật Idempotency Record thành `FAILED` và lưu trữ cố định cấu trúc báo lỗi (deterministic error response).
  - *Temporary Booking/infrastructure failure (Lỗi hạ tầng tạm thời)*: Cập nhật thành `FAILED` kèm phân loại lỗi Retryable (deterministic stale recovery).
  - *Provider session creation failure*: Không được xóa Idempotency Record. Đánh dấu Idempotency Record là lỗi. Đánh dấu Payment `FAILED`, xóa cờ `active_payment_id` tại Guard nếu nó trỏ đúng vào ID lỗi này. Lưu response báo lỗi cố định, ghi `PaymentLog`.

## 10. Error conventions
- **400 Bad Request**: Lỗi JSON, tham số sai/thiếu, sai UUID.
- **401 Unauthorized**: JWT thiếu/lỗi, X-Internal-Token lỗi.
- **403 Forbidden**: Sai Role hoặc vi phạm Access Control.
- **404 Not Found**: Dữ liệu không tồn tại.
- **409 Conflict**: Logic nghiệp vụ bị chặn (`PAYMENT_RETRY_TEMPORARILY_BLOCKED`, `IDEMPOTENCY_KEY_REUSED`, v.v.).
- **502 Bad Gateway**: Gọi Provider bị lỗi.
- **503 Service Unavailable**: Service interruption.

---
## 11. Customer Endpoints

### 11.1. Customer Create Online/MOCK Payment
| Item | Specification |
|---|---|
| Requirement ID | API-001 |
| Purpose | Khởi tạo Attempt thanh toán Online hoặc MOCK |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #159 |
| Method | POST |
| Path | `/api/payments` |
| Access | CUSTOMER |
| Headers | `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>` |
| Path parameters | None |
| Query parameters | None |
| Request body | `{"bookingId": 1001, "paymentMethod": "VNPAY"}` |
| Idempotency | Canonical SHA-256 |
| State Transition | NULL -> PENDING -> PROCESSING (Online) |

**Field Definition Table**:
| Field | Data Type | Required | Description |
|---|---|---|---|
| `bookingId` | Number | Yes | ID Booking cần thanh toán |
| `paymentMethod` | String | Yes | `MOCK`, `VNPAY`, `MOMO` |

**Validation**: `paymentMethod` không được là `CASH`. `MOCK` chỉ khả dụng trên Non-production.
**Preconditions**: Token hợp lệ, Booking ở trạng thái thanh toán khả dụng.
**Processing flow**:
1. (Phase A) DB Txn: Validate Token. Load/Create Idempotency Record (Validate Hash Canonical SHA-256). Đặt `PROCESSING`. Commit Txn.
2. (Phase B) Call Network: Gọi `GET /internal/bookings/{bookingId}/payment-context`.
3. (Phase C) DB Txn: Lock `booking_payment_guards`. Kiểm tra cờ Active và Settlement Hold. Cấp `next_attempt_number`. Insert `payments` (`PENDING`). Insert Snapshot & PaymentLog. Update Guard. Commit Txn.
4. (Phase D) Call Network: Khởi tạo Session URL với Provider.
5. (Phase E) DB Txn: Nếu có URL, đổi trạng thái PENDING -> PROCESSING, lưu Provider Order ID, set `settlement_hold_until`. Cập nhật Idempotency thành COMPLETED. Trả URL. Commit Txn.
**Database effects**: Ghi Guard, Payments, PaymentLogs, Snapshots, IdempotencyRecords.
**Concurrency**: Khóa ROW Guard.
**Integration**: Booking Service, VNPAY/MOMO.
**Success response**: `201 Created` (Trả về paymentId, paymentMethod, paymentUrl, expiresAt).
**Error responses**: `400 VALIDATION_ERROR`, `403 FORBIDDEN`, `409 PAYMENT_ACTIVE_ATTEMPT_EXISTS`, `409 PAYMENT_RETRY_TEMPORARILY_BLOCKED`, `502 PAYMENT_SESSION_CREATION_FAILED`.
**Security**: Không lấy Amount từ Frontend.
**Audit**: MDC Trace ID, PaymentLog.
**Verification notes**: Tham số truyền sang hãng `REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION`.

---
### 11.2. Customer Get Payment
| Item | Specification |
|---|---|
| Requirement ID | API-002 |
| Purpose | Xem chi tiết 1 Attempt của cá nhân |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #159 |
| Method | GET |
| Path | `/api/payments/{paymentId}` |
| Access | CUSTOMER |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | `paymentId` (Number) |
| Query parameters | None |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: JWT `userId` == `account_id`.
**Preconditions**: Tồn tại paymentId.
**Processing flow**: Truy vấn Payment, đối chiếu quyền, trả kết quả.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK` (Trả status, amount, currency, paymentMethod, createdAt).
**Error responses**: `403 FORBIDDEN`, `404 PAYMENT_NOT_FOUND`.
**Security**: Lọc sạch (Sanitized metadata), tuyệt đối không trả RAW Provider Payload.
**Audit**: N/A.

---
### 11.3. Customer Get Status
| Item | Specification |
|---|---|
| Requirement ID | API-003 |
| Purpose | Frontend tra cứu nhanh trạng thái (Dùng cho Browser Return) |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #159 |
| Method | GET |
| Path | `/api/payments/{paymentId}/status` |
| Access | CUSTOMER |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | `paymentId` (Number) |
| Query parameters | None |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: JWT `userId` == `account_id`.
**Processing flow**: Truy vấn status và reconciliationStatus.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK` (Trả status, reconciliationStatus).
**Error responses**: `403 FORBIDDEN`, `404 PAYMENT_NOT_FOUND`.
**Security**: Read-only an toàn.
**Audit**: N/A.

---
### 11.4. Customer Get History By Booking
| Item | Specification |
|---|---|
| Requirement ID | API-004 |
| Purpose | Liệt kê danh sách attempts theo Booking |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #159 |
| Method | GET |
| Path | `/api/payments/booking/{bookingId}` |
| Access | CUSTOMER |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | `bookingId` (Number) |
| Query parameters | `page`, `size` |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: Xác nhận quyền sở hữu của `userId` đối với các attempt.
**Processing flow**: Truy vấn danh sách phân trang.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK` (Phân trang metadata và list).
**Error responses**: `403 FORBIDDEN`.
**Security**: Phân quyền data.
**Audit**: N/A.

---
### 11.5. Customer Cancel Payment
| Item | Specification |
|---|---|
| Requirement ID | API-005 |
| Purpose | Khách hàng chủ động hủy lệnh (Chỉ khả dụng ở PENDING) |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #159 |
| Method | POST |
| Path | `/api/payments/{paymentId}/cancel` |
| Access | CUSTOMER |
| Headers | `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>` |
| Path parameters | `paymentId` (Number) |
| Query parameters | None |
| Request body | None |
| Idempotency | Canonical SHA-256 |
| State Transition | PENDING -> CANCELLED |

**Validation**: Payment thuộc sở hữu, trạng thái phải đang `PENDING`.
**Preconditions**: Chưa gọi thành công mạng tạo Session với Provider.
**Processing flow**: Verify Idempotency. Lock Guard và Payment. Đổi status `CANCELLED`. Ghi log. Cập nhật Idempotency.
**Database effects**: Cập nhật Payment, Guard, PaymentLog, Idempotency.
**Concurrency**: Lock ROW.
**Integration**: None.
**Success response**: `200 OK`.
**Error responses**: `409 PAYMENT_CANNOT_BE_CANCELLED`.
**Security**: Chỉ khách hàng sở hữu mới được hủy.
**Audit**: PaymentLog.

---
## 12. Employee CASH Endpoints

### 12.1. Employee Create CASH Attempt
| Item | Specification |
|---|---|
| Requirement ID | API-020 |
| Purpose | Khởi tạo Attempt tiền mặt (Chưa thu tiền) |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #160 |
| Method | POST |
| Path | `/api/employee/payments/cash` |
| Access | EMPLOYEE, ADMIN |
| Headers | `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>` |
| Path parameters | None |
| Query parameters | None |
| Request body | `{"bookingId": 1001}` |
| Idempotency | Canonical SHA-256 |
| State Transition | NULL -> PENDING |

**Field definition table**: `bookingId` (Number, Bắt buộc).
**Validation**: Role hợp lệ.
**Preconditions**: Booking khả dụng.
**Processing flow**: Reserved Idempotency -> GET Context Booking -> Lock Guard -> Insert Payment `PENDING`.
**Database effects**: Insert Payment, Guard, Snapshot, Log. Không thiết lập Settlement Hold cho CASH.
**Concurrency**: Lock Guard.
**Integration**: Booking.
**Success response**: `201 Created` (Trả paymentId, status).
**Error responses**: `409 PAYMENT_ACTIVE_ATTEMPT_EXISTS`.
**Security**: Phân quyền EMPLOYEE.
**Audit**: Ghi log tạo.

---
### 12.2. Employee Collect CASH
| Item | Specification |
|---|---|
| Requirement ID | API-021 |
| Purpose | Ghi nhận thu tiền mặt và kết thúc giao dịch SUCCESS |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #160 |
| Method | POST |
| Path | `/api/employee/payments/{paymentId}/cash/collect` |
| Access | EMPLOYEE, ADMIN |
| Headers | `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>` |
| Path parameters | `paymentId` (Number) |
| Query parameters | None |
| Request body | `{"receivedAmount": 200000.00, "counterCode": "COUNTER-01"}` |
| Idempotency | Canonical SHA-256 |
| State Transition | PENDING -> SUCCESS |

**Field definition table**: `receivedAmount` (Decimal), `counterCode` (String).
**Validation**: `receivedAmount` >= `amount`. Phương thức phải là `CASH`. Trạng thái phải `PENDING`.
**Preconditions**: Giao dịch CASH đang tồn tại.
**Processing flow**: Lock Payment. Tính tiền thối `changeAmount`. Cập nhật trạng thái. Insert `cash_payment_details`. Insert Outbox Event. Update Guard `successful_payment_id`.
**Database effects**: Update Payment, Insert Outbox, Insert CashDetails.
**Concurrency**: Lock Payment.
**Integration**: Worker Outbox.
**Success response**: `200 OK` (Trả lại receivedAmount, changeAmount).
**Error responses**: `409 CASH_AMOUNT_INSUFFICIENT`, `409 CASH_ALREADY_COLLECTED`.
**Security**: Audit log thu ngân bắt buộc.
**Audit**: Ghi log người thao tác thu tiền.

---
### 12.3. Employee Cancel CASH
| Item | Specification |
|---|---|
| Requirement ID | API-022 |
| Purpose | Hủy CASH attempt chưa thu tiền |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #160 |
| Method | POST |
| Path | `/api/employee/payments/{paymentId}/cash/cancel` |
| Access | EMPLOYEE, ADMIN |
| Headers | `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>` |
| Path parameters | `paymentId` (Number) |
| Query parameters | None |
| Request body | None |
| Idempotency | Canonical SHA-256 |
| State Transition | PENDING -> CANCELLED |

**Validation**: Là giao dịch `CASH`, đang `PENDING`.
**Processing flow**: Đổi trạng thái, giải tỏa Guard.
**Success response**: `200 OK`.

---
## 13. Provider Callback Contract
*(Lưu ý: Không dùng `ApiResponse` để phản hồi Provider).*

### 13.1. VNPay IPN Webhook
| Item | Specification |
|---|---|
| Requirement ID | INT-001 |
| Purpose | Tiếp nhận Webhook VNPay IPN |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #162 |
| Method | REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION |
| Path | `/api/payments/callback/vnpay` |
| Access | PUBLIC |
| Headers | Provider-defined |
| Path parameters | None |
| Query parameters | Provider-defined |
| Request body | Provider-defined |
| Idempotency | Webhook Inbox |
| State Transition | PROCESSING -> SUCCESS / FAILED; Exceptional Late Success |

**Field definitions**: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
**Validation**: Rate limit, Sanitize.
**Preconditions**: Request hợp lệ từ mạng hãng.
**Processing flow**:
1. Lọc Rate Limit. Sanitize Payload.
2. Deduplication Fields (REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION). Nếu trùng (Duplicate), trả về Acknowledgement Format (REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION).
3. Xác minh Signature (Thuật toán: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION). 
4. Lock Row Payment. Xử lý Late Success (Nếu có). Ghi Outbox.
5. Cập nhật Status. Trả Acknowledgement.
**Database effects**: Insert Webhook Inbox, Update Payment, Insert Outbox.
**Concurrency**: Lock Payment.
**Integration**: Worker Outbox.
**Success acknowledgement**: Provider-defined acknowledgement (REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION).
**Error acknowledgement**: Provider-defined acknowledgement (REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION).
**Security**: Xác thực chữ ký.
**Audit**: Ghi log Webhook Inbox (Sanitized metadata).
**Verification notes**: Tuyệt đối không giả định `RspCode`, `HmacSHA256` hoặc GET/POST trước khi tham chiếu Specs chính thức.

### 13.2. MoMo IPN Webhook
| Item | Specification |
|---|---|
| Requirement ID | INT-002 |
| Purpose | Tiếp nhận Webhook MoMo IPN |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #163 |
| Method | REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION |
| Path | `/api/payments/callback/momo` |
| Access | PUBLIC |
| Headers | Provider-defined |
| Path parameters | None |
| Query parameters | Provider-defined |
| Request body | Provider-defined |
| Idempotency | Webhook Inbox |
| State Transition | PROCESSING -> SUCCESS / FAILED; Exceptional Late Success |

**Field definitions**: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
**Validation**: Rate limit, Sanitize.
**Preconditions**: Request hợp lệ từ mạng hãng.
**Processing flow**:

1. Callback Controller tiếp nhận request, áp dụng giới hạn kích thước và rate limit.
2. Chuẩn hóa và sanitize dữ liệu trước khi lưu trữ.
3. Xây dựng `deduplication_key` theo các field chính thức của MoMo:
   `REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION`.
4. Insert `payment_webhook_events`.
5. Nếu callback đã tồn tại, không lặp lại side effect và trả acknowledgement chính thức của MoMo.
6. Xác minh chữ ký theo cơ chế chính thức của MoMo:
   `REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION`.
7. Resolve Payment dựa trên provider order identifier hoặc transaction identifier đã xác minh.
8. Lock Payment row và kiểm tra amount, currency, trạng thái hiện tại.
9. Xử lý normal success, failure, duplicate hoặc exceptional late success.
10. Ghi `PaymentLog`, cập nhật `booking_payment_guards` và insert các Outbox event cần thiết trong cùng local transaction.
11. Trả acknowledgement theo định dạng chính thức của MoMo.
**Database effects**: Insert Webhook Inbox, Update Payment, Insert Outbox.
**Concurrency**: Lock Payment.
**Integration**: Worker Outbox.
**Success acknowledgement**: Provider-defined acknowledgement (REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION).
**Error acknowledgement**: Provider-defined acknowledgement (REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION).
**Security**: Xác thực chữ ký bằng thuật toán riêng.
**Audit**: Ghi log Webhook Inbox (Sanitized metadata).
**Verification notes**:
- Signature Mechanism: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
- Canonicalization: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
- Deduplication Fields: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
- Amount Scaling: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
- Provider Transaction Identifier: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
- Acknowledgement Format: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.
- Retry Behavior: REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION.

### 13.3. Dev/Test MOCK Callback
| Item | Specification |
|---|---|
| Requirement ID | API-030 |
| Purpose | Cho phép mô phỏng Webhook trên môi trường cục bộ |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #159 |
| Method | POST |
| Path | `/api/payments/callback/mock` |
| Access | PUBLIC (Disabled and not routed in production) |
| Headers | Không |
| Path parameters | Không |
| Query parameters | Không |
| Request body | `{"paymentId": 123, "simulatedStatus": "SUCCESS"}` |
| Idempotency | Webhook Inbox |
| State Transition | PROCESSING -> SUCCESS / FAILED |

**Validation**: Endpoint chỉ được đăng ký trên profile local/dev/test và không được định tuyến trong production.

**Processing flow**:
1. Xác thực profile hiện tại cho phép MOCK.
2. Validate `paymentId` và `simulatedStatus`.
3. Tạo `deduplication_key` ổn định cho lần mô phỏng.
4. Insert bản ghi `payment_webhook_events`.
5. Lock Payment row và kiểm tra trạng thái hiện tại.
6. Thực hiện transition hợp lệ sang `SUCCESS` hoặc `FAILED`.
7. Ghi `PaymentLog`, cập nhật `booking_payment_guards` và insert các Outbox event cần thiết trong cùng local transaction.
8. Trả `ApiResponse` dành cho môi trường phát triển/kiểm thử.

---
## 14. Booking Internal Contract

### 14.1. GET Payment Context
| Item | Specification |
|---|---|
| Requirement ID | INT-101 |
| Purpose | Payment Service lấy thông tin Booking có thẩm quyền trước khi tạo Payment attempt |
| Status | PROPOSED |
| Implementation Owner | Booking Service Owner |
| Contract Requester/Reviewer | Dương Thiện Nhân — Payment Service Owner |
| Issue | #158 |
| Method | GET |
| Path | `/internal/bookings/{bookingId}/payment-context` |
| Access | INTERNAL — không được định tuyến công khai |
| Headers | `X-Internal-Token: <internal-token>` |
| Path parameters | `bookingId` |
| Query parameters | None |
| Request body | None |
| Idempotency | N/A |
| State Transition | N/A |

**Validation**:
- `X-Internal-Token` hợp lệ.
- `bookingId` đúng định dạng và Booking tồn tại.
- Booking Service xác định trạng thái `payable`, chủ sở hữu, số tiền, đơn vị tiền tệ và thời hạn.

**Processing flow**:
1. Xác thực internal token.
2. Tra cứu Booking theo `bookingId`.
3. Tổng hợp dữ liệu thanh toán có thẩm quyền.
4. Trả `analyticsSnapshot` bất biến phục vụ Payment Service lưu snapshot.

**Database effects**: Read-only tại Booking Service.

**Integration**: Booking Service là chủ sở hữu và triển khai endpoint; Payment Service là consumer.

**Success response — 200 OK**:
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

**Error responses**:
- `401 INTERNAL_TOKEN_INVALID`: Thiếu hoặc sai `X-Internal-Token`.
- `404 BOOKING_NOT_FOUND`: Booking không tồn tại.
- `409 BOOKING_NOT_PAYABLE`: Booking không còn đủ điều kiện thanh toán.

**Security**:
- `X-Internal-Token` bắt buộc.
- Target Gateway Exposure: không định tuyến công khai.
- Runtime Gateway Enforcement: `REQUIRES VERIFICATION`.

### 14.2. POST Payment Results
| Item | Specification |
|---|---|
| Requirement ID | INT-102 |
| Purpose | Payment Outbox Worker gửi kết quả thanh toán sang Booking Service |
| Status | PROPOSED |
| Implementation Owner | Booking Service Owner |
| Contract Requester/Reviewer | Dương Thiện Nhân — Payment Service Owner |
| Issue | #158 |
| Method | POST |
| Path | `/internal/bookings/{bookingId}/payment-results` |
| Access | INTERNAL — không được định tuyến công khai |
| Headers | `X-Internal-Token: <internal-token>` |
| Path parameters | `bookingId` |
| Query parameters | None |
| Request body | JSON |
| Idempotency | Deduplicate theo `eventId` |
| State Transition | `PENDING_PAYMENT → CONFIRMED` khi `result = SUCCESS` và Booking chấp nhận áp dụng |

> `FAILED`, `CANCELLED` và `EXPIRED` là kết quả ở cấp Payment attempt. Các kết quả này không tự động thay đổi Booking status hoặc giải phóng ghế. Chính sách hết hạn Booking và giải phóng ghế thuộc Booking Service.

**Request body payload**:
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

**Validation**:
- `X-Internal-Token` hợp lệ.
- `eventId`, `schemaVersion`, `paymentId`, `paymentTransactionCode`, `paymentMethod`, `result`, `amount`, `currency` và `occurredAt` hợp lệ.
- `bookingId` trên path khớp Booking được áp dụng.
- `externalTransactionId` có thể null với `CASH` hoặc `MOCK`.

**Processing flow**:
1. Xác thực internal token.
2. Kiểm tra `eventId` đã được xử lý hay chưa.
3. Nếu trùng `eventId`, trả kết quả idempotent `ALREADY_PROCESSED`.
4. Nếu Booking đã được xác nhận bởi Payment khác, ghi nhận event nhưng không tạo vé lần hai; trả `ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT`.
5. Nếu Booking còn hợp lệ và `result = SUCCESS`, chuyển Booking sang trạng thái xác nhận và tạo các hiệu ứng nghiệp vụ đúng một lần.
6. Lưu `eventId` trong cùng local transaction với thay đổi Booking.

**Database effects**: Thực hiện tại Booking Service; deduplication record và thay đổi Booking phải cùng local transaction.

**Concurrency**: Booking Service phải lock hoặc dùng optimistic concurrency để ngăn hai Payment result cùng xác nhận Booking.

**Booking Result Responses — HTTP 200**:

1. **Event đã xử lý**:
```json
{
  "success": true,
  "message": "Payment result already processed",
  "errorCode": null,
  "data": {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "applied": false,
    "duplicate": true,
    "result": "ALREADY_PROCESSED"
  },
  "errors": null
}
```

2. **Booking đã được xác nhận bởi Payment khác**:
```json
{
  "success": true,
  "message": "Payment result acknowledged but not applied",
  "errorCode": null,
  "data": {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "applied": false,
    "duplicate": false,
    "result": "ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT"
  },
  "errors": null
}
```

3. **Booking được xác nhận thành công**:
```json
{
  "success": true,
  "message": "Payment result applied successfully",
  "errorCode": null,
  "data": {
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "applied": true,
    "duplicate": false,
    "result": "BOOKING_CONFIRMED"
  },
  "errors": null
}
```

**Error responses**:
- `400 VALIDATION_ERROR`: Payload không hợp lệ.
- `401 INTERNAL_TOKEN_INVALID`: Thiếu hoặc sai `X-Internal-Token`.
- `404 BOOKING_NOT_FOUND`: Booking không tồn tại.

**Delivery semantics**:
- Ba response HTTP 200 phía trên đều là acknowledgement thành công ở góc độ Outbox delivery.
- Với `ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT`, Payment Outbox được đánh dấu `PUBLISHED`; Payment vẫn `SUCCESS` và giữ `reconciliation_status = REQUIRED`.
- Không retry vô hạn khi Booking đã xác nhận bởi Payment khác.

**Security**:
- `X-Internal-Token` bắt buộc.
- Target Gateway Exposure: không định tuyến công khai.
- Runtime Gateway Enforcement: `REQUIRES VERIFICATION`.

---
## 15. Operations Contract
(Admin Ownership/Scope = System-wide operational access. Mọi API đều yêu cầu quyền ADMIN).

### 15.1. GET Admin List
| Item | Specification |
|---|---|
| Requirement ID | OPS-001 |
| Purpose | Tra cứu toàn bộ danh sách Payment |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | GET |
| Path | `/api/admin/payments` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | None |
| Query parameters | `page`, `size`, `status`, `paymentMethod`, `bookingId`, `accountId`, `reconciliationStatus`, `createdFrom`, `createdTo` |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: Role Admin. (Default page size = 20, max = 100).
**Processing flow**: Truy vấn phân trang dựa trên Filter.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK` (Trả sanitized operational metadata list).
**Error responses**: `403 FORBIDDEN`.
**Security**: Chỉ xuất thông tin Sanitized.
**Audit**: N/A.
**Observability**: N/A.

### 15.2. GET Admin Detail
| Item | Specification |
|---|---|
| Requirement ID | OPS-002 |
| Purpose | Tra cứu chi tiết một Payment phục vụ hỗ trợ khách hàng |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | GET |
| Path | `/api/admin/payments/{paymentId}` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | `paymentId` |
| Query parameters | None |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: ID tồn tại.
**Processing flow**: Kết hợp dữ liệu Payment core data, PaymentLog summary, sanitized provider summary, reconciliation data, outbox/webhook counts.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK`.
**Error responses**: `404 PAYMENT_NOT_FOUND`.
**Security**: Không được trả về unsanitized raw provider payload, provider secret, internal token, card data, JWT.
**Audit**: N/A.
**Observability**: N/A.

### 15.3. GET by Transaction Code
| Item | Specification |
|---|---|
| Requirement ID | OPS-003 |
| Purpose | Tìm kiếm bằng Transaction Code |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | GET |
| Path | `/api/admin/payments/transaction/{paymentTransactionCode}` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | `paymentTransactionCode` |
| Query parameters | None |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: Format code hợp lệ.
**Processing flow**: Tìm kiếm chính xác 1 kết quả. Nếu mã invalid format -> `400`. Tìm không ra -> `404`.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response — 200 OK**:
```json
{
  "success": true,
  "message": "Payment retrieved successfully",
  "errorCode": null,
  "data": {
    "paymentId": 123,
    "paymentTransactionCode": "PAY-1001-XYZ",
    "bookingId": 1001,
    "accountId": 15,
    "status": "SUCCESS",
    "paymentMethod": "VNPAY",
    "amount": 250000.00,
    "currency": "VND",
    "reconciliationStatus": "NONE",
    "createdAt": "2026-07-03T10:00:00Z"
  },
  "errors": null
}
```
**Error responses**: `400 VALIDATION_ERROR`, `404 PAYMENT_NOT_FOUND`.
**Security**: Sanitized metadata only.
**Audit**: N/A.
**Observability**: N/A.

### 15.4. Failed Webhook Query
| Item | Specification |
|---|---|
| Requirement ID | OPS-004 |
| Purpose | Tra cứu danh sách Webhook xử lý lỗi lỗi |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | GET |
| Path | `/api/admin/payments/webhooks/failed` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | None |
| Query parameters | `status`, `provider`, `page`, `size` |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: Role Admin.
**Processing flow**: Lọc `payment_webhook_events` theo status.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK` (Sanitized payload metadata only, do not return full raw provider payload by default).
**Error responses**: `403 FORBIDDEN`.
**Security**: Chỉ trả sanitized metadata.
**Audit**: N/A.
**Observability**: N/A.

### 15.5. Failed Outbox Query
| Item | Specification |
|---|---|
| Requirement ID | OPS-005 |
| Purpose | Tra cứu danh sách Outbox chưa phân phối thành công |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | GET |
| Path | `/api/admin/payments/outbox/failed` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | None |
| Query parameters | `destination`, `eventType`, `status`, `page`, `size` |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: Role Admin.
**Processing flow**: Truy vấn bảng Outbox.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK` (Bao gồm destination, eventType, status, attemptCount, nextRetryAt, lastError).
**Error responses**: `403 FORBIDDEN`.
**Security**: Admin Access.
**Audit**: N/A.
**Observability**: N/A.

### 15.6. Reconciliation List
| Item | Specification |
|---|---|
| Requirement ID | OPS-006 |
| Purpose | Tra cứu danh sách thanh toán ngoại lệ (Late Success) |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | GET |
| Path | `/api/admin/payments/reconciliation` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <token>` |
| Path parameters | None |
| Query parameters | `paymentMethod`, `bookingId`, `createdFrom`, `createdTo`, `reason` |
| Request body | None |
| Idempotency | N/A |
| State Transition | NONE |

**Validation**: Role Admin.
**Processing flow**: Luôn luôn filter ngầm `reconciliation_status = REQUIRED`.
**Database effects**: Read-only.
**Concurrency**: None.
**Integration**: None.
**Success response**: `200 OK`.
**Error responses**: `403 FORBIDDEN`.
**Security**: Admin Access.
**Audit**: N/A.
**Observability**: N/A.

### 15.7. Webhook Replay
| Item | Specification |
|---|---|
| Requirement ID | OPS-007 |
| Purpose | Admin yêu cầu hệ thống xử lý lại một Webhook xử lý lỗi |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | POST |
| Path | `/api/admin/payments/webhooks/{webhookEventId}/replay` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <adminToken>`, `Idempotency-Key: <UUID>` |
| Path parameters | `webhookEventId` |
| Query parameters | None |
| Request body | None |
| Idempotency | Canonical SHA-256 |
| State Transition | (Tại Webhook) FAILED -> PROCESSING |

**Validation**: Chỉ cho phép replay `FAILED` hoặc chưa xử lý (unprocessed records). Do not replay successful records (trừ khi có override cụ thể trong tương lai).
**Preconditions**: Record tồn tại.
**Processing flow**: 
1. Validate Idempotency & Eligibility.
2. Nạp lại thông tin. Preserve original webhook event (không mutate original sanitized payload).
3. Đẩy lại quy trình xử lý nội bộ. Return deterministic response.
**Database effects**: Ghi log audit, cập nhật retry_count, xử lý payment if passed.
**Concurrency**: Lock Webhook Row.
**Integration**: Gọi nội bộ hàm xử lý Provider.
**Success response**: `200 OK`.
**Error responses**: `409 WEBHOOK_REPLAY_NOT_ALLOWED`, `404 WEBHOOK_EVENT_NOT_FOUND`, `409 IDEMPOTENCY_KEY_REUSED`.
**Security**: Yêu cầu quyền vận hành cấp cao.
**Audit**: Audit log ghi nhận danh tính Admin (Id) đã thực hiện thao tác Replay.
**Observability**: MDC Trace ID tái lập.

### 15.8. Outbox Replay
| Item | Specification |
|---|---|
| Requirement ID | OPS-008 |
| Purpose | Admin yêu cầu hệ thống phát hành lại Outbox Event |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | POST |
| Path | `/api/admin/payments/outbox/{outboxEventId}/replay` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <adminToken>`, `Idempotency-Key: <UUID>` |
| Path parameters | `outboxEventId` |
| Query parameters | None |
| Request body | None |
| Idempotency | Canonical SHA-256 |
| State Transition | (Tại Outbox) FAILED/DEAD_LETTER -> PENDING |

**Validation**: Only allow `FAILED` or `DEAD_LETTER`. Do not double-publish when an event is already `PUBLISHED`.
**Preconditions**: Tồn tại Event.
**Processing flow**: 
1. Khôi phục Event. Preserve `eventId`. Do not create a new business event.
2. Reset delivery metadata safely (attempt_count, next_retry).
**Database effects**: Update Outbox Row.
**Concurrency**: Lock Outbox Row.
**Integration**: Worker sẽ pick up sau khi status chuyển PENDING.
**Success response**: `200 OK`.
**Error responses**: `409 OUTBOX_REPLAY_NOT_ALLOWED`, `404 OUTBOX_EVENT_NOT_FOUND`.
**Security**: Quyền Admin.
**Audit**: Ghi log người thao tác.
**Observability**: Giữ nguyên Correlation ID/Trace ID của Outbox gốc.

### 15.9. Resolve Reconciliation
| Item | Specification |
|---|---|
| Requirement ID | OPS-009 |
| Purpose | Kế toán chốt hoàn tất xử lý thủ công cho một khoản ngoại lệ |
| Status | PROPOSED |
| Owner | Dương Thiện Nhân |
| Issue | #169 |
| Method | POST |
| Path | `/api/admin/payments/{paymentId}/reconciliation/resolve` |
| Access | ADMIN |
| Headers | `Authorization: Bearer <adminToken>`, `Idempotency-Key: <UUID>` |
| Path parameters | `paymentId` |
| Query parameters | None |
| Request body | `{"resolutionNote": "Provider transaction verified manually", "resolutionType": "CONFIRMED_VALID_PAYMENT"}` |
| Idempotency | Canonical SHA-256 |
| State Transition | (Tại trường reconciliation) REQUIRED -> RESOLVED |

- **Field definitions**: `resolutionNote` (String), `resolutionType` (String).
- **Validation**: Only allow `REQUIRED` -> `RESOLVED`.
- **Preconditions**: Thuộc diện Late Success (REQUIRED).
- **Processing flow**:
1. Check Idempotency (Trùng hash thì replay response chuẩn).
2. Lock Payment. `resolvedByAccountId` = JWT Subject.
3. Update `reconciliation_resolved_at`.
4. Persist resolution note in sanitized audit metadata.
5. Write PaymentLog. Do not automatically refund. Do not automatically change Booking. Do not silently update Analytics revenue (trừ phi sau này có chức năng riêng).
**Database effects**: Update Payment, Insert PaymentLog, Insert Idempotency.
**Concurrency**: Lock Payment.
**Integration**: Không gọi ngoại vi.
**Success response**:
```json
{
  "success": true,
  "errorCode": null,
  "message": "Resolved",
  "data": {
    "paymentId": 123,
    "previousReconciliationStatus": "REQUIRED",
    "reconciliationStatus": "RESOLVED",
    "resolvedAt": "2026-07-03T11:00:00Z",
    "resolvedByAccountId": 45
  },
  "errors": null
}
```
**Error responses**: `409 RECONCILIATION_ALREADY_RESOLVED`, `409 IDEMPOTENCY_KEY_REUSED`.
**Security**: Admin Access.
**Audit**: Ghi nhận Audit Log người thay đổi.
**Observability**: Lưu vào MDC Trace.

---
## 16. Error Catalog

### 16.1. Customer, Employee, Admin and Internal REST Errors

| HTTP Code | Classification | Error Code | Ý nghĩa |
|---|---|---|---|
| 400 | REST Error | `VALIDATION_ERROR` | JSON, path/query parameter hoặc field không hợp lệ. |
| 400 | REST Error | `IDEMPOTENCY_KEY_REQUIRED` | Thiếu hoặc sai định dạng `Idempotency-Key`. |
| 401 | REST Error | `UNAUTHORIZED` | JWT thiếu, hết hạn hoặc không hợp lệ. |
| 401 | Internal REST Error | `INTERNAL_TOKEN_INVALID` | Thiếu hoặc sai `X-Internal-Token`. |
| 403 | REST Error | `FORBIDDEN` | Identity hợp lệ nhưng không có role hoặc phạm vi truy cập phù hợp. |
| 404 | REST Error | `PAYMENT_NOT_FOUND` | Payment không tồn tại. |
| 404 | Internal REST Error | `BOOKING_NOT_FOUND` | Booking không tồn tại. |
| 404 | Operations Error | `WEBHOOK_EVENT_NOT_FOUND` | Webhook inbox record không tồn tại. |
| 404 | Operations Error | `OUTBOX_EVENT_NOT_FOUND` | Outbox record không tồn tại. |
| 409 | Business Conflict | `BOOKING_NOT_PAYABLE` | Booking không còn đủ điều kiện thanh toán. |
| 409 | Business Conflict | `PAYMENT_ACTIVE_ATTEMPT_EXISTS` | Booking đang có Payment attempt ở trạng thái active. |
| 409 | Business Conflict | `PAYMENT_RETRY_TEMPORARILY_BLOCKED` | Settlement Hold đang chặn tạo attempt mới. |
| 409 | Business Conflict | `PAYMENT_CANNOT_BE_CANCELLED` | Payment không ở trạng thái hoặc phương thức cho phép hủy. |
| 409 | Business Conflict | `CASH_AMOUNT_INSUFFICIENT` | Số tiền khách đưa nhỏ hơn số tiền cần thanh toán. |
| 409 | Business Conflict | `CASH_ALREADY_COLLECTED` | CASH Payment đã được thu thành công. |
| 409 | Idempotency Conflict | `IDEMPOTENCY_KEY_REUSED` | Cùng key nhưng request hash khác. |
| 409 | Idempotency Conflict | `IDEMPOTENCY_REQUEST_IN_PROGRESS` | Request cùng key đang được xử lý. |
| 409 | Operations Conflict | `WEBHOOK_REPLAY_NOT_ALLOWED` | Webhook không ở trạng thái cho phép replay. |
| 409 | Operations Conflict | `OUTBOX_REPLAY_NOT_ALLOWED` | Outbox không ở trạng thái cho phép replay. |
| 409 | Operations Conflict | `RECONCILIATION_ALREADY_RESOLVED` | Payment đã được hoàn tất đối soát. |
| 502 | Upstream Error | `PAYMENT_SESSION_CREATION_FAILED` | Provider không tạo được checkout/session hợp lệ. |
| 503 | Integration Error | `BOOKING_SERVICE_UNAVAILABLE` | Booking Service tạm thời không khả dụng. |

### 16.2. Internal Processing Results

Các giá trị dưới đây là kết quả nội bộ hoặc business acknowledgement, không phải lỗi REST dành cho Customer:

| Code | Ý nghĩa |
|---|---|
| `PAYMENT_LATE_SUCCESS_RECONCILIATION_REQUIRED` | Late success đã được ghi nhận và cần đối soát. |
| `ALREADY_PROCESSED` | Booking đã xử lý cùng `eventId`. |
| `ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT` | Booking đã được xác nhận bởi Payment khác. |
| `BOOKING_CONFIRMED` | Booking đã áp dụng Payment result thành công. |

### 16.3. Provider Callback Errors

- `PROVIDER_SIGNATURE_INVALID` là kết quả xử lý webhook nội bộ, không có HTTP status chung cố định trong tài liệu này.
- HTTP status và acknowledgement trả cho VNPay/MoMo phải tuân theo tài liệu chính thức của từng Provider:
  `REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION`.
- Provider callback không sử dụng LoraFilm `ApiResponse`.

---
## 17. Security Matrix

| Requirement ID | Endpoint/Category | Target Gateway Exposure | Runtime Gateway Verification | Role | Ownership/Scope | Internal Token | Provider Signature | Profile Restriction | Idempotency | Sensitive Data Rules |
|---|---|---|---|---|---|---|---|---|---|---|
| SEC-101 | Customer Create | PUBLIC | REQUIRES VERIFICATION | CUSTOMER | Payment/Booking thuộc `userId` | Không | Không | `MOCK` chỉ non-production | Canonical SHA-256 | Không nhận amount từ Frontend; không trả provider payload |
| SEC-102 | Customer Detail/Status | PUBLIC | REQUIRES VERIFICATION | CUSTOMER | `Payment.account_id == JWT.userId` | Không | Không | Không | Không | Chỉ trả sanitized metadata |
| SEC-103 | Customer History | PUBLIC | REQUIRES VERIFICATION | CUSTOMER | Booking/attempt thuộc `userId` | Không | Không | Không | Không | Không lộ dữ liệu tài khoản khác |
| SEC-104 | Customer Cancel | PUBLIC | REQUIRES VERIFICATION | CUSTOMER | Payment thuộc `userId` | Không | Không | Không | Canonical SHA-256 | Không expose provider internals |
| SEC-201 | Employee CASH Create | PUBLIC | REQUIRES VERIFICATION | EMPLOYEE, ADMIN | Phạm vi nghiệp vụ quầy theo policy | Không | Không | Không | Canonical SHA-256 | Amount lấy từ Booking |
| SEC-202 | Employee CASH Collect/Cancel | PUBLIC | REQUIRES VERIFICATION | EMPLOYEE, ADMIN | Phạm vi nghiệp vụ quầy theo policy | Không | Không | Không | Canonical SHA-256 | Cashier lấy từ JWT; audit bắt buộc |
| SEC-301 | Admin Operations | PUBLIC | REQUIRES VERIFICATION | ADMIN | System-wide operational access | Không | Không | Không | Canonical SHA-256 cho replay/resolve | Chỉ sanitized metadata; không expose raw payload/secret/token/card data |
| SEC-401 | VNPay Callback | PUBLIC | REQUIRES VERIFICATION | N/A | Provider callback | Không | REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION | Không | Webhook Inbox Deduplication | Sanitize trước khi persist; không dùng `ApiResponse` |
| SEC-402 | MoMo Callback | PUBLIC | REQUIRES VERIFICATION | N/A | Provider callback | Không | REQUIRES OFFICIAL PROVIDER DOCUMENT VERIFICATION | Không | Webhook Inbox Deduplication | Sanitize trước khi persist; không dùng `ApiResponse` |
| SEC-403 | MOCK Callback | Disabled và không routed trong production | REQUIRES VERIFICATION | N/A | Dev/Test only | Không | Không | Local/Dev/Test only | Webhook Inbox Deduplication | Không publish MOCK revenue vào production topic |
| SEC-501 | Booking Payment Context | Không định tuyến công khai | REQUIRES VERIFICATION | N/A | Service-to-service | Bắt buộc | Không | Không | Không | Chỉ trả dữ liệu contract cần thiết |
| SEC-502 | Booking Payment Results | Không định tuyến công khai | REQUIRES VERIFICATION | N/A | Service-to-service | Bắt buộc | Không | Không | Deduplicate theo `eventId` | Không chứa secret hoặc raw provider payload |

---
## 18. Requirement Traceability
Toàn bộ API đáp ứng các Requirement liên kết với Issue #157 đến #170 (Xem Traceability trong thiết kế tổng quan).

## 19. Deferred Endpoints
Không triển khai trong Release 1:
- Hoàn tiền tự động (Refund) thông qua API hoặc Event.
