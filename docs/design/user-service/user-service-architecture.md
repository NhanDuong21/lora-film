# TÀI LIỆU KIẾN TRÚC USER SERVICE

**Mã tài liệu:** USER-ARCH-

---

# Mục lục

1. Giới thiệu
2. Phạm vi
3. Trách nhiệm của Service
4. Kiến trúc tổng thể
5. Module nghiệp vụ
6. Luồng xử lý

---

# 1. Giới thiệu

## 1.1 Mục đích

User Service chịu trách nhiệm quản lý toàn bộ thông tin người dùng trong hệ thống rạp chiếu phim.

Khác với Auth Service chỉ quản lý xác thực, User Service quản lý dữ liệu nghiệp vụ của người dùng sau khi tài khoản đã được xác thực.

Service này được thiết kế theo kiến trúc Microservice, sở hữu cơ sở dữ liệu riêng và chỉ giao tiếp với các service khác thông qua REST API hoặc Kafka Event.

---

# 2. Phạm vi

## Bao gồm

- Quản lý hồ sơ người dùng
- Quản lý khách hàng
- Quản lý nhân viên
- Quản lý phòng ban
- Quản lý chức vụ
- Quản lý bảng lương
- Quản lý ảnh đại diện

---

## Không bao gồm

Các nghiệp vụ sau thuộc service khác:

- Đăng nhập
- Đăng ký
- JWT
- OAuth2
- Refresh Token
- Quản lý phim
- Đặt vé
- Thanh toán
- Khuyến mãi
- Điểm thưởng

---

# 3. Trách nhiệm của User Service

User Service được chia thành 4 module chính.

## 3.1 Hồ sơ người dùng

Chức năng

- Xem thông tin cá nhân
- Cập nhật thông tin
- Thay đổi ảnh đại diện

---

## 3.2 Quản lý khách hàng

Chức năng

- Danh sách khách hàng
- Xem chi tiết
- Tìm kiếm
- Lọc theo trạng thái
- Khóa/Mở khóa hồ sơ

---

## 3.3 Quản lý nhân viên

Chức năng

- Danh sách nhân viên
- Hồ sơ nhân viên
- Chức vụ
- Phòng ban
- Trạng thái làm việc
- Ngày tuyển dụng

---

## 3.4 Quản lý bảng lương

Chức năng

- Tạo bảng lương
- Cập nhật bảng lương
- Phê duyệt bảng lương
- Xem lịch sử lương
- Thống kê lương

---

# 4. Kiến trúc tổng thể

```text
                   API Gateway
                        │
                        ▼
                 User Controller
                        │
                        ▼
                 Service Layer
                        │
      ┌─────────────────┼────────────────┐
      ▼                 ▼                ▼

CustomerService   EmployeeService   PayrollService

      │                 │                │

      └─────────────────┼────────────────┘
                        ▼
                 Repository Layer
                        ▼
                     MySQL
```

---

# 5. Các Module

## Module Customer

Chịu trách nhiệm quản lý khách hàng.

Bao gồm

- Danh sách
- Chi tiết
- Trạng thái
- Tìm kiếm

---

## Module Employee

Chịu trách nhiệm quản lý nhân sự.

Bao gồm

- Hồ sơ
- Phòng ban
- Chức vụ
- Trạng thái làm việc

---

## Module Payroll

Quản lý tiền lương.

Bao gồm

- Lương cơ bản
- Phụ cấp
- Thưởng
- Khấu trừ
- Tổng lương

---

## Module User Profile

Quản lý thông tin cá nhân.

Bao gồm

- Họ tên
- Avatar
- Số điện thoại
- Ngày sinh
- Giới tính

---

# 6. Luồng xử lý tổng quát

```text
Client

↓

Gateway

↓

JWT Validation

↓

User Controller

↓

Service

↓

Repository

↓

Database

↓

Response
```

---

# 7. Quy tắc thiết kế

User Service tuân theo các nguyên tắc sau:

- Một service chỉ chịu trách nhiệm một domain.
- Không truy cập trực tiếp database của service khác.
- Chỉ giao tiếp thông qua REST API hoặc Kafka.
- Tách biệt Controller, Service và Repository.
- Sử dụng DTO cho mọi request/response.
- Mọi thao tác cập nhật dữ liệu đều ghi log.
- Phân quyền theo Role.

---

# 8. Module Quản lý Khách hàng (Customer Management)

## 8.1 Mục đích

Module Customer Management chịu trách nhiệm quản lý toàn bộ thông tin khách hàng của hệ thống.

Lưu ý:

Module này **không thực hiện đăng ký hoặc đăng nhập**.

Việc tạo tài khoản do **Auth Service** đảm nhiệm.

