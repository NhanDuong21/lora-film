# Hướng dẫn Kiểm thử và Mô tả Kết quả Triển khai - Issue #134 (Score Persistence Foundation)

## 1. Hướng dẫn chạy Test tự động (Persistence Test)

Chạy toàn bộ test:
```bash
mvn test
# hoặc
./mvnw test
```

Chạy riêng test của Issue #134:
```bash
mvn -Dtest=ScorePersistenceIntegrationTest test
```

Hoặc chạy trực tiếp class `ScorePersistenceIntegrationTest` trong IntelliJ IDEA hoặc Eclipse.

---

## 2. Kết quả mong đợi

Toàn bộ test phải **PASS**. Các nội dung được kiểm tra bao gồm:

| Hạng mục | Input | Output mong đợi |
| :--- | :--- | :--- |
| **Entity Mapping** | Khởi tạo Entity và load từ Database | Mapping đúng table, column, enum và foreign key |
| **Repository CRUD** | Thêm, tìm, cập nhật dữ liệu | Dữ liệu được lưu và truy xuất chính xác |
| **Tier Query** | accumulatedPoints = 0 | Trả về `SILVER` |
| **Tier Query** | accumulatedPoints = 450 | Trả về `GOLD` |
| **Tier Query** | accumulatedPoints = 1000 | Trả về `DIAMOND` |
| **Next Tier Query** | accumulatedPoints = 450 | Trả về `DIAMOND` là tier tiếp theo |
| **Lowest Tier Query** | Không có input | Trả về `SILVER` |
| **Enum Mapping** | Lưu `ScoreTransactionType` hoặc `ReconciliationStatus` | Database lưu đúng dạng String và đọc lại đúng Enum |
| **Foreign Key Mapping** | `UserScore` liên kết `MembershipTier` | Quan hệ được map đúng, không lỗi JPA |
| **Nullable UNIQUE** | Chèn nhiều bản ghi có `event_id = NULL` hoặc `request_id = NULL` | Thành công |
| **UNIQUE Constraint** | Chèn 2 bản ghi có cùng `event_id` hoặc `request_id` khác NULL | Lần chèn thứ hai thất bại |
| **Idempotency** | Chèn 2 bản ghi có cùng `idempotencyKey` | Lần chèn thứ hai thất bại |
| **Atomic Deduction** | `currentPoints = 100`, đồng thời 2 request cùng trừ 80 điểm | Chỉ 1 request thành công, số dư cuối cùng = 20, không âm |
| **Atomic Addition** | `currentPoints = 100`, cộng thêm 50 điểm | `currentPoints = 150` |
| **Pessimistic Lock** | Hai transaction cùng cập nhật một `UserScore` | Transaction thứ hai phải chờ transaction đầu tiên hoàn thành |
| **Transaction Rollback** | Cập nhật `UserScore` thành công nhưng insert `ScoreHistory` thất bại | Toàn bộ transaction rollback, dữ liệu không bị thay đổi |
| **Delete Restrict (MembershipTier)** | Xóa Tier đang được `UserScore` sử dụng | Bị từ chối do Foreign Key |
| **Delete Restrict (ScoreHistory)** | Xóa `ScoreHistory` đang được bản ghi khác tham chiếu | Bị từ chối do Foreign Key |

---

## 3. Ghi chú
* Issue này chỉ xây dựng tầng Persistence Foundation.
* Các Issue tiếp theo như Earn Score, Redeem Score, Refund/Revoke hoặc Balance API sẽ sử dụng trực tiếp các Entity và Repository đã được triển khai trong Issue này.

---

# Hướng dẫn Kiểm thử và Mô tả Kết quả Triển khai - Issue #135 (Score Query Foundation)

## 1. Các Nội Dung Đã Kiểm Thử (What Was Tested)

