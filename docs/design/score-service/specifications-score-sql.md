# SCORE SERVICE DATABASE SPECIFICATION

Version: 1.0

Database: MySQL 8+

Service: score-service

---

# 1. Overview

Score Service là hệ thống Loyalty trung tâm của toàn bộ hệ thống.

Service chịu trách nhiệm quản lý:

- Membership Tier
- Loyalty Point
- Available Balance
- Held Balance
- Point Earn
- Point Redeem
- Hold
- Commit
- Release
- Refund
- Revoke
- Point Expiration
- Reconciliation
- Manual Adjustment
- Immutable Ledger
- Audit

Score Service là Single Source of Truth cho toàn bộ dữ liệu Loyalty.

Không service nào khác được phép thay đổi Point trực tiếp.

---

# 2. Database Design Principle

## 2.1 Projection + Ledger

Database được thiết kế theo mô hình

Projection + Immutable Ledger

Trong đó

Projection

là

user_scores

được tối ưu cho Query.

Ledger

là

score_history

được tối ưu cho Audit và Reconciliation.

Balance luôn có thể tính lại hoàn toàn từ Ledger.

---

## 2.2 Append Only

score_history

không được phép

UPDATE

không được phép

DELETE

Mọi thay đổi đều tạo Transaction mới.

---

## 2.3 Idempotency

Tất cả Transaction thay đổi Point đều phải có

Idempotency Key

để chống cộng hoặc trừ điểm nhiều lần.

---

## 2.4 Event Driven

Sau khi DB Commit thành công

Outbox Event

được tạo.

Worker sẽ Publish Event lên Kafka.

Không Publish Event trước DB Commit.

---

# 3. Database Tables

| Table | Purpose |
|--------|----------|
| membership_tiers | Membership configuration |
| user_scores | Current Balance Projection |
| score_holds | Hold Point |
| point_expiration_buckets | FIFO Expiration |
| score_history | Immutable Ledger |
| reconciliation_runs | Reconciliation Batch |
| reconciliation_details | Reconciliation Result |
| outbox_events | Reliable Event Publish |
| audit_logs | Operation Audit |

---

# 4. Relationship

membership_tiers

↓

user_scores

↓

score_holds

↓

score_history

↓

audit_logs

↓

outbox_events

↓

reconciliation

---

# 5. Naming Convention

Primary Key

id

Foreign Key

xxx_id

Datetime

created_at

updated_at

Business UUID

transaction_uuid

Business Event

event_id

Correlation

correlation_id

Idempotency

idempotency_key

---

# 6. Character Set

utf8mb4

Collation

utf8mb4_unicode_ci

---

# 7. Timezone

Database

UTC

Application

Asia/Ho_Chi_Minh

API

ISO-8601

---

# 8. Storage Engine

InnoDB

---

# 9. Transaction Isolation

READ COMMITTED

Score Update

SELECT ... FOR UPDATE

Optimistic Lock

version column

---

# 10. Database Rules

Balance

Projection

Ledger

Source Of Truth

History

Read Only

Hold

TTL

Expire

FIFO

Reconciliation

Scheduled Job

Audit

Append Only

Outbox

Reliable Publish
# 11. Table Specification — membership_tiers

---

## 11.1 Purpose

Lưu cấu hình các hạng thành viên (Membership Tier).

Bảng này chỉ chứa dữ liệu cấu hình, không chứa dữ liệu giao dịch.

Một User tại một thời điểm chỉ thuộc duy nhất một Tier.

Tier được xác định dựa trên `accumulated_points` của User.

---

## 11.2 Relationships

```
membership_tiers (1)
        │
        │
        ▼
user_scores (N)
```

---

## 11.3 Columns

| Column | Type | Nullable | Description |
|---------|------|----------|-------------|
| id | INT | No | Primary Key |
| tier_name | VARCHAR(50) | No | Tên hạng thành viên (SILVER, GOLD, DIAMOND...) |
| min_points | INT | No | Số điểm tích lũy tối thiểu để đạt hạng |
| earning_rate | DECIMAL(5,2) | No | Tỷ lệ tích điểm áp dụng cho hạng |
| description | TEXT | Yes | Mô tả |
| created_at | TIMESTAMP | No | Thời gian tạo |
| updated_at | TIMESTAMP | No | Thời gian cập nhật |

