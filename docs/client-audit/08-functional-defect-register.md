# Functional defect register

## Tóm tắt

| ID | Priority | Trạng thái sau implementation | Regression risk |
|---|---|---|---|
| P0-01 | P0 | FIXED / BROWSER_VERIFIED | Backend đổi field lần nữa hoặc trả non-finite number |
| P0-02 | P0 | FIXED / BROWSER_VERIFIED | Legacy payment-log enum/optional aggregate mới |
| P0-03 | P0 | FIXED_BY_SCOPE / BROWSER_VERIFIED | Route/menu cũ vô tình được expose lại |
| P0-04 | P0 | SECURITY_FIXED / DATA_PRECONDITION_OPEN | Security matrix regress hoặc booking không payable |
| P0-05 | P0 | FIXED / BROWSER_VERIFIED | Booking/ticket/payment eventual-consistency |
| P0-06 | P0 | FIXED / TEST_AND_BROWSER_VERIFIED | Seat type mới không có fallback/gate |
| P0-07 | P0 | FIXED / BROWSER_VERIFIED | Feature flag bật nhưng helper/network vắng mặt |

## P0-01 — Score dashboard response mismatch

- Reproduction: login ADMIN → `/admin/scores/dashboard`; `GET /api/admin/scores/dashboard` trả 200 nhưng React gọi numeric formatter trên `undefined` và crash.
- Expected / actual: dashboard phải render số 0 hoặc số backend trả về; actual cũ là blank/crash sau response thành công.
- Request/response evidence: wire response dùng `pendingReconciliations` và `lastReconciliationDate`; client cũ đọc `pendingReconciliationMismatches` và `lastReconciliationTime`.
- Console/DB evidence: Console cũ có `toLocaleString` trên `undefined`; không phải lỗi DB.
- Probable root cause: thiếu adapter contract và default finite value.
- Module sửa: `client/src/features/score/admin/services/scoreAdminService.js`, dashboard pages và regression test.
- Fix/verification: normalize sang `ScoreDashboardViewModel`, default number bằng 0, bảo vệ formatter. Browser mở dashboard thành công, API 200, 0 Console error.
- Status: **FIXED / BROWSER_VERIFIED**.

## P0-02 — Admin CASH payment detail 500

- Reproduction: ADMIN mở `/admin/payments/<cash-payment-id>` với payment có snapshot/log/outbox nhưng không có webhook/refund/reconciliation.
- Expected / actual: detail giữ nguyên URL và render optional sections rỗng; actual cũ là API 500 rồi client âm thầm quay về list.
- Request/response evidence: `GET /api/admin/payments/<id>` cũ 500 khi đọc legacy log event; sau sửa trả 200 và detail hiển thị CASH snapshot/log.
- Console/DB evidence: record demo có cash snapshot, logs và outbox; legacy event type `PAYMENT_SUCCESS` không nằm trong Java enum. Không thay DB/schema.
- Probable root cause: deserialization/mapping enum legacy và UI error handling redirect sai.
- Module sửa: payment `PaymentLogEventType`, persistence regression fixture, `AdminPaymentDetailPage` error/retry state.
- Regression risk: thêm event type legacy khác hoặc optional aggregate bị dereference.
- Fix/verification: payment suite 97/97; browser detail giữ URL, response 200, 0 Console error.
- Status: **FIXED / BROWSER_VERIFIED**.

## P0-03 — STAFF POS dùng customer cart và mock payment

- Reproduction: STAFF vào `/employee` hoặc `/employee/pos`; client cũ gọi `/api/customer/cart` hai lần, nhận 500 và expose mock success/failure controls.
- Expected / actual: STAFF v1 chỉ có cash collection; actual cũ landing vào unfinished POS.
- Request/response evidence: customer-cart request không thuộc STAFF contract; mock endpoint không phải production operation.
- Console/DB evidence: hai GET 500; không có business mutation hợp lệ để kiểm chứng POS.
- Probable root cause: employee route tái sử dụng customer feature trước khi API/permission hoàn tất.
- Module sửa: employee route config/layout/navigation.
- Regression risk: deep link/menu import cũ expose lại POS hoặc mock controls.
- Fix/verification: `/employee` và `/employee/pos` redirect sang `/employee/payments/cash`; STAFF menu chỉ có “Thu Tiền Tại Quầy”.
- Status: **FIXED_BY_SCOPE / BROWSER_VERIFIED**.

## P0-04 — STAFF cash authorization và legacy booking snapshot

