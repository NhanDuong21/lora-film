# BÁO CÁO KIỂM THỬ E2E (END-TO-END TEST FLOW) - PROMOTION SERVICE

## 1. Thông tin chung
- **Service**: `promotion-service`
- **Tính năng**: 
  - Promotion Query, Validation and Preview (Issue #130)
  - Promotion Apply & Reservation Flow (Issue #131)
- **Mục tiêu**: 
  - Kiểm thử các luồng truy vấn thông tin chương trình khuyến mãi (Public), xác thực mã giảm giá (validate), xem trước mức giảm giá (preview) liên kết với dữ liệu booking thực tế từ `booking-service`.
  - Kiểm thử quy trình áp dụng mã khuyến mãi chính thức từ các dịch vụ nội bộ, thực hiện giữ chỗ (RESERVED) nguyên tử bảo đảm đồng thời (concurrency), đảm bảo tính an toàn dữ liệu và cơ chế khôi phục (rollback) giao dịch khi có sự cố.

---

## Hướng dẫn chung: Chuẩn bị kiểm thử (Prerequisites)

Các API khách hàng sử dụng (`validate` và `preview`) được bảo vệ bằng JWT Token thông thường. Tuy thế, API áp dụng chính thức (`apply`) là API nội bộ (Internal API) hướng East-West, yêu cầu xác thực bằng Header chứa Token bảo mật nội bộ `X-Internal-Token`.

### Điều kiện chuẩn bị:
1. **Dịch vụ đang chạy**:
   - `eureka-server` chạy tại cổng `8761`.
   - `booking-service` chạy tại cổng `8083`.
   - `promotion-service` chạy tại cổng `8087`.
2. **Database & Seed Data**:
   - Chiến dịch khuyến mãi (`promotion_campaigns`) đang ở trạng thái kích hoạt (`is_active = true`), trong khoảng thời gian hiệu lực (`start_date <= now <= end_date`).
   - Mã khuyến mãi (`promotions`) tương ứng đang kích hoạt (`is_active = true`), còn lượt dùng (`used_count < usage_limit`), trong thời gian hiệu lực.
   - Có một Booking hợp lệ trong `booking-service` ở trạng thái `PENDING_PAYMENT` có `user_id = 1` và `total_amount = 100000.00`.
   - Đối với kiểm thử hết hạn, đảm bảo thời gian `expires_at` của Booking trong cơ sở dữ liệu lớn hơn thời gian hiện tại lúc gọi API.
3. **Mã xác thực & Cấu hình Token**:
   - **JWT Token**: Sử dụng token hợp lệ của User (khách hàng sở hữu Booking) khi gọi các API khách hàng (`/api/promotions/...`).
   - **Internal Token**: Khi gọi API nội bộ (`/internal/promotions/apply`), phải thêm header `X-Internal-Token` với giá trị khớp với cấu hình nội bộ (mặc định là `secret-internal-token`).

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
    "promotionCode": "PERCENT10",
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
  - **Negative Case 2 (Mã chưa bắt đầu)**: Dùng mã có `startDate` trong tương lai -> API trả về `409 Conflict` (Mã lỗi `PROMOTION_NOT_STARTED` kèm payload chứa `startDate` trong `"data"` gd).
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
  1. Ghi nhận giá trị lượt sử dụng `usedCount` của mã `PERCENT10` và tổng số bản ghi trong bảng `promotion_usages`.
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
  - Sử dụng JMeter hoặc Apache Bench gửi đồng thời 100 luồng request gọi `POST /api/promotions/validate` và `POST /api/promotions/preview` cho cùng một mã `PERCENT10` và cùng `bookingId`.
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

### Flow 9: Áp dụng khuyến mãi nội bộ thành công - Giảm giá theo phần trăm (Internal Apply Promotion Success - Percentage Discount)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Hệ thống thực hiện áp dụng mã giảm giá và giữ chỗ chính thức khi người dùng bấm thanh toán đơn hàng (sử dụng mã giảm giá theo phần trăm).
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "PERCENT10",
    "bookingId": 1,
    "userId": 1,
    "bookingAmount": 100000.00,
    "bookingExpiresAt": "2026-07-10T12:00:00"
  }
  ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - Trả về `201 Created`.
    - Dữ liệu trả về chứa `promotionUsageId`, `promotionId`, `discountAmount` (ví dụ: `10000`), `finalAmount` (`90000`), và trạng thái `RESERVED`.
    - Lượt sử dụng chương trình khuyến mãi tăng lên 1 (`used_count++` dưới Database).
    - Tạo bản ghi mới trong bảng `promotion_usages` ở trạng thái `RESERVED` chứa chi tiết thông tin giảm giá.

---

### Flow 10: Áp dụng khuyến mãi nội bộ thành công - Giảm giá tiền cố định (Internal Apply Promotion Success - Fixed Amount Discount)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Áp dụng mã giảm giá trực tiếp một khoản tiền cố định khi thanh toán đơn hàng.
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "FIXED50K",
    "bookingId": 2,
    "userId": 1,
    "bookingAmount": 100000.00,
    "bookingExpiresAt": "2026-07-10T12:00:00"
  }
  ```
- **Kết quả mong đợi**:
  - **Happy Case**:
    - Trả về `201 Created`.
    - Trả về `discountAmount = 50000` và `finalAmount = 50000` với trạng thái `RESERVED`.
    - Bản ghi `PromotionUsage` được tạo mới thành công, `used_count` của mã `FIXED50K` tăng lên 1.

---

### Flow 11: Kiểm soát lỗi mã khuyến mãi không tồn tại (Promotion Not Found)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Cuộc gọi gửi lên một mã khuyến mãi hoàn toàn không có trong hệ thống dữ liệu.
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "NONEXISTENT",
    "bookingId": 1,
    "userId": 1,
    "bookingAmount": 100000.00,
    "bookingExpiresAt": "2026-07-10T12:00:00"
  }
  ```
