# Notification Service API Specification

## 1. Thông Tin Chung

| Mục             | Nội dung                        |
| :-------------- | :------------------------------ |
| Service         | `notification-service`          |
| Feature         | Email Verification Notification |
| API liên quan   | Send Verification Email         |
| Issue liên quan | #54                             |
| Người phụ trách | Hoàng                           |
| Trạng thái      | Ready for Review                |
| Ngày cập nhật   | 16/06/2026                      |

---

# 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả API Contract cho Notification Service phục vụ luồng gửi Email Verification sau khi người dùng đăng ký tài khoản.

Sprint 1 chỉ yêu cầu:

* Định nghĩa API Contract.
* Lưu log notification vào database.
* Quản lý trạng thái notification.
* Chưa yêu cầu tích hợp SMTP hoặc gửi email thật.

Mục tiêu:

* Backend và Frontend thống nhất request/response.
* Auth Service có thể gọi Notification Service theo đúng contract.
* Làm cơ sở cho việc tích hợp Email Sender ở Sprint sau.

---

# 3. Database Scope

## 3.1 Notification Templates

Bảng lưu các mẫu nội dung thông báo.

```sql
notification_templates
```

Ví dụ:

| template_code       | channel_type |
| ------------------- | ------------ |
| EMAIL_VERIFICATION  | EMAIL        |
| TICKET_CONFIRMATION | EMAIL        |
| RESET_PASSWORD      | EMAIL        |

---

## 3.2 Notification Logs

Bảng lưu lịch sử gửi thông báo.

```sql
notification_logs
```

Các trạng thái được hỗ trợ:

| Status  | Ý nghĩa                  |
| ------- | ------------------------ |
| PENDING | Đã tiếp nhận yêu cầu gửi |
| SENT    | Đã gửi thành công        |
| FAILED  | Gửi thất bại             |

---

# 4. Service URL

## Notification Service

```text
http://localhost:8085
```

## Internal Endpoint

```text
POST /internal/notifications/email-verification
```

Endpoint này chỉ được gọi bởi các service nội bộ như:

* auth-service
* user-service
* api-gateway (nếu được cấu hình)

Frontend không gọi trực tiếp endpoint này.

---

# 5. Email Verification API

## 5.1 Mục Tiêu

API dùng để tiếp nhận yêu cầu gửi Email Verification sau khi người dùng đăng ký tài khoản.

Notification Service sẽ:

* Validate request.
* Lấy template EMAIL_VERIFICATION.
* Tạo notification log.
* Đưa trạng thái ban đầu là PENDING.
* Trả response cho service gọi.

Sprint 1:

* Chưa gửi email thật.
* Chỉ tạo log và lưu trạng thái.

---

## 5.2 Endpoint Information

| Mục            | Nội dung                                   |
| :------------- | :----------------------------------------- |
| Method         | POST                                       |
| Endpoint       | /internal/notifications/email-verification |
| Content-Type   | application/json                           |
| Authentication | Internal Service Only                      |

---

## 5.3 Request Headers

```http
Content-Type: application/json
```

---

## 5.4 Request Body

```json
{
  "accountId": 1,
  "email": "user@gmail.com",
  "fullName": "Nguyen Van A",
  "verificationLink": "http://localhost:8080/api/auth/verify-email?token=abc123"
}
```

---

## 5.5 Request Field Description

| Field            | Type   | Required | Mô tả                        |
| ---------------- | ------ | -------- | ---------------------------- |
| accountId        | Long   | Yes      | ID tài khoản từ Auth Service |
| email            | String | Yes      | Email nhận thông báo         |
| fullName         | String | Yes      | Họ tên người dùng            |
| verificationLink | String | Yes      | Link xác thực email          |

---

# 6. Backend Processing Flow

```text
Receive Request
↓
Validate Request
↓
Find Template EMAIL_VERIFICATION
↓
Render Email Content
↓
Insert Notification Log
↓
Status = PENDING
↓
Return Response
```

---

# 7. Response Success

Status:

```http
200 OK
```

Response:

```json
{
  "success": true,
  "message": "Email verification notification accepted",
  "data": {
    "notificationId": 10,
    "status": "PENDING"
  }
}
```

---

## 7.1 Response Field Description

| Field               | Type    | Mô tả                   |
| ------------------- | ------- | ----------------------- |
| success             | Boolean | Kết quả xử lý           |
| message             | String  | Thông báo               |
| data.notificationId | Long    | ID notification log     |
| data.status         | String  | Trạng thái notification |

---

# 8. Error Cases

## Case 1 - Missing Email

Status:

```http
400 Bad Request
```

```json
{
  "success": false,
  "message": "Recipient email is required",
  "errorCode": "NOTIFICATION_EMAIL_REQUIRED"
}
```

---

## Case 2 - Invalid Email Format

Status:

```http
400 Bad Request
```

```json
{
  "success": false,
  "message": "Invalid email format",
  "errorCode": "NOTIFICATION_INVALID_EMAIL"
}
```

---

## Case 3 - Verification Link Missing

Status:

```http
400 Bad Request
```

```json
{
  "success": false,
  "message": "Verification link is required",
  "errorCode": "NOTIFICATION_LINK_REQUIRED"
}
```

---

## Case 4 - Template Not Found

Status:

```http
404 Not Found
```

```json
{
  "success": false,
  "message": "Notification template not found",
  "errorCode": "NOTIFICATION_TEMPLATE_NOT_FOUND"
}
```

---

## Case 5 - Internal Server Error

Status:

```http
500 Internal Server Error
```

```json
{
  "success": false,
  "message": "Internal server error",
  "errorCode": "INTERNAL_SERVER_ERROR"
}
```

---

# 9. Notification Status Lifecycle

## PENDING

Notification vừa được tạo.

Điều kiện:

* Request hợp lệ.
* Đã lưu notification_logs.
* Chưa gửi email.

Ví dụ:

```sql
status = 'PENDING'
```

---

## SENT

Notification đã gửi thành công.

Điều kiện:

* SMTP Provider trả về thành công.

Ví dụ:

```sql
status = 'SENT'
```

---

## FAILED

Notification gửi thất bại.

Điều kiện:

* SMTP lỗi.
* Timeout.
* Mail server từ chối.

Ví dụ:

```sql
status = 'FAILED'
error_message = 'SMTP connection timeout'
```

---

# 10. Database Mapping

## notification_templates

Template được sử dụng:

```text
EMAIL_VERIFICATION
```

Ví dụ content:

```html
Hello {name},

Please verify your account by clicking the link below:

{verificationLink}

Thank you.
```

---

## notification_logs

Ví dụ record được tạo:

```sql
id = 10
template_code = 'EMAIL_VERIFICATION'
user_id = 1
recipient = 'user@gmail.com'
channel_type = 'EMAIL'
status = 'PENDING'
```

---

# 11. Status Code Summary

| Status Code | Ý nghĩa               |
| ----------- | --------------------- |
| 200         | Notification accepted |
| 400         | Invalid request       |
| 404         | Template not found    |
| 500         | Internal server error |

---

# 12. Scope Chưa Bao Gồm

Các chức năng sau chưa thuộc Sprint 1:

* SMTP Integration
* Gmail Sender
* SendGrid Integration
* Retry Queue
* Kafka Notification Event
* SMS Notification
* Push Notification
* Batch Notification
* Notification Dashboard
* Email Tracking

Các chức năng trên sẽ được thực hiện ở Sprint sau.
