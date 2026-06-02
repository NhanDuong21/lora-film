# Hướng Dẫn Chuẩn Hóa Sequence Diagram

Tài liệu này quy định các tiêu chuẩn thiết kế, cấu trúc thư mục, quy ước đặt tên và danh sách phân công vẽ Sequence Diagram cho dự án Website Đặt Vé Xem Phim Trực Tuyến. Việc chuẩn hóa giúp toàn bộ 5 thành viên trong nhóm phát triển đồng bộ hóa thiết kế luồng nghiệp vụ trước khi tiến hành viết code.

---

## 1. Mục Đích (Purpose)

Sequence Diagram (Biểu đồ tuần tự) được sử dụng để mô tả chi tiết cách thức các thành phần trong hệ thống phân tán tương tác với nhau theo trình tự thời gian. Đối với hệ thống microservices của chúng ta, việc vẽ Sequence Diagram giúp:
*   Làm rõ luồng đi của dữ liệu giữa **Client**, **API Gateway**, các **Backend Services**, **Redis Cache**, **MySQL Database** và **Kafka Message Broker**.
*   Phân biệt rõ ràng giữa các giao tiếp đồng bộ (synchronous HTTP/gRPC/WebSocket calls) và không đồng bộ (asynchronous Kafka events).
*   Phát hiện sớm các lỗ hổng logic, các điểm nghẽn hiệu năng, và xác định vị trí thực hiện validate dữ liệu.
*   Làm tài liệu tham chiếu chuẩn cho việc viết code, viết test cases và hỗ trợ quá trình kiểm thử (UAT).

---

## 2. Cấu Trúc Thư Mục Chuẩn (Standard Folder Structure)

Để dễ dàng quản lý phiên bản trên Git, tất cả các tài liệu mô tả luồng nghiệp vụ và hình ảnh sơ đồ đi kèm phải được lưu trữ đúng theo cấu trúc thư mục phân rã dưới đây:

```text
hcm26_cpl_java_05_group3/
├── docs/
│   └── architecture/
│       ├── sequence-diagrams.md       # File tài liệu hướng dẫn này
│       ├── diagrams/                  # Thư mục chứa mã nguồn sơ đồ và ảnh xuất ra
│       │   ├── <flow-name>-sequence.drawio
│       │   └── <flow-name>-sequence.png
│       └── sequences/                 # Thư mục chứa các tài liệu giải thích chi tiết
│           ├── sequence-template.md   # File mẫu tái sử dụng
│           └── <flow-name>-sequence.md
```

---

## 3. Quy Tắc Đặt Tên (Naming Conventions)

Tất cả các tệp tin liên quan đến Sequence Diagram phải được đặt tên đồng nhất theo định dạng viết thường, phân tách bằng dấu gạch ngang (kebab-case) và hậu tố `-sequence`:

*   **Tệp tài liệu giải thích (.md):** `docs/architecture/sequences/<flow-name>-sequence.md`
*   **Tệp nguồn thiết kế Draw.io (.drawio):** `docs/architecture/diagrams/<flow-name>-sequence.drawio`
*   **Tệp hình ảnh xuất ra (.png):** `docs/architecture/diagrams/<flow-name>-sequence.png`

#### Ví dụ chuẩn:
*   `docs/architecture/sequences/booking-sequence.md`
*   `docs/architecture/diagrams/booking-sequence.drawio`
*   `docs/architecture/diagrams/booking-sequence.png`

---

## 4. Danh Sách Phân Công (Sequence Assignment List)

Dưới đây là bảng theo dõi tiến độ và phân công thiết kế Sequence Diagram cho từng nghiệp vụ cốt lõi trong hệ thống:

| Tên Nghiệp Vụ (Flow Name) | Tệp Tài Liệu (.md) | Thành Viên Phụ Trách | Trạng Thái Hiện Tại |
| :--- | :--- | :--- | :--- |
| **Đăng ký tài khoản (Register)** | `register-sequence.md` | Nhóm (Cả nhóm) | Bản thảo nháp (already drafted) |
| **Đăng nhập (Login)** | `login-sequence.md` | Nhóm (Cả nhóm) | Bản thảo nháp (already drafted) |
| **Đặt vé xem phim (Booking)** | `booking-sequence.md` | **Hoàng** | Đang thực hiện |
| **Thanh toán hóa đơn (Payment)** | `payment-sequence.md` | **Vinh** | Đang thực hiện |
| **Khuyến mãi & Điểm tích lũy (Promotion & Score)** | `promotion-score-sequence.md` | **Khang** | Đang thực hiện |
| **Thông báo & Thống kê (Notification & Analytics)** | `notification-analytics-sequence.md` | **Thành** | Đang thực hiện |

