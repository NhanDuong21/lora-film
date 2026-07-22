# BOOKING SERVICE DEMONSTRATION DOCUMENT

**Project:** Cinema Booking System
**Module:** Booking Service
**Version:** 1.0
**Architecture:** Microservice Architecture
**Pattern:** Domain Driven Design (DDD) + Event Driven Architecture
**Technology Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, MySQL 8, Redis, Kafka (Future), Docker

---

# Table of Contents

1. Introduction
2. Business Overview
3. Booking Business Flow
4. Domain Driven Design
5. System Architecture
6. Database Design
7. Aggregate Design
8. Booking Lifecycle
9. Seat Reservation Lifecycle
10. Payment Integration
11. Event Driven Architecture
12. API Specification
13. Validation Rules
14. Business Rules
15. Security
16. Exception Handling
17. Logging
18. Scheduler
19. Redis Lock
20. Outbox Pattern
21. Inbox Pattern
22. Retry Strategy
23. Dead Letter Queue
24. Reconciliation
25. Idempotency
26. Performance Optimization
27. Sequence Diagrams
28. Deployment
29. Demonstration Scenario
30. Testing Scenario
31. Future Improvements

---

# 1. Introduction

## 1.1 Purpose

Booking Service là một trong những microservice quan trọng nhất của hệ thống Cinema Booking.

Service này chịu trách nhiệm quản lý toàn bộ vòng đời của một đơn đặt vé, từ thời điểm người dùng xác nhận giữ ghế cho đến khi booking được thanh toán, hoàn thành hoặc bị hủy.

Booking Service được xây dựng theo kiến trúc **Microservice** kết hợp với **Domain Driven Design (DDD)** nhằm đảm bảo khả năng mở rộng, dễ bảo trì và dễ tích hợp với các service khác trong hệ thống.

---

## 1.2 Objectives

Booking Service được xây dựng với các mục tiêu sau:

- Quản lý toàn bộ Booking của khách hàng.
- Quản lý Ticket.
- Quản lý Snapshot dữ liệu tại thời điểm đặt vé.
- Quản lý Booking Status.
- Lưu lịch sử thay đổi trạng thái.
- Hỗ trợ Event Driven Architecture.
- Hỗ trợ tích hợp Payment Service.
- Hỗ trợ Retry và Reconciliation.
- Đảm bảo dữ liệu nhất quán trong môi trường Microservice.

---

## 1.3 Responsibilities

Booking Service chịu trách nhiệm:

- Tạo Booking.
- Hủy Booking.
- Xác nhận Booking.
- Hoàn tiền Booking.
- Quản lý Ticket.
- Ghi Booking History.
- Ghi Audit Log.
- Ghi Operation Log.
- Tạo Outbox Event.
- Nhận Payment Event.

---

## 1.4 Out of Scope

Booking Service **không chịu trách nhiệm**:

- Thanh toán trực tiếp.
- Quản lý người dùng.
- Quản lý phim.
- Quản lý rạp.
- Quản lý suất chiếu.
- Quản lý ghế.
- Tính toán giá vé theo thời gian thực.
- Gửi Email hoặc Notification.

Các nghiệp vụ trên thuộc các microservice khác.

---

# 2. Business Overview

## 2.1 Business Problem

Trong hệ thống bán vé xem phim, việc nhiều người cùng chọn một ghế tại cùng thời điểm là vấn đề phổ biến.

Nếu không có cơ chế quản lý phù hợp sẽ dẫn đến:

- Double Booking.
- Mất đồng bộ dữ liệu.
- Thanh toán thành công nhưng không còn ghế.
- Hai khách hàng cùng sở hữu một vé.

Booking Service được thiết kế để loại bỏ các vấn đề trên bằng cách tách biệt rõ trách nhiệm giữa các service và quản lý trạng thái Booking theo vòng đời rõ ràng.

---

## 2.2 Position in System

Booking Service là trung tâm kết nối giữa các dịch vụ khác.

```text
                    +----------------------+
                    |    Auth Service      |
                    +----------+-----------+
                               |
                               |
+--------------+      +--------v---------+      +----------------+
| Movie Service+----->| Booking Service  |<-----+ Payment Service|
+--------------+      +--------+---------+      +----------------+
                               |
                               |
                  +------------v------------+
                  | Notification Service    |
                  +-------------------------+
```

Booking Service không lưu dữ liệu của Movie Service hay User Service mà chỉ lưu các khóa tham chiếu và Snapshot cần thiết.

---

## 2.3 Business Responsibilities

Booking Service quản lý:

### Booking

Thông tin đơn đặt vé.

Bao gồm:

- Booking Code.
- User ID.
- Showtime ID.
- Total Amount.
- Booking Status.
- Payment Deadline.

---

### Ticket

Danh sách vé thuộc Booking.

Một Booking có thể chứa nhiều Ticket.

---

### Booking Snapshot

Snapshot được tạo ngay khi Booking được sinh ra.

Snapshot bao gồm:

- Movie.
- Cinema.
- Auditorium.
- Showtime.
- Seat.
- Promotion.
- Pricing.

Snapshot không được cập nhật sau khi Booking được tạo nhằm đảm bảo tính toàn vẹn dữ liệu.

---

### Booking History

Mỗi lần Booking thay đổi trạng thái sẽ sinh thêm một bản ghi lịch sử.

Ví dụ:

```text
PENDING_PAYMENT

↓

CONFIRMED

↓

COMPLETED
```

Sẽ có ba bản ghi trong `booking_status_histories`.

---

### Audit Log

Mọi thay đổi quan trọng đều được ghi nhận:

- Ai thực hiện.
- Thực hiện lúc nào.
- Giá trị cũ.
- Giá trị mới.
- Hành động.

Audit Log phục vụ truy vết và kiểm toán.

---

### Operation Log

Ghi nhận các thao tác nghiệp vụ:

- CREATE_BOOKING.
- CANCEL_BOOKING.
- CONFIRM_BOOKING.
- REFUND_BOOKING.

Operation Log phục vụ theo dõi vận hành hệ thống.

---

## 2.4 Business Goals

Booking Service cần đáp ứng các tiêu chí sau:

- Không tạo Booking trùng.
- Không tạo Ticket trùng.
- Không làm mất dữ liệu.
- Không phụ thuộc Payment Gateway.
- Có thể mở rộng sang nhiều cổng thanh toán.
- Có khả năng phục hồi sau lỗi.
- Có khả năng tích hợp Event Driven.

---

# 3. Booking Business Flow

## 3.1 High Level Flow

Quy trình tổng quát của nghiệp vụ đặt vé:

```text
User

↓

Đăng nhập

↓

Chọn phim

↓

Chọn rạp

↓

Chọn suất chiếu

↓

Chọn ghế

↓

Seat Reservation Service giữ ghế

↓

Booking Service tạo Booking

↓

Payment Service thanh toán

↓

Booking Service xác nhận Booking

↓

Notification Service gửi thông báo

↓

Khách hàng nhận vé điện tử
```

---

## 3.2 Detailed Business Flow

### Bước 1 — Chọn ghế

Người dùng chọn một hoặc nhiều ghế trên giao diện.

Frontend gửi yêu cầu đến Seat Reservation Service để giữ ghế.

Nếu giữ ghế thành công, hệ thống trả về:

- Reservation ID.
- Expired Time.

Booking Service chưa được gọi ở bước này.

---

### Bước 2 — Tạo Booking

Frontend gửi:

- Showtime ID.
- Reservation IDs.
- Promotion Code (nếu có).

Booking Service kiểm tra:

- User hợp lệ.
- Showtime hợp lệ.
- Reservation còn hiệu lực.
- Reservation thuộc đúng người dùng.
- Booking chưa tồn tại.

Nếu hợp lệ:

- Sinh Booking Code.
- Tạo Booking.
- Tạo Ticket.
- Lưu Snapshot.
- Ghi History.
- Ghi Audit.
- Ghi Operation.
- Ghi Outbox Event.

Booking được tạo với trạng thái:

```text
PENDING_PAYMENT
```

---

### Bước 3 — Thanh toán

Booking Service không xử lý thanh toán.

Frontend chuyển người dùng sang Payment Service.

Payment Service thực hiện:

- Chọn phương thức thanh toán.
- Gọi Payment Gateway.
- Xác nhận kết quả.

Sau đó gửi Payment Event về Booking Service.

---

### Bước 4 — Xác nhận Booking

Khi nhận được Payment Success Event:

Booking Service:

- Kiểm tra Booking.
- Kiểm tra trạng thái hiện tại.
- Chuyển trạng thái sang `CONFIRMED`.
- Ghi History.
- Ghi Audit.
- Ghi Operation.
- Tạo Outbox Event.

---

### Bước 5 — Hoàn thành

Sau khi suất chiếu kết thúc hoặc theo quy trình nghiệp vụ:

Booking chuyển sang:

```text
COMPLETED
```

Booking được lưu trữ để phục vụ tra cứu lịch sử.

---

## 3.3 Business State Machine

```text
                 +------------------+
                 | PENDING_PAYMENT  |
                 +---------+--------+
                           |
        +------------------+------------------+
        |                  |                  |
        |                  |                  |
        v                  v                  v
 +--------------+   +--------------+   +--------------+
 | CONFIRMED    |   | CANCELLED    |   | EXPIRED      |
 +------+-------+   +--------------+   +--------------+
        |
        |
        +----------------------+
        |                      |
        v                      v
+---------------+      +---------------+
| COMPLETED     |      | REFUNDED      |
+---------------+      +---------------+
```

Chỉ các chuyển trạng thái hợp lệ mới được phép thực hiện.

---

# 4. Domain Driven Design (DDD)

## 4.1 Why Domain Driven Design?

Booking là một nghiệp vụ có mức độ phức tạp cao.

Nếu chỉ sử dụng mô hình CRUD truyền thống:

- Business Logic sẽ nằm trong Controller hoặc Service.
- Các module phụ thuộc lẫn nhau.
- Khó mở rộng khi tích hợp Payment, Promotion hoặc Notification.
- Khó đảm bảo tính nhất quán của dữ liệu trong môi trường Microservice.

Vì vậy Booking Service được thiết kế theo **Domain Driven Design (DDD)**.

DDD giúp:

