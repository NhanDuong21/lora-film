# API dịch vụ Đặt vé và giữ ghế

> Đã đồng bộ với controller và cấu hình bảo mật hiện tại ngày 26/08/2026. Tài liệu chỉ liệt kê chức năng đã có trong mã nguồn, không giữ kế hoạch sprint hoặc endpoint dự kiến.

## 1. Thông tin nhanh

| Nội dung | Giá trị |
|---|---|
| Mục đích | Quản lý giữ ghế, booking, vé, quầy soát vé, đồ ăn và các thao tác vận hành liên quan. |
| Cổng chạy trực tiếp | `http://localhost:8083` |
| Gọi từ frontend | `http://localhost:8080` qua API Gateway |
| OpenAPI JSON | `http://localhost:8083/v3/api-docs` |
| Swagger UI | `http://localhost:8083/swagger-ui.html` |
| Route được Gateway chuyển tiếp | `/api/bookings/**`<br>`/api/seat-reservations/**`<br>`/api/customer/concessions/**`<br>`/api/customer/cart/**`<br>`/api/admin/bookings/**`<br>`/api/admin/monitoring/**`<br>`/api/admin/foods/**`<br>`/api/manager/bookings/**`<br>`/api/manager/ticket-operations/**`<br>`/api/employee/ticket-operations/**` |
| Quy mô hiện tại | 14 controller, 74 endpoint (đã tính các đường dẫn bí danh) |

Nguồn kiểm chứng là controller dưới `server/booking-service/src/main/java/`, SecurityConfig của service và cấu hình route của API Gateway.

## 2. Cách đọc và gọi API

- Frontend chỉ gọi qua API Gateway. Đường dẫn trong bảng được giữ nguyên khi Gateway chuyển tiếp.
- Endpoint ghi **Công khai** không cần Bearer token. Endpoint còn lại phải gửi `Authorization: Bearer <access-token>` trừ nhóm nội bộ.
- Endpoint **Nội bộ** không được Gateway công khai; service khác gọi thẳng cổng đích và gửi header token đã cấu hình.
- Tên hàm xử lý được giữ nguyên như trong code để có thể tìm kiếm nhanh. Request body, query parameter, validation và schema response xem tại Swagger UI/OpenAPI đang chạy.
- `publicId`, `id` hoặc `accountId` phải dùng đúng loại định danh endpoint yêu cầu; không tự thay UUID bằng ID số nội bộ.

## 3. Quy tắc bảo mật hiện tại

