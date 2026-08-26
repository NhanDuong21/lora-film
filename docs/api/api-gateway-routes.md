# API Gateway Routes Specification

## 1. Thông Tin Chung

| Mục             | Nội dung                                                    |
| --------------- | ----------------------------------------------------------- |
| Service         | `api-gateway`                                               |
| Feature         | Secure Gateway Routing for Auth Service and User Service    |
| API liên quan   | Auth API, User API, Internal API Policy                     |
| Issue liên quan | Secure Internal Routes and Review API Gateway Configuration |
| Người phụ trách | Trương Hoàng Khang                                          |
| Trạng thái      | Ready for Review / Testing                                  |
| Branch          | `fix/issue-secure-internal-gateway-routes`                  |
| Ngày cập nhật   | 19/06/2026                                                  |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này mô tả cấu hình route của **API Gateway** cho hệ thống **LoraFilm / Movie Booking System**.

Mục tiêu chính:

* Đảm bảo Frontend chỉ gọi **một base URL duy nhất** thông qua API Gateway.
* Route các request Authentication từ Frontend đến `auth-service`.
* Route các request User/Profile từ Frontend đến `user-service`.
* Phân loại rõ route theo nhóm: **Public API**, **Protected API**, **Internal API**.
* Đảm bảo các endpoint nội bộ như `/internal/users/**` không bị expose qua API Gateway.
* Làm tài liệu tham chiếu để Frontend, Backend và Tester thống nhất cách gọi API.
* Hỗ trợ test login/register/profile và kiểm tra bảo mật internal route qua Gateway.

---

## 3. Tổng Quan Kiến Trúc Route

### 3.1. API Gateway URL

Frontend chỉ gọi API qua Gateway:

```txt
http://localhost:8080
```

### 3.2. Service Direct URL

Các URL dưới đây chỉ dùng cho Backend debug hoặc test riêng từng service:

```txt
Auth Service: http://localhost:8081
User Service: http://localhost:8086
```

Frontend không được gọi trực tiếp các service port này.

### 3.3. Request Flow Chính

```txt
React Frontend
→ API Gateway :8080
→ Auth Service :8081 / User Service :8086
→ MySQL
```

### 3.4. Internal Backend Flow

Các internal flow không đi qua Frontend và không được expose qua API Gateway.

Ví dụ tạo user profile sau khi đăng ký tài khoản:

```txt
Auth Service
→ Kafka ACCOUNT_CREATED event
→ User Service
→ Create User Profile
```

Hoặc trong trường hợp backend-to-backend call trực tiếp:

```txt
Auth Service
→ http://localhost:8086/internal/users
→ User Service
```

Lưu ý: Direct internal call chỉ dành cho backend/internal flow. Frontend và external client không được gọi endpoint này.

---

## 4. Danh Sách Port Local

| Thành phần           | Port | Base URL / Discovery     | Ghi chú                                              |
| -------------------- | ---- | ------------------------ | ---------------------------------------------------- |
| Eureka Server        | 8761 | `http://localhost:8761`  | Netflix Eureka Service Discovery Dashboard           |
| API Gateway          | 8080 | `http://localhost:8080`  | Frontend gọi API qua Gateway                         |
| Auth Service         | 8081 | `lb://auth-service`      | Xử lý register, login, JWT/authentication            |
| Movie Service        | 8082 | `lb://movie-service`     | Quản lý phim, lịch chiếu, phòng chiếu                |
| Booking Service      | 8083 | `lb://booking-service`   | Quản lý đặt vé, giữ ghế                              |
| Payment Service      | 8084 | `lb://payment-service`   | Xử lý thanh toán                                     |
| Notification Service | 8085 | `lb://notification-service` | Gửi email, thông báo                             |
| User Service         | 8086 | `lb://user-service`      | Xử lý user profile và user management                |
| Promotion Service    | 8087 | `lb://promotion-service` | Quản lý khuyến mãi, voucher                          |
| Score Service        | 8088 | `lb://score-service`     | Quản lý điểm thưởng, thành viên                      |
| Analytics Service    | 8089 | `lb://analytics-service` | Thống kê, báo cáo doanh thu                          |
| Frontend             | 5173 | `http://localhost:5173`  | React/Vite local dev server                          |
| MySQL                | 3307 | `localhost:3307`         | Docker host port map tới MySQL container port `3306` |
| Kafka                | 9092 | `localhost:9092`         | Event broker cho backend internal flow               |
| Redis                | 6379 | `localhost:6379`         | Cache/session/token nếu service sử dụng              |

