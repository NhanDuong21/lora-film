# Analytics Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                                                          |
| -------------- | --------------------------------------------------------------------------------- |
| Service        | `analytics-service`                                                               |
| Feature        | Revenue Analytics and Dashboard Reporting                                         |
| API liên quan  | Daily Revenue, Movie Revenue, Dashboard Summary, Top Movies, Internal Aggregation |
| Contract Owner | Dương Thiện Nhân                                                                  |
| Backend Owner  | Trương Hoàng Khang                                                                |
| Reviewer       | Trương Hoàng Khang                                                                |
| Trạng thái     | Draft / Ready for Review                                                          |
| Milestone      | Sprint 2 - Core Service API Foundation                                            |
| Ngày cập nhật  | 21/06/2026                                                                        |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả API Contract cho `analytics-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Làm cơ sở triển khai dashboard và báo cáo doanh thu.
* Thống nhất contract giữa Frontend, API Gateway, Analytics Service và các service phát sinh dữ liệu.
* Xác định rõ Analytics Service là read model và data aggregation service.
* Phân biệt dữ liệu nguồn với dữ liệu tổng hợp.
* Chuẩn hóa API doanh thu theo ngày, khoảng thời gian và phim.
* Chuẩn hóa dashboard summary và top movie.
* Xác định timezone, currency, date range và aggregation rule.
* Mô tả hướng cập nhật dữ liệu bằng Kafka event hoặc scheduled aggregation.
* Bảo đảm event aggregation có idempotency.
* Ghi rõ eventual consistency.
* Không sử dụng Analytics để quyết định trạng thái Booking hoặc Payment.
* Ghi rõ những điểm có thể mismatch với schema Sprint 0 để service owner review.
* Làm cơ sở tách implementation issue sau khi contract được duyệt.

---

## 3. Phạm Vi Analytics Service

Analytics Service chịu trách nhiệm:

* Lưu dữ liệu doanh thu tổng hợp theo ngày.
* Lưu doanh thu và số vé bán tích lũy theo phim.
* Cung cấp dữ liệu dashboard.
* Cung cấp báo cáo theo khoảng thời gian.
* Cung cấp bảng xếp hạng phim theo doanh thu hoặc số vé.
* Tính tổng doanh thu, tổng booking và booking bị hủy từ read model.
* Nhận event từ Booking/Payment hoặc chạy scheduled aggregation.
* Recalculate hoặc rebuild dữ liệu tổng hợp khi cần.
* Xử lý event idempotent.
* Trả dữ liệu rỗng theo dạng `0` hoặc danh sách rỗng.
* Ghi nhận thời điểm dữ liệu được cập nhật gần nhất.

Analytics Service không chịu trách nhiệm:

* Xác nhận payment.
* Xác nhận booking.
* Quản lý ticket.
* Quản lý movie.
* Thay đổi dữ liệu nguồn.
* Truy cập trực tiếp database service khác trong runtime production.
* Làm source of truth cho doanh thu.
* Thực hiện refund.
* Xây data warehouse hoặc BI platform hoàn chỉnh.
* Quyết định user có được thanh toán hoặc nhận vé không.

---

## 4. Physical Schema Sprint 0

### 4.1. Bảng `daily_revenue_stats`

```sql
CREATE TABLE `daily_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key',
  `stat_date` date UNIQUE NOT NULL COMMENT 'Ngay tong hop du lieu, e.g., 2026-06-03',
  `total_revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Tong doanh thu thuc te thu duoc trong ngay',
  `total_bookings_count` int NOT NULL DEFAULT 0 COMMENT 'Tong so don hang thanh cong',
  `cancelled_bookings_count` int NOT NULL DEFAULT 0 COMMENT 'Tong so don hang bi huy/timeout',
  `updated_at` timestamp DEFAULT (now())
);
```

### 4.2. Bảng `movie_revenue_stats`

```sql
CREATE TABLE `movie_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `movie_id` bigint UNIQUE NOT NULL COMMENT 'Logical Ref sang movies.id cua Movie Service',
  `movie_title` varchar(255) NOT NULL COMMENT 'Luu dem ten phim de hien thi bao cao nhanh ma khong can goi API sang Movie Service',
  `total_tickets_sold` int NOT NULL DEFAULT 0 COMMENT 'Tong so luong ve da ban ra',
  `total_revenue` decimal(14,2) NOT NULL DEFAULT 0 COMMENT 'Tong doanh thu rieng cua bo phim nay',
  `updated_at` timestamp DEFAULT (now())
);
```

---

## 5. Phân Tích Schema Hiện Tại

### 5.1. Nghiệp vụ schema hỗ trợ

Schema hiện tại hỗ trợ:

* Tổng doanh thu từng ngày.
* Tổng booking thành công từng ngày.
* Tổng booking hủy hoặc timeout từng ngày.
* Doanh thu tích lũy theo phim.
* Số vé bán tích lũy theo phim.
* Hiển thị nhanh tên phim mà không cần gọi Movie Service.
* Top phim theo tổng doanh thu.
* Top phim theo tổng số vé bán.
* Dashboard doanh thu cơ bản.

### 5.2. Ý nghĩa `stat_date UNIQUE`

Mỗi ngày chỉ có một record:

```txt
daily_revenue_stats.stat_date UNIQUE
```

Update dữ liệu ngày phải dùng:

```txt
upsert theo statDate
```

Không tạo nhiều row cho cùng một ngày.

### 5.3. Ý nghĩa `movie_id UNIQUE`

Mỗi phim chỉ có một record tổng hợp:

```txt
movie_revenue_stats.movie_id UNIQUE
```

Điều này có nghĩa bảng hiện tại lưu:

```txt
Tổng doanh thu tích lũy toàn thời gian theo phim
```

Bảng này chưa hỗ trợ:

```txt
Doanh thu từng phim theo từng ngày
Doanh thu phim trong khoảng thời gian tùy chọn
Trend doanh thu riêng của một phim
```

