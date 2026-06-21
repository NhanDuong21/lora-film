# Promotion Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                                                  |
| -------------- | ------------------------------------------------------------------------- |
| Service        | `promotion-service`                                                       |
| Feature        | Campaign, Promotion Code, Discount Validation and Usage Tracking          |
| API liên quan  | Campaign Query, Promotion Query, Validate, Apply, Usage, Admin Management |
| Contract Owner | Dương Thiện Nhân                                                          |
| Backend Owner  | Trần Lương Thiện Hoàng                                                    |
| Reviewer       | Trần Lương Thiện Hoàng                                                    |
| Trạng thái     | Draft / Ready for Review                                                  |
| Milestone      | Sprint 2 - Core Service API Foundation                                    |
| Ngày cập nhật  | 21/06/2026                                                                |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả API Contract cho `promotion-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Thống nhất contract giữa Frontend, API Gateway, Booking Service, Payment Service và Promotion Service.
* Làm cơ sở implement campaign, promotion code, validation, discount calculation và usage tracking.
* Xác định rõ Promotion Service là source of truth cho promotion rule.
* Không cho Frontend tự tính hoặc tự quyết định discount cuối cùng.
* Chuẩn hóa request, response, validation, HTTP status và error code.
* Xác định rõ global usage limit, per-user usage limit và idempotency.
* Ghi rõ các điểm có thể mismatch với schema Sprint 0 để service owner review.
* Làm cơ sở tách implementation issue sau khi contract được duyệt.

---

## 3. Phạm Vi Promotion Service

Promotion Service chịu trách nhiệm:

* Quản lý promotion campaign.
* Quản lý promotion code.
* Kiểm tra thời gian hiệu lực.
* Kiểm tra trạng thái campaign và promotion.
* Kiểm tra minimum order amount.
* Kiểm tra global usage limit.
* Kiểm tra per-user usage limit.
* Tính discount preview.
* Apply promotion vào booking.
* Ghi nhận promotion usage.
* Revert usage khi booking/payment thất bại nếu nghiệp vụ cho phép.
* Quản lý thống kê usage cơ bản.
* Bảo đảm việc apply promotion idempotent.

Promotion Service không chịu trách nhiệm:

* Quản lý booking.
* Tính giá vé gốc.
* Quản lý payment.
* Quản lý điểm thưởng.
* Quản lý movie/showtime/seat.
* Gửi notification.
* Truy cập trực tiếp database của Booking hoặc User Service.
* Recommendation Engine hoặc promotion personalization.

---

## 4. Physical Schema Sprint 0

### 4.1. Bảng `promotion_campaigns`

```sql
CREATE TABLE `promotion_campaigns` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Campaign ID',
  `campaign_name` varchar(150) NOT NULL,
  `description` text,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);
```

### 4.2. Bảng `promotions`

```sql
CREATE TABLE `promotions` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Promotion/Voucher ID',
  `campaign_id` bigint NOT NULL COMMENT 'Foreign Key noi bo ket noi voi promotion_campaigns',
  `promotion_code` varchar(50) UNIQUE NOT NULL COMMENT 'Ma voucher khach hang nhap, e.g., LORAFILM2026',
  `description` text,
  `discount_type` varchar(20) NOT NULL COMMENT 'PERCENTAGE, FIXED_AMOUNT',
  `discount_value` decimal(10,2) NOT NULL COMMENT 'Gia tri giam, e.g., 10.00 cho percent hoac 20000.00 cho fixed',
  `max_discount_amount` decimal(10,2) COMMENT 'So tien giam toi da neu dung PERCENTAGE, Null neu dung FIXED_AMOUNT',
  `min_order_amount` decimal(10,2) DEFAULT 0 COMMENT 'Gia tri don hang toi thieu de duoc ap dung ma',
  `usage_limit` int NOT NULL COMMENT 'Tong so lan ma nay duoc phep su dung tren toan he thong',
  `used_count` int DEFAULT 0 COMMENT 'So lan ma nay da thuc te duoc dung, used_count <= usage_limit',
  `limit_per_user` int DEFAULT 1 COMMENT 'So lan toi da mot khach hang duoc dung ma nay',
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);
```

### 4.3. Bảng `promotion_usages`

```sql
CREATE TABLE `promotion_usages` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `promotion_id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `booking_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang bookings.id cua Booking Service',
  `applied_at` timestamp DEFAULT (now())
);
```

### 4.4. Quan hệ nội bộ

```sql
ALTER TABLE `promotions`
ADD FOREIGN KEY (`campaign_id`)
REFERENCES `promotion_campaigns` (`id`)
ON DELETE CASCADE;

ALTER TABLE `promotion_usages`
ADD FOREIGN KEY (`promotion_id`)
REFERENCES `promotions` (`id`)
ON DELETE CASCADE;
```

---

## 5. Phân Tích Schema Hiện Tại

### 5.1. Những nghiệp vụ schema hỗ trợ

Schema hiện tại hỗ trợ:

* Tạo và quản lý campaign.
* Tạo promotion code unique.
* Hai loại discount: phần trăm và số tiền cố định.
* Minimum booking amount.
* Maximum discount cho percentage promotion.
* Global usage limit.
* Per-user usage limit.
* Promotion activation theo thời gian.
* Ghi nhận promotion đã được áp dụng vào booking.
* Mỗi booking chỉ được gắn với một usage record.

### 5.2. Ý nghĩa của `booking_id UNIQUE`

Constraint hiện tại:

```txt
promotion_usages.booking_id UNIQUE
```

có nghĩa:

```txt
Một booking chỉ được áp dụng tối đa một promotion.
```

Contract Sprint 2 giữ rule này.

Nếu tương lai hỗ trợ stacking nhiều promotion trên cùng booking, phải:

* Gỡ unique khỏi `booking_id`.
* Thiết kế priority/combination rule.
* Cập nhật Booking, Payment và Promotion Contract.
* Tạo schema alignment issue riêng.

### 5.3. Giới hạn schema hiện tại

Schema chưa có:

```txt
promotion_usages.status
promotion_usages.discount_amount
promotion_usages.original_amount
promotion_usages.final_amount
promotion_usages.reverted_at
promotion_usages.revert_reason
promotion_usages.updated_at

