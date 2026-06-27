# Báo Cáo Kiểm Thử Tích Hợp Hệ Thống (E2E) - Luồng Xác Thực và Hồ Sơ Người Dùng LoraFilm

> [!NOTE]
> Báo cáo này ghi nhận kết quả kiểm thử hộp đen (Black-box E2E testing) kết hợp kiểm tra trạng thái dữ liệu (State Verification) trên cơ sở dữ liệu MySQL và Redis của hệ thống **LoraFilm React + Vite Frontend** tích hợp qua **API Gateway**.

---

## 1. Thông Tin Môi Trường Kiểm Thử

Hệ thống được chạy trên môi trường Windows Local với cấu hình định tuyến như sau:
* **React Frontend**: `http://localhost:5174` (Chạy qua Vite Dev Server, cổng 5173 đã bị chiếm dụng nên Vite tự động chuyển sang 5174).
* **API Gateway**: `http://localhost:8080` (Định tuyến toàn bộ yêu cầu backend).
* **Auth Service**: `http://localhost:8081` (Dịch vụ xác thực độc lập).
* **User Service**: `http://localhost:8086` (Dịch vụ quản lý thông tin khách hàng và nhân viên).
* **CCCD Check API**: `http://localhost:8192/api/cccd/check` (Dịch vụ nội bộ mô phỏng kiểm tra căn cước công dân).

---

## 2. Kiểm Thử Dịch Vụ Kiểm Tra CCCD (CCCD Check API Validation)

Kiểm thử viên đã gửi các yêu cầu trực tiếp đến endpoint `POST http://localhost:8192/api/cccd/check` để kiểm tra cơ chế xác thực API Key và cấu trúc phản hồi:

### Kết quả kiểm tra:
* **Khả năng tiếp cận (Reachability)**: ĐẠT (Cổng 8192 phản hồi nhanh chóng).
* **Mã trạng thái (Status Code)**: 
  * `200 OK` khi đính kèm tiêu đề hợp lệ `x-api-key: lora_cccd_2026_secret`.
  * `401 Unauthorized` khi thiếu hoặc sai khóa bảo mật `x-api-key`.
* **Cấu trúc phản hồi (Response Structure)**: Chứa đầy đủ các trường thông tin mong đợi:
  * `valid` (boolean)
  * `cccdMasked` (string - che mặt nạ dạng `092******123`)
  * `provinceCode` (string)
  * `provinceName` (string)
  * `gender` (string: `MALE`/`FEMALE`)
  * `birthYear` (int)
* **Quy tắc trích xuất dữ liệu**: Thử nghiệm với mã CCCD `092098001234` giải mã thành công:
  * Tỉnh thành: Cần Thơ (`092`)
  * Giới tính: Nam (`MALE`, số thứ tư đại diện là `0` thế kỷ 20)
  * Năm sinh: `1998`

---

## 3. Bảng Tổng Hợp Kịch Bản Kiểm Thử E2E (E2E Scenarios Summary)

