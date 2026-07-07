# Hướng dẫn Kiểm thử Tích hợp (E2E) và Mô tả Kết quả - Movie Analytics APIs

Tài liệu này hướng dẫn chi tiết quy trình kiểm thử tự động và thủ công nhóm API phân tích doanh thu phim (Movie Analytics) thuộc dịch vụ `analytics-service`.

---

## 1. Nội Dung Đã Kiểm Thử (What Was Tested)

*   **Kiểm thử tự động (Automated Tests):** 
    *   Thực thi lệnh kiểm thử thành công: **19 tests run**, **0 failures**, **0 errors**, **0 skipped**.
    *   Bao gồm 12 bài test Unit cho tầng Service và 7 bài test Tích hợp (MockMvc) cho tầng Controller.
*   **Kiểm thử phân quyền (Security Authorization):**
    *   Xác minh quyền truy cập: Chỉ tài khoản có quyền `ROLE_ADMIN` hoặc `ROLE_MANAGER` được phép truy cập.
    *   Chặn tài khoản khách hàng `ROLE_CUSTOMER` (trả lỗi 403 Forbidden) và Anonymous (trả lỗi 401 Unauthorized).
*   **Kiểm thử Swagger/OpenAPI:**
    *   Cấu hình tài liệu Swagger và cơ chế gắn token xác thực Bearer token.

---

## 2. Hướng dẫn chạy Test tự động (Terminal Test)

Trong thư mục gốc của dự án, mở terminal và thực thi lệnh:

```bash
# Chạy bộ test cho analytics-service
mvn clean test -f server/analytics-service/pom.xml
```

**Kết quả mong đợi:**
*   Kết quả build: `BUILD SUCCESS`
*   Đầu ra báo cáo kiểm thử:
    ```txt
    [INFO] Running com.project.analyticsservice.controller.MovieAnalyticsControllerTest
    ...
    [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.78 s -- in com.project.analyticsservice.controller.MovieAnalyticsControllerTest
    [INFO] Running com.project.analyticsservice.service.impl.MovieAnalyticsServiceImplTest
    ...
    [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.419 s -- in com.project.analyticsservice.service.impl.MovieAnalyticsServiceImplTest
    [INFO] 
    [INFO] Results:
    [INFO] 
    [INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    ```

---

## 3. Hướng dẫn Test tay chi tiết bằng Swagger (Manual Testing)

### Bước A: Chuẩn bị Token JWT
Để kiểm thử các API được bảo mật, bạn cần chuẩn bị hai loại Token: Token có quyền quản trị (Admin/Manager) và Token của khách hàng (Customer).

1. Mở Swagger của **`auth-service`** (`http://localhost:8081/swagger-ui.html`).
2. Sử dụng API `POST /api/auth/register` đăng ký hai tài khoản khác nhau, thực hiện xác thực mã OTP qua `POST /api/auth/verify`.
3. Trong MySQL database bảng `user_db.users`, gán vai trò (`role`) cho hai tài khoản:
   * Tài khoản 1: `role = 'ADMIN'` hoặc `role = 'MANAGER'`
   * Tài khoản 2: `role = 'CUSTOMER'`
4. Sử dụng API `POST /api/auth/login` để đăng nhập cho từng tài khoản và lưu lại chuỗi `accessToken` tương ứng.

---

### Bước B: Nạp dữ liệu mô phỏng (Mock Data Seed)
Do dịch vụ phân tích dữ liệu hoạt động độc lập dạng Read Model, hãy chạy tập lệnh SQL sau trong database của bạn (`analytics_db`) để tạo dữ liệu test:

```sql
-- Dọn dẹp bảng dữ liệu cũ
TRUNCATE TABLE analytics_db.movie_revenue_stats;
TRUNCATE TABLE analytics_db.movie_daily_revenue_stats;

-- 1. Nạp dữ liệu Lifetime (movie_revenue_stats)
INSERT INTO analytics_db.movie_revenue_stats (movie_id, movie_title, total_tickets_sold, total_revenue, updated_at)
VALUES 
(101, 'Avengers', 850, 98500000.00, '2026-06-21 21:30:00'),
(102, 'Spider-Man', 500, 55000000.00, '2026-06-21 21:35:00'),
(103, 'Iron Man', 0, 0.00, '2026-06-21 21:40:00'),
(104, 'Inside Out', 10, -50000.00, '2026-06-21 21:45:00'); -- test doanh thu âm do refund

-- 2. Nạp dữ liệu theo ngày (movie_daily_revenue_stats)
INSERT INTO analytics_db.movie_daily_revenue_stats (movie_id, movie_title, stat_date, tickets_sold, revenue, updated_at)
VALUES 
(101, 'Avengers', '2026-06-19', 20, 2300000.00, '2026-06-19 23:59:00'),
(101, 'Avengers', '2026-06-21', 24, 2780000.00, '2026-06-21 21:30:00'),
(102, 'Spider-Man', '2026-06-20', 10, 1100000.00, '2026-06-20 23:59:00'),
(104, 'Inside Out', '2026-06-21', 10, -50000.00, '2026-06-21 21:45:00');
```

