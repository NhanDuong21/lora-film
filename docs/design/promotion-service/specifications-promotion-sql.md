# Promotion Service Database Design Specification

# Tóm tắt công dụng các bảng trong Promotion Service

| STT | Bảng | Công dụng |
|-----|-------|-----------|
| 1 | **promotion_campaigns** | Quản lý thông tin tổng thể của các chương trình khuyến mãi như tên, thời gian, trạng thái, ngân sách và phạm vi áp dụng. Đây là điểm bắt đầu của mọi chương trình Promotion. |
| 2 | **promotion_rules** | Lưu các điều kiện và hành động của từng chương trình khuyến mãi. Promotion Engine sử dụng bảng này để xác định khách hàng có đủ điều kiện nhận ưu đãi hay không. |
| 3 | **coupons** | Quản lý toàn bộ Coupon được phát hành trong hệ thống, bao gồm mã Coupon, điều kiện sử dụng, giới hạn sử dụng và trạng thái hiện tại. |
| 4 | **coupon_redemptions** | Ghi nhận toàn bộ lịch sử sử dụng Coupon sau khi giao dịch thanh toán thành công, phục vụ đối soát, thống kê và kiểm toán. |
| 5 | **vouchers** | Quản lý toàn bộ Voucher thuộc sở hữu của khách hàng, bao gồm quyền lợi, giá trị ưu đãi, điều kiện áp dụng và trạng thái sử dụng. |
| 6 | **voucher_redemptions** | Ghi nhận lịch sử sử dụng Voucher của khách hàng, lưu giá trị ưu đãi thực tế và liên kết với Booking, Payment để phục vụ Audit và Analytics. |
| 7 | **promotion_reservations** | Khóa tạm thời (Soft Lock) Coupon hoặc Voucher trong quá trình thanh toán nhằm ngăn chặn nhiều giao dịch sử dụng cùng một Promotion tại cùng một thời điểm. |
| 8 | **compensation_vouchers** | Quản lý các Voucher bồi thường được phát hành khi xảy ra lỗi hệ thống hoặc theo chính sách chăm sóc khách hàng, đồng thời theo dõi quy trình phê duyệt và phát hành. |
| 9 | **promotion_configurations** | Lưu các cấu hình động của Promotion Service như thời gian Reservation, giới hạn Promotion, Retry Policy và các tham số vận hành mà không cần thay đổi mã nguồn. |
| 10 | **approval_histories** | Lưu toàn bộ lịch sử phê duyệt các nghiệp vụ quan trọng như Campaign, Rule, Compensation và Configuration nhằm đáp ứng yêu cầu quản trị doanh nghiệp. |
| 11 | **audit_logs** | Ghi nhận toàn bộ lịch sử thao tác trên hệ thống để phục vụ kiểm toán, bảo mật, truy vết sự cố và tuân thủ quy định (Compliance). |
| 12 | **outbox_events** | Triển khai Transactional Outbox Pattern, lưu các Business Event trước khi publish sang Kafka nhằm đảm bảo dữ liệu và sự kiện luôn nhất quán. |
| 13 | **promotion_idempotency_keys** | Lưu Idempotency Key của các yêu cầu quan trọng để ngăn chặn xử lý trùng lặp khi Client hoặc các Microservices gửi lại cùng một Request. |
| 14 | **promotion_integration_events** | Lưu trạng thái xử lý, retry và chống trùng lặp cho các sự kiện tích hợp đi vào Promotion Service. |
| 15 | **promotion_scheduler_job_executions** | Lưu lịch sử chạy và kết quả của các scheduler job. |
| 16 | **promotion_scheduler_locks** | Quản lý distributed lock cho scheduler khi chạy nhiều instance. |

## Phân loại theo chức năng

| Nhóm | Các bảng | Vai trò |
|------|-----------|----------|
| **Master Data** | promotion_configurations | Quản lý cấu hình nền của hệ thống. |
| **Core Business** | promotion_campaigns, promotion_rules, coupons, vouchers | Quản lý toàn bộ chương trình khuyến mãi và quyền lợi của khách hàng. |
| **Runtime Business** | promotion_reservations, coupon_redemptions, voucher_redemptions | Xử lý và ghi nhận dữ liệu phát sinh trong quá trình áp dụng khuyến mãi theo thời gian thực. |
| **Financial** | compensation_vouchers | Quản lý ngân sách bồi thường do hệ thống phát hành. |
| **Governance** | approval_histories, audit_logs | Đảm bảo kiểm soát, phê duyệt và truy vết toàn bộ hoạt động của hệ thống. |
| **Integration** | outbox_events, promotion_integration_events | Đồng bộ dữ liệu và giao tiếp với các Microservices thông qua Kafka theo Event-Driven Architecture. |
| **Infrastructure** | promotion_idempotency_keys, promotion_scheduler_job_executions, promotion_scheduler_locks | Đảm bảo tính nhất quán dữ liệu, chống xử lý trùng lặp và điều phối scheduler. |

# Promotion Service Domain Design

## 1. Mục tiêu

Promotion Service được tổ chức theo mô hình **Package by Feature (DDD Lite)** tương tự Movie Service.

Mỗi Domain đại diện cho một **Business Capability** thay vì một bảng dữ liệu. Một Domain sẽ chịu trách nhiệm toàn bộ nghiệp vụ, API, Service, Repository và Entity liên quan đến chức năng đó.

Việc tổ chức theo Domain giúp:

- Dễ mở rộng.
- Giảm phụ thuộc giữa các module.
- Dễ bảo trì.
- Phù hợp với Microservices Production.
- Đồng nhất với cấu trúc của Movie Service.

---

# 2. Danh sách Domain

| Domain | Vai trò | Bảng sử dụng |
|----------|----------|----------------|
| promotion | Quản lý chương trình khuyến mãi và Rule | promotion_campaigns, promotion_rules, approval_histories |
| benefit | Quản lý Coupon, Voucher và quyền lợi khách hàng | coupons, coupon_redemptions, vouchers, voucher_redemptions, compensation_vouchers |
| reservation | Quản lý Reservation trong quá trình Booking | promotion_reservations |
| configuration | Quản lý cấu hình động của Promotion Service | promotion_configurations |
| integration | Kafka, Event, Outbox, Redis, Feign | outbox_events, promotion_integration_events |
| common | Thành phần dùng chung của toàn bộ Service | audit_logs, promotion_idempotency_keys, promotion_scheduler_job_executions, promotion_scheduler_locks |

---

# 3. Chi tiết từng Domain

---

# 3.1 Promotion Domain

## Mục đích

Đây là Domain quan trọng nhất của Promotion Service.

Domain này chịu trách nhiệm quản lý toàn bộ vòng đời của một chương trình khuyến mãi.

Bao gồm

- Campaign
- Rule
- Approval
- Publish Campaign
- Activate / Deactivate
- Rule Engine Configuration

---

## Bảng sử dụng

| Bảng | Vai trò |
|-------|----------|
| promotion_campaigns | Lưu thông tin Campaign |
| promotion_rules | Lưu điều kiện và hành động của Promotion |
| approval_histories | Lưu lịch sử phê duyệt Campaign và Rule |

---

## Chức năng

- CRUD Campaign
- CRUD Rule
- Publish Campaign
- Activate Campaign
- Disable Campaign
- Approval Workflow
- Search Campaign
- Search Rule

---

## Quan hệ

```
Campaign

↓

Rule

↓

Benefit
```

---

# 3.2 Benefit Domain

## Mục đích

Quản lý toàn bộ quyền lợi mà khách hàng nhận được.

Đây là Domain lớn thứ hai trong Promotion Service.

---

## Bảng sử dụng

| Bảng | Vai trò |
|-------|----------|
| coupons | Quản lý Coupon |
| coupon_redemptions | Lịch sử sử dụng Coupon |
| vouchers | Quản lý Voucher |
| voucher_redemptions | Lịch sử sử dụng Voucher |
| compensation_vouchers | Voucher bồi thường |

---

## Chức năng

- Generate Coupon
- Import Coupon
- Export Coupon
- Issue Voucher
- Customer Voucher
- Redeem Coupon
- Redeem Voucher
- Compensation Voucher

---

## Quan hệ

```
Rule

↓

Coupon

Voucher

↓

Reservation
```

---

# 3.3 Reservation Domain

## Mục đích

Quản lý việc giữ chỗ Promotion trong quá trình Booking.

Đây là Runtime Domain có lượng giao dịch lớn nhất.

---

## Bảng sử dụng

| Bảng | Vai trò |
|-------|----------|
| promotion_reservations | Reservation của Promotion |

---

## Chức năng

- Reserve Promotion
- Lock Coupon
- Lock Voucher
- Release Reservation
- Expire Reservation
- Timeout Scheduler

---

## Quan hệ

```
Booking

↓

Reservation

↓

Payment
```

---

# 3.4 Configuration Domain

## Mục đích

Quản lý các cấu hình động của Promotion Service.

Không cần thay đổi Source Code khi thay đổi cấu hình.

---

## Bảng sử dụng

| Bảng | Vai trò |
|-------|----------|
| promotion_configurations | Dynamic Configuration |

---

## Chức năng

- Reservation Timeout
- Retry Policy
- Promotion Limit
- Feature Toggle
- Dynamic Configuration

---

## Quan hệ

```
Configuration

↓

Promotion Engine

↓

Reservation

↓

Scheduler
```

---

# 3.5 Integration Domain

## Mục đích

Quản lý toàn bộ quá trình giao tiếp với các hệ thống bên ngoài.

Đây là Domain Infrastructure.

---

## Bảng sử dụng

| Bảng | Vai trò |
|-------|----------|
| outbox_events | Transactional Outbox |

---

## Thành phần

- Kafka Producer
- Kafka Consumer
- Outbox Publisher
- Redis
- Feign Client
- Internal API
- Event Publisher

---

## Chức năng

- Publish Event
- Retry
- Dead Letter
- Event Versioning
- Kafka Integration

---

## Quan hệ

```
Business

↓

Outbox

↓

Kafka

↓

Other Services
```

---

# 3.6 Common Domain

## Mục đích

Chứa toàn bộ thành phần dùng chung của hệ thống.

Không chứa nghiệp vụ Promotion.

---

## Bảng sử dụng

| Bảng | Vai trò |
|-------|----------|
| audit_logs | Audit toàn hệ thống |
| promotion_idempotency_keys | Chống Duplicate Request |

---

## Thành phần

- Base Entity
- Exception
- Security
- JWT
- Validator
- Constants
- Utility
- Audit
- Idempotency
- Common DTO
- Response Wrapper

---

## Chức năng

- Audit
- Security
- Validation
- Exception Handling
- Duplicate Protection
- Logging

---

# 4. Mapping Domain và Database

| Domain | Bảng |
|----------|-------|
| promotion | promotion_campaigns |
| promotion | promotion_rules |
| promotion | approval_histories |
| benefit | coupons |
| benefit | coupon_redemptions |
| benefit | vouchers |
| benefit | voucher_redemptions |
| benefit | compensation_vouchers |
| reservation | promotion_reservations |
| configuration | promotion_configurations |
| integration | outbox_events |
| integration | promotion_integration_events |
| common | audit_logs |
| common | promotion_idempotency_keys |
| common | promotion_scheduler_job_executions |
| common | promotion_scheduler_locks |

---

# 5. Cấu trúc thư mục đề xuất

```text
promotion-service
│
├── promotion
│
├── benefit
│
├── reservation
│
├── configuration
│
├── integration
│
├── common
│
└── PromotionServiceApplication.java
```

---

# 6. Quan hệ giữa các Domain

```text
                 Promotion
                     │
                     ▼
                 Benefit
                     │
                     ▼
                Reservation
                     │
                     ▼
                 Integration
                     │
                     ▼
              Other Services

          ▲
          │
Configuration ────────────────┐
                               │
Common ────────────────────────┘
```

---

# 7. Tổng kết

Promotion Service được chia thành **06 Domain chính**, mỗi Domain đại diện cho một nhóm nghiệp vụ lớn thay vì một bảng dữ liệu riêng lẻ.

Cách tổ chức này đồng nhất với cấu trúc của Movie Service, giúp mã nguồn dễ bảo trì, dễ mở rộng và phù hợp với kiến trúc **Package by Feature (DDD Lite)** trong môi trường Microservices Production.

# Phase 1 - Tổng quan hệ thống

---

# 1. Giới thiệu

## 1.1 Mục tiêu

Promotion Service là một Microservice độc lập chịu trách nhiệm quản lý toàn bộ dữ liệu và nghiệp vụ liên quan đến các chương trình khuyến mãi trong hệ thống đặt vé xem phim.

Promotion Service là nơi duy nhất quản lý:

- Promotion Campaign
- Promotion Rule
- Coupon
- Voucher
- Reservation Promotion
- Redemption History
- Compensation Voucher
- Approval Workflow
- Audit
- Kafka Outbox
- Idempotency
- Configuration

Service này chịu trách nhiệm xác định một giao dịch có được hưởng ưu đãi hay không, lưu lại toàn bộ lịch sử áp dụng khuyến mãi và đảm bảo tính nhất quán của dữ liệu trong suốt vòng đời của một chương trình khuyến mãi.

---

## 1.2 Phạm vi của Promotion Service

Promotion Service chỉ quản lý dữ liệu thuộc quyền sở hữu của Promotion.

Bao gồm:

- Campaign
- Rule
- Coupon
- Voucher
- Reservation
- Redemption
- Audit
- Configuration

Promotion Service KHÔNG quản lý:

- User
- Movie
- Cinema
- Auditorium
- Showtime
- Booking
- Payment
- Notification

Những dữ liệu trên thuộc các Microservice khác.

Promotion Service chỉ lưu Public ID để liên kết nghiệp vụ.

Ví dụ

```
user_public_id

booking_public_id

payment_public_id

movie_public_id

cinema_public_id
```

Promotion Service không tạo Foreign Key sang Database của các Service khác.

---

# 2. Trách nhiệm của Service

Promotion Service chịu trách nhiệm các nghiệp vụ sau.

## Promotion Campaign Management

Quản lý vòng đời của các chương trình khuyến mãi.

Ví dụ

- Tạo Campaign
- Chỉnh sửa Campaign
- Kích hoạt Campaign
- Ngừng Campaign
- Đóng Campaign

---

## Promotion Rule Engine

Quản lý các điều kiện và hành động giảm giá.

Ví dụ

- Giảm theo %
- Giảm theo tiền
- Tặng Voucher
- Tặng Coupon
- Giảm theo Showtime
- Giảm theo Seat
- Giảm theo Cinema
- Giảm theo Movie
- Giảm theo Membership

---

## Coupon Management

Quản lý Coupon.

Bao gồm

- Sinh Coupon
- Khóa Coupon
- Hủy Coupon
- Kiểm tra Coupon
- Áp dụng Coupon

---

## Voucher Management

Quản lý Voucher.

Bao gồm

- Phát Voucher
- Thu hồi Voucher
- Gia hạn Voucher
- Áp dụng Voucher

---

## Reservation

Lưu trạng thái giữ khuyến mãi trong thời gian người dùng đang thanh toán.

Mục tiêu

Không cho nhiều giao dịch sử dụng cùng một Coupon hoặc Voucher cùng lúc.

---

## Redemption

Lưu lịch sử sử dụng Coupon và Voucher.

Bao gồm

- Ai sử dụng
- Khi nào
- Booking nào
- Payment nào
- Reservation nào

---

## Compensation

Quản lý Voucher bồi thường.

Ví dụ

Khách thanh toán thất bại.

Hệ thống tự phát hành Voucher xin lỗi.

---

## Approval Workflow

Quản lý luồng duyệt.

Ví dụ

Draft

↓

Pending Approval

↓

Approved

↓

Rejected

↓

Published

---

## Audit

Lưu toàn bộ lịch sử thay đổi dữ liệu.

---

## Kafka Outbox

Đảm bảo Event được phát đi đúng một lần sau khi Transaction thành công.

---

## Idempotency

Ngăn cùng một Request được xử lý nhiều lần.

---

# 3. Kiến trúc Microservice

Promotion Service áp dụng mô hình Database per Service.

Mỗi Service có Database riêng.

```
+------------------------+
| Promotion Service      |
| promotion_service DB   |
+------------------------+

+------------------------+
| Booking Service        |
| booking_service DB     |
+------------------------+

+------------------------+
| Payment Service        |
| payment_service DB     |
+------------------------+
```

Promotion Service không truy cập trực tiếp Database của Service khác.

Mọi giao tiếp đều thông qua

- REST API
- Kafka Event
- Internal API

---

# 4. Quan hệ với các Service khác

```
                     +----------------+
                     | User Service   |
                     +--------+-------+
                              |
                              |
                              |
+--------------+      +-------v--------+
| Movie Service|<---->|Promotion Service|
+--------------+      +-------+--------+
                              |
                              |
          +-------------------+----------------+
          |                   |                |
          |                   |                |
+---------v------+   +--------v-------+   +----v-------------+
|Booking Service |   |Payment Service |   |NotificationService|
+----------------+   +----------------+   +-------------------+
```

Promotion Service nhận dữ liệu từ:

- User Service
- Movie Service
- Booking Service
- Payment Service

Promotion Service phát Event cho:

- Booking
- Notification
- Analytics
- Loyalty (nếu có)

---

# 5. Danh sách bảng

Promotion Service hiện bao gồm 16 bảng.

## Core Business

- promotion_campaigns
- promotion_rules
- coupons
- coupon_redemptions
- vouchers
- voucher_redemptions
- promotion_reservations

---

## Compensation

- compensation_vouchers

---

## Infrastructure

- promotion_configurations
- approval_histories
- audit_logs
- outbox_events
- promotion_idempotency_keys
- promotion_integration_events
- promotion_scheduler_job_executions
- promotion_scheduler_locks

---

# 6. Kiến trúc dữ liệu

```
Campaign
    │
    ▼
Promotion Rule
    │
    ├──────────────┐
    │              │
    ▼              ▼
Coupon         Voucher
    │              │
    ▼              ▼
Coupon        Voucher
Reservation   Reservation
    │              │
    └──────┬───────┘
           │
           ▼
Promotion Reservation
           │
           ▼
Booking
           │
           ▼
Payment
           │
           ▼
Coupon Redemption
Voucher Redemption
           │
           ▼
Kafka Outbox
           │
           ▼
Notification
```

---

# 7. Quy ước chung

Toàn bộ bảng trong Promotion Service đều tuân theo các nguyên tắc sau.

