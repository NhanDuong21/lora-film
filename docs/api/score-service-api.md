# Score Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                                                        |
| -------------- | ------------------------------------------------------------------------------- |
| Service        | `score-service`                                                                 |
| Feature        | Loyalty Point, Membership Tier and Score History Management                     |
| API liên quan  | Score Balance, Score History, Membership Tier, Earn, Redeem, Refund, Adjustment |
| Contract Owner | Dương Thiện Nhân                                                                |
| Backend Owner  | Trương Hoàng Khang                                                              |
| Reviewer       | Trương Hoàng Khang                                                              |
| Trạng thái     | Updated after Owner Review / Ready for Re-review                                |
| Milestone      | Sprint 2 - Core Service API Foundation                                          |
| Ngày cập nhật  | 22/06/2026                                                                      |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả API Contract cho `score-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Thống nhất contract giữa Frontend, API Gateway, Booking Service, Payment Service và Score Service.
* Làm cơ sở implement loyalty point, membership tier và score history.
* Xác định rõ cách cộng điểm, trừ điểm, hoàn điểm và điều chỉnh điểm.
* Bảo đảm mọi thay đổi số dư đều có history.
* Không cho Frontend tự tính hoặc tự quyết định số điểm được cộng.
* Xác định rõ idempotency theo booking hoặc event.
* Xác định rõ cách tính membership tier.
* Chuẩn hóa endpoint, request, response, validation, HTTP status và error code.
* Ghi rõ các điểm có thể chưa khớp với schema Sprint 0 để service owner review.
* Làm cơ sở tách implementation issue sau khi contract được duyệt.

---

## 3. Clarify Naming

Trong hệ thống LoraFilm:

```txt
Score = điểm thưởng / loyalty point
```

Score Service quản lý:

* Điểm khả dụng.
* Tổng điểm tích lũy.
* Hạng thành viên.
* Lịch sử giao dịch điểm.

Score Service không quản lý:

```txt
Movie rating
Movie review
Movie score
Star rating
Comment hoặc đánh giá phim
```

Nếu hệ thống cần đánh giá phim, phải thuộc Movie Service hoặc một Review Service riêng trong tương lai.

---

## 4. Phạm Vi Score Service

Score Service chịu trách nhiệm:

* Quản lý danh sách membership tier.
* Quản lý số điểm khả dụng của user.
* Quản lý tổng điểm tích lũy.
* Xác định tier hiện tại.
* Cộng điểm sau booking/payment thành công.
* Trừ điểm khi user đổi thưởng hoặc sử dụng điểm.
* Hoàn điểm khi giao dịch redeem bị rollback.
* Thu hồi điểm đã cộng nếu booking bị hoàn tiền hoặc hủy hợp lệ.
* Ghi score history cho mọi biến động.
* Điều chỉnh điểm nội bộ bởi admin.
* Bảo đảm số dư không âm.
* Bảo đảm earn/redeem/refund idempotent.
* Recalculate membership tier.

Score Service không chịu trách nhiệm:

* Quản lý user profile.
* Quản lý booking.
* Quản lý payment.
* Quản lý promotion.
* Quản lý movie rating/review.
* Truy cập trực tiếp database của User, Booking hoặc Payment Service.
* Tự xác nhận payment thành công.
* Tính final payment amount.

---

## 5. Physical Schema Sprint 0

### 5.1. Bảng `membership_tiers`

```sql
CREATE TABLE `membership_tiers` (
  `id` int PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Tier ID',
  `tier_name` varchar(50) UNIQUE NOT NULL COMMENT 'SILVER, GOLD, DIAMOND',
  `min_points` int NOT NULL COMMENT 'So diem toi thieu de dat duoc hang nay, e.g., 0, 200, 500',
  `earning_rate` decimal(5,2) NOT NULL DEFAULT 0.05 COMMENT 'Ty le tich diem, e.g., hang Vang duoc tich 7% gia tri don hang',
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);
```

### 5.2. Bảng `user_scores`

```sql
CREATE TABLE `user_scores` (
  `user_id` bigint PRIMARY KEY COMMENT 'Shared Primary Key - Logical Ref sang users.account_id cua User Service',
  `current_points` int NOT NULL DEFAULT 0 COMMENT 'So diem kha dung hien tai de doi qua',
  `accumulated_points` int NOT NULL DEFAULT 0 COMMENT 'Tong diem da tich luy trong doi de xet hang thanh vien',
  `current_tier_id` int NOT NULL DEFAULT 1,
  `updated_at` timestamp DEFAULT (now())
);
```

### 5.3. Bảng `score_history`

```sql
CREATE TABLE `score_history` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `booking_id` bigint COMMENT 'Nullable neu la doi qua, Logical Ref sang bookings.id cua Booking Service',
  `point_change` int NOT NULL COMMENT 'Gia tri diem bien dong, e.g., +15 hoac -50',
  `transaction_type` varchar(30) NOT NULL COMMENT 'EARN_BY_BOOKING, SPEND_FOR_REWARD, EXPIRED',
  `description` text COMMENT 'Chi tiet su kien, e.g., Tich diem tu don hang LORAFILM-123',
  `created_at` timestamp DEFAULT (now())
);
```

### 5.4. Quan hệ nội bộ

```sql
ALTER TABLE `user_scores`
ADD FOREIGN KEY (`current_tier_id`)
REFERENCES `membership_tiers` (`id`);

ALTER TABLE `score_history`
ADD FOREIGN KEY (`user_id`)
REFERENCES `user_scores` (`user_id`)
ON DELETE CASCADE;
```

---

## 6. Phân Tích Schema Hiện Tại

### 6.1. Nghiệp vụ schema hỗ trợ

Schema hiện tại hỗ trợ:

* Định nghĩa nhiều membership tier.
* Thiết lập ngưỡng điểm tối thiểu cho tier.
* Thiết lập earning rate theo tier.
* Quản lý điểm khả dụng.
* Quản lý điểm tích lũy.
* Lưu tier hiện tại.
* Ghi lịch sử cộng/trừ điểm.
* Liên kết score history với booking nếu có.

### 6.2. Ý nghĩa của các loại điểm

#### `current_points`

```txt
Điểm khả dụng hiện tại
```

Điểm này:

* Tăng khi user earn point.
* Giảm khi user redeem point.
* Tăng lại khi hoàn điểm redeem.
* Có thể giảm khi thu hồi điểm từ booking refund.
* Không được âm.

#### `accumulated_points`

```txt
Tổng điểm tích lũy dùng để xét tier
```

Contract Sprint 2 mặc định:

* Tăng khi earn point hợp lệ.
* Không giảm khi user redeem point.
* Có thể giảm khi điểm earn bị thu hồi do refund/cancel.
* Không tự giảm do điểm khả dụng hết hạn nếu tier dùng lifetime points.

Quyết định chính thức cho Sprint 2:

```txt
accumulatedPoints = Lifetime Accumulated Points
```

Không áp dụng membership cycle, tier expiration hoặc reset schedule trong Sprint 2.

### 6.3. Giới hạn schema hiện tại

Schema chưa có:

```txt
score_history.balance_before
score_history.balance_after
score_history.accumulated_before
score_history.accumulated_after
score_history.idempotency_key
score_history.reference_history_id
score_history.payment_id
score_history.reward_id
score_history.created_by
score_history.expired_at

