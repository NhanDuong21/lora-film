# Frontend Auth & Real API Integration Specification - LoraFilm

Tài liệu này mô tả cách React Frontend tích hợp với hệ thống Backend thông qua API Gateway trong dự án LoraFilm.

Tài liệu này phản ánh trạng thái mới của Frontend sau khi đã migrate các màn hình từ UI prototype vào repo chính và chuyển repo thật sang nguyên tắc:

> Repo Frontend thật chỉ sử dụng API thật.
> Các màn chưa có API thật vẫn giữ UI shell / empty state, không dùng mock business data.

---

## 1. Tổng Quan

Frontend hiện sử dụng React + Vite và gọi API thông qua API Gateway.

Luồng gọi API chuẩn:

```txt
React Frontend
→ API Gateway :8080
→ Auth Service / User Service
```

Frontend không gọi trực tiếp service nội bộ.

---

## 2. API Gateway Base URL

Frontend sử dụng biến môi trường để cấu hình API Gateway:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Gateway base URL local:

```txt
http://localhost:8080
```

Frontend tuyệt đối không gọi trực tiếp:

```txt
http://localhost:8081
http://localhost:8086
/internal/users
```

---

## 3. Các API Thật Hiện Có

### 3.1. API Gateway

```txt
GET /health
```

### 3.2. Auth APIs

Frontend gọi các API Auth thông qua API Gateway:

```txt
POST /api/auth/register
POST /api/auth/verify
POST /api/auth/login
POST /api/auth/refresh-token
```

### 3.3. User APIs

Frontend gọi User Profile API thông qua API Gateway:

```txt
GET /api/users/{accountId}
```

### 3.4. Internal API Không Dùng Cho Frontend

Endpoint sau chỉ dành cho internal/backend flow, Frontend không được gọi:

```txt
POST /internal/users
```

---

## 4. Cấu Trúc Frontend Hiện Tại

```txt
client/src
 ┣ assets
 ┣ components
 ┃ ┣ admin
 ┃ ┣ common
 ┃ ┣ customer
 ┃ ┣ employee
 ┃ ┣ home
 ┃ ┗ layout
 ┣ contexts
 ┃ ┣ AuthContext.jsx
 ┃ ┗ DataContext.jsx
 ┣ pages
 ┃ ┣ admin
 ┃ ┣ auth
 ┃ ┃ ┣ Login.jsx
 ┃ ┃ ┣ Register.jsx
 ┃ ┃ ┗ VerifyOtp.jsx
 ┃ ┣ customer
 ┃ ┃ ┣ CustomerProfilePage.jsx
 ┃ ┃ ┣ CinemaDetailPage.jsx
 ┃ ┃ ┣ MasterBookingFunnelPage.jsx
 ┃ ┃ ┣ MovieDetailPage.jsx
 ┃ ┃ ┣ MovieDiscoveryPage.jsx
 ┃ ┃ ┗ SeatSelectionPage.jsx
 ┃ ┣ employee
 ┃ ┗ public
 ┃ ┃ ┗ Home.jsx
 ┣ routes
 ┃ ┗ AppRoutes.jsx
 ┣ services
 ┃ ┣ authService.js
 ┃ ┣ cccdService.js
 ┃ ┗ userService.js
 ┣ utils
 ┃ ┗ authStorage.js
 ┣ App.jsx
 ┣ index.css
 ┗ main.jsx
```

---

## 5. Quy Tắc Dữ Liệu Thật Và Mock Data

Repo Frontend thật không sử dụng mock business data.

Các màn chưa có API thật vẫn được giữ lại UI shell, nhưng không hiển thị dữ liệu giả.

Ví dụ:

* Movie Discovery chưa có Movie API thật.
* Movie Detail chưa có Movie API thật.
* Seat Selection chưa có Booking/Seat API thật.
* Admin pages chưa có API thật.
* Employee pages chưa có API thật.