## Public ID

Mỗi bảng đều có

```
public_id
```

Dùng để giao tiếp với các Service khác.

Không expose Internal ID.

---

## Internal ID

```
id
```

Chỉ dùng cho Database.

Không sử dụng trong API.

---

## Audit

Toàn bộ bảng đều có

```
created_at

created_by

updated_at

updated_by

deleted_at

deleted_by
```

Để theo dõi toàn bộ vòng đời dữ liệu.

---

## Soft Delete

Promotion Service không Hard Delete dữ liệu nghiệp vụ.

Dữ liệu chỉ được đánh dấu

```
deleted_at

deleted_by
```

để phục vụ:

- Audit
- Rollback
- Báo cáo
- Điều tra sự cố

---

## Version

Các bảng nghiệp vụ sử dụng trường

```
version
```

để hỗ trợ Optimistic Lock và chống cập nhật đồng thời.

---

## JSON

Các trường

```
conditions_json

actions_json

metadata_json
```

được sử dụng để mở rộng nghiệp vụ mà không cần thay đổi cấu trúc Database.

---

# 8. Cấu trúc tài liệu

Đối với mỗi bảng trong các Phase tiếp theo, tài liệu sẽ bao gồm đầy đủ các nội dung sau:

1. Tóm tắt công dụng của bảng.
2. Vai trò của bảng trong hệ thống.
3. Thời điểm dữ liệu được tạo.
4. Thời điểm dữ liệu được cập nhật.
5. Thời điểm dữ liệu được đọc.
6. Thời điểm dữ liệu được xóa mềm.
7. Luồng nghiệp vụ của bảng.
8. Những bảng sử dụng bảng này.
9. Những bảng được bảng này hỗ trợ.
10. Giải thích chi tiết từng cột.
11. Quan hệ giữa từng cột với các bảng khác.
12. Business Rule liên quan.
13. Ví dụ dữ liệu thực tế.
14. Luồng hoạt động thực tế của bảng trong toàn bộ Promotion Service.

---
**Kết thúc Phase 1**

# Promotion Service Database Design Specification

# Phase 2 - Bảng promotion_campaigns

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | promotion_campaigns |
| Vai trò | Core Business |
| Độ ưu tiên | Rất cao |
| Được tạo đầu tiên | Có |
| Bảng trung tâm | Có |

---

# 2. Tóm tắt công dụng

Bảng **promotion_campaigns** là bảng trung tâm của toàn bộ Promotion Service.

Bảng này lưu thông tin của tất cả các chương trình khuyến mãi được tạo trong hệ thống.

Mỗi Campaign đại diện cho một chiến dịch kinh doanh, ví dụ:

- Khuyến mãi Noel
- Khuyến mãi Tết
- Flash Sale cuối tuần
- Happy Hour
- Sinh nhật khách hàng
- Mua 2 tặng 1
- Giảm giá theo ngân hàng
- Giảm giá theo ví điện tử

Mọi Promotion Rule, Coupon và Voucher đều phải thuộc về một Campaign.

Có thể hiểu Campaign là "cha" của toàn bộ dữ liệu Promotion.

---

# 3. Mục đích của bảng

Bảng này dùng để lưu:

- Thông tin chiến dịch
- Thời gian diễn ra
- Trạng thái hoạt động
- Loại chiến dịch
- Mô tả chiến dịch
- Người tạo
- Phiên bản
- Audit

Bảng này KHÔNG lưu:

- Điều kiện giảm giá
- Danh sách Coupon
- Danh sách Voucher
- Lịch sử sử dụng

Những dữ liệu đó được lưu ở bảng khác.

---

# 4. Khi nào dữ liệu được tạo

Một bản ghi Campaign sẽ được tạo khi:

- Admin tạo Campaign
- Marketing tạo Campaign
- Hệ thống import Campaign
- Đồng bộ Campaign từ hệ thống khác

Ví dụ

```
Noel 2027

Flash Sale

Summer 2028

Black Friday
```

---

# 5. Khi nào dữ liệu được cập nhật

Campaign được cập nhật khi:

- đổi tên
- đổi mô tả
- đổi thời gian
- đổi trạng thái
- đổi hình thức hoạt động
- thay đổi Rule
- cập nhật Version

---

# 6. Khi nào dữ liệu được đọc

Campaign được đọc rất thường xuyên bởi:

Promotion Engine

↓

Booking Service

↓

Admin Portal

↓

Scheduler

↓

Rule Engine

↓

Coupon Generator

↓

Voucher Generator

↓

Analytics

---

# 7. Khi nào dữ liệu được xóa

Campaign gần như không bao giờ Hard Delete.

Chỉ Soft Delete.

```
deleted_at

deleted_by
```

Lý do

- Audit

- Báo cáo

- Điều tra

- Rollback

---

# 8. Vai trò trong hệ thống

Campaign là điểm bắt đầu của toàn bộ luồng Promotion.

Không có Campaign sẽ không tồn tại:

- Rule

- Coupon

- Voucher

- Reservation

- Redemption

---

# 9. Luồng nghiệp vụ

```
Admin

↓

Create Campaign

↓

promotion_campaigns

↓

Promotion Rule

↓

Coupon

↓

Voucher

↓

Reservation

↓

Redemption
```

---

# 10. Những bảng sử dụng promotion_campaigns

| Bảng | Mục đích |
|-------|----------|
| promotion_rules | Xác định Rule thuộc Campaign nào |
| coupons | Coupon được phát hành bởi Campaign |
| vouchers | Voucher được phát hành bởi Campaign |
| promotion_reservations | Biết Reservation áp dụng Campaign nào |
| approval_histories | Duyệt Campaign |
| audit_logs | Ghi log Campaign |
| outbox_events | Phát Event Campaign |

---

# 11. promotion_campaigns hỗ trợ bảng nào

Campaign là dữ liệu gốc hỗ trợ:

- promotion_rules

- coupons

- vouchers

- promotion_reservations

- coupon_redemptions

- voucher_redemptions

- compensation_vouchers

---

# 12. Quan hệ với các bảng khác

```
promotion_campaigns

│

├────────────► promotion_rules

│

├────────────► coupons

│

├────────────► vouchers

│

├────────────► promotion_reservations

│

├────────────► approval_histories

│

├────────────► audit_logs

│

└────────────► outbox_events
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ của Database.

### Lưu gì

Số nguyên tăng tự động.

### Được dùng ở đâu

Join nội bộ.

### Có trả về API không

Không.

---

## public_id

### Công dụng

Định danh công khai của Campaign.

### Lưu gì

UUID.

Ví dụ

```
bfab2e52-a7...
```

### Được sử dụng bởi

- promotion_rules

- coupons

- vouchers

- reservation

- audit

- kafka

### Không bao giờ thay đổi

Đúng.

---

## code

### Công dụng

Mã Campaign.

Ví dụ

```
NOEL2027

BLACKFRIDAY

SUMMER2028
```

Được dùng để tìm kiếm và quản trị.

---

## name

### Công dụng

Tên hiển thị của Campaign.

Ví dụ

```
Khuyến mãi Noel

Flash Sale Cuối Tuần
```

Được hiển thị trên giao diện Admin.

---

## description

### Công dụng

Mô tả chi tiết chiến dịch.

Ví dụ

```
Giảm 20% tất cả vé CGV cuối tuần.
```

---

## campaign_type

### Công dụng

Phân loại Campaign.

Ví dụ

```
COUPON

VOUCHER

CASHBACK

FLASHSALE
```

Promotion Engine sử dụng trường này để xác định hướng xử lý.

---

## status

### Công dụng

Trạng thái Campaign.

Ví dụ

```
DRAFT

PENDING

ACTIVE

PAUSED

EXPIRED

CANCELLED
```

Rule Engine chỉ xử lý Campaign có trạng thái ACTIVE.

---

## start_time

### Công dụng

Thời điểm Campaign bắt đầu.

Scheduler sử dụng để tự động kích hoạt Campaign.

---

## end_time

### Công dụng

Thời điểm kết thúc.

Scheduler tự động đóng Campaign khi hết hạn.

---

## version

### Công dụng

Hỗ trợ Optimistic Lock.

Ngăn hai Admin cập nhật cùng lúc.

---

## metadata_json

### Công dụng

Lưu thông tin mở rộng.

Ví dụ

```
Banner

Theme

Color

Remark

Marketing Info
```

---

## created_at

Thời điểm tạo Campaign.

---

## created_by

Public ID người tạo.

---

## updated_at

Thời điểm cập nhật gần nhất.

---

## updated_by

Public ID người cập nhật.

---

## deleted_at

Thời điểm Soft Delete.

---

## deleted_by

Người thực hiện Soft Delete.

---

# 14. Index

Campaign được Index theo:

- public_id
- code
- status
- campaign_type
- start_time
- end_time
- deleted_at

Các Index này phục vụ:

- Admin Search
- Scheduler
- Promotion Engine
- Dashboard
- API Filter

---

# 15. Business Rule

BR-001

Campaign Code không được trùng.

---

BR-002

Campaign phải có thời gian bắt đầu.

---

BR-003

Ngày kết thúc phải lớn hơn ngày bắt đầu.

---

BR-004

Campaign ở trạng thái ACTIVE mới được áp dụng.

---

BR-005

Campaign bị Soft Delete sẽ không được sử dụng.

---

BR-006

Một Campaign có thể chứa nhiều Promotion Rule.

---

# 16. Ví dụ dữ liệu

| Code | Name | Type | Status |
|------|------|------|---------|
| NOEL2027 | Noel 2027 | COUPON | ACTIVE |
| FLASH01 | Flash Sale | FLASHSALE | ACTIVE |
| MOMDAY | Mother's Day | VOUCHER | DRAFT |

---

# 17. Vai trò trong toàn bộ hệ thống

promotion_campaigns là bảng gốc của Promotion Service.

Toàn bộ các bảng nghiệp vụ phía sau đều được tạo ra hoặc hoạt động dựa trên Campaign.

Nếu Campaign không tồn tại hoặc không ở trạng thái ACTIVE thì Promotion Rule, Coupon, Voucher và Reservation sẽ không được phép hoạt động.

---

**Kết thúc Phase 2**
# Promotion Service Database Design Specification

# Phase 3 - Bảng promotion_rules

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | promotion_rules |
| Vai trò | Core Business |
| Độ ưu tiên | Rất cao |
| Thuộc Campaign | Có |
| Bảng Rule Engine | Có |

---

# 2. Tóm tắt công dụng

Bảng **promotion_rules** lưu toàn bộ điều kiện và hành động của từng chương trình khuyến mãi.

Nếu **promotion_campaigns** trả lời câu hỏi:

> "Đây là chương trình khuyến mãi nào?"

thì **promotion_rules** sẽ trả lời:

> "Điều kiện nào để được giảm giá và sẽ được giảm như thế nào?"

Mỗi Rule đại diện cho một tập điều kiện và hành động độc lập.

Một Campaign có thể chứa nhiều Rule khác nhau.

Ví dụ

```
Campaign Noel

│

├── Rule 1
│   Giảm 20%
│
├── Rule 2
│   Chỉ áp dụng cuối tuần
│
├── Rule 3
│   Chỉ CGV
│
└── Rule 4
    Chỉ khách hàng Gold
```

Promotion Engine sẽ đọc Rule để quyết định giao dịch có đủ điều kiện hưởng ưu đãi hay không.

---

# 3. Mục đích của bảng

Bảng này lưu:

- Điều kiện áp dụng
- Hành động giảm giá
- Thứ tự thực thi Rule
- Độ ưu tiên
- Hiệu lực Rule
- Version Rule
- Metadata

Không lưu:

- Coupon
- Voucher
- Reservation
- Redemption

---

# 4. Khi nào dữ liệu được tạo

Rule được tạo khi:

- Admin tạo Campaign
- Marketing bổ sung Rule
- Clone Campaign
- Import Campaign

Ví dụ

```
Giảm 20%

Giảm 50.000đ

Tặng Voucher

Tặng Combo

Giảm theo Showtime

Giảm theo Seat

Giảm theo Cinema
```

---

# 5. Khi nào dữ liệu được cập nhật

Rule được cập nhật khi:

- đổi điều kiện
- đổi mức giảm
- đổi thời gian
- đổi trạng thái
- đổi độ ưu tiên
- đổi Version

---

# 6. Khi nào dữ liệu được đọc

Promotion Rule là bảng được đọc nhiều nhất trong Promotion Service.

Được đọc bởi

```
Promotion Engine

↓

Booking

↓

Payment

↓

Reservation

↓

Coupon Generator

↓

Voucher Generator
```

Gần như mọi request áp dụng Promotion đều phải đọc bảng này.

---

# 7. Khi nào dữ liệu được xóa

Không Hard Delete.

Chỉ Soft Delete.

```
deleted_at

deleted_by
```

---

# 8. Vai trò trong hệ thống

Rule là trái tim của Promotion Engine.

Campaign chỉ mô tả chương trình.

Rule mới là nơi quyết định

- Có được giảm hay không.
- Giảm bao nhiêu.
- Tặng gì.
- Áp dụng khi nào.
- Áp dụng cho ai.

---

# 9. Luồng nghiệp vụ

```
Booking

↓

Promotion Engine

↓

Load Campaign

↓

Load Rule

↓

Evaluate Conditions

↓

Execute Actions

↓

Return Discount
```

---

# 10. Những bảng sử dụng promotion_rules

| Bảng | Mục đích |
|-------|----------|
| coupons | Sinh Coupon theo Rule |
| vouchers | Sinh Voucher theo Rule |
| promotion_reservations | Xác định Rule đã áp dụng |
| audit_logs | Ghi lịch sử thay đổi Rule |
| outbox_events | Publish Event |

---

# 11. promotion_rules hỗ trợ bảng nào

Rule hỗ trợ:

- coupons
- vouchers
- promotion_reservations
- coupon_redemptions
- voucher_redemptions
- compensation_vouchers

Rule quyết định toàn bộ Discount Engine hoạt động.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

│

└────────────► promotion_rules

                     │

                     ├────────────► coupons

                     │

                     ├────────────► vouchers

                     │

                     ├────────────► reservation

                     │

                     ├────────────► audit

                     │

                     └────────────► outbox
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

Không sử dụng ngoài API.

---

## public_id

### Công dụng

UUID của Rule.

Được các bảng khác sử dụng để biết Rule nào đã được áp dụng.

---

## campaign_public_id

### Công dụng

Liên kết Rule với Campaign.

### Hỗ trợ bảng nào

```
promotion_campaigns.public_id

↓

promotion_rules.campaign_public_id
```

Một Campaign có nhiều Rule.

Một Rule chỉ thuộc một Campaign.

---

## code

### Công dụng

Mã Rule.

Ví dụ

```
RULE-001

RULE-DISCOUNT

RULE-WEEKEND
```

Được sử dụng trong quản trị và Audit.

---

## name

Tên hiển thị của Rule.

Ví dụ

```
Weekend Discount

VIP Customer

Marvel Promotion
```

---

## description

Mô tả Rule.

Ví dụ

```
Giảm 20% cho vé VIP cuối tuần.
```

---

## rule_type

### Công dụng

Phân loại Rule.

Ví dụ

```
PERCENT_DISCOUNT

FIXED_AMOUNT

FREE_ITEM

CASHBACK

FREE_VOUCHER

FREE_COUPON
```

Promotion Engine sử dụng trường này để chọn thuật toán tính ưu đãi.

---

## priority

### Công dụng

Độ ưu tiên của Rule.

Rule có Priority nhỏ hơn sẽ được chạy trước.

Ví dụ

```
Priority

1

↓

5

↓

10

↓

100
```

---

## execution_order

### Công dụng

Thứ tự thực thi.

Ví dụ

```
Rule 1

↓

Rule 2

↓

Rule 3
```

Khác với Priority.

Priority quyết định Rule nào quan trọng hơn.

Execution Order quyết định thứ tự chạy.

---

## stackable

### Công dụng

Cho phép cộng dồn ưu đãi.

Ví dụ

```
Rule A

10%

+

Rule B

50.000đ
```

Nếu TRUE

↓

Áp dụng cả hai.

Nếu FALSE

↓

Chỉ chọn một.

---

## stop_further_rules

### Công dụng

Nếu Rule này thành công thì dừng đánh giá các Rule còn lại.

Ví dụ

```
VIP Customer

↓

Được giảm 50%

↓

Không cần chạy các Rule tiếp theo.
```

---

## enabled

### Công dụng

Bật hoặc tắt Rule.

Promotion Engine bỏ qua Rule bị Disable.

---

## conditions_json

### Công dụng

Lưu toàn bộ điều kiện của Rule.

Ví dụ

```
Movie

Cinema

Showtime

Seat

Ticket Type

Membership

Payment Method

Holiday

Birthday

Minimum Amount

Maximum Quantity
```

Promotion Engine đọc JSON này để đánh giá giao dịch.

---

## actions_json

### Công dụng

Lưu hành động khi Rule thỏa điều kiện.

Ví dụ

```
Giảm %

Giảm tiền

Tặng Voucher

Tặng Coupon

Free Combo

Cashback
```

Promotion Engine thực thi JSON này sau khi Conditions hợp lệ.

---

## metadata_json

Thông tin mở rộng.

Ví dụ

```
Remark

Theme

Marketing

Color

