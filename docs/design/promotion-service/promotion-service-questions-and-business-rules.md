# PROMOTION SERVICE — BỘ CÂU HỎI KHẢO SÁT YÊU CẦU & BUSINESS RULES ĐẦY ĐỦ
### Dựa trên `promotion-service-plan.md` (v2) — Hệ thống bán vé xem phim, tham chiếu CGV Cinemas / Galaxy Cinema (Việt Nam)

> Tài liệu này gồm 2 phần: **(A)** Bộ câu hỏi mà một BA/Product Owner thực thụ cần hỏi Marketing, Finance, Legal, CSKH, Vận hành rạp **trước khi** chốt business rule — vì nhiều quyết định trong Promotion Service ảnh hưởng trực tiếp đến doanh thu, không thể tự suy đoán kỹ thuật. **(B)** Bộ Business Rules đầy đủ, đánh mã (BR-xxx) để dev/QA có thể trực tiếp implement và viết test case, được suy luận dựa trên chính sách thực tế đã công bố công khai của CGV/Galaxy (Happy Day, vé tặng, chính sách không hoàn/huỷ vé...) và điền các khoảng trống bằng giả định hợp lý (**đánh dấu 🟡 ASSUMPTION** — cần Marketing/Legal xác nhận lại trước khi lên production).

---

# PHẦN A — BỘ CÂU HỎI KHẢO SÁT YÊU CẦU (DISCOVERY QUESTIONS)

## A1. Campaign — Vòng đời & Ngân sách

1. Ai có quyền **tạo** campaign? Ai có quyền **duyệt**? Có phân cấp phê duyệt theo giá trị ngân sách không (VD < 50 triệu: Marketing Manager duyệt; > 50 triệu: cần thêm CFO/Giám đốc vùng)?
2. Khi campaign **hết ngân sách giữa chừng** lúc khách đang thanh toán (race condition), hệ thống nên: (a) từ chối giao dịch cuối cùng, hay (b) vẫn cho qua nếu đã "giữ chỗ" ngân sách trước đó? Ngưỡng chấp nhận vượt ngân sách bao nhiêu % là "chấp nhận được" về mặt kế toán?
3. Một campaign có được phép **sửa** sau khi đã ACTIVE và đã có giao dịch không? Nếu sửa `discount_value`, các giao dịch **đã redeem trước đó** có bị ảnh hưởng hồi tố không, hay chỉ áp dụng cho giao dịch mới (forward-only)?
4. Campaign có thể áp dụng đồng thời cho **nhiều rạp với mức giảm khác nhau theo khu vực** không (VD Hà Nội/HCM giảm ít hơn tỉnh lẻ do chi phí mặt bằng)?
5. Có cần cơ chế **"khoá campaign khẩn cấp"** (kill switch) khi phát hiện lỗi cấu hình đang gây thất thoát, và ai có quyền bấm nút đó (on-call engineer hay chỉ Marketing Manager)?
6. Ngân sách Campaign tính theo **tiền mặt thực tế bị giảm**, hay tính theo **doanh thu cơ hội mất đi** (opportunity cost, VD vé tặng 0đ tính theo giá vé gốc)?

## A2. Coupon & Voucher

7. Coupon có được phép **chuyển nhượng** giữa các tài khoản không (VD tặng bạn bè)?
8. Với mã giới hạn `max_redemptions_per_user = 1`, định nghĩa "1 user" là theo `userId`, theo **số điện thoại**, hay theo **thiết bị/CCCD** (chống 1 người tạo nhiều tài khoản để dùng nhiều lần)?
9. Voucher hết hạn không dùng thì xử lý thế nào — có **thông báo nhắc trước X ngày** không?
10. Vé tặng (voucher 0đ) có áp dụng được cho **suất chiếu sớm, suất chiếu đặc biệt (IMAX/4DX/ScreenX), ngày Lễ/Tết** không? (Theo thực tế Galaxy: **không** áp dụng cho suất chiếu sớm.)
11. Một giao dịch có được **dùng nhiều voucher cùng lúc** không, hay giới hạn tối đa mấy voucher/đơn (thực tế Galaxy giới hạn 1 voucher/giao dịch tại quầy)?
12. Voucher có áp dụng được cho **vé nhóm/vé đoàn (B2B)** không, hay chỉ dành cho giao dịch cá nhân (thực tế Galaxy: chính sách thành viên không áp dụng cho Co-Sales/vé đoàn)?