Các màn này cần hiển thị empty state, ví dụ:

```txt
No real data available yet.
This module is waiting for backend API integration.
```

Mock data vẫn có thể được duy trì ở project UI prototype riêng, không dùng trong repo thật.

---

## 6. Service Layer

Frontend gọi API thông qua service layer, không gọi trực tiếp API trong component/page.

### 6.1. `authService.js`

Đường dẫn:

```txt
client/src/services/authService.js
```

Chứa các function:

```js
register(payload)
verifyOtp(payload)
login(payload)
refreshToken(refreshToken)
```

Mapping API:

```txt
register     -> POST /api/auth/register
verifyOtp    -> POST /api/auth/verify
login        -> POST /api/auth/login
refreshToken -> POST /api/auth/refresh-token
```

---

### 6.2. `userService.js`

Đường dẫn:

```txt
client/src/services/userService.js
```

Chứa function:

```js
getUserProfile(accountId)
```

Mapping API:

```txt
GET /api/users/{accountId}
```

API này cần gửi header:

```txt
Authorization: Bearer <authToken>
```

---

### 6.3. `cccdService.js`

Đường dẫn:

```txt
client/src/services/cccdService.js
```

Dùng để gọi CCCD Check API bên ngoài:

```txt
POST https://api-check-cccd.lorafilm.xyz/api/cccd/check
```

Headers:

```txt
Content-Type: application/json
x-api-key: <VITE_CCCD_API_KEY>
```

CCCD API chỉ kiểm tra định dạng CCCD, không xác minh CCCD có tồn tại trong cơ sở dữ liệu quốc gia.

---

## 7. Auth Storage

File:

```txt
client/src/utils/authStorage.js
```

Auth storage quản lý trạng thái phiên đăng nhập trong browser.

Các key được phép lưu:

```txt
authToken
refreshToken
tokenType
userEmail
userRole
accountId
pendingAccountId
```

Các function chính:

```js
setAuthData(data)
getAuthToken()
getRefreshToken()
getUserEmail()
getUserRole()
getAccountId()
clearAuthData()
isAuthenticated()
setPendingAccountId(accountId)
getPendingAccountId()
clearPendingAccountId()
```

Quy tắc bảo mật:

* Không lưu full CCCD vào localStorage.
* Không log full CCCD ra console.
* Không hiển thị full CCCD trên UI.
* Chỉ hiển thị `cccdMasked`.

---

## 8. Route Mapping

### 8.1. Public Routes

```txt
/ -> pages/public/Home.jsx
```

### 8.2. Auth Routes

```txt
/login      -> pages/auth/Login.jsx
/register   -> pages/auth/Register.jsx
/verify-otp -> pages/auth/VerifyOtp.jsx
```

### 8.3. Customer Routes

```txt
/profile -> pages/customer/CustomerProfilePage.jsx
```

Các route customer khác hiện chủ yếu là UI shell / empty state nếu chưa có API thật:

```txt
/movies
/movies/:id
/cinemas/:id
/booking
/seats
```

Tên route cụ thể phụ thuộc `AppRoutes.jsx`.

### 8.4. Admin Routes

Admin pages nằm trong:

```txt
pages/admin
```

Hiện tại các màn admin đã được migrate UI, nhưng chưa nằm trong scope API thật nếu backend chưa có endpoint tương ứng.

### 8.5. Employee Routes

Employee pages nằm trong:

```txt
pages/employee
```

Hiện tại các màn employee đã được migrate UI, nhưng chưa nằm trong scope API thật nếu backend chưa có endpoint tương ứng.

---

## 9. Header Authentication State

Header nằm tại:

```txt
client/src/components/layout/Header.jsx
```

Header phải phản ánh đúng trạng thái đăng nhập.

### 9.1. Khi Chưa Đăng Nhập

Header hiển thị:

* Login
* Register nếu UI có

Click Login điều hướng tới:

