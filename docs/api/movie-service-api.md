# Movie Service API Contract

## 1. API Principles

* Customer APIs chỉ expose dữ liệu public/active.
* Admin APIs yêu cầu authentication và admin role.
* Internal APIs yêu cầu internal token.
* Customer APIs nên dùng `slug` hoặc `public_id` thay vì auto-increment database id.
* API responses không được expose deleted records.
* Movie Service không quản lý trạng thái ghế `HELD` / `BOOKED`.

---

## 2. Customer APIs

### 2.1. Movie Listing

```http
GET /api/movies?status=&genreId=&keyword=&city=&cinemaId=&date=&page=&size=&sort=
```

#### Supported status

```txt
now-showing
coming-soon
```

#### Response Example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "data": [
      {
        "publicId": "UUID-1234",
        "slug": "avengers-endgame",
        "title": "Avengers: Endgame",
        "originalTitle": "Avengers: Endgame",
        "synopsis": "After the devastating events...",
        "durationMinutes": 181,
        "ageRating": "P",
        "releaseDate": "2019-04-26",
        "endDate": "2019-07-26",
        "genres": ["Action", "Sci-Fi"],
        "primaryPoster": "https://example.com/poster.jpg",
        "status": "NOW_SHOWING"
      }
    ],
    "pageNo": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 2.2. Movie Detail

```http
GET /api/movies/{movieSlug}
```

#### Response Example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "publicId": "UUID-1234",
    "slug": "avengers-endgame",
    "title": "Avengers: Endgame",
    "originalTitle": "Avengers: Endgame",
    "synopsis": "After the devastating events...",
    "durationMinutes": 181,
    "ageRating": "P",
    "releaseDate": "2019-04-26",
    "endDate": "2019-07-26",
    "genres": ["Action", "Sci-Fi"],
    "primaryPoster": "https://example.com/poster.jpg",
    "status": "NOW_SHOWING"
  }
}
```

---

### 2.3. Movie Showtimes

```http
GET /api/movies/{movieSlug}/showtimes?city=&cinemaId=&date=
```

#### Behavior

* Response group showtimes theo cinema.
* Chỉ trả về showtimes có status `OPEN_FOR_BOOKING`.

---

### 2.4. Cinema Listing

```http
GET /api/cinemas?city=&district=&keyword=&page=&size=
```

#### Behavior

* Chỉ trả về active cinemas.

#### Response Example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "data": [
      {
        "publicId": "CINEMA-UUID-001",
        "name": "Lorafilm Quận 1",
        "slug": "lorafilm-quan-1",
        "address": "123 Lê Lợi, Quận 1",
        "city": "Hồ Chí Minh",
        "district": "Quận 1",
        "hotline": "19001234",
        "status": "ACTIVE"
      }
    ],
    "pageNo": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 2.5. Cinema Detail

```http
GET /api/cinemas/{cinemaSlug}
```

#### Response Example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "publicId": "CINEMA-UUID-001",
    "name": "Lorafilm Quận 1",
    "slug": "lorafilm-quan-1",
    "address": "123 Lê Lợi, Quận 1",
    "city": "Hồ Chí Minh",
    "district": "Quận 1",
    "hotline": "19001234",
    "status": "ACTIVE"
  }
}
```

---

### 2.6. Cinema Showtimes

```http
GET /api/cinemas/{cinemaSlug}/showtimes?date=
```

#### Behavior

* Chỉ trả về open showtimes.

---

### 2.7. Showtime Search

```http
GET /api/showtimes?movieSlug=&cinemaSlug=&city=&date=&format=&audioLanguage=&subtitleLanguage=
```

#### Behavior

* Chỉ trả về open showtimes.

#### Response Example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "data": [
      {
        "showtimePublicId": "SHOWTIME-UUID-555",
        "movie": {
          "publicId": "uuid",
          "slug": "dune-part-two",
          "title": "Dune: Part Two"
        },
        "movieVersion": {
          "publicId": "uuid",
          "versionName": "IMAX Vietsub",
          "format": "IMAX",
          "audioLanguage": "EN",
          "subtitleLanguage": "VI"
        },
        "cinema": {
          "publicId": "uuid",
          "slug": "lorafilm-nguyen-trai",
          "name": "LoraFilm Nguyễn Trãi",
          "timezone": "Asia/Ho_Chi_Minh"
        },
        "auditorium": {
          "publicId": "uuid",
          "name": "Room 2",
          "screenType": "IMAX",
          "soundType": "DOLBY_ATMOS"
        },
        "startTime": "2026-07-20T19:30:00Z",
        "endTime": "2026-07-20T21:50:00Z",
        "status": "OPEN_FOR_BOOKING"
      }
    ],
    "pageNo": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 2.8. Showtime Detail

```http
GET /api/showtimes/{showtimePublicId}
```