## A3. Membership Tier & Điểm (ranh giới với score-service)

13. Hạng thành viên được tính lại theo **năm dương lịch cố định** hay theo **12 tháng lăn (rolling)** kể từ ngày đăng ký?
14. Khi user bị **hạ hạng** (do không đủ chi tiêu năm sau), các voucher/quyền lợi đã cấp theo hạng cũ có bị thu hồi không, hay giữ nguyên đến khi hết hạn tự nhiên?
15. Giao dịch có **dùng coupon/voucher giảm giá** thì có được tính vào "tổng chi tiêu" để xét hạng không? (Theo thực tế CGV: **không** — chỉ giao dịch nguyên giá mới được tính tích lũy chi tiêu/vé.)
16. Điểm tích lũy có **hết hạn** không, sau bao lâu (CGV/Galaxy thường theo chu kỳ 12 tháng)? — câu hỏi này thuộc score-service nhưng Promotion cần biết để thiết kế UI cảnh báo "điểm sắp hết hạn, đổi ngay".
17. Khi Promotion yêu cầu score-service **hold điểm** nhưng score-service timeout/down, có cho phép khách **thanh toán phần còn lại bằng tiền mặt/thẻ** thay vì chặn toàn bộ giao dịch không?

## A4. Stacking, Ưu tiên & Xung đột

18. Trong trường hợp khách vừa là **VIP** vừa có **coupon Happy Wednesday** vừa có **voucher sinh nhật** cho cùng 1 giao dịch — thứ tự ưu tiên áp dụng chính xác là gì? Có cho cộng dồn cả 3 không, hay chỉ 1 được chọn?
19. Nếu một khuyến mãi làm giá vé về **0đ hoặc âm** (voucher tiền mặt lớn hơn giá vé), hệ thống xử lý phần dư thế nào — mất, hay giữ lại dùng cho giao dịch sau (không thực tế nhưng cần chốt rõ)?
20. Combo (vé + bắp nước) có tính khuyến mãi **riêng cho từng thành phần**, hay tính trên tổng combo? (VD giảm 20% chỉ áp dụng cho vé, không áp dụng cho bắp nước.)

## A5. Đặt vé, Thanh toán, Hoàn/Huỷ

21. Theo chính sách thực tế đa số rạp VN (**vé đã thanh toán không hoàn/huỷ**) — vậy khuyến mãi/coupon/voucher đã redeem trên vé đó có được **hoàn lại lượt dùng** trong bất kỳ trường hợp nào không (VD lỗi hệ thống, huỷ suất chiếu do sự cố kỹ thuật của rạp)?
22. Nếu rạp **chủ động huỷ suất chiếu** (cháy máy chiếu, sự cố...), coupon/voucher đã dùng có được **hoàn lại tự động** vào ví khách không, hay CSKH xử lý thủ công từng case?
23. Có cho phép **đổi suất chiếu** (không huỷ, chỉ đổi giờ/ngày trong cùng rạp) khi vé đã áp dụng khuyến mãi không? Khuyến mãi có tiếp tục hợp lệ cho suất chiếu mới không (VD coupon chỉ áp dụng thứ 4 nhưng đổi sang thứ 5)?

## A6. Gian lận & Bảo mật

