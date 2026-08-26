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

Backend yêu cầu Java 21 và Maven 3.9+. MySQL 8, Redis, Kafka và Zookeeper cho
môi trường local được chạy bằng Docker Compose. Frontend yêu cầu phiên bản
Node.js tương thích với Vite 8 và npm.

## Cấu trúc repository

```text
.
├── client/          # React/Vite frontend
├── server/          # 9 Spring Boot microservice
├── api-gateway/     # Gateway tại cổng 8080
├── eureka-server/   # Service discovery tại cổng 8761
├── docker/          # MySQL init cho môi trường local
├── docs/            # API, canonical schema, ERD và quy trình GitLab
└── retrospective/   # Báo cáo retrospective theo sprint
```

Xem [mục lục tài liệu](docs/README.md) để tìm tài liệu theo chủ đề.

## Chạy local trên Windows

### 1. Yêu cầu trước khi bắt đầu

Cài đặt và bảo đảm các lệnh sau chạy được trong PowerShell:

- JDK 21: `java -version`.
- Maven 3.9+: `mvn -version`.
- Docker Desktop có Docker Compose: `docker compose version`.
- Node.js và npm tương thích với Vite 8: `node -v` và `npm -v`.

Docker Desktop phải được mở trước khi khởi động dự án. Các cổng `2181`, `3307`,
`6379`, `9092`, `9093`, `8080`-`8089` và `8761` cần đang trống.

### 2. Thiết lập lần đầu sau khi clone

#### 2.1. Copy các file cấu hình local

Chạy tại thư mục gốc bằng PowerShell:

```powershell
Copy-Item .env.example .env
Copy-Item client/.env.example client/.env

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

Các lệnh copy này chỉ cần chạy một lần. Nếu đã có cấu hình local riêng, không
copy đè lên các file đó.

Các file `.env`, `client/.env` và `application.properties` đã được Git ignore.
Không commit secret. Kiểm tra lại `.env` sau khi copy; các giá trị mẫu chỉ dành
cho local và phải được thay trước khi dùng ở môi trường khác. Những tính năng
tích hợp như gửi mail hoặc upload Cloudinary cần credential hợp lệ tương ứng.

Ba internal token phục vụ incident flow không có fallback trong cấu hình mẫu
và bắt buộc phải được cấp qua môi trường: `PROMOTION_TO_BOOKING_INTERNAL_TOKEN`,
`PROMOTION_TO_PAYMENT_ASSESSMENT_TOKEN` và `SHOWTIME_TO_PAYMENT_INTERNAL_TOKEN`.
`.env.example` đã cung cấp giá trị local đồng bộ giữa các service; không xóa hoặc
để trống các giá trị này. Service sẽ từ chối khởi động nếu một token bắt buộc
không được cấp.

#### 2.2. Khởi động hạ tầng Docker

```powershell
docker compose up -d
docker compose ps
```

Compose khởi động MySQL (`localhost:3307`), Redis (`6379`), Zookeeper (`2181`)
và Kafka (`9092`). Đợi MySQL chuyển sang trạng thái `healthy` trước khi khởi tạo
schema. Các Java service không chạy trong Compose.

#### 2.3. Khởi tạo database

Các service dùng `spring.jpa.hibernate.ddl-auto=validate`; Hibernate không tự
tạo bảng. Với database mới, chạy 9 file canonical trong
`docs/database/mysql/schema/` bằng MySQL Workbench hoặc MySQL client:

```text
analytics-service-schema.sql    auth-service-schema.sql
booking-service-schema.sql      movie-service-schema.sql
notification-service-schema.sql payment-service-schema.sql
promotion-service-schema.sql    score-service-schema.sql
user-service-schema.sql
```

Có thể dùng MySQL client nằm sẵn trong container để nạp toàn bộ schema mà không
cần cài MySQL client trên Windows. Chỉ chạy lệnh sau với database local mới hoặc
sau khi chủ động tạo lại dữ liệu local:

```powershell
Get-ChildItem docs/database/mysql/schema/*.sql |
  Sort-Object Name |
  ForEach-Object {
    Write-Host "Applying $($_.Name)..."
    Get-Content -LiteralPath $_.FullName -Raw |
      docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'

    if ($LASTEXITCODE -ne 0) {
      throw "Failed to apply $($_.Name)"
    }
  }
```

Nếu dùng MySQL Workbench hoặc MySQL client cài trên Windows, kết nối tới
`127.0.0.1:3307` bằng tài khoản root trong `.env` rồi chạy đủ 9 file trên.

Đọc file trước khi chạy: một số schema có câu lệnh tạo lại database. Repository
này chỉ hỗ trợ khởi tạo database mới từ canonical schema, không hỗ trợ nâng cấp
in-place một database thuộc revision cũ. Xem
[hướng dẫn database](docs/database/mysql/README.md).

### 3. Chọn cách khởi động backend

Sau khi hoàn tất thiết lập lần đầu và khởi tạo database, chọn một trong hai cách
dưới đây. Cách dùng script phù hợp để chạy nhanh toàn bộ backend; cách thủ công
phù hợp khi cần quan sát hoặc debug từng service.

#### Cách A — Dùng script start/stop/check (khuyến nghị)

Từ thư mục gốc, chạy toàn bộ backend:

```powershell
.\scripts\start-backend.ps1
```

`start-backend.ps1` sẽ:

1. Dừng các Java process cũ đang chiếm cổng của dự án.
2. Chạy `docker compose up -d` để bảo đảm hạ tầng đang hoạt động.
3. Nạp biến từ `.env` cho các Maven process.
4. Khởi động Eureka, API Gateway và 9 microservice ở chế độ nền.
5. Chờ các cổng sẵn sàng rồi tự chạy kiểm tra health/Eureka.

Vì script đã gọi Docker Compose nên ở những lần chạy sau không bắt buộc chạy
riêng `docker compose up -d` trước. Script không khởi tạo bảng database; bước
chạy canonical schema ở lần thiết lập đầu tiên vẫn là bắt buộc.

Có thể kiểm tra lại trạng thái bất cứ lúc nào bằng:

```powershell
.\scripts\check-dev.ps1
```

Lệnh kiểm tra backend trong repository hiện có tên `check-dev.ps1`. Script kiểm
tra Docker Compose, các cổng/health endpoint và trạng thái đăng ký trên Eureka.

Để dừng backend và cả hạ tầng Docker:

```powershell
.\scripts\stop-backend.ps1
```

Để chỉ dừng Java backend nhưng giữ MySQL, Redis, Kafka và Zookeeper chạy nhằm
khởi động nhanh hơn ở lần sau:

```powershell
.\scripts\stop-backend.ps1 -KeepInfrastructure
```

Các process Maven được chạy ẩn. Khi có lỗi, xem log tại:

```powershell
Get-ChildItem "$env:TEMP\lorafilm-backend-logs"
```

Nếu PowerShell chặn thực thi script, chạy với execution policy chỉ áp dụng cho
process hiện tại, ví dụ:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

#### Cách B — Khởi động thủ công từng module

Khởi động theo thứ tự:

1. `eureka-server`
2. 9 service trong `server/`
3. `api-gateway`

Maven/Spring Boot không tự đọc file `.env`. Trong **mỗi PowerShell terminal mới**,
đứng tại thư mục gốc và nạp `.env` trước khi chạy module:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
    Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2].Trim()
  }
}
```

Sau đó chạy mỗi module ở một terminal riêng. Eureka Server:

```powershell
mvn -f eureka-server/pom.xml spring-boot:run
```

Ví dụ với một service:

```powershell
mvn -f server/movie-service/pom.xml spring-boot:run
```

Với Gateway:

```powershell
mvn -f api-gateway/pom.xml spring-boot:run
```

Lặp lại lệnh service với 9 thư mục trong `server/`. Nhấn `Ctrl+C` tại từng
terminal để dừng module chạy thủ công. Có thể dùng `scripts/check-dev.ps1` để
kiểm tra toàn bộ backend sau khi khởi động.

Eureka UI có tại `http://localhost:8761`. Gateway health có tại
`http://localhost:8080/health`. Movie, Booking, Notification, Promotion, Score
và Analytics dùng `/actuator/health`; Payment và User cung cấp `/health` riêng.
Auth Service hiện được kiểm tra qua startup log và trạng thái đăng ký trên
Eureka vì chưa có health endpoint riêng.

### 4. Khởi động frontend

Backend script không khởi động frontend. Mở một terminal khác và chạy:

```powershell
cd client
npm ci
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