*   **Kiểm thử tự động (Automated Tests):** 
    *   Thực thi lệnh `mvn clean verify` thành công tuyệt đối.
    *   Tổng số: **17 tests run**, **0 failures**, **0 errors**, **0 skipped** (bao gồm 15 bài test JPA/Persistence và 2 bài test Controller Integration mới thêm).
*   **Kiểm thử tích hợp & API:** 
    *   Xác minh các endpoint qua Swagger UI của `score-service` sau khi xác thực JWT Token từ `auth-service`.
    *   Xác thực quyền truy cập giữa các microservices bằng Header `X-Internal-Token`.
    *   Kiểm tra toàn bộ các kịch bản xem trước đổi điểm (Redeem Preview) và các trường hợp lỗi nghiệp vụ (mã lỗi tương ứng với exception định nghĩa).

---

## 2. Hướng dẫn chạy Test tự động (Terminal Test)

Trong thư mục `server/score-service`, vui lòng đảm bảo dừng mọi tiến trình ứng dụng đang chạy trước, sau đó thực thi lệnh:

```bash
# Di chuyển vào service và chạy test
cd server/score-service
mvn clean verify
```

**Kết quả mong đợi:**

- Kết quả build: `BUILD SUCCESS`
- Tổng số test: **17 tests run**, **0 failures**, **0 errors**, **0 skipped** (bao gồm 15 bài test JPA/Persistence và 2 bài test Controller Integration mới thêm).

---

## 3. Hướng dẫn Test tay chi tiết bằng Swagger (Manual Testing)

### Bước A: Chuẩn bị Token JWT để cấu hình Swagger

1. Truy cập vào Swagger của **`auth-service`** (`http://localhost:8081/swagger-ui.html`).
2. Sử dụng API đăng ký tài khoản `POST /api/auth/register` và xác thực mã OTP bằng `POST /api/auth/verify`.
3. Gọi API đăng nhập `POST /api/auth/login` để lấy giá trị token `accessToken` trong JSON trả về.
4. Mở Swagger của **`score-service`** (`http://localhost:8088/swagger-ui.html`).
5. Bấm vào nút **Authorize** ở góc phải phía trên Swagger, nhập: `Bearer <token_của_bạn>` rồi nhấn Authorize để cấu hình quyền truy cập.

---

### Bước B: Thiết lập khớp ID người dùng và nạp điểm Test

#### 1. Tìm User ID của bạn:

- Gọi API `GET /api/scores/me` trên Swagger của `score-service`, xem `userId` trong response (ví dụ là `4`).

#### 2. Cấu hình khớp ID chủ sở hữu Booking trong code:

- Mở file `BookingInternalClientImpl.java` dòng 24.
- Đổi giá trị `setUserId(...)` khớp với ID tài khoản hiện tại của bạn:

```java
context.setUserId(4L); // Thay bằng ID thực tế của tài khoảng bạn đang login (ví dụ 4L)
```
- Lưu file và **khởi động lại `score-service`**.

#### 3. Nạp điểm test trong database:

- Mở MySQL Workbench và chạy câu lệnh SQL nạp điểm cho tài khoản của bạn (ví dụ ID = 4):

```sql
UPDATE movie_db.user_scores 
SET current_points = 500, accumulated_points = 1000 
WHERE user_id = 4;
```

---

### Bước C: Thực hiện Test từng API trên Swagger theo trình tự

#### 1. API: `GET /api/membership-tiers` (Public)

- Không cần đăng nhập, nhấn Execute.
- **Kết quả mong đợi:** HTTP **200 OK**, trả về mảng danh sách Tier đã sắp xếp theo `minPoints` tăng dần: `SILVER (0)` -> `GOLD (400)` -> `DIAMOND (1000)`.

#### 2. API: `GET /api/scores/me` (Yêu cầu Token)

- Thực hiện gửi request.
- **Kết quả mong đợi:** HTTP **200 OK**, hiển thị thông tin điểm và hạng thẻ tương ứng với dữ liệu đã nạp ở Database.

