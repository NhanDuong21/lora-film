# Client test execution plan and result

| Phiên | Kết quả hiện tại | Còn cần làm |
|---|---|---|
| Runtime/registry/config | Baseline services và infrastructure up; browser dùng app thật | Chuẩn hóa health contract và chạy preflight sát giờ demo |
| Guest/auth/404 | Route guard và login redirect pass | Hai vòng golden sweep cuối |
| CUSTOMER read/state | Owner/cross-account, success/cancelled states pass | Expired/failure variants trên disposable records |
| CUSTOMER fresh booking | Đến seat selection đã kiểm tra | Food/promotion/payment mutation success + failure |
| STAFF cash/security | Role matrix và lookup integration pass tới eligibility | Tạo payable booking, collect/double-submit/DB verify |
| ADMIN movie/facility/pricing | Movie detail, pricing complete và TMDB degradation pass | Mutation lifecycle chỉ với disposable data |
| ADMIN payment/score/analytics | Payment detail, score dashboard và read routes pass | Refund/reconciliation disposable regression |
| Browser/Console/Network | P0 routes không Console error; không unexpected 4xx/5xx trên verified routes | Full two-pass sweep sát giờ demo |

## Automated gates đã chạy

- Client: **102 files / 400 tests pass**.
- Build: **PASS**.
- ESLint: **0 error / 0 warning**.
- Booking: **165 tests pass**.
- Payment: **97 tests pass**.
- Movie focused: **40 tests pass**.

Test timezone dùng ba subprocess có timeout cục bộ 15 giây để tránh false negative khi chạy cùng Spring integration suites; assertion và production code không thay đổi.
