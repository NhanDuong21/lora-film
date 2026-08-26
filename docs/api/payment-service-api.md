# API dịch vụ Thanh toán và hoàn tiền

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Khởi tạo thanh toán, nhận callback nhà cung cấp, thu tiền mặt, hoàn tiền, đối soát và nghiệp vụ kế toán. |
| Cổng chạy trực tiếp | `http://localhost:8084` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8084/v3/api-docs` |
| Swagger UI | `http://localhost:8084/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/payments/**`<br>`/api/employee/payments/**`<br>`/api/admin/payments/**`<br>`/api/manager/payments/**`<br>`/api/vnpay/**` |
| Quy mô hiện tại | 10 controller, 68 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/payment-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Callback và return của nhà cung cấp là công khai; API thanh toán của khách hàng yêu cầu đăng nhập.
- Nhóm quản trị, quản lý, nhân viên và kế toán được kiểm tra bằng vai trò hoặc quyền trong SecurityConfig.
- API /internal/payments/** gọi trực tiếp service và yêu cầu X-Internal-Token.
- MockCallbackController chỉ hoạt động khi payment.providers.mock.enabled=true.

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

### Nhóm `AccountingOperationsController`

Đường dẫn gốc: `/api/admin/payments/accounting`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/payments/accounting/audit-events` | `auditEvents` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/accounting/cash-sessions` | `cashSessions` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/cash-sessions/{publicId}/verify` | `verifyCashSession` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/accounting/overview` | `overview` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/accounting/periods` | `periods` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/periods` | `createPeriod` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/periods/{publicId}/actions` | `periodAction` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/refunds/{paymentPublicId}/requests` | `requestRefund` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/refunds/{refundPublicId}/approve` | `approveRefund` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/refunds/{refundPublicId}/reject` | `rejectRefund` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/accounting/settlements` | `settlements` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/settlements` | `importSettlement` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/accounting/settlements/{publicId}` | `settlement` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/accounting/settlements/{publicId}/lock` | `lockSettlement` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminPaymentController`

Đường dẫn gốc: `/api/admin/payments`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/payments` | `search` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/{paymentPublicId}` | `detail` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/{paymentPublicId}/refunds` | `createRefund` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/export` | `export` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/outbox` | `outbox` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/outbox/{eventId}/replay` | `replayOutbox` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/reconciliations` | `reconciliations` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/reconciliations/{publicId}/assign` | `assign` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/reconciliations/{publicId}/resolve` | `resolve` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/refunds` | `refunds` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/refunds/{refundPublicId}` | `refundDetail` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/refunds/{refundPublicId}/cash/complete` | `completeCashRefund` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/refunds/{refundPublicId}/retry` | `retryRefund` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/payments/webhooks` | `webhooks` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/payments/webhooks/{webhookId}/replay` | `replayWebhook` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `EmployeePaymentController`

Đường dẫn gốc: `/api/employee/payments`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/employee/payments/{paymentId:\\d+}/cash/cancel` | `cancelCompat` | Nhân viên |
| POST | `/api/employee/payments/{paymentId:\\d+}/cash/collect` | `collectCompat` | Nhân viên |
| GET | `/api/employee/payments/{paymentPublicId:[a-fA-F0-9-]{36}}` | `getPayment` | Nhân viên |
| POST | `/api/employee/payments/{paymentPublicId:[a-fA-F0-9-]{36}}/cash/cancel` | `cancelCash` | Nhân viên |
| POST | `/api/employee/payments/{paymentPublicId:[a-fA-F0-9-]{36}}/cash/collect` | `collectCash` | Nhân viên |
| POST | `/api/employee/payments/{paymentPublicId:[a-fA-F0-9-]{36}}/refund-requests` | `createRefundRequest` | Nhân viên |
| GET | `/api/employee/payments/booking` | `lookupBooking` | Nhân viên |
| POST | `/api/employee/payments/cash` | `createCash` | Nhân viên |
| POST | `/api/employee/payments/counter-sessions` | `openCounterSession` | Nhân viên |
| POST | `/api/employee/payments/counter-sessions/{sessionPublicId:[a-fA-F0-9-]{36}}/close` | `closeCounterSession` | Nhân viên |
| GET | `/api/employee/payments/counter-sessions/current` | `currentCounterSession` | Nhân viên |
| GET | `/api/employee/payments/counter-sessions/history` | `counterSessionHistory` | Nhân viên |
| GET | `/api/employee/payments/refund-candidate` | `refundCandidate` | Nhân viên |
| POST | `/api/employee/payments/refund-requests/{refundPublicId}/cash/complete` | `completeCashRefund` | Nhân viên |
| GET | `/api/employee/payments/refund-requests/cash-pending` | `cashRefundsPendingAtCounter` | Nhân viên |

### Nhóm `HealthController`

Đường dẫn gốc: `/`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/health` | `health` | Công khai |

### Nhóm `InternalEmergencyPaymentController`

Đường dẫn gốc: `/internal/payments/emergency`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/payments/emergency/assess` | `assessPayments` | Nội bộ (token service) |
| POST | `/internal/payments/emergency/stop` | `stopPendingPayments` | Nội bộ (token service) |

### Nhóm `InternalRefundController`

Đường dẫn gốc: `/internal/payments/refunds`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/payments/refunds/showtimes/{showtimePublicId}` | `refundCancelledShowtime` | Nội bộ (token service) |

### Nhóm `ManagerPaymentController`

Đường dẫn gốc: `/api/manager/payments`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/manager/payments` | `search` | Vai trò `MANAGER` |
| GET | `/api/manager/payments/{paymentPublicId}` | `detail` | Vai trò `MANAGER` |
| GET | `/api/manager/payments/refund-requests` | `refundRequests` | Vai trò `MANAGER` |
| POST | `/api/manager/payments/refund-requests/{refundPublicId}/approve` | `approve` | Vai trò `MANAGER` |
| POST | `/api/manager/payments/refund-requests/{refundPublicId}/reject` | `reject` | Vai trò `MANAGER` |
| GET | `/api/manager/payments/summary` | `summary` | Vai trò `MANAGER` |

### Nhóm `MockCallbackController`

Đường dẫn gốc: `/api/payments/mock`.

> Nhóm này chỉ hoạt động khi `payment.providers.mock.enabled=true`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/payments/mock/{paymentPublicId}/complete` | `complete` | Đã đăng nhập |

### Nhóm `PaymentController`

Đường dẫn gốc: `/api/payments`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/payments` | `createPayment` | Đã đăng nhập |
| GET | `/api/payments/{paymentId:\\d+}` | `getPaymentCompat` | Đã đăng nhập |
| POST | `/api/payments/{paymentId:\\d+}/cancel` | `cancelCompat` | Đã đăng nhập |
| GET | `/api/payments/{paymentId:\\d+}/status` | `getPaymentStatusCompat` | Đã đăng nhập |
| GET | `/api/payments/{paymentPublicId:[a-fA-F0-9-]{36}}` | `getPayment` | Đã đăng nhập |
| POST | `/api/payments/{paymentPublicId:[a-fA-F0-9-]{36}}/cancel` | `cancelPayment` | Đã đăng nhập |
| GET | `/api/payments/{paymentPublicId:[a-fA-F0-9-]{36}}/status` | `getPaymentStatus` | Đã đăng nhập |
| GET | `/api/payments/booking/{bookingId:\\d+}` | `getByBookingCompat` | Đã đăng nhập |
| GET | `/api/payments/booking/{bookingPublicId:[a-fA-F0-9-]{36}}` | `getPaymentsByBooking` | Đã đăng nhập |

### Nhóm `ProviderPaymentController`

Đường dẫn gốc: `/api/payments`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/payments/callback/momo` | `momoIpn` | Công khai |
| GET | `/api/payments/callback/vnpay` | `vnpayIpn` | Công khai |
| GET | `/api/payments/return/momo` | `momoReturn` | Công khai |
| GET | `/api/payments/return/vnpay` | `vnpayReturn` | Công khai |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
