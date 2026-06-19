# Tài Liệu Kiểm Thử Tích Hợp Hệ Thống - Luồng Xác Thực Và Đăng Ký LoraFilm

## Lịch sử chỉnh sửa

**Ngày:** 19/06/2026 | **Người chỉnh sửa:** Trần Hiển Vinh

* **Cập nhật Backend (JwtResponse):** Bổ sung trường `token` trong `JwtResponse.java` để đồng bộ hoàn toàn với API Contract.
* **Cập nhật Backend (UserProfileResponse):** Đồng bộ tên thuộc tính trong DTO Java `UserProfileResponse` từ `isVerifiedPhone` thành `verifiedPhone` để tránh nhầm lẫn bảo trì.

---

## 1. Tổng Quan Kiến Trúc Và Định Tuyến Mạng

Tài liệu này đặc tả quy trình và kết quả kiểm thử tích hợp (E2E) đối với luồng Đăng ký (Register) và Đăng nhập (Login) trên hệ thống Đặt vé Xem phim trực tuyến LoraFilm. Quy trình kiểm thử tập trung vào việc xác minh tính chính xác của các đường đi mạng giữa React Frontend, API Gateway và các dịch vụ nội bộ (auth-service, user-service), đồng thời đối chiếu với các giao kèo giao tiếp (API Contracts).

### Sơ Đồ Định Tuyến Mạng Thực Tế

Hệ thống triển khai mô hình tập trung luồng yêu cầu từ máy khách (client) qua API Gateway để định tuyến tới các dịch vụ microservices tương ứng:

1. **React Frontend**: Hoạt động tại địa chỉ phát triển mặc định http://localhost:5173.
2. **API Gateway**: Cấu hình tại cổng http://localhost:8080. Mọi request gọi từ Frontend đều phải hướng trực tiếp tới Gateway này.
3. **Auth-Service**: Cấu hình cổng nội bộ http://localhost:8081. Gateway ánh tuyến các yêu cầu /api/auth/** về dịch vụ này.
4. **User-Service**: Cấu hình cổng nội bộ http://localhost:8086. Gateway ánh tuyến các yêu cầu /api/users/** về dịch vụ này.
5. **CCCD Check API**: Địa chỉ độc lập https://api-check-cccd.lorafilm.xyz/api/cccd/check, sử dụng API Key bảo mật để phân tích cấu trúc mã định danh của công dân Việt Nam.

```txt
[React Frontend:5173]
       │
       ├─(Đăng ký / Đăng nhập / Lấy Profile)─> [API Gateway:8080]
       │                                             │
       │                                             ├─(/api/auth/**)──> [auth-service:8081]
       │                                             └─(/api/users/**)─> [user-service:8086]
       │
       └─(Kiểm tra định dạng CCCD)───────────> [CCCD Check API (HTTPS)]
```

### Ma Trận Kiểm Tra Định Tuyến Yêu Cầu (Request Verification Matrix)

Nhằm đảm bảo tính cô lập và bảo mật hệ thống, kiểm thử viên tiến hành xác thực cấu hình định tuyến đầu cuối của các dịch vụ để cam kết không tồn tại bất kỳ yêu cầu trực tiếp nào từ Frontend tới các cổng nội bộ:

| Yêu Cầu Gửi Đi Từ Frontend | Cổng Đích Kỳ Vọng | Cổng Đích Thực Tế | Trạng Thái | Đánh Giá Kỹ Thuật |
| :--- | :--- | :--- | :--- | :--- |
| Yêu cầu đăng ký tài khoản | http://localhost:8080/api/auth/register | http://localhost:8080/api/auth/register | ĐẠT | Gọi qua API Gateway, không gọi trực tiếp cổng 8081. |
| Yêu cầu xác thực OTP | http://localhost:8080/api/auth/verify | http://localhost:8080/api/auth/verify | ĐẠT | Gọi qua API Gateway, không gọi trực tiếp cổng 8081. |
| Yêu cầu đăng nhập hệ thống | http://localhost:8080/api/auth/login | http://localhost:8080/api/auth/login | ĐẠT | Gọi qua API Gateway, không gọi trực tiếp cổng 8081. |
| Yêu cầu làm mới Token | http://localhost:8080/api/auth/refresh-token | http://localhost:8080/api/auth/refresh-token | ĐẠT | Gọi qua API Gateway, không gọi trực tiếp cổng 8081. |
| Yêu cầu lấy thông tin hồ sơ | http://localhost:8080/api/users/{accountId} | http://localhost:8080/api/users/{accountId} | ĐẠT | Gọi qua API Gateway, không gọi trực tiếp cổng 8086. |
| Yêu cầu kiểm tra CCCD | https://api-check-cccd.lorafilm.xyz/... | https://api-check-cccd.lorafilm.xyz/... | ĐẠT | Gọi trực tiếp dịch vụ bên thứ ba bằng HTTPS bảo mật. |

*Ghi chú quan trọng*: Qua rà soát mã nguồn ở tệp cấu hình môi trường Frontend client/.env, biến VITE_API_BASE_URL được thiết lập chính xác giá trị http://localhost:8080. Không tìm thấy bất kỳ chuỗi cứng nào chứa cổng 8081 hoặc 8086 trong mã nguồn ứng dụng client.

---

## 2. Danh Sách Kịch Bản Kiểm Thử Tích Hợp (E2E Test Scenarios)

Dưới đây là các kịch bản kiểm thử tích hợp chi tiết được thực hiện nhằm kiểm định toàn bộ chu trình đăng ký, kiểm tra căn cước, đối chiếu nghiệp vụ, kích hoạt OTP và lưu giữ trạng thái phiên đăng nhập.

### Kịch Bản 1: Luồng Đăng Ký Tài Khoản Thành Công (Successful Registration E2E)
- **Mục đích**: Xác nhận người dùng có thể tạo tài khoản mới thành công khi cung cấp đầy đủ thông tin hợp lệ, đồng thời kiểm tra sự kích hoạt ngầm của tiến trình khởi tạo hồ sơ bất đồng bộ qua Kafka.
- **Các bước thực hiện**:
  1. Truy cập vào trang Đăng ký thành viên /register.
  2. Nhập đầy đủ thông tin hợp lệ vào biểu mẫu:
     - Họ và tên: Nguyen Van A
     - Email: testuser@example.com
     - Số điện thoại: 0901234567
     - CCCD: 092205006789 (Thực hiện nhấn nút "Kiểm tra" và nhận phản hồi hợp lệ).
     - Ngày sinh: 2005-06-12
     - Mật khẩu: User@123
     - Xác nhận mật khẩu: User@123
  3. Nhấn nút "Đăng ký tài khoản".
- **Dữ liệu đầu vào**:
  ```json
  {
    "fullName": "Nguyen Van A",
    "email": "testuser@example.com",
    "phoneNumber": "0901234567",
    "cccd": "092205006789",
    "birthday": "2005-06-12",
    "password": "User@123"
  }
  ```
- **Kết quả mong đợi**:
  - Máy chủ trả về phản hồi mã trạng thái HTTP 201 Created (hoặc 200 OK kèm mã thành công trong dữ liệu trả về).
  - Tài khoản mới được ghi nhận vào cơ sở dữ liệu của auth-service với trạng thái is_active = 0 và registration_completed = 0.
  - Một sự kiện ACCOUNT_CREATED được phát xuất lên hàng đợi Kafka trên topic auth.account.created.v1.
  - Dịch vụ user-service tiêu thụ sự kiện này thành công và tự động tạo bản ghi hồ sơ tương ứng trong cơ sở dữ liệu của mình.
  - Một mã xác thực OTP gồm 6 chữ số được hệ thống tạo ra và ghi nhận trong nhật ký giao diện điều khiển (console log) của máy chủ xác thực.
  - Giao diện người dùng hiển thị thông báo thành công và tự động chuyển hướng sang trang Xác thực OTP /verify-otp.
