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
| Trạng thái     | Approved / Ready for Implementation                                      |
| Milestone      | Sprint 2 - Core Service API Foundation                                    |
| Ngày cập nhật  | 24/06/2026                                                                |

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

## 5. Phân Tích Schema và Quyết Định Chính Thức

### 5.1. Những nghiệp vụ schema Sprint 0 hỗ trợ

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

### 5.2. Một promotion cho mỗi booking

Sprint 2 giữ constraint:

```txt
promotion_usages.booking_id UNIQUE
```

Điều này có nghĩa:

```txt
Một booking chỉ được áp dụng tối đa một promotion.
```

Promotion stacking không nằm trong Sprint 2.

### 5.3. Promotion Usage Lifecycle

Lifecycle chính thức:

```txt
RESERVED
APPLIED
REVERTED
```

Allowed transitions:

```txt
RESERVED → APPLIED
RESERVED → REVERTED
```

Không cho:

```txt
APPLIED → RESERVED
REVERTED → RESERVED
REVERTED → APPLIED
```

Usage được tạo ngay khi Apply:

```txt
Apply
→ Create RESERVED usage
→ Increment used_count
```

Payment success:

```txt
RESERVED → APPLIED
```

Booking cancelled hoặc expired:

```txt
RESERVED → REVERTED
→ Decrement used_count đúng một lần
```

Payment `FAILED` nhưng booking vẫn còn thời gian retry:

```txt
Giữ usage ở RESERVED
```

### 5.4. Discount Snapshot và Source of Truth

Promotion Service bắt buộc lưu snapshot tại thời điểm apply:

```txt
original_amount
discount_amount
final_amount
```

Source of truth được chốt:

```txt
Promotion Service
→ tính discount
→ lưu discount snapshot để audit

Booking Service
→ lưu finalAmount chính thức của booking

Payment Service
→ lấy payable amount từ Booking Service
→ không tự tính lại discount
```

### 5.5. Revert Audit và Expiration

Promotion usage phải lưu:

```txt
reverted_at
revert_reason
updated_at
expires_at
```

`expires_at` là snapshot của `booking.expires_at` tại thời điểm apply.

Booking Service vẫn là source of truth của booking expiry.

Promotion Service dùng `promotion_usages.expires_at` cho:

* Reconciliation.
* Cleanup usage bị treo.
* Phục hồi khi internal call từ Booking Service bị lỗi.

### 5.6. Concurrency và Optimistic Locking

Schema phải bổ sung:

```txt
promotions.version
promotion_usages.version
```

Entity tương ứng dùng `@Version`.

Ngoài optimistic locking, tăng `used_count` vẫn phải dùng atomic conditional update để không vượt `usage_limit`.

### 5.7. Delete Policy

Sprint 2 không hard delete campaign hoặc promotion.

Chỉ disable bằng:

```txt
is_active = false
```

Foreign key nội bộ phải dùng `ON DELETE RESTRICT` hoặc behavior tương đương để tránh mất usage history.

### 5.8. Schema Alignment Bắt Buộc

Trước Backend implementation, schema phải bổ sung:

```txt
promotion_usages.status
promotion_usages.original_amount
promotion_usages.discount_amount
promotion_usages.final_amount
promotion_usages.expires_at
promotion_usages.reverted_at
promotion_usages.revert_reason
promotion_usages.updated_at
promotion_usages.version
promotions.version
index (promotion_id, user_id, status)
index (status, expires_at)
```

Schema được cập nhật trong issue:

```txt
[Database] Align Promotion Schema with Promotion API Contract
```

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

### 10.3. PromotionUsageStatus

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

Status này là bắt buộc và đã được schema alignment trước implementation.

### 10.4. Promotion Usage Timestamp Semantics

API response sử dụng các field lifecycle sau:

```txt
reservedAt
confirmedAt
revertedAt
revertReason
```

Quy ước:

| Field | Ý nghĩa |
|---|---|
| `reservedAt` | Thời điểm tạo usage ở trạng thái `RESERVED`; luôn có giá trị |
| `confirmedAt` | Thời điểm chuyển `RESERVED → APPLIED`; null nếu chưa confirm |
| `revertedAt` | Thời điểm chuyển `RESERVED → REVERTED`; null nếu chưa revert |
| `revertReason` | Lý do nghiệp vụ khi revert; null nếu usage chưa `REVERTED` |

Không sử dụng field `appliedAt` vì tên này gây nhầm lẫn giữa thời điểm reserve và thời điểm confirm.

