# API dịch vụ Phân tích

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Cung cấp số liệu doanh thu, lượt đặt vé, hiệu quả phim và lịch sử nhu cầu cho màn hình quản trị. |
| Cổng chạy trực tiếp | `http://localhost:8089` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8089/v3/api-docs` |
| Swagger UI | `http://localhost:8089/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/analytics/**`<br>`/api/admin/reports/**` |
| Quy mô hiện tại | 3 controller, 11 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/analytics-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- API báo cáo qua Gateway yêu cầu đăng nhập và quyền xem báo cáo phù hợp.
- API dưới /internal/** chỉ dành cho service khác và yêu cầu X-Internal-Token.

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

### Nhóm `AnalyticsController`

Đường dẫn gốc: `/api/analytics`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| PATCH | `/api/analytics/alerts/{id}/acknowledge` | `acknowledgeAlert` | Một trong các quyền: `ROLE_ADMIN`, `ROLE_MANAGER`, `ANALYTICS_MANAGE` |
| GET | `/api/analytics/cinemas` | `cinemas` | Đã đăng nhập |
| GET | `/api/analytics/dashboard` | `dashboard` | Đã đăng nhập |
| GET | `/api/analytics/jobs` | `jobs` | Một trong các quyền: `ROLE_ADMIN`, `ANALYTICS_REBUILD` |
| POST | `/api/analytics/jobs/rebuild` | `rebuild` | Một trong các quyền: `ROLE_ADMIN`, `ANALYTICS_REBUILD` |
| PATCH | `/api/analytics/recommendations/{id}/status` | `updateRecommendation` | Một trong các quyền: `ROLE_ADMIN`, `ROLE_MANAGER`, `ANALYTICS_MANAGE` |

### Nhóm `InternalDemandHistoryController`

Đường dẫn gốc: `/internal/analytics`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/analytics/demand-snapshot` | `snapshot` | Nội bộ (token service) |

### Nhóm `MovieAnalyticsController`

Đường dẫn gốc: `/api/analytics/movies`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/analytics/movies` | `getMovieRevenueList` | Một trong các quyền: `ROLE_ADMIN`, `ROLE_MANAGER` |
| GET | `/api/analytics/movies/{movieId}` | `getMovieRevenueDetail` | Một trong các quyền: `ROLE_ADMIN`, `ROLE_MANAGER` |
| GET | `/api/analytics/movies/{movieId}/trend` | `getMovieRevenueTrend` | Một trong các quyền: `ROLE_ADMIN`, `ROLE_MANAGER` |
| GET | `/api/analytics/movies/top` | `getTopMovies` | Một trong các quyền: `ROLE_ADMIN`, `ROLE_MANAGER` |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
