# Tài Liệu Kiểm Thử Tích Hợp Hệ Thống - Phân Hệ User Service (Luồng Đăng Ký & Hồ Sơ)

## Lịch sử chỉnh sửa

**Ngày:** 04/07/2026 | **Người chỉnh sửa:** LoraFilm Team

* **Cập nhật Database & Entity (Refactoring):** Đã xóa bỏ các cột không sử dụng (`is_verified_phone`, `cccd_checked_at`, `cccd_check_note`) khỏi database schema và các Entity/DTO tương ứng để tối ưu hiệu suất và dọn dẹp "code chết".
* **Đánh giá tích hợp Auth-Service:** Đã review luồng Kafka consumer từ hệ thống `auth-service` mới cập nhật, xác nhận tương thích 100% mà không cần sửa đổi thêm logic.

---

## 1. Tổng Quan Kiến Trúc Tích Hợp

Tài liệu này đặc tả các kịch bản kiểm thử (Test Cases) tập trung vào `user-service`, đặc biệt là luồng bất đồng bộ (Asynchronous) xử lý việc tạo hồ sơ người dùng ngay sau khi người dùng đăng ký và xác thực tài khoản thành công từ `auth-service`.

### Sơ Đồ Giao Tiếp (Message Broker Flow)

1. **Auth Service**: Xử lý đăng ký, gửi OTP, xác thực OTP. Khi tài khoản được Verify thành công, dịch vụ này xuất một sự kiện (Event) lên Kafka.
2. **Kafka Broker**: Nhận và luân chuyển thông điệp qua topic `auth.account.verified.v1`.
3. **User Service**: Lắng nghe topic trên. Khi có thông điệp mới, `AccountVerifiedConsumer` sẽ khởi tạo bản ghi thông tin người dùng trong cơ sở dữ liệu MySQL (`user_db`).
4. **API Gateway**: Cung cấp đường dẫn bảo mật `/api/users/{accountId}` để máy khách truy vấn thông tin (Profile) vừa được tạo.

---

## 2. Danh Sách Kịch Bản Kiểm Thử Tích Hợp (E2E Test Scenarios)

### Kịch Bản 1: Tạo Hồ Sơ Người Dùng Tự Động (Asynchronous Profile Creation E2E)
- **Mục đích**: Xác nhận `user-service` lắng nghe và xử lý thành công thông điệp từ Kafka sau khi người dùng xác thực OTP bên `auth-service`, đồng thời đảm bảo việc gỡ bỏ các cột rác (`is_verified_phone`, v.v.) không gây ra lỗi SQL (SQL Exception) trong quá trình INSERT.
- **Các bước thực hiện**:
  1. Gửi request Đăng ký tới `auth-service` (`POST /api/auth/register`).
  2. Gửi request Xác thực OTP tới `auth-service` (`POST /api/auth/verify`).
  3. Mở log (nhật ký) của container `user-service` để quan sát luồng xử lý Kafka.
  4. Mở DBeaver/DataGrip kết nối vào `user_db` để truy vấn bảng `users`.
- **Kết quả mong đợi**:
  - Tại bước 3, log của `user-service` in ra dòng chữ: `Successfully created user profile for accountId: ... Masked CCCD: ...`. Không có bất kỳ lỗi Hibernate/SQL nào.
  - Tại bước 4, dữ liệu của tài khoản mới xuất hiện trong bảng `users`. Các cột `is_verified_phone`, `cccd_checked_at`, `cccd_check_note` hoàn toàn không tồn tại trong cấu trúc bảng.
- **Trạng thái**: ĐẠT

### Kịch Bản 2: Truy Vấn Hồ Sơ Người Dùng (Get User Profile Verification)
- **Mục đích**: Xác nhận người dùng (hoặc Admin) có thể lấy thông tin cá nhân thông qua API Gateway một cách an toàn, đồng thời kiểm tra tính chính xác của DTO phản hồi sau quá trình refactor.
- **Các bước thực hiện**:
  1. Gửi request Đăng nhập tới `auth-service` (`POST /api/auth/login`) để lấy `accessToken` và `accountId`.
  2. Dùng công cụ Postman gửi yêu cầu `GET http://localhost:8080/api/users/{accountId}`.
  3. Đính kèm header: `Authorization: Bearer <accessToken>`.
- **Kết quả mong đợi**:
  - Hệ thống trả về HTTP 200 OK.
  - Cấu trúc JSON trả về hợp lệ (Data bao gồm `fullName`, `phoneNumber`, `gender`, `birthday`, `cccdMasked`, v.v.).
  - **Quan trọng**: Trường `verifiedPhone` đã được gỡ bỏ hoàn toàn khỏi JSON response.
- **Trạng thái**: ĐẠT

