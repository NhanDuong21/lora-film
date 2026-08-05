# Movie Booking System

Welcome to the online movie booking system project by Group 3. This repository contains the complete source code of the project including the Frontend (Client), Backend (Microservices Server), and API Gateway.

## Technology Stack

**Frontend (Client):**
- React 19 (with Vite)
- React Router DOM
- Axios

**Backend (Server & API Gateway):**
- Java 21
- Spring Boot (Microservices)
- Spring Cloud Gateway
- Maven 3.9+
- Database: MySQL 8+

## Project Structure

```text
hcm26_cpl_java_05_group3/
├── client/                 # Frontend application (React/Vite)
├── server/                 # Backend services (Java/Spring Boot)
│   ├── auth-service/       # User authentication service (JWT/OAuth2)
│   ├── movie-service/      # Service managing movies, cinemas, schedules
│   ├── booking-service/    # Booking processing service
│   ├── payment-service/    # Payment integration service
│   ├── notification-service/# Email/SMS notification service
│   ├── user-service/       # User profiles and management service
│   ├── analytics-service/  # Data insights and reporting service
│   ├── promotion-service/  # Coupons and discount service
│   └── score-service/      # Loyalty points and movie rating service
├── api-gateway/            # Intermediate API Gateway routing requests
└── docs/                   # Project documentation (workflow, structure...)
```
*(For more details, see [Project Structure Documentation](docs/project-structure.md))*

## Quy trình clone/pull và chạy local trên Windows

### 1. Chuẩn bị máy

Cài Java 21, Maven 3.9+, Node.js/npm, Docker Desktop và MySQL Workbench (hoặc
MySQL client). Mọi lệnh dưới đây chạy trong PowerShell tại thư mục gốc project.

### 2. Clone lần đầu

```powershell
git clone <repository_url>
cd hcm26_cpl_java_05_group3
git checkout develop
```

### 3. Quy trình pull code hằng ngày

Nếu đang sửa dở code, lưu thay đổi trước khi pull. Sau đó chạy đúng thứ tự:

```powershell
git status
git fetch origin
git checkout develop
git pull --ff-only origin develop
```

Nếu `git status` còn file đang sửa và chưa muốn commit, có thể cất tạm rồi lấy
lại sau khi pull:

```powershell
git stash push -u -m "wip-before-pull"
git pull --ff-only origin develop
git stash pop
```

Nếu pull báo conflict, xử lý từng file, chạy `git add`, rồi tiếp tục theo hướng
dẫn Git; không dùng `reset --hard` nếu chưa chắc chắn vì có thể mất thay đổi.

Các file `application.properties` và `.env` là file local bị ignore nên Git không
tự cập nhật chúng. Sau mỗi lần pull có thay đổi cấu hình, copy lại 11 file mẫu:

Tạo file môi trường cho Docker và khởi động MySQL, Redis, Kafka:

```powershell
Copy-Item .env.example .env -Force
docker compose up -d
docker compose ps
```

Copy 11 file cấu hình mẫu trước khi chạy Java service:

```powershell
$applications = @(
  'eureka-server', 'api-gateway',
  'server/auth-service', 'server/movie-service', 'server/booking-service',
  'server/payment-service', 'server/notification-service', 'server/user-service',
  'server/promotion-service', 'server/score-service', 'server/analytics-service'
)
foreach ($application in $applications) {
  Copy-Item "$application/src/main/resources/application.example.properties" `
            "$application/src/main/resources/application.properties" -Force
}
```

Spring Boot không tự đọc `.env` ở thư mục gốc; mỗi file được copy là cấu hình độc
lập. Các key JWT, token nội bộ, Cloudinary, TMDB, mail, VNPay/MoMo đã có sẵn trong
file example và vẫn cho phép ghi đè bằng biến môi trường. Nếu dùng tài khoản MySQL
khác, chỉ sửa `spring.datasource.username` và `spring.datasource.password` trong
9 file dưới `server/*-service`; không cần sửa Eureka hay API Gateway.

Tất cả 9 service dùng `spring.jpa.hibernate.ddl-auto=validate`, nên phải Execute 9
schema trong `docs/database/mysql/` trước lần chạy đầu tiên. Kết nối Workbench tới
`127.0.0.1:3307` bằng tài khoản trong `.env`, rồi chạy các file:

```text
analytics-service-schema.sql    auth-service-schema.sql
booking-service-schema.sql      movie-service-schema.sql
notification-service-schema.sql payment-service-schema.sql
promotion-service-schema.sql    score-service-schema.sql
user-service-schema.sql
```

Notification và Promotion có lệnh drop/recreate database; chỉ chạy trên database
local mới hoặc sao lưu dữ liệu trước. Các schema còn lại cũng nên chạy trên database
trống để tránh lỗi bảng đã tồn tại.

Khởi động theo thứ tự: `eureka-server` (8761), 9 service (8081–8089), rồi
`api-gateway` (8080). Mỗi module chạy ở một terminal riêng bằng `mvn spring-boot:run`.

## Chạy frontend

Mở terminal mới:

```bash
cd client
npm install
npm run dev
```

## Chạy backend và kiểm tra nhanh

Docker Compose chỉ cung cấp MySQL, Redis, Zookeeper và Kafka; các service Java
chạy ngoài Docker. Trong từng thư mục service, có thể biên dịch trước rồi chạy:

```bash
mvn clean compile
mvn spring-boot:run
```

Thứ tự cổng: Eureka `8761`; auth `8081`, movie `8082`, booking `8083`, payment
`8084`, notification `8085`, user `8086`, promotion `8087`, score `8088`, analytics
`8089`; Gateway `8080`. Kiểm tra Eureka tại `http://localhost:8761`, Gateway tại
`http://localhost:8080`, frontend tại `http://localhost:5173`.

## Docker Compose — hạ tầng dùng chung local

File `docker-compose.yml` ở thư mục gốc cung cấp:

- MySQL 8
- Redis
- Zookeeper
- Kafka

Các application service vẫn chạy bằng Java ở máy local. Compose chỉ chạy hạ tầng
và tạo database; schema phải Execute thủ công như hướng dẫn ở trên.

### Yêu cầu

- Đã cài Docker Desktop hoặc Docker Engine
- Đã bật Docker Compose
- Lệnh `docker` và `docker compose` chạy được trong terminal

### Thiết lập

1. Copy file môi trường (PowerShell dùng `Copy-Item .env.example .env`):

```powershell
Copy-Item .env.example .env -Force
```

2. Chỉ sửa các biến sau nếu muốn đổi tài khoản MySQL Docker:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER`
- `MYSQL_PASSWORD`

### Khởi động hạ tầng

```bash
docker compose up -d
```

### Dừng hạ tầng, giữ dữ liệu

```bash
docker compose down
```

### Xem log

```bash
docker compose logs -f
```

Hoặc xem riêng MySQL:

```bash
docker compose logs -f mysql
```

### Xóa container và volume (mất dữ liệu local)

```bash
docker compose down -v
```

### Kiểm tra hạ tầng

- MySQL: `localhost:3307`
- Redis: `localhost:6379`
- Zookeeper: `localhost:2181`
- Kafka: `localhost:9092`

Kiểm tra trạng thái trực tiếp:

```bash
docker compose ps
```
Sau khi Java service đã chạy, kiểm tra thêm `GET /health` (ví dụ
`http://localhost:8081/health`, `http://localhost:8086/health`).

Gateway tìm service qua Eureka và expose API tại `http://localhost:8080`.

## Basic Branching Rules

The project adopts a branch management process inspired by Git Flow:
- **`main`**: Production branch, contains the most stable source code. No direct pushing allowed.
- **`develop`**: Main integration branch. All feature branches must branch off from here.
- **Development Branch Prefixes**:
  - `feature/<issue-id>-<description>`: Develop a new feature.
  - `fix/<issue-id>-<description>`: Fix a bug.
  - `docs/<issue-id>-<description>`: Update documentation.
  - `setup/<issue-id>-<description>`: Configuration setup, CI/CD.
  - `test/<issue-id>-<description>`: Add/edit test cases.

## Merge Request (MR) Process

All code intended for `develop` must go through a Merge Request (MR).
1. Complete the code on your local branch, ensuring it runs well and passes tests.
2. Push the branch to GitLab.
3. Create an MR with a clear title (following Conventional Commits standards), with `develop` as the target branch.
4. Write a detailed description for the MR and link the corresponding Issue (e.g., `Closes #1`).
5. Assign the Team Leader/Reviewer (Thành) to review the code.
6. Once the MR is approved and passes the pipeline, it will be merged into `develop`.

*(For more details, see [GitLab Workflow Guidelines](docs/gitlab-workflow.md))*

## Team Members

- **Phan Tuấn Thành** - Team Leader / Developer
- **Dương Thiện Nhân** - Member / Developer
- **Trần Hiển Vinh** - Member / Developer
- **Trương Hoàng Khang** - Member / Developer
- **Trần Lương Thiện Hoàn** - Member / Developer
---
**Note:** See detailed rules regarding Git, commits, and workflows in [docs/gitlab-workflow.md](docs/gitlab-workflow.md).
