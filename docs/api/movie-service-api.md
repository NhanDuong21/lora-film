# Movie Service API Specification

## 1. Thông Tin Chung

| Mục            | Nội dung                                         |
| -------------- | ------------------------------------------------ |
| Service        | `movie-service`                                  |
| Feature        | Movie, Genre, Room, Seat and Showtime Management |
| API liên quan  | Movies, Genres, Rooms, Seats, Showtimes          |
| Contract Owner | Dương Thiện Nhân                                 |
| Backend Owner  | Phan Tuấn Thành                                  |
| Reviewer       | Phan Tuấn Thành                                  |
| Trạng thái     | Approved                                         |
| Milestone      | Sprint 2 - Core Service API Foundation           |
| Ngày cập nhật  | 23/06/2026                                       |

---

## 2. Mục Tiêu Tài Liệu

Tài liệu này đặc tả các API thuộc `movie-service` của hệ thống **LoraFilm**.

Mục tiêu:

* Thống nhất API Contract giữa Backend, Frontend, API Gateway và các service liên quan.
* Làm cơ sở để Backend triển khai API mà không phải tự suy đoán endpoint, request, response hoặc business rule.
* Giúp Frontend chuẩn bị các màn hình danh sách phim, chi tiết phim, suất chiếu, phòng và sơ đồ ghế.
* Phân định rõ dữ liệu thuộc Movie Service và dữ liệu thuộc Booking Service.
* Chuẩn hóa validation, authorization, status code và error code.
* Làm cơ sở để tách các implementation issue sau khi contract được duyệt.

---

## 3. Phạm Vi Movie Service

Movie Service sở hữu các bảng:

```txt
movies
genres
movies_genres
rooms
seats
showtimes
```

Movie Service chịu trách nhiệm:

* Quản lý thông tin phim.
* Quản lý thể loại phim.
* Quản lý quan hệ nhiều-nhiều giữa phim và thể loại.
* Quản lý phòng chiếu.
* Quản lý cấu trúc ghế vật lý của từng phòng.
* Quản lý suất chiếu.
* Quản lý giá vé cơ bản theo suất chiếu.
* Quản lý trạng thái phim, phòng, ghế và suất chiếu.

Movie Service không chịu trách nhiệm:

* Giữ ghế thời gian thực.
* Xác định ghế đang được giữ hoặc đã bán.
* Booking.
* Ticket.
* Payment.
* Promotion.
* Điểm thưởng.
* Doanh thu thực tế.

Các nghiệp vụ trên thuộc Booking, Payment, Promotion, Score và Analytics Service.

---

## 4. Physical Schema
- Physical schema trong tài liệu phản ánh cấu trúc dữ liệu hiện tại và chỉ dùng làm căn cứ đối chiếu API contract. Schema, constraint, index và migration cuối cùng do Movie Service Owner xác nhận. Nếu schema hiện tại không đáp ứng contract, contract và implementation phải được thống nhất trong quá trình review.
### 4.1. Bảng `movies`

| Field            | Type         | Ghi chú                       |
| ---------------- | ------------ | ----------------------------- |
| id               | bigint       | Primary key                   |
| title            | varchar(255) | Tên phim                      |
| description      | text         | Mô tả phim                    |
| duration_minutes | int          | Thời lượng phim               |
| director         | varchar(100) | Đạo diễn                      |
| actor            | varchar(255) | Danh sách diễn viên dạng text |
| release_date     | date         | Ngày khởi chiếu (NOT NULL)    |
| end_date         | date         | Ngày kết thúc chiếu (NOT NULL)|
| poster_url       | varchar(255) | URL poster                    |
| trailer_url      | varchar(255) | URL trailer                   |
| age_rating       | varchar(10)  | P, K, T13, T16, T18           |
| status           | varchar(30)  | Trạng thái phim (NOT NULL)    |
| created_at       | timestamp    | Ngày tạo                      |
| updated_at       | timestamp    | Ngày cập nhật                 |

### 4.2. Bảng `genres`

| Field      | Type         | Ghi chú              |
| ---------- | ------------ | -------------------- |
| id         | int          | Primary key          |
| genre_name | varchar(100) | Tên thể loại, unique |

### 4.3. Bảng `movies_genres`

| Field    | Type   | Ghi chú                   |
| -------- | ------ | ------------------------- |
| movie_id | bigint | FK nội bộ tới `movies.id` |
| genre_id | int    | FK nội bộ tới `genres.id` |

### 4.4. Bảng `rooms`

| Field       | Type        | Ghi chú           |
| ----------- | ----------- | ----------------- |
| id          | int         | Primary key       |
| room_name   | varchar(50) | Tên phòng, unique |
| total_seats | int         | Tổng số ghế       |
| screen_type | varchar(20) | Loại màn hình (NOT NULL) |
| status      | varchar(20) | Trạng thái phòng (NOT NULL)|

### 4.5. Bảng `seats`

| Field       | Type        | Ghi chú                  |
| ----------- | ----------- | ------------------------ |
| id          | bigint      | Primary key              |
| room_id     | int         | FK nội bộ tới `rooms.id` |
| seat_row    | varchar(5)  | Hàng ghế                 |
| seat_number | int         | Số ghế                   |
| seat_type   | varchar(20) | Loại ghế (NOT NULL)      |
| status      | varchar(20) | Trạng thái vật lý (NOT NULL)|

### 4.6. Bảng `showtimes`