Để hỗ trợ các báo cáo đó, cần bảng dạng:

```txt
movie_daily_revenue_stats
```

với unique:

```txt
movie_id + stat_date
```

Trong Sprint 2, contract mặc định:

* Daily API trả tổng toàn hệ thống theo ngày.
* Movie API trả tổng tích lũy toàn thời gian theo phim.
* Không tuyên bố movie revenue filter theo ngày nếu schema chưa align.

### 5.4. Giới hạn schema hiện tại

Schema chưa có:

```txt
daily_revenue_stats.total_tickets_sold
daily_revenue_stats.refunded_bookings_count
daily_revenue_stats.refund_amount
daily_revenue_stats.gross_revenue
daily_revenue_stats.discount_amount
daily_revenue_stats.average_order_value
daily_revenue_stats.currency
daily_revenue_stats.last_event_at

movie_revenue_stats.total_bookings_count
movie_revenue_stats.cancelled_bookings_count
movie_revenue_stats.refund_amount
movie_revenue_stats.stat_date

processed_analytics_events.event_id
```

### 5.5. Doanh thu gross hay net

Schema dùng field:

```txt
total_revenue
```

nhưng chưa ghi rõ đây là:

```txt
Gross revenue
```

hay:

```txt
Net revenue
```

Contract Sprint 2 đề xuất:

```txt
totalRevenue = số tiền thực tế đã thanh toán thành công
             - số tiền đã refund thành công
```

Tức là:

```txt
Net recognized revenue
```

Không tính:

* Payment `PENDING`
* Payment `FAILED`
* Payment `CANCELLED`
* Booking chưa thanh toán
* Giá trị promotion discount
* Giá trị score redeem không được thanh toán bằng tiền

Reviewer phải xác nhận định nghĩa này.

---

## 6. Analytics Là Read Model

Analytics Service chỉ lưu dữ liệu đã tổng hợp.

### Source of truth

| Dữ liệu                        | Source of truth              |
| ------------------------------ | ---------------------------- |
| Payment status và paid amount  | Payment Service              |
| Booking status và ticket count | Booking Service              |
| Movie ID và movie metadata     | Movie Service                |
| Promotion discount             | Promotion Service            |
| Analytics aggregate            | Analytics Service read model |

Analytics Service không được dùng để trả lời:

```txt
Booking đã confirmed chưa?
Payment đã success chưa?
Ticket có hợp lệ không?
```

Các câu hỏi trên phải gọi service sở hữu dữ liệu tương ứng.

---

## 7. Eventual Consistency

Analytics data có thể chậm hơn dữ liệu nguồn.

Ví dụ:

```txt
Payment SUCCESS lúc 20:00:00
Analytics nhận event lúc 20:00:03
Dashboard cập nhật lúc 20:00:04
```

Đây là hành vi hợp lệ.

API có thể trả:

```txt
lastUpdatedAt
```

để Frontend biết thời điểm tổng hợp gần nhất.

Analytics không đảm bảo strong consistency hoặc realtime tuyệt đối trong Sprint 2.

---

## 8. API Gateway và Service URL

### 8.1. API Gateway

Frontend gọi Analytics API qua:

```txt
http://localhost:8080
```

### 8.2. Analytics Service Direct URL

Chỉ dùng cho debug hoặc backend integration:

```txt
http://localhost:8089
```

Port chính thức lấy từ cấu hình project.

### 8.3. Query Flow

```txt
Admin Dashboard
→ API Gateway
→ Analytics Service
→ Analytics Database
```

### 8.4. Aggregation Flow

```txt
Booking / Payment Event
→ Kafka hoặc Internal Aggregation API
→ Analytics Service
→ Upsert aggregate tables
```

---

## 9. Quy Ước Chung

### 9.1. Admin API Header

```http
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

### 9.2. Internal API Header

```http
X-Internal-Token: <internal-token>
Content-Type: application/json
```

### 9.3. Date Format

Date-only:

```txt
YYYY-MM-DD
```

Ví dụ:

```txt
2026-06-21
```

Datetime:

```txt
ISO-8601
YYYY-MM-DDTHH:mm:ss
```

### 9.4. Timezone

Timezone nghiệp vụ chuẩn:

```txt
Asia/Ho_Chi_Minh
```

Một giao dịch được phân vào `statDate` dựa trên thời điểm nghiệp vụ sau khi chuyển về timezone này.

### 9.5. Currency

Currency chuẩn:

```txt
VND
```

Sprint 2 không hỗ trợ multi-currency.

### 9.6. Amount Format

Amount trả dạng number:

```json
{
  "totalRevenue": 12500000
}
```

Không trả chuỗi đã format:

```json
{
  "totalRevenue": "12.500.000 VND"
}
```

Frontend chịu trách nhiệm format hiển thị.

### 9.7. Pagination

* `page` bắt đầu từ `0`.
* `size` mặc định `10`.
* `size` tối đa `100`.

---

## 10. Date Range Rules

### 10.1. Required Validation

```txt
startDate <= endDate
```

### 10.2. Default Range

Nếu không truyền ngày:

```txt
startDate = ngày đầu tháng hiện tại
endDate = ngày hiện tại
```

### 10.3. Maximum Query Range

Đề xuất:

```txt
366 ngày
```

Nếu vượt:

```txt
ANALYTICS_DATE_RANGE_TOO_LARGE
```

### 10.4. Future Dates

Cho phép query future date nhưng trả dữ liệu `0` hoặc list rỗng.

Không coi là lỗi.

### 10.5. Inclusive Range

Date range là inclusive:

```txt
startDate <= statDate <= endDate
```

---

## 11. Common Response Contract

### 11.1. Success

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

### 11.2. Error

```json
{
  "success": false,
  "message": "Operation failed",
  "errorCode": "ERROR_CODE",
  "data": null,
  "errors": null
}
```

### 11.3. Validation Error

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "data": null,
  "errors": [
    {
      "field": "endDate",
      "message": "End date must not be before start date"
    }
  ]
}
```

---

