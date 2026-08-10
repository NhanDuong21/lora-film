# Tài liệu dự án LoraFilm

Đây là điểm bắt đầu cho tài liệu của repository. Tài liệu được chia theo mục
đích sử dụng để người đọc không phải dựa vào các báo cáo audit hoặc kế hoạch đã
hoàn thành.

## Bắt đầu phát triển

- [Hướng dẫn chạy toàn hệ thống](../README.md)
- [Frontend](../client/README.md)
- [Backend microservices](../server/README.md)
- [Quy trình GitLab](gitlab-workflow.md)
- [Schema và migration](database/README.md)

## Kiến trúc và dữ liệu

- [Thiết kế tổng thể](architecture/system-design.md)
- [Biện luận kiến trúc](architecture/architecture-advice.md)
- [Chuẩn sequence diagram](architecture/sequence-diagrams.md)
- [Physical ERD theo service](erd/README.md)
- `design/`: business rules và quyết định thiết kế theo service.
- `events/`: hợp đồng sự kiện liên service.

Các file `.drawio` trong `architecture/diagrams/services/` là nguồn để chỉnh sửa
sơ đồ service. Ảnh PNG trong `architecture/diagrams/` và `erd/physical/` là bản
render dùng trong tài liệu.

## API

| Thành phần | Tài liệu |
|---|---|
| API Gateway | [Route contract](api/api-gateway-routes.md) |
| Auth | [Auth API](api/auth-service-api.md) |
| User | [User API](api/user-api.md) |
| Movie | [Movie API](api/movie-service-api.md) |
| Booking | [Booking API](api/booking-service-api.md) |
| Payment | [Payment API](api/payment-service-api.md) |
| Promotion | [Promotion API](api/promotion-service-api.md) |
| Score | [Score API](api/score-service-api.md) |
| Notification | [Notification API](api/notification-service-api.md) |
| Analytics | [Analytics API](api/analytics-service-api.md) |

Khi tài liệu API khác với runtime, controller và OpenAPI của service đang chạy
là bằng chứng hiện hành. Cần cập nhật lại file contract trong cùng merge request
thay đổi endpoint.

## Vận hành, demo và kiểm thử

- `demo/`: các luồng demo và hướng dẫn sandbox/runtime.
- `notification/`: kiến trúc, bảo mật, provider, testing và runbook của
  Notification Service.
- `auto-schedule-refactor/`: API, kiến trúc, quyết định, rollout và rollback của
  Demand-Aware Auto Schedule.
- `test/`: test matrix còn dùng cho kiểm thử tích hợp hoặc kiểm thử tay.
- `client-audit/`: snapshot kiểm chứng frontend và checklist demo ngày
  2026-08-10; không dùng thay cho source code hoặc test tự động.
- `../retrospective/`: biên bản retrospective theo sprint.

Các tài liệu `auto-schedule-phase-s2.md` đến `auto-schedule-phase-s5.md` dưới
`design/movie-service/` là tham chiếu lịch sử cần giữ để giải thích cách replay
các preview đã lưu với strategy version cũ. Strategy mặc định hiện tại được mô
tả trong `auto-schedule-refactor/`.

## Nguồn sự thật

| Nội dung | Nguồn ưu tiên |
|---|---|
| Cấu hình local | `*.example.properties`, `.env.example`, `docker-compose.yml` |
| Schema hiện tại | `database/mysql/*-service-schema.sql` |
| Nâng cấp database đang có dữ liệu | `database/mysql/migrations/*.sql` theo thứ tự ngày |
| HTTP runtime | Controller và OpenAPI sinh từ service đang chạy |
| Route frontend | `client/src/routes/` và `client/src/features/**/routes.jsx` |
| Quyết định nghiệp vụ | Tài liệu trong `design/` và test tương ứng |

## Quy tắc duy trì

1. Dùng đường dẫn tương đối; không đưa đường dẫn tuyệt đối trên máy cá nhân vào
   tài liệu.
2. Không commit ảnh minh chứng nếu không đặt ảnh trong repository và liên kết
   bằng đường dẫn tương đối.
3. Kế hoạch/audit tạm thời phải được xóa sau khi hoàn thành hoặc chắt lọc quyết
   định còn giá trị vào tài liệu kiến trúc, API hay runbook.
4. Không ghi số lượng test đã pass như một cam kết lâu dài. Ghi lệnh kiểm tra để
   người đọc chạy lại trên phiên bản hiện tại.
5. Thay đổi endpoint, schema, biến môi trường hoặc quy trình vận hành phải cập
   nhật tài liệu liên quan trong cùng merge request.