- Danh sách đồ ăn có thể xem công khai. Kênh Socket.IO hiển thị tình trạng ghế cũng công khai, nhưng API HTTP `/api/seat-reservations/showtime/**` hiện vẫn cần JWT khi đi qua Gateway.
- Các nhóm /api/admin/**, /api/manager/** và /api/employee/** được giới hạn theo vai trò hoặc quyền tương ứng.
- API /internal/** gọi trực tiếp service, dùng X-Internal-Token; luồng tương thích cũng chấp nhận X-Service-Token.

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

### Nhóm `AdminBookingController`

Đường dẫn gốc: `/api/admin/bookings`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/bookings` | `getBookings` | Vai trò `ADMIN` |
| GET | `/api/admin/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}` | `getBookingDetail` | Vai trò `ADMIN` |
| PUT | `/api/admin/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/status` | `updateBookingStatus` | Vai trò `ADMIN` |
| GET | `/api/admin/bookings/summary` | `getOperationsSummary` | Vai trò `ADMIN` |

### Nhóm `AdminBookingFoodController`

Đường dẫn gốc: `/api/admin`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/bookings/{bookingId}/foods` | `getFoodOrder` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/foods` | `getAllFoods` | Quản trị (theo cấu hình bảo mật) |
| POST | `/api/admin/foods` | `addFood` | Quản trị (theo cấu hình bảo mật) |
| DELETE | `/api/admin/foods/{id}` | `deleteFood` | Quản trị (theo cấu hình bảo mật) |
| PUT | `/api/admin/foods/{id}` | `updateFood` | Quản trị (theo cấu hình bảo mật) |
| PATCH | `/api/admin/foods/{id}/restore` | `restoreFood` | Quản trị (theo cấu hình bảo mật) |
| GET | `/api/admin/foods/statistics` | `getFoodStatistics` | Quản trị (theo cấu hình bảo mật) |

### Nhóm `AdminMonitoringController`

Đường dẫn gốc: `/api/admin/monitoring`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/admin/monitoring/summary` | `getMonitoringSummary` | Một trong các vai trò: `ADMIN`, `OPERATIONS_MANAGER`, `FINANCE_DIRECTOR` |

### Nhóm `CustomerBookingController`

Đường dẫn gốc: `/api/bookings`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/bookings` | `getMyBookings` | Đã đăng nhập |
| POST | `/api/bookings` | `createBooking` | Đã đăng nhập |
| DELETE | `/api/bookings/{publicId}` | `cancelBooking` | Đã đăng nhập |
| GET | `/api/bookings/{publicId}` | `getBookingDetail` | Đã đăng nhập |
| POST | `/api/bookings/{publicId}/finalize-checkout` | `finalizeCheckout` | Đã đăng nhập |
| POST | `/api/bookings/{publicId}/payment` | `initiatePayment` | Đã đăng nhập |
| POST | `/api/bookings/{publicId}/promotions/preview` | `previewPromotions` | Đã đăng nhập |
| POST | `/api/bookings/{publicId}/resend-email` | `resendEmail` | Đã đăng nhập |
| GET | `/api/bookings/active` | `getActiveBooking` | Đã đăng nhập |
| GET | `/api/bookings/code/{bookingCode}` | `getBookingByCode` | Đã đăng nhập |
| GET | `/api/bookings/spending-summary` | `getMySpendingSummary` | Đã đăng nhập |

### Nhóm `CustomerBookingFoodController`

Đường dẫn gốc: `/api/bookings/{bookingId}/foods`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/bookings/{bookingId}/foods` | `getFoodOrder` | Đã đăng nhập |
| POST | `/api/bookings/{bookingId}/foods` | `addFoodItem` | Đã đăng nhập |
| DELETE | `/api/bookings/{bookingId}/foods/{foodItemId}` | `removeFoodItem` | Đã đăng nhập |
| PUT | `/api/bookings/{bookingId}/foods/{foodItemId}` | `updateFoodQuantity` | Đã đăng nhập |

### Nhóm `CustomerConcessionsController`

Đường dẫn gốc: `/api/customer/concessions`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/customer/concessions` | `getConcessions` | Công khai |

### Nhóm `CustomerFoodCartController`

Đường dẫn gốc: `/api/customer/cart`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/customer/cart` | `getCart` | Đã đăng nhập |
| POST | `/api/customer/cart/checkout` | `checkoutCart` | Đã đăng nhập |
| POST | `/api/customer/cart/items` | `addFoodToCart` | Đã đăng nhập |
| DELETE | `/api/customer/cart/items/{itemId}` | `removeFoodItem` | Đã đăng nhập |
| PUT | `/api/customer/cart/items/{itemId}` | `updateFoodQuantity` | Đã đăng nhập |
| POST | `/api/customer/cart/mock-pay` | `mockPay` | Đã đăng nhập |

### Nhóm `EmployeeTicketCheckerController`

Đường dẫn gốc: `/api/employee/ticket-operations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/employee/ticket-operations/handoffs` | `handoffs` | Quyền `TICKET_SCAN` |
| POST | `/api/employee/ticket-operations/handoffs` | `handoff` | Quyền `TICKET_SCAN` |
| GET | `/api/employee/ticket-operations/history` | `history` | Quyền `TICKET_SCAN` |
| POST | `/api/employee/ticket-operations/scan` | `scan` | Quyền `TICKET_SCAN` |
| GET | `/api/employee/ticket-operations/showtimes` | `showtimes` | Quyền `TICKET_SCAN` |
| GET | `/api/employee/ticket-operations/summary` | `summary` | Quyền `TICKET_SCAN` |

### Nhóm `InternalBookingController`

Đường dẫn gốc: `/internal/bookings`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/internal/bookings/{bookingId:\\d+}/payment-context` | `getPaymentContext` | Nội bộ (token service) |
| POST | `/internal/bookings/{bookingId:\\d+}/payment-results` | `recordPaymentResult` | Nội bộ (token service) |
| GET | `/internal/bookings/{bookingId:\\d+}/score-redemption-context` | `getScoreRedemptionContext` | Nội bộ (token service) |
| POST | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89aAbB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}}/refund-results` | `recordRefundResultByPublicId` | Nội bộ (token service) |
| POST | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/confirm` | `confirmBooking` | Nội bộ (token service) |
| POST | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/expire` | `expireBooking` | Nội bộ (token service) |
| GET | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/lifecycle-context` | `getLifecycleContext` | Nội bộ (token service) |
| GET | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/payment-context` | `getPaymentContextByPublicId` | Nội bộ (token service) |
| POST | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/payment-results` | `recordPaymentResultByPublicId` | Nội bộ (token service) |
| POST | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/refund` | `refundBooking` | Nội bộ (token service) |
| GET | `/internal/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/score-redemption-context` | `getScoreRedemptionContextByPublicId` | Nội bộ (token service) |
| GET | `/internal/bookings/code/{bookingCode}` | `getBookingByCode` | Nội bộ (token service) |
| GET | `/internal/bookings/code/{bookingCode}/payment-context` | `getPaymentContextByCode` | Nội bộ (token service) |
| POST | `/internal/bookings/showtimes/{showtimePublicId:[a-fA-F0-9-]{36}}/emergency-close` | `closeShowtimeForEmergency` | Nội bộ (token service) |

### Nhóm `InternalSeatReservationController`

Đường dẫn gốc: `/internal/seat-reservations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/internal/seat-reservations/availability` | `checkAvailability` | Nội bộ (token service) |
| POST | `/internal/seat-reservations/convert` | `convertReservation` | Nội bộ (token service) |
| POST | `/internal/seat-reservations/expire` | `expireReservation` | Nội bộ (token service) |
| POST | `/internal/seat-reservations/release` | `releaseReservation` | Nội bộ (token service) |

### Nhóm `ManagerBookingController`

Đường dẫn gốc: `/api/manager/bookings`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/manager/bookings` | `search` | Vai trò `MANAGER` |
| GET | `/api/manager/bookings/{bookingPublicId}` | `detail` | Vai trò `MANAGER` |
| PUT | `/api/manager/bookings/{bookingPublicId}/cancel-hold` | `cancelHold` | Vai trò `MANAGER` |
| GET | `/api/manager/bookings/{bookingPublicId}/foods` | `foodOrder` | Vai trò `MANAGER` |
| GET | `/api/manager/bookings/summary` | `summary` | Vai trò `MANAGER` |

### Nhóm `ManagerTicketControlController`

Đường dẫn gốc: `/api/manager/ticket-operations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/manager/ticket-operations/handoffs` | `handoffs` | Vai trò `MANAGER` |
| GET | `/api/manager/ticket-operations/history` | `history` | Vai trò `MANAGER` |
| GET | `/api/manager/ticket-operations/summary` | `summary` | Vai trò `MANAGER` |

### Nhóm `SeatReservationController`

Đường dẫn gốc: `/api/seat-reservations`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| DELETE | `/api/seat-reservations` | `releaseSeats` | Đã đăng nhập |
| GET | `/api/seat-reservations` | `getMyReservations` | Đã đăng nhập |
| POST | `/api/seat-reservations` | `holdSeats` | Đã đăng nhập |
| GET | `/api/seat-reservations/{publicId}` | `getReservationDetail` | Đã đăng nhập |
| POST | `/api/seat-reservations/{publicId}/extend` | `extendReservation` | Đã đăng nhập |
| GET | `/api/seat-reservations/showtime/{showtimeId}/occupied-seats` | `getOccupiedSeatsByShowtime` | Đã đăng nhập |
| GET | `/api/seat-reservations/showtimes/{showtimePublicId}/availability` | `getPublicAvailability` | Đã đăng nhập |

### Nhóm `TicketController`

Đường dẫn gốc: `/api/bookings`.

| Phương thức | Đường dẫn đầy đủ | Hàm xử lý | Quyền truy cập |
|---|---|---|---|
| GET | `/api/bookings/{publicId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}/tickets` | `getTicketsByBookingId` | Đã đăng nhập |

## 6. Quy tắc cập nhật tài liệu

Khi thêm, xóa hoặc đổi endpoint, cần cập nhật đồng thời controller, SecurityConfig, route Gateway (nếu frontend cần gọi) và file này. OpenAPI runtime là nguồn chuẩn cho field request/response; tài liệu Markdown là mục lục dễ đọc và bản kiểm tra phạm vi.
