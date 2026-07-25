# SCORE SERVICE — DESIGN & IMPLEMENTATION PLAN (v1)

### Movie Ticketing Platform — Microservices Architecture
### Reference: CGV, Galaxy Cinema, Lotte Cinema (Vietnam)
### Scope: **score-service độc lập** (không bao gồm promotion-service)

---

# 01. Introduction

Score Service chịu trách nhiệm quản lý toàn bộ **Membership**, **Loyalty Point**, **Membership Tier**, **Point Ledger**, **Point History** và toàn bộ vòng đời điểm thưởng của khách hàng.

Đây là **Single Source of Truth** của toàn bộ dữ liệu điểm và hạng thành viên trong hệ thống.

Các service khác (Promotion, Booking, Analytics, Notification...) chỉ được đọc hoặc yêu cầu thay đổi thông qua Internal API hoặc Event.

---

# 02. Business Vision

> "Mọi điểm thưởng và hạng thành viên đều được quản lý tập trung tại Score Service nhằm đảm bảo tính chính xác, nhất quán và có thể kiểm toán."

Mục tiêu:

- Gia tăng tỷ lệ khách hàng quay lại.
- Khuyến khích mua vé thường xuyên.
- Xây dựng chương trình Loyalty lâu dài.
- Cho phép Marketing thay đổi Tier Rule mà không sửa dữ liệu.
- Hỗ trợ Audit, Reconciliation và Financial Tracking.

---

# 03. Goals

1. Quản lý Membership.
2. Quản lý Loyalty Point.
3. Quản lý Membership Tier.
4. Earn Point.
5. Redeem Point.
6. Hold / Commit / Release Point.
7. Point Expiration.
8. Tier Upgrade / Downgrade.
9. Manual Adjustment.
10. Audit toàn bộ lịch sử.
11. Đồng bộ với Booking / Promotion / Analytics.
12. Đảm bảo Idempotency và Data Consistency.

---

# 04. Scope

Score Service chịu trách nhiệm:

- Membership
- Membership Tier
- Point Wallet
- Point Ledger
- Point History
- Earn Rule
- Redeem Rule
- Point Hold
- Point Commit
- Point Release
- Point Expiration
- Tier Calculation
- Tier Upgrade
- Tier Downgrade
- Lifetime Point
- Admin Adjustment
- CSV Bulk Adjustment
- Scheduler
- Reconciliation
- Audit

---

# 05. Out Of Scope

Không chịu trách nhiệm:

| Chức năng | Thuộc Service |
|------------|---------------|
| Booking | booking-service |
| Payment | payment-service |
| Promotion | promotion-service |
| Movie | movie-service |
| User Authentication | auth-service |
| User Profile | user-service |
| Notification | notification-service |

---

# 06. Business Domain

| Domain | Owner |
|---------|-------|
| Membership | Score |
| Tier | Score |
| Loyalty Point | Score |
| Point Ledger | Score |
| Point History | Score |
| Tier Benefit | Promotion |
| Discount | Promotion |
| Coupon | Promotion |
| Voucher | Promotion |
| Booking | Booking |
| Payment | Payment |

---

# 07. Ubiquitous Language

| Thuật ngữ | Ý nghĩa |
|-----------|----------|
| Membership | Tài khoản Loyalty |
| Membership Tier | Hạng thành viên |
| Current Point | Điểm hiện có |
| Lifetime Point | Tổng điểm tích lũy |
| Earn | Cộng điểm |
| Redeem | Dùng điểm |
| Hold | Giữ điểm |
| Commit | Trừ điểm chính thức |
| Release | Trả điểm Hold |
| Expire | Hết hạn điểm |
| Ledger | Sổ cái giao dịch |
| History | Lịch sử hiển thị |
| Adjustment | Admin cộng/trừ |
| Reconciliation | Đối soát |
| Idempotency | Chống xử lý trùng |

---

# 08. Business Rules

## Membership

