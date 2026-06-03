# Tài Liệu Sơ Đồ Thực Thể Quan Hệ Vật Lý (Physical ERD)

Thư mục này lưu trữ toàn bộ các sơ đồ thiết kế kiến trúc thực thể quan hệ mức vật lý (Physical Entity-Relationship Diagram) phục vụ công tác trực quan hóa luồng dữ liệu và hỗ trợ giải trình kiến trúc hệ thống LoraFilm với Mentor.

## 📂 Cấu Trúc Phân Vị
* `physical/`: Thư mục chứa ảnh sơ đồ ERD độc lập dạng định dạng chất lượng cao (`.png`) được kết xuất (export) trực tiếp từ công cụ thiết kế **dbdiagram.io**.

## 🗺️ Bản Đồ Theo Dõi Danh Mục Sơ Đồ (ERD Mapping)

Dưới đây là danh sách quản lý và phân cấp các tệp tin sơ đồ tương ứng với từng phân hệ dịch vụ độc lập:

| Tên Dịch Vụ (Service) | Tệp Tin Sơ Đồ PNG (`docs/erd/physical/`) | Công Cụ Thiết Kế Gốc | Trạng Thái Thiết Kế |
| :--- | :--- | :--- | :--- |
| **1. Auth Service** | `auth-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **2. User Service** | `user-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **3. Movie Service** | `movie-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **4. Booking Service** | `booking-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **5. Payment Service** | `payment-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **6. Promotion Service** | `promotion-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **7. Score Service** | `score-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **8. Notification Service**| `notification-service-physical-erd.png` | dbdiagram.io | Hoàn thành |
| **9. Analytics Service** | `analytics-service-physical-erd.png` | dbdiagram.io | Hoàn thành |

## 🛠️ Quy Định Cập Nhật Và Chỉnh Sửa Sơ Đồ
1. **Không dùng một bản ERD tập trung độc nhất:** Do hệ thống chạy theo mô hình cơ sở dữ liệu phân tán microservices, mỗi service bắt buộc phải giữ một sơ đồ ERD riêng biệt.
2. **Quy trình cập nhật:** Khi phát sinh yêu cầu thay đổi nghiệp vụ cần chỉnh sửa bảng:
    * Bước 1: Thành viên phụ trách truy cập công cụ **dbdiagram.io**, cập nhật mã cấu trúc DBML tương ứng.
    * Bước 2: Thực hiện **Export $\rightarrow$ Export to PNG** và lưu đè file ảnh mới vào thư mục `physical/`.
    * Bước 3: Thực hiện **Export $\rightarrow$ Export to MySQL** để sinh file mã SQL mới lưu đè vào thư mục `docs/database/mysql/` tương ứng.
    * Bước 4: Tạo Merge Request để Leader Thành rà soát kiểm tra logic.
