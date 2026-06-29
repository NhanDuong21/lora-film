# Phản hồi Review Merge Request - Feature: Earn Score and Membership Tier Calculation (Issue #136)

Chào anh/chị, tôi gửi phản hồi chi tiết về các chức năng đã hoàn thành và hướng dẫn kiểm kiểm thử cho tính năng tích điểm (Earn Score) & tự động tính toán lại hạng thành viên của `score-service`:

---

## 1. Tóm tắt các chức năng đã triển khai

* **Endpoint mới:** `POST /internal/scores/earn` dành riêng cho tích hợp nội bộ giữa các microservices (Booking, Payment).
* **Cơ chế bảo mật:** Bảo mật bằng Token nội bộ, yêu cầu header `X-Internal-Token` khớp với cấu hình hệ thống (không cho phép JWT của khách hàng gọi trực tiếp).
* **Giải thuật tích điểm & Nâng hạng động (Data-driven):**
  * Điểm tích lũy được tính bằng: `floor(eligibleAmount * currentEarningRate / 1000)` sử dụng `BigDecimal` với chế độ làm tròn `RoundingMode.FLOOR` để tránh sai số.
  * Tỷ lệ tích điểm (`earningRate`) được lấy từ Hạng thẻ của khách hàng **trước khi giao dịch tích điểm diễn ra**.
  * Tự động xác định và nâng hạng thẻ mới dựa trên điểm số lũy kế sau khi cộng điểm mà **không hardcode bất kỳ ngưỡng điểm hay tên hạng thẻ nào trong code**.
  * Tự động khởi tạo tài khoản điểm (Lazy Initialization) với hạng thẻ thấp nhất trong database nếu người dùng chưa có tài khoản điểm.
* **Cơ chế xử lý tranh chấp & Idempotency:**
  * Khóa bi quan (`PESSIMISTIC_WRITE`) trên tài khoản điểm của người dùng giúp ngăn chặn việc mất mát điểm khi nhiều tiến trình chạy đồng thời.
  * Hỗ trợ tính năng không trùng lặp (Idempotent) thông qua kiểm tra đồng thời cả `eventId` và `idempotencyKey`. Nếu trùng lặp, trả về thông tin giao dịch cũ mà không thay đổi database.
  * Tự động bắt lỗi xung đột chỉ mục duy nhất (Unique Index Race) của Database khi hai luồng đồng thời cố gắng ghi đè cùng một khóa và trả về kết quả thành công thay vì ném lỗi giao dịch.
  * Kiểm soát tràn số nguyên điểm (`Integer.MAX_VALUE`) và trả về lỗi nghiệp vụ `SCORE_POINT_OVERFLOW`.

---

## 2. Hướng dẫn chạy Test tự động (Terminal Test)

Trong thư mục `server/score-service`, vui lòng đảm bảo dừng mọi tiến trình ứng dụng đang chạy trước, sau đó thực thi lệnh:
```bash
# Di chuyển vào service và chạy test
cd server/score-service
mvn clean verify
```
**Kết quả mong đợi:**
* Kết quả build: `BUILD SUCCESS`
* Tổng số test: **28 tests run**, **0 failures**, **0 errors**, **0 skipped** (bao gồm 15 bài test JPA/Persistence và 13 bài test tích hợp mới thêm).

---

## 3. Hướng dẫn Test tay chi tiết bằng Swagger (Manual Testing)

### Bước A: Chuẩn bị Header X-Internal-Token trên Swagger
1. Mở Swagger của **`score-service`** (`http://localhost:8088/swagger-ui.html`).
2. Bấm vào nút **Authorize** ở góc phải phía trên Swagger.
3. Tìm đến phần **internalAuth (apiKey)**, nhập giá trị token nội bộ: `secret-internal-token` rồi nhấn **Authorize** để cấu hình quyền truy cập.

---

### Bước B: Thiết lập khớp ID người dùng và nạp điểm Test

#### 1. Tìm User ID của bạn:
* Gọi API `GET /api/scores/me` trên Swagger của `score-service`, xem `userId` trong response (ví dụ là `4`).

#### 2. Cấu hình khớp ID chủ sở hữu Booking trong code:
* Mở file [BookingInternalClientImpl.java](file:hcm26_cpl_java_05_group3/server/score-service/src/main/java/com/project/scoreservice/client/impl/BookingInternalClientImpl.java) dòng 24.
* Đổi giá trị `setUserId(...)` khớp với ID tài khoản hiện tại của bạn:
  ```java
  context.setUserId(4L); // Thay bằng ID thực tế của bạn (ví dụ 4L)
  ```
* Lưu file và **khởi động lại `score-service`**.

#### 3. Reset dữ liệu điểm cũ của tài khoản (để test nâng hạng chính xác):
* Mở MySQL Workbench và chạy câu lệnh SQL xóa dữ liệu điểm cũ của User ID = 4 (nếu có) để đưa điểm về 0:
  ```sql
  DELETE FROM movie_db.user_scores WHERE user_id = 4;
  DELETE FROM movie_db.score_history WHERE user_id = 4;
  ```