---

## 11.4 Constraints

Primary Key

```
PRIMARY KEY(id)
```

Unique

```
tier_name
```

---

## 11.5 Business Rules

### BR-TIER-001

Tier Name phải duy nhất.

---

### BR-TIER-002

Không được phép có hai Tier cùng tên.

---

### BR-TIER-003

min_points phải >= 0.

---

### BR-TIER-004

earning_rate phải > 0.

---

### BR-TIER-005

Không được xóa Tier đang được User sử dụng.

---

### BR-TIER-006

Tier thấp nhất phải tồn tại.

Ví dụ

```
SILVER
```

---

### BR-TIER-007

Tier Upgrade được xác định bằng accumulated_points.

---

### BR-TIER-008

Tier chỉ được thay đổi thông qua Score Service.

---

### BR-TIER-009

Marketing có thể thay đổi earning_rate.

Không cần Deploy hệ thống.

---

### BR-TIER-010

Không được hard-code Tier trong source code.

---

## 11.6 Typical Data

| id | tier_name | min_points | earning_rate |
|----|-----------|-----------:|-------------:|
| 1 | SILVER | 0 | 0.05 |
| 2 | GOLD | 400 | 0.07 |
| 3 | DIAMOND | 1000 | 0.10 |

---

## 11.7 Query Example

Lấy Tier hiện tại

```sql
SELECT *
FROM membership_tiers
WHERE id = ?;
```

Lấy Tier theo điểm

```sql
SELECT *
FROM membership_tiers
WHERE min_points <= ?
ORDER BY min_points DESC
LIMIT 1;
```

---

# 12. Table Specification — user_scores

---

## 12.1 Purpose

Đây là bảng Projection.

Lưu trạng thái điểm hiện tại của User để đọc nhanh.

Không dùng để Audit.

Không dùng để Reconciliation.

Không dùng để tính lại lịch sử.

Nguồn dữ liệu gốc luôn là Ledger (`score_history`).

---

## 12.2 Relationships

```
membership_tiers

↓

user_scores

↓

score_holds

↓

score_history
```

---

## 12.3 Columns

| Column | Type | Nullable | Description |
|---------|------|----------|-------------|
| user_id | BIGINT | No | User ID |
| current_points | INT | No | Điểm hiện có |
| held_points | INT | No | Điểm đang Hold |
| accumulated_points | INT | No | Lifetime Point |
| current_tier_id | INT | No | Tier hiện tại |
| membership_status | ENUM | No | ACTIVE / LOCKED / INACTIVE / MERGED |
| outstanding_points | INT | No | Điểm còn thiếu cần thu hồi |
| version | BIGINT | No | Optimistic Lock |
| created_at | TIMESTAMP | No | Thời gian tạo |
| updated_at | TIMESTAMP | No | Thời gian cập nhật |

---

## 12.4 Constraints

Primary Key

```
user_id
```

Foreign Key

```
current_tier_id

→ membership_tiers.id
```

---

## 12.5 Business Rules

### BR-SCORE-001

Current Point không được âm.

---

### BR-SCORE-002

Held Point không được âm.

---

### BR-SCORE-003

Held Point ≤ Current Point.

---

### BR-SCORE-004

Available Point

```
Current Point

-

Held Point
```

---

### BR-SCORE-005

Accumulated Point chỉ tăng.

Ngoại lệ

Revoke.

---

### BR-SCORE-006

Balance chỉ được thay đổi thông qua Transaction.

---

### BR-SCORE-007

Không Update Balance bằng SQL thủ công.

---

### BR-SCORE-008

Concurrent Update phải dùng

```
SELECT FOR UPDATE

hoặc

Optimistic Lock
```

---

### BR-SCORE-009

Tier được tính từ accumulated_points.

---

### BR-SCORE-010

User mới tạo

```
Current = 0

Held = 0

Accumulated = 0

Tier = SILVER
```

---

## 12.6 Example

| User | Current | Held | Available | Lifetime |
|------|--------:|-----:|----------:|---------:|
| 1001 | 560 | 100 | 460 | 1300 |

---

## 12.7 Query Example

