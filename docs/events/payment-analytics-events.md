# Payment → Analytics Event Contract

Release 1 phát sự kiện doanh thu bằng transactional outbox sau khi Booking
Service đã chấp nhận kết quả Payment SUCCESS.

## Topic và idempotency

- Topic mặc định: `payment-success.v1`.
- Kafka message key: `paymentPublicId`.
- `eventId` của outbox là khóa dedupe của consumer.
- Replay giữ nguyên `eventId`.
- CASH, VNPAY và MOMO được phát trong môi trường production.
- MOCK chỉ được bật ở local/test; không được cấu hình vào production.

## Payload `PAYMENT_SUCCEEDED`

```json
{
  "eventId": "516ba5e0-27c5-4fb6-a0cc-58637818247d",
  "schemaVersion": "1.0",
  "paymentPublicId": "d14bd538-83b8-4778-8200-5a49de7af0df",
  "bookingPublicId": "74bbbca7-b513-482b-851e-e7cc7a8cf66a",
  "provider": "VNPAY",
  "amount": 325000,
  "currency": "VND",
  "succeededAt": "2026-07-27T04:15:30Z",
  "movieId": 9,
  "moviePublicId": "seed-movie-09",
  "movieTitle": "Nhà Có Năm Nàng Tiên",
  "showtimePublicId": "17a52470-eb9c-5165-a90a-7a86fa483938",
  "cinemaPublicId": "cinema-hai-chau",
  "ticketCount": 2,
  "ticketAmount": 170000,
  "foodAmount": 155000,
  "discountAmount": 0,
  "totalAmount": 325000
}
```

## Nguồn dữ liệu

Các trường phim, suất chiếu, rạp và breakdown tài chính được Payment snapshot từ
`payment-context` authoritative của Booking lúc tạo attempt. Booking lấy các giá
trị này từ snapshot bất biến, không tính lại theo Movie/policy hiện tại.

Các invariant:

- `amount == totalAmount`.
- `ticketAmount + foodAmount - discountAmount == totalAmount`.
- `currency` là mã ISO viết hoa.
- `succeededAt` là UTC `Instant`.
- Không chứa email, số điện thoại, JWT, secret provider hoặc raw webhook.

## Điều kiện không phát doanh thu

Không tạo event chuẩn khi:

- Payment chưa SUCCESS.
- Chữ ký/provider order/amount/currency không hợp lệ.
- Booking trả `reconciliationRequired` hoặc 409.
- Payment đang `REQUIRED`/`IN_REVIEW` reconciliation.

Khi Kafka unavailable, outbox giữ event và retry exponential. Sau ngưỡng cấu
hình, event chuyển `DEAD_LETTER`; Admin có thể replay mà không đổi `eventId`.

## Payload `PAYMENT_REFUNDED`

Sự kiện chỉ được tạo sau khi Booking Service chấp nhận `REFUND_SUCCESS`:

```json
{
  "eventId": "9b293c1c-23a3-4de5-a13a-fdc85be0eff1",
  "eventType": "PAYMENT_REFUNDED",
  "schemaVersion": "1.0",
  "refundPublicId": "32ee6d62-008e-43f9-8bfd-3bb113a23534",
  "paymentPublicId": "d14bd538-83b8-4778-8200-5a49de7af0df",
  "bookingPublicId": "74bbbca7-b513-482b-851e-e7cc7a8cf66a",
  "provider": "VNPAY",
  "refundType": "PARTIAL",
  "refundComponent": "CONCESSION",
  "reasonCode": "CONCESSION_UNAVAILABLE",
  "amount": 50000,
  "currency": "VND",
  "refundedAt": "2026-07-29T05:00:00Z"
}
```

Kafka message key là `paymentPublicId`; `eventId` không đổi khi replay. Analytics
phải ghi nhận đây là khoản giảm trừ doanh thu, không phải một Payment âm mới.