24. Ngưỡng nào được coi là "gian lận" — bao nhiêu lần thử coupon sai trong bao lâu thì khoá tài khoản/IP tạm thời?
25. Khi phát hiện gian lận **sau khi** giao dịch đã hoàn tất (vé đã xuất), có thu hồi vé/report công an, hay chỉ khoá tài khoản và không cấp khuyến mãi tương lai?
26. Nhân viên nội bộ (quầy vé, CSKH) có được cấp quyền **tự ý cấp voucher đền bù** không giới hạn không, hay bắt buộc có ngưỡng phê duyệt và audit định kỳ để tránh nhân viên lạm quyền (insider fraud)?

## A7. Pháp lý

27. Doanh nghiệp đã có quy trình nội bộ **nộp thông báo khuyến mại lên Sở Công Thương** chưa? Ai (phòng ban nào) chịu trách nhiệm nộp, và Promotion Service có cần **chặn cứng** campaign chưa có `legal_notification_ref` hay chỉ cảnh báo?
28. Có chương trình khuyến mãi nào cần giới hạn riêng theo nhà phát hành phim hoặc suất chiếu đặc biệt không?

## A8. Vận hành đa kênh (Omnichannel)

29. Khuyến mãi có áp dụng **giống nhau giữa Web, App, quầy vé tại rạp, và kiosk tự phục vụ** không, hay có sự khác biệt (VD một số coupon chỉ áp dụng online)?
30. Khi mạng tại rạp bị gián đoạn (quầy vé offline tạm thời), có cơ chế **fallback áp dụng khuyến mãi ngoại tuyến** (offline-first) rồi đồng bộ lại sau không, hay bắt buộc phải online 100%?

---

# PHẦN B — BUSINESS RULES ĐẦY ĐỦ

## B1. Campaign Rules

| Mã | Business Rule |
|---|---|
| BR-CAMP-01 | Một Campaign chỉ chuyển từ `SCHEDULED` sang `ACTIVE` khi: `current_time >= start_date` VÀ `legal_check_status = PASSED` VÀ `budget_total > 0`. |
| BR-CAMP-02 | Campaign tự động chuyển sang `PAUSED` (không phải `COMPLETED`) khi `budget_used >= budget_total * 0.98` (ngưỡng 98% để tránh lệch giữa Redis counter và DB trong lúc reconcile) — Admin phải xác nhận thủ công để `COMPLETED` hoặc nạp thêm ngân sách. |
| BR-CAMP-03 | Campaign có `budget_total > 50.000.000 VNĐ` 🟡*ASSUMPTION — cần Finance xác nhận ngưỡng* bắt buộc phải qua bước duyệt cấp 2 (role `FINANCE_DIRECTOR`), không chỉ `MARKETING_MANAGER`. |
| BR-CAMP-04 | Sau khi Campaign đã có ít nhất 1 giao dịch `CONFIRMED`, các trường `discount_type`, `discount_value`, `conditions` **không được sửa trực tiếp** — mọi thay đổi tạo ra bản ghi `CampaignRule` phiên bản mới với `effective_from = now()`, giao dịch cũ giữ nguyên rule đã áp dụng tại thời điểm đó (forward-only, không hồi tố). |
| BR-CAMP-05 | Người tạo Campaign (`created_by`) **không được** là người duyệt (`approved_by`) cùng một campaign — 4-eyes principle bắt buộc ở tầng database constraint, không chỉ ở UI. |
| BR-CAMP-06 | Promotion dạng phần trăm phải có `0 < discount_value <= 100`; đây là validation dữ liệu, không tạo thêm ngưỡng giá trị đơn. |
| BR-CAMP-07 | Có "kill switch" cấp Admin: campaign có thể bị chuyển `PAUSED` ngay lập tức bởi role `SYSTEM_ADMIN` hoặc `MARKETING_MANAGER` bất kể trạng thái hiện tại (trừ `COMPLETED`/`CANCELLED`), hiệu lực tức thời qua invalidate cache Redis `promo:campaign:active` trong vòng ≤ 5 giây. |
| BR-CAMP-08 | Campaign áp dụng mức giảm khác nhau theo khu vực/rạp (`applies_to.cinemaIds`) phải khai báo rõ, không được để trống (mặc định trống = áp dụng toàn quốc, phải là lựa chọn tường minh, không phải giá trị ngầm định gây nhầm lẫn). |

