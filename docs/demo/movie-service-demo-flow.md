# Movie Service Sprint 3 Demo Flow

## 1. Goal

Demo cuối Sprint 3 phải chứng minh **Movie Service** đã trở thành **Cinema Catalog & Showtime Management Service**, không còn là CRUD movie / room / showtime đơn giản.

Movie Service cần thể hiện được đầy đủ các năng lực chính:

* Public movie catalog cho customer.
* Cinema catalog.
* Auditorium và seat layout.
* Showtime scheduling.
* Showtime lifecycle.
* Showtime pricing.
* Showtime validation rules.
* Showtime status history.
* Boundary rõ ràng với Booking Service.

---

## 2. Customer Demo Flow

```text
Home / Movie Listing
→ Movie Detail
→ Select City / Cinema / Date
→ Showtime List
→ Showtime Detail
→ Seat Layout with Price
```

---

## 3. Step-by-step Demo

### Step 1 — View Now Showing Movies

#### API

```http
GET /api/movies?status=now-showing
```

#### Expected

* Customer thấy danh sách phim đang chiếu.
* Mỗi phim có:

  * Poster
  * Title
  * Age rating
  * Genres
  * Release date
* Customer không thấy:

  * `DRAFT` movie
  * `INACTIVE` movie
  * Deleted movie

---

### Step 2 — View Coming Soon Movies

#### API

```http
GET /api/movies?status=coming-soon
```

#### Expected

* Customer thấy danh sách phim sắp chiếu.
* Customer không thấy:

  * Draft movie
  * Inactive movie
  * Deleted movie

---

### Step 3 — View Movie Detail

#### API

```http
GET /api/movies/{movieSlug}
```

#### Expected

Response trả về đầy đủ thông tin movie detail, bao gồm:

* Movie basic info
* Genres
* Directors
* Actors
* Character names
* Production companies
* Movie versions
* Poster
* Banner
* Trailer
* Still images
* Translations nếu có

---

### Step 4 — Select City / Cinema / Date

#### APIs

```http
GET /api/cinemas?city=Ho%20Chi%20Minh
```

```http
GET /api/movies/{movieSlug}/showtimes?city=Ho%20Chi%20Minh&date=2026-07-20
```

#### Expected

* Customer thấy cinema list theo city.
* Customer thấy showtimes được group theo cinema.
* Showtime hiển thị rõ movie version, ví dụ:

  * `IMAX Vietsub`
  * `2D Lồng tiếng`
  * `4DX Vietsub`
  * `2D Phụ đề`

---

### Step 5 — View Cinema Detail

#### API

```http
GET /api/cinemas/{cinemaSlug}
```

#### Expected

Response trả về thông tin cinema detail, bao gồm:

* Cinema name
* City
* District
* Address
* Hotline
* Timezone
* Images / gallery
* Operating hours
* Active status

---

### Step 6 — View Showtime Detail

#### API

```http
GET /api/showtimes/{showtimePublicId}
```

#### Expected

Response trả về thông tin showtime detail, bao gồm:

* Movie
* Movie version
* Cinema
* Auditorium
* Start time
* End time
* Status là `OPEN_FOR_BOOKING`

---

### Step 7 — View Seat Layout with Price

#### API

```http
GET /api/showtimes/{showtimePublicId}/seat-layout
```

#### Expected

Response đủ dữ liệu để Frontend render sơ đồ ghế:

* Seats render được theo row / column.
* Có seat code, ví dụ:

  * `A1`
  * `A2`
  * `F7`
* Có seat type, ví dụ:

  * `STANDARD`
  * `VIP`
  * `COUPLE`
* Có price theo từng seat.
* Currency là `VND`.
* Có field `blockedForShowtime` nếu ghế bị block riêng cho suất chiếu đó.
* Không trả về trạng thái `HELD` / `BOOKED`.

#### Important

Movie Service chỉ trả về master seat layout và pricing.

Trạng thái ghế động như `AVAILABLE`, `HELD`, `BOOKED` thuộc trách nhiệm của Booking Service.

---

## 4. Admin / Runtime Validation Demo

### Case 1 — Reject Overlapping Showtime

#### Action

Admin tạo showtime trong cùng auditorium với occupied interval bị overlap với showtime khác.

#### Expected

```txt
SHOWTIME_OVERLAP_CONFLICT
```

#### Meaning

Hệ thống phải reject vì một auditorium không thể có hai suất chiếu bị trùng thời gian sử dụng phòng.

Occupied interval nên bao gồm:

* Showtime start time
* Showtime end time
* Cleaning buffer nếu có

---

### Case 2 — Reject Showtime During Cinema Closure

#### Action

Admin tạo showtime trong khoảng thời gian cinema đang có active closure period.

#### Expected

```txt
CINEMA_CLOSURE_CONFLICT
```

#### Meaning

Nếu cinema đang đóng cửa theo lịch closure, hệ thống không cho phép tạo hoặc mở showtime trong khoảng thời gian đó.

