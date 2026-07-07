# Hướng dẫn kiểm thử issue #134

## Chạy toàn bộ test

```bash
mvn test
```

Hoặc:

```bash
./mvnw test
```

Chạy riêng test của Issue #134:
```bash
mvn -Dtest=ScorePersistenceIntegrationTest test
```

Hoặc chạy trực tiếp class:

```
ScorePersistenceIntegrationTest
```

trong IntelliJ IDEA hoặc Eclipse.

---

# Kết quả mong đợi

Toàn bộ test phải **PASS**.

Các nội dung được kiểm tra bao gồm:

| Hạng mục | Input | Output mong đợi |
|----------|-------|-----------------|
| Entity Mapping | Khởi tạo Entity và load từ Database | Mapping đúng table, column, enum và foreign key |
| Repository CRUD | Thêm, tìm, cập nhật dữ liệu | Dữ liệu được lưu và truy xuất chính xác |
| Tier Query | `accumulatedPoints = 0` | Trả về **SILVER** |
| Tier Query | `accumulatedPoints = 450` | Trả về **GOLD** |
| Tier Query | `accumulatedPoints = 1000` | Trả về **DIAMOND** |
| Next Tier Query | `accumulatedPoints = 450` | Trả về **DIAMOND** là tier tiếp theo |
| Lowest Tier Query | Không có input | Trả về **SILVER** |
| Enum Mapping | Lưu `ScoreTransactionType` hoặc `ReconciliationStatus` | Database lưu đúng dạng `String` và đọc lại đúng Enum |
| Foreign Key Mapping | `UserScore` liên kết `MembershipTier` | Quan hệ được map đúng, không lỗi JPA |
| Nullable UNIQUE | Chèn nhiều bản ghi có `event_id = NULL` hoặc `request_id = NULL` | Thành công |
| UNIQUE Constraint | Chèn 2 bản ghi có cùng `event_id` hoặc `request_id` khác `NULL` | Lần chèn thứ hai thất bại |
| Idempotency | Chèn 2 bản ghi có cùng `idempotencyKey` | Lần chèn thứ hai thất bại |
| Atomic Deduction | `currentPoints = 100`, đồng thời 2 request cùng trừ 80 điểm | Chỉ 1 request thành công, số dư cuối cùng = **20**, không âm |
| Atomic Addition | `currentPoints = 100`, cộng thêm 50 điểm | `currentPoints = 150` |
| Pessimistic Lock | Hai transaction cùng cập nhật một `UserScore` | Transaction thứ hai phải chờ transaction đầu tiên hoàn thành |
| Transaction Rollback | Cập nhật `UserScore` thành công nhưng insert `ScoreHistory` thất bại | Toàn bộ transaction rollback, dữ liệu không bị thay đổi |
| Delete Restrict (`MembershipTier`) | Xóa Tier đang được `UserScore` sử dụng | Bị từ chối do Foreign Key |
| Delete Restrict (`ScoreHistory`) | Xóa `ScoreHistory` đang được bản ghi khác tham chiếu | Bị từ chối do Foreign Key |

---

# Ghi chú

Issue này chỉ xây dựng tầng **Persistence Foundation**.

Các Issue tiếp theo như:

- Earn Score
- Redeem Score
- Refund / Revoke
- Balance API

sẽ sử dụng trực tiếp các **Entity** và **Repository** đã được triển khai trong Issue này.
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

### Bước B: Thiết lập User ID và nạp điểm Test

#### 1. Tìm User ID của bạn:

- Gọi API `GET /api/scores/me` trên Swagger của `score-service`, xem `userId` trong response (ví dụ là `4`).

#### 2. Cấu hình khớp ID chủ sở hữu Booking trong code:

- Mở file `BookingInternalClientImpl.java` dòng 24.
- Đổi giá trị `setUserId(...)` khớp với ID tài khoản hiện tại của bạn:

```java
context.setUserId(4L); // Thay bằng ID thực tế của tài khoản bạn đang login (ví dụ 4L)
```
- Lưu file và **khởi động lại `score-service`**.

#### 3. Nạp điểm test trong database:

- Mở MySQL Workbench và chạy câu lệnh SQL nạp điểm cho tài khoản của bạn (ví dụ ID = 4):

```sql
UPDATE score_db.user_scores 
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

#### 3. API: `GET /api/scores/me/history` (Yêu cầu Token - Kiểm tra sort)

Thực hiện test các trường hợp lỗi định dạng query đầu vào:

- **Case 3.1: Page index âm**
  - Thiết lập param: `page = -1`
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_QUERY"`.
- **Case 3.2: Sắp xếp theo cột không hợp lệ (Không thuộc whitelist)**
  - Thiết lập param: `sort = userScore.id,desc`
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_QUERY"`.
- **Case 3.3: Khoảng thời gian không hợp lệ (`from` > `to`)**
  - Thiết lập param: `from = 2026-06-30T00:00:00`, `to = 2026-06-29T00:00:00`
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_QUERY"`.