#### Response Example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "showtimePublicId": "SHOWTIME-UUID-555",
    "movie": {
      "publicId": "uuid",
      "slug": "dune-part-two",
      "title": "Dune: Part Two"
    },
    "movieVersion": {
      "publicId": "uuid",
      "versionName": "IMAX Vietsub",
      "format": "IMAX",
      "audioLanguage": "EN",
      "subtitleLanguage": "VI"
    },
    "cinema": {
      "publicId": "uuid",
      "slug": "lorafilm-nguyen-trai",
      "name": "LoraFilm Nguyễn Trãi",
      "timezone": "Asia/Ho_Chi_Minh"
    },
    "auditorium": {
      "publicId": "uuid",
      "name": "Room 2",
      "screenType": "IMAX",
      "soundType": "DOLBY_ATMOS"
    },
    "startTime": "2026-07-20T19:30:00Z",
    "endTime": "2026-07-20T21:50:00Z",
    "status": "OPEN_FOR_BOOKING"
  }
}
```

---

### 2.9. Showtime Seat Layout

```http
GET /api/showtimes/{showtimePublicId}/seat-layout
```

#### Response example

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "showtimePublicId": "uuid",
    "movie": {
      "publicId": "uuid",
      "slug": "dune-part-two",
      "title": "Dune: Part Two"
    },
    "movieVersion": {
      "publicId": "uuid",
      "versionName": "IMAX Vietsub",
      "format": "IMAX",
      "audioLanguage": "EN",
      "subtitleLanguage": "VI"
    },
    "cinema": {
      "publicId": "uuid",
      "slug": "lorafilm-nguyen-trai",
      "name": "LoraFilm Nguyễn Trãi",
      "timezone": "Asia/Ho_Chi_Minh"
    },
    "auditorium": {
      "publicId": "uuid",
      "name": "Room 2",
      "screenType": "IMAX",
      "soundType": "DOLBY_ATMOS"
    },
    "startTime": "2026-07-20T19:30:00Z",
    "endTime": "2026-07-20T21:50:00Z",
    "seats": [
      {
        "publicId": "uuid",
        "seatCode": "F7",
        "rowLabel": "F",
        "seatNumber": 7,
        "positionRow": 6,
        "positionColumn": 7,
        "seatType": "VIP",
        "price": 120000,
        "currency": "VND",
        "status": "ACTIVE",
        "blockedForShowtime": false
      }
    ]
  }
}
```

#### Important

* Seat status ở response này là master/operation status.
* `HELD` / `BOOKED` không được trả về từ Movie Service.
* Trạng thái giữ ghế và đã đặt ghế thuộc trách nhiệm của Booking Service.

---

## 3. Admin APIs

### 3.1. Aggregate Movie Create

```http
POST /api/admin/movies/full
```

#### Behavior

* Tạo movie cùng với genres, credits, production companies, versions và media trong một transaction.
* Nếu bất kỳ nested validation nào fail, rollback toàn bộ transaction.

---

### 3.2. Movie Management

```http
POST /api/admin/movies
PUT /api/admin/movies/{moviePublicId}
PATCH /api/admin/movies/{moviePublicId}/status
DELETE /api/admin/movies/{moviePublicId}
```

#### Important

* `DELETE` là soft delete.

---

### 3.3. Genre Management

```http
POST /api/admin/genres
PUT /api/admin/genres/{genrePublicId}
PATCH /api/admin/genres/{genrePublicId}/status
DELETE /api/admin/genres/{genrePublicId}
```

---

### 3.4. People / Credits

```http
POST /api/admin/people
PUT /api/admin/people/{personPublicId}
PATCH /api/admin/people/{personPublicId}/status
DELETE /api/admin/people/{personPublicId}
```

```http
POST /api/admin/movies/{moviePublicId}/credits
PUT /api/admin/movies/{moviePublicId}/credits
```

---

### 3.5. Production Companies

```http
POST /api/admin/production-companies
PUT /api/admin/production-companies/{companyPublicId}
PATCH /api/admin/production-companies/{companyPublicId}/status
DELETE /api/admin/production-companies/{companyPublicId}
```

```http
POST /api/admin/movies/{moviePublicId}/production-companies
PUT /api/admin/movies/{moviePublicId}/production-companies
```

---

### 3.6. Movie Versions / Media

```http
POST /api/admin/movies/{moviePublicId}/versions
PUT /api/admin/movie-versions/{versionPublicId}
PATCH /api/admin/movie-versions/{versionPublicId}/status
DELETE /api/admin/movie-versions/{versionPublicId}
```

```http
POST /api/admin/movies/{moviePublicId}/media
PUT /api/admin/movie-media/{mediaPublicId}
PATCH /api/admin/movie-media/{mediaPublicId}/status
DELETE /api/admin/movie-media/{mediaPublicId}
```

---

### 3.7. Cinema Management

```http
POST /api/admin/cinemas
PUT /api/admin/cinemas/{cinemaPublicId}
PATCH /api/admin/cinemas/{cinemaPublicId}/status
DELETE /api/admin/cinemas/{cinemaPublicId}
```

