# Notification Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                                                       |
| -------------- | ------------------------------------------------------------------------------ |
| Service        | `notification-service`                                                         |
| Feature        | Notification Template, Internal Send, Delivery Log and User Notification Query |
| API liên quan  | Template Management, Internal Send, Notification Logs, Retry, User Query       |
| Contract Owner | Dương Thiện Nhân                                                               |
| Backend Owner  | Trần Lương Thiện Hoàng                                                         |
| Reviewer       | Trần Lương Thiện Hoàng                                                         |
| Trạng thái     | Draft / Ready for Review                                                       |
| Milestone      | Sprint 2 - Core Service API Foundation                                         |
| Ngày cập nhật  | 21/06/2026                                                                     |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả API Contract cho `notification-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Thống nhất contract giữa Notification Service, API Gateway và các service phát sinh notification.
* Làm cơ sở implement template management, internal send request và notification log.
* Chuẩn hóa channel, trạng thái gửi, retry và error handling.
* Phân loại rõ Public, Protected, Admin và Internal endpoint.
* Không để service khác thao tác trực tiếp Notification database.
* Không expose Internal Send API tùy tiện qua API Gateway.
* Không lưu hoặc log secret/provider credential.
* Hỗ trợ mock provider hoặc foundation flow trong Sprint 2.
* Ghi rõ các điểm có thể mismatch với schema Sprint 0 để service owner review.
* Làm cơ sở tách implementation issue sau khi contract được duyệt.

---

## 3. Phạm Vi Notification Service

Notification Service chịu trách nhiệm:

* Quản lý notification template.
* Validate template code và channel.
* Render placeholder variables.
* Tiếp nhận yêu cầu gửi notification từ service nội bộ.
* Chọn provider tương ứng với channel.
* Gửi mock hoặc provider notification.
* Ghi notification log cho từng send request.
* Ghi nhận trạng thái thành công/thất bại.
* Lưu lỗi provider đã được sanitize.
* Retry notification thất bại theo policy.
* Cho phép admin tra cứu notification log.
* Cho phép user xem notification của mình nếu channel/nghiệp vụ hỗ trợ.

Notification Service không chịu trách nhiệm:

* Quản lý tài khoản hoặc hồ sơ user.
* Quản lý booking, payment, promotion hoặc score.
* Tự quyết định lúc nào nghiệp vụ phải gửi notification.
* Truy cập database của service khác.
* Lưu SMTP password, API key hoặc provider secret trong database.
* Rollback nghiệp vụ Booking/Payment nếu notification gửi thất bại.
* Quản lý marketing recommendation nâng cao.

---

## 4. Physical Schema Sprint 0

### 4.1. Bảng `notification_templates`

```sql
CREATE TABLE `notification_templates` (
  `id` int PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Template ID',
  `template_code` varchar(100) UNIQUE NOT NULL COMMENT 'e.g., EMAIL_VERIFICATION, TICKET_CONFIRMATION',
  `title` varchar(255) NOT NULL COMMENT 'Tieu de email hoac tieu de thong bao push',
  `content` text NOT NULL COMMENT 'Noi dung mau co chua cac bien cho, e.g., Kính gui {name}, ma ve cua ban la {code}',
  `channel_type` varchar(30) NOT NULL COMMENT 'EMAIL, SMS, PUSH_NOTIFICATION',
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT (now()),
  `updated_at` timestamp DEFAULT (now())
);
```

### 4.2. Bảng `notification_logs`

```sql
CREATE TABLE `notification_logs` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key - Log ID',
  `template_code` varchar(100) COMMENT 'Co the null neu la thong bao tu do khong dung mau',
  `user_id` bigint NOT NULL COMMENT 'Logical Ref sang users.account_id cua User Service',
  `recipient` varchar(150) NOT NULL COMMENT 'Dia chi nhan thuc te: email hoac so dien thoai',
  `channel_type` varchar(30) NOT NULL COMMENT 'EMAIL, SMS, PUSH',
  `actual_title` varchar(255) COMMENT 'Tieu de thuc te da truyen bien hoan thien',
  `actual_content` text NOT NULL COMMENT 'Noi dung thuc te da duoc truyen bien hoan thien de gui di',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED',
  `error_message` text COMMENT 'Luu loi tra ve tu các nha cung cap SMTP, Firebase, Twilio neu gui that bai',
  `sent_at` timestamp COMMENT 'Thoi gian thuc te tin nhan roi khoi he thong',
  `created_at` timestamp DEFAULT (now())
);
```

### 4.3. Quan hệ nội bộ

```sql
ALTER TABLE `notification_logs`
ADD FOREIGN KEY (`template_code`)
REFERENCES `notification_templates` (`template_code`);
```

---

## 5. Phân Tích Schema Hiện Tại

### 5.1. Nghiệp vụ schema hỗ trợ

Schema hiện tại hỗ trợ:

* Template code unique.
* Template title và content.
* Template theo channel.
* Enable/disable template.
* Notification có hoặc không dùng template.
* Liên kết notification với user.
* Lưu recipient thực tế.
* Lưu nội dung đã render.
* Trạng thái `PENDING`, `SENT`, `FAILED`.
* Lưu lỗi provider.
* Lưu thời điểm gửi thành công.

### 5.2. Giới hạn schema hiện tại

Schema chưa có:

```txt
notification_templates.template_name
notification_templates.variables_schema
notification_templates.description
notification_templates.version