#### 4. API: `GET /internal/scores/users/{userId}` (Yêu cầu X-Internal-Token)

- **Ý nghĩa:** Gọi nội bộ giữa các microservices để lấy nhanh thông tin điểm và tỉ lệ tích lũy của user.
- **Case 4.1: Thiếu hoặc sai header `X-Internal-Token`**
  - Nhập `userId = 4` (hoặc ID tài khoản của bạn).
  - Trong phần Headers của Swagger (hoặc Postman), để trống hoặc nhập sai header `X-Internal-Token`.
  - **Kết quả:** HTTP **401 Unauthorized**, `errorCode = "UNAUTHORIZED"`, `message = "Invalid internal token"`.
- **Case 4.2: Truyền đúng Header `X-Internal-Token` (Happy Case)**
  - Nhập `userId = 4` (hoặc ID tài khoản của bạn).
  - Thêm header: `X-Internal-Token = secret-internal-token`
  - **Kết quả:** HTTP **200 OK**, hiển thị thông tin điểm và `earningRate` tương ứng của User.

#### 5. API: `POST /api/scores/me/redeem-preview` (Yêu cầu Token)

Thực hiện gửi request body JSON theo từng kịch bản sau để kiểm tra:

- **Case 5.1: Happy Case (Xem trước thành công)**
  - Request Body: `{ "bookingId": 1001, "requestedPoints": 100 }`
  - **Kết quả:** HTTP **200 OK**, trả về số tiền quy đổi `redeemValue` là `100000` VND. Dữ liệu điểm trong DB vẫn giữ nguyên không thay đổi (không lưu vết trừ điểm).
- **Case 5.2: Nhập số điểm bằng 0 hoặc âm (Lỗi Blocker đã fix)**
  - Request Body: `{ "bookingId": 1001, "requestedPoints": 0 }` (hoặc `-10`)
  - **Kết quả:** HTTP **400 Bad Request**, `errorCode = "SCORE_INVALID_POINT_AMOUNT"`.
- **Case 5.3: Yêu cầu đổi điểm lớn hơn số dư**
  - Request Body: `{ "bookingId": 1001, "requestedPoints": 999999 }`
  - **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_INSUFFICIENT_BALANCE"`.
