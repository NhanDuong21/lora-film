# Promotion Service

Promotion Service quản lý campaign, promotion/voucher, claim của khách hàng,
reservation trong luồng booking, lifecycle scheduler, outbox event và monitoring.
Service chạy tại cổng `8087` và sở hữu database `promotion_db`.

## Chạy local

Từ thư mục này:

```powershell
Copy-Item src/main/resources/application.example.properties `
          src/main/resources/application.properties
mvn test
mvn spring-boot:run
```

Trước khi chạy, cần:

- MySQL, Redis, Kafka và Eureka đang hoạt động.
- Áp dụng `docs/database/mysql/promotion-service-schema.sql` từ thư mục gốc.
- Giữ token nội bộ giữa Booking/Payment và Promotion đồng bộ qua biến môi
  trường; không dùng giá trị local mặc định ở môi trường chia sẻ.

Hibernate dùng `ddl-auto=validate` và không tự cập nhật schema.

## Phạm vi API

| Nhóm | Prefix tiêu biểu |
|---|---|
| Customer promotion và wallet | `/api/promotions/**`, `/api/customers/me/promotions/**`, `/api/customers/me/promotion-history` |
| Admin campaign/promotion | `/api/admin/promotion-campaigns/**`, `/api/admin/promotions/**` |
| Admin reservation/config/event/monitoring | `/api/admin/reservations/**`, `/api/admin/configurations/**`, `/api/admin/events/**`, `/api/admin/promotion-monitoring/**`, `/api/admin/promotion-operations/**` |
| Booking/Payment integration | `/internal/runtime/**`, `/internal/reservations/**` |
| Internal operations | `/internal/events/**`, `/internal/schedulers/**`, `/internal/configurations/**` |

- OpenAPI JSON: `http://localhost:8087/v3/api-docs`
- Swagger UI: `http://localhost:8087/swagger-ui.html`
- Health: `http://localhost:8087/actuator/health`

Contract đầy đủ nằm tại
[`docs/api/promotion-service-api.md`](../../docs/api/promotion-service-api.md).
Các business rule và quyết định thiết kế nằm trong
[`docs/design/promotion-service/`](../../docs/design/promotion-service/).

## Cấu trúc code

```text
com.project.promotionservice/
├── promotion/      # Campaign, promotion và customer claim
├── reservation/    # Preview, reserve, confirm, release, reverse
├── integration/    # Kafka, outbox, scheduler và operational API
├── configuration/  # Dynamic configuration
└── common/         # Security, error, monitoring và shared infrastructure
```

Business logic nằm trong domain/application service; controller chỉ xử lý HTTP,
validation và authorization. Repository không được chứa policy nghiệp vụ. Mọi
thay đổi schema phải cập nhật canonical schema và migration phù hợp trong cùng
Merge Request.