- Reproduction: STAFF tìm booking tại `/employee/payments/cash`.
- Expected / actual: STAFF được lookup/collect booking payable; actual ban đầu 403 vì backend chỉ chấp nhận `EMPLOYEE`/`SUPERVISOR`/`ADMIN`. Sau khi mở role, integration lộ thêm 500 do `priceSnapshot.seats()` null trên legacy amount-only snapshot.
- Request/response evidence: sau sửa security và snapshot adapter, request không còn 403/500. Seed kiểm tra trả 409 `BOOKING_NOT_PAYABLE` đúng business rule vì `amountLockedAt` nằm ngoài khoảng payable tại thời điểm test.
- Console/DB evidence: DB có booking pending nhưng không có record thỏa `amount_locked_at <= now < expires_at`; không sửa seed bằng SQL để ép pass.
- Probable root cause: role vocabulary lệch và internal payment view giả định snapshot mới luôn có seat array.
- Module sửa: payment `SecurityConfig`; booking `InternalBookingPaymentServiceImpl` và test legacy presentation fallback.
- Regression risk: route security nới nhầm cho CUSTOMER, snapshot thiếu cả presentation/reservation, double submit.
- Fix/verification: payment security 9/9; booking focused 15/15 và full 165/165. Browser STAFF landing/menu đúng; lookup đến business eligibility thay vì auth/server error.
- Status: **SECURITY_FIXED / DATA_PRECONDITION_OPEN**. Cash mutation cần disposable payable booking.

## P0-05 — Payment success receipt ghi 0 ghế/0đ

- Reproduction: CUSTOMER mở `/bookings/success?bookingId=<owned-success-booking>`.
- Expected / actual: issued tickets, seat count và tổng tiền đồng nhất; actual cũ chỉ đọc `booking.tickets`, không gọi ticket API và dùng sai `finalAmount`/`snapshot` thay vì `totalAmount`/`presentation`.
- Request/response evidence: booking response có `ticketAmount=300000`, `totalAmount=305000`, `paymentStatus=SUCCESS`; tickets endpoint trả 4 tickets.
- Console/DB evidence: API data đúng nhưng UI cũ hiển thị 0 ghế/0đ; không cần sửa DB.
- Probable root cause: frontend contract drift và thiếu consistency join.
- Module sửa: `BookingSuccessPage`, ticket fetch/normalization và tests.
- Regression risk: tickets phát hành chậm hoặc payment/booking status tạm thời chưa đồng nhất.
- Fix/verification: browser hiển thị 4 vé, 4 QR, “Đặt giữ 4 ghế”, “Thanh toán đã xác nhận”, tổng 305.000đ; 0 Console error.
- Status: **FIXED / BROWSER_VERIFIED**.

## P0-06 — Accessible seat pricing incomplete

- Reproduction: mở pricing của showtime `OPEN_FOR_BOOKING`; response cũ `complete=false`, `missingSeatTypes` chứa `DISABLED` dù auditorium có ghế hỗ trợ active.
- Expected / actual: DISABLED bookable có giá, và transition mở bán chỉ hợp lệ khi pricing complete.
- Request/response evidence: sau sửa, pricing hiển thị STANDARD 80.000đ, VIP 105.000đ, COUPLE 195.000đ, DISABLED 80.000đ và `complete=true`.
- Console/DB evidence: policy hiện hữu có STANDARD/VIP/COUPLE; không thêm schema/migration hoặc sửa trực tiếp seed.
- Probable root cause: resolver yêu cầu exact seat-type price và lifecycle/query không dùng cùng completeness rule.
- Module sửa: `AccessibleSeatPricing`, price resolver, pricing/query/booking-context/showtime services.
- Regression risk: seat type mới, policy ambiguous, read fallback vô tình che DRAFT incomplete state.
- Fix/verification: movie focused suite 40/40; browser showtime pricing complete và không Console error.
- Status: **FIXED / TEST_AND_BROWSER_VERIFIED**.

## P0-07 — TMDB helper vắng mặt gây 502

- Reproduction: mở saved movie detail; review panel cũ tự gọi helper ở 9005 và scheduler tiếp tục retry.
- Expected / actual: movie đã lưu luôn xem/sửa được; TMDB chỉ là optional tool. Actual cũ có request 502.
- Request/response evidence: sau sửa, movie detail chỉ đọc local data/sync state; không phát sinh review 502 khi flag tắt.
- Console/DB evidence: helper 9005 không chạy; browser movie detail 0 Console error và không có unexpected HTTP problem.
- Probable root cause: dependency tùy chọn bị coi là runtime prerequisite.
- Module sửa: TMDB frontend hook/panel, movie scheduler flags và example properties.
- Regression risk: môi trường bật flag nhưng thiếu preflight/helper/network.
- Fix/verification: review action degrade an toàn; schedulers off mặc định; saved movie detail browser-verified.
- Status: **FIXED / BROWSER_VERIFIED**.

## Delivery blocker — payment test bootstrap

- Baseline: Testcontainers init dùng user `test` chạy canonical DDL và không có quyền tạo `payment_db`, khiến ApplicationContext fail trước test nghiệp vụ.
- Fix: test JDBC URL khởi tạo container/database `payment_db` sẵn; canonical production DDL giữ nguyên.
- Evidence: payment suite **97/97 pass**, không production schema change.
- Status: **FIXED**.