- **Case 5.4: Sai chủ sở hữu Booking**
  - Request Body: `{ "bookingId": 1002, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **403 Forbidden**, `errorCode = "SCORE_BOOKING_OWNERSHIP_MISMATCH"`.
- **Case 5.5: Booking không tồn tại / Dịch vụ lỗi**
  - Request Body: `{ "bookingId": 9999, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **503 Service Unavailable**, `errorCode = "BOOKING_SERVICE_UNAVAILABLE"`.
- **Case 5.6: Vé đã bị hủy hoặc trạng thái không hợp lệ**
  - Request Body: `{ "bookingId": 1003, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_BOOKING_NOT_ELIGIBLE"`.
- **Case 5.7: Vé quá hạn thanh toán**
  - Request Body: `{ "bookingId": 1004, "requestedPoints": 10 }`
  - **Kết quả:** HTTP **409 Conflict**, `errorCode = "SCORE_BOOKING_NOT_ELIGIBLE"`.

---
# Hướng dẫn Kiểm thử và Mô tả Kết quả Triển khai - Issue #136 (Earn Score Flow)

## 1. Hướng dẫn chạy Test tự động (Terminal Test)

Trong thư mục `server/score-service`, vui lòng đảm bảo **dừng mọi tiến trình ứng dụng đang chạy** trước, sau đó thực thi lệnh:

```bash
# Di chuyển vào service và chạy test
cd server/score-service
mvn clean verify
```

## Kết quả mong đợi

- **Kết quả build:** `BUILD SUCCESS`
- **Tổng số test:** `28 tests run, 0 failures, 0 errors, 0 skipped`
  - Bao gồm:
    - **15** bài test JPA/Persistence
    - **13** bài test tích hợp (Integration Test)

---

## 2. Hướng dẫn Test tay chi tiết bằng Swagger (Manual Testing)

### Bước A: Chuẩn bị Header `X-Internal-Token`

1. Mở Swagger của **score-service** (`http://localhost:8088/swagger-ui.html`).
2. Nhấn nút **Authorize** ở góc trên bên phải.
3. Chọn mục **internalAuth (apiKey)**.
4. Nhập token:
   ```text
   secret-internal-token
   ```
5. Nhấn **Authorize** để hoàn tất cấu hình.

---

### Bước B: Thiết lập User ID và chuẩn bị dữ liệu

#### 1. Tìm User ID
Gọi API `GET /api/scores/me` trên Swagger (đã authorize JWT) để xem `userId` trả về (ví dụ là `4`).

#### 2. Cập nhật User ID trong code
Mở file `BookingInternalClientImpl.java` (Tại dòng 24), đổi:
```java
context.setUserId(4L); // Thay bằng User ID thực tế của bạn
```
Sau đó lưu file và khởi động lại `score-service`.

#### 3. Reset dữ liệu điểm (khuyến nghị)
Mở MySQL Workbench và chạy:
```sql
DELETE FROM score_db.user_scores WHERE user_id = 4;
DELETE FROM score_db.score_history WHERE user_id = 4;
```
*(Thay 4 bằng User ID thực tế của bạn)*

---

### Bước C: Kiểm thử từng API tích điểm

#### API: `POST /internal/scores/earn`

> **Yêu cầu Header**
> `X-Internal-Token: secret-internal-token`

#### Case 1.1 - Người dùng mới (Lazy Initialization)
* **Request**:
  ```json
  {
    "userId": 4,
    "bookingId": 2001,
    "eligibleAmount": 100000,
    "eventId": "evt-earn-001",
    "idempotencyKey": "idem-earn-001"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP `200 OK`
  - Tự động tạo tài khoản điểm cho User 4 với hạng **SILVER** (Earning Rate 0.05).
  - Điểm nhận được: `100000 * 0.05 / 1000 = 5` điểm.
  - Phản hồi chứa: `pointChange = 5`, `balanceAfter = 5`, `currentTier = SILVER`, `idempotent = false`.

#### Case 1.2 - Nâng hạng lên GOLD
* **Request**:
  ```json
  {
    "userId": 4,
    "bookingId": 2002,
    "eligibleAmount": 8000000,
    "eventId": "evt-earn-002",
    "idempotencyKey": "idem-earn-002"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP `200 OK`. Điểm cộng thêm: `8000000 * 0.05 / 1000 = 400` điểm.
  - Tổng tích lũy sau giao dịch đạt `405` điểm (vượt ngưỡng 400) -> Nâng lên **GOLD**.
  - Phản hồi chứa: `pointChange = 400`, `balanceAfter = 405`, `previousTier = SILVER`, `currentTier = GOLD`, `tierChanged = true`.

#### Case 1.3 - Tích điểm khi đã là GOLD
* **Request**:
  ```json
  {
    "userId": 4,
    "bookingId": 2003,
    "eligibleAmount": 100000,
    "eventId": "evt-earn-003",
    "idempotencyKey": "idem-earn-003"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP `200 OK`. Áp dụng tỷ lệ tích hạng GOLD (7%): `100000 * 0.07 / 1000 = 7` điểm.
  - Phản hồi chứa: `pointChange = 7`, `balanceAfter = 412`, `currentTier = GOLD`, `tierChanged = false`.

#### Case 1.4 - Kiểm tra Idempotency (Lọc trùng)
* Gửi lại chính xác request của **Case 1.3**.
* **Kết quả mong đợi**: HTTP `200 OK`. Không cộng thêm điểm, không thêm lịch sử. Phản hồi giống hệt Case 1.3 kèm `"idempotent": true`.

#### Case 1.5 - Idempotency Conflict
* **Request** (cùng eventId/idempotencyKey nhưng khác user/booking):
  ```json
  {
    "userId": 9999,
    "bookingId": 9999,
    "eligibleAmount": 100000,
    "eventId": "evt-earn-003",
    "idempotencyKey": "idem-earn-003"
  }
  ```
* **Kết quả mong đợi**: HTTP `409 Conflict`, `errorCode = SCORE_IDEMPOTENCY_CONFLICT`.

#### Case 1.6 - Floor Rounding
* **Request**:
  ```json
  {
    "userId": 4,
    "bookingId": 2004,
    "eligibleAmount": 15000,
    "eventId": "evt-earn-004",
    "idempotencyKey": "idem-earn-004"
  }
  ```
* **Kết quả mong đợi**: HTTP `200 OK`. GOLD rate 7%: 15000 * 0.07 = 1050; 1050 / 1000 = 1.05 -> Làm tròn xuống = 1 điểm.
  - Phản hồi: `pointChange = 1`, `balanceAfter = 413`.

#### Case 1.7 - Validation Error
* **Request**:
  ```json
  {
    "userId": -4,
    "bookingId": 2005,
    "eligibleAmount": -15000,
    "eventId": "",
    "idempotencyKey": "idem-earn-005"
  }
  ```
* **Kết quả mong đợi**: HTTP `400 Bad Request`, `errorCode = VALIDATION_ERROR`.

#### Case 1.8 - Security Filter
* Không truyền hoặc truyền sai header `X-Internal-Token` khi gọi API.
* **Kết quả mong đợi**: HTTP `401 Unauthorized`, `errorCode = UNAUTHORIZED`, `message = Invalid internal token`.

---
# Hướng Hướng Dẫn Kiểm Thử và Mô Tả Kết Quả Triển Khai - Issue #137 (Score Redeem & Refund)

## 1. Hướng dẫn chạy Test tự động (Terminal Test)

Trong thư mục `server/score-service`, thực thi lệnh:

```bash
cd server/score-service
mvn clean test
```

## Kết quả mong đợi

- **Kết quả build:** `BUILD SUCCESS`
- **Tổng số test:** `44 tests run, 0 failures, 0 errors, 0 skipped`
  - Bao gồm:
    - **15** bài test JPA/Persistence
    - **13** bài test tích hợp luồng tích điểm (Earn)
    - **16** bài test tích hợp luồng tiêu điểm & hoàn điểm (Redeem & Refund) mới thêm.

---

## 2. Hướng dẫn Test tay chi tiết bằng Swagger (Manual Testing)

### Bước A: Chuẩn bị cấu hình Swagger và nạp điểm test
1. Mở Swagger của `score-service` (`http://localhost:8088/swagger-ui.html`).
2. Chọn **Authorize** ở góc trên bên phải, nhập `Bearer <accessToken>` của khách hàng (lấy từ auth-service) và token nội bộ `secret-internal-token` ở mục `internalAuth (apiKey)`.
3. Chuẩn bị dữ liệu: Nạp sẵn điểm vào database cho User ID = 4 là `100` điểm:
   ```sql
   UPDATE score_db.user_scores SET current_points = 100 WHERE user_id = 4;
   ```

---

### Bước B: Kiểm thử luồng Tiêu Điểm (POST `/internal/scores/redeem`)

> **Yêu cầu Header**: `X-Internal-Token: secret-internal-token`

#### Case 1.1 - Đổi điểm thành công (Happy Case)
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3001,
    "points": 40,
    "eventId": "SCORE-REDEEM-BOOKING-3001",
    "idempotencyKey": "REDEEM:BOOKING:3001"
  }
  ```
* **Kết quả mong đợi**: HTTP `200 OK`
  ```json
  {
    "success": true,
    "message": "Score redeemed successfully",
    "data": {
      "userId": 4,
      "bookingId": 3001,
      "redeemedPoints": 40,
      "redeemValue": 40000,
      "currentPoints": 60,
      "accumulatedPoints": 100,
      "historyId": 7004,
      "idempotent": false
    }
  }
  ```
  *(Database: `current_points` của user 4 giảm xuống 60. `accumulated_points` giữ nguyên 100. Tạo dòng lịch sử `REDEEM_FOR_BOOKING` điểm `-40`)*.

#### Case 1.2 - Đổi điểm lặp lại (Idempotency - Retry)
* Gửi lại chính xác request của **Case 1.1**.
* **Kết quả mong đợi**: HTTP `200 OK`
  ```json
  {
    "success": true,
    "message": "Score redeem event was already processed",
    "data": {
      "userId": 4,
      "bookingId": 3001,
      "redeemedPoints": 40,
      "currentPoints": 60,
      "historyId": 7004,
      "idempotent": true
    }
  }
  ```

#### Case 1.3 - Xung đột Idempotency (Idempotency Conflict)
* Gửi request giữ nguyên `eventId` và `idempotencyKey` của Case 1.1 nhưng đổi `bookingId` sang `3002`.
  ```json
  {
    "userId": 4,
    "bookingId": 3002,
    "points": 40,
    "eventId": "SCORE-REDEEM-BOOKING-3001",
    "idempotencyKey": "REDEEM:BOOKING:3001"
  }
  ```
* **Kết quả mong đợi**: HTTP `409 Conflict`
  ```json
  {
    "success": false,
    "message": "Idempotency conflict: event or key is already used for another request context",
    "errorCode": "SCORE_IDEMPOTENCY_CONFLICT",
    "data": null,
    "errors": null
  }
  ```

#### Case 1.4 - Số dư điểm không đủ (Insufficient Balance)
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3003,
    "points": 200,
    "eventId": "SCORE-REDEEM-BOOKING-3003",
    "idempotencyKey": "REDEEM:BOOKING:3003"
  }
  ```
* **Kết quả mong đợi**: HTTP `409 Conflict`
  ```json
  {
    "success": false,
    "message": "Insufficient score balance",
    "errorCode": "SCORE_INSUFFICIENT_BALANCE",
    "data": {
      "availablePoints": 60,
      "requestedPoints": 200
    },
    "errors": null
  }
  ```

#### Case 1.5 - Lỗi Dữ liệu đầu vào (Validation Error)
* **Request (Body JSON)** (ví dụ điểm âm và thiếu eventId):
  ```json
  {
    "userId": 4,
    "bookingId": 3004,
    "points": -10,
    "eventId": "",
    "idempotencyKey": "REDEEM:BOOKING:3004"
  }
  ```
* **Kết quả mong đợi**: HTTP `400 Bad Request`
  ```json
  {
    "success": false,
    "message": "Validation failed",
    "errorCode": "VALIDATION_ERROR",
    "data": null,
    "errors": [
      {
        "field": "points",
        "message": "Points must be greater than zero"
      },
      {
        "field": "eventId",
        "message": "Event ID must be specified"
      }
    ]
  }
  ```

---

### Bước C: Kiểm thử luồng Hoàn Điểm (POST `/internal/scores/refund-redeem`)

> **Yêu cầu Header**: `X-Internal-Token: secret-internal-token`

#### Case 2.1 - Hoàn điểm thành công (Happy Case)
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3001,
    "points": 40,
    "originalRedeemEventId": "SCORE-REDEEM-BOOKING-3001",
    "eventId": "SCORE-REFUND-BOOKING-3001",
    "idempotencyKey": "REFUND_REDEEM:BOOKING:3001",
    "reason": "Booking cancelled"
  }
  ```
* **Kết quả mong đợi**: HTTP `200 OK`
  ```json
  {
    "success": true,
    "message": "Redeemed score refunded successfully",
    "data": {
      "userId": 4,
      "bookingId": 3001,
      "refundedPoints": 40,
      "currentPoints": 100,
      "accumulatedPoints": 100,
      "originalHistoryId": 7004,
      "historyId": 7005,
      "idempotent": false
    }
  }
  ```
  *(Database: `current_points` của user 4 tăng từ 60 về lại 100. Tạo dòng lịch sử `REFUND_REDEEM` điểm `+40` liên kết đến ID giao dịch gốc `7004`)*.

#### Case 2.2 - Hoàn điểm lặp lại (Idempotency - Retry)
* Gửi lại chính xác request của **Case 2.1**.
* **Kết quả mong đợi**: HTTP `200 OK`
  ```json
  {
    "success": true,
    "message": "Redeemed score refund was already processed",
    "data": {
      "userId": 4,
      "bookingId": 3001,
      "refundedPoints": 40,
      "currentPoints": 100,
      "accumulatedPoints": 100,
      "originalHistoryId": 7004,
      "historyId": 7005,
      "idempotent": true
    }
  }
  ```

#### Case 2.3 - Giao dịch gốc không tồn tại (Original Redeem Not Found)
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3001,
    "points": 40,
    "originalRedeemEventId": "SCORE-REDEEM-NON-EXISTENT",
    "eventId": "SCORE-REFUND-BOOKING-3002",
    "idempotencyKey": "REFUND_REDEEM:BOOKING:3002",
    "reason": "Cancel"
  }
  ```