| Rule | Decision |
|------|----------|
| Membership tạo khi | OTP Verify thành công |
| Membership/User | 1-1 |
| Membership ID | Immutable |
| Guest | Không hỗ trợ |
| Đổi SĐT | Giữ Membership |
| Merge Account | Không hỗ trợ |
| Delete User | Theo User Service |
| Lock User | Theo User Service |
| Soft Delete | Theo User Service |

---

## Membership Tier

Tier được cấu hình động.

Mặc định:

- SILVER
- GOLD
- DIAMOND

Admin có thể:

- thêm Tier
- sửa điều kiện
- bật/tắt Tier

Không hard-code.

Tier được xác định bằng:

Lifetime Point.

---

Tier không giảm khi:

- Redeem Point
- Point Expired

Tier chỉ giảm khi:

User không phát sinh giao dịch mua vé trong hơn 01 tháng.

---

Tier Upgrade:

- Real-time
- Sau giao dịch Earn thành công.

Tier Downgrade:

- Scheduler chạy định kỳ.

---

# 09. Responsibilities

## Score Service PHẢI

- Quản lý Membership.
- Tính Earn Point.
- Quản lý Point Wallet.
- Quản lý Tier.
- Quản lý Lifetime Point.
- Kiểm tra đủ điểm.
- Hold Point.
- Commit Point.
- Release Point.
- Expire Point.
- Revoke Earn.
- Recalculate Tier.
- Audit toàn bộ giao dịch.
- Publish Event.

---

## Score Service KHÔNG

- Không giảm giá.
- Không giữ ghế.
- Không tạo Booking.
- Không thanh toán.
- Không gửi Email.
- Không quản lý Voucher.
- Không quản lý Coupon.
- Không tính Promotion.

---

# 10. Domain Model

```

User
│
├── Membership
│
├── Point Wallet
│
├── Membership Tier
│
├── Point Ledger
│
├── Point History
│
└── Point Expiration

```

Relationship

Membership (1)
│
├── (1) Wallet
├── (N) Ledger
├── (N) History
└── (1) Tier

---

# 11. Aggregate Design

Membership Aggregate

```

Membership
├── Wallet
├── Tier
├── Ledger
└── History

```

Invariant:

- Wallet >= 0
- Lifetime >= Current
- Tier luôn hợp lệ
- Ledger immutable
- Membership chỉ có 1 Wallet

---

# 12. Lifecycle

Membership

```

OTP Verified
↓

ACTIVE
↓

LOCKED
↓

ACTIVE
↓

SOFT_DELETED

```

Point

```

Available
↓

Hold
↓

Commit

```

hoặc

```

Available
↓

Hold
↓

Release

```

hoặc

```

Available
↓

Expired

```

Tier

```

Silver
↓

Gold
↓

Diamond

```

hoặc

```

Diamond
↓

Gold
↓

Silver

```

(Downgrade Scheduler)

---
# 13. Architecture

```text
                    API Gateway
                         │
        ┌────────────────┼────────────────┐
        │                │                │
 booking-service   promotion-service  admin-api
        │                │                │
        └────────────────┼────────────────┘
                         │
                  SCORE SERVICE
 ┌──────────────────────────────────────────────┐
 │ Membership Module                            │
 │ Tier Module                                  │
 │ Point Engine                                 │
 │ Earn Engine                                  │
 │ Redeem Engine                                │
 │ Hold Engine                                  │
 │ Expiration Engine                            │
 │ Adjustment Engine                            │
 │ Reconciliation Module                        │
 │ Scheduler                                    │
 │ Internal API                                │
 └──────────────────────────────────────────────┘
             │                     │
          MySQL                 Redis
             │                     │
             └────────Kafka────────┘
```

Nguyên tắc:

- Stateless Service.
- Database chỉ chứa dữ liệu Score.
- Không JOIN DB với service khác.
- Giao tiếp bằng REST Internal API hoặc Kafka Event.

---

# 14. Internal Modules

```text
score-service/

├── membership-module/
├── membership-tier-module/
├── point-wallet-module/
├── earn-module/
├── redeem-module/
├── hold-module/
├── expiration-module/
├── adjustment-module/
├── reconciliation-module/
├── scheduler/
├── kafka/
├── acl/
│   ├── booking-client/
│   ├── promotion-client/
│   └── analytics-client/
├── admin-api/
├── customer-api/
└── internal-api/
```

