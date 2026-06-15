# User Service API Specification

## 1. Thông Tin Chung

| Mục                | Nội dung                               |
| ------------------ | -------------------------------------- |
| Service            | `user-service`                         |
| Feature            | User Profile Management                |
| API liên quan      | Create Profile, Get Profile            |
| Người phụ trách BE | (Backend Developer)                    |
| Người phụ trách FE | (Frontend Developer)                   |
| Trạng thái         | Ready for Implement                    |
| Ngày cập nhật      | 14/06/2026                             |

---

## Lịch Sử Chỉnh Sửa
- **15/06/2026**: Bổ sung đặc tả Event-Driven (Kafka Consumer) cho chức năng tự động tạo User Profile.
- **14/06/2026**: Cập nhật Local URL từ port `8082` sang `8086`. Đổi tên trường trả về từ `isVerifiedPhone` thành `verifiedPhone` (do gỡ bỏ Lombok).
- **13/06/2026**: Khởi tạo tài liệu đặc tả User Profile APIs.

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API liên quan đến thông tin cá nhân (Profile) của người dùng thuộc **User Service**.

Mục tiêu chính:
* Định nghĩa rõ ràng API nội bộ (`/internal/users`) để Auth Service gọi sau khi đăng ký tài khoản thành công.
* Định nghĩa API public/protected (`/api/users/{accountId}`) để Frontend có thể truy xuất thông tin hồ sơ của người dùng.
* Đảm bảo tính thống nhất trong kiểu dữ liệu và field response, đặc biệt với các thông tin nhạy cảm như CCCD.

---

## 3. API Gateway Và Service URL

### 3.1. API Gateway URL
Frontend sử dụng URL này để gọi API public/protected:
```txt
http://localhost:8080
```

### 3.2. User Service Direct URL
Chỉ dùng cho Backend debug hoặc cho các service nội bộ giao tiếp với nhau (VD: Auth Service gọi sang User Service):
```txt
http://localhost:8086
```

---

# 4. Internal Create User Profile API

## 4.1. Mục Tiêu API
API này được Auth Service gọi đồng bộ (hoặc bất đồng bộ tùy kiến trúc) ngay sau khi tài khoản người dùng được khởi tạo thành công tại Auth Service. Mục đích là để cấp phát và lưu trữ dữ liệu hồ sơ cá nhân cơ bản ở User Service.

Lưu ý: Đây là **Internal API**, API Gateway không được phép expose endpoint này ra ngoài cho Frontend hay External Client gọi.

---

## 4.2. Thông Tin Endpoint

| Mục           | Nội dung                                  |
| ------------- | ----------------------------------------- |
| Method        | `POST`                                    |
| Endpoint      | `/internal/users`                         |
| Local URL     | `http://localhost:8086/internal/users`    |
| Gateway URL   | (Không expose qua API Gateway)            |
| Content-Type  | `application/json`                        |
| Auth Required | Có thể dùng Internal Token (TBD)          |
| Role Required | None (Internal)                           |

---

## 4.3. Request Headers

| Header       | Required | Example            | Mô tả                |
| ------------ | -------: | ------------------ | -------------------- |
| Content-Type |      Yes | `application/json` | Kiểu dữ liệu gửi lên |

---

## 4.4. Request Body

```json
{
  "accountId": 1,
  "fullName": "Nguyen Van A",
  "phoneNumber": "0901234567",
  "cccd": "092205006789",
  "cccdMasked": "092******789",
  "provinceCode": "092",
  "provinceName": "Cần Thơ",
  "birthYear": 2005,
  "gender": "MALE",
  "birthday": "2005-06-12",
  "cccdCheckNote": "This API only checks CCCD format."
}
```

---

## 4.5. Giải Thích Field Request

