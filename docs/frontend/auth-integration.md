# Tài Liệu Tích Hợp Hệ Thống Xác Thực Frontend - LoraFilm

Tài liệu này đặc tả toàn bộ kiến trúc tích hợp hệ thống xác thực (Authentication & Authorization) giữa React Frontend (Vite) và hệ thống Backend qua API Gateway (cổng 8080) trong dự án LoraFilm. Tài liệu này đóng vai trò làm API Contract và hướng dẫn vận hành cho các lập trình viên frontend và backend.

## 1. Cấu Hình Môi Trường (Environment Variables)

Hệ thống frontend sử dụng tệp `.env` để phân giải động các biến môi trường tại thời điểm build/runtime. Tất cả các dịch vụ kết nối mạng phải sử dụng các biến này và không được sử dụng giá trị mặc định cứng.

Các biến cấu hình trong `.env` và `.env.example`:
- `VITE_API_BASE_URL`: Địa chỉ API Gateway trung tâm (http://localhost:8080). Cấm gọi trực tiếp đến các cổng dịch vụ nội bộ (8081, 8086).
- `VITE_USE_AUTH_MOCK`: Cờ bật/tắt chế độ giả lập dữ liệu xác thực (false). Chuyển sang kết nối trực tiếp API Gateway.
- `VITE_CCCD_API_URL`: Endpoint xác thực căn cước công dân bên thứ ba (https://api-check-cccd.lorafilm.xyz/api/cccd/check).
- `VITE_CCCD_API_KEY`: Mã khóa bảo mật kết nối dịch vụ CCCD (lora_cccd_2026_secret).

## 2. Cấu Trúc Thư Mục Dịch Vụ (Service Directory Hierarchy)

Cấu trúc các tệp liên quan đến xác thực và dữ liệu người dùng tại thư mục `client/`:
- `client/src/services/authService.js`: Chứa các hàm giao tiếp API xác thực (đăng ký, xác thực OTP, đăng nhập, làm mới token).
- `client/src/services/userService.js`: Chứa hàm giao tiếp lấy thông tin cá nhân.
- `client/src/services/cccdService.js`: Chứa hàm kết nối dịch vụ kiểm tra CCCD bên thứ ba.
- `client/src/utils/authStorage.js`: Tiện ích quản lý lưu trữ trạng thái phiên đăng nhập của trình duyệt.
- `client/src/pages/Register.jsx`: Giao diện và luồng xử lý đăng ký tài khoản.
- `client/src/pages/Login.jsx`: Giao diện và luồng xử lý đăng nhập tài khoản.
- `client/src/pages/VerifyOtp.jsx`: Giao diện và luồng xác thực mã OTP.

## 3. Quản Lý Trạng Thái Phiên Đăng Nhập (Token Storage)

Tất cả các thông tin bảo mật và phiên được quản lý tập trung thông qua `authStorage.js` nhằm cô lập việc thao tác trực tiếp với localStorage:
- `setAuthData(data)`: Lưu các thông tin `authToken` (chứa Access Token), `refreshToken`, `tokenType`, `userEmail`, và `userRole` vào localStorage.
- `getAuthToken()`, `getRefreshToken()`, `getUserEmail()`, `getUserRole()`: Trích xuất các trường dữ liệu tương ứng.
- `clearAuthData()`: Xóa sạch toàn bộ thông tin phiên khỏi localStorage khi người dùng đăng xuất hoặc khi token hết hạn.
- `isAuthenticated()`: Trả về trạng thái boolean biểu thị người dùng đã đăng nhập hay chưa dựa trên sự tồn tại của `authToken`.
- `setPendingAccountId(id)` / `getPendingAccountId()`: Lưu trữ tạm thời ID tài khoản chưa kích hoạt vào bộ nhớ RAM của ứng dụng để phục vụ luồng OTP.

Lưu ý bảo mật: Không bao giờ lưu trữ số căn cước công dân (CCCD) chưa được mã hóa vào localStorage hoặc in ra log console để tránh rò rỉ dữ liệu nhạy cảm của khách hàng.

## 4. Đặc Tả Các Luồng Giao Dịch (Transaction State Maps)

### Luồng 1: Đăng Ký Tài Khoản (Register)
1. Người dùng điền thông tin đăng ký (Họ tên, Email, Số điện thoại, Số CCCD, Ngày sinh, Mật khẩu).
2. Form đăng ký gửi yêu cầu `POST` tới địa chỉ `${VITE_API_BASE_URL}/api/auth/register`.
3. Khi nhận phản hồi thành công (`200 OK` hoặc `201 Created`), trích xuất `accountId` từ dữ liệu phản hồi (`data.accountId`).
4. Gán `accountId` vào bộ nhớ tạm bằng `setPendingAccountId(accountId)`.
5. Hiển thị thông báo thành công và tự động chuyển hướng người dùng tới giao diện nhập mã OTP (`/verify-otp`). Luồng đăng ký hoàn toàn không tự động đăng nhập.

### Luồng 2: Kiểm Tra Định Dạng Căn Cước Công Dân (Identity Validation)
1. Trên giao diện đăng ký, khi trường CCCD đạt đủ 12 chữ số, kích hoạt sự kiện truy vấn CCCD.
2. Gọi API kiểm tra tại `cccdService.js` thông qua yêu cầu `POST` tới `VITE_CCCD_API_URL` kèm tiêu đề bảo mật `x-api-key`.
3. Nhận về thông tin đã được xử lý che bớt ký tự (`cccdMasked`, `provinceName`, `genderLabel`, `birthYear`). Hiển thị các thông tin này ngay phía dưới trường nhập liệu.
4. Đối chiếu năm sinh từ bộ chọn ngày sinh (`birthday`) của người dùng với `birthYear` phản hồi từ dịch vụ CCCD.
5. Nếu phát hiện sai lệch thông tin năm sinh, đóng băng chức năng gửi form (submit), hiển thị hộp cảnh báo đỏ và vô hiệu hóa nút đăng ký.

### Luồng 3: Xác Thực Mã OTP (OTP Verification)
1. Giao diện `/verify-otp` kiểm tra sự tồn tại của `pendingAccountId`. Nếu không tìm thấy, điều hướng ngược lại trang đăng ký (`/register`).
2. Người dùng nhập mã OTP gồm 6 chữ số.
3. Gửi yêu cầu `POST` tới `${VITE_API_BASE_URL}/api/auth/verify` chứa `accountId` và `otp`.
4. Nếu mã chính xác (`200 OK`), hệ thống kích hoạt tài khoản thành công. Xóa tài khoản chờ khỏi bộ nhớ tạm bằng `clearPendingAccountId()` và chuyển hướng trực tiếp về giao diện đăng nhập (`/login`). Quy trình đảm bảo không chuyển hướng vòng qua trang đăng ký.
5. Nếu thất bại, bắt lỗi và hiển thị phản hồi tương ứng:
   - Lỗi `AUTH_INVALID_OTP`: Hiển thị cảnh báo "OTP không chính xác".
   - Lỗi `AUTH_VERIFICATION_EXPIRED`: Hiển thị cảnh báo "Mã OTP đã hết hạn".

### Luồng 4: Đăng Nhập Hệ Thống (Login)
1. Người dùng nhập email và mật khẩu tại trang `/login`.
2. Gửi yêu cầu `POST` tới `${VITE_API_BASE_URL}/api/auth/login`.
3. Nếu phản hồi trả về lỗi `AUTH_ACCOUNT_NOT_VERIFIED` (403 Forbidden):
   - Hệ thống trích xuất ID tài khoản từ lỗi (nếu có) và gán làm tài khoản chờ xác thực.
   - Chuyển hướng ngay lập tức người dùng sang trang `/verify-otp` để hoàn tất xác thực kích hoạt tài khoản.
4. Nếu đăng nhập thành công:
   - Gọi `setAuthData(data.data)` để lưu bộ Token.
   - Điều hướng về trang chủ `/`.
5. Nếu xảy ra các lỗi khác, ánh xạ thông báo lỗi tiếng Việt thân thiện:
   - `AUTH_INVALID_CREDENTIALS`: "Email hoặc mật khẩu không chính xác."
   - `AUTH_ACCOUNT_INACTIVE`: "Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt."
   - `VALIDATION_ERROR`: "Dữ liệu nhập vào không hợp lệ."
   - `INTERNAL_SERVER_ERROR`: "Lỗi hệ thống từ máy chủ. Vui lòng thử lại sau."

### Luồng 5: Gia Hạn Phiên Đăng Nhập (Refresh Token)
1. Khi Access Token hết hạn, thực hiện gửi yêu cầu `POST` tới `${VITE_API_BASE_URL}/api/auth/refresh-token` kèm theo dữ liệu `refreshToken` được lấy từ `getRefreshToken()`.
2. Nếu máy chủ phản hồi thành công bộ token mới, ghi đè lại các bản ghi lưu trữ thông tin token cũ trong localStorage thông qua `setAuthData`.
3. Nếu mã Refresh Token không còn hiệu lực hoặc đã hết hạn, lập tức giải phóng mọi biến phiên bảo mật bằng `clearAuthData()` và điều hướng trình duyệt quay lại giao diện `/login` để yêu cầu người dùng thao tác đăng nhập lại từ đầu.

### Luồng 6: Lấy Thông Tin Cá Nhân (Profile)
1. Do phản hồi đăng nhập thành công từ đối tượng Login response ban đầu không chứa trực tiếp khóa `accountId` ở cấp cao nhất của cấu trúc dữ liệu trả về, Phân hệ Frontend triển khai Phương án 1 (Option 1) bằng cách thực hiện phân tích giải mã chuỗi JWT `accessToken` ở phía Client thông qua thư viện `jwt-decode` để trích xuất trường thông tin claim `userId`.
2. Giá trị `userId` trích xuất được sử dụng làm thông tin định danh tương đương cho `accountId` phục vụ cho luồng truy vấn.
3. Gửi yêu cầu `GET` tới API Gateway thông qua địa chỉ `${VITE_API_BASE_URL}/api/users/{accountId}`.
4. Tự động đính kèm tiêu đề xác thực cấu trúc `Authorization: Bearer <authToken>` từ hàm `getAuthToken()`.
5. Khi hiển thị hồ sơ thông tin cá nhân lên giao diện, chỉ được phép kết xuất trường thông tin đã được mã hóa che bớt ký tự `cccdMasked` (ví dụ: `092******789`). Nghiêm cấm hiển thị hoặc ghi chép số định danh căn cước gốc (CCCD) chưa mã hóa lên giao diện, in ra bảng ghi sự kiện (console log) hay ghi đè vào các khóa lưu trữ cục bộ (localStorage).
6. Khuyến nghị: Phân hệ Backend nên bổ sung thuộc tính accountId trực tiếp vào cấu trúc Login response body hoặc đổi tên claim dữ liệu từ userId thành accountId để đồng bộ mã nguồn sạch hơn giữa các service.

## 5. Danh Sách Ánh Xạ Dữ Liệu (Data Mapping Blocks)

Dưới đây là đặc tả chi tiết ánh xạ dữ liệu đầu vào và đầu ra của các API:

### API Đăng ký tài khoản (`POST /api/auth/register`)
- Request Body:
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
- Response Success (200 OK):
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

### API Xác thực OTP (`POST /api/auth/verify`)
- Request Body:
  ```json
  {
    "accountId": 1,
    "otp": "123456"
  }
  ```

### API Đăng nhập (`POST /api/auth/login`)
- Request Body:
  ```json
  {
    "email": "user@example.com",
    "password": "User@123"
  }
  ```
- Response Success (200 OK):
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

### API Lấy thông tin cá nhân (`GET /api/users/{accountId}`)
- Headers:
  `Authorization: Bearer <authToken>`
- Response Success (200 OK):
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

## 6. Hiện Trạng Kiến Trúc Hàng Đợi Tin Nhắn Kafka (Kafka Messaging Infrastructure Status)

Hệ thống hàng đợi tin nhắn Apache Kafka dùng để đồng bộ hóa bất đồng bộ các dữ liệu liên quan đến luồng khởi tạo người dùng (ACCOUNT_CREATED) giữa dịch vụ auth-service và user-service đã được cấu hình, triển khai và kiểm thử nghiệm thu thành công trên môi trường hệ thống.

Do đó:
- Luồng gửi và nhận thông tin sự kiện đăng ký tài khoản qua Kafka hoạt động ổn định end-to-end theo đúng sơ đồ kiến trúc EDA (Kiến trúc hướng sự kiện).
- Khi người dùng kích hoạt tài khoản thành công qua mã OTP tại trang /verify-otp, auth-service sẽ tự động publish một bản tin sự kiện dạng chuỗi JSON lên Kafka Topic với tên định danh auth.account.created.v1.
- Phân hệ user-service liên tục lắng nghe và tiêu thụ (consume) bản tin từ topic trên để tự động khởi tạo bản ghi hồ sơ cá nhân (user_profiles) trong cơ sở dữ liệu của mình, đảm bảo tính bất biến (idempotency) và không gây trùng lặp dữ liệu. Các luồng gọi đồng bộ trực tiếp tạm thời thông qua endpoint nội bộ (/internal/users) đã hoàn toàn được gỡ bỏ để chuyển giao 100% sang kiến trúc hướng sự kiện sạch.

## 7. Hạn Chế Về Dữ Liệu Đăng Nhập Hiện Tại

Trường hợp API phản hồi đăng nhập thành công của một số phiên bản Backend cũ có thể thiếu thuộc tính accountId trong đối tượng phản hồi. Đây là lỗi thiết kế luồng dữ liệu cần được khắc phục từ phía backend. Frontend khuyến nghị lập trình viên backend duy trì cấu trúc dữ liệu phản hồi đăng nhập đồng nhất chứa trường thông tin định danh accountId để frontend có thể trực tiếp thực hiện việc truy vấn thông tin cá nhân khách hàng sau khi chuyển hướng trang.
