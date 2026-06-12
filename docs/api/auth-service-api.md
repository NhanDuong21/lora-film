# Auth Service API Specification

## 1. Thông Tin Chung

| Mục                | Nội dung                               |
| ------------------ | -------------------------------------- |
| Service            | `auth-service`                         |
| Feature            | Authentication                         |
| API liên quan      | Register, Login                        |
| Issue liên quan    | #15, #16, #19, #42, #48, #49, #50, #51 |
| Người phụ trách BE | Trần Hiển Vinh                         |
| Người phụ trách FE | Dương Thiện Nhân                       |
| Trạng thái         | Ready for Review                       |
| Ngày cập nhật      | 12/06/2026                             |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API Authentication của hệ thống **LoraFilm**.

Mục tiêu chính:

* Giúp Backend và Frontend thống nhất request/response trước hoặc song song với quá trình implement.
* Giúp Frontend có thể code form, validate input, mock response và xử lý success/error mà không cần đọc trực tiếp source code Backend.
* Làm API Contract cho các API Register và Login.
* Ghi rõ Frontend phải gọi API thông qua API Gateway, không gọi trực tiếp `auth-service`.
* Làm cơ sở để Backend refactor Register/Login theo đúng contract đã thống nhất.

---

## 3. API Gateway Và Service URL

### 3.1. API Gateway URL

Frontend sử dụng URL này để gọi API:

```txt
http://localhost:8080
```

### 3.2. Auth Service Direct URL

Chỉ dùng cho Backend debug hoặc test riêng service:

```txt
http://localhost:8081
```

### 3.3. CCCD Check API URL

API kiểm tra CCCD theo quy luật định dạng:

```txt
https://api-check-cccd.lorafilm.xyz/api/cccd/check
```

### 3.4. Request Flow Chính

```txt
React Frontend
→ API Gateway :8080
→ Auth Service :8081
→ MySQL
```

### 3.5. Request Flow Có Kiểm Tra CCCD

```txt
React Register Form
→ CCCD Check API
→ Fill / validate derived information
→ API Gateway :8080
→ Auth Service :8081
→ User Service / User Schema
→ MySQL
```

---

## 4. Quy Tắc Gọi API

Frontend chỉ được gọi API thông qua API Gateway:

```txt
POST http://localhost:8080/api/auth/register
POST http://localhost:8080/api/auth/login
```

Frontend không gọi trực tiếp Auth Service:

```txt
POST http://localhost:8081/api/auth/register
POST http://localhost:8081/api/auth/login
```

Auth Service direct URL chỉ dùng cho backend developer test/debug.

---

# 5. CCCD Check API

## 5.1. Mục Tiêu

API này dùng để kiểm tra định dạng CCCD theo quy luật số CCCD Việt Nam.

Frontend có thể dùng API này ở màn hình Register để người dùng chỉ cần nhập CCCD, sau đó hệ thống suy ra một số thông tin như:

* Mã tỉnh/thành
* Tên tỉnh/thành
* Giới tính
* Năm sinh
* Tuổi theo năm

Lưu ý: API này **chỉ kiểm tra định dạng CCCD**, không xác minh CCCD có tồn tại thật trong cơ sở dữ liệu quốc gia.

---

## 5.2. Thông Tin Endpoint

| Mục           | Nội dung                                             |
| ------------- | ---------------------------------------------------- |
| Method        | `POST`                                               |
| Endpoint      | `/api/cccd/check`                                    |
| Full URL      | `https://api-check-cccd.lorafilm.xyz/api/cccd/check` |
| Content-Type  | `application/json`                                   |
| Auth Required | Theo cấu hình API key                                |
| Role Required | None                                                 |

---

## 5.3. Request Headers

| Header       | Required | Example                 | Mô tả                   |
| ------------ | -------: | ----------------------- | ----------------------- |
| Content-Type |      Yes | `application/json`      | Kiểu dữ liệu gửi lên    |
| x-api-key    |      Yes | `lora_cccd_2026_secret` | API key service yêu cầu |

Ví dụ:

```http
Content-Type: application/json
x-api-key: lora_cccd_2026_secret
```

