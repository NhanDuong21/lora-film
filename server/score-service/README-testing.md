# Hướng dẫn Kiểm thử và Mô tả Kết quả Triển khai - Issue #135 (Score Query Foundation)

Tài liệu này tổng hợp các công việc đã thực hiện cho **Issue #135 (Score Balance, Tier and History Query APIs)** và hướng dẫn từng bước chi tiết cách lấy mã token/key bảo mật, cấu hình xác thực và thực hiện kiểm thử các API thông qua Swagger UI.

---

## 1. Công việc đã thực hiện (Summary of Changes)
* **Cấu hình & Kết nối**:
  * Tích hợp Spring Security, JWT và cấu hình Swagger hỗ trợ xác thực 2 lớp (Bearer JWT & X-Internal-Token).
  * Trỏ kết nối Database MySQL đến port `3307` của Docker.
* **Xây dựng DTOs & Exception Handler**:
  * Tạo các DTO chuẩn hóa (không expose Entity hay các trường nhạy cảm).
  * Triển khai bộ Exception Handler trả về cấu trúc lỗi chuẩn hóa của hệ thống.
* **Xây dựng Logic Nghiệp vụ**:
  * **Lazy Initialization**: Tự động tạo mới tài khoản điểm cho user mới khi gọi các API `/me`, xử lý tranh chấp tranh chấp khoá chính an toàn trong môi trường bất đồng bộ.
  * **Tính hạng thành viên**: Lookup động hạng dựa trên điểm sàn `minPoints` (ngưỡng SILVER=0, GOLD=400, DIAMOND=1000). Tính điểm tích lũy cần thiết để lên hạng tiếp theo.
  * **Bộ lọc lịch sử giao dịch**: Hỗ trợ phân trang, lọc theo thời gian, loại giao dịch, mã booking và áp dụng whitelist sắp xếp.
  * **Xem trước quy đổi**: Tính giá trị giảm giá VND của điểm dựa trên tỷ lệ cấu hình tập trung ở properties (`score.redemption.value-per-point: 1000`), không thay đổi số dư tài khoản.
  * **Booking Validation Stub**: Giả lập gọi API Booking-Service để kiểm tra quyền sở hữu, thời gian hết hạn và trạng thái của booking trước khi cho xem thử.

---

## 2. Hướng dẫn lấy Token Xác thực & Khóa bảo mật (Authentication Credentials)

Để gọi các API thành công, bạn cần lấy mã Token khách hàng và Key bảo mật cho các cuộc gọi nội bộ:

### Bước 2.1: Lấy mã JWT Token khách hàng từ `auth-service`
1. Đăng ký tài khoản khách hàng mới (nếu chưa có)
2. Đăng nhập tài khoảng
3. Copy chuỗi `accessToken` nhận được trong JSON response.

### Bước 2.2: Khóa bảo mật dịch vụ nội bộ (Internal Key)
* API nội bộ `/internal/**` được bảo vệ bằng header `X-Internal-Token`.
* Khóa bảo mật này được định cấu hình trực tiếp tại file `server/score-service/src/main/resources/application.properties` tại dòng `app.internal-token`:
  ```properties
  app.internal-token=secret-internal-token
  ```
* Giá trị của khóa cần dùng là: **`secret-internal-token`** (chỉ lấy chuỗi chữ này, không bao gồm phần `app.internal-token=`).

---

## 3. Hướng dẫn khởi chạy ứng dụng và cấu hình Swagger UI
1. Đảm bảo container MySQL của Docker đang chạy trên cổng `3307`.
2. Khởi chạy ứng dụng bằng lệnh:
   ```bash
   mvn spring-boot:run
   ```
   *Ứng dụng chạy trên cổng `8088`.*
3. Truy cập Swagger UI qua trình duyệt:
   `http://localhost:8088/swagger-ui/index.html`
4. Cấu hình quyền truy cập (Authorize):
   * Click nút **Authorize** ở góc trên bên phải của Swagger UI.
   * Tại mục **BearerAuth (http, Bearer)**: Dán chuỗi `accessToken` thu được ở Bước 2.1 vào.
   * Tại mục **InternalTokenAuth (apiKey)**: Điền chuỗi **`secret-internal-token`** vào.
   * Nhấn **Authorize** rồi nhấn **Close**.