promotions.version
promotions.deleted_at
```

Do đó schema hiện tại chưa thể hiện rõ:

* Usage đang được reserve hay đã áp dụng chính thức.
* Usage đã bị revert hay chưa.
* Discount snapshot tại thời điểm apply.
* Amount trước và sau khi giảm.
* Lý do hoàn lại lượt dùng.
* Optimistic locking version.

### 5.4. Hướng Sprint 2

Contract đề xuất lifecycle usage:

```txt
RESERVED
APPLIED
REVERTED
```

Tuy nhiên schema hiện chưa có `status`.

Reviewer cần quyết định một trong hai hướng:

#### Hướng A — Bổ sung status

```txt
promotion_usages.status
promotion_usages.discount_amount
promotion_usages.reverted_at
```

Đây là hướng khuyến nghị nếu Sprint 2 cần apply/revert usage đúng nghiệp vụ.

#### Hướng B — Không sửa schema trong Sprint 2

* Chỉ insert `promotion_usages` khi booking/payment đã được xác nhận.
* Discount preview không tạo usage.
* Khi cần revert thì xóa usage và giảm `used_count`.
* Audit và lịch sử revert sẽ hạn chế.

Hướng B đơn giản nhưng khó audit hơn.

---

## 6. Database-per-Service và Logical Reference

Các field sau là logical references:

```txt
promotion_usages.user_id
promotion_usages.booking_id
```

Promotion Service:

* Không tạo foreign key vật lý sang User hoặc Booking database.
* Không đọc trực tiếp database của service khác.
* Không tự sửa booking total bằng SQL.
* Phải giao tiếp qua API hoặc event contract.

### Source of truth

| Dữ liệu                    | Source of truth   |
| -------------------------- | ----------------- |
| Campaign và promotion rule | Promotion Service |
| Promotion usage            | Promotion Service |
| User identity              | User/Auth Service |
| Booking amount và status   | Booking Service   |
| Payment status             | Payment Service   |
| Movie/showtime/seat        | Movie Service     |

---

## 7. API Gateway và Service URL

### 7.1. API Gateway

Frontend chỉ gọi:

```txt
http://localhost:8080
```

### 7.2. Promotion Service Direct URL

Chỉ dùng để debug hoặc backend integration:

```txt
http://localhost:8087
```

Port chính thức lấy từ cấu hình project.

### 7.3. Request Flow

```txt
React Frontend
→ API Gateway
→ Promotion Service
→ Promotion Database
```

### 7.4. Apply Flow

```txt
Frontend chọn promotion
→ Booking Service cung cấp booking amount hợp lệ
→ Promotion Service validate
→ Promotion Service tính discount
→ Promotion Service ghi nhận usage
→ Booking/Payment dùng finalAmount đã được xác nhận
```

Promotion Service không tin booking amount do Frontend tự gửi làm source of truth cuối cùng.

---

## 8. Quy Ước Chung

### 8.1. Protected API Header

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

### 8.2. Internal API Header

```http
X-Internal-Token: <internal-token>
Content-Type: application/json
```

Cơ chế internal authentication phải đồng bộ với API Gateway và service security design.

### 8.3. Datetime

Sử dụng ISO-8601:

```txt
YYYY-MM-DDTHH:mm:ss
```

Timezone nghiệp vụ:

```txt
Asia/Ho_Chi_Minh
```

### 8.4. Currency

Sprint 2 chỉ hỗ trợ:

```txt
VND
```

Các amount trả về dạng number:

```json
{
  "discountAmount": 20000,
  "finalAmount": 220000
}
```

### 8.5. Promotion Code Normalization

Promotion code phải được:

* Trim khoảng trắng.
* Chuyển về uppercase trước khi lưu và tìm kiếm.
* So sánh không phân biệt hoa/thường ở tầng API.

Ví dụ:

```txt
lorafilm2026
LORAFILM2026
 LORAFILM2026
```

đều được normalize thành:

```txt
LORAFILM2026
```

---

## 9. Common Response Contract

### 9.1. Success

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

### 9.2. Error

```json
{
  "success": false,
  "message": "Operation failed",
  "errorCode": "ERROR_CODE",
  "data": null,
  "errors": null
}
```

### 9.3. Validation Error

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "data": null,
  "errors": [
    {
      "field": "promotionCode",
      "message": "Promotion code is required"
    }
  ]
}
```

---

## 10. Enum Definitions

### 10.1. DiscountType

```txt
PERCENTAGE
FIXED_AMOUNT
```

### 10.2. Promotion Availability Status

Status này có thể được derive từ `is_active`, thời gian và usage:

```txt
UPCOMING
ACTIVE
EXPIRED
DISABLED
OUT_OF_USAGE
```

Không nhất thiết lưu trực tiếp trong database.

### 10.3. PromotionUsageStatus Đề Xuất

```txt
RESERVED
APPLIED
REVERTED
```

| Status   | Mô tả                                                  |
| -------- | ------------------------------------------------------ |
| RESERVED | Đã giữ một lượt dùng cho booking đang chờ hoàn tất     |
| APPLIED  | Booking/payment đã thành công, lượt dùng được xác nhận |
| REVERTED | Booking thất bại/hủy và lượt dùng đã được hoàn lại     |

Status này cần schema alignment nếu được implement.

---

## 11. Campaign Lifecycle

Campaign được xem là `ACTIVE` khi đồng thời:

```txt
isActive = true
currentTime >= startDate
currentTime <= endDate
```

Các trạng thái derive:

```txt
UPCOMING:
currentTime < startDate

ACTIVE:
isActive = true
và nằm trong thời gian hiệu lực

EXPIRED:
currentTime > endDate

DISABLED:
isActive = false
```

Không hard delete campaign đã có promotion hoặc usage.

---

## 12. Promotion Availability Rules

Promotion hợp lệ khi tất cả điều kiện sau đúng:

```txt
promotion.isActive = true
campaign.isActive = true
currentTime nằm trong campaign time range
currentTime nằm trong promotion time range
usedCount < usageLimit
userUsageCount < limitPerUser
bookingAmount >= minOrderAmount
booking chưa apply promotion khác
```

Nếu bất kỳ điều kiện nào không đạt, promotion không được apply.

---

## 13. Discount Calculation Rules

### 13.1. Percentage Discount

Công thức:

```txt
rawDiscount = bookingAmount × discountValue / 100
```

Nếu có `maxDiscountAmount`:

```txt
discountAmount = min(rawDiscount, maxDiscountAmount)
```

Nếu không có:

```txt
discountAmount = rawDiscount
```

Ví dụ:

```txt
bookingAmount = 500000
discountValue = 10
maxDiscountAmount = 30000

rawDiscount = 50000
discountAmount = 30000
finalAmount = 470000
```

### 13.2. Fixed Amount Discount

```txt
discountAmount = min(discountValue, bookingAmount)
```

Ví dụ:

```txt
bookingAmount = 100000
discountValue = 150000

discountAmount = 100000
finalAmount = 0
```

### 13.3. Final Amount

```txt
finalAmount = max(bookingAmount - discountAmount, 0)
```

Không được trả final amount âm.

### 13.4. Rounding

Với VND:

```txt
discountAmount và finalAmount làm tròn về số nguyên VND
```

Reviewer cần xác nhận rounding strategy:

```txt
HALF_UP
FLOOR
```

Đề xuất:

```txt
HALF_UP
```

---

## 14. API Classification

### 14.1. Public APIs

```txt
GET /api/promotions/active
GET /api/promotions/{promotionId}
```

Public response không trả dữ liệu usage cá nhân.

### 14.2. Protected Customer APIs

```txt
POST /api/promotions/validate
POST /api/promotions/preview
GET  /api/promotions/me/usages
```

### 14.3. Internal APIs

```txt
POST /internal/promotions/apply
POST /internal/promotions/usages/{usageId}/confirm
POST /internal/promotions/usages/{usageId}/revert
GET  /internal/promotions/bookings/{bookingId}
```

Apply chính thức nên đi qua internal API do Booking Service gọi, không để Frontend tự ghi usage.

### 14.4. Admin APIs

```txt
POST   /api/admin/promotion-campaigns
GET    /api/admin/promotion-campaigns
GET    /api/admin/promotion-campaigns/{campaignId}
PUT    /api/admin/promotion-campaigns/{campaignId}
PATCH  /api/admin/promotion-campaigns/{campaignId}/status

POST   /api/admin/promotions
GET    /api/admin/promotions
GET    /api/admin/promotions/{promotionId}
PUT    /api/admin/promotions/{promotionId}
PATCH  /api/admin/promotions/{promotionId}/status

GET    /api/admin/promotions/{promotionId}/usages
GET    /api/admin/promotions/{promotionId}/statistics
```

---

## 15. Endpoint Summary

| Method | Endpoint                                     | Access    | Mục đích                    |
| ------ | -------------------------------------------- | --------- | --------------------------- |
| GET    | `/api/promotions/active`                     | Public    | Danh sách promotion active  |
| GET    | `/api/promotions/{id}`                       | Public    | Promotion detail            |
| POST   | `/api/promotions/validate`                   | Protected | Validate promotion code     |
| POST   | `/api/promotions/preview`                    | Protected | Tính discount preview       |
| GET    | `/api/promotions/me/usages`                  | Protected | Usage của user              |
| POST   | `/internal/promotions/apply`                 | Internal  | Apply promotion vào booking |
| POST   | `/internal/promotions/usages/{id}/confirm`   | Internal  | Xác nhận usage              |
| POST   | `/internal/promotions/usages/{id}/revert`    | Internal  | Revert usage                |
| GET    | `/internal/promotions/bookings/{bookingId}`  | Internal  | Usage theo booking          |
| POST   | `/api/admin/promotion-campaigns`             | Admin     | Tạo campaign                |
| GET    | `/api/admin/promotion-campaigns`             | Admin     | Danh sách campaign          |
| GET    | `/api/admin/promotion-campaigns/{id}`        | Admin     | Campaign detail             |
| PUT    | `/api/admin/promotion-campaigns/{id}`        | Admin     | Cập nhật campaign           |
| PATCH  | `/api/admin/promotion-campaigns/{id}/status` | Admin     | Enable/disable campaign     |
| POST   | `/api/admin/promotions`                      | Admin     | Tạo promotion               |
| GET    | `/api/admin/promotions`                      | Admin     | Danh sách promotion         |
| GET    | `/api/admin/promotions/{id}`                 | Admin     | Promotion detail quản trị   |
| PUT    | `/api/admin/promotions/{id}`                 | Admin     | Cập nhật promotion          |
| PATCH  | `/api/admin/promotions/{id}/status`          | Admin     | Enable/disable promotion    |
| GET    | `/api/admin/promotions/{id}/usages`          | Admin     | Usage list                  |
| GET    | `/api/admin/promotions/{id}/statistics`      | Admin     | Usage statistics            |

---

# 16. Public Promotion APIs

## 16.1. Get Active Promotions

### Endpoint

```http
GET /api/promotions/active
```

### Query Parameters

| Parameter      | Type    | Required | Validation      |
| -------------- | ------- | -------: | --------------- |
| page           | integer |       No | >= 0            |
| size           | integer |       No | 1–50            |
| discountType   | string  |       No | Enum hợp lệ     |
| minOrderAmount | number  |       No | >= 0            |
| sort           | string  |       No | field,direction |

### Response Success