- **Kết quả thực tế**:
  - Máy chủ trả về mã HTTP 201 Created với dữ liệu thông tin tài khoản được che định dạng CCCD (cccdMasked).
  - Dữ liệu tài khoản lưu trữ đúng trạng thái chưa kích hoạt. Sự kiện Kafka khởi tạo hồ sơ thành công bất đồng bộ.
  - Mã OTP xuất hiện trên nhật ký máy chủ để phục vụ kiểm thử.
  - Giao diện chuyển hướng thành công tới /verify-otp sau 1,5 giây.
- **Trạng thái**: ĐẠT

### Kịch Bản 2: Dịch Vụ Kiểm Trả CCCD Thứ Ba (Third-Party CCCD Check API Lookup)
- **Mục đích**: Xác minh tính năng kiểm tra tự động định dạng CCCD hoạt động đúng đắn khi người dùng nhập đủ 12 chữ số, đảm bảo điền chính xác các dữ liệu dẫn xuất được che mặt nạ bảo mật.
- **Các bước thực hiện**:
  1. Tại biểu mẫu đăng ký, di chuyển đến ô nhập liệu "Số CCCD".
  2. Nhập chính xác chuỗi 12 chữ số: 092205006789.
  3. Nhấp chọn nút "Kiểm tra" hoặc quan sát hệ thống tự kích hoạt cuộc gọi API khi nhập đủ độ dài.
- **Dữ liệu đầu vào**: Ô nhập liệu CCCD nhận giá trị "092205006789".
- **Kết quả mong đợi**:
  - Giao diện thực hiện gửi yêu cầu POST đến https://api-check-cccd.lorafilm.xyz/api/cccd/check kèm mã khóa tiêu đề x-api-key.
  - Phản hồi nhận được chứa các trường dữ liệu hợp lệ: valid: true, provinceName: "Cần Thơ", genderLabel: "Nam", birthYear: 2005, cccdMasked: "092******789".
  - Phía dưới ô nhập liệu CCCD hiển thị ngay lập tức khối thông tin đã được che mặt nạ để người dùng xác nhận tính chính xác của tài liệu định danh.
- **Kết quả thực tế**:
  - Yêu cầu HTTP POST được kích hoạt chính xác qua công cụ Axios.
  - Giao diện hiển thị trực quan các thông tin giải mã bao gồm: Mã CCCD đã che (092******789), Tỉnh thành (Cần Thơ), Giới tính (Nam), Năm sinh (2005).
- **Trạng thái**: ĐẠT

### Kịch Bản 3: Ràng Buộc Sai Lệch Ngày Sinh (Cross-Field Birthday Mismatch Validation)
- **Mục đích**: Đảm bảo hệ thống phát hiện và ngăn chặn đăng ký nếu năm sinh người dùng chọn trên lịch không trùng khớp với năm sinh giải mã từ số CCCD.
- **Các bước thực hiện**:
  1. Nhập số CCCD hợp lệ giải mã ra năm sinh 2005: 092205006789.
  2. Tại ô "Ngày sinh", chọn ngày sinh có năm khác biệt, ví dụ: 1999-06-12.
  3. Nhấn thử nút đăng ký hoặc di chuyển chuột ra khỏi vùng nhập liệu để kiểm tra tự động.
- **Dữ liệu đầu vào**:
  - Số CCCD: 092205006789 (Năm giải mã từ CCCD: 2005)
  - Ngày sinh chọn trên lịch: 1999-06-12 (Năm nhập: 1999)