---

### Bước C: Thực hiện gọi API trên Swagger

Mở Swagger của **`analytics-service`** (`http://localhost:8089/swagger-ui.html`). Nhấn nút **Authorize** ở góc trên bên phải, nhập `Bearer <AccessToken_Quản_Trị>` đã chuẩn bị ở Bước A, sau đó bấm Authorize.

#### 1. API: `GET /api/analytics/movies` (Xem danh sách thống kê)

*   **Case 1.1: Chế độ Lifetime (Không truyền tham số ngày)**
    *   **Tham số**: Để trống `startDate` và `endDate`.
    *   **Kết quả mong đợi**: HTTP **200 OK**. JSON có dạng:
        ```json
        {
          "success": true,
          "message": "Movie revenue statistics retrieved successfully",
          "data": {
            "mode": "LIFETIME",
            "period": null,
            "content": [
              {
                "movieId": 101,
                "movieTitle": "Avengers",
                "totalTicketsSold": 850,
                "totalRevenue": 98500000.00,
                "currency": "VND",
                "updatedAt": "2026-06-21T21:30:00"
              },
              ...
            ]
          }
        }
        ```
*   **Case 1.2: Chế độ Date Range (Cộng gộp khoảng ngày)**
    *   **Tham số**: `startDate = 2026-06-01`, `endDate = 2026-06-21`
    *   **Kết quả mong đợi**: HTTP **200 OK**. JSON chứa `mode: "DATE_RANGE"`, trường `period` ghi nhận ngày lọc. Các giá trị được cộng dồn từ bảng daily stats.
*   **Case 1.3: Tìm kiếm theo tiêu đề phim (Search case-insensitive)**
    *   **Tham số**: `movieTitle = avengers`
    *   **Kết quả mong đợi**: Danh sách chỉ chứa các phim trùng khớp (không phân biệt hoa thường).
*   **Case 1.4: Lỗi truyền thiếu 1 ngày**
    *   **Tham số**: `startDate = 2026-06-01`, `endDate` để trống.
    *   **Kết quả mong đợi**: HTTP **400 Bad Request**, `errorCode = "ANALYTICS_INVALID_DATE_RANGE"`.
*   **Case 1.5: Lỗi tìm kiếm khoảng ngày quá rộng (> 92 ngày)**
    *   **Tham số**: `startDate = 2026-01-01`, `endDate = 2026-06-01`.
    *   **Kết quả mong đợi**: HTTP **400 Bad Request**, `errorCode = "ANALYTICS_DATE_RANGE_TOO_LARGE"`.

#### 2. API: `GET /api/analytics/movies/{movieId}` (Xem chi tiết)

*   **Case 2.1: Xem chi tiết chế độ Lifetime**
    *   **Đường dẫn**: `/api/analytics/movies/101`
    *   **Kết quả mong đợi**: HTTP **200 OK**, trả về thông tin phim kèm phép chia làm tròn `averageRevenuePerTicket = 115882.35` (98,500,000 / 850).
*   **Case 2.2: Xem chi tiết với số lượng vé bán bằng 0**
    *   **Đường dẫn**: `/api/analytics/movies/103` (Phim Iron Man đã seed 0 vé)
    *   **Kết quả mong đợi**: HTTP **200 OK**, tính toán `averageRevenuePerTicket = 0.00` (Không bị lỗi chia cho 0).
*   **Case 2.3: Xem chi tiết với doanh thu âm (Do refund)**
    *   **Đường dẫn**: `/api/analytics/movies/104` (Phim Inside Out đã seed doanh thu âm)
    *   **Kết quả mong đợi**: HTTP **200 OK**, `averageRevenuePerTicket = -5000.00` (Không bị ép về 0).
