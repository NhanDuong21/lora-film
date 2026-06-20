# Movie Service API Specification

## 1. Thông Tin Chung

| Mục                      | Nội dung                                         |
| ------------------------ | ------------------------------------------------ |
| Service                  | `movie-service`                                  |
| Feature                  | Movie, Genre, Room, Seat and Showtime Management |
| API liên quan            | Movies, Genres, Rooms, Seats, Showtimes          |
| Người phụ trách Contract | Dương Thiện Nhân                                 |
| Người phụ trách Backend  | Phan Tuấn Thành                                  |
| Trạng thái               | Draft / Ready for Review                         |
| Sprint                   | Sprint 2 - Core Service API Foundation           |
| Ngày cập nhật            | 20/06/2026                                       |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API thuộc `movie-service` của hệ thống **LoraFilm**.

Mục tiêu chính:

* Thống nhất API Contract giữa Backend, Frontend, API Gateway và các service liên quan.
* Làm cơ sở để Backend triển khai Movie Service mà không tự thay đổi endpoint hoặc response.
* Giúp Frontend chuẩn bị các màn hình danh sách phim, chi tiết phim, suất chiếu và sơ đồ ghế.
* Phân định rõ dữ liệu do Movie Service quản lý và dữ liệu thuộc Booking Service.
* Chuẩn hóa request, response, validation, business rule, status code và error code.

---

## 3. Phạm Vi Movie Service

Movie Service sở hữu và quản lý các bảng:

```txt
movies
genres
movies_genres
rooms
seats
showtimes
```

Movie Service chịu trách nhiệm quản lý:

* Thông tin phim.
* Thể loại phim.
* Quan hệ nhiều-nhiều giữa phim và thể loại.
* Phòng chiếu.
* Sơ đồ ghế vật lý của từng phòng.
* Lịch suất chiếu.
* Giá vé cơ bản theo suất chiếu.
* Trạng thái phim, phòng, ghế và suất chiếu.

Movie Service không chịu trách nhiệm quản lý:

* Trạng thái ghế đang được giữ.
* Ghế đã được bán.
* Booking.
* Ticket.
* Payment.
* Promotion.
* Doanh thu thực tế.

Các nghiệp vụ trên thuộc Booking, Payment, Promotion và Analytics Service.

---

## 4. Database-per-Service và Logical Reference

Hệ thống áp dụng nguyên tắc Database-per-Service.

Các service khác chỉ lưu ID của Movie Service dưới dạng logical reference:

```txt
movieId
showtimeId
roomId
seatId
```

Ví dụ:

```txt
Booking Service
→ lưu showtimeId và seatId

Analytics Service
→ lưu movieId và movieTitle dạng snapshot
```

Không tạo foreign key vật lý từ database của Booking hoặc Analytics sang database của Movie Service.

Movie Service không được truy cập trực tiếp database của service khác.

---

## 5. API Gateway và Service URL

### 5.1. API Gateway URL

Frontend phải gọi API thông qua API Gateway:

```txt
http://localhost:8080
```

### 5.2. Movie Service Direct URL

Chỉ dùng cho Backend debug hoặc test riêng service:

```txt
http://localhost:<MOVIE_SERVICE_PORT>
```

Port chính thức phải được xác nhận theo cấu hình project.

### 5.3. Request Flow

```txt
React Frontend
→ API Gateway
→ Movie Service
→ Movie Service Database
```

Frontend không được gọi trực tiếp port nội bộ của Movie Service.

---

## 6. Phân Loại API

### Public API

Cho phép người dùng chưa đăng nhập truy cập:

```txt
GET /api/movies
GET /api/movies/{movieId}
GET /api/genres
GET /api/movies/{movieId}/showtimes
GET /api/showtimes/{showtimeId}
```

### Protected API

Yêu cầu Bearer Token:

```txt
GET /api/rooms/{roomId}/seats
```

Endpoint này chỉ trả cấu trúc vật lý của ghế, không trả trạng thái giữ ghế theo thời gian thực.

