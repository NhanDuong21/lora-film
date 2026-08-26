# API dịch vụ Thông báo

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Gửi thông báo nội bộ, quản lý hộp thư trong ứng dụng, mẫu thông báo và cấu hình nhà cung cấp email. |
| Cổng chạy trực tiếp | `http://localhost:8085` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8085/v3/api-docs` |
| Swagger UI | `http://localhost:8085/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/v1/notifications/**`<br>`/api/v1/admin/notification-templates/**`<br>`/api/v1/admin/notifications/**`<br>`/api/v1/admin/notification-settings/**` |
| Quy mô hiện tại | 5 controller, 35 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/notification-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Hộp thư /api/v1/notifications/** yêu cầu người dùng đăng nhập.
- API quản trị mẫu và vận hành yêu cầu vai trò quản trị; một số thao tác đọc hoặc gửi thử mẫu cho phép quản lý.
- API /api/v1/internal/** gọi trực tiếp service và yêu cầu X-Internal-Token.

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

### Nhóm `AdminEmailProviderConfigurationController`

Đường dẫn gốc: `/api/v1/admin/notification-settings/email-provider`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/v1/admin/notification-settings/email-provider` | `status` | Vai trò `ADMIN` |
| PUT | `/api/v1/admin/notification-settings/email-provider` | `update` | Vai trò `ADMIN` |
| POST | `/api/v1/admin/notification-settings/email-provider/test` | `test` | Vai trò `ADMIN` |

### Nhóm `AdminNotificationOperationsController`

Đường dẫn gốc: `/api/v1/admin/notifications`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/v1/admin/notifications` | `requests` | Đã đăng nhập |
| GET | `/api/v1/admin/notifications/{publicId}` | `request` | Đã đăng nhập |
| GET | `/api/v1/admin/notifications/dashboard` | `dashboard` | Đã đăng nhập |
| GET | `/api/v1/admin/notifications/dead-letters` | `deadLetters` | Đã đăng nhập |
| POST | `/api/v1/admin/notifications/deliveries/{deliveryPublicId}/retry` | `retry` | Đã đăng nhập |

### Nhóm `AdminTemplateController`

Đường dẫn gốc: `/api/v1/admin/notification-templates`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/v1/admin/notification-templates` | `list` | Đã đăng nhập |
| GET | `/api/v1/admin/notification-templates/{templateKey}` | `published` | Đã đăng nhập |
| POST | `/api/v1/admin/notification-templates/{templateKey}/archive` | `archive` | Vai trò `ADMIN` |
| DELETE | `/api/v1/admin/notification-templates/{templateKey}/drafts/{draftId}` | `deleteDraft` | Vai trò `ADMIN` |
| GET | `/api/v1/admin/notification-templates/{templateKey}/drafts/{draftId}` | `getDraft` | Đã đăng nhập |
| PUT | `/api/v1/admin/notification-templates/{templateKey}/drafts/{draftId}` | `updateDraft` | Vai trò `ADMIN` |
| POST | `/api/v1/admin/notification-templates/{templateKey}/preview` | `preview` | Đã đăng nhập |
| POST | `/api/v1/admin/notification-templates/{templateKey}/preview-published` | `previewPublished` | Đã đăng nhập |
| POST | `/api/v1/admin/notification-templates/{templateKey}/publish` | `publish` | Vai trò `ADMIN` |
| POST | `/api/v1/admin/notification-templates/{templateKey}/restore` | `restore` | Vai trò `ADMIN` |
| POST | `/api/v1/admin/notification-templates/{templateKey}/rollback` | `rollback` | Vai trò `ADMIN` |
| POST | `/api/v1/admin/notification-templates/{templateKey}/test-send` | `testSend` | Một trong các vai trò: `ADMIN`, `MANAGER` |
| POST | `/api/v1/admin/notification-templates/{templateKey}/validate` | `validate` | Đã đăng nhập |
| GET | `/api/v1/admin/notification-templates/{templateKey}/versions` | `versions` | Đã đăng nhập |
| GET | `/api/v1/admin/notification-templates/{templateKey}/versions/{version}` | `version` | Đã đăng nhập |
| GET | `/api/v1/admin/notification-templates/{templateKey}/versions/diff` | `diff` | Đã đăng nhập |
| GET | `/api/v1/admin/notification-templates/coverage` | `coverage` | Đã đăng nhập |
| POST | `/api/v1/admin/notification-templates/drafts` | `createDraft` | Vai trò `ADMIN` |

### Nhóm `InAppNotificationController`

Đường dẫn gốc: `/api/v1/notifications`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/v1/notifications` | `list` | Đã đăng nhập |
| PATCH | `/api/v1/notifications/{publicId}/read` | `markRead` | Đã đăng nhập |
| PATCH | `/api/v1/notifications/read-all` | `markAllRead` | Đã đăng nhập |
| GET | `/api/v1/notifications/unread-count` | `unread` | Đã đăng nhập |

### Nhóm `InternalNotificationController`

Đường dẫn gốc: `/api/v1/internal/notifications`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/v1/internal/notifications` | `create` | Nội bộ (token service) |
| GET | `/api/v1/internal/notifications/{publicId}` | `get` | Nội bộ (token service) |
| POST | `/api/v1/internal/notifications/{publicId}/cancel` | `cancel` | Nội bộ (token service) |
| GET | `/api/v1/internal/notifications/{publicId}/deliveries` | `deliveries` | Nội bộ (token service) |
| POST | `/api/v1/internal/notifications/batch` | `batch` | Nội bộ (token service) |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
