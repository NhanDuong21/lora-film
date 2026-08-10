# LoraFilm client runtime readiness

Baseline: branch `hotfix/final-debug`, môi trường development/demo, 2026-08-03.

Mức sẵn sàng sau implementation: **DEMO_READY_WITH_PRECONDITIONS**. Các P0 read-route và UI-state đã được sửa; luồng tạo booking mới, food/voucher/payment mutation và STAFF cash collection vẫn cần record disposable hợp lệ để chạy golden regression hai lần.

| Thành phần | Trạng thái sau sửa | Port | Bằng chứng / ghi chú |
|---|---|---:|---|
| Client | RUNNING / VERIFIED | 5173 | Guest, CUSTOMER, STAFF và ADMIN đã được smoke-test bằng trình duyệt thật |
| API Gateway | RUNNING | 8080 | API cùng origin qua Vite proxy; không còn absolute Gateway URL ở client |
| Eureka | RUNNING | 8761 | Mười application service đăng ký `UP` tại baseline |
| Auth | RUNNING | 8081 | Login theo role và return-to CUSTOMER đã được kiểm chứng |
| Movie | RUNNING / DEGRADED_SAFE | 8082 | TMDB là dependency tùy chọn; review/scheduler tắt mặc định khi helper 9005 vắng mặt |
| Booking | RUNNING / FIXED | 8083 | Internal payment view hỗ trợ legacy amount-only snapshot; custom `/health` vẫn là P1 contract gap |
| Payment | RUNNING / FIXED | 8084 | CASH detail mở 200; payment suite 97/97 pass |
| Notification | RUNNING | 8085 | Route admin chạy; health-style request vẫn yêu cầu xác thực |
| User | RUNNING | 8086 | Profile/avatar route đã được kiểm tra |
| Promotion | RUNNING | 8087 | Customer/admin data đọc được; health-style request vẫn yêu cầu xác thực |
| Score | RUNNING / FIXED | 8088 | Dashboard API 200 và UI không còn crash |
| Analytics | RUNNING | 8089 | Admin analytics chạy; health-style request vẫn yêu cầu xác thực |
| MySQL | RUNNING / UNCHANGED | 3307 | Không DROP/TRUNCATE/migration hay sửa seed trực tiếp |
| Redis / Kafka / ZooKeeper | RUNNING | 6379 / 9092 / 2181 | Infrastructure baseline hoạt động |

## Regression gates

- Client: **102 files, 400/400 tests pass**.
- Client production build: **PASS**.
- ESLint: **PASS, 0 error / 0 warning**.
- Booking service: **165/165 tests pass**.
- Payment service: **97/97 tests pass**.
- Movie pricing/showtime focused suite: **40/40 tests pass**.
- Browser: score dashboard, CASH payment detail, accessible-seat pricing, movie detail without TMDB 502, CUSTOMER success/cancelled states và role redirects đều không có Console error; các route P0 đã kiểm tra không có unexpected 4xx/5xx.

Credentials và tokens bị loại khỏi source, log và tài liệu.