---

# 15. Database Design

## membership

| Column | Description |
|---------|-------------|
| id | UUID |
| user_id | FK User |
| tier_id | Current Tier |
| status | ACTIVE / LOCKED / DELETED |
| created_at | Created Time |
| updated_at | Updated Time |

---

## membership_tier

| Column | Description |
|---------|-------------|
| id | UUID |
| name | SILVER / GOLD... |
| min_lifetime_point | Điều kiện |
| earn_rate | % Earn |
| max_earn_per_booking | Earn Limit |
| max_redeem_per_booking | Redeem Limit |
| priority | Order |
| active | Boolean |

Admin có thể:

- thêm Tier
- disable Tier
- thay Earn Rate
- thay điều kiện

Không cần deploy.

---

## point_wallet

| Column | Description |
|---------|-------------|
| membership_id | FK |
| current_point | Có thể dùng |
| hold_point | Đang Hold |
| lifetime_point | Không bao giờ giảm |
| expired_point | Tổng đã hết hạn |
| updated_at | Audit |

Rule

```
Available
=
Current
-
Hold
```

Không bao giờ âm.

---

## point_ledger

Immutable.

Không UPDATE.

Không DELETE.

| Column | Description |
|---------|-------------|
| id | UUID |
| membership_id | FK |
| transaction_type | ENUM |
| point_change | +/- |
| before_balance | Before |
| after_balance | After |
| booking_id | Nullable |
| event_id | Nullable |
| idempotency_key | Unique |
| reason | Nullable |
| created_at | Time |

Transaction Type

- EARN
- REDEEM
- HOLD
- COMMIT
- RELEASE
- EXPIRE
- REVOKE
- MANUAL_ADD
- MANUAL_DEDUCT
- IMPORT
- SYSTEM

---

## point_history

Là dữ liệu hiển thị cho khách.

Có thể gom nhiều Ledger thành 1 History.

Ví dụ

```
Earn 50 điểm
```

thay vì

```
Ledger A

Ledger B

Ledger C
```

---

## point_hold

| Column | Description |
|---------|-------------|
| hold_id | UUID |
| booking_id | FK |
| membership_id | FK |
| point | Hold Amount |
| expired_at | TTL |
| status | HOLD / COMMIT / RELEASE |

---

## point_expiration_bucket

Dùng FIFO.

| Bucket | Point | Expire Date |
|---------|-------|-------------|
| 2026-01 | 150 | 31/12/2026 |
| 2026-05 | 80 | 31/12/2026 |
| 2027-03 | 120 | 31/12/2027 |

Không expire trực tiếp Wallet.

Expire theo Bucket.

---

# 16. Redis Strategy

Redis chỉ cache.

Không là Source of Truth.

Keys

```
score:wallet:{membershipId}

score:tier:{membershipId}

score:hold:{bookingId}

score:idempotency:{key}

score:lock:{membershipId}
```

TTL

| Key | TTL |
|------|-----|
| Wallet | 15 phút |
| Tier | 30 phút |
| Hold | Booking TTL |
| Idempotency | 24 giờ |

---

# 17. Cache Strategy

| Data | Strategy |
|------|----------|
| Wallet | Cache Aside |
| Tier | Cache Aside |
| Tier Config | Startup Cache |
| Membership | Cache Aside |
| Hold | Redis Only |
| Admin Config | Refresh định kỳ |

Cache bị xóa khi:

- Earn
- Redeem
- Commit
- Release
- Expire
- Adjustment
- Tier Change

---

# 18. ACL (Anti Corruption Layer)

Score chỉ biết ID.

Không biết logic Booking.

Không biết Promotion.

ACL

```
Booking Adapter

Promotion Adapter

Analytics Adapter
```

Không để DTO ngoài đi vào Domain.

---

# 19. Kafka Events

## Consume

```
booking.completed

booking.refunded

booking.cancelled

promotion.point.commit

promotion.point.release

user.deleted

user.locked
```

---

## Produce