---

## 4. Các kịch bản kiểm thử chi tiết trên Swagger UI

### Kịch bản 1: Lấy danh sách hạng thành viên (Public)
* **Endpoint**: `GET /api/membership-tiers`
* **Xác thực**: Không yêu cầu token.
* **Cách thực hiện**: 
  1. Click chọn endpoint `GET /api/membership-tiers`.
  2. Click nút **Try it out**.
  3. Click nút **Execute** (không cần nhập thêm tham số gì).
* **Kết quả kỳ vọng**: Trả về HTTP `200 OK` chứa danh sách 3 hạng (SILVER, GOLD, DIAMOND) được sắp xếp theo `minPoints` tăng dần.

---

### Kịch bản 2: Lấy số dư và hạng của khách hàng mới (Lazy Initialization)
* **Endpoint**: `GET /api/scores/me`
* **Xác thực**: Yêu cầu Bearer JWT đã cấu hình ở Mục 3.
* **Cách thực hiện**:
  1. Click chọn endpoint `GET /api/scores/me`.
  2. Click nút **Try it out**.
  3. Click nút **Execute**.
* **Kết quả kỳ vọng**:
  * Tài khoản điểm cho user ID 15 được tạo tự động dưới database MySQL với 0 điểm (SILVER).
  * Trả về HTTP `200 OK` có nội dung:
    ```json
    {
      "success": true,
      "message": "Score balance retrieved successfully",
      "data": {
        "userId": 15,
        "currentPoints": 0,
        "accumulatedPoints": 0,
        "currentTier": { "tierId": 1, "tierName": "SILVER", "minPoints": 0, "earningRate": 0.05 },
        "nextTier": { "tierId": 2, "tierName": "GOLD", "minPoints": 400, "pointsRequired": 400 },
        "updatedAt": "2026-06-28..."
      }
    }
    ```

---

### Kịch bản 3: Truy vấn tiến độ hạng thành viên
* **Endpoint**: `GET /api/scores/me/tier`
* **Xác thực**: Yêu cầu Bearer JWT.
* **Cách thực hiện**:
  1. Click chọn endpoint `GET /api/scores/me/tier`.
  2. Click nút **Try it out**.
  3. Click nút **Execute**.
* **Kết quả kỳ vọng**: Trả về hạng hiện tại, điểm tích lũy và cấu hình nâng hạng tiếp theo kèm số điểm cần đạt.

---

### Kịch bản 4: Truy vấn lịch sử điểm và kiểm tra các validations
* **Endpoint**: `GET /api/scores/me/history`
* **Xác thực**: Yêu cầu Bearer JWT.

#### Các tình huống lỗi đầu vào (Validation checks):
* **Kịch bản 4A (Phân trang âm)**:
  * Click **Try it out**. Nhập tham số:
    * `page`: `-1`
    * `size`: `10`
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `400 Bad Request`, mã lỗi `SCORE_INVALID_QUERY`.
* **Kịch bản 4B (Sắp xếp không hợp lệ)**:
  * Click **Try it out**. Nhập tham số:
    * `page`: `0`
    * `size`: `10`
    * `sort`: `userScore.id` (cột không được whitelist).
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `400 Bad Request`, mã lỗi `SCORE_INVALID_QUERY`.
* **Kịch bản 4C (Khoảng thời gian sai lệch)**:
  * Click **Try it out**. Nhập tham số:
    * `from`: `2026-06-28T12:00:00`
    * `to`: `2026-06-27T12:00:00` (from sau to)
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `400 Bad Request`, mã lỗi `SCORE_INVALID_QUERY`.
* **Kịch bản 4D (Hợp lệ)**:
  * Click **Try it out**. Giữ nguyên các tham số mặc định (hoặc nhập `page=0, size=10, sort=createdAt,desc`).
  * Nhấn **Execute**.
  * **Kết quả**: Trả về HTTP `200 OK` kèm danh sách lịch sử trống hoặc các bản ghi của chính user.

---

### Kịch bản 5: Xem thử quy đổi điểm sang tiền vé (Redeem Preview)
* **Endpoint**: `POST /api/scores/me/redeem-preview`
* **Xác thực**: Yêu cầu Bearer JWT.