Campaign Owner
```

---

## effective_from

Ngày Rule bắt đầu có hiệu lực.

---

## effective_to

Ngày Rule kết thúc.

---

## version

Version của Rule.

Hỗ trợ Optimistic Lock.

---

## created_at

Ngày tạo.

---

## created_by

Người tạo.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Rule được Index theo:

- public_id
- campaign_public_id
- rule_type
- enabled
- priority
- execution_order
- effective_from
- effective_to
- deleted_at

Các Index phục vụ:

- Rule Engine
- Scheduler
- Dashboard
- Admin API

---

# 15. Business Rule

BR-007

Một Rule chỉ thuộc một Campaign.

---

BR-008

Rule bị Disable sẽ không được chạy.

---

BR-009

Rule hết hiệu lực sẽ bị bỏ qua.

---

BR-010

Priority nhỏ hơn được xử lý trước.

---

BR-011

Nếu stop_further_rules = TRUE thì Promotion Engine dừng đánh giá các Rule tiếp theo.

---

BR-012

Nếu stackable = FALSE thì Rule không được cộng dồn với Rule khác.

---

# 16. Ví dụ dữ liệu

| Code | Name | Type | Priority | Enabled |
|------|------|------|----------|----------|
| RULE-001 | Weekend Discount | PERCENT_DISCOUNT | 1 | TRUE |
| RULE-002 | Marvel Promotion | FIXED_AMOUNT | 5 | TRUE |
| RULE-003 | VIP Member | FREE_VOUCHER | 10 | FALSE |

---

# 17. Vai trò trong toàn bộ hệ thống

promotion_rules là trung tâm của **Rule Engine**.

Toàn bộ quá trình xác định giao dịch có được hưởng khuyến mãi hay không đều phụ thuộc vào bảng này.

Campaign chỉ định nghĩa chương trình, còn Rule mới là nơi quyết định logic nghiệp vụ, điều kiện áp dụng và hành động giảm giá thực tế.

---

**Kết thúc Phase 3**

# Promotion Service Database Design Specification

# Phase 4 - Bảng coupons

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | coupons |
| Vai trò | Core Business |
| Độ ưu tiên | Rất cao |
| Được sinh từ | Promotion Campaign + Promotion Rule |
| Đối tượng sử dụng | Người dùng hoặc hệ thống |

---

# 2. Tóm tắt công dụng

Bảng **coupons** dùng để lưu toàn bộ Coupon được phát hành trong hệ thống.

Coupon là một mã khuyến mãi (Promotion Code) mà người dùng phải nhập hoặc hệ thống tự áp dụng để được hưởng ưu đãi.

Coupon chỉ mô tả quyền được giảm giá.

Coupon KHÔNG lưu lịch sử đã sử dụng.

Lịch sử sử dụng được lưu tại bảng:

```
coupon_redemptions
```

Có thể hiểu đơn giản

```
promotion_campaigns

↓

Sinh Rule

↓

Rule sinh Coupon

↓

Coupon được phát hành

↓

Khách hàng sử dụng Coupon

↓

Coupon Redemption
```

---

# 3. Mục đích của bảng

Bảng này lưu

- Mã Coupon
- Giá trị Coupon
- Trạng thái Coupon
- Điều kiện sử dụng
- Rule tạo Coupon
- Campaign tạo Coupon
- Chủ sở hữu Coupon
- Thời gian hiệu lực
- Số lần được phép sử dụng
- Số lần đã sử dụng

Không lưu

- Booking
- Payment
- Reservation
- Lịch sử sử dụng

---

# 4. Khi nào dữ liệu được tạo

Coupon được tạo khi

- Admin tạo Coupon
- Marketing import Coupon
- Promotion Rule sinh Coupon
- API Generate Coupon
- Batch Generate

Ví dụ

```
NOEL2027

WELCOME100

VIP50

MOVIE20

CGV100K
```

---

# 5. Khi nào dữ liệu được cập nhật

Coupon được cập nhật khi

- đổi trạng thái
- đổi hiệu lực
- đổi chủ sở hữu
- tăng số lượt sử dụng
- khóa Coupon
- hủy Coupon

---

# 6. Khi nào dữ liệu được đọc

Coupon được đọc bởi

```
Booking Service

↓

Promotion Engine

↓

Payment

↓

Reservation

↓

Admin Portal
```

Đây là một trong những bảng được đọc nhiều nhất.

---

# 7. Khi nào dữ liệu được xóa

Coupon gần như không bao giờ Hard Delete.

Chỉ Soft Delete.

```
deleted_at

deleted_by
```

---

# 8. Vai trò trong hệ thống

Coupon là quyền giảm giá có thể được người dùng sử dụng.

Promotion Rule quyết định

"Có tạo Coupon hay không"

Coupon quyết định

"Khách hàng đang sở hữu quyền giảm giá nào"

Coupon Redemption quyết định

"Coupon đó đã được sử dụng hay chưa"

---

# 9. Luồng nghiệp vụ

```
Campaign

↓

Promotion Rule

↓

Generate Coupon

↓

Coupon

↓

Reservation

↓

Payment

↓

Coupon Redemption
```

---

# 10. Những bảng sử dụng coupons

| Bảng | Mục đích |
|-------|----------|
| promotion_reservations | Reservation giữ Coupon |
| coupon_redemptions | Lưu lịch sử sử dụng Coupon |
| audit_logs | Audit thay đổi Coupon |
| approval_histories | Duyệt Coupon (nếu có) |
| outbox_events | Publish Event |

---

# 11. coupons hỗ trợ bảng nào

Coupon hỗ trợ

- promotion_reservations
- coupon_redemptions
- compensation_vouchers

Coupon là nguồn dữ liệu để xác định khách hàng đang sở hữu quyền giảm giá nào.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

│

└────────► promotion_rules

                 │

                 ▼

             coupons

                 │

        ┌────────┴────────┐

        │                 │

        ▼                 ▼

promotion_reservations

coupon_redemptions
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

Không sử dụng ngoài API.

---

## public_id

### Công dụng

UUID của Coupon.

Được Reservation và Redemption sử dụng.

---

## campaign_public_id

### Công dụng

Xác định Coupon thuộc Campaign nào.

Hỗ trợ

```
promotion_campaigns.public_id

↓

coupons.campaign_public_id
```

---

## rule_public_id

### Công dụng

Xác định Coupon được sinh từ Rule nào.

Hỗ trợ

```
promotion_rules.public_id

↓

coupons.rule_public_id
```

Nếu Rule thay đổi có thể biết Coupon được tạo từ Rule nào.

---

## owner_public_id

### Công dụng

Lưu Public ID người sở hữu Coupon.

Ví dụ

```
User A

User B

User C
```

Nếu NULL

↓

Coupon dùng chung.

Nếu có giá trị

↓

Coupon chỉ dành cho chủ sở hữu.

---

## coupon_code

### Công dụng

Mã Coupon khách hàng nhập.

Ví dụ

```
WELCOME50

NOEL2027

FLASH100

VIP2028
```

Booking Service sẽ tìm Coupon theo trường này.

---

## coupon_type

### Công dụng

Phân loại Coupon.

Ví dụ

```
PUBLIC

PRIVATE

SYSTEM

COMPENSATION
```

Promotion Engine sẽ xử lý khác nhau theo từng loại.

---

## status

### Công dụng

Trạng thái Coupon.

Ví dụ

```
ACTIVE

USED

LOCKED

EXPIRED

CANCELLED
```

Booking chỉ được sử dụng Coupon ở trạng thái ACTIVE.

---

## conditions_json

### Công dụng

Lưu điều kiện sử dụng riêng của Coupon.

Ví dụ

```
Movie

Cinema

Membership

Minimum Amount

Maximum Quantity

Payment Method
```

Promotion Engine sẽ kiểm tra trước khi áp dụng.

---

## actions_json

### Công dụng

Lưu thông tin giảm giá.

Ví dụ

```
20%

50.000đ

100.000đ

Free Ticket

Free Combo
```

---

## total_usage_limit

### Công dụng

Tổng số lần Coupon được phép sử dụng.

Ví dụ

```
100

1000

1
```

---

## usage_count

### Công dụng

Đã sử dụng bao nhiêu lần.

Sau mỗi Redemption sẽ tăng lên.

---

## effective_from

Ngày bắt đầu sử dụng.

---

## effective_to

Ngày hết hạn.

---

## metadata_json

Thông tin mở rộng.

Ví dụ

```
Marketing

Remark

Campaign Source

Batch Number
```

---

## version

Optimistic Lock.

---

## created_at

Ngày tạo.

---

## created_by

Người tạo.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Coupon được Index theo

- public_id
- coupon_code
- owner_public_id
- campaign_public_id
- rule_public_id
- status
- effective_from
- effective_to
- deleted_at

Các Index phục vụ

- Booking API
- Promotion Engine
- Reservation
- Dashboard
- Admin Search

---

# 15. Business Rule

BR-013

Coupon Code không được trùng.

---

BR-014

Coupon phải thuộc một Campaign.

---

BR-015

Coupon phải được sinh bởi một Promotion Rule.

---

BR-016

Coupon hết hạn không được sử dụng.

---

BR-017

Coupon ở trạng thái USED không được sử dụng tiếp.

---

BR-018

usage_count không được lớn hơn total_usage_limit.

---

BR-019

Coupon chỉ được Reservation giữ một lần tại cùng một thời điểm.

---

# 16. Ví dụ dữ liệu

| Coupon Code | Type | Owner | Status |
|--------------|------|--------|---------|
| WELCOME100 | PUBLIC | NULL | ACTIVE |
| VIP2027 | PRIVATE | User A | ACTIVE |
| SORRY2027 | COMPENSATION | User C | ACTIVE |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **coupons** là nơi quản lý toàn bộ quyền giảm giá dựa trên mã Coupon.

Đây là nguồn dữ liệu chính để Promotion Engine xác định khách hàng có sở hữu Coupon hợp lệ hay không trước khi tạo Reservation và tiến hành thanh toán.

Sau khi Coupon được sử dụng thành công, quyền giảm giá sẽ được chuyển sang bảng **coupon_redemptions** để lưu lịch sử và phục vụ đối soát, báo cáo cũng như Audit.

---

**Kết thúc Phase 4**

# Promotion Service Database Design Specification

# Phase 5 - Bảng coupon_redemptions

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | coupon_redemptions |
| Vai trò | Runtime Business |
| Độ ưu tiên | Rất cao |
| Được tạo khi | Coupon được sử dụng |
| Chức năng | Lưu lịch sử sử dụng Coupon |

---

# 2. Tóm tắt công dụng

Bảng **coupon_redemptions** dùng để lưu toàn bộ lịch sử sử dụng Coupon trong hệ thống.

Khác với bảng **coupons** chỉ lưu thông tin của Coupon, bảng này ghi nhận **mỗi lần Coupon được áp dụng vào một giao dịch thực tế**.

Mỗi lần khách hàng sử dụng Coupon thành công hoặc thất bại đều có thể tạo một bản ghi Redemption tùy theo nghiệp vụ.

Bảng này là nguồn dữ liệu phục vụ:

- Audit
- Báo cáo
- Đối soát
- Điều tra sự cố
- Thống kê doanh thu
- Phân tích hiệu quả Campaign

---

# 3. Mục đích của bảng

Bảng này lưu

- Coupon nào đã được sử dụng
- Ai sử dụng
- Booking nào sử dụng
- Payment nào sử dụng
- Reservation nào sử dụng
- Giá trị giảm giá thực tế
- Thời điểm sử dụng
- Trạng thái sử dụng

Không lưu

- Điều kiện giảm giá
- Thông tin Campaign
- Logic Promotion

Các dữ liệu đó đã tồn tại ở bảng khác.

---

# 4. Khi nào dữ liệu được tạo

Redemption được tạo khi

- Booking thanh toán thành công
- Coupon được áp dụng thành công
- Hệ thống ghi nhận việc sử dụng Coupon
- Đồng bộ lịch sử từ hệ thống khác (nếu có)

Ví dụ

```
User A

↓

Booking

↓

Coupon WELCOME100

↓

Thanh toán thành công

↓

Coupon Redemption
```

---

# 5. Khi nào dữ liệu được cập nhật

Redemption có thể được cập nhật khi

- Payment thay đổi trạng thái
- Booking bị hủy
- Rollback giao dịch
- Hoàn tiền

Thông thường Redemption rất ít thay đổi sau khi được tạo.

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Admin Portal

↓

Analytics

↓

Finance

↓

Customer Support

↓

Audit
```

Đây là bảng chủ yếu phục vụ truy vấn lịch sử.

---

# 7. Khi nào dữ liệu được xóa

Không Hard Delete.

Chỉ Soft Delete trong các trường hợp đặc biệt theo chính sách lưu trữ dữ liệu.

Thông thường dữ liệu Redemption sẽ được giữ lại để phục vụ đối soát.

---

# 8. Vai trò trong hệ thống

Coupon Redemption là bằng chứng cho thấy Coupon đã được sử dụng.

Nếu Coupon đại diện cho quyền được giảm giá.

Thì Coupon Redemption đại diện cho hành động đã sử dụng quyền đó.

Không có Redemption thì hệ thống không thể biết Coupon đã được sử dụng bao nhiêu lần.

---

# 9. Luồng nghiệp vụ

```
User

↓

Nhập Coupon

↓

Promotion Engine

↓

Reservation

↓

Payment Success

↓

coupon_redemptions

↓

Analytics
```

---

# 10. Những bảng sử dụng coupon_redemptions

| Bảng | Mục đích |
|-------|----------|
| audit_logs | Ghi Audit |
| outbox_events | Publish Event |
| compensation_vouchers | Phát Voucher bồi thường nếu cần |

---

# 11. coupon_redemptions hỗ trợ bảng nào

Coupon Redemption hỗ trợ

- analytics
- audit_logs
- dashboard
- compensation_vouchers

Đây là nguồn dữ liệu chính để thống kê hiệu quả sử dụng Coupon.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

        │

promotion_rules

        │

coupons

        │

        ▼

coupon_redemptions

        │

        ├────────► analytics

        │

        ├────────► audit_logs

        │

        └────────► outbox_events
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

Không sử dụng ngoài API.

---

## public_id

### Công dụng

UUID của Redemption.

Được sử dụng trong Audit và Kafka Event.

---

## coupon_public_id

### Công dụng

Xác định Coupon nào đã được sử dụng.

Liên kết

```
coupons.public_id

↓

coupon_redemptions.coupon_public_id
```

---

## booking_public_id

### Công dụng

Xác định Booking sử dụng Coupon.

Liên kết với Booking Service thông qua Public ID.

Promotion Service không tạo Foreign Key.

---

## payment_public_id

### Công dụng

Xác định Payment liên quan.

Dùng cho đối soát và hoàn tiền.

---

## reservation_public_id

### Công dụng

Liên kết với Promotion Reservation.

Cho phép biết Coupon đã được Reservation nào giữ trước khi thanh toán.

---

## user_public_id

### Công dụng

Người đã sử dụng Coupon.

Dùng cho

- Báo cáo
- Lịch sử khách hàng
- Điều tra gian lận

---

## redeemed_amount

### Công dụng

Giá trị giảm thực tế.

Ví dụ

```
50.000

100.000

20%
```

Đây là số tiền hoặc giá trị đã được áp dụng tại thời điểm thanh toán.

---

## currency

### Công dụng

Đơn vị tiền tệ.

Ví dụ

```
VND

USD
```

---

## redemption_status

### Công dụng

Trạng thái sử dụng Coupon.

Ví dụ

```
SUCCESS

FAILED

ROLLED_BACK

REFUNDED

CANCELLED
```

---

## redeemed_at

### Công dụng

Thời điểm Coupon được sử dụng thành công.

Được sử dụng trong

- Báo cáo
- Dashboard
- Analytics

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Device

Platform

Channel

Remark

Source
```

---

## created_at

Ngày tạo bản ghi.

---

## created_by

Người tạo.

Thông thường là System.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Coupon Redemption được Index theo

- public_id
- coupon_public_id
- booking_public_id
- payment_public_id
- reservation_public_id
- user_public_id
- redemption_status
- redeemed_at

Các Index phục vụ

- Lịch sử khách hàng
- Dashboard
- Analytics
- Audit

---

# 15. Business Rule

BR-020

Một Coupon Redemption chỉ thuộc một Coupon.

---

BR-021

Một Redemption phải tham chiếu đến một Booking.

---

BR-022

Coupon chỉ được ghi nhận Redemption sau khi Payment thành công.

---

BR-023

Nếu Payment bị hoàn tiền, Redemption phải được cập nhật trạng thái phù hợp.

---

BR-024

Giá trị redeemed_amount phải đúng với kết quả Promotion Engine tính toán tại thời điểm giao dịch.

---

BR-025

Coupon Redemption là dữ liệu lịch sử, không được chỉnh sửa giá trị giảm sau khi giao dịch hoàn tất, ngoại trừ cập nhật trạng thái theo nghiệp vụ hoàn tiền hoặc hủy giao dịch.

---

# 16. Ví dụ dữ liệu

| Coupon | Booking | Payment | Amount | Status |
|---------|----------|----------|---------|---------|
| WELCOME100 | BK-001 | PAY-001 | 100000 | SUCCESS |
| VIP50 | BK-002 | PAY-002 | 50000 | SUCCESS |
| FLASH20 | BK-003 | PAY-003 | 20000 | REFUNDED |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **coupon_redemptions** là nguồn dữ liệu lịch sử chính của Coupon.

Toàn bộ báo cáo doanh thu, thống kê hiệu quả Campaign, theo dõi chi phí khuyến mãi, kiểm tra gian lận và Audit đều dựa trên dữ liệu của bảng này.

Đây là bảng xác nhận rằng một Coupon đã thực sự được sử dụng trong một giao dịch và là căn cứ để ngăn Coupon tiếp tục được sử dụng vượt quá giới hạn cho phép.

---

**Kết thúc Phase 5**

# Promotion Service Database Design Specification

# Phase 6 - Bảng vouchers

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | vouchers |
| Vai trò | Core Business |
| Độ ưu tiên | Rất cao |
| Được sinh từ | Promotion Campaign + Promotion Rule |
| Đối tượng sử dụng | Người dùng hoặc hệ thống |

---

# 2. Tóm tắt công dụng

Bảng **vouchers** dùng để lưu toàn bộ Voucher được phát hành trong hệ thống.

Khác với Coupon, Voucher đại diện cho **một quyền lợi đã được cấp cho một người dùng cụ thể**.

Thông thường Voucher sẽ được:

- Phát sau khi hoàn thành một sự kiện.
- Tặng cho khách hàng.
- Đền bù khi giao dịch lỗi.
- Quà sinh nhật.
- Quà thành viên.
- Quà tích điểm.

Voucher luôn có chủ sở hữu.

Khác với Coupon, Voucher gần như không được chia sẻ giữa nhiều người dùng.

---

# 3. Mục đích của bảng

Bảng này dùng để lưu

- Voucher đã phát hành
- Chủ sở hữu Voucher
- Rule tạo Voucher
- Campaign tạo Voucher
- Giá trị Voucher
- Thời gian hiệu lực
- Điều kiện sử dụng
- Trạng thái Voucher
- Giới hạn sử dụng

Không lưu

- Lịch sử sử dụng
- Reservation
- Payment

Lịch sử sử dụng được lưu tại bảng

```
voucher_redemptions
```

---

# 4. Khi nào dữ liệu được tạo

Voucher được tạo khi

- Promotion Rule phát Voucher
- Khách hàng đạt điều kiện
- Sinh nhật khách hàng
- Đổi điểm Loyalty
- Hoàn tiền
- Đền bù giao dịch
- Admin phát Voucher

Ví dụ

```
Voucher sinh nhật