```json
{
  "success": true,
  "message": "Active promotions retrieved successfully",
  "data": {
    "content": [
      {
        "promotionId": 101,
        "promotionCode": "LORAFILM2026",
        "description": "Giảm 10% tối đa 30000 VND",
        "discountType": "PERCENTAGE",
        "discountValue": 10,
        "maxDiscountAmount": 30000,
        "minOrderAmount": 200000,
        "startDate": "2026-06-20T00:00:00",
        "endDate": "2026-07-20T23:59:59",
        "availabilityStatus": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

Không trả:

```txt
usedCount chi tiết nếu không cần
user usage history
internal configuration
```

---

## 16.2. Get Promotion Detail

### Endpoint

```http
GET /api/promotions/{promotionId}
```

### Response Success

```json
{
  "success": true,
  "message": "Promotion retrieved successfully",
  "data": {
    "promotionId": 101,
    "campaignId": 10,
    "campaignName": "LoraFilm Summer 2026",
    "promotionCode": "LORAFILM2026",
    "description": "Giảm 10% tối đa 30000 VND",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxDiscountAmount": 30000,
    "minOrderAmount": 200000,
    "limitPerUser": 1,
    "startDate": "2026-06-20T00:00:00",
    "endDate": "2026-07-20T23:59:59",
    "availabilityStatus": "ACTIVE"
  }
}
```

### Error: Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Promotion not found",
  "errorCode": "PROMOTION_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

# 17. Validate Promotion API

## 17.1. Endpoint

```http
POST /api/promotions/validate
```

### Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

### Request Body

```json
{
  "promotionCode": "LORAFILM2026",
  "bookingId": 1001
}
```

Frontend không gửi `userId`.

Promotion Service lấy user từ JWT.

`bookingId` được dùng để Promotion Service hoặc backend orchestration lấy booking amount hợp lệ.

### Không khuyến nghị

```json
{
  "promotionCode": "LORAFILM2026",
  "bookingAmount": 240000
}
```

vì amount do Frontend gửi không phải source of truth.

### Processing Flow

```txt
Normalize promotion code
→ Resolve authenticated user
→ Find promotion
→ Validate campaign
→ Validate promotion status/time
→ Validate booking ownership/status
→ Get booking amount từ Booking Service
→ Check global usage limit
→ Check per-user usage limit
→ Check booking chưa dùng promotion
→ Check minimum booking amount
→ Calculate discount
→ Return validation result
```

Validate chỉ kiểm tra và tính toán.

Validate không:

* Tăng `used_count`.
* Tạo `promotion_usages`.
* Thay đổi booking.

### Response Valid

```json
{
  "success": true,
  "message": "Promotion is valid",
  "data": {
    "valid": true,
    "promotionId": 101,
    "promotionCode": "LORAFILM2026",
    "bookingId": 1001,
    "originalAmount": 240000,
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "discountAmount": 24000,
    "finalAmount": 216000,
    "expiresAt": "2026-07-20T23:59:59"
  }
}
```

### Response Invalid

Với lỗi business rule, API có thể trả HTTP tương ứng thay vì `valid: false`.

Contract này ưu tiên:

```txt
200 khi hợp lệ
4xx khi không hợp lệ
```

để Frontend xử lý rõ theo `errorCode`.

---

## 17.2. Promotion Disabled

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Promotion is disabled",
  "errorCode": "PROMOTION_DISABLED",
  "data": null,
  "errors": null
}
```

## 17.3. Promotion Not Started

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Promotion has not started yet",
  "errorCode": "PROMOTION_NOT_STARTED",
  "data": {
    "startDate": "2026-07-01T00:00:00"
  },
  "errors": null
}
```

## 17.4. Promotion Expired

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Promotion has expired",
  "errorCode": "PROMOTION_EXPIRED",
  "data": null,
  "errors": null
}
```

## 17.5. Usage Limit Reached

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Promotion usage limit has been reached",
  "errorCode": "PROMOTION_USAGE_LIMIT_REACHED",
  "data": null,
  "errors": null
}
```

## 17.6. User Limit Reached

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "You have reached the usage limit for this promotion",
  "errorCode": "PROMOTION_USER_LIMIT_REACHED",
  "data": null,
  "errors": null
}
```

## 17.7. Minimum Amount Not Met

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking amount does not meet the promotion minimum",
  "errorCode": "PROMOTION_MINIMUM_AMOUNT_NOT_MET",
  "data": {
    "minimumAmount": 200000,
    "currentAmount": 150000
  },
  "errors": null
}
```

## 17.8. Booking Already Has Promotion

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking already has an applied promotion",
  "errorCode": "PROMOTION_BOOKING_ALREADY_APPLIED",
  "data": null,
  "errors": null
}
```

---

# 18. Discount Preview API

## 18.1. Endpoint

```http
POST /api/promotions/preview
```

### Request

```json
{
  "promotionCode": "LORAFILM2026",
  "bookingId": 1001
}
```

### Response

```json
{
  "success": true,
  "message": "Discount preview calculated successfully",
  "data": {
    "promotionId": 101,
    "promotionCode": "LORAFILM2026",
    "originalAmount": 240000,
    "discountAmount": 24000,
    "finalAmount": 216000,
    "currency": "VND",
    "previewOnly": true
  }
}
```

Preview:

* Không tạo usage.
* Không giữ lượt dùng.
* Không đảm bảo promotion vẫn còn khả dụng khi apply sau đó.
* Frontend phải hiểu đây chỉ là kết quả tạm thời.

---

# 19. Apply Promotion Internal API

## 19.1. Endpoint

```http
POST /internal/promotions/apply
```

API này do Booking Service hoặc orchestration layer gọi.

Frontend không được gọi trực tiếp.

### Request Body

```json
{
  "promotionCode": "LORAFILM2026",
  "bookingId": 1001,
  "userId": 15,
  "bookingAmount": 240000
}
```

Trong internal flow, `bookingAmount` có thể được Booking Service gửi vì Booking Service là source of truth.

Promotion Service vẫn có thể xác minh lại nếu cần.

### Field Definitions

| Field         | Type   | Required | Validation            |
| ------------- | ------ | -------: | --------------------- |
| promotionCode | string |      Yes | Không rỗng, tối đa 50 |
| bookingId     | number |      Yes | > 0                   |
| userId        | number |      Yes | > 0                   |
| bookingAmount | number |      Yes | >= 0                  |

### Processing Flow

```txt
Normalize code
→ Validate promotion/campaign/time
→ Validate limits
→ Validate bookingId chưa có usage
→ Calculate discount
→ Atomically reserve usage capacity
→ Insert promotion_usages
→ Increment used_count
→ Return discount result
```

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Promotion applied successfully",
  "data": {
    "usageId": 5001,
    "promotionId": 101,
    "promotionCode": "LORAFILM2026",
    "bookingId": 1001,
    "userId": 15,
    "originalAmount": 240000,
    "discountAmount": 24000,
    "finalAmount": 216000,
    "usageStatus": "RESERVED",
    "appliedAt": "2026-06-21T20:15:00"
  }
}
```

Nếu schema không bổ sung status, response có thể bỏ `usageStatus` hoặc trả trạng thái derive.

---

## 19.2. Idempotent Apply

Idempotency key nghiệp vụ:

```txt
bookingId
```

Do schema có:

```txt
promotion_usages.booking_id UNIQUE
```

Nếu cùng booking gọi apply lại cùng promotion:

```txt
Trả usage hiện tại
Không insert thêm
Không tăng used_count thêm
```

Response:

```json
{
  "success": true,
  "message": "Promotion was already applied to this booking",
  "data": {
    "usageId": 5001,
    "promotionId": 101,
    "bookingId": 1001,
    "idempotent": true
  }
}
```

Nếu cùng booking gọi promotion code khác:

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Booking already has another promotion",
  "errorCode": "PROMOTION_BOOKING_ALREADY_APPLIED",
  "data": null,
  "errors": null
}
```