| Field        | Type          | Ghi chú                   |
| ------------ | ------------- | ------------------------- |
| id           | bigint        | Primary key               |
| movie_id     | bigint        | FK nội bộ tới `movies.id` |
| room_id      | int           | FK nội bộ tới `rooms.id`  |
| start_time   | timestamp     | Thời gian bắt đầu         |
| end_time     | timestamp     | Thời gian kết thúc        |
| ticket_price | decimal(10,2) | Giá vé cơ bản             |
| status       | varchar(20)   | Trạng thái suất chiếu (NOT NULL)|
| created_at   | timestamp     | Ngày tạo                  |
| updated_at   | timestamp     | Ngày cập nhật             |

---

## 5. Database-per-Service và Logical Reference

Hệ thống áp dụng nguyên tắc Database-per-Service.

Các service khác chỉ lưu ID thuộc Movie Service dưới dạng logical reference:

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

Movie Service không truy cập trực tiếp database của service khác.

---

## 6. API Gateway và Service URL

### 6.1. API Gateway URL

Frontend gọi API thông qua:

```txt
http://localhost:8080
```

### 6.2. Movie Service Direct URL

Chỉ dùng để Backend debug hoặc test riêng service:

```txt
http://localhost:8082
```

Port chính thức lấy từ cấu hình project, không hardcode trong Frontend.

### 6.3. Request Flow

```txt
React Frontend
→ API Gateway
→ Movie Service
→ Movie Service Database
```

Frontend không gọi trực tiếp port nội bộ của Movie Service.

---

## 7. Quy Ước Chung

### 7.1. Content Type

```http
Content-Type: application/json
```

### 7.2. Authorization Header

Protected/Admin API yêu cầu:

```http
Authorization: Bearer <accessToken>
```

### 7.3. Date Format

```txt
YYYY-MM-DD
```

Ví dụ:

```txt
2026-06-20
```

### 7.4. Datetime Format

Sử dụng ISO-8601:

```txt
YYYY-MM-DDTHH:mm:ss
```

Ví dụ:

```txt
2026-06-20T19:00:00
```

### 7.5. Timezone

Timezone nghiệp vụ mặc định:

```txt
Asia/Ho_Chi_Minh
```

Backend phải xử lý thống nhất timezone khi lưu và trả dữ liệu.

### 7.6. Currency

```txt
VND
```

`ticketPrice` được trả về dưới dạng number:

```json
{
  "ticketPrice": 120000
}
```

Không hiển thị ký hiệu tiền tệ trong giá trị API.

### 7.7. Pagination

* `page` bắt đầu từ `0`.
* `size` mặc định `10`.
* `size` tối đa `50`.

### 7.8. Sorting

Format:

```txt
sort=<field>,<direction>
```

Ví dụ:

```txt
sort=releaseDate,desc
```

Direction hợp lệ:

```txt
asc
desc
```

---

## 8. Common Response Contract

### 8.1. Success Response

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {}
}
```

### 8.2. Error Response

```json
{
  "success": false,
  "message": "Operation failed",
  "errorCode": "ERROR_CODE",
  "data": null,
  "errors": null
}
```

### 8.3. Validation Error

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "data": null,
  "errors": [
    {
      "field": "title",
      "message": "Title is required"
    }
  ]
}
```

### 8.4. Field Definitions

| Field     | Type              |        Required | Mô tả              |
| --------- | ----------------- | --------------: | ------------------ |
| success   | boolean           |             Yes | Trạng thái request |
| message   | string            |             Yes | Thông báo kết quả  |
| data      | object/array/null |             Yes | Dữ liệu response   |
| errorCode | string/null       |      Error only | Mã lỗi ổn định     |
| errors    | array/null        | Validation only | Chi tiết lỗi field |

---

## 9. Phân Loại API

### 9.1. Public API

Không yêu cầu đăng nhập:

```txt
GET /api/movies
GET /api/movies/{movieId}
GET /api/genres
GET /api/genres/{genreId}
GET /api/movies/{movieId}/showtimes
GET /api/showtimes/{showtimeId}
```

### 9.2. Protected API

Yêu cầu Bearer Token:

```txt
GET /api/rooms/{roomId}/seats
```

API này chỉ trả cấu trúc ghế vật lý, không trả trạng thái giữ ghế hoặc booking.

### 9.3. Admin API

Yêu cầu role `ADMIN` hoặc permission phù hợp:

```txt
GET    /api/admin/movies
GET    /api/admin/movies/{movieId}
POST   /api/admin/movies
PUT    /api/admin/movies/{movieId}
PATCH  /api/admin/movies/{movieId}/status

GET    /api/admin/genres
GET    /api/admin/genres/{genreId}
POST   /api/admin/genres
PUT    /api/admin/genres/{genreId}

GET    /api/admin/rooms
GET    /api/admin/rooms/{roomId}
POST   /api/admin/rooms
PUT    /api/admin/rooms/{roomId}
PATCH  /api/admin/rooms/{roomId}/status

GET    /api/admin/rooms/{roomId}/seats
GET    /api/admin/seats/{seatId}
POST   /api/admin/rooms/{roomId}/seats
PUT    /api/admin/seats/{seatId}
PATCH  /api/admin/seats/{seatId}/status

GET    /api/admin/showtimes
GET    /api/admin/showtimes/{showtimeId}
POST   /api/admin/showtimes
PUT    /api/admin/showtimes/{showtimeId}
PATCH  /api/admin/showtimes/{showtimeId}/status
```

---

## 10. Endpoint Summary

