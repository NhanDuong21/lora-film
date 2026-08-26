# Promotion Service

Promotion Service quản lý campaign, promotion/voucher, claim của khách hàng,
reservation trong luồng booking, lifecycle scheduler, outbox event và monitoring.
Lớp smart orchestration bổ sung playbook đã duyệt, run bất biến, audience
snapshot/member và issue job bất đồng bộ mà không thay transaction core.
Nội dung quảng bá ra homepage/Promotion Center được quản lý độc lập qua
`CampaignPresentation`. Promotion template tiếp tục chỉ sở hữu luật ưu đãi;
ảnh được lưu ngoài MySQL và database chỉ giữ URL/storage key cùng metadata.
Service chạy tại cổng `8087` và sở hữu database `promotion_db`.

Cloudinary là provider ảnh mặc định và dùng chung các biến môi trường
`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` của
project. Ảnh promotion nằm trong folder `lorafilm/promotions/campaigns`; MySQL
chỉ lưu secure URL và Cloudinary public ID. Chỉ đặt
`PROMOTION_ASSET_PROVIDER=local` khi cần chạy offline.

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
- Áp dụng `docs/database/mysql/schema/promotion-service-schema.sql` từ thư mục gốc.
- Schema canonical đã bao gồm smart orchestration, campaign presentation và mọi
  cấu trúc hiện hành. Repository không nạp dữ liệu demo và không hỗ trợ nâng cấp
  in-place database cũ.
- Giữ token nội bộ giữa Booking/Payment và Promotion đồng bộ qua biến môi
  trường; không dùng giá trị local mặc định ở môi trường chia sẻ.

Hibernate dùng `ddl-auto=validate` và không tự cập nhật schema.

## Guardrail của smart orchestration

- Approval gắn với `playbookVersion` và SHA-256 của cả playbook, campaign và
  promotion. Cấu hình đổi sau lúc submit/approve sẽ bị chặn cho đến khi gửi
  duyệt lại; benefit tự động không có mức chi phí tối đa cũng bị từ chối.
- `budgetLimit` là ngân sách theo tháng. Worker khóa row playbook và audience
  member khi giữ ngân sách/quota, nên nhiều worker không thể cùng chi vượt trần;
  retry cùng member không giữ tiền lần hai.
- Refund của First-to-Second chỉ revoke ngay khi voucher còn AVAILABLE và chưa
  có reservation. Reservation đang hoạt động chuyển thành `REVOCATION_PENDING`;
  voucher đã dùng được giữ nguyên lịch sử và chuyển run sang review anomaly.
- Run lưu `approvedConfigHash`, `authorizedBy` và chạy với actor `SYSTEM`; audit
  vẫn phân biệt maker, checker và lần chạy hệ thống.

## Phạm vi API

| Nhóm | Prefix tiêu biểu |
|---|---|
| Customer promotion và wallet | `/api/promotions/**`, `/api/customers/me/promotions/**`, `/api/customers/me/promotion-history` |
| Manager promotion theo rạp | `/api/manager/promotions/workspace`, `/campaigns`, `/automations`, `/distribution-options`, `/incidents` |
| Admin campaign/promotion | `/api/admin/promotion-campaigns/**`, `/api/admin/promotions/**` |
| Smart orchestration | `/api/admin/promotion-opportunities`, `/api/admin/promotion-playbooks/**`, `/api/admin/promotion-runs/**` |
| Admin reservation/config/event/monitoring | `/api/admin/reservations/**`, `/api/admin/configurations/**`, `/api/admin/events/**`, `/api/admin/promotion-monitoring/**`, `/api/admin/promotion-operations/**` |
| Booking/Payment integration | `/internal/runtime/**`, `/internal/reservations/**` |
| Internal operations | `/internal/events/**`, `/internal/schedulers/**`, `/internal/configurations/**` |

- OpenAPI JSON: `http://localhost:8087/v3/api-docs`
- Swagger UI: `http://localhost:8087/swagger-ui.html`
- Health: `http://localhost:8087/actuator/health`

Contract đầy đủ nằm tại
[`docs/api/promotion-service-api.md`](../../docs/api/promotion-service-api.md).
Code runtime và test tương ứng là nguồn hiện hành cho business rule.

## Cấu trúc code

```text
com.project.promotionservice/
├── automation/     # Playbook, run, audience snapshot/member và issue worker
├── promotion/      # Campaign, promotion và customer claim
├── reservation/    # Preview, reserve, confirm, release, reverse
├── integration/    # Kafka, outbox, scheduler và operational API
├── configuration/  # Dynamic configuration
└── common/         # Security, error, monitoring và shared infrastructure
```

Business logic nằm trong domain/application service; controller chỉ xử lý HTTP,
validation và authorization. Repository không được chứa policy nghiệp vụ. Mọi
thay đổi schema phải cập nhật canonical schema trong cùng Merge Request.