---

# 20. Confirm Usage API

## 20.1. Endpoint

```http
POST /internal/promotions/usages/{usageId}/confirm
```

Dùng khi booking/payment đã hoàn tất.

### Request

```json
{
  "bookingId": 1001,
  "confirmedAt": "2026-06-21T20:20:00"
}
```

### Allowed Transition

```txt
RESERVED → APPLIED
```

### Response

```json
{
  "success": true,
  "message": "Promotion usage confirmed successfully",
  "data": {
    "usageId": 5001,
    "bookingId": 1001,
    "status": "APPLIED"
  }
}
```

### Idempotency

Nếu usage đã `APPLIED`:

* Trả `200 OK`.
* Không tăng `used_count`.
* Không tạo duplicate effect.

---

# 21. Revert Usage API

## 21.1. Endpoint

```http
POST /internal/promotions/usages/{usageId}/revert
```

### Request

```json
{
  "bookingId": 1001,
  "reason": "Booking expired before payment"
}
```

### Allowed Conditions

Có thể revert khi:

```txt
Booking bị CANCELLED
Booking bị EXPIRED
Payment thất bại và booking không còn khả năng thanh toán
Booking creation rollback
```

### Response

```json
{
  "success": true,
  "message": "Promotion usage reverted successfully",
  "data": {
    "usageId": 5001,
    "bookingId": 1001,
    "status": "REVERTED"
  }
}
```

### Side Effects

Khi revert:

```txt
promotion_usages.status → REVERTED
promotions.used_count → used_count - 1
```

Không để `used_count` nhỏ hơn `0`.

### Schema Limitation

Schema hiện tại không có `status`.

Nếu không refactor schema, implementation có thể:

```txt
DELETE promotion_usages
DECREMENT promotions.used_count
```

Nhưng cách này mất lịch sử usage và không khuyến nghị cho production.

---

# 22. Get Usage by Booking

## 22.1. Endpoint

```http
GET /internal/promotions/bookings/{bookingId}
```

### Response Success

```json
{
  "success": true,
  "message": "Promotion usage retrieved successfully",
  "data": {
    "usageId": 5001,
    "promotionId": 101,
    "promotionCode": "LORAFILM2026",
    "bookingId": 1001,
    "userId": 15,
    "status": "APPLIED",
    "appliedAt": "2026-06-21T20:15:00"
  }
}
```

### No Usage

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "No promotion usage found for this booking",
  "errorCode": "PROMOTION_USAGE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

# 23. Get Current User Promotion Usages

## 23.1. Endpoint

```http
GET /api/promotions/me/usages
```

### Query Parameters

| Parameter   | Type     | Required |
| ----------- | -------- | -------: |
| page        | integer  |       No |
| size        | integer  |       No |
| promotionId | number   |       No |
| status      | string   |       No |
| from        | datetime |       No |
| to          | datetime |       No |

### Response

```json
{
  "success": true,
  "message": "Promotion usages retrieved successfully",
  "data": {
    "content": [
      {
        "usageId": 5001,
        "promotionId": 101,
        "promotionCode": "LORAFILM2026",
        "bookingId": 1001,
        "status": "APPLIED",
        "appliedAt": "2026-06-21T20:15:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

# 24. Campaign Admin APIs

## 24.1. Create Campaign

### Endpoint

```http
POST /api/admin/promotion-campaigns
```

### Request

```json
{
  "campaignName": "LoraFilm Summer 2026",
  "description": "Ưu đãi mùa hè",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-31T23:59:59",
  "isActive": true
}
```

### Validation

| Field        | Rule                         |
| ------------ | ---------------------------- |
| campaignName | Required, tối đa 150         |
| description  | Optional                     |
| startDate    | Required                     |
| endDate      | Required, phải sau startDate |
| isActive     | Optional, mặc định true      |

### Response

Status: `201 Created`

```json
{
  "success": true,
  "message": "Promotion campaign created successfully",
  "data": {
    "campaignId": 10,
    "campaignName": "LoraFilm Summer 2026",
    "description": "Ưu đãi mùa hè",
    "startDate": "2026-07-01T00:00:00",
    "endDate": "2026-07-31T23:59:59",
    "isActive": true,
    "availabilityStatus": "UPCOMING",
    "createdAt": "2026-06-21T20:00:00"
  }
}
```

---

## 24.2. Get Campaign List

```http
GET /api/admin/promotion-campaigns
```

Query:

```txt
page
size
isActive
availabilityStatus
from
to
sort
```

Trả pagination theo common format.

---

## 24.3. Get Campaign Detail

```http
GET /api/admin/promotion-campaigns/{campaignId}
```

Response có thể bao gồm:

```json
{
  "campaignId": 10,
  "campaignName": "LoraFilm Summer 2026",
  "promotionCount": 3,
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-31T23:59:59",
  "isActive": true,
  "availabilityStatus": "UPCOMING"
}
```

---

## 24.4. Update Campaign

```http
PUT /api/admin/promotion-campaigns/{campaignId}
```

### Business Rule

Không được cập nhật thời gian làm campaign trở nên không hợp lệ:

```txt
endDate <= startDate
```

Nếu campaign đã có promotion đang được sử dụng, thay đổi thời gian phải được kiểm tra tác động.

---

## 24.5. Enable/Disable Campaign

```http
PATCH /api/admin/promotion-campaigns/{campaignId}/status
```

Request:

```json
{
  "isActive": false
}
```

Khi campaign bị disable:

* Tất cả promotion thuộc campaign không còn được validate/apply.
* Không tự động xóa promotion hoặc usage cũ.
* Usage đã `APPLIED` vẫn được giữ để audit.

---

# 25. Promotion Admin APIs

## 25.1. Create Promotion

### Endpoint

```http
POST /api/admin/promotions
```

### Request — Percentage

```json
{
  "campaignId": 10,
  "promotionCode": "LORAFILM2026",
  "description": "Giảm 10% tối đa 30000 VND",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxDiscountAmount": 30000,
  "minOrderAmount": 200000,
  "usageLimit": 1000,
  "limitPerUser": 1,
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-31T23:59:59",
  "isActive": true
}
```

### Request — Fixed Amount

```json
{
  "campaignId": 10,
  "promotionCode": "GIAM20K",
  "description": "Giảm 20000 VND",
  "discountType": "FIXED_AMOUNT",
  "discountValue": 20000,
  "maxDiscountAmount": null,
  "minOrderAmount": 100000,
  "usageLimit": 500,
  "limitPerUser": 1,
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-31T23:59:59",
  "isActive": true
}
```

### Validation Rules

#### Common

* Campaign phải tồn tại.
* Promotion time range phải hợp lệ.
* Promotion time nên nằm trong campaign time range.
* Code unique sau normalize.
* `usageLimit > 0`.
* `limitPerUser > 0`.
* `minOrderAmount >= 0`.

#### Percentage

```txt
0 < discountValue <= 100
maxDiscountAmount > 0 nếu được cung cấp
```

#### Fixed Amount

```txt
discountValue > 0
maxDiscountAmount phải null
```

### Response

Status: `201 Created`

```json
{
  "success": true,
  "message": "Promotion created successfully",
  "data": {
    "promotionId": 101,
    "campaignId": 10,
    "promotionCode": "LORAFILM2026",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxDiscountAmount": 30000,
    "minOrderAmount": 200000,
    "usageLimit": 1000,
    "usedCount": 0,
    "limitPerUser": 1,
    "isActive": true
  }
}
```

---

## 25.2. Duplicate Code

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Promotion code already exists",
  "errorCode": "PROMOTION_CODE_ALREADY_EXISTS",
  "data": null,
  "errors": null
}
```