Sau khi tài khoản được xác thực thành công, Auth Service sẽ phát sự kiện Kafka:

```
ACCOUNT_VERIFIED
```

User Service sẽ tiếp nhận sự kiện và tự động tạo hồ sơ khách hàng.

---

# 8.2 Chức năng

Khách hàng bao gồm các chức năng sau:

- Xem danh sách khách hàng
- Xem chi tiết khách hàng
- Tìm kiếm khách hàng
- Lọc khách hàng
- Cập nhật hồ sơ
- Khóa hồ sơ
- Mở khóa hồ sơ

---

# 8.3 Luồng tạo khách hàng

```text
Auth Service

↓

ACCOUNT_VERIFIED

↓

Kafka

↓

User Service

↓

Consumer

↓

Tạo Customer Profile

↓

Lưu Database
```

---

# 8.4 Trạng thái khách hàng

| Trạng thái | Ý nghĩa         |
| ---------- | --------------- |
| ACTIVE     | Đang hoạt động  |
| INACTIVE   | Không hoạt động |
| BLOCKED    | Bị khóa         |

---

# 8.5 Business Rules

| Mã          | Quy tắc                                   |
| ----------- | ----------------------------------------- |
| USER-BR-001 | Mỗi Account chỉ có một Customer Profile   |
| USER-BR-002 | Không được tạo Customer bằng API          |
| USER-BR-003 | Customer được tạo khi Account đã xác thực |
| USER-BR-004 | Admin có thể khóa Customer                |
| USER-BR-005 | Customer không được tự xóa hồ sơ          |

---

# 8.6 Sequence Diagram

```text
ACCOUNT_VERIFIED

↓

Kafka

↓

User Consumer

↓

Validate Event

↓

Create Customer Profile

↓

Database

↓

Success
```

---

# 8.7 API

## Danh sách khách hàng

```
GET /api/admin/customers
```

Quyền

```
ADMIN

MANAGER
```

---

## Chi tiết khách hàng

```
GET /api/admin/customers/{id}
```

---

## Tìm kiếm

```
GET /api/admin/customers/search
```

Query

```
keyword

status

page

size
```

---

## Khóa khách hàng

```
PUT /api/admin/customers/{id}/block
```

---

## Mở khóa

```
PUT /api/admin/customers/{id}/unblock
```

---

# 9. Module Quản lý Nhân viên

## 9.1 Mục đích

Module quản lý toàn bộ nhân sự trong hệ thống.

Bao gồm

- Nhân viên bán vé
- Nhân viên soát vé
- Quản lý rạp
- Admin

---

# 9.2 Chức năng

- Danh sách nhân viên

- Hồ sơ nhân viên

- Chức vụ

- Phòng ban

- Trạng thái làm việc

- Ngày tuyển dụng

- Nghỉ việc

---

# 9.3 Luồng tạo nhân viên

```text
Admin

↓

Create Employee

↓

Validate

↓

Create Employee Profile

↓

Database

↓

Publish

EMPLOYEE_CREATED
```

---

# 9.4 Trạng thái nhân viên

| Trạng thái | Ý nghĩa        |
| ---------- | -------------- |
| ACTIVE     | Đang làm việc  |
| ON_LEAVE   | Đang nghỉ phép |
| SUSPENDED  | Tạm đình chỉ   |
| RESIGNED   | Đã nghỉ việc   |

---

# 9.5 Chức vụ

```text
ADMIN

MANAGER

CASHIER

TICKET_CHECKER

STAFF
```

---

# 9.6 Phòng ban

```text
Administration

Ticket Counter

Cinema Operation

Finance

Human Resource
```

---

# 9.7 Business Rules

| Mã          | Quy tắc                                                            |
| ----------- | ------------------------------------------------------------------ |
| USER-BR-006 | Mỗi Employee chỉ thuộc một Department                              |
| USER-BR-007 | Một Employee chỉ có một Position tại một thời điểm                 |
| USER-BR-008 | Employee nghỉ việc không được đăng nhập nếu Account bị vô hiệu hóa |
| USER-BR-009 | Chỉ ADMIN hoặc MANAGER được tạo nhân viên                          |
| USER-BR-010 | Không được xóa cứng hồ sơ nhân viên                                |

---

# 9.8 API

## Danh sách nhân viên

```
GET /api/admin/employees
```

---

## Chi tiết

```
GET /api/admin/employees/{id}
```

---

## Tạo

```
POST /api/admin/employees
```

---

## Cập nhật

```
PUT /api/admin/employees/{id}
```

---

## Nghỉ việc

```
PUT /api/admin/employees/{id}/resign
```

---

## Đình chỉ

