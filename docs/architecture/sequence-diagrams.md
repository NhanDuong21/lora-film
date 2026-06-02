# Hướng Dẫn Chuẩn Hóa Sequence Diagram (Sequence Diagrams Guideline)

Tài liệu này quy định các tiêu chuẩn thiết kế, cấu trúc thư mục, quy ước đặt tên và phân công nhiệm vụ vẽ Sequence Diagram cho dự án Website Đặt Vé Xem Phim Trực Tuyến. Việc chuẩn hóa này giúp các thành viên trong nhóm phát triển đồng bộ hóa thiết kế luồng nghiệp vụ của hệ thống phân tán một cách đồng nhất.

---

## 1. Mục Đích (Purpose)

Tài liệu này được thiết lập nhằm chuẩn hóa quy trình thiết kế, cách vẽ và cấu trúc file cho toàn bộ các thành viên thuộc dự án. Điều này giúp các thành viên dễ dàng cộng tác, duy trì tài liệu thiết kế hệ thống và giúp Mentor của dự án dễ dàng kiểm duyệt, đánh giá các luồng tương tác giữa các microservice, cache, và database.

---

## 2. Cấu Trúc Thư Mục Chuẩn (Standard Folder Structure)

Để đảm bảo việc lưu trữ và theo dõi lịch sử Git một cách khoa học, cấu trúc thư mục liên quan tới Sequence Diagram được chuẩn hóa như sau:

```text
docs/
└── architecture/
    ├── sequence-diagrams.md             # File tài liệu hướng dẫn này
    ├── sequences/                       # Thư mục chứa các tài liệu giải thích chi tiết luồng nghiệp vụ
    │   ├── sequence-template.md         # File mẫu tái sử dụng cho các luồng nghiệp vụ mới
    │   ├── register-sequence.md
    │   ├── login-sequence.md
    │   ├── booking-sequence.md
    │   ├── payment-sequence.md
    │   ├── promotion-score-sequence.md
    │   └── notification-analytics-sequence.md
    └── diagrams/                        # Thư mục chứa các file thiết kế Draw.io và hình ảnh PNG tương ứng
        ├── register-sequence.drawio
        ├── register-sequence.png
        ├── login-sequence.drawio
        ├── login-sequence.png
        ├── booking-sequence.drawio
        ├── booking-sequence.png
        ├── payment-sequence.drawio
        ├── promotion-score-sequence.drawio
        ├── notification-analytics-sequence.drawio
        ├── system-diagram.drawio
        └── system-diagram.png
```

---

## 3. Quy Tắc Đặt Tên (Naming Convention)

Tất cả các tệp tin liên quan đến Sequence Diagram phải được đặt tên đồng nhất theo định dạng kebab-case viết thường và kết thúc bằng hậu tố `-sequence`:

*   **Tệp tài liệu Markdown giải thích:** `docs/architecture/sequences/<flow-name>-sequence.md`
*   **Tệp thiết kế nguồn Draw.io:** `docs/architecture/diagrams/<flow-name>-sequence.drawio`
*   **Tệp hình ảnh xuất ra:** `docs/architecture/diagrams/<flow-name>-sequence.png`

#### Ví dụ chuẩn:
*   `booking-sequence.md`
*   `booking-sequence.drawio`
*   `booking-sequence.png`

---

## 4. Bảng Phân Công Công Việc (Sequence Assignment Matrix)

Dưới đây là bảng phân công thiết kế chi tiết cho từng luồng nghiệp vụ của dự án:

| Luồng Nghiệp Vụ (Flow Name) | Thành Viên Phân Công (Assignee) | Markdown File Path | Draw.io File Path | PNG File Path | Trạng Thái (Status) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Đăng ký tài khoản (Register)** | Nhóm (Cả nhóm) | `docs/architecture/sequences/register-sequence.md` | `docs/architecture/diagrams/register-sequence.drawio` | `docs/architecture/diagrams/register-sequence.png` | Bản thảo nháp (already drafted) |
| **Đăng nhập (Login)** | Nhóm (Cả nhóm) | `docs/architecture/sequences/login-sequence.md` | `docs/architecture/diagrams/login-sequence.drawio` | `docs/architecture/diagrams/login-sequence.png` | Bản thảo nháp (already drafted) |
| **Đặt vé xem phim (Booking)** | Hoàng | `docs/architecture/sequences/booking-sequence.md` | `docs/architecture/diagrams/booking-sequence.drawio` | `docs/architecture/diagrams/booking-sequence.png` | Đang thực hiện (In-progress) |
| **Thanh toán hóa đơn (Payment)** | Vinh | `docs/architecture/sequences/payment-sequence.md` | `docs/architecture/diagrams/payment-sequence.drawio` | `docs/architecture/diagrams/payment-sequence.png` | Đang thực hiện (In-progress) |
| **Khuyến mãi & Điểm tích lũy (Promotion & Score)** | Khang | `docs/architecture/sequences/promotion-score-sequence.md` | `docs/architecture/diagrams/promotion-score-sequence.drawio` | `docs/architecture/diagrams/promotion-score-sequence.png` | Đang thực hiện (In-progress) |
| **Thông báo & Thống kê (Notification & Analytics)** | Thành | `docs/architecture/sequences/notification-analytics-sequence.md` | `docs/architecture/diagrams/notification-analytics-sequence.drawio` | `docs/architecture/diagrams/notification-analytics-sequence.png` | Đang thực hiện (In-progress) |