---

## 5.4. Request Body

```json
{
  "cccd": "092205006789"
}
```

---

## 5.5. Giải Thích Field Request

| Field | Type   | Required | Validate  | Mô tả                   |
| ----- | ------ | -------: | --------- | ----------------------- |
| cccd  | string |      Yes | 12 chữ số | Số CCCD người dùng nhập |

---

## 5.6. Response Success

Status: `200 OK`

```json
{
  "valid": true,
  "cccdMasked": "092******789",
  "provinceCode": "092",
  "provinceName": "Cần Thơ",
  "gender": "MALE",
  "genderLabel": "Nam",
  "birthYear": 2005,
  "ageByYear": 21,
  "randomCode": "006789",
  "message": "CCCD format is valid",
  "note": "This API only checks CCCD format. It does not verify whether the CCCD exists in the national citizen database."
}
```

---

## 5.7. Giải Thích Field Response

| Field        | Type    | Mô tả                                       |
| ------------ | ------- | ------------------------------------------- |
| valid        | boolean | CCCD có hợp lệ theo format hay không        |
| cccdMasked   | string  | CCCD đã được che bớt để hiển thị an toàn    |
| provinceCode | string  | Mã tỉnh/thành trong CCCD                    |
| provinceName | string  | Tên tỉnh/thành suy ra từ mã CCCD            |
| gender       | string  | Giới tính dạng enum, ví dụ `MALE`, `FEMALE` |
| genderLabel  | string  | Giới tính hiển thị tiếng Việt               |
| birthYear    | number  | Năm sinh suy ra từ CCCD                     |
| ageByYear    | number  | Tuổi tính theo năm hiện tại                 |
| randomCode   | string  | Dãy số ngẫu nhiên cuối CCCD                 |
| message      | string  | Thông báo kết quả                           |
| note         | string  | Ghi chú phạm vi kiểm tra                    |

---

## 5.8. Response Error

### Case 1: CCCD sai định dạng

Status: `400 Bad Request`

```json
{
  "valid": false,
  "message": "CCCD format is invalid",
  "note": "This API only checks CCCD format. It does not verify whether the CCCD exists in the national citizen database."
}
```

### Case 2: Thiếu API key hoặc API key không hợp lệ

Status: `401 Unauthorized`

```json
{
  "success": false,
  "message": "Invalid API key",
  "errorCode": "INVALID_API_KEY"
}
```

### Case 3: Lỗi server CCCD API

Status: `500 Internal Server Error`

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR"
}
```

---

## 5.9. Frontend Handling

Khi người dùng nhập CCCD ở form Register:

1. Frontend gọi CCCD Check API.
2. Nếu `valid = true`, frontend có thể tự động hiển thị hoặc gán các thông tin:

   * `provinceName`
   * `gender`
   * `birthYear`
   * `cccdMasked`
3. Frontend vẫn cần người dùng nhập các field bắt buộc khác:

   * Họ tên
   * Email
   * Số điện thoại
   * CCCD
   * Ngày sinh
   * Password
4. Frontend có thể đối chiếu `birthYear` từ CCCD với `birthday` người dùng nhập.
5. Nếu năm sinh trong CCCD không khớp với ngày sinh, frontend hiển thị cảnh báo hoặc không cho submit.
6. Frontend không được lưu CCCD vào `localStorage`.

---

## 5.10. Lưu Ý Bảo Mật

* Không nên hiển thị đầy đủ CCCD sau khi người dùng nhập.
* Có thể hiển thị dạng masked: `092******789`.
* Không log CCCD đầy đủ ở frontend console.
* Không lưu CCCD vào localStorage.
* CCCD là dữ liệu cá nhân nhạy cảm, chỉ lưu ở database nếu thật sự cần cho nghiệp vụ.

---

# 6. Register API

## 6.1. Mục Tiêu API

API này dùng để đăng ký tài khoản người dùng.

Frontend gửi thông tin đăng ký lên Backend thông qua API Gateway. Backend tạo tài khoản đăng nhập trong `auth-service` và lưu thông tin hồ sơ người dùng ở `user-service` hoặc schema tương ứng.

Trong định hướng mới, form Register sẽ giảm số lượng field người dùng phải nhập thủ công bằng cách sử dụng CCCD Check API để suy ra một số thông tin như giới tính, tỉnh/thành và năm sinh.

---

## 6.2. Trạng Thái Implement

| Mục                   | Nội dung                                               |
| --------------------- | ------------------------------------------------------ |
| Contract Status       | Ready for Backend Refactor                             |
| Implementation Target | Register API sẽ được Vinh refactor theo contract này   |
| FE Usage              | Frontend code theo request/response trong tài liệu này |
| CCCD Flow             | Frontend check CCCD trước khi submit Register          |
| User Profile          | Dữ liệu profile thuộc phạm vi User Service             |

---

## 6.3. Thông Tin Endpoint

| Mục           | Nội dung                                  |
| ------------- | ----------------------------------------- |
| Method        | `POST`                                    |
| Endpoint      | `/api/auth/register`                      |
| Local URL     | `http://localhost:8081/api/auth/register` |
| Gateway URL   | `http://localhost:8080/api/auth/register` |
| Content-Type  | `application/json`                        |
| Auth Required | No                                        |
| Role Required | None                                      |