```
PUT /api/admin/employees/{id}/suspend
```

---

# 10. Sequence Diagram

```text
Admin

↓

POST Employee

↓

Controller

↓

Employee Service

↓

Validation

↓

Repository

↓

Database

↓

Publish EMPLOYEE_CREATED

↓

Return Success
```

---

# 11. Validation

## Customer

- Không được trùng accountId
- Email chỉ đọc (lấy từ Auth Service)
- Trạng thái mặc định ACTIVE

---

## Employee

- Department bắt buộc
- Position bắt buộc
- Hire Date bắt buộc
- Salary phải lớn hơn 0
- Không được tạo Employee nếu Account chưa ACTIVE

---

# 12. Module Quản lý Bảng lương (Payroll Management)

## 12.1 Mục đích

Module Payroll chịu trách nhiệm quản lý bảng lương của nhân viên trong hệ thống rạp chiếu phim.

Module này chỉ phục vụ nghiệp vụ nội bộ và không liên quan đến khách hàng.

Việc tính lương được thực hiện theo từng kỳ (tháng).

---

# 12.2 Chức năng

Bao gồm

- Tạo bảng lương
- Cập nhật bảng lương
- Phê duyệt bảng lương
- Xem bảng lương
- Xem lịch sử lương
- Xuất bảng lương

---

# 12.3 Thành phần lương

```text
Lương tháng

=

Lương cơ bản

+

Phụ cấp

+

Thưởng

-

Khấu trừ
```

---

# 12.4 Trạng thái bảng lương

| Trạng thái       | Ý nghĩa       |
| ---------------- | ------------- |
| DRAFT            | Đang tạo      |
| PENDING_APPROVAL | Chờ duyệt     |
| APPROVED         | Đã duyệt      |
| PAID             | Đã thanh toán |
| CANCELLED        | Đã hủy        |

---

# 12.5 Business Rules

| Mã          | Nội dung                                            |
| ----------- | --------------------------------------------------- |
| USER-BR-011 | Một nhân viên chỉ có một bảng lương trong một tháng |
| USER-BR-012 | Không được chỉnh sửa bảng lương đã APPROVED         |
| USER-BR-013 | Không được xóa bảng lương                           |
| USER-BR-014 | Chỉ ADMIN hoặc MANAGER được phê duyệt               |
| USER-BR-015 | Tổng lương phải lớn hơn hoặc bằng 0                 |

---

# 12.6 Luồng xử lý

```text
Manager

↓

Tạo Payroll

↓

Kiểm tra dữ liệu

↓

Tính tổng lương

↓

Lưu Database

↓

PENDING_APPROVAL
```

---

# 12.7 Phê duyệt

```text
Manager

↓

Approve Payroll

↓

Update Status

↓

APPROVED

↓

Publish

PAYROLL_APPROVED
```

---

# 12.8 API

## Danh sách bảng lương

```
GET /api/admin/payrolls
```

---

## Chi tiết

```
GET /api/admin/payrolls/{id}
```

---

## Tạo

```
POST /api/admin/payrolls
```

---

## Cập nhật

```
PUT /api/admin/payrolls/{id}
```

---

## Phê duyệt

```
PUT /api/admin/payrolls/{id}/approve
```

---

## Thanh toán

```
PUT /api/admin/payrolls/{id}/paid
```

---

# 13. Module Hồ sơ cá nhân (Profile)

## Mục đích

Cho phép người dùng quản lý thông tin cá nhân.

Lưu ý

Email và mật khẩu được quản lý bởi Auth Service.

User Service chỉ quản lý dữ liệu hồ sơ.

---

## Chức năng

- Xem hồ sơ
- Cập nhật thông tin
- Cập nhật avatar
- Cập nhật số điện thoại
- Cập nhật ngày sinh
- Cập nhật giới tính

---

## API

### Xem hồ sơ

```
GET /api/profile
```

---

### Cập nhật

```
PUT /api/profile
```

---

### Upload Avatar

```
POST /api/profile/avatar
```

---

# 14. Dashboard

User Service cung cấp Dashboard cho Admin.

---

## Dashboard Khách hàng

Bao gồm

- Tổng số khách hàng

- Khách hàng mới

- Khách hàng đang hoạt động

- Khách hàng bị khóa

---

API

```
GET /api/admin/dashboard/customers
```

---

## Dashboard Nhân viên

Bao gồm

- Tổng số nhân viên

- Theo phòng ban

- Theo chức vụ

- Đang làm việc

- Nghỉ việc

---

API

```
GET /api/admin/dashboard/employees
```

---

## Dashboard Payroll

Bao gồm

- Tổng bảng lương

- Chờ duyệt