user_scores.version
membership_tiers.max_points
membership_tiers.is_active
membership_tiers.benefit_description
```

### 6.4. Vấn đề idempotency

Schema hiện tại không có unique field để ngăn:

```txt
Cộng điểm hai lần cho cùng booking
```

Chỉ có `booking_id`, nhưng không unique và có thể được dùng cho cả earn lẫn refund.

Nếu Sprint 2 cần bảo đảm idempotency mạnh, khuyến nghị thêm:

```txt
score_history.idempotency_key
```

và unique constraint.

Ví dụ:

```txt
EARN:BOOKING:1001
REFUND:BOOKING:1001
REDEEM:BOOKING:1001:REQUEST-ABC
```

Nếu không sửa schema, implementation phải kiểm tra theo:

```txt
booking_id + transaction_type
```

nhưng cách này yếu hơn và khó hỗ trợ nhiều nghiệp vụ trên cùng booking.

---

## 7. Database-per-Service và Logical Reference

Các field sau là logical references:

```txt
user_scores.user_id
score_history.user_id
score_history.booking_id
```

Score Service:

* Không tạo foreign key vật lý sang User hoặc Booking database.
* Không truy cập trực tiếp database của service khác.
* Không đọc Payment database.
* Phải nhận dữ liệu qua Internal API hoặc event.
* Không tự xác nhận booking/payment thành công.

### Source of truth

| Dữ liệu                       | Source of truth   |
| ----------------------------- | ----------------- |
| User identity                 | User/Auth Service |
| Booking information           | Booking Service   |
| Paid amount và payment status | Payment Service   |
| Loyalty point balance         | Score Service     |
| Membership tier               | Score Service     |
| Promotion discount            | Promotion Service |

---

## 8. API Gateway và Service URL

### 8.1. API Gateway

Frontend chỉ gọi:

```txt
http://localhost:8080
```

### 8.2. Score Service Direct URL

Chỉ dùng cho debug hoặc backend integration:

```txt
http://localhost:8088
```

Port chính thức lấy từ cấu hình project.

### 8.3. Customer Query Flow

```txt
React Frontend
→ API Gateway
→ Score Service
→ Score Database
```

### 8.4. Earn Flow

```txt
Payment SUCCESS
→ Payment/Booking Service gửi Internal API hoặc Event
→ Score Service tính điểm
→ Update user_scores
→ Insert score_history
→ Recalculate tier
```

---

## 9. Quy Ước Chung

### 9.1. Protected API Header

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

### 9.2. Internal API Header

```http
X-Internal-Token: <internal-token>
Content-Type: application/json
```

### 9.3. Admin API Header

```http
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

### 9.4. Datetime Format

```txt
ISO-8601
YYYY-MM-DDTHH:mm:ss
```

### 9.5. Timezone

```txt
Asia/Ho_Chi_Minh
```

### 9.6. Point Format

Score là số nguyên:

```json
{
  "currentPoints": 150,
  "accumulatedPoints": 350
}
```

Không sử dụng point dạng decimal trong Sprint 2.

---

## 10. Common Response Contract

### 10.1. Success

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

### 10.2. Error

```json
{
  "success": false,
  "message": "Operation failed",
  "errorCode": "ERROR_CODE",
  "data": null,
  "errors": null
}
```

### 10.3. Validation Error

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
    }
  ]
}
```

---

## 11. Transaction Type Definitions

### 11.1. ScoreTransactionType chính thức

```txt
EARN_BY_BOOKING
REDEEM_FOR_BOOKING
REFUND_REDEEM
REVOKE_EARN_BY_REFUND
MANUAL_ADD
MANUAL_DEDUCT
EXPIRED
```

### 11.2. Ý nghĩa

| Transaction Type      | Point Change | Mô tả                                       |
| --------------------- | -----------: | ------------------------------------------- |
| EARN_BY_BOOKING       |        Dương | Cộng điểm sau booking/payment thành công    |
| REDEEM_FOR_BOOKING    |           Âm | Dùng điểm để giảm giá booking               |
| REFUND_REDEEM         |        Dương | Hoàn lại điểm đã redeem                     |
| REVOKE_EARN_BY_REFUND |           Âm | Thu hồi điểm đã cộng vì booking được refund |
| MANUAL_ADD            |        Dương | Admin cộng điểm thủ công                    |
| MANUAL_DEDUCT         |           Âm | Admin trừ điểm thủ công                     |
| EXPIRED               |           Âm | Điểm hết hạn                                |

Schema comment hiện chỉ ghi:

```txt
EARN_BY_BOOKING
SPEND_FOR_REWARD
EXPIRED
```

Bộ enum trên là chính thức cho Sprint 2.

Không tiếp tục sử dụng:

```txt
SPEND_FOR_REWARD
```

Tên chuẩn là:

```txt
REDEEM_FOR_BOOKING
```

---

## 12. Membership Tier Rules

### 12.1. Tier Calculation

Tier hiện tại được xác định bằng:

```txt
Tier có minPoints lớn nhất
mà minPoints <= accumulatedPoints
```

Ví dụ:

| Tier    | Min Points | Earning Rate |
| ------- | ---------: | -----------: |
| SILVER  |          0 |         0.05 |
| GOLD    |        200 |         0.07 |
| DIAMOND |        500 |         0.10 |

User có:

```txt
accumulatedPoints = 350
```

thì tier là:

```txt
GOLD
```

### 12.2. Earning Rate

Earning rate được lấy từ tier hiện tại tại thời điểm earn.

Ví dụ:

```txt
paidAmount = 240000 VND
earningRate = 0.05
```

Không nên tính trực tiếp:

```txt
240000 × 0.05 = 12000 điểm
```

nếu 1 điểm không tương đương 1 VND.

Cần có conversion unit rõ ràng.

Công thức chính thức cho Sprint 2:

```txt
earnedPoints =
floor((eligibleAmount / 1000) × earningRate × 100)
```

Công thức này tương đương:

```txt
earnedPoints = floor(eligibleAmount × earningRate / 1000)
```

Ví dụ:

```txt
eligibleAmount = 240000
earningRate = 0.05