---

## 6.4. Request Headers

| Header       | Required | Example            | Mô tả                |
| ------------ | -------: | ------------------ | -------------------- |
| Content-Type |      Yes | `application/json` | Kiểu dữ liệu gửi lên |

```http
Content-Type: application/json
```

---

## 6.5. Request Body

```json
{
  "fullName": "Nguyen Van A",
  "email": "user@example.com",
  "phoneNumber": "0901234567",
  "cccd": "092205006789",
  "birthday": "2005-06-12",
  "password": "User@123"
}
```

---

## 6.6. Giải Thích Field Request

| Field       | Type   | Required | Validate                          | Mô tả                                       |
| ----------- | ------ | -------: | --------------------------------- | ------------------------------------------- |
| fullName    | string |      Yes | Không rỗng                        | Họ tên người dùng                           |
| email       | string |      Yes | Đúng format email                 | Email đăng ký, dùng làm tài khoản đăng nhập |
| phoneNumber | string |      Yes | Đúng format số điện thoại         | Số điện thoại người dùng                    |
| cccd        | string |      Yes | 12 chữ số                         | Số CCCD người dùng                          |
| birthday    | date   |      Yes | Format `YYYY-MM-DD`               | Ngày sinh người dùng nhập                   |
| password    | string |      Yes | Không rỗng, nên tối thiểu 6 ký tự | Mật khẩu đăng ký                            |

---

## 6.7. Derived Fields Từ CCCD

Các field sau không nhất thiết để người dùng nhập tay. Hệ thống có thể lấy từ CCCD Check API:

| Field        | Source         | Mô tả                       |
| ------------ | -------------- | --------------------------- |
| cccdMasked   | CCCD Check API | CCCD đã che bớt             |
| provinceCode | CCCD Check API | Mã tỉnh/thành               |
| provinceName | CCCD Check API | Tên tỉnh/thành              |
| gender       | CCCD Check API | Giới tính dạng enum         |
| genderLabel  | CCCD Check API | Giới tính tiếng Việt        |
| birthYear    | CCCD Check API | Năm sinh suy ra             |
| ageByYear    | CCCD Check API | Tuổi tính theo năm hiện tại |
| randomCode   | CCCD Check API | Dãy số ngẫu nhiên cuối CCCD |

---

## 6.8. Business Rules

| Rule               | Mô tả                                                            |
| ------------------ | ---------------------------------------------------------------- |
| Email unique       | Email không được trùng với tài khoản đã có                       |
| Phone unique       | Số điện thoại không được trùng nếu hệ thống yêu cầu              |
| CCCD unique        | CCCD không được trùng nếu hệ thống lưu CCCD                      |
| CCCD valid         | CCCD phải hợp lệ theo CCCD Check API                             |
| Birthday match     | Năm trong `birthday` nên khớp với `birthYear` suy ra từ CCCD     |
| Default role       | User mới mặc định có role `CUSTOMER`                             |
| Hash password      | Mật khẩu phải được hash trước khi lưu                            |
| Account active     | Hiện tại tài khoản có thể active ngay sau đăng ký                |
| Email verification | Chưa bắt buộc trong Sprint 1, nếu kịp sẽ chuẩn bị token/contract |