Mapping schema:

```txt
reservedAt  ↔ promotion_usages.reserved_at
confirmedAt ↔ promotion_usages.confirmed_at
revertedAt  ↔ promotion_usages.reverted_at
revertReason ↔ promotion_usages.revert_reason
```

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

Discount calculation dùng:

```txt
Data type: BigDecimal
Rounding mode: HALF_UP
Scale: 0 đối với VND
```

Không dùng `double` hoặc `float` cho phép tính tài chính.

Ví dụ:

```java
discountAmount = rawDiscount.setScale(0, RoundingMode.HALF_UP);
finalAmount = bookingAmount.subtract(discountAmount).max(BigDecimal.ZERO);
```

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
    "expiresAt": "2026-06-21T20:30:00",
    "reservedAt": "2026-06-21T20:15:00",
    "confirmedAt": null,
    "revertedAt": null,
    "revertReason": null
  }
}
```

`usageStatus` được lưu trực tiếp trong `promotion_usages.status`.

`expiresAt` được copy từ `booking.expires_at` tại thời điểm apply.

`reservedAt` là thời điểm tạo usage ở trạng thái `RESERVED`.

`confirmedAt` chỉ có giá trị khi usage chuyển `RESERVED → APPLIED`.

`revertedAt` và `revertReason` chỉ có giá trị khi usage chuyển `RESERVED → REVERTED`.

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
    "status": "APPLIED",
    "reservedAt": "2026-06-21T20:15:00",
    "confirmedAt": "2026-06-21T20:20:00",
    "revertedAt": null,
    "revertReason": null
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
    "status": "REVERTED",
    "reservedAt": "2026-06-21T20:15:00",
    "confirmedAt": null,
    "revertedAt": "2026-06-21T20:25:00",
    "revertReason": "Booking expired before payment"
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

### Audit Fields

Khi revert phải cập nhật:

```txt
status = REVERTED
reverted_at = thời điểm revert
revert_reason = lý do nghiệp vụ
updated_at = thời điểm cập nhật
```

Không xóa `promotion_usages` vì phải giữ lịch sử audit.

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
    "reservedAt": "2026-06-21T20:15:00",
    "confirmedAt": "2026-06-21T20:20:00",
    "revertedAt": null,
    "revertReason": null
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
        "reservedAt": "2026-06-21T20:15:00",
        "confirmedAt": "2026-06-21T20:20:00",
        "revertedAt": null,
        "revertReason": null
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
        "reservedAt": "2026-06-21T20:15:00",
        "confirmedAt": "2026-06-21T20:20:00",
        "revertedAt": null,
        "revertReason": null
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
→ Promotion Service tính discount
→ Promotion Service lưu discount snapshot
→ Promotion Service trả discountAmount/finalAmount
→ Booking Service lưu finalAmount chính thức
→ Payment Service lấy payable amount từ Booking Service
→ Payment success
→ Promotion usage được confirm APPLIED
```

## 29.2. Booking Failure

```txt
Booking CANCELLED hoặc EXPIRED
→ Booking Service gọi revert usage
→ Promotion Service chuyển RESERVED → REVERTED
→ Promotion Service giảm used_count đúng một lần
```

## 29.3. Payment Failure

Payment `FAILED` nhưng Booking vẫn còn thời gian retry:

```txt
Không revert usage
Giữ usage ở RESERVED
```

Chỉ revert khi Booking Service xác nhận booking đã:

```txt
CANCELLED
EXPIRED
```

## 29.4. Source of Truth cho Amount

```txt
Promotion Service
→ source of truth cho promotion rule và discount calculation
→ lưu originalAmount, discountAmount, finalAmount để audit

Booking Service
→ source of truth cho finalAmount chính thức của booking

Payment Service
→ chỉ lấy payable amount từ Booking Service
→ không tự gọi Promotion Service để tính lại discount
```

## 29.5. Internal Communication

Sprint 2 dùng Internal REST API:

```txt
POST /internal/promotions/apply
POST /internal/promotions/usages/{usageId}/confirm
POST /internal/promotions/usages/{usageId}/revert
GET  /internal/promotions/bookings/{bookingId}
```

Internal API sử dụng:

```http
X-Internal-Token: <internal-token>
```

Frontend không được gọi trực tiếp các endpoint này.

# 30. Usage Reservation Timeout

Booking Service là source of truth của booking expiry.

Khi tạo usage:

```txt
promotion_usages.expires_at = booking.expires_at
```

Nếu booking hết hạn:

```txt
RESERVED → REVERTED
→ decrement used_count đúng một lần
```

Promotion Service có thể nhận internal call từ Booking Service hoặc chạy reconciliation worker dựa trên:

```txt
status = RESERVED
AND expires_at < now
```

Worker chỉ là cơ chế phục hồi; không thay thế Booking Service notification.

Worker phải idempotent và không được revert usage đã `APPLIED` hoặc `REVERTED`.

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

Sprint 2 không expose Hard Delete API cho campaign hoặc promotion.

Campaign và promotion chỉ được disable bằng:

```txt
is_active = false
```

Lý do:

* Giữ lịch sử booking.
* Hỗ trợ audit.
* Không phá dữ liệu usage.
* Không làm mất thông tin đối soát.

Foreign key nội bộ phải dùng:

```txt
ON DELETE RESTRICT
```

hoặc behavior tương đương.

Không dùng `ON DELETE CASCADE` nếu có thể làm mất `promotion_usages`.

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
| `PROMOTION_OPTIMISTIC_LOCK_CONFLICT`    |  409 | Dữ liệu bị cập nhật đồng thời  |
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

Bắt buộc bổ sung:

```txt
promotion_usages.status
```

Allowed values:

```txt
RESERVED
APPLIED
REVERTED
```

## 34.2. Discount Snapshot

Bắt buộc lưu:

```txt
original_amount
discount_amount
final_amount
```

Promotion Service lưu snapshot này để audit.

Booking Service lưu `finalAmount` chính thức của booking.

## 34.3. Usage Lifecycle Timestamps

Schema chính thức sử dụng:

```txt
reserved_at
confirmed_at
reverted_at
revert_reason
```

API Contract tương ứng sử dụng:

```txt
reservedAt
confirmedAt
revertedAt
revertReason
```

Không dùng `applied_at` hoặc `appliedAt` trong schema/API chính thức.

## 34.4. Revert Tracking

Bắt buộc bổ sung:

```txt
reverted_at
revert_reason
updated_at
```

Không xóa usage record khi revert.

## 34.5. Usage Expiration

Bắt buộc bổ sung:

```txt
expires_at
```

Giá trị được copy từ `booking.expires_at` khi apply.

## 34.6. User Usage Index

Bắt buộc bổ sung:

```txt
(promotion_id, user_id, status)
```

để tối ưu kiểm tra per-user limit trên usage `RESERVED` và `APPLIED`.

## 34.7. Expiration Index

Bắt buộc bổ sung:

```txt
(status, expires_at)
```

để hỗ trợ reconciliation worker.

## 34.8. Usage Uniqueness

Tiếp tục giữ:

```txt
booking_id UNIQUE
```

Sprint 2 không hỗ trợ promotion stacking.

## 34.9. Optimistic Locking

Bắt buộc bổ sung:

```txt
promotions.version
promotion_usages.version
```

Entity sử dụng `@Version`.

## 34.10. Cascade Delete

Rà soát và loại bỏ `ON DELETE CASCADE` có thể làm mất usage history.

Dùng `ON DELETE RESTRICT` hoặc disable bằng `is_active = false`.

## 34.11. Used Count Consistency

Apply phải atomic:

```txt
Increment used_count
+
Insert RESERVED usage
```

Revert phải atomic:

```txt
RESERVED → REVERTED
+
Decrement used_count đúng một lần
```

Có thể bổ sung reconciliation job để phát hiện lệch dữ liệu.

## 34.12. Related Schema Issue

```txt
[Database] Align Promotion Schema with Promotion API Contract
```

Schema Alignment MR phải merge trước Backend implementation.

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

Implementation chỉ bắt đầu sau khi:

```txt
Promotion Contract MR được merge
+
Promotion Schema Alignment MR được merge
+
SQL và Physical ERD đã đồng bộ
```

Các implementation issue đề xuất:

```txt
[Backend] Implement Promotion Campaign Management APIs

[Backend] Implement Promotion Management and Query APIs

[Backend] Implement Promotion Validation and Discount Calculation

[Backend] Implement Promotion Usage Apply, Confirm and Revert Flow

[Backend] Implement Promotion Usage Reconciliation Worker
```

Thứ tự đề xuất:

```txt
Schema Alignment
→ Campaign/Promotion Management
→ Validate/Preview
→ Apply RESERVED Usage
→ Confirm/Revert Flow
→ Reconciliation Worker
```