Voucher VIP

Voucher thành viên Gold

Voucher Cashback

Voucher xin lỗi khách hàng
```

---

# 5. Khi nào dữ liệu được cập nhật

Voucher được cập nhật khi

- đổi trạng thái
- đổi ngày hết hạn
- khóa Voucher
- tăng số lần sử dụng
- gia hạn Voucher

---

# 6. Khi nào dữ liệu được đọc

Voucher được đọc bởi

```
Booking Service

↓

Promotion Engine

↓

Reservation

↓

Payment

↓

Customer Portal

↓

Admin Portal
```

---

# 7. Khi nào dữ liệu được xóa

Voucher không Hard Delete.

Chỉ Soft Delete.

```
deleted_at

deleted_by
```

Voucher đã sử dụng vẫn phải được lưu để phục vụ Audit.

---

# 8. Vai trò trong hệ thống

Voucher là tài sản khuyến mãi thuộc sở hữu của khách hàng.

Voucher thể hiện rằng

"Khách hàng đang sở hữu một quyền lợi có thể sử dụng trong tương lai."

Voucher chỉ thể hiện quyền được giảm giá.

Việc Voucher đã được sử dụng hay chưa sẽ được ghi nhận tại bảng

```
voucher_redemptions
```

---

# 9. Luồng nghiệp vụ

```
Promotion Rule

↓

Generate Voucher

↓

Voucher

↓

Customer Wallet

↓

Reservation

↓

Payment

↓

Voucher Redemption
```

---

# 10. Những bảng sử dụng vouchers

| Bảng | Mục đích |
|-------|----------|
| promotion_reservations | Giữ Voucher trong quá trình thanh toán |
| voucher_redemptions | Lưu lịch sử sử dụng Voucher |
| compensation_vouchers | Voucher đền bù |
| audit_logs | Ghi Audit |
| outbox_events | Publish Event |

---

# 11. vouchers hỗ trợ bảng nào

Voucher hỗ trợ

- promotion_reservations
- voucher_redemptions
- compensation_vouchers
- analytics

Voucher là nguồn dữ liệu để xác định khách hàng đang sở hữu quyền lợi gì.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

        │

promotion_rules

        │

        ▼

     vouchers

        │

 ┌──────┴────────┐

 │               │

 ▼               ▼

promotion_reservations

voucher_redemptions

        │

        ▼

compensation_vouchers
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

Không trả về API.

---

## public_id

### Công dụng

UUID của Voucher.

Được sử dụng trong

- Reservation
- Redemption
- Audit
- Kafka

---

## campaign_public_id

### Công dụng

Xác định Voucher thuộc Campaign nào.

Liên kết

```
promotion_campaigns.public_id

↓

vouchers.campaign_public_id
```

---

## rule_public_id

### Công dụng

Xác định Voucher được tạo bởi Rule nào.

Giúp Audit và truy vết nguồn gốc Voucher.

---

## owner_public_id

### Công dụng

Public ID của chủ sở hữu Voucher.

Voucher chỉ được sử dụng bởi chủ sở hữu.

Ví dụ

```
User A

User B

User C
```

---

## voucher_code

### Công dụng

Mã Voucher.

Ví dụ

```
VIP2027

BIRTHDAY50

GOLD100

SORRY001
```

Được dùng khi cần nhập mã hoặc tra cứu.

---

## voucher_type

### Công dụng

Phân loại Voucher.

Ví dụ

```
REWARD

COMPENSATION

CASHBACK

MEMBERSHIP

SYSTEM
```

Promotion Engine xử lý theo từng loại.

---

## status

### Công dụng

Trạng thái Voucher.

Ví dụ

```
ACTIVE

USED

LOCKED

EXPIRED

CANCELLED
```

---

## conditions_json

### Công dụng

Điều kiện áp dụng Voucher.

Ví dụ

```
Movie

Cinema

Membership

Minimum Amount

Payment Method

Ticket Type
```

Promotion Engine đọc JSON để kiểm tra điều kiện.

---

## actions_json

### Công dụng

Thông tin ưu đãi.

Ví dụ

```
Giảm 50.000

Giảm 20%

Free Ticket

Free Combo

Cashback
```

---

## total_usage_limit

### Công dụng

Tổng số lần Voucher được phép sử dụng.

Thông thường

```
1
```

Một số Voucher đặc biệt có thể lớn hơn.

---

## usage_count

### Công dụng

Số lần Voucher đã được sử dụng.

Được cập nhật sau mỗi Redemption thành công.

---

## effective_from

Ngày bắt đầu có hiệu lực.

---

## effective_to

Ngày hết hiệu lực.

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Batch

Campaign Source

Remark

Marketing

Channel
```

---

## version

Hỗ trợ Optimistic Lock.

---

## created_at

Ngày tạo Voucher.

---

## created_by

Người tạo.

Có thể là

- Admin
- Marketing
- System

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Voucher được Index theo

- public_id
- voucher_code
- owner_public_id
- campaign_public_id
- rule_public_id
- status
- effective_from
- effective_to
- deleted_at

Các Index phục vụ

- Booking API
- Customer Wallet
- Dashboard
- Analytics
- Promotion Engine

---

# 15. Business Rule

BR-026

Voucher luôn thuộc một Campaign.

---

BR-027

Voucher luôn được sinh bởi một Promotion Rule.

---

BR-028

Voucher phải có chủ sở hữu.

---

BR-029

Voucher hết hạn không được sử dụng.

---

BR-030

Voucher ở trạng thái USED không được sử dụng lại.

---

BR-031

usage_count không được vượt quá total_usage_limit.

---

BR-032

Một Voucher chỉ được Reservation giữ bởi một giao dịch tại cùng một thời điểm.

---

# 16. Ví dụ dữ liệu

| Voucher Code | Owner | Type | Status |
|---------------|-------|------|---------|
| VIP2027 | User A | REWARD | ACTIVE |
| BIRTHDAY50 | User B | MEMBERSHIP | ACTIVE |
| SORRY001 | User C | COMPENSATION | ACTIVE |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **vouchers** là nơi quản lý toàn bộ Voucher mà hệ thống đã phát hành cho khách hàng.

Đây là nguồn dữ liệu chính để xác định khách hàng đang sở hữu những quyền lợi nào trước khi thực hiện đặt vé.

Voucher sẽ được Promotion Engine kiểm tra trong quá trình tính khuyến mãi và sau khi giao dịch hoàn tất, việc sử dụng Voucher sẽ được ghi nhận tại bảng **voucher_redemptions** nhằm phục vụ Audit, thống kê, đối soát và quản lý vòng đời của Voucher.

---

**Kết thúc Phase 6**

# Promotion Service Database Design Specification

# Phase 7 - Bảng voucher_redemptions

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | voucher_redemptions |
| Vai trò | Runtime Business |
| Độ ưu tiên | Rất cao |
| Được tạo khi | Voucher được sử dụng |
| Chức năng | Lưu lịch sử sử dụng Voucher |

---

# 2. Tóm tắt công dụng

Bảng **voucher_redemptions** dùng để lưu toàn bộ lịch sử sử dụng Voucher trong hệ thống.

Nếu bảng **vouchers** lưu quyền sở hữu Voucher thì bảng **voucher_redemptions** lưu việc quyền đó đã được sử dụng như thế nào.

Mỗi lần khách hàng sử dụng Voucher thành công sẽ sinh ra một bản ghi Redemption.

Bảng này phục vụ:

- Audit
- Analytics
- Báo cáo
- Đối soát
- Chăm sóc khách hàng
- Điều tra giao dịch

Voucher sau khi được sử dụng sẽ không mất đi khỏi bảng **vouchers**, mà lịch sử sẽ được lưu tại bảng này.

---

# 3. Mục đích của bảng

Bảng này lưu

- Voucher nào được sử dụng
- Chủ sở hữu Voucher
- Booking sử dụng
- Payment sử dụng
- Reservation liên quan
- Giá trị ưu đãi thực tế
- Thời điểm sử dụng
- Trạng thái sử dụng

Không lưu

- Điều kiện Voucher
- Logic Promotion
- Campaign

---

# 4. Khi nào dữ liệu được tạo

Voucher Redemption được tạo khi

- Booking thanh toán thành công
- Voucher được áp dụng thành công
- Promotion Engine xác nhận Voucher hợp lệ

Ví dụ

```
User A

↓

Voucher VIP50

↓

Booking

↓

Payment Success

↓

Voucher Redemption
```

---

# 5. Khi nào dữ liệu được cập nhật

Voucher Redemption được cập nhật khi

- Booking bị hủy
- Payment Refund
- Rollback

Thông thường dữ liệu rất ít thay đổi sau khi tạo.

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Admin Portal

↓

Finance

↓

Analytics

↓

Customer Support

↓

Audit
```

---

# 7. Khi nào dữ liệu được xóa

Không Hard Delete.

Chỉ Soft Delete theo chính sách lưu trữ.

Voucher Redemption gần như luôn được giữ lại phục vụ đối soát.

---

# 8. Vai trò trong hệ thống

Voucher Redemption là bằng chứng rằng Voucher đã được sử dụng.

Nếu bảng

```
vouchers
```

lưu

"Khách hàng có Voucher"

thì bảng

```
voucher_redemptions
```

lưu

"Khách hàng đã sử dụng Voucher."

Đây là căn cứ để

- tăng usage_count
- khóa Voucher
- thống kê Campaign
- tính chi phí Marketing

---

# 9. Luồng nghiệp vụ

```
Customer

↓

Chọn Voucher

↓

Promotion Engine

↓

Reservation

↓

Payment

↓

voucher_redemptions

↓

Analytics
```

---

# 10. Những bảng sử dụng voucher_redemptions

| Bảng | Mục đích |
|-------|----------|
| compensation_vouchers | Sinh Voucher bồi thường nếu giao dịch lỗi |
| audit_logs | Ghi Audit |
| outbox_events | Publish Event |

---

# 11. voucher_redemptions hỗ trợ bảng nào

Voucher Redemption hỗ trợ

- audit_logs
- analytics
- dashboard
- compensation_vouchers

Là nguồn dữ liệu thống kê việc sử dụng Voucher.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

        │

promotion_rules

        │

     vouchers

        │

        ▼

voucher_redemptions

        │

        ├────────► analytics

        │

        ├────────► audit_logs

        │

        └────────► outbox_events
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

Không sử dụng trong API.

---

## public_id

### Công dụng

UUID của Voucher Redemption.

Được sử dụng trong

- Kafka
- Audit

---

## voucher_public_id

### Công dụng

Xác định Voucher đã được sử dụng.

Liên kết

```
vouchers.public_id

↓

voucher_redemptions.voucher_public_id
```

---

## booking_public_id

### Công dụng

Booking đã sử dụng Voucher.

Liên kết với Booking Service thông qua Public ID.

---

## payment_public_id

### Công dụng

Payment liên quan.

Phục vụ Refund và truy vết giao dịch.

---

## reservation_public_id

### Công dụng

Reservation đã giữ Voucher trước khi thanh toán.

Cho phép truy vết toàn bộ vòng đời giao dịch.

---

## user_public_id

### Công dụng

Chủ sở hữu Voucher.

Được dùng trong

- Lịch sử khách hàng
- Báo cáo
- Chống gian lận

---

## redeemed_amount

### Công dụng

Giá trị ưu đãi thực tế mà Voucher mang lại.

Ví dụ

```
100.000

50.000

20%
```

Lưu đúng giá trị tại thời điểm giao dịch.

---

## currency

### Công dụng

Đơn vị tiền tệ.

Ví dụ

```
VND

USD
```

---

## redemption_status

### Công dụng

Trạng thái Redemption.

Ví dụ

```
SUCCESS

FAILED

ROLLED_BACK

REFUNDED

CANCELLED
```

---

## redeemed_at

### Công dụng

Thời điểm Voucher được sử dụng.

Được dùng trong

- Dashboard
- Analytics
- Báo cáo doanh thu

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Platform

Channel

Source

Remark

IP Address
```

---

## created_at

Ngày tạo.

---

## created_by

Người tạo.

Thông thường là System.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Voucher Redemption được Index theo

- public_id
- voucher_public_id
- booking_public_id
- payment_public_id
- reservation_public_id
- user_public_id
- redemption_status
- redeemed_at

Các Index phục vụ

- Dashboard
- Customer History
- Audit
- Analytics

---

# 15. Business Rule

BR-033

Một Voucher Redemption chỉ thuộc một Voucher.

---

BR-034

Voucher chỉ được Redemption sau khi Payment thành công.

---

BR-035

Voucher Redemption phải tham chiếu tới Booking đã sử dụng Voucher.

---

BR-036

Voucher Redemption phải lưu đúng giá trị ưu đãi thực tế tại thời điểm thanh toán.

---

BR-037

Nếu giao dịch hoàn tiền, Redemption phải được cập nhật trạng thái tương ứng.

---

BR-038

Voucher Redemption là dữ liệu lịch sử và không được thay đổi giá trị ưu đãi sau khi giao dịch hoàn tất.

---

# 16. Ví dụ dữ liệu

| Voucher | Booking | Payment | Amount | Status |
|----------|----------|----------|---------|---------|
| VIP50 | BK-001 | PAY-001 | 50000 | SUCCESS |
| BIRTHDAY100 | BK-002 | PAY-002 | 100000 | SUCCESS |
| GOLD20 | BK-003 | PAY-003 | 20000 | REFUNDED |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **voucher_redemptions** là nơi lưu toàn bộ lịch sử sử dụng Voucher của khách hàng.

Đây là nguồn dữ liệu quan trọng phục vụ thống kê hiệu quả các chương trình khuyến mãi, đối soát tài chính, kiểm tra gian lận, chăm sóc khách hàng và Audit.

Sau mỗi giao dịch thành công, Voucher sẽ được ghi nhận tại bảng này để đảm bảo hệ thống luôn biết Voucher nào đã được sử dụng, sử dụng khi nào, bởi ai và cho giao dịch nào.

---

**Kết thúc Phase 7**

# Promotion Service Database Design Specification

# Phase 8 - Bảng promotion_reservations

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | promotion_reservations |
| Vai trò | Runtime Business |
| Độ ưu tiên | Cực kỳ cao |
| Được tạo khi | Khách hàng bắt đầu giữ khuyến mãi trước thanh toán |
| Chức năng | Giữ (Reserve) Coupon hoặc Voucher để tránh sử dụng đồng thời |

---

# 2. Tóm tắt công dụng

Bảng **promotion_reservations** dùng để lưu trạng thái giữ tạm (Reservation) của Coupon hoặc Voucher trong quá trình khách hàng đang thanh toán.

Đây là một trong những bảng quan trọng nhất của Promotion Service vì nó giải quyết bài toán **Concurrency (đồng thời)**.

Ví dụ

```
Voucher VIP50

↓

User A

↓

Đang thanh toán

↓

Voucher bị Reservation

↓

User B

↓

Không thể sử dụng Voucher đó
```

Nếu không có Reservation thì cùng một Voucher hoặc Coupon có thể bị nhiều người sử dụng cùng lúc.

---

# 3. Mục đích của bảng

Bảng này dùng để lưu

- Coupon đang được giữ
- Voucher đang được giữ
- Booking đang giữ Promotion
- Người đang sử dụng
- Thời gian hết hạn Reservation
- Trạng thái Reservation

Không lưu

- Lịch sử sử dụng
- Giá trị giảm giá cuối cùng

---

# 4. Khi nào dữ liệu được tạo

Reservation được tạo khi

- Khách nhập Coupon
- Khách chọn Voucher
- Promotion Engine xác nhận Promotion hợp lệ
- Booking bắt đầu bước thanh toán

Ví dụ

```
Customer

↓

Nhập Coupon

↓

Promotion Engine

↓

Create Reservation

↓

Khóa Coupon

↓

Thanh toán
```

---

# 5. Khi nào dữ liệu được cập nhật

Reservation được cập nhật khi

- Gia hạn thời gian giữ
- Payment thành công
- Payment thất bại
- Booking bị hủy
- Reservation hết hạn

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Promotion Engine

↓

Booking Service

↓

Payment Service

↓

Scheduler

↓

Background Job
```

Đây là bảng được đọc và cập nhật rất thường xuyên.

---

# 7. Khi nào dữ liệu được xóa

Reservation thường không Hard Delete.

Khi Reservation kết thúc

↓

Đánh dấu

```
COMPLETED

EXPIRED

CANCELLED
```

Sau một thời gian mới được dọn dẹp theo chính sách lưu trữ.

---

# 8. Vai trò trong hệ thống

Reservation là cơ chế chống sử dụng Promotion đồng thời.

Ví dụ

```
Voucher

↓

Reservation

↓

Payment

↓

Voucher Redemption
```

Trong khoảng thời gian Reservation còn hiệu lực

↓

Không giao dịch nào khác được sử dụng Promotion đó.

---

# 9. Luồng nghiệp vụ

```
Customer

↓

Booking

↓

Promotion Engine

↓

Validate Coupon

↓

Create Reservation

↓

Lock Promotion

↓

Payment

↓

Success

↓

Create Redemption

↓

Release Reservation
```

Nếu Payment thất bại

```
Reservation

↓

Expired

↓

Release Coupon

↓

Cho phép người khác sử dụng
```

---

# 10. Những bảng sử dụng promotion_reservations

| Bảng | Mục đích |
|-------|----------|
| coupon_redemptions | Xác định Reservation tạo Redemption nào |
| voucher_redemptions | Xác định Reservation tạo Redemption nào |
| audit_logs | Ghi Audit |
| outbox_events | Publish Event |
| promotion_idempotency_keys | Kiểm tra Request đã Reservation chưa |

---

# 11. promotion_reservations hỗ trợ bảng nào

Reservation hỗ trợ

- coupons
- vouchers
- coupon_redemptions
- voucher_redemptions
- booking
- payment

Đây là bảng bảo vệ toàn bộ Promotion Service khỏi lỗi sử dụng Promotion nhiều lần đồng thời.

---

# 12. Quan hệ với các bảng

```
coupons

      │

      ▼

promotion_reservations

      ▲

      │

vouchers

      │

      ▼

coupon_redemptions