- **Kết quả mong đợi**:
  - Trình duyệt phát hiện sự sai lệch năm sinh ở phía giao diện (so sánh birthdayYear !== cccdData.birthYear).
  - Giao diện xuất hiện thông báo lỗi: "Năm sinh không trùng khớp với thông tin trên căn cước công dân".
  - Nút "Đăng ký tài khoản" bị vô hiệu hóa hoàn toàn, chặn hành vi gửi dữ liệu lên máy chủ.
  - Nếu cố tình gửi trực tiếp yêu cầu không qua giao diện, máy chủ backend phải trả về lỗi 400 Bad Request kèm mã lỗi USER_BIRTHDAY_CCCD_MISMATCH.
- **Kết quả thực tế**:
  - Dòng cảnh báo hiển thị ngay dưới ô Ngày sinh khi mất tiêu điểm.
  - Nút Submit bị chuyển sang trạng thái vô hiệu hóa (disabled), ngăn chặn yêu cầu mạng tiếp tục gửi đi.
- **Trạng thái**: ĐẠT

### Kịch Bản 4: Xử Lý Trùng Lặp Thông Tin Đăng Ký (Duplicate Parameter Boundary Checks)
- **Mục đích**: Xác nhận hệ thống phản hồi lỗi chính xác và giao diện hiển thị cảnh báo thích hợp khi người dùng cố tình đăng ký bằng Email, Số điện thoại hoặc CCCD đã được sử dụng trước đó.
- **Các bước thực hiện**:
  1. Tiến hành thực hiện đăng ký tài khoản với Email đã tồn tại trong hệ thống. Nhấn đăng ký.
  2. Tiến hành thực hiện đăng ký tài khoản với Số điện thoại đã tồn tại trong hệ thống. Nhấn đăng ký.
  3. Tiến hành thực hiện đăng ký tài khoản với Số CCCD đã tồn tại trong hệ thống. Nhấn đăng ký.
- **Dữ liệu đầu vào**:
  - Thử nghiệm 1: Email nhan@gmail.com (Đã tồn tại)
  - Thử nghiệm 2: Số điện thoại 0979158662 (Đã tồn tại)
  - Thử nghiệm 3: CCCD 092205006384 (Đã tồn tại)
- **Kết quả mong đợi**:
  - Trùng Email: Trả về lỗi 409 Conflict kèm mã lỗi AUTH_EMAIL_ALREADY_EXISTS. Giao diện hiển thị: "Email này đã được sử dụng."
  - Trùng Số điện thoại: Trả về lỗi 409 Conflict kèm mã lỗi USER_PHONE_ALREADY_EXISTS. Giao diện hiển thị: "Số điện thoại này đã được sử dụng."
  - Trùng CCCD: Trả về lỗi 409 Conflict kèm mã lỗi USER_CCCD_ALREADY_EXISTS. Giao diện hiển thị: "Số căn cước công dân này đã được sử dụng."
- **Kết quả thực tế**:
  - Giao diện phản hồi chuẩn xác theo bảng ánh xạ mã lỗi nghiệp vụ từ phản hồi của Axios, hiển thị biểu ngữ cảnh báo màu đỏ với thông báo khớp hoàn toàn với mong đợi.
- **Trạng thái**: ĐẠT

### Kịch Bản 5: Luồng Đăng Nhập Hệ Thống (Login Core Flow E2E)
- **Mục đích**: Đảm bảo người dùng đã kích hoạt tài khoản có thể đăng nhập thành công vào hệ thống thông qua API Gateway bằng thông tin xác thực chính xác.
- **Các bước thực hiện**:
  1. Truy cập vào trang Đăng nhập /login.
  2. Nhập Email và Mật khẩu chính xác của tài khoản đã được xác thực OTP thành công.
  3. Nhấn nút "Xác nhận đăng nhập".
- **Dữ liệu đầu vào**:
  - Email: nhan@gmail.com
  - Mật khẩu: Nhan@123
- **Kết quả mong đợi**:
  - Máy chủ trả về mã trạng thái HTTP 200 OK.
  - Phản hồi chứa các dữ liệu mã thông báo bao gồm accessToken, refreshToken, expiresIn, email và vai trò role của người dùng.
  - Ứng dụng client chuyển hướng người dùng về trang chủ / và cập nhật trạng thái đã đăng nhập trên toàn bộ giao diện.