---

## 6.9. Backend Processing Flow

```txt
Receive register request
→ Validate request body
→ Check duplicate email in Auth Service
→ Check CCCD format / use CCCD derived information
→ Create account in Auth Service
→ Assign default role CUSTOMER
→ Hash password
→ Store account with is_active
→ Create user profile in User Service or user schema
→ Return register response
```

---

## 6.10. User Profile Data Sau Khi Register

Sau khi register thành công, hệ thống cần lưu hoặc chuẩn bị lưu các dữ liệu profile sau ở User Service:

| Field         | Source           |
| ------------- | ---------------- |
| accountId     | Auth Service     |
| fullName      | Register request |
| phoneNumber   | Register request |
| cccd          | Register request |
| cccdMasked    | CCCD Check API   |
| provinceCode  | CCCD Check API   |
| provinceName  | CCCD Check API   |
| gender        | CCCD Check API   |
| birthday      | Register request |
| birthYear     | CCCD Check API   |
| cccdCheckedAt | System time      |
| cccdCheckNote | CCCD Check API   |

---

## 6.11. Response Success

Status: `200 OK` hoặc `201 Created`

```json
{
  "success": true,
  "message": "Register successfully",
  "data": {
    "accountId": 1,
    "email": "user@example.com",
    "role": "CUSTOMER",
    "fullName": "Nguyen Van A",
    "phoneNumber": "0901234567",
    "cccdMasked": "092******789",
    "provinceName": "Cần Thơ",
    "gender": "MALE",
    "birthYear": 2005
  }
}
```

---

## 6.12. Giải Thích Field Response

| Field             | Type    | Mô tả                                    |
| ----------------- | ------- | ---------------------------------------- |
| success           | boolean | Trạng thái xử lý thành công hay thất bại |
| message           | string  | Thông báo kết quả                        |
| data.accountId    | number  | ID tài khoản vừa tạo                     |
| data.email        | string  | Email vừa đăng ký                        |
| data.role         | string  | Vai trò mặc định của người dùng          |
| data.fullName     | string  | Họ tên người dùng                        |
| data.phoneNumber  | string  | Số điện thoại người dùng                 |
| data.cccdMasked   | string  | CCCD đã che bớt                          |
| data.provinceName | string  | Tỉnh/thành suy ra từ CCCD                |
| data.gender       | string  | Giới tính suy ra từ CCCD                 |
| data.birthYear    | number  | Năm sinh suy ra từ CCCD                  |

---

## 6.13. Response Error

### Case 1: Email đã tồn tại

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Email already exists",
  "errorCode": "AUTH_EMAIL_ALREADY_EXISTS",
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

### Case 3: CCCD đã tồn tại

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "CCCD already exists",
  "errorCode": "USER_CCCD_ALREADY_EXISTS",
  "data": null
}
```

### Case 4: CCCD không hợp lệ

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "CCCD format is invalid",
  "errorCode": "USER_CCCD_INVALID",
  "data": null
}
```

### Case 5: Ngày sinh không khớp với CCCD

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Birthday does not match CCCD birth year",
  "errorCode": "USER_BIRTHDAY_CCCD_MISMATCH",
  "data": null
}
```

### Case 6: Dữ liệu gửi lên không hợp lệ

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    },
    {
      "field": "password",
      "message": "Password is required"
    },
    {
      "field": "cccd",
      "message": "CCCD must contain 12 digits"
    }
  ]
}
```

### Case 7: Lỗi User Service khi tạo profile

Status: `502 Bad Gateway` hoặc `500 Internal Server Error`

```json
{
  "success": false,
  "message": "Failed to create user profile",
  "errorCode": "USER_PROFILE_CREATE_FAILED",
  "data": null
}
```

### Case 8: Lỗi server

