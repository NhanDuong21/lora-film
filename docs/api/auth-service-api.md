# Auth Service API Specification

## 1. Thông Tin Chung

| Mục | Nội dung |
| :--- | :--- |
| Service | `auth-service` |
| Feature | Authentication |
| API liên quan | Register, Login, Verify, Refresh Token |
| Issue liên quan | #15, #16, #19, #42, #48, #49, #50, #51 |
| Người phụ trách BE | Trần Hiển Vinh |
| Người phụ trách FE | Dương Thiện Nhân |
| Trạng thái         | Ready for Review (Refactored) |
| Ngày cập nhật      | 23/06/2026 |

---

## Lịch Sử Chỉnh Sửa

**Ngày:** 23/06/2026

* **Kiến trúc mới:** Thay toàn bộ luồng đăng ký sang Event-Driven với Kafka Request-Reply Pattern. Auth Service không còn publish `ACCOUNT_CREATED` hay gọi HTTP sang User Service nữa.
* **Luồng mới:** Register → `REGISTRATION_VALIDATION_REQUESTED` → User Service validate → `REGISTRATION_VALIDATION_RESULT` → Auth tạo account + gửi OTP → Verify OTP → `ACCOUNT_VERIFIED` → User Service tạo profile.
* **Response mới:** API Register giờ chỉ trả về `requestId` và `message`, không còn trả về profile data.
* **Verify mới:** API Verify chỉ nhận `email` (không còn `accountId`).

**Ngày:** 20/06/2026

* **Cập nhật Backend:** Đồng bộ request body của hai API `send-otp` và `resend-otp` (chỉ nhận trường `email` thay vì `accountId`).
* **Cập nhật Backend:** Mở rộng logic `resend-otp` cho nhiều mục đích (FORGOTTEN PASSWORD, LOGIN...) thay vì chỉ giới hạn xác thực tài khoản (REGISTRATION).
* **Cập nhật API Contract:** Xóa bỏ hoàn toàn trường `token` bị trùng lặp trong cấu trúc phản hồi của API Login và Refresh Token.

**Ngày:** 19/06/2026 | **Người chỉnh sửa:** Trần Hiển Vinh

* **Cập nhật Backend:** Loại bỏ trường `token` trong `JwtResponse.java` để dọn dẹp API Contract (chỉ giữ lại `accessToken`).

- **17/06/2026**: Cập nhật các quy tắc validate cho fullName, phoneNumber, password, birthday (giới hạn tuổi >= 13, không ở tương lai) để đồng bộ hoàn toàn với Frontend.

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API Authentication của hệ thống **LoraFilm**.

Mục tiêu chính:

* Giúp Backend và Frontend thống nhất request/response trước hoặc song song với quá trình implement.
* Giúp Frontend có thể code form, validate input, mock response và xử lý success/error mà không cần đọc trực tiếp source code Backend.
* Làm API Contract cho các API Register và Login, bổ sung thêm luồng Verify OTP và Refresh Token.
* Ghi rõ Frontend phải gọi API thông qua API Gateway, không gọi trực tiếp `auth-service`.
* Làm cơ sở để Backend refactor Register/Login theo đúng contract đã thống nhất.

---

## 3. API Gateway Và Service URL

### 3.1. API Gateway URL

Frontend sử dụng URL này để gọi API:
`http://localhost:8080`

### 3.2. Auth Service Direct URL

Chỉ dùng cho Backend debug hoặc test riêng service:
`http://localhost:8081`

### 3.3. CCCD Check API URL

API kiểm tra CCCD theo quy luật định dạng:
`https://api-check-cccd.lorafilm.xyz/api/cccd/check`

### 3.4. Request Flow Chính

```txt
React Frontend
→ API Gateway :8080
→ Auth Service :8081
→ MySQL
```

### 3.5. Request Flow Có Kiểm Tra CCCD và OTP

```txt
React Register Form
→ CCCD Check API (validate CCCD format)
→ Fill / validate derived information
→ API Gateway :8080
→ Auth Service :8081
   ↳ Check duplicate email
   ↳ Validate CCCD via CCCD Check API
   ↳ Save registration data temporarily in Redis
   ↳ Publish REGISTRATION_VALIDATION_REQUESTED to Kafka
   ↳ Wait (up to 10s) for validation result
← User Service consumes event, checks phone/CCCD in DB + Redis reservation
← User Service publishes REGISTRATION_VALIDATION_RESULT to Kafka
→ Auth Service resumes:
   ↳ If FAILED → return 409 Conflict
   ↳ If SUCCESS → create account in DB (account_status='PENDING')
   ↳ Send OTP to email
   ↳ Return 202 Accepted with requestId
→ React OTP Verify Form
→ API Gateway :8080 → Auth Service :8081 (verify OTP)
   ↳ Activate account (account_status='ACTIVE')
   ↳ Publish ACCOUNT_VERIFIED to Kafka
← User Service consumes ACCOUNT_VERIFIED → creates User Profile in DB
→ Redirect to Login
```

---

## 4. Quy Tắc Gọi API

Frontend chỉ được gọi API thông qua API Gateway:

* `POST http://localhost:8080/api/auth/register`
* `POST http://localhost:8080/api/auth/verify`
* `POST http://localhost:8080/api/auth/login`
* `POST http://localhost:8080/api/auth/refresh-token`