* **Kết quả mong đợi**: HTTP `404 Not Found`
  ```json
  {
    "success": false,
    "message": "Original redeem transaction not found",
    "errorCode": "SCORE_ORIGINAL_TRANSACTION_NOT_FOUND",
    "data": null,
    "errors": null
  }
  ```

#### Case 2.4 - Sai lệch thông tin người dùng hoặc đơn vé (Mismatched context)
* **Request (Body JSON)** (ví dụ sai bookingId):
  ```json
  {
    "userId": 4,
    "bookingId": 9999,
    "points": 40,
    "originalRedeemEventId": "SCORE-REDEEM-BOOKING-3001",
    "eventId": "SCORE-REFUND-BOOKING-3003",
    "idempotencyKey": "REFUND_REDEEM:BOOKING:3003",
    "reason": "Cancel"
  }
  ```
* **Kết quả mong đợi**: HTTP `400 Bad Request`
  ```json
  {
    "success": false,
    "message": "Original transaction user or booking mismatch",
    "errorCode": "SCORE_TRANSACTION_MISMATCH",
    "data": null,
    "errors": null
  }
  ```

#### Case 2.5 - Số điểm hoàn vượt quá số điểm đã đổi (Refund Exceeds Redeemed)
* **Request (Body JSON)** (yêu cầu hoàn 100 điểm khi gốc chỉ đổi 40):
  ```json
  {
    "userId": 4,
    "bookingId": 3001,
    "points": 100,
    "originalRedeemEventId": "SCORE-REDEEM-BOOKING-3001",
    "eventId": "SCORE-REFUND-BOOKING-3004",
    "idempotencyKey": "REFUND_REDEEM:BOOKING:3004",
    "reason": "Cancel"
  }
  ```