voucher_redemptions
```

Reservation đóng vai trò trung gian giữa Promotion và Redemption.

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

Không sử dụng trong API.

---

## public_id

### Công dụng

UUID của Reservation.

Được sử dụng bởi

- Booking
- Payment
- Audit
- Kafka

---

## campaign_public_id

### Công dụng

Campaign mà Reservation đang áp dụng.

Giúp truy vết nguồn gốc Promotion.

---

## rule_public_id

### Công dụng

Rule đã được áp dụng.

Cho phép xác định chính xác Rule tạo ra Reservation.

---

## coupon_public_id

### Công dụng

Coupon đang được giữ.

Nếu Reservation sử dụng Voucher thì trường này có thể NULL.

---

## voucher_public_id

### Công dụng

Voucher đang được giữ.

Nếu Reservation sử dụng Coupon thì trường này có thể NULL.

---

## booking_public_id

### Công dụng

Booking đang sử dụng Promotion.

Liên kết với Booking Service thông qua Public ID.

---

## payment_public_id

### Công dụng

Payment liên quan.

Ban đầu có thể NULL.

Sau khi Payment được tạo sẽ cập nhật.

---

## user_public_id

### Công dụng

Người đang giữ Promotion.

Dùng để chống nhiều tài khoản sử dụng cùng Promotion.

---

## reservation_status

### Công dụng

Trạng thái Reservation.

Ví dụ

```
PENDING

ACTIVE

COMPLETED

EXPIRED

CANCELLED
```

Ý nghĩa

- **PENDING**: Reservation vừa được tạo.
- **ACTIVE**: Promotion đang được giữ.
- **COMPLETED**: Thanh toán thành công.
- **EXPIRED**: Hết thời gian giữ.
- **CANCELLED**: Booking bị hủy hoặc người dùng hủy.

---

## reserved_at

### Công dụng

Thời điểm bắt đầu giữ Promotion.

---

## expires_at

### Công dụng

Thời điểm Reservation hết hạn.

Ví dụ

```
Reserved

08:00

↓

Expires

08:15
```

Scheduler sẽ tự động giải phóng Reservation quá hạn.

---

## released_at

### Công dụng

Thời điểm Promotion được giải phóng.

Ví dụ

- Payment thành công
- Payment thất bại
- Reservation hết hạn

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Device

Platform

IP

Channel

Remark
```

---

## created_at

Ngày tạo Reservation.

---

## created_by

Thông thường là System.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Reservation được Index theo

- public_id
- booking_public_id
- payment_public_id
- coupon_public_id
- voucher_public_id
- user_public_id
- reservation_status
- expires_at

Các Index phục vụ

- Booking API
- Promotion Engine
- Scheduler
- Payment
- Dashboard

---

# 15. Business Rule

BR-039

Một Coupon hoặc Voucher chỉ được có một Reservation ở trạng thái ACTIVE tại cùng một thời điểm.

---

BR-040

Reservation phải có thời gian hết hạn.

---

BR-041

Reservation hết hạn phải được Scheduler tự động giải phóng.

---

BR-042

Payment thành công phải chuyển Reservation sang trạng thái COMPLETED.

---

BR-043

Payment thất bại phải giải phóng Reservation.

---

BR-044

Reservation không được tồn tại vô thời hạn.

---

BR-045

Coupon hoặc Voucher chỉ được tạo Redemption khi Reservation đang ở trạng thái ACTIVE.

---

# 16. Ví dụ dữ liệu

| Promotion | Booking | User | Status | Expires |
|------------|----------|------|----------|----------|
| Coupon WELCOME100 | BK-001 | User A | ACTIVE | 08:15 |
| Voucher VIP50 | BK-002 | User B | COMPLETED | 09:30 |
| Coupon FLASH20 | BK-003 | User C | EXPIRED | 10:00 |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **promotion_reservations** là cơ chế khóa mềm (Soft Lock) của Promotion Service.

Đây là thành phần đảm bảo một Coupon hoặc Voucher không bị nhiều giao dịch sử dụng đồng thời, giúp ngăn chặn tình trạng vượt quá giới hạn sử dụng, trùng lặp khuyến mãi và sai lệch dữ liệu khi có nhiều người dùng thanh toán cùng lúc.

Reservation là cầu nối giữa quá trình **kiểm tra khuyến mãi** và **ghi nhận lịch sử sử dụng**, đóng vai trò quan trọng trong việc đảm bảo tính nhất quán dữ liệu và khả năng mở rộng của toàn bộ Promotion Service.

---

**Kết thúc Phase 8**

# Promotion Service Database Design Specification

# Phase 9 - Bảng compensation_vouchers

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | compensation_vouchers |
| Vai trò | Business Support |
| Độ ưu tiên | Cao |
| Được tạo khi | Hệ thống cần bồi thường khách hàng |
| Chức năng | Quản lý toàn bộ Voucher bồi thường |

---

# 2. Tóm tắt công dụng

Bảng **compensation_vouchers** dùng để lưu toàn bộ thông tin liên quan đến các Voucher bồi thường được hệ thống phát hành cho khách hàng khi xảy ra sự cố.

Khác với bảng **vouchers** lưu mọi Voucher trong hệ thống, bảng này chỉ tập trung vào **nghiệp vụ bồi thường (Compensation)**.

Ví dụ

- Thanh toán thành công nhưng không tạo Booking.
- Booking thất bại sau khi đã trừ tiền.
- Promotion bị lỗi.
- Hủy suất chiếu.
- Hoàn tiền không thể thực hiện ngay.
- Hệ thống bảo trì gây ảnh hưởng khách hàng.
- Chương trình chăm sóc khách hàng đặc biệt.

Bảng này giúp doanh nghiệp theo dõi toàn bộ chi phí phát sinh từ các Voucher bồi thường.

---

# 3. Mục đích của bảng

Bảng này lưu

- Voucher bồi thường nào được phát hành
- Nguyên nhân bồi thường
- Giao dịch gây ra bồi thường
- Người được bồi thường
- Trạng thái xử lý
- Người phê duyệt
- Thời điểm phát hành

Không lưu

- Điều kiện Voucher
- Logic Promotion
- Lịch sử sử dụng Voucher

Các dữ liệu đó được quản lý bởi bảng vouchers và voucher_redemptions.

---

# 4. Khi nào dữ liệu được tạo

Compensation Voucher được tạo khi

- Booking thất bại
- Payment lỗi
- Refund thất bại
- Promotion Engine lỗi
- Admin phát Voucher bồi thường
- CSKH xử lý khiếu nại
- Hệ thống tự động bồi thường theo Business Rule

Ví dụ

```
Payment Success

↓

Booking Failed

↓

Create Compensation Voucher

↓

Customer Wallet
```

---

# 5. Khi nào dữ liệu được cập nhật

Được cập nhật khi

- Voucher đã phát hành
- Voucher bị hủy
- Thay đổi lý do
- Thay đổi người phê duyệt
- Hoàn tất quy trình bồi thường

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Customer Support

↓

Finance

↓

Admin Portal

↓

Audit

↓

Analytics
```

---

# 7. Khi nào dữ liệu được xóa

Không Hard Delete.

Chỉ Soft Delete.

Lịch sử bồi thường phải được lưu lại để phục vụ kiểm toán và đối soát tài chính.

---

# 8. Vai trò trong hệ thống

Compensation Voucher giúp doanh nghiệp

- Bồi thường khách hàng nhanh chóng.
- Theo dõi toàn bộ chi phí phát sinh.
- Phân tích nguyên nhân lỗi hệ thống.
- Đánh giá chất lượng dịch vụ.

Đây là bảng hỗ trợ nghiệp vụ chăm sóc khách hàng và vận hành.

---

# 9. Luồng nghiệp vụ

```
Booking

↓

Payment

↓

System Error

↓

Compensation Decision

↓

Create Voucher

↓

compensation_vouchers

↓

Customer Wallet

↓

Voucher Redemption
```

---

# 10. Những bảng sử dụng compensation_vouchers

| Bảng | Mục đích |
|-------|----------|
| vouchers | Voucher được phát hành để bồi thường |
| voucher_redemptions | Theo dõi Voucher đã được sử dụng |
| audit_logs | Ghi lịch sử |
| approval_histories | Lưu quá trình phê duyệt |
| outbox_events | Publish Event |

---

# 11. compensation_vouchers hỗ trợ bảng nào

Compensation Voucher hỗ trợ

- vouchers
- voucher_redemptions
- analytics
- finance
- customer_support
- audit_logs

Đây là nguồn dữ liệu chính để thống kê chi phí bồi thường.

---

# 12. Quan hệ với các bảng

```
Booking

      │

Payment

      │

      ▼

compensation_vouchers

      │

      ▼

vouchers

      │

      ▼

voucher_redemptions
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

---

## public_id

### Công dụng

UUID của Compensation Voucher.

Được sử dụng trong Audit và Kafka.

---

## voucher_public_id

### Công dụng

Voucher được phát hành để bồi thường.

Liên kết

```
vouchers.public_id

↓

compensation_vouchers.voucher_public_id
```

---

## booking_public_id

### Công dụng

Booking gây ra việc bồi thường.

Giúp truy vết nguyên nhân.

---

## payment_public_id

### Công dụng

Payment liên quan.

Được Finance sử dụng để đối soát.

---

## user_public_id

### Công dụng

Khách hàng được bồi thường.

---

## compensation_type

### Công dụng

Loại bồi thường.

Ví dụ

```
PAYMENT_FAILURE

BOOKING_FAILURE

SHOW_CANCELLED

SYSTEM_ERROR

CUSTOMER_SERVICE

MANUAL
```

---

## compensation_reason

### Công dụng

Lý do chi tiết.

Ví dụ

```
Thanh toán thành công nhưng không tạo Booking.

Suất chiếu bị hủy.

Lỗi Promotion Engine.
```

---

## compensation_amount

### Công dụng

Giá trị bồi thường.

Ví dụ

```
100.000

200.000
```

---

## currency

### Công dụng

Đơn vị tiền tệ.

Ví dụ

```
VND

USD
```

---

## approval_status

### Công dụng

Trạng thái phê duyệt.

Ví dụ

```
PENDING

APPROVED

REJECTED
```

---

## approved_by

### Công dụng

Public ID người phê duyệt.

---

## approved_at

### Công dụng

Thời điểm phê duyệt.

---

## issued_at

### Công dụng

Thời điểm Voucher được phát hành.

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Remark

Evidence

Support Ticket

Operator

Department
```

---

## created_at

Ngày tạo.

---

## created_by

Người tạo.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Compensation Voucher được Index theo

- public_id
- voucher_public_id
- booking_public_id
- payment_public_id
- user_public_id
- compensation_type
- approval_status
- issued_at

Các Index phục vụ

- Customer Support
- Finance
- Dashboard
- Analytics
- Audit

---

# 15. Business Rule

BR-046

Một Compensation Voucher phải tham chiếu đến một Voucher hợp lệ.

---

BR-047

Voucher chỉ được phát hành sau khi Compensation được phê duyệt hoặc đáp ứng điều kiện tự động.

---

BR-048

Compensation phải có lý do rõ ràng.

---

BR-049

Mọi Compensation đều phải truy vết được Booking hoặc Payment liên quan nếu có.

---

BR-050

Compensation Voucher không được chỉnh sửa giá trị sau khi Voucher đã phát hành.

---

BR-051

Mọi Compensation đều phải được ghi Audit.

---

# 16. Ví dụ dữ liệu

| Voucher | Booking | Type | Amount | Approval |
|----------|----------|------|---------|----------|
| SORRY001 | BK-001 | PAYMENT_FAILURE | 100000 | APPROVED |
| APOLOGY50 | BK-002 | SHOW_CANCELLED | 50000 | APPROVED |
| VIPCOMP01 | BK-003 | CUSTOMER_SERVICE | 150000 | PENDING |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **compensation_vouchers** là trung tâm quản lý toàn bộ nghiệp vụ bồi thường của Promotion Service.

Bảng này giúp doanh nghiệp kiểm soát chi phí phát sinh do lỗi hệ thống hoặc chính sách chăm sóc khách hàng, đồng thời đảm bảo mọi Voucher bồi thường đều có thể truy vết đầy đủ từ nguyên nhân phát sinh, quy trình phê duyệt cho đến quá trình khách hàng sử dụng Voucher.

---

**Kết thúc Phase 9**

# Promotion Service Database Design Specification

# Phase 10 - Bảng promotion_configurations

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | promotion_configurations |
| Vai trò | System Configuration |
| Độ ưu tiên | Rất cao |
| Được tạo khi | Khởi tạo hệ thống hoặc Admin cấu hình |
| Chức năng | Quản lý toàn bộ cấu hình động của Promotion Service |

---

# 2. Tóm tắt công dụng

Bảng **promotion_configurations** dùng để lưu các cấu hình (Configuration) của Promotion Service mà không cần thay đổi source code.

Thay vì hard-code các giá trị trong ứng dụng, toàn bộ thông số nghiệp vụ sẽ được quản lý tập trung tại bảng này.

Ví dụ

- Thời gian giữ Reservation
- Cho phép Stack Promotion
- Giới hạn số Promotion mỗi Booking
- Bật/Tắt Voucher
- Bật/Tắt Coupon
- Tỷ lệ Retry
- Kafka Retry
- Scheduler Interval
- Cache TTL
- Rule Engine Configuration

Thông qua bảng này, Admin có thể thay đổi hành vi của hệ thống mà không cần triển khai lại dịch vụ.

---

# 3. Mục đích của bảng

Bảng này lưu

- Key cấu hình
- Giá trị cấu hình
- Kiểu dữ liệu
- Phạm vi áp dụng
- Mô tả
- Trạng thái

Không lưu

- Business Data
- Campaign
- Coupon
- Voucher
- Redemption

Đây là bảng cấu hình hệ thống.

---

# 4. Khi nào dữ liệu được tạo

Configuration được tạo khi

- Khởi tạo hệ thống
- Thêm tính năng mới
- Admin thêm cấu hình
- DevOps thêm cấu hình vận hành

Ví dụ

```
PROMOTION_RESERVATION_TIMEOUT

↓

15 phút
```

---

# 5. Khi nào dữ liệu được cập nhật

Được cập nhật khi

- Thay đổi Business Rule
- Điều chỉnh tham số
- Thay đổi Scheduler
- Điều chỉnh Retry
- Thay đổi Cache

Một số cấu hình có thể được áp dụng ngay sau khi cập nhật, một số khác yêu cầu làm mới Cache hoặc khởi động lại dịch vụ.

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Promotion Engine

↓

Scheduler

↓

Kafka Consumer

↓

Booking Service

↓

Admin Portal
```

Hầu như mọi thành phần của Promotion Service đều sử dụng bảng này.

---

# 7. Khi nào dữ liệu được xóa

Thông thường không xóa.

Nếu một cấu hình không còn sử dụng

↓

Đánh dấu

```
INACTIVE
```

hoặc

```
DEPRECATED
```

Không nên Hard Delete để tránh mất lịch sử cấu hình.

---

# 8. Vai trò trong hệ thống

Promotion Configuration là trung tâm quản lý toàn bộ hành vi động của Promotion Service.

Thông qua bảng này, doanh nghiệp có thể

- thay đổi thời gian Reservation
- bật/tắt Promotion
- thay đổi giới hạn sử dụng
- thay đổi Scheduler
- điều chỉnh Retry
- thay đổi Cache

mà không cần sửa mã nguồn.

---

# 9. Luồng nghiệp vụ

```
Admin

↓

Update Configuration

↓

promotion_configurations

↓

Configuration Cache

↓

Promotion Engine

↓

Booking

↓

Scheduler

↓

Kafka Consumer
```

---

# 10. Những bảng sử dụng promotion_configurations

| Bảng | Mục đích |
|-------|----------|
| promotion_reservations | Thời gian Reservation |
| promotion_rules | Rule Engine Configuration |
| coupons | Giới hạn Coupon |
| vouchers | Giới hạn Voucher |
| outbox_events | Retry Policy |
| audit_logs | Ghi lịch sử thay đổi cấu hình |

---

# 11. promotion_configurations hỗ trợ bảng nào

Configuration hỗ trợ gần như toàn bộ Promotion Service

- promotion_campaigns
- promotion_rules
- coupons
- vouchers
- reservation
- scheduler
- kafka
- cache
- monitoring

Đây là bảng cấu hình trung tâm.

---

# 12. Quan hệ với các bảng

```
promotion_configurations

        │

        ├────────► Promotion Engine

        ├────────► Reservation

        ├────────► Scheduler

        ├────────► Kafka

        ├────────► Cache

        └────────► Monitoring
```

Khác với các bảng nghiệp vụ khác, bảng này chủ yếu được tham chiếu bởi tầng Service thay vì có quan hệ khóa ngoại trực tiếp.

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

---

## public_id

### Công dụng

UUID của Configuration.

Được sử dụng trong Audit và API quản trị.

---

## config_key

### Công dụng

Tên duy nhất của cấu hình.

Ví dụ

```
PROMOTION_RESERVATION_TIMEOUT

MAX_PROMOTION_PER_BOOKING

ENABLE_VOUCHER

ENABLE_COUPON

ENABLE_STACKABLE

OUTBOX_RETRY_LIMIT

CACHE_TTL

SCHEDULER_INTERVAL
```

Không được phép trùng.

---

## config_value

### Công dụng

Giá trị của cấu hình.

Ví dụ

```
15

true

false

3600

100
```

Tùy thuộc vào kiểu dữ liệu của `config_type`.

---

## config_type

### Công dụng

Kiểu dữ liệu của cấu hình.

Ví dụ

```
STRING

INTEGER

BOOLEAN

DECIMAL

JSON

DURATION
```

Giúp hệ thống chuyển đổi giá trị chính xác khi sử dụng.

---

## config_group

### Công dụng

Nhóm cấu hình.

Ví dụ

```
RESERVATION

RULE_ENGINE

CACHE

KAFKA

OUTBOX

SCHEDULER

GENERAL
```

Giúp Admin dễ dàng quản lý.

---

## description

### Công dụng

Mô tả ý nghĩa của cấu hình.

Được hiển thị trên giao diện quản trị.

---

## editable

### Công dụng

Cho biết Admin có được phép thay đổi cấu hình hay không.

Ví dụ

```
true

false
```

Một số cấu hình hệ thống quan trọng chỉ DevOps mới được thay đổi.

---

## requires_restart

### Công dụng

Cho biết thay đổi cấu hình có yêu cầu khởi động lại dịch vụ hay không.

Ví dụ

```
true

false
```

---

## status