*   **Case 2.4: Mã lỗi phim không tồn tại**
    *   **Đường dẫn**: `/api/analytics/movies/999`
    *   **Kết quả mong đợi**: HTTP **404 Not Found**, `errorCode = "ANALYTICS_MOVIE_STATS_NOT_FOUND"`.

#### 3. API: `GET /api/analytics/movies/{movieId}/trend` (Xu hướng doanh thu)

*   **Case 3.1: Xu hướng tự động điền các ngày trống (Happy Case)**
    *   **Tham số**: `/api/analytics/movies/101/trend?startDate=2026-06-19&endDate=2026-06-21&includeEmptyDates=true`
    *   **Kết quả mong đợi**: HTTP **200 OK**. Trả về mảng `statistics` đủ 3 ngày. Trong đó ngày `2026-06-20` không có giao dịch trong DB sẽ tự động điền: `ticketsSold: 0`, `revenue: 0`.
*   **Case 3.2: Xu hướng không điền các ngày trống**
    *   **Tham số**: `/api/analytics/movies/101/trend?startDate=2026-06-19&endDate=2026-06-21&includeEmptyDates=false`
    *   **Kết quả mong đợi**: HTTP **200 OK**. Chỉ trả về 2 phần tử (ngày 19 và 21).

#### 4. API: `GET /api/analytics/movies/top` (Bảng xếp hạng)

*   **Case 4.1: Xếp hạng chế độ Lifetime**
    *   **Tham số**: `metric = REVENUE`, `limit = 5`
    *   **Kết quả mong đợi**: HTTP **200 OK**. Trả về danh sách phim được sắp xếp theo doanh thu giảm dần, gán thứ tự `rank` tăng dần từ 1, 2, 3...
*   **Case 4.2: Xếp hạng chế độ Date Range**
    *   **Tham số**: `metric = TICKETS_SOLD`, `limit = 2`, `startDate = 2026-06-01`, `endDate = 2026-06-21`
    *   **Kết quả mong đợi**: HTTP **200 OK**. Trả về bảng xếp hạng tính toán số vé bán trong thời gian lọc.
*   **Case 4.3: Lỗi truyền limit không hợp lệ**
    *   **Tham số**: `limit = 60` (hoặc `0`)
    *   **Kết quả mong đợi**: HTTP **400 Bad Request**, `errorCode = "ANALYTICS_INVALID_QUERY"`.

#### 5. Kiểm thử phân quyền (Role Restrictions)

*   **Case 5.1: Gọi bằng Token Khách hàng (Customer)**
    *   Nhấn **Authorize** ở góc phải trên Swagger, nhập Token của tài khoản **Customer** đã lấy ở Bước A.
    *   Thực hiện gọi bất kỳ API nào ở trên (ví dụ `GET /api/analytics/movies`).
    *   **Kết quả mong đợi**: HTTP **403 Forbidden**, `errorCode = "FORBIDDEN"`, `message = "Access denied"`.
*   **Case 5.2: Gọi không gửi Token (Anonymous)**
    *   Bấm **Logout** trên Swagger Authorize để xóa token.
    *   Thực hiện gọi bất kỳ API nào.
    *   **Kết quả mong đợi**: HTTP **401 Unauthorized**, `errorCode = "UNAUTHORIZED"`, `message = "Unauthorized access"`.

---

## 4. Kết Quả Nghiệm Thu (Acceptance Sign-off)

| Tiêu Chí Đánh Giá | Kết Quả Đạt Được | Kết Luận |
| :--- | :--- | :--- |
| **Quy chuẩn API** | Các API đầu ra trả về các DTO tách biệt, trường số tiền doanh thu định dạng số (`BigDecimal`), tiền tệ `VND` khớp 100% contract. | **ĐẠT** |
| **Phân trang & Sắp xếp** | Thực hiện phân trang và sắp xếp trực tiếp tại Database bằng native queries trên các cột tính toán (như `totalRevenue`, `totalTicketsSold`). | **ĐẠT** |
| **Deterministic Title** | Sử dụng câu lệnh JOIN với bảng Lifetime để lấy title snapshot mới nhất hoặc fallback sang `MAX` title, không gọi external service. | **ĐẠT** |
| **Swagger UI** | Tài liệu hiển thị đầy đủ, mô tả rõ các tham số và cấu trúc phản hồi lỗi tương ứng với thiết kế. | **ĐẠT** |
| **Security** | Đã cấu hình và test thành công cơ chế chặn phân quyền bằng JWT Token. | **ĐẠT** |
