# API Gateway Routes Specification

## 1. Thông Tin Chung

| Mục                | Nội dung                                           |
| ------------------ | -------------------------------------------------- |
| Service            | `api-gateway`                                      |
| Feature            | Gateway Routing for Auth Service and User Service  |
| API liên quan      | Auth API, User API                                 |
| Issue liên quan    | #53                                               |
| Người phụ trách    | Trương Hoàng Khang                                |
| Trạng thái         | Ready for Development / Testing                    |
| Branch             | `feature/issue-53-api-gateway-auth-user-routes`    |
| Ngày cập nhật      | 12/06/2026                                        |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này mô tả cấu hình route của **API Gateway** cho hệ thống **LoraFilm / Movie Booking System**.

Mục tiêu chính:

* Đảm bảo Frontend chỉ gọi **một base URL duy nhất** thông qua API Gateway.
* Route các request Authentication từ Frontend đến `auth-service`.
* Route các request User/Profile từ Frontend đến `user-service`.
* Ghi rõ port local của Gateway và các service liên quan.
* Làm tài liệu tham chiếu để Frontend, Backend và Tester thống nhất cách gọi API.
* Hỗ trợ test login/register và user API thông qua Gateway.

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

### 3.3. Request Flow Chính

```txt
React Frontend
→ API Gateway :8080
→ Auth Service :8081 / User Service :8086
→ MySQL
```

---

## 4. Danh Sách Port Local

| Thành phần    | Port | Base URL                | Ghi chú                                  |
| ------------- | ---- | ----------------------- | ---------------------------------------- |
| Frontend      | 5173 | `http://localhost:5173` | React/Vite local dev server              |
| API Gateway   | 8080 | `http://localhost:8080` | Frontend gọi API qua Gateway             |
| Auth Service  | 8081 | `http://localhost:8081` | Xử lý register, login, JWT/authentication |
| User Service  | 8086 | `http://localhost:8086` | Xử lý user profile và user management    |
| MySQL         | 3306 | `localhost:3306`        | Database local                           |

---

## 5. Quy Tắc Gọi API Từ Frontend

Frontend **chỉ được gọi API thông qua API Gateway**.

### 5.1. Đúng

```txt
POST http://localhost:8080/api/auth/register
POST http://localhost:8080/api/auth/login
GET  http://localhost:8080/api/users/{id}
```

### 5.2. Không Đúng

Frontend không gọi trực tiếp service nội bộ:

```txt
POST http://localhost:8081/api/auth/register
POST http://localhost:8081/api/auth/login
GET  http://localhost:8086/api/users/{id}
```

Các direct URL chỉ dùng cho Backend developer debug hoặc test service độc lập.

---

## 6. Route `/api/auth/**` Đến Auth Service

### 6.1. Mục Tiêu

Route này dùng để chuyển toàn bộ request liên quan đến Authentication từ API Gateway sang `auth-service`.

### 6.2. Route Mapping

| Gateway Path  | Target Service | Target URL              | Mô tả                         |
| ------------- | -------------- | ----------------------- | ----------------------------- |
| `/api/auth/**` | `auth-service` | `http://localhost:8081` | Register, login, auth-related |

### 6.3. Endpoint Chính

| Method | Gateway Endpoint          | Target Endpoint                 | Mô tả              |
| ------ | ------------------------- | ------------------------------- | ------------------ |
| POST   | `/api/auth/register`      | `http://localhost:8081/api/auth/register` | Đăng ký tài khoản |
| POST   | `/api/auth/login`         | `http://localhost:8081/api/auth/login`    | Đăng nhập          |

### 6.4. Request Flow

```txt
Frontend
→ POST http://localhost:8080/api/auth/login
→ API Gateway
→ http://localhost:8081/api/auth/login
→ Auth Service xử lý
→ API Gateway trả response về Frontend
```

---

## 7. Route `/api/users/**` Đến User Service

### 7.1. Mục Tiêu

Route này dùng để chuyển các request liên quan đến user profile hoặc user management từ API Gateway sang `user-service`.

### 7.2. Route Mapping

| Gateway Path   | Target Service | Target URL              | Mô tả                              |
| -------------- | -------------- | ----------------------- | ---------------------------------- |
| `/api/users/**` | `user-service` | `http://localhost:8086` | User profile, user management APIs |

### 7.3. Endpoint Dự Kiến

| Method | Gateway Endpoint       | Target Endpoint              | Mô tả                         |
| ------ | ---------------------- | ---------------------------- | ----------------------------- |
| GET    | `/api/users/{id}`      | `http://localhost:8086/api/users/{id}`      | Lấy thông tin user theo ID    |
| GET    | `/api/users/me`        | `http://localhost:8086/api/users/me`        | Lấy thông tin user hiện tại   |
| PUT    | `/api/users/{id}`      | `http://localhost:8086/api/users/{id}`      | Cập nhật thông tin user       |

> Lưu ý: Endpoint cụ thể phụ thuộc vào trạng thái implement của `user-service`. Nếu User Service chưa hoàn chỉnh, chỉ test các endpoint đã sẵn sàng.

---

## 8. Route `/internal/users/**` Nếu Cần Test Nội Bộ

### 8.1. Mục Tiêu

