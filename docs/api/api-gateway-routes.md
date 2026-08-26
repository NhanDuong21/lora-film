# Danh mục route của API Gateway

> Đã đồng bộ với `application.example.properties`, `RouteValidator` và `AuthenticationFilter` ngày 26/08/2026.

## 1. Vai trò của Gateway

Frontend gọi `http://localhost:8080`. Gateway chọn service đích, giới hạn tốc độ, kiểm tra JWT ở route được bảo vệ và chuyển tiếp thông tin người dùng đã xác minh. Gateway không đổi prefix đường dẫn.

API nội bộ dưới `/internal/**` hoặc `/api/v1/internal/**` không được đưa ra Gateway. Backend gọi trực tiếp cổng service đích và gửi token nội bộ.

## 2. Cổng local

| Thành phần | Cổng |
|---|---:|
| API Gateway | 8080 |
| Auth | 8081 |
| Movie | 8082 |
| Booking | 8083 |
| Payment | 8084 |
| Notification | 8085 |
| User | 8086 |
| Promotion | 8087 |
| Score | 8088 |
| Analytics | 8089 |
| Eureka | 8761 |

## 3. Route chuyển tiếp hiện tại

| Route | Đích | Phạm vi |
|---|---|---|
| `auth-service` | `lb://auth-service` | `/api/auth/**`<br>`/oauth2/**`<br>`/login/oauth2/**`<br>`/api/roles/**`<br>`/api/permissions/**`<br>`/api/access-profiles/**`<br>`/api/accounts/**`<br>`/api/audits/**` |
| `user-service` | `lb://user-service` | `/api/users/**`<br>`/api/admin/user-audits` |
| `movie-service` | `lb://movie-service` | `/api/movies/**`<br>`/api/genres/**`<br>`/api/showtimes/**`<br>`/api/cinemas/**`<br>`/api/customer/movies/**`<br>`/api/customer/showtimes/**`<br>`/api/customer/genres/**`<br>`/api/public/people/**`<br>`/api/manager/cinemas/**`<br>`/api/manager/showtimes/**`<br>các nhóm `/api/admin/**` về phim, thể loại, rạp, phòng, ghế, suất chiếu, giá, TMDB, lịch tự động và địa điểm |
| `promotion-service` | `lb://promotion-service` | `/api/promotions/**`<br>`/api/customers/me/promotions/**`<br>`/api/customers/me/promotion-history`<br>`/api/manager/promotions/**`<br>các nhóm `/api/admin/**` về khuyến mãi, chiến dịch, cơ hội, kịch bản, lượt chạy, bất thường, giám sát, vận hành, giữ ưu đãi, cấu hình và sự kiện |
| `score-service` | `lb://score-service` | `/api/scores/**`<br>`/api/admin/scores/**`<br>`/api/membership-tiers/**`<br>`/api/admin/membership-tiers/**` |
| `booking-service` | `lb://booking-service` | `/api/bookings/**`<br>`/api/seat-reservations/**`<br>`/api/customer/concessions/**`<br>`/api/customer/cart/**`<br>`/api/admin/bookings/**`<br>`/api/admin/monitoring/**`<br>`/api/admin/foods/**`<br>`/api/manager/bookings/**`<br>`/api/manager/ticket-operations/**`<br>`/api/employee/ticket-operations/**` |
| `payment-service` | `lb://payment-service` | `/api/payments/**`<br>`/api/employee/payments/**`<br>`/api/admin/payments/**`<br>`/api/manager/payments/**`<br>`/api/vnpay/**` |
| `notification-service` | `lb://notification-service` | `/api/v1/notifications/**`<br>`/api/v1/admin/notification-templates/**`<br>`/api/v1/admin/notifications/**`<br>`/api/v1/admin/notification-settings/**` |
| `analytics-service` | `lb://analytics-service` | `/api/analytics/**`<br>`/api/admin/reports/**` |
| `tmdb-api` | `TMDB_SERVICE_URL` | `/api/import/**`<br>`/api/tmdb/**` |
| `global-location-api` | `LOCATION_API_BASE_URL` | `/api/v1/address/**`<br>`/api/v1/geocode/**` |
| `booking-seat-socket` | `BOOKING_SOCKET_URI` | `/socket.io/**` |

Danh sách pattern chính xác nằm tại `api-gateway/src/main/resources/application.example.properties`. Các file API của từng service liệt kê endpoint cụ thể.

## 4. Route công khai

Các endpoint Auth công khai gồm đăng ký, kiểm tra số định danh, đăng nhập, xác minh email, gửi OTP, làm mới token, quên mật khẩu và đặt lại mật khẩu.

GET công khai theo prefix: `/api/customer/movies`, `/api/customer/genres`, `/api/customer/showtimes`, `/api/public/people`, `/api/customer/concessions`, `/api/cinemas`, `/api/showtimes`, `/api/membership-tiers`, `/api/promotions/public`, `/api/promotions/offers`, `/api/promotions/assets`, `/api/users/profile/avatar/files/`.

Callback và return thanh toán dưới `/api/payments/callback/**` và `/api/payments/return/**`, WebSocket `/socket.io/**`, OAuth2, health, Swagger và OpenAPI cũng không yêu cầu Bearer token tại Gateway.

## 5. Route cần đăng nhập

Route không nằm trong danh sách công khai phải gửi `Authorization: Bearer <access-token>`. Gateway kiểm tra chữ ký, loại token, thời hạn và trạng thái thu hồi trong Redis. Token hợp lệ phải có `userId`, `role`, `tokenType=access` và subject.

Gateway luôn xóa header nhận dạng do client tự gắn rồi tạo lại từ JWT: `loggedInUser`, `loggedInUserId`, `loggedInRole`, `loggedInPermissions` và các header `X-Authenticated-*`.

## 6. Giới hạn tốc độ và dịch vụ ngoài

Mặc định mỗi client được bổ sung 20 token/giây và có thể dồn tối đa 40 token. Có thể đổi bằng `GATEWAY_RATE_LIMIT_REPLENISH_RATE` và `GATEWAY_RATE_LIMIT_BURST_CAPACITY`.

Route TMDB có circuit breaker và fallback `/gateway-fallback/tmdb`. Route Location tự gắn `x-api-key`. Khóa API phải được cấp qua biến môi trường khi chạy ngoài local.

## 7. Endpoint riêng của Gateway

| Phương thức | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/health` | Kiểm tra Gateway đang chạy |
| Nội bộ | `/gateway-fallback/tmdb` | Trả lỗi 503 khi dịch vụ TMDB không sẵn sàng |

## 8. Lưu ý từ lần kiểm toán này

- Gateway vẫn có quy tắc công khai cho `GET /api/auth/registrations/{id}/status`, nhưng Auth Service hiện không có controller cho endpoint đó. Không xem đây là API đang dùng.
- `GET /api/auditoriums/{id}/seat-layout` có trong Movie Service nhưng chưa có pattern Gateway tương ứng.
- `PUT /api/admin/user-audits/{id}/review` có trong User Service, nhưng Gateway hiện chỉ map chính xác `/api/admin/user-audits`, không map đường dẫn con.
- Khi thêm endpoint frontend mới, phải cập nhật controller, route Gateway và tài liệu service tương ứng.
