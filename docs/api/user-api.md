# User Service API Specification

## 1. Thông Tin Chung

| Mục                | Nội dung                               |
| ------------------ | -------------------------------------- |
| Service            | `user-service`                         |
| Feature            | User Profile Management                |
| API liên quan      | Create Profile, Get Profile            |
| Người phụ trách BE | Phan Tuấn Thành                    |
| Người phụ trách FE | Dương Hoàng Nhân                   |
| Trạng thái         | Updated (Event-Driven Architecture)    |
| Ngày cập nhật      | 23/06/2026                             |

---

## Lịch Sử Chỉnh Sửa
- **23/06/2026**: Refactor toàn bộ theo kiến trúc Event-Driven. Xóa bỏ Internal HTTP API (`/internal/users`). User Service giờ chỉ giao tiếp qua Kafka Events (`REGISTRATION_VALIDATION_REQUESTED` và `ACCOUNT_VERIFIED`).
- **17/06/2026**: Đồng bộ các quy tắc validate của internal create profile API (fullName, phoneNumber) với Auth Service và Frontend.
- **14/06/2026**: Cập nhật Local URL từ port `8082` sang `8086`.
- **13/06/2026**: Khởi tạo tài liệu đặc tả User Profile APIs.

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API liên quan đến thông tin cá nhân (Profile) của người dùng thuộc **User Service**.

Mục tiêu chính:
* Định nghĩa các Kafka Event mà User Service lắng nghe để thực hiện validate và tạo User Profile bất đồng bộ.
* Định nghĩa API public/protected (`/api/users/{accountId}`) để Frontend có thể truy xuất thông tin hồ sơ của người dùng.
* Đảm bảo tính thống nhất trong kiểu dữ liệu và field response, đặc biệt với các thông tin nhạy cảm như CCCD.

> **Lưu ý kiến trúc:** User Service **không còn** cung cấp Internal HTTP API (`/internal/users`) nữa. Toàn bộ giao tiếp giữa Auth Service và User Service được thực hiện qua Kafka Events.

---

## 3. API Gateway Và Service URL

### 3.1. API Gateway URL
Frontend sử dụng URL này để gọi API public/protected:
```txt
http://localhost:8080
```

### 3.2. User Service Direct URL
Chỉ dùng cho Backend debug hoặc test trực tiếp service:
```txt
http://localhost:8086
```

> **Lưu ý:** Auth Service không gọi trực tiếp sang User Service qua HTTP. Toàn bộ giao tiếp nội bộ thực hiện qua Kafka.

---

# 4. Kafka Event Consumers (Event-Driven Architecture)

> **Quan trọng:** User Service không còn cung cấp REST API nội bộ (`/internal/users`) nữa. Toàn bộ giao tiếp với Auth Service được thực hiện qua Kafka theo mô hình **Request-Reply Pattern**.

---

## 4.1. REGISTRATION_VALIDATION_REQUESTED Consumer

### Mô Tả
User Service lắng nghe sự kiện này từ Auth Service khi có yêu cầu đăng ký mới. User Service sẽ kiểm tra xem `phoneNumber` và `cccd` có bị trùng lặp không (kiểm tra cả DB và Redis reservation), sau đó publish kết quả lại.

### Thông Tin Consumer

| Mục                | Nội dung                                             |
| ------------------ | ---------------------------------------------------- |
| **Topic**          | `auth.registration.validation.requested.v1`          |
| **Consumer Group** | `user-service-validation-group`                      |
| **Loại Sự Kiện**   | `REGISTRATION_VALIDATION_REQUESTED`                  |
| **Hành động**      | Kiểm tra phone/CCCD, reserve Redis, publish result   |

### Payload Nhận Vào

```json
{
  "eventId": "uuid",
  "eventType": "REGISTRATION_VALIDATION_REQUESTED",
  "occurredAt": "2026-06-23T00:00:00Z",
  "data": {
    "requestId": "uuid",
    "email": "user@example.com",
    "phoneNumber": "0901234567",
    "cccd": "092205006789"
  }
}
```

