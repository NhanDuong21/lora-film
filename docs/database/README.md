# Quản lý schema MySQL

`docs/database/mysql/` là nguồn canonical cho database-per-service của LoraFilm.
Mỗi microservice sở hữu một schema; Hibernate chỉ kiểm tra mapping bằng
`spring.jpa.hibernate.ddl-auto=validate` và không tự tạo hay sửa bảng.

## Canonical schema

| File | Database |
|---|---|
| `auth-service-schema.sql` | `auth_db` |
| `movie-service-schema.sql` | `movie_db` |
| `booking-service-schema.sql` | `booking_db` |
| `payment-service-schema.sql` | `payment_db` |
| `notification-service-schema.sql` | `notification_db` |
| `user-service-schema.sql` | `user_db` |
| `promotion-service-schema.sql` | `promotion_db` |
| `score-service-schema.sql` | `score_db` |
| `analytics-service-schema.sql` | `analytics_db` |

Canonical schema dùng để dựng database mới ở trạng thái hiện tại. Luôn đọc phần
đầu file trước khi chạy vì một số script có thể drop/recreate database hoặc seed
dữ liệu demo.

## Migration

`mysql/migrations/` chứa thay đổi tăng dần cho database đã tồn tại. Tên file bắt
đầu bằng ngày `YYYYMMDD`; áp dụng theo thứ tự thời gian và chỉ chọn migration của
service/feature cần nâng cấp.

Quy trình an toàn:

1. Xác nhận đúng database và môi trường đích.
2. Sao lưu dữ liệu cần giữ.
3. Đọc precondition, câu lệnh destructive và yêu cầu phiên bản của migration.
4. Chạy migration theo thứ tự ngày, không chạy lại file nếu file không được thiết
   kế idempotent.
5. Khởi động service và kiểm tra health/log khi Hibernate validate schema.

Các migration không được Docker Compose hay Spring tự động thực thi.

## Khởi tạo local

Root `docker-compose.yml` tạo MySQL và các database ban đầu qua
`docker/mysql/init/`, nhưng không nạp bảng từ thư mục tài liệu này. Sau
`docker compose up -d`, dùng MySQL Workbench hoặc MySQL client để chạy canonical
schema cần thiết.

Ví dụ:

```powershell
Get-Content docs/database/mysql/movie-service-schema.sql -Raw |
  mysql.exe -h 127.0.0.1 -P 3307 -u root -p
```

## Ranh giới dữ liệu

- Không tạo foreign key vật lý hoặc query trực tiếp sang database của service
  khác.
- Tham chiếu liên service dùng public/business identifier theo contract, không
  dựa vào việc numeric ID tình cờ trùng nhau giữa các database.
- Tính nhất quán liên service được đảm bảo ở application layer, internal API,
  outbox/event và reconciliation job tương ứng.
- Mọi thay đổi bảng/index/constraint cần cập nhật entity, canonical schema,
  migration và tài liệu contract trong cùng Merge Request.