Lưu ý: Trong Docker, MySQL container dùng port `3306`, nhưng máy host truy cập qua port `3307` nếu docker compose đang map `3307:3306`. Gateway định tuyến động qua Eureka bằng giao thức `lb://<service-name>`.

---

## 5. Route Classification

| Route                  | Target Service (Eureka ID) | Type                            | Exposed via Gateway | Authentication        | Expected                                         |
| ---------------------- | -------------------------- | ------------------------------- | ------------------- | --------------------- | ------------------------------------------------ |
| `/api/auth/**`         | `auth-service`             | Public / Protected tùy endpoint | Yes                 | Tùy endpoint          | Register/Login hoạt động qua Gateway             |
| `/api/users/**`        | `user-service`             | Protected API                   | Yes                 | Bearer JWT            | User/Profile API hoạt động qua Gateway           |
| `/api/movies/**`       | `movie-service`            | Public / Protected tùy endpoint | Yes                 | Tùy endpoint          | Quản lý và tra cứu phim qua Gateway              |
| `/api/promotions/**`   | `promotion-service`        | Public / Protected tùy endpoint | Yes                 | Tùy endpoint          | Khuyến mãi / Voucher hoạt động qua Gateway       |
| `/api/scores/**`       | `score-service`            | Protected API                   | Yes                 | Bearer JWT            | Tra cứu & tích điểm hoạt động qua Gateway        |
| `/api/bookings/**`     | `booking-service`          | Protected API                   | Yes                 | Bearer JWT            | Đặt vé xem phim hoạt động qua Gateway            |
| `/api/payments/**`     | `payment-service`          | Protected API                   | Yes                 | Bearer JWT            | Thanh toán hoạt động qua Gateway                 |
| `/api/notifications/**`| `notification-service`     | Protected / Internal API        | Yes                 | Bearer JWT            | Quản lý thông báo qua Gateway                    |
| `/api/analytics/**`    | `analytics-service`        | Protected API (Admin)           | Yes                 | Bearer JWT (Admin)    | Báo cáo thống kê hoạt động qua Gateway           |
| `/internal/**`         | All Services               | Internal API                    | No                  | Internal backend only | Gateway trả `404 Not Found` hoặc `403 Forbidden` |

---

## 6. Quy Tắc Gọi API Từ Frontend

Frontend **chỉ được gọi API thông qua API Gateway**.

### 6.1. Đúng

```txt
POST http://localhost:8080/api/auth/register
POST http://localhost:8080/api/auth/login
POST http://localhost:8080/api/auth/verify
POST http://localhost:8080/api/auth/refresh-token
GET  http://localhost:8080/api/users/{accountId}
```

### 6.2. Không Đúng

Frontend không gọi trực tiếp service port:

```txt
POST http://localhost:8081/api/auth/register
POST http://localhost:8081/api/auth/login
GET  http://localhost:8086/api/users/{accountId}
```

Frontend không gọi internal API:

```txt
POST http://localhost:8080/internal/users
POST http://localhost:8086/internal/users
```

Các direct service URL chỉ dùng cho Backend developer debug hoặc test service độc lập.

---

## 7. Public API, Protected API, Internal API

### 7.1. Public API