- **Kết quả mong đợi**:
  - API trả về `404 Not Found` kèm mã lỗi nghiệp vụ `PROMOTION_NOT_FOUND`.
  - Không có bất kỳ thay đổi nào dưới Database.

---

### Flow 12: Kiểm soát lỗi mã khuyến mãi bị vô hiệu hóa (Promotion Disabled)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Khuyến mãi có thuộc tính `is_active = false` hoặc chiến dịch liên kết có `is_active = false`.
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "DISABLED_CODE",
    "bookingId": 1,
    "userId": 1,
    "bookingAmount": 100000.00,
    "bookingExpiresAt": "2026-07-10T12:00:00"
  }
  ```
- **Kết quả mong đợi**:
  - API trả về `409 Conflict` kèm mã lỗi nghiệp vụ `PROMOTION_DISABLED`.
  - Từ chối áp dụng và không ghi nhận thông tin giữ chỗ.

---

### Flow 13: Kiểm soát lỗi mã khuyến mãi chưa bắt đầu hiệu lực (Promotion Not Started)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Chiến dịch hoặc mã có thời gian hiệu lực bắt đầu (`start_date`) lớn hơn thời điểm hiện tại.
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "UPCOMING_CODE",
    "bookingId": 1,
    "userId": 1,
    "bookingAmount": 100000.00,
    "bookingExpiresAt": "2026-07-10T12:00:00"
  }
  ```
- **Kết quả mong đợi**:
  - API trả về `409 Conflict` kèm mã lỗi nghiệp vụ `PROMOTION_NOT_STARTED`.
  - Trả về thông tin bổ sung chứa mốc thời gian bắt đầu `startDate` trong `data`.

---

### Flow 14: Kiểm soát lỗi mã khuyến mãi đã hết hạn (Promotion Expired)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Cuộc gọi sử dụng mã có mốc kết thúc (`end_date`) nhỏ hơn thời điểm hiện tại.
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Request Body**:
  ```json
  {
    "promotionCode": "EXPIRED_CODE",
    "bookingId": 1,
    "userId": 1,
    "bookingAmount": 100000.00,
    "bookingExpiresAt": "2026-07-10T12:00:00"
  }
  ```
- **Kết quả mong đợi**:
  - API trả về `409 Conflict` kèm mã lỗi nghiệp vụ `PROMOTION_EXPIRED`.

---

