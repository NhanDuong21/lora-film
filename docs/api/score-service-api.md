# API dịch vụ Điểm và hạng thành viên

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Tra cứu số dư điểm, lịch sử điểm, cộng, trừ, hoàn điểm và quản lý hạng thành viên. |
| Cổng chạy trực tiếp | `http://localhost:8088` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8088/v3/api-docs` |
| Swagger UI | `http://localhost:8088/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/scores/**`<br>`/api/admin/scores/**`<br>`/api/membership-tiers/**`<br>`/api/admin/membership-tiers/**` |
| Quy mô hiện tại | 6 controller, 33 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/score-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- GET /api/membership-tiers là công khai; dữ liệu điểm cá nhân yêu cầu đăng nhập.
- API quản trị yêu cầu quyền đọc, điều chỉnh, xuất dữ liệu hoặc quản lý hạng tương ứng.
- API /internal/** gọi trực tiếp service và yêu cầu X-Internal-Token.

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

### Nhóm `AdminMembershipTierController`

Đường dẫn gốc: `/api/admin/membership-tiers`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/membership-tiers` | `getTiers` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/membership-tiers` | `createTier` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/membership-tiers/{tierId}` | `getTierDetail` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/membership-tiers/{tierId}` | `updateTier` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminScoreController`

Đường dẫn gốc: `/api/admin/scores/users`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/scores/users` | `getUserScores` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/users/{userId}` | `getUserScoreDetail` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/scores/users/{userId}/adjustments` | `adjustUserScore` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/scores/users/{userId}/adjustments/reverse` | `reverseUserAdjustment` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/users/{userId}/expiring` | `getUserExpiringPoints` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/users/{userId}/history` | `getUserHistory` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/scores/users/{userId}/recalculate-tier` | `recalculateTier` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/scores/users/{userId}/status` | `updateScoreAccountStatus` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/users/{userId}/tier-history` | `getUserTierHistory` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminScoreOperationController`

Đường dẫn gốc: `/api/admin/scores`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/scores/audit` | `getAuditLogs` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/dashboard` | `getDashboardStats` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/export` | `exportData` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/scores/reconciliation` | `runReconciliation` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/reconciliation/details` | `getReconciliationDetails` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/scores/reconciliation/runs` | `getReconciliationRuns` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `InternalScoreController`

Đường dẫn gốc: `/internal/scores`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| POST | `/internal/scores/commit` | `commitPoints` | Nội bộ (token service) |
| POST | `/internal/scores/earn` | `earnPoints` | Nội bộ (token service) |
| POST | `/internal/scores/hold` | `holdPoints` | Nội bộ (token service) |
| POST | `/internal/scores/redeem` | `redeemPoints` | Nội bộ (token service) |
| POST | `/internal/scores/refund-redeem` | `refundRedeem` | Nội bộ (token service) |
| POST | `/internal/scores/release` | `releasePoints` | Nội bộ (token service) |
| POST | `/internal/scores/revoke-earn` | `revokeEarn` | Nội bộ (token service) |
| GET | `/internal/scores/users/{userId}` | `getUserScore` | Nội bộ (token service) |

### Nhóm `MembershipTierController`

Đường dẫn gốc: `/api/membership-tiers`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/membership-tiers` | `getMembershipTiers` | Công khai |

### Nhóm `ScoreController`

Đường dẫn gốc: `/api/scores/me`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/scores/me` | `getScoreBalance` | Đã đăng nhập |
| GET | `/api/scores/me/expiring` | `getExpiringPoints` | Đã đăng nhập |
| GET | `/api/scores/me/history` | `getScoreHistory` | Đã đăng nhập |
| POST | `/api/scores/me/redeem-preview` | `previewRedeem` | Đã đăng nhập |
| GET | `/api/scores/me/tier-history` | `getTierHistory` | Đã đăng nhập |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