- **Kết quả thực tế**:
  - Phản hồi từ cổng Gateway trả về thông tin đăng nhập thành công đầy đủ mã thông báo dạng chuỗi JWT.
  - Người dùng được chuyển hướng an toàn về trang chủ ứng dụng.
- **Trạng thái**: ĐẠT

### Kịch Bản 6: Kiểm Trả Sai Lệch Thông Tin Đăng Nhập (Invalid Credentials Matrix)
- **Mục đích**: Xác nhận hệ thống chặn các nỗ lực đăng nhập bằng thông tin tài khoản không tồn tại hoặc mật khẩu sai lệch, trả về mã lỗi cụ thể.
- **Các bước thực hiện**:
  1. Nhập địa chỉ Email chưa từng đăng ký hoặc mật khẩu không chính xác.
  2. Nhấn nút "Xác nhận đăng nhập".
- **Dữ liệu đầu vào**: Email "wronguser@gmail.com", Mật khẩu "WrongPass123".
- **Kết quả mong đợi**:
  - Máy chủ phản hồi mã lỗi 401 Unauthorized kèm mã lỗi cấu trúc AUTH_INVALID_CREDENTIALS.
  - Giao diện hiển thị thông báo: "Email hoặc mật khẩu không chính xác."
- **Kết quả thực tế**:
  - Nhận phản hồi HTTP 401. Biểu ngữ thông báo lỗi trên biểu mẫu đăng nhập hiển thị chính xác chuỗi thông tin cảnh báo cho người dùng.
- **Trạng thái**: ĐẠT

### Kịch Bản 7: Tài Khoản Chưa Kích Hoạt OTP (Inactive/Unverified Account Handling)
- **Mục đích**: Xác hiện hành vi của hệ thống khi tài khoản đã đăng ký nhưng chưa thực hiện nhập mã xác thực OTP trước đó tiến hành đăng nhập.
- **Các bước thực hiện**:
  1. Đăng ký một tài khoản mới nhưng không nhập mã OTP ở bước tiếp theo.
  2. Quay lại trang đăng nhập /login, nhập đúng Email và Mật khẩu vừa tạo.
  3. Nhấn chọn đăng nhập.
- **Dữ liệu đầu vào**: Tài khoản mới tạo chưa xác thực kích hoạt.
- **Kết quả mong đợi**:
  - Dịch vụ xác thực trả về mã lỗi 403 Forbidden kèm mã định danh nghiệp vụ AUTH_ACCOUNT_NOT_VERIFIED.
  - Ứng dụng khách nhận dạng mã lỗi này, tự động trích xuất ID tài khoản từ phản hồi lỗi để ghi nhớ trạng thái phiên chưa kích hoạt.
  - Hiển thị thông báo: "Tài khoản chưa được xác thực. Đang chuyển hướng sang trang xác thực OTP..."
  - Tự động chuyển hướng người dùng trở lại trang xác thực OTP /verify-otp sau 1,5 giây để hoàn thành quy trình kích hoạt.
- **Kết quả thực tế**:
  - Máy chủ trả về lỗi 403 kèm mã `AUTH_ACCOUNT_NOT_VERIFIED` và `accountId`. Giao diện client đã bắt được lỗi này, hiển thị thông báo đúng và chuyển hướng thành công sang trang `/verify-otp`.
- **Trạng thái**: ĐẠT

### Kịch Bản 8: Bộ Nhớ Lưu Trữ Trạng Thái Phiên (Session Token Storage Validation)

* **Mục đích**: Đảm bảo chuỗi mã thông báo JWT (Access Token) và Refresh Token sau khi xác thực thành công được lưu trữ cục bộ trên trình duyệt đúng với tên khóa quy định trong giao ước hiện tại, đồng thời xác minh không lưu trữ dữ liệu cá nhân nhạy cảm dạng văn bản rõ như CCCD đầy đủ.