```http
POST /api/admin/cinemas/{cinemaPublicId}/media
PUT /api/admin/cinema-media/{mediaPublicId}
PATCH /api/admin/cinema-media/{mediaPublicId}/status
```

```http
PUT /api/admin/cinemas/{cinemaPublicId}/operating-hours
```

```http
POST /api/admin/cinemas/{cinemaPublicId}/closure-periods
PATCH /api/admin/cinema-closure-periods/{closureId}/cancel
```

---

### 3.8. Auditorium / Seat Layout

```http
POST /api/admin/cinemas/{cinemaPublicId}/auditoriums
PUT /api/admin/auditoriums/{auditoriumPublicId}
PATCH /api/admin/auditoriums/{auditoriumPublicId}/status
DELETE /api/admin/auditoriums/{auditoriumPublicId}
```

```http
POST /api/admin/auditoriums/{auditoriumPublicId}/maintenance-windows
PATCH /api/admin/auditorium-maintenance-windows/{windowId}/cancel
```

```http
POST /api/admin/seat-types
PUT /api/admin/seat-types/{seatTypePublicId}
PATCH /api/admin/seat-types/{seatTypePublicId}/status
```

```http
POST /api/admin/auditoriums/{auditoriumPublicId}/seats/bulk
GET /api/admin/auditoriums/{auditoriumPublicId}/seat-layout
PUT /api/admin/seats/{seatPublicId}
PATCH /api/admin/seats/{seatPublicId}/status
DELETE /api/admin/seats/{seatPublicId}
```

---

### 3.9. Showtime / Pricing

```http
POST /api/admin/showtimes
PUT /api/admin/showtimes/{showtimePublicId}
```

```http
PATCH /api/admin/showtimes/{showtimePublicId}/open
PATCH /api/admin/showtimes/{showtimePublicId}/close
PATCH /api/admin/showtimes/{showtimePublicId}/cancel
PATCH /api/admin/showtimes/{showtimePublicId}/finish
```

```http
PUT /api/admin/showtimes/{showtimePublicId}/prices
GET /api/admin/showtimes/{showtimePublicId}/prices
```

```http
POST /api/admin/showtimes/{showtimePublicId}/blocked-seats
PATCH /api/admin/showtime-blocked-seats/{blockedSeatId}/cancel
```

```http
GET /api/admin/showtimes/{showtimePublicId}/status-history
```

---

## 4. Internal APIs

### 4.1. Showtime Booking Context

```http
POST /internal/showtimes/{showtimePublicId}/booking-context
X-Internal-Token: ...
Content-Type: application/json
```

#### Request

```json
{
  "seatPublicIds": ["uuid-1", "uuid-2"]
}
```

#### Response

```json
{
  "showtimePublicId": "uuid",
  "movie": {
    "publicId": "uuid",
    "title": "Dune: Part Two",
    "slug": "dune-part-two"
  },
  "movieVersion": {
    "publicId": "uuid",
    "versionName": "IMAX Vietsub",
    "format": "IMAX",
    "audioLanguage": "EN",
    "subtitleLanguage": "VI"
  },
  "cinema": {
    "publicId": "uuid",
    "name": "LoraFilm Nguyễn Trãi",
    "timezone": "Asia/Ho_Chi_Minh"
  },
  "auditorium": {
    "publicId": "uuid",
    "name": "Room 2"
  },
  "status": "OPEN_FOR_BOOKING",
  "seats": [
    {
      "publicId": "uuid-1",
      "seatCode": "F7",
      "seatType": "VIP",
      "price": 120000,
      "currency": "VND"
    }
  ],
  "totalAmount": 120000,
  "currency": "VND"
}
```

#### Validation

* Internal token required.
* Showtime phải tồn tại.
* Showtime phải có status `OPEN_FOR_BOOKING`.
* Selected seats phải thuộc auditorium của showtime.
* Selected seats phải active.
* Selected seats không bị blocked cho showtime đó.
* Prices phải tồn tại.
* Movie Service không validate trạng thái `HELD` / `BOOKED`.

---

## 5. Responsibility Boundary

### Movie Service chịu trách nhiệm

* Movie metadata.
* Cinema metadata.
* Auditorium metadata.
* Seat master layout.
* Showtime schedule.
* Showtime lifecycle.
* Showtime pricing.
* Showtime blocked seats.
* Validate booking context cho Booking Service.

### Movie Service không chịu trách nhiệm

* Giữ ghế tạm thời.
* Xác nhận ghế đã được đặt.
* Quản lý booking transaction.
* Quản lý payment transaction.
* Trả về seat state `HELD` / `BOOKED` cho customer APIs.

### Booking Service chịu trách nhiệm

* Seat reservation.
* Seat lock.
* Trạng thái ghế theo showtime: `AVAILABLE`, `HELD`, `BOOKED`.
* Booking lifecycle.
* Payment coordination.
