# Movie Service Design Notes

## 1. Current Schema Problems

Schema Movie Service hiện tại chưa đủ cho một production-like cinema system.

Schema cũ gồm:

- movies;
- genres;
- movies_genres;
- rooms;
- seats;
- showtimes.

Các vấn đề chính:

1. Không có `cinemas`, nên hệ thống ngầm hiểu chỉ có một rạp duy nhất.
2. `rooms.room_name` đang unique toàn hệ thống, sai với thực tế vì nhiều rạp đều có thể có "Room 01".
3. `rooms` không thuộc cinema, nên showtime không biết đang chiếu ở rạp nào.
4. `movies.director` và `movies.actor` là text phẳng, không quản lý được people/credits.
5. Không có production company/distributor.
6. Không có movie versions, nên không quản lý được 2D/IMAX/Vietsub/Lồng tiếng.
7. `poster_url` và `trailer_url` nằm trực tiếp trong movies, không hỗ trợ nhiều media.
8. `seats` thiếu `position_row`, `position_column`, nên khó render layout thật.
9. `seat_type` là string, không có bảng `seat_types`, khó quản lý pricing.
10. `showtimes.ticket_price` chỉ có một giá cho cả suất, không hỗ trợ giá theo loại ghế.
11. Không có cinema operating hours.
12. Không có cinema closure periods.
13. Không có auditorium maintenance windows.
14. Không có showtime status history.
15. Không có `created_by`, `updated_by`.
16. Không có soft delete strategy.
17. Không có public identifier strategy, dễ expose auto-increment ID ra public API.
18. Không có timezone cho cinema.
19. Không có cleaning/buffer time cho auditorium.
20. Không có concurrency strategy khi tạo showtime, dễ bị race condition.
21. Không có rule rõ cho publish/unpublish movie.
22. Không có rule rõ cho open/cancel/close showtime.
23. Không có schema hỗ trợ future Booking context.
24. Index hiện tại chỉ hỗ trợ query cơ bản, không giải quyết overlap/business validation.
25. API hiện tại chủ yếu là CRUD, chưa giải quyết customer browsing theo movie/cinema/date/showtime.

Kết luận: không nên tiếp tục vá feature trên schema cũ. Sprint 3 cần redesign Movie Service theo hướng production-like.


# Movie Service Business Rules

## Table of Contents