Frontend không gọi trực tiếp Auth Service:

* `POST http://localhost:8081/api/auth/...`

Auth Service direct URL chỉ dùng cho backend developer test/debug.

---

## 5. CCCD Check API

### 5.1. Mục Tiêu

API này dùng để kiểm tra định dạng CCCD theo quy luật số CCCD Việt Nam.
Frontend có thể dùng API này ở màn hình Register để người dùng chỉ cần nhập CCCD, sau đó hệ thống suy ra một số thông tin như:

* Mã tỉnh/thành
* Tên tỉnh/thành
* Giới tính
* Năm sinh
* Tuổi theo năm

Lưu ý: API này **chỉ kiểm tra định dạng CCCD**, không xác minh CCCD có tồn tại thật trong cơ sở dữ liệu quốc gia.

### 5.2. Thông Tin Endpoint

| Mục | Nội dung |
| ------------------ | -------------------------------------- |
| Method | POST |
| Endpoint | `/api/cccd/check` |
| Full URL | `https://api-check-cccd.lorafilm.xyz/api/cccd/check` |
| Content-Type | application/json |
| Auth Required | Theo cấu hình API key |
| Role Required | None |

### 5.3. Request Headers

| Header | Required | Example | Mô tả |
| :--- | :--- | :--- | :--- |
| Content-Type | Yes | application/json | Kiểu dữ liệu gửi lên |
| x-api-key | Yes | lora_cccd_2026_secret | API key service yêu cầu |

Ví dụ:

```http
Content-Type: application/json
x-api-key: lora_cccd_2026_secret
```

### 5.4. Request Body

```json
{
  "cccd": "092205006789"
}
```

### 5.5. Giải Thích Field Request

| Field | Type | Required | Validate | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| cccd | string | Yes | 12 chữ số | Số CCCD người dùng nhập |

### 5.6. Response Success

Status: **200 OK**

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

### 5.7. Giải Thích Field Response

| Field | Type | Mô tả |
| :--- | :--- | :--- |
| valid | boolean | CCCD có hợp lệ theo format hay không |
| cccdMasked | string | CCCD đã được che bớt để hiển thị an toàn |
| provinceCode | string | Mã tỉnh/thành trong CCCD |
| provinceName | string | Tên tỉnh/thành suy ra từ mã CCCD |
| gender | string | Giới tính dạng enum, ví dụ `MALE`, `FEMALE` |
| genderLabel | string | Giới tính hiển thị tiếng Việt |
| birthYear | number | Năm sinh suy ra từ CCCD |
| ageByYear | number | Tuổi tính theo năm hiện tại |
| randomCode | string | Dãy số ngẫu nhiên cuối CCCD |
| message | string | Thông báo kết quả |
| note | string | Ghi chú phạm vi kiểm tra |

### 5.8. Response Error

**Case 1: CCCD sai định dạng**
Status: **400 Bad Request**

```json
{
  "valid": false,
  "message": "CCCD format is invalid",
  "note": "This API only checks CCCD format. It does not verify whether the CCCD exists in the national citizen database."
}
```

**Case 2: Thiếu API key hoặc API key không hợp lệ**
Status: **401 Unauthorized**

```json
{
  "success": false,
  "message": "Invalid API key",
  "errorCode": "INVALID_API_KEY"
}
```

**Case 3: Lỗi server CCCD API**
Status: **500 Internal Server Error**

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR"
}
```

### 5.9. Frontend Handling

Khi người dùng nhập CCCD ở form Register:

1. Frontend gọi CCCD Check API.
2. Nếu `valid = true`, frontend có thể tự động hiển thị hoặc gán các thông tin:
   * provinceName
   * gender
   * birthYear
   * cccdMasked
3. Frontend vẫn cần người dùng nhập các field bắt buộc khác:
   * Họ tên
   * Email
   * Số điện thoại
   * CCCD
   * Ngày sinh
   * Password
4. Frontend có thể đối chiếu `birthYear` từ CCCD với `birthday` người dùng nhập. Nếu năm sinh trong CCCD không khớp với ngày sinh, frontend hiển thị cảnh báo hoặc không cho submit.
5. Frontend không được lưu CCCD vào `localStorage`.

### 5.10. Lưu Ý Bảo Mật

* Không nên hiển thị đầy đủ CCCD sau khi người dùng nhập.
* Có thể hiển thị dạng masked: `092******789`.
* Không log CCCD đầy đủ ở frontend console.
* Không lưu CCCD vào localStorage.
* CCCD là dữ liệu cá nhân nhạy cảm, chỉ lưu ở database nếu thật sự cần cho nghiệp vụ.

---

## 6. Register API

### 6.1. Mục Tiêu API

API này dùng để đăng ký tài khoản người dùng.
Frontend gửi thông tin đăng ký lên Backend thông qua API Gateway. Backend thực hiện:
1. Kiểm tra email không trùng lặp.
2. Validate format CCCD qua CCCD Check API.
3. Lưu dữ liệu đăng ký tạm thời vào Redis.
4. Publish sự kiện `REGISTRATION_VALIDATION_REQUESTED` lên Kafka để User Service kiểm tra phone/CCCD.
5. Chờ kết quả validation (tối đa 10 giây).
6. Nếu validation thành công: tạo tài khoản trong DB (`account_status='PENDING'`) và gửi OTP qua email.
7. Trả về `requestId` cho Frontend.

Sau khi OTP được xác thực thành công (qua `/api/auth/verify`), Auth Service mới publish sự kiện `ACCOUNT_VERIFIED` lên Kafka để User Service tạo User Profile.

### 6.2. Trạng Thái Implement

| Mục | Nội dung |
| :--- | :--- |
| Contract Status | Ready for Backend Refactor |
| Implementation Target | Register API sẽ được Vinh refactor theo contract này (có tích hợp OTP) |
| FE Usage | Frontend code theo request/response trong tài liệu này |
| CCCD Flow | Frontend check CCCD trước khi submit Register |
| User Profile | Dữ liệu profile thuộc phạm vi User Service |

### 6.3. Thông Tin Endpoint

| Mục | Nội dung |
| :--- | :--- |
| Method | POST |
| Endpoint | `/api/auth/register` |
| Local URL | `http://localhost:8081/api/auth/register` |
| Gateway URL | `http://localhost:8080/api/auth/register` |
| Content-Type | application/json |
| Auth Required | No |
| Role Required | None |