#### 3. API: `GET /api/scores/me/tier` (Yêu cầu Token)

- Thực hiện gửi request.
- **Kết quả mong đợi:** HTTP **200 OK**, hiển thị chi tiết hạng thẻ hiện tại của tài khoản.

#### 4. API: `GET /api/scores/me/history` (Yêu cầu Token - Kiểm tra sort)

Thực hiện test các trường hợp lỗi định dạng query đầu vào:

- **Case 4.1: Page index âm**
  - Thiết lập param: `page = -1`
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_QUERY"`.
- **Case 4.2: Sắp xếp theo cột không hợp lệ (Không thuộc whitelist)**
  - Thiết lập param: `sort = userScore.id,desc`
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_QUERY"`.
- **Case 4.3: Khoảng thời gian không hợp lệ (`from` > `to`)**
  - Thiết lập param: `from = 2026-06-30T00:00:00`, `to = 2026-06-29T00:00:00`
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_QUERY"`.

#### 5. API: `GET /internal/scores/users/{userId}` (Yêu cầu X-Internal-Token)

- **Ý nghĩa:** Gọi nội bộ giữa các microservices để lấy nhanh thông tin điểm và tỉ lệ tích lũy của user.
- **Case 5.1: Thiếu hoặc sai header `X-Internal-Token`**
  - Nhập `userId = 4` (hoặc ID tài khoản của bạn).
  - Trong phần Headers của Swagger (hoặc Postman), để trống hoặc nhập sai header `X-Internal-Token`.
  - **Kết quả:** HTTP **401 Unauthorized**, `errorCode = "UNAUTHORIZED"`, `message = "Invalid internal token"`.
- **Case 5.2: Truyền đúng Header `X-Internal-Token` (Happy Case)**
  - Nhập `userId = 4` (hoặc ID tài khoản của bạn).
  - Thêm header: `X-Internal-Token = secret-internal-token`
  - **Kết quả:** HTTP **200 OK**, hiển thị thông tin điểm và `earningRate` tương ứng của User.

#### 6. API: `POST /api/scores/me/redeem-preview` (Yêu cầu Token)

Thực hiện gửi request body JSON theo từng kịch bản sau để kiểm tra:

- **Case 6.1: Happy Case (Xem trước thành công)**
  - Request Body: `{ "bookingId": 1001, "requestedPoints": 100 }`
  - **Kết quả:** HTTP **200 OK**, trả về số tiền quy đổi `redeemValue` là `100000` VND. Dữ liệu điểm trong DB vẫn giữ nguyên không thay đổi (không lưu vết trừ điểm).
- **Case 6.2: Nhập số điểm bằng 0 hoặc âm (Lỗi Blocker đã fix)**
  - Request Body: `{ "bookingId": 1001, "requestedPoints": 0 }` (hoặc `-10`)
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_POINT_AMOUNT"`.
- **Case 6.3: Yêu cầu đổi điểm lớn hơn số dư**
  - Request Body: `{ "bookingId": 1001, "requestedPoints": 999999 }`
  - **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_INSUFFICIENT_BALANCE"`.
- **Case 6.4: Sai chủ sở hữu Booking**
  - Request Body: `{ "bookingId": 1002, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **403 Forbidden**, `errorCode = "SCORE_BOOKING_OWNERSHIP_MISMATCH"`.
- **Case 6.5: Booking không tồn tại / Dịch vụ lỗi**
  - Request Body: `{ "bookingId": 9999, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **503 Service Unavailable**, `errorCode = "BOOKING_SERVICE_UNAVAILABLE"`.
- **Case 6.6: Vé đã bị hủy hoặc trạng thái không hợp lệ**
  - Request Body: `{ "bookingId": 1003, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_BOOKING_NOT_ELIGIBLE"`.
- **Case 6.7: Vé quá hạn thanh toán**
  - Request Body: `{ "bookingId": 1004, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_BOOKING_NOT_ELIGIBLE"`.
