# Luồng Nghiệp Vụ Thông Báo & Thống Kê (Notification & Analytics) - Sequence Diagram

Tài liệu này mô tả chi tiết sơ đồ tuần tự và luồng xử lý nghiệp vụ cho tính năng **Gửi Thông Báo và Ghi Nhận Thống Kê** của hệ thống Đặt Vé Xem Phim Trực Tuyến sau khi khách hàng đặt vé hoặc thanh toán thành công.

*   **Thành viên phụ trách:** Nhóm (Cả nhóm)
*   **Trạng thái tài liệu:** Bản thảo nháp (already drafted)

---

## 1. Mục Đích (Purpose)

Mô tả quy trình hệ thống xử lý bất đồng bộ các tác vụ gửi thông báo (email/SMS) cho khách hàng và ghi nhận dữ liệu phân tích thống kê thông qua Kafka, ngay sau khi các sự kiện đặt vé hoặc thanh toán diễn ra thành công.

---

## 2. Phạm Vi Hệ Thống (Scope)

Quá trình này diễn ra ngay sau khi Booking Service hoặc Payment Service hoàn tất nghiệp vụ chính của mình. Các thành phần tham gia bao gồm: Client (User), Booking Service, Payment Service, Kafka (Message Broker), Notification Service, Email/SMS Provider, Analytics Service và hệ cơ sở dữ liệu MySQL (Analytics DB).

---

## 3. Thành Phần Tham Gia (Participants)

Dưới đây là danh sách chi tiết các đối tượng và dịch vụ xuất hiện trong sơ đồ tuần tự:

| Thành Phần (Participant) | Loại (Actor / Service / DB / Broker) | Vai Trò & Nhiệm Vụ Trong Luồng |
| :--- | :--- | :--- |
| **User** | Actor | Người dùng thực hiện hành động đặt vé hoặc thanh toán. |
| **Booking Service** | Service | Xử lý logic đặt vé, giao tiếp với Payment Service và phát hành (publish) sự kiện lên Kafka. |
| **Payment Service** | Service | Xử lý logic thanh toán và phát hành (publish) sự kiện lên Kafka khi thanh toán thành công. |
| **Kafka** | Message Broker | Lắng nghe, lưu trữ và phân phối các sự kiện (`booking.success`, `payment.success`) tới các service đăng ký theo dõi. |
| **Notification Service** | Service | Lắng nghe (consume) sự kiện từ Kafka và thực hiện gửi thông báo xác nhận vé cho người dùng. |
| **Email/SMS Provider** | 3rd Party Service | Dịch vụ bên thứ ba chịu trách nhiệm gửi Email hoặc SMS thực tế tới khách hàng. |
| **Analytics Service** | Service | Lắng nghe (consume) sự kiện từ Kafka để ghi nhận, tổng hợp dữ liệu thống kê hệ thống. |
| **MySQL / Analytics DB** | DB | Lưu trữ dữ liệu thống kê phục vụ cho mục đích kết xuất báo cáo trong tương lai. |

---

## 4. Sơ Đồ Sequence Diagram (Diagram Image)

*Sơ đồ tuần tự thể hiện kiến trúc Event-driven của luồng Thông báo & Thống kê.*

![Sơ đồ tuần tự Thông Báo & Thống Kê](../diagrams/notification-analytics-sequence.png)

---

## 5. Mô Tả Luồng Xử Lý Chính (Main Flow Steps)

Dưới đây là các bước tương tác trong luồng xử lý thành công (Happy Path) dựa trên thiết kế hướng sự kiện:

1.  **Bước 1:** Người dùng (User) khởi tạo hành động đặt vé hoặc thanh toán gửi tới Booking Service.
2.  **Bước 2:** Hệ thống xử lý theo 2 nhánh tùy vào tiến trình:
    *   **Trường hợp Đặt vé thành công [Booking Success]:** Booking Service xử lý thành công và phát hành sự kiện `booking.success event` lên Kafka.
    *   **Trường hợp Thanh toán thành công [Payment Success]:** Booking Service gửi lệnh xử lý thanh toán (Process payment) tới Payment Service. Sau khi hoàn tất, Payment Service phát hành sự kiện `payment.success event` lên Kafka.