| Kịch Bản | Mục Tiêu Kiểm Thử | Trạng Thái | Chi Tiết Ghi Nhận & Bằng Chứng |
| :--- | :--- | :---: | :--- |
| **1. Đăng ký trống** | Kiểm tra hiển thị thông báo lỗi khi không điền biểu mẫu. | **ĐẠT** | Tất cả các ô nhập liệu (Họ tên, Email, SĐT, CCCD, Mật khẩu) hiển thị cảnh báo yêu cầu nhập thông tin màu đỏ. |
| **2. Sai khớp ngày sinh** | Kiểm tra ràng buộc so khớp năm sinh trên lịch với năm sinh CCCD. | **ĐẠT** | Nhập CCCD sinh năm 1998 nhưng chọn lịch năm 2005. Giao diện báo lỗi sai lệch ngày sinh và vô hiệu hóa nút đăng ký. |
| **3. Kiểm tra CCCD** | Nút kiểm tra CCCD gọi thành công bên thứ ba và điền thông tin giải mã. | **ĐẠT** | Nhập `092098001234`, nhấn kiểm tra và hiển thị ngay thông tin Cần Thơ, Nam, 1998 đã che mặt nạ. |
| **4. Submit Đăng ký** | Đăng ký thành viên mới và chuyển hướng trang OTP. | **ĐẠT** | Đăng ký thành công tài khoản `e2e.lorafilm.20260627@example.com` với CCCD `092098001234` và SĐT `0989009999`. Trình duyệt chuyển hướng đến `/verify-otp`. |
| **5. Đăng nhập chưa OTP** | Thử đăng nhập tài khoản chưa xác thực OTP. | **ĐẠT** | Hệ thống trả về lỗi `AUTH_ACCOUNT_NOT_VERIFIED`, hiển thị biểu ngữ cảnh báo và tự động điều hướng trở lại màn hình nhập OTP. |
| **6. OTP không chính xác**| Nhập mã OTP không đúng để xem thông báo lỗi. | **ĐẠT** | Nhập mã ngẫu nhiên và hệ thống hiển thị thông báo lỗi màu đỏ `"Mã OTP không chính xác. Vui lòng kiểm tra lại."`. |
| **7. Xác thực OTP đúng** | Xác thực thành công tài khoản bằng OTP từ Redis. | **ĐẠT** | Lấy mã OTP lưu tại Redis key `otp:REGISTRATION:...` (mã giả lập `123456`), nhập thành công và được tự động chuyển hướng về `/login`. |
| **8. Đăng nhập thành công**| Đăng nhập tài khoản đã xác thực. | **ĐẠT** | Nhập thông tin tài khoản vừa xác thực, đăng nhập thành công và chuyển hướng về trang chủ. |
| **9. Lưu trữ Token** | Kiểm tra Token và dữ liệu được lưu tại `localStorage`. | **ĐẠT** | Ghi nhận sự tồn tại của `authToken`, `refreshToken`, `tokenType: Bearer`, `userEmail`, `userRole` và `userAccountId` trong Local Storage. CCCD gốc không bị lưu. |
| **10. Hồ sơ thành viên** | Xem chi tiết thông tin cá nhân và kiểm tra trễ Kafka. | **ĐÃ FIX — CHỜ RETEST API THỰC** | Bằng chứng cũ chỉ xác nhận họ tên, SĐT và CCCD nên chưa bao phủ đầy đủ `birthday` và `gender`. Review sau đó phát hiện UI bỏ trống `phoneNumber`, `birthday`, `gender`. Mapping đã được sửa; xem [Retest sau Review Request Changes](#retest-sau-review-request-changes). |
| **11. Khôi phục phiên** | Tải lại trang (F5) kiểm tra tính bền bỉ của phiên làm việc. | **ĐẠT** | Sau khi F5 trang `/profile`, người dùng vẫn giữ trạng thái đăng nhập, dữ liệu hồ sơ hiển thị đầy đủ mà không bị đá ra ngoài. |
| **12. Route Guard & Role** | Chặn tài khoản CUSTOMER truy cập trang ADMIN `/admin`. | **ĐẠT** | Truy cập `/admin` bằng tài khoản customer bị chặn hoàn toàn và tự động chuyển hướng về trang chủ `/`. |
| **13. Đăng xuất (Logout)** | Nhấn nút đăng xuất để hủy phiên làm việc. | **ĐẠT** | Các token trong `localStorage` bị xóa hoàn toàn. Truy cập trực tiếp vào `/profile` lập tức bị điều hướng về `/login`. |
| **14. Đăng nhập Staff** | Kiểm thử đăng nhập vai trò Nhân viên (Staff/Employee). | **TẠM KHÓA** | Không thể thực hiện kiểm thử thực tế do bảng `employee_profiles` của hệ thống hiện tại đang trống và không có tài khoản staff mẫu nào được cài đặt sẵn. |

---

## 4. Bằng Chứng Thực Nghiệm Chi Tiết (E2E Visual Evidence)

### Luồng Đăng Ký và Kiểm Tra CCCD (Register & CCCD Verification)
Quá trình điền biểu mẫu, kiểm tra tính hợp lệ của CCCD và so khớp năm sinh trước khi gửi yêu cầu đăng ký:

![Register Flow E2E](C:/Users/LENOVO/.gemini/antigravity/brain/223bff5d-cb8b-4835-add6-b2bcea84c018/register_flow_e2e_1782549185987.webp)

---

### Luồng Xác Thực OTP, Đăng Nhập và Xem Hồ Sơ Cá Nhân (OTP, Login, Profile & Guards)
Quá trình xác thực OTP bằng mã `123456`, thực hiện đăng nhập, kiểm tra thông tin hồ sơ đã che CCCD, tải lại trang để khôi phục phiên, kiểm tra quyền Admin, và đăng xuất:

![Auth and Profile E2E](C:/Users/LENOVO/.gemini/antigravity/brain/223bff5d-cb8b-4835-add6-b2bcea84c018/auth_and_profile_e2e_retry_1782549633564.webp)

---

### Giao Diện Trang Chủ Khi Đã Đăng Nhập Thành Công (Homepage Success State)
Avatar người dùng hiển thị chữ cái đầu tên người dùng ("E") trên thanh điều hướng góc trên bên phải đại diện cho trạng thái đăng nhập thành công của khách hàng:

![Homepage Layout](C:/Users/LENOVO/.gemini/antigravity/brain/223bff5d-cb8b-4835-add6-b2bcea84c018/.system_generated/click_feedback/click_feedback_1782549710567.png)

---

## 5. Phân Tích Lỗi Hệ Thống và Hạn Chế Môi Trường (Defects vs. Environment Blockers)

### Lỗi Frontend (Frontend Defects)
* **Profile mapping (đã sửa trong mã nguồn)**: Review phát hiện dữ liệu `phoneNumber`, `birthday`, `gender` có trong API response nhưng chưa được liên kết ổn định vào UI. Kết quả retest chi tiết được ghi ở phần bên dưới.

### Hạn Chế/Rào Cản Môi Trường (Environment Blockers)
* **Thiếu Tài Khoản Nhân Viên (Staff Account Missing)**:
  * *Mô tả*: Không có tài khoản mẫu nào có vai trò `STAFF` hoặc `EMPLOYEE` để kiểm thử luồng đăng nhập phân quyền nhân viên. Bảng `employee_profiles` trống hoàn toàn trong cơ sở dữ liệu `user_db`.
  * *Tác động*: Kịch bản kiểm thử phân quyền nhân viên và truy cập các route `/employee/**` tạm thời bị khóa (Blocked) và chỉ được xác nhận qua code review (đã cấu hình đúng `allowedRoles={["EMPLOYEE", "STAFF"]}` trong `AppRoutes.jsx`).
* **Technical Debt trong Lưu Trữ Token**:
  * *Mô tả*: Việc lưu trữ `authToken` và `refreshToken` trong `localStorage` giúp luồng phát triển frontend dễ dàng tích hợp, nhưng chưa đạt mức độ bảo mật cao nhất cho Production do nguy cơ bị tấn công XSS đánh cắp token.
  * *Kiến nghị*: Nâng cấp cơ chế lưu trữ `refreshToken` sang `HttpOnly Cookie` trong tương lai để bảo vệ phiên người dùng tốt hơn.

---

## Retest sau Review Request Changes

### Vấn đề được review

Backend trả đầy đủ hồ sơ, nhưng Frontend chưa hiển thị ổn định:

- `phoneNumber`
- `birthday`
- `gender`

Bằng chứng E2E cũ chỉ kiểm tra một phần dữ liệu Profile và đã đánh dấu ĐẠT quá sớm. Nội dung cũ được giữ trong bảng kịch bản, đồng thời test case 10 được liên kết đến kết quả retest này.

### Nguyên nhân gốc

- `getUserProfile()` trả API wrapper, trong khi từng consumer tự unwrap; `CustomerProfilePage` còn gọi API lần thứ hai thay vì dùng `AuthContext` làm nguồn dữ liệu duy nhất.
- State cục bộ của email/SĐT khởi tạo trước khi profile bất đồng bộ tải xong và đồng bộ theo object `user` tổng hợp thay vì theo `profile` và email phiên.
- Birthday chưa được chuẩn hóa rõ ràng cho `input[type="date"]`.
- Gender được so sánh qua nhãn hiển thị thay vì enum `MALE`, `FEMALE`, `OTHER`.
- CCCD có fallback giả hard-code khi API chưa có dữ liệu.

### Thay đổi đã thực hiện

- `client/src/services/userService.js`: unwrap duy nhất tại service và trả `response.data.data`.
- `client/src/contexts/AuthContext.jsx`: lưu trực tiếp profile object; tách loading, pending và real-error state.
- `client/src/pages/customer/CustomerProfilePage.jsx`: bỏ request trùng; đồng bộ email/SĐT sau khi profile tải; bind `phoneNumber`; chuẩn hóa birthday; bind gender theo enum; chỉ hiển thị `cccdMasked`; bổ sung loading/error UI.
- `client/src/utils/authStorage.js`: đã kiểm tra, không cần đổi; email lấy từ phiên và không lưu CCCD.

### Kết quả retest

Retest binding bằng trình duyệt headless trên `/profile`, sử dụng đúng cấu trúc response Backend đã xác nhận. Dữ liệu cá nhân được mô tả theo trạng thái để tránh lặp PII.

| Field | API trả về | Frontend hiển thị | Kết quả |
|---|---|---|---|
| fullName | Có | Có | PASS |
| email | Auth session | Có | PASS |
| phoneNumber | Có | Có | PASS |
| birthday | Có, `yyyy-MM-dd` | Có, date đúng định dạng | PASS |
| gender | Có, enum `MALE` | Có, nhãn `Nam` được chọn | PASS |
| cccdMasked | Có | Có, chỉ bản đã che | PASS |

### Regression Result

| Hạng mục | Kết quả |
|---|---|
| `npm run build` | PASS — Vite production build thành công |
| `npm run lint` | FAIL — 12 lỗi, 1 cảnh báo còn lại ở các file ngoài phạm vi thay đổi; targeted lint cho bốn file Profile/Auth PASS |
| `npm test` | NOT RUN — `package.json` không có script `test` |
| Profile initial load | PASS — sáu trường yêu cầu bind đúng trong browser test |
| Refresh Profile | PASS — các trường vẫn populated |
| Navigate away / return | PASS — các trường vẫn populated |
| Auth session restoration | PASS trong browser binding test; API thực chưa xác nhận vì test credential trả `401` |
| Route guard khi không có session | PASS — `/profile` chuyển về `/login` |
| Logout qua UI | BLOCKED — không có credential hợp lệ để tạo phiên API thực |
| URL bị cấm | PASS — không có request tới port `8086` hoặc `/internal/users`; Profile chỉ gọi `/api/users/{accountId}` qua Gateway |
| Full CCCD / null / undefined | PASS — chỉ bind `cccdMasked`, dùng controlled empty values và đã xóa fallback giả |

### Kết luận retest

**BLOCKED — Không đủ môi trường kiểm thử.** Mapping và browser binding đã được sửa và xác nhận, nhưng chưa thể kết luận review comment được giải quyết hoàn toàn ở runtime API thực: tài khoản E2E hiện có trả `401 Unauthorized` với credential trong tài liệu. Cần credential hợp lệ và chạy lại login → Profile → refresh → navigate → logout trước khi đổi kết luận thành PASS.

---

## 7. Kết Luận và Đánh Giá Sẵn Sàng (Acceptance & Readiness Sign-off)

Mapping và binding Profile đã đạt kiểm tra build và browser binding. Luồng E2E với API thực cần được chạy lại bằng tài khoản hợp lệ trước khi xác nhận sẵn sàng nghiệm thu và merge.