Status: `500 Internal Server Error`

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "data": null
}
```

---

## 6.14. Danh Sách Status Code

| Status Code | Ý nghĩa               | Khi nào xảy ra                                       |
| ----------: | --------------------- | ---------------------------------------------------- |
|   200 / 201 | Đăng ký thành công    | User/account được tạo thành công                     |
|         400 | Bad Request           | Dữ liệu gửi lên không hợp lệ                         |
|         409 | Conflict              | Email, phone hoặc CCCD đã tồn tại                    |
|         502 | Bad Gateway           | Auth Service không tạo được profile bên User Service |
|         500 | Internal Server Error | Lỗi server                                           |

---

# 7. Login API

## 7.1. Mục Tiêu API

API này dùng để đăng nhập tài khoản người dùng.

Frontend gửi `email` và `password` lên Backend. Backend kiểm tra thông tin đăng nhập. Nếu hợp lệ, Backend trả về JWT token, loại token, email và role của người dùng.

---

## 7.2. Trạng Thái Implement

| Mục                   | Nội dung                                        |
| --------------------- | ----------------------------------------------- |
| Contract Status       | Ready for FE                                    |
| Implementation Status | Implemented Basic Flow                          |
| FE Usage              | Frontend có thể gọi qua API Gateway             |
| Token Type            | Bearer                                          |
| Response Target       | Backend cần giữ response đúng theo contract này |

---

## 7.3. Thông Tin Endpoint

| Mục           | Nội dung                               |
| ------------- | -------------------------------------- |
| Method        | `POST`                                 |
| Endpoint      | `/api/auth/login`                      |
| Local URL     | `http://localhost:8081/api/auth/login` |
| Gateway URL   | `http://localhost:8080/api/auth/login` |
| Content-Type  | `application/json`                     |
| Auth Required | No                                     |
| Role Required | None                                   |

---

## 7.4. Request Headers

| Header       | Required | Example            | Mô tả                |
| ------------ | -------: | ------------------ | -------------------- |
| Content-Type |      Yes | `application/json` | Kiểu dữ liệu gửi lên |

```http
Content-Type: application/json
```

---

## 7.5. Request Body

```json
{
  "email": "nhan@gmail.com",
  "password": "Nhan@123"
}
```

---

## 7.6. Giải Thích Field Request

| Field    | Type   | Required | Validate          | Mô tả              |
| -------- | ------ | -------: | ----------------- | ------------------ |
| email    | string |      Yes | Đúng format email | Email đăng nhập    |
| password | string |      Yes | Không rỗng        | Mật khẩu đăng nhập |

---

## 7.7. Response Success

Status: `200 OK`

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "token": "jwt-token-here",
    "tokenType": "Bearer",
    "email": "nhan@gmail.com",
    "role": "CUSTOMER"
  }
}
```

---

## 7.8. Giải Thích Field Response

| Field          | Type    | Mô tả                                       |
| -------------- | ------- | ------------------------------------------- |
| success        | boolean | Trạng thái xử lý thành công hay thất bại    |
| message        | string  | Thông báo kết quả                           |
| data.token     | string  | JWT token dùng để gọi các API cần đăng nhập |
| data.tokenType | string  | Loại token, hiện tại là Bearer              |
| data.email     | string  | Email của người dùng đăng nhập              |
| data.role      | string  | Vai trò của người dùng                      |

---

## 7.9. Response Error

### Case 1: Sai email hoặc mật khẩu

Status: `401 Unauthorized`

```json
{
  "success": false,
  "message": "Invalid email or password",
  "errorCode": "AUTH_INVALID_CREDENTIALS",
  "data": null
}
```

### Case 2: Tài khoản chưa được kích hoạt

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "Account is not active",
  "errorCode": "AUTH_ACCOUNT_INACTIVE",
  "data": null
}
```

### Case 3: Dữ liệu gửi lên không hợp lệ

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    },
    {
      "field": "password",
      "message": "Password is required"
    }
  ]
}
```

### Case 4: Lỗi server

Status: `500 Internal Server Error`

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "data": null
}
```

---

## 7.10. Danh Sách Status Code