## B2. Coupon Rules

| Mã | Business Rule |
|---|---|
| BR-COUP-01 | Một `userId` chỉ được redeem thành công một `couponCode` cụ thể tối đa `max_redemptions_per_user` lần (mặc định = 1). Định danh chống trùng dùng **cả `userId` lẫn số điện thoại đã xác thực** 🟡*ASSUMPTION* để hạn chế tạo tài khoản ảo. |
| BR-COUP-02 | Coupon **không được chuyển nhượng** giữa các `userId` khác nhau trừ khi thuộc loại `campaign.type = GIFT_TRANSFERABLE` được đánh dấu tường minh khi tạo campaign. |
| BR-COUP-03 | Khi `redemptions_count` đạt `max_redemptions` (hết lượt toàn hệ thống), request redeem tiếp theo trả lỗi `COUPON_EXHAUSTED` ngay tại bước `validate`, không cho vào `reserve`. |
| BR-COUP-04 | Redeem coupon giới hạn số lượng thấp (`max_redemptions < 1000`) bắt buộc dùng Redis atomic `DECRBY` có kiểm tra floor = 0 hoặc DB row lock — cấm dùng "check-then-act" không có lock (race condition kinh điển). |
| BR-COUP-05 | Số lần nhập sai coupon liên tiếp từ cùng `userId`/IP vượt **5 lần trong 10 phút** 🟡*ASSUMPTION* → tạm khoá thử coupon 15 phút, ghi nhận vào `fraud-detection-module`. |
| BR-COUP-06 | Coupon dạng `SINGLE_USE` (sinh hàng loạt cho sự kiện) tự động chuyển `status = USED` ngay khi `CONFIRMED`, không thể redeem lại kể cả khi giao dịch sau đó bị hoàn (vì chính sách vé không hoàn/huỷ — xem BR-ROLLBACK-04). |
| BR-COUP-07 | Coupon revoke bởi Admin (`POST /admin/coupons/{code}/revoke`) không ảnh hưởng các `CouponRedemption` đã `CONFIRMED` trước đó — chỉ chặn redeem mới. |

## B3. Voucher Rules

| Mã | Business Rule |
|---|---|
| BR-VOU-01 | Vé tặng (voucher 0đ, nguồn `BIRTHDAY`/`TIER_UPGRADE`) **không áp dụng** cho: suất chiếu sớm (trước ngày công chiếu chính thức), suất chiếu đặc biệt (IMAX/4DX/ScreenX/Gold Class), ngày Lễ/Tết, và các phim thuộc diện `isPremiere = true` từ movie-service. |
| BR-VOU-02 | Voucher sinh nhật có hạn sử dụng **4 tháng** 🟡*theo thực tế Galaxy* kể từ ngày phát hành (`issued_at + 120 ngày`), tự động chuyển `EXPIRED` sau đó, không gia hạn. |
| BR-VOU-03 | Customer chỉ được gửi **tối đa 1 lựa chọn thủ công**: voucher trong ví, coupon hoặc một system promotion cụ thể. Runtime được áp dụng tối đa 1 voucher/coupon cùng tối đa 1 AUTO promotion khi cả hai promotion và cả hai campaign đều bật `stackable`; mặc định không cộng dồn. `allowMultipleVoucherPerOrder` chỉ còn là metadata tương thích dữ liệu cũ. |
| BR-VOU-04 | Voucher **không được quy đổi thành tiền mặt** dưới bất kỳ hình thức nào (không hoàn tiền chênh lệch nếu giá trị voucher lớn hơn giá vé). |
| BR-VOU-05 | Engine luôn bảo vệ mức giá tốt nhất: so sánh voucher/coupon customer chọn với AUTO tốt nhất, chỉ cộng dồn khi BR-VOU-03 cho phép và chọn tổ hợp có tổng giảm cao nhất. Nếu AUTO tốt hơn lựa chọn thủ công thì không reserve/consume voucher hoặc coupon đó; checkout phải thông báo rõ cho customer. |
| BR-VOU-06 | Voucher chính sách thành viên **không áp dụng cho giao dịch vé nhóm/vé đoàn B2B** (Co-Sales) — Eligibility Engine phải kiểm tra `orderType != GROUP_BOOKING` trước khi cho áp dụng voucher cá nhân. |
| BR-VOU-07 | Voucher rách/hỏng/quá hạn tại quầy: nhân viên quét mã không hợp lệ → hệ thống trả lỗi rõ ràng `VOUCHER_EXPIRED`/`VOUCHER_INVALID`, không cho override thủ công trừ khi có quyền `CSKH_AGENT` + lý do ghi nhận vào audit log. |