```
score.earned

score.redeemed

score.expired

score.revoked

score.adjusted

score.tier.upgraded

score.tier.downgraded
```

Analytics chỉ Subscribe.

Không gọi API.

---

# 20. Scheduler

| Job | Frequency |
|------|-----------|
| Expiration | Daily |
| Tier Downgrade | Daily |
| Hold Timeout | Every Minute |
| Reconciliation | Nightly |
| Redis Cleanup | Hourly |
| Cache Refresh | Every 30 mins |
| Import Retry | Every 10 mins |

---

# 21. Sequence - Earn

```text
Booking Completed
        │
        ▼
Booking Event
        │
        ▼
Score Validate
        │
        ▼
Calculate Earn
        │
        ▼
Update Wallet
        │
        ▼
Update Lifetime
        │
        ▼
Recalculate Tier
        │
        ▼
Save Ledger
        │
        ▼
Publish Event
```

---

# 22. Sequence - Redeem

```text
Booking Create
      │
      ▼
Redeem Request
      │
      ▼
Enough Point?
      │
      ▼
YES
      │
      ▼
Hold Point
      │
      ▼
Payment
```

Payment Success

```
Commit Hold

↓

Ledger

↓

Event
```

Payment Fail

```
Release Hold

↓

Wallet Restore

↓

Ledger
```

---

# 23. Sequence - Refund

```text
Refund Request
      │
      ▼
Calculate Ratio
      │
      ▼
Revoke Earn
      │
      ▼
Point >= 0
      │
      ▼
Update Wallet
      │
      ▼
Recalculate Tier
      │
      ▼
Ledger
```

Rule

```
actualDeduct

=

min(
wallet.current,
requested
)
```

Không cho Wallet âm.
# 24. Membership Engine

Membership là thực thể gốc (Aggregate Root) của Score Service.

Mỗi User chỉ có duy nhất 01 Membership.

```text
User
 │
 └── Membership
      ├── Wallet
      ├── Tier
      ├── Ledger
      └── History
```

## Business Rules

| Rule | Decision |
|------|----------|
| Membership tạo khi | OTP Verify thành công |
| Membership/User | 1-1 |
| Membership ID | Không đổi |
| Guest | Không có Membership |
| Soft Delete User | Membership bị khóa |
| Lock User | Không Earn/Redeem |
| Unlock | Tiếp tục sử dụng |

---

# 25. Membership Tier Engine

Tier được cấu hình bởi Admin.

Không hard-code.

## Default Tier

| Tier | Lifetime Point |
|------|----------------|
| SILVER | >=0 |
| GOLD | >=400 |
| DIAMOND | >=1000 |

## Tier Rule

| Rule | Decision |
|------|----------|
| Earn tính theo Tier | Có |
| Redeem ảnh hưởng Tier | Không |
| Expire ảnh hưởng Tier | Không |
| Lifetime Point giảm | Không |
| Tier Upgrade | Real-time |
| Tier Downgrade | Scheduler |

## Tier Upgrade

```text
Earn Point
     │
Lifetime Point
     │
>= Next Tier
     │
Upgrade
```

Upgrade ngay sau khi Earn thành công.

---

## Tier Downgrade

Chạy bởi Scheduler.

Điều kiện

- Không phát sinh giao dịch mua vé trong hơn **01 tháng**.

```text
Scheduler
      │
Check Last Booking
      │
>30 ngày
      │
Downgrade
```

---

# 26. Point Wallet Engine

Wallet lưu trạng thái hiện tại.

```text
Wallet

Current Point

Hold Point

Lifetime Point

Expired Point
```

## Rule

```
Available

=

Current

-

Hold
```

Wallet luôn thỏa:

- Current >= 0
- Hold >= 0
- Available >= 0
- Lifetime >= Current

---

# 27. Earn Engine

Earn xảy ra sau khi Booking thành công.

## Eligible Amount

Earn dựa trên:

- Vé
- Đồ ăn
- Combo

Không tính:

- Gift Card
- Điểm Redeem
- Voucher giảm giá

## Formula

```
Earn Point

=

floor(

Eligible Amount

×

Earn Rate

)
```