### Công dụng

Trạng thái cấu hình.

Ví dụ

```
ACTIVE

INACTIVE

DEPRECATED
```

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Default Value

Minimum

Maximum

Validation Regex

Remark
```

---

## version

### Công dụng

Hỗ trợ Optimistic Lock.

---

## created_at

Ngày tạo cấu hình.

---

## created_by

Người tạo.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Configuration được Index theo

- public_id
- config_key
- config_group
- status
- editable
- deleted_at

Các Index phục vụ

- Configuration API
- Admin Portal
- Cache Refresh
- Monitoring

---

# 15. Business Rule

BR-065

Config Key phải là duy nhất trong toàn hệ thống.

---

BR-066

Mỗi Config phải xác định rõ kiểu dữ liệu (`config_type`).

---

BR-067

Giá trị cấu hình phải hợp lệ với kiểu dữ liệu đã khai báo.

---

BR-068

Không được chỉnh sửa Config ở trạng thái DEPRECATED.

---

BR-069

Mọi thay đổi cấu hình phải được ghi vào Audit Log.

---

BR-070

Các cấu hình đánh dấu `requires_restart = true` chỉ có hiệu lực sau khi dịch vụ được khởi động lại hoặc tải lại cấu hình theo cơ chế của hệ thống.

---

BR-071

Các cấu hình có giới hạn (`Minimum`, `Maximum`) phải được kiểm tra trước khi lưu.

---

# 16. Ví dụ dữ liệu

| Config Key | Value | Type | Group | Status |
|-------------|-------|------|--------|--------|
| PROMOTION_RESERVATION_TIMEOUT | 15 | INTEGER | RESERVATION | ACTIVE |
| ENABLE_VOUCHER | true | BOOLEAN | GENERAL | ACTIVE |
| MAX_PROMOTION_PER_BOOKING | 3 | INTEGER | RULE_ENGINE | ACTIVE |
| CACHE_TTL | 300 | INTEGER | CACHE | ACTIVE |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **promotion_configurations** là trung tâm quản lý toàn bộ cấu hình động của Promotion Service.

Việc đưa các tham số nghiệp vụ và vận hành vào cơ sở dữ liệu giúp hệ thống linh hoạt hơn, giảm phụ thuộc vào mã nguồn, hỗ trợ thay đổi nhanh các chính sách khuyến mãi, tối ưu vận hành và đáp ứng tốt các yêu cầu triển khai trong môi trường Production.

---

**Kết thúc Phase 10**

# Promotion Service Database Design Specification

# Phase 11 - Bảng approval_histories

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | approval_histories |
| Vai trò | Governance / Audit |
| Độ ưu tiên | Rất cao |
| Được tạo khi | Có một nghiệp vụ yêu cầu phê duyệt |
| Chức năng | Lưu toàn bộ lịch sử phê duyệt trong Promotion Service |

---

# 2. Tóm tắt công dụng

Bảng **approval_histories** dùng để lưu toàn bộ lịch sử phê duyệt (Approval Workflow) của các nghiệp vụ trong Promotion Service.

Trong môi trường Production, rất nhiều thao tác không được phép thực hiện ngay mà phải trải qua quy trình phê duyệt.

Ví dụ

- Publish Campaign
- Kích hoạt Promotion
- Phát Voucher bồi thường
- Hủy Campaign
- Thay đổi Rule
- Thay đổi Configuration

Thay vì chỉ lưu trạng thái hiện tại, hệ thống cần lưu toàn bộ lịch sử để phục vụ:

- Audit
- Kiểm toán
- Điều tra sự cố
- Truy vết người thao tác
- Tuân thủ quy định nội bộ

---

# 3. Mục đích của bảng

Bảng này lưu

- Đối tượng cần phê duyệt
- Loại nghiệp vụ
- Người gửi yêu cầu
- Người phê duyệt
- Kết quả phê duyệt
- Lý do từ chối
- Thời điểm xử lý

Không lưu

- Dữ liệu Campaign
- Rule
- Voucher

Bảng này chỉ quản lý quy trình phê duyệt.

---

# 4. Khi nào dữ liệu được tạo

Approval History được tạo khi

- Campaign gửi duyệt
- Compensation gửi duyệt
- Configuration gửi duyệt
- Rule gửi duyệt
- Admin yêu cầu thay đổi dữ liệu quan trọng

Ví dụ

```
Marketing

↓

Submit Campaign

↓

Approval History

↓

Manager Review
```

---

# 5. Khi nào dữ liệu được cập nhật

Được cập nhật khi

- Approve
- Reject
- Withdraw
- Re-submit
- Escalate

Một Approval có thể trải qua nhiều bước trước khi hoàn tất.

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Admin Portal

↓

Manager

↓

Finance

↓

Audit

↓

Compliance

↓

Dashboard
```

---

# 7. Khi nào dữ liệu được xóa

Không Hard Delete.

Approval History là dữ liệu pháp lý và phải được lưu theo chính sách lưu trữ của doanh nghiệp.

---

# 8. Vai trò trong hệ thống

Approval History là trung tâm của quy trình kiểm soát (Governance).

Thông qua bảng này hệ thống biết

- Ai tạo yêu cầu.
- Ai phê duyệt.
- Ai từ chối.
- Phê duyệt lúc nào.
- Vì sao bị từ chối.

Đây là nền tảng của cơ chế Four Eyes Principle (4 mắt) trong doanh nghiệp.

---

# 9. Luồng nghiệp vụ

```
Marketing

↓

Create Campaign

↓

Submit Approval

↓

approval_histories

↓

Manager Review

↓

Approve / Reject

↓

Campaign Status Updated
```

---

# 10. Những bảng sử dụng approval_histories

| Bảng | Mục đích |
|-------|----------|
| promotion_campaigns | Publish Campaign |
| promotion_rules | Publish Rule |
| compensation_vouchers | Phê duyệt Voucher bồi thường |
| promotion_configurations | Phê duyệt thay đổi cấu hình |
| audit_logs | Ghi lịch sử |

---

# 11. approval_histories hỗ trợ bảng nào

Approval History hỗ trợ

- promotion_campaigns
- promotion_rules
- compensation_vouchers
- promotion_configurations

Đây là bảng dùng chung cho toàn bộ Workflow của Promotion Service.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

promotion_rules

compensation_vouchers

promotion_configurations

        │

        ▼

approval_histories

        │

        ▼

audit_logs
```

Approval History không phụ thuộc vào một bảng duy nhất mà sử dụng cơ chế tham chiếu theo loại nghiệp vụ.

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

---

## public_id

### Công dụng

UUID của Approval.

Được sử dụng trong API và Audit.

---

## resource_type

### Công dụng

Loại đối tượng cần phê duyệt.

Ví dụ

```
CAMPAIGN

RULE

COMPENSATION

CONFIGURATION
```

Giúp một bảng có thể quản lý nhiều loại nghiệp vụ.

---

## resource_public_id

### Công dụng

UUID của đối tượng cần phê duyệt.

Ví dụ

```
Campaign UUID

Voucher UUID
```

---

## approval_level

### Công dụng

Cấp phê duyệt.

Ví dụ

```
LEVEL_1

LEVEL_2

LEVEL_3
```

Hỗ trợ nhiều cấp phê duyệt.

---

## requested_by

### Công dụng

Public ID của người gửi yêu cầu.

---

## approver_public_id

### Công dụng

Public ID của người phê duyệt.

---

## approval_action

### Công dụng

Hành động được thực hiện.

Ví dụ

```
SUBMITTED

APPROVED

REJECTED

WITHDRAWN

ESCALATED

RESUBMITTED
```

---

## approval_status

### Công dụng

Trạng thái hiện tại.

Ví dụ

```
PENDING

APPROVED

REJECTED

CANCELLED
```

---

## comment

### Công dụng

Ý kiến của người phê duyệt.

Ví dụ

```
Thiếu ngân sách.

Sai thời gian Campaign.

Đã kiểm tra.
```

---

## approved_at

### Công dụng

Thời điểm phê duyệt.

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Department

Reason Code

Workflow Version

Escalation

Attachment
```

---

## created_at

Ngày gửi yêu cầu.

---

## created_by

Người tạo.

---

## updated_at

Ngày cập nhật.

---

## updated_by

Người cập nhật.

---

## deleted_at

Ngày Soft Delete.

---

## deleted_by

Người Soft Delete.

---

# 14. Index

Approval History được Index theo

- public_id
- resource_type
- resource_public_id
- approval_status
- approver_public_id
- requested_by
- approved_at

Các Index phục vụ

- Approval Queue
- Dashboard
- Audit
- Compliance
- Reporting

---

# 15. Business Rule

BR-072

Mỗi Approval phải tham chiếu đến đúng một đối tượng nghiệp vụ.

---

BR-073

Một Approval chỉ có thể ở một trạng thái tại một thời điểm.

---

BR-074

Đối tượng ở trạng thái PENDING không được Publish hoặc Activate.

---

BR-075

Approval bị REJECTED phải lưu lý do từ chối.

---

BR-076

Người tạo yêu cầu không được tự phê duyệt yêu cầu của chính mình (Four Eyes Principle), trừ khi có chính sách đặc biệt được cấu hình.

---

BR-077

Mọi hành động phê duyệt phải được ghi vào Audit Log.

---

BR-078

Approval đã APPROVED hoặc REJECTED không được chỉnh sửa trực tiếp; mọi thay đổi phải tạo một yêu cầu phê duyệt mới.

---

# 16. Ví dụ dữ liệu

| Resource | Request By | Approver | Action | Status |
|------------|------------|----------|---------|---------|
| Campaign Summer | Marketing | Manager | APPROVED | APPROVED |
| Compensation Voucher | CSKH | Operations Manager | REJECTED | REJECTED |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **approval_histories** là trung tâm quản lý quy trình phê duyệt của Promotion Service.

Bảng này giúp đảm bảo mọi thay đổi quan trọng đều được kiểm soát, có thể truy vết đầy đủ người thực hiện, người phê duyệt và lịch sử xử lý. Đây là thành phần quan trọng để đáp ứng yêu cầu quản trị doanh nghiệp, kiểm toán nội bộ, tuân thủ quy định và đảm bảo tính minh bạch trong toàn bộ vòng đời của các chương trình khuyến mãi.

---

**Kết thúc Phase 11**

# Promotion Service Database Design Specification

# Phase 12 - Bảng audit_logs

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | audit_logs |
| Vai trò | Audit / Compliance |
| Độ ưu tiên | Cực kỳ cao |
| Được tạo khi | Mọi thao tác quan trọng trong hệ thống |
| Chức năng | Lưu toàn bộ lịch sử thao tác của Promotion Service |

---

# 2. Tóm tắt công dụng

Bảng **audit_logs** dùng để lưu toàn bộ lịch sử hoạt động của Promotion Service.

Khác với Log của ứng dụng (Application Log), Audit Log là bằng chứng nghiệp vụ (Business Evidence).

Audit Log trả lời các câu hỏi:

- Ai thực hiện?
- Thực hiện lúc nào?
- Thực hiện thao tác gì?
- Thực hiện trên dữ liệu nào?
- Giá trị trước là gì?
- Giá trị sau là gì?
- Có thành công hay không?
- Thực hiện từ đâu?

Đây là bảng quan trọng phục vụ

- Kiểm toán
- Điều tra sự cố
- Bảo mật
- Truy vết
- Tuân thủ (Compliance)

---

# 3. Mục đích của bảng

Bảng này lưu

- Thao tác
- Người thao tác
- Đối tượng thao tác
- Giá trị trước
- Giá trị sau
- Địa chỉ IP
- Thiết bị
- Kết quả

Không lưu

- Business Data chính
- Coupon
- Voucher
- Campaign

Audit chỉ lưu lịch sử thay đổi.

---

# 4. Khi nào dữ liệu được tạo

Audit Log được tạo khi

- Tạo Campaign
- Cập nhật Campaign
- Xóa mềm Campaign
- Tạo Rule
- Publish Rule
- Tạo Coupon
- Sử dụng Coupon
- Tạo Voucher
- Sử dụng Voucher
- Reservation
- Compensation
- Approval
- Thay đổi Configuration
- Login Admin
- Thao tác API nội bộ

Ví dụ

```
Admin

↓

Update Campaign

↓

Audit Log
```

---

# 5. Khi nào dữ liệu được cập nhật

Thông thường Audit Log không cập nhật.

Sau khi ghi

↓

Immutable

Không chỉnh sửa.

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Admin

↓

Security Team

↓

Audit Team

↓

Compliance

↓

Finance

↓

Customer Support
```

---

# 7. Khi nào dữ liệu được xóa

Không Hard Delete.

Theo chính sách lưu trữ

Ví dụ

```
5 năm

7 năm

10 năm
```

Tùy quy định doanh nghiệp.

---

# 8. Vai trò trong hệ thống

Audit Log là "hộp đen" (Black Box) của Promotion Service.

Nếu xảy ra bất kỳ sự cố nào,

Audit Log phải trả lời được

- Ai làm?
- Làm gì?
- Khi nào?
- Trên dữ liệu nào?
- Giá trị trước và sau?

---

# 9. Luồng nghiệp vụ

```
Business Action

↓

Service Layer

↓

Audit Event

↓

audit_logs

↓

Dashboard

↓

Security

↓

Compliance
```

---

# 10. Những bảng sử dụng audit_logs

| Bảng | Mục đích |
|-------|----------|
| promotion_campaigns | Ghi thay đổi Campaign |
| promotion_rules | Ghi thay đổi Rule |
| coupons | Ghi thay đổi Coupon |
| vouchers | Ghi thay đổi Voucher |
| promotion_reservations | Ghi Reservation |
| compensation_vouchers | Ghi bồi thường |
| promotion_configurations | Ghi thay đổi cấu hình |
| approval_histories | Ghi phê duyệt |

---

# 11. audit_logs hỗ trợ bảng nào

Audit Log hỗ trợ

- Security
- Compliance
- Dashboard
- Monitoring
- Investigation
- Customer Support

Audit Log được sử dụng bởi gần như mọi module trong hệ thống.

---

# 12. Quan hệ với các bảng

```
promotion_campaigns

promotion_rules

coupons

vouchers

promotion_reservations

compensation_vouchers

promotion_configurations

approval_histories

          │

          ▼

      audit_logs
```

Audit Log là bảng dùng chung cho toàn bộ Promotion Service và thường không sử dụng khóa ngoại cứng để tránh ảnh hưởng hiệu năng và giữ khả năng lưu vết ngay cả khi dữ liệu nghiệp vụ đã bị xóa mềm.

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

---

## public_id

### Công dụng

UUID của Audit Log.

Được sử dụng trong API và Dashboard.

---

## resource_type

### Công dụng

Loại dữ liệu bị tác động.

Ví dụ

```
CAMPAIGN

RULE

COUPON

VOUCHER

CONFIGURATION

APPROVAL
```

---

## resource_public_id

### Công dụng

UUID của đối tượng bị tác động.

Cho phép truy vết đến bản ghi nghiệp vụ tương ứng.

---

## action

### Công dụng

Loại thao tác.

Ví dụ

```
CREATE

UPDATE

DELETE

ACTIVATE

DEACTIVATE

APPROVE

REJECT

RESERVE

REDEEM
```

---

## actor_public_id

### Công dụng

Public ID của người hoặc hệ thống thực hiện thao tác.

Có thể là

- Admin
- Marketing
- Finance
- Scheduler
- Internal Service

---

## actor_type

### Công dụng

Loại tác nhân.

Ví dụ

```
USER

ADMIN

SYSTEM

SCHEDULER

SERVICE
```

---

## before_data_json

### Công dụng

Giá trị trước khi thay đổi.

Ví dụ

```
Status = DRAFT
```

---

## after_data_json

### Công dụng

Giá trị sau khi thay đổi.

Ví dụ

```
Status = ACTIVE
```

---

## ip_address

### Công dụng

Địa chỉ IP của tác nhân.

Phục vụ

- Security
- Điều tra
- Chống gian lận

---

## user_agent

### Công dụng

Thông tin trình duyệt hoặc ứng dụng.

Ví dụ

```
Chrome

Firefox

Internal Service

Mobile App
```

---

## request_id

### Công dụng

Request ID hoặc Correlation ID.

Cho phép truy vết xuyên suốt giữa các microservices.

---

## trace_id

### Công dụng

Distributed Trace ID.

Sử dụng với

- OpenTelemetry
- Zipkin
- Jaeger

---

## success

### Công dụng

Kết quả thao tác.

Ví dụ

```
true

false
```

---

## error_message

### Công dụng

Thông tin lỗi nếu thao tác thất bại.

---

## occurred_at

### Công dụng

Thời điểm thao tác xảy ra.

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Endpoint

HTTP Method

Service Name

Hostname

Remark
```

---

## created_at

Ngày ghi Audit.

Thông thường bằng `occurred_at`.

---

## created_by

Thông thường là

```
SYSTEM
```

---

# 14. Index

Audit Log được Index theo

- public_id
- resource_type
- resource_public_id
- action
- actor_public_id
- occurred_at
- request_id
- trace_id

Các Index phục vụ

- Audit Dashboard
- Security Investigation
- Compliance
- Distributed Tracing
- Customer Support

---

# 15. Business Rule

BR-079

Mọi thao tác thay đổi dữ liệu quan trọng đều phải ghi Audit Log.

---

BR-080

Audit Log là dữ liệu bất biến (Immutable), không được chỉnh sửa sau khi ghi.

---

BR-081

Audit Log phải lưu đầy đủ thời gian, tác nhân và hành động.

---

BR-082

Audit Log phải hỗ trợ truy vết xuyên suốt giữa các microservices thông qua `request_id` hoặc `trace_id`.

---

BR-083

Thông tin nhạy cảm (ví dụ: khóa bí mật, mật khẩu, token) không được ghi trực tiếp vào Audit Log.

---

BR-084

Các thao tác thất bại cũng phải được ghi nhận để phục vụ điều tra và phân tích.

---

BR-085

Audit Log phải được lưu giữ theo chính sách lưu trữ và tuân thủ quy định pháp lý của doanh nghiệp.

---

# 16. Ví dụ dữ liệu

| Resource | Action | Actor | Success | Occurred At |
|-----------|--------|-------|----------|-------------|
| Campaign Summer | CREATE | Admin | true | 2026-08-01 08:00 |
| Voucher VIP50 | REDEEM | Customer | true | 2026-08-01 10:35 |
| Promotion Config | UPDATE | DevOps | false | 2026-08-03 09:20 |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **audit_logs** là nền tảng cho khả năng truy vết và kiểm toán của Promotion Service.