| Method | Endpoint                                   | Access    | Mục đích                       |
| ------ | ------------------------------------------ | --------- | ------------------------------ |
| GET    | `/api/movies`                              | Public    | Danh sách phim                 |
| GET    | `/api/movies/{movieId}`                    | Public    | Chi tiết phim                  |
| GET    | `/api/genres`                              | Public    | Danh sách thể loại             |
| GET    | `/api/genres/{genreId}`                    | Public    | Chi tiết thể loại              |
| GET    | `/api/movies/{movieId}/showtimes`          | Public    | Suất chiếu theo phim           |
| GET    | `/api/showtimes/{showtimeId}`              | Public    | Chi tiết suất chiếu            |
| GET    | `/api/rooms/{roomId}/seats`                | Protected | Sơ đồ ghế vật lý               |
| GET    | `/api/admin/movies`                        | Admin     | Danh sách phim quản trị        |
| GET    | `/api/admin/movies/{movieId}`              | Admin     | Chi tiết phim quản trị         |
| POST   | `/api/admin/movies`                        | Admin     | Tạo phim                       |
| PUT    | `/api/admin/movies/{movieId}`              | Admin     | Cập nhật phim                  |
| PATCH  | `/api/admin/movies/{movieId}/status`       | Admin     | Cập nhật trạng thái phim       |
| GET    | `/api/admin/genres`                        | Admin     | Danh sách thể loại quản trị    |
| GET    | `/api/admin/genres/{genreId}`              | Admin     | Chi tiết thể loại quản trị     |
| POST   | `/api/admin/genres`                        | Admin     | Tạo thể loại                   |
| PUT    | `/api/admin/genres/{genreId}`              | Admin     | Cập nhật thể loại              |
| GET    | `/api/admin/rooms`                         | Admin     | Danh sách phòng                |
| GET    | `/api/admin/rooms/{roomId}`                | Admin     | Chi tiết phòng                 |
| POST   | `/api/admin/rooms`                         | Admin     | Tạo phòng                      |
| PUT    | `/api/admin/rooms/{roomId}`                | Admin     | Cập nhật phòng                 |
| PATCH  | `/api/admin/rooms/{roomId}/status`         | Admin     | Cập nhật trạng thái phòng      |
| GET    | `/api/admin/rooms/{roomId}/seats`          | Admin     | Danh sách ghế quản trị         |
| GET    | `/api/admin/seats/{seatId}`                | Admin     | Chi tiết ghế                   |
| POST   | `/api/admin/rooms/{roomId}/seats`          | Admin     | Tạo ghế hàng loạt              |
| PUT    | `/api/admin/seats/{seatId}`                | Admin     | Cập nhật ghế                   |
| PATCH  | `/api/admin/seats/{seatId}/status`         | Admin     | Cập nhật trạng thái ghế        |
| GET    | `/api/admin/showtimes`                     | Admin     | Danh sách suất chiếu           |
| GET    | `/api/admin/showtimes/{showtimeId}`        | Admin     | Chi tiết suất chiếu            |
| POST   | `/api/admin/showtimes`                     | Admin     | Tạo suất chiếu                 |
| PUT    | `/api/admin/showtimes/{showtimeId}`        | Admin     | Cập nhật suất chiếu            |
| PATCH  | `/api/admin/showtimes/{showtimeId}/status` | Admin     | Cập nhật trạng thái suất chiếu |

---

# 11. Public Movie APIs

## 11.1. Get Movie List

### Endpoint

```http
GET /api/movies
```

### Query Parameters

| Parameter   | Type    | Required | Validation       | Mô tả          |
| ----------- | ------- | -------: | ---------------- | -------------- |
| page        | integer |       No | >= 0             | Trang hiện tại |
| size        | integer |       No | 1–50             | Số phần tử     |
| search      | string  |       No | Tối đa 255 ký tự | Tìm theo tên   |
| status      | string  |       No | MovieStatus      | Trạng thái     |
| genreId     | integer |       No | > 0              | Thể loại       |
| releaseFrom | date    |       No | YYYY-MM-DD       | Từ ngày        |
| releaseTo   | date    |       No | YYYY-MM-DD       | Đến ngày       |
| sort        | string  |       No | field,direction  | Sắp xếp        |

### Public Filtering Rule

Public API chỉ trả các phim được phép hiển thị:

```txt
UPCOMING
NOW_SHOWING
ENDED
```

Không trả phim:

```txt
INACTIVE
```

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
            "genreName": "Action"
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

Status: `200 OK`

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

### Response Error: Invalid Query

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Invalid movie query parameters",
  "errorCode": "MOVIE_INVALID_QUERY",
  "data": null,
  "errors": [
    {
      "field": "size",
      "message": "Size must be between 1 and 50"
    }
  ]
}
```

### Response Error: Genre Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Genre not found",
  "errorCode": "GENRE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

## 11.2. Get Movie Detail

### Endpoint

```http
GET /api/movies/{movieId}
```

### Path Parameter

| Field   | Type    | Required | Validation |
| ------- | ------- | -------: | ---------- |
| movieId | integer |      Yes | > 0        |

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
        "genreName": "Action"
      }
    ]
  }
}
```

### Response Error: Movie Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Movie not found",
  "errorCode": "MOVIE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

Phim `INACTIVE` được xem như không tồn tại với Public API.

---

# 12. Public Genre APIs

## 12.1. Get Genre List

### Endpoint

```http
GET /api/genres
```

### Response Success

Status: `200 OK`

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

### Empty Result

```json
{
  "success": true,
  "message": "Genres retrieved successfully",
  "data": []
}
```

---

## 12.2. Get Genre Detail

### Endpoint

```http
GET /api/genres/{genreId}
```

### Response Success

```json
{
  "success": true,
  "message": "Genre retrieved successfully",
  "data": {
    "id": 1,
    "genreName": "Action"
  }
}
```

### Response Error

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Genre not found",
  "errorCode": "GENRE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

---

# 13. Public Showtime APIs

## 13.1. Get Showtimes by Movie

### Endpoint

```http
GET /api/movies/{movieId}/showtimes
```

### Query Parameters

| Parameter | Type   | Required | Validation     | Mô tả      |
| --------- | ------ | -------: | -------------- | ---------- |
| date      | date   |       No | YYYY-MM-DD     | Ngày chiếu |
| status    | string |       No | ShowtimeStatus | Trạng thái |

