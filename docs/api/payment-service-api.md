# Payment Service API — Release 1

Tài liệu này mô tả hợp đồng đang được triển khai cho VNPay Sandbox, MoMo
Sandbox, tiền mặt tại quầy và MOCK dành riêng cho local/test.

## Nguyên tắc

- Booking Service sở hữu số tiền, tiền tệ, chủ đơn và thời hạn thanh toán.
- Payment Service không nhận `amount`, `currency` hoặc thời hạn từ trình duyệt.
- Public UUID là định danh API. Numeric ID chỉ còn là compatibility adapter trong
  một release.
- Mọi thời điểm được lưu và trao đổi bằng ISO-8601 UTC.
- Browser Return chỉ điều hướng. IPN/callback đã xác minh mới có thẩm quyền ghi
  nhận kết quả tài chính.
- Payment SUCCESS chỉ phát doanh thu sau khi Booking Service chấp nhận kết quả.
- Refund được Payment Service điều phối theo số tiền thực thu; Booking chỉ nhận kết
  quả hoàn tiền đã xác nhận để cập nhật projection nghiệp vụ.
- Không hỗ trợ hoàn riêng từng vé. Ghế `BOOKED` không được mở bán lại chỉ vì có
  một khoản hoàn tiền.

Response thông thường dùng wrapper:

```json
{
  "success": true,
  "message": "Thành công",
  "errorCode": null,
  "data": {},
  "errors": null
}
```

## Customer API

Tất cả API dưới đây yêu cầu JWT CUSTOMER và kiểm tra `accountId` của Booking:

| Method | Endpoint | Ghi chú |
|---|---|---|
| POST | `/api/payments` | Tạo attempt VNPay hoặc MoMo; yêu cầu `Idempotency-Key` |
| GET | `/api/payments/{paymentPublicId}` | Chi tiết giao dịch thuộc khách hàng |
| GET | `/api/payments/{paymentPublicId}/status` | Trạng thái dùng cho polling |
| GET | `/api/payments/booking/{bookingPublicId}` | Các attempt của một Booking |
| POST | `/api/payments/{paymentPublicId}/cancel` | Chỉ hủy khi chưa có provider session hoạt động |

Request tạo giao dịch:

```json
{
  "bookingPublicId": "74bbbca7-b513-482b-851e-e7cc7a8cf66a",
  "paymentMethod": "VNPAY"
}
```

`paymentMethod` dành cho customer nhận `VNPAY` hoặc `MOMO`. Khi chạy local/test
và `payment.providers.mock.enabled=true`, `MOCK` được phép để kiểm thử. Request
compatibility có thể gửi `bookingId` thay `bookingPublicId`, nhưng không được gửi
cả hai.

Response tạo giao dịch chứa:

```json
{
  "paymentPublicId": "d14bd538-83b8-4778-8200-5a49de7af0df",
  "bookingPublicId": "74bbbca7-b513-482b-851e-e7cc7a8cf66a",
  "status": "PROCESSING",
  "provider": "VNPAY",
  "amount": 150000,
  "currency": "VND",
  "paymentUrl": "https://sandbox.vnpayment.vn/...",
  "expiresAt": "2026-07-27T12:00:00Z"
}
```

Cùng actor, endpoint, `Idempotency-Key` và payload trả lại đúng Payment cũ. Cùng
key nhưng payload khác trả `409 IDEMPOTENCY_KEY_REUSED`.

## Provider API

| Method | Endpoint | Tính chất |
|---|---|---|
| GET | `/api/payments/callback/vnpay` | IPN có thẩm quyền, HMAC-SHA512 |
| POST | `/api/payments/callback/momo` | IPN có thẩm quyền, HMAC-SHA256 |
| GET | `/api/payments/return/vnpay` | Xác minh chữ ký và redirect |
| GET | `/api/payments/return/momo` | Xác minh chữ ký và redirect |
| POST | `/api/payments/mock/{paymentPublicId}/complete` | Local/test, owner-only |

Return URL không cập nhật Payment hoặc Booking. Frontend nhận
`paymentPublicId`, sau đó polling API trạng thái. Callback trùng hoàn toàn được
ACK idempotent; cùng deduplication key nhưng payload khác tạo đối soát.

## CASH tại quầy

Quyền: `EMPLOYEE`, `SUPERVISOR`, `ADMIN`.

| Method | Endpoint |
|---|---|
| GET | `/api/employee/payments/booking?reference={uuid-or-code}` |
| POST | `/api/employee/payments/cash` |
| GET | `/api/employee/payments/{paymentPublicId}` |
| POST | `/api/employee/payments/{paymentPublicId}/cash/collect` |
| POST | `/api/employee/payments/{paymentPublicId}/cash/cancel` |

Request tạo CASH nhận đúng một trong `bookingPublicId` hoặc `bookingCode`.
Collect nhận `{ "receivedAmount": 200000 }`; server kiểm tra deadline/payable,
đối chiếu số phải thu và tự tính tiền thừa.

## Admin và Accountant

Base path: `/api/admin/payments`.

- `GET /api/admin/payments`: danh sách và lọc.
- `GET /api/admin/payments/export`: xuất CSV.
- `GET /api/admin/payments/{paymentPublicId}`: chi tiết.
- `GET /api/admin/payments/webhooks|outbox|reconciliations`: hàng đợi vận hành.
- `POST /api/admin/payments/webhooks/{id}/replay`: replay webhook hợp lệ.
- `POST /api/admin/payments/outbox/{eventId}/replay`: replay cùng event ID.
- `POST /api/admin/payments/reconciliations/{publicId}/assign`.
- `POST /api/admin/payments/reconciliations/{publicId}/resolve`.
- `GET /api/admin/payments/refunds`: danh sách yêu cầu hoàn tiền.
- `GET /api/admin/payments/refunds/{refundPublicId}`: chi tiết hoàn tiền.
- `POST /api/admin/payments/{paymentPublicId}/refunds`: tạo hoàn toàn phần hoặc
  hoàn một phần; yêu cầu header `Idempotency-Key`.
