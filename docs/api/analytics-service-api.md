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
| Trạng thái     | Approved / Ready for Implementation                                               |
| Milestone      | Sprint 2 - Core Service API Foundation                                            |
| Ngày cập nhật  | 22/06/2026                                                                        |

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

## 4. Physical Schema và Schema Alignment Direction

### 4.1. Bảng `daily_revenue_stats`

Schema Sprint 0:

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

Schema cần bổ sung trước implementation:

```sql
ALTER TABLE `daily_revenue_stats`
ADD COLUMN `total_tickets_sold` int NOT NULL DEFAULT 0
COMMENT 'Tong so ve da ban thanh cong trong ngay';
```

### 4.2. Bảng `movie_revenue_stats`

Schema Sprint 0:

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

Bảng này tiếp tục lưu lifetime aggregate theo movie.

### 4.3. Bảng `movie_daily_revenue_stats`

Analytics Service hỗ trợ revenue theo date range và trend theo movie, vì vậy cần bổ sung bảng:

```sql
CREATE TABLE `movie_daily_revenue_stats` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `movie_id` bigint NOT NULL COMMENT 'Logical Ref sang movies.id cua Movie Service',
  `movie_title` varchar(255) NOT NULL COMMENT 'Movie title snapshot',
  `stat_date` date NOT NULL,
  `tickets_sold` int NOT NULL DEFAULT 0,
  `revenue` decimal(14,2) NOT NULL DEFAULT 0,
  `updated_at` timestamp DEFAULT (now()),
  UNIQUE (`movie_id`, `stat_date`)
);
```

Bảng này dùng cho:

- Revenue từng phim theo ngày.
- Revenue theo date range.
- Ticket count theo date range.
- Trend revenue theo movie.
- Top movie trong một khoảng thời gian.

### 4.4. Bảng `processed_analytics_events`

Analytics Service sử dụng Kafka aggregation và cần replay event an toàn.

Bổ sung:

```sql
CREATE TABLE `processed_analytics_events` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` varchar(150) UNIQUE NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `source_service` varchar(100) NOT NULL,
  `processed_at` timestamp DEFAULT (now())
);
```

Bảng này bảo đảm:

- Không aggregate cùng event hai lần.
- Consumer có thể retry/replay an toàn.
- Idempotency không chỉ phụ thuộc Kafka consumer offset.

### 4.5. Schema Alignment Requirement

Các thay đổi sau là bắt buộc trước Backend implementation:

```txt
daily_revenue_stats.total_tickets_sold
movie_daily_revenue_stats
processed_analytics_events
```

Schema được cập nhật trong issue riêng:

```txt
[Database] Align Analytics Schema with Analytics API Contract
```

## 5. Phân Tích Schema và Quyết Định Chính Thức

### 5.1. Lifetime và Daily Aggregate

Analytics Service duy trì hai mức aggregate theo phim:

```txt
movie_revenue_stats
→ lifetime revenue và lifetime tickets sold