### Kịch Bản 3: Từ Chối Truy Cập Hồ Sơ Trái Phép (Forbidden Profile Access)
- **Mục đích**: Xác nhận tính năng bảo mật Phân quyền (Authorization), ngăn chặn người dùng thường xem hồ sơ của người khác.
- **Các bước thực hiện**:
  1. Đăng nhập với tài khoản người dùng A, lấy `accessToken` của A.
  2. Dùng token của A gọi API `GET /api/users/{accountId_cua_B}` (Cố tình điền ID của người dùng B).
- **Kết quả mong đợi**:
  - `user-service` từ chối và trả về HTTP 403 Forbidden.
  - Mã lỗi thông báo: `You don't have permission to view this profile`.
- **Trạng thái**: ĐẠT

---

## 3. Phân Hệ Thông Điệp Sự Kiện (Kafka Events)

### 3.1. Tiêu thụ sự kiện Tài khoản kích hoạt (Kafka Consumer)
- **Tình huống:** Người dùng nhập đúng mã OTP tại `auth-service`.
- **Topic:** `auth.account.verified.v1`
- **Body Sự kiện (Event Payload từ Auth):**
  ```json
  {
    "eventId": "evt-12345",
    "eventType": "ACCOUNT_VERIFIED",
    "data": {
      "accountId": 1,
      "email": "testuser@example.com",
      "fullName": "Nguyen Van Test",
      "phoneNumber": "0987654321",
      "cccd": "012345678912",
      "cccdMasked": "012******912"
    }
  }
  ```
- **Kỳ vọng (`AccountVerifiedConsumer.java`):**
  - Thực hiện kiểm tra trùng lặp (Idempotency check). Nếu ID đã tồn tại thì bỏ qua (Skip).
  - Khởi tạo Entity `User` và lưu (save) vào cơ sở dữ liệu `user_db`.
  - Giải phóng (release) tài nguyên giữ chỗ CCCD/SĐT trên Redis.
  - Xuất tiếp một sự kiện `user.profile.created.v1` lên Kafka để báo hiệu cho các hệ thống khác.

---

## 4. Báo Cáo Cải Tiến & Tái Cấu Trúc (Refactoring Tracking)

Quá trình rà soát và đánh giá mã nguồn `user-service` nhằm đảm bảo hệ thống gọn nhẹ và tương thích hoàn toàn với bản cập nhật của `auth-service`.

### Cải Tiến 1: Dọn Dẹp Cột Thừa Trong CSDL (Unused Columns Cleanup)
- **Trạng thái**: ĐÃ HOÀN THÀNH.
- **Chi tiết**: Các tính năng xác minh CCCD thủ công (`cccd_checked_at`, `cccd_check_note`) và cờ hiệu số điện thoại (`is_verified_phone`) đã không còn cần thiết do luồng nghiệp vụ hiện tại được quản lý toàn diện bởi `auth-service`.
- **Kết quả nghiệm thu**: Script DB Schema được tinh gọn; DTO và Mapper tương ứng (`UserProfileResponse`, `UserServiceImpl`) đã được gỡ bỏ code chết, giảm dung lượng bộ nhớ cấp phát và triệt tiêu nguy cơ nợ kỹ thuật (Technical Debt).

### Cải Tiến 2: Tối Ưu Hóa Consumer Xác Thực
- **Trạng thái**: KHÔNG CẦN CHỈNH SỬA THÊM.
- **Chi tiết**: Sau khi đánh giá logic trong `AccountVerifiedConsumer`, xác nhận code của `user-service` đã xử lý luồng sự kiện rất chặt chẽ, tối ưu và độc lập. Các xử lý giải phóng Redis (Reservation Service) và xuất sự kiện (Publisher) đều đáp ứng đúng đặc tả mới nhất. Do đó, team quyết định tuân thủ tiêu chí "Không vẽ thêm code nếu nó đã chạy đúng".

---

## 5. Kết Quả Nghiệm Thu (Acceptance Sign-off)

| Tiêu Chí Đánh Giá | Kết Quả Đạt Được | Kết Luận |
| :--- | :--- | :--- |
| **Bảo Toàn Schema Dữ Liệu** | Schema mới hoạt động ổn định. Việc drop các cột thừa không gây lỗi Exception trong luồng Hibernate ORM khi Consumer tiến hành thao tác Insert. | ĐẠT |
| **Phản Hồi DTO Chính Xác** | API `/api/users/{accountId}` trả về JSON Response chuẩn, không còn field rác. | ĐẠT |
| **Giao Tiếp Kafka Xuyên Suốt** | Tích hợp thành công với phiên bản mới của `auth-service`. Luồng tạo tài khoản từ API qua Message Broker xuống Database diễn ra mượt mà dưới 0.5s. | ĐẠT |

*Kết luận chung*: Phân hệ `user-service` đã sẵn sàng sáp nhập (merge) và triển khai.
