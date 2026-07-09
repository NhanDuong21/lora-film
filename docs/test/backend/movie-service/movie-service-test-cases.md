# Movie Service Backend Test Matrix

## 1. Movie Catalog Tests

- Create movie success.
- Create movie with duration <= 0 should fail.
- Create movie with endDate before releaseDate should fail.
- Create movie without required fields should fail.
- Publish movie without genre should fail.
- Publish movie without active version should fail.
- Publish movie without primary poster should fail.
- Customer cannot see DRAFT movie.
- Customer cannot see INACTIVE movie.
- Customer can see NOW_SHOWING movie.
- Customer can see UPCOMING movie in coming-soon API.
- Soft-deleted movie is hidden from customer APIs.
- Duplicate active slug should fail.
- Reuse slug after soft delete should be allowed if active_slug strategy is implemented.

---

## 2. Movie Version / Media Tests

- Create movie version success.
- Duplicate movie version should fail.
- Inactive version is hidden from customer.
- Showtime cannot use inactive movie version.
- Create movie media success.
- Primary poster validation works.
- Media display order works.
- Inactive media is hidden from customer.

---

## 3. People / Production Tests

- Create person success.
- Add director to movie success.
- Add actor with character_name success.
- Add production company to movie success.
- One person can participate in multiple movies.
- One movie can have multiple production companies.
- Inactive/deleted person should not be used in new credits.

---

## 4. Cinema Tests

- Create cinema success.
- Cinema without city/address should fail.
- Activate cinema without operating hours should fail.
- Customer cannot see DRAFT cinema.
- Customer cannot see INACTIVE/PERMANENTLY_CLOSED cinema.
- Customer can see ACTIVE cinema.
- Cinema timezone defaults to Asia/Ho_Chi_Minh.
- Create operating hours success.
- Invalid day_of_week should fail.
- close_time <= open_time should fail.
- Create closure period success.
- Invalid closure time should fail.
- Cancelled closure period does not block showtime.

---

## 5. Auditorium / Seat Layout Tests

- Create auditorium success.
- Auditorium name duplicate in same cinema should fail.
- Same auditorium name in different cinema should pass.
- Capacity <= 0 should fail.
- cleaning_buffer_minutes < 0 should fail.
- Create maintenance window success.
- Invalid maintenance time should fail.
- Create seat type success.
- Create bulk seat layout success.
- Duplicate seat code in same auditorium should fail.
- Duplicate position row/column in same auditorium should fail.
- Seat type inactive should fail when assigning new seat.
- Seat layout returns ordered row/column.
- Capacity validation works.

---

## 6. Showtime Tests

- Create showtime success.
- Create showtime with end_time <= start_time should fail.
- Create showtime with movie version not belonging to movie should fail.
- Create showtime with auditorium not belonging to cinema should fail.
- Create showtime with inactive movie version should fail.
- Create showtime with inactive cinema should fail.
- Create showtime with inactive auditorium should fail.
- Create showtime outside movie release window should fail.
- Create showtime outside cinema operating hours should fail.
- Create showtime overlapping another non-cancelled showtime should fail.
- Create showtime overlapping existing showtime including cleaning buffer should fail.
- Create showtime overlapping cinema closure period should fail.
- Create showtime overlapping auditorium maintenance window should fail.
- Cancelled showtime should not block overlap if business rule allows.
- Open showtime without prices should fail.
- Cancel showtime without reason should fail.
- Invalid status transition should fail.
- Status history is created when status changes.
- Customer only sees OPEN_FOR_BOOKING showtimes.

---

## 7. Showtime Concurrency Tests

- Simulate two concurrent requests creating overlapping showtimes in the same auditorium.
- Expected:
  - one request succeeds;
  - one request fails with SHOWTIME_OVERLAP_CONFLICT;
  - database contains no overlapping showtimes.

Implementation note:
- Service should use transaction + SELECT FOR UPDATE on auditorium row before overlap check.

---

## 8. Pricing Tests

- Set showtime prices success.
- Negative price should fail.
- Currency defaults to VND.
- Missing price for active seat type should block opening showtime.
- Seat layout returns price by seat type.
- Price is read from showtime_prices snapshot.
- Cannot update prices for FINISHED/CANCELLED showtime.

---

## 9. Showtime Blocked Seats Tests

- Admin blocks seat for showtime success.
- Blocking seat not belonging to showtime auditorium should fail.
- Duplicate blocked seat should fail.
- Cancel blocked seat success.
- Seat layout marks blockedForShowtime.
- Blocked seat is not HELD/BOOKED state.

---

## 10. Internal Booking Context Tests

- Internal API requires internal token.
- Invalid internal token should fail.
- Valid internal token should pass.
- Showtime not found should fail.
- Showtime not OPEN_FOR_BOOKING should fail.
- Seat not belonging to showtime auditorium should fail.
- Inactive seat should fail.
- Blocked seat should fail.
- Missing price should fail.
- Total amount is calculated by Movie Service.
- Response includes movie/cinema/auditorium/seat/price context.
- Response does not include HELD/BOOKED state.

---

## 11. Audit / Security Tests

- Admin create entity sets created_by.
- Admin update entity sets updated_by.
- Admin soft delete entity sets deleted_by/deleted_at.
- Client cannot override created_by/updated_by/deleted_by.
- Customer cannot call admin APIs.
- Customer cannot call internal APIs.
- Admin can call admin APIs.
- Error response format is consistent.

---

## 12. Customer API Visibility Tests

- Customer cannot see draft movie.
- Customer cannot see inactive movie.
- Customer cannot see deleted movie.
- Customer cannot see inactive cinema.
- Customer cannot see cancelled showtime.
- Customer cannot see closed showtime.
- Customer cannot see finished showtime.
- Customer only sees active media/version.