### Admin API

Yêu cầu role `ADMIN` hoặc quyền quản lý tương ứng:

```txt
POST  /api/admin/movies
PUT   /api/admin/movies/{movieId}
PATCH /api/admin/movies/{movieId}/status

POST  /api/admin/genres
PUT   /api/admin/genres/{genreId}

POST  /api/admin/rooms
PUT   /api/admin/rooms/{roomId}
PATCH /api/admin/rooms/{roomId}/status

POST  /api/admin/rooms/{roomId}/seats
PUT   /api/admin/seats/{seatId}
PATCH /api/admin/seats/{seatId}/status

POST  /api/admin/showtimes
PUT   /api/admin/showtimes/{showtimeId}
PATCH /api/admin/showtimes/{showtimeId}/status
```

---

# 7. Movie Query APIs

## 7.1. Get Movie List

### Endpoint

```http
GET /api/movies
```

### Query Parameters

| Parameter   |   Type | Required | Mô tả                                  |
| ----------- | -----: | -------: | -------------------------------------- |
| page        | number |       No | Trang hiện tại, mặc định `0`           |
| size        | number |       No | Số phần tử, mặc định `10`, tối đa `50` |
| search      | string |       No | Tìm theo tên phim                      |
| status      | string |       No | Lọc theo trạng thái phim               |
| genreId     | number |       No | Lọc theo thể loại                      |
| releaseFrom |   date |       No | Ngày phát hành từ                      |
| releaseTo   |   date |       No | Ngày phát hành đến                     |
| sort        | string |       No | Ví dụ `releaseDate,desc`               |

### Response Success

Status: `200 OK`

