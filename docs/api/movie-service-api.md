# API dịch vụ Phim, rạp và suất chiếu

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Quản lý phim, thể loại, người tham gia, rạp, phòng chiếu, ghế, suất chiếu, giá và lịch tự động. |
| Cổng chạy trực tiếp | `http://localhost:8082` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8082/v3/api-docs` |
| Swagger UI | `http://localhost:8082/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/movies/**`<br>`/api/genres/**`<br>`/api/admin/**`<br>`/api/showtimes/**`<br>`/api/cinemas/**`<br>`/api/manager/**`<br>`/api/customer/**`<br>`/api/public/people/**` |
| Quy mô hiện tại | 38 controller, 163 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/movie-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Các API GET dành cho khách ở /api/customer/**, /api/public/people/**, /api/cinemas/** và /api/showtimes/** là công khai qua Gateway.
- API /api/admin/** yêu cầu vai trò quản trị; /api/manager/** yêu cầu vai trò quản lý.
- API /internal/** gọi trực tiếp Movie Service và yêu cầu X-Internal-Token.

Gateway xóa các header nhận dạng do client tự gửi và tự gắn thông tin người dùng sau khi xác minh JWT. Client không được tự tạo `loggedInUser`, `loggedInUserId`, `loggedInRole` hoặc các header `X-Authenticated-*`.

## 4. Dạng phản hồi

Phần lớn endpoint trả về lớp bọc `ApiResponse`. Một phản hồi thành công thường có dạng:

```json
{
  "success": true,
  "message": "Thao tác thành công",
  "data": {}
}
```

Khi lỗi, response có thể bổ sung `errorCode`, `errors` hoặc `timestamp` tùy service. Các mã HTTP thường gặp:

| Mã | Ý nghĩa |
|---|---|
| 200 | Thành công |
| 201 | Đã tạo dữ liệu |
| 202 | Đã tiếp nhận và xử lý bất đồng bộ |
| 204 | Thành công, không có nội dung trả về |
| 400 | Dữ liệu gửi lên không hợp lệ |
| 401 | Thiếu hoặc sai thông tin đăng nhập/token nội bộ |
| 403 | Đã đăng nhập nhưng không đủ quyền |
| 404 | Không tìm thấy dữ liệu |
| 409 | Xung đột trạng thái hoặc trùng yêu cầu |
| 500 | Lỗi không mong đợi ở server |

## 5. Danh mục endpoint hiện hành

Mỗi nhóm tương ứng một controller thực tế. Quyền chi tiết lấy từ `@PreAuthorize`; nếu controller không khai báo riêng, bảng ghi theo nhóm đường dẫn và SecurityConfig.

### Nhóm `AdminAuditoriumController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| DELETE | `/api/admin/auditoriums/{auditoriumPublicId}` | `deleteAuditorium` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/auditoriums/{auditoriumPublicId}` | `updateAuditorium` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/cinemas/{cinemaPublicId}/auditoriums` | `createAuditorium` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/cinemas/{cinemaPublicId}/auditoriums/{auditoriumPublicId}/clone` | `cloneAuditoriumLayout` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/cinemas/{cinemaPublicId}/auditoriums/with-layout` | `createAuditoriumWithLayout` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminAuditoriumLayoutSourceController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/auditorium-layout-templates` | `getTemplates` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/auditoriums/{auditoriumPublicId}/clone-preview` | `getClonePreview` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/auditoriums/{auditoriumPublicId}/validate-layout` | `validateLayout` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/auditoriums/{sourceAuditoriumPublicId}/clone` | `cloneAsNew` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/auditoriums/from-template` | `createFromTemplate` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminAuditoriumMaintenanceController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/auditoriums/{auditoriumPublicId}/maintenance-windows` | `getMaintenanceWindows` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/auditoriums/{auditoriumPublicId}/maintenance-windows` | `createWindow` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/maintenance-windows/{maintenanceWindowId}/cancel` | `cancelWindow` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/maintenance-windows/{maintenanceWindowId}/extend` | `extendWindow` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/maintenance-windows/{maintenanceWindowId}/resolve` | `resolveWindow` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminAuditoriumMaintenanceImpactController`

Đường dẫn gốc: `/api/admin/auditoriums`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/auditoriums/{auditoriumPublicId}/maintenance-windows/impact-preview` | `preview` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminAutoScheduleController`

Đường dẫn gốc: `/api/admin/auto-schedules`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/auto-schedules/preflight` | `preflight` | Vai trò `ADMIN` |

### Nhóm `AdminCinemaClosureImpactController`

Đường dẫn gốc: `/api/admin/cinemas`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/cinemas/{cinemaPublicId}/closure-periods/impact-preview` | `preview` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminCinemaController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| DELETE | `/api/admin/cinema-media/{mediaPublicId}` | `deleteCinemaMedia` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/cinema-media/{mediaPublicId}` | `updateCinemaMedia` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/cinemas` | `getAdminCinemas` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/cinemas` | `createCinema` | Quản trị (theo cấu hình bảo mật) |
| DELETE | `/api/admin/cinemas/{cinemaPublicId}` | `deleteCinema` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/cinemas/{cinemaPublicId}` | `getAdminCinemaDetail` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/cinemas/{cinemaPublicId}` | `updateCinema` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/cinemas/{cinemaPublicId}/closure-periods` | `getAdminCinemaClosurePeriods` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/cinemas/{cinemaPublicId}/closure-periods` | `createClosurePeriod` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/cinemas/{cinemaPublicId}/media` | `addCinemaMedia` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/cinemas/{cinemaPublicId}/operating-hours` | `updateOperatingHours` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/cinemas/{cinemaPublicId}/readiness` | `getCinemaReadiness` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/cinemas/{cinemaPublicId}/status` | `updateCinemaStatus` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/closure-periods/{closurePeriodId}/cancel` | `cancelClosurePeriod` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminCinemaMediaUploadController`

Đường dẫn gốc: `/api/admin/cinemas/media`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/cinemas/media/upload` | `uploadMedia` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminGenreController`

Đường dẫn gốc: `/api/admin/genres`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/genres` | `getGenres` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/genres` | `createGenre` | Quản trị (theo cấu hình bảo mật) |
| DELETE | `/api/admin/genres/{publicId}` | `deleteGenre` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/genres/{publicId}` | `getGenre` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/genres/{publicId}` | `updateGenre` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminMovieController`

Đường dẫn gốc: `/api/admin/movies`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/movies` | `getMovies` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/movies` | `createMovie` | Quản trị (theo cấu hình bảo mật) |
| DELETE | `/api/admin/movies/{publicId}` | `deleteMovie` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/movies/{publicId}` | `getMovieDetail` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/movies/{publicId}` | `updateMovie` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/movies/{publicId}/credits` | `assignCreditsPost` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/movies/{publicId}/credits` | `assignCreditsPut` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/movies/{publicId}/exhibition-periods` | `getExhibitionPeriods` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/movies/{publicId}/exhibition-periods` | `createExhibitionPeriod` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/movies/{publicId}/genres` | `assignGenresPost` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/movies/{publicId}/genres` | `assignGenresPut` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/movies/{publicId}/launch-readiness` | `getLaunchReadiness` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/movies/{publicId}/production-companies` | `assignProductionCompaniesPost` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/movies/{publicId}/production-companies` | `assignProductionCompaniesPut` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/movies/{publicId}/status` | `updateMovieStatus` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/movies/{publicId}/status-history` | `getStatusHistory` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/movies/{publicId}/tmdb-review` | `getTmdbReview` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/movies/bulk-approve` | `bulkApproveTmdbMovies` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/movies/summary` | `getMovieSummary` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/movies/tmdb-queue-breakdown` | `getTmdbQueueBreakdown` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminPersonController`

Đường dẫn gốc: `/api/admin/people`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/people` | `createPerson` | Quyền `ROLE_ADMIN` |
| DELETE | `/api/admin/people/{personId}` | `deletePerson` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/people/{personId}` | `updatePerson` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/people/by-name` | `findPersonByName` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminPricePolicyController`

Đường dẫn gốc: `/api/admin/pricing`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/pricing/policies` | `search` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/pricing/policies` | `create` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/pricing/policies/{publicId}` | `get` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/pricing/policies/{publicId}` | `update` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/pricing/policies/{publicId}/activate` | `activate` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/pricing/policies/{publicId}/copy` | `copy` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/pricing/policies/{publicId}/deactivate` | `deactivate` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/pricing/policies/{publicId}/usage` | `usage` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/pricing/resolve-preview` | `preview` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminProductionCompanyController`

Đường dẫn gốc: `/api/admin/production-companies`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/production-companies` | `createProductionCompany` | Quyền `ROLE_ADMIN` |
| DELETE | `/api/admin/production-companies/{companyId}` | `deleteProductionCompany` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/production-companies/{companyId}` | `updateProductionCompany` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/production-companies/by-name` | `findProductionCompanyByName` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminSeatController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/auditoriums/{auditoriumPublicId}/seats/bulk` | `bulkCreateSeats` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/seats/{seatPublicId}` | `updateSeat` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminSeatTypeController`

Đường dẫn gốc: `/api/admin/seat-types`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/seat-types` | `getAllSeatTypes` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/seat-types` | `createSeatType` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/seat-types/{seatTypePublicId}` | `updateSeatType` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminShowtimeController`

Đường dẫn gốc: `/api/admin/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/showtimes` | `createShowtime` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/showtimes/{showtimePublicId}` | `updateShowtime` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/showtimes/{showtimePublicId}/status` | `transitionStatus` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/showtimes/{showtimePublicId}/status-history` | `getStatusHistory` | Quyền `ROLE_ADMIN` |
| DELETE | `/api/admin/showtimes/batch/{batchId}` | `deleteBatch` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/showtimes/batch/{batchId}/status` | `transitionBatchStatus` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/showtimes/batch/{batchId}/status-preview` | `previewBatchStatus` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminShowtimePricingController`

Đường dẫn gốc: `/api/admin/showtimes/{showtimeId}`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/showtimes/{showtimeId}/prices` | `getPrices` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/showtimes/{showtimeId}/prices` | `updatePrices` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/showtimes/{showtimeId}/pricing` | `getPricing` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/showtimes/{showtimeId}/pricing/resolve` | `resolvePricing` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminShowtimeQueryController`

Đường dẫn gốc: `/api/admin/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/showtimes` | `getAdminShowtimes` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/showtimes/{showtimePublicId}` | `getAdminShowtimeDetail` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminShowtimeScheduleController`

Đường dẫn gốc: `/api/admin/showtime-schedules`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/showtime-schedules` | `getPreviewHistory` | Vai trò `ADMIN` |
| GET | `/api/admin/showtime-schedules/{previewPublicId}` | `getPreview` | Vai trò `ADMIN` |
| POST | `/api/admin/showtime-schedules/{previewPublicId}/apply` | `applyPreview` | Vai trò `ADMIN` |
| POST | `/api/admin/showtime-schedules/{previewPublicId}/cancel` | `cancelPreview` | Vai trò `ADMIN` |
| PUT | `/api/admin/showtime-schedules/{previewPublicId}/items` | `updateSelections` | Vai trò `ADMIN` |
| POST | `/api/admin/showtime-schedules/{previewPublicId}/pricing-readiness` | `checkPricingReadiness` | Vai trò `ADMIN` |
| GET | `/api/admin/showtime-schedules/eligible-movies` | `getEligibleMovies` | Vai trò `ADMIN` |
| POST | `/api/admin/showtime-schedules/generate-preview` | `generatePreview` | Vai trò `ADMIN` |

### Nhóm `AdminShowtimeSeatController`

Đường dẫn gốc: `/api/admin/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/showtimes/{showtimePublicId}/blocked-seats` | `blockSeats` | Quyền `ROLE_ADMIN` |
| PUT | `/api/admin/showtimes/{showtimePublicId}/blocked-seats/release` | `releaseSeats` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/showtimes/{showtimePublicId}/seat-control` | `getSeatControl` | Quyền `ROLE_ADMIN` |

### Nhóm `AdminTmdbController`

Đường dẫn gốc: `/api/admin/tmdb`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/tmdb/approve` | `approveTmdbMovie` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AuditoriumQueryController`

Đường dẫn gốc: `/`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/auditoriums/{id}/seat-layout` | `getAdminSeatLayout` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/auditoriums/{id}/seat-layout` | `getCustomerSeatLayout` | Chỉ gọi trực tiếp; Gateway chưa định tuyến |

### Nhóm `CinemaController`

Đường dẫn gốc: `/api/cinemas`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/cinemas` | `getCinemas` | Công khai |
| GET | `/api/cinemas/{cinemaIdOrSlug}` | `getCinemaDetail` | Công khai |
| GET | `/api/cinemas/{cinemaPublicId}/closure-periods` | `getCinemaClosurePeriods` | Công khai |

### Nhóm `CustomerGenreController`

Đường dẫn gốc: `/api/customer/genres`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/customer/genres` | `getActiveGenres` | Công khai |

### Nhóm `CustomerMovieController`

Đường dẫn gốc: `/api/customer/movies`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/customer/movies` | `getMovies` | Công khai |
| GET | `/api/customer/movies/{identifier}` | `getMovieDetail` | Công khai |
| GET | `/api/customer/movies/{identifier}/booking-options` | `getBookingOptions` | Công khai |
| GET | `/api/customer/movies/{identifier}/credits` | `getMovieCredits` | Công khai |
| GET | `/api/customer/movies/{identifier}/production-companies` | `getMovieProductionCompanies` | Công khai |

### Nhóm `CustomerShowtimeController`

Đường dẫn gốc: `/api/customer/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/customer/showtimes/{showtimePublicId}/seat-layout` | `getSeatLayout` | Công khai |

### Nhóm `InternalCinemaController`

Đường dẫn gốc: `/internal/cinemas`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/internal/cinemas/{publicId}/exists` | `exists` | Nội bộ (token service) |

### Nhóm `InternalShowtimeController`

Đường dẫn gốc: `/internal/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/showtimes/{showtimeId}/booking-context` | `getBookingContext` | Nội bộ (token service) |
| GET | `/internal/showtimes/{showtimeId}/seat-layout` | `getSeatLayout` | Nội bộ (token service) |
| POST | `/internal/showtimes/by-public-id/{showtimePublicId}/booking-context` | `getBookingContextByPublicId` | Nội bộ (token service) |
| GET | `/internal/showtimes/by-public-id/{showtimePublicId}/presentation` | `getPresentationByPublicId` | Nội bộ (token service) |

### Nhóm `LocationAdminController`

Đường dẫn gốc: `/api/admin/locations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/locations/suggestions` | `getSuggestions` | Vai trò `ADMIN` |

### Nhóm `ManagerAuditoriumMaintenanceImpactController`

Đường dẫn gốc: `/api/manager/cinemas/{cinemaPublicId}/auditoriums`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/manager/cinemas/{cinemaPublicId}/auditoriums/{auditoriumPublicId}/maintenance-windows/impact-preview` | `preview` | Quyền `ROLE_MANAGER` |

### Nhóm `ManagerCinemaController`

Đường dẫn gốc: `/api/manager`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/manager/cinemas` | `getAssignedCinemas` | Quyền `ROLE_MANAGER` |
| GET | `/api/manager/cinemas/{cinemaPublicId}` | `getAssignedCinema` | Quyền `ROLE_MANAGER` |
| POST | `/api/manager/cinemas/{cinemaPublicId}/auditoriums/{auditoriumPublicId}/maintenance-windows` | `createMaintenanceWindow` | Quyền `ROLE_MANAGER` |
| GET | `/api/manager/cinemas/{cinemaPublicId}/maintenance-windows` | `getMaintenanceWindows` | Quyền `ROLE_MANAGER` |
| PUT | `/api/manager/cinemas/{cinemaPublicId}/maintenance-windows/{maintenanceWindowId}/cancel` | `cancelMaintenanceWindow` | Quyền `ROLE_MANAGER` |
| PUT | `/api/manager/cinemas/{cinemaPublicId}/maintenance-windows/{maintenanceWindowId}/extend` | `extendMaintenanceWindow` | Quyền `ROLE_MANAGER` |
| PUT | `/api/manager/cinemas/{cinemaPublicId}/maintenance-windows/{maintenanceWindowId}/resolve` | `resolveMaintenanceWindow` | Quyền `ROLE_MANAGER` |
| GET | `/api/manager/showtimes` | `getShowtimes` | Quyền `ROLE_MANAGER` |
| GET | `/api/manager/showtimes/{showtimePublicId}` | `getShowtime` | Quyền `ROLE_MANAGER` |
| PUT | `/api/manager/showtimes/{showtimePublicId}/status` | `transitionShowtimeStatus` | Quyền `ROLE_MANAGER` |
| GET | `/api/manager/showtimes/{showtimePublicId}/status-history` | `getShowtimeStatusHistory` | Quyền `ROLE_MANAGER` |

### Nhóm `ManagerShowtimeSeatController`

Đường dẫn gốc: `/api/manager/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/manager/showtimes/{showtimePublicId}/blocked-seats` | `blockSeats` | Quyền `ROLE_MANAGER` |
| PUT | `/api/manager/showtimes/{showtimePublicId}/blocked-seats/release` | `releaseSeats` | Quyền `ROLE_MANAGER` |
| GET | `/api/manager/showtimes/{showtimePublicId}/seat-control` | `getSeatControl` | Quyền `ROLE_MANAGER` |

### Nhóm `MovieMediaController`

Đường dẫn gốc: `/`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| DELETE | `/api/admin/movie-media/{mediaId}` | `deleteMedia` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/movie-media/{mediaId}` | `getMedia` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/movie-media/{mediaId}` | `updateMedia` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/movies/{movieId}/media` | `getAdminMedia` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/movies/{movieId}/media` | `createMedia` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/movies/{movieId}/media` | `getCustomerMedia` | Đã đăng nhập |

### Nhóm `MovieVersionController`

Đường dẫn gốc: `/`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| DELETE | `/api/admin/movie-versions/{versionId}` | `deleteVersion` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/movie-versions/{versionId}` | `getVersion` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/movie-versions/{versionId}` | `updateVersion` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/movies/{movieId}/versions` | `getAllVersions` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/movies/{movieId}/versions` | `createVersion` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/movies/{movieId}/versions` | `getActiveVersions` | Đã đăng nhập |

### Nhóm `PublicPersonController`

Đường dẫn gốc: `/api/public/people`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/public/people` | `getPeople` | Công khai |
| GET | `/api/public/people/{identifier}` | `getPerson` | Công khai |
| GET | `/api/public/people/{identifier}/movies` | `getPersonMovies` | Công khai |

### Nhóm `ShowtimeController`

Đường dẫn gốc: `/api/showtimes`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/showtimes` | `getShowtimes` | Công khai |
| GET | `/api/showtimes/{showtimePublicId}` | `getShowtimeDetail` | Công khai |
| GET | `/api/showtimes/{showtimePublicId}/seat-layout` | `getSeatLayout` | Công khai |

### Nhóm `TmdbAdminController`

Đường dẫn gốc: `/api/admin/tmdb`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/tmdb/sync/{tmdbId}` | `syncMovieById` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/tmdb/sync/bulk/reset` | `resetBulkSync` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/tmdb/sync/bulk/start` | `startBulkSync` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/tmdb/sync/bulk/status` | `getBulkSyncStatus` | Quyền `ROLE_ADMIN` |
| POST | `/api/admin/tmdb/sync/bulk/stop` | `stopBulkSync` | Quyền `ROLE_ADMIN` |
| GET | `/api/admin/tmdb/sync/state` | `getSyncState` | Quyền `ROLE_ADMIN` |

### Nhóm `TmdbMovieSearchController`

Đường dẫn gốc: `/api/admin/tmdb/movies`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/tmdb/movies/search` | `search` | Quyền `ROLE_ADMIN` |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
