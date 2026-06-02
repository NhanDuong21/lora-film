# [Tên Nghiệp Vụ] - Sequence Diagram

Tài liệu này mô tả chi tiết sơ đồ tuần tự và luồng xử lý nghiệp vụ cho tính năng **[Tên Nghiệp Vụ]** của hệ thống Đặt Vé Xem Phim Trực Tuyến.

---

## 1. Mục Đích (Purpose)

*Mô tả ngắn gọn mục tiêu của luồng nghiệp vụ này (ví dụ: mô tả quy trình giữ ghế và thanh toán vé xem phim trực tuyến của khách hàng, đảm bảo tính nhất quán dữ liệu và trải nghiệm người dùng).*

---

## 2. Phạm Vi Hệ Thống (Scope)

*Liệt kê các thành phần, hệ thống nội bộ hoặc bên thứ ba trực tiếp tham gia vào luồng xử lý này (ví dụ: Client React, API Gateway, booking-service, payment-service, cổng thanh toán VNPay).*

---

## 3. Thành Phần Tham Gia (Participants)

Dưới đây là danh sách chi tiết các đối tượng và dịch vụ xuất hiện trong sơ đồ tuần tự:

| Thành Phần (Participant) | Loại (Actor / Service / DB / Cache) | Vai Trò & Nhiệm Vụ Trong Luồng |
| :--- | :--- | :--- |
| **Tên đối tượng** | Actor / Service / DB / Cache | Mô tả vai trò xử lý thông tin hoặc lưu trữ dữ liệu. |
| ... | ... | ... |

---

## 4. Sơ Đồ Sequence Diagram (Diagram Image)

*Nhúng hình ảnh sơ đồ tuần tự đã xuất dưới dạng file PNG tại đây. Đảm bảo file ảnh được lưu trữ tại thư mục `docs/architecture/diagrams/`.*

![Sơ đồ tuần tự [Tên Nghiệp Vụ]](../diagrams/[flow-name]-sequence.png)

---

## 5. Mô Tả Luồng Xử Lý Chính (Main Flow Steps)

Dưới đây là các bước tương tác trong luồng xử lý thành công (Happy Path):

1.  **Bước 1:** [Mô tả chi tiết tác nhân nào gửi thông tin gì, đi qua đâu].
2.  **Bước 2:** [Mô tả hệ thống xử lý thông tin, lưu trữ database hoặc cache].
3.  **Bước 3:** [Mô tả phản hồi dữ liệu thành công trả về cho client].
4.  ...

---

## 6. Luồng Xử Lý Thay Thế / Lỗi (Alternative/Error Flows)

Quy trình xử lý các trường hợp ngoại lệ, lỗi logic hoặc lỗi kết nối:

### Lỗi A: [Tên trường hợp lỗi - ví dụ: Xác thực thất bại hoặc token hết hạn]
*   **Mô tả:** [Chi tiết khi lỗi xảy ra ở bước nào].
*   **Xử lý:**
    1.  [Bước xử lý lỗi 1].
    2.  [Bước xử lý lỗi 2].
    3.  Trả về mã HTTP status code tương ứng (ví dụ: `401 Unauthorized` hoặc `403 Forbidden`).

### Lỗi B: [Tên trường hợp lỗi - ví dụ: Ghế đã bị người khác chọn trước]
*   **Mô tả:** [Chi tiết lỗi khi hệ thống kiểm tra tình trạng khả dụng].
*   **Xử lý:**
    1.  [Bước xử lý lỗi 1].
    2.  Trả về mã HTTP status code tương ứng (ví dụ: `409 Conflict`).

---

## 7. Ghi Chú Thiết Kế (Design Notes)

*Ghi lại các lưu ý kỹ thuật quan trọng liên quan đến hiệu năng, bảo mật hoặc đồng bộ trạng thái:*
*   **Redis Caching / Lock:** *Cấu hình thời gian giữ khóa tạm thời (TTL), tên key trên Redis (ví dụ: `lock:seat:<seat_id>`).*
*   **Kafka Event Payload:** *Mô tả cấu trúc dữ liệu gửi lên Kafka topic (ví dụ: JSON payload chứa `bookingId`, `userId`, `email`).*
*   **State Management:** *Cách cập nhật trạng thái đơn hàng (ví dụ: PENDING -> PAID -> COMPLETED).*

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
