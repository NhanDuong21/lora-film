# API dịch vụ Người dùng và nhân sự

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Quản lý hồ sơ người dùng, khách hàng, nhân viên, phòng ban, chức vụ, chấm công, lương và kiểm soát dữ liệu cá nhân. |
| Cổng chạy trực tiếp | `http://localhost:8086` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8086/v3/api-docs` |
| Swagger UI | `http://localhost:8086/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/users/**`<br>`/api/admin/user-audits` |
| Quy mô hiện tại | 15 controller, 80 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/user-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Ảnh đại diện dưới /api/users/profile/avatar/files/** có thể đọc công khai; các API người dùng khác yêu cầu đăng nhập.
- Quyền quản trị, quản lý nhân sự và quản trị dữ liệu cá nhân được kiểm tra tại controller.
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

### Nhóm `CustomerController`

Đường dẫn gốc: `/api/users/customers`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/customers` | `search` | Một trong các vai trò: `ADMIN`, `MANAGER`, `CUSTOMER_VIEW` |
| GET | `/api/users/customers/{id}` | `get` | Một trong các vai trò: `ADMIN`, `MANAGER`, `CUSTOMER_VIEW` |
| POST | `/api/users/customers/{id}/access-actions` | `applyAccessAction` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('CUSTOMER_UPDATE')` |
| GET | `/api/users/customers/counter-search` | `counterSearch` | Điều kiện trong code: `hasAuthority('BOOKING_MANAGE') or hasAnyRole('ADMIN','MANAGER')` |

### Nhóm `DashboardController`

Đường dẫn gốc: `/api/users/dashboard`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/dashboard` | `summary` | Một trong các vai trò: `ADMIN`, `MANAGER`, `DASHBOARD_VIEW` |
| GET | `/api/users/dashboard/customers` | `domainSummary` | Một trong các vai trò: `ADMIN`, `MANAGER`, `DASHBOARD_VIEW` |
| GET | `/api/users/dashboard/employees` | `domainSummary` | Một trong các vai trò: `ADMIN`, `MANAGER`, `DASHBOARD_VIEW` |
| GET | `/api/users/dashboard/payrolls` | `domainSummary` | Một trong các vai trò: `ADMIN`, `MANAGER`, `DASHBOARD_VIEW` |

### Nhóm `DepartmentController`

Đường dẫn gốc: `/api/users/departments`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/departments` | `list` | Một trong các vai trò: `ADMIN`, `MANAGER`, `DEPARTMENT_VIEW`, `EMPLOYEE_VIEW` |
| POST | `/api/users/departments` | `create` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('DEPARTMENT_CREATE')` |
| DELETE | `/api/users/departments/{id}` | `delete` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('DEPARTMENT_DELETE')` |
| PUT | `/api/users/departments/{id}` | `update` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('DEPARTMENT_UPDATE')` |
| GET | `/api/users/departments/search` | `search` | Một trong các vai trò: `ADMIN`, `MANAGER`, `DEPARTMENT_VIEW`, `EMPLOYEE_VIEW` |

### Nhóm `EmployeeController`

Đường dẫn gốc: `/api/users/employees`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/employees` | `search` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_VIEW`, `PAYROLL_VIEW`, `PAYROLL_CREATE`, `PAYROLL_UPDATE` |
| POST | `/api/users/employees` | `create` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')` |
| GET | `/api/users/employees/{accountId}` | `get` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_VIEW` |
| GET | `/api/users/employees/{accountId}/actions` | `actionHistory` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_VIEW` |
| POST | `/api/users/employees/{accountId}/actions` | `applyAction` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')` |
| PUT | `/api/users/employees/{accountId}/cinema-assignment` | `assignCinema` | Vai trò `ADMIN` |
| GET | `/api/users/employees/eligible-accounts` | `eligibleAccounts` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')` |
| GET | `/api/users/employees/me` | `getMyWorkContext` | Đã đăng nhập |

### Nhóm `EmployeeDocumentController`

Đường dẫn gốc: `/api/users/employees/{accountId}/documents`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/employees/{accountId}/documents` | `list` | Đã đăng nhập |
| POST | `/api/users/employees/{accountId}/documents` | `upload` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_UPDATE` |
| DELETE | `/api/users/employees/{accountId}/documents/{documentId}` | `delete` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_UPDATE` |
| GET | `/api/users/employees/{accountId}/documents/{documentId}/file` | `download` | Đã đăng nhập |
| GET | `/api/users/employees/{accountId}/documents/history` | `history` | Đã đăng nhập |

### Nhóm `HealthController`

Đường dẫn gốc: `/`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/health` | `health` | Công khai |

### Nhóm `InternalEmployeeScopeController`

Đường dẫn gốc: `/api/v1/internal/employees`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/v1/internal/employees/{accountId}/cinema-scope` | `cinemaScope` | Nội bộ (token service) |
| POST | `/api/v1/internal/employees/directory` | `directory` | Nội bộ (token service) |

### Nhóm `InternalUserController`

Đường dẫn gốc: `/api/v1/internal/users`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/v1/internal/users/{accountId}/notification-recipient` | `notificationRecipient` | Nội bộ (token service) |
| GET | `/api/v1/internal/users/birthday-eligible` | `birthdayEligible` | Nội bộ (token service) |
| POST | `/api/v1/internal/users/validate-active` | `validateActiveUsers` | Nội bộ (token service) |

### Nhóm `ManagerWorkforceController`

Đường dẫn gốc: `/api/users/manager`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/manager/attendance` | `attendance` | Vai trò `MANAGER` |
| GET | `/api/users/manager/leave-requests` | `leaves` | Vai trò `MANAGER` |
| POST | `/api/users/manager/leave-requests/{leaveId}/actions` | `leaveAction` | Vai trò `MANAGER` |
| GET | `/api/users/manager/shifts` | `shifts` | Vai trò `MANAGER` |
| POST | `/api/users/manager/shifts` | `createShift` | Vai trò `MANAGER` |
| POST | `/api/users/manager/shifts/{shiftId}/cancel` | `cancelShift` | Vai trò `MANAGER` |
| GET | `/api/users/manager/staff` | `staff` | Vai trò `MANAGER` |

### Nhóm `PayrollController`

Đường dẫn gốc: `/api/users/payrolls`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/payrolls` | `search` | Một trong các vai trò: `ADMIN`, `MANAGER`, `PAYROLL_VIEW` |
| POST | `/api/users/payrolls` | `create` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PAYROLL_CREATE')` |
| GET | `/api/users/payrolls/{id}` | `get` | Một trong các vai trò: `ADMIN`, `MANAGER`, `PAYROLL_VIEW` |
| PUT | `/api/users/payrolls/{id}` | `update` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PAYROLL_UPDATE')` |
| POST | `/api/users/payrolls/{id}/actions` | `action` | Đã đăng nhập |
| POST | `/api/users/payrolls/generate` | `generate` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('PAYROLL_CREATE')` |
| GET | `/api/users/payrolls/me` | `mine` | Điều kiện trong code: `hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('EMPLOYEE_PAYROLL_VIEW'))` |
| GET | `/api/users/payrolls/summary` | `summary` | Một trong các vai trò: `ADMIN`, `MANAGER`, `PAYROLL_VIEW` |

### Nhóm `PiiGovernanceController`

Đường dẫn gốc: `/api/users/pii-governance`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/users/pii-governance/erase-due` | `eraseDue` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| GET | `/api/users/pii-governance/summary` | `summary` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |
| POST | `/api/users/pii-governance/users/{accountId}/retention` | `scheduleRetention` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')` |

### Nhóm `PositionController`

Đường dẫn gốc: `/api/users/positions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/positions` | `list` | Một trong các vai trò: `ADMIN`, `MANAGER`, `POSITION_VIEW`, `EMPLOYEE_VIEW` |
| POST | `/api/users/positions` | `create` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('POSITION_CREATE')` |
| DELETE | `/api/users/positions/{id}` | `delete` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('POSITION_DELETE')` |
| PUT | `/api/users/positions/{id}` | `update` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('POSITION_UPDATE')` |
| GET | `/api/users/positions/search` | `search` | Một trong các vai trò: `ADMIN`, `MANAGER`, `POSITION_VIEW`, `EMPLOYEE_VIEW` |

### Nhóm `UserAuditController`

Đường dẫn gốc: `/api/admin/user-audits`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/user-audits` | `search` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/user-audits/{id}/review` | `review` | Chỉ gọi trực tiếp; cần `ADMIN` hoặc `SYSTEM_CONFIGURATION` |

### Nhóm `UserController`

Đường dẫn gốc: `/api/users`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/{accountId}` | `getUserProfile` | Đã đăng nhập |
| GET | `/api/users/admin/batch` | `getUserProfiles` | Đã đăng nhập |
| GET | `/api/users/admin/search` | `searchUserProfiles` | Đã đăng nhập |
| GET | `/api/users/directory/display-names` | `getAccountDisplayNames` | Một trong các vai trò: `ADMIN`, `MANAGER` |
| GET | `/api/users/profile` | `getOwnProfile` | Đã đăng nhập |
| PUT | `/api/users/profile` | `updateOwnProfile` | Đã đăng nhập |
| DELETE | `/api/users/profile/avatar` | `deleteAvatar` | Đã đăng nhập |
| POST | `/api/users/profile/avatar` | `uploadAvatar` | Đã đăng nhập |
| GET | `/api/users/profile/avatar/files/{fileName:.+}` | `getAvatarFile` | Công khai |

### Nhóm `WorkforceTimeController`

Đường dẫn gốc: `/api/users/workforce`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/users/workforce/attendance` | `attendance` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_VIEW` |
| POST | `/api/users/workforce/attendance/{shiftId}/correction` | `correct` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')` |
| POST | `/api/users/workforce/attendance/check-in` | `checkIn` | Điều kiện trong code: `hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('EMPLOYEE_ATTENDANCE_UPDATE'))` |
| POST | `/api/users/workforce/attendance/check-out` | `checkOut` | Điều kiện trong code: `hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('EMPLOYEE_ATTENDANCE_UPDATE'))` |
| GET | `/api/users/workforce/attendance/me` | `myAttendance` | Điều kiện trong code: `hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('EMPLOYEE_ATTENDANCE_VIEW'))` |
| GET | `/api/users/workforce/leave-requests` | `leaves` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_VIEW` |
| POST | `/api/users/workforce/leave-requests` | `createLeave` | Điều kiện trong code: `hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('EMPLOYEE_LEAVE_CREATE'))` |
| POST | `/api/users/workforce/leave-requests/{id}/actions` | `leaveAction` | Đã đăng nhập |
| GET | `/api/users/workforce/leave-requests/me` | `myLeaves` | Điều kiện trong code: `hasRole('ADMIN') or (hasRole('EMPLOYEE') and hasAuthority('EMPLOYEE_SCHEDULE_VIEW'))` |
| GET | `/api/users/workforce/shifts` | `shifts` | Một trong các vai trò: `ADMIN`, `MANAGER`, `EMPLOYEE_VIEW` |
| POST | `/api/users/workforce/shifts` | `createShift` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')` |
| POST | `/api/users/workforce/shifts/{id}/cancel` | `cancelShift` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')` |
| POST | `/api/users/workforce/shifts/batch` | `createShiftBatch` | Điều kiện trong code: `hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')` |
| GET | `/api/users/workforce/shifts/me` | `myShifts` | Đã đăng nhập |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
