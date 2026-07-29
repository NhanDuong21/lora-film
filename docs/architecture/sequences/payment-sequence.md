# Payment Release 1 — Luồng nghiệp vụ

## Thanh toán online

```mermaid
sequenceDiagram
    actor Customer
    participant UI as React Client
    participant P as Payment Service
    participant B as Booking Service
    participant DB as Payment MySQL
    participant Provider as VNPay/MoMo
    participant K as Kafka

    Customer->>UI: Chọn VNPay hoặc MoMo
    UI->>B: Finalize checkout
    UI->>P: POST /api/payments + Idempotency-Key
    P->>B: GET payment-context (internal token)
    B-->>P: Locked amount, owner, deadline, snapshot
    P->>DB: Reserve guard + attempt + idempotency
    P->>Provider: Tạo provider session
    Provider-->>P: paymentUrl/orderId
    P->>DB: Finalize PROCESSING
    P-->>UI: Payment URL
    UI->>Provider: Redirect khách hàng

    par IPN có thẩm quyền
        Provider->>P: Signed callback/IPN
        P->>P: Verify signature/order/amount/result
        P->>DB: Inbox + Payment result + Booking outbox
    and Browser return không có thẩm quyền
        Provider->>P: Browser return
        P->>P: Verify signature only
        P-->>UI: Redirect /payments/return
        UI->>P: Poll Payment status
    end

    loop Outbox retry
        P->>B: POST payment-results
        alt Booking chấp nhận
            B->>B: HELD → BOOKED, CONFIRMED, tickets
            B-->>P: accepted=true
            P->>DB: Tạo Analytics outbox
            P->>K: PAYMENT_SUCCEEDED
        else Booking yêu cầu đối soát
            B-->>P: 409 reconciliationRequired
            P->>DB: Hoàn tất delivery + tạo reconciliation
        else Booking tạm thời unavailable
            P->>DB: Backoff/retry, sau ngưỡng thành DEAD_LETTER
        end
    end
```

Booking/MySQL `seat_reservations` là nguồn giữ ghế dài hạn duy nhất. Payment
không đọc Redis, không giải phóng ghế và không kéo dài deadline. FAILED/CANCELLED
attempt không hủy Booking; khách có thể thử attempt mới trước deadline gốc khi
kết quả provider đã chắc chắn.

## Callback trùng và mâu thuẫn

- Provider + deduplication key + cùng raw-body hash: ACK idempotent, không lặp
  transition hoặc outbox.
- Cùng deduplication key nhưng hash khác: trả conflict/ACK phù hợp provider và
  tạo reconciliation.
- Chữ ký sai: lưu inbox đã sanitize để audit, không thay đổi Payment và không
  cho replay.
- SUCCESS đến sau deadline vẫn ghi nhận sự thật tài chính ở Payment, nhưng
  Booking không tự xác nhận; case được chuyển sang đối soát.

## CASH tại quầy

```mermaid
sequenceDiagram
    actor Employee
    participant UI as Employee Client
    participant P as Payment Service
    participant B as Booking Service
    participant DB as Payment MySQL

    Employee->>UI: Nhập mã đơn/UUID
    UI->>P: GET booking lookup
    P->>B: GET payment-context theo UUID hoặc code
    B-->>P: Payable + số tiền authoritative
    P-->>UI: Thông tin cần thu
    UI->>P: POST cash + Idempotency-Key
    P->>DB: Reserve CASH attempt
    Employee->>UI: Nhập tiền khách đưa
    UI->>P: POST cash/collect + Idempotency-Key
    P->>B: Recheck context/deadline
    P->>DB: SUCCESS + tiền nhận/tiền thừa + outbox
```

Thiếu tiền bị từ chối. Duplicate collect/cancel dùng cùng idempotency key trả lại
kết quả cũ. CASH cũng đi qua đúng outbox Booking và Analytics như provider thật.

## Recovery

- Provider request chạy ngoài DB transaction.
- Order/request ID ổn định cho cùng attempt.
- Mất response hoặc timeout đặt settlement hold; scheduler query provider trước
  khi cho retry.
- Outbox dùng owner token + lease; network call chạy ngoài transaction.
- Replay giữ nguyên event ID để downstream dedupe.

## Hoàn tiền

```mermaid
sequenceDiagram
    participant Trigger as Hệ thống/Admin
    participant P as Payment Service
    participant Provider as VNPay/MoMo/CASH
    participant B as Booking Service
    participant K as Analytics

    Trigger->>P: Tạo refund idempotent
    P->>P: Kiểm tra SUCCESS, snapshot và số tiền còn lại
    alt Online
        P->>Provider: Full/partial refund
        Provider-->>P: SUCCESS/FAILED/UNKNOWN
        opt UNKNOWN
            P->>Provider: Query refund trước khi retry
        end
    else CASH
        P->>P: REQUIRES_ACTION
        Trigger->>P: Xác nhận biên nhận hoàn tại quầy
    end
    P->>B: REFUND_SUCCESS qua outbox
    alt Booking chấp nhận
        B->>B: Cộng dồn refunded amount
        B-->>P: accepted
        P->>K: PAYMENT_REFUNDED
    else Booking từ chối
        B-->>P: reconciliationRequired
        P->>P: Tạo hồ sơ đối soát
    end
```

Refund tự động được tạo khi thanh toán thành công đến trễ, Booking từ chối xác
nhận, thu trùng hoặc suất chiếu bị hủy. Admin chỉ tạo hoàn một phần cho bắp nước,
chênh lệch giá và điều chỉnh nghiệp vụ. Chưa có hoàn riêng từng vé; refund không
thay đổi sức chứa phòng hoặc mở lại ghế `BOOKED`.