- Tách biệt Business Logic khỏi Infrastructure.
- Mỗi Domain có trách nhiệm rõ ràng.
- Giảm sự phụ thuộc giữa các module.
- Dễ mở rộng sang Event Driven Architecture.
- Thuận lợi khi bảo trì và kiểm thử.

---

# 4.2 Bounded Context

Trong toàn bộ hệ thống Cinema Booking, mỗi Microservice là một **Bounded Context** độc lập.

```text
                     Cinema Booking System

 ┌─────────────────────────────────────────────────────┐
 │                                                     │
 │                 Microservice Layer                  │
 │                                                     │
 └─────────────────────────────────────────────────────┘

        │

        ├──────── Auth Service

        ├──────── User Service

        ├──────── Movie Service

        ├──────── Booking Service

        ├──────── Payment Service

        ├──────── Notification Service

        ├──────── Promotion Service

        └──────── Analytics Service
```

Booking Service chỉ quản lý dữ liệu thuộc Booking Domain.

Không truy cập trực tiếp Database của service khác.

---

# 4.3 Booking Domain

Booking Domain bao gồm:

```text
Booking

Ticket

Booking History

Snapshot

Audit

Operation Log

Outbox Event
```

Không bao gồm:

```text
Movie

Cinema

Seat

User

Payment
```

Những đối tượng trên chỉ được tham chiếu bằng ID hoặc Snapshot.

---

# 4.4 Domain Model

```text
                  Booking Aggregate

                     Booking
                        │
     ┌──────────────────┼──────────────────┐
     │                  │                  │
     │                  │                  │
 Ticket         Status History        Snapshot
     │
     │
 Audit Log
     │
 Operation Log
     │
 Outbox Event
```

Booking là Aggregate Root.

Các Entity khác chỉ được thao tác thông qua Booking.

---

# 4.5 Aggregate Root

Aggregate Root:

```text
Booking
```

Booking chịu trách nhiệm:

- Validate Business Rule.
- Quản lý trạng thái.
- Sinh Ticket.
- Sinh Snapshot.
- Sinh Event.
- Điều phối toàn bộ Aggregate.

Không Service nào được cập nhật trực tiếp:

```text
booking_tickets

booking_snapshots

booking_status_histories
```

Nếu muốn thay đổi phải thông qua Booking Aggregate.

---

# 4.6 Entity Design

Booking Service sử dụng các Entity sau.

## Booking

Đại diện cho đơn đặt vé.

Thuộc tính chính:

- Booking Code.
- User ID.
- Showtime ID.
- Booking Status.
- Total Amount.

---

## Booking Ticket

Đại diện cho từng vé.

Một Booking có thể có nhiều Ticket.

---

## Booking Snapshot

Lưu toàn bộ dữ liệu tại thời điểm Booking được tạo.

Snapshot giúp dữ liệu không thay đổi nếu Movie hoặc Showtime bị chỉnh sửa sau đó.

---

## Booking Status History

Lưu toàn bộ lịch sử chuyển trạng thái.

Không bao giờ cập nhật.

Chỉ Insert.

---

## Booking Audit Log

Lưu thông tin kiểm toán.

Ví dụ:

- Ai hủy Booking.
- Ai xác nhận Booking.
- Khi nào.

---

## Booking Operation Log

Lưu các thao tác hệ thống.

Ví dụ:

```text
CREATE_BOOKING

CONFIRM_BOOKING

CANCEL_BOOKING

REFUND_BOOKING
```

---

## Booking Outbox Event

Chuẩn bị dữ liệu để publish Event.

Không publish trực tiếp trong Transaction.

---

# 4.7 Domain Service

Booking Service sử dụng các Domain Service sau.

## BookingService

Chịu trách nhiệm:

- Create Booking.
- Cancel Booking.
- Confirm Booking.
- Refund Booking.
- Change Status.

---

## BookingTicketService

Quản lý Ticket.

---

## BookingHistoryService

Quản lý Status History.

---

## AuditService

Ghi Audit.

---

## OperationLogService

Ghi Operation Log.

---

## OutboxService

Sinh Outbox Event.

---

# 4.8 Repository

Repository chỉ chịu trách nhiệm truy cập dữ liệu.

Ví dụ:

```text
BookingRepository

BookingTicketRepository

BookingSnapshotRepository

BookingHistoryRepository
```

Repository không chứa Business Logic.

---

# 4.9 Domain Rules

Một số Business Rule quan trọng.

Booking:

- Không được tạo nếu Reservation hết hạn.
- Không được Confirm hai lần.
- Không được Cancel Booking đã Confirm.
- Không được Refund Booking chưa Confirm.

---

Ticket:

- Chỉ sinh khi Booking được tạo.

---

Snapshot:

- Chỉ tạo một lần.
- Không cập nhật.

---

History:

- Không Update.
- Không Delete.

---

Audit:

- Mọi thay đổi đều phải ghi.

---

# 5. System Architecture

## 5.1 Overall Architecture

```text
                        Client

                           │

                     API Gateway

                           │

         ┌─────────────────┼──────────────────┐

         │                 │                  │

   Auth Service      Movie Service     Booking Service

                                             │

                     ┌───────────────────────┼────────────────────────┐

                     │                       │                        │

             Payment Service      Notification Service       Promotion Service
```

Booking Service là trung tâm của quá trình đặt vé.

---

# 5.2 Internal Layer

```text
Controller

↓

Application Service

↓

Domain Service

↓

Repository

↓

MySQL
```

Infrastructure:

```text
Redis

Scheduler

Kafka (Future)

Docker
```

---

# 5.3 Package Structure

```text
booking-service

├── controller

├── service

├── repository

├── entity

├── dto

├── mapper

├── validation

├── exception

├── scheduler

├── security

├── configuration

├── event

├── audit

├── common
```

---

# 5.4 Communication

Booking Service sử dụng:

## Synchronous

REST API.

Ví dụ:

```text
Booking → Movie

Booking → Promotion
```

---

## Asynchronous

Event.

Ví dụ:

```text
Payment Success

↓

Booking Confirm

↓

Notification
```

---

# 5.5 Transaction Boundary

Một Transaction của Booking chỉ bao gồm:

```text
Booking

↓

Ticket

↓

Snapshot

↓

History

↓

Audit

↓

Operation Log

↓

Outbox
```

Không bao gồm:

```text
Payment

Notification

Movie

User
```

Điều này đảm bảo Transaction ngắn và tránh Distributed Transaction.

---

# 5.6 Error Isolation

Nếu Payment Service lỗi.

Booking vẫn hoạt động.

Nếu Notification lỗi.

Booking vẫn hoạt động.

Nếu Kafka lỗi.

Booking vẫn hoạt động.

Đây là ưu điểm của Outbox Pattern.

---

# 6. Database Design

## 6.1 Database Overview

Booking Service sử dụng cơ sở dữ liệu độc lập.

Không chia sẻ Database với bất kỳ Microservice nào.

```text
Booking Database

│

├── bookings

├── booking_tickets

├── seat_reservations

├── booking_status_histories

├── booking_snapshots

├── booking_payment_events

├── booking_outbox_events

├── booking_inbox_events

├── booking_retry_tasks

├── booking_reconciliation_tasks

├── booking_dead_letter_events

├── booking_idempotency_keys

├── booking_operation_logs

└── booking_audit_logs
```

---

## 6.2 Database Principles

Thiết kế Database tuân theo các nguyên tắc:

- Một Service sở hữu một Database.
- Không Foreign Key sang Database Service khác.
- Chỉ tham chiếu bằng ID.
- Snapshot để lưu dữ liệu lịch sử.
- Soft Delete với dữ liệu nghiệp vụ nếu cần.
- Audit đầy đủ.

---

## 6.3 Core Tables

### bookings

Lưu thông tin đơn đặt vé.

Là bảng trung tâm của toàn bộ Booking Service.

---

### booking_tickets

Lưu từng vé thuộc Booking.

Quan hệ:

```text
Booking

1

↓

N

Booking Ticket
```

---

### seat_reservations

Lưu trạng thái giữ ghế.

Được sử dụng trước khi Booking được tạo.

---

### booking_snapshots

Lưu dữ liệu Movie, Showtime, Cinema, Seat, Promotion tại thời điểm đặt vé.

Mục đích:

Đảm bảo dữ liệu lịch sử không thay đổi.

---

### booking_status_histories

Lưu lịch sử chuyển trạng thái.

Mỗi lần chuyển trạng thái:

Insert một bản ghi mới.

---

### booking_operation_logs

Theo dõi các thao tác nghiệp vụ.

---

### booking_audit_logs

Theo dõi ai đã thực hiện thao tác.

---

### booking_outbox_events

Chuẩn bị dữ liệu gửi sang Message Broker.

---

### booking_inbox_events

Ngăn xử lý Event trùng lặp.

---

### booking_retry_tasks

Lưu các tác vụ Retry.

---

### booking_dead_letter_events

Lưu các Event xử lý thất bại hoàn toàn.

---

### booking_reconciliation_tasks

Đối soát dữ liệu Booking và Payment.

---

### booking_idempotency_keys

Ngăn tạo Booking trùng khi Client gửi lại Request.

---

## 6.4 Entity Relationship

```text
Booking

├──────── Booking Ticket

├──────── Booking Snapshot

├──────── Booking Status History

├──────── Audit Log

├──────── Operation Log

└──────── Outbox Event
```

Booking là trung tâm.

Các bảng còn lại đều phụ thuộc vào Booking.

---

## 6.5 Common Columns

Hầu hết các bảng đều có:

```text
id

public_id

created_at

updated_at

created_by

updated_by

deleted_at

deleted_by

version
```

Ý nghĩa:

- Hỗ trợ Audit.
- Optimistic Locking.
- Soft Delete.
- Truy vết thay đổi.

---

# 7. Aggregate Design

## 7.1 Aggregate Overview

Trong Domain Driven Design (DDD), Aggregate là một tập hợp các Entity và Value Object được quản lý như một đơn vị nhất quán (Consistency Boundary).

Booking Service lựa chọn **Booking** làm Aggregate Root vì mọi nghiệp vụ của hệ thống đều bắt đầu và kết thúc từ Booking.

Booking Aggregate đảm bảo:

- Không tạo Ticket khi Booking chưa tồn tại.
- Không thay đổi trạng thái Booking trái Business Rule.
- Không cho phép Entity bên ngoài sửa trực tiếp dữ liệu con.
- Mọi thay đổi đều được kiểm soát thông qua Aggregate Root.

---

## 7.2 Aggregate Structure

```text
                       Booking
                 (Aggregate Root)
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        │                │                │
 BookingTicket    BookingSnapshot   BookingStatusHistory
        │
        │
 BookingAuditLog
        │
 BookingOperationLog
        │
 BookingOutboxEvent
```

Booking là Aggregate Root.

Các Entity còn lại không được expose trực tiếp ra bên ngoài Domain.

---

## 7.3 Aggregate Responsibilities

Booking Aggregate chịu trách nhiệm:

- Tạo Booking.
- Validate Booking.
- Quản lý Booking Status.
- Sinh Ticket.
- Sinh Snapshot.
- Sinh Booking History.
- Sinh Audit Log.
- Sinh Operation Log.
- Sinh Outbox Event.

Booking Aggregate **không chịu trách nhiệm**:

- Thanh toán.
- Giữ ghế.
- Gửi Notification.
- Tính giá từ Movie Service.

---

## 7.4 Aggregate Boundary

Aggregate chỉ quản lý dữ liệu trong Booking Database.

```text
Booking Aggregate

├── bookings

├── booking_tickets

├── booking_snapshots

├── booking_status_histories

├── booking_operation_logs

├── booking_audit_logs

└── booking_outbox_events
```

Không truy cập:

```text
movie_db

payment_db

user_db
```

---

## 7.5 Aggregate Consistency

Booking Aggregate đảm bảo:

Nếu Create Booking thành công thì:

- Booking tồn tại.
- Ticket tồn tại.
- Snapshot tồn tại.
- History tồn tại.
- Audit tồn tại.
- Operation Log tồn tại.
- Outbox Event tồn tại.

Nếu một bước thất bại:

Toàn bộ Transaction Rollback.

---

## 7.6 Aggregate Transaction

```text
Create Booking

↓

Insert Booking

↓

Insert Ticket

↓

Insert Snapshot

↓

Insert History

↓

Insert Audit

↓

Insert Operation Log

↓

Insert Outbox

↓

Commit
```

Nếu bất kỳ bước nào lỗi:

```text
Rollback
```

---

## 7.7 Aggregate Rules

Không được:

```text
BookingTicketRepository.save(...)
```

từ Controller.

Không được:

```text
BookingSnapshotRepository.save(...)
```

từ Service khác.

Chỉ:

```java
bookingService.createBooking(...)
```

được phép sinh toàn bộ Aggregate.

---

# 8. Booking Lifecycle

## 8.1 Lifecycle Overview

Booking không chỉ là một bản ghi trong Database.

Booking có một vòng đời rõ ràng từ khi được tạo cho đến khi kết thúc.

```text
Create

↓

Pending Payment

↓

Confirmed

↓

Completed
```

Ngoài ra còn có:

```text
Pending Payment

↓

Cancelled
```

và

```text
Pending Payment

↓

Expired
```

---

## 8.2 Booking Status

Booking Service sử dụng các trạng thái sau.

### PENDING_PAYMENT

Booking vừa được tạo.

Đặc điểm:

- Ghế đã được giữ.
- Chưa thanh toán.
- Có thời gian hết hạn.

---

### CONFIRMED

Thanh toán thành công.

Booking hợp lệ.

Ticket được kích hoạt.

---

### COMPLETED

Suất chiếu kết thúc.

Booking hoàn thành.

Không cho phép thay đổi.

---

### CANCELLED

Người dùng chủ động hủy.

Ghế sẽ được giải phóng.

---

### EXPIRED

Booking quá thời gian thanh toán.

Hệ thống tự động hủy.

---

### REFUNDED

Booking đã được hoàn tiền.

---

## 8.3 State Diagram

```text
                     PENDING_PAYMENT
                    /        |       \
                   /         |        \
                  /          |         \
                 /           |          \
        CONFIRMED      CANCELLED     EXPIRED
            |
            |
      +-----+------+
      |            |
      |            |
 COMPLETED    REFUNDED
```

---

## 8.4 Allowed Transition

Cho phép:

```text
PENDING_PAYMENT

↓

CONFIRMED
```

```text
PENDING_PAYMENT

↓

CANCELLED
```

```text
PENDING_PAYMENT

↓

EXPIRED
```

```text
CONFIRMED

↓

COMPLETED
```

```text
CONFIRMED

↓

REFUNDED
```

---

## 8.5 Invalid Transition

Không cho phép:

```text
COMPLETED

↓

PENDING_PAYMENT
```

Không cho phép:

```text
EXPIRED

↓

CONFIRMED
```

Không cho phép:

```text
REFUNDED

↓

CONFIRMED
```

Không cho phép:

```text
CANCELLED

↓

CONFIRMED
```

---

## 8.6 Booking Expiration

Booking luôn có:

```text
expires_at
```

Ví dụ:

```text
Booking Time

20:00

↓

Expire

20:10
```

Nếu quá thời gian:

Scheduler sẽ:

```text
PENDING_PAYMENT

↓

EXPIRED
```

---

## 8.7 Booking Cancellation

Người dùng chỉ được hủy:

```text
PENDING_PAYMENT
```

Không được hủy:

```text
CONFIRMED

COMPLETED

REFUNDED

EXPIRED
```

---

## 8.8 Booking Confirmation

Payment Service gửi:

```text
SUCCESS
```

Booking Service:

```text
CONFIRMED
```

Đồng thời:

- History.
- Audit.
- Operation.
- Outbox.

---

## 8.9 Booking Completion

Sau khi phim kết thúc.

Booking:

```text
CONFIRMED

↓

COMPLETED
```

Booking được lưu làm lịch sử.

---

# 9. Seat Reservation Lifecycle

## 9.1 Mục tiêu

Booking Service **không giữ ghế**.

Việc giữ ghế thuộc:

```text
Seat Reservation Module
```

Booking chỉ sử dụng:

```text
Reservation ID
```

để tạo Booking.

---

## 9.2 Reservation Flow

```text
User

↓

Select Seat

↓

Seat Reservation Service

↓

Redis Lock

↓

Reservation Created

↓

Booking Service

↓

Booking Created
```

---

## 9.3 Reservation Status

Reservation có thể ở các trạng thái:

```text
HELD

CONFIRMED

RELEASED

EXPIRED
```

---

### HELD

Ghế đang được giữ.

Có thể tạo Booking.

---

### CONFIRMED

Booking thành công.

Reservation không thể sử dụng lần nữa.

---

### RELEASED

Ghế được giải phóng.

Có thể được người khác giữ.

---

### EXPIRED

Hết thời gian giữ ghế.

Booking không được phép tạo.

---

## 9.4 Reservation Validation

Booking Service kiểm tra:

- Reservation tồn tại.
- Reservation thuộc User.
- Reservation chưa hết hạn.
- Reservation chưa convert.
- Reservation thuộc đúng Showtime.

Nếu sai:

```text
BOOKING_010

BOOKING_011

BOOKING_012
```

---

## 9.5 Reservation Sequence

```text
User

↓

Choose Seat

↓

Seat Reservation

↓

Redis Lock

↓

Reservation

↓

Booking Request

↓

Validate Reservation

↓

Create Booking

↓

Reservation Confirmed
```

---

## 9.6 Release Seat

Nếu:

```text
Booking Cancelled
```

hoặc

```text
Booking Expired
```

Seat Reservation Service sẽ nhận Event và giải phóng ghế.

Booking Service không tự mở ghế.

---

## 9.7 Why Separate Reservation?

Nếu Booking trực tiếp giữ ghế:

- Coupling cao.
- Transaction dài.
- Khó scale.

Việc tách Seat Reservation thành Domain riêng giúp:

- Scale độc lập.
- Redis Lock riêng.
- Retry độc lập.
- Không ảnh hưởng Booking Domain.

---

# 10. Payment Integration

## 10.1 Overview

Booking Service **không trực tiếp xử lý thanh toán**.

Thay vào đó, Payment Service là service duy nhất giao tiếp với Payment Gateway.

Booking Service chỉ quan tâm đến **kết quả thanh toán**.

Điều này giúp:

- Tách biệt trách nhiệm giữa các service.
- Dễ thay đổi cổng thanh toán (VNPay, MoMo, ZaloPay, Stripe, PayPal, ...).
- Giảm coupling.
- Tuân thủ nguyên tắc Single Responsibility.

---

## 10.2 Architecture

```text
                User

                  │

                  ▼

          Booking Service

                  │

      Create Booking (Pending)

                  │

                  ▼

          Payment Service

                  │

                  ▼

          Payment Gateway

                  │

                  ▼

          Payment Service

                  │

                  ▼

        Booking Internal API

                  │

                  ▼

         Booking Confirmed
```

---

## 10.3 Payment Lifecycle

```text
Booking Created

↓

PENDING_PAYMENT

↓

Payment Processing

↓

SUCCESS

↓

CONFIRMED
```

Hoặc

```text
PENDING_PAYMENT

↓

FAILED

↓

CANCELLED
```

Hoặc

```text
CONFIRMED

↓

REFUND SUCCESS

↓

REFUNDED
```

---

## 10.4 Payment Event

Payment Service gửi Event nội bộ:

```json
{
  "eventId": "evt-001",
  "paymentId": "PAY001",
  "bookingId": 120,
  "paymentStatus": "SUCCESS",
  "amount": 320000,
  "paidAt": "2026-07-20T20:01:30"
}
```

Booking Service:

- Validate Event.
- Validate Booking.
- Update Status.
- Insert History.
- Insert Audit.
- Insert Operation.
- Insert Outbox.

---

## 10.5 Internal API

Payment Service gọi:

```http
POST /internal/bookings/{bookingId}/confirm
```

Ví dụ:

```http
POST /internal/bookings/120/confirm
```

Booking Service:

- Kiểm tra Booking.
- Kiểm tra Status.
- Confirm.

Response:

```json
{
  "success": true,
  "bookingStatus": "CONFIRMED"
}
```

---

## 10.6 Payment Failed

Payment Service gửi:

```text
FAILED
```

Booking Service:

```text
PENDING_PAYMENT

↓

CANCELLED
```

Đồng thời:

- History.
- Audit.
- Operation.
- Outbox.

---

## 10.7 Refund

Payment Service:

```text
REFUND_SUCCESS
```

Booking Service:

```text
CONFIRMED

↓

REFUNDED
```

---

## 10.8 Booking Expired

Nếu Payment không hoàn thành.

Scheduler:

```text
PENDING_PAYMENT

↓

EXPIRED
```

Payment Service không tham gia.

---

## 10.9 Sequence Diagram

```text
User

↓

Booking Service

↓

Booking Created

↓

Payment Service

↓

Payment Gateway

↓

Payment Success

↓

Booking Internal API

↓

Booking Confirmed

↓

Outbox Event

↓

Notification Service
```

---

# 11. Event Driven Architecture

## 11.1 Why Event Driven?

Microservice không nên gọi REST liên tục.

Ví dụ:

Booking Confirmed.

Nếu Booking gọi:

Notification

Analytics

Promotion

Email

Score

...

thì:

```text
Booking

↓

Notification

↓

Analytics

↓

Promotion

↓

Score
```

Coupling sẽ rất lớn.

Giải pháp:

Event Driven.

---

## 11.2 Architecture

```text
                Booking

                  │

         Create Outbox Event

                  │

                  ▼

         Message Broker

        (Kafka/RabbitMQ)

                  │

   ┌──────────────┼──────────────┐

   ▼              ▼              ▼

Notification   Analytics     Promotion
```

---

## 11.3 Domain Events

Booking phát sinh các Event sau.

### BOOKING_CREATED

Khi:

Booking được tạo.

Consumer:

- Analytics.

---

### BOOKING_CONFIRMED

Khi:

Thanh toán thành công.

Consumer:

- Notification.
- Analytics.
- Promotion.

---

### BOOKING_CANCELLED

Consumer:

- Seat Reservation.
- Analytics.

---

### BOOKING_EXPIRED

Consumer:

- Seat Reservation.
- Analytics.

---

### BOOKING_REFUNDED

Consumer:

- Notification.
- Analytics.

---

## 11.4 Event Structure

Ví dụ:

```json
{
  "eventId":"uuid",
  "aggregate":"Booking",
  "eventType":"BOOKING_CONFIRMED",
  "aggregateId":"120",
  "occurredAt":"2026-07-20T20:10:00",
  "payload":{
      ...
  }
}
```

---

## 11.5 Event Ordering

Booking Service đảm bảo:

```text
BOOKING_CREATED

↓

BOOKING_CONFIRMED

↓

BOOKING_COMPLETED
```

Không publish sai thứ tự.

---

## 11.6 Event Reliability

Booking không publish trực tiếp.

Thay vào đó:

```text
Booking Transaction

↓

Insert Outbox

↓

Commit

↓

Publisher

↓

Kafka
```

Nếu Kafka lỗi.

Booking vẫn Commit.

---

## 11.7 Event Consumer

Consumer phải:

- Idempotent.
- Retry được.
- Không xử lý duplicate.

---

# 12. Domain Events & Messaging Pattern

## 12.1 Outbox Pattern

Booking Service áp dụng:

Outbox Pattern.

Flow:

```text
Booking

↓

Insert Booking

↓

Insert Ticket

↓

Insert History

↓

Insert Outbox

↓

Commit
```

Publisher đọc Outbox.

---

## 12.2 Inbox Pattern

Consumer nhận Event.

Flow:

```text
Receive Event

↓

Check Inbox

↓

Processed?

↓

YES

↓

Ignore

↓

NO

↓

Process

↓

Save Inbox
```

---

## 12.3 Idempotent Consumer

Ví dụ.

Nhận:

```text
BOOKING_CONFIRMED
```

2 lần.

Booking chỉ xử lý:

1 lần.

---

## 12.4 Retry Flow

Nếu Consumer lỗi.

```text
Receive Event

↓

Process

↓

Error

↓

Retry

↓

Retry

↓

Retry

↓

Success
```

---

## 12.5 Dead Letter Queue

Nếu Retry quá giới hạn.

```text
Retry

↓

Retry

↓

Retry

↓

Fail

↓

Dead Letter
```

Admin có thể xử lý lại.

---

## 12.6 Reconciliation

Ví dụ:

Booking:

```text
PENDING_PAYMENT
```

Payment:

```text
SUCCESS
```

Scheduler:

```text
Detect

↓

Repair

↓

CONFIRMED
```

---

## 12.7 Event Timeline

Ví dụ.

```text
20:00

BOOKING_CREATED

↓

20:01

PAYMENT_SUCCESS

↓

20:01

BOOKING_CONFIRMED

↓

20:02

EMAIL_SENT

↓

20:02

POINT_ADDED
```

---

## 12.8 Advantages

Event Driven giúp:

- Giảm Coupling.
- Dễ Scale.
- Retry độc lập.
- Không ảnh hưởng Booking Transaction.
- Hỗ trợ nhiều Consumer.

---

## 12.9 Future Architecture

Hiện tại:

```text
Booking

↓

Outbox
```

Tương lai:

```text
Booking

↓

Kafka

↓

Notification

Analytics

Promotion

Recommendation

Search

Big Data

Data Lake
```

## Booking không cần thay đổi Business Logic.

# 13. REST API SPECIFICATION

## 13.1 Overview

Booking Service cung cấp ba nhóm API chính:

| API Group    | Đối tượng sử dụng |
| ------------ | ----------------- |
| Customer API | Mobile App / Web  |
| Admin API    | CMS / Dashboard   |
| Internal API | Các Microservice  |

Tất cả API đều trả về JSON và tuân theo RESTful API Design.

---

# 13.2 Base URL

```http
/api/v1
```

Internal API

```http
/internal
```

---

# 13.3 Authentication

Customer API

```text
JWT Access Token
```

Admin API

```text
JWT + ROLE_ADMIN
```

Internal API

```text
Internal Secret
Service Token
mTLS (Future)
```

---

# 13.4 Response Format

Success

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "timestamp": "2026-07-20T20:00:00"
}
```

Error

```json
{
  "success": false,
  "errorCode": "BOOKING_001",
  "message": "Booking not found",
  "timestamp": "2026-07-20T20:00:00"
}
```

---

# CUSTOMER APIs

---

# 13.5 Create Booking

```http
POST /api/v1/bookings
```

## Description

Tạo Booking mới từ Seat Reservation.

Booking chỉ được tạo khi:

- Reservation hợp lệ.
- Ghế đang được giữ.
- Showtime đang mở bán.
- User đã đăng nhập.

---

## Request

```json
{
  "showtimeId": 1001,
  "reservationIds": [21, 22, 23],
  "promotionCode": "SUMMER10",
  "note": "Near screen"
}
```

---

## Validation

Booking Service kiểm tra:

✔ User tồn tại

✔ Reservation tồn tại

✔ Reservation thuộc User

✔ Reservation chưa hết hạn

✔ Showtime tồn tại

✔ Showtime chưa kết thúc

✔ Promotion hợp lệ (nếu có)

✔ Tổng tiền hợp lệ

---

## Business Flow

```text
Validate Reservation

↓

Validate Showtime

↓

Validate Promotion

↓

Create Booking

↓

Create Tickets

↓

Create Snapshot

↓

Create History

↓

Create Audit

↓

Create Outbox

↓

Return Booking
```

---

## Response

HTTP

```text
201 Created
```

```json
{
  "bookingId": 100,
  "bookingCode": "LORAFILM-20260720-000001",
  "status": "PENDING_PAYMENT",
  "totalAmount": 320000,
  "expiredAt": "2026-07-20T20:10:00"
}
```

---

## Error Codes

| Code        | Meaning                         |
| ----------- | ------------------------------- |
| BOOKING_008 | Showtime invalid                |
| BOOKING_010 | Reservation invalid             |
| BOOKING_011 | Reservation expired             |
| BOOKING_012 | Reservation not belongs to User |
| BOOKING_014 | Promotion invalid               |

---

# 13.6 Get My Bookings

```http
GET /api/v1/bookings
```

---

## Description

Lấy danh sách Booking của User hiện tại.

---

## Query Parameters

```text
page

size

status

fromDate

toDate