## 12. Security Classification

### 12.1. Admin/Manager APIs

```txt
/api/admin/analytics/**
```

Dùng cho:

* Dashboard.
* Revenue reports.
* Top movie reports.
* Statistics query.

### 12.2. Internal APIs

```txt
/internal/analytics/**
```

Chỉ dùng cho:

* Aggregation event.
* Recalculate.
* Rebuild.
* Backfill.

Không expose công khai qua API Gateway.

### 12.3. Public APIs

Sprint 2 không có Public Analytics API.

### 12.4. Customer APIs

Sprint 2 không có Customer Analytics API.

---

## 13. Endpoint Summary

| Method | Endpoint                                       | Access                    | Mục đích                   |
| ------ | ---------------------------------------------- | ------------------------- | -------------------------- |
| GET    | `/api/admin/analytics/dashboard`               | Admin/Manager             | Dashboard summary          |
| GET    | `/api/admin/analytics/revenue/daily`           | Admin/Manager             | Doanh thu từng ngày        |
| GET    | `/api/admin/analytics/revenue/summary`         | Admin/Manager             | Tổng hợp trong khoảng ngày |
| GET    | `/api/admin/analytics/movies`                  | Admin/Manager             | Revenue theo phim          |
| GET    | `/api/admin/analytics/movies/{movieId}`        | Admin/Manager             | Chi tiết aggregate phim    |
| GET    | `/api/admin/analytics/movies/top`              | Admin/Manager             | Top phim                   |
| POST   | `/internal/analytics/events/payment-succeeded` | Internal                  | Aggregate payment success  |
| POST   | `/internal/analytics/events/payment-refunded`  | Internal                  | Aggregate refund           |
| POST   | `/internal/analytics/events/booking-cancelled` | Internal                  | Aggregate cancel           |
| POST   | `/internal/analytics/recalculate/daily`        | Internal/Admin restricted | Tính lại ngày              |
| POST   | `/internal/analytics/rebuild`                  | Internal/Admin restricted | Rebuild read model         |
| GET    | `/internal/analytics/health/data`              | Internal                  | Data health foundation     |

---

# 14. Dashboard Summary API

## 14.1. Endpoint

```http
GET /api/admin/analytics/dashboard
```

### Query Parameters

| Parameter             | Type    | Required | Mô tả              |
| --------------------- | ------- | -------: | ------------------ |
| startDate             | date    |       No | Mặc định đầu tháng |
| endDate               | date    |       No | Mặc định hôm nay   |
| comparePreviousPeriod | boolean |       No | Mặc định false     |
| topMovieLimit         | integer |       No | 1–20, mặc định 5   |

### Response Success

```json
{
  "success": true,
  "message": "Analytics dashboard retrieved successfully",
  "data": {
    "period": {
      "startDate": "2026-06-01",
      "endDate": "2026-06-21",
      "timezone": "Asia/Ho_Chi_Minh",
      "currency": "VND"
    },
    "summary": {
      "totalRevenue": 12500000,
      "totalSuccessfulBookings": 320,
      "totalCancelledBookings": 27,
      "totalTicketsSold": null,
      "averageOrderValue": 39062.5
    },
    "today": {
      "date": "2026-06-21",
      "totalRevenue": 850000,
      "successfulBookings": 22,
      "cancelledBookings": 2
    },
    "currentMonth": {
      "month": "2026-06",
      "totalRevenue": 12500000,
      "successfulBookings": 320,
      "cancelledBookings": 27
    },
    "topMovies": [
      {
        "rank": 1,
        "movieId": 101,
        "movieTitle": "Avengers",
        "totalTicketsSold": 850,
        "totalRevenue": 98500000
      }
    ],
    "comparison": null,
    "lastUpdatedAt": "2026-06-21T21:30:00"
  }
}
```

### Schema Limitation

`totalTicketsSold` trong dashboard period không thể tính chính xác từ schema hiện tại theo khoảng ngày.

Có thể:

* Trả `null`.
* Bỏ field khỏi response Sprint 2.
* Hoặc bổ sung `total_tickets_sold` vào daily stats.

Không được lấy tổng lifetime của tất cả movie rồi giả định đó là ticket count của date range.

### Average Order Value

```txt
averageOrderValue =
totalRevenue / totalSuccessfulBookings
```

Nếu booking count bằng `0`:

```txt
averageOrderValue = 0
```

---

## 14.2. Period Comparison

Nếu:

```txt
comparePreviousPeriod = true
```

Hệ thống so sánh với khoảng thời gian ngay trước đó có cùng số ngày.

Ví dụ:

```txt
Current: 15/06 → 21/06
Previous: 08/06 → 14/06
```

Response:

```json
{
  "comparison": {
    "previousPeriod": {
      "startDate": "2026-06-08",
      "endDate": "2026-06-14",
      "totalRevenue": 10000000,
      "successfulBookings": 280,
      "cancelledBookings": 20
    },
    "changes": {
      "revenuePercentage": 25.0,
      "successfulBookingsPercentage": 14.29,
      "cancelledBookingsPercentage": 35.0
    }
  }
}
```

### Percentage Rule

```txt
changePercentage =
(current - previous) / previous × 100
```

Nếu previous bằng `0`:

* Current cũng bằng `0` → `0`.
* Current lớn hơn `0` → `null` hoặc direction `NEW_ACTIVITY`.

Contract đề xuất trả `null` để tránh chia cho `0`.

---

# 15. Daily Revenue API

## 15.1. Endpoint

```http
GET /api/admin/analytics/revenue/daily
```

### Query Parameters

| Parameter         | Type    | Required | Validation          |
| ----------------- | ------- | -------: | ------------------- |
| startDate         | date    |      Yes | `YYYY-MM-DD`        |
| endDate           | date    |      Yes | `YYYY-MM-DD`        |
| includeEmptyDates | boolean |       No | Mặc định true       |
| sort              | string  |       No | `statDate,asc/desc` |

### Response Success

