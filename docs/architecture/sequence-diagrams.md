# Sequence diagrams

Thư mục `sequences/` mô tả luồng; ảnh render tương ứng nằm trong `diagrams/`.
Các sơ đồ là tài liệu kiến trúc hỗ trợ và phải được cập nhật khi contract liên
service thay đổi.

## Danh mục hiện có

| Luồng | Mô tả | Ảnh |
|---|---|---|
| Đăng ký | [register-sequence.md](sequences/register-sequence.md) | [PNG](diagrams/register-sequence.png) |
| Đăng nhập | [login-sequence.md](sequences/login-sequence.md) | [PNG](diagrams/login-sequence.png) |
| Đặt vé | [booking-sequence.md](sequences/booking-sequence.md) | [PNG](diagrams/booking-sequence.png) |
| Thanh toán | [payment-sequence.md](sequences/payment-sequence.md) | [PNG](diagrams/payment-sequence.png) |
| Promotion | [promotion-sequence.md](sequences/promotion-sequence.md) | [PNG](diagrams/promotion-sequence.png) |
| Score | [score-sequence.md](sequences/score-sequence.md) | [PNG](diagrams/score-sequence.png) |
| Notification và Analytics | [notification-analytics-sequence.md](sequences/notification-analytics-sequence.md) | [PNG](diagrams/notification-analytics-sequence.png) |

Luồng Promotion và Score được tách riêng vì hai service sở hữu domain và
database khác nhau. Contract liên quan nằm trong `docs/api/` và `docs/events/`.

## Quy ước

- Tên file dùng kebab-case và hậu tố `-sequence`.
- Participant phải dùng đúng boundary hiện tại: client, Gateway, owning service,
  database/cache và event broker khi thực sự tham gia.
- Thể hiện rõ lời gọi đồng bộ, event bất đồng bộ, transaction boundary và các
  nhánh lỗi quan trọng.
- Không mô tả service truy cập trực tiếp database của service khác.
- Không đưa secret, dữ liệu cá nhân hoặc URL môi trường thật vào sơ đồ.
- Markdown và ảnh PNG phải được cập nhật cùng nhau; chỉ liên kết asset đã commit
  bằng đường dẫn tương đối.

Draw.io source của các service diagram nằm trong `diagrams/services/`. Với
sequence diagram chỉ có PNG lịch sử, sửa nội dung Markdown trước và tạo lại ảnh
nguồn/render trong cùng Merge Request nếu luồng thay đổi đáng kể.

## Checklist review

- Endpoint, event name và owner khớp code/contract hiện tại.
- Success flow và failure flow quan trọng đều có mặt.
- Retry, idempotency và eventual consistency được thể hiện khi có liên quan.
- Thứ tự bước trong phần mô tả khớp hình.
- Tất cả liên kết nội bộ tồn tại.
