# API dịch vụ Xác thực và phân quyền

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Đăng ký, đăng nhập, OTP, quản lý phiên, tài khoản, vai trò, quyền và hồ sơ truy cập. |
| Cổng chạy trực tiếp | `http://localhost:8081` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs` |
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/auth/**`<br>`/oauth2/**`<br>`/login/oauth2/**`<br>`/api/roles/**`<br>`/api/permissions/**`<br>`/api/access-profiles/**`<br>`/api/accounts/**`<br>`/api/audits/**` |
| Quy mô hiện tại | 7 controller, 43 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/auth-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Đăng ký, đăng nhập, kiểm tra số định danh, OTP, làm mới token và khôi phục mật khẩu là các luồng công khai được liệt kê trong bảng endpoint.
- Các API còn lại yêu cầu Bearer token; API quản trị còn kiểm tra vai trò hoặc quyền tại controller.

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

### Nhóm `AccessProfileController`

Đường dẫn gốc: `/api/access-profiles`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/access-profiles` | `getAllProfiles` | Điều kiện trong code: `hasRole('ADMIN') or hasAnyAuthority('ROLE_VIEW', 'PERMISSION_VIEW', 'SYSTEM_CONFIGURATION')` |
| PUT | `/api/access-profiles/{id}/permissions` | `updatePermissions` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')` |

### Nhóm `AccountController`

Đường dẫn gốc: `/api/accounts`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/accounts` | `getAccounts` | Điều kiện trong code: `hasRole('ADMIN') or hasAnyAuthority('SYSTEM_CONFIGURATION', 'EMPLOYEE_CREATE')` |
| GET | `/api/accounts/{id}` | `getAccountById` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| PUT | `/api/accounts/{id}/access-profile` | `updateAccessProfile` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| PUT | `/api/accounts/{id}/cinema-assignments` | `updateManagerCinemaAssignments` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| POST | `/api/accounts/{id}/password-reset` | `sendPasswordReset` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| POST | `/api/accounts/{id}/resend-invitation` | `resendInvitation` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')` |
| POST | `/api/accounts/{id}/revoke-sessions` | `revokeSessions` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| PUT | `/api/accounts/{id}/role` | `updateRole` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| PUT | `/api/accounts/{id}/status` | `updateStatus` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| POST | `/api/accounts/employee` | `createEmployeeAccount` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')` |

### Nhóm `AuditController`

Đường dẫn gốc: `/api/audits`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/audits` | `getAuditLogs` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| PUT | `/api/audits/{id}/review` | `review` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |

### Nhóm `AuthController`

Đường dẫn gốc: `/api/auth`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/auth/change-email/request` | `requestChangeEmail` | Đã đăng nhập |
| POST | `/api/auth/change-email/verify` | `verifyChangeEmail` | Đã đăng nhập |
| POST | `/api/auth/change-password` | `changePassword` | Đã đăng nhập |
| POST | `/api/auth/forgot-password` | `forgotPassword` | Công khai |
| POST | `/api/auth/identity-number/inspect` | `inspectIdentityNumber` | Công khai |
| POST | `/api/auth/login` | `login` | Công khai |
| POST | `/api/auth/logout` | `logout` | Đã đăng nhập |
| POST | `/api/auth/logout-all` | `logoutAll` | Đã đăng nhập |
| GET | `/api/auth/me` | `getMe` | Đã đăng nhập |
| POST | `/api/auth/refresh` | `refreshToken` | Công khai |
| POST | `/api/auth/refresh-token` | `refreshToken` | Công khai |
| POST | `/api/auth/register` | `register` | Công khai |
| POST | `/api/auth/reset-password` | `resetPassword` | Công khai |
| POST | `/api/auth/send-otp` | `sendOtp` | Công khai |
| POST | `/api/auth/verify` | `verify` | Công khai |
| POST | `/api/auth/verify-email` | `verify` | Công khai |

### Nhóm `PermissionController`

Đường dẫn gốc: `/api/permissions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/permissions` | `getAllPermissions` | Điều kiện trong code: `hasRole('ADMIN') or hasAnyAuthority('PERMISSION_VIEW', 'ROLE_CREATE', 'ROLE_UPDATE')` |
| POST | `/api/permissions` | `createPermission` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PERMISSION_CREATE')` |
| DELETE | `/api/permissions/{id}` | `deletePermission` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PERMISSION_DELETE')` |
| GET | `/api/permissions/{id}` | `getPermissionById` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PERMISSION_VIEW')` |
| PUT | `/api/permissions/{id}` | `updatePermission` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PERMISSION_UPDATE')` |

### Nhóm `RoleController`

Đường dẫn gốc: `/api/roles`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/roles` | `getAllRoles` | Điều kiện trong code: `hasRole('ADMIN') or hasAnyAuthority('ROLE_VIEW', 'SYSTEM_CONFIGURATION')` |
| POST | `/api/roles` | `createRole` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('ROLE_CREATE')` |
| DELETE | `/api/roles/{id}` | `deleteRole` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('ROLE_DELETE')` |
| GET | `/api/roles/{id}` | `getRoleById` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('ROLE_VIEW')` |
| PUT | `/api/roles/{id}` | `updateRole` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')` |

### Nhóm `SessionController`

Đường dẫn gốc: `/api/auth/sessions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| DELETE | `/api/auth/sessions` | `revokeAllSessions` | Đã đăng nhập |
| GET | `/api/auth/sessions` | `getSessions` | Đã đăng nhập |
| DELETE | `/api/auth/sessions/{id}` | `revokeSession` | Đã đăng nhập |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