```json
{
  "success": true,
  "message": "Daily revenue statistics retrieved successfully",
  "data": {
    "startDate": "2026-06-19",
    "endDate": "2026-06-21",
    "currency": "VND",
    "timezone": "Asia/Ho_Chi_Minh",
    "statistics": [
      {
        "statDate": "2026-06-19",
        "totalRevenue": 700000,
        "successfulBookings": 18,
        "cancelledBookings": 1,
        "updatedAt": "2026-06-19T23:59:00"
      },
      {
        "statDate": "2026-06-20",
        "totalRevenue": 0,
        "successfulBookings": 0,
        "cancelledBookings": 0,
        "updatedAt": null
      },
      {
        "statDate": "2026-06-21",
        "totalRevenue": 850000,
        "successfulBookings": 22,
        "cancelledBookings": 2,
        "updatedAt": "2026-06-21T21:30:00"
      }
    ]
  }
}
```

### Empty Date Behavior

Nếu `includeEmptyDates = true`:

* Các ngày không có row vẫn được trả.
* Giá trị numeric là `0`.
* `updatedAt = null`.

Nếu `includeEmptyDates = false`:

* Chỉ trả ngày có record.

---

# 16. Revenue Summary API

## 16.1. Endpoint

```http
GET /api/admin/analytics/revenue/summary
```

### Query Parameters

```txt
startDate
endDate
```

### Response Success

```json
{
  "success": true,
  "message": "Revenue summary retrieved successfully",
  "data": {
    "startDate": "2026-06-01",
    "endDate": "2026-06-21",
    "totalRevenue": 12500000,
    "successfulBookings": 320,
    "cancelledBookings": 27,
    "averageOrderValue": 39062.5,
    "currency": "VND",
    "timezone": "Asia/Ho_Chi_Minh",
    "lastUpdatedAt": "2026-06-21T21:30:00"
  }
}
```

### Empty Data

```json
{
  "success": true,
  "message": "Revenue summary retrieved successfully",
  "data": {
    "startDate": "2026-01-01",
    "endDate": "2026-01-31",
    "totalRevenue": 0,
    "successfulBookings": 0,
    "cancelledBookings": 0,
    "averageOrderValue": 0,
    "currency": "VND",
    "timezone": "Asia/Ho_Chi_Minh",
    "lastUpdatedAt": null
  }
}
```

---

# 17. Movie Revenue List API

## 17.1. Endpoint

```http
GET /api/admin/analytics/movies
```

### Query Parameters

| Parameter  | Type    | Required | Validation                                       |
| ---------- | ------- | -------: | ------------------------------------------------ |
| page       | integer |       No | >= 0                                             |
| size       | integer |       No | 1–100                                            |
| movieId    | number  |       No | > 0                                              |
| movieTitle | string  |       No | Search text                                      |
| sortBy     | string  |       No | `totalRevenue`, `totalTicketsSold`, `movieTitle` |
| direction  | string  |       No | `asc`, `desc`                                    |

### Important Limitation

Không hỗ trợ `startDate/endDate` trong endpoint này với schema hiện tại.

Dữ liệu là:

```txt
Lifetime aggregate theo movie
```

### Response Success

```json
{
  "success": true,
  "message": "Movie revenue statistics retrieved successfully",
  "data": {
    "content": [
      {
        "movieId": 101,
        "movieTitle": "Avengers",
        "totalTicketsSold": 850,
        "totalRevenue": 98500000,
        "currency": "VND",
        "updatedAt": "2026-06-21T21:30:00"
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

# 18. Movie Revenue Detail API

## 18.1. Endpoint

```http
GET /api/admin/analytics/movies/{movieId}
```

### Response Success

```json
{
  "success": true,
  "message": "Movie revenue statistics retrieved successfully",
  "data": {
    "movieId": 101,
    "movieTitle": "Avengers",
    "totalTicketsSold": 850,
    "totalRevenue": 98500000,
    "averageRevenuePerTicket": 115882.35,
    "currency": "VND",
    "updatedAt": "2026-06-21T21:30:00"
  }
}
```

### Average Revenue per Ticket

```txt
averageRevenuePerTicket =
totalRevenue / totalTicketsSold
```

Nếu ticket count bằng `0`:

```txt
averageRevenuePerTicket = 0
```

### Error: Movie Stats Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Movie revenue statistics not found",
  "errorCode": "ANALYTICS_MOVIE_STATS_NOT_FOUND",
  "data": null,
  "errors": null
}
```

Không có analytics row không đồng nghĩa Movie Service không có phim đó.

---

# 19. Top Movies API

## 19.1. Endpoint

```http
GET /api/admin/analytics/movies/top
```

### Query Parameters

| Parameter | Type    | Required | Validation                |
| --------- | ------- | -------: | ------------------------- |
| metric    | string  |       No | `REVENUE`, `TICKETS_SOLD` |
| limit     | integer |       No | 1–50, mặc định 10         |
| direction | string  |       No | Mặc định `desc`           |

### Response Success

```json
{
  "success": true,
  "message": "Top movies retrieved successfully",
  "data": {
    "metric": "REVENUE",
    "currency": "VND",
    "movies": [
      {
        "rank": 1,
        "movieId": 101,
        "movieTitle": "Avengers",
        "totalTicketsSold": 850,
        "totalRevenue": 98500000
      },
      {
        "rank": 2,
        "movieId": 102,
        "movieTitle": "Doraemon",
        "totalTicketsSold": 760,
        "totalRevenue": 76000000
      }
    ],
    "lastUpdatedAt": "2026-06-21T21:30:00"
  }
}
```

### Empty Data

```json
{
  "success": true,
  "message": "Top movies retrieved successfully",
  "data": {
    "metric": "REVENUE",
    "currency": "VND",
    "movies": [],
    "lastUpdatedAt": null
  }
}
```

---

# 20. Internal Payment Success Aggregation

## 20.1. Endpoint

```http
POST /internal/analytics/events/payment-succeeded
```

Endpoint này mô tả contract nội bộ.