Lấy Balance

```sql
SELECT *
FROM user_scores
WHERE user_id = ?;
```

Available Point

```sql
SELECT
current_points-held_points
FROM user_scores
WHERE user_id=?;
```

Lock Balance

```sql
SELECT *
FROM user_scores
WHERE user_id=?
FOR UPDATE;
```
# 13. Table Specification — score_holds

---

## 13.1 Purpose

Lưu các giao dịch **Hold Point** trong quá trình đặt vé.

Hold Point chỉ là thao tác **tạm giữ điểm**, chưa trừ khỏi số dư hiện tại.

Điểm chỉ thực sự bị trừ khi thực hiện **Commit**.

Nếu Booking bị hủy hoặc hết thời gian giữ ghế, Hold sẽ được **Release**.

---

## 13.2 Relationships

```
user_scores (1)

        │

        │

        ▼

score_holds (N)

        │

        ▼

score_history
```

---

## 13.3 Columns

| Column | Type | Nullable | Description |
|---------|------|----------|-------------|
| id | BIGINT | No | Primary Key |
| user_id | BIGINT | No | User sở hữu Hold |
| booking_id | BIGINT | No | Booking đang giữ điểm |
| hold_points | INT | No | Số điểm được giữ |
| status | ENUM | No | ACTIVE / COMMITTED / RELEASED / EXPIRED |
| expires_at | TIMESTAMP | No | Thời gian hết hạn Hold |
| committed_at | TIMESTAMP | Yes | Thời gian Commit |
| released_at | TIMESTAMP | Yes | Thời gian Release |
| idempotency_key | VARCHAR(100) | No | Idempotency |
| created_at | TIMESTAMP | No | Created Time |

---

## 13.4 Constraints

Primary Key

```
id
```

Unique

```
booking_id
```

Foreign Key

```
user_id

↓

user_scores.user_id
```

---

## 13.5 Business Rules

### BR-HOLD-001

Một Booking chỉ có tối đa một Hold đang ACTIVE.

---

### BR-HOLD-002

Hold Point không làm thay đổi Current Point.

---

### BR-HOLD-003

Hold chỉ tăng Held Point.

---

### BR-HOLD-004

Hold Point không được lớn hơn Available Point.

---

### BR-HOLD-005

Hold phải có TTL.

---

### BR-HOLD-006

TTL phải nhỏ hơn hoặc bằng thời gian giữ ghế của Booking.

---

### BR-HOLD-007

Hết TTL phải tự động Release.

---

### BR-HOLD-008

Hold Request phải Idempotent.

---

### BR-HOLD-009

Hold không làm thay đổi Tier.

---

### BR-HOLD-010

Hold không làm thay đổi Lifetime Point.

---

## 13.6 Lifecycle

```
ACTIVE

↓

COMMITTED

hoặc

↓

RELEASED

hoặc

↓

EXPIRED
```

---

## 13.7 Query Example

Hold còn hiệu lực

```sql
SELECT *
FROM score_holds
WHERE booking_id=?
AND status='ACTIVE';
```

Các Hold đã hết hạn

```sql
SELECT *
FROM score_holds
WHERE expires_at < NOW()
AND status='ACTIVE';
```

---

# 14. Table Specification — point_expiration_buckets

---

## 14.1 Purpose

Quản lý từng "lô điểm" (Point Bucket) để hỗ trợ Expiration theo FIFO.

Mỗi lần Earn sẽ tạo một Bucket mới.

Redeem sẽ tiêu điểm từ Bucket cũ nhất.

Expire cũng sẽ thực hiện trên Bucket cũ nhất.

Bảng này giúp triển khai chính xác nghiệp vụ:

- FIFO
- Expiration
- Outstanding Calculation
- Reconciliation

---

## 14.2 Relationships

```
score_history (Earn)

↓

point_expiration_buckets

↓

Expire Job
```

---

## 14.3 Columns

| Column | Type | Nullable | Description |
|---------|------|----------|-------------|
| id | BIGINT | No | Primary Key |
| user_id | BIGINT | No | Owner |
| earn_history_id | BIGINT | No | Earn Transaction |
| original_points | INT | No | Điểm Earn ban đầu |
| remaining_points | INT | No | Điểm còn lại |
| expiration_date | DATE | No | Ngày hết hạn |
| status | ENUM | No | ACTIVE / EXPIRED / FULLY_USED |
| created_at | TIMESTAMP | No | Created Time |

