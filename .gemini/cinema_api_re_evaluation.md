# Báo Cáo Tái Đánh Giá Toàn Diện Hệ Thống Cinema API (Sau Phase 2)

Tài liệu này thực hiện đánh giá lại toàn bộ các API trong phạm vi Cinema (Rạp chiếu phim) dựa trên trạng thái mã nguồn hiện tại của dự án. Nội dung phân tích tập trung vào sự hợp lý của các endpoints, dữ liệu đầu vào (DTO fields & validation), phân trang và bộ lọc của các API lấy dữ liệu (GET).

---

## 1. Đánh Giá Phạm Vi API (Dư thừa vs Thiếu sót)

### 1.1. Các API Dư Thừa (Redundant Endpoints)
Sau khi bổ sung các API quản trị và lịch đóng cửa rạp ở Phase 2, hai API public sau vẫn đang tồn tại:
1.  `GET /api/cinemas/{cinemaPublicId}/media`
2.  `GET /api/cinemas/{cinemaPublicId}/operating-hours`

> [!NOTE]
> **Đánh giá tính cần thiết:**
> Hai API này vẫn bị xem là **dư thừa** trong hầu hết các kịch bản thực tế vì API chi tiết rạp [GET /api/cinemas/{cinemaIdOrSlug}](file:///h:/Code%20Java/OJT_saleTicket/hcm26_cpl_java_05_group3/server/movie-service/src/main/java/com/lorafilm/movie/cinema/controller/CinemaController.java#L32) trả về đối tượng [CinemaDetailDto](file:///h:/Code%20Java/OJT_saleTicket/hcm26_cpl_java_05_group3/server/movie-service/src/main/java/com/lorafilm/movie/cinema/dto/CinemaDetailDto.java) đã bao gồm đầy đủ danh sách giờ mở cửa (`operatingHours`) và bộ sưu tập ảnh/video (`gallery`).
>
> **Khuyến nghị:** Nên gỡ bỏ hoặc đánh dấu `@Deprecated` để tránh client thực hiện nhiều HTTP request thừa.

### 1.2. Các API Còn Thiếu (Missing Endpoints)
Mặc dù các API quản trị cơ bản (Search, Detail, Soft Delete, Media Delete, Closure list) đã được tích hợp đầy đủ, hệ thống vẫn thiếu các API hỗ trợ nâng cao sau:
1.  **`PUT /api/admin/closure-periods/{closurePeriodId}` (Cập nhật lịch đóng cửa):**
    *   *Mô tả:* Hiện tại Admin chỉ có thể tạo hoặc hủy lịch đóng cửa. Nếu muốn thay đổi thời gian đóng rạp hoặc chỉnh sửa lý do đóng rạp, Admin bắt buộc phải hủy lịch cũ và tạo lại lịch mới.
    *   *Độ ưu tiên:* Trung bình.
2.  **`PUT /api/admin/cinemas/{cinemaPublicId}/operating-hours/{dayOfWeek}` (Chỉnh sửa giờ mở cửa đơn lẻ):**
    *   *Mô tả:* Hiện tại API [PUT /api/admin/cinemas/{cinemaPublicId}/operating-hours](file:///h:/Code%20Java/OJT_saleTicket/hcm26_cpl_java_05_group3/server/movie-service/src/main/java/com/lorafilm/movie/cinema/controller/AdminCinemaController.java#L73) yêu cầu gửi lên chính xác danh sách 7 ngày hoạt động của tuần. Hệ thống chưa hỗ trợ cập nhật nhanh giờ mở cửa của duy nhất một ngày cụ thể (ví dụ: chỉ đổi giờ mở cửa Chủ Nhật).
    *   *Độ ưu tiên:* Thấp.

---

## 2. Đánh Giá Các Trường Dữ Liệu Đầu Vào (Input Fields & Validation)

Dưới đây là bảng đánh giá chi tiết tính hợp lý của các trường dữ liệu truyền vào trong các Request DTO hiện tại:

| Request DTO | Tên trường dữ liệu | Kiểu dữ liệu | Trạng thái Validation | Đánh giá & Rủi ro | Giải pháp đề xuất |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **`CreateCinemaRequest`** & **`UpdateCinemaRequest`** | `latitude` & `longitude` | `BigDecimal` | ❌ Không có | Client có thể truyền giá trị tọa độ không có thực (ngoài dải `[-90, 90]` đối với Latitude và `[-180, 180]` đối với Longitude). | Thêm các annotation: `@DecimalMin("-90.0")`, `@DecimalMax("90.0")` cho `latitude` và tương tự cho `longitude`. |
| | `district` | `String` | ⚠️ `@NotBlank` | DTO bắt buộc nhập quận/huyện nhưng trong DB entity cột `district` cho phép `NULL`. Việc này gây mâu thuẫn giữa DB schema và API. | Nếu `district` là bắt buộc: Thêm thuộc tính `nullable = false` vào [Cinema entity](file:///h:/Code%20Java/OJT_saleTicket/hcm26_cpl_java_05_group3/server/movie-service/src/main/java/com/lorafilm/movie/cinema/domain/entity/Cinema.java#L32). Nếu không bắt buộc: Bỏ `@NotBlank` trong DTO. |
| | `name` | `String` | `@NotBlank` | Service sinh slug trực tiếp từ `name`. Nếu rạp trùng tên (ví dụ: "CGV Vincom" ở nhiều tỉnh thành), hệ thống sẽ ném lỗi validate trùng slug. | Nên sinh slug kết hợp giữa tên rạp và tỉnh/quận (ví dụ: `cgv-vincom-quan-1`) để đảm bảo tính duy nhất. |
| **`CreateCinemaMediaRequest`** & **`UpdateCinemaMediaRequest`** | `url` | `String` | ⚠️ `@NotBlank` | Không kiểm định định dạng. Client có thể gửi văn bản thông thường thay vì một liên kết tài nguyên ảnh/video hợp lệ. | Bổ sung annotation `@URL` (Hibernate Validator) để ràng buộc định dạng đường dẫn. |
| | `displayOrder` | `Integer` | ❌ Không có | Có thể truyền số âm (`-1`, `-10`), gây sai lệch thuật toán sắp xếp thứ tự hiển thị của thư viện ảnh rạp. | Thêm `@Min(value = 0, message = "Display order cannot be negative")`. |
| **`CreateCinemaClosurePeriodRequest`** | `reason` | `String` | ❌ Không có | Độ dài tối đa trong database là `VARCHAR(255)`. Nếu Admin nhập lý do quá dài (ví dụ: mô tả chi tiết bảo trì), hệ thống sẽ trả về lỗi DB `DataIntegrityViolationException`. | Bổ sung `@Size(max = 255, message = "Reason cannot exceed 255 characters")`. |

---

## 3. Đánh Giá Các API GET (Pagination & Filtering)

### 3.1. API Lấy Danh Sách Rạp Công Khai: `GET /api/cinemas`
*   **Phân trang (`page`, `size`):** Đã đầy đủ thông qua đối tượng `PageResponse`.
*   **Bộ lọc (`city`, `district`, `keyword`):** Đầy đủ và cần thiết cho khách hàng tìm kiếm rạp.
*   **Đánh giá & Thiếu sót:**
    1.  **Thiếu cơ chế Sắp xếp (Sorting):** Hiện tại [getCinemas](file:///h:/Code%20Java/OJT_saleTicket/hcm26_cpl_java_05_group3/server/movie-service/src/main/java/com/lorafilm/movie/cinema/service/CinemaServiceImpl.java#L74) gọi JPA không kèm tham số `Sort`, dẫn đến kết quả trả về không ổn định. Nên bổ sung sắp xếp mặc định theo tên rạp tăng dần (`name ASC`) hoặc khoảng cách địa lý.
    2.  **Thiếu bộ lọc Khoảng cách (Geo-distance search):** Vì hệ thống lưu kinh độ và vĩ độ của rạp, kịch bản người dùng tìm các rạp gần vị trí hiện tại của mình là rất phổ biến. Nên bổ sung lọc theo `latitude`, `longitude` và `radiusInKm`.

### 3.2. API Lấy Danh Sách Rạp Cho Admin: `GET /api/admin/cinemas`
*   **Phân trang & Bộ lọc:** Đã được thiết kế tối ưu ở Phase 2 (bao gồm lọc trạng thái, thành phố, từ khóa và bộ lọc xóa mềm `showDeleted`).
*   **Sắp xếp:** Đã hỗ trợ phân tích tham số `sort` linh hoạt, mặc định sắp xếp theo `createdAt DESC` giúp hiển thị rạp mới tạo lên đầu trang.
*   **Đánh giá:** Rất đầy đủ và hợp lý cho nghiệp vụ admin.

### 3.3. API Danh Sách Lịch Đóng Cửa Cho Admin: `GET /api/admin/cinemas/{cinemaPublicId}/closure-periods`
*   **Phân trang & Bộ lọc:** Đã có phân trang, hỗ trợ bộ lọc trạng thái (`ACTIVE`, `CANCELLED`) và lọc lịch tương lai (`upcomingOnly`).
*   **Sắp xếp:** Mặc định sắp xếp tăng dần theo thời gian bắt đầu (`startTime ASC`) giúp quản lý lịch sắp diễn ra dễ dàng.
*   **Đánh giá:** Rất đầy đủ và cần thiết.

### 3.4. API Danh Sách Lịch Đóng Cửa Cho Khách Hàng: `GET /api/cinemas/{cinemaPublicId}/closure-periods`
*   **Phân trang & Bộ lọc:** Không cần phân trang vì số lượng lịch đóng cửa đang/sắp diễn ra của một rạp tại một thời điểm thường rất ít (dưới 5 bản ghi). Chỉ lọc duy nhất các đợt đóng cửa có hiệu lực ở tương lai.
*   **Đánh giá:** Phù hợp và cần thiết để hiển thị cảnh báo cho khách hàng trước khi đặt vé.