### Public Showtime Rule

Public API chỉ trả các suất chiếu:

```txt
SCHEDULED
OPEN
```

Không trả:

```txt
CANCELLED
COMPLETED
CLOSED
```

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

### Empty Result

```json
{
  "success": true,
  "message": "Showtimes retrieved successfully",
  "data": []
}
```

### Response Error: Movie Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Movie not found",
  "errorCode": "MOVIE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Response Error: Invalid Date

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Invalid showtime date",
  "errorCode": "SHOWTIME_INVALID_DATE",
  "data": null,
  "errors": null
}
```

### Response Error: Invalid Status

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Invalid showtime status",
  "errorCode": "SHOWTIME_INVALID_STATUS",
  "data": null,
  "errors": null
}
```

---

## 13.2. Get Showtime Detail

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

### Response Error: Showtime Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Showtime not found",
  "errorCode": "SHOWTIME_NOT_FOUND",
  "data": null,
  "errors": null
}
```

Suất chiếu `CANCELLED` không được hiển thị bằng Public API.

---

# 14. Protected Room Seat API

## 14.1. Get Room Seats

### Endpoint

```http
GET /api/rooms/{roomId}/seats
```

### Headers

```http
Authorization: Bearer <accessToken>
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

### Response Error: Unauthorized

Status: `401 Unauthorized`

```json
{
  "success": false,
  "message": "Unauthorized access",
  "errorCode": "UNAUTHORIZED",
  "data": null,
  "errors": null
}
```

### Response Error: Room Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Room not found",
  "errorCode": "ROOM_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Response Error: Room Not Available

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Room is not available",
  "errorCode": "ROOM_NOT_AVAILABLE",
  "data": null,
  "errors": null
}
```

### Quan Trọng

Movie Service chỉ trả:

```txt
seat structure
seat type
physical seat status
```

Movie Service không trả:

```txt
AVAILABLE
HELD
BOOKED
```

Trạng thái giữ ghế và đặt ghế thuộc Booking Service.

---

# 15. Admin Movie APIs

## 15.1. Get Admin Movie List

### Endpoint

```http
GET /api/admin/movies
```

### Headers

```http
Authorization: Bearer <accessToken>
```

### Query Parameters

Giống `GET /api/movies`, nhưng Admin được phép xem cả:

```txt
UPCOMING
NOW_SHOWING
ENDED
INACTIVE
```

### Response Success

Sử dụng pagination contract như Public Movie List.

### Response Error

* `400 MOVIE_INVALID_QUERY`
* `401 UNAUTHORIZED`
* `403 FORBIDDEN`

---

## 15.2. Get Admin Movie Detail

```http
GET /api/admin/movies/{movieId}
```

Admin có thể xem cả phim `INACTIVE`.

### Error

* `404 MOVIE_NOT_FOUND`
* `401 UNAUTHORIZED`
* `403 FORBIDDEN`

---

## 15.3. Create Movie

### Endpoint

```http
POST /api/admin/movies
```

### Headers

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
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

### Field Definitions

| Field           | Type          | Required | Validation               |
| --------------- | ------------- | -------: | ------------------------ |
| title           | string        |      Yes | 1–255 ký tự              |
| description     | string        |       No | Không giới hạn nghiệp vụ |
| durationMinutes | integer       |      Yes | > 0                      |
| director        | string        |       No | Tối đa 100 ký tự         |
| actor           | string        |       No | Tối đa 255 ký tự         |
| releaseDate     | date          |      Yes | YYYY-MM-DD               |
| endDate         | date          |      Yes | >= releaseDate           |
| posterUrl       | string        |       No | URL hợp lệ, tối đa 255   |
| trailerUrl      | string        |       No | URL hợp lệ, tối đa 255   |
| ageRating       | string        |       No | AgeRating enum           |
| status          | string        |      Yes | MovieStatus              |
| genreIds        | array<number> |      Yes | Không rỗng, không trùng  |

### Processing Rules

* Trim khoảng trắng đầu/cuối.
* Không cho title rỗng sau khi trim.
* Tất cả genre phải tồn tại.
* `durationMinutes > 0`.
* `endDate >= releaseDate`.

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

### Response Error: Validation

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "data": null,
  "errors": [
    {
      "field": "durationMinutes",
      "message": "Duration must be greater than 0"
    }
  ]
}
```

### Response Error: Invalid Date Range

```json
{
  "success": false,
  "message": "Movie end date cannot be before release date",
  "errorCode": "MOVIE_INVALID_DATE_RANGE",
  "data": null,
  "errors": null
}
```

### Response Error: Genre Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "One or more genres were not found",
  "errorCode": "GENRE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Response Error: Forbidden

Status: `403 Forbidden`

```json
{
  "success": false,
  "message": "Access denied",
  "errorCode": "FORBIDDEN",
  "data": null,
  "errors": null
}
```

---

## 15.4. Update Movie

### Endpoint

```http
PUT /api/admin/movies/{movieId}
```

PUT được xem là full update. Các field bắt buộc phải được gửi đầy đủ.

### Business Rules

* Không cho đổi `durationMinutes` nếu đã có showtime tương lai, trừ khi tất cả showtime liên quan được cập nhật trong cùng nghiệp vụ.
* Không tự động thay đổi `endTime` của showtime cũ nếu chưa có rule rõ ràng.
* Không cho xóa toàn bộ genre.
* Không cho date range không hợp lệ.

### Response Success

```json
{
  "success": true,
  "message": "Movie updated successfully",
  "data": {
    "id": 1,
    "title": "Avengers Updated",
    "status": "NOW_SHOWING"
  }
}
```

### Response Error: Movie Not Found