### Flow 15: Xác thực và đối chiếu Booking Context (Booking Validation)
- **API**: `POST /internal/promotions/apply`
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Các kịch bản kiểm soát**:
  1. **Booking không tồn tại (Not Found)**:
     - Dùng `bookingId` không có thực (Ví dụ: `99999`) -> Trả về `404 Not Found` với mã lỗi `PROMOTION_BOOKING_NOT_FOUND`.
  2. **Booking đã quá hạn thanh toán (Expired)**:
     - Sửa `expires_at` của Booking trong database hoặc gửi `bookingExpiresAt` trong request có mốc thời gian nằm trong quá khứ -> Trả về `409 Conflict` với mã lỗi `PROMOTION_BOOKING_NOT_ELIGIBLE` ("Booking has expired").
  3. **Sai tài khoản đặt vé (User Mismatch)**:
     - Gửi `userId = 999` trong request (DB lưu `user_id = 1`) -> Trả về `409 Conflict` với mã lỗi `PROMOTION_BOOKING_NOT_ELIGIBLE` ("Booking user mismatch").
  4. **Số tiền không trùng khớp (Amount Mismatch)**:
     - Gửi `bookingAmount = 150000.00` trong request (DB lưu `total_amount = 100000.00`) -> Trả về `409 Conflict` với mã lỗi `PROMOTION_BOOKING_NOT_ELIGIBLE` ("Booking amount mismatch").
  5. **Trạng thái Booking không hợp lệ**:
     - Booking có trạng thái là `CONFIRMED` hoặc `CANCELLED` -> Trả về `409 Conflict` với mã lỗi `PROMOTION_BOOKING_NOT_ELIGIBLE` ("Booking is not in PENDING_PAYMENT status").

---

### Flow 16: Kiểm soát giới hạn lượt dùng tối đa trên mỗi khách hàng (Per User Limit Reached)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Một khách hàng cố tình sử dụng cùng một mã vượt quá số lần cho phép (`limit_per_user` cấu hình trong DB).
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Cách thực hiện**:
  1. Giả sử mã có cấu hình giới hạn tối đa `limit_per_user = 1`.
  2. Chạy **Flow 9** thành công để tạo lượt sử dụng đầu tiên của `userId = 1`.
  3. Chạy tiếp một request khác dùng mã đó cho `bookingId = 2` của cùng `userId = 1`.
- **Kết quả mong đợi**:
  - Request thứ hai bị từ chối với lỗi `409 Conflict`.
  - Trả về mã lỗi nghiệp vụ `PROMOTION_USER_LIMIT_REACHED`.
  - Giá trị lượt sử dụng `used_count` của chương trình khuyến mãi không tăng thêm.

---

### Flow 17: Kiểm soát giới hạn lượt dùng tối đa toàn hệ thống (Global Usage Limit Exceeded)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Tổng số lượt đăng ký áp dụng mã đã đạt trần cho phép (`used_count = usage_limit`).
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Cách thực hiện**:
  1. Trong Database, sửa giá trị `used_count = 100` và `usage_limit = 100` của mã muốn test.
  2. Thực hiện gửi yêu cầu áp dụng mã đó cho một booking mới.
- **Kết quả mong đợi**:
  - Trả về `409 Conflict` kèm lỗi `PROMOTION_USAGE_LIMIT_EXCEEDED` ("Usage limit has been reached").
  - Giao dịch bị hủy bỏ, bảo toàn số lượt sử dụng.

---

### Flow 18: Đảm bảo tính chống trùng lắp (Idempotence Matching)
- **API**: `POST /internal/promotions/apply`
- **Ngữ cảnh**: Hệ thống nhận lại cùng một yêu cầu áp dụng mã cho một đơn hàng (Ví dụ: do Client gửi trùng lặp hoặc thực hiện Retry khi gặp trục trặc mạng).
- **Headers**:
  - `X-Internal-Token`: `secret-internal-token`
  - `Content-Type`: `application/json`
- **Kịch bản kiểm thử**:
  1. **Khớp tham số (Idempotency Success)**:
     - Gửi Request A cho `bookingId = 1` -> Trả về `201 Created` kèm thông tin giữ chỗ thành công.
     - Tiếp tục gửi nguyên trạng Request A một lần nữa -> Hệ thống nhận diện trùng khớp và ngay lập tức trả lại thông tin giống 100% như Request A cùng mã trạng thái `201 Created` / `200 OK` mà không ghi nhận thêm bất kỳ `used_count` nào hay chèn dữ liệu mới vào bảng `promotion_usages`.
  2. **Lệch tham số (Idempotency Mismatch)**:
     - Gửi lại request với cùng `bookingId = 1` nhưng thay đổi thông tin `bookingAmount = 250000.00` -> Hệ thống so sánh dữ liệu lịch sử và báo lỗi ngay lập tức: `409 Conflict` kèm thông báo `PROMOTION_BOOKING_NOT_ELIGIBLE` ("Idempotency request mismatch").

