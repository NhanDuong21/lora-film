# Luồng Nghiệp Vụ Khuyến Mãi & Điểm Tích Lũy - Sequence Diagram

Tài liệu này mô tả chi tiết sơ đồ tuần tự và luồng xử lý nghiệp vụ cho tính năng **Khuyến Mãi & Điểm Tích Lũy** của hệ thống Đặt Vé Xem Phim Trực Tuyến.

*   **Thành viên phụ trách:** Khang
*   **Trạng thái tài liệu:** Đang thực hiện (In-progress)

---

## 1. Mục Đích (Purpose)

*Mô tả ngắn gọn mục tiêu của luồng nghiệp vụ này (ví dụ: mô tả quy trình áp dụng mã giảm giá/khuyến mãi cho hóa đơn, tính toán tích lũy điểm thưởng cho thành viên sau khi thanh toán thành công và khấu trừ điểm thưởng nếu dùng để đổi quà/giảm giá).*

---

## 2. Phạm Vi Hệ Thống (Scope)

*Liệt kê các thành phần, hệ thống nội bộ hoặc bên thứ ba trực tiếp tham gia vào luồng xử lý này (ví dụ: Client React, API Gateway, booking-service, promotion-service).*

---

## 3. Thành Phần Tham Gia (Participants)

Dưới đây là danh sách chi tiết các đối tượng và dịch vụ xuất hiện trong sơ đồ tuần tự:

| Thành Phần (Participant) | Loại (Actor / Service / DB / Cache) | Vai Trò & Nhiệm Vụ Trong Luồng |
| :--- | :--- | :--- |
| **Khách hàng** | Actor | Chọn mã khuyến mãi và xác nhận tích/tiêu điểm trên ứng dụng. |
| **Client (React)** | Client | Hiển thị danh sách mã khuyến mãi khả dụng và tổng điểm tích lũy của user. |
| **API Gateway** | Gateway | Định tuyến các request liên quan đến mã giảm giá và điểm thành viên. |
| **promotion-service** | Service | Xử lý logic áp dụng mã giảm giá, kiểm tra điều kiện khuyến mãi và quản lý điểm tích lũy của khách hàng. |
| **booking-service** | Service | Áp dụng số tiền giảm trừ vào hóa đơn và tính toán số tiền thanh toán cuối cùng. |
| **MySQL (Promotion DB)** | DB | Lưu trữ thông tin mã giảm giá, lịch sử sử dụng voucher và thông tin điểm tích lũy của user. |

---

## 4. Sơ Đồ Sequence Diagram (Diagram Image)

*Nhúng hình ảnh sơ đồ tuần tự đã xuất dưới dạng file PNG tại đây. Đảm bảo file ảnh được lưu trữ tại thư mục `docs/architecture/diagrams/`.*

![Sơ đồ tuần tự Khuyến Mãi & Điểm Tích Lũy](../diagrams/promotion-score-sequence.png)

---

## 5. Mô Tả Luồng Xử Lý Chính (Main Flow Steps)

Dưới đây là các bước tương tác trong luồng xử lý thành công (Happy Path):

1.  **Bước 1:** Người dùng chọn áp dụng một mã khuyến mãi từ danh sách tại bước thanh toán trên Client.
2.  **Bước 2:** Client gửi yêu cầu áp mã giảm giá qua API Gateway tới promotion-service.
3.  **Bước 3:** promotion-service truy vấn thông tin voucher trong MySQL DB, validate hạn sử dụng, số lượng còn lại và điều kiện áp dụng.
4.  **Bước 4:** Nếu hợp lệ, promotion-service tính toán số tiền giảm giá và phản hồi kết quả về booking-service để cập nhật giá trị hóa đơn.
5.  **Bước 5:** Sau khi thanh toán thành công, booking-service gửi sự kiện thông báo để promotion-service thực hiện cộng điểm tích lũy thành viên tương ứng với giá trị hóa đơn.

---

## 6. Luồng Xử Lý Thay Thế / Lỗi (Alternative/Error Flows)

Quy trình xử lý các trường hợp ngoại lệ, lỗi logic hoặc lỗi kết nối:

### Lỗi A: Mã khuyến mãi hết hạn hoặc hết lượt sử dụng
*   **Mô tả:** Voucher người dùng chọn đã vượt quá số lượt dùng tối đa hoặc quá hạn dùng.
*   **Xử lý:**
    1.  promotion-service kiểm tra DB và phát hiện voucher không đủ điều kiện.
    2.  Hệ thống trả về mã lỗi `400 Bad Request` kèm nội dung lỗi cụ thể.
    3.  Client hiển thị thông báo "Mã giảm giá không hợp lệ hoặc đã hết lượt dùng".

### Lỗi B: Số điểm tích lũy dùng để đổi voucher không đủ
*   **Mô tả:** Người dùng yêu cầu quy đổi điểm thưởng để giảm giá nhưng tài khoản không đủ số dư điểm khả dụng.
*   **Xử lý:**
    1.  promotion-service kiểm tra số dư điểm của user và phát hiện không đủ điểm.
    2.  Hệ thống trả về mã lỗi `400 Bad Request`.

---

## 7. Ghi Chú Thiết Kế (Design Notes)

*Ghi lại các lưu ý kỹ thuật quan trọng liên quan đến hiệu năng, bảo mật hoặc đồng bộ trạng thái:*
*   **Redis Caching:** Các thông tin chi tiết của mã khuyến mãi đang trong chương trình chạy nóng nên được cache trên Redis để chịu tải tốt hơn khi có lượng truy vấn đồng thời lớn.
*   **Đồng nhất dữ liệu:** Việc trừ điểm hoặc cập nhật số lượt dùng của voucher phải được thực thi trong một Database Transaction để tránh bị race condition khi nhiều người dùng áp mã cùng lúc.

---

## 8. Checklist Tự Kiểm Tra (Self-Checklist)

Lập trình viên phụ trách phải tự tích chọn kiểm tra trước khi yêu cầu review:

- [ ] Sơ đồ UML được vẽ rõ ràng, không bị chồng chéo các đường lifelines.
- [ ] Các HTTP status code trả về được ghi nhận cụ thể (`200 OK`, `400 Bad Request`, v.v.).
- [ ] Đã mô tả đầy đủ các bước kiểm tra tính hợp lệ dữ liệu (Validation).
- [ ] Không có microservice nào truy cập chéo database của nhau.
- [ ] Đã nhúng đúng đường dẫn ảnh tương đối tới thư mục `diagrams/`.

---

> [!NOTE]
> **Lưu ý:** Sequence Diagram này đại diện cho thiết kế kiến trúc ban đầu của tính năng và có thể được điều chỉnh linh hoạt trong quá trình phát triển thực tế để phù hợp với các cải tiến công nghệ.