Production có thể thay bằng Kafka consumer.

### Request

```json
{
  "eventId": "PAYMENT-SUCCESS-3001",
  "eventType": "PAYMENT_SUCCEEDED",
  "occurredAt": "2026-06-21T20:12:00",
  "paymentId": 3001,
  "bookingId": 1001,
  "userId": 15,
  "paidAmount": 216000,
  "currency": "VND",
  "movieId": 101,
  "movieTitle": "Avengers",
  "ticketCount": 2
}
```

### Field Definitions

| Field       | Type     | Required | Validation            |
| ----------- | -------- | -------: | --------------------- |
| eventId     | string   |      Yes | Unique idempotency ID |
| eventType   | string   |      Yes | `PAYMENT_SUCCEEDED`   |
| occurredAt  | datetime |      Yes | ISO-8601              |
| paymentId   | number   |      Yes | > 0                   |
| bookingId   | number   |      Yes | > 0                   |
| userId      | number   |       No | > 0 nếu có            |
| paidAmount  | number   |      Yes | >= 0                  |
| currency    | string   |      Yes | `VND`                 |
| movieId     | number   |      Yes | > 0                   |
| movieTitle  | string   |      Yes | Không rỗng            |
| ticketCount | integer  |      Yes | > 0                   |

### Processing

```txt
Validate event
→ Check eventId chưa xử lý
→ Convert occurredAt sang Asia/Ho_Chi_Minh
→ Determine statDate
→ Upsert daily revenue
→ Upsert movie revenue
→ Mark event processed
→ Commit
```

### Daily Update

```txt
totalRevenue += paidAmount
totalBookingsCount += 1
```

### Movie Update

```txt
totalRevenue += paidAmount
totalTicketsSold += ticketCount
```

### Response

```json
{
  "success": true,
  "message": "Payment success event aggregated successfully",
  "data": {
    "eventId": "PAYMENT-SUCCESS-3001",
    "statDate": "2026-06-21",
    "processed": true
  }
}
```

---

# 21. Internal Payment Refund Aggregation

## 21.1. Endpoint

```http
POST /internal/analytics/events/payment-refunded
```

### Request

```json
{
  "eventId": "PAYMENT-REFUNDED-3001",
  "eventType": "PAYMENT_REFUNDED",
  "occurredAt": "2026-06-22T09:00:00",
  "paymentId": 3001,
  "bookingId": 1001,
  "refundAmount": 216000,
  "currency": "VND",
  "movieId": 101,
  "movieTitle": "Avengers",
  "refundedTicketCount": 2,
  "originalPaymentDate": "2026-06-21"
}
```

### Refund Date Attribution

Reviewer cần chốt một trong hai hướng:

#### Hướng A — Trừ vào ngày refund

```txt
statDate = refund occurred date
```

Ưu điểm:

* Phản ánh dòng tiền theo ngày.
* Không sửa dữ liệu lịch sử cũ.

Nhược điểm:

* Một ngày có thể có doanh thu âm nếu refund lớn.

#### Hướng B — Điều chỉnh ngày payment gốc

```txt
statDate = original payment date
```

Ưu điểm:

* Báo cáo ngày bán phản ánh net cuối cùng.

Nhược điểm:

* Dữ liệu lịch sử bị thay đổi.

Contract đề xuất:

```txt
Hướng A — trừ vào ngày refund
```

### Update Direction

Daily:

```txt
totalRevenue -= refundAmount
```

Movie:

```txt
totalRevenue -= refundAmount
totalTicketsSold -= refundedTicketCount
```

Không để:

```txt
totalTicketsSold < 0
```

Việc totalRevenue có được âm theo ngày hay không cần reviewer xác nhận.

### Schema Limitation

Schema không lưu riêng:

```txt
refundAmount
refundedBookingCount
```

Refund chỉ làm thay đổi `total_revenue`, nên khó phân biệt gross và refund trên dashboard.

---

# 22. Internal Booking Cancelled Aggregation

## 22.1. Endpoint

```http
POST /internal/analytics/events/booking-cancelled
```

### Request

```json
{
  "eventId": "BOOKING-CANCELLED-1002",
  "eventType": "BOOKING_CANCELLED",
  "occurredAt": "2026-06-21T20:30:00",
  "bookingId": 1002,
  "previousStatus": "PENDING_PAYMENT",
  "currentStatus": "CANCELLED",
  "reason": "USER_CANCELLED"
}
```

### Processing

```txt
cancelledBookingsCount += 1
```

Không giảm revenue nếu booking chưa thanh toán.

Nếu booking đã thanh toán rồi refund, revenue adjustment phải đến từ:

```txt
PAYMENT_REFUNDED
```

Không trừ revenue từ cả hai event để tránh double subtraction.

---

# 23. Idempotency Rules

## 23.1. Required Idempotency

Mỗi aggregation event phải có:

```txt
eventId
```

Cùng event không được aggregate hai lần.

### Duplicate Response

```json
{
  "success": true,
  "message": "Analytics event was already processed",
  "data": {
    "eventId": "PAYMENT-SUCCESS-3001",
    "processed": false,
    "idempotent": true
  }
}
```

### Schema Limitation

Schema hiện chưa có bảng:

```txt
processed_analytics_events
```

Khuyến nghị bổ sung:

```txt
event_id UNIQUE
event_type
processed_at
source_service
```

Hoặc dùng Kafka exactly-once/outbox pattern phù hợp.

Không được chỉ tin consumer offset nếu dữ liệu cần có khả năng replay an toàn.

---

# 24. Atomic Aggregation Rules

Một payment success event cập nhật:

```txt
daily_revenue_stats
+
movie_revenue_stats
+
processed event marker
```

Tất cả phải nằm trong cùng transaction.

Nếu một bước lỗi:

```txt
Rollback toàn bộ
```

Không được:

```txt
Daily update thành công
nhưng Movie update thất bại
```

rồi vẫn mark event processed.

---

# 25. Upsert Rules

### Daily Stats

Pseudo SQL:

```sql
INSERT INTO daily_revenue_stats (
  stat_date,
  total_revenue,
  total_bookings_count,
  cancelled_bookings_count
)
VALUES (
  :statDate,
  :revenue,
  :bookingCount,
  :cancelledCount
)
ON DUPLICATE KEY UPDATE
  total_revenue = total_revenue + :revenue,
  total_bookings_count = total_bookings_count + :bookingCount,
  cancelled_bookings_count = cancelled_bookings_count + :cancelledCount,
  updated_at = CURRENT_TIMESTAMP;
```

### Movie Stats

```sql
INSERT INTO movie_revenue_stats (
  movie_id,
  movie_title,
  total_tickets_sold,
  total_revenue
)
VALUES (
  :movieId,
  :movieTitle,
  :ticketCount,
  :revenue
)
ON DUPLICATE KEY UPDATE
  movie_title = :movieTitle,
  total_tickets_sold = total_tickets_sold + :ticketCount,
  total_revenue = total_revenue + :revenue,
  updated_at = CURRENT_TIMESTAMP;
```

Các ví dụ SQL chỉ mô tả direction, không bắt buộc implementation cụ thể.

---

# 26. Scheduled Recalculation Direction

Analytics Service có thể chạy job:

```txt
Daily aggregation reconciliation
```

Ví dụ:

```txt
00:15 mỗi ngày
→ kiểm tra dữ liệu ngày hôm trước
→ rebuild hoặc reconcile aggregate
```

Tuy nhiên trong database-per-service:

* Analytics không được query trực tiếp Payment DB.
* Recalculation cần API export, event replay hoặc dữ liệu staging hợp lệ.
* Không kết nối trực tiếp JDBC sang DB service khác trong production.

Sprint 2 có thể dùng seed data để test query foundation.

---

# 27. Recalculate Daily Statistics API

## 27.1. Endpoint

```http
POST /internal/analytics/recalculate/daily
```

### Request

```json
{
  "statDate": "2026-06-21",
  "mode": "REBUILD",
  "requestId": "RECALCULATE-DAILY-20260621-01"
}
```

### Modes

```txt
REBUILD
RECONCILE
```

* `REBUILD`: thay thế aggregate theo source dataset.
* `RECONCILE`: so sánh và điều chỉnh chênh lệch.

### Response

Status: `202 Accepted`

```json
{
  "success": true,
  "message": "Daily analytics recalculation accepted",
  "data": {
    "requestId": "RECALCULATE-DAILY-20260621-01",
    "statDate": "2026-06-21",
    "status": "ACCEPTED"
  }
}
```

### Security

Endpoint này:

* Chỉ Internal/Admin đặc quyền.
* Không expose public.
* Phải audit người/request khởi chạy.
* Phải idempotent theo `requestId`.

Schema hiện chưa có rebuild job tracking.

Sprint 2 có thể chỉ mô tả direction, chưa implement.

---

# 28. Rebuild Analytics API

## 28.1. Endpoint

```http
POST /internal/analytics/rebuild
```

### Request

```json
{
  "startDate": "2026-06-01",
  "endDate": "2026-06-21",
  "includeMovieStats": true,
  "requestId": "REBUILD-ANALYTICS-20260621-01"
}
```

### Response

```json
{
  "success": true,
  "message": "Analytics rebuild accepted",
  "data": {
    "requestId": "REBUILD-ANALYTICS-20260621-01",
    "status": "ACCEPTED"
  }
}
```

Full rebuild là operation nặng.

Không nên chạy trực tiếp đồng bộ trong HTTP request.

---

# 29. Analytics Data Health API

## 29.1. Endpoint

```http
GET /internal/analytics/health/data
```

### Response

```json
{
  "success": true,
  "message": "Analytics data health retrieved successfully",
  "data": {
    "latestDailyStatDate": "2026-06-21",
    "latestDailyUpdatedAt": "2026-06-21T21:30:00",
    "latestMovieUpdatedAt": "2026-06-21T21:30:00",
    "aggregationLagSeconds": 4,
    "status": "HEALTHY"
  }
}
```

Schema hiện không lưu event lag trực tiếp; một số field có thể derive từ timestamp/runtime metrics.

---

# 30. Empty Data Rules

Không có dữ liệu không phải lỗi.

### Summary

Trả:

```txt
totalRevenue = 0
successfulBookings = 0
cancelledBookings = 0
```

### List

