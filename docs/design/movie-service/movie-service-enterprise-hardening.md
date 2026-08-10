# Movie Service Enterprise Hardening Decisions

## 1. Public Identifier Strategy

Movie Service dùng `BIGINT AUTO_INCREMENT` làm primary key nội bộ để tối ưu join/index trong database.

Tuy nhiên, customer-facing APIs không nên expose trực tiếp auto-increment ID.

Các entity public-facing cần có:

- `public_id` cho opaque public identifier;
- `slug` cho SEO-friendly resources như movie/cinema nếu phù hợp.

Public APIs nên ưu tiên:

- `GET /api/movies/{movieSlug}`;
- `GET /api/cinemas/{cinemaSlug}`;
- `GET /api/showtimes/{showtimePublicId}`;
- `GET /api/showtimes/{showtimePublicId}/seat-layout`.

Internal numeric `id` chỉ dùng nội bộ service, admin/debug context hoặc mapping service-to-service khi cần.

---

## 2. Soft Delete Strategy

Các entity có dữ liệu lịch sử hoặc có thể được tham chiếu bởi showtime/booking tương lai không được hard delete.

Soft delete dùng:

- `deleted_at`;
- `deleted_by`;
- status fields nếu phù hợp.

Customer APIs luôn filter:

```sql
deleted_at IS NULL
```
Các bảng có slug unique như movies, cinemas, genres dùng generated active slug column để enforce unique slug chỉ cho non-deleted records.
### Ví dụ:
```sql
active_slug VARCHAR(280)
  GENERATED ALWAYS AS (
    CASE WHEN deleted_at IS NULL THEN slug ELSE NULL END
  ) STORED,
UNIQUE KEY uk_movies_active_slug (active_slug)
```
Lý do không dùng UNIQUE(slug, deleted_at) trực tiếp: MySQL cho phép nhiều NULL trong unique key, nên cách đó không enforce tốt active duplicate slug.

## 3. Cinema Timezone Strategy
Mỗi cinema có cột:
```sql
timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh'
```
Sprint 3 mặc định hệ thống vận hành ở Việt Nam, nhưng không hardcode timezone trong business logic.

Showtime validation liên quan đến operating hours cần dùng timezone của cinema.

Datetime response cho frontend nên thống nhất theo ISO-8601.

## 4. Cleaning Buffer Strategy
Mỗi auditorium có:
```sql
cleaning_buffer_minutes INT NOT NULL DEFAULT 15
```
>showtimes.end_time là thời điểm phim kết thúc.

Thời gian phòng bị chiếm dụng thực tế là:
```txt
occupied_start = start_time
occupied_end = end_time + cleaning_buffer_minutes
```
Khi check overlap, service phải dùng occupied interval, không chỉ dùng end_time.
## 5. Showtime Concurrency Strategy
Showtime creation phải an toàn khi có nhiều admin tạo suất chiếu đồng thời.
Không được chỉ làm:
```sql
SELECT COUNT(*) ...
INSERT ...
```
vì có thể bị race condition.

Chiến lược Sprint 3:

Tạo showtime trong database transaction.

Lock target auditorium row trước khi check overlap.
```sql
SELECT id
FROM auditoriums
WHERE id = :auditoriumId
FOR UPDATE;
```
Sau khi có lock, service mới validate:

auditorium belongs to cinema;

movie version belongs to movie;

release window;

operating hours;

cinema closure;

auditorium maintenance;

showtime overlap;

cleaning buffer.

Nếu pass thì insert showtime.
Future option:

Redis distributed lock với key movie:showtime:create:auditorium:{auditoriumId}:{date} nếu service scale horizontal mạnh.

Sprint 3 mặc định dùng DB transaction + row-level lock.
## 6. i18n Strategy
Movie Service hỗ trợ future i18n bằng bảng:
```txt
movie_translations
```
Bảng này lưu title/synopsis theo locale.

Default fields trong movies vẫn dùng cho demo tiếng Việt.

Frontend Sprint 3 chưa bắt buộc phải làm multi-language UI.
## 7. Showtime Blocked Seats Strategy

Movie Service không quản lý HELD/BOOKED state.

Tuy nhiên, Movie Service có thể quản lý showtime_blocked_seats cho operator/admin block ghế theo từng suất chiếu.

Ví dụ:

- ghế dành cho khách mời;
- ghế khóa vận hành;
- ghế hỏng riêng trong suất đó;
- ghế dành cho event/VIP.
> showtime_blocked_seats không đại diện cho Booking state.

Các trạng thái sau vẫn thuộc Booking Service:

- HELD;
- BOOKED;
- PAID;
- TICKETED.

## 8. Aggregate Movie Create Strategy

Admin có thể quản lý movie theo từng phần nhỏ, nhưng hệ thống cũng nên hỗ trợ aggregate create API để tránh dữ liệu dở dang.

API đề xuất: 
```http
POST /api/admin/movies/full
```
API này tạo trong một transaction:

- movie;
- genres;
- credits;
- production companies;
- versions;
- media.

Nếu bất kỳ phần nào fail validation, rollback toàn bộ.

Mục tiêu: tránh tình trạng movie được tạo nhưng thiếu poster/version/genre.