---

## 14.4 Constraints

Primary Key

```
id
```

Foreign Key

```
earn_history_id

↓

score_history.id
```

---

## 14.5 Business Rules

### BR-EXP-001

Mỗi Earn Transaction tạo đúng một Bucket.

---

### BR-EXP-002

Remaining Point luôn nhỏ hơn hoặc bằng Original Point.

---

### BR-EXP-003

Redeem luôn lấy Bucket cũ nhất.

(FIFO)

---

### BR-EXP-004

Expire chỉ áp dụng với Remaining Point.

---

### BR-EXP-005

Bucket đã hết điểm chuyển trạng thái

```
FULLY_USED
```

---

### BR-EXP-006

Bucket hết hạn chuyển

```
EXPIRED
```

---

### BR-EXP-007

Bucket không được Update trực tiếp ngoài Score Service.

---

### BR-EXP-008

Expiration Job phải Idempotent.

---

### BR-EXP-009

Expire không được làm Current Point âm.

---

### BR-EXP-010

Bucket là dữ liệu hỗ trợ Expiration.

Không thay thế Ledger.

---

## 14.6 Example

| Earn | Original | Remaining | Expire |
|------|---------:|----------:|--------|
| 5001 | 100 | 40 | 2027-12-31 |
| 5010 | 200 | 200 | 2028-01-10 |

---

## 14.7 Query Example

Bucket sắp hết hạn

```sql
SELECT *
FROM point_expiration_buckets
WHERE expiration_date<=CURDATE()+30
AND status='ACTIVE';
```

Bucket FIFO

```sql
SELECT *
FROM point_expiration_buckets
WHERE user_id=?
AND remaining_points>0
ORDER BY expiration_date;
```
# 15. Table Specification — score_history

---

## 15.1 Purpose

Đây là bảng quan trọng nhất của Score Service.

`score_history` đóng vai trò **Immutable Ledger**, lưu toàn bộ giao dịch thay đổi điểm trong suốt vòng đời của Loyalty Account.

Đây là **Source of Truth** để:

- Audit
- Reconciliation
- Point History
- Fraud Investigation
- Financial Reporting
- Event Replay

Balance trong `user_scores` chỉ là dữ liệu tổng hợp (Projection).

---

## 15.2 Relationships

```
user_scores (1)

        │

        ▼

score_history (N)

        │

        ├──────────────► point_expiration_buckets

        ├──────────────► outbox_events

        └──────────────► audit_logs
```

---

## 15.3 Supported Transaction Types

| Transaction | Description |
|------------|-------------|
| EARN_BY_BOOKING | Cộng điểm từ Booking |
| HOLD | Giữ điểm tạm thời |
| REDEEM_FOR_BOOKING | Commit Redeem |
| RELEASE | Release Hold |
| REFUND_REDEEM | Trả lại điểm Redeem |
| REVOKE_EARN_BY_REFUND | Thu hồi điểm Earn |
| MANUAL_ADD | Admin cộng điểm |
| MANUAL_DEDUCT | Admin trừ điểm |
| EXPIRED | Điểm hết hạn |
| RECONCILIATION | Điều chỉnh đối soát |
| REVERSE_ADJUSTMENT | Hoàn tác Adjustment |

---

## 15.4 Columns

| Column | Description |
|---------|-------------|
| id | Ledger ID |
| transaction_uuid | UUID của Transaction |
| user_id | User |
| booking_id | Booking |
| payment_id | Payment |
| promotion_id | Promotion |
| event_id | Business Event |
| correlation_id | Correlation ID |
| idempotency_key | Idempotency |
| transaction_type | Loại giao dịch |
| requested_point_change | Điểm yêu cầu |
| point_change | Điểm thực tế |
| balance_before | Balance trước |
| balance_after | Balance sau |
| held_before | Held trước |
| held_after | Held sau |
| accumulated_before | Lifetime trước |
| accumulated_after | Lifetime sau |
| tier_snapshot | Tier tại thời điểm giao dịch |
| reference_history_id | Ledger tham chiếu |
| source_service | Booking / Promotion / Admin... |
| metadata | JSON mở rộng |
| reason | Lý do |
| created_by | Operator |
| created_at | Thời gian tạo |

