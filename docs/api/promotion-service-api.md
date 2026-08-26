# API dịch vụ Khuyến mãi

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Quản lý chiến dịch, mã giảm giá, voucher, quyền lợi khách hàng, giữ ưu đãi và tự động hóa khuyến mãi. |
| Cổng chạy trực tiếp | `http://localhost:8087` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8087/v3/api-docs` |
| Swagger UI | `http://localhost:8087/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/promotions/**`<br>`/api/customers/me/**`<br>`/api/manager/promotions/**`<br>`/api/admin/**` |
| Quy mô hiện tại | 18 controller, 92 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/promotion-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Danh mục dưới /api/promotions/public, /api/promotions/offers và /api/promotions/assets/** cho phép GET công khai.
- API khách hàng còn lại yêu cầu đăng nhập; API quản trị dùng quyền chi tiết ghi tại từng endpoint.
- API /internal/** gọi trực tiếp service, yêu cầu đúng một X-Service-Name và một X-Internal-Token.
- Các thao tác giữ, xác nhận hoặc giải phóng ưu đãi còn yêu cầu X-Idempotency-Key.

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

### Nhóm `AdminCampaignController`

Đường dẫn gốc: `/api/admin/promotion-campaigns`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/promotion-campaigns` | `searchCampaigns` | Quyền `PROMOTION_VIEW` |
| POST | `/api/admin/promotion-campaigns` | `createCampaign` | Quyền `PROMOTION_AUTHOR` |
| DELETE | `/api/admin/promotion-campaigns/{id}` | `deleteCampaign` | Quyền `PROMOTION_AUTHOR` |
| GET | `/api/admin/promotion-campaigns/{id}` | `getCampaign` | Quyền `PROMOTION_VIEW` |
| PUT | `/api/admin/promotion-campaigns/{id}` | `updateCampaign` | Quyền `PROMOTION_AUTHOR` |
| GET | `/api/admin/promotion-campaigns/{id}/approval-history` | `getApprovalHistory` | Quyền `PROMOTION_AUDIT_VIEW` |
| POST | `/api/admin/promotion-campaigns/{id}/approve` | `approveCampaign` | Một trong các quyền: `PROMOTION_APPROVE_STANDARD`, `PROMOTION_APPROVE_HIGH_BUDGET` |
| POST | `/api/admin/promotion-campaigns/{id}/legal-review` | `reviewLegalStatus` | Quyền `PROMOTION_LEGAL_REVIEW` |
| POST | `/api/admin/promotion-campaigns/{id}/override-approval` | `overrideApproval` | Quyền `PROMOTION_OVERRIDE` |
| POST | `/api/admin/promotion-campaigns/{id}/reject` | `rejectCampaign` | Một trong các quyền: `PROMOTION_APPROVE_STANDARD`, `PROMOTION_APPROVE_HIGH_BUDGET` |
| PATCH | `/api/admin/promotion-campaigns/{id}/status` | `transitionCampaignStatus` | Một trong các quyền: `PROMOTION_AUTHOR`, `PROMOTION_PUBLISH`, `PROMOTION_OPERATE`, `PROMOTION_EMERGENCY_STOP` |

### Nhóm `AdminCampaignEmergencyController`

Đường dẫn gốc: `/api/admin/promotion-campaigns`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/api/admin/promotion-campaigns/{id}/force-release` | `forceRelease` | Quyền `PROMOTION_FORCE_RELEASE` |
| GET | `/api/admin/promotion-campaigns/{id}/force-release-impact` | `impact` | Một trong các quyền: `PROMOTION_EMERGENCY_STOP`, `PROMOTION_FORCE_RELEASE` |

### Nhóm `AdminCampaignPresentationController`

Đường dẫn gốc: `/api/admin/promotion-campaigns/{campaignId}/presentation`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/promotion-campaigns/{campaignId}/presentation` | `get` | Quyền `PROMOTION_VIEW` |
| PUT | `/api/admin/promotion-campaigns/{campaignId}/presentation` | `update` | Quyền `PROMOTION_AUTHOR` |
| DELETE | `/api/admin/promotion-campaigns/{campaignId}/presentation/cover` | `removeCover` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotion-campaigns/{campaignId}/presentation/cover` | `uploadCover` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotion-campaigns/{campaignId}/presentation/publish` | `publish` | Quyền `PROMOTION_PUBLISH` |
| POST | `/api/admin/promotion-campaigns/{campaignId}/presentation/unpublish` | `unpublish` | Quyền `PROMOTION_PUBLISH` |

### Nhóm `AdminConfigurationController`

Đường dẫn gốc: `/api/admin/configurations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/configurations` | `search` | Một trong các vai trò: `ADMIN`, `CONFIGURATION_MANAGER` |
| POST | `/api/admin/configurations` | `create` | Một trong các vai trò: `ADMIN`, `CONFIGURATION_MANAGER` |
| DELETE | `/api/admin/configurations/{id}` | `delete` | Một trong các vai trò: `ADMIN`, `CONFIGURATION_MANAGER` |
| GET | `/api/admin/configurations/{id}` | `detail` | Một trong các vai trò: `ADMIN`, `CONFIGURATION_MANAGER` |
| PUT | `/api/admin/configurations/{id}` | `update` | Một trong các vai trò: `ADMIN`, `CONFIGURATION_MANAGER` |

### Nhóm `AdminEventController`

Đường dẫn gốc: `/api/admin/events`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/events` | `history` | Một trong các vai trò: `ADMIN`, `OPERATIONS_MANAGER`, `FINANCE_DIRECTOR` |
| GET | `/api/admin/events/{id}` | `detail` | Một trong các vai trò: `ADMIN`, `OPERATIONS_MANAGER`, `FINANCE_DIRECTOR` |
| GET | `/api/admin/events/jobs` | `jobs` | Một trong các vai trò: `ADMIN`, `OPERATIONS_MANAGER` |
| POST | `/api/admin/events/jobs/{jobName}/run` | `run` | Vai trò `OPERATIONS_MANAGER` |

### Nhóm `AdminPromotionAutomationController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/promotion-anomaly-cases` | `anomalyCases` | Quyền `PROMOTION_AUDIT_VIEW` |
| POST | `/api/admin/promotion-anomaly-cases/{id}/assign` | `assignAnomaly` | Quyền `PROMOTION_OPERATE` |
| POST | `/api/admin/promotion-anomaly-cases/{id}/resolve` | `resolveAnomaly` | Quyền `PROMOTION_OPERATE` |
| GET | `/api/admin/promotion-opportunities` | `opportunities` | Quyền `PROMOTION_VIEW` |
| GET | `/api/admin/promotion-playbooks` | `playbooks` | Quyền `PROMOTION_VIEW` |
| POST | `/api/admin/promotion-playbooks` | `create` | Quyền `PROMOTION_AUTHOR` |
| PUT | `/api/admin/promotion-playbooks/{id}` | `update` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotion-playbooks/{id}/approve` | `approve` | Một trong các quyền: `PROMOTION_APPROVE_STANDARD`, `PROMOTION_APPROVE_HIGH_BUDGET` |
| POST | `/api/admin/promotion-playbooks/{id}/pause` | `pause` | Quyền `PROMOTION_OPERATE` |
| POST | `/api/admin/promotion-playbooks/{id}/run` | `runNow` | Quyền `PROMOTION_OPERATE` |
| POST | `/api/admin/promotion-playbooks/{id}/submit` | `submit` | Quyền `PROMOTION_AUTHOR` |
| GET | `/api/admin/promotion-runs` | `runs` | Quyền `PROMOTION_VIEW` |
| GET | `/api/admin/promotion-runs/{id}` | `run` | Quyền `PROMOTION_VIEW` |
| POST | `/api/admin/promotion-runs/{id}/issue-jobs` | `issue` | Quyền `PROMOTION_OPERATE` |

### Nhóm `AdminPromotionController`

Đường dẫn gốc: `/api/admin/promotions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/promotions` | `search` | Quyền `PROMOTION_VIEW` |
| POST | `/api/admin/promotions` | `create` | Quyền `PROMOTION_AUTHOR` |
| DELETE | `/api/admin/promotions/{id}` | `delete` | Quyền `PROMOTION_AUTHOR` |
| GET | `/api/admin/promotions/{id}` | `detail` | Quyền `PROMOTION_VIEW` |
| PUT | `/api/admin/promotions/{id}` | `update` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotions/{id}/activate` | `activate` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotions/{id}/clone` | `clonePromotion` | Quyền `PROMOTION_AUTHOR` |
| GET | `/api/admin/promotions/{id}/clone-draft` | `cloneDraft` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotions/{id}/issue` | `issue` | Quyền `PROMOTION_AUTHOR` |
| POST | `/api/admin/promotions/{id}/pause` | `pause` | Quyền `PROMOTION_OPERATE` |

### Nhóm `AdminPromotionMonitoringController`

Đường dẫn gốc: `/api/admin/promotion-monitoring`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/promotion-monitoring/summary` | `summary` | Quyền `PROMOTION_AUDIT_VIEW` |

### Nhóm `AdminPromotionOperationsController`

Đường dẫn gốc: `/api/admin/promotion-operations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/promotion-operations/search` | `search` | Quyền `PROMOTION_AUDIT_VIEW` |

### Nhóm `AdminReservationController`

Đường dẫn gốc: `/api/admin/reservations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/reservations` | `history` | Quyền `PROMOTION_AUDIT_VIEW` |

### Nhóm `CustomerPromotionController`

Đường dẫn gốc: `/api`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/customers/me/promotion-history` | `walletHistory` | Đã đăng nhập |
| GET | `/api/customers/me/promotions` | `wallet` | Đã đăng nhập |
| GET | `/api/customers/me/promotions/{id}` | `walletDetail` | Đã đăng nhập |
| POST | `/api/promotions/{id}/claim` | `claim` | Đã đăng nhập |
| GET | `/api/promotions/public` | `publicPromotions` | Công khai |
| GET | `/api/promotions/system` | `systemPromotions` | Đã đăng nhập |

### Nhóm `InternalConfigurationController`

Đường dẫn gốc: `/internal/configurations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/internal/configurations/{key}` | `get` | Vai trò `INTERNAL` |

### Nhóm `InternalEventController`

Đường dẫn gốc: `/internal/events`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/events/dlq/reprocess` | `reprocess` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/events/publish` | `publish` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/events/retry` | `retry` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/events/retry/{id}` | `retry` | Vai trò `OPERATIONS_SERVICE` |
| GET | `/internal/events/status` | `status` | Vai trò `OPERATIONS_SERVICE` |

### Nhóm `InternalPromotionReservationController`

Đường dẫn gốc: `/internal/reservations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/reservations` | `reserve` | Vai trò `BOOKING_SERVICE` |
| GET | `/internal/reservations/{reservationId}` | `getDetail` | Một trong các vai trò: `BOOKING_SERVICE`, `PAYMENT_SERVICE` |
| POST | `/internal/reservations/{reservationId}/cancel` | `cancel` | Vai trò `BOOKING_SERVICE` |
| POST | `/internal/reservations/{reservationId}/confirm` | `confirm` | Một trong các vai trò: `BOOKING_SERVICE`, `PAYMENT_SERVICE` |
| POST | `/internal/reservations/{reservationId}/refresh` | `refresh` | Vai trò `BOOKING_SERVICE` |
| POST | `/internal/reservations/{reservationId}/release` | `release` | Một trong các vai trò: `BOOKING_SERVICE`, `PAYMENT_SERVICE` |
| POST | `/internal/reservations/{reservationId}/reverse` | `reverse` | Một trong các vai trò: `BOOKING_SERVICE`, `PAYMENT_SERVICE` |

### Nhóm `InternalPromotionRuntimeController`

Đường dẫn gốc: `/internal/runtime`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/runtime/preview` | `preview` | Một trong các vai trò: `BOOKING_SERVICE`, `PAYMENT_SERVICE` |
| POST | `/internal/runtime/validate` | `preview` | Một trong các vai trò: `BOOKING_SERVICE`, `PAYMENT_SERVICE` |

### Nhóm `InternalSchedulerController`

Đường dẫn gốc: `/internal/schedulers`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/schedulers/cache/refresh` | `refreshCache` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/campaigns/activate` | `activateCampaigns` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/campaigns/expire` | `expireCampaigns` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/outbox/publish` | `publishOutbox` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/outbox/retry` | `retryOutbox` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/promotions/activate` | `activatePromotions` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/promotions/expire` | `expirePromotions` | Vai trò `OPERATIONS_SERVICE` |
| POST | `/internal/schedulers/wallet/expire` | `expireWallet` | Vai trò `OPERATIONS_SERVICE` |

### Nhóm `ManagerPromotionController`

Đường dẫn gốc: `/api/manager/promotions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/manager/promotions/automations` | `automations` | Điều kiện trong code: `hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW')` |
| GET | `/api/manager/promotions/campaigns` | `campaigns` | Điều kiện trong code: `hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW')` |
| GET | `/api/manager/promotions/distribution-options` | `distributionOptions` | Điều kiện trong code: `hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW')` |
| POST | `/api/manager/promotions/distribution-options/{promotionId}/issue` | `issue` | Điều kiện trong code: `hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW') and hasAuthority('PROMOTION_DISTRIBUTE_LOCAL')` |
| GET | `/api/manager/promotions/incidents` | `incidents` | Điều kiện trong code: `hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW') and hasAuthority('PROMOTION_AUDIT_VIEW')` |
| GET | `/api/manager/promotions/workspace` | `workspace` | Điều kiện trong code: `hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW')` |

### Nhóm `PublicPromotionOfferController`

Đường dẫn gốc: `/api/promotions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/promotions/assets/{storageKey}` | `asset` | Công khai |
| GET | `/api/promotions/offers` | `offers` | Công khai |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