## B4. Discount Rule (tự động)

| Mã | Business Rule |
|---|---|
| BR-DISC-01 | "Happy Wednesday": giá vé cố định (VD 55.000–70.000 VNĐ tuỳ khu vực) áp dụng **chỉ cho vé 2D, ghế Standard**, loại trừ IMAX/4DX/ScreenX, loại trừ ngày Lễ/Tết trong `excludeDates`. |
| BR-DISC-02 | "Culture Day" (giảm 20% cho trẻ em/người cao tuổi): yêu cầu `requiresVerification = true` — hệ thống chỉ **cho phép áp dụng tại quầy** (nhân viên xác minh giấy tờ trực tiếp), **không cho áp dụng qua kênh online tự động** trừ khi có cơ chế xác thực giấy tờ số hoá riêng. |
| BR-DISC-03 | Giảm giá theo hạng thành viên (`tier_benefit_mapping`) chỉ áp dụng cho **vé cá nhân nguyên giá**, không cộng dồn với vé đã ở diện Happy Day/Culture Day trừ khi rule tường minh cho phép (mặc định: chọn mức giảm có lợi hơn cho khách, không cộng dồn cả hai — theo Priority Engine ở BR-STACK). |
| BR-DISC-04 | Giá vé suất sáng sớm (`isEarlyMorning = true`, đọc từ movie-service) được giảm cố định 🟡*ASSUMPTION mức % cụ thể do Marketing quyết định*, không cộng dồn thêm với Happy Wednesday nếu trùng ngày (loại trừ lẫn nhau, chọn mức lợi hơn). |
| BR-DISC-05 | Giao dịch có sử dụng **bất kỳ hình thức khuyến mại/coupon/voucher** nào **không được tính vào "tổng chi tiêu tích luỹ"** để xét hạng thành viên — kể cả khi chỉ một phần vé trong đơn hàng được giảm giá, chỉ những vé nguyên giá trong cùng đơn mới được tính (theo đúng chính sách CGV thực tế). |
| BR-DISC-06 | Nếu đơn hàng có **vé 0đ (vé tặng)** lẫn với **vé trả nguyên giá** trong cùng giao dịch, chỉ số vé trả nguyên giá được tính vào tổng vé tích luỹ; vé 0đ không tính doanh thu, không tính vé tích luỹ. |

## B5. Stacking / Priority / Conflict Resolution Rules