Trả:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0
}
```

### Top Movie

Trả:

```json
{
  "movies": []
}
```

Không trả `404` chỉ vì khoảng ngày không có doanh thu.

---

# 31. Movie Title Snapshot Rule

`movie_revenue_stats.movie_title` là snapshot/cache để dashboard không phải gọi Movie Service.

Khi Movie Service đổi tên phim:

* Event mới có thể cập nhật `movie_title`.
* Analytics không cần giữ lịch sử tên cũ trong Sprint 2.
* `movieId` vẫn là logical reference ổn định.

Analytics Service không dùng `movie_title` làm định danh.

---

# 32. Revenue Calculation Rules

## 32.1. Included

Chỉ tính:

```txt
Payment SUCCESS
```

với amount thực tế được xác nhận.

## 32.2. Excluded

Không tính:

```txt
Payment PENDING
Payment PROCESSING
Payment FAILED
Payment CANCELLED
Booking PENDING_PAYMENT
Promotion discount
Score redeem value
```

## 32.3. Refund

Khi `Payment REFUNDED`:

```txt
totalRevenue -= refundAmount
```

## 32.4. Promotion

Ví dụ:

```txt
Original booking amount = 240000
Promotion discount = 24000
Paid amount = 216000
```

Revenue ghi nhận:

```txt
216000
```

Không phải `240000`.

## 32.5. Score Redeem

Nếu user dùng:

```txt
50000 VND bằng điểm
```

và thanh toán tiền mặt/cổng:

```txt
166000 VND
```

Contract đề xuất revenue là:

```txt
166000 VND
```

vì đó là số tiền thực thu.

Reviewer cần xác nhận policy này.

---

# 33. Booking and Ticket Count Rules

### Successful Booking

Tăng khi payment success và booking được xác nhận thành công.

Cần tránh cả:

```txt
PAYMENT_SUCCESS
```

và:

```txt
BOOKING_CONFIRMED
```

đều tăng booking count.

Phải chọn một canonical aggregation event.

Contract đề xuất:

```txt
BOOKING_CONFIRMED
```

là event cập nhật booking/ticket count.

Trong khi:

```txt
PAYMENT_SUCCESS
```

cập nhật revenue.

Tuy nhiên để transaction thống nhất, team cũng có thể dùng một event enriched:

```txt
BOOKING_CONFIRMED_WITH_PAYMENT
```

Reviewer cần chốt.

### Ticket Count

Movie stats tăng bằng số ticket của booking confirmed.

Không tính ticket thuộc:

* Booking pending.
* Booking cancelled trước payment.
* Reservation.

---

# 34. Concurrency Rules

Hai event cho cùng aggregate có thể đến đồng thời.

Update phải atomic:

```txt
totalRevenue = totalRevenue + delta
```

Không dùng:

```txt
SELECT totalRevenue
→ cộng trong application
→ UPDATE
```

mà không lock/version.

Idempotency marker phải được kiểm tra trong transaction.

---

# 35. Event Ordering Rules

Có thể xảy ra:

```txt
PAYMENT_REFUNDED đến trước PAYMENT_SUCCEEDED
```

do retry hoặc partition/order issue.

Sprint 2 direction:

* Event cùng payment/booking nên dùng message key nhất quán.
* Kafka key đề xuất: `bookingId` hoặc `paymentId`.
* Consumer cần xử lý hoặc trì hoãn invalid ordering.
* Không làm aggregate âm sai do event đến lệch thứ tự.
* Có thể ghi failed event để reconciliation.

Full out-of-order handling nằm ngoài Sprint 2 foundation.

---

# 36. Seed Data Direction

Sprint 2 cho phép dùng seed data để triển khai query APIs.

Seed data phải:

* Nằm trong Analytics DB.
* Có ghi rõ là development data.
* Không bị trình bày như dữ liệu event integration thật.
* Không yêu cầu kết nối Booking/Payment thật.
* Có date range đủ để test dashboard/trend.
* Có nhiều movie để test ranking.

Query API phải giữ nguyên contract để sau này thay seed bằng event aggregation mà không đổi Frontend.

---

# 37. Security Rules

* Analytics dashboard yêu cầu Admin hoặc Manager role.
* Không expose Internal aggregation endpoint.
* Internal API phải có authentication.
* Không cho Frontend gửi event aggregate.
* Không cho client tự truyền revenue để cập nhật.
* Query phải validate range để tránh request quá nặng.
* Không trả dữ liệu cá nhân nếu không cần.
* Không trả payment credential hoặc provider payload.
* Rebuild/recalculate cần permission cao hơn query.
* Mọi manual rebuild nên được audit.

Permission đề xuất:

```txt
ANALYTICS_READ
ANALYTICS_REVENUE_READ
ANALYTICS_REBUILD
ANALYTICS_RECONCILE
```

---

# 38. Caching Direction

Dashboard có thể cache ngắn hạn:

```txt
30–60 giây
```

Cache key cần chứa:

```txt
date range
filter
top limit
comparison option
```

Cache invalidation có thể xảy ra:

* Sau aggregation event.
* Theo TTL.
* Sau rebuild.

Caching không bắt buộc trong Sprint 2.

---

# 39. Error Code Catalog

| Error Code                          |         HTTP | Ý nghĩa                      |
| ----------------------------------- | -----------: | ---------------------------- |
| `ANALYTICS_INVALID_DATE_FORMAT`     |          400 | Date sai format              |
| `ANALYTICS_INVALID_DATE_RANGE`      |          400 | End date trước start date    |
| `ANALYTICS_DATE_RANGE_TOO_LARGE`    |          400 | Khoảng query quá lớn         |
| `ANALYTICS_INVALID_METRIC`          |          400 | Metric không hợp lệ          |
| `ANALYTICS_INVALID_SORT_FIELD`      |          400 | Sort field không hợp lệ      |
| `ANALYTICS_MOVIE_STATS_NOT_FOUND`   |          404 | Không có aggregate theo phim |
| `ANALYTICS_EVENT_ALREADY_PROCESSED` | 200 hoặc 409 | Event đã aggregate           |
| `ANALYTICS_EVENT_INVALID`           |          400 | Event thiếu/sai dữ liệu      |
| `ANALYTICS_EVENT_OUT_OF_ORDER`      |          409 | Event sai thứ tự             |
| `ANALYTICS_CURRENCY_NOT_SUPPORTED`  |          400 | Currency không hỗ trợ        |
| `ANALYTICS_REBUILD_ALREADY_RUNNING` |          409 | Rebuild đang chạy            |
| `ANALYTICS_REBUILD_NOT_AVAILABLE`   |          501 | Foundation chưa hỗ trợ       |
| `ANALYTICS_SOURCE_UNAVAILABLE`      |          503 | Nguồn dữ liệu không khả dụng |
| `ANALYTICS_AGGREGATION_FAILED`      |          500 | Aggregate thất bại           |
| `ANALYTICS_INVALID_QUERY`           |          400 | Query không hợp lệ           |
| `VALIDATION_ERROR`                  |          400 | Validation lỗi               |
| `UNAUTHORIZED`                      |          401 | Chưa xác thực                |
| `FORBIDDEN`                         |          403 | Không có quyền               |
| `INTERNAL_SERVER_ERROR`             |          500 | Lỗi hệ thống                 |

---

# 40. Schema Alignment Notes

## 40.1. Daily Ticket Count

Dashboard cần tổng ticket theo khoảng ngày nhưng schema chưa có:

```txt
daily_revenue_stats.total_tickets_sold
```

Nếu đây là requirement Sprint 2, cần thêm field.

## 40.2. Movie Revenue by Date

Schema hiện tại chỉ có lifetime movie aggregate.

Muốn hỗ trợ movie revenue theo khoảng thời gian cần bảng:

```txt
movie_daily_revenue_stats
```

## 40.3. Gross, Refund và Net Revenue

Schema chỉ có:

```txt
total_revenue
```

Nếu dashboard cần breakdown phải thêm:

```txt
gross_revenue
refund_amount
net_revenue
```

## 40.4. Event Idempotency

Schema chưa có processed events table.

Khuyến nghị thêm:

```txt
processed_analytics_events
```

## 40.5. Rebuild Job Tracking

Schema chưa có:

```txt
analytics_jobs
```

Nếu implement async rebuild cần job table hoặc external job tracking.

## 40.6. Cancellation Type

`cancelled_bookings_count` đang gộp:

```txt
User cancelled
System timeout
Admin cancelled
```

Nếu cần breakdown phải bổ sung field hoặc dimension.

## 40.7. Movie Snapshot

Chỉ lưu movie title, chưa lưu:

```txt
genre
duration
rating
releaseDate
```

Không cần thêm nếu dashboard Sprint 2 không yêu cầu.

## 40.8. Updated Timestamp

`updated_at DEFAULT now()` không tự động cập nhật ở mọi MySQL configuration.

Implementation cần chủ động set `updated_at` khi upsert hoặc thêm `ON UPDATE`.

---

# 41. Out of Scope

* Data warehouse.
* Data lake.
* BI tool integration.
* Real-time analytics hoàn chỉnh.
* Customer behavior analytics.
* Funnel analytics.
* Cohort analysis.
* Forecasting.
* Recommendation Engine.
* Machine Learning.
* Multi-currency.
* Branch/cinema dimension nếu schema chưa có.
* Showtime-level revenue report.
* Genre-level revenue report.
* Employee performance analytics.
* Promotion performance nâng cao.
* Score analytics nâng cao.
* Production Kafka/outbox implementation.
* Direct access database service khác.
* Backend code trong issue contract này.
* Schema update ngoài review process.

---

# 42. Implementation Issue Direction

Sau khi contract được review và schema alignment hoàn tất nếu cần, có thể tách:

```txt
[Backend] Implement Analytics Dashboard and Daily Revenue APIs

