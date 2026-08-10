# Remediation plan status

| Batch | Trạng thái | Kết quả |
|---|---|---|
| 1. Admin score/payment stability | COMPLETE | Score response được normalize; payment legacy log mapping sửa; detail giữ URL và có retry; payment test bootstrap pass |
| 2. STAFF role/navigation/cash | COMPLETE_WITH_DATA_PRECONDITION | `STAFF` được cấp cash API; landing/menu thu gọn; legacy booking snapshot fallback sửa; cần disposable payable booking để chạy mutation |
| 3. Customer result/auth UX | COMPLETE | Route guard/return-to, tickets endpoint, receipt normalization và cancelled-ticket invalid state đã browser-verified |
| 4. Showtime/pricing | COMPLETE | Accessible seat dùng STANDARD price; read/lifecycle completeness dùng source of truth hiện có; không migration |
| 5. Optional dependencies | COMPLETE_EXCEPT_HEALTH_CONTRACT | TMDB feature-gated, scheduler off mặc định, QR fallback; cross-service health normalization chuyển sang follow-up P1 |
| 6. Navigation/content/UI | COMPLETE_FOR_DEMO_SCOPE | Avatar, filters, analytics link, score nav, localization, footer/contact/loyalty/home copy, API proxy và duplicate GET được xử lý |
| 7. Regression/hardening | COMPLETE_WITH_MUTATION_PRECONDITION | 400 client + 165 booking + 97 payment + 40 movie tests pass; build/lint pass; browser P0 sweep pass |

## Follow-up bắt buộc trước khi gắn nhãn `DEMO_READY`

1. Tạo booking disposable đang payable bằng application flow, không sửa SQL seed.
2. Chạy customer fresh booking → concessions/promotion → payment result hai lần trên record riêng.
3. Chạy STAFF lookup/collect, double-submit và DB/audit verification trên disposable cash booking.
4. Chạy health preflight thống nhất và hai vòng Guest → CUSTOMER → STAFF → ADMIN liên tiếp.
5. Chỉ sau khi không có unexpected 4xx/5xx, Console error, duplicate mutation hoặc state lệch mới đổi trạng thái thành `DEMO_READY`.
