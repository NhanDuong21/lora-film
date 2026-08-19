# Audit vòng đời Score Service — 19/08/2026

## Kết luận điều hành

Sau vòng audit và refine, module đã **đủ rõ ràng để demo, UAT và cho admin vận hành trên dữ liệu seed**. Luồng chính không còn là các màn CRUD rời rạc: admin có thể phát hiện bất thường, đi từ hồ sơ khách hàng sang hồ sơ điểm, kiểm tra ledger, xử lý khiếu nại có dấu vết, đóng băng riêng tài khoản điểm và theo dõi đối soát.

Module **chưa nên được gọi là production-ready end-to-end** cho tiền thật cho đến khi hoàn thành các gate P0 ở cuối tài liệu. Lý do chính không còn nằm ở hình thức UI mà ở độ phủ dữ liệu và đối soát xuyên service.

## Bằng chứng từ dữ liệu seed

| Chỉ số | Giá trị quan sát | Ý nghĩa vận hành |
|---|---:|---|
| Hồ sơ customer | 3 | Population do user-service trả về |
| Score account | 8 | Population do score-service quản lý |
| Chênh population | +5 | Cần xác minh mapping/backfill trước go-live |
| Score account active / frozen | 8 / 0 | Admin nhìn được trạng thái nghiệp vụ điểm riêng với trạng thái đăng nhập |
| Điểm khả dụng toàn hệ thống | 7.584 | Số có thể tiêu, đã trừ phần tạm giữ |
| Điểm đang giữ | 0 | Nghĩa vụ chưa commit/release |
| Điểm xét hạng | 7.590 | Tách khỏi điểm khả dụng |
| Điểm đã dùng | 6 | Được tính từ ledger thực, không còn hard-code `0` |
| Outstanding | 0 | Chưa có nợ điểm cần thu hồi |
| Đối soát gần nhất | 6/8 account, 75% | `0 mismatch` không đồng nghĩa toàn bộ population đã sạch |
| Customer kiểm thử | Account 6 — 58 khả dụng, 0 giữ, 64 xét hạng | Khớp giữa admin workspace và customer loyalty center |

## Vòng đời admin sau refine

1. **Phát hiện:** “Bàn điều hành điểm thưởng” hiển thị available, held, tier points, outstanding, issued/redeemed/expired, population gap, độ phủ và tuổi của lần đối soát gần nhất.
2. **Định danh:** từ danh sách khách hàng, admin mở thẳng hồ sơ score bằng Account ID; thao tác khóa đăng nhập và đóng băng điểm được tách riêng để tránh nhầm phạm vi.
3. **Điều tra:** workspace score hiển thị số dư, hold, điểm hạng, outstanding, trạng thái, công thức hiện hành, ledger và snapshot của từng nghiệp vụ.
4. **Xử lý khiếu nại:** bắt buộc mã case, lý do, request/idempotency key; có preview số dư và bước xác nhận. Việc ảnh hưởng điểm hạng là lựa chọn riêng có cảnh báo.
5. **Đảo điều chỉnh:** chỉ chọn giao dịch điều chỉnh thủ công gần đây và yêu cầu xác nhận; backend chặn đảo lặp.
6. **Giảm rủi ro tức thời:** admin có thể freeze/unfreeze score account với lý do và case mà không khóa quyền đăng nhập của customer.
7. **Quản lý chính sách hạng:** UI dùng phần trăm dễ hiểu, hiển thị công thức và số điểm ví dụ; cảnh báo policy mới chỉ áp dụng giao dịch mới và ledger giữ snapshot.
8. **Đối soát:** mặc định mở hàng đợi exception; manual run được hạ xuống vùng nâng cao. Kết quả phân loại balance/hold/tier mismatch và deep-link về hồ sơ account.
9. **Truy vết:** nhật ký tích điểm và ledger trả về source, correlation/case, snapshot số dư/hold/tier/rate phục vụ điều tra.

## Vòng đời customer sau refine

