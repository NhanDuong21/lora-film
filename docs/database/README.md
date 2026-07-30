# Tài Liệu Quản Trị Cơ Sở Dữ Liệu (Database Schemas)

Thư mục này lưu trữ toàn bộ các tệp tin cấu trúc mã nguồn SQL (.sql) khởi tạo cơ sở dữ liệu cho hệ thống Đặt Vé Xem Phim Trực Tuyến LoraFilm. Dự án áp dụng mô hình kiến trúc Microservices, do đó việc quản lý dữ liệu tuân thủ nghiêm ngặt nguyên lý Database-per-Service.

## Cấu Trúc Thư Mục Lưu Trữ
* mysql/: Chứa các file mã nguồn SQL khởi tạo bảng, khóa chính, chỉ mục nội bộ cho từng microservice.
    * auth-service-schema.sql: Schema phân hệ xác thực, tài khoản, và phân quyền.
    * user-service-schema.sql: Schema phân hệ quản lý hồ sơ thông tin cá nhân và nhân sự.
    * movie-service-schema.sql: Schema cốt lõi quản lý thông tin phim, phòng chiếu, ghế vật lý, suất chiếu.
    * booking-service-schema.sql: Schema quản lý đơn hàng đặt vé, chi tiết vé, giữ chỗ tạm thời.
    * payment-service-schema.sql: Schema lưu vết giao dịch tài chính, lịch sử đối soát dòng tiền.
    * promotion-service-schema.sql: Schema phân hệ chiến dịch và lịch sử sử dụng mã giảm giá.
    * score-service-schema.sql: Schema tích điểm thưởng và phân hạng thành viên khách hàng.
    * Notification service dùng một file duy nhất tại
      `server/notification-service/deployment/database/notification-schema.sql`;
      database chỉ lưu dữ liệu vận hành, không lưu nội dung template.
    * analytics-service-schema.sql: Kho lưu trữ đệm số liệu tổng hợp doanh thu phục vụ báo cáo Dashboard.

## Nguyên Tắc Thiết Kế Sống Còn (Crucial Principles)
* Quy tắc bắt buộc toàn team: Tuyệt đối không tạo Foreign Key vật lý chéo (Cross-Service). Giữa database của các service khác nhau không được phép tồn tại liên kết ràng buộc REFERENCES vật lý.
* Tham chiếu logic (Logical References): Các trường định danh như user_id, movie_id, booking_id, showtime_id, seat_id chỉ được định nghĩa dưới dạng kiểu dữ liệu số nguyên thuần túy (bigint). Tính toàn vẹn dữ liệu được kiểm soát hoàn toàn ở tầng mã nguồn ứng dụng (Java Spring Boot) hoặc thông qua luồng điều hướng của Kafka Message Queue.
* Tính tự chủ (Service Autonomy): Mỗi service co toàn quyền chỉnh sửa cấu trúc bảng của mình mà không được làm ảnh hưởng hoặc gây lỗi biên dịch cơ sở dữ liệu của service khác.

## Hướng Dẫn Triển Khai Kiểm Thử Local
* Đảm bảo môi trường máy cá nhân đã cài đặt Docker Container và MySQL client.
* Khi khởi chạy hệ thống, các file .sql trong thư mục mysql/ sẽ được nạp tự động vào các container database tương ứng thông qua cấu hình volume trong docker-compose.yml.