```json
{
  "success": true,
  "message": "Movies retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Avengers",
        "durationMinutes": 180,
        "releaseDate": "2026-06-20",
        "endDate": "2026-07-20",
        "posterUrl": "https://example.com/poster.jpg",
        "trailerUrl": "https://example.com/trailer.mp4",
        "ageRating": "T16",
        "status": "NOW_SHOWING",
        "genres": [
          {
            "id": 1,
            "name": "Action"
          }
        ]
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

### Empty Result

Nếu không có phim phù hợp:

```json
{
  "success": true,
  "message": "Movies retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 10,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

Không trả `404` khi danh sách rỗng.

---

## 7.2. Get Movie Detail

### Endpoint

```http
GET /api/movies/{movieId}
```

### Response Success

Status: `200 OK`

```json
{
  "success": true,
  "message": "Movie retrieved successfully",
  "data": {
    "id": 1,
    "title": "Avengers",
    "description": "Movie description",
    "durationMinutes": 180,
    "director": "Director Name",
    "actor": "Actor A, Actor B",
    "releaseDate": "2026-06-20",
    "endDate": "2026-07-20",
    "posterUrl": "https://example.com/poster.jpg",
    "trailerUrl": "https://example.com/trailer.mp4",
    "ageRating": "T16",
    "status": "NOW_SHOWING",
    "genres": [
      {
        "id": 1,
        "name": "Action"
      }
    ]
  }
}
```

### Response Error

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Movie not found",
  "errorCode": "MOVIE_NOT_FOUND",
  "data": null
}
```

---

# 8. Genre APIs

## 8.1. Get Genre List

### Endpoint

```http
GET /api/genres
```

### Response Success

```json
{
  "success": true,
  "message": "Genres retrieved successfully",
  "data": [
    {
      "id": 1,
      "genreName": "Action"
    },
    {
      "id": 2,
      "genreName": "Animation"
    }
  ]
}
```

---

# 9. Showtime Query APIs

## 9.1. Get Showtimes by Movie

### Endpoint

```http
GET /api/movies/{movieId}/showtimes
```

### Query Parameters

| Parameter |   Type | Required | Mô tả                   |
| --------- | -----: | -------: | ----------------------- |
| date      |   date |       No | Ngày cần lấy suất chiếu |
| status    | string |       No | Trạng thái suất chiếu   |

### Response Success

```json
{
  "success": true,
  "message": "Showtimes retrieved successfully",
  "data": [
    {
      "id": 10,
      "movieId": 1,
      "roomId": 2,
      "roomName": "Cinema 02",
      "screenType": "IMAX",
      "startTime": "2026-06-20T19:00:00",
      "endTime": "2026-06-20T22:00:00",
      "ticketPrice": 120000,
      "status": "OPEN"
    }
  ]
}
```

Danh sách rỗng trả `200 OK` với `data: []`.

---

## 9.2. Get Showtime Detail

### Endpoint

```http
GET /api/showtimes/{showtimeId}
```

### Response Success

```json
{
  "success": true,
  "message": "Showtime retrieved successfully",
  "data": {
    "id": 10,
    "movie": {
      "id": 1,
      "title": "Avengers",
      "durationMinutes": 180,
      "ageRating": "T16"
    },
    "room": {
      "id": 2,
      "roomName": "Cinema 02",
      "screenType": "IMAX"
    },
    "startTime": "2026-06-20T19:00:00",
    "endTime": "2026-06-20T22:00:00",
    "ticketPrice": 120000,
    "status": "OPEN"
  }
}
```

---

# 10. Room and Seat Query APIs

## 10.1. Get Room Seats

### Endpoint

```http
GET /api/rooms/{roomId}/seats
```

### Response Success

```json
{
  "success": true,
  "message": "Room seats retrieved successfully",
  "data": {
    "roomId": 2,
    "roomName": "Cinema 02",
    "screenType": "IMAX",
    "totalSeats": 100,
    "seats": [
      {
        "id": 101,
        "seatRow": "A",
        "seatNumber": 1,
        "seatType": "STANDARD",
        "status": "ACTIVE"
      }
    ]
  }
}
```

### Quan trọng

Movie Service chỉ trả:

* Cấu trúc ghế.
* Loại ghế.
* Trạng thái vật lý của ghế.

Movie Service không trả:

```txt
AVAILABLE
HELD
BOOKED
```

Các trạng thái giữ ghế và đặt ghế thuộc Booking Service.

---

# 11. Admin Movie APIs

## 11.1. Create Movie

### Endpoint

```http
POST /api/admin/movies
```

### Request Body

```json
{
  "title": "Avengers",
  "description": "Movie description",
  "durationMinutes": 180,
  "director": "Director Name",
  "actor": "Actor A, Actor B",
  "releaseDate": "2026-06-20",
  "endDate": "2026-07-20",
  "posterUrl": "https://example.com/poster.jpg",
  "trailerUrl": "https://example.com/trailer.mp4",
  "ageRating": "T16",
  "status": "UPCOMING",
  "genreIds": [1, 2]
}
```

### Business Validation

* `title` không được rỗng.
* `durationMinutes > 0`.
* `endDate` không được trước `releaseDate`.
* Tất cả `genreIds` phải tồn tại.
* `status` phải thuộc enum hợp lệ.

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Movie created successfully",
  "data": {
    "id": 1,
    "title": "Avengers",
    "status": "UPCOMING"
  }
}
```

---

## 11.2. Update Movie

```http
PUT /api/admin/movies/{movieId}
```

Request body sử dụng cấu trúc tương tự Create Movie.

---

## 11.3. Update Movie Status

```http
PATCH /api/admin/movies/{movieId}/status
```

```json
{
  "status": "NOW_SHOWING"
}
```

---

# 12. Admin Genre APIs

## 12.1. Create Genre

```http
POST /api/admin/genres
```

```json
{
  "genreName": "Science Fiction"
}
```

Genre name phải unique, không phân biệt hoa thường sau khi normalize.

## 12.2. Update Genre

```http
PUT /api/admin/genres/{genreId}
```

---

# 13. Admin Room APIs

## 13.1. Create Room

```http
POST /api/admin/rooms
```

```json
{
  "roomName": "Cinema 02",
  "totalSeats": 100,
  "screenType": "IMAX",
  "status": "ACTIVE"
}
```

### Business Rules