Ví dụ

```
Booking

500.000

Tier Gold

7%

↓

35 Point
```

## Rule

| Rule | Decision |
|------|----------|
| Earn sau Payment Success | Có |
| Earn khi Booking Failed | Không |
| Earn khi Cancel | Không |
| Earn nhiều lần | Chặn bằng Idempotency |
| Max Earn | Theo Tier |

---

## Earn Flow

```text
Booking Success
      │
      ▼
Validate
      │
      ▼
Calculate
      │
      ▼
Update Wallet
      │
      ▼
Update Lifetime
      │
      ▼
Ledger
      │
      ▼
Tier Check
      │
      ▼
Publish Event
```

---

# 28. Redeem Engine

Redeem dùng Point thay tiền.

## Rule

| Rule | Decision |
|------|----------|
| Chỉ dùng Current Point | Có |
| Hold trước Payment | Có |
| Commit sau Payment | Có |
| Payment Fail | Release |
| Wallet âm | Không |
| Redeem > Wallet | Reject |

---

## Redeem Flow

```text
Customer
     │
Redeem Request
     │
Enough Point?
     │
YES
     │
Hold Point
     │
Payment
```

---

# 29. Hold Engine

Hold chỉ khóa điểm.

Chưa trừ.

```text
Current

100

↓

Hold

20

↓

Available

80
```

Rule

| Rule | Decision |
|------|----------|
| Hold nhiều Booking | Có |
| Hold Timeout | Có |
| Hold Expired | Release |
| Commit | Sau Payment |
| Release | Payment Fail |

---

# 30. Commit Engine

Commit xảy ra khi thanh toán thành công.

```text
Hold

↓

Commit

↓

Ledger

↓

Wallet
```

Rule

- Không Commit nếu Hold không tồn tại.
- Chỉ Commit một lần.
- Idempotent.

---

# 31. Release Engine

Release trả lại Point.

Trigger

- Payment Failed
- Booking Timeout
- Booking Cancel trước Payment

```text
Hold

↓

Release

↓

Wallet Restore
```

Không tạo Earn.

Không thay Tier.

---

# 32. Point Expiration Engine

Điểm hết hạn theo năm.

Không expire trực tiếp Wallet.

Expire theo Bucket.

```text
Bucket A

120

31/12/2026

Bucket B

80

31/12/2027
```

Scheduler đọc Bucket.

Nếu hết hạn

↓

Ledger(EXPIRE)

↓

Wallet

---

## Rule

| Rule | Decision |
|------|----------|
| FIFO | Có |
| Expire Current | Có |
| Expire Lifetime | Không |
| Expire Hold | Không |
| Rollback Expire | Không |
| Notification | Có |

---

# 33. Adjustment Engine

Admin được cộng/trừ điểm.

Không sửa Ledger.

Luôn tạo Transaction mới.

Transaction

```
MANUAL_ADD

MANUAL_DEDUCT
```

---

Adjustment Type

| Type | Description |
|------|-------------|
| Compensation | Đền bù |
| Customer Care | CSKH |
| Campaign | Chiến dịch |
| Migration | Import |
| Correction | Sửa dữ liệu |

---

Rule

| Rule | Decision |
|------|----------|
| Có Reason | Bắt buộc |
| Có Audit | Bắt buộc |
| Có Approver | Khuyến nghị |
| CSV Import | Hỗ trợ |

---

# 34. Revoke Engine

Dùng khi Booking Refund.

Earn trước đó phải bị thu hồi.

```
Earn

50

↓

Refund

↓

Revoke

50
```

Nếu Wallet hiện tại chỉ còn

20

↓

Actual Deduct

20

Không cho Wallet âm.

Lifetime Point vẫn giảm theo chính sách hiện tại của hệ thống.

---

## Formula

```
Actual Deduct

=

min(

Wallet Current,

Requested Point
)
```

---

# 35. Reconciliation Engine

Đối soát chạy định kỳ.

Mục tiêu:

- Phát hiện mất Ledger.
- Sai Wallet.
- Sai Kafka Event.
- Sai Booking.