notification_logs.event_id
notification_logs.idempotency_key
notification_logs.provider
notification_logs.provider_message_id
notification_logs.retry_count
notification_logs.max_retry
notification_logs.last_retry_at
notification_logs.next_retry_at
notification_logs.updated_at
notification_logs.delivered_at
notification_logs.read_at
notification_logs.is_read
notification_logs.failure_code
notification_logs.request_source
notification_logs.reference_type
notification_logs.reference_id
```

### 5.3. User Notification Center Limitation

Schema hiện tại chỉ ghi delivery log.

Nó chưa có field hỗ trợ đầy đủ:

```txt
isRead
readAt
deletedByUser
```

Vì vậy API `mark read` chỉ có thể implement đúng nghĩa nếu:

* Bổ sung `is_read`/`read_at`, hoặc
* Tạo bảng notification inbox riêng.

Sprint 2 có thể:

```txt
Chỉ implement log/query
Không implement mark read
```

hoặc tạo schema alignment nếu notification center là bắt buộc.

### 5.4. Idempotency Limitation

Schema chưa có:

```txt
event_id
idempotency_key
```

Do đó chưa ngăn được gửi trùng khi:

* Kafka event được deliver lại.
* Internal API retry.
* Service caller timeout rồi gọi lại.
* Consumer restart.

Khuyến nghị bổ sung unique idempotency key hoặc bảng processed events.

### 5.5. Retry Limitation

Schema hiện chỉ có status và error message.

Chưa thể theo dõi rõ:

* Đã retry bao nhiêu lần.
* Khi nào retry tiếp.
* Có vượt max retry không.
* Retry attempt nào thành công.

Reviewer phải quyết định Sprint 2:

* Chỉ retry trong memory/foundation.
* Hay bổ sung retry fields vào schema.

---

## 6. Database-per-Service và Logical Reference

Field:

```txt
notification_logs.user_id
```

là logical reference tới User Service.

Notification Service:

* Không tạo foreign key vật lý sang User database.
* Không truy cập trực tiếp User database.
* Không tự lấy email/phone từ database service khác.
* Recipient phải được gửi trong Internal Request hoặc lấy thông qua API hợp lệ.
* Không thay đổi dữ liệu Booking, Payment, Promotion hoặc Score.

### Source of truth

| Dữ liệu                    | Source of truth         |
| -------------------------- | ----------------------- |
| Notification template      | Notification Service    |
| Notification delivery log  | Notification Service    |
| User profile/email/phone   | User Service            |
| Business event             | Service phát sinh event |
| Booking/payment status     | Booking/Payment Service |
| Provider delivery response | Notification provider   |

---

## 7. API Gateway và Service URL

### 7.1. API Gateway

Frontend chỉ gọi:

```txt
http://localhost:8080
```

### 7.2. Notification Service Direct URL

Chỉ dùng cho debug hoặc backend integration:

```txt
http://localhost:8085
```

Port chính thức lấy từ cấu hình project.

### 7.3. User Query Flow

```txt
React Frontend
→ API Gateway
→ Notification Service
→ Notification Database
```

### 7.4. Internal Send Flow

```txt
Auth / Booking / Payment / Promotion / Score Service
→ Internal API hoặc Kafka Event
→ Notification Service
→ Render template
→ Provider/Mock sender
→ Notification log
```

---

## 8. Quy Ước Chung

### 8.1. Protected API Header

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

### 8.2. Admin API Header

```http
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

### 8.3. Internal API Header

```http
X-Internal-Token: <internal-token>
Content-Type: application/json
```

Cơ chế internal authentication phải đồng bộ với API Gateway/security design.

### 8.4. Datetime Format

```txt
ISO-8601
YYYY-MM-DDTHH:mm:ss
```

### 8.5. Timezone

```txt
Asia/Ho_Chi_Minh
```

### 8.6. Pagination

* `page` bắt đầu từ `0`.
* `size` mặc định `10`.
* `size` tối đa `50`.

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
      "field": "templateCode",
      "message": "Template code is required"
    }
  ]
}
```

---

## 10. Channel Definitions

### 10.1. NotificationChannel

```txt
EMAIL
SMS
PUSH_NOTIFICATION
IN_APP
```

Schema hiện liệt kê:

```txt
EMAIL
SMS
PUSH_NOTIFICATION
```

`notification_logs` comment lại dùng `PUSH`.

Contract chuẩn hóa thành:

```txt
PUSH_NOTIFICATION
```

Reviewer cần xác nhận enum chính thức.

### 10.2. Sprint 2 Channel Direction

Sprint 2 có thể hỗ trợ:

```txt
EMAIL foundation/mock
IN_APP/log foundation
```

Các channel sau có thể chỉ ở mức contract:

```txt
SMS
PUSH_NOTIFICATION
```

### 10.3. Recipient Rules

| Channel           | Recipient                                             |
| ----------------- | ----------------------------------------------------- |
| EMAIL             | Email address                                         |
| SMS               | Phone number                                          |
| PUSH_NOTIFICATION | Device token hoặc push target                         |
| IN_APP            | Có thể dùng userId, không nhất thiết recipient string |

Schema hiện bắt buộc `recipient NOT NULL`, nên `IN_APP` có thể dùng:

```txt
USER:<userId>
```

hoặc cần schema alignment.

---

## 11. Notification Status Lifecycle

### 11.1. NotificationStatus đề xuất

```txt
PENDING
PROCESSING
SENT
FAILED
RETRYING
CANCELLED
```

Schema Sprint 0 hiện chỉ có:

```txt
PENDING
SENT
FAILED
```

### 11.2. Allowed Transitions

| Current    | Allowed Next                        |
| ---------- | ----------------------------------- |
| PENDING    | PROCESSING, SENT, FAILED, CANCELLED |
| PROCESSING | SENT, FAILED                        |
| FAILED     | RETRYING, CANCELLED                 |
| RETRYING   | PROCESSING, SENT, FAILED            |
| SENT       | Không thay đổi delivery status      |
| CANCELLED  | Không có                            |

### 11.3. Minimal Sprint 2 Lifecycle

Nếu không mở rộng status:

```txt
PENDING
→ SENT

PENDING
→ FAILED