Đây là nơi ghi nhận toàn bộ các hành động quan trọng xảy ra trong hệ thống, giúp doanh nghiệp điều tra sự cố, đáp ứng yêu cầu kiểm toán, bảo mật và tuân thủ. Audit Log cũng là thành phần quan trọng trong kiến trúc Microservices hiện đại khi kết hợp với Correlation ID và Distributed Tracing để theo dõi toàn bộ vòng đời của một yêu cầu xuyên suốt nhiều dịch vụ.

---

**Kết thúc Phase 12**

# Promotion Service Database Design Specification

# Phase 13 - Bảng outbox_events

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | outbox_events |
| Vai trò | Integration / Messaging |
| Độ ưu tiên | Cực kỳ cao |
| Được tạo khi | Có Business Event cần publish |
| Chức năng | Đảm bảo gửi Event sang Kafka (hoặc Message Broker) một cách đáng tin cậy |

---

# 2. Tóm tắt công dụng

Bảng **outbox_events** được sử dụng để triển khai **Transactional Outbox Pattern** trong kiến trúc Microservices.

Mục tiêu là đảm bảo:

- Không mất Event.
- Không publish Event trước khi Transaction Commit.
- Không xảy ra tình trạng Database thành công nhưng Kafka thất bại.
- Hỗ trợ Retry khi Message Broker gặp sự cố.

Trong Production, gần như mọi Microservice đều cần Outbox Pattern thay vì publish Kafka trực tiếp trong Transaction.

Ví dụ

```
Create Voucher

↓

Database Commit

↓

Create Outbox Event

↓

Background Publisher

↓

Kafka

↓

Notification Service

↓

Analytics Service
```

---

# 3. Mục đích của bảng

Bảng này lưu

- Business Event
- Aggregate
- Payload
- Trạng thái Publish
- Retry Count
- Error
- Thời gian Publish

Không lưu

- Business Data
- Coupon
- Voucher
- Campaign

Chỉ lưu Event cần gửi.

---

# 4. Khi nào dữ liệu được tạo

Outbox Event được tạo khi

- Campaign được Publish
- Rule được Publish
- Coupon được tạo
- Voucher được tạo
- Coupon được sử dụng
- Voucher được sử dụng
- Compensation hoàn tất
- Approval hoàn tất
- Reservation hết hạn

Ví dụ

```
Create Coupon

↓

Insert Coupon

↓

Insert Outbox Event

↓

Commit

↓

Publisher

↓

Kafka
```

---

# 5. Khi nào dữ liệu được cập nhật

Được cập nhật khi

- Publish thành công
- Retry
- Publish thất bại
- Dead Letter
- Scheduler Retry

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Outbox Publisher

↓

Scheduler

↓

Kafka Publisher

↓

Monitoring

↓

Dashboard
```

Thông thường Business API không đọc bảng này.

---

# 7. Khi nào dữ liệu được xóa

Thông thường không xóa ngay.

Sau khi Publish thành công

↓

Lưu thêm một khoảng thời gian

↓

Archive

↓

Hoặc Cleanup Job

---

# 8. Vai trò trong hệ thống

Outbox Event là cầu nối giữa

```
Database

↓

Kafka

↓

Microservices
```

Đây là thành phần đảm bảo tính nhất quán dữ liệu (Eventually Consistent) trong hệ thống phân tán.

---

# 9. Luồng nghiệp vụ

```
Business Transaction

↓

Insert Business Data

↓

Insert Outbox Event

↓

Commit

↓

Background Publisher

↓

Kafka

↓

Consumer

↓

Mark Published
```

Nếu Publish lỗi

```
Retry

↓

Retry Count +1

↓

Publish lại

↓

Dead Letter
```

---

# 10. Những bảng sử dụng outbox_events

| Bảng | Mục đích |
|-------|----------|
| promotion_campaigns | Publish Campaign Event |
| promotion_rules | Publish Rule Event |
| coupons | Coupon Created |
| coupon_redemptions | Coupon Redeemed |
| vouchers | Voucher Created |
| voucher_redemptions | Voucher Redeemed |
| promotion_reservations | Reservation Event |
| compensation_vouchers | Compensation Event |
| approval_histories | Approval Event |

---

# 11. outbox_events hỗ trợ bảng nào

Outbox Event hỗ trợ

- Kafka
- Notification Service
- Booking Service
- Payment Service
- Analytics Service
- Audit Service
- Recommendation Service

Đây là trung tâm giao tiếp giữa Promotion Service và các Microservices khác.

---

# 12. Quan hệ với các bảng

```
Business Tables

│

├──────── Campaign

├──────── Rule

├──────── Coupon

├──────── Voucher

├──────── Reservation

├──────── Approval

└──────── Compensation

        │

        ▼

outbox_events

        │

        ▼

Kafka

        │

        ▼

Notification

Analytics

Booking

Payment

Recommendation
```

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

---

## public_id

### Công dụng

UUID của Event.

Được sử dụng trong

- Kafka
- Retry
- Dashboard

---

## aggregate_type

### Công dụng

Loại Aggregate tạo Event.

Ví dụ

```
CAMPAIGN

RULE

COUPON

VOUCHER

RESERVATION
```

---

## aggregate_public_id

### Công dụng

UUID của Aggregate.

Ví dụ

```
Voucher UUID

Coupon UUID

Campaign UUID
```

---

## event_type

### Công dụng

Tên Business Event.

Ví dụ

```
CAMPAIGN_CREATED

CAMPAIGN_ACTIVATED

RULE_UPDATED

COUPON_CREATED

COUPON_REDEEMED

VOUCHER_CREATED

VOUCHER_REDEEMED
```

---

## payload_json

### Công dụng

Toàn bộ dữ liệu gửi sang Kafka.

Ví dụ

```json
{
  "voucherId": "...",
  "userId": "...",
  "amount": 50000
}
```

Payload nên chứa đầy đủ dữ liệu mà Consumer cần để xử lý, tránh phải gọi ngược lại Promotion Service nếu không cần thiết.

---

## event_version

### Công dụng

Phiên bản của Event.

Ví dụ

```
v1

v2

v3
```

Giúp Consumer xử lý tương thích khi Event thay đổi theo thời gian.

---

## topic_name

### Công dụng

Kafka Topic.

Ví dụ

```
promotion-events

voucher-events

coupon-events
```

---

## event_key

### Công dụng

Kafka Partition Key.

Ví dụ

```
Voucher UUID

Coupon UUID

Campaign UUID
```

Giúp đảm bảo thứ tự xử lý đối với cùng một Aggregate.

---

## publish_status

### Công dụng

Trạng thái Publish.

Ví dụ

```
PENDING

PUBLISHED

FAILED

DEAD_LETTER
```

---

## retry_count

### Công dụng

Số lần Retry.

Ví dụ

```
0

1

2

3
```

---

## max_retry

### Công dụng

Giới hạn Retry.

Ví dụ

```
5
```

Khi vượt quá giới hạn

↓

Dead Letter.

---

## last_retry_at

### Công dụng

Lần Retry gần nhất.

---

## published_at

### Công dụng

Thời điểm Publish thành công.

---

## error_message

### Công dụng

Lỗi Publish gần nhất.

Ví dụ

```
Kafka Timeout

Broker Unavailable

Serialization Error
```

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
Partition

Offset

Producer

Cluster

Headers
```

---

## created_at

Ngày tạo Event.

Thông thường bằng thời điểm Commit Transaction.

---

## created_by

Thông thường là

```
SYSTEM
```

---

# 14. Index

Outbox Event được Index theo

- public_id
- aggregate_type
- aggregate_public_id
- event_type
- publish_status
- created_at
- published_at
- retry_count

Các Index phục vụ

- Outbox Publisher
- Retry Job
- Monitoring
- Dashboard
- Kafka Integration

---

# 15. Business Rule

BR-086

Một Business Transaction phải ghi Business Data và Outbox Event trong cùng một Transaction.

---

BR-087

Outbox Event chỉ được Publish sau khi Transaction đã Commit thành công.

---

BR-088

Event Publish thất bại phải được Retry theo chính sách cấu hình.

---

BR-089

Khi vượt quá `max_retry`, Event phải chuyển sang trạng thái `DEAD_LETTER`.

---

BR-090

Mỗi Event phải có `aggregate_type`, `aggregate_public_id` và `event_type`.

---

BR-091

Payload phải đủ dữ liệu để Consumer xử lý, hạn chế gọi ngược lại Promotion Service.

---

BR-092

Outbox Publisher phải bảo đảm tính **idempotent**, tránh publish trùng cùng một Event nhiều lần.

---

# 16. Ví dụ dữ liệu

| Event Type | Aggregate | Status | Retry | Topic |
|------------|-----------|---------|--------|--------|
| COUPON_CREATED | Coupon | PUBLISHED | 0 | promotion-events |
| VOUCHER_REDEEMED | Voucher | PUBLISHED | 0 | voucher-events |
| CAMPAIGN_ACTIVATED | Campaign | DEAD_LETTER | 5 | promotion-events |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **outbox_events** là nền tảng cho việc tích hợp giữa Promotion Service và các Microservices khác.

Việc áp dụng **Transactional Outbox Pattern** giúp đảm bảo dữ liệu trong cơ sở dữ liệu và các sự kiện gửi qua Kafka luôn nhất quán, ngay cả khi xảy ra lỗi mạng hoặc sự cố Message Broker. Đây là một thành phần quan trọng trong kiến trúc Microservices hiện đại, giúp tăng độ tin cậy, khả năng mở rộng và khả năng phục hồi của toàn bộ hệ thống.

---

**Kết thúc Phase 13**

# Promotion Service Database Design Specification

# Phase 14 - Bảng promotion_idempotency_keys

---

# 1. Thông tin bảng

| Thuộc tính | Giá trị |
|------------|----------|
| Tên bảng | promotion_idempotency_keys |
| Vai trò | Infrastructure / Reliability |
| Độ ưu tiên | Cực kỳ cao |
| Được tạo khi | Client gửi Idempotency-Key hoặc Internal Request |
| Chức năng | Chống xử lý trùng lặp (Duplicate Request) |

---

# 2. Tóm tắt công dụng

Bảng **promotion_idempotency_keys** dùng để đảm bảo một yêu cầu chỉ được xử lý **một lần duy nhất**, ngay cả khi Client hoặc Service gửi yêu cầu nhiều lần.

Đây là thành phần rất quan trọng trong hệ thống Microservices.

Ví dụ

```
User

↓

Apply Coupon

↓

Timeout

↓

User bấm lại

↓

Promotion Service

↓

Không tạo Promotion lần thứ hai
```

Nếu không có Idempotency

↓

Có thể xảy ra

- Trừ Coupon hai lần
- Tạo Voucher hai lần
- Redemption hai lần
- Reservation hai lần
- Publish Kafka hai lần

---

# 3. Mục đích của bảng

Bảng này lưu

- Idempotency Key
- Request Hash
- Response
- Trạng thái xử lý
- Thời gian hết hạn
- Kết quả xử lý

Không lưu

- Coupon
- Voucher
- Campaign

Đây là bảng hạ tầng phục vụ tính nhất quán dữ liệu.

---

# 4. Khi nào dữ liệu được tạo

Được tạo khi

- Client gửi Header

```
Idempotency-Key
```

hoặc

- Internal Service gửi Request có Idempotency Key.

Ví dụ

```
POST

/api/promotions/apply

↓

Idempotency-Key

↓

Create Record
```

---

# 5. Khi nào dữ liệu được cập nhật

Được cập nhật khi

- Request xử lý thành công
- Request thất bại
- Retry
- Lưu Response
- Hết thời gian hiệu lực

---

# 6. Khi nào dữ liệu được đọc

Được đọc bởi

```
Promotion API

↓

Promotion Engine

↓

Internal API

↓

Retry Service

↓

Scheduler
```

Mỗi Request quan trọng đều cần kiểm tra bảng này trước khi xử lý.

---

# 7. Khi nào dữ liệu được xóa

Thông thường không xóa ngay.

Sau khi

```
Expired

↓

Cleanup Job

↓

Archive

↓

Delete
```

Thời gian lưu phụ thuộc chính sách hệ thống.

Ví dụ

```
24 giờ

48 giờ

7 ngày
```

---

# 8. Vai trò trong hệ thống

Idempotency Key giúp hệ thống xử lý

```
Exactly Once Processing
```

thay vì

```
At Least Once
```

Đây là thành phần cực kỳ quan trọng của Production System.

---

# 9. Luồng nghiệp vụ

```
Client

↓

POST Request

↓

Idempotency Filter

↓

promotion_idempotency_keys

↓

Không tồn tại

↓

Business Logic

↓

Save Response

↓

Return Client
```

Nếu Request trùng

```
Client

↓

POST

↓

Idempotency Filter

↓

Record Exists

↓

Return Previous Response
```

Không thực hiện Business Logic lần thứ hai.

---

# 10. Những bảng sử dụng promotion_idempotency_keys

| Bảng | Mục đích |
|-------|----------|
| promotion_reservations | Tránh Reservation trùng |
| coupon_redemptions | Tránh Redemption trùng |
| voucher_redemptions | Tránh Redemption trùng |
| outbox_events | Tránh Publish Event trùng |
| audit_logs | Ghi nhận Retry và Duplicate Request |

---

# 11. promotion_idempotency_keys hỗ trợ bảng nào

Idempotency hỗ trợ

- Promotion API
- Booking Service
- Payment Service
- Kafka Publisher
- Retry Job
- Scheduler

Là lớp bảo vệ chống Duplicate Request cho toàn bộ Promotion Service.

---

# 12. Quan hệ với các bảng

```
Client

      │

      ▼

Promotion API

      │

      ▼

promotion_idempotency_keys

      │

      ▼

Business Tables

      │

      ▼

Outbox Events
```

Bảng này hoạt động ở tầng hạ tầng (Infrastructure Layer), không có khóa ngoại trực tiếp tới các bảng nghiệp vụ.

---

# 13. Giải thích từng trường

## id

### Công dụng

Khóa chính nội bộ.

---

## public_id

### Công dụng

UUID của Idempotency Record.

---

## idempotency_key

### Công dụng

Khóa duy nhất do Client hoặc Internal Service gửi.

Ví dụ

```
c82a9a9b-48f6-45cb-b3d5-1d4d07e6e3a4
```

Không được phép trùng trong thời gian còn hiệu lực.

---

## request_hash

### Công dụng

Giá trị Hash của Request Body.

Ví dụ

```
SHA-256(Request Body)
```

Giúp phát hiện trường hợp

```
Cùng Idempotency Key

↓

Khác Request
```

Đây là hành vi không hợp lệ.

---

## http_method

### Công dụng

Phương thức HTTP.

Ví dụ

```
POST

PUT

PATCH
```

---

## request_uri

### Công dụng

API được gọi.

Ví dụ

```
/api/promotions/apply
```

---

## requester_public_id

### Công dụng

Public ID của người hoặc hệ thống gửi Request.

---

## processing_status

### Công dụng

Trạng thái xử lý.

Ví dụ

```
PROCESSING

SUCCESS

FAILED

EXPIRED
```

---

## response_status

### Công dụng

HTTP Status đã trả về.

Ví dụ

```
200

201

400

409

500
```

---

## response_body_json

### Công dụng

Response đã trả về cho Client.

Nếu Request được gửi lại

↓

Promotion Service trả Response này thay vì xử lý lại.

---

## expires_at

### Công dụng

Thời điểm Idempotency Record hết hiệu lực.

Sau thời điểm này

↓

Request được phép xử lý lại.

---

## metadata_json

### Công dụng

Thông tin mở rộng.

Ví dụ

```
IP

Device

Platform

Client Version

Correlation ID
```

---

## created_at

Ngày tạo.

---

## updated_at

Ngày cập nhật.

Thông thường khi Request hoàn tất.

---

# 14. Index

Idempotency được Index theo

- public_id
- idempotency_key (Unique)
- requester_public_id
- processing_status
- expires_at
- created_at

Các Index phục vụ

- API Gateway
- Retry
- Duplicate Detection
- Cleanup Job

---

# 15. Business Rule

BR-093

Idempotency Key phải là duy nhất trong thời gian còn hiệu lực.

---

BR-094

Một Idempotency Key chỉ được xử lý một lần.

---

BR-095

Nếu Request Hash khác nhau nhưng cùng Idempotency Key thì hệ thống phải từ chối yêu cầu.

---

BR-096

Response của Request đầu tiên phải được lưu để trả về cho các Request trùng lặp.

---

BR-097

Idempotency Record hết hạn phải được Cleanup theo chính sách lưu trữ.

---

BR-098

Các API tạo hoặc thay đổi dữ liệu quan trọng (POST, PUT, PATCH) nên hỗ trợ Idempotency.

---

BR-099

Idempotency Key không được tái sử dụng cho một Request khác trước khi hết thời gian hiệu lực.

---

# 16. Ví dụ dữ liệu

| Idempotency Key | API | Status | Response | Expired |
|-----------------|-----|--------|----------|----------|
| 9a4f...01 | POST /apply | SUCCESS | 200 | 2026-08-01 10:30 |
| b71d...44 | POST /redeem | PROCESSING | - | 2026-08-01 11:00 |
| e92c...98 | POST /voucher | FAILED | 500 | 2026-08-01 11:15 |

---

# 17. Vai trò trong toàn bộ hệ thống

Bảng **promotion_idempotency_keys** là lớp bảo vệ giúp Promotion Service xử lý các yêu cầu một cách an toàn trong môi trường phân tán.

Bằng cách lưu trữ khóa định danh, nội dung yêu cầu và kết quả xử lý, hệ thống có thể ngăn chặn các yêu cầu trùng lặp do người dùng gửi lại, do mạng không ổn định hoặc do cơ chế Retry của các Microservices. Đây là thành phần quan trọng để đảm bảo tính nhất quán dữ liệu, tránh phát sinh giao dịch trùng và nâng cao độ tin cậy của toàn bộ hệ thống.

---

**Kết thúc Phase 14**

# Promotion Service Database Design Specification

# Phase 15 - Tổng quan quan hệ dữ liệu (ERD), Data Flow và Dependency

---

# 1. Mục tiêu

Phase này mô tả kiến trúc dữ liệu tổng thể của Promotion Service.

Bao gồm