Status: `404 Not Found`

```json
{
  "success": false,
  "message": "Movie not found",
  "errorCode": "MOVIE_NOT_FOUND",
  "data": null,
  "errors": null
}
```

### Response Error: Existing Showtimes Conflict

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Movie duration cannot be changed because future showtimes already exist",
  "errorCode": "MOVIE_HAS_FUTURE_SHOWTIMES",
  "data": null,
  "errors": null
}
```

---

## 15.5. Update Movie Status

### Endpoint

```http
PATCH /api/admin/movies/{movieId}/status
```

### Request Body

```json
{
  "status": "NOW_SHOWING"
}
```

### Allowed Transitions

| Current     | Allowed Next          |
| ----------- | --------------------- |
| UPCOMING    | NOW_SHOWING, INACTIVE |
| NOW_SHOWING | ENDED, INACTIVE       |
| ENDED       | INACTIVE              |
| INACTIVE    | UPCOMING              |

Các transition khác không hợp lệ.

### Response Success

```json
{
  "success": true,
  "message": "Movie status updated successfully",
  "data": {
    "id": 1,
    "status": "NOW_SHOWING"
  }
}
```

### Response Error

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Invalid movie status transition",
  "errorCode": "MOVIE_INVALID_STATUS_TRANSITION",
  "data": null,
  "errors": null
}
```

---

# 16. Admin Genre APIs

## 16.1. Get Admin Genre List

```http
GET /api/admin/genres
```

Response giống Public Genre List.

## 16.2. Get Admin Genre Detail

```http
GET /api/admin/genres/{genreId}
```

## 16.3. Create Genre

### Endpoint

```http
POST /api/admin/genres
```

### Request

```json
{
  "genreName": "Science Fiction"
}
```

### Business Rules

* Trim khoảng trắng.
* Tối đa 100 ký tự.
* Tên genre unique không phân biệt hoa thường.
* Schema hiện tại không có status.

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Genre created successfully",
  "data": {
    "id": 3,
    "genreName": "Science Fiction"
  }
}
```

### Response Error: Duplicate

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Genre already exists",
  "errorCode": "GENRE_ALREADY_EXISTS",
  "data": null,
  "errors": null
}
```

## 16.4. Update Genre

```http
PUT /api/admin/genres/{genreId}
```

### Error

* `404 GENRE_NOT_FOUND`
* `409 GENRE_ALREADY_EXISTS`
* `400 VALIDATION_ERROR`

### Delete Policy

Không hỗ trợ hard delete genre trong Sprint 2.

Nếu cần xóa genre sau này, phải kiểm tra genre có đang được movie sử dụng hay không và tạo issue/schema change riêng.

---

# 17. Admin Room APIs

## 17.1. Get Room List

```http
GET /api/admin/rooms
```

### Query Parameters

| Field      | Type       | Required |
| ---------- | ---------- | -------: |
| page       | integer    |       No |
| size       | integer    |       No |
| status     | RoomStatus |       No |
| screenType | ScreenType |       No |
| search     | string     |       No |

### Response Success

Trả pagination.

## 17.2. Get Room Detail

```http
GET /api/admin/rooms/{roomId}
```

### Response

```json
{
  "success": true,
  "message": "Room retrieved successfully",
  "data": {
    "id": 2,
    "roomName": "Cinema 02",
    "totalSeats": 100,
    "screenType": "IMAX",
    "status": "ACTIVE"
  }
}
```

## 17.3. Create Room

### Endpoint

```http
POST /api/admin/rooms
```

### Request

```json
{
  "roomName": "Cinema 02",
  "totalSeats": 100,
  "screenType": "IMAX",
  "status": "ACTIVE"
}
```

### Validation

| Field      | Validation                  |
| ---------- | --------------------------- |
| roomName   | Required, unique, tối đa 50 |
| totalSeats | > 0                         |
| screenType | ScreenType enum             |
| status     | RoomStatus enum             |

### Response Success

Status: `201 Created`

### Response Error: Duplicate Room

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Room already exists",
  "errorCode": "ROOM_ALREADY_EXISTS",
  "data": null,
  "errors": null
}
```

## 17.4. Update Room

```http
PUT /api/admin/rooms/{roomId}
```

### Business Rules

* Không cho đổi `totalSeats` nếu số ghế thực tế đã tồn tại không khớp.
* Không cho đổi trạng thái không hợp lệ.
* Không cho chuyển room sang `INACTIVE` nếu có showtime tương lai đang hoạt động.

### Response Error

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Room has future showtimes",
  "errorCode": "ROOM_HAS_FUTURE_SHOWTIMES",
  "data": null,
  "errors": null
}
```

## 17.5. Update Room Status

```http
PATCH /api/admin/rooms/{roomId}/status
```

### Allowed Transitions

| Current     | Allowed Next          |
| ----------- | --------------------- |
| ACTIVE      | MAINTENANCE, INACTIVE |
| MAINTENANCE | ACTIVE, INACTIVE      |
| INACTIVE    | ACTIVE                |

---

# 18. Admin Seat APIs

## 18.1. Get Seats by Room

```http
GET /api/admin/rooms/{roomId}/seats
```

Admin được xem cả seat `INACTIVE` và `MAINTENANCE`.

## 18.2. Get Seat Detail

```http
GET /api/admin/seats/{seatId}
```

## 18.3. Create Seats for Room

### Endpoint

```http
POST /api/admin/rooms/{roomId}/seats
```

### Request

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

### Field Rules

| Field      | Validation      |
| ---------- | --------------- |
| seats      | Không rỗng      |
| seatRow    | 1–5 ký tự       |
| seatNumber | > 0             |
| seatType   | SeatType enum   |
| status     | SeatStatus enum |

