# Tài Liệu Biện Luận và Tư Vấn Kiến Trúc (Architecture Advice Document)

Tài liệu này cung cấp cơ sở lập luận kỹ thuật (technical justification) và biện luận giải pháp công nghệ cho dự án Website Đặt Vé Xem Phim Trực Tuyến. Đây là tài liệu tham khảo chính thức giúp nhóm phát triển tự tin trình bày và bảo vệ các lựa chọn công nghệ của mình trước các Mentor và Hội đồng công nghệ.

---

## 1. Giới Thiệu Tổng Quan (Introduction)

Mục đích của tài liệu này là đưa ra các lập luận phân tích chi tiết về mặt kỹ thuật đối với từng thành phần công nghệ được sử dụng trong hệ thống. Tài liệu này đóng vai trò như một báo cáo biện luận phòng thủ, phân tích rõ ràng các ưu điểm, nhược điểm và rủi ro đi kèm của từng công nghệ, đồng thời cung cấp các giải pháp giảm thiểu tương ứng.

> [!NOTE]
> **Phạm vi tài liệu:** Đây là phần biện luận lựa chọn công nghệ. Topology và
> service ownership hiện hành được mô tả trong
> [`system-design.md`](system-design.md); code/config hiện tại được ưu tiên khi
> một đề xuất trong tài liệu này chưa được triển khai.

---

## 2. Lý Do Lựa Chọn Công Nghệ (Technology Justifications)

Dưới đây là các lý do thực tế và chuyên sâu lý giải tại sao nhóm lựa chọn các công nghệ cụ thể cho dự án này:

### 2.1. Frontend: React + Vite
*   **Tốc độ Build vượt trội:** Sử dụng công cụ Vite giúp thời gian khởi chạy server local và build ứng dụng nhanh hơn gấp nhiều lần so với Webpack truyền thống nhờ vào cơ chế tận dụng tối đa ES Modules gốc của trình duyệt.
*   **Cơ chế Hot Module Replacement (HMR) cực nhanh:** Giúp cập nhật giao diện ngay lập tức khi thay đổi code mà không cần tải lại toàn bộ trang, giữ nguyên trạng thái ứng dụng trong quá trình debug giao diện.
*   **Hiệu năng Single Page Application (SPA) gọn nhẹ:** Cung cấp trải nghiệm mượt mà, chuyển trang không giật lag cho khách hàng đặt vé xem phim, đồng thời giảm thiểu tải cho server bằng cách render hoàn toàn tại phía client.

### 2.2. Backend Services: Java Spring Boot
*   **Hệ sinh thái phong phú và trưởng thành:** Spring Boot thừa hưởng toàn bộ sức mạnh của nền tảng Java với hàng ngàn thư viện hỗ trợ tích hợp sẵn có (như Spring Security, Spring Data JPA, Spring Cloud).
*   **Tính an toàn kiểu dữ liệu (Type-Safety):** Java là ngôn ngữ statically-typed, giúp phát hiện sớm các lỗi cú pháp và logic ngay từ lúc compile, giảm thiểu lỗi crash hệ thống ở môi trường production.
*   **Hỗ trợ đa luồng (Multi-threading) mạnh mẽ:** Khả năng xử lý song song vượt trội, thích hợp với các dịch vụ backend microservices cần xử lý hàng ngàn giao dịch đặt chỗ đồng thời.

### 2.3. Cổng Kết Nối: API Gateway (Spring Cloud Gateway)
*   **Điểm truy cập tập trung (Single Entry Point):** Giúp che giấu cấu trúc mạng nội bộ bên trong, giúp client chỉ cần giao tiếp với một domain duy nhất thay vì phải gọi trực tiếp tới từng microservice.
*   **Xử lý các mối quan tâm chung (Cross-cutting Concerns):** Tự động hóa các tác vụ lặp đi lặp lại như định tuyến động (dynamic routing), cân bằng tải (load balancing), kiểm tra tính hợp lệ của token JWT và lọc bảo mật đầu vào trước khi request chạm tới các service nghiệp vụ.

### 2.4. Cơ Sở Dữ Liệu Quan Hệ: MySQL
*   **Tuân thủ chặt chẽ ACID:** Đảm bảo các giao dịch (transactions) đặt vé và thanh toán trực tuyến diễn ra một cách chính xác tuyệt đối. Tránh hoàn toàn lỗi không đồng nhất dữ liệu hoặc mất mát thông tin tài chính của khách hàng.
*   **Khả năng tối ưu truy vấn tốt:** Hỗ trợ lập chỉ mục (indexing) mạnh mẽ, truy vấn kết hợp nhiều bảng (join queries) hiệu quả đối với các cấu trúc dữ liệu quan hệ phức tạp như lịch chiếu, ghế ngồi và hóa đơn.

### 2.5. Lớp Đệm Dữ Liệu: Redis (Cache & Session Store)
*   **Lưu trữ trên RAM (In-Memory Database) tốc độ cực cao:** Đọc/ghi dữ liệu trong thời gian dưới mili-giây, thích hợp cho việc cache thông tin suất chiếu hoặc danh sách phim hot có tần suất đọc lớn nhưng ít khi thay đổi.
*   **Khóa trạng thái tạm thời (Locking Timeout / TTL):** Rất quan trọng trong nghiệp vụ đặt vé. Sử dụng cơ chế Time-To-Live (TTL) của Redis để giữ ghế tạm thời (seat lock) trong vòng 5-10 phút khi khách đang thực hiện thanh toán, tự động giải phóng ghế nếu khách hủy giao dịch mà không làm ảnh hưởng tới MySQL.