* **Các bước thực hiện**:

  1. Thực hiện đăng nhập thành công với tài khoản hợp lệ.
  2. Sau khi được điều hướng về trang chủ, nhấn phím F12 mở Công cụ Nhà phát triển (Developer Tools).
  3. Di chuyển đến tab Ứng dụng (Application) -> Bộ nhớ Cục bộ (Local Storage) của địa chỉ http://localhost:5173.
  4. Kiểm tra sự tồn tại của các khóa lưu trữ token theo giao ước.
  5. Kiểm tra không tồn tại dữ liệu CCCD đầy đủ hoặc dữ liệu định danh nhạy cảm chưa che mặt nạ trong Local Storage.

* **Dữ liệu đầu vào**: Trạng thái đăng nhập thành công.

* **Kết quả mong đợi**:

  * Mã thông báo Access Token được lưu trữ chính xác dưới tên khóa `authToken`.
  * Mã thông báo Refresh Token được lưu trữ chính xác dưới tên khóa `refreshToken`.
  * Kiểu token được lưu dưới khóa `tokenType` với giá trị `Bearer`.
  * Có thể lưu các thông tin định danh tối thiểu phục vụ giao diện như `userEmail`, `userRole`, `userAccountId`.
  * Không lưu trữ thông tin CCCD đầy đủ chưa che mặt nạ tại bất kỳ khu vực nào của Local Storage.

* **Kết quả thực tế**:

  * Kiểm tra Local Storage ghi nhận các cặp giá trị:

    * `authToken`: [Chuỗi JWT hợp lệ]
    * `refreshToken`: [Mã UUID hợp lệ]
    * `tokenType`: `Bearer`
    * `userEmail`: `nhannhinhanh63@gmail.com`
    * `userRole`: `CUSTOMER`
    * `userAccountId`: `15`
  * Hoàn toàn không phát hiện sự lưu trữ thông tin CCCD thô dưới dạng văn bản rõ.

* **Ghi chú bảo mật**:

  * Cách lưu `authToken` và `refreshToken` trong Local Storage phù hợp với giai đoạn development/testing/demo để Frontend dễ dàng gọi API Gateway và kiểm thử luồng xác thực.
  * Tuy nhiên, Local Storage có thể bị truy cập bởi JavaScript nếu ứng dụng tồn tại lỗ hổng XSS. Vì vậy, đây chưa phải phương án tối ưu cho môi trường production.
  * Khi nâng cấp production, khuyến nghị chuyển `refreshToken` sang `HttpOnly Secure SameSite Cookie`, giữ `accessToken` có thời gian sống ngắn, tiếp tục hash Refresh Token trong database, đồng thời bổ sung cơ chế revoke token và refresh token rotation.

* **Trạng thái**: ĐẠT

* **Kết luận**: PASS theo yêu cầu API contract hiện tại. Cần ghi nhận cải tiến bảo mật token storage như một technical debt cho giai đoạn production hardening.

### Kịch Bản 9: Gửi Lại Mã Xác Thực OTP Thành Công (Resend OTP Success)
- **Mục đích**: Kiểm tra chức năng gửi lại mã OTP cho tài khoản chưa được xác thực.
- **Các bước thực hiện**:
  1. Người dùng vào trang xác thực OTP (hoặc được điều hướng từ đăng nhập thất bại do chưa xác thực).
  2. Đợi thời gian đếm ngược kết thúc, nhấn "Gửi lại mã".
- **Kết quả mong đợi**:
  - API trả về thành công (200 OK). Frontend hiển thị "Mã OTP mới đã được gửi thành công.".
  - Bộ đếm thời gian countdown bắt đầu lại từ 30s.
  - Mã OTP mới được tạo và mã cũ bị vô hiệu hóa.
- **Trạng thái**: ĐẠT