| Field         | Type   | Required | Validate          | Mô tả                                      |
| ------------- | ------ | -------: | ----------------- | ------------------------------------------ |
| accountId     | number |      Yes | > 0               | ID của tài khoản từ Auth Service           |
| fullName      | string |      Yes | Không rỗng        | Họ tên người dùng                          |
| phoneNumber   | string |      Yes | Format SĐT        | Số điện thoại (Unique)                     |
| cccd          | string |      Yes | 12 chữ số         | Số CCCD đầy đủ (Unique)                    |
| cccdMasked    | string |      Yes | Format che kí tự  | CCCD đã che để hiển thị                    |
| provinceCode  | string |       No | -                 | Mã tỉnh/thành suy ra từ CCCD               |
| provinceName  | string |       No | -                 | Tên tỉnh/thành suy ra từ CCCD              |
| birthYear     | number |       No | > 1900            | Năm sinh suy ra từ CCCD                    |
| gender        | string |      Yes | Enum              | `MALE`, `FEMALE`, `OTHER`                  |
| birthday      | date   |      Yes | Format YYYY-MM-DD | Ngày sinh người dùng nhập                  |
| cccdCheckNote | string |       No | -                 | Note/Log từ CCCD Validation lúc đăng ký    |

---

## 4.6. Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "User profile created successfully",
  "data": {
    "accountId": 1,
    "fullName": "Nguyen Van A",
    "phoneNumber": "0901234567",
    "gender": "MALE"
  }
}
```

---

## 4.7. Response Error

### Case 1: CCCD đã tồn tại
Status: `409 Conflict`
```json
{
  "success": false,
  "message": "CCCD already exists",
  "errorCode": "USER_CCCD_ALREADY_EXISTS",
  "data": null
}
```

### Case 2: Số điện thoại đã tồn tại
Status: `409 Conflict`
```json
{
  "success": false,
  "message": "Phone number already exists",
  "errorCode": "USER_PHONE_ALREADY_EXISTS",
  "data": null
}
```

### Case 3: Account ID đã có hồ sơ
Status: `409 Conflict`
```json
{
  "success": false,
  "message": "User profile already exists for this account",
  "errorCode": "USER_PROFILE_ALREADY_EXISTS",
  "data": null
}
```

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

# 6. Kafka Event Listeners (Event-Driven)

## 6.1. ACCOUNT_CREATED Event
Bên cạnh Internal API (`/internal/users`), hệ thống hỗ trợ mô hình giao tiếp bất đồng bộ (Asynchronous) thông qua Kafka. Khi người dùng đăng ký tài khoản thành công, User Service sẽ nhận được thông điệp từ Kafka và tự động tạo hồ sơ mà không cần chờ Frontend hay API Gateway gọi thêm.

### Thông Tin Consumer

| Mục                 | Nội dung                                            |
| ------------------- | --------------------------------------------------- |
| **Topic**           | `auth.account.created.v1`                           |
| **Consumer Group**  | `user-service-account-created-consumer`             |
| **Loại Sự Kiện**    | `ACCOUNT_CREATED`                                   |
| **Hành động**       | Khởi tạo bản ghi hồ sơ mới trong Database           |

### Payload Tiêu Thụ Điển Hình
User Service sẽ tự động ép kiểu (Deserialize) JSON nhận được thành class `AccountCreatedEvent`. Dữ liệu quan trọng nằm trong biến `data` của Payload:

```json
{
  "eventId": "uuid",
  "eventType": "ACCOUNT_CREATED",
  "timestamp": "2026-06-15T15:00:00Z",
  "data": {
    "accountId": 4,
    "email": "levanc99@gmail.com",
    "fullName": "Lê Văn C",
    "phoneNumber": "0933112233",
    "cccd": "048099123456",
    "cccdMasked": "048******456",
    "provinceCode": "048",
    "provinceName": "Đà Nẵng",
    "gender": "MALE",
    "birthYear": 1999,
    "birthday": "1999-05-15"
  }
}
```

### Cơ Chế Xử Lý Lỗi (Error Handling)
1. **Idempotency (Chống trùng lặp):** Consumer sẽ tự động kiểm tra `accountId` trong bảng `users` trước khi Insert. Nếu đã tồn tại, Consumer ghi log cảnh báo trùng lặp và bỏ qua Message một cách an toàn.
2. **Poison Pill Protection:** Sử dụng `ErrorHandlingDeserializer` để ngăn chặn các bản tin lỗi cấu trúc (ví dụ thiếu Type Header) gây ra tình trạng lặp vô tận (Infinite poll loop) làm treo dịch vụ.
