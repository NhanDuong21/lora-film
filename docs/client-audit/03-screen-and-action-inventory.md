# Screen and action inventory

## Working read actions sau implementation

- Role login/logout, public discovery, protected-route redirect và CUSTOMER return-to hợp lệ.
- Movie/cinema/showtime browsing, filters, search, pagination và optional TMDB degradation.
- Customer profile, loyalty, promotion wallet, owned booking history/detail, success receipt và payment return.
- Cancelled/expired/refunded ticket presentation ở trạng thái vô hiệu, không QR/admission copy.
- STAFF cash landing/search contract với role `STAFF`; route/menu unfinished đã bị ẩn khỏi scope.
- Admin movie/facility/scheduling/pricing/booking/payment/score/HR/analytics/notification reads.

## P0 disposition

- P0-01 score dashboard: **FIXED**.
- P0-02 admin payment detail: **FIXED**.
- P0-03 unfinished STAFF POS/mock: **REMOVED_FROM_SCOPE**.
- P0-04 STAFF role/legacy booking snapshot: **FIXED**, cash mutation còn disposable-data precondition.
- P0-05 success tickets/total: **FIXED**.
- P0-06 accessible-seat pricing: **FIXED**.
- P0-07 TMDB dependency: **FEATURE_GATED / DEGRADED_SAFE**.

## Mutation policy và coverage gap

Create/update/cancel/publish/refund/collect actions chỉ chạy trên disposable records, có confirmation, duplicate-submit, Network/Console và DB/audit verification. Phiên này không sửa trực tiếp seed và chưa có booking cash hiện tại thỏa payable window, nên fresh customer mutation, food/voucher recalculation, STAFF collect và refund/reconciliation vẫn phải hoàn tất trước nhãn `DEMO_READY`.
