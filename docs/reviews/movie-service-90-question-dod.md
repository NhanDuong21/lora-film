# Movie Service 90-question Definition of Done Mapping

## Evidence Rule

Mỗi câu hỏi phải có ít nhất một hoặc nhiều evidence:

- DB support;
- API support;
- validation/rule support;
- test evidence;
- frontend/runtime evidence.

Nếu chỉ có field trong DB nhưng chưa có API, validation hoặc test thì chưa được xem là done.

Status values:

- Planned;
- Implemented;
- Tested;
- Demoed;
- Deferred.

---

## Mapping Table

| # | Question | DB Support | API Support | Validation / Rule | Test Evidence | Frontend / Runtime Evidence | Status |
|---|---|---|---|---|---|---|---|
| 1 | Phim này đang ở trạng thái gì? | `movies.status` | `GET /api/movies/{movieSlug}` | Customer hides DRAFT/INACTIVE | Movie visibility tests | Movie detail page | Planned |
| 2 | Phim có title/original title/slug/synopsis/duration/age rating/release/end date không? | `movies` | Movie detail API | Movie validation | Movie create/update tests | Movie detail page | Planned |
| 3 | Phim thuộc genre nào? | `genres`, `movie_genres` | Movie detail API | Genre must be active | Genre mapping tests | Movie detail page | Planned |
| 4 | Phim có bao nhiêu phiên bản chiếu? | `movie_versions` | `GET /api/movies/{slug}` | Only active versions public | Movie version tests | Movie detail page | Planned |
| 5 | Phiên bản nào là 2D/3D/IMAX/4DX? | `movie_versions.format` | Movie versions API | Format validation | Version validation tests | Movie detail page | Planned |
| 6 | Phiên bản nào là Vietsub/lồng tiếng/audio/subtitle? | `movie_versions` | Movie versions API | Language validation | Version tests | Movie detail page | Planned |
| 7 | Phiên bản nào active/inactive? | `movie_versions.status` | Movie versions API | Hide inactive | Visibility tests | Movie detail page | Planned |
| 8 | Phim có poster/banner/trailer/still images nào? | `movie_media` | Movie media API | Only active media | Media tests | Movie detail page | Planned |
| 9 | Media nào primary? | `movie_media.is_primary` | Movie media API | Primary poster required | Publish validation tests | Movie detail page | Planned |
| 10 | Media hiển thị theo thứ tự nào? | `movie_media.display_order` | Movie media API | Order by display_order | Media order tests | Movie detail page | Planned |
| 11 | Ai là đạo diễn? | `people`, `movie_credits` | Movie credits API | role_type DIRECTOR | Credit tests | Movie detail page | Planned |
| 12 | Ai là diễn viên? | `people`, `movie_credits` | Movie credits API | role_type ACTOR | Credit tests | Movie detail page | Planned |
| 13 | Diễn viên đóng vai gì? | `movie_credits.character_name` | Movie credits API | Actor may have character | Credit tests | Movie detail page | Planned |
| 14 | Ai là biên kịch/producer? | `movie_credits.role_type` | Movie credits API | WRITER/PRODUCER roles | Credit tests | Movie detail page | Planned |
| 15 | Hãng sản xuất là hãng nào? | `production_companies`, `movie_production_companies` | Movie detail API | Company role production | Company tests | Movie detail page | Planned |
| 16 | Hãng phát hành/distributor là ai? | `movie_production_companies.role` | Movie detail API | DISTRIBUTOR role | Company tests | Movie detail page | Planned |
| 17 | Một người có thể đóng nhiều phim không? | `movie_credits` | Admin credits API | many-to-many | Credit mapping tests | Swagger/Postman | Planned |
| 18 | Một phim có nhiều production companies không? | `movie_production_companies` | Admin production API | many-to-many | Company mapping tests | Swagger/Postman | Planned |
| 19 | Rạp thuộc thành phố/quận nào? | `cinemas.city`, `cinemas.district` | Cinema APIs | Required fields | Cinema tests | Cinema page | Planned |
| 20 | Địa chỉ/hotline/mô tả của rạp là gì? | `cinemas` | Cinema detail API | Required address | Cinema tests | Cinema page | Planned |
| 21 | Rạp có logo/banner/gallery/map không? | `cinema_media` | Cinema media API | Active media only | Cinema media tests | Cinema page | Planned |
| 22 | Rạp đang status gì? | `cinemas.status` | Cinema detail/admin API | Customer only ACTIVE | Visibility tests | Cinema page | Planned |
| 23 | Rạp mở cửa ngày/giờ nào? | `cinema_operating_hours` | Cinema detail API | Operating hours validation | Operating hour tests | Cinema page | Planned |
| 24 | Rạp có closure period không? | `cinema_closure_periods` | Admin closure API | Active closure blocks showtime | Closure tests | Swagger/Postman | Planned |
| 25 | Có tạo showtime khi rạp đóng không? | `cinema_closure_periods`, `cinemas.status` | Admin showtime API | Reject if closed/closure | Showtime tests | Swagger/Postman | Planned |
| 26 | Đóng rạp thì showtime tương lai xử lý sao? | `cinemas.status`, `cinema_closure_periods`, `showtimes` | Admin cinema/showtime APIs | Require cancel/close or block new showtime | Closure tests | Swagger/Postman | Planned |
| 27 | Rạp có bao nhiêu phòng? | `auditoriums` | Cinema detail/admin API | Auditorium belongs to cinema | Auditorium tests | Cinema page | Planned |
| 28 | Phòng thuộc screen/sound type nào? | `auditoriums.screen_type`, `sound_type` | Auditorium API | Type validation | Auditorium tests | Showtime detail | Planned |
| 29 | Phòng capacity bao nhiêu? | `auditoriums.capacity` | Auditorium API | capacity > 0 | Auditorium tests | Admin UI/Postman | Planned |
| 30 | Phòng status gì? | `auditoriums.status` | Auditorium API | Only active for showtime | Auditorium tests | Admin UI/Postman | Planned |
| 31 | Phòng có maintenance window không? | `auditorium_maintenance_windows` | Admin maintenance API | Active maintenance blocks showtime | Maintenance tests | Swagger/Postman | Planned |
| 32 | Có tạo showtime khi phòng maintenance không? | `auditorium_maintenance_windows` | Admin showtime API | Reject overlap maintenance | Showtime tests | Swagger/Postman | Planned |
| 33 | Tên phòng unique trong cùng rạp không? | `UNIQUE(cinema_id, name)` | Admin auditorium API | DB + service validation | Auditorium tests | Swagger/Postman | Planned |
| 34 | Phòng có layout ghế như thế nào? | `seats`, `seat_types` | Seat layout API | Order by position | Seat tests | Seat layout page | Planned |
| 35 | Ghế có row/number/code/position không? | `seats` | Seat layout API | Required fields | Seat tests | Seat layout page | Planned |
| 36 | Ghế thuộc type nào? | `seat_types`, `seats.seat_type_id` | Seat layout API | Seat type active | Seat tests | Seat layout page | Planned |
| 37 | Ghế đôi có pair/group không? | `seats.pair_group` | Seat layout API | Couple seat pairing rule | Seat tests | Seat layout page | Planned |
| 38 | Ghế maintenance/inactive có hiện không? | `seats.status` | Seat layout API | Display master status | Seat visibility tests | Seat layout page | Planned |
| 39 | Capacity có khớp số ghế active không? | `auditoriums.capacity`, `seats` | Admin seat API | Capacity validation | Seat tests | Admin UI/Postman | Planned |
| 40 | Có sửa layout khi đã có showtime tương lai/open không? | `seats`, `showtimes` | Admin seat API | Restrict dangerous updates | Seat layout tests | Swagger/Postman | Planned |
| 41 | Suất chiếu dùng movie nào? | `showtimes.movie_id` | Showtime detail API | Movie exists | Showtime tests | Showtime page | Planned |
| 42 | Suất chiếu dùng movie version nào? | `showtimes.movie_version_id` | Showtime detail API | Version belongs to movie | Showtime tests | Showtime page | Planned |
| 43 | Suất ở rạp/phòng nào? | `showtimes.cinema_id`, `auditorium_id` | Showtime detail API | Auditorium belongs to cinema | Showtime tests | Showtime page | Planned |
| 44 | Start/end time là gì? | `showtimes.start_time`, `end_time` | Showtime detail API | end > start | Showtime tests | Showtime page | Planned |
| 45 | Showtime nằm trong release window không? | `movies.release_date/end_date`, `showtimes` | Admin showtime API | Validate release window | Showtime tests | Swagger/Postman | Planned |
| 46 | Showtime nằm trong giờ mở cửa không? | `cinema_operating_hours`, `cinemas.timezone` | Admin showtime API | Validate by cinema timezone | Showtime tests | Swagger/Postman | Planned |
| 47 | Showtime overlap suất khác không? | `showtimes`, `auditoriums.cleaning_buffer_minutes` | Admin showtime API | Transaction + FOR UPDATE + overlap check | Concurrency/overlap tests | Swagger/Postman | Planned |
| 48 | Showtime overlap maintenance không? | `auditorium_maintenance_windows` | Admin showtime API | Reject overlap | Showtime tests | Swagger/Postman | Planned |
| 49 | Showtime overlap closure không? | `cinema_closure_periods` | Admin showtime API | Reject overlap | Showtime tests | Swagger/Postman | Planned |
| 50 | Showtime có cleaning buffer không? | `auditoriums.cleaning_buffer_minutes` | Admin showtime API | Occupied interval includes buffer | Showtime tests | Swagger/Postman | Planned |
| 51 | Showtime status gì? | `showtimes.status` | Showtime APIs | Lifecycle rule | Status tests | Showtime page | Planned |
| 52 | Customer thấy showtime nào? | `showtimes.status` | Customer showtime APIs | Only OPEN_FOR_BOOKING | Visibility tests | Showtime page | Planned |
| 53 | Ai được open/cancel/close/finish? | Security config | Admin APIs | Admin role required | Security tests | Swagger/Postman | Planned |
| 54 | Lý do cancel là gì? | `showtimes.cancellation_reason` | Cancel API | Reason required | Cancel tests | Swagger/Postman | Planned |
| 55 | Có lưu status history không? | `showtime_status_history` | Status history API | Save on transition | Status history tests | Swagger/Postman | Planned |
| 56 | Suất có giá từng seat type không? | `showtime_prices` | Price APIs | Required for active seat types | Price tests | Seat layout page | Planned |
| 57 | Giá có VND không? | `showtime_prices.currency` | Price APIs | Default VND | Price tests | Seat layout page | Planned |
| 58 | Giá có âm không? | CHECK price >= 0 | Price APIs | Reject negative | Price tests | Swagger/Postman | Planned |
| 59 | Có thiếu giá seat type active không? | `showtime_prices`, `seat_types` | Open showtime API | Reject missing price | Price tests | Swagger/Postman | Planned |
| 60 | Giá snapshot theo showtime không? | `showtime_prices` | Seat layout API | No global dynamic price | Price tests | Seat layout page | Planned |
| 61 | Có sửa giá sau khi open không? | `showtimes.status`, `showtime_prices` | Price API | Status-based restriction | Price tests | Swagger/Postman | Planned |
| 62 | Customer xem now showing không? | `movies.status` | Movie listing API | Visibility | Movie tests | Movie page | Planned |
| 63 | Customer xem coming soon không? | `movies.status` | Movie listing API | Visibility | Movie tests | Movie page | Planned |
| 64 | Customer lọc phim theo genre/city/cinema/date không? | movies/showtimes/cinemas | Movie search API | Filter validation | API filter tests | Movie page | Planned |
| 65 | Customer xem rạp theo city/district không? | `cinemas` | Cinema listing API | Active only | Cinema tests | Cinema page | Planned |
| 66 | Customer xem showtime theo movie/cinema/date không? | `showtimes` | Showtime search API | Open only | Showtime tests | Showtime page | Planned |
| 67 | Customer xem seat layout và price không? | `seats`, `showtime_prices` | Seat layout API | Open showtime only | Seat layout tests | Seat layout page | Planned |
| 68 | Customer thấy draft/inactive/cancelled không? | Status fields | Customer APIs | Hide non-public data | Visibility tests | Runtime evidence | Planned |
| 69 | Admin tạo/sửa/publish movie thế nào? | `movies` | Admin movie APIs | Lifecycle validation | Admin tests | Swagger/Postman | Planned |
| 70 | Admin quản lý people/credits/companies thế nào? | people/credits/companies | Admin APIs | Validation | Admin tests | Swagger/Postman | Planned |
| 71 | Admin quản lý versions thế nào? | `movie_versions` | Admin version APIs | Active/inactive rules | Version tests | Swagger/Postman | Planned |
| 72 | Admin quản lý media thế nào? | `movie_media` | Admin media APIs | Primary/order/status | Media tests | Swagger/Postman | Planned |
| 73 | Admin mở thêm rạp thế nào? | `cinemas` | Admin cinema APIs | Activation rules | Cinema tests | Swagger/Postman | Planned |
| 74 | Admin đóng/bảo trì rạp thế nào? | `cinemas`, `closure_periods` | Admin status/closure APIs | Closure rules | Cinema tests | Swagger/Postman | Planned |
| 75 | Admin thêm phòng thế nào? | `auditoriums` | Admin auditorium APIs | Unique/capacity | Auditorium tests | Swagger/Postman | Planned |
| 76 | Admin tạo seat layout thế nào? | `seats` | Bulk seat API | Duplicate validation | Seat tests | Swagger/Postman | Planned |
| 77 | Admin tạo showtime thế nào? | `showtimes` | Admin showtime API | Full validation | Showtime tests | Swagger/Postman | Planned |
| 78 | Admin set price thế nào? | `showtime_prices` | Price APIs | Price validation | Price tests | Swagger/Postman | Planned |
| 79 | Bản ghi do ai tạo? | `created_by` | Admin APIs | Set from security context | Audit tests | DB/runtime evidence | Planned |
| 80 | Bản ghi do ai cập nhật? | `updated_by` | Admin APIs | Set from security context | Audit tests | DB/runtime evidence | Planned |
| 81 | Ai đổi trạng thái showtime? | `showtime_status_history.changed_by` | Status APIs | Save actor | Status tests | DB/runtime evidence | Planned |
| 82 | Ai cancel showtime và lý do? | `showtimes`, `showtime_status_history` | Cancel API | Reason required | Cancel tests | DB/runtime evidence | Planned |
| 83 | Customer gọi admin API được không? | Security config | Admin APIs | Reject customer | Security tests | Runtime evidence | Planned |
| 84 | Internal API có public không? | Internal token filter | Internal API | Token required | Security tests | Runtime evidence | Planned |
| 85 | Có hard delete lịch sử không? | `deleted_at`, `deleted_by` | Delete APIs | Soft delete | Soft delete tests | DB evidence | Planned |
| 86 | Booking lấy showtime context từ đâu? | showtime/seat/price tables | Internal booking context API | Internal token | API tests | Swagger/Postman | Planned |
| 87 | SeatIds validate thuộc showtime không? | seats/auditorium/showtime | Internal API | Seat belongs to auditorium | Internal API tests | Swagger/Postman | Planned |
| 88 | Total amount authoritative ở đâu? | `showtime_prices` | Internal API | Sum selected seat prices | Price tests | Internal API evidence | Planned |
| 89 | Movie Service trả đủ Booking snapshot không? | showtime/movie/cinema/seat/price | Internal API | Required fields | Internal API tests | Swagger/Postman | Planned |
| 90 | Movie Service tránh HELD/BOOKED không? | No booking state table | Internal/customer APIs | Boundary rule | Review/test evidence | Design/runtime evidence | Planned |