| Status Code | Ý nghĩa               | Khi nào xảy ra               |
| ----------: | --------------------- | ---------------------------- |
|         200 | OK                    | Đăng nhập thành công         |
|         400 | Bad Request           | Dữ liệu gửi lên không hợp lệ |
|         401 | Unauthorized          | Sai email hoặc mật khẩu      |
|         403 | Forbidden             | Tài khoản chưa active        |
|         500 | Internal Server Error | Lỗi server                   |

---

# 8. Frontend Notes

## 8.1. Token Storage

Sau khi login thành công, frontend lấy token từ:

```txt
response.data.data.token
```

và lưu vào `localStorage`.

Key đề xuất:

```txt
authToken
```

Ví dụ:

```js
localStorage.setItem("authToken", response.data.data.token);
localStorage.setItem("tokenType", response.data.data.tokenType);
localStorage.setItem("userEmail", response.data.data.email);
localStorage.setItem("userRole", response.data.data.role);
```

---

## 8.2. Authorization Header Cho Các API Sau

Khi gọi các API cần xác thực ở sprint sau, frontend gửi header:

```http
Authorization: Bearer <token>
```

Ví dụ:

```js
headers: {
  Authorization: `Bearer ${localStorage.getItem("authToken")}`
}
```

---

## 8.3. Register Form Fields

Register form đề xuất chỉ cần người dùng nhập:

| Field        | Người dùng nhập? | Ghi chú        |
| ------------ | ---------------: | -------------- |
| fullName     |              Yes | Họ tên         |
| email        |              Yes | Email đăng ký  |
| phoneNumber  |              Yes | Số điện thoại  |
| cccd         |              Yes | CCCD           |
| birthday     |              Yes | Ngày sinh      |
| password     |              Yes | Mật khẩu       |
| gender       |               No | Suy ra từ CCCD |
| provinceName |               No | Suy ra từ CCCD |
| birthYear    |               No | Suy ra từ CCCD |

---

## 8.4. Register UI Flow Đề Xuất

```txt
User nhập CCCD
→ Frontend gọi CCCD Check API
→ Nếu CCCD hợp lệ, hiển thị gender/province/birthYear
→ User nhập các field còn lại
→ User submit form
→ Frontend gọi POST /api/auth/register qua API Gateway
→ Backend tạo account/user
→ Frontend redirect sang Login
```

---

## 8.5. Login UI Flow

```txt
User nhập email/password
→ Frontend gọi POST /api/auth/login qua API Gateway
→ Nếu success = true, lưu authToken vào localStorage
→ Redirect user theo role hoặc về Home Page
→ Nếu lỗi, hiển thị message từ response
```

---

# 9. Database Notes

## 9.1. Auth Service Schema

Auth Service nên chỉ quản lý dữ liệu đăng nhập và phân quyền:

* accounts
* roles
* permissions
* roles_permissions
* refresh_tokens
* audit_logs

Không nên lưu toàn bộ profile người dùng trong Auth Service.

---

## 9.2. Auth Service Schema Hiện Tại

```sql
CREATE TABLE `accounts` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Logical Ref to User Service',
  `email` varchar(100) UNIQUE NOT NULL COMMENT 'Dùng làm tên đăng nhập chính',
  `password_hash` varchar(255) NOT NULL,
  `role_id` int NOT NULL,
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);

CREATE TABLE `roles` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `role_name` varchar(50) UNIQUE NOT NULL COMMENT 'CUSTOMER, STAFF, ADMIN',
  `description` varchar(255)
);

CREATE TABLE `permissions` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `permission_code` varchar(100) UNIQUE NOT NULL,
  `description` varchar(255)
);

CREATE TABLE `roles_permissions` (
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`)
);

CREATE TABLE `refresh_tokens` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `token` varchar(255) UNIQUE NOT NULL,
  `expiry_date` timestamp NOT NULL,
  `is_revoked` boolean DEFAULT false,
  `created_at` timestamp DEFAULT (now())
);

CREATE TABLE `audit_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint,
  `action` varchar(100) NOT NULL,
  `ip_address` varchar(45),
  `user_agent` varchar(255),
  `created_at` timestamp DEFAULT (now())
);

ALTER TABLE `accounts`
ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `roles_permissions`
ADD FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;

ALTER TABLE `roles_permissions`
ADD FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE;

ALTER TABLE `refresh_tokens`
ADD FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;
```