---

## 25.3. Get Promotion List

```http
GET /api/admin/promotions
```

Query parameters:

| Parameter          | Type     |
| ------------------ | -------- |
| page               | integer  |
| size               | integer  |
| campaignId         | number   |
| code               | string   |
| discountType       | string   |
| isActive           | boolean  |
| availabilityStatus | string   |
| from               | datetime |
| to                 | datetime |
| sort               | string   |

---

## 25.4. Get Admin Promotion Detail

```http
GET /api/admin/promotions/{promotionId}
```

Response có thể trả đầy đủ:

```json
{
  "success": true,
  "message": "Promotion retrieved successfully",
  "data": {
    "promotionId": 101,
    "campaignId": 10,
    "promotionCode": "LORAFILM2026",
    "description": "Giảm 10% tối đa 30000 VND",
    "discountType": "PERCENTAGE",
    "discountValue": 10,
    "maxDiscountAmount": 30000,
    "minOrderAmount": 200000,
    "usageLimit": 1000,
    "usedCount": 125,
    "remainingUsage": 875,
    "limitPerUser": 1,
    "startDate": "2026-07-01T00:00:00",
    "endDate": "2026-07-31T23:59:59",
    "isActive": true,
    "availabilityStatus": "ACTIVE"
  }
}
```

---

## 25.5. Update Promotion

```http
PUT /api/admin/promotions/{promotionId}
```

### Update Restrictions

Nếu promotion đã có usage:

* Không nên đổi `promotionCode`.
* Không nên đổi `discountType`.
* Không nên sửa `discountValue` theo hướng ảnh hưởng booking đã apply.
* Không được đặt `usageLimit < usedCount`.
* Có thể disable promotion.
* Có thể tăng usage limit.
* Có thể thay đổi end date nếu business cho phép.

Promotion usage cũ vẫn phải giữ discount snapshot đã áp dụng.

Schema hiện chưa lưu snapshot trong `promotion_usages`; đây là điểm cần review.

---

## 25.6. Enable/Disable Promotion

```http
PATCH /api/admin/promotions/{promotionId}/status
```

Request:

```json
{
  "isActive": false
}
```

Disable promotion:

* Ngăn validate/apply mới.
* Không xóa usage cũ.
* Không tự động revert booking đã áp dụng.

---

# 26. Usage Statistics APIs

## 26.1. Get Usage List

```http
GET /api/admin/promotions/{promotionId}/usages
```

Query:

```txt
page
size
userId
bookingId
status
from
to
```

Response:

```json
{
  "success": true,
  "message": "Promotion usages retrieved successfully",
  "data": {
    "content": [
      {
        "usageId": 5001,
        "userId": 15,
        "bookingId": 1001,
        "status": "APPLIED",
        "appliedAt": "2026-06-21T20:15:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 26.2. Get Basic Statistics

```http
GET /api/admin/promotions/{promotionId}/statistics
```

Response:

```json
{
  "success": true,
  "message": "Promotion statistics retrieved successfully",
  "data": {
    "promotionId": 101,
    "usageLimit": 1000,
    "usedCount": 125,
    "remainingUsage": 875,
    "reservedUsageCount": 5,
    "appliedUsageCount": 115,
    "revertedUsageCount": 5
  }
}
```

Các số liệu theo status chỉ khả dụng nếu schema có usage status.

---

# 27. Concurrency Rules

## 27.1. Global Usage Limit

Hai request đồng thời không được làm:

```txt
usedCount > usageLimit
```

Không dùng flow không atomic:

```txt
SELECT used_count
→ kiểm tra
→ UPDATE
```

Khuyến nghị dùng atomic conditional update:

```sql
UPDATE promotions
SET used_count = used_count + 1
WHERE id = :promotionId
  AND used_count < usage_limit;
