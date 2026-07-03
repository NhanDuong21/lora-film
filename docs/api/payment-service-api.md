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
- Xác thực Internal Backend qua Header: `X-Internal-Token: <SECRET>`.

## 9. Idempotency
- Áp dụng trên toàn bộ lệnh POST thay đổi trạng thái.
- Header bắt buộc: `Idempotency-Key` (UUID).
- Dữ liệu idempotency được khóa trên bảng `payment_idempotency_records`.
- Thuật toán Hash: **Canonical SHA-256** (Dựa trên operation, accountId, bookingId và normalized request body, không bao gồm khoảng trắng rỗng hoặc headers bí mật).
- **Quy trình xử lý lỗi tại Idempotency Phase**:
  - *Booking business rejection (Ví dụ hết hạn)*: Cập nhật Idempotency Record thành `FAILED` và lưu trữ cố định cấu trúc báo lỗi (deterministic error response).
  - *Temporary Booking/infrastructure failure (Lỗi hạ tầng tạm thời)*: Cập nhật thành `FAILED` kèm phân loại lỗi Retryable (deterministic stale recovery).
  - *Provider session creation failure*: Tuyệt đối KHÔNG xóa Idempotency Record. Đánh dấu Idempotency Record là lỗi. Đánh dấu Payment `FAILED`, xóa cờ `active_payment_id` tại Guard nếu nó trỏ đúng vào ID lỗi này. Lưu response báo lỗi cố định, ghi `PaymentLog`.

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

**Validation**: Disabled in production.
**Processing flow**: Nhận payload mô phỏng, chạy tương đương luồng Webhook.

---
## 14. Booking Internal Contract

### 14.1. GET Payment Context
| Item | Specification |
|---|---|
| Requirement ID | INT-101 |
| Purpose | Payment lấy thông tin giỏ hàng trước khởi tạo |
| Status | PROPOSED |
| Owner | Booking Service Owner |
| Issue | #158 |
| Method | GET |
| Path | `/internal/bookings/{bookingId}/payment-context` |
| Access | INTERNAL (Không routed public) |
| Headers | `X-Internal-Token: <SECRET>` |
| Path parameters | `bookingId` |
| Query parameters | None |
| Request body | None |
| Idempotency | N/A |
| State Transition | N/A |

**Validation**: Token hợp lệ.
**Database effects**: Read-only (Bên Booking).
**Integration**: Booking.
**Success response**: `200 OK` (Trả amount, accountId, payable, analyticsSnapshot).
**Error responses**: `403 FORBIDDEN` (Lỗi Token).
**Security**: `X-Internal-Token` bắt buộc.

### 14.2. POST Payment Results
| Item | Specification |
|---|---|
| Requirement ID | INT-102 |
| Purpose | Payment gửi Outbox chốt vé sang Booking |
| Status | PROPOSED |
| Owner | Booking Service Owner |
| Issue | #158 |
| Method | POST |
| Path | `/internal/bookings/{bookingId}/payment-results` |
| Access | INTERNAL (Không routed public) |
| Headers | `X-Internal-Token: <SECRET>` |
| Path parameters | `bookingId` |
| Query parameters | None |
| Request body | JSON |
| Idempotency | Event Deduplication |
| State Transition | PENDING_PAYMENT → CONFIRMED khi result = SUCCESS và Booking chấp nhận áp dụng. |
| FAILED / CANCELLED / EXPIRED | Không tự động thay đổi Booking status hoặc giải phóng ghế. Booking expiration thuộc Booking Service. |
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

**Booking Result Responses (Sử dụng ApiResponse chuẩn)**:
1. Trùng Event (Đã xử lý): 
   `{"success": true, "message": "Result processed", "errorCode": null, "data": { "applied": false, "duplicate": true, "result": "ALREADY_PROCESSED" }}`
2. Event Mới nhưng vé đã bị chốt bởi attempt khác (Late Success):
   `{"success": true, "message": "Result processed", "errorCode": null, "data": { "applied": false, "duplicate": false, "result": "ALREADY_CONFIRMED_BY_ANOTHER_PAYMENT" }}`
3. Chốt vé thành công:
   `{"success": true, "message": "Result processed", "errorCode": null, "data": { "applied": true, "duplicate": false, "result": "BOOKING_CONFIRMED" }}`

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
**Security**: Tuyệt đối không xuất (must not expose) unsanitized raw provider payload, provider secret, internal token, card data, JWT.
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
**Success response**: `200 OK` (Tương đương Admin Detail).
**Error responses**: `400 VALIDATION_ERROR`, `404 PAYMENT_NOT_FOUND`.
**Security**: Sanitized metadata only.
**Audit**: N/A.
**Observability**: N/A.

### 15.4. Failed Webhook Query
| Item | Specification |
|---|---|
| Requirement ID | OPS-004 |
| Purpose | Tra cứu danh sách Webhook kẹt lỗi |
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
**Security**: Chặn RAW data.
**Audit**: N/A.
**Observability**: N/A.

### 15.5. Failed Outbox Query
| Item | Specification |
|---|---|
| Requirement ID | OPS-005 |
| Purpose | Tra cứu danh sách Outbox không thể phân phối |
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
| Purpose | Admin yêu cầu hệ thống xử lý lại một Webhook kẹt |
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
**Error responses**: `400 WEBHOOK_REPLAY_NOT_ALLOWED`, `404 WEBHOOK_EVENT_NOT_FOUND`, `409 IDEMPOTENCY_KEY_REUSED`.
**Security**: Yêu cầu quyền vận hành cấp cao.
**Audit**: Audit log ghi nhận danh tính Admin (Id) đã thực hiện thao tác Replay.
**Observability**: MDC Trace ID tái lập.