- Đã duyệt

- Đã thanh toán

- Tổng chi phí lương

---

API

```
GET /api/admin/dashboard/payrolls
```

---

# 15. Sequence Diagram

## Cập nhật hồ sơ

```text
User

↓

PUT /profile

↓

Gateway

↓

User Controller

↓

Profile Service

↓

Validation

↓

Repository

↓

Database

↓

Success
```

---

## Tạo Payroll

```text
Manager

↓

POST Payroll

↓

Payroll Service

↓

Validation

↓

Calculate Salary

↓

Repository

↓

Database

↓

Publish PAYROLL_CREATED
```

---

# 16. Validation

## Hồ sơ

| Trường     | Quy tắc                     |
| ---------- | --------------------------- |
| Full Name  | Bắt buộc                    |
| Phone      | Định dạng hợp lệ            |
| Birth Date | Không lớn hơn ngày hiện tại |
| Avatar     | Đúng định dạng ảnh          |

---

## Payroll

| Trường       | Quy tắc |
| ------------ | ------- |
| Basic Salary | > 0     |
| Allowance    | >=0     |
| Bonus        | >=0     |
| Deduction    | >=0     |

---

# 17. Phân quyền

| API             | CUSTOMER | EMPLOYEE | MANAGER | ADMIN |
| --------------- | -------- | -------- | ------- | ----- |
| GET /profile    | ✔        | ✔        | ✔       | ✔     |
| PUT /profile    | ✔        | ✔        | ✔       | ✔     |
| GET Customers   | ❌       | ❌       | ✔       | ✔     |
| GET Employees   | ❌       | ❌       | ✔       | ✔     |
| Create Employee | ❌       | ❌       | ✔       | ✔     |
| Create Payroll  | ❌       | ❌       | ✔       | ✔     |
| Approve Payroll | ❌       | ❌       | ✔       | ✔     |

---

# 18. Kafka Events

## Consumer

```text
ACCOUNT_VERIFIED

ACCOUNT_DISABLED

ACCOUNT_LOCKED

ACCOUNT_DELETED
```

---

## Producer

```text
CUSTOMER_CREATED

EMPLOYEE_CREATED

EMPLOYEE_UPDATED

PAYROLL_CREATED

PAYROLL_APPROVED

PAYROLL_PAID
```

---

# 20. Thiết kế Cơ sở dữ liệu (Database Design)

## 20.1 Tổng quan

User Service sở hữu cơ sở dữ liệu riêng:

```
user_db
```

Database này **không lưu thông tin xác thực** (email đăng nhập, mật khẩu, JWT, Refresh Token).

Các thông tin đó thuộc **Auth Service**.

User Service chỉ lưu dữ liệu nghiệp vụ.

---

# 21. Database Schema

```text
user_db

├── users
├── customer_profiles
├── employee_profiles
├── departments
├── positions
├── payrolls
├── payroll_details
└── avatars
```

---

# 22. Entity Relationship Diagram (ERD)

```text
                    AUTH SERVICE

                    accounts
                       │
                 account_id
                       │
======================= Kafka =======================

                       │
                       ▼

                 +-------------+
                 |    users    |
                 +-------------+
                       │
          ┌────────────┴────────────┐
          ▼                         ▼

customer_profiles          employee_profiles
                                   │
                    ┌──────────────┴─────────────┐
                    ▼                            ▼
               departments                  positions
                    │
                    └──────────────┐
                                   ▼
                              payrolls
                                   │
                                   ▼
                           payroll_details
```

---

# 23. Bảng users

Đây là bảng trung tâm của User Service.

Mỗi Account từ Auth Service sẽ tương ứng với đúng một User.

---

## Chức năng

Lưu thông tin chung.

Không phân biệt Customer hay Employee.

---

## Cấu trúc

| Cột        | Kiểu         | Ràng buộc |
| ---------- | ------------ | --------- |
| id         | BIGINT       | PK        |
| account_id | BIGINT       | UNIQUE    |
| full_name  | VARCHAR(150) | NOT NULL  |
| phone      | VARCHAR(20)  |           |
| gender     | ENUM         |           |
| birth_date | DATE         |           |
| avatar_url | VARCHAR(255) |           |
| status     | ENUM         |           |
| created_at | TIMESTAMP    |           |
| updated_at | TIMESTAMP    |           |

---

## Trạng thái

```text
ACTIVE

INACTIVE

BLOCKED
```

---

# 24. customer_profiles

Chỉ tồn tại nếu User là khách hàng.

---

## Cấu trúc

| Cột           | Kiểu        |
| ------------- | ----------- |
| id            | BIGINT      |
| user_id       | FK          |
| customer_code | VARCHAR(30) |
| joined_at     | DATE        |
| note          | TEXT        |

