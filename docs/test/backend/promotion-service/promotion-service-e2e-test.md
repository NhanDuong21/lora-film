# BÁO CÁO KIỂM THỬ E2E (END-TO-END TEST FLOW)

## 1. Thông tin chung
- **Service**: `promotion-service`
- **Tính năng**: Promotion Query, Validation and Preview
- **Mục tiêu**: Kiểm thử các luồng truy vấn thông tin chương trình khuyến mãi (Public), xác thực mã giảm giá (validate) và xem trước mức giảm giá (preview) liên kết với dữ liệu booking thực tế từ `booking-service`.

---

## Hướng dẫn chung: Chuẩn bị kiểm thử (Prerequisites)

Các API xác thực mã (`validate`) và xem trước (`preview`) là các endpoint được bảo vệ (protected) và yêu cầu thông tin của khách hàng (user). 

### Điều kiện chuẩn bị:
1. **Dịch vụ đang chạy**:
   - `booking-service` chạy tại cổng `8083`.
   - `promotion-service` chạy tại cổng `8087`.
2. **Database & Seed Data**:
   - Chiến dịch khuyến mãi (`promotion_campaigns`) đang ở trạng thái kích hoạt (`is_active = true`), trong khoảng thời gian hiệu lực (`start_date <= now <= end_date`).
   - Mã khuyến mãi (`promotions`) tương ứng đang kích hoạt (`is_active = true`), còn lượt dùng (`used_count < usage_limit`), trong thời gian hiệu lực.
   - Có một Booking hợp lệ trong `booking-service` ở trạng thái `PENDING_PAYMENT`.
3. **Mã xác thực (JWT Token)**:
   - Đăng nhập qua `auth-service` hoặc sử dụng một token hợp lệ của User (khách hàng sở hữu Booking trên).
   - Thiết lập header `Authorization` trên Swagger UI hoặc Postman với giá trị: `Bearer <jwt-token>`.

---

## 2. Các Luồng Kiểm Thử (Test Flows)

### Flow 1: Truy vấn danh sách khuyến mãi đang hoạt động (Query Active Promotions)
- **API**: `GET /api/promotions/active`
- **Ngữ cảnh**: Người dùng xem danh sách các mã giảm giá khả dụng trên giao diện để lựa chọn.
- **Headers**:
  - `Content-Type`: `application/json`
- **Query Parameters (Tùy chọn)**:
  - `discountType`: Lọc theo loại hình giảm giá (`PERCENTAGE` hoặc `FIXED_AMOUNT`).
  - `minOrderAmount`: Lọc các khuyến mãi có điều kiện giá trị đơn hàng tối thiểu nhỏ hơn hoặc bằng số tiền này (ví dụ: `150000`).
  - `page`, `size`, `sort`: Tham số phân trang và sắp xếp (mặc định: `sort=createdAt,desc`).
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK` kèm danh sách khuyến mãi hợp lệ.
    - Danh sách chỉ chứa các chương trình khuyến mãi đang hoạt động (cả Promotion và Campaign đều `is_active = true`, thời gian hiện tại nằm trong khoảng hiệu lực, và số lượng đã dùng chưa vượt quá giới hạn).
    - Các thông tin cấu hình nội bộ admin hoặc thống kê (như `usedCount`, `usageLimit`, `status`, `createdAt`, `updatedAt`) **không được hiển thị** (bị ẩn hoặc trả về `null` nhờ cấu hình loại trừ trường null).
  - **Negative Case (Lỗi tham số sắp xếp)**: Gửi tham số `sort=invalidField,desc` -> API trả về `400 Bad Request` với mã lỗi `PROMOTION_INVALID_SORT`.

---

### Flow 2: Xem chi tiết chương trình khuyến mãi (Promotion Detail)
- **API**: `GET /api/promotions/{promotionId}`
- **Ngữ cảnh**: Khách hàng nhấn xem chi tiết điều kiện áp dụng của một mã cụ thể.
- **Headers**:
  - `Content-Type`: `application/json`
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK` chứa thông tin chi tiết của khuyến mãi bao gồm cả tên chiến dịch (`campaignName`) và giới hạn số lần sử dụng trên mỗi tài khoản (`limitPerUser`).
  - **Negative Case (Không tìm thấy mã)**: Truyền `promotionId = 99999` (không tồn tại) -> API trả về `404 Not Found` với mã lỗi `PROMOTION_NOT_FOUND`.

---

### Flow 3: Xác thực mã giảm giá (Validate Promotion)
- **API**: `POST /api/promotions/validate`
- **Ngữ cảnh**: Người dùng nhập mã giảm giá tại màn hình thanh toán và bấm áp dụng.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
  - `Content-Type`: `application/json`