### 15.8. Outbox Replay
| Item | Specification |
|---|---|
| Requirement ID | OPS-008 |
| Purpose | Admin ép hệ thống gửi lại Outbox Event |
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
**Error responses**: `400 OUTBOX_REPLAY_NOT_ALLOWED`, `404 OUTBOX_EVENT_NOT_FOUND`.
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
  }
}
```
**Error responses**: `409 RECONCILIATION_ALREADY_RESOLVED`, `409 IDEMPOTENCY_KEY_REUSED`.
**Security**: Admin Access.
**Audit**: Ghi nhận Audit Log người thay đổi.
**Observability**: Lưu vào MDC Trace.

---
## 16. Error Catalog
| HTTP Code | Thuộc tính / Phân loại | Thông báo (Error Code) | Ý nghĩa nghiệp vụ / Nội bộ |
|---|---|---|---|
| 400 | REST Error | `VALIDATION_ERROR` | Lỗi JSON, tham số, sai UUID. |
| 401 | REST Error | `UNAUTHORIZED` | Token user hết hạn / rỗng. |
| 401 | REST Error | `INTERNAL_TOKEN_INVALID` | Lỗi X-Internal-Token. |
| 403 | Webhook Internal | `PROVIDER_SIGNATURE_INVALID`| (Không phải REST Customer Error). Dành cho xử lý Webhook nội bộ hãng. |
| 403 | REST Error | `FORBIDDEN` | Sai Role / Lỗi truy cập / Vi phạm Security. |
| 404 | REST Error | `PAYMENT_NOT_FOUND` | Không có attempt Payment được nhắc đến. |
| 409 | REST Error | `PAYMENT_ACTIVE_ATTEMPT_EXISTS`| Guard báo đang có 1 luồng bận PENDING/PROCESSING. |
| 409 | REST Error | `PAYMENT_RETRY_TEMPORARILY_BLOCKED` | Bị chặn do Settlement Hold. |
| 409 | REST Error | `IDEMPOTENCY_KEY_REUSED` | Trùng UUID, nhưng lệch SHA-256 Payload Hash. |
| 409 | Internal | `PAYMENT_LATE_SUCCESS_RECONCILIATION_REQUIRED` | Internal Operation status. |
| 502 | REST Error | `PAYMENT_SESSION_CREATION_FAILED`| Giao tiếp Provider Init tạo Session thất bại. |
| 502 | Internal | `WEBHOOK_PROCESSING_MODE_UNVERIFIED`| Internal Operation status. |
| 503 | REST Error | `BOOKING_SERVICE_UNAVAILABLE` | Service interruption cục bộ. |

---
## 17. Security Matrix
| Requirement ID | Endpoint/Category | Target Gateway Exposure | Runtime Gateway Verification | Role | Ownership/Scope | Internal Token | Provider Signature | Profile Restriction | Idempotency | Sensitive Data Rules |
|---|---|---|---|---|---|---|---|---|---|---|
| SEC-101 | Customer Create | PUBLIC | REQUIRES VERIF. | CUSTOMER | Chủ tài khoản | Không | Không | Disabled in Prod (MOCK)| Canonical SHA256 | Chặn RAW Data |
| SEC-102 | Cust. Detail/Status| PUBLIC | REQUIRES VERIF. | CUSTOMER | Chủ tài khoản | Không | Không | Không | Không | Chỉ Sanitized Metadata |
| SEC-104 | Cust. History | PUBLIC | REQUIRES VERIF. | CUSTOMER | Chủ tài khoản | Không | Không | Không | Không | Mảng an toàn |
| SEC-105 | Cust. Cancel | PUBLIC | REQUIRES VERIF. | CUSTOMER | Chủ tài khoản | Không | Không | Không | Canonical SHA256 | N/A |
| SEC-201 | Emp. CASH Create | PUBLIC | REQUIRES VERIF. | EMPLOYEE, ADMIN | System-wide | Không | Không | Không | Canonical SHA256 | N/A |
| SEC-202 | Emp. CASH Collect | PUBLIC | REQUIRES VERIF. | EMPLOYEE, ADMIN | System-wide | Không | Không | Không | Canonical SHA256 | Audit Log Thu Ngân |
| SEC-301 | Admin Endpoints | PUBLIC | REQUIRES VERIF. | ADMIN | System-wide operational access | Không | Không | Không | Canonical SHA256 (cho thao tác Replay/Resolve) | Sanitized metadata (Cấm RAW Payload) |
| SEC-401 | Webhook Callback | PUBLIC | REQUIRES VERIF. | N/A | Provider | Không | REQUIRES VERIF. | Disabled in Prod (MOCK) | Inbox Dedup | Sanitize RAW Data |
| SEC-501 | Booking Internal API | Không Routed Public | REQUIRES VERIF. | N/A | Server to Server | Mandatory | Không | Không | Event Dedup | N/A |

---
## 18. Requirement Traceability
Toàn bộ API đáp ứng các Requirement liên kết với Issue #157 đến #170 (Xem Traceability trong thiết kế tổng quan).

## 19. Deferred Endpoints
Không triển khai trong Release 1:
- Hoàn tiền tự động (Refund) thông qua API hoặc Event.
