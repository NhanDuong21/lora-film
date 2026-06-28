# Score Service Sequence

> Đây là draft flow cho **Score module** trong hệ thống đặt vé xem phim online.
> Score Service là service riêng, không validate mã khuyến mãi. Service này chỉ xử lý cộng điểm cho user sau khi booking thành công.

## Mục đích

Tài liệu này mô tả luồng cộng điểm cho user sau khi booking/payment thành công.

## Diagram liên quan

```text
docs/architecture/diagrams/score-sequence.puml
docs/architecture/diagrams/score-sequence.drawio
docs/architecture/diagrams/score-sequence.png
```

## Mô tả flow

### Bước 1: Booking thành công

Sau khi user thanh toán thành công, Booking Service cập nhật trạng thái booking thành công.

### Bước 2: Booking Service publish event vào Kafka

Booking Service publish event `BookingCompleted` vào Kafka.

Ví dụ event:

```json
{
  "eventName": "BookingCompleted",
  "bookingId": "BOOKING_ID",
  "userId": "USER_ID",
  "finalAmount": 160000,
  "ticketQuantity": 2,
  "promotionCode": "SALE20",
  "completedAt": "2026-06-04T10:00:00"
}
```

### Bước 3: Score Service consume event từ Kafka

Score Service nhận event `BookingCompleted` từ Kafka.

Việc dùng Kafka giúp tách luồng cộng điểm khỏi luồng booking/payment chính.

### Bước 4: Score Service tính điểm thưởng

Score Service tính điểm dựa trên rule của hệ thống.

Ví dụ:

```text
score = finalAmount / 10000
```

Nếu `finalAmount = 160000`, user nhận được:

```text
16 điểm
```

### Bước 5: Score Service lưu lịch sử cộng điểm

Score Service lưu lịch sử cộng điểm để theo dõi user được cộng điểm từ booking nào.

### Bước 6: Score Service cập nhật tổng điểm user

Score Service cập nhật tổng điểm tích lũy của user một cách atomic (nguyên tử) thông qua câu lệnh SQL UPDATE trực tiếp (không load entity trước), sử dụng các method như `addPointsAtomic` hoặc `addCurrentPointsOnlyAtomic` nhằm tránh tranh chấp dữ liệu (race condition) dưới môi trường đa luồng.

### Bước 7: Score Service commit event

Sau khi xử lý thành công, Score Service commit event để tránh xử lý lại event cũ.

## Khả năng và Ràng buộc Lưu trữ (Persistence Capabilities & Constraints)

*   **Cơ chế chống lặp (Duplicate Detection Strategy):** Hệ thống không thực hiện truy vấn kiểm tra trước khi chèn (ví dụ: `existsBy...`) để tránh race conditions dạng Time-of-Check to Time-of-Use. Thay vào đó, tính duy nhất được đảm bảo hoàn toàn ở mức database bằng các UNIQUE constraints: `uk_score_history_idempotency` (`idempotency_key`), `uk_score_history_event_id` (`event_id`), và `uk_score_history_request_id` (`request_id`). Mọi hành vi chèn trùng lặp sẽ ném ra `DataIntegrityViolationException` và rollback transaction.
*   **Điểm tích lũy trọn đời (Accumulated Points):** Trường `accumulated_points` đại diện cho điểm tích lũy trọn đời để xét hạng thành viên (Membership Tier), do đó nó không giảm khi user thực hiện quy đổi điểm (`REDEEM_FOR_BOOKING`).
*   **Truy vấn Lịch sử Booking (Booking History Query):** Hỗ trợ tìm kiếm danh sách lịch sử điểm liên quan đến một booking cụ thể bằng `findByBookingId(Long bookingId)`, được tối ưu hóa bằng index `idx_score_history_booking`.
*   **Truy vấn Đối soát (Reconciliation Query):** Hỗ trợ tìm kiếm các bản ghi điểm theo trạng thái đối soát và sắp xếp tăng dần theo thời gian tạo bằng `findByReconciliationStatusOrderByCreatedAtAsc(ReconciliationStatus status)`, được tối ưu hóa bằng index `idx_score_history_reconciliation` để xử lý các yêu cầu cũ trước.

## Ghi chú

* Đây là draft flow cho Score module.
* Score Service là service riêng.
* Score Service không validate mã khuyến mãi.
* Promotion Service không nằm trong diagram này.
* Kafka được dùng để tách luồng cộng điểm khỏi luồng booking/payment chính.
* Code PlantUML để vẽ diagram nằm trong file `score-sequence.puml`.
