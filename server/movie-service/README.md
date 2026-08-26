# Movie Service

Chào mừng các bạn đến với Movie Service - một trong những microservice cốt lõi của hệ thống. Service này chịu trách nhiệm quản lý các thông tin liên quan đến phim, rạp chiếu (cinema), phòng chiếu (auditorium), ghế ngồi (seat), suất chiếu (showtime), và giá vé (pricing).

---

## 1. Hướng Dẫn Cài Đặt (Local Development Setup)

Để chạy service này ở môi trường local, các bạn vui lòng làm theo các bước sau:

### Bước 1: Copy cấu hình mẫu
Service này đã được cung cấp sẵn một file cấu hình mẫu. Hãy copy file này thành file cấu hình chính thức:

```bash
cp src/main/resources/application.example.properties src/main/resources/application.properties
```

### Bước 2: Cấu hình môi trường
Mở file `application.properties` vừa tạo ra và thay đổi các giá trị cấu hình (đặc biệt là thông tin kết nối database) sao cho phù hợp với môi trường local của bạn. 
*Lưu ý: File này đã được đưa vào .gitignore nên bạn có thể yên tâm chỉnh sửa mà không sợ vô tình đẩy lên repository chung.*

### Bước 3: Khởi động dịch vụ
Sau khi cấu hình Database xong và đảm bảo các dịch vụ nền (như Eureka) đang chạy, bạn có thể khởi động dự án thông qua IDE (IntelliJ IDEA, Eclipse, VSCode) hoặc thông qua Maven:

```bash
mvn spring-boot:run
```

---

## 2. Kiến Trúc Thư Mục (DDD Lite)

Service này áp dụng kiến trúc **Package by Feature / Domain-Driven Design (DDD) Lite**. Thay vì chia theo Layer truyền thống (gom tất cả controllers vào chung một chỗ), chúng ta sẽ chia code theo từng Tính Năng (Feature).

Cấu trúc tổng quan:
```text
movie-service/src/main/java/com/lorafilm/movie/
+-- auditorium/  # Quản lý phòng chiếu, màn hình và âm thanh
+-- cinema/      # Quản lý hệ thống rạp chiếu
+-- common/      # Các tiện ích và class cấu hình dùng chung
+-- movie/       # Quản lý thông tin chi tiết của phim
+-- pricing/     # Quản lý giá vé
+-- seat/        # Quản lý ghế ngồi
+-- showtime/    # Quản lý lịch và suất chiếu
```

Bên trong mỗi package feature (ngoại trừ common), mã nguồn sẽ được chia thành cấu trúc chuẩn như sau:
- `domain`: Chứa các Entity (Ánh xạ với Database), các Enum trạng thái, Value Objects, hoặc Domain interfaces. (Team đã khởi tạo sẵn toàn bộ các Enum cần thiết dựa trên thiết kế DB).
- `repository`: Chứa các interface kế thừa từ Spring Data JpaRepository để giao tiếp với CSDL.
- `service`: Chứa các interface mô tả nghiệp vụ (Business Logic) và các file implementation tương ứng.
- `dto`: Chứa các đối tượng truyền tải dữ liệu (Request, Response Payload).
- `controller`: Chứa các REST API Endpoints.

**Quy tắc cốt lõi**: Khi bạn được phân công làm một tính năng, chỉ cần tạo các class vào đúng thư mục của tính năng đó. Mọi thứ liên quan đến tính năng đó phải được đóng gói gọn gàng cùng với nhau.

---

## 3. Package Common (Tiện ích dùng chung)

Để code được đồng bộ, rõ ràng và tránh lặp lại, team đã thiết lập sẵn package `common`. Vui lòng BẮT BUỘC tuân thủ và sử dụng các class này:

- **Chuẩn hóa API Response (`common.api.ApiResponse`)**: Mọi API trả về đều phải được bọc trong `ApiResponse.ok(data)` hoặc `ApiResponse.error(message)`.
- **Phân trang (`common.api.PageResponse`)**: Dùng làm wrapper khi cần trả về một danh sách có hỗ trợ phân trang.
- **Xử lý Ngoại Lệ (`common.exception.BusinessException`)**: Thay vì throw lỗi chung chung, hãy sử dụng `throw new BusinessException(ErrorCode.YOUR_ERROR_CODE)`. `GlobalExceptionHandler` sẽ tự động bắt lỗi và format JSON chuẩn trả về cho Client. Danh sách `ErrorCode` đã được định nghĩa sẵn vô cùng chi tiết.
- **Auditable Entity (`common.audit.BaseAuditableEntity`)**: Mọi Entity mới tạo ra cần kế thừa (extend) class này để được tự động quản lý vòng đời dữ liệu: thời gian tạo (createdAt), thời gian cập nhật (updatedAt), và phục vụ cho soft-delete (deletedAt). Các thuộc tính trạng thái phổ biến cũng đã được cung cấp sẵn tại `common/enums/`.

---

## 4. Quản Lý Cơ Sở Dữ Liệu (Database)

- Service này **KHÔNG** dùng Flyway/Liquibase hoặc migration lịch sử.
- Database mới được dựng từ canonical schema tập trung tại `docs/database/mysql/movie-service-schema.sql`.
- Mọi thay đổi Entity phải được phản ánh trực tiếp vào canonical schema trong cùng thay đổi mã nguồn.

Chúc các bạn phát triển mã nguồn hiệu quả và chất lượng!