Trong cùng phòng, cặp sau phải unique:

```txt
seatRow + seatNumber
```

Bulk create là atomic:

```txt
Nếu một seat lỗi → rollback toàn bộ batch
```

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Seats created successfully",
  "data": {
    "roomId": 2,
    "createdCount": 2
  }
}
```

### Response Error: Duplicate Seat

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "One or more seats already exist",
  "errorCode": "SEAT_ALREADY_EXISTS",
  "data": null,
  "errors": [
    {
      "field": "seats[0]",
      "message": "Seat A1 already exists in this room"
    }
  ]
}
```

### Response Error: Batch Validation

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Seat batch validation failed",
  "errorCode": "SEAT_BATCH_VALIDATION_FAILED",
  "data": null,
  "errors": [
    {
      "field": "seats[1].seatNumber",
      "message": "Seat number must be greater than 0"
    }
  ]
}
```

## 18.4. Update Seat

```http
PUT /api/admin/seats/{seatId}
```

Không cho thay đổi vị trí ghế nếu seat đã được service khác sử dụng trong booking/ticket lịch sử.

Nếu cần thay đổi vị trí, nên tạo seat mới và vô hiệu hóa seat cũ.

## 18.5. Update Seat Status

```http
PATCH /api/admin/seats/{seatId}/status
```

### Allowed Transitions

| Current     | Allowed Next          |
| ----------- | --------------------- |
| ACTIVE      | MAINTENANCE, INACTIVE |
| MAINTENANCE | ACTIVE, INACTIVE      |
| INACTIVE    | ACTIVE                |

---

# 19. Admin Showtime APIs

## 19.1. Get Showtime List

```http
GET /api/admin/showtimes
```

### Query Parameters

| Field   | Type           | Required |
| ------- | -------------- | -------: |
| page    | integer        |       No |
| size    | integer        |       No |
| movieId | integer        |       No |
| roomId  | integer        |       No |
| date    | date           |       No |
| status  | ShowtimeStatus |       No |
| from    | datetime       |       No |
| to      | datetime       |       No |

Admin được xem mọi trạng thái.

## 19.2. Get Showtime Detail

```http
GET /api/admin/showtimes/{showtimeId}
```

## 19.3. Create Showtime

### Endpoint

```http
POST /api/admin/showtimes
```

### Request

```json
{
  "movieId": 1,
  "roomId": 2,
  "startTime": "2026-06-20T19:00:00",
  "ticketPrice": 120000,
  "status": "SCHEDULED"
}
```

### Field Definitions

| Field       | Type     | Required | Validation       |
| ----------- | -------- | -------: | ---------------- |
| movieId     | integer  |      Yes | > 0              |
| roomId      | integer  |      Yes | > 0              |
| startTime   | datetime |      Yes | Phải ở tương lai |
| ticketPrice | number   |      Yes | > 0              |
| status      | string   |      Yes | ShowtimeStatus   |

Backend tự tính:

```txt
endTime = startTime + movie.durationMinutes
```

Frontend không gửi `endTime`.

### Business Rules

* Movie phải tồn tại.
* Movie không được `INACTIVE`.
* Room phải tồn tại.
* Room phải `ACTIVE`.
* Start time phải ở tương lai.
* Start time phải nằm trong khoảng release date và end date của phim.
* Ticket price phải lớn hơn 0.
* Showtime không được trùng lịch trong cùng phòng.

### Cleanup Buffer

Khoảng nghỉ mặc định:

```txt
15 phút
```

Nên đưa thành cấu hình:

```properties
movie.showtime.cleanup-buffer-minutes=15
```

### Conflict Formula

```txt
newStart < existingEnd + cleanupBuffer
AND
newEnd + cleanupBuffer > existingStart
```

### Concurrency Rule

Conflict validation phải chạy trong transaction.

Nếu hai request đồng thời gây conflict:

```txt
Chỉ một request được thành công
```

### Response Success

Status: `201 Created`

```json
{
  "success": true,
  "message": "Showtime created successfully",
  "data": {
    "id": 10,
    "movieId": 1,
    "roomId": 2,
    "startTime": "2026-06-20T19:00:00",
    "endTime": "2026-06-20T22:00:00",
    "ticketPrice": 120000,
    "status": "SCHEDULED"
  }
}
```

### Error: Schedule Conflict

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Showtime conflicts with another showtime in this room",
  "errorCode": "SHOWTIME_SCHEDULE_CONFLICT",
  "data": null,
  "errors": null
}
```

### Error: Movie Not Available

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Movie is not available for scheduling",
  "errorCode": "MOVIE_NOT_AVAILABLE",
  "data": null,
  "errors": null
}
```

### Error: Room Not Available

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Room is not available",
  "errorCode": "ROOM_NOT_AVAILABLE",
  "data": null,
  "errors": null
}
```

### Error: Outside Release Period