Mọi thay đổi endpoint, request, response hoặc business rule phải cập nhật contract trong cùng MR.

# 37. Acceptance Criteria

* [x] Có schema Sprint 0 baseline.
* [x] Có campaign APIs.
* [x] Có promotion management APIs.
* [x] Có public/protected/internal/admin classification.
* [x] Có validate API.
* [x] Có discount preview API.
* [x] Có apply API.
* [x] Có confirm/revert usage direction.
* [x] Có lifecycle timestamp contract: reservedAt, confirmedAt, revertedAt, revertReason.
* [x] Có discount calculation rules.
* [x] Có global usage limit.
* [x] Có per-user usage limit.
* [x] Có minimum booking amount rule.
* [x] Có max percentage discount rule.
* [x] Có fixed discount cap rule.
* [x] Có concurrency rules.
* [x] Có atomic used_count update.
* [x] Có idempotency theo booking.
* [x] Có logical reference notes.
* [x] Có Booking/Payment integration direction.
* [x] Có security notes.
* [x] Có status/error code.
* [x] Có schema alignment requirements.
* [x] Có discount snapshot source-of-truth decision.
* [x] Có usage expiry và reconciliation direction.
* [x] Có optimistic locking direction.
* [x] Có soft-delete/restrict delete policy.
* [x] Hoàng review feasibility.
* [x] Contract sẵn sàng cho implementation.
* [x] MR target `develop`.

---

# 38. Review Decisions

Promotion Service Owner đã review và xác nhận:

1. Promotion Usage Lifecycle:

   ```txt
   RESERVED
   APPLIED
   REVERTED
   ```

2. Usage được tạo và `used_count` tăng ngay khi Apply.

3. Payment Success chuyển usage:

   ```txt
   RESERVED → APPLIED
   ```

4. Booking `CANCELLED` hoặc `EXPIRED` chuyển usage:

   ```txt
   RESERVED → REVERTED
   ```

5. Payment `FAILED` không revert nếu booking vẫn còn thời gian retry.

6. Một booking chỉ áp dụng một promotion trong Sprint 2.

7. Tiếp tục giữ:

   ```txt
   promotion_usages.booking_id UNIQUE
   ```

8. Promotion Service lưu:

   ```txt
   originalAmount
   discountAmount
   finalAmount
   ```

   để audit.

9. Booking Service lưu `finalAmount` chính thức của booking.

10. Payment Service chỉ lấy payable amount từ Booking Service.

11. Promotion usage lưu:

    ```txt
    reverted_at
    revert_reason
    updated_at
    expires_at
    ```

12. `expires_at` được copy từ booking expiry.

13. Internal integration sử dụng REST và `X-Internal-Token` trong Sprint 2.

14. Discount calculation sử dụng:

    ```txt
    BigDecimal
    RoundingMode.HALF_UP
    Scale 0 cho VND
    ```

15. Bắt buộc có optimistic locking bằng `version` cho `promotions` và `promotion_usages`.

16. Bắt buộc có index:

    ```txt
    (promotion_id, user_id, status)
    (status, expires_at)
    ```

17. Không hard delete campaign hoặc promotion trong Sprint 2.

18. Phải rà soát và loại bỏ `ON DELETE CASCADE` có thể làm mất usage history.

19. Schema alignment đã hoàn thành và khớp với API Contract trước Backend implementation.

20. Usage lifecycle timestamp chính thức:

    ```txt
    reservedAt  ↔ reserved_at
    confirmedAt ↔ confirmed_at
    revertedAt  ↔ reverted_at
    revertReason ↔ revert_reason
    ```

21. Không sử dụng `appliedAt` trong response chính thức vì field này không phân biệt được reserve và confirm.

22. Contract được chốt ở trạng thái:

    ```txt
    Approved / Ready for Implementation
    ```

# 39. Lịch Sử Chỉnh Sửa

| Ngày       | Nội dung                                                         | Người thực hiện  |
| ---------- | ---------------------------------------------------------------- | ---------------- |
| 21/06/2026 | Khởi tạo Promotion Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật theo review của Promotion Service Owner: usage lifecycle, discount snapshot, revert audit, expiry, optimistic locking, indexes và delete policy | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận tại đây sau khi schema MR tương ứng đã được merge.


| 24/06/2026 | Đồng bộ lifecycle timestamps với schema và approve implementation | Dương Thiện Nhân |