Route này chỉ dùng khi cần test nội bộ giữa các service hoặc test riêng User Service thông qua Gateway.

### 8.2. Route Mapping

| Gateway Path        | Target Service | Target URL              | Mục đích              |
| ------------------- | -------------- | ----------------------- | --------------------- |
| `/internal/users/**` | `user-service` | `http://localhost:8086` | Internal/debug testing |

### 8.3. Quy Ước Sử Dụng

* Không dùng route `/internal/users/**` cho Frontend production flow.
* Chỉ dùng khi Backend cần test nhanh User Service thông qua Gateway.
* Có thể disable hoặc giới hạn route này sau khi hoàn tất testing.

---

## 9. Cấu Hình Route Đề Xuất Cho Spring Cloud Gateway

Có thể cấu hình trong `api-gateway/src/main/resources/application.yml` như sau:

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
          uri: http://localhost:8081
          predicates:
            - Path=/api/auth/**

        - id: user-service
          uri: http://localhost:8086
          predicates:
            - Path=/api/users/**

        - id: internal-user-service
          uri: http://localhost:8086
          predicates:
            - Path=/internal/users/**
```

> Nếu project đang dùng Java Config thay vì `application.yml`, cần giữ nguyên logic mapping tương đương với các route trên.

---

## 10. Cấu Hình CORS Cho Frontend Local

### 10.1. Mục Tiêu

Cho phép Frontend local tại `http://localhost:5173` gọi API qua Gateway `http://localhost:8080`.

### 10.2. CORS Origin Được Cho Phép

```txt
http://localhost:5173
```

### 10.3. Headers Được Cho Phép

```txt
Content-Type
Authorization
```

### 10.4. Methods Được Cho Phép

```txt
GET, POST, PUT, PATCH, DELETE, OPTIONS
```

### 10.5. Cấu Hình CORS Đề Xuất

```yml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:5173"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders:
              - Content-Type
              - Authorization
            allowCredentials: true
```

---

## 11. Test Auth API Qua Gateway

### 11.1. Điều Kiện Trước Khi Test

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

### 11.2. Test Register Qua Gateway

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

### 11.3. Response Register Thành Công Dự Kiến

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

### 11.4. Test Login Qua Gateway

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "User@123"
  }'
```

### 11.5. Response Login Thành Công Dự Kiến

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "token": "jwt-token-here",
    "tokenType": "Bearer",
    "email": "user@example.com",
    "role": "CUSTOMER"
  }
}
```

---

## 12. Test User API Qua Gateway

### 12.1. Điều Kiện Trước Khi Test

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

### 12.2. Test User API Không Cần Token

Nếu User Service có endpoint public hoặc health check:

```bash
curl -X GET http://localhost:8080/api/users/health
```

Hoặc:

```bash
curl -X GET http://localhost:8080/api/users/1
```

### 12.3. Test User API Cần Token

Nếu endpoint yêu cầu đăng nhập, dùng token lấy từ Login API:

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <jwt-token-here>"
```

> Nếu User Service chưa sẵn sàng, ghi rõ trong MR/test note: `User Service route configured, pending endpoint implementation/testing`.

---

## 13. Acceptance Criteria

| STT | Tiêu chí | Trạng thái |
| --- | -------- | ---------- |
| 1 | Có file `docs/api/api-gateway-routes.md` | Done |
| 2 | Route `/api/auth/**` đến `auth-service` port `8081` | To Verify |
| 3 | Route `/api/users/**` đến `user-service` port `8086` | To Verify |
| 4 | Route `/internal/users/**` được cấu hình nếu cần test nội bộ | Optional |
| 5 | CORS cho Frontend local `http://localhost:5173` | To Verify |
| 6 | Test register qua Gateway | To Verify |
| 7 | Test login qua Gateway | To Verify |
| 8 | Test user API qua Gateway nếu User Service đã sẵn sàng | To Verify |
| 9 | Tài liệu ghi rõ port local từng service | Done |

---

## 14. Notes Cho Frontend

Frontend nên cấu hình Axios base URL như sau:

```js
const api = axios.create({
  baseURL: "http://localhost:8080",
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
api.get("/api/users/me", {
  headers: {
    Authorization: `Bearer ${localStorage.getItem("authToken")}`
  }
});
```

---

## 15. Scope Chưa Bao Gồm

Các nội dung sau không nằm trong scope của issue này:

* Implement logic Register/Login trong Auth Service.
* Implement đầy đủ User Profile API trong User Service.
* JWT validation filter tại Gateway nếu chưa có issue riêng.
* Service discovery bằng Eureka/Consul.
* Docker Compose cho toàn bộ microservices.
* Deploy Gateway lên server thật.

---

## 16. Ghi Chú Khi Merge Request

Branch thực hiện:

```txt
feature/issue-53-api-gateway-auth-user-routes
```

MR nên ghi rõ:

```txt
Closes #53
```

Checklist MR:

* Đã thêm/cập nhật `docs/api/api-gateway-routes.md`.
* Đã cấu hình route Auth Service qua Gateway.
* Đã cấu hình route User Service qua Gateway.
* Đã kiểm tra CORS với Frontend local.
* Đã test Login/Register qua Gateway hoặc ghi rõ lý do chưa test được.
* Đã test User API qua Gateway nếu User Service đã sẵn sàng.