earnedPoints = floor(240000 × 0.05 / 1000)
             = floor(12)
             = 12 điểm
```

Đây là công thức chính thức cho Sprint 2.

### 12.3. Eligible Amount

Quyết định chính thức:

```txt
eligibleAmount = số tiền thực trả bằng tiền sau promotion và sau phần giá trị được thanh toán bằng score
```

Không cộng điểm trên giá gốc trước giảm giá.

Payment Service hoặc Booking Service phải gửi amount đã được xác nhận.

### 12.4. Tier Upgrade

Sau mỗi earn/revoke/manual adjustment ảnh hưởng accumulated points:

```txt
Recalculate tier
```

Nếu tier thay đổi:

* Update `current_tier_id`.
* Có thể publish `MEMBERSHIP_TIER_CHANGED`.
* Không tự gửi notification trong Score Service.

### 12.5. Tier Downgrade

Sprint 2 cho phép tier downgrade khi `accumulatedPoints` giảm do `REVOKE_EARN_BY_REFUND` hoặc admin adjustment có ảnh hưởng accumulated points.

Ví dụ:

```txt
600 points → DIAMOND
Revoke 400 accumulated points
200 points → GOLD
```

---

## 13. Score Balance Rules

### 13.1. Non-negative Balance

Luôn bảo đảm:

```txt
currentPoints >= 0
accumulatedPoints >= 0
```

### 13.2. Redeem Rule

```txt
redeemPoints <= currentPoints
```

### 13.3. Current và Accumulated Point Behavior

| Operation              | currentPoints |                     accumulatedPoints |
| ---------------------- | ------------: | ------------------------------------: |
| Earn booking           |          Tăng |                                  Tăng |
| Redeem                 |          Giảm |                             Không đổi |
| Refund redeemed points |          Tăng |                             Không đổi |
| Revoke earned points   |          Giảm |                        Giảm theo rule |
| Manual add             |          Tăng |                   Tùy loại adjustment |
| Manual deduct          |          Giảm |                   Tùy loại adjustment |
| Expired                |          Giảm | Không đổi nếu accumulated là lifetime |

Behavior của `affectAccumulatedPoints` trong Admin Adjustment vẫn cần Score Service Owner xác nhận trước khi contract được approve. Nếu accumulated points được phép thay đổi, bắt buộc recalculate tier.

---

## 14. Production Readiness Decisions

Các quyết định sau đã được Score Service Owner chốt cho Sprint 2:

| Nội dung | Quyết định |
|---|---|
| `accumulatedPoints` | Lifetime accumulated points |
| Tier downgrade | Có |
| Redeem giảm accumulated | Không |
| Refund redeem tăng accumulated | Không |
| Revoke earn giảm accumulated | Có |
| Balance âm | Không cho phép |
| Earn trên phần thanh toán bằng điểm | Không |
| 1 point | 1.000 VND |
| Earn formula | `floor(eligibleAmount × earningRate / 1000)` |
| Database-level idempotency | Bắt buộc |
| Balance audit snapshot | Bắt buộc |
| Reference transaction | Bắt buộc cho refund/revoke |
| Admin adjustment audit | Bắt buộc |

## 14.1. Database-level Idempotency

Mọi transaction thay đổi score phải có `idempotencyKey` và được bảo vệ bằng unique constraint tại database.

```txt
score_history.idempotency_key UNIQUE NOT NULL
```

Duplicate event/request không được thay đổi balance lần hai.

## 14.2. Audit Snapshot

Mọi `score_history` record phải lưu:

```txt
balanceBefore
balanceAfter
accumulatedBefore
accumulatedAfter
```

Balance update và history insert phải nằm trong cùng transaction.

## 14.3. Original Transaction Reference

`REFUND_REDEEM` và `REVOKE_EARN_BY_REFUND` phải tham chiếu transaction gốc bằng:

```txt
referenceHistoryId
```

## 14.4. Admin Adjustment Audit

Admin adjustment phải lưu:

```txt
createdBy
requestId
reason
```

`requestId` phải idempotent.

## 14.5. Redeem Concurrency

Không dùng flow `SELECT → check → UPDATE` nếu không lock.

Hướng bắt buộc:

```sql
UPDATE user_scores
SET current_points = current_points - :points
WHERE user_id = :userId
  AND current_points >= :points;
```

Nếu affected rows bằng `0`, trả `SCORE_INSUFFICIENT_BALANCE`.

---

## 15. API Classification

### 15.1. Public APIs

```txt
GET /api/membership-tiers
```

### 15.2. Protected Customer APIs

```txt
GET  /api/scores/me
GET  /api/scores/me/history
GET  /api/scores/me/tier
POST /api/scores/me/redeem-preview
```

Customer không được tự gọi earn, redeem commit, refund hoặc adjustment.

### 15.3. Internal APIs

```txt
POST /internal/scores/earn
POST /internal/scores/redeem
POST /internal/scores/refund-redeem
POST /internal/scores/revoke-earn
POST /internal/scores/users/{userId}/recalculate-tier
GET  /internal/scores/users/{userId}
```

### 15.4. Admin APIs

```txt
GET   /api/admin/scores/users/{userId}
GET   /api/admin/scores/users/{userId}/history
POST  /api/admin/scores/users/{userId}/adjustments
POST  /api/admin/scores/users/{userId}/recalculate-tier

