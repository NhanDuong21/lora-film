# LoraFilm Movie Booking System

LoraFilm là hệ thống đặt vé xem phim gồm React client, API Gateway, Eureka và
9 Spring Boot microservice. Các service dùng database riêng, giao tiếp qua HTTP
và Kafka; hạ tầng local được cung cấp bằng Docker Compose.

## Thành phần chính

| Thành phần | Công nghệ | Cổng mặc định |
|---|---|---:|
| Frontend | React 19, Vite 8, React Router 7 | 5173 |
| API Gateway | Spring Cloud Gateway | 8080 |
| Eureka Server | Spring Cloud Netflix Eureka | 8761 |
| Auth Service | Spring Boot 3.3.5 | 8081 |
| Movie Service | Spring Boot 3.3.5 | 8082 |
| Booking Service | Spring Boot 3.3.5 | 8083 |
| Payment Service | Spring Boot 3.3.5 | 8084 |
| Notification Service | Spring Boot 3.3.5 | 8085 |
| User Service | Spring Boot 3.3.5 | 8086 |
| Promotion Service | Spring Boot 3.3.5 | 8087 |
| Score Service | Spring Boot 3.3.5 | 8088 |
| Analytics Service | Spring Boot 3.3.5 | 8089 |

Backend yêu cầu Java 21, Maven 3.9+ và MySQL 8. Frontend yêu cầu phiên bản
Node.js tương thích với Vite 8 và npm.

## Cấu trúc repository

```text
.
├── client/          # React/Vite frontend
├── server/          # 9 Spring Boot microservice
├── api-gateway/     # Gateway tại cổng 8080
├── eureka-server/   # Service discovery tại cổng 8761
├── docker/          # MySQL init cho môi trường local
├── docs/            # API, kiến trúc, schema, runbook và test matrix
└── retrospective/   # Báo cáo retrospective theo sprint
```

Xem [mục lục tài liệu](docs/README.md) để tìm tài liệu theo chủ đề.

## Chạy local trên Windows

### 1. Chuẩn bị cấu hình

Chạy tại thư mục gốc bằng PowerShell:

```powershell
Copy-Item .env.example .env

$modules = @(
  'eureka-server',
  'api-gateway',
  'server/auth-service',
  'server/movie-service',
  'server/booking-service',
  'server/payment-service',
  'server/notification-service',
  'server/user-service',
  'server/promotion-service',
  'server/score-service',
  'server/analytics-service'
)

foreach ($module in $modules) {
  Copy-Item "$module/src/main/resources/application.example.properties" `
            "$module/src/main/resources/application.properties"
}
```

`application.properties` và `.env` là cấu hình local đã được Git ignore. Không
commit secret. Các giá trị trong file mẫu có thể được ghi đè bằng biến môi
trường; hãy thay toàn bộ credential mặc định trước khi dùng ngoài máy local.

Ba internal token phục vụ incident flow không có fallback trong cấu hình mẫu
và bắt buộc phải được cấp qua môi trường: `PROMOTION_TO_BOOKING_INTERNAL_TOKEN`,
`PROMOTION_TO_PAYMENT_ASSESSMENT_TOKEN` và `SHOWTIME_TO_PAYMENT_INTERNAL_TOKEN`.
Service sẽ từ chối khởi động nếu một token bắt buộc bị bỏ trống.

### 2. Khởi động hạ tầng

```powershell
docker compose up -d
docker compose ps
```

Compose khởi động MySQL (`localhost:3307`), Redis (`6379`), Zookeeper (`2181`)
và Kafka (`9092`). Java service không chạy trong Compose.

### 3. Khởi tạo database

Các service dùng `spring.jpa.hibernate.ddl-auto=validate`; Hibernate không tự
tạo bảng. Với database mới, chạy 9 file canonical trong
`docs/database/mysql/` bằng MySQL Workbench hoặc MySQL client:

```text
analytics-service-schema.sql    auth-service-schema.sql
booking-service-schema.sql      movie-service-schema.sql
notification-service-schema.sql payment-service-schema.sql
promotion-service-schema.sql    score-service-schema.sql
user-service-schema.sql
```

Đọc file trước khi chạy: một số schema có câu lệnh tạo lại database. Với môi
trường đã có dữ liệu, sao lưu trước và dùng các file phù hợp trong
`docs/database/mysql/migrations/` thay vì chạy lại schema một cách máy móc. Xem
[hướng dẫn database](docs/database/README.md).

### 4. Khởi động backend

Khởi động theo thứ tự:

1. `eureka-server`
2. 9 service trong `server/`
3. `api-gateway`

Mỗi module chạy ở một terminal riêng:

```powershell
cd eureka-server
mvn spring-boot:run
```

Ví dụ với một service:

```powershell
cd server/movie-service
mvn spring-boot:run
```

Với Gateway:

```powershell
cd api-gateway
mvn spring-boot:run
```

Eureka UI có tại `http://localhost:8761`. Gateway health có tại
`http://localhost:8080/health`. Movie, Booking, Notification, Promotion, Score
và Analytics dùng `/actuator/health`; Payment và User cung cấp `/health` riêng.
Auth Service hiện được kiểm tra qua startup log và trạng thái đăng ký trên
Eureka vì chưa có health endpoint riêng.

### 5. Khởi động frontend

```powershell
cd client
npm install
npm run dev
```

Mở `http://localhost:5173`. Vite proxy `/api`, OAuth callback và Socket.IO tới
Gateway tại `http://localhost:8080`. Xem [hướng dẫn frontend](client/README.md).

## Kiểm tra thay đổi

Frontend:

```powershell
cd client
npm test
npm run lint
npm run build
```

Backend được build/test độc lập theo module:

```powershell
cd server/<service-name>
mvn test
```

```powershell
cd api-gateway
mvn test
```

Một số integration test cần Docker đang chạy. Swagger UI của từng service dùng
`/swagger-ui.html` hoặc `/swagger-ui/index.html` tùy cấu hình Springdoc; OpenAPI
JSON dùng `/v3/api-docs`.

## Lệnh hạ tầng thường dùng

```powershell
docker compose logs -f
docker compose down
```

Lệnh sau xóa cả volume và toàn bộ dữ liệu local của Compose:

```powershell
docker compose down -v
```

## Quy trình Git

- `main`: nhánh ổn định; không push trực tiếp.
- `develop`: nhánh tích hợp; feature branch tách từ đây.
- Prefix khuyến nghị: `feature/`, `fix/`, `docs/`, `test/`, `setup/`.
- Mọi thay đổi vào `develop` đi qua Merge Request và phải build/test theo phạm vi
  ảnh hưởng.

Chi tiết nằm trong [GitLab workflow](docs/gitlab-workflow.md).

## Thành viên

- Phan Tuấn Thành — Team Leader / Developer
- Dương Thiện Nhân — Developer
- Trần Hiển Vinh — Developer
- Trương Hoàng Khang — Developer
- Trần Lương Thiện Hoàn — Developer