---

## Business Rules

- Một User chỉ có tối đa một Customer Profile.

- Không được tạo Customer Profile thủ công.

- Chỉ tạo thông qua Kafka Event ACCOUNT_VERIFIED.

---

# 25. employee_profiles

Thông tin nhân sự.

---

## Cấu trúc

| Cột           | Kiểu        |
| ------------- | ----------- |
| id            | BIGINT      |
| user_id       | FK          |
| employee_code | VARCHAR(30) |
| department_id | FK          |
| position_id   | FK          |
| hire_date     | DATE        |
| status        | ENUM        |

---

## Trạng thái

```text
ONBOARDING

ACTIVE

ON_LEAVE

SUSPENDED

RESIGNED

CANCELLED
```

`ONBOARDING` bắt đầu khi quản trị viên hoàn tất hồ sơ và gửi lời mời. Hồ sơ chỉ chuyển sang
`ACTIVE` sau khi nhân viên tự đặt mật khẩu và kích hoạt tài khoản. Lời mời hết hạn không làm
mất hồ sơ. Khi hủy tiếp nhận, hồ sơ chuyển sang `CANCELLED`; thao tác mở lại tái sử dụng cùng
tài khoản và hồ sơ, đồng thời phát hành lời mời mới.

---

# 26. departments

Danh mục phòng ban.

---

## Cấu trúc

| Cột  | Kiểu         |
| ---- | ------------ |
| id   | BIGINT       |
| code | VARCHAR(20)  |
| name | VARCHAR(100) |

---

## Dữ liệu mặc định

```text
HR

FINANCE

OPERATION

TICKET

ADMINISTRATION
```

---

# 27. positions

Danh mục chức vụ.

---

## Cấu trúc

| Cột  | Kiểu         |
| ---- | ------------ |
| id   | BIGINT       |
| code | VARCHAR(30)  |
| name | VARCHAR(100) |

---

## Dữ liệu mặc định

```text
ADMIN

MANAGER

SUPERVISOR

CASHIER

TICKET_CHECKER

STAFF
```

---

# 28. payrolls

Lưu bảng lương.

---

## Cấu trúc

| Cột          | Kiểu    |
| ------------ | ------- |
| id           | BIGINT  |
| employee_id  | FK      |
| salary_month | DATE    |
| basic_salary | DECIMAL |
| allowance    | DECIMAL |
| bonus        | DECIMAL |
| deduction    | DECIMAL |
| total_salary | DECIMAL |
| status       | ENUM    |

---

## Status

```text
DRAFT

PENDING_APPROVAL

APPROVED

PAID
```

---

# 29. payroll_details

Chi tiết từng khoản.

Ví dụ

```text
Thưởng KPI

+

Thưởng doanh thu

-

Khấu trừ

+

Phụ cấp ăn trưa
```

---

## Cấu trúc

| Cột         | Kiểu         |
| ----------- | ------------ |
| id          | BIGINT       |
| payroll_id  | FK           |
| type        | ENUM         |
| description | VARCHAR(255) |
| amount      | DECIMAL      |

---

## TYPE

```text
ALLOWANCE

BONUS

DEDUCTION
```

---

# 30. avatars

Quản lý lịch sử ảnh đại diện.

---

| Cột         | Kiểu         |
| ----------- | ------------ |
| id          | BIGINT       |
| user_id     | FK           |
| file_name   | VARCHAR(255) |
| file_url    | VARCHAR(255) |
| uploaded_at | TIMESTAMP    |

---

# 31. Quan hệ giữa các bảng

```text
users.id

↓

customer_profiles.user_id

One-to-One
```

---

```text
users.id

↓

employee_profiles.user_id

One-to-One
```

---

```text
departments.id

↓

employee_profiles.department_id

One-to-Many
```

---

```text
positions.id

↓

employee_profiles.position_id

One-to-Many
```

---

```text
employee_profiles.id

↓

payrolls.employee_id

One-to-Many
```

---

```text
payrolls.id

↓

payroll_details.payroll_id

One-to-Many
```

---

# 32. Constraint

## Unique

```text
users.account_id

customer_profiles.user_id

employee_profiles.user_id

departments.code

positions.code

employee_profiles.employee_code

customer_profiles.customer_code
```

---

## Foreign Key

```text
customer_profiles.user_id

→ users.id
```

```text
employee_profiles.user_id

→ users.id
```

```text
employee_profiles.department_id

→ departments.id
```

```text
employee_profiles.position_id

→ positions.id
```

```text
payrolls.employee_id

→ employee_profiles.id
```