```

Nếu affected rows bằng `0`:

```txt
PROMOTION_USAGE_LIMIT_REACHED
```

## 27.2. Per-user Limit

Phải đếm active usage của user theo promotion.

Khuyến nghị có index:

```txt
promotion_id + user_id
```

Nếu dùng status:

```txt
Chỉ tính RESERVED và APPLIED
Không tính REVERTED
```

## 27.3. One Promotion per Booking

Dựa trên:

```txt
UNIQUE booking_id
```

Hai apply request đồng thời cho cùng booking:

* Chỉ một usage được tạo.
* Request còn lại phải trả idempotent result hoặc conflict.

## 27.4. Transaction Boundary

Apply promotion phải thực hiện trong một transaction:

```txt
Validate limits
→ Increment usedCount
→ Insert promotion usage
→ Commit
```

Nếu insert usage thất bại:

```txt
Rollback usedCount
```

---

# 28. Idempotency Rules

### Validate/Preview

Không thay đổi dữ liệu nên tự nhiên idempotent.

### Apply

Idempotency theo:

```txt
bookingId
```

### Confirm Usage

Gọi lại cùng `usageId`:

```txt
Không thay đổi usedCount
Không tạo duplicate effect
```

### Revert Usage

Gọi revert nhiều lần:

```txt
Chỉ decrement usedCount một lần
Các lần sau trả trạng thái REVERTED
```

---

# 29. Booking và Payment Integration Direction

## 29.1. Recommended Flow

```txt
Booking được tạo với original amount
→ User nhập promotion code
→ Frontend gọi validate/preview
→ Booking Service gọi internal apply
→ Promotion Service trả discountAmount/finalAmount
→ Booking Service lưu hoặc sử dụng discount snapshot
→ Payment Service lấy final payable amount
→ Payment success
→ Promotion usage được confirm
```

## 29.2. Booking Failure

```txt
Booking CANCELLED hoặc EXPIRED
→ Booking Service gọi revert usage
→ Promotion Service hoàn usedCount
```

## 29.3. Payment Failure

Payment `FAILED` nhưng Booking vẫn còn thời gian retry:

```txt
Không revert usage ngay
```

Chỉ revert khi:

```txt
Booking bị CANCELLED hoặc EXPIRED
```

## 29.4. Source of Truth cho Final Amount

Cần chốt giữa các contract:

```txt
Promotion Service tính discount
Booking Service lưu/giữ final booking amount
Payment Service lấy payable amount từ Booking Service
```

Payment Service không tự gọi Promotion Service để tính lại discount sau khi Booking đã chốt.

---

# 30. Usage Reservation Timeout

Nếu usage được tạo ở trạng thái `RESERVED`, nó phải gắn với thời hạn booking.

Khi booking hết hạn:

```txt
RESERVED → REVERTED
```

Promotion Service có thể:

* Nhận event/call từ Booking Service.
* Hoặc chạy reconciliation job để kiểm tra reservation cũ.

Schema hiện chưa có:

```txt
expires_at
```

Có thể dùng booking expiry từ Booking Service hoặc bổ sung usage expiry nếu cần.

---

# 31. Security Rules

* Public API chỉ trả promotion đang có thể công khai.
* Validate và preview yêu cầu authentication.
* Apply/revert/confirm chỉ dùng internal API.
* Frontend không được trực tiếp tạo `promotion_usages`.
* Frontend không được gửi `userId`.
* Frontend không phải source of truth của booking amount.
* Admin API yêu cầu role/permission phù hợp.
* Internal endpoint không expose công khai qua API Gateway.
* Không log dữ liệu cá nhân không cần thiết.
* Promotion code có thể hiển thị công khai nếu nghiệp vụ cho phép.

Permission đề xuất:

```txt
PROMOTION_READ
PROMOTION_CREATE
PROMOTION_UPDATE
PROMOTION_MANAGE
PROMOTION_USAGE_READ
```

---

# 32. Delete Policy

Không hard delete campaign hoặc promotion đã có usage.

Khuyến nghị:

```txt
is_active = false
```

Lý do:

* Giữ lịch sử booking.
* Hỗ trợ audit.
* Không phá dữ liệu usage.
* Không làm mất thông tin đối soát.

`ON DELETE CASCADE` hiện có thể xóa promotion và toàn bộ usage nếu campaign/promotion bị xóa vật lý.

Reviewer cần xác nhận:

```txt
Có cho phép hard delete hay không.
```

Khuyến nghị:

```txt
Không expose hard delete API trong Sprint 2.
```

---

# 33. Error Code Catalog

| Error Code                             | HTTP | Ý nghĩa                        |
| -------------------------------------- | ---: | ------------------------------ |
| `CAMPAIGN_NOT_FOUND`                   |  404 | Không tìm thấy campaign        |
| `CAMPAIGN_DISABLED`                    |  409 | Campaign bị disable            |
| `CAMPAIGN_NOT_STARTED`                 |  409 | Campaign chưa bắt đầu          |
| `CAMPAIGN_EXPIRED`                     |  409 | Campaign đã hết hạn            |
| `CAMPAIGN_INVALID_DATE_RANGE`          |  400 | Khoảng thời gian không hợp lệ  |
| `PROMOTION_NOT_FOUND`                  |  404 | Không tìm thấy promotion       |
| `PROMOTION_CODE_ALREADY_EXISTS`        |  409 | Code đã tồn tại                |
| `PROMOTION_DISABLED`                   |  409 | Promotion bị disable           |
| `PROMOTION_NOT_STARTED`                |  409 | Promotion chưa bắt đầu         |
| `PROMOTION_EXPIRED`                    |  409 | Promotion đã hết hạn           |
| `PROMOTION_INVALID_DISCOUNT_TYPE`      |  400 | Discount type sai              |
| `PROMOTION_INVALID_DISCOUNT_VALUE`     |  400 | Discount value sai             |
| `PROMOTION_INVALID_DATE_RANGE`         |  400 | Time range sai                 |
| `PROMOTION_USAGE_LIMIT_REACHED`        |  409 | Hết lượt toàn hệ thống         |
| `PROMOTION_USER_LIMIT_REACHED`         |  409 | User hết lượt                  |
| `PROMOTION_MINIMUM_AMOUNT_NOT_MET`     |  409 | Chưa đạt giá trị tối thiểu     |
| `PROMOTION_BOOKING_ALREADY_APPLIED`    |  409 | Booking đã có promotion        |
| `PROMOTION_USAGE_NOT_FOUND`            |  404 | Không tìm thấy usage           |
| `PROMOTION_USAGE_ALREADY_CONFIRMED`    |  409 | Usage đã confirm               |
| `PROMOTION_USAGE_ALREADY_REVERTED`     |  409 | Usage đã revert                |
| `PROMOTION_USAGE_INVALID_TRANSITION`   |  409 | Chuyển trạng thái sai          |
| `PROMOTION_BOOKING_NOT_FOUND`          |  404 | Booking không tồn tại          |
| `PROMOTION_BOOKING_NOT_ELIGIBLE`       |  409 | Booking không đủ điều kiện     |
| `PROMOTION_BOOKING_OWNERSHIP_MISMATCH` |  403 | Booking không thuộc user       |
| `BOOKING_SERVICE_UNAVAILABLE`          |  503 | Booking Service không khả dụng |
| `VALIDATION_ERROR`                     |  400 | Validation lỗi                 |
| `UNAUTHORIZED`                         |  401 | Chưa đăng nhập                 |
| `FORBIDDEN`                            |  403 | Không có quyền                 |
| `INTERNAL_SERVER_ERROR`                |  500 | Lỗi hệ thống                   |

---

# 34. Schema Alignment Notes

## 34.1. Usage Status

Contract đề xuất:

```txt
RESERVED
APPLIED
REVERTED
```

Schema chưa có `promotion_usages.status`.

## 34.2. Discount Snapshot

Contract trả:

```txt
originalAmount
discountAmount
finalAmount
```

Schema usage chưa lưu các field này.

Nếu Booking Service lưu snapshot đầy đủ thì Promotion Service có thể không cần lưu. Reviewer phải chốt source of truth.

## 34.3. Revert Tracking

Schema chưa có:

```txt
reverted_at
revert_reason
```

## 34.4. User Usage Index

Nên xem xét index:

```txt
(promotion_id, user_id)
```

để kiểm tra per-user limit.

## 34.5. Usage Uniqueness

Schema hiện chỉ unique:

```txt
booking_id
```

Có thể cần thêm rule/index phù hợp tùy lifecycle.

## 34.6. Optimistic Locking

Schema chưa có:

```txt
version
```

Có thể dùng atomic update trên `used_count` thay vì optimistic locking.

## 34.7. Cascade Delete

`ON DELETE CASCADE` có thể làm mất usage history nếu hard delete.

Khuyến nghị không hard delete qua API.

## 34.8. Used Count Consistency

`used_count` có thể lệch với số record usage nếu:

* Transaction không atomic.
* Revert lỗi.
* Manual data update.

Có thể cần reconciliation job hoặc query kiểm tra định kỳ.

---

# 35. Out of Scope

* Recommendation Engine.
* Personalized promotion.
* Promotion stacking.
* Multiple promotion trên một booking.
* Dynamic segmentation.
* Coupon distribution campaign.
* Referral code.
* Gift card.
* Cashback.
* Score redemption.
* Production Kafka/outbox implementation.
* Booking integration implementation thật.
* Payment integration implementation thật.
* Advanced analytics dashboard.
* Hard delete API.
* Backend code trong issue contract này.

---

# 36. Implementation Issue Direction

Sau khi contract được review và schema alignment hoàn tất nếu cần, có thể tách:

```txt
[Backend] Implement Promotion Campaign Management APIs