---

## 5. Quy Tắc Thiết Kế Chung (General Rules)

Khi thiết kế Sequence Diagram trên Draw.io hoặc các công cụ thiết kế khác, các thành viên bắt buộc phải tuân thủ các quy tắc sau:

### Các thành phần bắt buộc hiển thị (Participants)
Mỗi biểu đồ tuần tự của một luồng nghiệp vụ lớn phải thể hiện đầy đủ (nếu có tham gia) các tác nhân theo thứ tự từ trái qua phải:
1.  **Actor (Tác nhân):** Người dùng (Khách hàng, Quản trị viên, Nhân viên rạp).
2.  **Client (Giao diện):** Ứng dụng React chạy trên trình duyệt.
3.  **API Gateway:** Spring Cloud Gateway (cổng chặn, định tuyến và kiểm tra bảo mật).
4.  **Backend Services:** Các microservice nghiệp vụ tương ứng (ví dụ: `booking-service`, `payment-service`).
5.  **Caches:** Redis (sử dụng cho việc cache dữ liệu và lock ghế tạm thời).
6.  **Databases:** Cơ sở dữ liệu quan hệ MySQL của từng dịch vụ.
7.  **Message Brokers:** Apache Kafka (truyền thông điệp bất đồng bộ).
8.  **Third-parties:** Cổng thanh toán (VNPay/Momo) hoặc dịch vụ SMTP gửi mail.

### Quy chuẩn đường kết nối (Arrows & Lifelines)
*   **Lời gọi đồng bộ (Synchronous Call):** Sử dụng mũi tên nét liền đầu tam giác đặc (`->`) hướng từ đối tượng gọi sang đối tượng nhận.
*   **Phản hồi đồng bộ (Return Message):** Sử dụng mũi tên nét đứt đầu hở (`-->`) hướng ngược lại để mô tả dữ liệu trả về kèm mã HTTP status code (ví dụ: `200 OK`, `400 Bad Request`, `401 Unauthorized`).
*   **Lời gọi bất đồng bộ (Asynchronous Call):** Sử dụng mũi tên nét liền đầu hở (`->>`) khi gửi tin nhắn tới Kafka broker hoặc chạy các tác vụ chạy ngầm.

### Quy trình xác thực dữ liệu (Data Validation)
Tại mỗi điểm chạm dữ liệu, luồng xử lý phải mô tả rõ các hoạt động validate:
*   **Client-side Validation:** Validate định dạng đầu vào (ví dụ: định dạng email, mật khẩu không được rỗng) trước khi gửi API.
*   **Gateway-side Validation:** API Gateway kiểm tra chữ ký và thời hạn của token JWT.
*   **Service-side Validation:** Microservice nhận request kiểm tra logic nghiệp vụ phức tạp (ví dụ: kiểm tra số lượng vé khả dụng, kiểm tra số dư điểm tích lũy) và trả về mã lỗi thích hợp nếu không hợp lệ.

---

## 6. Checklist Trước Khi Tạo Merge Request

Trước khi gửi yêu cầu xem xét mã nguồn (Merge Request) lên nhánh `develop` cho Leader **Thành**, các thành viên phải tự kiểm tra các mục sau:

- [ ] Tên file `.md`, `.drawio`, và `.png` đã tuân thủ đúng định dạng kebab-case với hậu tố `-sequence`.
- [ ] File hình ảnh `.png` đã được xuất từ Draw.io và đặt đúng trong thư mục `docs/architecture/diagrams/`.
- [ ] Sơ đồ mô tả rõ ràng các phản hồi lỗi (Alternative Flows) như lỗi xác thực (401), lỗi dữ liệu đầu vào (400) hoặc lỗi thanh toán thất bại.
- [ ] Các microservice không gọi trực tiếp database của nhau mà đi qua cổng API hoặc giao tiếp bất đồng bộ qua Kafka.
- [ ] Đường dẫn tham chiếu ảnh trong file `.md` sử dụng đường dẫn tương đối chính xác.
- [ ] File mô tả giải thích luồng hoạt động khớp hoàn toàn 1-1 với sơ đồ hình ảnh trực quan.