- `POST /api/admin/payments/refunds/{refundPublicId}/retry`: thử lại khoản hoàn
  provider ở trạng thái cho phép.
- `POST /api/admin/payments/refunds/{refundPublicId}/cash/complete`: xác nhận nhân
  viên đã hoàn tiền mặt với mã biên nhận và ghi chú bắt buộc.

`ADMIN` được đọc/export/mutation. `ACCOUNTANT` chỉ được GET/export. Resolve bắt
buộc có mã xử lý và ghi chú.

Request hoàn tiền do Admin tạo:

```json
{
  "refundType": "PARTIAL",
  "refundComponent": "CONCESSION",
  "amount": 50000,
  "reasonCode": "CONCESSION_UNAVAILABLE",
  "note": "Quầy hết sản phẩm đã mua"
}
```

Quy tắc authoritative:

- `FULL + FULL_ORDER`: server tự lấy toàn bộ số tiền còn có thể hoàn.
- `PARTIAL`: chỉ nhận `CONCESSION`, `PRICE_DIFFERENCE` hoặc
  `OPERATIONAL_ADJUSTMENT`.
- Khoản `CONCESSION` không được vượt tiền bắp nước trong snapshot bất biến.
- Tổng các khoản đang chờ, đang xử lý, cần can thiệp và đã thành công không được
  vượt số tiền đã thu.
- `PARTIAL + FULL_ORDER` bị từ chối bằng `TICKET_REFUND_NOT_SUPPORTED`.
- Nếu trước đó đã hoàn một phần, yêu cầu hoàn toàn bộ phần còn lại được gửi tới
  provider dưới dạng partial refund an toàn.

Các trường hợp tự động hoàn toàn bộ phần còn lại:

- Provider báo SUCCESS sau deadline gốc (`LATE_PROVIDER_SUCCESS`).
- Booking không chấp nhận kết quả thanh toán thành công
  (`BOOKING_CONFIRMATION_FAILED`).
- Có giao dịch thu tiền thành công thứ hai cho cùng Booking
  (`DUPLICATE_CAPTURE`).
- Movie Service chuyển Showtime sang `CANCELLED` (`SHOWTIME_CANCELLED`).

Route nội bộ của Movie Service:

- `POST /internal/payments/refunds/showtimes/{showtimePublicId}`

Route này yêu cầu internal token, không đi qua Gateway và dùng `eventId` để chống
tạo trùng khi outbox Movie retry.

## Booking internal contract

Payment gọi trực tiếp Booking Service bằng internal token; các route này không
đi qua Gateway:

- `GET /internal/bookings/{bookingPublicId}/payment-context`
- `GET /internal/bookings/code/{bookingCode}/payment-context`
- `POST /internal/bookings/{bookingPublicId}/payment-results`

Context authoritative gồm owner, `amountLockedAt`, deadline, amount/currency và
snapshot analytics (`moviePublicId`, `movieTitle`, `showtimePublicId`,
`cinemaPublicId`, số lượng/tiền vé, bắp nước, giảm giá, tổng tiền).

Payment gửi refund đã xác nhận về cùng `payment-results` với kết quả
`REFUND_SUCCESS`. Booking cộng dồn các khoản refund thành công:

- Chưa đủ tổng tiền đơn: Booking vẫn `CONFIRMED`, vé và ghế vẫn `BOOKED`.
- Bằng đúng tổng tiền đơn: Booking chuyển `REFUNDED`.
- Không trường hợp nào giải phóng ghế đã `BOOKED`.

## Mã lỗi vận hành chính

| Code | HTTP | Ý nghĩa |
|---|---:|---|
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | Thiếu idempotency key |
| `IDEMPOTENCY_KEY_REUSED` | 409 | Cùng key nhưng payload khác |
| `PAYMENT_ATTEMPT_ACTIVE` | 409 | Booking đang có attempt hoạt động |
| `PAYMENT_PROVIDER_SESSION_ACTIVE` | 409 | Không thể hủy session provider đang hoạt động |
| `BOOKING_NOT_PAYABLE` | 409 | Booking chưa finalization, hết hạn hoặc không payable |
| `PAYMENT_AMOUNT_MISMATCH` | 409 | Callback không khớp số tiền |
| `PROVIDER_EVENT_CONFLICT` | 409 | Callback trùng khóa nhưng khác payload |
| `PAYMENT_RECONCILIATION_REQUIRED` | 409 | Cần xử lý đối soát |
| `BOOKING_SERVICE_UNAVAILABLE` | 503 | Chưa đọc được context authoritative |
| `REFUND_IDEMPOTENCY_KEY_REQUIRED` | 400 | Thiếu khóa chống tạo hoàn tiền trùng |
| `REFUND_IDEMPOTENCY_CONFLICT` | 409 | Cùng khóa nhưng nội dung hoàn tiền khác |
| `PAYMENT_NOT_REFUNDABLE` | 409 | Giao dịch chưa thanh toán thành công |
| `REFUND_AMOUNT_EXCEEDS_AVAILABLE` | 409 | Vượt số tiền còn có thể hoàn |
| `TICKET_REFUND_NOT_SUPPORTED` | 409 | Chưa hỗ trợ hoàn riêng từng vé |
| `CASH_REFUND_REQUIRES_MANUAL_SETTLEMENT` | 409 | Hoàn tiền mặt cần xử lý tại quầy |