### 6.4. Request Headers

| Header | Required | Example | Mô tả |
| :--- | :--- | :--- | :--- |
| Content-Type | Yes | application/json | Kiểu dữ liệu gửi lên |

Ví dụ:

```http
Content-Type: application/json
```

### 6.5. Request Body

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

### 6.6. Giải Thích Field Request

| Field | Type | Required | Validate | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| fullName | string | Yes | Dài 2-200 ký tự, chỉ chứa chữ cái và khoảng trắng, ít nhất 2 từ | Họ tên người dùng |
| email | string | Yes | Đúng format email, tối đa 100 ký tự | Email đăng ký, dùng làm tài khoản đăng nhập |
| phoneNumber | string | Yes | Đúng format số điện thoại Việt Nam gồm 10 chữ số bắt đầu bằng 0 | Số điện thoại người dùng |
| cccd | string | Yes | Đúng 12 chữ số | Số CCCD người dùng |
| birthday | date | Yes | Format `YYYY-MM-DD` | Ngày sinh người dùng nhập |
| password | string | Yes | Dài 8-50 ký tự, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số, và 1 ký tự đặc biệt | Mật khẩu đăng ký |

### 6.7. Derived Fields Từ CCCD

Các field sau không nhất thiết để người dùng nhập tay. Hệ thống có thể lấy từ CCCD Check API:

| Field | Source | Mô tả |
| :--- | :--- | :--- |
| cccdMasked | CCCD Check API | CCCD đã che bớt |
| provinceCode | CCCD Check API | Mã tỉnh/thành |
| provinceName | CCCD Check API | Tên tỉnh/thành |
| gender | CCCD Check API | Giới tính dạng enum |
| genderLabel | CCCD Check API | Giới tính tiếng Việt |
| birthYear | CCCD Check API | Năm sinh suy ra |
| ageByYear | CCCD Check API | Tuổi tính theo năm hiện tại |
| randomCode | CCCD Check API | Dãy số ngẫu nhiên cuối CCCD |

### 6.8. Business Rules

| Rule | Mô tả |
| :--- | :--- |
| Email unique            | Email không được trùng với tài khoản đã có trong DB (kiểm tra ngay tại Auth Service) |
| Phone/CCCD unique       | Auth Service hỏi User Service qua Kafka event trước khi tạo account; nếu trùng thì trả 409 |
| CCCD valid              | CCCD phải hợp lệ theo CCCD Check API |
| Birthday match          | Năm trong `birthday` nên khớp với `birthYear` suy ra từ CCCD |
| Birthday not in future  | Ngày sinh không được ở tương lai |
| Birthday age limit      | Người dùng đăng ký phải từ 13 tuổi trở lên |
| Default role            | User mới mặc định có role `CUSTOMER` |
| Hash password           | Mật khẩu phải được hash trước khi lưu |
| Account status          | Tài khoản mới tạo mặc định `account_status = 'PENDING'` |
| OTP Generation          | Sinh mã xác thực OTP ngẫu nhiên 6 chữ số có hiệu lực trong 5 phút và lưu tạm thời |
| Kafka Validation Timeout| Nếu User Service không phản hồi trong 10 giây → trả 500 Internal Server Error |

### 6.9. Backend Processing Flow

Receive register request
→ Validate request body
→ Check duplicate email in Auth Service DB
→ Check CCCD format via CCCD Check API / use CCCD derived information
→ Save registration data temporarily in Redis (TTL 15 minutes)
→ Publish `REGISTRATION_VALIDATION_REQUESTED` to Kafka (with requestId, phoneNumber, cccd)
→ Wait up to 10 seconds for `REGISTRATION_VALIDATION_RESULT` from User Service
   ↳ If FAILED (phone/CCCD duplicate) → return 409 Conflict
   ↳ If TIMEOUT → return 500 Internal Server Error
→ Create account in Auth Service DB (`account_status = 'PENDING'`)
→ Assign default role `CUSTOMER`
→ Hash password
→ Send OTP via email
→ Return 202 Accepted with requestId and message

### 6.10. Thông Tin Tạo User Profile