sort
```

---

## Example

```http
GET /api/v1/bookings?page=0&size=10
```

---

## Response

```json
{
  "content": [
    {
      "bookingCode": "LORAFILM-20260720-000001",
      "status": "CONFIRMED",
      "totalAmount": 320000,
      "movieTitle": "Superman"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 50
}
```

---

# 13.7 Get Booking Detail

```http
GET /api/v1/bookings/{bookingId}
```

---

## Description

Lấy chi tiết Booking.

---

## Business Rules

Chỉ:

- Chủ sở hữu Booking.
- Admin.

được phép xem.

---

## Response

```json
{
  "bookingCode": "LORAFILM...",
  "status": "CONFIRMED",
  "tickets": [],
  "snapshot": {},
  "paymentStatus": "SUCCESS"
}
```

---

## Error

```text
BOOKING_007
```

Không có quyền.

---

# 13.8 Cancel Booking

```http
DELETE /api/v1/bookings/{bookingId}
```

---

## Description

Hủy Booking.

---

## Business Rules

Cho phép:

```text
PENDING_PAYMENT
```

Không cho phép:

```text
CONFIRMED

COMPLETED

REFUNDED

EXPIRED
```

---

## Flow

```text
Validate Booking

↓

Validate Status

↓

Cancel Booking

↓

History

↓

Audit

↓

Operation

↓

Outbox
```

---

## Response

```json
{
  "status": "CANCELLED"
}
```

---

# 13.9 Get Booking Tickets

```http
GET /api/v1/bookings/{bookingId}/tickets
```

---

## Description

Lấy toàn bộ Ticket.

---

## Response

```json
[
  {
    "ticketCode": "TK001",
    "seat": "A01",
    "price": 120000
  },
  {
    "ticketCode": "TK002",
    "seat": "A02",
    "price": 120000
  }
]
```

---

# 13.10 Download Ticket (Future)

```http
GET /api/v1/bookings/{bookingId}/tickets/download
```

---

Response

```text
PDF
```

---

# 13.11 Get Booking History

```http
GET /api/v1/bookings/{bookingId}/history
```

---

## Response

```json
[
  {
    "status": "PENDING_PAYMENT",
    "createdAt": "..."
  },
  {
    "status": "CONFIRMED",
    "createdAt": "..."
  }
]
```

---

# 13.12 Search Booking By Code

```http
GET /api/v1/bookings/code/{bookingCode}
```

---

## Business Rules

Chỉ:

- Chủ Booking
- Admin

---

# 13.13 Check Booking Status

```http
GET /api/v1/bookings/{bookingId}/status
```

---

Response

```json
{
  "bookingStatus": "CONFIRMED",
  "paymentStatus": "SUCCESS"
}
```

---

# 13.14 Booking Summary

```http
GET /api/v1/bookings/summary
```

---

Response

```json
{
  "totalBooking": 12,
  "totalPaid": 3500000,
  "totalCancelled": 2
}
```

---

# 13.15 Customer API Summary

| Method | Endpoint               | Description     |
| ------ | ---------------------- | --------------- |
| POST   | /bookings              | Create Booking  |
| GET    | /bookings              | My Bookings     |
| GET    | /bookings/{id}         | Booking Detail  |
| DELETE | /bookings/{id}         | Cancel Booking  |
| GET    | /bookings/{id}/tickets | Ticket List     |
| GET    | /bookings/{id}/history | Booking History |
| GET    | /bookings/{id}/status  | Booking Status  |
| GET    | /bookings/code/{code}  | Search By Code  |
| GET    | /bookings/summary      | Booking Summary |

---

# 13.16 Common Validation

Mọi Customer API đều phải kiểm tra:

- JWT hợp lệ.
- User tồn tại.
- Booking tồn tại.
- Booking thuộc User.
- Không truy cập Booking của người khác.
- Không thao tác Booking đã bị xóa.

---

# 13.17 Rate Limit (Future)

Đề xuất giới hạn:

| API            | Limit              |
| -------------- | ------------------ |
| Create Booking | 10 requests/minute |
| Cancel Booking | 20 requests/minute |
| Query Booking  | 60 requests/minute |

Mục tiêu:

- Chống spam.
- Giảm tải hệ thống.
- Hạn chế brute force Booking Code.

---

# 13. REST API SPECIFICATION (Admin API & Internal API)

## 13.18 Admin APIs Overview

Admin API được sử dụng bởi:

- CMS
- Dashboard
- Customer Support
- Cinema Operator

Yêu cầu:

- JWT Access Token
- ROLE_ADMIN
- ROLE_SUPER_ADMIN (đối với một số API nhạy cảm)

---

# 13.19 Search Booking

```http
GET /api/v1/admin/bookings
```

## Description

Tra cứu Booking theo nhiều điều kiện.

---

## Query Parameters

| Parameter     | Required | Description        |
| ------------- | -------- | ------------------ |
| bookingCode   | No       | Mã Booking         |
| userId        | No       | ID người dùng      |
| showtimeId    | No       | ID suất chiếu      |
| movieId       | No       | ID phim (snapshot) |
| cinemaId      | No       | ID rạp (snapshot)  |
| status        | No       | Booking Status     |
| paymentStatus | No       | Payment Status     |
| fromDate      | No       | Từ ngày            |
| toDate        | No       | Đến ngày           |
| page          | Yes      | Trang              |
| size          | Yes      | Kích thước         |
| sort          | No       | Sắp xếp            |

---

## Example

```http
GET /api/v1/admin/bookings?page=0&size=20&status=CONFIRMED
```

---

## Response

```json
{
  "content": [
    {
      "bookingCode": "LORAFILM-20260720-000001",
      "customerName": "Nguyen Van A",
      "movieTitle": "Superman",
      "status": "CONFIRMED",
      "totalAmount": 320000
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 120
}
```

---

# 13.20 Booking Detail

```http
GET /api/v1/admin/bookings/{bookingId}
```

Admin có thể xem toàn bộ:

- Booking
- Ticket
- Snapshot
- Audit
- History
- Operation Log

---

# 13.21 Change Booking Status

```http
PATCH /api/v1/admin/bookings/{bookingId}/status
```

Request

```json
{
  "status": "CANCELLED",
  "reason": "Customer requested"
}
```

Business Rules

- Chỉ Admin.
- Validate trạng thái.
- Không cho phép chuyển trạng thái trái quy tắc.
- Ghi Audit.
- Ghi History.
- Ghi Operation.
- Sinh Outbox Event.

---

# 13.22 Manual Refund

```http
POST /api/v1/admin/bookings/{bookingId}/refund
```

Chỉ Admin.

Booking Service:

- Validate Booking.
- Validate Status.
- Gửi yêu cầu sang Payment Service.
- Chờ Payment Event.

Không tự cập nhật trạng thái ngay.

---

# 13.23 Resend Event

```http
POST /api/v1/admin/events/{eventId}/retry
```

Dùng khi:

- Event Publish thất bại.
- Kafka lỗi.
- Consumer lỗi.

---

# 13.24 View Audit Log

```http
GET /api/v1/admin/bookings/{bookingId}/audit
```

Response

```json
[
  {
    "actor": "admin01",
    "action": "CHANGE_STATUS",
    "oldValue": "PENDING_PAYMENT",
    "newValue": "CONFIRMED",
    "createdAt": "2026-07-20T20:02:00"
  }
]
```

---

# 13.25 View Operation Log

```http
GET /api/v1/admin/bookings/{bookingId}/operations
```

Response

```json
[
  {
    "operation": "CREATE_BOOKING",
    "executionTime": 18,
    "createdAt": "..."
  }
]
```

---

# 13.26 Internal APIs

Internal API chỉ dành cho các Microservice.

Không public.

---

## Confirm Booking

```http
POST /internal/bookings/{bookingId}/confirm
```

Caller

Payment Service

---

## Expire Booking

```http
POST /internal/bookings/{bookingId}/expire
```

Caller

Scheduler

---

## Refund Booking

```http
POST /internal/bookings/{bookingId}/refund
```

Caller

Payment Service

---

## Get Booking By Code

```http
GET /internal/bookings/code/{bookingCode}
```

Caller

Notification

Analytics

---

## Check Booking Exists

```http
GET /internal/bookings/{bookingId}/exists
```

Response

```json
{
  "exists": true
}
```

---

# 14. Validation Rules

## 14.1 Booking Validation

Khi tạo Booking phải kiểm tra:

- User tồn tại.
- Showtime tồn tại.
- Showtime mở bán.
- Showtime chưa kết thúc.
- Reservation tồn tại.
- Reservation chưa hết hạn.
- Reservation thuộc User.
- Promotion hợp lệ.
- Booking Amount > 0 (hoặc theo business).

---

## 14.2 Booking Status Validation

Cho phép:

```
PENDING_PAYMENT

↓

CONFIRMED
```

Cho phép:

```
PENDING_PAYMENT

↓

EXPIRED
```

Cho phép:

```
PENDING_PAYMENT

↓

CANCELLED
```

Không cho phép:

```
CONFIRMED

↓

PENDING_PAYMENT
```

---

## 14.3 Ticket Validation

- Ticket thuộc Booking.
- Ticket chưa bị hủy.
- Ticket tồn tại.

---

## 14.4 Snapshot Validation

Snapshot:

- Chỉ tạo một lần.
- Không cập nhật.
- Không xóa.

---

## 14.5 Idempotency Validation

Header

```
Idempotency-Key
```

Nếu Request trùng:

- Không tạo Booking mới.
- Trả Response cũ.

---

# 15. Business Rules

## Rule 1

Một Reservation chỉ được tạo một Booking.

---

## Rule 2

Booking Code phải duy nhất.

---

## Rule 3

Ticket chỉ được sinh khi Booking được tạo thành công.

---

## Rule 4

Snapshot không được cập nhật.

---

## Rule 5

Booking không tính giá.

Giá được lấy từ Snapshot.

---

## Rule 6

Booking không giữ ghế.

Seat Reservation quản lý ghế.

---

## Rule 7

Booking không thanh toán.

Payment Service quản lý thanh toán.

---

## Rule 8

Booking không gửi Email.

Notification Service chịu trách nhiệm.

---

## Rule 9

Booking không publish Kafka trực tiếp.

Chỉ ghi Outbox.

---

## Rule 10

History không Update.

History chỉ Insert.

---

# 16. Security Design

## Authentication

Customer

JWT.

Admin

JWT + ROLE_ADMIN.

Internal

Service Token.

---

## Authorization

Customer:

Được phép:

- Xem Booking của mình.
- Hủy Booking của mình.

Không được:

- Xem Booking người khác.
- Hủy Booking người khác.

---

Admin:

Được phép:

- Xem toàn bộ Booking.
- Đổi trạng thái.
- Refund.
- Retry Event.

---

Internal:

Chỉ Service nội bộ.

---

## Data Security

Không trả về:

- Password.
- JWT.
- Payment Token.
- Gateway Secret.

---

# 17. Exception Handling

## Global Exception Handler

Booking Service sử dụng:

```
@RestControllerAdvice
```

---

## Error Response

```json
{
  "success": false,
  "errorCode": "BOOKING_004",
  "message": "Booking expired"
}
```

---

## Standard Error Codes

| Code        | Description                |
| ----------- | -------------------------- |
| BOOKING_001 | Booking not found          |
| BOOKING_002 | Duplicate booking          |
| BOOKING_003 | Invalid status             |
| BOOKING_004 | Booking expired            |
| BOOKING_005 | Booking cancelled          |
| BOOKING_006 | Booking completed          |
| BOOKING_007 | Access denied              |
| BOOKING_008 | Showtime invalid           |
| BOOKING_009 | Showtime ended             |
| BOOKING_010 | Reservation invalid        |
| BOOKING_011 | Reservation expired        |
| BOOKING_012 | Reservation owner mismatch |
| BOOKING_013 | Ticket not found           |
| BOOKING_014 | Promotion invalid          |
| BOOKING_015 | Invalid transition         |

---

# 18. Logging & Monitoring

## Request Logging

Mỗi Request ghi:

- Request ID.
- User ID.
- Booking ID.
- Booking Code.
- HTTP Method.
- Endpoint.
- Execution Time.
- Response Status.

---

## Business Logging

Ví dụ:

```
CREATE_BOOKING

CONFIRM_BOOKING

EXPIRE_BOOKING

REFUND_BOOKING
```

---

## Audit Logging

Lưu:

- Actor.
- Action.
- Old Value.
- New Value.
- Timestamp.

---

## Performance Logging

Theo dõi:

- Slow Query.
- API > 500ms.
- Scheduler Execution Time.
- Retry Count.

---

## Health Check

Spring Boot Actuator

```
/actuator/health

/actuator/info

/actuator/prometheus
```

---

## Metrics

Theo dõi:

- Booking Created.
- Booking Confirmed.
- Booking Cancelled.
- Booking Expired.
- Booking Refunded.
- Payment Success Rate.
- Average Response Time.
- Error Rate.
- Retry Count.
- Dead Letter Count.

---

# 19. Scheduler Design

## 19.1 Overview

Booking Service sử dụng Scheduler để xử lý các nghiệp vụ chạy theo thời gian.

Scheduler giúp hệ thống tự động thực hiện các tác vụ định kỳ mà không cần người dùng can thiệp.

Các Scheduler trong Booking Service không xử lý Business Logic trực tiếp mà chỉ điều phối việc gọi Service tương ứng.

---

## 19.2 Responsibilities

Scheduler chịu trách nhiệm:

- Hết hạn Booking.
- Hết hạn Seat Reservation.
- Publish Outbox Event.
- Retry Event thất bại.
- Reconciliation dữ liệu.
- Dọn dẹp dữ liệu tạm.
- Đồng bộ trạng thái.

---

## 19.3 Scheduler List

| Scheduler                      | Chu kỳ    | Mục đích         |
| ------------------------------ | --------- | ---------------- |
| BookingExpirationScheduler     | 30s       | Hết hạn Booking  |
| ReservationExpirationScheduler | 30s       | Hết hạn giữ ghế  |
| OutboxPublisherScheduler       | 5s        | Publish Event    |
| RetryScheduler                 | 1 phút    | Retry Event      |
| ReconciliationScheduler        | 10 phút   | Đối soát Payment |
| CleanupScheduler               | Hàng ngày | Dọn dữ liệu cũ   |

---

# 19.4 Booking Expiration Scheduler

## Mục tiêu

Booking chỉ được phép chờ thanh toán trong một khoảng thời gian.

Ví dụ:

```
10 phút
```

Sau thời gian này:

```
PENDING_PAYMENT

↓

EXPIRED
```

---

## Flow

```text
Scheduler

↓

Find Expired Booking

↓

Validate Status

↓

Update Booking

↓

Insert History

↓

Insert Audit

↓

Insert Outbox

↓

Commit
```

---

## SQL Condition

Ví dụ:

```sql
status='PENDING_PAYMENT'
AND expires_at < NOW()
```

---

## Sequence

```text
Scheduler

↓

BookingRepository

↓

BookingService

↓

History

↓

Audit

↓

Outbox

↓

Done
```

---

# 19.5 Reservation Expiration Scheduler

Reservation có thời gian sống ngắn hơn Booking.

Ví dụ:

```
5 phút
```

Flow:

```text
Find HELD Reservation

↓

Expired

↓

Release Seat

↓

Publish Event
```

---

# 19.6 Cleanup Scheduler

Định kỳ:

- Xóa Retry thành công.
- Xóa Inbox quá cũ.
- Xóa Outbox đã Publish.
- Archive Audit Log.

Không xóa:

- Booking.
- Ticket.
- Snapshot.

---

# 19.7 Scheduler Best Practices

Scheduler:

✔ Idempotent

✔ Retry được

✔ Có log

✔ Có Monitoring

✔ Không chạy Business Logic phức tạp

---

# 20. Redis Lock

## 20.1 Why Redis?

Nếu hai người chọn cùng một ghế.

Ví dụ:

```
A01
```

cùng lúc.

Database Lock:

- Chậm.
- Khó scale.

Redis:

- Nhanh.
- Atomic.
- Phù hợp Distributed System.

---

## 20.2 Lock Flow

```text
User A

↓

Redis Lock

↓

Seat Held

↓

Booking
```

Trong khi đó:

```text
User B

↓

Redis

↓

Lock Failed
```

---

## 20.3 Lock Lifecycle

```text
Acquire Lock

↓

Seat Held

↓

Booking Created

↓

Release Lock
```

---

## 20.4 Lock Key

Ví dụ:

```text
seat:showtime:1001:A01
```

Hoặc

```text
seat:1001:A01
```

---

## 20.5 TTL

Redis Lock luôn có TTL.

Ví dụ:

```
300 giây
```

Hết thời gian:

Redis tự giải phóng.

---

## 20.6 Lua Script

Redis sử dụng Lua Script để:

- Lock nhiều ghế.
- Unlock nhiều ghế.
- Atomic.

Ví dụ.

```
A01

A02

A03
```

Hoặc Lock toàn bộ.

Không lock từng ghế riêng.

---

## 20.7 Why Lua?

Nếu Lock:

```
A01

OK

A02

FAIL

A03

OK
```

Sẽ dẫn đến dữ liệu sai.

Lua đảm bảo:

```
ALL SUCCESS

or

ALL FAIL
```

---

## 20.8 Lock Release

Redis Lock được giải phóng khi:

- Booking Created.
- Booking Cancelled.
- Reservation Expired.
- Payment Timeout.

---

## 20.9 Lock Failure

Nếu Lock thất bại.

Response:

```
409 Conflict
```

Error:

```
SEAT_ALREADY_RESERVED
```

---

## 20.10 Redis Monitoring

Theo dõi:

- Lock Count.
- Lock Timeout.
- Lock Fail.
- Average Lock Time.

---

# 21. Outbox Pattern

## 21.1 Problem

Giả sử:

Booking:

```
Commit thành công
```

Sau đó:

```
Kafka lỗi.
```

Booking tồn tại.

Nhưng Notification không nhận được.

Analytics không nhận được.

Dữ liệu không nhất quán.

---

## 21.2 Solution

Outbox Pattern.

Booking không Publish Event ngay.

Booking chỉ:

```
Insert Outbox
```

---

## 21.3 Flow

```text
Create Booking

↓

Insert Booking

↓

Insert Ticket

↓

Insert Snapshot

↓

Insert History

↓

Insert Audit

↓

Insert Outbox

↓

Commit
```

Sau đó.

```text
Publisher

↓

Read Outbox

↓

Kafka

↓

Success

↓

Mark Published
```

---

## 21.4 Outbox Table

Ví dụ.

```
booking_outbox_events
```

Bao gồm:

- Event ID.
- Aggregate ID.
- Event Type.
- Payload.
- Status.
- Retry Count.
- Created At.

---

## 21.5 Status

Outbox Event có:

```
PENDING
```

↓

```
PUBLISHED
```

↓

```
FAILED
```

---

## 21.6 Publisher

Publisher Scheduler:

```
5 giây
```

Thực hiện:

```text
Find Pending

↓

Publish

↓

Update Status
```

---

## 21.7 Retry

Nếu Kafka lỗi.

```
Retry Count++

```

Không xóa Event.

---

## 21.8 Failure

Nếu Retry vượt giới hạn.

```
Dead Letter
```

---

## 21.9 Benefits

Outbox Pattern đảm bảo:

- Không mất Event.
- Không cần Distributed Transaction.
- Booking luôn Commit trước.
- Event có thể Publish lại.

---

## 21.10 Sequence Diagram

```text
Booking Service

↓

Transaction

↓

Insert Booking

↓

Insert Ticket

↓

Insert Outbox

↓

Commit

↓

Publisher Scheduler

↓

Kafka

↓

Consumer
```

---

## 21.11 Monitoring

Theo dõi:

- Pending Event.
- Failed Event.
- Publish Time.
- Retry Count.
- Average Publish Time.

---

## 21.12 Best Practices

✔ Không Publish Kafka trong Transaction.

✔ Chỉ Publish sau Commit.

✔ Có Retry.

✔ Có Dead Letter.

✔ Có Monitoring.

✔ Có Audit.

---

# 22. Inbox Pattern

## 22.1 Overview

Trong kiến trúc Microservice, việc một Event được gửi nhiều lần là điều hoàn toàn có thể xảy ra.

Nguyên nhân:

- Network Timeout.
- Kafka Retry.
- Consumer Restart.
- Producer Retry.
- Message Broker Duplicate Delivery.

Nếu Booking Service xử lý cùng một Event nhiều lần có thể dẫn đến:

- Confirm Booking nhiều lần.
- Refund nhiều lần.
- Cộng điểm thưởng nhiều lần.
- Gửi Email nhiều lần.

Để giải quyết vấn đề này, Booking Service sử dụng **Inbox Pattern**.

---

## 22.2 Inbox Flow

```text
Receive Event

↓

Check Inbox

↓

Already Processed?

├── YES → Ignore
│
└── NO
      ↓
 Process Business Logic
      ↓
 Save Inbox Record
      ↓
 Commit
```

---

## 22.3 Inbox Table

```text
booking_inbox_events
```

| Column       | Description       |
| ------------ | ----------------- |
| id           | Primary Key       |
| event_id     | Event duy nhất    |
| event_type   | Loại Event        |
| producer     | Service gửi       |
| payload      | Nội dung Event    |
| processed    | Đã xử lý hay chưa |
| processed_at | Thời gian xử lý   |

---

## 22.4 Event Validation

Mỗi Event phải kiểm tra:

- Event ID.
- Aggregate ID.
- Event Type.
- Timestamp.
- Payload.
- Signature (Future).

---

## 22.5 Example

Payment gửi:

```
PAYMENT_SUCCESS
```

Do Retry.

Booking nhận:

```
PAYMENT_SUCCESS

PAYMENT_SUCCESS
```

Inbox:

```
Lần 1

Process

↓

Save Inbox
```

Lần 2

```
Check Inbox

↓

Found

↓

Ignore
```

---

## 22.6 Benefits

Inbox Pattern đảm bảo:

- Không xử lý trùng.
- Đảm bảo Idempotency.
- Có thể Audit.
- Có thể Retry an toàn.

---

# 23. Retry Strategy

## 23.1 Why Retry?

Một số lỗi chỉ mang tính tạm thời.

Ví dụ:

- Kafka tạm thời không phản hồi.
- Notification Service đang Restart.
- Payment Service Timeout.
- Redis bị quá tải.

Retry giúp hệ thống tự phục hồi mà không cần can thiệp thủ công.

---

## 23.2 Retry Flow

```text
Execute Task

↓

Success?

├── YES → Finish
│
└── NO
      ↓
 Increase Retry Count
      ↓
 Wait
      ↓
 Retry
```

---

## 23.3 Retry Policy

Ví dụ:

| Retry | Delay       |
| ----- | ----------- |
| Lần 1 | 5 giây      |
| Lần 2 | 15 giây     |
| Lần 3 | 30 giây     |
| Lần 4 | 60 giây     |
| Lần 5 | Dead Letter |

---

## 23.4 Exponential Backoff

Thay vì Retry liên tục.

Delay tăng dần.

```
5s

↓

10s

↓

20s

↓

40s

↓

80s
```

Giúp giảm tải hệ thống.

---

## 23.5 Retry Task Table

```
booking_retry_tasks
```

Ví dụ.

| Column          |
| --------------- |
| Task ID         |
| Retry Count     |
| Next Retry Time |
| Status          |
| Payload         |

---

## 23.6 Retry Scheduler

Mỗi phút.

```
Find Retry Task

↓

Execute

↓

Success?

↓

Delete

or

Retry Again
```

---

## 23.7 Retry Principles

Retry chỉ áp dụng cho:

✔ Network Error

✔ Timeout

✔ Temporary Failure

Không Retry:

- Validation Error.
- Business Error.
- Duplicate Request.

---

# 24. Dead Letter Queue

## 24.1 Overview

Nếu Retry nhiều lần vẫn thất bại.

Task sẽ chuyển sang:

```
Dead Letter
```

Không Retry vô hạn.

---

## 24.2 Dead Letter Flow

```text
Retry

↓

Retry

↓

Retry

↓

Retry

↓

Retry Failed

↓

Dead Letter
```

---

## 24.3 Dead Letter Table

```
booking_dead_letter_events
```

Bao gồm:

- Event ID.
- Payload.
- Error Message.
- Retry Count.
- Created Time.

---

## 24.4 Admin Retry

Admin có thể:

```
Retry Again
```

Thông qua Dashboard.

---

## 24.5 Monitoring

Theo dõi:

- Dead Letter Count.
- Retry Success Rate.
- Average Retry Time.

---

## 24.6 Alert

Nếu:

```
Dead Letter > Threshold
```

Hệ thống gửi Alert.

---

# 25. Reconciliation

## 25.1 Why Reconciliation?

Giả sử.

Booking:

```
PENDING_PAYMENT
```

Nhưng.

Payment:

```
SUCCESS
```

Do Network lỗi.

Booking chưa nhận Event.

Nếu không đối soát.

Booking sẽ sai trạng thái.

---

## 25.2 Reconciliation Flow

```text
Scheduler

↓

Find Suspicious Booking

↓

Call Payment Service

↓

Compare Result

↓

Repair Data
```

---

## 25.3 Cases

### Case 1

Booking

```
PENDING_PAYMENT
```

Payment

```
SUCCESS
```

↓

Booking

```
CONFIRMED
```

---

### Case 2

Booking

```
CONFIRMED
```

Payment

```
FAILED
```

↓

Raise Alert.

Không tự sửa.

---

## 25.4 Reconciliation Table

```
booking_reconciliation_tasks
```

Bao gồm:

- Booking ID.
- Expected Status.
- Actual Status.
- Retry Count.
- Last Checked.

---

## 25.5 Repair Strategy

Repair chỉ áp dụng:

- Event mất.
- Timeout.
- Network Failure.

Không Repair:

- Business Rule.
- Manual Operation.

---

# 26. Idempotency

## 26.1 Overview

Người dùng có thể nhấn nút:

```
Thanh toán
```

nhiều lần.

Hoặc:

```
Đặt vé
```

nhiều lần.

Nếu không có Idempotency.

Có thể sinh:

- Hai Booking.
- Hai Payment.
- Hai Ticket.

---

## 26.2 Idempotency Key

Header:

```http
Idempotency-Key: 5cfd88ab-5ec3-4a6f-9d35-91d8b40d95aa
```

---

## 26.3 Flow

```text
Receive Request

↓

Find Key

↓

Exists?

├── YES
│      ↓
│ Return Previous Response
│
└── NO
       ↓
 Execute Business Logic
       ↓
 Save Key
       ↓
 Return Response
```

---

## 26.4 Idempotency Table

```
booking_idempotency_keys
```

| Column       |
| ------------ |
| Key          |
| Request Hash |
| Response     |
| Created At   |
| Expired At   |

---

## 26.5 Duplicate Request

Client:

```
POST /bookings
```

Gửi hai lần.

Booking Service:

```
Booking #100
```

Lần hai.

```
Không tạo Booking mới.

↓

Trả lại Booking #100.
```

---

## 26.6 TTL

Idempotency Key nên có TTL.

Ví dụ.

```
24 giờ
```

Sau đó.

Scheduler xóa.

---

## 26.7 Benefits

Idempotency giúp:

- Không tạo Booking trùng.
- Không tạo Payment trùng.
- Không tạo Ticket trùng.
- Hỗ trợ Retry phía Client.
- Tăng độ tin cậy hệ thống.

---

# 26.8 Reliability Architecture

```text
                Client
                   │
                   ▼
          Idempotency Filter
                   │
                   ▼
            Booking Service
                   │
      ┌────────────┼────────────┐
      ▼            ▼            ▼
   Database     Outbox       Audit Log
      │
      ▼
 Publisher Scheduler
      │
      ▼
     Kafka
      │
      ▼
 Consumer
      │
      ▼
 Inbox Pattern
      │
      ▼
 Business Logic
      │
      ▼
 Retry Strategy
      │
      ▼
 Dead Letter
```

---

# 26.9 Reliability Summary

Booking Service sử dụng đầy đủ các cơ chế Enterprise để đảm bảo tính ổn định:

| Pattern           | Mục đích                      |
| ----------------- | ----------------------------- |
| Scheduler         | Tự động xử lý tác vụ định kỳ  |
| Redis Lock        | Chống đặt trùng ghế           |
| Outbox Pattern    | Đảm bảo Event không bị mất    |
| Inbox Pattern     | Chống xử lý Event trùng       |
| Retry Strategy    | Tự động phục hồi lỗi tạm thời |
| Dead Letter Queue | Lưu Event lỗi để xử lý sau    |
| Reconciliation    | Đồng bộ Booking và Payment    |
| Idempotency       | Chống Request trùng từ Client |

---

# 27. Sequence Diagrams

## 27.1 Create Booking

### Description

Đây là luồng nghiệp vụ chính của Booking Service.

Người dùng chọn ghế, tạo Booking và chờ thanh toán.

```text
Client

│

├──────────────► API Gateway

│

├──────────────► Booking Controller

│

├──────────────► Booking Service

│                     │

│                     ├── Validate JWT

│                     ├── Validate Reservation

│                     ├── Validate Showtime

│                     ├── Validate Promotion

│                     ├── Generate Booking Code

│                     ├── Create Booking

│                     ├── Create Tickets

│                     ├── Create Snapshot

│                     ├── Create Status History

│                     ├── Create Audit Log

│                     ├── Create Operation Log

│                     ├── Insert Outbox Event

│                     └── Commit Transaction

│

◄──────────────────── Return Booking
```

---

## 27.2 Payment Success

```text
Payment Gateway

↓

Payment Service

↓

Verify Signature

↓

Save Payment

↓

Call Internal API

↓

Booking Service

↓

Validate Booking

↓

Update Status

↓

Insert History

↓

Insert Audit

↓

Insert Operation

↓

Insert Outbox

↓

Commit

↓

Notification Service

↓

Analytics Service
```

---

## 27.3 Payment Failed

```text
Payment Gateway

↓

Payment Service

↓

Payment Failed

↓

Booking Service

↓

Booking Cancelled

↓

History

↓

Audit

↓

Outbox

↓

Commit
```

---

## 27.4 Booking Expired

```text
Scheduler

↓

Find Expired Booking

↓

Booking Service

↓

Validate Status

↓

Booking Expired

↓

History

↓

Audit

↓

Outbox

↓

Commit
```

---

## 27.5 Refund

```text
Admin

↓

Payment Service

↓

Refund Gateway

↓

Success

↓

Booking Service

↓

Booking Refunded

↓

History

↓

Audit

↓

Outbox
```

---

## 27.6 Retry Event

```text
Publisher

↓

Kafka Timeout

↓

Retry

↓

Retry

↓

Retry

↓

Success

↓

Published
```

---

## 27.7 Reconciliation

```text
Scheduler

↓

Find Pending Booking

↓

Payment Service

↓

SUCCESS

↓

Booking Confirmed

↓

Update Database
```

---

## 27.8 Add Food & Beverage

```text
User
 
↓
 
Booking Service (Add Food)

↓

Check Booking Status (PENDING_PAYMENT)

↓
 
Food Catalog Client (Validate Active, Sellable, Price)

↓
 
Calculate Subtotal & Final Amount

↓
 
Save BookingFoodItem (Snapshot JSON, Currency, Price)

↓

Update Booking Final Amount (PESSIMISTIC_WRITE Lock)

↓
 
Success Response
```

---

# 28. Deployment Architecture

## 28.1 Logical Architecture

```text
                    Internet
                        │
                        ▼
                 Nginx Reverse Proxy
                        │
                        ▼
                  API Gateway
                        │
 ┌──────────────┬───────────────┬──────────────┐
 ▼              ▼               ▼              ▼

Auth        Booking         Movie        Payment

Service      Service         Service      Service

                │
                ▼
             MySQL

                │
                ▼
              Redis

                │
                ▼
             Kafka Broker
```

---

## 28.2 Booking Service Architecture

```text
Controller

↓

Service

↓

Repository

↓

Database
```

Hỗ trợ:

- Spring Validation
- Spring Security
- JPA
- Transaction
- Optimistic Locking

---

## 28.3 Infrastructure

Booking Service sử dụng:

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- MySQL 8
- Redis
- Kafka
- Docker
- Nginx

---

## 28.4 Deployment Diagram

```text
Docker Host

├── api-gateway

├── auth-service

├── booking-service

├── movie-service

├── payment-service

├── notification-service

├── promotion-service

├── analytics-service

├── mysql

├── redis

└── kafka
```

---

## 28.5 Database Connection

Booking Service chỉ kết nối:

```text
booking_db
```

Không kết nối trực tiếp:

```text
movie_db

payment_db

user_db
```

---

## 28.6 Communication

Đồng bộ

```
REST
```

Bất đồng bộ

```
Kafka
```

Cache

```
Redis
```

---

# 30. Testing Strategy

## 30.1 Overview

Booking Service áp dụng chiến lược kiểm thử nhiều tầng nhằm đảm bảo:

- Đúng Business Rule.
- Không phát sinh lỗi Regression.
- Dễ bảo trì.
- Hỗ trợ CI/CD.

---

## 30.2 Testing Pyramid

```text
                    E2E Test
                 Integration Test
                     Unit Test
```

Tỷ lệ đề xuất:

| Test Type        | Tỷ lệ |
| ---------------- | ----- |
| Unit Test        | ~70%  |
| Integration Test | ~20%  |
| End-to-End Test  | ~10%  |

---

## 30.3 Unit Test

Kiểm thử từng thành phần độc lập.

Ví dụ:

- BookingService
- BookingTicketService
- BookingStatusHistoryService
- BookingValidator
- BookingCodeGenerator

Các thành phần phụ thuộc được mock.

---

## 30.4 Integration Test

Kiểm thử luồng giữa:

- Controller
- Service
- Repository
- Database

Kiểm tra:

- Transaction.
- Rollback.
- Mapping Entity.
- Repository Query.
- Validation.

---

## 30.5 API Test

Kiểm thử toàn bộ REST API.

Ví dụ:

### Create Booking

```http
POST /api/v1/bookings
```

Kiểm tra:

- HTTP Status.
- JSON Response.
- Validation.
- Authentication.
- Authorization.
- Error Response.

---

## 30.6 Scheduler Test

Kiểm thử:

- Booking Expired.
- Retry Scheduler.
- Cleanup Scheduler.
- Outbox Publisher.

---

## 30.7 Event Test

Kiểm tra:

- Outbox Record.
- Publisher.
- Retry.
- Inbox.
- Dead Letter.

---

## 30.8 Security Test

Kiểm thử:

- JWT Invalid.
- JWT Expired.
- ROLE_USER.
- ROLE_ADMIN.
- Internal API.

---

## 30.9 Performance Test

Kiểm tra:

- API Response Time.
- Concurrent Booking.
- Redis Lock.
- Database Connection Pool.

---

## 30.10 Test Coverage

Mục tiêu:

| Thành phần | Coverage         |
| ---------- | ---------------- |
| Service    | ≥ 90%            |
| Utility    | ≥ 95%            |
| Validator  | 100%             |
| Controller | Integration Test |

---

# 31. Performance Testing

## 31.1 Target

Booking Service phải đáp ứng tải lớn trong thời gian ngắn.

---

## 31.2 KPI

| Metric           | Target    |
| ---------------- | --------- |
| Average Response | < 200 ms  |
| P95 Response     | < 500 ms  |
| P99 Response     | < 1000 ms |
| Error Rate       | < 1%      |
| Availability     | ≥ 99.9%   |

---

## 31.3 Concurrent Users

Khuyến nghị:

| Concurrent User | Mục tiêu    |
| --------------- | ----------- |
| 100             | Pass        |
| 500             | Pass        |
| 1000            | Pass        |
| 3000            | Stress Test |

---

## 31.4 Database

Kiểm tra:

- Slow Query.
- Missing Index.
- N+1 Query.
- Lock Contention.

---

## 31.5 Redis

Kiểm tra:

- Lock Latency.
- Lock Timeout.
- Memory Usage.
- Key Expiration.

---

## 31.6 Kafka

Theo dõi:

- Publish Time.
- Consumer Lag.
- Retry Count.
- Dead Letter Count.

---

## 31.7 Load Test Scenario

### Scenario 1

500 User.

Đồng thời tạo Booking.

---

### Scenario 2

1000 User.

Đồng thời Query Booking.

---

### Scenario 3

500 User.

Thanh toán cùng lúc.

---

### Scenario 4

Hai User.

Đặt cùng một ghế.

Kết quả:

- Chỉ một Booking thành công.

---

# 32. Monitoring & Observability

## 32.1 Health Check

Spring Boot Actuator:

```text
/actuator/health

/actuator/info

/actuator/metrics

/actuator/prometheus
```

---

## 32.2 Metrics

Theo dõi:

- Booking Created.
- Booking Confirmed.
- Booking Cancelled.
- Booking Expired.
- Booking Refunded.
- Active Booking.
- Pending Payment.
- Average Response Time.
- Error Rate.

---

## 32.3 Database Metrics

Theo dõi:

- Connection Pool.
- Slow Query.
- Transaction Time.
- Lock Wait.

---

## 32.4 Redis Metrics

Theo dõi:

- Connected Client.
- Used Memory.
- Expired Key.
- Hit Rate.
- Miss Rate.

---

## 32.5 Kafka Metrics

Theo dõi:

- Publish TPS.
- Consumer Lag.
- Retry Count.
- Dead Letter.

---

## 32.6 Logging

Log chuẩn gồm:

- Request ID.
- User ID.
- Booking ID.
- Booking Code.
- HTTP Method.
- URI.
- Status Code.
- Execution Time.

Không ghi:

- Password.
- JWT.
- Payment Secret.
- Thông tin nhạy cảm.

---

## 32.7 Alert

Cảnh báo khi:

- Database Down.
- Redis Down.
- Kafka Down.
- Error Rate tăng cao.
- Dead Letter vượt ngưỡng.
- Scheduler dừng hoạt động.

---

# 33. Future Improvements

## 33.1 Distributed Lock

Thay Redis Lock đơn giản bằng:

- Redisson.
- Redis Cluster.

---

## 33.2 CQRS

Tách:

- Command.
- Query.

Giúp tăng khả năng mở rộng.

---

## 33.3 Event Sourcing

Lưu toàn bộ Event thay vì chỉ trạng thái cuối.

---

## 33.4 Saga Pattern

Điều phối:

- Booking.
- Payment.
- Promotion.
- Notification.

Theo mô hình Saga.

---

## 33.5 Multi Region

Triển khai:

- Active-Active.
- Multi Data Center.

---

## 33.6 Read Replica

Tách:

- Write Database.
- Read Database.

Giảm tải truy vấn.

---

## 33.7 Cache

Bổ sung:

- Booking Detail Cache.
- Showtime Cache.
- Promotion Cache.

---

## 33.8 Observability

Tích hợp:

- OpenTelemetry.
- Grafana.
- Prometheus.
- Jaeger.

---

# Production Ready Checklist

| Hạng mục          | Trạng thái |
| ----------------- | ---------- |
| Database Schema   | ✅         |
| Entity            | ✅         |
| Repository        | ✅         |
| Service Layer     | ✅         |
| Validation        | ✅         |
| DTO               | ✅         |
| Mapper            | ✅         |
| REST API          | ✅         |
| Swagger           | ✅         |
| Authentication    | ✅         |
| Authorization     | ✅         |
| Redis Lock        | ✅         |
| Scheduler         | ✅         |
| Outbox Pattern    | ✅         |
| Inbox Pattern     | ✅         |
| Retry Strategy    | ✅         |
| Dead Letter Queue | ✅         |
| Reconciliation    | ✅         |
| Idempotency       | ✅         |
| Audit Log         | ✅         |
| Operation Log     | ✅         |
| Status History    | ✅         |
| Monitoring        | ✅         |
| Health Check      | ✅         |
| Unit Test         | ✅         |
| Integration Test  | ✅         |
| Logging           | ✅         |
| Docker Ready      | ✅         |
| CI/CD Ready       | ✅         |

---

# Demo Checklist

## Chuẩn bị

- Khởi động tất cả Microservice.
- Kết nối MySQL.
- Kết nối Redis.
- Kết nối Kafka.
- Import dữ liệu mẫu.
- Mở Swagger UI.

---

## Thực hiện Demo

1. Đăng nhập.
2. Chọn phim.
3. Chọn suất chiếu.
4. Chọn ghế.
5. Tạo Booking.
6. Kiểm tra Booking Code.
7. Kiểm tra Ticket.
8. Kiểm tra Snapshot.
9. Thanh toán thành công.
10. Booking chuyển `CONFIRMED`.
11. Kiểm tra Audit Log.
12. Kiểm tra Status History.
13. Kiểm tra Outbox Event.
14. Mô phỏng Retry.
15. Mô phỏng Duplicate Request.
16. Mô phỏng Redis Lock.
17. Mô phỏng Booking Expired.
18. Mô phỏng Refund.
19. Kiểm tra Dashboard Monitoring.
20. Kiểm tra Health Check.

---

# Tổng kết

Booking Service được thiết kế theo kiến trúc **Microservice Enterprise**, với **Booking** là **Aggregate Root** và tuân thủ nguyên tắc **Single Responsibility**. Service chỉ chịu trách nhiệm quản lý vòng đời của đơn đặt vé; các nghiệp vụ như giữ ghế, thanh toán, gửi thông báo hay tích điểm được tách sang các service chuyên biệt và kết nối thông qua REST hoặc Event.

Hệ thống áp dụng các kỹ thuật quan trọng như:

- Redis Lock để ngăn đặt trùng ghế.
- Outbox/Inbox Pattern để đảm bảo tính nhất quán sự kiện.
- Retry Strategy và Dead Letter Queue để tăng khả năng chịu lỗi.
- Reconciliation để đồng bộ dữ liệu giữa Booking và Payment.
- Idempotency để chống xử lý trùng yêu cầu.
- Audit Log, Operation Log và Status History để truy vết toàn bộ vòng đời của Booking.

Kiến trúc này hướng đến các mục tiêu:

- Khả năng mở rộng (Scalability).
- Tính sẵn sàng cao (High Availability).
- Khả năng chịu lỗi (Fault Tolerance).
- Tính nhất quán dữ liệu (Eventual Consistency).
- Dễ bảo trì và phát triển (Maintainability).
