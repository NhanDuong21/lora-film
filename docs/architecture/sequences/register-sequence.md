# Luồng Nghiệp Vụ Đăng Ký Tài Khoản - Sequence Diagram

Tài liệu này mô tả chi tiết sơ đồ tuần tự và luồng xử lý nghiệp vụ cho tính năng **Đăng Ký Tài Khoản** của hệ thống Đặt Vé Xem Phim Trực Tuyến.

*   **Thành viên phụ trách:** Nhóm (Cả nhóm)
*   **Trạng thái tài liệu:** Bản thảo nháp (already drafted)

---

## 1. Mục Đích (Purpose)

Mô tả quy trình đăng ký tài khoản mới của người dùng trong hệ thống Đặt Vé Xem Phim Trực Tuyến, đảm bảo dữ liệu đăng ký hợp lệ, mật khẩu được mã hóa an toàn và không bị trùng lặp email đăng ký.

---

## 2. Phạm Vi Hệ Thống (Scope)

Bao gồm các thành phần: Client React, API Gateway, auth-service và MySQL database.

---

## 3. Thành Phần Tham Gia (Participants)

Dưới đây là danh sách chi tiết các đối tượng và dịch vụ xuất hiện trong sơ đồ tuần tự:

| Thành Phần (Participant) | Loại (Actor / Service / DB / Cache) | Vai Trò & Nhiệm Vụ Trong Luồng |
| :--- | :--- | :--- |
| **Người dùng** | Actor | Nhập thông tin đăng ký tại màn hình của client. |
| **Client (React)** | Client | Hiển thị form đăng ký, validate dữ liệu client-side và gửi request API. |
| **API Gateway** | Gateway | Tiếp nhận request và định tuyến tới auth-service. |
| **auth-service** | Service | Xử lý logic đăng ký tài khoản (kiểm tra email, mã hóa mật khẩu, lưu database). |
| **MySQL (Auth DB)** | DB | Lưu trữ thông tin tài khoản người dùng đã tạo. |

---

## 4. Sơ Đồ Sequence Diagram (Diagram Image)

![Sơ đồ tuần tự Đăng Ký Tài Khoản](../diagrams/register-sequence.png)

---

## 5. Mô Tả Luồng Xử Lý Chính (Main Flow Steps)

1.  **Bước 1:** Người dùng mở trang đăng ký và điền các thông tin yêu cầu: Tên tài khoản, Mật khẩu, Xác nhận mật khẩu, Họ và tên, Email, Số điện thoại. Sau khi hoàn thành, nhấn nút đăng ký.
2.  **Bước 2:** React Frontend thực hiện validate dữ liệu cơ bản ở phía client và gửi yêu cầu đăng ký (POST `/api/auth/register`) tới API Gateway.
3.  **Bước 3:** API Gateway nhận request và chuyển tiếp tới auth-service.
4.  **Bước 4:** auth-service validate tính hợp lệ của dữ liệu đầu vào (định dạng email, độ dài mật khẩu, mật khẩu xác nhận khớp nhau).
5.  **Bước 5:** auth-service kiểm tra sự tồn tại của email trong MySQL database (`SELECT * FROM users WHERE email = ?`).
6.  **Bước 6:** Nếu email chưa tồn tại, auth-service thực hiện mã hóa mật khẩu bằng thuật toán BCrypt.
7.  **Bước 7:** auth-service ghi nhận thông tin tài khoản mới vào database MySQL (`INSERT INTO users (...)`).
8.  **Bước 8:** Sau khi MySQL xác nhận lưu thành công, auth-service trả về mã HTTP `201 Created` qua API Gateway đến React Frontend.
9.  **Bước 9:** React Frontend hiển thị thông báo thành công và chuyển hướng người dùng sang trang đăng nhập.

---

## 6. Luồng Xử Lý Thay Thế / Ngoại lệ (Alternative/Error Flows)

### Lỗi A: Email đã tồn tại trong hệ thống (Conflict)
*   **Mô tả:** Email đăng ký đã được liên kết với một tài khoản khác từ trước.
*   **Xử lý:**
    1.  auth-service truy vấn DB thấy email đã tồn tại.
    2.  Hệ thống ngừng đăng ký và trả về mã lỗi `409 Conflict`.
    3.  Client hiển thị thông báo lỗi "Email đã được sử dụng".

### Lỗi B: Dữ liệu đầu vào không đúng định dạng (Bad Request)
*   **Mô tả:** Người dùng cố tình bypass validation client-side và gửi dữ liệu sai định dạng (ví dụ: email không có ký tự `@`).
*   **Xử lý:**
    1.  auth-service kiểm tra dữ liệu đầu vào và phát hiện sai sót.
    2.  Hệ thống trả về mã lỗi `400 Bad Request`.

---

## 7. Ghi Chú Thiết Kế (Design Notes)

*   **Mã hóa:** Tuyệt đối không lưu trữ mật khẩu dạng plain text dưới database. Bắt buộc sử dụng mã hóa BCrypt.
*   **Duy nhất:** Thiết lập trường `email` là UNIQUE trong schema MySQL để tránh ghi đè dữ liệu ở mức vật lý.

---

## 8. Checklist Tự Kiểm Tra (Self-Checklist)

Lập trình viên phụ trách phải tự tích chọn kiểm tra trước khi yêu cầu review:

- [x] Sơ đồ UML được vẽ rõ ràng, không bị chồng chéo các đường lifelines.
- [x] Các HTTP status code trả về được ghi nhận cụ thể (`201 Created`, `409 Conflict`, v.v.).
- [x] Đã mô tả đầy đủ các bước kiểm tra tính hợp lệ dữ liệu (Validation).
- [x] Không có microservice nào truy cập chéo database của nhau.
- [x] Đã nhúng đúng đường dẫn ảnh tương đối tới thư mục `diagrams/`.

---

> [!NOTE]
> **Lưu ý:** Sequence Diagram này đại diện cho thiết kế kiến trúc ban đầu của tính năng và có thể được điều chỉnh linh hoạt trong quá trình phát triển thực tế để phù hợp với các cải tiến công nghệ.