User Profile **không được tạo ngay** khi Register. Nó chỉ được tạo sau khi OTP xác thực thành công (Verify OTP), thông qua sự kiện Kafka `ACCOUNT_VERIFIED`. Dữ liệu profile sẽ được lấy từ bản ghi tạm thời trong Redis.

### 6.11. Response Success

Status: **202 Accepted**

```json
{
  "success": true,
  "message": "Registration initiated",
  "data": {
    "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "message": "Registration successful, please check your email for OTP"
  }
}
```

### 6.12. Giải Thích Field Response

| Field | Type | Mô tả |
| :--- | :--- | :--- |
| success | boolean | Trạng thái xử lý thành công hay thất bại |
| message | string | Thông báo kết quả |
| data.requestId | string | UUID request (dùng để tracking, không cần gửi lại) |
| data.message | string | Thông báo hướng dẫn người dùng kiểm tra email OTP |

### 6.13. Response Error

**Case 1: Email đã tồn tại**
Status: **409 Conflict**

```json
{
  "success": false,
  "message": "Email already exists",
  "errorCode": "AUTH_EMAIL_ALREADY_EXISTS",
  "data": null
}
```

**Case 2: Lỗi trùng lặp dữ liệu (Đã tồn tại)**
Status: **409 Conflict**

Trùng Số Điện Thoại:
```json
{
  "success": false,
  "message": "Phone number already exists.",
  "errorCode": "PHONE_NUMBER_ALREADY_EXISTS",
  "data": null
}
```

Trùng CCCD:
```json
{
  "success": false,
  "message": "CCCD already exists.",
  "errorCode": "CCCD_ALREADY_EXISTS",
  "data": null
}
```

**Case 3: Lỗi dữ liệu đang được giữ chỗ (Reserved)**
Status: **409 Conflict**
*HTTP Header kèm theo:* `Retry-After: 472`

Số điện thoại đang được đăng ký (chờ OTP):
```json
{
  "success": false,
  "message": "Phone number is currently reserved by another pending registration. Please try again later.",
  "errorCode": "PHONE_NUMBER_RESERVED",
  "data": {
    "retryAfterSeconds": 472
  }
}
```

CCCD đang được đăng ký (chờ OTP):
```json
{
  "success": false,
  "message": "CCCD is currently reserved by another pending registration. Please try again later.",
  "errorCode": "CCCD_RESERVED",
  "data": {
    "retryAfterSeconds": 472
  }
}
```

**Case 4: Email đã có yêu cầu đăng ký đang chờ (Pending Registration)**
Status: **409 Conflict**

```json
{
  "success": false,
  "message": "Registration is already pending verification. Please verify the OTP or request a new OTP.",
  "errorCode": "REGISTRATION_ALREADY_PENDING",
  "data": null
}
```

**Case 5: CCCD không hợp lệ**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "CCCD format is invalid",
  "errorCode": "USER_CCCD_INVALID",
  "data": null
}
```

**Case 6: Ngày sinh không khớp với CCCD**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "Birthday does not match CCCD birth year",
  "errorCode": "USER_BIRTHDAY_CCCD_MISMATCH",
  "data": null
}
```

**Case 7: Ngày sinh ở tương lai**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "Birth dates cannot be in the future.",
  "data": null
}
```

**Case 6: Chưa đủ 13 tuổi**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "You must be 13 years old or older.",
  "data": null
}
```

**Case 7: Dữ liệu gửi lên không hợp lệ**
Status: **400 Bad Request**

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
      "message": "password length must be between 8 and 50"
    },
    {
      "field": "cccd",
      "message": "CCCD must contain 12 digits"
    }
  ]
}
```

**Case 8: Kafka validation timeout (User Service không phản hồi trong 10s)**
Status: **500 Internal Server Error**

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "data": null
}
```

### 6.14. Danh Sách Status Code

| Status Code | Ý nghĩa | Khi nào xảy ra |
| :--- | :--- | :--- |
| 202 | Accepted | Đăng ký thành công, OTP đã gửi qua email |
| 400 | Bad Request | Dữ liệu gửi lên không đúng định dạng kiểm tra |
| 409 | Conflict | Email, phone hoặc CCCD đã tồn tại |
| 500 | Internal Server Error | Gặp lỗi không xác định hoặc Kafka timeout |

---

## 7. Verify API 

### 7.1. Mục Tiêu API

API này dùng để xác thực mã định danh OTP sau khi đăng ký thành công. Khi Frontend gửi đúng mã số OTP, hệ thống sẽ chuyển đổi trạng thái `account_status` của tài khoản lên `'ACTIVE'`, chính thức mở quyền đăng nhập hệ thống.

### 7.2. Thông Tin Endpoint

| Mục | Nội dung |
| :--- | :--- |
| Method | POST |
| Endpoint | `/api/auth/verify` |
| Local URL | `http://localhost:8081/api/auth/verify` |
| Gateway URL | `http://localhost:8080/api/auth/verify` |
| Content-Type | application/json |
| Auth Required | No |

### 7.3. Request Body

```json
{
  "email": "user@example.com",
  "otp": "123456",
  "purpose": "REGISTRATION"
}
```