| Mã | Business Rule |
|---|---|
| BR-STACK-01 | Engine đánh giá lựa chọn thủ công và toàn bộ AUTO hợp lệ, sau đó chọn phương án giảm nhiều nhất: một benefit đơn hoặc tổ hợp tối đa **1 manual + 1 AUTO** theo BR-VOU-03. Point Redemption được xử lý sau promotion. |
| BR-STACK-02 | **Không cho phép cộng dồn 2 Coupon phần trăm** trên cùng một giao dhàng, dù đến từ 2 campaign khác nhau — chỉ 1 coupon phần trăm được áp dụng theo Priority Engine. |
| BR-STACK-03 | Voucher tiền mặt chỉ được cộng dồn với tối đa một AUTO khi promotion-level và campaign-level `stackable` của cả hai phía đều bật. Campaign `exclusiveCampaign=true` không được ghép với campaign khác. |
| BR-STACK-04 | Khi 2 rule có cùng `priority`, hệ thống **luôn chọn phương án giảm nhiều tiền hơn cho khách** (customer-friendly default), trừ khi rule đánh dấu `forced: true`. |
| BR-STACK-05 | Nếu tổng mức giảm cộng dồn (Discount + Coupon + Voucher) làm giá vé về **≤ 0**, hệ thống chặn ở mức **giá tối thiểu = 0đ** (không cho âm), phần chênh lệch dư **bị mất, không hoàn lại, không chuyển sang giao dịch khác**. |
| BR-STACK-06 | Voucher/Coupon loại "miễn phí vé" (100% giảm) không được **cộng dồn với Point Redemption** trên cùng vé đó — nếu vé đã 0đ, hệ thống tự động khoá tuỳ chọn "dùng điểm" cho vé đó ở UI (tránh khách lãng phí điểm không cần thiết). |
| BR-STACK-07 | Combo (vé + bắp nước): khuyến mãi giảm giá vé **không tự động áp dụng cho phần bắp nước** trừ khi campaign khai báo tường minh `appliesTo: ["TICKET", "CONCESSION"]`. Mặc định combo tính khuyến mãi riêng theo từng dòng sản phẩm (line-item discount), không phải theo tổng combo. |

## B6. Eligibility Validation Rules

| Mã | Business Rule |
|---|---|
| BR-ELIG-01 | Thứ tự kiểm tra eligibility là **fail-fast**: (1) trạng thái campaign/coupon/voucher → (2) đối tượng người dùng (hạng, tuổi, mới/cũ) → (3) điều kiện giỏ hàng → (4) điều kiện suất chiếu → (5) kênh & thanh toán → (6) gian lận → (7) trần pháp lý. Dừng ngay ở bước đầu tiên không đạt, trả lý do cụ thể, không chạy tiếp các bước sau. |
| BR-ELIG-02 | Với điều kiện yêu cầu xác minh giấy tờ (trẻ em/người cao tuổi/học sinh-sinh viên), hệ thống **online không tự động cho ELIGIBLE** — trả về trạng thái `PENDING_VERIFICATION`, chỉ chuyển `ELIGIBLE` sau khi nhân viên quầy xác nhận qua Admin/POS API. |
| BR-ELIG-03 | Campaign gắn với phim cụ thể (`movieId`) chỉ ELIGIBLE nếu suất chiếu thuộc đúng `movieId` VÀ đúng định dạng (`format`) khai báo trong rule — không suy luận theo tên phim tương tự. |
| BR-ELIG-04 | Nếu ACL call sang score-service để lấy `tierLevel` timeout (> 500ms) hoặc lỗi, Eligibility Engine coi user thuộc hạng **thấp nhất mặc định (non-member)** 🟡*ASSUMPTION — cần Marketing xác nhận có chấp nhận được không, hay nên chặn hẳn giao dịch dùng tier-benefit lúc đó*, không được tự ý gán hạng cao hơn thực tế (nguyên tắc an toàn tài chính: thà từ chối nhầm còn hơn giảm giá nhầm). |

## B7. Redemption, Reservation & Rollback Rules