```txt
/login
```

Click Register điều hướng tới:

```txt
/register
```

---

### 9.2. Khi Đã Đăng Nhập

Header không hiển thị Login button.

Header nên hiển thị:

* Email hoặc tên người dùng nếu có.
* Role nếu có.
* Profile action.
* Logout action.

Click Profile điều hướng tới:

```txt
/profile
```

Click Logout cần:

* Gọi `clearAuthData()`.
* Cập nhật Header về trạng thái guest.
* Điều hướng về `/` hoặc `/login` tùy UX.

---

## 10. Register Flow

Page:

```txt
client/src/pages/auth/Register.jsx
```

API:

```txt
POST ${VITE_API_BASE_URL}/api/auth/register
```

Request body:

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

Sau khi register thành công:

1. Lấy `accountId` từ response nếu có.
2. Lưu tạm `pendingAccountId`.
3. Hiển thị thông báo đăng ký thành công.
4. Điều hướng sang `/verify-otp`.
5. Không tự động login sau register.

Register không gọi User Service để tạo profile.

Frontend không gọi:

```txt
/internal/users
```

---

## 11. CCCD Check Flow

Trong Register page, khi người dùng nhập đủ 12 số CCCD, Frontend có thể gọi CCCD Check API.

API:

```txt
POST ${VITE_CCCD_API_URL}
```

Headers:

```txt
Content-Type: application/json
x-api-key: ${VITE_CCCD_API_KEY}
```

Request body:

```json
{
  "cccd": "092205006789"
}
```

Response success mẫu:

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
  "message": "CCCD format is valid"
}
```

Frontend cần:

* Hiển thị `provinceName`.
* Hiển thị `genderLabel` hoặc `gender`.
* Hiển thị `birthYear`.
* Hiển thị `cccdMasked`.
* Đối chiếu `birthday` với `birthYear`.
* Nếu năm sinh không khớp, không cho submit.
* Không lưu full CCCD vào localStorage.
* Không log full CCCD ra console.

---

## 12. Verify OTP Flow

Page:

```txt
client/src/pages/auth/VerifyOtp.jsx
```

API:

```txt
POST ${VITE_API_BASE_URL}/api/auth/verify
```

Request body:

```json
{
  "accountId": 1,
  "otp": "123456"
}
```

Flow:

1. Lấy `pendingAccountId`.
2. Nếu không có `pendingAccountId`, điều hướng về `/register` hoặc `/login`.
3. User nhập OTP.
4. Submit OTP qua API Gateway.
5. Nếu verify thành công, xóa `pendingAccountId`.
6. Điều hướng sang `/login`.

Error handling:

```txt
AUTH_INVALID_OTP              -> OTP không chính xác.
AUTH_VERIFICATION_EXPIRED     -> OTP đã hết hạn.
AUTH_ACCOUNT_NOT_FOUND        -> Không tìm thấy tài khoản.
```

---

## 13. Login Flow

Page:

```txt
client/src/pages/auth/Login.jsx
```

API:

```txt
POST ${VITE_API_BASE_URL}/api/auth/login
```

Request body:

```json
{
  "email": "user@example.com",
  "password": "User@123"
}
```

Response success mẫu:

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "token": "jwt-token",
    "tokenType": "Bearer",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "accessToken": "jwt-token",
    "refreshToken": "uuid-token",
    "expiresIn": 86400
  }
}
```

Frontend xử lý:

1. Lấy `data.accessToken` hoặc fallback sang `data.token`.
2. Lưu token bằng `setAuthData(data)`.
3. Lưu `refreshToken`, `tokenType`, `userEmail`, `userRole`.
4. Nếu response có `accountId`, lưu `accountId`.
5. Điều hướng về `/`.
6. Header cập nhật trạng thái đã đăng nhập.

Error handling:

```txt
AUTH_INVALID_CREDENTIALS      -> Email hoặc mật khẩu không chính xác.
AUTH_ACCOUNT_NOT_VERIFIED     -> Tài khoản chưa xác thực OTP.
AUTH_ACCOUNT_INACTIVE         -> Tài khoản bị khóa hoặc chưa kích hoạt.
VALIDATION_ERROR              -> Dữ liệu nhập không hợp lệ.
INTERNAL_SERVER_ERROR         -> Lỗi hệ thống.
```

Nếu lỗi `AUTH_ACCOUNT_NOT_VERIFIED` có kèm `accountId`, Frontend có thể lưu `pendingAccountId` và điều hướng sang `/verify-otp`.

---

## 14. Refresh Token Flow

API:

```txt
POST ${VITE_API_BASE_URL}/api/auth/refresh-token
```

Request body:

```json
{
  "refreshToken": "uuid-refresh-token-here"
}
```

Response success mẫu:

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "token": "new-jwt-token",
    "accessToken": "new-jwt-token",
    "refreshToken": "new-uuid-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "email": "user@example.com",
    "role": "CUSTOMER"
  }
}
```

Frontend xử lý:

* Update `authToken`.
* Update `refreshToken`.
* Update `tokenType`.
* Update `userEmail`.
* Update `userRole`.
* Nếu refresh token invalid/expired thì clear auth storage và điều hướng về `/login`.

Trong Sprint này chưa bắt buộc phải implement auto refresh phức tạp.

---

## 15. Customer Profile Flow

Page:

```txt
client/src/pages/customer/CustomerProfilePage.jsx
```

Route:

```txt
/profile
```

API:

```txt
GET ${VITE_API_BASE_URL}/api/users/{accountId}
```

Headers:

```txt
Authorization: Bearer <authToken>
```

Response success mẫu:

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

Frontend xử lý:

* Nếu có `accountId`, gọi API profile.
* Nếu chưa có `accountId`, có thể decode JWT nếu token chứa `userId/accountId`.
* Nếu vẫn không có `accountId`, hiển thị message rõ ràng.
* Không fallback sang mock profile.
* Chỉ hiển thị `cccdMasked`.
* Không hiển thị full CCCD.

Khuyến nghị Backend:

* Nên bổ sung `accountId` vào Login response để Frontend lấy profile sạch hơn.
* Hoặc thống nhất claim JWT là `accountId`.

---

## 16. Kafka ACCOUNT_CREATED Status

Luồng tạo profile người dùng không do Frontend xử lý.

Frontend chỉ gọi:

```txt
POST /api/auth/register
POST /api/auth/verify
```

Sau khi account được xác thực, việc tạo User Profile thuộc trách nhiệm Backend thông qua Kafka event `ACCOUNT_CREATED`.

Frontend không gọi:

```txt
POST /internal/users
```

Nếu Kafka đã hoàn thành ở Backend, tài liệu Backend/Event Contract cần mô tả chi tiết topic, payload và consumer behavior.

Nếu Kafka chưa ổn định ở môi trường local của từng member, cần ghi rõ trong MR/test note.

---

## 17. Role & Permission Direction

Các role chính dự kiến:

```txt
ADMIN
EMPLOYEE
CUSTOMER
```

Customer:

* Xem phim.
* Đặt vé.
* Xem profile.
* Xem lịch sử đặt vé sau này.

Admin:

* Quản lý phim.
* Quản lý rạp.
* Quản lý suất chiếu.
* Quản lý nhân sự.
* Quản lý sự kiện.
* Xem tài chính.

Employee:

Employee dùng role `EMPLOYEE`, sau đó phân biệt quyền bằng permission hoặc position.

Permission/position dự kiến:

```txt
STAFF
SUPERVISOR
ACCOUNTANT
```

Hoặc permission flags:

```txt
CHECK_IN_TICKET
SELL_TICKET
MANAGE_SHOWTIME
VIEW_FINANCE
MANAGE_STAFF
```

Trong scope hiện tại chưa implement full permission system.

---

## 18. Validation Steps

### 18.1. Start Backend

```bash
cd server/auth-service
mvn spring-boot:run
```

```bash
cd server/user-service
mvn spring-boot:run
```

```bash
cd api-gateway
mvn spring-boot:run
```

---

### 18.2. Start Frontend

```bash
cd client
npm install
npm run dev
```

---

### 18.3. Test Header

1. Mở `/`.
2. Khi chưa login, Header hiển thị Login/Register.
3. Click Login chuyển tới `/login`.
4. Click Register chuyển tới `/register`.

---

### 18.4. Test Register

1. Mở `/register`.
2. Nhập CCCD hợp lệ.
3. Kiểm tra CCCD derived info nếu flow đang bật.
4. Submit Register.
5. Kiểm tra request đi tới:

```txt
http://localhost:8080/api/auth/register
```

6. Kiểm tra không có request tới:

```txt
http://localhost:8081
http://localhost:8086
/internal/users
```

7. Register success chuyển sang `/verify-otp`.

---

### 18.5. Test Verify OTP

1. Lấy OTP từ backend console hoặc email nếu backend đã có email flow.
2. Nhập OTP ở `/verify-otp`.
3. Kiểm tra request đi tới:

```txt
http://localhost:8080/api/auth/verify
```

4. Verify success chuyển sang `/login`.

---

### 18.6. Test Login

1. Mở `/login`.
2. Login bằng tài khoản hợp lệ.
3. Kiểm tra request đi tới:

```txt
http://localhost:8080/api/auth/login
```

4. Kiểm tra localStorage có:

```txt
authToken
refreshToken
tokenType
userEmail
userRole
```

5. Header chuyển sang trạng thái authenticated.

---

### 18.7. Test Profile

1. Sau khi login, click Profile.
2. Route chuyển tới `/profile`.
3. Nếu có `accountId`, kiểm tra request đi tới:

```txt
http://localhost:8080/api/users/{accountId}
```

4. Kiểm tra có Authorization header.
5. Kiểm tra UI chỉ hiển thị `cccdMasked`.
6. Kiểm tra UI không hiển thị full CCCD.

---

### 18.8. Test Screens Without API

Mở các màn chưa có API thật:

* Movie Discovery
* Movie Detail
* Seat Selection
* Booking Funnel
* Admin pages
* Employee pages

Kỳ vọng:

* Page render được.
* Không crash.
* Không hiển thị mock data.
* Có empty state hoặc thông báo chờ API.

---

## 19. Forbidden String Check

PowerShell:

```powershell
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "localhost:8081"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "localhost:8086"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "/internal/users"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "authMock"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "mockData"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "mockDashboardData"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "localStorage.setItem(`"cccd"
Get-ChildItem -Path client\src -Recurse -File | Select-String -SimpleMatch -Pattern "console.log(cccd"
```

---

## 20. Current Limitations

* Login response có thể chưa trả `accountId`, nên Profile auto fetch có thể cần decode JWT hoặc chờ Backend bổ sung `accountId`.
* Movie API chưa nằm trong scope hiện tại.
* Booking API chưa nằm trong scope hiện tại.
* Admin APIs chưa nằm trong scope hiện tại.
* Employee APIs chưa nằm trong scope hiện tại.
* Admin/Employee permission chưa implement đầy đủ.
* Các màn chưa có API thật chỉ giữ UI shell và empty state.
* Full permission guard sẽ được xử lý ở issue riêng.

---

## 21. Summary

Frontend hiện tại đã có baseline thật cho:

```txt
Home
Register
Verify OTP
Login
Customer Profile
Header Auth State
```

Repo thật chỉ sử dụng dữ liệu từ API thật thông qua API Gateway.

Các màn chưa có API thật vẫn được giữ lại để bảo toàn flow UI, nhưng không sử dụng mock business data trong repo thật.