### Kịch Bản 10: Gửi Lại Mã Xác Thực OTP Quá Sớm (Resend OTP Rate Limit)
- **Mục đích**: Kiểm tra cơ chế rate limiting/cooldown (chống spam) cho API gửi OTP.
- **Các bước thực hiện**:
  1. Gửi lại mã OTP thành công.
  2. Ngay lập tức dùng API (Postman/Curl) gọi tiếp POST `/api/auth/resend-otp` khi chưa hết 60s cooldown.
- **Kết quả mong đợi**:
  - Backend từ chối với lỗi 429 Too Many Requests, mã `AUTH_OTP_RESEND_TOO_SOON`, trả về biến `retryAfter`.
- **Trạng thái**: ĐẠT

### Kịch Bản 11: Xác Thực Mã Cũ Bị Vô Hiệu Sau Khi Gửi Lại (Invalid Old OTP)
- **Mục đích**: Đảm bảo bảo mật khi sinh OTP mới.
- **Các bước thực hiện**:
  1. Nhận mã OTP 1 nhưng không nhập.
  2. Nhấn gửi lại mã để lấy OTP 2.
  3. Thử dùng mã OTP 1 nhập vào form.
- **Kết quả mong đợi**:
  - Báo lỗi xác thực thất bại `AUTH_INVALID_OTP` do mã cũ đã bị hệ thống ghi đè/vô hiệu hóa.
- **Trạng thái**: ĐẠT


---

## 3. Báo Cáo Lệch Hợp Đồng (Discrepancy Tracking)

Quá trình đối chiếu giữa thiết kế lý thuyết trong các tài liệu đặc tả giao diện lập trình ứng dụng (docs/api/auth-service-api.md, docs/api/user-api.md) và mã nguồn triển khai thực tế của các dịch vụ đã phát hiện một số điểm sai lệch cấu trúc dưới đây.