Public API là API được phép gọi từ Frontend hoặc external client mà không cần đăng nhập, nếu nghiệp vụ cho phép.

Ví dụ:

```txt
POST /api/auth/register
POST /api/auth/login
POST /api/auth/verify
```

### 7.2. Protected API

Protected API là API được phép gọi từ Frontend hoặc external client thông qua Gateway, nhưng phải có token hợp lệ.

Ví dụ:

```txt
GET /api/users/{accountId}
PUT /api/users/{accountId}
GET /api/users/me
```

Các request protected API cần gửi header:

```txt
Authorization: Bearer <authToken>
```

### 7.3. Internal API

Internal API là API chỉ dành cho backend/internal flow, không dành cho Frontend hoặc external client.

Ví dụ:

```txt
POST /internal/users
```

Internal API không được expose qua API Gateway.

Nếu backend cần tạo user profile sau khi đăng ký tài khoản, flow ưu tiên là Kafka event:

```txt
Auth Service
→ publish ACCOUNT_CREATED event
→ Kafka
→ User Service consume event
→ create user profile
```

Nếu cần backend-to-backend call trực tiếp, service phải gọi direct service URL hoặc internal network URL, không đi qua Gateway public route.

---

## 8. Route `/api/auth/**` Đến Auth Service

### 8.1. Mục Tiêu

Route này dùng để chuyển request liên quan đến Authentication từ API Gateway sang `auth-service`.

### 8.2. Route Mapping

| Gateway Path   | Target Service | Target URL              | Type                            | Mô tả                                                         |
| -------------- | -------------- | ----------------------- | ------------------------------- | ------------------------------------------------------------- |
| `/api/auth/**` | `auth-service` | `http://localhost:8081` | Public / Protected tùy endpoint | Register, verify OTP, login, refresh token, auth-related APIs |

### 8.3. Endpoint Chính

| Method | Gateway Endpoint          | Target Endpoint                                | Type                    | Mô tả                |
| ------ | ------------------------- | ---------------------------------------------- | ----------------------- | -------------------- |
| POST   | `/api/auth/register`      | `http://localhost:8081/api/auth/register`      | Public                  | Đăng ký tài khoản    |
| POST   | `/api/auth/verify`        | `http://localhost:8081/api/auth/verify`        | Public                  | Xác thực OTP         |
| POST   | `/api/auth/login`         | `http://localhost:8081/api/auth/login`         | Public                  | Đăng nhập            |
| POST   | `/api/auth/refresh-token` | `http://localhost:8081/api/auth/refresh-token` | Protected / Token-based | Làm mới access token |

### 8.4. Request Flow

```txt
Frontend
→ POST http://localhost:8080/api/auth/login
→ API Gateway
→ http://localhost:8081/api/auth/login
→ Auth Service xử lý
→ API Gateway trả response về Frontend
```

---

## 9. Route `/api/users/**` Đến User Service

### 9.1. Mục Tiêu

Route này dùng để chuyển các request liên quan đến user profile hoặc user management từ API Gateway sang `user-service`.

Đây là **Protected API**, Frontend được gọi nhưng phải có JWT token hợp lệ.

### 9.2. Route Mapping

| Gateway Path    | Target Service | Target URL              | Type          | Mô tả                              |
| --------------- | -------------- | ----------------------- | ------------- | ---------------------------------- |
| `/api/users/**` | `user-service` | `http://localhost:8086` | Protected API | User profile, user management APIs |

### 9.3. Endpoint Dự Kiến

| Method | Gateway Endpoint         | Target Endpoint                               | Auth Required | Mô tả                             |
| ------ | ------------------------ | --------------------------------------------- | ------------- | --------------------------------- |
| GET    | `/api/users/{accountId}` | `http://localhost:8086/api/users/{accountId}` | Yes           | Lấy thông tin user theo accountId |
| GET    | `/api/users/me`          | `http://localhost:8086/api/users/me`          | Yes           | Lấy thông tin user hiện tại       |
| PUT    | `/api/users/{accountId}` | `http://localhost:8086/api/users/{accountId}` | Yes           | Cập nhật thông tin user           |

