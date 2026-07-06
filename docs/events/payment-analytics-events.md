# LoraFilm Payment Service
# Analytics Event Contract

## 1. Document Control

| Field | Value |
|---|---|
| Project | LoraFilm |
| Service | payment-service |
| Document Type | Event Contract |
| Implementation Owner | Dương Thiện Nhân |
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
- [5. Producer/consumer](#5-producerconsumer)
- [6. Topic topology](#6-topic-topology)
- [7. Ordering](#7-ordering)
- [8. Event envelope](#8-event-envelope)
- [9. PAYMENT_SUCCEEDED](#9-payment_succeeded)
- [10. Field definitions](#10-field-definitions)
- [11. Validation](#11-validation)
- [12. Snapshot source](#12-snapshot-source)
- [13. Reconciliation-required revenue behavior](#13-reconciliation-required-revenue-behavior)
- [14. Producer idempotency](#14-producer-idempotency)
- [15. Consumer idempotency and transaction](#15-consumer-idempotency-and-transaction)
- [16. Retry and DLQ](#16-retry-and-dlq)
- [17. Schema versioning](#17-schema-versioning)
- [18. Privacy](#18-privacy)
- [19. MOCK Isolation](#19-mock-isolation)
- [20. Observability](#20-observability)
- [21. Implementation mapping](#21-implementation-mapping)
- [22. Deferred PAYMENT_REFUNDED](#22-deferred-payment_refunded)
- [23. Reviewer checklist](#23-reviewer-checklist)

## 3. Purpose
Định nghĩa hợp đồng kỹ thuật cho sự kiện Analytics nhằm đồng bộ dữ liệu doanh thu một cách đáng tin cậy.

Production revenue events bao gồm:
- CASH
- VNPAY
- MOMO

Non-production revenue events (Chỉ tồn tại ở môi trường mô phỏng):
- MOCK

## 4. Scope
- Sự kiện `PAYMENT_SUCCEEDED`.
- Phát hành tự động qua Transactional Outbox.

## 5. Producer/consumer
- **Producer**: Payment Service (Outbox Worker).
- **Consumer**: Analytics Service.

## 6. Topic topology
**EVT-001**: 
- **Production Topic**: `payment.events.v1`
- **Production DLQ**: `payment.events.v1.dlq`
- **Non-Production MOCK Isolation**: Sử dụng environment-specific topic naming theo cấu hình (ví dụ `dev.payment.events.v1`) hoặc Kafka cluster cách ly hoàn toàn. Production `payment.events.v1` không bao giờ tiếp nhận MOCK revenue.

## 7. Ordering
**EVT-002**: Producer phân bổ các sự kiện vào partition thông qua Kafka Message Key là `bookingId`. Đảm bảo các thay đổi cho cùng 1 Booking diễn ra tuần tự. Không có bảo đảm thứ tự chéo giữa các Booking khác nhau.
- **Lưu ý**: Kafka message key là `bookingId`, danh tính aggregate (Aggregate Identity) của event là `PAYMENT`.

## 8. Event envelope
Tất cả sự kiện tuân thủ cấu trúc phong bì chung:

| Envelope Field | Type | Description |
|---|---|---|
| `eventId` | UUID | Định danh duy nhất cho event (Dùng để Consumer Deduplicate) |
| `eventType` | String | Tên loại sự kiện (VD: `PAYMENT_SUCCEEDED`) |
| `schemaVersion` | String | Phiên bản (VD: `1.0`) |
| `sourceService` | String | Tên service phát hành (`payment-service`) |
| `occurredAt` | Timestamp | Thời điểm sinh sự kiện (ISO-8601 UTC) |
| `correlationId` | String | Chuỗi liên kết trace qua nhiều services (Nullable) |
| `traceId` | String | OpenTelemetry Trace ID (Nullable) |
| `aggregateType` | String | Cố định: `PAYMENT` |
| `aggregateId` | String | Bằng với `paymentId` |
| `data` | JSON | Cấu trúc payload cụ thể theo sự kiện |

## 9. PAYMENT_SUCCEEDED
Báo cáo doanh thu và giao dịch thanh toán thành công.

### Example JSON
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "PAYMENT_SUCCEEDED",
  "schemaVersion": "1.0",
  "sourceService": "payment-service",
  "occurredAt": "2026-07-03T10:05:00Z",
  "correlationId": "trace-9999",
  "traceId": "span-8888",
  "aggregateType": "PAYMENT",
  "aggregateId": "55",
  "data": {
    "paymentId": 55,
    "paymentTransactionCode": "PAY-1001-XYZ",
    "bookingId": 1001,
    "paymentMethod": "VNPAY",
    "provider": "VNPAY",
    "paidAmount": 250000.00,
    "currency": "VND",
    "movieId": 99,
    "movieTitle": "Avengers",
    "ticketCount": 2,
    "reconciliationStatus": "NONE"
  }
}
```

## 10. Field definitions
| Field | Data Type | Required | Description |
|---|---|---|---|
| `paymentId` | Number | Yes | ID định danh attempt thanh toán nội bộ |
| `paymentTransactionCode` | String | Yes | Mã giao dịch nội bộ Payment Service |
| `bookingId` | Number | Yes | ID của Booking (Kafka Message Key) |
| `paymentMethod` | String | Yes | MOCK, CASH, VNPAY, MOMO |
| `provider` | String | No | VNPAY, MOMO, null đối với CASH/MOCK |
| `paidAmount` | Decimal | Yes | Tổng tiền khách thực tế đã thanh toán |
| `currency` | String | Yes | Đơn vị tiền tệ (VND) |
| `movieId` | Number | Yes | Lấy từ Analytics Snapshot |
| `movieTitle` | String | Yes | Lấy từ Analytics Snapshot |
| `ticketCount` | Number | Yes | Lấy từ Analytics Snapshot |
| `reconciliationStatus`| String | Yes | `NONE`, `REQUIRED` (Trong Product Release 1) |

## 11. Validation
**EVT-003**: `paidAmount > 0`. Event chỉ được sinh khi trạng thái của Payment đã được ghi nhận `SUCCESS`.

## 12. Snapshot source
**EVT-004**: Dữ liệu `movieId`, `movieTitle`, `ticketCount` được trích xuất từ bảng `payment_analytics_snapshots`. Payment Service không thực hiện gọi API mạng (Network call) đến Booking/Movie để trích xuất dữ liệu khi tạo sự kiện.

## 13. Reconciliation-required revenue behavior
**EVT-005**: 
- **Normal success (`reconciliationStatus = NONE`)**: Consumer cập nhật tổng doanh thu xác nhận thông thường.
- **Reconciliation-required success (`reconciliationStatus = REQUIRED`)**: Consumer KHÔNG cộng vào tổng doanh thu chuẩn. Dữ liệu này phải được lưu vào khu vực riêng (ví dụ: `analytics_reconciliation_events`) phục vụ tra cứu ngoại lệ.
- *Lưu ý*: `RESOLVED` là trạng thái bảo lưu dành cho cơ chế mở rộng trong tương lai, không xuất hiện trong sự kiện ở Release 1.

## 14. Producer idempotency
**EVT-006**: Outbox Pattern bảo vệ việc insert Event Payload vào Database cùng transaction với lệnh cập nhật Payment `SUCCESS`. Cột `eventId` không đổi ngay cả khi Worker Outbox thực hiện phát hành lại (Retry).

## 15. Consumer idempotency and transaction
**EVT-007**: **The Local Database transaction and Kafka Offset commit are not one distributed atomic transaction.**
Trường hợp xảy ra ngắt quãng (crash):
- Nếu quá trình DB commit thành công nhưng service ngưng hoạt động trước khi Kafka offset được commit, Kafka có thể sẽ tiến hành phát hành lại chính event đó (duplicate delivery).
Hành vi bắt buộc của Consumer:
- Deduplicate bằng `eventId`.
- Bảng `processed_analytics_events` phải được ghi cùng Local Transaction với các thao tác cập nhật doanh thu/đối soát.
- Lần phát hành trùng lặp không được phép làm tăng doanh thu kép.
- Offset Kafka chỉ được commit sau khi DB transaction cục bộ đã thành công. Bản thân Offset Kafka không được xem là cơ chế thay thế Idempotency.

## 16. Retry and DLQ
**EVT-008**:
- **Retryable Errors**: Lỗi mất kết nối tạm thời, khóa Database timeout. Consumer thực hiện Retry theo cấu hình (ví dụ: Exponential backoff).
- **Non-retryable Errors**: Lỗi logic nghiệp vụ, schema không hợp lệ.
- **DLQ Routing**: Sau khi hết số lượt Retry cấu hình, hoặc phát hiện Non-retryable error, Consumer chuyển Message vào `payment.events.v1.dlq` và cảnh báo Observability.

## 17. Schema versioning
**EVT-009**: 
- Consumers cần chủ động bỏ qua các optional fields không nhận diện được (ignore unknown optional fields when safe).
- Bất kỳ thao tác xóa bỏ trường dữ liệu (breaking field removals), thay đổi kiểu dữ liệu (type changes), hoặc thay đổi định nghĩa dữ liệu (semantic changes) đều bắt buộc phải gia tăng `schemaVersion`.
- Các schema phiên bản không được hỗ trợ sẽ bị đẩy vào DLQ.
- Producer và Consumer phải phối hợp (coordinated migration) trước khi thực hiện breaking changes.

## 18. Privacy
**EVT-010**: Không đưa dữ liệu cá nhân (PII), Secret keys, Dữ liệu thẻ tín dụng vào Event Payload.

## 19. MOCK Isolation
**EVT-011**: Production Analytics Service tuyệt đối không được phép tiếp nhận MOCK revenue. Sự kiện MOCK chỉ tồn tại ở các topic không thuộc Production cluster (Ví dụ: thông qua environment-specific topic naming cấu hình linh hoạt).

## 20. Observability
**EVT-012**: Các log và metrics của Consumer phải chứa `traceId` và `eventId`.

## 21. Implementation mapping
- `#157`: Schema cơ bản cho Snapshot, Outbox.
- `#161`: Outbox Worker Foundation.
- `#166`: Producer & Consumer Event logic.
- `#169`: Reconciliation Operations.
- `#170`: Runtime E2E verification.

## 22. Deferred PAYMENT_REFUNDED
- Trạng thái: DEFERRED — NOT IMPLEMENTED IN PRODUCT RELEASE 1.
- Ghi chú: Yêu cầu #166 không chịu trách nhiệm implement event hoàn tiền.

## 23. Reviewer checklist
- [ ] Xác minh Kafka Topic và DLQ Namespace đã phân tách an toàn MOCK/Production.
- [ ] Xác minh Consumer Transaction xử lý Idempotency và Commit Offset tuần tự.
- [ ] Xác minh luật `reconciliationStatus = REQUIRED` KHÔNG tính khống doanh thu chuẩn.
- [ ] Đảm bảo Schema Versioning tuân thủ luật Breaking Changes.
- [ ] Đảm bảo Aggregate Identity đúng là `PAYMENT`, Message Key là `bookingId`.