* **Kết quả mong đợi**: HTTP `400 Bad Request`
  ```json
  {
    "success": false,
    "message": "Refund points exceeds originally redeemed points",
    "errorCode": "SCORE_INVALID_REFUND_AMOUNT",
    "data": null,
    "errors": null
  }
  ```

#### Case 2.6 - Chặn hoàn điểm hai lần (Double Refund Prevention)
* Gửi yêu cầu hoàn điểm mới (khác eventId và idempotencyKey) cho cùng đơn hàng đã hoàn ở Case 2.1.
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3001,
    "points": 40,
    "originalRedeemEventId": "SCORE-REDEEM-BOOKING-3001",
    "eventId": "SCORE-REFUND-BOOKING-3001-NEW",
    "idempotencyKey": "REFUND_REDEEM:BOOKING:3001:NEW",
    "reason": "Duplicate cancel call"
  }
  ```
* **Kết quả mong đợi**: HTTP `409 Conflict`
  ```json
  {
    "success": false,
    "message": "Redeem transaction has already been refunded",
    "errorCode": "SCORE_ALREADY_REFUNDED",
    "data": null,
    "errors": null
  }
  ```

#### Case 2.7 - Lỗi Bảo mật API (Security Filter block)
* Không truyền header `X-Internal-Token` khi gọi endpoint hoàn điểm.
* **Kết quả mong đợi**: HTTP `401 Unauthorized`
  ```json
  {
    "success": false,
    "message": "Invalid internal token",
    "errorCode": "UNAUTHORIZED",
    "data": null,
    "errors": null
  }
  ```

---

### Bước D: Kiểm thử luồng Thu Hồi Điểm Earn (POST `/internal/scores/revoke-earn` - Issue #138)

> **Yêu cầu Header**: `X-Internal-Token: secret-internal-token`

#### Case 3.1 - Thu hồi đủ điểm thành công (Full Revoke - Happy Case)
* **Tình huống**: User đã earn 50 điểm từ booking `3005`. Hiện tại `currentPoints >= 50`.
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3005,
    "points": 50,
    "originalEarnEventId": "SCORE-EARN-BOOKING-3005",
    "eventId": "SCORE-REVOKE-BOOKING-3005",
    "idempotencyKey": "REVOKE_EARN:BOOKING:3005",
    "reason": "Payment refunded"
  }
  ```