### Lệch Hợp Đồng 1: [ĐÃ KHẮC PHỤC] Thiếu trường dữ liệu trong phản hồi đăng nhập (Login Response)
- **Trạng thái**: Đã được khắc phục trong Sprint hiện tại.
- **Chi tiết khắc phục**: Lớp `JwtResponse.java` đã được bổ sung đầy đủ trường `token` cũng như `accountId`. Backend hiện đã trả về đối tượng khớp 100% với API Contract quy định (`token`, `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `email`, `role`, `accountId`). Frontend không còn cần phải sử dụng toán tử fallback (dù vẫn có thể giữ lại để an toàn).

### Lệch Hợp Đồng 2: Lộ Tuyến Đường Dịch Vụ Nội Bộ Tại API Gateway (Security Route Exposure)
- **Đường dẫn Endpoint**: /internal/users/** (Dịch vụ user-service)
- **Tệp nguồn liên quan**: api-gateway/src/main/resources/application.properties
- **Mô tả điểm sai lệch**:
  - Theo tài liệu giao ước docs/api/user-api.md (mục 4.1 và 4.2), endpoint /internal/users là một API nội bộ (Internal API) dành riêng cho việc giao tiếp giữa các dịch vụ phía sau máy chủ (như auth-service gọi sang để tạo Profile). Tài liệu nêu rõ: "API Gateway không được phép expose endpoint này ra ngoài cho Frontend hay External Client gọi."
  - Tuy nhiên, trong tệp cấu hình của API Gateway thực tế, tuyến đường này vẫn được khai báo công khai cho phép truy cập từ bên ngoài:
    ```properties
    # Route: internal user-service
    spring.cloud.gateway.routes[1].id=internal-user-service
    spring.cloud.gateway.routes[1].uri=http://localhost:8086
    spring.cloud.gateway.routes[1].predicates[0]=Path=/internal/users/**
    ```
- **Mức độ ảnh hưởng**: Cao. Bất kỳ đối tượng bên ngoài nào cũng có thể gửi yêu cầu HTTP trực tiếp tới cổng http://localhost:8080/internal/users thông qua API Gateway để tùy ý tạo lập hoặc can thiệp dữ liệu người dùng mà không qua kiểm tra quyền hạn của Gateway. Đây là một lỗ hổng bảo mật nghiêm trọng.

### Lệch Hợp Đồng 3: Sai Biệt Tên Thuộc Tính Xác Thực Điện Thoại (Java Field Mismatch)
- **Đường dẫn Endpoint**: /api/users/{accountId} (Dịch vụ user-service)
- **Tệp nguồn liên quan**: server/user-service/src/main/java/com/project/userservice/dto/response/UserProfileResponse.java
- **Mô tả điểm sai lệch**:
  - Theo lịch sử chỉnh sửa tài liệu docs/api/user-api.md ngày 14/06/2026, tên trường đại diện cho trạng thái xác thực số điện thoại đã được đổi từ isVerifiedPhone thành verifiedPhone do gỡ bỏ thư viện Lombok.
  - Tuy nhiên, trong lớp thực thể và DTO Java UserProfileResponse.java, biến thành viên nội bộ vẫn được khai báo dưới tên isVerifiedPhone. Mặc dù vậy, do phương thức truy xuất getter được viết thủ công là getVerifiedPhone(), bộ chuyển đổi Jackson của Spring Boot vẫn tự động ánh xạ thuộc tính này thành verifiedPhone khi xuất chuỗi JSON phản hồi về máy khách.
- **Mức độ ảnh hưởng**: Thấp. Không gây lỗi thực thi ở thời điểm hiện tại do sự tương thích của cơ chế ánh xạ thuộc tính JavaBean, nhưng gây khó khăn và nhầm lẫn cho việc bảo trì mã nguồn Java.

---

## 4. Kết Quả Nghiệm Thu (Acceptance Sign-off)

### Bảng Tổng Hợp Kết Quả Đánh Giá Tích Hợp

| Tiêu Chí Đánh Giá | Kết Quả Đạt Được | Kiến Nghị Khắc Phục | Kết Luận |
| :--- | :--- | :--- | :--- |
| **Định Tuyến Của Gateway** | Các yêu cầu công khai đi qua cổng 8080. Không có yêu cầu trực tiếp cổng 8081/8086 từ phía client. | Cần loại bỏ hoặc chặn hoàn toàn tuyến /internal/users/** trên API Gateway để ngăn chặn rò rỉ bảo mật. | CẦN ĐIỀU CHỈNH |
| **Luồng Đăng Ký Tài Khoản** | Hoàn thành biểu mẫu, kiểm tra nghiệp vụ và chuyển đổi luồng sang OTP hoạt động trơn tru. | Không có. | ĐẠT |
| **Luồng Đăng Nhập** | Đăng nhập thành công trả về đầy đủ Token cặp, lưu trữ an toàn trên local storage. Lỗi Unverified Account và Resend OTP đã hoạt động hoàn hảo. | Không có. | ĐẠT |
| **Ràng Buộc Nghiệp Vụ** | Các quy tắc validate họ tên, email, mật khẩu, kiểm tra chéo năm sinh CCCD được áp dụng nghiêm ngặt. | Đồng bộ tên thuộc tính trong DTO Java UserProfileResponse từ isVerifiedPhone thành verifiedPhone để tránh nhầm lẫn bảo trì. | ĐẠT |

### Kết Luận Chung

Hệ thống tích hợp xác thực của LoraFilm về cơ bản đã đáp ứng đầy đủ yêu cầu nghiệp vụ của luồng Đăng ký và Đăng nhập đầu cuối. Các tương tác mạng, kiểm tra dữ liệu nhạy cảm (CCCD), lưu trữ Token và quy trình Gửi lại OTP (Resend OTP) đã được triển khai chính xác, hoạt động ổn định. Để chuẩn bị cho việc tích hợp vào nhánh chính develop, khuyến nghị đội ngũ phát triển nhanh chóng khắc phục lỗ hổng định tuyến tuyến API nội bộ trên Gateway. Các sai lệch về hợp đồng đăng nhập đã được xử lý triệt để.