Lưu ý: Endpoint cụ thể phụ thuộc vào trạng thái implement của `user-service`. Nếu User Service chưa hoàn chỉnh, chỉ test các endpoint đã sẵn sàng.

### 9.4. Request Flow

```txt
Frontend
→ GET http://localhost:8080/api/users/{accountId}
→ API Gateway
→ http://localhost:8086/api/users/{accountId}
→ User Service xử lý
→ API Gateway trả response về Frontend
```

Request cần header:

```txt
Authorization: Bearer <authToken>
```

---

## 10. Internal Routes Policy

### 10.1. Mục Tiêu

Các endpoint `/internal/**` chỉ dành cho backend/internal flow.

API Gateway không được expose các route internal ra ngoài cho Frontend hoặc External Client gọi.

### 10.2. Internal User Route

| Path                 | Type         | Exposed via Gateway | Expected Gateway Result              |
| -------------------- | ------------ | ------------------- | ------------------------------------ |
| `/internal/users/**` | Internal API | No                  | `404 Not Found` hoặc `403 Forbidden` |

### 10.3. Quy Ước Sử Dụng

* Frontend không được gọi `/internal/users`.
* External client không được gọi `/internal/users` qua API Gateway.
* Không thêm route `/internal/**` vào API Gateway nếu chưa có security design rõ ràng.
* Nếu cần giữ route dev-only, route đó phải được bảo vệ bằng security filter, internal token hoặc profile cấu hình riêng.
* Production config không được expose `/internal/**`.
* Backend service nếu cần giao tiếp nội bộ thì dùng Kafka/event flow hoặc direct service URL trong internal network.

### 10.4. Expected Result Sau Khi Fix

Request:

```txt
POST http://localhost:8080/internal/users
```

Expected:

```txt
404 Not Found
```

hoặc:

```txt
403 Forbidden
```

Không được trả về:

```txt
200 OK
201 Created
```

### 10.5. Response Mẫu Nếu Dùng Internal Block Filter

Nếu Gateway có filter chặn `/internal/**`, response có thể là:

```json
{
  "success": false,
  "status": 403,
  "errorCode": "INTERNAL_API_NOT_EXPOSED",
  "message": "This endpoint is internal and is not exposed through API Gateway.",
  "path": "/internal/users",
  "timestamp": "2026-06-19T17:02:08.382Z"
}
```

Nếu không dùng filter, Gateway có thể trả `404 Not Found`. Cả hai kết quả đều hợp lệ theo acceptance criteria.

---

## 11. Cấu Hình Route Spring Cloud Gateway

Project hiện tại có thể dùng `application.properties` hoặc `application.yml`.

### 11.1. Cấu Hình `application.properties`

File:

```txt
api-gateway/src/main/resources/application.properties
```

Cấu hình route và Eureka Discovery:

```properties
server.port=8080
spring.application.name=api-gateway

# ===== Eureka Discovery Client =====
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# =========================================================
# Dynamic Routes using Eureka Load Balancer (lb://)
# =========================================================

# Route: auth-service (Port 8081)
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=lb://auth-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/auth/**

# Route: user-service (Port 8086)
spring.cloud.gateway.routes[1].id=user-service
spring.cloud.gateway.routes[1].uri=lb://user-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/users/**

# Route: movie-service (Port 8082)
spring.cloud.gateway.routes[2].id=movie-service
spring.cloud.gateway.routes[2].uri=lb://movie-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/movies/**, /api/genres/**, /api/admin/movies/**, /api/admin/genres/**, /api/admin/rooms/**, /api/admin/seats/**, /api/admin/showtimes/**, /api/showtimes/**, /api/rooms/**, /api/seats/**

# Route: promotion-service (Port 8087)
spring.cloud.gateway.routes[3].id=promotion-service
spring.cloud.gateway.routes[3].uri=lb://promotion-service
spring.cloud.gateway.routes[3].predicates[0]=Path=/api/promotions/**, /api/admin/promotions/**, /api/admin/promotion-campaigns/**

# Route: score-service (Port 8088)
spring.cloud.gateway.routes[4].id=score-service
spring.cloud.gateway.routes[4].uri=lb://score-service
spring.cloud.gateway.routes[4].predicates[0]=Path=/api/scores/**, /api/admin/scores/**

# Route: booking-service (Port 8083)
spring.cloud.gateway.routes[5].id=booking-service
spring.cloud.gateway.routes[5].uri=lb://booking-service
spring.cloud.gateway.routes[5].predicates[0]=Path=/api/bookings/**, /api/tickets/**

# Route: payment-service (Port 8084)
spring.cloud.gateway.routes[6].id=payment-service
spring.cloud.gateway.routes[6].uri=lb://payment-service
spring.cloud.gateway.routes[6].predicates[0]=Path=/api/payments/**, /api/vnpay/**

# Route: notification-service (Port 8085)
spring.cloud.gateway.routes[7].id=notification-service
spring.cloud.gateway.routes[7].uri=lb://notification-service
spring.cloud.gateway.routes[7].predicates[0]=Path=/api/notifications/**

# Route: analytics-service (Port 8089)
spring.cloud.gateway.routes[8].id=analytics-service
spring.cloud.gateway.routes[8].uri=lb://analytics-service
spring.cloud.gateway.routes[8].predicates[0]=Path=/api/analytics/**, /api/admin/reports/**

# =========================================================
# CORS Configuration
# =========================================================
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowedOrigins=http://localhost:5173,http://localhost:5174
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowedMethods=GET,POST,PUT,PATCH,DELETE,OPTIONS
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowedHeaders=*
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowCredentials=true

spring.cloud.gateway.default-filters[0].name=DedupeResponseHeader
spring.cloud.gateway.default-filters[0].args.name=Access-Control-Allow-Origin Access-Control-Allow-Credentials
spring.cloud.gateway.default-filters[0].args.strategy=RETAIN_UNIQUE
```

Không được cấu hình route sau:

```properties
spring.cloud.gateway.routes[x].id=internal-user-service
spring.cloud.gateway.routes[x].uri=lb://user-service
spring.cloud.gateway.routes[x].predicates[0]=Path=/internal/users/**
```

### 11.2. Cấu Hình `application.yml`

Nếu project dùng YAML, cấu hình tương đương:

```yml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**

        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**

        - id: movie-service
          uri: lb://movie-service
          predicates:
            - Path=/api/movies/**, /api/genres/**, /api/admin/movies/**, /api/admin/genres/**, /api/admin/rooms/**, /api/admin/seats/**, /api/admin/showtimes/**, /api/showtimes/**, /api/rooms/**, /api/seats/**

        - id: promotion-service
          uri: lb://promotion-service
          predicates:
            - Path=/api/promotions/**, /api/admin/promotions/**, /api/admin/promotion-campaigns/**

        - id: score-service
          uri: lb://score-service
          predicates:
            - Path=/api/scores/**, /api/admin/scores/**

        - id: booking-service
          uri: lb://booking-service
          predicates:
            - Path=/api/bookings/**, /api/tickets/**

        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**, /api/vnpay/**

        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/notifications/**

        - id: analytics-service
          uri: lb://analytics-service
          predicates:
            - Path=/api/analytics/**, /api/admin/reports/**

      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:5173"
              - "http://localhost:5174"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true

      default-filters:
        - name: DedupeResponseHeader
          args:
            name: Access-Control-Allow-Origin Access-Control-Allow-Credentials
            strategy: RETAIN_UNIQUE
```

Không được thêm route sau:

```yml
- id: internal-user-service
  uri: http://localhost:8086
  predicates:
    - Path=/internal/users/**
```

---

## 12. Cấu Hình CORS Cho Frontend Local

### 12.1. Mục Tiêu

Cho phép Frontend local tại `http://localhost:5173` hoặc `http://localhost:5174` gọi API qua Gateway `http://localhost:8080`.

### 12.2. CORS Origin Được Cho Phép

```txt
http://localhost:5173
http://localhost:5174
```

### 12.3. Headers Được Cho Phép

```txt
Content-Type
Authorization
```

Hiện tại local config có thể dùng:

```txt
*
```

Tuy nhiên production config không nên dùng wildcard nếu không cần thiết.

### 12.4. Methods Được Cho Phép

```txt
GET, POST, PUT, PATCH, DELETE, OPTIONS
```

Nếu sau này API cần `PATCH`, có thể bổ sung `PATCH` vào allowed methods.

### 12.5. Production CORS Rule

Trong production config:

* Không dùng wildcard origin nếu không cần thiết.
* Chỉ allow domain frontend chính thức.
* Giữ `allowCredentials=true` nếu frontend cần gửi credentials/token theo policy.
* Kiểm tra không làm hỏng Register/Login/Profile flow.

---

## 13. Test Auth API Qua Gateway

### 13.1. Điều Kiện Trước Khi Test

Đảm bảo các service đã chạy:

```bash
cd server/auth-service
mvn spring-boot:run
```

```bash
cd api-gateway
mvn spring-boot:run
```

Kiểm tra port:

```txt
Auth Service: http://localhost:8081
API Gateway : http://localhost:8080
```

### 13.2. Test Register Qua Gateway

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nguyen Van A",
    "email": "user@example.com",
    "phoneNumber": "0901234567",
    "cccd": "092205006789",
    "birthday": "2005-06-12",
    "password": "User@123"
  }'
```

Expected:

```txt
200 OK
```

hoặc theo implementation hiện tại:

```txt
201 Created
```

Response mẫu:

```json
{
  "success": true,
  "message": "Register successfully",
  "data": {
    "accountId": 1,
    "email": "user@example.com",
    "role": "CUSTOMER"
  }
}
```

### 13.3. Test Login Qua Gateway

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "User@123"
  }'
```

Expected:

```txt
200 OK
```

Response mẫu:

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "token": "jwt-token-here",
    "accessToken": "jwt-token-here",
    "refreshToken": "refresh-token-here",
    "tokenType": "Bearer",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "expiresIn": 86400
  }
}
```

---

## 14. Test User API Qua Gateway

### 14.1. Điều Kiện Trước Khi Test

Đảm bảo các service đã chạy:

```bash
cd server/user-service
mvn spring-boot:run
```

```bash
cd api-gateway
mvn spring-boot:run
```

Kiểm tra port:

```txt
User Service: http://localhost:8086
API Gateway : http://localhost:8080
```

### 14.2. Test User Profile API Cần Token

Dùng token lấy từ Login API:

```bash
curl -X GET http://localhost:8080/api/users/15 \
  -H "Authorization: Bearer <jwt-token-here>"