---

## 15.5 Business Rules

### BR-LEDGER-001

Ledger chỉ được INSERT.

---

### BR-LEDGER-002

Không UPDATE Ledger.

---

### BR-LEDGER-003

Không DELETE Ledger.

---

### BR-LEDGER-004

Mọi giao dịch thay đổi điểm đều sinh Ledger mới.

---

### BR-LEDGER-005

Balance Before và After luôn phải được lưu.

---

### BR-LEDGER-006

Lifetime Before và After luôn phải được lưu.

---

### BR-LEDGER-007

Held Before và After luôn phải được lưu.

---

### BR-LEDGER-008

Tier phải được Snapshot.

---

### BR-LEDGER-009

Mọi Ledger đều có EventID.

---

### BR-LEDGER-010

Mọi Ledger đều có Idempotency Key.

---

### BR-LEDGER-011

Ledger phải tham chiếu giao dịch gốc khi Refund hoặc Reverse.

---

### BR-LEDGER-012

Metadata được lưu dưới dạng JSON để mở rộng.

---

### BR-LEDGER-013

Ledger là nguồn dữ liệu duy nhất phục vụ Reconciliation.

---

### BR-LEDGER-014

History hiển thị cho khách hàng được lấy từ Ledger sau khi Mapping.

---

### BR-LEDGER-015

Ledger không bao giờ bị chỉnh sửa sau khi Commit.

---

## 15.6 Reference Flow

Earn

```
Booking

↓

Ledger(EARN)
```

Redeem

```
Hold

↓

Ledger(HOLD)

↓

Ledger(REDEEM)
```

Refund

```
Ledger(REDEEM)

↓

Ledger(REFUND_REDEEM)
```

Revoke

```
Ledger(EARN)

↓

Ledger(REVOKE)
```

---

## 15.7 Query Example

Lịch sử User

```sql
SELECT *
FROM score_history
WHERE user_id=?
ORDER BY created_at DESC;
```

Lấy Ledger theo Event

```sql
SELECT *
FROM score_history
WHERE event_id=?;
```

Lấy Ledger theo Booking

```sql
SELECT *
FROM score_history
WHERE booking_id=?;
```

Lấy Ledger theo Correlation

```sql
SELECT *
FROM score_history
WHERE correlation_id=?;
```

---

# 16. Table Specification — reconciliation_runs

---

## 16.1 Purpose

Lưu thông tin mỗi lần chạy Reconciliation.

Mỗi Batch chỉ sinh một bản ghi.

---

## 16.2 Columns

| Column | Description |
|---------|-------------|
| id | Batch ID |
| batch_code | Mã Batch |
| started_at | Bắt đầu |
| finished_at | Kết thúc |
| total_users | Tổng User kiểm tra |
| mismatch_count | Tổng User sai lệch |
| status | RUNNING / SUCCESS / FAILED |
| created_at | Created Time |

---

## 16.3 Business Rules

- Chỉ Scheduler được tạo Batch.
- Không chạy hai Batch đồng thời.
- Batch phải Idempotent.
- Batch không sửa Balance trực tiếp.
- Chỉ tạo Report.

---

## 16.4 Query Example

```sql
SELECT *
FROM reconciliation_runs
ORDER BY id DESC;
```

---

# 17. Table Specification — reconciliation_details

---

## 17.1 Purpose

Lưu chi tiết sai lệch phát hiện trong từng Batch.

Một Batch có nhiều Detail.

---

## 17.2 Relationships

```
reconciliation_runs

↓

reconciliation_details
```

---

## 17.3 Columns

| Column | Description |
|---------|-------------|
| id | Detail ID |
| reconciliation_run_id | Batch |
| user_id | User |
| projected_balance | Balance hiện tại |
| calculated_balance | Balance từ Ledger |
| difference | Chênh lệch |
| status | OPEN / RESOLVED |
| resolved_by | Admin |
| resolved_at | Resolve Time |

---

## 17.4 Business Rules