* `roomName` phải unique.
* `totalSeats > 0`.
* `totalSeats` phải khớp với tổng số seat active sau khi cấu hình hoàn tất.

## 13.2. Update Room

```http
PUT /api/admin/rooms/{roomId}
```

## 13.3. Update Room Status

```http
PATCH /api/admin/rooms/{roomId}/status
```

---

# 14. Admin Seat APIs

## 14.1. Create Seats for Room

```http
POST /api/admin/rooms/{roomId}/seats
```

```json
{
  "seats": [
    {
      "seatRow": "A",
      "seatNumber": 1,
      "seatType": "STANDARD",
      "status": "ACTIVE"
    },
    {
      "seatRow": "A",
      "seatNumber": 2,
      "seatType": "STANDARD",
      "status": "ACTIVE"
    }
  ]
}
```

### Business Rules

Trong cùng một phòng, cặp sau phải unique:

```txt
seatRow + seatNumber
```

Không cho tạo trùng:

```txt
Room 1 - A1
Room 1 - A1
```

## 14.2. Update Seat

```http
PUT /api/admin/seats/{seatId}
```

## 14.3. Update Seat Status

```http
PATCH /api/admin/seats/{seatId}/status
```

---

# 15. Admin Showtime APIs

## 15.1. Create Showtime

```http
POST /api/admin/showtimes
```

```json
{
  "movieId": 1,
  "roomId": 2,
  "startTime": "2026-06-20T19:00:00",
  "ticketPrice": 120000,
  "status": "SCHEDULED"
}
```

Backend tính:

```txt
endTime = startTime + movie.durationMinutes
```

Không nên tin `endTime` do Frontend tự gửi.

### Showtime Conflict Rule

Một suất chiếu mới không được trùng thời gian với suất chiếu khác trong cùng phòng.

Điều kiện conflict:

```txt
newStartTime < existingEndTime + cleanupBuffer
AND
newEndTime + cleanupBuffer > existingStartTime
```

Đề xuất:

```txt
cleanupBuffer = 15 phút
```

Giá trị này cần được reviewer xác nhận hoặc chuyển thành cấu hình hệ thống.