* **Kết quả mong đợi**: HTTP `201 Created`
  ```json
  {
    "success": true,
    "message": "Earned score revoked successfully",
    "data": {
      "userId": 4,
      "bookingId": 3005,
      "requestedPoints": 50,
      "deductedPoints": 50,
      "outstandingPoints": 0,
      "currentPoints": 50,
      "accumulatedPoints": 50,
      "previousTier": "SILVER",
      "currentTier": "SILVER",
      "tierChanged": false,
      "historyId": 7006,
      "reconciliationStatus": "NONE",
      "requiresManualReconciliation": false,
      "idempotent": false
    }
  }
  ```

#### Case 3.2 - Thu hồi một phần do không đủ điểm khả dụng (Partial Revoke - Pending Reconciliation)
* **Tình huống**: User earn 100 điểm, nhưng đã dùng hết 80 điểm (chỉ còn `currentPoints = 20`). Thu hồi 100 điểm.
* **Request (Body JSON)**:
  ```json
  {
    "userId": 4,
    "bookingId": 3006,
    "points": 100,
    "originalEarnEventId": "SCORE-EARN-BOOKING-3006",
    "eventId": "SCORE-REVOKE-BOOKING-3006",
    "idempotencyKey": "REVOKE_EARN:BOOKING:3006",
    "reason": "Payment refunded"
  }
  ```
* **Kết quả mong đợi**: HTTP `201 Created`
  ```json
  {
    "success": true,
    "message": "Earned score revoked successfully",
    "data": {
      "userId": 4,
      "bookingId": 3006,
      "requestedPoints": 100,
      "deductedPoints": 20,
      "outstandingPoints": 80,
      "currentPoints": 0,
      "accumulatedPoints": 0,
      "previousTier": "SILVER",
      "currentTier": "SILVER",
      "tierChanged": false,
      "historyId": 7007,
      "reconciliationStatus": "PENDING",
      "requiresManualReconciliation": true,
      "idempotent": false
    }
  }
  ```
  *(Database: `current_points` về `0` (không bao giờ âm). `outstanding_points` = 80, `reconciliation_status` = `PENDING`)*.

#### Case 3.3 - Thu hồi làm giảm hạng thành viên (Tier Downgrade)
* **Tình huống**: User có `accumulatedPoints = 500` (đang ở hạng `GOLD`). Thu hồi 150 điểm khiến `accumulatedPoints` giảm xuống 350 (< 400).
* **Kết quả mong đợi**: HTTP `201 Created`, trường `previousTier` là `"GOLD"`, `currentTier` là `"SILVER"`, và `tierChanged` là `true`.