- Quan hệ giữa tất cả các bảng
- Luồng dữ liệu
- Dependency
- Business Flow
- Module Interaction
- Database Layer
- Service Layer
- External Services

Đây là bức tranh tổng thể của toàn bộ Database Design.

---

# 2. Phân loại bảng theo chức năng

Promotion Service gồm 16 bảng, chia thành 7 nhóm.

## 2.1 Master Data

```
promotion_configurations
```

Đặc điểm

- Ít thay đổi
- Dữ liệu nền
- Được tham chiếu nhiều

---

## 2.2 Core Business

```
promotion_campaigns

promotion_rules

coupons

vouchers
```

Đây là trung tâm của Promotion Service.

---

## 2.3 Runtime Business

```
promotion_reservations

coupon_redemptions

voucher_redemptions
```

Đây là dữ liệu phát sinh theo thời gian thực.

---

## 2.4 Financial

```
compensation_vouchers
```

Liên quan đến

- tài chính
- chi phí
- bồi thường

---

## 2.5 Governance

```
approval_histories

audit_logs
```

Quản trị doanh nghiệp.

---

## 2.6 Integration

```
outbox_events

promotion_integration_events
```

Kết nối Kafka.

---

## 2.7 Infrastructure

```
promotion_idempotency_keys

promotion_scheduler_job_executions

promotion_scheduler_locks
```

Chống Duplicate Request và điều phối scheduler.

---

# 3. Quan hệ giữa các bảng

```
                  promotion_campaigns
                            │
                            │
                            ▼
                    promotion_rules
                    ┌─────────┴──────────┐
                    │                    │
                    ▼                    ▼
                coupons             vouchers
                    │                    │
                    │                    │
                    ▼                    ▼
         promotion_reservations
               ┌────────┴─────────┐
               ▼                  ▼
 coupon_redemptions      voucher_redemptions
```

---

# 4. Quan hệ hỗ trợ

```
promotion_configurations

↓

Promotion Engine

↓

Reservation

↓

Rule Engine

↓

Scheduler

↓

Kafka
```

Configuration được sử dụng xuyên suốt hệ thống.

---

# 5. Quan hệ Governance

```
promotion_campaigns

promotion_rules

compensation_vouchers

promotion_configurations

↓

approval_histories

↓

audit_logs
```

Approval và Audit là hai lớp quản trị độc lập.

---

# 6. Quan hệ Integration

```
Business Tables

↓

outbox_events

↓

Kafka

↓

Notification

↓

Booking

↓

Payment

↓

Analytics
```

Outbox là cầu nối giữa Database và Kafka.

---

# 7. Quan hệ Infrastructure

```
Client

↓

API Gateway

↓

promotion_idempotency_keys

↓

Business Logic

↓

Database
```

Idempotency được kiểm tra trước khi xử lý Business Logic.

---

# 8. Dependency giữa các bảng

| Bảng | Phụ thuộc |
|--------|-----------|
| promotion_campaigns | Không phụ thuộc |
| promotion_rules | promotion_campaigns |
| coupons | promotion_rules |
| vouchers | promotion_rules |
| promotion_reservations | coupons, vouchers |
| coupon_redemptions | promotion_reservations |
| voucher_redemptions | promotion_reservations |
| compensation_vouchers | vouchers |
| approval_histories | Campaign, Rule, Compensation |
| audit_logs | Toàn bộ bảng |
| outbox_events | Toàn bộ bảng |
| promotion_configurations | Không phụ thuộc |
| promotion_idempotency_keys | Không phụ thuộc |

---

# 9. Data Flow

## Bước 1

Marketing tạo Campaign

```
Admin

↓

promotion_campaigns
```

---

## Bước 2

Marketing tạo Rule

```
Campaign

↓

promotion_rules
```

---

## Bước 3

Rule sinh Coupon hoặc Voucher

```
Rule

↓

Coupons

Voucher
```

---

## Bước 4

Customer Booking

```
Booking Service

↓

Promotion Engine

↓

Reservation
```

---

## Bước 5

Reservation thành công

```
Promotion Reservation

↓

Payment
```

---

## Bước 6

Payment thành công

```
Coupon Redemption

hoặc

Voucher Redemption
```

---

## Bước 7

Analytics

```
Redemption

↓

Dashboard

↓

BI
```

---

# 10. Event Flow

```
Business Transaction

↓

Insert Database

↓

Insert Outbox

↓

Commit

↓

Kafka

↓

Consumer
```

Đây là Transactional Outbox Pattern.

---

# 11. Approval Flow

```
Marketing

↓

Submit

↓

Approval

↓

Approve

↓

Publish Campaign
```

Nếu Reject

↓

Quay lại Marketing.

---

# 12. Compensation Flow

```
Payment Error

↓

Compensation

↓

Voucher

↓

Wallet

↓

Voucher Redemption
```

---

# 13. Reservation Flow

```
Coupon

↓

Reservation

↓

Payment

↓

Release
```

Nếu Timeout

↓

Release Reservation.

---

# 14. Audit Flow

```
Business Action

↓

Audit

↓

Dashboard

↓

Compliance
```

---

# 15. Retry Flow

```
Kafka Error

↓

Retry

↓

Retry

↓

Retry

↓

Dead Letter
```

---

# 16. Idempotency Flow

```
Client

↓

POST

↓

Idempotency

↓

Exists ?

YES

↓

Return Response

NO

↓

Business Logic
```

---

# 17. Kiến trúc phân tầng

```
REST API

↓

Application Layer

↓

Promotion Engine

↓

Repository

↓

Database
```

---

# 18. Quan hệ với các Microservices

```
Promotion Service

│

├──────── Booking Service

├──────── Payment Service

├──────── User Service

├──────── Notification Service

├──────── Auth Service

├──────── Analytics Service

└──────── Gateway
```

---

# 19. Luồng Kafka

```
Promotion

↓

Kafka

↓

Notification

↓

Analytics

↓

Recommendation

↓

Data Warehouse
```

---

# 20. Database Access Pattern

```
API

↓

Service

↓

Repository

↓

MySQL

↓

Redis

↓

Kafka
```

---

# 21. Dữ liệu có tần suất đọc cao

| Bảng | Mức độ |
|--------|---------|
| promotion_configurations | Rất cao |
| promotion_rules | Rất cao |
| coupons | Rất cao |
| vouchers | Rất cao |
| promotion_reservations | Cực cao |

---

# 22. Dữ liệu có tần suất ghi cao

| Bảng | Mức độ |
|--------|---------|
| promotion_reservations | Cực cao |
| coupon_redemptions | Cao |
| voucher_redemptions | Cao |
| outbox_events | Cực cao |
| audit_logs | Cực cao |

---

# 23. Bảng quan trọng nhất

Theo nghiệp vụ Production

| Xếp hạng | Bảng |
|----------|------|
| 1 | promotion_rules |
| 2 | promotion_reservations |
| 3 | outbox_events |
| 4 | promotion_campaigns |
| 5 | coupons |
| 6 | vouchers |
| 7 | audit_logs |
| 8 | coupon_redemptions |
| 9 | voucher_redemptions |
| 10 | promotion_idempotency_keys |

---

# 24. Các Design Pattern được sử dụng

| Pattern | Mục đích |
|----------|----------|
| Transactional Outbox | Đảm bảo publish Event sau Commit |
| Idempotency | Chống xử lý trùng lặp |
| Optimistic Locking | Chống ghi đè dữ liệu |
| Soft Delete | Bảo toàn dữ liệu lịch sử |
| Audit Trail | Truy vết thay đổi |
| Event-Driven Architecture | Giao tiếp giữa các Microservices |
| Retry Pattern | Xử lý lỗi tạm thời |
| Scheduler Pattern | Tự động hóa tác vụ định kỳ |
| Saga Pattern (phối hợp với Booking/Payment) | Điều phối giao dịch phân tán |
| Cache-Aside Pattern | Tăng hiệu năng đọc cấu hình và dữ liệu ít thay đổi |

---

# 25. Tổng kết kiến trúc

Promotion Service được thiết kế theo kiến trúc **Production-Ready Microservices**, với các đặc điểm:

- Phân tách rõ dữ liệu nghiệp vụ và dữ liệu hạ tầng.
- Hỗ trợ Event-Driven Architecture thông qua Kafka và Transactional Outbox.
- Đảm bảo tính nhất quán bằng Reservation, Idempotency và các cơ chế khóa phù hợp.
- Có đầy đủ khả năng Audit, Approval và Governance cho môi trường doanh nghiệp.
- Hỗ trợ mở rộng theo chiều ngang (Horizontal Scaling) và xử lý đồng thời với lưu lượng lớn.
- Tối ưu cho khả năng vận hành, giám sát, theo dõi chi phí khuyến mãi và truy vết trong môi trường Production.

---

**Kết thúc Phase 15**

# Promotion Service Database Design Specification

# Phase 16 - Tổng Business Flow của Promotion Service

---

# 1. Mục tiêu

Phase này mô tả toàn bộ vòng đời của một chương trình khuyến mãi trong hệ thống, từ lúc được tạo bởi Marketing cho đến khi hoàn tất ghi nhận sử dụng, phân tích và lưu trữ.

Đây là góc nhìn tổng thể về nghiệp vụ (Business View), không tập trung vào thiết kế bảng dữ liệu mà tập trung vào cách các thành phần phối hợp với nhau để vận hành Promotion Service trong môi trường Production.

---

# 2. Vòng đời của Promotion

```
Marketing

↓

Campaign

↓

Rule

↓

Coupon / Voucher

↓

Reservation

↓

Payment

↓

Redemption

↓

Analytics

↓

Archive
```

---

# 3. Business Flow 01 - Tạo Campaign

### Mục tiêu

Marketing tạo một chương trình khuyến mãi mới.

### Luồng xử lý

```
Marketing

↓

Create Campaign

↓

promotion_campaigns

↓

Audit Log

↓

Approval Workflow
```

### Kết quả

- Campaign được tạo.
- Trạng thái ban đầu là `DRAFT`.
- Chưa áp dụng cho khách hàng.

---

# 4. Business Flow 02 - Tạo Promotion Rule

### Mục tiêu

Định nghĩa điều kiện và hành động của chương trình khuyến mãi.

### Luồng xử lý

```
Campaign

↓

Create Rule

↓

promotion_rules

↓

Validation

↓

Audit
```

### Ví dụ

Điều kiện:

- Mua từ 2 vé.
- Thanh toán bằng MoMo.
- Suất chiếu sau 18:00.

Hành động:

- Giảm 50.000 VNĐ.

---

# 5. Business Flow 03 - Phê duyệt

```
Marketing

↓

Submit

↓

approval_histories

↓

Manager

↓

Approve

↓

Campaign ACTIVE
```

Nếu bị từ chối:

```
Reject

↓

Campaign DRAFT

↓

Marketing chỉnh sửa

↓

Submit lại
```

---

# 6. Business Flow 04 - Đồng bộ Cache

Sau khi Campaign hoặc Rule được kích hoạt:

```
Campaign ACTIVE

↓

Publish Event

↓

Kafka

↓

Promotion Engine

↓

Refresh Cache
```

Điều này giúp Rule mới có hiệu lực mà không cần khởi động lại dịch vụ.

---

# 7. Business Flow 05 - Khách hàng đặt vé

```
Customer

↓

Booking Service

↓

Promotion Service

↓

Promotion Engine
```

Promotion Engine thực hiện:

- Tìm Campaign đang hoạt động.
- Đánh giá Rule.
- Xác định Coupon hoặc Voucher hợp lệ.
- Tính toán mức ưu đãi.

---

# 8. Business Flow 06 - Tạo Reservation

```
Promotion Engine

↓

promotion_reservations

↓

Lock Coupon/Voucher
```

Mục tiêu:

- Ngăn nhiều giao dịch sử dụng cùng một quyền lợi.
- Đảm bảo tính nhất quán khi thanh toán đồng thời.

---

# 9. Business Flow 07 - Thanh toán

```
Booking

↓

Payment

↓

Success ?
```

### Nếu thất bại

```
Release Reservation

↓

Promotion Available
```

### Nếu thành công

```
Create Redemption
```

---

# 10. Business Flow 08 - Redemption

```
Coupon

↓

coupon_redemptions
```

hoặc

```
Voucher

↓

voucher_redemptions
```

Đồng thời:

- Tăng `usage_count`.
- Cập nhật trạng thái nếu đạt giới hạn sử dụng.
- Ghi Audit.
- Sinh Outbox Event.

---

# 11. Business Flow 09 - Kafka Event

```
Business Transaction

↓

outbox_events

↓

Kafka

↓

Notification

↓

Analytics

↓

Recommendation

↓

CRM
```

Ví dụ Event:

- CampaignActivated
- CouponCreated
- VoucherRedeemed

---

# 12. Business Flow 10 - Compensation

Nếu giao dịch gặp lỗi sau khi thanh toán:

```
Payment Success

↓

Booking Failed

↓

Compensation

↓

Voucher

↓

Customer Wallet
```

Toàn bộ quá trình được lưu tại:

- compensation_vouchers
- vouchers
- audit_logs

---

# 13. Business Flow 11 - Analytics

Từ dữ liệu Redemption và các event nghiệp vụ:

```
Kafka

↓

Analytics Service

↓

Dashboard

↓

BI

↓

Data Warehouse
```

Các chỉ số thường được thống kê:

- Doanh thu từ khuyến mãi.
- Chi phí khuyến mãi.
- Tỷ lệ sử dụng Coupon.
- Tỷ lệ sử dụng Voucher.
- Hiệu quả Campaign.
- Hiệu quả theo khu vực.

---

# 14. Business Flow 12 - Audit

Mọi thao tác quan trọng đều tạo Audit.

```
Business Action

↓

audit_logs

↓

Compliance

↓

Security

↓

Investigation
```

---

# 15. Business Flow 13 - Retry

Nếu Kafka lỗi

```
Pending

↓

Retry

↓

Retry

↓

Retry

↓

Dead Letter
```

Nếu API bị gọi lại

```
Client

↓

Idempotency

↓

Return Previous Response
```

---

# 16. Luồng tổng thể

```
Marketing

↓

Campaign

↓

Rule

↓

Approval

↓

Active

↓

Customer Booking

↓

Promotion Engine

↓

Reservation

↓

Payment

↓

Redemption

↓

Analytics

↓

Archive
```

---

# 17. Tương tác với các Microservices

| Microservice | Vai trò |
|--------------|---------|
| Auth Service | Xác thực và phân quyền |
| User Service | Thông tin khách hàng, hạng thành viên |
| Booking Service | Gửi yêu cầu tính và áp dụng khuyến mãi |
| Payment Service | Xác nhận thanh toán, hoàn tiền |
| Movie Service | Thông tin phim, suất chiếu, rạp |
| Notification Service | Gửi email, SMS, Push Notification |
| Analytics Service | Thống kê và báo cáo |
| Gateway Service | Định tuyến và bảo vệ API |

---

# 18. Luồng dữ liệu theo thời gian

```
Campaign
    │
    ▼
Rule
    │
    ▼
Coupon / Voucher
    │
    ▼
Reservation
    │
    ▼
Payment
    │
    ▼
Redemption
    │
    ▼
Analytics
    │
    ▼
Archive
```

---

# 19. Mục tiêu thiết kế

Promotion Service được xây dựng với các mục tiêu:

- Hỗ trợ khối lượng giao dịch lớn.
- Đảm bảo tính nhất quán dữ liệu.
- Hỗ trợ mở rộng theo chiều ngang.
- Dễ dàng tích hợp với các Microservices khác.
- Hỗ trợ Event-Driven Architecture.
- Đảm bảo khả năng Audit và Compliance.
- Hỗ trợ theo dõi ngân sách, chi phí khuyến mãi và báo cáo.
- Đảm bảo khả năng phục hồi khi xảy ra lỗi thông qua Retry, Idempotency và Transactional Outbox.

---

# 20. Tổng kết

Promotion Service được thiết kế theo mô hình **Production-Ready Enterprise Microservices**, bao phủ toàn bộ vòng đời của một chương trình khuyến mãi từ khâu xây dựng, phê duyệt, áp dụng cho khách hàng, xử lý giao dịch đến phân tích dữ liệu.

Hệ thống kết hợp các nguyên tắc **Domain-Driven Design (DDD)**, **Event-Driven Architecture (EDA)** và các mẫu thiết kế như **Transactional Outbox**, **Idempotency**, **Optimistic Locking**, **Soft Delete**, **Audit Trail** và **Saga Pattern** (ở mức phối hợp liên dịch vụ) để đảm bảo khả năng mở rộng, tính nhất quán dữ liệu, khả năng phục hồi và đáp ứng các yêu cầu vận hành trong môi trường Production.

---

# 21. Kết luận

Tài liệu này đã hoàn thành toàn bộ thiết kế cơ sở dữ liệu và nghiệp vụ của Promotion Service, bao gồm:

- 16 bảng dữ liệu cốt lõi và hỗ trợ.
- Quan hệ giữa các bảng.
- Business Rules.
- Luồng dữ liệu và luồng nghiệp vụ.
- Kiến trúc tích hợp Microservices.
- Các cơ chế bảo đảm tính nhất quán và độ tin cậy.
- Quy trình quản trị, kiểm toán và theo dõi chi phí khuyến mãi.

Đây là nền tảng để triển khai Promotion Service theo tiêu chuẩn doanh nghiệp, đáp ứng yêu cầu về hiệu năng, khả năng mở rộng, tính bảo mật và khả năng vận hành trong môi trường Production.

---

**Kết thúc Phase 16 - Hoàn thành Promotion Service Database Design Specification**

## 22. Runtime schema reconciliation (2026-07-31)

The Flyway files under `server/promotion-service/src/main/resources/db/migration`
are authoritative for deployment. The current sequence is V1 (base schema),
V2 (reservation/outbox hardening), V3 (configuration/integration and legacy
partner support), V4 (normalization of legacy `CHAR(n)` columns to
`VARCHAR(n)`), and V5 (removal of partner-funded promotions and financial
settlement). V4 is data-preserving and aligns Hibernate validation with the
numeric account IDs now emitted by Auth/User/Score while retaining UUID
compatibility. V5 removes `partners`, `partner_settlements` and the obsolete
partner/funding columns from existing databases.

This SQL design describes the full target domain. The implemented checkout
runtime intentionally remains one coupon or one voucher; multi-benefit
stacking, automatic discovery and the points saga require separate
cross-service contracts and are not silently enabled by the schema.