- **Request Body (Happy Case)**:
  ```json
  {
    "promotionCode": "LORAFILM2026",
    "bookingId": 1001
  }
  ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK` với dữ liệu xác thực:
      - `valid`: `true`
      - `discountAmount`: Số tiền giảm giá được tính chính xác (làm tròn số nguyên bằng `HALF_UP`).
      - `finalAmount`: Số tiền còn lại sau khi giảm (`originalAmount - discountAmount`).
      - `expiresAt`: Hạn thanh toán của Booking.
  - **Negative Case 1 (Mã đã vô hiệu hóa)**: Dùng mã có `is_active = false` -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_DISABLED`).
  - **Negative Case 2 (Mã chưa bắt đầu)**: Dùng mã có `startDate` trong tương lai -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_NOT_STARTED` kèm payload chứa `startDate` trong `"data"`).
  - **Negative Case 3 (Mã hết hạn)**: Dùng mã có `endDate` trong quá khứ -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_EXPIRED`).
  - **Negative Case 4 (Đơn hàng không đạt giá trị tối thiểu)**: Booking có số tiền nhỏ hơn `minOrderAmount` -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_MINIMUM_AMOUNT_NOT_MET` kèm payload chứa `minimumAmount` và `currentAmount` trong `"data"` giỏ hàng).
  - **Negative Case 5 (Vượt giới hạn lượt dùng của tài khoản)**: User đã sử dụng mã này đạt giới hạn `limitPerUser` từ trước -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_USER_LIMIT_REACHED`).
  - **Negative Case 6 (Booking đã được áp dụng mã)**: Booking đã áp dụng một mã khuyến mãi khác đang hoạt động -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_BOOKING_ALREADY_APPLIED`).
  - **Negative Case 7 (Trạng thái Booking không hợp lệ)**: Booking không ở trạng thái `PENDING_PAYMENT` (ví dụ đã được confirm hoặc cancel) -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_BOOKING_NOT_ELIGIBLE`).

---

### Flow 4: Xem trước mức giảm giá (Preview Discount)
- **API**: `POST /api/promotions/preview`
- **Ngữ cảnh**: Hệ thống tự động tính toán số tiền được giảm giá của từng mã khả dụng hiển thị lên UI để gợi ý cho khách hàng chọn.
- **Headers**:
  - `Authorization`: `Bearer <user-jwt-token>`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "PERCENT10",
    "bookingId": 1001
  }
  ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - API trả về `200 OK` chứa cấu trúc dữ liệu tối giản:
      - `previewOnly`: `true`
      - `currency`: `"VND"`
      - `discountAmount` và `finalAmount` đã tính toán.
      - **Không trả về** các trường cấu hình nâng cao như `valid`, `discountType`, `expiresAt`.
  - **Negative Case**: Áp dụng đầy đủ toàn bộ các trường hợp kiểm tra ràng buộc như Flow 3 và trả về đúng HttpStatus cùng mã lỗi tương ứng.

---

### Flow 5: Kiểm tra an toàn và bảo mật (Security Tests)
- **Mục tiêu**: Đảm bảo các API công khai và bảo mật hoạt động đúng quyền hạn được cấu hình.
- **Các kịch bản kiểm thử**:
  1. **Truy cập công khai (Public)**: Không truyền header `Authorization` khi gọi `GET /api/promotions/active` và `GET /api/promotions/{promotionId}` -> Trả về `200 OK`.
  2. **Truy cập không có token (Anonymous)**: Không truyền token khi gọi `POST /api/promotions/validate` hoặc `POST /api/promotions/preview` -> Trả về `401 Unauthorized`.
  3. **Token hết hạn / Không hợp lệ**: Gửi JWT token không hợp lệ hoặc đã hết hạn -> Trả về `401 Unauthorized`.

---

### Flow 6: Kiểm thử tác dụng phụ (Read-only / Side-effect Verification)
- **Mục tiêu**: Đảm bảo các API `validate` và `preview` là các luồng kiểm tra nghiệp vụ thuần túy, tuyệt đối không được ghi nhận tác dụng phụ thay đổi cơ sở dữ liệu.
- **Cách thực hiện**:
  1. Ghi nhận giá trị lượt sử dụng `usedCount` của mã `LORAFILM2026` và tổng số bản ghi trong bảng `promotion_usages`.
  2. Gọi liên tục các API `validate` và `preview`.
  3. Kiểm tra lại dữ liệu trong Database.
- **Kết quả mong đợi**:
  - Giá trị `usedCount` của chương trình khuyến mãi **không đổi**.
  - Không có bất kỳ bản ghi mới nào được sinh ra trong bảng `promotion_usages`.
  - Thông tin của đơn hàng trong `booking-service` không bị thay đổi.

---

### Flow 7: Kiểm thử tính đồng thời (Concurrency Testing)
- **Mục tiêu**: Đảm bảo khi có nhiều request đồng thời gọi kiểm tra hoặc xem trước cho cùng một mã hoặc cùng một booking, hệ thống không xảy ra xung đột khóa dữ liệu.
- **Cách thực hiện**:
  - Sử dụng JMeter hoặc Apache Bench gửi đồng thời 100 luồng request gọi `POST /api/promotions/validate` và `POST /api/promotions/preview` cho cùng một mã `LORAFILM2026` và cùng `bookingId`.
- **Kết quả mong đợi**:
  - 100% các request đều trả về kết quả thành công mà không gặp lỗi nghẽn khóa dữ liệu (Deadlock) hay tranh chấp khóa lạc quan (Optimistic Locking Exception) vì các API này hoàn toàn chỉ đọc (Read-only) và không giữ khóa độc quyền trên Database.

---

### Flow 8: Xử lý lỗi từ dịch vụ ngoài (Inter-service Failure Handling)
- **Mục tiêu**: Đảm bảo khi `booking-service` xảy ra sự cố hoặc trả về lỗi, `promotion-service` vẫn bắt và chuyển đổi mã lỗi chính xác theo tài liệu đặc tả API.
- **Các trường hợp kiểm thử**:
  1. **Không tìm thấy Booking (404)**: Khi `booking-service` phản hồi `404 Not Found` -> Trả về `404 Not Found` (Mã lỗi `PROMOTION_BOOKING_NOT_FOUND`).
  2. **Sai quyền sở hữu Booking (403)**: Khi khách hàng A cố tình lấy mã áp cho booking của khách hàng B, `booking-service` trả về `403 Forbidden` -> Trả về `403 Forbidden` (Mã lỗi `PROMOTION_BOOKING_OWNERSHIP_MISMATCH`).
  3. **Lỗi hệ thống hoặc sập kết nối (500 / Timeout)**: Ngắt kết nối mạng hoặc tắt `booking-service` -> Trả về `503 Service Unavailable` (Mã lỗi `BOOKING_SERVICE_UNAVAILABLE`).

---

## 3. Đánh giá Hiệu năng & Bảo mật (Performance & Security)

### Hiệu năng (Performance)
*   **Tránh truy vấn N+1**: Tất cả các thông tin khuyến mãi được nạp dữ liệu một lần bằng cách Join bảng Campaign trong một câu truy vấn duy nhất.
*   **Phân trang hiệu quả**: API active danh sách bắt buộc giới hạn kích thước phân trang trong khoảng `[1, 50]` để ngăn chặn việc tải dữ liệu quá lớn gây nghẽn bộ nhớ đệm.
*   **Định chỉ mục (Indexes)**: Tận dụng các trường đã đánh chỉ mục trong cơ sở dữ liệu (`promotion_code`, `campaign_id`, `booking_id`) để tối ưu hóa thời gian phản hồi dưới 100ms.

### Bảo mật (Security)
*   **Kiểm soát dữ liệu đầu ra**: Các thông tin vận hành nhạy cảm hoặc lịch sử hệ thống (`usedCount`, `usageLimit`) được ẩn đi đối với API khách hàng sử dụng để ngăn chặn rò rỉ dữ liệu.
*   **Danh sách trắng sắp xếp (Sort Whitelist)**: Giới hạn các cột được phép sắp xếp (`id`, `promotionCode`, `startDate`, `endDate`, `createdAt`, `updatedAt`). Các chuỗi độc hại cố tình tiêm nhiễm SQL Injection qua tham số sắp xếp sẽ bị chặn và loại bỏ ngay lập tức ở tầng kiểm soát đầu vào.
*   **Không lộ thông tin lỗi nội bộ**: Mọi ngoại lệ chưa được xử lý sẽ được bắt tại `GlobalExceptionHandler` và trả về mã lỗi chung `INTERNAL_SERVER_ERROR` thay vì hiển thị Stack Trace chi tiết ra Client.

---

## 4. Tổng hợp kết quả tự động (Automated Test Metrics)
- **Môi trường chạy**: Local Integration Tests với cơ sở dữ liệu MySQL và Spring Boot MockMvc.
- **Lệnh thực thi**: `mvn clean test`
- **Tổng số Test Cases**: 70 (bao gồm cả Unit và Integration Tests)
  - Các test case quản trị (Admin management): 57 tests
  - Các test case nghiệp vụ Khách hàng (Query, Validate, Preview & Security): 13 tests
- **Tỉ lệ Pass**: 100% (0 Failures, 0 Errors, 0 Skipped).

---

## 5. Danh sách kiểm tra cuối cùng (Final Verification Checklist)
- [x] Tuân thủ thiết kế đặc tả API Contract.
- [x] Đầy đủ các kiểm tra ràng buộc thời gian hiệu lực chương trình.
- [x] Tính toán số tiền giảm giá và số tiền cuối cùng làm tròn `HALF_UP` chính xác.
- [x] Kiểm soát đầy đủ giới hạn lượt dùng toàn hệ thống và giới hạn mỗi khách hàng.
- [x] Xác thực thành công trạng thái Booking và quyền sở hữu với `booking-service`.
- [x] Xử lý tốt các tình huống ngắt kết nối hoặc lỗi dịch vụ liên kết.
- [x] Bảo mật thông tin đầu ra, chặn tiêm nhiễm SQL Injection qua phân trang.
- [x] Đảm bảo tính chất chỉ đọc (Read-only) và an toàn đồng thời cho các API kiểm tra.
