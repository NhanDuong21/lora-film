# Hướng dẫn nhập phim từ TMDB

Tài liệu này mô tả hợp đồng giữa **Movie Service** và dịch vụ hỗ trợ TMDB. Dịch vụ TMDB chỉ cung cấp dữ liệu nguồn; Movie Service sở hữu trạng thái phim, thời gian khai thác tại rạp và quyền thao tác của quản trị viên.

## Nguyên tắc nghiệp vụ

1. Movie Service không tự nhập phim khi khởi động, trừ khi biến `TMDB_AUTO_SYNC_ENABLED=true` được cấu hình rõ ràng.
2. Phim mới từ TMDB luôn được lưu ở trạng thái `DRAFT` — trên giao diện là **Chờ hoàn thiện**.
3. Việc nhập dữ liệu không tự chuyển phim sang Sắp chiếu hoặc Đang chiếu.
4. `originalReleaseDate` là ngày phát hành gốc lấy từ TMDB.
5. `releaseDate` và `endDate` là thời gian của đợt khai thác hiện tại tại rạp.
6. Khi chiếu lại phim cũ, quản trị viên lập đợt khai thác mới; không sửa ngày phát hành gốc.
7. Phim cũ nhập từ TMDB chưa có thời gian khai thác tại rạp (`releaseDate = null`); quản trị viên phải lập một đợt tương lai nếu muốn chiếu lại.
8. Dữ liệu quản trị viên đã chỉnh không bị tiến trình TMDB tự động ghi đè.

## Cấu hình Movie Service

```properties
tmdb.integration-enabled=${TMDB_INTEGRATION_ENABLED:true}
tmdb.scheduler.enabled=${TMDB_AUTO_SYNC_ENABLED:false}
tmdb.batch-size=100
```

- `TMDB_INTEGRATION_ENABLED`: cho phép Movie Service kết nối tới dịch vụ TMDB.
- `TMDB_AUTO_SYNC_ENABLED`: cho phép chạy lịch tự động. Mặc định phải là `false`.

## API dành cho quản trị viên

### Xem trạng thái tiến trình

```http
GET /api/admin/tmdb/sync/state
```

Kết quả gồm phạm vi ngày, giới hạn, số phim đã xét, số phim nhập mới, số phim bỏ qua và thông báo dễ hiểu.

### Nhập nhiều phim

```http
POST /api/admin/tmdb/sync/bulk/start
Content-Type: application/json

{
  "scope": "FUTURE",
  "releaseDateFrom": "2026-08-08",
  "releaseDateTo": "2027-08-08",
  "maxMovies": 500
}
```

Các phạm vi được hỗ trợ:

- `FUTURE`: phim có ngày phát hành gốc trong tương lai.
- `PAST`: phim đã phát hành; bắt buộc chọn đầy đủ hai ngày.
- `RANGE`: một khoảng ngày tự chọn.
- `ALL`: không lọc ngày; vẫn phải đặt giới hạn tối đa 5.000 phim.

Chạy lại từ đầu:

```http
POST /api/admin/tmdb/sync/bulk/reset
```

Dừng tiến trình:

```http
POST /api/admin/tmdb/sync/bulk/stop
```

### Nhập một phim

```http
POST /api/admin/tmdb/sync/{tmdbId}
```

Luồng này phù hợp nhất khi quản trị viên muốn chiếu lại một phim cũ cụ thể.

## Hợp đồng với dịch vụ hỗ trợ TMDB

### Đọc danh sách theo con trỏ và khoảng ngày

```http
GET /api/tmdb/export
    ?cursor=0
    &limit=100
    &releaseDateFrom=2026-08-08
    &releaseDateTo=2027-08-08
```

Hai tham số ngày là tùy chọn. Dịch vụ hỗ trợ TMDB nên lọc trước khi trả dữ liệu để Movie Service không phải đọc toàn bộ kho. Movie Service vẫn lọc lại một lần nhằm bảo vệ quy tắc nghiệp vụ khi dịch vụ nguồn chưa hỗ trợ phiên bản hợp đồng mới.

Ví dụ kết quả:

```json
{
  "cursor": 0,
  "nextCursor": 100,
  "limit": 100,
  "hasMore": true,
  "movies": [
    {
      "tmdbId": 550,
      "lastUpdated": "2026-07-16T14:00:00",
      "qualityStatus": "ACCEPT",
      "movie": {
        "tmdbId": 550,
        "title": "Fight Club",
        "originalTitle": "Fight Club",
        "releaseDate": "1999-10-15",
        "runtimeMinutes": 139
      }
    }
  ]
}
```

### Đọc một phim

```http
GET /api/tmdb/movies/{tmdbId}
```

### Phim mới và phim được cập nhật

```http
GET /api/tmdb/movies/latest
GET /api/tmdb/movies/updated?lastUpdated=2026-08-06
```

### Tìm phim theo tên

Để màn quản trị có thể gợi ý phim theo tên, dịch vụ TMDB riêng cần cung cấp:

```http
GET /api/tmdb/search?query=Avatar&limit=8
x-api-key: <khóa nội bộ>
```

Phản hồi:

```json
{
  "results": [
    {
      "tmdbId": 19995,
      "title": "Avatar",
      "originalTitle": "Avatar",
      "releaseDate": "2009-12-18",
      "posterPath": "/kyeqWdyUXW608qlYkRqosgbbJyK.jpg",
      "overview": "Nội dung giới thiệu ngắn"
    }
  ]
}
```

Movie Service gọi API này ở phía máy chủ; frontend không gọi trực tiếp TMDB và không được nhận khóa nội bộ.

API dành cho màn quản trị:

```http
GET /api/admin/tmdb/movies/search?query=Avatar&limit=8
```

Kết quả có thêm `alreadyImported`, `localMoviePublicId` và `localMovieStatus` để ngăn admin nhập trùng phim.

Hai API tìm kiếm chỉ chạy khi quản trị viên nhập tên phim trên giao diện; chúng không tự nhập phim và không phụ thuộc lịch tự động.

## Đợt khai thác tại rạp

Xem lịch sử:

```http
GET /api/admin/movies/{moviePublicId}/exhibition-periods
```

Lập đợt mới:

```http
POST /api/admin/movies/{moviePublicId}/exhibition-periods
Content-Type: application/json

{
  "startDate": "2026-08-20",
  "endDate": "2026-09-05",
  "note": "Chiếu lại trong tuần lễ phim kinh điển"
}
```

Sau khi lập đợt tương lai cho phim Đã kết thúc, quản trị viên có thể chuyển phim sang **Sắp chiếu**. Ngày phát hành gốc không thay đổi.

## Chuyển đổi cơ sở dữ liệu

Chạy tệp:

```text
docs/database/mysql/migrations/20260807_separate_tmdb_release_and_exhibition_periods.sql
```

Migration sẽ:

- bổ sung `original_release_date`;
- cho phép phim TMDB chưa có thời gian khai thác được giữ ở Chờ hoàn thiện;
- tạo bảng lịch sử đợt khai thác;
- bổ sung thông tin tiến độ cho tiến trình TMDB.
