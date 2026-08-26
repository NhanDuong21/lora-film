# Tài liệu dự án LoraFilm

Đây là điểm bắt đầu cho tài liệu của repository. Tài liệu được chia theo mục
đích sử dụng để người đọc không phải dựa vào các báo cáo audit hoặc kế hoạch đã
hoàn thành.

## Bắt đầu phát triển

- [Hướng dẫn chạy toàn hệ thống](../README.md)
- [Frontend](../client/README.md)
- [Backend microservices](../server/README.md)
- [Quy trình GitLab](gitlab-workflow.md)
- [Canonical schema MySQL](database/README.md)

## Dữ liệu và sơ đồ

- [Canonical schema MySQL](database/README.md) dùng để khởi tạo database sạch.
- [Physical ERD theo service](erd/README.md) minh họa cấu trúc hiện tại.

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

## Nguồn sự thật

| Nội dung | Nguồn ưu tiên |
|---|---|
| Cấu hình local | `*.example.properties`, `.env.example`, `docker-compose.yml` |
| Schema hiện tại | `database/mysql/*-service-schema.sql` |
| Khởi tạo database mới | `database/mysql/*-service-schema.sql` |
| HTTP runtime | Controller và OpenAPI sinh từ service đang chạy |
| Route frontend | `client/src/routes/` và `client/src/features/**/routes.jsx` |
| Quy tắc nghiệp vụ | Code runtime và test tương ứng |

## Quy tắc duy trì

1. Dùng đường dẫn tương đối; không đưa đường dẫn tuyệt đối trên máy cá nhân vào
   tài liệu.
2. Không commit ảnh minh chứng nếu không đặt ảnh trong repository và liên kết
   bằng đường dẫn tương đối.
3. Kế hoạch/audit tạm thời phải được xóa sau khi hoàn thành; nội dung
   còn giá trị phải được chắt lọc vào API, database, ERD hoặc README phù hợp.
4. Không ghi số lượng test đã pass như một cam kết lâu dài. Ghi lệnh kiểm tra để
   người đọc chạy lại trên phiên bản hiện tại.
5. Thay đổi endpoint, schema, biến môi trường hoặc quy trình vận hành phải cập
   nhật tài liệu liên quan trong cùng merge request.