```text
payroll_details.payroll_id

→ payrolls.id
```

---

# 33. Chỉ mục (Index)

```sql
idx_users_account_id

idx_employee_code

idx_customer_code

idx_department

idx_position

idx_salary_month

idx_payroll_employee
```

---

# 34. Audit Fields

Tất cả các bảng đều có:

```sql
created_at

created_by

updated_at

updated_by
```

Riêng bảng `payrolls` có thêm:

```sql
approved_by

approved_at

paid_at
```

---

# 35. Thiết kế REST API

## 35.1 Tổng quan

Base URL

```
/api/users
```

Tất cả API đều yêu cầu JWT từ Auth Service.

```
Authorization: Bearer <access_token>
```

---

# 36. Nhóm API Hồ sơ cá nhân

## Xem hồ sơ

```
GET /api/users/profile
```

Quyền

```
CUSTOMER
EMPLOYEE
MANAGER
ADMIN
```

---

## Cập nhật hồ sơ

```
PUT /api/users/profile
```

---

## Upload Avatar

```
POST /api/users/profile/avatar
```

---

## Xóa Avatar

```
DELETE /api/users/profile/avatar
```

---

# 37. Nhóm API Khách hàng

## Danh sách khách hàng

```
GET /api/users/customers
```

---

## Chi tiết khách hàng

```
GET /api/users/customers/{id}
```

---

## Tìm kiếm khách hàng

```
GET /api/users/customers/search
```

Query Parameter

```
keyword

status

page

size
```

---

## Khóa khách hàng

```
PUT /api/users/customers/{id}/block
```

---

## Mở khóa khách hàng

```
PUT /api/users/customers/{id}/unblock
```

---

# 38. Nhóm API Nhân viên

## Danh sách nhân viên

```
GET /api/users/employees
```

---

## Chi tiết nhân viên

```
GET /api/users/employees/{id}
```

---

## Thêm nhân viên

```
POST /api/users/employees
```

---

## Cập nhật nhân viên

```
PUT /api/users/employees/{id}
```

---

## Nghỉ việc

```
PUT /api/users/employees/{id}/resign
```

---

## Đình chỉ

```
PUT /api/users/employees/{id}/suspend
```

---

# 39. Nhóm API Phòng ban

## Danh sách

```
GET /api/users/departments
```

---

## Thêm

```
POST /api/users/departments
```

---

## Cập nhật

```
PUT /api/users/departments/{id}
```

---

## Xóa

```
DELETE /api/users/departments/{id}
```

---

# 40. Nhóm API Chức vụ

## Danh sách

```
GET /api/users/positions
```

---

## Thêm

```
POST /api/users/positions
```

---

## Cập nhật

```
PUT /api/users/positions/{id}
```

---

## Xóa

```
DELETE /api/users/positions/{id}
```

---

# 41. Nhóm API Payroll

## Danh sách bảng lương

```
GET /api/users/payrolls
```

---

## Chi tiết bảng lương

```
GET /api/users/payrolls/{id}
```

---

## Tạo bảng lương

```
POST /api/users/payrolls
```

---

## Cập nhật

```
PUT /api/users/payrolls/{id}
```

---

## Phê duyệt

```
PUT /api/users/payrolls/{id}/approve
```

---

## Đánh dấu đã thanh toán

```
PUT /api/users/payrolls/{id}/paid
```

---

# 42. Request DTO

## Update Profile Request

```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0988888888",
  "gender": "MALE",
  "birthDate": "2004-06-24"
}
```

---

## Create Employee Request

```json
{
  "accountId": 15,
  "departmentId": 2,
  "positionId": 4,
  "hireDate": "2026-07-26"
}
```

---

## Create Payroll Request

```json
{
  "employeeId": 5,
  "salaryMonth": "2026-07",
  "basicSalary": 12000000,
  "allowance": 1500000,
  "bonus": 1000000,
  "deduction": 200000
}
```

---

# 43. Response DTO

## User Profile Response

```json
{
  "id": 10,
  "accountId": 15,
  "fullName": "Nguyễn Văn A",
  "phone": "0988888888",
  "gender": "MALE",
  "birthDate": "2004-06-24",
  "avatarUrl": "/uploads/avatar.png",
  "status": "ACTIVE"
}
```

---

## Employee Response

```json
{
  "employeeCode": "EMP0001",
  "department": "Operation",
  "position": "Cashier",
  "status": "ACTIVE"
}
```

---

## Payroll Response

```json
{
  "salaryMonth": "2026-07",
  "basicSalary": 12000000,
  "allowance": 1500000,
  "bonus": 1000000,
  "deduction": 200000,
  "totalSalary": 14300000,
  "status": "PENDING_APPROVAL"
}
```