1. [Movie Lifecycle](#1-movie-lifecycle)
2. [Movie Version Rule](#2-movie-version-rule)
3. [Movie Media Rule](#3-movie-media-rule)
4. [Cinema Lifecycle](#4-cinema-lifecycle)
5. [Cinema Operating Hours Rule](#5-cinema-operating-hours-rule)
6. [Cinema Closure Period Rule](#6-cinema-closure-period-rule)
7. [Auditorium Rule](#7-auditorium-rule)
8. [Seat Layout Rule](#8-seat-layout-rule)
9. [Showtime Creation Rule](#9-showtime-creation-rule)
10. [Showtime Overlap Rule](#10-showtime-overlap-rule)
11. [Showtime Concurrency Rule](#11-showtime-concurrency-rule)
12. [Showtime Lifecycle Rule](#12-showtime-lifecycle-rule)
13. [Showtime Open Rule](#13-showtime-open-rule)
14. [Pricing Rule](#14-pricing-rule)
15. [Showtime Blocked Seats Rule](#15-showtime-blocked-seats-rule)
16. [Public API Identifier Rule](#16-public-api-identifier-rule)
17. [Audit Rule](#17-audit-rule)

---

## 1. Movie Lifecycle

### Movie Status

Movie status gồm:

- `DRAFT`
- `UPCOMING`
- `NOW_SHOWING`
- `ENDED`
- `INACTIVE`

### Customer Visibility

Customer chỉ thấy:

- `UPCOMING` trong coming-soon API
- `NOW_SHOWING` trong now-showing API

Customer không thấy:

- `DRAFT`
- `INACTIVE`
- Soft-deleted movies

### Movie Publish Rule

Movie chỉ được publish nếu:

- Có `title`
- Có `slug`
- `duration_minutes > 0`
- Có `age_rating`
- Có `release_date`
- `end_date` là `null` hoặc `end_date >= release_date`
- Có ít nhất một active genre
- Có ít nhất một active movie version
- Có ít nhất một active primary `POSTER`

---

## 2. Movie Version Rule

Một movie có thể có nhiều version.

### Version Fields

Version gồm:

- `format`
- `audio_language`
- `subtitle_language`
- `dub_language`

### Rule

- Showtime phải trỏ tới `movie_version_id`
- Movie version phải thuộc movie
- Customer chỉ thấy active version
- Không tạo showtime với inactive version

---

## 3. Movie Media Rule

Movie media gồm:

- `POSTER`
- `BANNER`
- `TRAILER`
- `STILL_IMAGE`

### Rule

- Customer chỉ thấy active media
- `display_order` quyết định thứ tự hiển thị
- Movie cần ít nhất một primary `POSTER` để publish
- Service-level validation đảm bảo primary media logic

---

## 4. Cinema Lifecycle

### Cinema Status

Cinema status gồm:

- `DRAFT`
- `ACTIVE`
- `MAINTENANCE`
- `TEMPORARILY_CLOSED`
- `INACTIVE`
- `PERMANENTLY_CLOSED`

### Customer Visibility

Customer chỉ thấy cinema có status:

- `ACTIVE`

### Cinema Activation Rule

Cinema chỉ được chuyển sang `ACTIVE` nếu:

- Có `name`
- Có `slug`
- Có `city`
- Có `address`
- Có operating hours
- Có ít nhất một active auditorium

### Showtime Creation Restriction

Không tạo showtime nếu cinema:

- Không active
- Đang trong closure period
- Đã bị soft delete

---

## 5. Cinema Operating Hours Rule

Cinema có operating hours theo `day_of_week`.

Showtime phải nằm trong giờ mở cửa của cinema theo timezone của cinema.

Default timezone:

```text
Asia/Ho_Chi_Minh
```

---

## 6. Cinema Closure Period Rule

Cinema closure period dùng để đóng rạp tạm thời.

Không được tạo showtime nếu showtime occupied interval overlap với active closure period.

### Closure Period Status

Closure period có status:

- `ACTIVE`
- `CANCELLED`

### Rule

- `ACTIVE` closure period block showtime
- `CANCELLED` closure period không block showtime

---

## 7. Auditorium Rule

Auditorium thuộc cinema.

### Auditorium Status

Auditorium status gồm:

- `DRAFT`
- `ACTIVE`
- `MAINTENANCE`
- `INACTIVE`

### Rule

- Auditorium name unique trong cùng cinema
- Chỉ tạo showtime với active auditorium
- Không tạo showtime nếu auditorium maintenance window overlap
- `capacity > 0`
- `cleaning_buffer_minutes >= 0`

---

## 8. Seat Layout Rule

Seat thuộc auditorium.

### Seat Fields

Seat có:

- `row_label`
- `seat_number`
- `seat_code`
- `position_row`
- `position_column`
- `seat_type`
- `pair_group` nếu là couple seat

### Rule

- `seat_code` unique trong auditorium
- `position_row + position_column` unique trong auditorium
- `seat_type` phải active
- Seat layout trả về theo `position_row` / `position_column`
- Không hard delete seat nếu đã có lịch sử showtime
- Seat `active` / `maintenance` / `inactive` là master state, không phải booking state

---

## 9. Showtime Creation Rule

Showtime chỉ được tạo nếu:

- Movie tồn tại và không deleted
- Movie version tồn tại, active, và thuộc movie
- Cinema tồn tại, active, và không deleted
- Auditorium tồn tại, active, thuộc cinema, và không deleted
- `start_time < end_time`
- Showtime nằm trong release window của movie
- Showtime nằm trong operating hours của cinema
- Showtime không overlap active cinema closure period
- Showtime không overlap active auditorium maintenance window
- Showtime không overlap showtime khác cùng auditorium
- Cleaning buffer được tính khi check overlap

---

## 10. Showtime Overlap Rule

`showtimes.end_time` là thời điểm phim kết thúc.

Thời gian phòng bị chiếm dụng:

```text
occupied_start = start_time
occupied_end = end_time + auditorium.cleaning_buffer_minutes
```

Overlap check phải dùng occupied interval.

### Pseudo SQL

```sql
SELECT COUNT(*)
FROM showtimes s
JOIN auditoriums a ON a.id = s.auditorium_id
WHERE s.auditorium_id = :auditoriumId
  AND s.status NOT IN ('CANCELLED')
  AND s.deleted_at IS NULL
  AND s.start_time < :newOccupiedEnd
  AND TIMESTAMPADD(MINUTE, a.cleaning_buffer_minutes, s.end_time) > :newStartTime;
```

Nếu `count > 0` thì reject với error code:

```text
SHOWTIME_OVERLAP_CONFLICT
```

---

## 11. Showtime Concurrency Rule

Showtime creation phải chạy trong transaction.

Service phải lock target auditorium row trước khi validate overlap:

```sql
SELECT id
FROM auditoriums
WHERE id = :auditoriumId
FOR UPDATE;
```

Sau khi có lock mới validate và insert.

Mục tiêu: tránh hai admin tạo showtime overlap cùng lúc.

---

## 12. Showtime Lifecycle Rule

### Showtime Status

Showtime status gồm:

- `DRAFT`
- `OPEN_FOR_BOOKING`
- `CLOSED`
- `CANCELLED`
- `FINISHED`

### Allowed Transitions

```text
DRAFT -> OPEN_FOR_BOOKING
DRAFT -> CANCELLED

OPEN_FOR_BOOKING -> CLOSED
OPEN_FOR_BOOKING -> CANCELLED

CLOSED -> FINISHED
CLOSED -> CANCELLED

CANCELLED -> no transition
FINISHED -> no transition
```

### Rule

- Cancel showtime bắt buộc có reason
- Mọi status change phải ghi vào `showtime_status_history`

---

## 13. Showtime Open Rule

Showtime chỉ được open nếu:

- Status hiện tại là `DRAFT`
- Showtime ở tương lai
- Movie / movie version / cinema / auditorium còn active
- Không overlap
- Không nằm trong closure / maintenance
- Có price cho tất cả active seat types trong auditorium

Nếu thiếu price, reject với:

```text
SHOWTIME_PRICE_MISSING
```

---

## 14. Pricing Rule

Showtime price là snapshot theo showtime.

### Rule

- `price >= 0`
- `currency` mặc định là `VND`
- Mỗi active seat type trong auditorium phải có price
- Không lấy giá động từ seat_type global khi customer xem seat layout
- Không sửa price nếu showtime `FINISHED` hoặc `CANCELLED`

---

## 15. Showtime Blocked Seats Rule

`showtime_blocked_seats` dùng cho admin / operator block ghế trong một showtime cụ thể.

Đây không phải booking state.

Movie Service không quản lý:

- `HELD`
- `BOOKED`
- `PAID`
- `TICKETED`

---

## 16. Public API Identifier Rule

Public customer APIs không nên expose auto-increment numeric ID.

Customer APIs ưu tiên dùng:

- Movie `slug`
- Cinema `slug`
- Showtime `public_id`
- Seat `public_id` nếu cần

Internal DB vẫn dùng `BIGINT id`.

---

## 17. Audit Rule

Admin-managed records phải có:

- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

Soft-delete records có:

- `deleted_at`
- `deleted_by`

`created_by`, `updated_by`, `deleted_by` là logical user ID từ Auth/User Service.

Không tạo cross-service foreign key sang `user_db`.