### Logic Xử Lý
1. Kiểm tra `phoneNumber` trong DB → nếu tồn tại: publish FAILED với reason `PHONE_NUMBER_ALREADY_EXISTS`
2. Kiểm tra `cccd` trong DB → nếu tồn tại: publish FAILED với reason `CCCD_ALREADY_EXISTS`
3. Thử reserve `phoneNumber` và `cccd` trong Redis (TTL 15 phút) → nếu đang được reserve: publish FAILED với reason `REGISTRATION_CONFLICT`
4. Nếu tất cả OK → publish SUCCESS

### Sự Kiện Publish Lại (REGISTRATION_VALIDATION_RESULT)

| Mục                | Nội dung                                            |
| ------------------ | --------------------------------------------------- |
| **Topic**          | `auth.registration.validation.result.v1`            |
| **Payload**        | `{ requestId, status: "SUCCESS"/"FAILED", errorCode, retryAfterSeconds }` |

```json
{
  "eventId": "uuid",
  "eventType": "REGISTRATION_VALIDATION_RESULT",
  "occurredAt": "2026-06-23T00:00:00Z",
  "data": {
    "requestId": "uuid",
    "status": "FAILED",
    "errorCode": "PHONE_NUMBER_RESERVED",
    "retryAfterSeconds": 472
  }
}
```

---

## 4.2. ACCOUNT_VERIFIED Consumer

### Mô Tả
User Service lắng nghe sự kiện này từ Auth Service sau khi người dùng đã xác thực OTP thành công. User Service sẽ tạo bản ghi User Profile trong database.

### Thông Tin Consumer

| Mục                | Nội dung                                            |
| ------------------ | --------------------------------------------------- |
| **Topic**          | `auth.account.verified.v1`                          |
| **Consumer Group** | `user-service-account-verified-consumer`            |
| **Loại Sự Kiện**   | `ACCOUNT_VERIFIED`                                  |
| **Hành động**      | Tạo bản ghi User Profile trong DB, giải phóng Redis reservation |

### Payload Nhận Vào

```json
{
  "eventId": "uuid",
  "eventType": "ACCOUNT_VERIFIED",
  "occurredAt": "2026-06-23T00:00:00Z",
  "data": {
    "accountId": 1,
    "email": "user@example.com",
    "role": "CUSTOMER",
    "fullName": "Nguyen Van A",
    "phoneNumber": "0901234567",
    "cccd": "092205006789",
    "cccdMasked": "092******789",
    "provinceCode": "092",
    "provinceName": "Cần Thơ",
    "gender": "MALE",
    "birthYear": 2005,
    "birthday": "2005-06-12"
  }
}
```

### Cơ Chế Xử Lý Lỗi (Error Handling)
1. **Idempotency (Chống trùng lặp):** Consumer kiểm tra `accountId`, `phoneNumber`, `cccd` trong DB trước khi Insert. Nếu đã tồn tại, ghi log cảnh báo và bỏ qua Message.
2. **Redis Cleanup:** Sau khi tạo User Profile thành công, giải phóng reservation phone/CCCD trong Redis.
3. **Retry & DLQ:** Nếu xảy ra lỗi, RuntimeException được throw để Kafka trigger retry và Dead Letter Queue.

---

# 5. Get User Profile API

## 5.1. Mục Tiêu API
Lấy thông tin hồ sơ của một người dùng cụ thể. Frontend có thể gọi API này để hiển thị trang User Profile hoặc ở Header/Navigation.

Lưu ý:
- Vì lý do bảo mật, API chỉ trả về `cccdMasked` (số CCCD đã che đi), không bao giờ trả về số `cccd` đầy đủ của người dùng.

---

## 5.2. Thông Tin Endpoint