---

# 44. Chuẩn Response

## Thành công

```json
{
  "success": true,
  "message": "Thành công",
  "data": {}
}
```

---

## Thất bại

```json
{
  "success": false,
  "code": "USER_001",
  "message": "Không tìm thấy nhân viên"
}
```

---

# 45. Mã lỗi

| Mã       | Ý nghĩa                   |
| -------- | ------------------------- |
| USER_001 | Không tìm thấy User       |
| USER_002 | Không tìm thấy Customer   |
| USER_003 | Không tìm thấy Employee   |
| USER_004 | Không tìm thấy Department |
| USER_005 | Không tìm thấy Position   |
| USER_006 | Không tìm thấy Payroll    |
| USER_007 | Account đã tồn tại        |
| USER_008 | Dữ liệu không hợp lệ      |
| USER_009 | Không đủ quyền            |
| USER_010 | Không thể cập nhật        |

---

# 46. Validation Rule

## User

| Trường     | Quy tắc                     |
| ---------- | --------------------------- |
| Full Name  | Bắt buộc, tối đa 150 ký tự  |
| Phone      | 10–15 số                    |
| Birth Date | Không lớn hơn ngày hiện tại |

---

## Employee

| Trường     | Quy tắc  |
| ---------- | -------- |
| Department | Bắt buộc |
| Position   | Bắt buộc |
| Hire Date  | Bắt buộc |

---

## Payroll

| Trường       | Quy tắc |
| ------------ | ------- |
| Basic Salary | > 0     |
| Allowance    | >= 0    |
| Bonus        | >= 0    |
| Deduction    | >= 0    |

---

# 47. Ma trận phân quyền

| API               | CUSTOMER | EMPLOYEE | MANAGER | ADMIN |
| ----------------- | :------: | :------: | :-----: | :---: |
| GET /profile      |    ✅    |    ✅    |   ✅    |  ✅   |
| PUT /profile      |    ✅    |    ✅    |   ✅    |  ✅   |
| GET Customers     |    ❌    |    ❌    |   ✅    |  ✅   |
| Block Customer    |    ❌    |    ❌    |   ❌    |  ✅   |
| Create Employee   |    ❌    |    ❌    |   ✅    |  ✅   |
| Update Employee   |    ❌    |    ❌    |   ✅    |  ✅   |
| Approve Payroll   |    ❌    |    ❌    |   ✅    |  ✅   |
| Manage Department |    ❌    |    ❌    |   ❌    |  ✅   |
| Manage Position   |    ❌    |    ❌    |   ❌    |  ✅   |

---

# 48. OpenAPI

User Service cung cấp tài liệu API thông qua Swagger.

```
/swagger-ui/index.html
```

OpenAPI Specification

```
/v3/api-docs
```

---

# 49. Kiến trúc Package

## 49.1 Cấu trúc thư mục

```text
user-service/
├── src/main/java/com/project/userservice/
│   ├── client/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── enumtype/
│   ├── exception/
│   ├── kafka/
│   ├── mapper/
│   ├── repository/
│   ├── scheduler/
│   ├── security/
│   ├── service/
│   └── util/
├── src/main/resources/
│   ├── application.example.properties
│   └── application.properties          # local, Git ignored
└── pom.xml
```

Schema được quản lý thủ công tại
`docs/database/mysql/user-service-schema.sql`; service không dùng Flyway hay
Liquibase và Hibernate chỉ chạy `ddl-auto=validate`.

---

# 50. Chức năng từng Package

## controller

Chịu trách nhiệm tiếp nhận HTTP Request.

Ví dụ

```text
ProfileController

CustomerController

EmployeeController

DepartmentController

PositionController

PayrollController
```

---

## service

Xử lý nghiệp vụ.

```text
ProfileService

CustomerService

EmployeeService

DepartmentService

PayrollService
```

---

## repository

Tầng truy cập dữ liệu.

```text
UserRepository

EmployeeRepository

PayrollRepository
```

---

## entity

Các Entity ánh xạ Database.

```text
User

CustomerProfile

EmployeeProfile

Department

Position

Payroll

PayrollDetail
```

---

## dto

Bao gồm

- Request DTO
- Response DTO
- Event DTO

---

## mapper

Chuyển đổi

```
Entity

↓

DTO
```

Khuyến nghị

```
MapStruct
```

---

# 51. Kafka Event

## Consumer

User Service tiếp nhận các sự kiện từ Auth Service.

```text
ACCOUNT_CREATED

ACCOUNT_VERIFIED

ACCOUNT_DISABLED

ACCOUNT_LOCKED

ACCOUNT_DELETED
```

---