movie_daily_revenue_stats
→ revenue và tickets sold theo từng ngày
```

`movie_revenue_stats.movie_id UNIQUE` tiếp tục bảo đảm mỗi phim chỉ có một lifetime record.

`movie_daily_revenue_stats` sử dụng:

```txt
UNIQUE(movie_id, stat_date)
```

để hỗ trợ date range và trend.

### 5.2. Daily Aggregate

`daily_revenue_stats.stat_date UNIQUE` nghĩa là mỗi ngày chỉ có một record tổng hợp toàn hệ thống.

Update phải dùng upsert theo `statDate`.

Sau schema alignment, bảng hỗ trợ:

- Net revenue theo ngày.
- Successful booking count theo ngày.
- Cancelled booking count theo ngày.
- Total tickets sold theo ngày.

### 5.3. Revenue Definition

`total_revenue` được xác nhận là:

```txt
Net Revenue
```

Công thức nghiệp vụ:

```txt
Net Revenue
= Tổng số tiền thanh toán SUCCESS
- Tổng số tiền refund SUCCESS
```

Không tính:

- Payment `PENDING`.
- Payment `PROCESSING`.
- Payment `FAILED`.
- Payment `CANCELLED`.
- Booking chưa thanh toán.

Refund được trừ vào:

```txt
Ngày refund xảy ra
```

Doanh thu theo ngày có thể âm nếu tổng refund trong ngày lớn hơn tổng payment success trong ngày.

### 5.4. Promotion và Membership Discount

Analytics không cần biết discount đến từ nguồn nào.

Revenue được aggregate theo:

```txt
paidAmount cuối cùng đã được Payment Service xác nhận
```

Ví dụ:

```txt
Original amount = 240000
Silver membership discount = 10%
Paid amount = 216000
Revenue = 216000
```

Promotion discount và Membership discount đã được phản ánh trong `paidAmount`.

Hệ thống không có Score Redeem và không dùng điểm để thanh toán vé.

### 5.5. Kafka Aggregation

Analytics Service sử dụng Kafka làm hướng tích hợp chính trong Sprint 2.

Các event chính:

```txt
PAYMENT_SUCCEEDED
PAYMENT_REFUNDED
BOOKING_CANCELLED
```

Analytics là read model và không được dùng để:

- Xác nhận booking.
- Xác nhận payment.
- Quyết định user có nhận vé hay không.

### 5.6. Schema Alignment

Contract yêu cầu schema alignment trước implementation để bổ sung:

```txt
daily_revenue_stats.total_tickets_sold
movie_daily_revenue_stats
processed_analytics_events
```

## 6. Analytics Là Read Model

Analytics Service chỉ lưu dữ liệu đã tổng hợp.

### Source of truth

| Dữ liệu                        | Source of truth              |
| ------------------------------ | ---------------------------- |
| Payment status và paid amount  | Payment Service              |
| Booking status và ticket count | Booking Service              |
| Movie ID và movie metadata     | Movie Service                |
| Promotion và Membership discount | Đã phản ánh trong paidAmount từ Payment Service |
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

Gateway route chính thức:

```txt
/api/analytics/**
```

### 8.2. Analytics Service Direct URL

Chỉ dùng cho debug hoặc backend integration:

```txt
http://localhost:8089
```

Port `8089` không được expose trực tiếp ra Internet.

### 8.3. Query Flow

```txt
Admin / Manager Dashboard
→ API Gateway
→ /api/analytics/**
→ Analytics Service :8089
→ Analytics Database
```

### 8.4. Kafka Aggregation Flow

```txt
Payment / Booking Service
→ Kafka event
→ Analytics Consumer
→ Idempotency check
→ Upsert aggregate tables
→ Record processed event
```

Internal HTTP aggregation endpoints trong tài liệu chỉ dùng cho testing, recovery hoặc controlled administration nếu được bảo vệ; Kafka là hướng tích hợp chính.

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

Dashboard, Revenue Summary, Daily Revenue và các API query theo khoảng ngày chỉ hỗ trợ tối đa:

```txt
92 ngày
```

Nếu khoảng thời gian vượt quá 92 ngày:

```txt
ANALYTICS_DATE_RANGE_TOO_LARGE
```

Response:

```json
{
  "success": false,
  "message": "Analytics date range must not exceed 92 days",
  "errorCode": "ANALYTICS_DATE_RANGE_TOO_LARGE",
  "data": null,
  "errors": null
}
```

Giới hạn này áp dụng để bảo đảm hiệu năng query trong Sprint 2.

Các báo cáo dài hơn 92 ngày phải được chia thành nhiều request hoặc xử lý thông qua cơ chế export/report riêng trong sprint sau.

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
/api/analytics/**
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
| GET    | `/api/analytics/dashboard`               | Admin/Manager             | Dashboard summary          |
| GET    | `/api/analytics/revenue/daily`           | Admin/Manager             | Doanh thu từng ngày        |
| GET    | `/api/analytics/revenue/summary`         | Admin/Manager             | Tổng hợp trong khoảng ngày |
| GET    | `/api/analytics/movies`                  | Admin/Manager             | Revenue theo phim          |
| GET    | `/api/analytics/movies/{movieId}`        | Admin/Manager             | Chi tiết aggregate phim    |
| GET    | `/api/analytics/movies/top`              | Admin/Manager             | Top phim                   |
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
GET /api/analytics/dashboard
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
      "totalTicketsSold": 645,
      "averageOrderValue": 39062.5
    },
    "today": {
      "date": "2026-06-21",
      "totalRevenue": 850000,
      "successfulBookings": 22,
      "cancelledBookings": 2,
      "totalTicketsSold": 44
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

### Dashboard Scope

Dashboard response không trả object `currentMonth`.

Frontend cần dữ liệu tổng hợp theo tháng sẽ gọi:

```http
GET /api/analytics/revenue/summary
```

với:

```txt
startDate = ngày đầu tháng
endDate = ngày hiện tại hoặc ngày cuối tháng
```

Việc loại bỏ `currentMonth` giúp tránh trùng logic giữa Dashboard API và Revenue Summary API.

### Ticket Count Source

`summary.totalTicketsSold` được tính bằng tổng:

```txt
daily_revenue_stats.total_tickets_sold
```

trong khoảng ngày được query.

Field này chỉ được implement sau khi Schema Alignment MR bổ sung cột tương ứng.

### Average Order Value

Công thức:

```txt
averageOrderValue =
totalRevenue / totalSuccessfulBookings
```

Quy tắc tính toán Backend:

```txt
Data type: BigDecimal
Scale: 2
Rounding mode: HALF_UP
```

Ví dụ:

```java
averageOrderValue = totalRevenue.divide(
    BigDecimal.valueOf(totalSuccessfulBookings),
    2,
    RoundingMode.HALF_UP
);
```

Nếu `totalSuccessfulBookings = 0`:

```txt
averageOrderValue = 0.00
```

Không dùng `double` hoặc `float` để tính dữ liệu tài chính.

Quy tắc `BigDecimal`, scale `2`, `HALF_UP` cũng áp dụng cho các phép chia tài chính tương tự như `averageRevenuePerTicket`.

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
GET /api/analytics/revenue/daily
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
        "totalTicketsSold": 36,
        "updatedAt": "2026-06-19T23:59:00"
      },
      {
        "statDate": "2026-06-20",
        "totalRevenue": 0,
        "successfulBookings": 0,
        "cancelledBookings": 0,
        "totalTicketsSold": 0,
        "updatedAt": null
      },
      {
        "statDate": "2026-06-21",
        "totalRevenue": 850000,
        "successfulBookings": 22,
        "cancelledBookings": 2,
        "totalTicketsSold": 44,
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
GET /api/analytics/revenue/summary
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
    "totalTicketsSold": 645,
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
    "totalTicketsSold": 0,
    "averageOrderValue": 0,
    "currency": "VND",
    "timezone": "Asia/Ho_Chi_Minh",
    "lastUpdatedAt": null
  }
}
```

---

# 17. Movie Revenue APIs

## 17.1. Get Movie Revenue List

### Endpoint

```http
GET /api/analytics/movies
```

### Query Parameters

| Parameter | Type | Required | Validation |
|---|---|---:|---|
| page | integer | No | >= 0 |
| size | integer | No | 1–100 |
| movieId | number | No | > 0 |
| movieTitle | string | No | Search text |
| startDate | date | No | Phải đi cùng `endDate` |
| endDate | date | No | Không trước `startDate` |
| sortBy | string | No | `totalRevenue`, `totalTicketsSold`, `movieTitle` |
| direction | string | No | `asc`, `desc` |

### Query Mode

Nếu không truyền `startDate/endDate`:

```txt
Trả lifetime aggregate từ movie_revenue_stats
```

Nếu truyền đủ `startDate/endDate`:

```txt
Aggregate từ movie_daily_revenue_stats trong khoảng ngày
```

### Response Success

```json
{
  "success": true,
  "message": "Movie revenue statistics retrieved successfully",
  "data": {
    "mode": "DATE_RANGE",
    "period": {
      "startDate": "2026-06-01",
      "endDate": "2026-06-21"
    },
    "content": [
      {
        "movieId": 101,
        "movieTitle": "Avengers",
        "totalTicketsSold": 420,
        "totalRevenue": 48600000,
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

## 17.2. Get Movie Revenue Detail

### Endpoint

```http
GET /api/analytics/movies/{movieId}
```

### Query Parameters

| Parameter | Type | Required |
|---|---|---:|
| startDate | date | No |
| endDate | date | No |

Không truyền date range thì trả lifetime aggregate.

Có date range thì aggregate từ `movie_daily_revenue_stats`.

### Response Success

```json
{
  "success": true,
  "message": "Movie revenue statistics retrieved successfully",
  "data": {
    "movieId": 101,
    "movieTitle": "Avengers",
    "mode": "DATE_RANGE",
    "startDate": "2026-06-01",
    "endDate": "2026-06-21",
    "totalTicketsSold": 420,
    "totalRevenue": 48600000,
    "averageRevenuePerTicket": 115714.29,
    "currency": "VND",
    "updatedAt": "2026-06-21T21:30:00"
  }
}
```

Nếu ticket count bằng `0`:

```txt
averageRevenuePerTicket = 0
```

### Error

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

## 17.3. Get Movie Revenue Trend

### Endpoint

```http
GET /api/analytics/movies/{movieId}/trend
```

### Query Parameters

| Parameter | Type | Required |
|---|---|---:|
| startDate | date | Yes |
| endDate | date | Yes |
| includeEmptyDates | boolean | No |

### Response Success

```json
{
  "success": true,
  "message": "Movie revenue trend retrieved successfully",
  "data": {
    "movieId": 101,
    "movieTitle": "Avengers",
    "startDate": "2026-06-19",
    "endDate": "2026-06-21",
    "currency": "VND",
    "statistics": [
      {
        "statDate": "2026-06-19",
        "ticketsSold": 20,
        "revenue": 2300000
      },
      {
        "statDate": "2026-06-20",
        "ticketsSold": 0,
        "revenue": 0
      },
      {
        "statDate": "2026-06-21",
        "ticketsSold": 24,
        "revenue": 2780000
      }
    ]
  }
}
```

# 19. Top Movies API

## 19.1. Endpoint

```http
GET /api/analytics/movies/top
```

### Query Parameters

| Parameter | Type | Required | Validation |
|---|---|---:|---|
| metric | string | No | `REVENUE`, `TICKETS_SOLD` |
| limit | integer | No | 1–50, mặc định 10 |
| direction | string | No | Mặc định `desc` |
| startDate | date | No | Phải đi cùng `endDate` |
| endDate | date | No | Không trước `startDate` |

Không truyền date range:

```txt
Top movie theo lifetime aggregate
```

Có date range:

```txt
Top movie từ movie_daily_revenue_stats trong khoảng ngày
```

### Response Success

```json
{
  "success": true,
  "message": "Top movies retrieved successfully",
  "data": {
    "metric": "REVENUE",
    "mode": "DATE_RANGE",
    "period": {
      "startDate": "2026-06-01",
      "endDate": "2026-06-21"
    },
    "currency": "VND",
    "movies": [
      {
        "rank": 1,
        "movieId": 101,
        "movieTitle": "Avengers",
        "totalTicketsSold": 420,
        "totalRevenue": 48600000
      }
    ],
    "lastUpdatedAt": "2026-06-21T21:30:00"
  }
}
```

Không có dữ liệu trả:

```json
{
  "success": true,
  "message": "Top movies retrieved successfully",
  "data": {
    "metric": "REVENUE",
    "mode": "DATE_RANGE",
    "movies": [],
    "lastUpdatedAt": null
  }
}
```

# 20. Kafka Event Aggregation

## 20.1. Primary Integration Direction

Analytics Service sử dụng Kafka để cập nhật read model.

Các event chính:

```txt
PAYMENT_SUCCEEDED
PAYMENT_REFUNDED
BOOKING_CANCELLED
```

Internal HTTP endpoint trong các phần dưới chỉ là contract hỗ trợ testing, replay có kiểm soát hoặc recovery. Production integration ưu tiên Kafka consumer.

## 20.2. Event Time Consistency

Analytics Consumer bắt buộc sử dụng field:

```txt
occurredAt
```

từ event để xác định `statDate`.

Processing rule:

```txt
event.occurredAt
→ parse ISO-8601 datetime
→ convert sang Asia/Ho_Chi_Minh
→ lấy LocalDate
→ sử dụng làm statDate
```

Không được sử dụng:

```java
LocalDateTime.now()
LocalDate.now()
Instant.now()
```

để xác định ngày aggregate cho event.

System time chỉ có thể dùng cho:

- `processedAt`
- Logging
- Monitoring
- Technical timestamp

Không dùng system time thay thế business event time.

Ví dụ:

```txt
Event occurredAt: 2026-06-21T23:59:58+07:00
Consumer xử lý lúc: 2026-06-22T00:00:03+07:00
statDate bắt buộc: 2026-06-21
```

Rebuild và replay cũng phải giữ nguyên `statDate` được tính từ `occurredAt`, không dựa trên thời điểm replay.

## 20.3. `PAYMENT_SUCCEEDED`

Topic đề xuất:

```txt
payment.payment-succeeded.v1
```

Message key:

```txt
paymentId
```

Payload:

```json
{
  "eventId": "PAYMENT-SUCCESS-3001",
  "eventType": "PAYMENT_SUCCEEDED",
  "sourceService": "payment-service",
  "occurredAt": "2026-06-21T20:12:00",
  "paymentId": 3001,
  "bookingId": 1001,
  "paidAmount": 216000,
  "currency": "VND",
  "movieId": 101,
  "movieTitle": "Avengers",
  "ticketCount": 2
}
```

### Processing

```txt
Validate event
→ Insert/check processed_analytics_events
→ Convert occurredAt sang Asia/Ho_Chi_Minh
→ Determine statDate
→ Upsert daily_revenue_stats
→ Upsert movie_revenue_stats
→ Upsert movie_daily_revenue_stats
→ Commit transaction
```

### Daily Update

```txt
totalRevenue += paidAmount
totalBookingsCount += 1
totalTicketsSold += ticketCount
```

### Lifetime Movie Update

```txt
totalRevenue += paidAmount
totalTicketsSold += ticketCount
```

### Daily Movie Update

```txt
revenue += paidAmount
ticketsSold += ticketCount
```

`paidAmount` là số tiền cuối cùng sau Promotion hoặc Membership discount.

---

## 20.4. `PAYMENT_REFUNDED`

Topic đề xuất:

```txt
payment.payment-refunded.v1
```

Message key:

```txt
paymentId
```

Payload:

```json
{
  "eventId": "PAYMENT-REFUNDED-3001",
  "eventType": "PAYMENT_REFUNDED",
  "sourceService": "payment-service",
  "occurredAt": "2026-06-22T09:00:00",
  "paymentId": 3001,
  "bookingId": 1001,
  "refundAmount": 216000,
  "currency": "VND",
  "movieId": 101,
  "movieTitle": "Avengers",
  "refundedTicketCount": 2
}
```

### Refund Business Window

Theo business rule hiện tại, refund chỉ được xử lý trong tối đa:

```txt
1 giờ kể từ thời điểm đặt vé/thanh toán thành công
```

Analytics không chịu trách nhiệm quyết định refund có hợp lệ hay không.

Payment Service là service xác thực refund window trước khi publish:

```txt
PAYMENT_REFUNDED
```

Khi Analytics nhận event refund hợp lệ:

```txt
statDate = ngày của occurredAt trong event refund
```

Không backdate về ngày payment gốc.

Ví dụ:

```txt
Payment success: 2026-06-21T23:40:00+07:00
Refund occurred: 2026-06-22T00:20:00+07:00
```

Analytics ghi nhận:

```txt
Payment revenue: 2026-06-21
Refund deduction: 2026-06-22
```

Rebuild hoặc replay event phải giữ nguyên quy tắc này.

Refund được trừ vào ngày refund xảy ra.

Update:

```txt
daily_revenue_stats.totalRevenue -= refundAmount

movie_revenue_stats.totalRevenue -= refundAmount
movie_revenue_stats.totalTicketsSold -= refundedTicketCount

movie_daily_revenue_stats.revenue -= refundAmount
movie_daily_revenue_stats.ticketsSold -= refundedTicketCount
```

Không để lifetime ticket count nhỏ hơn `0`.

Daily net revenue có thể âm.

---

## 20.5. `BOOKING_CANCELLED`

Topic đề xuất:

```txt
booking.booking-cancelled.v1
```

Message key:

```txt
bookingId
```

Payload:

```json
{
  "eventId": "BOOKING-CANCELLED-1002",
  "eventType": "BOOKING_CANCELLED",
  "sourceService": "booking-service",
  "occurredAt": "2026-06-21T20:30:00",
  "bookingId": 1002,
  "previousStatus": "PENDING_PAYMENT",
  "currentStatus": "CANCELLED",
  "reason": "USER_CANCELLED"
}
```

Processing:

```txt
cancelledBookingsCount += 1
```

Không giảm revenue từ `BOOKING_CANCELLED`.

Nếu booking đã thanh toán và được refund, revenue chỉ được trừ bởi `PAYMENT_REFUNDED` để tránh double subtraction.

# 23. Idempotency Rules

## 23.1. Processed Events Table

Mỗi Kafka event phải có `eventId` duy nhất.

Consumer xử lý trong cùng transaction:

```txt
Insert processed_analytics_events(eventId)
→ Nếu duplicate key: bỏ qua event
→ Nếu insert thành công: update toàn bộ aggregate
→ Commit
```

Cùng event không được aggregate hai lần.

### Duplicate Handling

Duplicate event được acknowledge thành công và không tạo side effect mới.

Ví dụ internal/debug response:

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

Không được chỉ dựa vào Kafka consumer offset để bảo đảm idempotency.

# 24. Atomic Aggregation Rules

Một payment success event cập nhật:

```txt
daily_revenue_stats
+
movie_revenue_stats
+
movie_daily_revenue_stats
+
processed_analytics_events
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

```sql
INSERT INTO daily_revenue_stats (
  stat_date,
  total_revenue,
  total_bookings_count,
  cancelled_bookings_count,
  total_tickets_sold
)
VALUES (
  :statDate,
  :revenueDelta,
  :bookingDelta,
  :cancelledDelta,
  :ticketDelta
)
ON DUPLICATE KEY UPDATE
  total_revenue = total_revenue + :revenueDelta,
  total_bookings_count = total_bookings_count + :bookingDelta,
  cancelled_bookings_count = cancelled_bookings_count + :cancelledDelta,
  total_tickets_sold = total_tickets_sold + :ticketDelta,
  updated_at = CURRENT_TIMESTAMP;
```

### Lifetime Movie Stats

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
  :ticketDelta,
  :revenueDelta
)
ON DUPLICATE KEY UPDATE
  movie_title = :movieTitle,
  total_tickets_sold = total_tickets_sold + :ticketDelta,
  total_revenue = total_revenue + :revenueDelta,
  updated_at = CURRENT_TIMESTAMP;
```

### Daily Movie Stats

```sql
INSERT INTO movie_daily_revenue_stats (
  movie_id,
  movie_title,
  stat_date,
  tickets_sold,
  revenue
)
VALUES (
  :movieId,
  :movieTitle,
  :statDate,
  :ticketDelta,
  :revenueDelta
)
ON DUPLICATE KEY UPDATE
  movie_title = :movieTitle,
  tickets_sold = tickets_sold + :ticketDelta,
  revenue = revenue + :revenueDelta,
  updated_at = CURRENT_TIMESTAMP;
```

Các ví dụ SQL chỉ mô tả direction; implementation phải bảo đảm atomic transaction và không để lifetime ticket count âm.

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

## 32.1. Net Revenue

Analytics ghi nhận:

```txt
Net Revenue
= Payment SUCCESS paidAmount
- Payment REFUNDED refundAmount
```

## 32.2. Included

Chỉ tính:

```txt
Payment SUCCESS
```

với `paidAmount` cuối cùng được Payment Service xác nhận.

## 32.3. Excluded

Không tính:

```txt
Payment PENDING
Payment PROCESSING
Payment FAILED
Payment CANCELLED
Booking PENDING_PAYMENT
```

## 32.4. Refund

Khi nhận `PAYMENT_REFUNDED`:

```txt
totalRevenue -= refundAmount
```

Refund được ghi nhận vào ngày refund.

## 32.5. Promotion và Membership Discount

Analytics không tách nguồn discount.

Ví dụ:

```txt
Original amount = 240000
Silver membership discount = 10%
Paid amount = 216000
Revenue = 216000
```

Tương tự, nếu có Promotion discount, Analytics vẫn chỉ nhận và aggregate `paidAmount` cuối cùng.

Hệ thống không có Score Redeem, không dùng điểm để thanh toán vé và không quy đổi score thành tiền.

# 33. Booking and Ticket Count Rules

### Successful Booking và Ticket Count

Event `PAYMENT_SUCCEEDED` là event enriched dùng để cập nhật:

```txt
daily_revenue_stats.total_bookings_count
daily_revenue_stats.total_tickets_sold
movie_revenue_stats.total_tickets_sold
movie_daily_revenue_stats.tickets_sold
```

Payload phải chứa `bookingId`, `movieId` và `ticketCount`.

Không tăng các count trên từ event khác cho cùng nghiệp vụ để tránh double counting.

### Cancelled Booking Count

`BOOKING_CANCELLED` chỉ tăng:

```txt
daily_revenue_stats.cancelled_bookings_count
```

Không giảm successful booking hoặc ticket count nếu booking chưa từng thanh toán thành công.

Nếu booking đã thanh toán rồi refund, adjustment doanh thu và ticket count đến từ `PAYMENT_REFUNDED`.

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

Sprint 2 có thể dùng seed data cho development/test query, nhưng hướng tích hợp chính thức là Kafka aggregation.

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

Bắt buộc bổ sung:

```txt
daily_revenue_stats.total_tickets_sold
```

để Dashboard và Revenue Summary trả ticket count theo date range.

## 40.2. Movie Revenue by Date

Bắt buộc bổ sung:

```txt
movie_daily_revenue_stats
```

với:

```txt
UNIQUE(movie_id, stat_date)
```

để hỗ trợ:

- Revenue theo date range.
- Ticket count theo date range.
- Movie trend.
- Top movie theo period.

## 40.3. Processed Events

Bắt buộc bổ sung:

```txt
processed_analytics_events
```

với `event_id UNIQUE` để Kafka consumer replay an toàn.

## 40.4. Net Revenue

`total_revenue` được xác nhận là Net Revenue.

Contract không yêu cầu tách `gross_revenue`, `refund_amount`, `net_revenue` trong Sprint 2 nếu dashboard chỉ cần số net cuối cùng.

## 40.5. Rebuild Job Tracking

Schema chưa có `analytics_jobs`.

Nếu implement async rebuild đầy đủ, cần issue riêng hoặc external job tracking.

## 40.6. Cancellation Type

`cancelled_bookings_count` đang gộp user cancel, system timeout và admin cancel.

Nếu cần breakdown phải bổ sung dimension sau.

## 40.7. Updated Timestamp

Implementation phải chủ động set `updated_at` trong upsert hoặc dùng `ON UPDATE CURRENT_TIMESTAMP`.

## 40.8. Related Issue

```txt
[Database] Align Analytics Schema with Analytics API Contract
```

Schema Alignment MR phải merge trước Backend implementation.

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
* Kafka event publishing contract ngoài ba event đã chốt hoặc outbox production nâng cao.
* Direct access database service khác.
* Backend code trong issue contract này.
* Schema update ngoài review process.

---

# 42. Implementation Issue Direction

Implementation chỉ bắt đầu sau khi:

```txt
Analytics Contract MR được merge
+
Analytics Schema Alignment MR được merge
+
SQL và Physical ERD đã đồng bộ
```

Các implementation issue đề xuất:

```txt
[Backend] Implement Analytics Dashboard and Daily Revenue APIs

[Backend] Implement Lifetime and Date-Range Movie Analytics APIs

[Backend] Implement Kafka Analytics Event Consumers

[Backend] Implement Analytics Recalculation and Rebuild Direction
```

Thứ tự đề xuất:

```txt
Schema Alignment
→ Kafka Consumer Foundation
→ Daily/Lifetime Aggregation
→ Movie Date-Range Aggregation
→ Dashboard Query
→ Recalculate/Rebuild
```

Seed data chỉ phục vụ development/test, không thay thế Kafka integration chính thức.

# 43. Acceptance Criteria

* [ ] Có schema Sprint 0 baseline và schema alignment direction.
* [ ] Có source-of-truth clarification.
* [ ] Có eventual consistency notes.
* [ ] Có timezone và currency rule.
* [ ] Có date range validation.
* [ ] Có maximum query range.
* [ ] Có dashboard summary API.
* [ ] Có daily revenue API.
* [ ] Có revenue summary API.
* [ ] Có lifetime movie revenue API.
* [ ] Có movie revenue theo date range.
* [ ] Có movie revenue trend API.
* [ ] Có top movie API.
* [ ] Có empty data behavior.
* [ ] Có revenue calculation rules.
* [ ] Có refund direction.
* [ ] Có booking/ticket count rules.
* [ ] Có internal aggregation direction.
* [ ] Có scheduled recalculation direction.
* [ ] Có rebuild direction.
* [ ] Có Kafka event aggregation.
* [ ] Có processed events idempotency.
* [ ] Có atomic update rules.
* [ ] Có seed data direction.
* [ ] Có Admin/Internal security classification.
* [ ] Có error code catalog.
* [ ] Có schema alignment requirements.
* [ ] Khang review feasibility.
* [ ] Contract sẵn sàng implementation.
* [ ] MR target `develop`.

---

# 44. Review Decisions

Analytics Service Owner đã review và xác nhận:

1. Analytics Service port:

   ```txt
   8089
   ```

2. API Gateway route:

   ```txt
   /api/analytics/**
   ```

3. Revenue sử dụng:

   ```txt
   Net Revenue
   ```

4. Refund được trừ vào ngày refund xảy ra.

5. Analytics aggregate `paidAmount` cuối cùng sau Promotion hoặc Membership discount.

6. Hệ thống không có Score Redeem và không dùng điểm để thanh toán.

7. Analytics sử dụng Kafka aggregation.

8. Các event chính:

   ```txt
   PAYMENT_SUCCEEDED
   PAYMENT_REFUNDED
   BOOKING_CANCELLED
   ```

9. Analytics hỗ trợ cả:

   ```txt
   Lifetime statistics
   Date-range statistics
   Movie revenue trend
   ```

10. Bắt buộc bổ sung:

    ```txt
    daily_revenue_stats.total_tickets_sold
    movie_daily_revenue_stats
    processed_analytics_events
    ```

11. Analytics là read model, không phải source of truth của Booking hoặc Payment.

12. Schema alignment phải hoàn thành trước Backend implementation.

13. Dashboard và Revenue Summary chỉ cho query tối đa:

    ```txt
    92 ngày
    ```

14. Dashboard response không trả `currentMonth`.

15. Frontend cần dữ liệu tháng phải gọi Revenue Summary API.

16. Average Order Value sử dụng:

    ```txt
    BigDecimal
    Scale 2
    RoundingMode.HALF_UP
    ```

17. Consumer bắt buộc dùng `occurredAt` để xác định `statDate`.

18. Consumer không được dùng system time để xác định ngày aggregate.

19. Refund được ghi nhận vào ngày refund xảy ra, kể cả khi refund sang ngày hôm sau.

20. Refund business window tối đa 1 giờ được kiểm tra bởi Payment Service trước khi publish event.

21. Rebuild và replay phải dựa trên `occurredAt`, không backdate về transaction gốc.

# 45. Lịch Sử Chỉnh Sửa

| Ngày       | Nội dung                                                         | Người thực hiện  |
| ---------- | ---------------------------------------------------------------- | ---------------- |
| 21/06/2026 | Khởi tạo Analytics Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật theo review của Analytics Service Owner: Kafka aggregation, Net Revenue, movie date-range/trend, daily ticket count, processed events và schema alignment | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật quyết định review cuối: query limit 92 ngày, remove `currentMonth`, BigDecimal HALF_UP scale 2, `occurredAt` consistency và refund window 1 giờ | Dương Thiện Nhân |

Các thay đổi schema chỉ được ghi nhận tại đây sau khi schema MR tương ứng đã được merge.