---

### Flow 19: Giữ chỗ nguyên tử chống tranh chấp đồng thời (Atomic Reservation under Concurrency)
- **Mục tiêu**: Đảm bảo tại thời điểm mã khuyến mãi chỉ còn duy nhất 1 lượt sử dụng còn lại, nếu có 2 hoặc nhiều cuộc gọi đồng thời gửi yêu cầu, hệ thống chỉ chấp thuận duy nhất 1 và từ chối các yêu cầu còn lại để tránh việc vượt quá giới hạn.
- **Cách thực hiện**:
  1. Sửa giá trị mã khuyến mãi trong database: `usage_limit = 10`, `used_count = 9` (chỉ còn 1 lượt dùng duy nhất).
  2. Dùng JMeter hoặc Apache Bench gửi đồng thời 10 request áp dụng mã này cho 10 `bookingId` khác nhau.
- **Kết quả mong đợi**:
  - Chỉ có duy nhất **1 request thành công** (trả về `201 Created`, ghi nhận bản ghi RESERVED).
  - **9 request còn lại đều bị từ chối** với lỗi `409 Conflict` (Mã lỗi `PROMOTION_USAGE_LIMIT_EXCEEDED` hoặc khóa lạc quan).
  - Kiểm tra lại trong DB: trường `used_count` tăng chính xác lên `10` (bằng `usage_limit`), tuyệt đối không vượt quá giới hạn.

---

### Flow 20: Tự động khôi phục giao dịch khi lỗi lưu trữ (Rollback Transaction Verification)
- **Mục tiêu**: Đảm bảo tính nhất quán (ACID). Nếu quá trình chèn bản ghi `PromotionUsage` thất bại (ví dụ: lỗi khóa ngoại hoặc ngắt kết nối database nửa chừng), việc cập nhật tăng lượt sử dụng `used_count++` trước đó của `Promotion` phải được khôi phục về trạng thái cũ.
- **Cách thực hiện**:
  1. Kịch hoạt kịch bản thử nghiệm ném ra một lỗi runtime bất kỳ ngay sau câu lệnh SQL cập nhật lượt sử dụng nhưng trước khi hoàn tất commit giao dịch (Ví dụ như thiết lập một vi phạm ràng buộc dữ liệu tại bảng `promotion_usages`).
  2. Gửi request áp dụng mã.
- **Kết quả mong đợi**:
  - Toàn bộ giao dịch bị hủy bỏ (`Rollback`).
  - Lượt sử dụng `used_count` của chương trình khuyến mãi trong Database được khôi phục toàn vẹn về nguyên trạng ban đầu.
  - Không có bất kỳ bản ghi giữ chỗ rác nào được tạo ra trong cơ sở dữ liệu.

---

### Flow 21: Kiểm tra an ninh API nội bộ (Internal Token Security Validation)
- **Mục tiêu**: Đảm bảo API hướng nội bộ `/internal/...` chỉ chấp nhận giao tiếp đáng tin cậy.
- **Các kịch bản kiểm thử**:
  1. **Không truyền Token**: Gọi `POST /internal/promotions/apply` mà không truyền header `X-Internal-Token` -> API trả về `401 Unauthorized`.
  2. **Truyền sai Token**: Truyền `X-Internal-Token = wrong-token` -> API trả về `401 Unauthorized` kèm thông báo lỗi `"Invalid internal token"`.
  3. **Truyền đúng Token**: Truyền `X-Internal-Token = secret-internal-token` -> Yêu cầu được chấp nhận và đi vào xử lý logic nghiệp vụ thành công.

---