#### Các tình huống lỗi kiểm tra tính hợp lệ của đơn hàng (Booking context checks):
* **Kịch bản 5A (Số điểm yêu cầu không hợp lệ)**:
  * Click **Try it out**. Nhập JSON Body:
    ```json
    {
      "bookingId": 1001,
      "requestedPoints": 0
    }
    ```
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `400 Bad Request`, mã lỗi `SCORE_INVALID_POINT_AMOUNT`.
* **Kịch bản 5B (Không đủ số dư điểm)**:
  * Nhập JSON Body:
    ```json
    {
      "bookingId": 1001,
      "requestedPoints": 100
    }
    ```
    *(Tài khoản mới tạo chỉ có số dư 0)*
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `409 Conflict`, mã lỗi `SCORE_INSUFFICIENT_BALANCE`.
* **Kịch bản 5C (Booking không tồn tại / Dịch vụ lỗi)**:
  * Nhập JSON Body:
    ```json
    {
      "bookingId": 9999,
      "requestedPoints": 10
    }
    ```
    *(Để kiểm chứng lỗi, trước hết bạn cần cập nhật điểm bằng cách vào database docker chạy lệnh: `UPDATE user_scores SET current_points = 150 WHERE user_id = 15;`)*
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `503 Service Unavailable`, mã lỗi `BOOKING_SERVICE_UNAVAILABLE`.
* **Kịch bản 5D (Không trùng khớp chủ sở hữu booking)**:
  * Nhập JSON Body:
    ```json
    {
      "bookingId": 1002,
      "requestedPoints": 10
    }
    ```
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `403 Forbidden`, mã lỗi `SCORE_BOOKING_OWNERSHIP_MISMATCH`.
* **Kịch bản 5E (Booking không đủ điều kiện - đã hủy/thanh toán xong)**:
  * Nhập JSON Body:
    ```json
    {
      "bookingId": 1003,
      "requestedPoints": 10
    }
    ```
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `409 Conflict`, mã lỗi `SCORE_BOOKING_NOT_ELIGIBLE`.
* **Kịch bản 5F (Booking đã hết hạn)**:
  * Nhập JSON Body:
    ```json
    {
      "bookingId": 1004,
      "requestedPoints": 10
    }
    ```
  * Nhấn **Execute**.
  * **Kết quả**: Lỗi `409 Conflict`, mã lỗi `SCORE_BOOKING_NOT_ELIGIBLE`.
* **Kịch bản 5G (Xem thử thành công)**:
  * Nhập JSON Body:
    ```json
    {
      "bookingId": 1001,
      "requestedPoints": 100
    }
    ```
  * Nhấn **Execute**.
  * **Kết quả**: HTTP `200 OK`. Trả về:
    ```json
    {
      "success": true,
      "message": "Score redemption preview calculated successfully",
      "data": {
        "bookingId": 1001,
        "availablePoints": 150,
        "requestedPoints": 100,
        "redeemValue": 100000,
        "currency": "VND",
        "previewOnly": true
      }
    }
    ```
    *Số dư điểm trong DB không bị trừ bớt.*

---

### Kịch bản 6: Gọi API truy vấn điểm nội bộ (Internal Query)
* **Endpoint**: `GET /internal/scores/users/{userId}`
* **Xác thực**: Yêu cầu truyền Header `X-Internal-Token` với giá trị là `secret-internal-token`.

#### Các tình huống test:
* **Kịch bản 6A (Thiếu hoặc sai token nội bộ)**:
  * Tắt xác thực trong Swagger Authorize, hoặc thay đổi khóa trong Header thành chuỗi khác.
  * Click **Try it out**. Nhập `userId`: `15` và nhấn **Execute**.
  * **Kết quả**: Lỗi `401 Unauthorized` hoặc `403 Forbidden`.
* **Kịch bản 6B (Thành công)**:
  * Đảm bảo đã nhập `secret-internal-token` tại hộp Authorize ở đầu trang.
  * Click **Try it out**. Nhập `userId`: `15` và nhấn **Execute**.
  * **Kết quả**: Trả về HTTP `200 OK` dạng:
    ```json
    {
      "success": true,
      "message": "User score retrieved successfully",
      "data": {
        "userId": 15,
        "currentPoints": 150,
        "accumulatedPoints": 150,
        "tierName": "SILVER",
        "earningRate": 0.05
      }
    }
    ```