---

## 9.3. User Service Schema Đề Xuất Bổ Sung

Vì Register cần lưu CCCD và thông tin suy ra từ CCCD, các field này nên thuộc về `user-service`.

Bảng `users` nên bổ sung:

```sql
ALTER TABLE `users`
ADD COLUMN `cccd` varchar(12) UNIQUE COMMENT 'Số CCCD của người dùng',
ADD COLUMN `cccd_masked` varchar(20) COMMENT 'CCCD đã che bớt để hiển thị',
ADD COLUMN `province_code` varchar(10) COMMENT 'Mã tỉnh/thành suy ra từ CCCD',
ADD COLUMN `province_name` varchar(100) COMMENT 'Tên tỉnh/thành suy ra từ CCCD',
ADD COLUMN `birth_year` int COMMENT 'Năm sinh suy ra từ CCCD',
ADD COLUMN `cccd_checked_at` timestamp NULL COMMENT 'Thời điểm kiểm tra CCCD',
ADD COLUMN `cccd_check_note` varchar(255) COMMENT 'Ghi chú từ CCCD check service';
```

Bảng `users` sau khi bổ sung có thể gồm:

```sql
CREATE TABLE `users` (
  `account_id` bigint PRIMARY KEY COMMENT 'Shared Primary Key - Logical Ref từ accounts.id của Auth Service',
  `full_name` varchar(100) NOT NULL,
  `phone_number` varchar(15) UNIQUE NOT NULL,
  `cccd` varchar(12) UNIQUE,
  `cccd_masked` varchar(20),
  `province_code` varchar(10),
  `province_name` varchar(100),
  `gender` varchar(10) COMMENT 'MALE, FEMALE, OTHER',
  `birthday` date,
  `birth_year` int,
  `is_verified_phone` boolean DEFAULT false,
  `cccd_checked_at` timestamp NULL,
  `cccd_check_note` varchar(255),
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);
```

---

## 9.4. Lưu Ý Foreign Key Trong User Service

Schema hiện tại đang có câu FK chưa hợp lý:

```sql
ALTER TABLE `users`
ADD FOREIGN KEY (`account_id`) REFERENCES `employee_profiles` (`user_id`) ON DELETE CASCADE;
```

Quan hệ đúng hơn nên là `employee_profiles.user_id` tham chiếu về `users.account_id`:

```sql
ALTER TABLE `employee_profiles`
ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`account_id`) ON DELETE CASCADE;
```

Vì:

* `users` là bảng profile chính.
* `employee_profiles` là bảng mở rộng cho nhân viên.
* Không phải user nào cũng là employee.
* Employee profile nên phụ thuộc vào user, không phải ngược lại.

---

# 10. Scope Chưa Bao Gồm

Các chức năng sau chưa nằm trong scope hiện tại:

* Email verification
* Account activation bằng email
* Send mail xác thực
* Forgot password
* Refresh token flow hoàn chỉnh
* Logout API
* User profile API public hoàn chỉnh
* Tách transaction hoàn chỉnh giữa Auth Service và User Service
* Kafka event cho user registered

Các chức năng này sẽ được tách thành issue riêng ở sprint sau.

---

# 11. Quy Tắc API Contract Cho Các Sprint Sau

Từ các sprint sau, mỗi API mới nên có Markdown API Specification trước hoặc song song với lúc implement.

Flow đề xuất:

```txt
API Specification
→ Backend Implement
→ Frontend Integration
→ Test
→ Review/Merge
```

Frontend sẽ dựa vào API Specification để chuẩn bị:

* UI form
* Validate input
* Request body
* Mock response
* Success handling
* Error handling
* API service function

Backend sẽ dựa vào API Specification để đảm bảo:

* Endpoint đúng
* Request body đúng
* Response format đúng
* Status code đúng
* Error code rõ ràng

Nếu Backend thay đổi request/response, tài liệu API Specification phải được cập nhật trong cùng MR hoặc issue liên quan.