#### Case 3.4 - Thu hồi lặp lại (Idempotency - Retry)
* Gửi lại request của Case 3.1 hoặc 3.2.
* **Kết quả mong đợi**: HTTP `200 OK`, `idempotent = true`, không trừ thêm điểm hay tạo thêm history.

#### Case 3.5 - Xung đột Idempotency (Idempotency Conflict)
* Gửi request với cùng `idempotencyKey` hoặc `eventId` nhưng số điểm `points` khác.
* **Kết quả mong đợi**: HTTP `409 Conflict`, mã lỗi `SCORE_IDEMPOTENCY_CONFLICT`.

---
# Hướng dẫn Kiểm thử và Mô tả Kết quả Triển khai - Issue #139 (Admin APIs)

## 1. Mở Swagger của `score-service`
* Hãy đăng nhập bằng tài khoản có quyền Admin (hoặc thêm các authority: `SCORE_READ`, `SCORE_ADJUST`, `SCORE_MANAGE`, `MEMBERSHIP_TIER_READ`, `MEMBERSHIP_TIER_MANAGE` vào token của bạn).
* Thiết lập Bearer token trong Swagger Authorize.

## 2. Các kịch bản kiểm thử API Admin Score

### 2.1. API: `GET /api/admin/scores/users/{userId}` (Get User Score Detail)
* **Mục tiêu**: Lấy chi tiết điểm và hạng thẻ của một user.
* **Kịch bản**: Nhập `userId` của tài khoản đã tồn tại trong database (ví dụ: `4`).
* **Kết quả mong đợi**: HTTP `200 OK`, trả về đầy đủ các thông tin: `userId`, `currentPoints`, `accumulatedPoints`, `currentTier`, `nextTier`, `updatedAt`.
* **Kịch bản lỗi**: Nhập `userId` không tồn tại (ví dụ: `9999`).
* **Kết quả mong đợi**: HTTP `404 Not Found`, `errorCode = "SCORE_ACCOUNT_NOT_FOUND"`.

### 2.2. API: `GET /api/admin/scores/users/{userId}/history` (Get User Score History)
* **Mục tiêu**: Xem lịch sử giao dịch điểm của user với nhiều tham số lọc.
* **Kịch bản**: Nhập `userId = 4`. Thiết lập các tham số lọc mong muốn (ví dụ: `page = 0`, `size = 10`, `sort = createdAt,desc`).
* **Kết quả mong đợi**: HTTP `200 OK`, trả về danh sách lịch sử điểm bao gồm đầy đủ thông tin: `createdBy`, `requestId`, `reason`, `requestedPointChange`, `outstandingPoints`, `reconciliationStatus`.
* **Kịch bản lỗi**: Sắp xếp theo một cột không thuộc danh sách whitelist (ví dụ: `sort = invalidColumn,desc`).
* **Kết quả mong đợi**: HTTP `400 Bad Request`, `errorCode = "SCORE_INVALID_QUERY"`.

### 2.3. API: `POST /api/admin/scores/users/{userId}/adjustments` (Manual Score Adjustment)
* **Mục tiêu**: Cộng hoặc trừ điểm thủ công cho user.
* **Case 2.3.1: Cộng điểm thủ công không ảnh hưởng hạng (MANUAL_ADD - affectAccumulatedPoints = false)**
  * **Request Body**:
    ```json
    {
      "adjustmentType": "ADD",
      "points": 100,
      "affectAccumulatedPoints": false,
      "reason": "Customer support compensation",
      "requestId": "REQ-ADJUST-ADD-001"
    }
    ```
  * **Kết quả mong đợi**: HTTP `201 Created`, `currentPoints` tăng thêm 100, `accumulatedPoints` giữ nguyên, `idempotent = false`.
* **Case 2.3.2: Cộng điểm thủ công nâng hạng (MANUAL_ADD - affectAccumulatedPoints = true)**
  * **Request Body**:
    ```json
    {
      "adjustmentType": "ADD",
      "points": 400,
      "affectAccumulatedPoints": true,
      "reason": "Bonus points for special upgrade",
      "requestId": "REQ-ADJUST-ADD-002"
    }
    ```
  * **Kết quả mong đợi**: HTTP `201 Created`, `currentPoints` và `accumulatedPoints` cùng tăng 400. Hạng thành viên của user được nâng lên `GOLD` (trường `tierChanged = true`).