## Luồng xử lý

```text
Kafka

↓

Consumer

↓

Validate

↓

Business Logic

↓

Database

↓

ACK
```

---

## Producer

Các Event phát ra

```text
CUSTOMER_CREATED

CUSTOMER_BLOCKED

EMPLOYEE_CREATED

EMPLOYEE_UPDATED

EMPLOYEE_RESIGNED

PAYROLL_CREATED

PAYROLL_APPROVED

PAYROLL_PAID
```

---

## Ví dụ Event

```json
{
  "eventId": "uuid",
  "eventType": "EMPLOYEE_CREATED",
  "occurredAt": "2026-07-26T10:30:00",
  "employeeId": 15
}
```

---

# 52. Cache

Redis chỉ sử dụng để cache dữ liệu thường xuyên truy cập.

Ví dụ

```text
Department List

Position List

Dashboard Summary
```

Không cache

- Payroll
- Employee Profile
- Customer Profile

---

# 53. Logging

Mọi Request đều ghi log.

Thông tin ghi nhận

- Request Id
- User Id
- Endpoint
- Execution Time
- HTTP Status

Không ghi

- JWT
- Password
- Thông tin nhạy cảm

---

# 54. Monitoring

Các chỉ số cần theo dõi

```text
Tổng số Customer

Tổng số Employee

Payroll đã tạo

Payroll chờ duyệt

Payroll đã thanh toán

Kafka Consumer Lag

Database Connection Pool

Redis Hit Rate

Average Response Time
```

---

# 55. Cấu hình triển khai

Biến môi trường

```text
MYSQL_HOST

MYSQL_PORT

MYSQL_DATABASE

MYSQL_USERNAME

MYSQL_PASSWORD

KAFKA_BOOTSTRAP_SERVERS

REDIS_HOST

REDIS_PORT

UPLOAD_DIRECTORY
```

---

# 56. Nguyên tắc lập trình

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- Constructor Injection
- DTO Pattern
- Service Pattern
- Repository Pattern
- Global Exception Handler
- Bean Validation
- OpenAPI 3

---

# 57. Khả năng mở rộng

User Service được thiết kế theo hướng Stateless.

Có thể mở rộng theo chiều ngang.

```text
                API Gateway
                      │
      ┌───────────────┼───────────────┐
      ▼               ▼               ▼
 User-1          User-2          User-3
      │               │               │
      └───────────────┼───────────────┘
                      ▼
                    MySQL
                      │
                    Redis
                      │
                    Kafka
```

---

# 58. Hướng phát triển

Trong các phiên bản tiếp theo có thể bổ sung:

### Quản lý ca làm

- Lịch làm việc
- Đổi ca
- Chấm công

---

### Hồ sơ đào tạo

- Khóa học
- Chứng chỉ
- Đánh giá năng lực

---

### Quản lý hợp đồng

- Hợp đồng thử việc
- Hợp đồng chính thức
- Gia hạn hợp đồng

---

### Dashboard HR

- Biến động nhân sự
- Thống kê nghỉ việc
- Hiệu suất phòng ban

---

### Dashboard CRM

- Khách hàng mới
- Khách hàng quay lại
- Khách hàng VIP
- Tần suất sử dụng dịch vụ

---

# 59. Best Practices

- Không truy cập trực tiếp database của Auth Service.
- Đồng bộ dữ liệu giữa các service thông qua Kafka Event.
- Mọi API phải xác thực JWT trước khi xử lý.
- Không lưu thông tin xác thực (mật khẩu, JWT, Refresh Token) trong User Service.
- Chỉ sử dụng `account_id` làm khóa liên kết với Auth Service.
- Mọi thao tác cập nhật dữ liệu cần ghi Audit Log.

---

# 60. Kết luận

User Service là service chịu trách nhiệm quản lý toàn bộ dữ liệu nghiệp vụ liên quan đến con người trong hệ thống Cinema.

Phạm vi bao gồm:

- Quản lý hồ sơ người dùng
- Quản lý khách hàng
- Quản lý nhân viên
- Quản lý phòng ban
- Quản lý chức vụ
- Quản lý bảng lương
- Dashboard nhân sự
- Dashboard khách hàng

User Service không xử lý:

- Đăng nhập
- Đăng ký
- JWT
- OAuth2
- Phân quyền
- Refresh Token

Các chức năng trên thuộc **Auth Service**.

Việc tách biệt Auth Service và User Service giúp:

- Độc lập dữ liệu
- Dễ mở rộng
- Dễ bảo trì
- Tăng khả năng mở rộng theo mô hình Microservice
- Phù hợp với kiến trúc Production của hệ thống quản lý rạp chiếu phim.

---