| Mã | Business Rule |
|---|---|
| BR-REDEEM-01 | Trạng thái Reservation (`HELD`) có TTL **bằng đúng** TTL giữ ghế của booking-service (không tự đặt TTL riêng lệch nhau), để tránh trường hợp ghế hết hạn nhưng khuyến mãi vẫn giữ (hoặc ngược lại). |
| BR-REDEEM-02 | `confirm()` chỉ được gọi **đúng một lần** cho mỗi `orderId` (idempotent) — gọi lần 2 với cùng `orderId` trả về kết quả đã xử lý trước đó, không redeem trùng, không trừ ngân sách 2 lần. |
| BR-ROLLBACK-01 | Rollback chỉ được thực hiện khi Reservation đang ở trạng thái `HELD` — **không được rollback một Reservation đã `CONFIRMED`** (vì vé đã xuất, theo chính sách "vé đã thanh toán không hoàn/huỷ" của ngành rạp chiếu phim VN). |
| BR-ROLLBACK-02 | Nếu payment thất bại/timeout **trước khi** vé được xuất, rollback giải phóng đầy đủ: coupon/voucher trở lại trạng thái khả dụng, ngân sách campaign được hoàn lại, point-hold (nếu có) gọi `release` sang score-service. |
| BR-ROLLBACK-03 | Sau khi vé đã `CONFIRMED` (thanh toán thành công, vé đã xuất), **không có API nào trong Promotion Service được phép tự động hoàn coupon/voucher/điểm** — mọi trường hợp ngoại lệ (rạp huỷ suất chiếu do sự cố kỹ thuật) phải qua quy trình **CSKH thủ công có phê duyệt** (`POST /admin/vouchers/issue` với `source = COMPENSATION`), không phải rollback tự động của hệ thống. |
| BR-ROLLBACK-04 | Coupon loại `SINGLE_USE` đã chuyển `status = USED` **không được** khôi phục lại kể cả khi giao dịch liên quan sau đó bị huỷ bởi quy trình CSKH thủ công — phát hành voucher đền bù mới thay vì tái sử dụng coupon cũ (tránh lỗi kế toán đối soát coupon 2 lần). |
| BR-REDEEM-03 | Reservation quá hạn TTL mà chưa được `confirm()`/`rollback()` tường minh (do lỗi mất event) sẽ bị **job self-healing tự động rollback** trong vòng ≤ 2 phút kể từ khi hết hạn — không được để Reservation "treo" vô thời hạn gây khoá tài nguyên (lượt coupon, ngân sách). |
| BR-REDEEM-04 | Quota mỗi khách được đếm theo từng `promotionPublicId`. `campaign.maxRedemptionsPerUser` là trần cho mỗi promotion trong campaign, không cộng lượt của promotion gốc và các clone; giới hạn hiệu lực là `min(promotion.maxRedemptionsPerUser, campaign.maxRedemptionsPerUser)`. `campaign.maxRedemptions` và ngân sách vẫn dùng chung toàn chiến dịch. |

## B8. Point / Tier Integration Boundary Rules

| Mã | Business Rule |
|---|---|
| BR-POINT-01 | Promotion Service **không bao giờ** tự trừ/cộng điểm trong database của mình — mọi thay đổi số dư điểm bắt buộc đi qua API `hold/commit/release` của score-service. |
| BR-POINT-02 | Khi score-service không khả dụng (circuit breaker OPEN), Promotion **vẫn cho phép** hoàn tất các khuyến mãi không phụ thuộc điểm/hạng (Coupon, Voucher, Discount cố định theo lịch), chỉ **tạm khoá** tuỳ chọn "dùng điểm đổi ưu đãi" với thông báo rõ ràng cho khách, không chặn toàn bộ luồng thanh toán. |
| BR-POINT-03 | `point-hold` phải có `expires_at` — nếu quá hạn mà chưa `commit`, tự động `release` phía score-service; Promotion phải đảm bảo TTL phía mình **ngắn hơn hoặc bằng** TTL hold phía score-service để tránh lệch trạng thái. |
| BR-POINT-04 | Voucher phát hành từ hình thức "đổi điểm" (`source = POINT_REDEEM`) chỉ được tạo **sau khi** score-service xác nhận `commit` thành công — không phát voucher trước rồi trừ điểm sau (tránh rủi ro phát voucher miễn phí nếu bước trừ điểm thất bại). |

## B9. Fraud Detection Rules