---

### Case 3 — Reject Showtime During Auditorium Maintenance

#### Action

Admin tạo showtime trong khoảng thời gian auditorium đang có maintenance window.

#### Expected

```txt
AUDITORIUM_MAINTENANCE_CONFLICT
```

#### Meaning

Nếu auditorium đang bảo trì, hệ thống không cho phép tạo hoặc mở showtime trong khoảng thời gian bảo trì đó.

---

### Case 4 — Reject Open Showtime Without Price

#### Action

Admin mở showtime trước khi setting price cho toàn bộ active seat types trong auditorium.

#### Expected

```txt
SHOWTIME_PRICE_MISSING
```

#### Meaning

Showtime không được chuyển sang trạng thái `OPEN_FOR_BOOKING` nếu chưa có đủ giá vé cho các loại ghế active.

Ví dụ auditorium có các active seat types:

* `STANDARD`
* `VIP`
* `COUPLE`

Thì showtime phải có đủ price cho cả 3 loại ghế này trước khi open.

---

### Case 5 — Cancel Showtime with Reason

#### Action

Admin cancel showtime với cancellation reason.

#### Expected

* Showtime status được chuyển thành `CANCELLED`.
* `cancellation_reason` được lưu lại.
* `showtime_status_history` được tạo.
* Customer không còn thấy showtime này trên các API public.

#### Related API

```http
PATCH /api/admin/showtimes/{showtimePublicId}/cancel
```

---

### Case 6 — Verify Audit

#### Action

Admin creates / updates / deletes movie, cinema hoặc showtime.

#### Expected

Audit fields được set đúng:

* `created_by` được set khi tạo mới.
* `updated_by` được set khi cập nhật.
* `deleted_by` được set khi soft delete.

#### Important

Soft delete không xoá physical record khỏi database.

Customer APIs không được trả về soft-deleted records.

---

## 5. Non-goal Demo Clarification

Movie Service demo không bao gồm các phần sau:

* Seat hold
* Booking creation
* Payment
* Promotion
* Ticket issuing
* Real-time seat availability

Các trạng thái seat theo thời gian thực như:

* `AVAILABLE`
* `HELD`
* `BOOKED`

sẽ thuộc Booking Service ở sprint sau.

---

## 6. Demo Success Criteria

Sprint 3 demo được xem là đạt nếu chứng minh được các điểm sau:

### Customer side

* Customer xem được danh sách phim đang chiếu.
* Customer xem được danh sách phim sắp chiếu.
* Customer xem được movie detail đầy đủ metadata.
* Customer lọc được cinema / city / date.
* Customer xem được showtime theo movie hoặc cinema.
* Customer xem được showtime detail.
* Customer xem được seat layout kèm giá vé.
* Customer không thấy draft / inactive / deleted data.
* Customer không thấy showtime đã cancel / close / finish.
* Customer không thấy trạng thái ghế `HELD` / `BOOKED`.

### Admin side

* Admin tạo và quản lý movie metadata.
* Admin tạo và quản lý cinema.
* Admin tạo và quản lý auditorium.
* Admin tạo và quản lý seat layout.
* Admin tạo và quản lý showtime.
* Admin setting price cho showtime.
* Admin open / close / cancel / finish showtime.
* Admin xem được status history.
* System reject các business case sai.

### Validation side

Hệ thống phải reject đúng các case:

* Showtime overlap.
* Showtime nằm trong cinema closure period.
* Showtime nằm trong auditorium maintenance window.
* Showtime thiếu price.
* Showtime cancel không có reason nếu business rule yêu cầu reason.
* Showtime dùng inactive movie / inactive movie version / inactive cinema / inactive auditorium.

---

## 7. Suggested Demo Order

```text
1. Seed dữ liệu movie, genre, people, production company, version, media.
2. Seed dữ liệu cinema, auditorium, seat type, seat layout.
3. Admin tạo showtime ở trạng thái DRAFT.
4. Admin set price cho showtime.
5. Admin open showtime.
6. Customer xem movie listing.
7. Customer vào movie detail.
8. Customer chọn city / cinema / date.
9. Customer xem showtime list.
10. Customer vào showtime detail.
11. Customer xem seat layout with price.
12. Admin demo các validation conflict.
13. Admin cancel showtime với reason.
14. Customer refresh lại và không còn thấy showtime đã cancel.
15. Admin kiểm tra status history và audit fields.
```

---

## 8. Important Boundary Statement

Movie Service là source of truth cho:

* Movie metadata
* Cinema metadata
* Auditorium metadata
* Seat master layout
* Showtime schedule
* Showtime lifecycle
* Showtime pricing
* Showtime blocked seats

Movie Service không phải source of truth cho:

* Seat reservation
* Seat lock
* Booking lifecycle
* Payment lifecycle
* Real-time seat availability
* Ticket issuing

Vì vậy, trong Sprint 3, việc Seat Layout API không trả về `HELD` / `BOOKED` là đúng thiết kế.