> [!WARNING]
> **Lưu ý về các tệp tin hình ảnh PNG:** Các tệp hình ảnh `.png` của sơ đồ tuần tự chưa được khởi tạo sẵn (hiện đang trống) do hạn chế trong việc tạo trực tiếp tệp ảnh nhị phân. Khi các thành viên hoàn thiện thiết kế trên file `.drawio` tương ứng, bắt buộc phải thực hiện export sơ đồ ra định dạng `.png` và lưu vào thư mục `docs/architecture/diagrams/` trước khi tạo Merge Request.

---

## 5. Quy Tắc Thiết Kế Chung (General Rules)

Khi vẽ Sequence Diagram, các thành viên bắt buộc phải mô tả đầy đủ vòng đời các đối tượng và định dạng đường mũi tên tương tác như sau:

*   **Các thành phần bắt buộc (Participants):** Sơ đồ phải phản ánh đúng luồng đi từ **Actors** (Người dùng) $\rightarrow$ **API Gateway** (Spring Cloud Gateway) $\rightarrow$ **Services** (Microservices nghiệp vụ) $\rightarrow$ **Database** (MySQL) / **Cache** (Redis) / **Message Queue** (Apache Kafka).
*   **Đường truyền đồng bộ (Synchronous):** Sử dụng mũi tên nét liền đầu tam giác đặc (`->`) cho lời gọi đồng bộ (như HTTP REST API) và mũi tên nét đứt đầu hở (`-->`) cho phản hồi tương ứng kèm HTTP Status Code (ví dụ: `200 OK`, `400 Bad Request`, `401 Unauthorized`).
*   **Đường truyền bất đồng bộ (Asynchronous):** Sử dụng mũi tên nét liền đầu hở (`->>`) khi gửi thông điệp bất đồng bộ (như gửi event lên Kafka Broker) hoặc các tiến trình ngầm không cần chờ phản hồi lập tức.
*   **Xác thực và Validate lỗi:** Luôn mô tả rõ ràng các điểm kiểm tra tính hợp lệ của dữ liệu (validation), các trường hợp xảy ra lỗi xác thực quyền truy cập hay lỗi nghiệp vụ tại API Gateway và từng Service cụ thể.

---

## 6. Checklist Trước Khi Tạo Merge Request

Trước khi tạo Merge Request để gửi bản vẽ thiết kế tới Team Leader **Thành** phê duyệt, hãy tự kiểm tra:

- [ ] Đường dẫn tương đối trỏ tới file ảnh sơ đồ hoạt động chính xác (ví dụ: `../diagrams/<flow-name>-sequence.png`).
- [ ] File PNG sơ đồ đã được xuất đầy đủ và lưu trong `docs/architecture/diagrams/`.
- [ ] Không có microservice nào kết nối và truy cập chéo sang database của microservice khác.
- [ ] Đã mô tả đầy đủ các luồng xử lý lỗi/ngoại lệ (Alternative/Error Flows).
- [ ] Toàn bộ nội dung văn bản giải thích bằng tiếng Việt và khớp chính xác 1-1 với thứ tự các bước trong sơ đồ.

---

## 7. Ghi Chú Vòng Đời (Lifecycle Note)

> [!NOTE]
> Các Sequence Diagram trong hệ thống ban đầu chỉ đại diện cho các bản thảo thiết kế sớm của các Sprint đầu tiên. Trong quá trình phát triển dự án thực tế, các thành viên có thể chủ động cập nhật linh hoạt sơ đồ và tài liệu giải thích tương ứng để phản ánh đúng cấu trúc thay đổi của mã nguồn trong các sprint tiếp theo.