POST  /api/admin/membership-tiers
GET   /api/admin/membership-tiers
GET   /api/admin/membership-tiers/{tierId}
PUT   /api/admin/membership-tiers/{tierId}
```

---

# 16. Endpoint Summary

| Method | Endpoint                                        | Access    | Mục đích                |
| ------ | ----------------------------------------------- | --------- | ----------------------- |
| GET    | `/api/membership-tiers`                         | Public    | Danh sách tier          |
| GET    | `/api/scores/me`                                | Protected | Score balance hiện tại  |
| GET    | `/api/scores/me/history`                        | Protected | Lịch sử điểm            |
| GET    | `/api/scores/me/tier`                           | Protected | Tier hiện tại           |
| POST   | `/api/scores/me/redeem-preview`                 | Protected | Preview đổi điểm        |
| POST   | `/internal/scores/earn`                         | Internal  | Cộng điểm               |
| POST   | `/internal/scores/redeem`                       | Internal  | Trừ/redeem điểm         |
| POST   | `/internal/scores/refund-redeem`                | Internal  | Hoàn điểm redeem        |
| POST   | `/internal/scores/revoke-earn`                  | Internal  | Thu hồi điểm earn       |
| POST   | `/internal/scores/users/{id}/recalculate-tier`  | Internal  | Tính lại tier           |
| GET    | `/internal/scores/users/{id}`                   | Internal  | Lấy score nội bộ        |
| GET    | `/api/admin/scores/users/{id}`                  | Admin     | Score detail user       |
| GET    | `/api/admin/scores/users/{id}/history`          | Admin     | History user            |
| POST   | `/api/admin/scores/users/{id}/adjustments`      | Admin     | Điều chỉnh điểm         |
| POST   | `/api/admin/scores/users/{id}/recalculate-tier` | Admin     | Tính lại tier           |
| POST   | `/api/admin/membership-tiers`                   | Admin     | Tạo tier                |
| GET    | `/api/admin/membership-tiers`                   | Admin     | Danh sách tier quản trị |
| GET    | `/api/admin/membership-tiers/{id}`              | Admin     | Tier detail             |
| PUT    | `/api/admin/membership-tiers/{id}`              | Admin     | Cập nhật tier           |

---

# 17. Membership Tier Query APIs

## 17.1. Get Membership Tiers

### Endpoint

```http
GET /api/membership-tiers
```

### Response Success

```json
{
  "success": true,
  "message": "Membership tiers retrieved successfully",
  "data": [
    {
      "tierId": 1,
      "tierName": "SILVER",
      "minPoints": 0,
      "earningRate": 0.05
    },
    {
      "tierId": 2,
      "tierName": "GOLD",
      "minPoints": 200,
      "earningRate": 0.07
    },
    {
      "tierId": 3,
      "tierName": "DIAMOND",
      "minPoints": 500,
      "earningRate": 0.10
    }
  ]
}
```

Danh sách được sort theo:

```txt
minPoints ASC
```

---

# 18. Customer Score Query APIs

## 18.1. Get Current User Score Balance

### Endpoint

```http
GET /api/scores/me
```

Backend lấy `userId` từ JWT.

Frontend không truyền user ID.

### Response Success

```json
{
  "success": true,
  "message": "Score balance retrieved successfully",
  "data": {
    "userId": 15,
    "currentPoints": 150,
    "accumulatedPoints": 350,
    "currentTier": {
      "tierId": 2,
      "tierName": "GOLD",
      "minPoints": 200,
      "earningRate": 0.07
    },
    "nextTier": {
      "tierId": 3,
      "tierName": "DIAMOND",
      "minPoints": 500,
      "pointsRequired": 150
    },
    "updatedAt": "2026-06-21T20:30:00"
  }
}
```

Nếu user chưa có `user_scores`, Score Service có thể tự khởi tạo:

```txt
currentPoints = 0
accumulatedPoints = 0
tier = tier thấp nhất
```

Hoặc trả `SCORE_ACCOUNT_NOT_FOUND`.

Contract khuyến nghị lazy initialization khi user lần đầu truy vấn hoặc earn point.

---

## 18.2. Get Current Membership Tier

### Endpoint

```http
GET /api/scores/me/tier
```

### Response Success

```json
{
  "success": true,
  "message": "Membership tier retrieved successfully",
  "data": {
    "tierId": 2,
    "tierName": "GOLD",
    "minPoints": 200,
    "earningRate": 0.07,
    "accumulatedPoints": 350,
    "nextTier": {
      "tierId": 3,
      "tierName": "DIAMOND",
      "pointsRequired": 150
    }
  }
}
```

---

## 18.3. Get Current User Score History

### Endpoint

```http
GET /api/scores/me/history
```

### Query Parameters

| Parameter       | Type     | Required | Validation      |
| --------------- | -------- | -------: | --------------- |
| page            | integer  |       No | >= 0            |
| size            | integer  |       No | 1–50            |
| transactionType | string   |       No | Enum hợp lệ     |
| bookingId       | number   |       No | > 0             |
| from            | datetime |       No | ISO-8601        |
| to              | datetime |       No | ISO-8601        |
| sort            | string   |       No | field,direction |

### Response Success

```json
{
  "success": true,
  "message": "Score history retrieved successfully",
  "data": {
    "content": [
      {
        "historyId": 7001,
        "bookingId": 1001,
        "pointChange": 12,
        "transactionType": "EARN_BY_BOOKING",
        "balanceBefore": 150,
        "balanceAfter": 162,
        "accumulatedBefore": 350,
        "accumulatedAfter": 362,
        "referenceHistoryId": null,
        "description": "Earned points from booking LORA-20260621-0001",
        "createdAt": "2026-06-21T20:30:00"
      },
      {
        "historyId": 7002,
        "bookingId": 1002,
        "pointChange": -50,
        "transactionType": "REDEEM_FOR_BOOKING",
        "balanceBefore": 162,
        "balanceAfter": 112,
        "accumulatedBefore": 362,
        "accumulatedAfter": 362,
        "referenceHistoryId": null,
        "description": "Redeemed points for booking LORA-20260621-0002",
        "createdAt": "2026-06-21T21:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

# 19. Redeem Preview API

## 19.1. Endpoint

```http
POST /api/scores/me/redeem-preview
```

### Request

```json
{
  "bookingId": 1001,
  "requestedPoints": 100
}
```

Preview không thay đổi số dư.

### Processing

```txt
Resolve user from JWT
→ Validate booking belongs to user
→ Get current score balance
→ Validate requestedPoints
→ Calculate discount value
→ Return preview
```

### Point Redemption Rate

Quyết định chính thức cho Sprint 2:

```txt
1 point = 1,000 VND
```

Ví dụ:

```txt
100 points = 100,000 VND
```

Tỷ lệ này là cố định trong Sprint 2 và phải được cấu hình tập trung, không hardcode rải rác trong service.

### Response Success

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

### Error: Insufficient Points

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Insufficient score balance",
  "errorCode": "SCORE_INSUFFICIENT_BALANCE",
  "data": {
    "availablePoints": 50,
    "requestedPoints": 100
  },
  "errors": null
}
```

---

# 20. Earn Score Internal API

## 20.1. Endpoint

```http
POST /internal/scores/earn
```

API này do Payment Service, Booking Service hoặc event consumer gọi.

Frontend không được gọi.

### Request Body

```json
{
  "userId": 15,
  "bookingId": 1001,
  "eligibleAmount": 240000,
  "paymentTransactionCode": "PAY-LORAFILM-20260621-998877",
  "eventId": "PAYMENT-SUCCESS-3001",
  "idempotencyKey": "EARN:BOOKING:1001"
}
```

### Field Definitions

| Field                  | Type   | Required | Validation             |
| ---------------------- | ------ | -------: | ---------------------- |
| userId                 | number |      Yes | > 0                    |
| bookingId              | number |      Yes | > 0                    |
| eligibleAmount         | number |      Yes | >= 0                   |
| paymentTransactionCode | string |       No | Không rỗng nếu có      |
| eventId                | string |      Yes | Business event identifier |
| idempotencyKey         | string |      Yes | Unique database idempotency key |

### Source of Truth

`eligibleAmount` phải đến từ backend service đáng tin cậy.

Không nhận amount từ Frontend.

### Processing Flow

```txt
Validate internal request
→ Check eventId chưa xử lý
→ Load/create user score
→ Load current membership tier
→ Calculate earned points
→ Update currentPoints
→ Update accumulatedPoints
→ Insert score history
→ Recalculate tier
→ Commit transaction
→ Return result
```

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Score earned successfully",
  "data": {
    "userId": 15,
    "bookingId": 1001,
    "earnedPoints": 12,
    "currentPoints": 162,
    "accumulatedPoints": 362,
    "previousTier": "GOLD",
    "currentTier": "GOLD",
    "tierChanged": false,
    "historyId": 7003
  }
}
```

### Idempotent Response

Nếu cùng `eventId` đã được xử lý:

```json
{
  "success": true,
  "message": "Score earn event was already processed",
  "data": {
    "userId": 15,
    "bookingId": 1001,
    "idempotent": true
  }
}
```

Không cộng điểm lần hai.

### Error: Invalid Amount

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Eligible amount must not be negative",
  "errorCode": "SCORE_INVALID_ELIGIBLE_AMOUNT",
  "data": null,
  "errors": null
}
```

### Error: Tier Not Configured

Status: `500 Internal Server Error`

```json
{
  "success": false,
  "message": "Membership tier configuration is invalid",
  "errorCode": "SCORE_TIER_CONFIGURATION_INVALID",
  "data": null,
  "errors": null
}
```

---

# 21. Redeem Score Internal API

## 21.1. Endpoint

```http
POST /internal/scores/redeem
```

Do Booking Service hoặc orchestration layer gọi.

### Request Body

```json
{
  "userId": 15,
  "bookingId": 1002,
  "points": 50,
  "eventId": "SCORE-REDEEM-BOOKING-1002",
  "idempotencyKey": "REDEEM:BOOKING:1002"
}
```

### Processing Flow

```txt
Validate internal request
→ Check idempotency
→ Lock user score row
→ Verify currentPoints >= requested points
→ Deduct currentPoints
→ Insert score history
→ Commit
```

### Response Success

```json
{
  "success": true,
  "message": "Score redeemed successfully",
  "data": {
    "userId": 15,
    "bookingId": 1002,
    "redeemedPoints": 50,
    "redeemValue": 50000,
    "currentPoints": 112,
    "historyId": 7004
  }
}
```

### Error: Insufficient Balance

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Insufficient score balance",
  "errorCode": "SCORE_INSUFFICIENT_BALANCE",
  "data": {
    "availablePoints": 30,
    "requestedPoints": 50
  },
  "errors": null
}
```

### Concurrency Rule

Không được dùng flow không atomic:

```txt
SELECT current_points
→ kiểm tra
→ UPDATE
```

nếu không lock row.

Khuyến nghị:

```sql
UPDATE user_scores
SET current_points = current_points - :points
WHERE user_id = :userId
  AND current_points >= :points;
```

Nếu affected rows bằng `0`:

```txt
SCORE_INSUFFICIENT_BALANCE
```

---

# 22. Refund Redeemed Points

## 22.1. Endpoint

```http
POST /internal/scores/refund-redeem
```

Dùng khi:

* Booking bị cancel.
* Booking bị expire.
* Payment không hoàn tất và booking không còn hiệu lực.
* Redeem transaction cần rollback.

### Request

```json
{
  "userId": 15,
  "bookingId": 1002,
  "points": 50,
  "originalRedeemEventId": "SCORE-REDEEM-BOOKING-1002",
  "eventId": "SCORE-REFUND-BOOKING-1002",
  "idempotencyKey": "REFUND_REDEEM:BOOKING:1002",
  "reason": "Booking expired before payment"
}
```

### Rules

* Phải xác định được redeem transaction gốc.
* Không hoàn quá số điểm đã redeem.
* Không hoàn hai lần.
* Không tăng `accumulatedPoints`.

### Response Success

```json
{
  "success": true,
  "message": "Redeemed score refunded successfully",
  "data": {
    "userId": 15,
    "bookingId": 1002,
    "refundedPoints": 50,
    "currentPoints": 162,
    "historyId": 7005
  }
}
```

---

# 23. Revoke Earned Points

## 23.1. Endpoint

```http
POST /internal/scores/revoke-earn
```

Dùng khi booking/payment đã được refund hoặc giao dịch earn bị đảo ngược.

### Request

```json
{
  "userId": 15,
  "bookingId": 1001,
  "points": 12,
  "originalEarnEventId": "PAYMENT-SUCCESS-3001",
  "eventId": "PAYMENT-REFUND-3001",
  "idempotencyKey": "REVOKE_EARN:BOOKING:1001:PAYMENT-REFUND-3001",
  "reason": "Payment refunded"
}
```

### Current Balance Insufficient Case

Tình huống:

```txt
User đã dùng một phần điểm
→ currentPoints nhỏ hơn số điểm cần thu hồi
```

Contract Sprint 2 không cho balance âm.

Reviewer phải chọn một hướng:

#### Hướng A — Deduct tối đa current balance

```txt
actualDeducted = min(currentPoints, pointsToRevoke)
```

Phần thiếu cần manual reconciliation.

#### Hướng B — Không cho revoke và tạo debt

Schema chưa hỗ trợ debt.

Contract khuyến nghị Hướng A trong Sprint 2 và ghi log cảnh báo.

### Response

```json
{
  "success": true,
  "message": "Earned score revoked successfully",
  "data": {
    "userId": 15,
    "bookingId": 1001,
    "requestedPoints": 12,
    "deductedPoints": 12,
    "currentPoints": 150,
    "accumulatedPoints": 350,
    "historyId": 7006,
    "requiresManualReconciliation": false
  }
}
```

---

# 24. Recalculate Membership Tier

## 24.1. Internal Endpoint

```http
POST /internal/scores/users/{userId}/recalculate-tier
```

## 24.2. Admin Endpoint

```http
POST /api/admin/scores/users/{userId}/recalculate-tier
```

### Processing

```txt
Load accumulatedPoints
→ Find highest eligible tier
→ Compare currentTier
→ Update if changed
→ Return result
```

### Response

```json
{
  "success": true,
  "message": "Membership tier recalculated successfully",
  "data": {
    "userId": 15,
    "accumulatedPoints": 510,
    "previousTier": "GOLD",
    "currentTier": "DIAMOND",
    "tierChanged": true
  }
}
```

---

# 25. Internal Score Query

## 25.1. Endpoint

```http
GET /internal/scores/users/{userId}
```

Dùng cho Booking hoặc service nội bộ cần kiểm tra balance.

### Response

```json
{
  "success": true,
  "message": "User score retrieved successfully",
  "data": {
    "userId": 15,
    "currentPoints": 150,
    "accumulatedPoints": 350,
    "tierName": "GOLD",
    "earningRate": 0.07
  }
}
```

---

# 26. Admin Score APIs

## 26.1. Get User Score Detail

```http
GET /api/admin/scores/users/{userId}
```

### Response

```json
{
  "success": true,
  "message": "User score retrieved successfully",
  "data": {
    "userId": 15,
    "currentPoints": 150,
    "accumulatedPoints": 350,
    "currentTier": {
      "tierId": 2,
      "tierName": "GOLD",
      "earningRate": 0.07
    },
    "updatedAt": "2026-06-21T20:30:00"
  }
}
```

---

## 26.2. Get User Score History

```http
GET /api/admin/scores/users/{userId}/history
```

Hỗ trợ:

```txt
page
size
transactionType
bookingId
from
to
sort
```

---

## 26.3. Manual Score Adjustment

### Endpoint

```http
POST /api/admin/scores/users/{userId}/adjustments
```

### Request — Add

```json
{
  "adjustmentType": "ADD",
  "points": 100,
  "affectAccumulatedPoints": false,
  "reason": "Customer support compensation",
  "requestId": "ADMIN-ADJUST-20260621-001"
}
```

### Request — Deduct

```json
{
  "adjustmentType": "DEDUCT",
  "points": 50,
  "affectAccumulatedPoints": false,
  "reason": "Correction of duplicated points",
  "requestId": "ADMIN-ADJUST-20260621-002"
}
```

### Rules

* `points > 0`.
* Reason bắt buộc.
* Request ID dùng cho idempotency.
* Không cho balance âm.
* Ghi admin identity vào audit nếu schema hỗ trợ.
* Nếu `affectAccumulatedPoints = true`, phải recalculate tier.

### Response

```json
{
  "success": true,
  "message": "User score adjusted successfully",
  "data": {
    "userId": 15,
    "adjustmentType": "ADD",
    "pointChange": 100,
    "currentPoints": 250,
    "accumulatedPoints": 350,
    "historyId": 7007
  }
}
```

### Schema Limitation

Schema hiện chưa có:

```txt
created_by
request_id
```

Nếu cần audit/idempotency đầy đủ, phải schema alignment.

---

# 27. Membership Tier Admin APIs

## 27.1. Create Membership Tier

### Endpoint

```http
POST /api/admin/membership-tiers
```

### Request

```json
{
  "tierName": "PLATINUM",
  "minPoints": 1000,
  "earningRate": 0.12
}
```

### Validation

| Field       | Rule                                          |
| ----------- | --------------------------------------------- |
| tierName    | Required, unique, tối đa 50                   |
| minPoints   | >= 0                                          |
| earningRate | > 0                                           |
| minPoints   | Không trùng ngưỡng tier khác nếu team yêu cầu |

### Response

Status: `201 Created`

```json
{
  "success": true,
  "message": "Membership tier created successfully",
  "data": {
    "tierId": 4,
    "tierName": "PLATINUM",
    "minPoints": 1000,
    "earningRate": 0.12
  }
}
```

---

## 27.2. Get Admin Membership Tiers

```http
GET /api/admin/membership-tiers
```

Trả thêm số lượng user nếu implementation hỗ trợ.

---

## 27.3. Get Membership Tier Detail

```http
GET /api/admin/membership-tiers/{tierId}
```

---

## 27.4. Update Membership Tier

```http
PUT /api/admin/membership-tiers/{tierId}
```

### Business Rules

Nếu thay đổi `minPoints`:

* Có thể ảnh hưởng tier của nhiều user.
* Không tự động cập nhật toàn bộ user trong cùng HTTP request nếu dữ liệu lớn.
* Có thể cần batch recalculation.
* Sprint 2 có thể chỉ ghi nhận yêu cầu chạy recalculate.

Nếu thay đổi `earningRate`:

* Chỉ áp dụng cho giao dịch earn mới.
* Không tính lại lịch sử cũ.

---

# 28. Score History Behavior

Mỗi biến động score phải tạo một `score_history` record trong cùng transaction.

Ví dụ earn:

```txt
point_change = +12
transaction_type = EARN_BY_BOOKING
booking_id = 1001
```

Ví dụ redeem:

```txt
point_change = -50
transaction_type = REDEEM_FOR_BOOKING
booking_id = 1002
```

Ví dụ hoàn redeem:

```txt
point_change = +50
transaction_type = REFUND_REDEEM
booking_id = 1002
```

Không được update `user_scores` mà không insert history.

Nếu insert history lỗi:

```txt
Rollback balance update
```

---

# 29. Concurrency Rules

## 29.1. Earn Concurrent Requests

Hai earn request cùng event:

```txt
Chỉ cộng một lần
```

Cần idempotency check và transaction.

## 29.2. Redeem Concurrent Requests

Hai redeem request đồng thời:

```txt
Không được làm currentPoints âm
```

Khuyến nghị atomic conditional update.

## 29.3. Balance and History

Operation phải atomic:

```txt
Update user_scores
→ Insert score_history
→ Recalculate tier nếu cần
→ Commit
```

Nếu bất kỳ bước nào lỗi:

```txt
Rollback toàn bộ
```

## 29.4. User Score Initialization

Hai request đầu tiên đồng thời không được tạo trùng `user_scores`.

Dựa vào:

```txt
user_id PRIMARY KEY
```

Request còn lại phải đọc record đã tồn tại.

---

# 30. Idempotency Rules

### Earn

```txt
eventId unique
```

### Redeem

```txt
eventId hoặc requestId unique
```

### Refund Redeem

```txt
originalRedeemEventId + refund eventId
```

### Revoke Earn

```txt
originalEarnEventId + revoke eventId
```

### Admin Adjustment

```txt
requestId unique
```

Schema hiện chưa có idempotency key.

Nếu không sửa schema, service có thể dùng bảng idempotency riêng hoặc kiểm tra `bookingId + transactionType`, nhưng contract khuyến nghị schema alignment.

---

# 31. Score Expiry Direction

Schema hiện có transaction type:

```txt
EXPIRED
```

nhưng chưa có:

```txt
point_batch
earned_at
expires_at
remaining_points
```

Vì vậy không thể xác định chính xác điểm nào hết hạn theo FIFO.

Direction hiện tại cho Sprint 2:

```txt
Không implement point expiry hoàn chỉnh
```

Điểm này cần Score Service Owner xác nhận lần cuối trước khi contract được approve.

Có thể giữ `EXPIRED` như hướng mở rộng.

Muốn implement phải thiết kế:

* Point batches.
* Expiry date.
* Remaining point per batch.
* FIFO redemption.
* Scheduled expiry job.

Cần schema issue riêng.

---

# 32. Booking, Payment và Promotion Integration

## 32.1. Earn Direction

```txt
Booking CONFIRMED
+
Payment SUCCESS
→ Score earn
```

Khuyến nghị trigger từ Payment success event hoặc Booking confirmed event, nhưng chỉ chọn một nguồn để tránh duplicate.

Contract đề xuất:

```txt
Payment SUCCESS
→ Booking CONFIRMED
→ BOOKING_CONFIRMED event
→ Score Service earn
```

Reviewer cần xác nhận source event cuối cùng.

## 32.2. Eligible Amount

Điểm earn dựa trên:

```txt
final paid amount
```

sau:

* Promotion discount.
* Redeemed points nếu nghiệp vụ quy định.

Quyết định chính thức:

```txt
Không cộng điểm trên phần giá trị được thanh toán bằng score.
Chỉ cộng điểm trên số tiền thực trả bằng tiền.
```

## 32.3. Redeem Direction

```txt
Booking đang PENDING_PAYMENT
→ Booking Service request redeem
→ Score Service trừ điểm
→ Booking/Payment sử dụng redeemValue
```

## 32.4. Booking Failure

```txt
Booking CANCELLED hoặc EXPIRED
→ hoàn điểm đã redeem
```

## 32.5. Refund Direction

```txt
Payment REFUNDED
→ thu hồi điểm đã earn
→ hoàn lại điểm đã redeem nếu policy cho phép
```

---

# 33. Security Rules

* Customer chỉ được xem score của chính mình.
* Frontend không được tự gọi earn/redeem commit/refund.
* Frontend không được gửi user ID cho `/me` endpoints.
* Frontend không được tự tính earned points.
* Internal API phải được bảo vệ.
* Admin adjustment cần permission riêng.
* Admin adjustment bắt buộc có reason.
* Internal endpoints không expose công khai qua Gateway.
* Không cho client trực tiếp thay đổi current points.
* Không tin amount hoặc earning rate từ Frontend.

Permission đề xuất:

```txt
SCORE_READ
SCORE_MANAGE
SCORE_ADJUST
MEMBERSHIP_TIER_READ
MEMBERSHIP_TIER_MANAGE
```

---

# 34. Delete Policy

Không hard delete:

```txt
user_scores
score_history
membership_tiers đã được sử dụng
```

Lý do:

* Giữ audit.
* Giữ lịch sử điểm.
* Không phá foreign key nội bộ.
* Không làm mất tier của user.

Schema tier chưa có `is_active`.

Nếu cần ngừng sử dụng tier, có thể:

* Thêm `is_active`.
* Hoặc không assign user mới vào tier đó.

Không expose delete API trong Sprint 2.

---

# 35. Error Code Catalog

| Error Code                             |         HTTP | Ý nghĩa                        |
| -------------------------------------- | -----------: | ------------------------------ |
| `SCORE_ACCOUNT_NOT_FOUND`              |          404 | Không tìm thấy score account   |
| `SCORE_HISTORY_NOT_FOUND`              |          404 | Không tìm thấy history         |
| `SCORE_INSUFFICIENT_BALANCE`           |          409 | Không đủ điểm                  |
| `SCORE_INVALID_POINT_AMOUNT`           |          400 | Số điểm không hợp lệ           |
| `SCORE_INVALID_ELIGIBLE_AMOUNT`        |          400 | Amount tính điểm sai           |
| `SCORE_BALANCE_WOULD_BE_NEGATIVE`      |          409 | Operation làm điểm âm          |
| `SCORE_EVENT_ALREADY_PROCESSED`        | 409 hoặc 200 | Event đã xử lý                 |
| `SCORE_ORIGINAL_TRANSACTION_NOT_FOUND` |          404 | Không tìm thấy transaction gốc |
| `SCORE_REFUND_ALREADY_PROCESSED`       |          409 | Hoàn điểm rồi                  |
| `SCORE_REVOKE_ALREADY_PROCESSED`       |          409 | Thu hồi rồi                    |
| `SCORE_REDEEM_NOT_ALLOWED`             |          409 | Không được redeem              |
| `SCORE_RECONCILIATION_REQUIRED`        |          409 | Cần xử lý thủ công             |
| `SCORE_TIER_NOT_FOUND`                 |          404 | Không tìm thấy tier            |
| `SCORE_TIER_NAME_ALREADY_EXISTS`       |          409 | Tên tier trùng                 |
| `SCORE_TIER_THRESHOLD_CONFLICT`        |          409 | Ngưỡng tier xung đột           |
| `SCORE_TIER_CONFIGURATION_INVALID`     |          500 | Cấu hình tier sai              |
| `SCORE_INVALID_TRANSACTION_TYPE`       |          400 | Transaction type sai           |
| `SCORE_INVALID_QUERY`                  |          400 | Query sai                      |
| `BOOKING_SERVICE_UNAVAILABLE`          |          503 | Booking Service không khả dụng |
| `PAYMENT_SERVICE_UNAVAILABLE`          |          503 | Payment Service không khả dụng |
| `VALIDATION_ERROR`                     |          400 | Validation lỗi                 |
| `UNAUTHORIZED`                         |          401 | Chưa đăng nhập                 |
| `FORBIDDEN`                            |          403 | Không có quyền                 |
| `INTERNAL_SERVER_ERROR`                |          500 | Lỗi hệ thống                   |

---

# 36. Schema Alignment Bắt Buộc Trước Implementation

Score implementation chưa được bắt đầu trước khi Schema Alignment MR được merge.

## 36.1. Bắt buộc bổ sung vào `score_history`

```txt
idempotency_key VARCHAR(100) UNIQUE NOT NULL
balance_before INT NOT NULL
balance_after INT NOT NULL
accumulated_before INT NOT NULL
accumulated_after INT NOT NULL
reference_history_id BIGINT NULL
created_by BIGINT NULL
request_id VARCHAR(100) NULL
```

## 36.2. Constraint và index

```txt
UNIQUE(idempotency_key)
UNIQUE(request_id)
INDEX(user_id, created_at)
INDEX(user_id, transaction_type, created_at)
INDEX(booking_id)
```

`reference_history_id` là self-reference nội bộ tới `score_history.id`.

Không tạo physical FK cho `booking_id` hoặc `created_by` sang database service khác.

## 36.3. Transaction Types Chính Thức

```txt
EARN_BY_BOOKING
REDEEM_FOR_BOOKING
REFUND_REDEEM
REVOKE_EARN_BY_REFUND
MANUAL_ADD
MANUAL_DEDUCT
EXPIRED
```

## 36.4. Blocker Rule

Implementation issue chỉ được chuyển sang `Ready` khi:

```txt
Contract MR đã được approve và merge
+
Score Schema Alignment MR đã merge
+
Score Service Owner xác nhận Ready for Implementation
```

---

# 37. Out of Scope

* Movie rating/review.
* Star rating.
* Gamification nâng cao.
* Achievement/badge.
* Daily mission.
* Referral score.
* Point transfer giữa user.
* Point gifting.
* Point expiry theo batch.
* Tier period reset.
* Tier benefit engine.
* Full reward catalog.
* Production Kafka/outbox implementation.
* Booking/Payment integration implementation thật.
* Backend code trong issue này.
* Schema update ngoài review process.

---

# 38. Implementation Issue Direction

Sau khi contract được duyệt và schema alignment hoàn tất nếu cần, có thể tách:

```txt
[Backend] Implement Score Balance and History Query APIs

[Backend] Implement Earn and Membership Tier Calculation

[Backend] Implement Redeem, Refund and Revoke Score Flow

[Backend] Implement Score Admin Adjustment and Tier Management APIs
```

Nếu giảm scope Sprint 2:

```txt
Issue 1: Score balance, tier and history query
Issue 2: Earn score after booking
Issue 3: Redeem and refund score foundation
```

Implementation issue chỉ chuyển `Ready` khi:

```txt
Contract đã được duyệt
+
Schema bắt buộc đã align
+
Khang xác nhận feasibility
```

---

# 39. Acceptance Criteria

* [ ] Có schema Sprint 0 baseline.
* [ ] Có naming clarification Score vs Rating.
* [ ] Có score balance API.
* [ ] Có score history API.
* [ ] Có membership tier APIs.
* [ ] Có earn API direction.
* [ ] Có redeem API direction.
* [ ] Có refund redeem direction.
* [ ] Có revoke earn direction.
* [ ] Có admin adjustment API.
* [ ] Có tier calculation rule.
* [ ] Có earning point formula.
* [ ] Có redeem conversion rule.
* [ ] Có non-negative balance rule.
* [ ] Có transaction/history atomic rule.
* [ ] Có concurrency rule.
* [ ] Có idempotency rule.
* [ ] Có logical reference notes.
* [ ] Có Booking/Payment integration direction.
* [ ] Có internal/admin security rules.
* [ ] Có error code catalog.
* [ ] Có schema mismatch notes.
* [ ] Khang review feasibility.
* [ ] Contract sẵn sàng implementation.
* [ ] MR target `develop`.

---

# 40. Các Điểm Còn Cần Owner Xác Nhận

Các quyết định production chính đã được chốt. Còn các điểm sau cần Khang xác nhận trước khi contract chuyển sang `Approved`:

1. Earn chỉ được trigger duy nhất từ `BOOKING_CONFIRMED` hay `PAYMENT_SUCCESS`.
2. Khi revoke nhưng `currentPoints` nhỏ hơn số điểm cần thu hồi, Sprint 2 dùng partial deduction + reconciliation hay reject toàn bộ.
3. Có hỗ trợ `affectAccumulatedPoints = true` cho Admin Adjustment trong Sprint 2 không.
4. Có sử dụng lazy initialization cho `user_scores` không.
5. Có bổ sung `membership_tiers.is_active` trong Sprint 2 không.
6. Point expiry được xác nhận là ngoài scope Sprint 2 hay không.

---

# 41. Lịch Sử Chỉnh Sửa

| Ngày       | Nội dung                                                     | Người thực hiện  |
| ---------- | ------------------------------------------------------------ | ---------------- |
| 21/06/2026 | Khởi tạo Score Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật theo Production Readiness Review của Score Service Owner; chốt business rules, idempotency, audit snapshot, reference transaction, concurrency và schema alignment bắt buộc | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận tại đây sau khi schema MR tương ứng đã được merge.