3.  **Bước 3:** Kafka đảm nhận việc lưu trữ và tiến hành phân phối các sự kiện này (Store and distribute events) tới các consumer.
4.  **Bước 4 (Xử lý Thông báo):** Notification Service lắng nghe (Consume event) sự kiện từ Kafka, gọi API tới Email/SMS Provider để gửi thông báo/email xác nhận vé cho người dùng và nhận lại thông điệp xác nhận đã gửi (Confirm sent).
5.  **Bước 5 (Xử lý Thống kê):** Analytics Service lắng nghe (Consume event) sự kiện từ Kafka, tiến hành ghi nhận dữ liệu thống kê (Record analytics data) vào MySQL / Analytics DB và nhận lại xác nhận đã lưu thành công (Confirm saved). Dữ liệu này sẽ được dùng cho báo cáo sau này.

---

## 6. Luồng Xử Lý Thay Thế / Lỗi (Alternative/Error Flows)

Quy trình xử lý các trường hợp ngoại lệ, lỗi logic hoặc lỗi mạng:

### Lỗi A: Kafka Broker không phản hồi (Broker Down)
*   **Mô tả:** Booking Service hoặc Payment Service không thể publish event lên Kafka do hệ thống Kafka gặp sự cố.
*   **Xử lý:**
    1.  Service phát hành sự kiện thực hiện retry (thử lại) theo cơ chế Exponential Backoff.
    2.  Nếu vẫn thất bại, lưu sự kiện lỗi vào bảng `outbox` trong database cục bộ để quét và publish bù sau khi Kafka phục hồi (Tránh mất dữ liệu).
    3.  Nghiệp vụ người dùng vẫn được tính là thành công, tuy nhiên thông báo sẽ đến muộn hơn.

### Lỗi B: Email/SMS Provider gặp sự cố
*   **Mô tả:** Dịch vụ gửi email hoặc SMS của bên thứ 3 từ chối kết nối hoặc báo lỗi.
*   **Xử lý:**
    1.  Notification Service nhận phản hồi lỗi từ Provider.
    2.  Hệ thống tiếp tục thực hiện cơ chế retry cục bộ. Nếu thất bại sau nhiều lần, đẩy message này vào một hàng đợi Dead Letter Queue (DLQ).
    3.  Admin/Hệ thống giám sát có thể kiểm tra DLQ để gửi lại thông báo cho khách hàng thủ công hoặc khi Provider hoạt động ổn định trở lại.

---

## 7. Ghi Chú Thiết Kế (Design Notes)

*   **Bất đồng bộ (Asynchronous):** Việc tách rời Notification và Analytics ra khỏi luồng Booking/Payment chính giúp thời gian phản hồi API (Response Time) trả về cho người dùng nhanh chóng hơn rất nhiều.
*   **Decoupling:** Sự cố ở Notification Service hoặc Analytics Service sẽ hoàn toàn không làm ảnh hưởng (block) đến việc đặt vé hay thanh toán của khách hàng.
*   **Tính mở rộng (Scalability):** Khi có lượng đặt vé tăng đột biến, hệ thống có thể mở rộng (scale) riêng biệt các consumer (Notification/Analytics) để xử lý lượng lớn events từ Kafka mà không làm nghẽn hệ thống core.

---

## 8. Checklist Tự Kiểm Tra (Self-Checklist)

Lập trình viên phụ trách phải tự tích chọn kiểm tra trước khi yêu cầu review:

- [x] Sơ đồ UML được vẽ rõ ràng, không bị chồng chéo các đường lifelines.
- [x] Các luồng publish/consume message qua Kafka được mô tả cụ thể, rõ ràng.
- [x] Đã liệt kê đủ các thành phần tham gia (Services, Kafka, Database).
- [x] Đã mô tả hướng xử lý lỗi khi Message Broker (Kafka) hoặc 3rd Party gặp sự cố.
- [x] Đã nhúng đúng đường dẫn ảnh tương đối tới thư mục `diagrams/`.

---

> [!NOTE]
> **Lưu ý:** Sequence Diagram này đại diện cho thiết kế kiến trúc ban đầu của tính năng và có thể được điều chỉnh linh hoạt trong quá trình phát triển thực tế để phù hợp với các cải tiến công nghệ.