| Mục           | Nội dung                                  |
| ------------- | ----------------------------------------- |
| Method        | `GET`                                     |
| Endpoint      | `/api/users/{accountId}`                  |
| Local URL     | `http://localhost:8086/api/users/1`       |
| Gateway URL   | `http://localhost:8080/api/users/1`       |
| Content-Type  | `application/json`                        |
| Auth Required | Yes (Bearer Token)                        |
| Role Required | `CUSTOMER`, `STAFF`, `ADMIN`              |

*Lưu ý bảo mật: User thông thường chỉ được xem profile của chính mình.*

---

## 5.3. Request Headers

| Header        | Required | Example                     | Mô tả              |
| ------------- | -------: | --------------------------- | ------------------ |
| Authorization |      Yes | `Bearer eyJhbGciOiJIUzI1...`| JWT Token          |

---

## 5.4. Response Success

Status: `200 OK`

```json
{
  "success": true,
  "message": "User profile retrieved successfully",
  "data": {
    "accountId": 1,
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

---

## 5.5. Giải Thích Field Response

| Field             | Type    | Mô tả                                       |
| ----------------- | ------- | ------------------------------------------- |
| accountId         | number  | ID của account (map với bảng accounts)      |
| fullName          | string  | Họ và tên người dùng                        |
| phoneNumber       | string  | Số điện thoại                               |
| gender            | string  | Giới tính (`MALE`, `FEMALE`, `OTHER`)       |
| birthday          | string  | Ngày sinh (`YYYY-MM-DD`)                    |
| cccdMasked        | string  | CCCD đã được che một phần an toàn           |
| provinceName      | string  | Tỉnh/thành nơi làm thẻ CCCD suy ra từ mã    |
| birthYear         | number  | Năm sinh suy ra từ CCCD                     |
| verifiedPhone     | boolean | Trạng thái SĐT đã xác thực hay chưa         |

---

## 5.6. Response Error

### Case 1: Không tìm thấy User
Status: `404 Not Found`
```json
{
  "success": false,
  "message": "User profile not found",
  "errorCode": "USER_NOT_FOUND",
  "data": null
}
```

### Case 2: Unauthorized (Chưa đăng nhập / Sai Token)
Status: `401 Unauthorized`
```json
{
  "success": false,
  "message": "Unauthorized access",
  "data": null
}
```

---

# 6. Tổng Quan Luồng Event-Driven

## 6.1. Toàn Bộ Luồng Đăng Ký

```txt
[Client] POST /api/auth/register
      ↓
[Auth Service]
  - Check duplicate email (Auth DB)
  - Validate CCCD format
  - Save PendingRegistrationData to Redis
  - Publish REGISTRATION_VALIDATION_REQUESTED
      ↓ Kafka
[User Service]
  - Check phone/CCCD in DB
  - Reserve phone/CCCD in Redis (TTL 15 min)
  - Publish REGISTRATION_VALIDATION_RESULT
      ↓ Kafka
[Auth Service]
  - If FAILED → return 409
  - If SUCCESS → create Account in DB
  - Send OTP via email
  - Return 202 Accepted { requestId, message }
      ↓
[Client] POST /api/auth/verify { email, otp, purpose: "REGISTRATION" }
      ↓
[Auth Service]
  - Verify OTP
  - Activate Account (registration_completed=1)
  - Publish ACCOUNT_VERIFIED
      ↓ Kafka
[User Service]
  - Create User Profile in DB
  - Release Redis reservation
```

## 6.2. Danh Sách Kafka Topics

| Topic | Publisher | Consumer | Mô tả |
| ----- | --------- | -------- | ------ |
| `auth.registration.validation.requested.v1` | Auth Service | User Service | Yêu cầu validate phone/CCCD |
| `auth.registration.validation.result.v1` | User Service | Auth Service | Kết quả validate |
| `auth.account.verified.v1` | Auth Service | User Service | Trigger tạo User Profile |