| Mã | Business Rule |
|---|---|
| BR-FRAUD-01 | Một `deviceId`/IP thực hiện **> 10 giao dịch redeem coupon khác `userId` trong 1 giờ** 🟡*ASSUMPTION* → tự động gắn cờ `fraud.alert`, không tự động chặn (tránh chặn nhầm khách hàng thật ở khu vực mạng chung như KTX/văn phòng), chuyển cho đội Vận hành review. |
| BR-FRAUD-02 | Tài khoản mới đăng ký (< 24 giờ) redeem coupon "khách hàng mới" giá trị cao (> ngưỡng cấu hình) ngay lập tức → yêu cầu xác thực bổ sung (OTP lần 2) trước khi `confirm`. |
| BR-FRAUD-03 | Nhân viên (`CSKH_AGENT`) phát voucher đền bù vượt quá `giá trị ngưỡng/tháng` 🟡*ASSUMPTION cần Finance chốt số* phải qua phê duyệt cấp quản lý, có báo cáo định kỳ hằng tháng để phát hiện lạm quyền nội bộ (insider fraud). |

## B10. Legal Compliance Rules (Pháp luật Việt Nam)

| Mã | Business Rule |
|---|---|
| BR-LEGAL-01 | Runtime không áp trần giảm 50%. Percentage tối đa 100%; fixed/full discount được chặn ở `originalAmount`, vì vậy final amount tối thiểu là 0đ. Điều kiện giá trị đơn chỉ lấy từ `minimumOrderAmount` do admin cấu hình. |
| BR-LEGAL-02 | Hệ thống theo dõi **tổng số ngày trong năm dương lịch** mà một loại vé/dịch vụ được áp dụng chương trình giảm giá — cảnh báo Marketing khi đạt **100/120 ngày** (ngưỡng cảnh báo sớm), chặn cứng khi đạt 120 ngày trừ trường hợp thuộc khuyến mại tập trung do Nhà nước tổ chức. |
| BR-LEGAL-03 | Các Campaign thuộc diện phải thông báo Sở Công Thương **không được** chuyển sang `ACTIVE` nếu thiếu `legal_notification_ref` VÀ `start_date` cách ngày submit hồ sơ **dưới 3 ngày làm việc**. |
| BR-LEGAL-04 | Rượu, thuốc lá không được dùng làm hàng hoá khuyến mại — nếu combo bắp nước có đồ uống có cồn (trường hợp đặc biệt tại một số rạp cao cấp), `legal-compliance-module` phải chặn combo đó khỏi mọi hình thức khuyến mại giảm giá/tặng kèm. |

---

## Ghi chú sử dụng tài liệu này

- Mọi rule đánh dấu **🟡 ASSUMPTION** là giả định kỹ thuật hợp lý dựa trên khảo sát thực tế ngành, **bắt buộc phải được Marketing/Finance/Legal xác nhận lại bằng số liệu chính thức của doanh nghiệp** trước khi đưa vào production — đây chính là mục đích của Phần A (Bộ câu hỏi khảo sát).
- Khuyến nghị: tổ chức 1 buổi **Business Rule Workshop** với đại diện Marketing, Finance, Legal, CSKH, Vận hành rạp, dùng trực tiếp Phần A làm agenda — mỗi câu trả lời sẽ xác nhận hoặc điều chỉnh rule tương ứng ở Phần B, sau đó review lại `promotion-service-plan.md` (mục 15–23) để đồng bộ Rule Engine.
- Mỗi Business Rule ở Phần B nên được ánh xạ trực tiếp thành **1 test case** trong Testing Strategy (mục 54 của kế hoạch chính) trước khi release.

## Runtime status note (2026-07-31)

Rules in this document include business assumptions that still require
Marketing/Finance/Legal confirmation. Implemented runtime behavior is narrower:
one benefit per checkout, no automatic rule discovery/stacking, and no Score
point mutation. Campaign legal/approval gates, percentage validation up to 100%,
reservation TTL/idempotency and budget enforcement are implemented in
Promotion. Partner-funded promotions and settlement are out of scope.
Cross-service eligibility is accepted only from a trusted caller
until User/Movie/Score contracts are connected.