1. Booking/payment hợp lệ phát sinh điểm theo công thức: `giá trị hợp lệ × tỷ lệ hạng ÷ 1.000đ`, làm tròn xuống.
2. Khi dùng điểm cho đơn chưa hoàn tất, điểm chuyển sang **tạm giữ**; sau đó commit nếu thành công hoặc release nếu thất bại/hủy.
3. Customer luôn thấy riêng **điểm khả dụng**, **điểm tạm giữ** và **tổng điểm xét hạng**; dùng điểm không làm giảm tiến độ hạng.
4. Ledger có bộ lọc tích/dùng/hoàn-thu hồi/tạm giữ, dùng nhãn `Booking ID` thay vì giả định ID nội bộ là mã đơn public.
5. Điểm được quản lý theo bucket hết hạn, ưu tiên dùng lô gần hết hạn trước.
6. Refund/revoke/expired/manual adjustment đều có nhãn nghiệp vụ phía customer; trạng thái score bị khóa có banner riêng.
7. Tổng quan tài khoản chỉ chọn suất chiếu ở tương lai; dữ liệu kiểm thử hiện hiển thị đúng “Chưa có suất chiếu sắp tới”.

## Các sửa đổi kỹ thuật quan trọng

- Dashboard lấy số phát hành, đã dùng, hết hạn và trạng thái đối soát từ repository thay vì số giả.
- Sửa invariant đối soát điểm hạng: cộng chênh `accumulatedAfter - accumulatedBefore`, không cộng mọi giao dịch ledger dương. Vì vậy hoàn điểm dùng (`REFUND_REDEEM`) không còn làm phình điểm xét hạng.
- Enrich response admin với available/held/outstanding/status, source, case/correlation, snapshot rate/tier/balance.
- Bổ sung API freeze/unfreeze score account có audit và outbox event.
- Ledger mới ghi snapshot đầy đủ cho earn, hold, commit, release, redeem, refund, revoke và expiration.
- Deep-link customer → score workspace → complaint/audit/reconciliation giúp giảm tra cứu thủ công bằng numeric ID.
- Công thức customer được sửa theo đúng backend; loại bỏ claim sai “10.000đ = 1 điểm”.

## Gate còn lại trước go-live

### P0 — bắt buộc

1. **Khép population:** xác định vì sao user-service có 3 customer nhưng score-service có 8 account; backfill hoặc loại orphan rồi chạy đối soát đủ 100%.
2. **Đối soát xuyên service:** lần đối soát hiện xác nhận ledger ↔ projection/hold nội bộ. Cần thêm invariant đối chiếu booking/payment/refund source event để phát hiện giao dịch gốc bị thiếu, trùng hoặc sai trạng thái.
3. **Lịch đối soát và SLA cảnh báo:** chạy tự động theo lịch, cảnh báo khi coverage < 100%, run quá hạn, mismatch hoặc outbox thất bại; manual run chỉ dùng cho điều tra.
4. **Backfill snapshot lịch sử:** các ledger seed cũ (đặc biệt HOLD) chưa có held/rate/source snapshot chính xác. Code mới bảo đảm event tương lai, nhưng dữ liệu cũ cần migration nếu giữ khi go-live.

### P1 — hardening vận hành

1. Maker-checker/approval cho điều chỉnh vượt ngưỡng, thay đổi policy hạng và unfreeze account nhạy cảm.
2. Version/effective time cho chính sách hạng để có thể schedule, rollback và giải thích chính xác policy tại thời điểm giao dịch.
3. RBAC và masking cho export/PII; ghi audit tải dữ liệu và giới hạn phạm vi theo vai trò.
4. Contract/integration test cho toàn chuỗi booking → hold → payment → commit/release → refund/revoke, gồm retry, event trùng và out-of-order.

## Kiểm chứng đã chạy

- Client production build: **pass** — Vite, 2.274 modules.
- ESLint các file score thay đổi: **pass**.
- Client score tests: **4/4 pass**.
- Backend score-service full suite: **104/104 pass**. Trong lúc chạy full suite đã phát hiện test profile không override internal token nên 36 request bị `403`; cấu hình test đã được tách rõ khỏi secret production và chạy lại toàn bộ thành công.
- Live UI audit bằng hai vai admin/customer: **pass**; không thực hiện điều chỉnh điểm, freeze hoặc manual reconciliation lên dữ liệu đang chạy.

## Ảnh audit

![Bàn điều hành admin](admin-score-operations-dashboard.png)

![Workspace hồ sơ điểm customer](admin-customer-score-workspace.png)

![Workspace xử lý khiếu nại](admin-score-complaint-workspace.png)

![Hàng đợi đối soát](admin-score-reconciliation-queue.png)

![Trung tâm điểm phía customer](customer-loyalty-center.png)