### Flow 22: Đồng bộ hóa xử lý sự cố từ dịch vụ liên kết (Booking Service Failure Mapping & Timeout)
- **Mục tiêu**: Khi dịch vụ liên kết `booking-service` gặp sự cố (như lỗi nghiệp vụ, sập cổng dịch vụ hoặc bị timeout treo mạng), hệ thống phải trả về mã lỗi chuẩn chỉnh thay vì ném stack trace lỗi kết nối thô.
- **Các kịch bản kiểm thử**:
  1. **Booking Service bị sập (Down) hoặc lỗi kết nối**:
     - Tiến hành tắt ứng dụng `booking-service` (hoặc cấu hình sai port).
     - Gửi yêu cầu áp dụng khuyến mãi nội bộ -> API trả về `503 Service Unavailable` cùng mã lỗi nghiệp vụ `BOOKING_SERVICE_UNAVAILABLE` và thông điệp rõ ràng `"Booking service is down or timed out"`.
  2. **Booking Service trả về lỗi nghiệp vụ 404 (Not Found)**:
     - Khi `booking-service` trả về mã lỗi 404 (không tồn tại Booking) -> Hệ thống ánh xạ thành lỗi `404 Not Found` kèm mã lỗi nghiệp vụ `PROMOTION_BOOKING_NOT_FOUND`.
  3. **Kết nối bị timeout (Connection/Read Timeout)**:
     - Khi `booking-service` không phản hồi trong thời gian quy định (connect-timeout = 3 giây, read-timeout = 5 giây) -> Kết nối RestTemplate bị hủy và ném ra `ResourceAccessException` / `SocketTimeoutException`.
     - Hệ thống tự động bắt lỗi và ánh xạ thành mã lỗi `BOOKING_SERVICE_UNAVAILABLE` với mã HTTP `503 Service Unavailable`.

---

### Flow 23: Xác thực lưu vết dữ liệu dưới Database (Database Verification after Apply Success)
- **Mục tiêu**: Đảm bảo các thông tin sau khi áp dụng mã thành công được lưu trữ chính xác dưới cơ sở dữ liệu để phục vụ quy trình đối soát sau này.
- **Cách thực hiện**:
  1. Chạy thành công **Flow 9** (Áp dụng mã `PERCENT10` cho `bookingId = 1`).
  2. Thực hiện truy vấn cơ sở dữ liệu trực tiếp trong bảng `promotion_usages` ứng với `booking_id = 1`.
- **Kết quả kiểm tra thực tế trong DB**:
  - Trạng thái cột `status` phải lưu giá trị là `RESERVED`.
  - Giá trị cột `discount_amount` phải lưu đúng `10000.00`.
  - Giá trị cột `final_amount` phải lưu đúng `90000.00`.
  - Giá trị cột `original_amount` phải lưu đúng `100000.00`.
  - Cột `expires_at` phải ghi nhận chính xác hạn thanh toán đồng bộ từ Booking.
  - Trường `confirmed_at` và `reverted_at` phải ở trạng thái `NULL` (chờ thanh toán hoặc hoàn tác ở các bước sau).

---

## 3. Kiểm thử tính đồng thời (Concurrency Testing)

Hệ thống bảo vệ tranh chấp tài nguyên (Concurrency) bằng cơ chế cập nhật SQL nguyên tử ở tầng cơ sở dữ liệu (Atomic SQL Update) kết hợp cơ chế khóa lạc quan (Optimistic Locking) trên thực thể `Promotion` và `PromotionUsage` qua trường `@Version`.

### Kịch bản kiểm thử hiệu năng đồng thời:
*   **Công cụ thực hiện**: Sử dụng Apache JMeter tạo ra luồng kiểm thử giả lập 100 luồng request gửi đồng thời (Ramp-up period: 1 giây) gọi tới API `POST /internal/promotions/apply` để cạnh tranh 5 lượt sử dụng còn lại của chương trình khuyến mãi.
*   **Kết quả kỳ vọng**:
    *   Chỉ có chính xác **5 yêu cầu được ghi nhận thành công** (`201 Created`).
    *   **95 yêu cầu còn lại bị từ chối** một cách an toàn (`409 Conflict`) do hết lượt dùng hoặc tranh chấp khóa dữ liệu.
    *   Không xảy ra lỗi nghẽn hệ thống (Deadlock) hay sập kết nối Database Pool.
    *   Giá trị `used_count` trong database tăng lên chính xác thêm 5 đơn vị.

---

## 4. Hiệu năng & Bảo mật (Performance & Security)