[Backend] Implement Movie Revenue and Top Movie APIs

[Backend] Implement Analytics Event Aggregation Foundation

[Backend] Implement Analytics Recalculation and Rebuild Direction
```

Nếu giảm scope Sprint 2:

```txt
Issue 1: Seed Analytics Data
Issue 2: Dashboard + Daily Revenue Query
Issue 3: Movie Revenue + Top Movies Query
```

Event aggregation có thể chuyển sang Sprint sau nếu Sprint 2 chỉ làm query foundation.

Implementation issue chỉ chuyển `Ready` khi:

```txt
Contract đã được duyệt
+
Schema bắt buộc đã align
+
Khang xác nhận feasibility
```

---

# 43. Acceptance Criteria

* [ ] Có schema Sprint 0 baseline.
* [ ] Có source-of-truth clarification.
* [ ] Có eventual consistency notes.
* [ ] Có timezone và currency rule.
* [ ] Có date range validation.
* [ ] Có maximum query range.
* [ ] Có dashboard summary API.
* [ ] Có daily revenue API.
* [ ] Có revenue summary API.
* [ ] Có movie revenue API.
* [ ] Có top movie API.
* [ ] Có empty data behavior.
* [ ] Có revenue calculation rules.
* [ ] Có refund direction.
* [ ] Có booking/ticket count rules.
* [ ] Có internal aggregation direction.
* [ ] Có scheduled recalculation direction.
* [ ] Có rebuild direction.
* [ ] Có event idempotency.
* [ ] Có atomic update rules.
* [ ] Có seed data direction.
* [ ] Có Admin/Internal security classification.
* [ ] Có error code catalog.
* [ ] Có schema mismatch notes.
* [ ] Khang review feasibility.
* [ ] Contract sẵn sàng implementation.
* [ ] MR target `develop`.

---

# 44. Các Điểm Reviewer Cần Xác Nhận

Khang cần xác nhận:

1. Analytics Service port chính thức.
2. `total_revenue` là gross hay net revenue.
3. Refund được trừ vào ngày refund hay ngày payment gốc.
4. Có cho daily revenue âm do refund không.
5. Revenue tính theo amount thực trả sau promotion không.
6. Có cộng revenue trên phần thanh toán bằng score không.
7. Canonical event nào tăng successful booking count.
8. Canonical event nào tăng ticket count.
9. Có dùng `PAYMENT_SUCCESS` hay `BOOKING_CONFIRMED` làm event chính.
10. Có cần `total_tickets_sold` trong daily stats không.
11. Có cần movie revenue theo date range trong Sprint 2 không.
12. Có cần bảng `movie_daily_revenue_stats` không.
13. Có cần gross/refund/net breakdown không.
14. Maximum query range là bao nhiêu ngày.
15. Có hỗ trợ comparison trend trong Sprint 2 không.
16. Analytics query dùng seed data hay event integration.
17. Có dùng Kafka trong Sprint 2 không.
18. Message key là `bookingId` hay `paymentId`.
19. Có cần processed events table không.
20. Có implement refund aggregation không.
21. Có implement cancelled booking aggregation không.
22. Recalculate/rebuild có nằm trong Sprint 2 không.
23. Có cần job tracking table không.
24. Dashboard role là Admin, Manager hay cả hai.
25. Có cache dashboard không.
26. Có cần schema alignment issue trước implementation không.

---

# 45. Lịch Sử Chỉnh Sửa

| Ngày       | Nội dung                                                         | Người thực hiện  |
| ---------- | ---------------------------------------------------------------- | ---------------- |
| 21/06/2026 | Khởi tạo Analytics Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận tại đây sau khi schema MR tương ứng đã được merge.