- Không tự sửa Balance.
- Chỉ ghi nhận sai lệch.
- Sau khi xác minh, điều chỉnh thông qua Adjustment Transaction.
- Detail không được xóa.

---

## 17.5 Query Example

```sql
SELECT *
FROM reconciliation_details
WHERE status='OPEN';
```
# 18. Table Specification — outbox_events

---

## 18.1 Purpose

`outbox_events` triển khai **Transactional Outbox Pattern** nhằm đảm bảo tính nhất quán giữa Database và Message Broker.

Mọi Event chỉ được Publish sau khi Transaction Database đã Commit thành công.

Nếu Kafka hoặc RabbitMQ gặp sự cố, Event sẽ được giữ lại và Worker sẽ Retry cho đến khi Publish thành công.

---

## 18.2 Relationships

```
score_history

        │

        ▼

outbox_events

        │

        ▼

Kafka / RabbitMQ

        │

        ▼

Analytics
Notification
Booking
Promotion
```

---

## 18.3 Columns

| Column | Type | Description |
|---------|------|-------------|
| id | BIGINT | Primary Key |
| aggregate_type | VARCHAR(50) | SCORE |
| aggregate_id | BIGINT | User ID |
| event_type | VARCHAR(100) | POINT_EARNED, POINT_REDEEMED... |
| event_id | VARCHAR(150) | Business Event ID |
| correlation_id | VARCHAR(150) | Correlation ID |
| payload | JSON | Event Payload |
| status | ENUM | PENDING / PUBLISHED / FAILED |
| retry_count | INT | Số lần Retry |
| published_at | TIMESTAMP | Thời điểm Publish |
| created_at | TIMESTAMP | Thời điểm tạo |

---

## 18.4 Business Rules

### BR-OUTBOX-001

Outbox Record phải được tạo trong cùng Database Transaction với Ledger.

---

### BR-OUTBOX-002

Không Publish Event trước khi DB Commit.

---

### BR-OUTBOX-003

Worker chỉ đọc Event có trạng thái

```
PENDING
```

---

### BR-OUTBOX-004

Publish thành công

↓

```
PUBLISHED
```

---

### BR-OUTBOX-005

Publish thất bại

↓

Retry.

---

### BR-OUTBOX-006

Retry theo Exponential Backoff.

---

### BR-OUTBOX-007

Retry vượt ngưỡng

↓

```
FAILED
```

↓

DLQ.

---

### BR-OUTBOX-008

Consumer cũng phải kiểm tra Idempotency.

---

### BR-OUTBOX-009

Không Publish cùng một Event hai lần.

---

### BR-OUTBOX-010

Payload phải là Snapshot tại thời điểm Transaction.

---

## 18.5 Query Example

Pending Event

```sql
SELECT *
FROM outbox_events
WHERE status='PENDING'
ORDER BY id
LIMIT 100;
```

Retry Failed

```sql
SELECT *
FROM outbox_events
WHERE status='FAILED';
```

---

# 19. Table Specification — audit_logs

---

## 19.1 Purpose

Lưu Audit Log phục vụ:

- Security
- Compliance
- Fraud Investigation
- Internal Audit
- Customer Support

Khác với Ledger, Audit tập trung ghi nhận **hành động của người hoặc hệ thống** thay vì thay đổi số dư điểm.

---

## 19.2 Relationships

```
Admin

↓

Audit

↓

Score History
```

---

## 19.3 Columns

| Column | Type | Description |
|---------|------|-------------|
| id | BIGINT | Primary Key |
| operator_id | BIGINT | Người thực hiện |
| user_id | BIGINT | User bị ảnh hưởng |
| action | VARCHAR(100) | ADJUST_POINT, EXPORT_HISTORY... |
| target_type | VARCHAR(50) | USER / HISTORY / HOLD |
| target_id | BIGINT | ID đối tượng |
| request_ip | VARCHAR(100) | IP Address |
| user_agent | TEXT | Browser / Device |
| correlation_id | VARCHAR(150) | Correlation ID |
| description | TEXT | Nội dung |
| created_at | TIMESTAMP | Created Time |

---

## 19.4 Business Rules

### BR-AUDIT-001

Mọi thao tác Admin phải ghi Audit.