### Hiệu năng (Performance)
*   **Atomic DB Update**: Thay vì thực hiện luồng đọc `used_count` lên bộ nhớ rồi tăng 1 và ghi xuống (dễ gây ra race conditions), hệ thống cập nhật lượt dùng nguyên tử bằng câu lệnh SQL duy nhất có điều kiện kiểm tra giới hạn tại DB.
*   **Transactional Isolation**: Toàn bộ luồng áp dụng và tạo bản ghi được đóng gói trong một Transaction duy nhất giúp giảm thiểu thời gian chiếm giữ kết nối dữ liệu.
*   **Optimistic Locking**: Tích hợp trường `@Version` trên thực thể để tránh việc ghi đè dữ liệu cũ khi có luồng chỉnh sửa đồng thời từ trang quản trị Admin.
*   **Timeout Reliability**: Cấu hình thời gian kết nối tối đa 3 giây (`connect-timeout`) và thời gian đọc socket tối đa 5 giây (`read-timeout`) cho `RestTemplate` nhằm tránh nguy cơ treo luồng vô hạn khi dịch vụ booking gặp sự cố tải cao hoặc treo mạng.

### Bảo mật (Security)
*   **Xác thực Token nội bộ chéo (East-West Traffic)**: Bảo vệ hoàn toàn API nội bộ bằng cơ chế lọc Header `X-Internal-Token` độc lập, tránh việc kẻ xấu cố tình khai thác cổng API này từ môi trường Public bên ngoài.
*   **Đối soát dữ liệu kép (Double validation)**: Không tin tưởng mù quáng vào số tiền do client gửi lên mà luôn đối soát chéo giá trị đơn hàng thực tế lấy trực tiếp từ `booking-service`.
*   **Chống rò rỉ dữ liệu nhạy cảm**: Ẩn toàn bộ thông tin Stack Trace lỗi hệ thống, thay vào đó trả về các mã lỗi nghiệp vụ chuẩn hóa tại `GlobalExceptionHandler`.

---

## 5. Tổng hợp kết quả tự động (Automated Test Metrics)
*   **Môi trường chạy**: Local Integration Tests với cơ sở dữ liệu MySQL và Spring Boot MockMvc.
*   **Lệnh thực thi**: `mvn clean test`
*   **Tổng số Test Cases hoạt động**: **88** (bao gồm cả Unit và Integration Tests)
    *   Các test case quản trị và nghiệp vụ khách hàng cũ: 70 tests.
    *   Các test case tích hợp mới cho Issue #131 (xác thực token, giữ chỗ, kiểm soát giới hạn, idempotency, concurrent race): **16 tests** (nằm trong file `InternalPromotionControllerTest.java`).
    *   Các test case kiểm thử lỗi timeout và ngoại lệ RestTemplate: **2 tests** (nằm trong file `RealBookingInternalClientTest.java`).
*   **Tỉ lệ vượt qua (Pass Rate)**: **100%** (0 Failures, 0 Errors, 0 Skipped).

---

## 6. Danh sách kiểm tra cuối cùng (Final Verification Checklist)
- [x] Đảm bảo tuân thủ thiết kế đặc tả API Contract Sprint 2 đối với endpoint nội bộ.
- [x] Kiểm thử giữ chỗ nguyên tử (Atomic reservation) hoạt động chính xác và an toàn.
- [x] Đảm bảo tính chất chống trùng lắp (Idempotency) theo đúng ID của Booking.
- [x] Toàn bộ các kiểm tra nghiệp vụ ràng buộc thời gian hiệu lực và trạng thái Booking hoạt động đầy đủ.
- [x] Tính toán số tiền được giảm giá và số tiền cuối cùng làm tròn `HALF_UP` chính xác theo đơn vị tiền tệ VND.
- [x] Cơ chế Rollback tự động phục hồi nguyên trạng dữ liệu khi có lỗi xảy ra.
- [x] Token bảo mật nội bộ được đồng bộ hóa và bảo vệ API nội bộ thành công.
- [x] Cấu hình timeout (connect & read timeout) cho RestTemplate và map lỗi về `BOOKING_SERVICE_UNAVAILABLE`.
- [x] Viết thêm 2 unit test kiểm định kịch bản timeout và lỗi RestTemplate thành công.
- [x] 100% các ca kiểm thử tự động (88 tests) chạy thành công vượt qua kiểm duyệt.