### Response Conflict

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Showtime conflicts with another showtime in this room",
  "errorCode": "SHOWTIME_SCHEDULE_CONFLICT",
  "data": null
}
```

## 15.2. Update Showtime

```http
PUT /api/admin/showtimes/{showtimeId}
```

## 15.3. Update Showtime Status

```http
PATCH /api/admin/showtimes/{showtimeId}/status
```

---

# 16. Enum và Status Đề Xuất

## 16.1. Movie Status

```txt
UPCOMING
NOW_SHOWING
ENDED
INACTIVE
```

## 16.2. Room Status

```txt
ACTIVE
MAINTENANCE
INACTIVE
```

## 16.3. Seat Status

```txt
ACTIVE
MAINTENANCE
INACTIVE
```

## 16.4. Seat Type

```txt
STANDARD
VIP
COUPLE
```

## 16.5. Showtime Status

```txt
SCHEDULED
OPEN
CLOSED
CANCELLED
COMPLETED
```

Các enum cuối cùng phải được Thành xác nhận trước khi implement.

---

# 17. Error Code Chuẩn

| Error Code                   | HTTP Status | Ý nghĩa                           |
| ---------------------------- | ----------: | --------------------------------- |
| `MOVIE_NOT_FOUND`            |         404 | Không tìm thấy phim               |
| `GENRE_NOT_FOUND`            |         404 | Không tìm thấy thể loại           |
| `ROOM_NOT_FOUND`             |         404 | Không tìm thấy phòng              |
| `SEAT_NOT_FOUND`             |         404 | Không tìm thấy ghế                |
| `SHOWTIME_NOT_FOUND`         |         404 | Không tìm thấy suất chiếu         |
| `MOVIE_INVALID_DATE_RANGE`   |         400 | Ngày phim không hợp lệ            |
| `MOVIE_INVALID_DURATION`     |         400 | Thời lượng phim không hợp lệ      |
| `GENRE_ALREADY_EXISTS`       |         409 | Thể loại đã tồn tại               |
| `ROOM_ALREADY_EXISTS`        |         409 | Phòng đã tồn tại                  |
| `SEAT_ALREADY_EXISTS`        |         409 | Ghế đã tồn tại trong phòng        |
| `SHOWTIME_SCHEDULE_CONFLICT` |         409 | Trùng lịch suất chiếu             |
| `SHOWTIME_INVALID_TIME`      |         400 | Thời gian suất chiếu không hợp lệ |
| `INVALID_STATUS_TRANSITION`  |         409 | Chuyển trạng thái không hợp lệ    |
| `VALIDATION_ERROR`           |         400 | Request không hợp lệ              |
| `UNAUTHORIZED`               |         401 | Chưa đăng nhập                    |
| `FORBIDDEN`                  |         403 | Không đủ quyền                    |
| `INTERNAL_SERVER_ERROR`      |         500 | Lỗi hệ thống                      |

---

# 18. Quy Tắc Xóa Dữ Liệu

Không ưu tiên hard delete cho:

```txt
movies
rooms
seats
showtimes
```

Thay vào đó sử dụng status:

```txt
INACTIVE
CANCELLED
ENDED
```

Lý do:

* Các ID có thể đã được Booking Service lưu dưới dạng logical reference.
* Cần giữ dữ liệu lịch sử.
* Tránh làm mất khả năng hiển thị booking/ticket cũ.

Hard delete chỉ được cân nhắc khi dữ liệu chưa từng được sử dụng và có xác nhận nghiệp vụ rõ ràng.

---

# 19. Authorization Rules

| Nhóm API                     | Quyền                                     |
| ---------------------------- | ----------------------------------------- |
| Xem phim/thể loại/suất chiếu | Public                                    |
| Xem sơ đồ ghế vật lý         | Protected                                 |
| Quản lý phim                 | ADMIN                                     |
| Quản lý thể loại             | ADMIN                                     |
| Quản lý phòng và ghế         | ADMIN                                     |
| Quản lý suất chiếu           | ADMIN hoặc EMPLOYEE có permission phù hợp |

Nếu hệ thống sử dụng permission chi tiết, có thể định nghĩa:

```txt
MOVIE_READ
MOVIE_MANAGE
ROOM_MANAGE
SHOWTIME_MANAGE
```

---

# 20. Cross-Service Integration Notes

## Booking Service

Booking Service sử dụng:

```txt
showtimeId
seatId
```

Booking Service chịu trách nhiệm quản lý:

```txt
seat reservation
seat availability
booking status
ticket
```

## Analytics Service

Analytics Service có thể lưu:

```txt
movieId
movieTitle
```

`movieTitle` được lưu dạng snapshot để báo cáo lịch sử không bị ảnh hưởng khi tên phim thay đổi.

## Frontend

Frontend chỉ gọi:

```txt
http://localhost:8080/api/...
```

Frontend không gọi trực tiếp Movie Service.

---

# 21. Scope Chưa Bao Gồm

* Automatic Showtime Scheduler.
* Real-time seat locking.
* Seat availability theo booking.
* Movie rating và review.
* Actor management riêng.
* Cinema branch/multi-location management.
* Dynamic pricing.
* Recommendation engine.
* Revenue calculation.
* Production media upload.

Các chức năng này sẽ được tạo thành issue riêng khi có requirement.

---

# 22. Acceptance Criteria của Contract

Contract được xem là hoàn thành khi:

* Có danh sách endpoint theo từng nhóm nghiệp vụ.
* Có request/response mẫu.
* Có pagination/filter/sort.
* Có business rules.
* Có enum/status.
* Có HTTP status và error code.
* Có phân loại Public/Protected/Admin API.
* Có logical reference notes.
* Không tạo Room/Seat/Showtime Service riêng.
* Thành review và xác nhận feasibility.
* Tài liệu đủ rõ để tách implementation issues.