---

### BR-AUDIT-002

Export dữ liệu phải ghi Audit.

---

### BR-AUDIT-003

Manual Adjustment phải ghi Audit.

---

### BR-AUDIT-004

Audit không được Update.

---

### BR-AUDIT-005

Audit không được Delete.

---

### BR-AUDIT-006

Audit chỉ đọc.

---

### BR-AUDIT-007

Audit chỉ Admin được xem.

---

### BR-AUDIT-008

Audit phải lưu Correlation ID.

---

### BR-AUDIT-009

Audit phải lưu Operator.

---

### BR-AUDIT-010

Audit phải hỗ trợ truy vết theo Booking hoặc Event.

---

## 19.5 Query Example

Lịch sử thao tác Admin

```sql
SELECT *
FROM audit_logs
WHERE operator_id=?
ORDER BY created_at DESC;
```

Audit theo User

```sql
SELECT *
FROM audit_logs
WHERE user_id=?
ORDER BY created_at DESC;
```

---

# 20. Database Index Strategy

## Primary Index

- membership_tiers(id)
- user_scores(user_id)
- score_holds(id)
- point_expiration_buckets(id)
- score_history(id)
- reconciliation_runs(id)
- reconciliation_details(id)
- outbox_events(id)
- audit_logs(id)

---

## Frequently Used Indexes

### user_scores

```
(current_tier_id)
(membership_status)
```

---

### score_holds

```
(user_id,status)
(booking_id)
(expires_at)
```

---

### point_expiration_buckets

```
(user_id,expiration_date)
(status,expiration_date)
```

---

### score_history

```
(user_id,created_at)
(booking_id)
(event_id)
(transaction_uuid)
(transaction_type,created_at)
(correlation_id)
(idempotency_key)
(created_at)
```

---

### outbox_events

```
(status,id)
(event_id)
```

---

### reconciliation_details

```
(status)
(user_id)
```

---

### audit_logs

```
(operator_id)
(user_id)
(created_at)
```

---

# 21. Partition Strategy

Các bảng có tốc độ tăng dữ liệu lớn nên Partition theo thời gian.

| Table | Strategy |
|--------|----------|
| score_history | Monthly Partition |
| audit_logs | Monthly Partition |
| outbox_events | Monthly Partition |

Ví dụ:

```
2026_01

2026_02

2026_03
```

---

# 22. Data Retention Policy

| Table | Retention |
|--------|----------:|
| membership_tiers | Forever |
| user_scores | Forever |
| score_holds | 12 tháng sau khi hoàn tất |
| point_expiration_buckets | 5 năm |
| score_history | Không xóa (Append-only) |
| reconciliation_runs | 5 năm |
| reconciliation_details | 5 năm |
| outbox_events | 6 tháng sau khi Publish |
| audit_logs | 5–10 năm (theo chính sách doanh nghiệp) |

---

# 23. Backup & Recovery Strategy

- Full Backup hằng ngày.
- Incremental Backup mỗi giờ.
- Binary Log phục vụ Point-in-Time Recovery (PITR).
- Kiểm tra Restore định kỳ trên môi trường Staging.
- Mã hóa Backup khi lưu trữ.

---

# 24. Summary

Kiến trúc cơ sở dữ liệu của Score Service được thiết kế theo mô hình **Projection + Immutable Ledger**, đáp ứng các yêu cầu của một hệ thống Loyalty trong môi trường Microservices.

Các đặc điểm chính:

- Ledger là **Single Source of Truth**.
- Balance được lưu dưới dạng Projection để tối ưu truy vấn.
- Hỗ trợ Earn, Hold, Commit, Release, Refund, Revoke, Expiration và Adjustment.
- Đảm bảo **Idempotency**, **Auditability** và **Reconciliation**.
- Tích hợp **Transactional Outbox Pattern** để phát hành Event an toàn.
- Tối ưu cho khả năng mở rộng với Index, Partition và chiến lược Backup phù hợp.

Cấu trúc này đáp ứng đầy đủ các nghiệp vụ Loyalty Point, Membership Tier và Point Ledger của hệ thống đặt vé rạp chiếu phim theo hướng Production.
# 25. Data Integrity Rules