[Backend] Implement Promotion Management and Query APIs

[Backend] Implement Promotion Validation and Discount Calculation

[Backend] Implement Promotion Usage Apply, Confirm and Revert Flow
```

Nếu giảm scope Sprint 2:

```txt
Issue 1: Campaign and Promotion CRUD
Issue 2: Validate and Preview Promotion
Issue 3: Apply and Usage Tracking Foundation
```

Implementation issue chỉ chuyển `Ready` khi:

```txt
Contract đã được duyệt
+
Schema bắt buộc đã align
+
Hoàng xác nhận feasibility
```

---

# 37. Acceptance Criteria

* [ ] Có schema Sprint 0 baseline.
* [ ] Có campaign APIs.
* [ ] Có promotion management APIs.
* [ ] Có public/protected/internal/admin classification.
* [ ] Có validate API.
* [ ] Có discount preview API.
* [ ] Có apply API.
* [ ] Có confirm/revert usage direction.
* [ ] Có discount calculation rules.
* [ ] Có global usage limit.
* [ ] Có per-user usage limit.
* [ ] Có minimum booking amount rule.
* [ ] Có max percentage discount rule.
* [ ] Có fixed discount cap rule.
* [ ] Có concurrency rules.
* [ ] Có atomic used_count update.
* [ ] Có idempotency theo booking.
* [ ] Có logical reference notes.
* [ ] Có Booking/Payment integration direction.
* [ ] Có security notes.
* [ ] Có status/error code.
* [ ] Có schema mismatch notes.
* [ ] Hoàng review feasibility.
* [ ] Contract sẵn sàng cho implementation.
* [ ] MR target `develop`.

---

# 38. Các Điểm Reviewer Cần Xác Nhận

Hoàng cần xác nhận:

1. Promotion Service port chính thức.
2. Một booking chỉ được áp dụng một promotion hay không.
3. Có giữ `promotion_usages.booking_id UNIQUE` không.
4. Usage được tạo lúc apply hay chỉ sau payment success.
5. Có cần lifecycle `RESERVED/APPLIED/REVERTED` không.
6. Có thêm `promotion_usages.status` không.
7. Có lưu `discount_amount` snapshot trong usage không.
8. Booking Service hay Promotion Service giữ final amount snapshot.
9. Revert usage dùng status hay xóa record.
10. Có thêm `reverted_at` và `revert_reason` không.
11. `used_count` tăng lúc reserve hay lúc payment success.
12. Nếu tăng lúc reserve, khi booking expire phải revert thế nào.
13. Payment fail nhưng booking còn retry có giữ usage không.
14. Discount rounding dùng `HALF_UP` hay rule khác.
15. Promotion time phải nằm hoàn toàn trong campaign time không.
16. Có cho sửa promotion sau khi đã có usage không.
17. Có cần index `(promotion_id, user_id)` không.
18. Có giữ `ON DELETE CASCADE` không.
19. Internal apply được Booking Service gọi qua REST hay Kafka.
20. Có cần schema alignment issue trước implementation không.

---

# 39. Lịch Sử Chỉnh Sửa

| Ngày       | Nội dung                                                         | Người thực hiện  |
| ---------- | ---------------------------------------------------------------- | ---------------- |
| 21/06/2026 | Khởi tạo Promotion Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận tại đây sau khi schema MR tương ứng đã được merge.