* **Case 2.3.3: Trừ điểm thủ công làm giảm hạng (MANUAL_DEDUCT - affectAccumulatedPoints = true)**
  * **Request Body**:
    ```json
    {
      "adjustmentType": "DEDUCT",
      "points": 400,
      "affectAccumulatedPoints": true,
      "reason": "Correction of duplicate points",
      "requestId": "REQ-ADJUST-DED-001"
    }
    ```
  * **Kết quả mong đợi**: HTTP `201 Created`, `currentPoints` và `accumulatedPoints` cùng giảm 400. Hạng thành viên của user quay về `SILVER` (trường `tierChanged = true`).
* **Case 2.3.4: Trừ điểm thủ công gây số dư âm (Balance Underflow)**
  * Gửi request trừ số điểm lớn hơn số dư hiện tại của user.
  * **Kết quả mong đợi**: HTTP `409 Conflict`, `errorCode = "SCORE_BALANCE_WOULD_BE_NEGATIVE"`.
* **Case 2.3.5: Idempotency (Retry)**
  * Gửi lại đúng request của Case 2.3.1.
  * **Kết quả mong đợi**: HTTP `200 OK`, `idempotent = true`, không ghi nhận cộng điểm lần hai.
* **Case 2.3.6: Idempotency Conflict**
  * Gửi request có cùng `requestId` của Case 2.3.1 nhưng đổi `points` thành `200`.
  * **Kết quả mong đợi**: HTTP `409 Conflict`, `errorCode = "SCORE_ADJUSTMENT_IDEMPOTENCY_CONFLICT"`.

### 2.4. API: `POST /api/admin/scores/users/{userId}/recalculate-tier` (Recalculate User Tier)
* **Mục tiêu**: Ép buộc tính toán và cập nhật lại hạng thành viên cho user dựa trên điểm tích lũy hiện có.
* **Kịch bản**: Gọi API cho user.
* **Kết quả mong đợi**: HTTP `200 OK`, trả về thông tin `previousTier`, `currentTier` và `tierChanged`.

---

## 3. Các kịch bản kiểm thử API Admin Membership Tier

### 3.1. API: `POST /api/admin/membership-tiers` (Create Tier)
* **Mục tiêu**: Tạo hạng thành viên mới.
* **Request Body**:
  ```json
  {
    "tierName": "PLATINUM",
    "minPoints": 1500,
    "earningRate": 0.12,
    "description": "Platinum VIP member"
  }
  ```
* **Kết quả mong đợi**: HTTP `201 Created`, tạo thành công tier `PLATINUM`.
* **Kịch bản lỗi (Trùng tên)**: Gửi lại request trên với tên `"PLATINUM"`.
* **Kết quả mong đợi**: HTTP `409 Conflict`, `errorCode = "SCORE_TIER_NAME_ALREADY_EXISTS"`.
* **Kịch bản lỗi (Trùng ngưỡng điểm)**: Gửi request tạo tier mới với `minPoints = 400` (đã trùng với GOLD).
* **Kết quả mong đợi**: HTTP `409 Conflict`, `errorCode = "SCORE_TIER_THRESHOLD_CONFLICT"`.

### 3.2. API: `GET /api/admin/membership-tiers` (List Tiers)
* **Kết quả mong đợi**: HTTP `200 OK`, trả về danh sách tất cả các tier đã sắp xếp tăng dần theo `minPoints`. Mỗi tier đi kèm trường `userCount` thể hiện số lượng user hiện tại ở hạng đó.

### 3.3. API: `GET /api/admin/membership-tiers/{tierId}` (Get Detail)
* **Kết quả mong đợi**: HTTP `200 OK`, hiển thị thông tin chi tiết hạng thành viên của `{tierId}`.

### 3.4. API: `PUT /api/admin/membership-tiers/{tierId}` (Update Tier)
* **Mục tiêu**: Cập nhật cấu hình hạng thành viên.
* **Case 3.4.1: Cập nhật thông số bình thường (không đổi minPoints)**
  * Gửi PUT request cập nhật `earningRate` hoặc `description`.
  * **Kết quả mong đợi**: HTTP `200 OK`, `recalculationRequired = false`.
* **Case 3.4.2: Cập nhật thay đổi ngưỡng điểm (đổi minPoints)**
  * Gửi PUT request thay đổi `minPoints` (ví dụ từ 400 thành 500).
  * **Kết quả mong đợi**: HTTP `200 OK`, `recalculationRequired = true` (không chạy bulk update bất đồng bộ chặn thread).
* **Case 3.4.3: Vi phạm bảo vệ Lowest Tier**
  * Gửi PUT request cập nhật tier `SILVER` (đang có `minPoints = 0`) thành `minPoints = 100`.
  * **Kết quả mong đợi**: HTTP `409 Conflict`, `errorCode = "SCORE_TIER_CONFIGURATION_INVALID"` (do hệ thống không cho phép sửa hạng thẻ duy nhất có minPoints = 0 thành giá trị khác 0).