Để đảm bảo tính nhất quán dữ liệu, Score Service phải tuân thủ các nguyên tắc sau.

## 25.1 Projection vs Ledger

- `user_scores` chỉ là Projection.
- `score_history` là nguồn dữ liệu gốc (Source of Truth).
- Projection luôn có thể được xây dựng lại từ Ledger.

---

## 25.2 Atomic Transaction

Các thao tác sau phải nằm trong **một Database Transaction**:

- Cập nhật `user_scores`
- Tạo `score_history`
- Tạo `point_expiration_buckets` (nếu Earn)
- Tạo `outbox_events`

Nếu một bước thất bại thì toàn bộ Transaction phải Rollback.

---

## 25.3 Balance Formula

Current Point được tính theo:

```
Earn
+ Manual Add
+ Refund Redeem

-

Redeem
- Expire
- Revoke
- Manual Deduct
```

Held Point không làm thay đổi Current Point.

Available Point:

```
Available

=

Current

-

Held
```

---

## 25.4 Ledger Consistency

Mọi thay đổi Balance đều phải có Ledger.

Không tồn tại trường hợp:

- Balance thay đổi nhưng không có Ledger.
- Ledger tồn tại nhưng Balance không cập nhật.

---

## 25.5 Idempotency

Các Transaction sau phải Idempotent:

- Earn
- Hold
- Commit
- Release
- Refund
- Revoke
- Expire
- Adjustment

---

# 26. Concurrency Strategy

## Earn

```
SELECT ... FOR UPDATE

↓

Update Projection

↓

Insert Ledger

↓

Commit
```

---

## Redeem

```
Lock User

↓

Kiểm tra Available Point

↓

Update Held Point

↓

Commit
```

---

## Commit

```
Lock User

↓

Current -= Hold

Held -= Hold

↓

Insert Ledger

↓

Commit
```

---

## Revoke

```
Lock User

↓

Actual Deduct

↓

Outstanding

↓

Insert Ledger

↓

Commit
```

---

# 27. Database Migration Strategy

Schema được quản lý bằng Flyway.

Mỗi thay đổi Database phải tạo Migration mới.

Ví dụ:

```
V1__create_membership_tiers.sql

V2__create_user_scores.sql

V3__create_score_holds.sql

V4__create_point_expiration_bucket.sql

V5__create_score_history.sql

V6__create_reconciliation.sql

V7__create_outbox.sql

V8__create_audit_logs.sql

V9__seed_membership_tiers.sql
```

Không chỉnh sửa Migration đã phát hành Production.

Mọi thay đổi đều tạo Migration Version mới.

---

# 28. Future Extension

Thiết kế hiện tại hỗ trợ mở rộng các tính năng sau mà không cần thay đổi cấu trúc chính.

## Membership

- PLATINUM
- VIP
- CORPORATE
- EMPLOYEE

---

## Point

- Referral Point
- Birthday Point
- Campaign Point
- Event Point

---

## Redeem

- Gift Card
- Merchandise
- Voucher Exchange

---

## Expiration

- Rolling 12 Months
- Calendar Year
- Campaign Expiration

---

## Fraud Detection

- Device Fingerprint
- Risk Score
- OTP Verification
- ML Detection

---

## Reporting

- Daily Report
- Monthly Report
- Finance Dashboard
- Marketing Dashboard
- Customer Statement

---

## Integration

- Kafka
- RabbitMQ
- CDC
- Debezium
- Event Replay
- Saga Pattern

---

# 29. Conclusion

Cơ sở dữ liệu của Score Service được thiết kế theo mô hình **Projection + Immutable Ledger**, đáp ứng các yêu cầu của một hệ thống Loyalty quy mô Production.

Kiến trúc đảm bảo:

- Single Source of Truth.
- Event-Driven Architecture.
- Idempotent Processing.
- Audit & Traceability.
- Reconciliation.
- FIFO Point Expiration.
- Hold / Commit / Release Flow.
- Tier Management.
- High Concurrency.
- Horizontal Scalability.

Database này là nền tảng để triển khai đầy đủ các nghiệp vụ Loyalty Point, Membership Tier và Point Ledger trong hệ thống quản lý rạp chiếu phim theo kiến trúc Microservices.