Kiểm tra

```text
Wallet

=

Σ Ledger
```

Nếu lệch

↓

Flag

↓

Admin Review

---

Rule

| Rule | Decision |
|------|----------|
| Auto Repair | Không |
| Manual Adjustment | Có |
| Audit | Có |
| Export CSV | Có |
| Retry | Có |
# 36. Public API Design

## Customer APIs

| Method | Endpoint | Description |
|---------|----------|-------------|
| GET | /api/scores/me | Thông tin Membership |
| GET | /api/scores/me/history | Lịch sử điểm |
| GET | /api/scores/me/tier | Tier hiện tại |
| POST | /api/scores/me/redeem-preview | Kiểm tra khả năng Redeem |

---

## Admin APIs

| Method | Endpoint | Description |
|---------|----------|-------------|
| GET | /api/admin/memberships | Danh sách Membership |
| GET | /api/admin/memberships/{id} | Chi tiết |
| PUT | /api/admin/memberships/{id}/tier | Đổi Tier |
| POST | /api/admin/adjustments | Cộng/Trừ điểm |
| POST | /api/admin/adjustments/import | Import CSV |
| GET | /api/admin/reconciliation | Đối soát |
| GET | /api/admin/tiers | Danh sách Tier |
| POST | /api/admin/tiers | Tạo Tier |
| PUT | /api/admin/tiers/{id} | Cập nhật Tier |

---

## Internal APIs

| Method | Endpoint | Used By |
|---------|----------|----------|
| GET | /internal/users/{id}/wallet | Booking |
| POST | /internal/redeem/hold | Booking |
| POST | /internal/redeem/commit | Booking |
| POST | /internal/redeem/release | Booking |
| POST | /internal/earn | Booking |
| POST | /internal/revoke | Booking |
| GET | /internal/tiers/{userId} | Promotion |

---

# 37. Validation Rules

## Membership

- User phải tồn tại.
- Membership chỉ được tạo một lần.
- User bị khóa → Reject.

---

## Earn

- Booking phải SUCCESS.
- Booking chưa Earn trước đó.
- Amount > 0.
- Event hợp lệ.
- Idempotency Key hợp lệ.

---

## Redeem

- Membership ACTIVE.
- Điểm đủ.
- Booking hợp lệ.
- Không Redeem nhiều lần.
- Hold chưa tồn tại.

---

## Adjustment

- Point > 0.
- Reason bắt buộc.
- Admin bắt buộc.
- Membership tồn tại.

---

# 38. Error Code

| Code | Description |
|------|-------------|
| SCORE_001 | Membership Not Found |
| SCORE_002 | Tier Not Found |
| SCORE_003 | Wallet Not Found |
| SCORE_004 | Insufficient Point |
| SCORE_005 | Duplicate Event |
| SCORE_006 | Invalid Hold |
| SCORE_007 | Hold Expired |
| SCORE_008 | Invalid Tier |
| SCORE_009 | Membership Locked |
| SCORE_010 | Invalid Adjustment |
| SCORE_011 | Invalid Booking |
| SCORE_012 | Point Already Expired |

---

# 39. Idempotency Strategy

Các API thay đổi dữ liệu bắt buộc có:

```
Idempotency-Key
```

Áp dụng cho:

- Earn
- Hold
- Commit
- Release
- Revoke
- Adjustment

Nếu Key đã xử lý:

```
Return Previous Result
```

Không tạo Ledger mới.

---

# 40. Concurrency Strategy

Một Membership chỉ được cập nhật bởi một transaction tại cùng thời điểm.

Sử dụng:

- Optimistic Lock (@Version)
- Redis Distributed Lock (nếu scale nhiều instance)

```text
Lock Membership
        │
Update Wallet
        │
Save Ledger
        │
Unlock
```

Tránh:

- Double Earn
- Double Redeem
- Lost Update
- Race Condition

---

# 41. Transaction Strategy

Mỗi nghiệp vụ là một Transaction.

Ví dụ Earn:

```text
Update Wallet
      │
Update Lifetime
      │
Save Ledger
      │
Save History
      │
Publish Event
```