### 2.6. Hệ Thống Tin Nhắn: Apache Kafka
*   **Giảm thiểu sự phụ thuộc trực tiếp (Decoupling):** Cho phép các microservices giao tiếp không đồng bộ. Booking, Payment, Promotion, Score, Notification và Analytics trao đổi các lifecycle event theo contract mà không cần dùng chung database.
*   **Khả năng chịu tải và lưu trữ thông điệp lớn (High Throughput):** Đảm bảo không bỏ sót bất kỳ sự kiện thanh toán hoặc gửi vé nào ngay cả khi hệ thống notification gặp sự cố tạm thời, nhờ cơ chế lưu trữ hàng đợi tin nhắn bền vững của Kafka.

### 2.7. Xác Thực: JWT (JSON Web Tokens)
*   **Access token tự chứa (Self-contained):** JWT mang identity và authority cần thiết để Gateway/service kiểm tra nhanh. Auth Service vẫn quản lý refresh token, session và trạng thái thu hồi để hỗ trợ logout, đổi mật khẩu và vô hiệu hóa truy cập.
*   **Phù hợp với Microservices:** API Gateway có thể tự giải mã và kiểm tra chữ ký token JWT một cách độc lập để quyết định cho phép request đi qua hay không, không cần phải thực hiện truy vấn database hoặc gọi API tới `auth-service` cho mỗi request.

### 2.8. Hạ tầng local bằng Docker Compose

*   **Môi trường hạ tầng nhất quán:** Root Compose cố định MySQL, Redis, Kafka và Zookeeper cho máy phát triển.
*   **Ranh giới hiện tại:** Compose không đóng gói 9 Java service, Gateway, Eureka hay frontend. Các application process chạy trực tiếp bằng Maven/npm như hướng dẫn trong root README.

---

## 3. Phân Tích Đánh Giá & Quản Trị Rủi Ro (Trade-offs & Risk Matrix)

Việc áp dụng kiến trúc phân tán (Microservices) với tập hợp công nghệ trên mang lại nhiều lợi ích lớn nhưng cũng đi kèm với các thách thức kỹ thuật. Dưới đây là bảng phân tích chi tiết:

| Thành phần kiến trúc / Công nghệ | Ưu điểm (Pros) | Nhược điểm & Rủi ro (Cons / Risks) | Giải pháp giảm thiểu rủi ro (Mitigation Strategies) |
| :--- | :--- | :--- | :--- |
| **Kiến trúc Microservices** | * Phát triển và deploy các dịch vụ độc lập.<br/>* Lỗi ở một service không làm sập toàn bộ hệ thống. | * Hệ thống phức tạp, khó debug khi xảy ra lỗi liên kết.<br/>* Khó đảm bảo tính nhất quán dữ liệu tức thời (Data Consistency). | * Áp dụng cơ chế Distributed Tracing (như Spring Cloud Sleuth/Zipkin) để theo dõi request.<br/>* Sử dụng mô hình Event-driven và Transaction bù (Saga Pattern) cho các giao dịch liên dịch vụ. |
| **Spring Cloud Gateway** | * Điểm kiểm soát bảo mật và rate limiting tập trung.<br/>* Giảm tải logic xác thực cho backend. | * Trở thành điểm nghẽn cổ chai (Single Point of Failure - SPOF) nếu gateway bị sập hoặc quá tải. | * Triển khai cụm Gateway chạy song song dưới sự điều phối của Load Balancer.<br/>* Cấu hình cơ chế Circuit Breaker (Resilience4j) để ngắt các request lỗi kéo dài. |
| **Apache Kafka** | * Xử lý luồng sự kiện không đồng bộ hiệu năng cao.<br/>* Đảm bảo tính tin cậy của thông điệp gửi đi. | * Khó cấu hình và vận hành ổn định trong môi trường local.<br/>* Rủi ro xử lý trùng lặp thông điệp (Duplicate message). | * Sử dụng Docker-compose đóng gói sẵn cấu hình Kafka tiêu chuẩn.<br/>* Thiết kế logic xử lý sự kiện có tính idempotency (đảm bảo xử lý nhiều lần một event vẫn cho ra một kết quả duy nhất). |
| **Redis Cache** | * Giảm tải tối đa cho cơ sở dữ liệu MySQL.<br/>* Xử lý lock ghế phòng chiếu thời gian thực cực nhanh. | * Rủi ro dữ liệu trên Cache bị lệch nhịp so với Database gốc (Stale Cache). | * Thiết lập chính sách hết hạn (TTL) ngắn và hợp lý cho từng loại dữ liệu.<br/>* Thực hiện cơ chế chủ động xóa/ghi đè cache (Cache Eviction) ngay khi database có thay đổi. |
| **JWT Stateless Authentication** | * Tốc độ xác thực cực nhanh.<br/>* Không tốn tài nguyên lưu trữ session trên server. | * Khó thu hồi hoặc vô hiệu hóa token lập tức trước khi nó hết hạn (ví dụ: khi user đổi mật khẩu hoặc logout). | * Sử dụng thời hạn sống của Access Token ngắn (15-30 phút).<br/>* Kết hợp lưu trữ danh sách đen các token bị hủy bỏ (Token Blacklist) trên Redis Cache để tra cứu nhanh. |

---

> [!CAUTION]
> **Ràng buộc kiến trúc cốt lõi:**
> 1. Mỗi microservice tuyệt đối không được truy cập trực tiếp database của microservice khác. Việc chia sẻ dữ liệu chỉ thực hiện qua REST API bảo mật được định tuyến bởi API Gateway hoặc thông qua sự kiện không đồng bộ trên Kafka.
> 2. Mọi kết nối WebSocket đồng bộ sơ đồ ghế phải được quản lý chặt chẽ về số lượng session đồng thời để tránh tình trạng tràn bộ nhớ (Out Of Memory) trên `booking-service`.
