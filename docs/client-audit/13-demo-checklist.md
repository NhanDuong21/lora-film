# Demo checklist

## Đã xác minh trong phiên 2026-08-03

- [x] Đúng branch `hotfix/final-debug`; không branch/commit/push/rebase/reset/amend.
- [x] Gateway, services và infrastructure đạt baseline runtime.
- [x] TMDB vắng mặt được degrade an toàn, movie detail không 502.
- [x] Showtime kiểm tra có đủ giá STANDARD/VIP/COUPLE/DISABLED.
- [x] ADMIN, STAFF và CUSTOMER login/landing đúng scope.
- [x] Score dashboard và admin CASH payment detail không crash/redirect/error.
- [x] CUSTOMER success có 4 vé, tổng 305.000đ; cancelled ticket không có QR/admission copy.
- [x] Client 400/400; booking 165/165; payment 97/97; movie focused 40/40.
- [x] Production build và lint 0/0 pass.
- [x] Không expose credential/token, không sửa trực tiếp demo data, không bật mock payment.

## Precondition phải hoàn tất ngay trước demo

- [ ] Tạo một disposable cash booking đang trong khoảng payable bằng application flow.
- [ ] Chạy STAFF lookup → amount/change → collect → double-submit guard → DB/audit verify.
- [ ] Chạy fresh CUSTOMER booking với food/promotion và payment success/failure trên disposable records.
- [ ] Chạy runtime/health preflight; không dựa riêng vào port/Eureka `UP`.
- [ ] Chạy golden route sweep hai vòng liên tiếp và xác nhận Network/Console sạch.

## Kịch bản demo an toàn hiện tại

- [x] Role login và landing.
- [x] Guest/movie discovery.
- [x] Admin movie → facilities → pricing → showtime read flow.
- [x] Customer profile/history/payment return/result/ticket bằng seed đã xác minh.
- [x] Admin booking/payment/score/analytics read flow.
- [ ] STAFF cash collection chỉ chạy sau khi disposable booking precondition được đáp ứng.