* Tham số `purpose` nhận một trong các giá trị: `REGISTRATION`, `FORGOTTEN PASSWORD`, `CHANGE EMAIL`, `CHANGE PASSWORD`.

### 7.4. Response Success

Status: **200 OK**

```json
{
  "success": true,
  "message": "Account verified successfully",
  "data": null
}
```

### 7.5. Response Error

**Case 1: Mã OTP không chính xác**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "Invalid OTP code",
  "errorCode": "AUTH_INVALID_OTP",
  "data": null
}
```

**Case 2: Mã OTP đã hết hiệu lực (quá 5 phút)**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "Verification OTP code has expired",
  "errorCode": "AUTH_VERIFICATION_EXPIRED",
  "data": null
}
```

**Case 3: Không tìm thấy tài khoản tương ứng**
Status: **404 Not Found**

```json
{
  "success": false,
  "message": "Account not found",
  "errorCode": "AUTH_ACCOUNT_NOT_FOUND",
  "data": null
}
```

---

## 7.5. Send OTP API

### 7.5.1. Mục Tiêu API

Gửi mã OTP cho các mục đích như REGISTER. 

| Mục | Nội dung |
| :--- | :--- |
| Method | POST |
| Endpoint | `/api/auth/send-otp` |
| Local URL | `http://localhost:8081/api/auth/send-otp` |
| Content-Type | application/json |

### 7.5.2. Request Body

```json
{
  "email": "user@example.com",
  "purpose": "REGISTRATION"
}
```

* Tham số `purpose` nhận một trong các giá trị: `REGISTRATION`, `FORGOTTEN PASSWORD`, `CHANGE EMAIL`, `CHANGE PASSWORD`.

### 7.5.3. Response Success

Status: **200 OK**

```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "accountId": 15,
    "expiresIn": 300,
    "resendAvailableIn": 60
  }
}
```

### 7.5.4. Response Error

**Case 1: Quá giới hạn rate limit (1 phút)**
Status: **429 Too Many Requests**

```json
{
  "success": false,
  "message": "Please wait before requesting another OTP.",
  "errorCode": "OTP_RATE_LIMIT",
  "data": {
    "retryAfter": 45
  }
}
```

---

## 7.6. Resend OTP API

### 7.6.1. Mục Tiêu API

Gửi lại mã OTP trong trường hợp mã cũ bị hết hạn, chưa nhận được hoặc cần lấy lại mã mới. Dùng chung cho tất cả các mục đích (REGISTRATION, FORGOTTEN PASSWORD...).

| Mục | Nội dung |
| :--- | :--- |
| Method | POST |
| Endpoint | `/api/auth/resend-otp` |
| Local URL | `http://localhost:8081/api/auth/resend-otp` |
| Content-Type | application/json |

### 7.6.2. Request Body

```json
{
  "email": "user@example.com",
  "purpose": "REGISTRATION"
}
```

* Tham số `purpose` nhận một trong các giá trị: `REGISTRATION`, `FORGOTTEN PASSWORD`, `CHANGE EMAIL`, `CHANGE PASSWORD`.

### 7.6.3. Response Success

Status: **200 OK**

```json
{
  "success": true,
  "message": "OTP resent successfully",
  "data": {
    "accountId": 15,
    "expiresIn": 300,
    "resendAvailableIn": 60
  }
}
```

### 7.6.4. Response Error

**Case 1: Tài khoản không tồn tại**
Status: **404 Not Found**

```json
{
  "success": false,
  "message": "Account not found",
  "errorCode": "AUTH_ACCOUNT_NOT_FOUND",
  "data": null
}
```

**Case 2: Tài khoản đã xác thực**
Status: **409 Conflict**

```json
{
  "success": false,
  "message": "Account is already verified",
  "errorCode": "AUTH_ACCOUNT_ALREADY_VERIFIED",
  "data": null
}
```

**Case 3: Quá giới hạn rate limit (1 phút)**
Status: **429 Too Many Requests**

```json
{
  "success": false,
  "message": "Please wait before requesting another OTP",
  "errorCode": "OTP_RATE_LIMIT",
  "data": {
    "retryAfter": 45
  }
}
```

### 7.6.5. Ghi Chú Bảo Mật & Business Rules

* Account gọi API bắt buộc phải tồn tại.
* Nếu `purpose` là `REGISTRATION` thì tài khoản phải đang trong trạng thái chờ kích hoạt (`account_status` = 'PENDING'). Các mục đích khác không bị giới hạn.
* Mỗi lần gọi `Resend OTP`, mã OTP cũ chưa sử dụng sẽ bị thay thế (ghi đè trên Redis) và tự động hết hiệu lực.
* Không trả mã OTP mới sinh dưới dạng chuỗi rõ ràng (plaintext) qua phản hồi HTTP.
* Nếu Notification Service chưa sẵn sàng (như trong môi trường DEV/TEST), hệ thống tạm thời chỉ log mã OTP ra Console.
* Áp dụng nghiêm ngặt thời gian chờ (cooldown 60s) giữa mỗi lần yêu cầu để chống spam.

**Case 1: Quá giới hạn rate limit (1 phút)**
Status: **429 Too Many Requests**

```json
{
  "success": false,
  "message": "Please wait before requesting another OTP.",
  "errorCode": "OTP_RATE_LIMIT",
  "data": {
    "retryAfter": 45
  }
}
```