Nếu lỗi trước khi Commit:

```
Rollback
```

Không để Wallet và Ledger lệch nhau.

---

# 42. Outbox Pattern

Để tránh mất Kafka Event.

```text
Business Transaction
       │
Save Ledger
       │
Save Outbox
       │
Commit
       │
Background Publisher
       │
Kafka
```

Không publish Kafka trực tiếp trong Transaction.

---

# 43. Audit

Audit mọi thay đổi.

Lưu:

- User
- Admin
- Time
- IP
- API
- Before
- After
- Reason

Ledger không được sửa.

Nếu cần điều chỉnh:

```
Create New Ledger
```

---

# 44. Logging

Log các sự kiện:

- Earn
- Redeem
- Hold
- Commit
- Release
- Expire
- Adjustment
- Tier Change
- Scheduler

Không log:

- JWT
- Password
- OTP
- Token

---

# 45. Monitoring

## Metrics

| Metric | Description |
|---------|-------------|
| earn_total | Tổng Earn |
| redeem_total | Tổng Redeem |
| hold_total | Hold |
| expire_total | Expired |
| adjustment_total | Adjustment |
| reconciliation_failed | Lỗi đối soát |
| tier_upgrade_total | Upgrade |
| tier_downgrade_total | Downgrade |

---

## Health Check

- Database
- Redis
- Kafka
- Scheduler

Actuator:

```
/health

/metrics

/prometheus
```

---

# 46. Security

## Authentication

- Customer API → JWT
- Admin API → JWT + Role
- Internal API → Internal Token hoặc mTLS

---

## Authorization

| Role | Permission |
|------|------------|
| CUSTOMER | Xem điểm |
| ADMIN | Quản lý điểm |
| SUPER_ADMIN | Toàn quyền |
| INTERNAL_SERVICE | Internal API |

---

## Sensitive APIs

- Manual Adjustment
- Import CSV
- Tier Configuration
- Reconciliation

Chỉ ADMIN.

---

# 47. Performance

Mục tiêu

| Item | Target |
|------|--------|
| Read Wallet | <100ms |
| Earn | <300ms |
| Redeem | <300ms |
| Hold | <200ms |
| History | <500ms |

---

Tối ưu

- Redis Cache
- Index Database
- Async Kafka
- Batch Scheduler
- Pagination

---

# 48. Testing Strategy

## Unit Test

- Earn Calculator
- Tier Calculator
- Wallet Service
- Expiration Service

---

## Integration Test

- Database
- Kafka
- Redis
- Scheduler

---

## API Test

- Customer API
- Admin API
- Internal API

---

## Concurrency Test

Kiểm tra:

- Double Redeem
- Double Earn
- Parallel Hold
- Lost Update

---

# 49. Roadmap

## Phase 1

- Membership
- Wallet
- Tier
- Earn
- History

---

## Phase 2

- Redeem
- Hold
- Commit
- Release

---

## Phase 3

- Expiration
- Scheduler
- Reconciliation
- Admin Adjustment

---

## Phase 4

- Dashboard
- Analytics
- Bulk Import
- Campaign Integration

---

# 50. Definition of Done (DoD)

Một chức năng chỉ được coi là hoàn thành khi:

- Business Rule đầy đủ.
- Validation đầy đủ.
- API hoàn chỉnh.
- Database Migration hoàn chỉnh.
- Unit Test đạt yêu cầu.
- Integration Test đạt yêu cầu.
- Không vi phạm Domain Boundary.
- Có Audit Log.
- Có Metrics.
- Có Error Code.
- Có Swagger.
- Có Idempotency.
- Có Transaction.
- Có Security.
- Có Logging.
- Được Review và Merge.

---

# 51. Future Enhancements

- Multi-level Membership.
- Point Transfer giữa người dùng.
- Family Membership.
- Birthday Bonus Point.
- AI Fraud Detection.
- Dynamic Earn Campaign.
- Tier Benefit Engine.
- Cross-brand Loyalty.
- Mobile Push Reminder trước khi điểm hết hạn.
- Real-time Loyalty Dashboard.