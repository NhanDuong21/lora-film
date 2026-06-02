# Luồng Nghiệp Vụ Đăng Nhập - Sequence Diagram

Tài liệu này mô tả chi tiết sơ đồ tuần tự và luồng xử lý nghiệp vụ cho tính năng **Đăng Nhập** của hệ thống Đặt Vé Xem Phim Trực Tuyến.

*   **Thành viên phụ trách:** Nhóm (Cả nhóm)
*   **Trạng thái tài liệu:** Bản thảo nháp (already drafted)

---

## 1. Mục Đích (Purpose)

*Mô tả ngắn gọn mục tiêu của luồng nghiệp vụ này (ví dụ: mô tả quy trình đăng nhập, xác thực thông tin người dùng và cấp phát JWT token để truy cập hệ thống).*

---

## 2. Phạm Vi Hệ Thống (Scope)

*Liệt kê các thành phần, hệ thống nội bộ hoặc bên thứ ba trực tiếp tham gia vào luồng xử lý này (ví dụ: Client React, API Gateway, auth-service, MySQL Database).*

---

## 3. Thành Phần Tham Gia (Participants)

Dưới đây là danh sách chi tiết các đối tượng và dịch vụ xuất hiện trong sơ đồ tuần tự:

| Thành Phần (Participant) | Loại (Actor / Service / DB / Cache) | Vai Trò & Nhiệm Vụ Trong Luồng |
| :--- | :--- | :--- |
| **Khách hàng / Admin** | Actor | Người dùng thực hiện yêu cầu đăng nhập vào hệ thống. |
| **Client (React)** | Client | Giao diện nhận thông tin credentials và lưu trữ JWT token. |
| **API Gateway** | Gateway | Định tuyến request đăng nhập và validate JWT cho các request sau. |
| **auth-service** | Service | Xác thực thông tin người dùng và sinh mã JWT token. |
| **MySQL (Auth DB)** | DB | Lưu trữ thông tin tài khoản và mật khẩu đã mã hóa của người dùng. |

---

## 4. Sơ Đồ Sequence Diagram (Diagram Image)

*Nhúng hình ảnh sơ đồ tuần tự đã xuất dưới dạng file PNG tại đây. Đảm bảo file ảnh được lưu trữ tại thư mục `docs/architecture/diagrams/`.*

![Sơ đồ tuần tự Đăng Nhập](../diagrams/login-sequence.png)

---

## 5. Mô Tả Luồng Xử Lý Chính (Main Flow Steps)

Dưới đây là các bước tương tác trong luồng xử lý thành công (Happy Path):

1.  **Bước 1:** Người dùng nhập username và password tại màn hình đăng nhập trên Client.
2.  **Bước 2:** Client gửi yêu cầu đăng nhập qua API Gateway đến auth-service.
3.  **Bước 3:** auth-service truy vấn thông tin người dùng trong MySQL DB và kiểm tra tính hợp lệ của mật khẩu.
4.  **Bước 4:** Nếu mật khẩu chính xác, auth-service sinh mã JWT và trả về cho Client.
5.  **Bước 5:** Client lưu trữ JWT vào localStorage hoặc Cookie và chuyển hướng người dùng sang trang chủ.

---

## 6. Luồng Xử Lý Thay Thế / Lỗi (Alternative/Error Flows)

Quy trình xử lý các trường hợp ngoại lệ, lỗi logic hoặc lỗi kết nối:

### Lỗi A: Thông tin tài khoản hoặc mật khẩu không chính xác
*   **Mô tả:** Người dùng nhập sai mật khẩu hoặc tài khoản không tồn tại trong hệ thống.
*   **Xử lý:**
    1.  auth-service kiểm tra MySQL và phát hiện credentials không khớp.
    2.  Hệ thống trả về mã lỗi `401 Unauthorized`.
    3.  Client hiển thị thông báo lỗi lên màn hình đăng nhập cho người dùng.

### Lỗi B: Tài khoản bị khóa tạm thời
*   **Mô tả:** Tài khoản của người dùng đang trong trạng thái bị khóa do vi phạm chính sách hoặc nhập sai mật khẩu quá nhiều lần.
*   **Xử lý:**
    1.  auth-service truy vấn trạng thái tài khoản trong DB và phát hiện tài khoản bị khóa.
    2.  Hệ thống trả về mã lỗi `403 Forbidden` kèm thông điệp chi tiết.

---

## 7. Ghi Chú Thiết Kế (Design Notes)

*Ghi lại các lưu ý kỹ thuật quan trọng liên quan đến hiệu năng, bảo mật hoặc đồng bộ trạng thái:*
*   **Bảo mật:** Mật khẩu của người dùng bắt buộc phải được băm bằng thuật toán mạnh (ví dụ: BCrypt) trước khi so khớp và lưu trữ.
*   **JWT Payload:** Token JWT trả về phải chứa các claim cơ bản như `userId`, `username`, `roles` và có thời gian hết hạn (TTL) hợp lý.

---

## 8. Checklist Tự Kiểm Tra (Self-Checklist)

Lập trình viên phụ trách phải tự tích chọn kiểm tra trước khi yêu cầu review:

- [x] Sơ đồ UML được vẽ rõ ràng, không bị chồng chéo các đường lifelines.
- [x] Các HTTP status code trả về được ghi nhận cụ thể (`200 OK`, `401 Unauthorized`, v.v.).
- [x] Đã mô tả đầy đủ các bước kiểm tra tính hợp lệ dữ liệu (Validation).
- [x] Không có microservice nào truy cập chéo database của nhau.
- [x] Đã nhúng đúng đường dẫn ảnh tương đối tới thư mục `diagrams/`.

---

> [!NOTE]
> **Lưu ý:** Sequence Diagram này đại diện cho thiết kế kiến trúc ban đầu của tính năng và có thể được điều chỉnh linh hoạt trong quá trình phát triển thực tế để phù hợp với các cải tiến công nghệ.