FAILED
→ PENDING khi retry
```

Khuyến nghị thêm `RETRYING` nếu cần thể hiện rõ.

---

## 12. Template Variable Syntax

Template sử dụng placeholder:

```txt
{name}
{otp}
{bookingCode}
{movieTitle}
{showtime}
```

### Variable Rules

* Variable name chỉ chứa chữ, số và `_`.
* Không cho expression tùy ý.
* Không execute code trong template.
* Tất cả variable bắt buộc phải có giá trị trước khi gửi.
* Variable thừa có thể bị bỏ qua hoặc validation warning.
* Variable thiếu phải trả lỗi.

Ví dụ template:

```txt
Kính gửi {name}, mã xác thực của bạn là {otp}.
```

Payload:

```json
{
  "name": "Dương Thiện Nhân",
  "otp": "123456"
}
```

Rendered:

```txt
Kính gửi Dương Thiện Nhân, mã xác thực của bạn là 123456.
```

---

## 13. Security Classification

### 13.1. Internal

```txt
POST /internal/notifications/send
POST /internal/notifications/send-batch
POST /internal/notifications/{notificationId}/retry
```

Không expose công khai qua Gateway.

### 13.2. Protected

```txt
GET   /api/notifications/me
GET   /api/notifications/me/{notificationId}
PATCH /api/notifications/me/{notificationId}/read
PATCH /api/notifications/me/read-all
```

Mark read chỉ implement nếu schema hỗ trợ.

### 13.3. Admin

```txt
/api/admin/notification-templates/**
/api/admin/notification-logs/**
```

### 13.4. Public

Sprint 2 không cần Public Notification API.

---

## 14. Endpoint Summary

| Method | Endpoint                                        | Access    | Mục đích                  |
| ------ | ----------------------------------------------- | --------- | ------------------------- |
| GET    | `/api/notifications/me`                         | Protected | Notification/log của user |
| GET    | `/api/notifications/me/{id}`                    | Protected | Notification detail       |
| PATCH  | `/api/notifications/me/{id}/read`               | Protected | Mark read nếu hỗ trợ      |
| PATCH  | `/api/notifications/me/read-all`                | Protected | Mark all read nếu hỗ trợ  |
| POST   | `/internal/notifications/send`                  | Internal  | Gửi một notification      |
| POST   | `/internal/notifications/send-batch`            | Internal  | Gửi batch foundation      |
| POST   | `/internal/notifications/{id}/retry`            | Internal  | Retry notification        |
| POST   | `/api/admin/notification-templates`             | Admin     | Tạo template              |
| GET    | `/api/admin/notification-templates`             | Admin     | Danh sách template        |
| GET    | `/api/admin/notification-templates/{id}`        | Admin     | Template detail           |
| PUT    | `/api/admin/notification-templates/{id}`        | Admin     | Cập nhật template         |
| PATCH  | `/api/admin/notification-templates/{id}/status` | Admin     | Enable/disable template   |
| GET    | `/api/admin/notification-logs`                  | Admin     | Danh sách log             |
| GET    | `/api/admin/notification-logs/{id}`             | Admin     | Log detail                |
| POST   | `/api/admin/notification-logs/{id}/retry`       | Admin     | Retry thủ công            |

---

# 15. Template Management APIs

## 15.1. Create Notification Template

### Endpoint

```http
POST /api/admin/notification-templates
```

### Request Body

```json
{
  "templateCode": "TICKET_CONFIRMATION",
  "title": "Xác nhận vé LoraFilm",
  "content": "Xin chào {name}, vé của bạn có mã {bookingCode}. Suất chiếu lúc {showtime}.",
  "channelType": "EMAIL",
  "isActive": true
}
```

### Field Definitions

| Field        | Type    | Required | Validation                    |
| ------------ | ------- | -------: | ----------------------------- |
| templateCode | string  |      Yes | Uppercase, unique, tối đa 100 |
| title        | string  |      Yes | Không rỗng, tối đa 255        |
| content      | string  |      Yes | Không rỗng                    |
| channelType  | string  |      Yes | Enum hợp lệ                   |
| isActive     | boolean |       No | Mặc định true                 |

### Template Code Normalization

```txt
ticket_confirmation
TICKET_CONFIRMATION
 Ticket_Confirmation
```

được normalize thành:

```txt
TICKET_CONFIRMATION
```

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Notification template created successfully",
  "data": {
    "templateId": 10,
    "templateCode": "TICKET_CONFIRMATION",
    "title": "Xác nhận vé LoraFilm",
    "content": "Xin chào {name}, vé của bạn có mã {bookingCode}. Suất chiếu lúc {showtime}.",
    "channelType": "EMAIL",
    "isActive": true,
    "createdAt": "2026-06-21T21:00:00"
  }
}
```

### Duplicate Template Code

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Notification template code already exists",
  "errorCode": "NOTIFICATION_TEMPLATE_CODE_ALREADY_EXISTS",
  "data": null,
  "errors": null
}
```

---

## 15.2. Get Notification Templates

### Endpoint

```http
GET /api/admin/notification-templates
```

### Query Parameters

| Parameter   | Type    | Required |
| ----------- | ------- | -------: |
| page        | integer |       No |
| size        | integer |       No |
| code        | string  |       No |
| channelType | string  |       No |
| isActive    | boolean |       No |
| sort        | string  |       No |

### Response Success

```json
{
  "success": true,
  "message": "Notification templates retrieved successfully",
  "data": {
    "content": [
      {
        "templateId": 10,
        "templateCode": "TICKET_CONFIRMATION",
        "title": "Xác nhận vé LoraFilm",
        "channelType": "EMAIL",
        "isActive": true,
        "createdAt": "2026-06-21T21:00:00",
        "updatedAt": "2026-06-21T21:00:00"
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

## 15.3. Get Notification Template Detail

### Endpoint

```http
GET /api/admin/notification-templates/{templateId}
```

### Response Success

```json
{
  "success": true,
  "message": "Notification template retrieved successfully",
  "data": {
    "templateId": 10,
    "templateCode": "TICKET_CONFIRMATION",
    "title": "Xác nhận vé LoraFilm",
    "content": "Xin chào {name}, vé của bạn có mã {bookingCode}. Suất chiếu lúc {showtime}.",
    "channelType": "EMAIL",
    "isActive": true,
    "createdAt": "2026-06-21T21:00:00",
    "updatedAt": "2026-06-21T21:00:00"
  }
}
```

### Error

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Notification template not found",
  "errorCode": "NOTIFICATION_TEMPLATE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

## 15.4. Update Notification Template

### Endpoint

```http
PUT /api/admin/notification-templates/{templateId}
```

### Request

```json
{
  "templateCode": "TICKET_CONFIRMATION",
  "title": "Vé xem phim của bạn đã được xác nhận",
  "content": "Xin chào {name}, booking {bookingCode} đã được xác nhận.",
  "channelType": "EMAIL",
  "isActive": true
}
```

### Business Rules

* Không nên đổi `templateCode` sau khi đã có log sử dụng.
* Nếu đổi template code, foreign key log có thể bị ảnh hưởng.
* Update template chỉ ảnh hưởng notification gửi mới.
* Notification log cũ giữ `actualTitle` và `actualContent` đã render.

Khuyến nghị:

```txt
templateCode immutable sau khi tạo
```

---

## 15.5. Enable/Disable Notification Template

### Endpoint

```http
PATCH /api/admin/notification-templates/{templateId}/status
```

### Request

```json
{
  "isActive": false
}
```

### Response

```json
{
  "success": true,
  "message": "Notification template status updated successfully",
  "data": {
    "templateId": 10,
    "templateCode": "TICKET_CONFIRMATION",
    "isActive": false
  }
}
```

Template bị disable:

* Không được dùng cho send request mới.
* Không xóa log cũ.
* Không làm thay đổi notification đã gửi.

---

# 16. Internal Send Notification API

## 16.1. Endpoint

```http
POST /internal/notifications/send
```

API này do service nội bộ hoặc Kafka consumer sử dụng.

Frontend không được gọi trực tiếp.

### Request dùng template

```json
{
  "eventId": "BOOKING-CONFIRMED-1001",
  "templateCode": "TICKET_CONFIRMATION",
  "userId": 15,
  "recipient": "nhan@example.com",
  "channelType": "EMAIL",
  "variables": {
    "name": "Dương Thiện Nhân",
    "bookingCode": "LORA-20260621-0001",
    "showtime": "21/06/2026 20:30"
  },
  "reference": {
    "type": "BOOKING",
    "id": "1001"
  }
}
```

### Request thông báo tự do

```json
{
  "eventId": "ADMIN-NOTICE-20260621-001",
  "templateCode": null,
  "userId": 15,
  "recipient": "nhan@example.com",
  "channelType": "EMAIL",
  "title": "Thông báo từ LoraFilm",
  "content": "Suất chiếu của bạn có thay đổi.",
  "reference": {
    "type": "BOOKING",
    "id": "1001"
  }
}
```

### Field Definitions

| Field          | Type   |    Required | Validation                       |
| -------------- | ------ | ----------: | -------------------------------- |
| eventId        | string |         Yes | Idempotency identifier           |
| templateCode   | string | Conditional | Required nếu không gửi free-form |
| userId         | number |         Yes | > 0                              |
| recipient      | string |         Yes | Hợp lệ theo channel              |
| channelType    | string |         Yes | Enum hợp lệ                      |
| variables      | object | Conditional | Required theo placeholder        |
| title          | string | Conditional | Required với free-form           |
| content        | string | Conditional | Required với free-form           |
| reference.type | string |          No | BOOKING/PAYMENT/ACCOUNT/...      |
| reference.id   | string |          No | Logical reference                |

### Processing Flow

```txt
Validate internal authentication
→ Validate request
→ Check eventId/idempotency
→ Load active template nếu có
→ Verify channel khớp template
→ Validate placeholder variables
→ Render title/content
→ Sanitize content
→ Create notification log PENDING
→ Send through mock/provider
→ Update SENT hoặc FAILED
→ Return result
```

### Response Success — Sent synchronously

Status: `201 Created`

```json
{
  "success": true,
  "message": "Notification sent successfully",
  "data": {
    "notificationId": 5001,
    "eventId": "BOOKING-CONFIRMED-1001",
    "templateCode": "TICKET_CONFIRMATION",
    "userId": 15,
    "channelType": "EMAIL",
    "recipient": "n***@example.com",
    "status": "SENT",
    "sentAt": "2026-06-21T21:10:00"
  }
}
```

### Response Accepted — Async Direction

Nếu xử lý async:

Status: `202 Accepted`

```json
{
  "success": true,
  "message": "Notification request accepted",
  "data": {
    "notificationId": 5001,
    "status": "PENDING",
    "acceptedAt": "2026-06-21T21:10:00"
  }
}
```

Sprint 2 có thể dùng synchronous mock flow; async Kafka/provider worker để sprint sau.

---

## 16.2. Template Disabled

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Notification template is disabled",
  "errorCode": "NOTIFICATION_TEMPLATE_DISABLED",
  "data": null,
  "errors": null
}
```

## 16.3. Channel Mismatch

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Requested channel does not match template channel",
  "errorCode": "NOTIFICATION_TEMPLATE_CHANNEL_MISMATCH",
  "data": {
    "templateChannel": "EMAIL",
    "requestedChannel": "SMS"
  },
  "errors": null
}
```

## 16.4. Missing Template Variables

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Required template variables are missing",
  "errorCode": "NOTIFICATION_TEMPLATE_VARIABLE_MISSING",
  "data": {
    "missingVariables": [
      "bookingCode",
      "showtime"
    ]
  },
  "errors": null
}
```

## 16.5. Invalid Recipient

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Notification recipient is invalid",
  "errorCode": "NOTIFICATION_INVALID_RECIPIENT",
  "data": null,
  "errors": null
}
```

---

# 17. Notification Idempotency

## 17.1. Idempotency Key

Internal send request phải có:

```txt
eventId
```

Ví dụ:

```txt
BOOKING-CONFIRMED-1001
PAYMENT-SUCCESS-3001
ACCOUNT-OTP-RESEND-15-3
```

### Rule

Cùng `eventId` và cùng notification purpose:

```txt
Không gửi lại ngoài ý muốn
Không tạo duplicate notification log
```

### Idempotent Response

```json
{
  "success": true,
  "message": "Notification request was already processed",
  "data": {
    "notificationId": 5001,
    "status": "SENT",
    "idempotent": true
  }
}
```

### Schema Limitation

Schema chưa có `event_id`.

Nếu không bổ sung field, idempotency chỉ có thể xử lý bằng:

* Redis key tạm thời.
* Bảng processed events riêng.
* Kiểm tra nội dung/log không đáng tin cậy.

Khuyến nghị tạo schema alignment nếu event-driven flow được implement.

---

# 18. Provider Send Behavior

## 18.1. Provider Abstraction

Notification Service nên có abstraction:

```txt
NotificationSender
├── EmailSender
├── SmsSender
├── PushNotificationSender
└── MockNotificationSender
```

Contract không bắt buộc công nghệ cụ thể.

## 18.2. Email Direction

Có thể dùng:

* SMTP mock/local.
* Mailtrap.
* Gmail SMTP cho môi trường dev nếu được phép.
* Provider production ở sprint sau.

## 18.3. SMS Direction

Sprint 2 không bắt buộc provider thật.

Có thể:

* Mock log.
* Disable channel.
* Trả `NOTIFICATION_CHANNEL_NOT_SUPPORTED`.

## 18.4. Push Direction

Có thể dùng Firebase ở sprint sau.

Schema hiện chưa có device token management.

## 18.5. Provider Secret

Provider credentials phải nằm trong:

```txt
Environment variables
Secret manager
Deployment configuration
```

Không lưu trong:

```txt
notification_templates
notification_logs
source code
Git repository
Frontend
```

---

# 19. Notification Log Behavior

## 19.1. Initial Log

Khi nhận send request hợp lệ:

```txt
status = PENDING
```

Log phải được tạo trước khi gọi provider để có khả năng audit.

## 19.2. Success

Provider/mock gửi thành công:

```txt
status = SENT
sentAt = now
errorMessage = null
```

## 19.3. Failure

Provider gửi thất bại:

```txt
status = FAILED
errorMessage = sanitized provider error
sentAt = null
```

## 19.4. Sensitive Data

Không lưu trong `actualContent` hoặc `errorMessage`:

```txt
SMTP password
Provider API key
Bearer token
Full card data
CVV
Password
Refresh token
Sensitive internal header
```

### OTP

OTP có thể cần gửi trong actual content, nhưng việc lưu plaintext OTP trong notification log là rủi ro.

Khuyến nghị:

* Mask OTP trong log.
* Hoặc không lưu actual content đầy đủ đối với OTP template.
* Hoặc có retention ngắn.

Reviewer cần xác nhận policy.

---

# 20. Retry Direction

## 20.1. Retry Conditions

Có thể retry với lỗi tạm thời:

```txt
Provider timeout
Connection failure
HTTP 429
HTTP 502/503/504
SMTP temporary failure
```

Không retry với:

```txt
Invalid recipient
Template invalid
Missing variables
Provider authentication failure kéo dài
Unsupported channel
```

## 20.2. Retry Policy Đề Xuất

```txt
Maximum retry attempts: 3
Backoff: 30 giây, 2 phút, 5 phút
```

## 20.3. Retry Status

Hướng đầy đủ:

```txt
FAILED
→ RETRYING
→ SENT hoặc FAILED
```

Schema hiện chưa có retry metadata.

## 20.4. Retry Internal API

```http
POST /internal/notifications/{notificationId}/retry
```

### Request

```json
{
  "reason": "Automatic retry after temporary provider failure",
  "requestId": "RETRY-NOTIFICATION-5001-1"
}
```

### Response

```json
{
  "success": true,
  "message": "Notification retry accepted",
  "data": {
    "notificationId": 5001,
    "status": "RETRYING",
    "retryCount": 1
  }
}
```

### Retry Limit Reached

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Notification retry limit has been reached",
  "errorCode": "NOTIFICATION_RETRY_LIMIT_REACHED",
  "data": null,
  "errors": null
}
```

---

# 21. Batch Send Direction

## 21.1. Endpoint

```http
POST /internal/notifications/send-batch
```

Sprint 2 chỉ cần foundation; không bắt buộc production bulk sending.

### Request

```json
{
  "batchId": "BATCH-MOVIE-REMINDER-20260621",
  "templateCode": "SHOWTIME_REMINDER",
  "channelType": "EMAIL",
  "notifications": [
    {
      "eventId": "SHOWTIME-REMINDER-USER-15-1001",
      "userId": 15,
      "recipient": "nhan@example.com",
      "variables": {
        "name": "Dương Thiện Nhân",
        "movieTitle": "Avengers",
        "showtime": "20:30"
      }
    }
  ]
}
```

### Response

Status: `202 Accepted`

```json
{
  "success": true,
  "message": "Notification batch accepted",
  "data": {
    "batchId": "BATCH-MOVIE-REMINDER-20260621",
    "acceptedCount": 1,
    "rejectedCount": 0
  }
}
```

Advanced batch processing nằm ngoài Sprint 2.

---

# 22. User Notification Query APIs

## 22.1. Get Current User Notifications

### Endpoint

```http
GET /api/notifications/me
```

Backend lấy user ID từ JWT.

### Query Parameters

| Parameter    | Type     | Required |
| ------------ | -------- | -------: |
| page         | integer  |       No |
| size         | integer  |       No |
| channelType  | string   |       No |
| status       | string   |       No |
| templateCode | string   |       No |
| from         | datetime |       No |
| to           | datetime |       No |
| sort         | string   |       No |

### Response Success

```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": {
    "content": [
      {
        "notificationId": 5001,
        "templateCode": "TICKET_CONFIRMATION",
        "channelType": "EMAIL",
        "title": "Vé xem phim của bạn đã được xác nhận",
        "content": "Booking LORA-20260621-0001 đã được xác nhận.",
        "status": "SENT",
        "sentAt": "2026-06-21T21:10:00",
        "createdAt": "2026-06-21T21:09:58"
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

### Security Note

Customer API không nên trả:

```txt
errorMessage kỹ thuật
provider response
recipient đầy đủ nếu không cần
internal event ID
secret hoặc header
```

---

## 22.2. Get Notification Detail

### Endpoint

```http
GET /api/notifications/me/{notificationId}
```

Customer chỉ được xem notification có `userId` thuộc chính mình.

### Response Success

```json
{
  "success": true,
  "message": "Notification retrieved successfully",
  "data": {
    "notificationId": 5001,
    "templateCode": "TICKET_CONFIRMATION",
    "channelType": "EMAIL",
    "title": "Vé xem phim của bạn đã được xác nhận",
    "content": "Booking LORA-20260621-0001 đã được xác nhận.",
    "status": "SENT",
    "sentAt": "2026-06-21T21:10:00",
    "createdAt": "2026-06-21T21:09:58"
  }
}
```

### Error

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Notification not found",
  "errorCode": "NOTIFICATION_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

## 22.3. Mark Notification as Read

### Endpoint

```http
PATCH /api/notifications/me/{notificationId}/read
```

### Response

```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": {
    "notificationId": 5001,
    "isRead": true,
    "readAt": "2026-06-21T21:30:00"
  }
}
```

### Schema Limitation

Schema chưa có:

```txt
is_read
read_at
```

Endpoint này không implement trước khi schema được align.

---

## 22.4. Mark All Notifications as Read

```http
PATCH /api/notifications/me/read-all
```

Cũng phụ thuộc schema `is_read/read_at`.

---

# 23. Admin Notification Log APIs

## 23.1. Get Notification Logs

### Endpoint

```http
GET /api/admin/notification-logs
```

### Query Parameters

| Parameter    | Type     | Required |
| ------------ | -------- | -------: |
| page         | integer  |       No |
| size         | integer  |       No |
| userId       | number   |       No |
| templateCode | string   |       No |
| channelType  | string   |       No |
| status       | string   |       No |
| recipient    | string   |       No |
| from         | datetime |       No |
| to           | datetime |       No |
| sort         | string   |       No |

### Response

```json
{
  "success": true,
  "message": "Notification logs retrieved successfully",
  "data": {
    "content": [
      {
        "notificationId": 5001,
        "templateCode": "TICKET_CONFIRMATION",
        "userId": 15,
        "recipient": "n***@example.com",
        "channelType": "EMAIL",
        "status": "SENT",
        "sentAt": "2026-06-21T21:10:00",
        "createdAt": "2026-06-21T21:09:58"
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

## 23.2. Get Notification Log Detail

### Endpoint

```http
GET /api/admin/notification-logs/{notificationId}
```

### Response

```json
{
  "success": true,
  "message": "Notification log retrieved successfully",
  "data": {
    "notificationId": 5001,
    "templateCode": "TICKET_CONFIRMATION",
    "userId": 15,
    "recipient": "n***@example.com",
    "channelType": "EMAIL",
    "actualTitle": "Vé xem phim của bạn đã được xác nhận",
    "actualContent": "Booking LORA-20260621-0001 đã được xác nhận.",
    "status": "SENT",
    "errorMessage": null,
    "sentAt": "2026-06-21T21:10:00",
    "createdAt": "2026-06-21T21:09:58"
  }
}
```

Actual content phải được mask nếu chứa dữ liệu nhạy cảm.

---

## 23.3. Admin Manual Retry

### Endpoint

```http
POST /api/admin/notification-logs/{notificationId}/retry
```

### Request

```json
{
  "reason": "Manual retry after provider recovery"
}
```

Chỉ cho retry notification `FAILED` và chưa vượt retry limit.

---

# 24. Event-driven Direction

Notification Service có thể consume các event:

```txt
ACCOUNT_CREATED
OTP_REQUESTED
BOOKING_CREATED
BOOKING_CONFIRMED
BOOKING_CANCELLED
BOOKING_EXPIRED
PAYMENT_SUCCESS
PAYMENT_FAILED
PROMOTION_APPLIED
SCORE_EARNED
MEMBERSHIP_TIER_CHANGED
```

### Event Contract Requirement

Mỗi event cần có:

```txt
eventId
eventType
occurredAt
sourceService
userId
recipient hoặc dữ liệu để resolve recipient
templateCode
variables
referenceId
```

### Example

```json
{
  "eventId": "BOOKING-CONFIRMED-1001",
  "eventType": "BOOKING_CONFIRMED",
  "occurredAt": "2026-06-21T21:10:00",
  "sourceService": "booking-service",
  "userId": 15,
  "recipient": "nhan@example.com",
  "templateCode": "TICKET_CONFIRMATION",
  "variables": {
    "name": "Dương Thiện Nhân",
    "bookingCode": "LORA-20260621-0001"
  }
}
```

Kafka implementation không bắt buộc trong issue contract này.

---

# 25. Failure Isolation Rules

Notification failure không được:

* Rollback booking.
* Rollback payment.
* Rollback promotion.
* Rollback score.
* Làm API nghiệp vụ chính thất bại sau khi transaction chính đã commit.

Ví dụ:

```txt
Payment SUCCESS
→ Booking CONFIRMED
→ Notification FAILED
```

Kết quả:

```txt
Payment vẫn SUCCESS
Booking vẫn CONFIRMED
Notification ghi FAILED và retry sau
```

---

# 26. Concurrency Rules

## 26.1. Duplicate Internal Requests

Hai request cùng event:

```txt
Chỉ tạo/gửi một notification
```

Cần idempotency mechanism.

## 26.2. Retry Race

Automatic retry và manual retry không được chạy đồng thời cho cùng notification.

Có thể dùng:

* Row lock.
* Optimistic lock.
* Distributed lock.
* Atomic status transition.

## 26.3. Status Transition

Update status phải có điều kiện:

```sql
UPDATE notification_logs
SET status = 'PROCESSING'
WHERE id = :id
  AND status IN ('PENDING', 'FAILED', 'RETRYING');
```

---

# 27. Security Rules

* Internal Send API không expose qua Gateway.
* Internal endpoint phải có authentication riêng.
* Customer chỉ xem notification của chính mình.
* Admin query cần permission phù hợp.
* Không lưu provider secret trong DB.
* Không log authorization header.
* Không log access/refresh token.
* Không log password.
* OTP content phải có masking/retention policy.
* Recipient phải được validate theo channel.
* Free-form notification chỉ cho internal/admin caller đáng tin cậy.
* Không cho Frontend tự gửi email/SMS tới recipient tùy ý.
* Template content không được chứa executable expression.
* Provider payload phải sanitize trước khi lưu.

Permission đề xuất:

```txt
NOTIFICATION_READ
NOTIFICATION_LOG_READ
NOTIFICATION_RETRY
NOTIFICATION_TEMPLATE_READ
NOTIFICATION_TEMPLATE_CREATE
NOTIFICATION_TEMPLATE_UPDATE
```

---

# 28. Delete Policy

Không hard delete:

```txt
notification_logs
```

Lý do:

* Audit.
* Retry.
* Troubleshooting.
* Đối soát provider.

Không hard delete template đã được sử dụng.

Khuyến nghị:

```txt
is_active = false
```

Schema hiện có foreign key từ log sang `template_code` nhưng không khai báo `ON DELETE`.

Do đó hard delete template đã có log sẽ thất bại hoặc gây vấn đề.

Không expose delete template API trong Sprint 2.

---

# 29. Data Retention Direction

Notification log có thể chứa dữ liệu cá nhân.

Cần policy:

* Log thông thường: giữ theo thời gian quy định của project.
* OTP: retention ngắn hoặc mask ngay.
* Provider error: sanitize.
* Recipient: mask khi trả qua API.
* Nội dung nhạy cảm: không lưu hoặc mã hóa nếu cần.

Sprint 2 chỉ cần ghi rõ direction, chưa cần production retention job.

---

# 30. Error Code Catalog

| Error Code                                  |         HTTP | Ý nghĩa                      |
| ------------------------------------------- | -----------: | ---------------------------- |
| `NOTIFICATION_NOT_FOUND`                    |          404 | Không tìm thấy notification  |
| `NOTIFICATION_TEMPLATE_NOT_FOUND`           |          404 | Không tìm thấy template      |
| `NOTIFICATION_TEMPLATE_CODE_ALREADY_EXISTS` |          409 | Template code đã tồn tại     |
| `NOTIFICATION_TEMPLATE_DISABLED`            |          409 | Template bị disable          |
| `NOTIFICATION_TEMPLATE_CHANNEL_MISMATCH`    |          400 | Channel không khớp template  |
| `NOTIFICATION_TEMPLATE_VARIABLE_MISSING`    |          400 | Thiếu variable               |
| `NOTIFICATION_TEMPLATE_RENDER_FAILED`       |          500 | Render template lỗi          |
| `NOTIFICATION_INVALID_CHANNEL`              |          400 | Channel không hợp lệ         |
| `NOTIFICATION_CHANNEL_NOT_SUPPORTED`        |          409 | Channel chưa hỗ trợ          |
| `NOTIFICATION_INVALID_RECIPIENT`            |          400 | Recipient không hợp lệ       |
| `NOTIFICATION_ALREADY_SENT`                 | 409 hoặc 200 | Notification đã gửi          |
| `NOTIFICATION_EVENT_ALREADY_PROCESSED`      | 409 hoặc 200 | Event đã xử lý               |
| `NOTIFICATION_SEND_FAILED`                  |          502 | Provider gửi thất bại        |
| `NOTIFICATION_PROVIDER_UNAVAILABLE`         |          503 | Provider không khả dụng      |
| `NOTIFICATION_PROVIDER_AUTH_FAILED`         |          502 | Provider authentication lỗi  |
| `NOTIFICATION_RETRY_NOT_ALLOWED`            |          409 | Không được retry             |
| `NOTIFICATION_RETRY_LIMIT_REACHED`          |          409 | Vượt retry limit             |
| `NOTIFICATION_INVALID_STATUS_TRANSITION`    |          409 | Status transition sai        |
| `NOTIFICATION_MARK_READ_NOT_SUPPORTED`      |          501 | Schema chưa hỗ trợ mark read |
| `NOTIFICATION_INVALID_QUERY`                |          400 | Query không hợp lệ           |
| `VALIDATION_ERROR`                          |          400 | Validation lỗi               |
| `UNAUTHORIZED`                              |          401 | Chưa xác thực                |
| `FORBIDDEN`                                 |          403 | Không có quyền               |
| `INTERNAL_SERVER_ERROR`                     |          500 | Lỗi hệ thống                 |

---

# 31. Schema Alignment Notes

## 31.1. Event Idempotency

Contract yêu cầu:

```txt
eventId hoặc idempotencyKey
```

Schema chưa có field tương ứng.

Khuyến nghị thêm:

```txt
notification_logs.event_id UNIQUE
```

hoặc bảng processed events.

## 31.2. Retry Tracking

Contract đề xuất:

```txt
retry_count
last_retry_at
next_retry_at
```

Schema chưa có.

## 31.3. User Read State

Contract đề xuất Protected Notification Center:

```txt
is_read
read_at
```

Schema chưa có.

Nếu không align, bỏ mark-read APIs khỏi Sprint 2.

## 31.4. Provider Tracking

Schema chưa có:

```txt
provider
provider_message_id
```

Hai field này hữu ích cho đối soát.

## 31.5. Request Source và Reference

Schema chưa lưu:

```txt
source_service
reference_type
reference_id
```

Có thể cần để trace notification về Booking/Payment.

## 31.6. Notification Status

Contract đề xuất thêm:

```txt
PROCESSING
RETRYING
CANCELLED
```

Schema comment hiện chỉ có `PENDING`, `SENT`, `FAILED`.

## 31.7. Template Variables Metadata

Schema chưa lưu danh sách variable bắt buộc.

Có thể:

* Parse từ content runtime.
* Hoặc thêm `variables_schema`.

Sprint 2 có thể parse placeholder runtime.

## 31.8. Template Code Foreign Key

Log tham chiếu template bằng business key:

```txt
template_code
```

Điều này làm template code khó đổi.

Reviewer cần xác nhận `templateCode` immutable.

## 31.9. Channel Enum Inconsistency

Template dùng:

```txt
PUSH_NOTIFICATION
```

Log comment dùng:

```txt
PUSH
```

Phải chuẩn hóa enum.

---

# 32. Out of Scope

* Production SMS provider.
* Production push notification provider.
* Provider trả phí.
* Marketing automation.
* User segmentation.
* Campaign scheduling nâng cao.
* Notification preference center.
* Unsubscribe management.
* Multilingual template engine.
* Rich HTML template builder.
* Attachment management.
* Real-time WebSocket notification center.
* Advanced batch sending.
* Provider delivery receipt đầy đủ.
* Notification analytics nâng cao.
* Production Kafka/outbox implementation.
* Backend implementation trong issue contract này.
* Schema update ngoài review process.

---

# 33. Implementation Issue Direction

Sau khi contract được duyệt và schema alignment hoàn thành nếu cần, có thể tách:

```txt
[Backend] Implement Notification Template Management APIs

[Backend] Implement Internal Notification Send and Mock Provider

[Backend] Implement Notification Log Query and Retry Flow

[Backend] Implement User Notification Query APIs
```

Nếu giảm scope Sprint 2:

```txt
Issue 1: Template CRUD
Issue 2: Internal Send + Mock Email
Issue 3: Notification Logs
```

User mark-read API chỉ tạo implementation issue khi schema hỗ trợ.

Implementation issue chỉ chuyển `Ready` khi:

```txt
Contract đã được duyệt
+
Schema bắt buộc đã align
+
Hoàng xác nhận feasibility
```

---

# 34. Acceptance Criteria

* [ ] Có schema Sprint 0 baseline.
* [ ] Có template management contract.
* [ ] Có internal send contract.
* [ ] Có free-form notification direction.
* [ ] Có template variable rules.
* [ ] Có channel definitions.
* [ ] Có notification status lifecycle.
* [ ] Có notification log behavior.
* [ ] Có retry direction.
* [ ] Có idempotency rules.
* [ ] Có user notification query.
* [ ] Có mark-read limitation.
* [ ] Có admin log query.
* [ ] Có event-driven direction.
* [ ] Có failure isolation rules.
* [ ] Có logical reference notes.
* [ ] Có internal route policy.
* [ ] Có provider secret security.
* [ ] Có sensitive log policy.
* [ ] Có error code catalog.
* [ ] Có schema mismatch notes.
* [ ] Hoàng review feasibility.
* [ ] Contract sẵn sàng implementation.
* [ ] MR target `develop`.

---

# 35. Các Điểm Reviewer Cần Xác Nhận

Hoàng cần xác nhận:

1. Notification Service port chính thức.
2. Sprint 2 hỗ trợ channel nào.
3. Có dùng `EMAIL`, `SMS`, `PUSH_NOTIFICATION`, `IN_APP` không.
4. Chuẩn hóa `PUSH` hay `PUSH_NOTIFICATION`.
5. Có implement provider thật hay chỉ mock.
6. Internal send xử lý sync hay async.
7. Có dùng Kafka trong Sprint 2 không.
8. Có cần thêm `event_id/idempotency_key` không.
9. Có cần thêm `retry_count`, `next_retry_at` không.
10. Có thêm status `PROCESSING`, `RETRYING`, `CANCELLED` không.
11. Có implement notification center cho user không.
12. Có cần `is_read/read_at` không.
13. Có cho customer xem Email/SMS delivery log không.
14. Có cần thêm provider và provider message ID không.
15. Có lưu actual content đầy đủ không.
16. OTP có được lưu plaintext trong log không.
17. Recipient cần mask như thế nào.
18. Có cho phép free-form notification không.
19. `templateCode` có immutable sau khi tạo không.
20. Có cần variables schema không hay parse placeholder runtime.
21. Retry tối đa bao nhiêu lần.
22. Lỗi nào được retry.
23. Có cần source service/reference fields không.
24. Có cần schema alignment issue trước implementation không.

---

# 36. Lịch Sử Chỉnh Sửa

| Ngày       | Nội dung                                                            | Người thực hiện  |
| ---------- | ------------------------------------------------------------------- | ---------------- |
| 21/06/2026 | Khởi tạo Notification Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận tại đây sau khi schema MR tương ứng đã được merge.
