# Movie Service

Chào mừng các bạn đến với **Movie Service** - một trong những microservice cốt lõi của dự án **Lorafilm**. Service này chịu trách nhiệm quản lý thông tin liên quan đến phim, rạp chiếu (cinema), phòng chiếu (auditorium), ghế ngồi (seat), suất chiếu (showtime), và giá vé (pricing).

---

## 🛠 Hướng Dẫn Cài Đặt (Local Development)

Để chạy service này ở dưới local, các bạn làm theo các bước sau nhé:

1. **Copy cấu hình mẫu:**
   Service này đã được cung cấp sẵn một file cấu hình mẫu là `application.example.properties`. Hãy copy file này thành `application.properties`:
   
   ```bash
   cp src/main/resources/application.example.properties src/main/resources/application.properties
   ```

2. **Cấu hình môi trường của bạn:**
   Mở file `application.properties` vừa tạo ra và thay đổi các giá trị cấu hình (đặc biệt là mật khẩu database `spring.datasource.password`) sao cho phù hợp với môi trường local của bạn.
   *(Lưu ý: File `application.properties` đã được đưa vào `.gitignore` nên các bạn yên tâm sửa mà không lo bị push nhầm lên git).*

3. **Chạy dự án:**
   Sau khi đã config xong Database và đảm bảo MySQL/Eureka đang chạy, bạn có thể khởi động project thông qua IDE (IntelliJ, Eclipse, VSCode) hoặc bằng Maven:
   ```bash
   mvn spring-boot:run
   ```

---

## 🏗 Kiến Trúc Thư Mục (DDD Lite)

Service này áp dụng kiến trúc **Package by Feature / Domain-Driven Design (DDD) Lite**. Thay vì chia theo Layer (tất cả controller nằm chung 1 chỗ, tất cả service nằm chung 1 chỗ), chúng ta sẽ chia code theo **Tính Năng (Feature)**.

Mỗi feature/domain (như `movie`, `cinema`, `showtime`...) sẽ là một package riêng biệt. Bên trong mỗi package đó sẽ có cấu trúc chuẩn như sau:

- 📂 `domain`: Chứa các Entity (Mapped với Database), Value Objects, hoặc Domain interfaces.
- 📂 `repository`: Chứa các interface kế thừa từ `JpaRepository` để giao tiếp với DB.
- 📂 `service`: Chứa các interface Business Logic và file implementation (vd: `MovieServiceImpl`).
- 📂 `dto`: Chứa các object truyền tải dữ liệu (Request, Response) cho riêng feature đó.
- 📂 `controller`: Chứa REST API Endpoints phục vụ cho phía client hoặc các service khác gọi tới.

**Lợi ích:** Khi bạn cần sửa một tính năng, bạn chỉ cần vào đúng thư mục của tính năng đó, tất cả mọi thứ liên quan đều nằm sẵn ở đó.

---

##  Package Common (Tiện ích dùng chung)

Để code được đồng bộ, gọn gàng và không lặp lại, team đã setup sẵn package `common`. Vui lòng **bắt buộc** sử dụng các class này trong quá trình dev:

1. **Chuẩn hoá API Response (`common/api/ApiResponse.java`):**
   Mọi API trả về đều phải được bọc trong `ApiResponse.ok(data)` hoặc `ApiResponse.error(message)`. Điều này giúp client luôn nhận được một format chuẩn `{"success": true, "message": "...", "data": ...}`.
   
2. **Phân trang (`common/api/PageResponse.java`):**
   Sử dụng khi cần trả về danh sách có phân trang.

3. **Xử lý Ngoại Lệ (Exception Handling):**
   - Đừng throw `RuntimeException` hay `Exception` chung chung.
   - Hãy dùng `throw new BusinessException(ErrorCode.YOUR_ERROR_CODE)`.
   - `GlobalExceptionHandler` sẽ tự động bắt lấy lỗi này và trả về mã lỗi thích hợp cùng với HTTP Status tương ứng cho Client.

4. **Auditable Entity (`common/audit/BaseAuditableEntity.java`):**
   Mọi Entity muốn tự động lưu vết ngày tạo (`created_at`) và ngày cập nhật (`updated_at`) chỉ cần kế thừa (extend) class `BaseAuditableEntity`. Code đã được tích hợp sẵn JPA Auditing (`@PrePersist`, `@PreUpdate`).

---

## 🗄 Quản lý Cơ Sở Dữ Liệu (Database)

**LƯU Ý QUAN TRỌNG:**
- Service này **KHÔNG** tự quản lý schema migration (Flyway/Liquibase) bên trong folder của nó.
- Toàn bộ script tạo bảng, sửa bảng (Migrations) đều được quản lý tập trung ở thư mục gốc của workspace (ví dụ: `database/mysql/`).
- Các bạn chỉ viết code Java Entity ở đây, còn lúc tạo bảng thực tế thì nhớ báo hoặc tạo pull request ở thư mục database chung nhé!

---