**Case 2: Tài khoản không tồn tại**
Status: **404 Not Found**

```json
{
  "success": false,
  "message": "Account not found",
  "errorCode": "AUTH_ACCOUNT_NOT_FOUND",
  "data": null
}
```

**Case 3: Tài khoản đã xác thực**
Status: **409 Conflict**

```json
{
  "success": false,
  "message": "Account is already verified",
  "errorCode": "AUTH_ACCOUNT_ALREADY_VERIFIED",
  "data": null
}
```

**Case 4: Validation error**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "accountId",
      "message": "Account ID is required"
    }
  ]
}
```

---

## 8. Login API

### 8.1. Mục Tiêu API

API này dùng để đăng nhập tài khoản người dùng.
Frontend gửi `email` và `password` lên Backend. Backend kiểm tra thông tin đăng nhập. Nếu hợp lệ, Backend trả về cặp JWT token (Access Token & Refresh Token), loại token, hạn định thời gian hết hạn (`expiresIn`), email và role của người dùng.

Refresh Token được tạo khi đăng nhập thành công có thời hạn sử dụng là **5 ngày kể từ thời điểm phát hành**.

Thông tin Refresh Token được lưu trong bảng `refresh_tokens` với:
- `expiry_date = created_at + 5 days`
- `is_revoked = false`

Sau khi hết thời hạn 5 ngày, người dùng phải đăng nhập lại để nhận bộ Token mới.

### 8.2. Trạng Thái Implement

| Mục | Nội dung |
| :--- | :--- |
| Contract Status | Ready for FE |
| Implementation Status | Implemented Basic Flow & Refactored with Token Pair |
| FE Usage | Frontend có thể gọi qua API Gateway |
| Token Type | Bearer |
| Response Target | Backend cần giữ response đúng theo contract đã cập nhật này |

### 8.3. Thông Tin Endpoint

| Mục | Nội dung |
| :--- | :--- |
| Method | POST |
| Endpoint | `/api/auth/login` |
| Local URL | `http://localhost:8081/api/auth/login` |
| Gateway URL | `http://localhost:8080/api/auth/login` |
| Content-Type | application/json |
| Auth Required | No |
| Role Required | None |

### 8.4. Request Headers

| Header | Required | Example | Mô tả |
| :--- | :--- | :--- | :--- |
| Content-Type | Yes | application/json | Kiểu dữ liệu gửi lên |

Ví dụ:

```http
Content-Type: application/json
```

### 8.5. Request Body

```json
{
  "email": "nhan@gmail.com",
  "password": "Nhan@123"
}
```

### 8.6. Giải Thích Field Request

| Field | Type | Required | Validate | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| email | string | Yes | Đúng format email | Email đăng nhập |
| password | string | Yes | Không rỗng | Mật khẩu đăng nhập |

### 8.7. Response Success

Status: **200 OK**

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "tokenType": "Bearer",
    "email": "user@gmail.com",
    "role": "CUSTOMER",
    "accountId": 1,
    "accessToken": "jwt-token",
    "refreshToken": "uuid-token",
    "expiresIn": 86400
  }
}
```

### 8.8. Giải Thích Field Response

| Field | Type | Mô tả |
| :--- | :--- | :--- |
| success | boolean | Trạng thái xử lý thành công hay thất bại |
| message | string | Thông báo kết quả |
| data.accessToken| string | JWT access token mới dùng để gọi các API cần xác thực |
| data.refreshToken| string | UUID token dùng để call API refresh-token gia hạn |
| data.tokenType | string | Loại token, hiện tại là Bearer |
| data.expiresIn | number | Thời gian sống của chuỗi Access Token tính bằng giây |
| data.email | string | Email của người dùng đăng nhập |
| data.role | string | Vai trò của người dùng |
| data.accountId | number | ID của tài khoản |

### 8.9. Response Error

**Case 1: Sai email hoặc mật khẩu**
Status: **401 Unauthorized**

```json
{
  "success": false,
  "message": "Invalid email or password",
  "errorCode": "AUTH_INVALID_CREDENTIALS",
  "data": null,
  "errors": null
}
```

**Case 2: Tài khoản chưa hoàn thành xác thực OTP**
Status: **403 Forbidden**

```json
{
  "success": false,
  "message": "Account is not verified",
  "errorCode": "AUTH_ACCOUNT_NOT_VERIFIED",
  "data": {
    "accountId": 1
  }
}
```

**Case 3: Tài khoản đã bị khóa hoặc không hoạt động**
Status: **403 Forbidden**

```json
{
  "success": false,
  "message": "Account is not active",
  "errorCode": "AUTH_ACCOUNT_INACTIVE",
  "data": null
}
```

**Case 4: Dữ liệu gửi lên không hợp lệ**
Status: **400 Bad Request**

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "email is invalid"
    },
    {
      "field": "password",
      "message": "password is required"
    }
  ]
}
```