```

Expected:

```txt
200 OK
```

Response mẫu:

```json
{
  "success": true,
  "message": "User profile retrieved successfully",
  "data": {
    "accountId": 15,
    "fullName": "Nguyen Van A",
    "phoneNumber": "0901234567",
    "gender": "MALE",
    "birthday": "2005-06-12",
    "cccdMasked": "092******789",
    "provinceName": "Cần Thơ",
    "birthYear": 2005,
    "verifiedPhone": false
  }
}
```

Lưu ý: Nếu User Service chưa sẵn sàng hoặc chưa có data user tương ứng, ghi rõ trong MR/test note.

---

## 15. Test Internal Route Bị Chặn

### 15.1. Mục Tiêu

Đảm bảo `/internal/users/**` không còn public qua API Gateway.

### 15.2. Request Test

```bash
curl -X POST http://localhost:8080/internal/users \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": 999,
    "fullName": "Unauthorized Test",
    "phoneNumber": "0900000000"
  }'
```

### 15.3. Expected Result

Kết quả hợp lệ:

```txt
404 Not Found
```

hoặc:

```txt
403 Forbidden
```

Không được trả về:

```txt
200 OK
201 Created
```

### 15.4. Ý Nghĩa

Nếu response là `404 Not Found`, nghĩa là Gateway không có route `/internal/users/**`.

Nếu response là `403 Forbidden`, nghĩa là Gateway có security filter chủ động chặn `/internal/**`.

Cả hai đều hợp lệ theo security requirement.

Nếu response là `200 OK` hoặc `201 Created`, nghĩa là internal route vẫn đang bị expose qua Gateway và issue chưa được fix.

---

## 16. Forbidden String Check

### 16.1. Kiểm Tra Frontend Không Gọi Internal API

PowerShell:

```powershell
Get-ChildItem -Path client\src -Recurse -File |
Select-String -SimpleMatch -Pattern "/internal/users"
```

Expected:

```txt
No result
```

### 16.2. Kiểm Tra Frontend Không Gọi Trực Tiếp Service Port

PowerShell:

```powershell
Get-ChildItem -Path client\src -Recurse -File |
Select-String -SimpleMatch -Pattern "localhost:8081","localhost:8086"
```

Expected:

```txt
No result
```

### 16.3. Kiểm Tra Gateway Không Còn Internal Route

PowerShell:

```powershell
Get-ChildItem -Path api-gateway\src\main\resources -Recurse -File |
Select-String -SimpleMatch -Pattern "Path=/internal/users", "/internal/users/**", "internal-user-service"
```

Expected:

```txt
No result
```

Nếu các chuỗi này chỉ xuất hiện trong tài liệu Markdown để mô tả policy/test case thì không sao. Không được xuất hiện trong file config chạy thật như `application.properties` hoặc `application.yml`.

---

## 17. Sprint 2 Service Route Pattern

Các service route Sprint 2 chỉ được expose khi API đã có service contract rõ ràng.

Pattern dự kiến:

```txt
/api/movies/**
/api/bookings/**
/api/payments/**
/api/promotions/**
/api/scores/**
/api/notifications/**
/api/analytics/**
```

Quy tắc:

* Chỉ expose API public/protected.
* Không expose `/internal/**`.
* Mỗi route mới phải có owner/service contract.
* Protected API phải có security design rõ ràng.
* Route mới phải được cập nhật vào tài liệu này.

Không được expose:

```txt
/internal/**
```

---

## 18. Route Review Checklist

Trước khi thêm route mới vào Gateway, cần kiểm tra:

```txt
[ ] Route có owner service rõ ràng
[ ] Route có service API contract
[ ] Route đã được phân loại Public / Protected / Internal
[ ] Public API không chứa dữ liệu nhạy cảm
[ ] Protected API có JWT/security design
[ ] Internal API không expose qua Gateway
[ ] Frontend có nhu cầu dùng route này
[ ] CORS impact đã được kiểm tra
[ ] Validation steps đã được cập nhật
[ ] Documentation đã được cập nhật
```

---

## 19. Acceptance Criteria

| STT | Tiêu chí                                                                           | Trạng thái |
| --- | ---------------------------------------------------------------------------------- | ---------- |
| 1   | Có file `docs/api/api-gateway-routes.md`                                           | Done       |
| 2   | Route `/internal/users/**` không còn public qua Gateway                            | Done       |
| 3   | External client gọi `/internal/users/**` nhận `404 Not Found` hoặc `403 Forbidden` | Done       |
| 4   | Route `/api/auth/**` đến `auth-service` port `8081`                                | To Verify  |
| 5   | Route `/api/users/**` đến `user-service` port `8086`                               | To Verify  |
| 6   | `/api/auth/login` vẫn hoạt động qua Gateway                                        | To Verify  |
| 7   | `/api/users/{accountId}` vẫn hoạt động qua Gateway với token                       | To Verify  |
| 8   | Frontend không gọi trực tiếp `localhost:8081` hoặc `localhost:8086`                | To Verify  |
| 9   | Frontend không gọi `/internal/users`                                               | To Verify  |
| 10  | CORS cho Frontend local `http://localhost:5173` không bị hỏng                      | To Verify  |
| 11  | Gateway routes được phân loại rõ Public / Protected / Internal                     | Done       |
| 12  | Documentation được cập nhật                                                        | Done       |
| 13  | E2E Auth/Register/Profile flow vẫn pass                                            | To Verify  |
| 14  | MR target vào `develop`                                                            | To Do      |

---

## 20. Notes Cho Frontend

Frontend nên cấu hình Axios base URL bằng biến môi trường:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Ví dụ tạo Axios instance:

```js
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json"
  }
});
```

Ví dụ gọi Login:

```js
api.post("/api/auth/login", {
  email: "user@example.com",
  password: "User@123"
});
```

Ví dụ gọi User API có token:

```js
api.get("/api/users/15", {
  headers: {
    Authorization: `Bearer ${localStorage.getItem("authToken")}`
  }
});
```

Frontend không được gọi:

```txt
http://localhost:8081
http://localhost:8086
/internal/users
```

---

## 21. Scope Chưa Bao Gồm

Các nội dung sau không nằm trong scope của issue này:

* Không implement service APIs Sprint 2.
* Không redesign authentication system.
* Không thay đổi Kafka event.
* Không implement service discovery.
* Không deploy production Gateway trong issue này.
* Không thêm rate limiting nếu chưa có requirement.
* Không implement đầy đủ JWT validation filter tại Gateway nếu chưa có issue riêng.

---

## 22. Ghi Chú Khi Merge Request

Branch thực hiện:

```txt
fix/issue-secure-internal-gateway-routes
```

MR target:

```txt
develop
```

MR nên ghi rõ:

```txt
Secure internal routes by removing /internal/users/** from API Gateway.
```

Checklist MR:

```txt
[ ] Đã xóa route /internal/users/** khỏi Gateway config
[ ] Đã đổi route /api/users/** thành index hợp lệ nếu dùng application.properties
[ ] Đã test POST http://localhost:8080/internal/users trả 404 hoặc 403
[ ] Đã test /api/auth/login vẫn hoạt động
[ ] Đã test /api/users/{accountId} vẫn hoạt động với token
[ ] Đã kiểm tra Frontend không gọi /internal/users
[ ] Đã kiểm tra Frontend không gọi trực tiếp localhost:8081/8086
[ ] Đã kiểm tra CORS với Frontend local
[ ] Đã cập nhật docs/api/api-gateway-routes.md
[ ] Đã cập nhật docs/api/user-service-api.md nếu cần
[ ] Đã cập nhật luồng auth phía client nếu cần
[ ] Đã cập nhật test auth/gateway nếu cần
[ ] MR target vào develop
```

---

## 23. Summary

API Gateway hiện chỉ expose các route public/protected cần thiết cho Frontend:

```txt
/api/auth/**
/api/users/**
```

Internal route sau đã bị loại khỏi API Gateway:

```txt
/internal/users/**
```

Kết quả mong đợi:

```txt
POST http://localhost:8080/internal/users
→ 404 Not Found
```

hoặc:

```txt
POST http://localhost:8080/internal/users
→ 403 Forbidden
```

Điều này đảm bảo internal/backend API không bị public ra ngoài thông qua Gateway, đồng thời Register/Login/Profile flow vẫn tiếp tục đi qua API Gateway theo đúng kiến trúc.