---

### Bước C: Thực hiện Test từng API trên Swagger theo trình tự

#### 1. API: `POST /internal/scores/earn` (Yêu cầu X-Internal-Token)
Thực hiện gửi request body JSON theo từng kịch bản sau để kiểm tra:

* **Case 1.1: Tích điểm cho người dùng mới chưa có tài khoản (Lazy Initialization)**
  * Request Body:
    ```json
    {
      "userId": 4,
      "bookingId": 2001,
      "eligibleAmount": 100000,
      "eventId": "evt-earn-001",
      "idempotencyKey": "idem-earn-001"
    }
    ```
  * **Kết quả:** HTTP **200 OK**
    * Hệ thống tự động tạo tài khoản điểm cho User `4` với hạng thẻ mặc định là `SILVER` (tỷ lệ tích lũy là 0.05).
    * Cộng điểm: `100,000 * 0.05 / 1,000 = 5` điểm.
    * Response: `pointChange = 5`, `balanceAfter = 5`, `previousTier = "SILVER"`, `currentTier = "SILVER"`, `idempotent = false`.

* **Case 1.2: Tích số điểm lớn nâng hạng thẻ (Upgrade to GOLD)**
  * Request Body:
    ```json
    {
      "userId": 4,
      "bookingId": 2002,
      "eligibleAmount": 8000000,
      "eventId": "evt-earn-002",
      "idempotencyKey": "idem-earn-002"
    }
    ```
  * **Kết quả:** HTTP **200 OK**
    * Cộng điểm: `8,000,000 * 0.05 / 1,000 = 400` điểm (Vẫn áp dụng tỉ lệ 0.05 của SILVER trước giao dịch).
    * Tổng điểm tích lũy mới: `405` điểm (lớn hơn ngưỡng 400 của hạng GOLD).
    * Response: `pointChange = 400`, `balanceAfter = 405`, `previousTier = "SILVER"`, `currentTier = "GOLD"`, `tierChanged = true`.

* **Case 1.3: Tích điểm khi đã ở hạng thẻ GOLD (Kiểm tra áp dụng Earning Rate mới)**
  * Request Body:
    ```json
    {
      "userId": 4,
      "bookingId": 2003,
      "eligibleAmount": 100000,
      "eventId": "evt-earn-003",
      "idempotencyKey": "idem-earn-003"
    }
    ```
  * **Kết quả:** HTTP **200 OK**
    * Cộng điểm: `100,000 * 0.07 / 1,000 = 7` điểm (Áp dụng tỉ lệ 0.07 của GOLD).
    * Response: `pointChange = 7`, `balanceAfter = 412`, `previousTier = "GOLD"`, `currentTier = "GOLD"`, `tierChanged = false`.

* **Case 1.4: Kiểm tra tính không trùng lặp (Idempotency - Gửi lại y hệt Case 1.3)**
  * Request Body: Gửi lại y hệt body của Case 1.3.
  * **Kết quả:** HTTP **200 OK**
    * Hệ thống không ghi thêm lịch sử điểm, không cộng thêm điểm vào số dư.
    * Response trả về y hệt kết quả Case 1.3 nhưng có thêm flag: `"idempotent": true`.

* **Case 1.5: Lỗi xung đột Idempotency (Trùng mã nhưng sai thông tin yêu cầu)**
  * Request Body:
    ```json
    {
      "userId": 9999,
      "bookingId": 9999,
      "eligibleAmount": 100000,
      "eventId": "evt-earn-003",
      "idempotencyKey": "idem-earn-003"
    }
    ```
  * **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_IDEMPOTENCY_CONFLICT"`.

* **Case 1.6: Kiểm tra cơ chế làm tròn sàn (Floor Rounding)**
  * Request Body:
    ```json
    {
      "userId": 4,
      "bookingId": 2004,
      "eligibleAmount": 15000,
      "eventId": "evt-earn-004",
      "idempotencyKey": "idem-earn-004"
    }
    ```
  * **Kết quả:** HTTP **200 OK**
    * Do đang ở hạng GOLD (0.07), tích 15,000 VND $\rightarrow$ `15,000 * 0.07 = 1050 / 1000 = 1.05` điểm $\rightarrow$ làm tròn xuống là `1` điểm.
    * Response: `pointChange = 1`, `balanceAfter = 413`.

* **Case 1.7: Lỗi Định dạng đầu vào (Validation Error)**
  * Request Body:
    ```json
    {
      "userId": -4,
      "bookingId": 2005,
      "eligibleAmount": -15000,
      "eventId": "",
      "idempotencyKey": "idem-earn-005"
    }
    ```
  * **Kết quả:** HTTP **400 Bad Request**, `errorCode = "VALIDATION_ERROR"`.

* **Case 1.8: Lỗi bảo mật chặn truy cập khi thiếu token (Security Filter)**
  * Thực hiện gọi API mà không truyền Header `X-Internal-Token` (hoặc truyền sai).
  * **Kết quả:** HTTP **401 Unauthorized**, `errorCode = "UNAUTHORIZED"`, message `"Invalid internal token"`.