**Case 5: Lỗi server**
Status: **500 Internal Server Error**

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR",
  "data": null
}
```

### 8.10. Danh Sách Status Code

| Status Code | Ý nghĩa | Khi nào xảy ra |
| :--- | :--- | :--- |
| 200 | OK | Đăng nhập thành công và trả về bộ Token đầy đủ |
| 400 | Bad Request | Dữ liệu gửi lên không đúng định dạng kiểm tra |
| 401 | Unauthorized | Nhập sai tài khoản email hoặc mật khẩu |
| 422 | Unprocessable Content | Dữ liệu gửi lên không đúng định dạng kiểm tra |
| 403 | Forbidden | Tài khoản chưa kích hoạt OTP hoặc bị khóa (`account_status` không phải 'ACTIVE')   |

| 500 | Internal Server Error | Gặp lỗi không xác định tại hệ thống máy chủ |

---

## 9. Refresh Token API 

### 9.1. Mục Tiêu API


API này dùng để gia hạn lại phiên đăng nhập khi Access Token hết hiệu lực.

Refresh Token có thời hạn sử dụng là **5 ngày kể từ thời điểm được tạo**.

Khi Frontend gửi lên một Refresh Token hợp lệ, hệ thống sẽ:

- Kiểm tra Refresh Token có tồn tại.
- Kiểm tra Refresh Token chưa hết hạn.
- Kiểm tra Refresh Token chưa bị thu hồi (`isRevoked = false`).
- Kiểm tra tài khoản tương ứng còn hoạt động và đã hoàn thành xác thực.

Nếu hợp lệ, hệ thống sẽ:

- Đánh dấu Refresh Token hiện tại là đã thu hồi (`isRevoked = true`).
- Sinh Access Token mới.
- Sinh Refresh Token mới với thời hạn **5 ngày**.
- Lưu Refresh Token mới vào database.
- Trả về bộ Token mới cho Frontend.
### 9.2. Thông Tin Endpoint

| Mục | Nội dung |
| :--- | :--- |
| Method | POST |
| Endpoint | `/api/auth/refresh-token` |
| Local URL | `http://localhost:8081/api/auth/refresh-token` |
| Gateway URL | `http://localhost:8080/api/auth/refresh-token` |
| Content-Type | application/json |
| Auth Required | No |

### 9.3. Request Body

```json
{
  "refreshToken": "uuid-refresh-token-here"
}
```

### 9.4. Response Success

Status: **200 OK**

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "new-jwt-token-here",
    "refreshToken": "new-uuid-refresh-token-here",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "email": "nhan@gmail.com",
    "role": "CUSTOMER",
    "accountId": 1
  }
}
```
### Ghi chú

- `expiresIn` là thời gian hết hạn của Access Token.
- `refreshToken` trả về trong response là Refresh Token mới.
- Refresh Token mới có thời hạn sử dụng **5 ngày kể từ thời điểm API Refresh Token được gọi thành công**.
### 9.5. Response Error

**Case 1: Refresh Token đã hết hạn, bị hủy hoặc không tồn tại**
Status: **401 Unauthorized**

*Lưu ý: Có các thông báo lỗi cụ thể tương ứng từng trường hợp:*
- `"Refresh token not found"`
- `"Refresh token is revoked"`
- `"Refresh token is expired"`

```json
{
  "success": false,
  "message": "Refresh token not found",
  "errorCode": "AUTH_INVALID_REFRESH_TOKEN",
  "data": null
}
```

**Case 2: Không tìm thấy thông tin tài khoản đính kèm**
Status: **401 Unauthorized**

```json
{
  "success": false,
  "message": "Account not found",
  "errorCode": "AUTH_INVALID_REFRESH_TOKEN",
  "data": null
}
```

**Case 3: Tài khoản đã bị khóa hoặc không hoạt động**
Status: **403 Forbidden**

```json
{
  "success": false,
  "message": "Account is not active",
  "errorCode": "AUTH_ACCOUNT_INACTIVE",
  "data": null
}
```

**Case 4: Tài khoản chưa hoàn thành xác thực OTP**
Status: **403 Forbidden**

```json
{
  "success": false,
  "message": "Account is not verified",
  "errorCode": "AUTH_ACCOUNT_NOT_VERIFIED",
  "data": {
    "accountId": 1
  }
}
```

---

## 10. Frontend Notes

### 10.1. Token Storage

Sau khi login hoặc refresh token thành công, frontend lấy token từ:
`response.data.data.accessToken` (hoặc `.token`) và `response.data.data.refreshToken` để lưu vào `localStorage`.

Key đề xuất:
`authToken` và `refreshToken`

Ví dụ:

```javascript
localStorage.setItem("authToken", response.data.data.accessToken);
localStorage.setItem("refreshToken", response.data.data.refreshToken);
localStorage.setItem("tokenType", response.data.data.tokenType);
localStorage.setItem("userEmail", response.data.data.email);
localStorage.setItem("userRole", response.data.data.role);
```

### 10.2. Authorization Header Cho Các API Sau

Khi gọi các API cần xác thực ở sprint sau, frontend gửi header:
`Authorization: Bearer <token>`

Ví dụ:

```javascript
headers: {
  Authorization: `Bearer ${localStorage.getItem("authToken")}`
}
```

### 10.3. Register Form Fields

Register form đề xuất chỉ cần người dùng nhập:

| Field | Người dùng nhập? | Ghi chú |
| :--- | :--- | :--- |
| fullName | Yes | Họ tên |
| email | Yes | Email đăng ký |
| phoneNumber | Yes | Số điện thoại |
| cccd | Yes | CCCD |
| birthday | Yes | Ngày sinh |
| password | Yes | Mật khẩu |
| gender | No | Suy ra từ CCCD |
| provinceName | No | Suy ra từ CCCD |
| birthYear | No | Suy ra từ CCCD |

### 10.4. Register UI Flow Đề Xuất Hoàn Chỉnh

User nhập CCCD
→ Frontend gọi CCCD Check API
→ Nếu CCCD hợp lệ, hiển thị gender/province/birthYear trên UI để check
→ User điền nốt các field bắt buộc còn lại
→ User submit form Register qua API Gateway (Nhận về `accountId`)
→ **Frontend redirect sang màn hình điền mã xác thực OTP**
→ User nhập mã OTP lấy từ console log hệ thống backend gửi lên API `/api/auth/verify`
→ Kích hoạt hoàn tất, Frontend tự động điều hướng sang màn hình Login

### 10.5. Login UI Flow

User nhập email/password
→ Frontend gọi POST `/api/auth/login` qua API Gateway
→ Nếu lỗi `AUTH_ACCOUNT_NOT_VERIFIED` (403), chuyển tiếp người dùng về giao diện nhập OTP để kích hoạt lại.
→ Nếu `success = true`, lưu cả `authToken` và `refreshToken` vào localStorage
→ Redirect user theo role hoặc về Home Page
→ Nếu lỗi khác xảy ra, hiển thị rõ ràng message từ response hệ thống

---

## 11. Database Notes

### 11.1. Auth Service Schema

Auth Service nên chỉ quản lý dữ liệu đăng nhập và phân quyền:

* accounts
* roles
* permissions
* roles_permissions
* refresh_tokens
* audit_logs

Không nên lưu toàn bộ profile người dùng trong Auth Service.

### 11.2. Auth Service Schema Hiện Tại (Đã Điều Chỉnh Kiểu Dữ Liệu)

```sql
CREATE TABLE `accounts` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Logical Ref to User Service',
  `email` varchar(100) UNIQUE NOT NULL COMMENT 'Dùng làm tên đăng nhập chính',
  `password_hash` varchar(255) NOT NULL,
  `role_id` int NOT NULL,
  `account_status` varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING, ACTIVE, SUSPENDED, BLOCKED',
  `version` int DEFAULT 0 COMMENT 'For optimistic locking',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `roles` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `role_name` varchar(50) UNIQUE NOT NULL COMMENT 'CUSTOMER, STAFF, ADMIN',
  `description` varchar(255),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `permissions` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `permission_code` varchar(100) UNIQUE NOT NULL,
  `description` varchar(255),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `roles_permissions` (
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`)
);

CREATE TABLE `refresh_tokens` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `token_hash` varchar(255) UNIQUE NOT NULL,
  `expiry_date` timestamp NOT NULL,
  `is_revoked` boolean DEFAULT false,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `audit_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint,
  `action` varchar(100) NOT NULL,
  `ip_address` varchar(45),
  `user_agent` varchar(255),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint
);