Status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Showtime is outside the movie release period",
  "errorCode": "SHOWTIME_OUTSIDE_MOVIE_RELEASE_PERIOD",
  "data": null,
  "errors": null
}
```

## 19.4. Update Showtime

```http
PUT /api/admin/showtimes/{showtimeId}
```

### Business Rules

* Không cho update showtime đã `COMPLETED`.
* Không cho update thời gian nếu showtime đã có booking, trừ flow nghiệp vụ riêng.
* Phải kiểm tra conflict lại.
* Backend tính lại `endTime`.

### Error: Showtime Has Bookings

Status: `409 Conflict`

```json
{
  "success": false,
  "message": "Showtime cannot be updated because bookings already exist",
  "errorCode": "SHOWTIME_HAS_BOOKINGS",
  "data": null,
  "errors": null
}
```

## 19.5. Update Showtime Status

```http
PATCH /api/admin/showtimes/{showtimeId}/status
```

### Allowed Transitions

| Current   | Allowed Next      |
| --------- | ----------------- |
| SCHEDULED | OPEN, CANCELLED   |
| OPEN      | CLOSED, CANCELLED |
| CLOSED    | COMPLETED         |
| CANCELLED | Không có          |
| COMPLETED | Không có          |

### Cancel Rule

Nếu showtime đã có booking:

```txt
Movie Service không tự hoàn tiền.
Movie Service chỉ phát tín hiệu trạng thái CANCELLED.
Booking/Payment/Notification xử lý tiếp qua integration flow hoặc event ở sprint sau.
```

---

# 20. Enum Definitions

## 20.1. MovieStatus

```txt
UPCOMING
NOW_SHOWING
ENDED
INACTIVE
```

## 20.2. RoomStatus

```txt
ACTIVE
MAINTENANCE
INACTIVE
```

## 20.3. ScreenType

```txt
STANDARD
IMAX
4DX
```

## 20.4. SeatStatus

```txt
ACTIVE
MAINTENANCE
INACTIVE
```

## 20.5. SeatType

```txt
STANDARD
VIP
COUPLE
```

## 20.6. ShowtimeStatus

```txt
SCHEDULED
OPEN
CLOSED
CANCELLED
COMPLETED
```

## 20.7. AgeRating

Đề xuất:

```txt
P
K
T13
T16
T18
```

Enum cuối cùng phải đồng bộ với business rule và UI.

---

# 21. Movie Status Source of Truth

Trong Sprint 2, Movie Status được quản lý trực tiếp bởi Admin API.

Không tự động suy ra status hoàn toàn từ ngày.

Tuy nhiên:

* `releaseDate`
* `endDate`
* `status`

phải hợp lý với nhau.

Ví dụ:

```txt
Không nên để NOW_SHOWING khi releaseDate còn ở tương lai.
Không nên để UPCOMING khi endDate đã qua.
```

Scheduled job tự cập nhật status có thể được bổ sung ở issue riêng sau.

---

# 22. Delete Policy

Không hỗ trợ hard delete cho:

```txt
movies
rooms
seats
showtimes
```

Sử dụng trạng thái:

```txt
INACTIVE
CANCELLED
ENDED
```

Lý do:

* ID có thể đã được service khác lưu dưới dạng logical reference.
* Cần giữ lịch sử.
* Tránh làm hỏng booking/ticket cũ.

Genre chưa có status trong schema nên Sprint 2 không hỗ trợ delete genre.

Mọi yêu cầu delete phải được tách issue/schema change riêng.

---

# 23. Idempotency và Duplicate Request

Sprint 2 chưa bắt buộc hỗ trợ header:

```http
Idempotency-Key
```

Client phải:

* Disable nút submit khi request đang xử lý.
* Không gửi lặp create request.

Backend vẫn phải bảo vệ duplicate bằng:

* Unique constraint.
* Business validation.
* Transaction.

Áp dụng cho:

* Create Movie.
* Create Genre.
* Create Room.
* Bulk Create Seats.
* Create Showtime.

---

# 24. Authorization Rules

| Nhóm API             | Quyền                             |
| -------------------- | --------------------------------- |
| Xem phim             | Public                            |
| Xem thể loại         | Public                            |
| Xem suất chiếu       | Public                            |
| Xem sơ đồ ghế vật lý | Authenticated User                |
| Quản lý phim         | ADMIN                             |
| Quản lý thể loại     | ADMIN                             |
| Quản lý phòng        | ADMIN                             |
| Quản lý ghế          | ADMIN                             |
| Quản lý suất chiếu   | ADMIN hoặc EMPLOYEE có permission |

Permission đề xuất:

```txt
MOVIE_READ
MOVIE_MANAGE
GENRE_MANAGE
ROOM_MANAGE
SEAT_MANAGE
SHOWTIME_MANAGE
```

---

# 25. Error Code Catalog

| Error Code                              | HTTP Status | Ý nghĩa                          |
| --------------------------------------- | ----------: | -------------------------------- |
| `MOVIE_NOT_FOUND`                       |         404 | Không tìm thấy phim              |
| `MOVIE_INVALID_QUERY`                   |         400 | Query phim không hợp lệ          |
| `MOVIE_INVALID_DATE_RANGE`              |         400 | Date range không hợp lệ          |
| `MOVIE_INVALID_DURATION`                |         400 | Duration không hợp lệ            |
| `MOVIE_INVALID_STATUS`                  |         400 | Status không hợp lệ              |
| `MOVIE_INVALID_STATUS_TRANSITION`       |         409 | Chuyển status không hợp lệ       |
| `MOVIE_HAS_FUTURE_SHOWTIMES`            |         409 | Phim đang có showtime tương lai  |
| `MOVIE_NOT_AVAILABLE`                   |         409 | Phim không khả dụng              |
| `GENRE_NOT_FOUND`                       |         404 | Không tìm thấy thể loại          |
| `GENRE_ALREADY_EXISTS`                  |         409 | Thể loại đã tồn tại              |
| `ROOM_NOT_FOUND`                        |         404 | Không tìm thấy phòng             |
| `ROOM_ALREADY_EXISTS`                   |         409 | Phòng đã tồn tại                 |
| `ROOM_NOT_AVAILABLE`                    |         409 | Phòng không khả dụng             |
| `ROOM_HAS_FUTURE_SHOWTIMES`             |         409 | Phòng có showtime tương lai      |
| `ROOM_TOTAL_SEATS_MISMATCH`             |         409 | Tổng số ghế không khớp           |
| `ROOM_INVALID_STATUS_TRANSITION`        |         409 | Chuyển status phòng không hợp lệ |
| `SEAT_NOT_FOUND`                        |         404 | Không tìm thấy ghế               |
| `SEAT_ALREADY_EXISTS`                   |         409 | Ghế đã tồn tại                   |
| `SEAT_INVALID_TYPE`                     |         400 | Loại ghế không hợp lệ            |
| `SEAT_INVALID_POSITION`                 |         400 | Vị trí ghế không hợp lệ          |
| `SEAT_BATCH_VALIDATION_FAILED`          |         400 | Batch ghế không hợp lệ           |
| `SEAT_IN_USE`                           |         409 | Ghế đã được sử dụng              |
| `SHOWTIME_NOT_FOUND`                    |         404 | Không tìm thấy suất chiếu        |
| `SHOWTIME_INVALID_DATE`                 |         400 | Ngày truy vấn không hợp lệ       |
| `SHOWTIME_INVALID_STATUS`               |         400 | Status không hợp lệ              |
| `SHOWTIME_INVALID_TIME`                 |         400 | Thời gian không hợp lệ           |
| `SHOWTIME_INVALID_PRICE`                |         400 | Giá vé không hợp lệ              |
| `SHOWTIME_OUTSIDE_MOVIE_RELEASE_PERIOD` |         400 | Ngoài thời gian chiếu phim       |
| `SHOWTIME_SCHEDULE_CONFLICT`            |         409 | Trùng lịch phòng                 |
| `SHOWTIME_HAS_BOOKINGS`                 |         409 | Suất chiếu đã có booking         |
| `SHOWTIME_INVALID_STATUS_TRANSITION`    |         409 | Chuyển status không hợp lệ       |
| `VALIDATION_ERROR`                      |         400 | Request validation lỗi           |
| `UNAUTHORIZED`                          |         401 | Chưa xác thực                    |
| `FORBIDDEN`                             |         403 | Không có quyền                   |
| `INTERNAL_SERVER_ERROR`                 |         500 | Lỗi hệ thống                     |

---

# 26. Cross-Service Integration Notes

## 26.1. Booking Service

Booking Service sử dụng:

```txt
showtimeId
seatId
```

Booking Service chịu trách nhiệm:

```txt
seat reservation
real-time availability
booking status
ticket
```

## 26.2. Analytics Service

Analytics Service có thể lưu:

```txt
movieId
movieTitle
```

`movieTitle` là snapshot phục vụ dữ liệu lịch sử.

## 26.3. Payment Service

Movie Service không giao tiếp trực tiếp với Payment Service trong flow cơ bản.

## 26.4. Notification Service

Sau này Movie Service có thể publish event khi:

```txt
SHOWTIME_CANCELLED
MOVIE_STATUS_CHANGED
```

Không nằm trong implementation scope hiện tại.

---

# 27. Frontend Notes

Frontend chỉ gọi:

```txt
http://localhost:8080/api/...
```

Frontend không gọi trực tiếp Movie Service port.

Frontend không được dùng trạng thái vật lý của seat để suy ra:

```txt
AVAILABLE
HELD
BOOKED
```

Frontend phải lấy seat availability từ Booking Service.

---

# 28. Scope Chưa Bao Gồm

* Automatic Showtime Scheduler.
* Real-time seat locking.
* Seat availability theo booking.
* Movie rating và review.
* Actor CRUD riêng.
* Cinema branch hoặc multi-location.
* Dynamic pricing.
* Recommendation engine.
* Revenue calculation.
* Production media upload.
* Showtime cancellation compensation flow.
* Kafka event implementation.
* Scheduled movie status update.
* Hard delete API.

---

# 29. Implementation Issue Direction

Sau khi contract được duyệt, có thể tách thành các issue:

```txt
[Backend] Implement Movie and Genre APIs
[Backend] Implement Room and Seat APIs
[Backend] Implement Showtime APIs
```

Mỗi implementation issue phải tuân thủ contract này.

Nếu implementation cần thay đổi endpoint, request, response hoặc business rule:

```txt
Contract phải được cập nhật trong cùng MR.
```

---

# 30. Acceptance Criteria

Contract được xem là hoàn thành khi:

* [x] Có endpoint summary đầy đủ.
* [x] Có Public/Protected/Admin classification.
* [x] Có request headers.
* [x] Có path/query parameter definitions.
* [x] Có field definitions.
* [x] Có success response.
* [x] Có error response cho từng nhóm API.
* [x] Có pagination/filter/sort.
* [x] Có business rules.
* [x] Có enum/status.
* [x] Có status transition rules.
* [x] Có date/time/timezone convention.
* [x] Có currency convention.
* [x] Có logical reference notes.
* [x] Có concurrency rule cho Showtime.
* [x] Có idempotency/duplicate request note.
* [x] Có delete policy.
* [x] Không tạo Room/Seat/Showtime Service riêng.
* [x] Thành xác nhận contract khả thi với code hiện tại.
* [x] Tài liệu đủ rõ để tách implementation issues.
* [x] MR target vào `develop`.

---

# 31. Các Điểm Reviewer Cần Xác Nhận

Reviewer cần xác nhận:

1. [x] Prefix `/api/admin/**` có phù hợp với Gateway/Security hiện tại không.
2. [x] Movie Service port chính thức.
3. [x] `cleanupBuffer = 15 phút`.
4. [x] Backend tự tính `endTime`.
5. [x] Bộ enum status.
6. [x] Age rating enum.
7. [x] API xem seat structure là Protected.
8. [x] Bulk create seats rollback toàn bộ khi một item lỗi.
9. [x] Không hard delete dữ liệu.
10. [x] PUT là full update.
11. [x] Không cho đổi duration khi có future showtime.
12. [x] Showtime không được sửa khi đã có booking.
13. [x] Phân quyền ADMIN/EMPLOYEE và permission code.