CREATE TABLE `account_providers` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `provider_name` varchar(50) NOT NULL COMMENT 'google, facebook, apple, github',
  `provider_account_id` varchar(255) NOT NULL COMMENT 'Provider specific user ID (e.g. sub in JWT)',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint,
  UNIQUE KEY `uk_provider_account` (`provider_name`, `provider_account_id`)
);

ALTER TABLE `accounts` ADD CONSTRAINT `fk_accounts_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT;
ALTER TABLE `roles_permissions` ADD CONSTRAINT `fk_roles_permissions_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE;
ALTER TABLE `roles_permissions` ADD CONSTRAINT `fk_roles_permissions_permissions` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE;
ALTER TABLE `refresh_tokens` ADD CONSTRAINT `fk_refresh_tokens_accounts` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;
ALTER TABLE `account_providers` ADD CONSTRAINT `fk_account_providers_accounts` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;

CREATE INDEX `idx_accounts_email` ON `accounts` (`email`);
CREATE INDEX `idx_accounts_role_id` ON `accounts` (`role_id`);
CREATE INDEX `idx_refresh_tokens_account_id` ON `refresh_tokens` (`account_id`);
CREATE INDEX `idx_audit_logs_account_id` ON `audit_logs` (`account_id`);
CREATE INDEX `idx_audit_logs_action` ON `audit_logs` (`action`);
CREATE INDEX `idx_account_providers_account_id` ON `account_providers` (`account_id`);
```

### 11.3. User Service Schema Đề Xuất Bổ Sung

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

### 11.4. Lưu Ý Foreign Key Trong User Service

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

## 12. Scope Chưa Bao Gồm

Các chức năng sau chưa nằm trong scope hiện tại và được dời làm issue riêng ở sprint sau:

* Email verification thực tế gửi qua hòm thư (Hiện tại dùng OTP mô phỏng lưu tạm in console log)
* Account activation bằng email link gửi về
* Send mail xác thực thông báo hành động công việc
* Forgot password tìm lại mật khẩu
* Logout API hủy bỏ token chủ động từ người dùng
* User profile API public hoàn chỉnh các trường mở rộng
* Tách transaction hoàn chỉnh phân rã độc lập giữa Auth Service và User Service

---

## 13. Quy Tắc API Contract Cho Các Sprint Sau

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
