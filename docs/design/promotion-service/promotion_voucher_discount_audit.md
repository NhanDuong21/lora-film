# Báo cáo rà soát Promotion / Voucher / Discount

**Phạm vi:** mã nguồn trong `features.zip` và `server.zip`
**Ngày rà soát:** 2026-08-01
**Cách thực hiện:** static code review xuyên suốt frontend, Promotion Service, Booking Service, Payment Service, vòng đời booking/refund, scheduler, database migration và test hiện có.

> Lưu ý tại thời điểm audit ban đầu: môi trường cung cấp cho lượt rà soát chưa chạy được Maven/frontend, nên kết luận ban đầu chủ yếu dựa trên static review. Phần **9. Kết quả khắc phục** ở cuối tài liệu là trạng thái mới nhất sau khi refactor và chạy lại test trực tiếp trên workspace đầy đủ.

---

## 1. Kết luận điều hành

Kiến trúc hiện tại đã có các thành phần đúng hướng: catalog Campaign/Promotion, ví voucher, preview, reserve, confirm, release, budget reservation, idempotency và khóa pessimistic. Tuy nhiên, **nguồn sự thật của quyết định giảm giá chưa được khóa xuyên suốt từ Booking đến Payment**, và một số cấu hình quan trọng được lưu nhưng không được engine thực thi.

### Nhóm cần sửa ngay

1. Có thể nhận ưu đãi theo một phương thức thanh toán rồi thanh toán bằng phương thức khác.
2. Điều kiện “tài khoản đã xác minh” đang luôn đúng cho mọi người dùng đăng nhập.
3. `exclusiveCampaign`, `campaign.stackable`, `campaign.priority` không được engine áp dụng.
4. Giới hạn chiến dịch bị tính riêng theo loại promotion và còn có thể bị vượt khi nhiều promotion cùng được áp dụng trong một đơn.
5. Chọn voucher/coupon thủ công làm biến mất toàn bộ AUTO promotion, kể cả các promotion được phép cộng dồn.
6. Refund không có nghiệp vụ hoàn lại voucher/lượt dùng/ngân sách đã confirm.
7. Ledger redemption lưu số tiền sai ngữ nghĩa khi có nhiều promotion cộng dồn.
8. Promotion/campaign có thể bị sửa hoặc soft-delete làm sai lịch sử và phá tham chiếu wallet/reservation.

---

## 2. Luồng hiện tại

1. Frontend tải ví voucher và promotion hệ thống.
2. Customer chọn tối đa một voucher/system promotion hoặc nhập coupon.
3. Booking Service tạo `contextJson`, gọi Promotion Service preview.
4. Khi finalize, Booking Service gọi preview lần nữa rồi reserve promotion.
5. Promotion Service khóa campaign/promotion/wallet, tạo reservation và redemption, giữ `budgetReserved`.
6. Booking khóa `finalAmount`.
7. Payment Service nhận một `paymentMethod` mới từ request, lấy số tiền đã khóa từ Booking và tạo giao dịch.
8. Khi payment success, Booking Service confirm promotion reservation, sau đó confirm booking.
9. Booking cancelled/expired thì release reservation chưa confirm; refund không đảo promotion đã confirm.

Điểm yếu cốt lõi là bước 3 và bước 7 không cùng một quyết định thanh toán bất biến do server sở hữu.

---

# 3. Phát hiện chi tiết

## CRITICAL-01 — Bypass điều kiện phương thức thanh toán

**Hiện trạng**

- Booking đưa `paymentMethod` từ request checkout vào điều kiện promotion:
  `booking-service/.../BookingServiceImpl.java:1382-1396`
- Promotion engine dùng trực tiếp giá trị đó:
  `promotion-service/.../PromotionConditionEvaluator.java:118-119`
- Sau khi promotion được reserve, Booking khóa số tiền:
  `BookingServiceImpl.java:1256-1315`
- Payment Service lại nhận `paymentMethod` độc lập từ request mới:
  `payment-service/.../CreatePaymentRequest.java:7-15`
  `payment-service/.../PaymentServiceImpl.java:95-124`
- `BookingPaymentContext` không chứa phương thức đã khóa hoặc phương thức đủ điều kiện:
  `payment-service/.../BookingPaymentContext.java:6-16`
- Payment callback chỉ kiểm tra amount/currency, không đối chiếu provider với điều kiện promotion:
  `booking-service/.../InternalBookingPaymentServiceImpl.java:448-469, 783-800`

**Kịch bản khai thác**

1. Finalize checkout với `paymentMethod=MOMO` để nhận promotion chỉ dành cho MOMO.
2. Gọi Payment API trực tiếp với `paymentMethod=VNPAY` hoặc provider khác.
3. Hệ thống vẫn thu đúng `finalAmount` đã giảm và confirm promotion.

Frontend hiện gửi cùng một phương thức ở cả hai request, nhưng API backend không bắt buộc điều đó nên có thể bị bypass bằng DevTools/Postman/script.

**Trường hợp đặc biệt nguy hiểm**

Nếu promotion làm đơn hàng về 0 đồng, `confirmFreeCheckout()` xác nhận reservation bằng `PROMOTION_SERVICE/FULL_DISCOUNT`, hoàn toàn không có giao dịch ở provider đã được dùng làm điều kiện:
`BookingServiceImpl.java:1470-1485`.

**Khắc phục bắt buộc**

- Booking phải lưu `lockedPaymentMethod` hoặc `eligiblePaymentProviders` cùng amount lock.
- `BookingPaymentContext` phải trả trường này cho Payment Service.
- Payment Service từ chối provider không khớp.
- Callback phải đối chiếu provider thực tế với provider đã khóa.
- Với đơn 0 đồng, promotion có điều kiện provider phải bị từ chối hoặc phải có chính sách rõ ràng không yêu cầu payment provider.
- Tốt hơn: tạo server-owned payment intent trước, rồi dùng provider từ intent để reserve promotion; không dùng provider do client tự khai hai lần.

---

## HIGH-01 — Điều kiện xác minh danh tính luôn được coi là đúng

**Bằng chứng**

`BookingServiceImpl.java:1392` luôn thực hiện:

```java
context.put("identityVerified", true);
```

Promotion engine tin giá trị này tại:
`PromotionConditionEvaluator.java:140-142`.

**Tác động**

Mọi tài khoản đăng nhập đều vượt qua promotion có `requiresVerification=true`, bất kể tài khoản đã KYC/xác minh email/số điện thoại hay chưa.

**Khắc phục**

- Lấy trạng thái xác minh từ User/Auth Service hoặc claim đã ký đáng tin cậy.
- Xác định rõ “verified” là xác minh gì.
- Không lấy được dữ liệu thì fail closed, không mặc định `true`.

---

## HIGH-02 — Cấu hình cấp Campaign không được Promotion Engine thực thi

Các trường sau được lưu, sửa và trả ra API:

- `PromotionCampaign.priority`
- `PromotionCampaign.stackable`
- `PromotionCampaign.exclusiveCampaign`

Nhưng runtime engine chỉ đọc `Promotion.stackable` và `Promotion.priority`:
`PromotionEngineService.java:354-380, 455-476, 618`.

Tìm kiếm toàn bộ runtime không thấy `getExclusiveCampaign()` được dùng và không thấy campaign-level stackable/priority tham gia lựa chọn.

**Tác động**

Admin có thể cấu hình chiến dịch độc quyền hoặc không cộng dồn nhưng checkout vẫn ghép promotion trái cấu hình. Đây là lỗi “UI/API nói một đằng, engine chạy một nẻo”.

**Khắc phục**

Trong `compatible()` và thuật toán sắp hạng phải áp dụng:

- `exclusiveCampaign=true`: không được ghép với promotion từ campaign khác.
- `campaign.stackable=false`: chặn tổ hợp theo chính sách đã định nghĩa.
- Campaign priority phải là một phần của tie-breaker, sau đó mới đến promotion priority.

---

## HIGH-03 — Giới hạn Campaign bị chia nhỏ theo Promotion Type

`PromotionEngineService.requireCapacity()` dùng:

- `countCampaignRedemptionsByPromotionType(...)`
- `countCampaignUserRedemptionsByPromotionType(...)`

Tại `PromotionEngineService.java:536-546`.

Repository đã có sẵn hàm đếm toàn campaign nhưng không dùng:
`PromotionRedemptionRepository.java:41-51, 67-79`.

**Tác động**

Campaign `maxRedemptions=100` có thể cho phép gần:

- 100 AUTO
- 100 VOUCHER
- 100 COUPON

Tương tự, giới hạn một người dùng trên campaign có thể bị vượt qua bằng các loại promotion khác nhau.

**Khắc phục**

- Dùng count toàn campaign cho trường campaign-level.
- Nếu muốn giới hạn từng loại, tạo các field riêng như `maxAutoRedemptions`, `maxVoucherRedemptions`, không tái diễn giải `maxRedemptions`.

---

## HIGH-04 — Có thể vượt hạn mức ngay trong một reservation nhiều promotion

Mỗi candidate được gọi `requireCapacity()` riêng trước khi tổ hợp cuối cùng được chọn:
`PromotionEngineService.java:333-351, 509-553`.

Ví dụ campaign còn 1 lượt nhưng hai promotion cùng campaign đều thấy count hiện tại chưa chạm trần; cả hai cùng được chọn và reservation tạo hai redemption.

**Tác động**

- Vượt `campaign.maxRedemptions`.
- Vượt `campaign.maxRedemptionsPerUser`.
- `redemptionCount` có thể tăng nhiều lần trên cùng một đơn.

**Khắc phục**

Sau khi chọn tổ hợp, dưới cùng transaction/lock phải kiểm tra capacity theo **tổng delta của tổ hợp**:

- theo promotion;
- theo campaign;
- theo user + campaign;
- theo wallet;
- theo ngân sách.

---

## HIGH-05 — Chọn voucher/coupon thủ công làm mất AUTO promotion

Trong `PromotionEngineService.preview()`:

- Nếu không có lựa chọn thủ công, engine tải AUTO candidates.
- Nếu có bất kỳ wallet ID, promotion ID hoặc coupon code, engine không tải AUTO promotion mặc định.

Bằng chứng: `PromotionEngineService.java:84-106`.

**Tác động**

Một voucher/coupon có `stackable=true` vẫn làm mất toàn bộ ưu đãi tự động. Customer có thể nhận tổng giảm thấp hơn chỉ vì chọn voucher.

**Khắc phục**

- Luôn nạp AUTO candidates hợp lệ.
- Thêm manual candidates vào cùng tập.
- Chọn tổ hợp tốt nhất theo compatibility.
- Chỉ tắt AUTO khi request có cờ rõ ràng như `manualOnly=true`.

---

## HIGH-06 — Frontend chỉ cho chọn một promotion dù backend hỗ trợ mảng và stacking

Frontend dùng state `selectedPromotion` số ít. Chọn voucher sẽ xóa coupon; nhập coupon sẽ xóa promotion đã chọn:
`BookingCheckoutPage.jsx:819-853, 925-939`.

Payload finalize chỉ gửi tối đa một wallet và một system promotion trong luồng UI thông thường:
`BookingCheckoutPage.jsx:989-1004`.

**Tác động**

- Tính năng `stackable` gần như không sử dụng được bởi customer.
- Backend và frontend có hai mô hình sản phẩm khác nhau.
- Customer không biết promotion nào xung đột và vì sao.

**Khắc phục**

Chọn một trong hai hướng:

1. Hỗ trợ multi-select thật sự, backend trả compatibility/conflict reason; hoặc
2. Quy định customer chỉ chọn một voucher, AUTO do engine tự ghép, rồi bỏ các UI/config gây hiểu nhầm.

---

## HIGH-07 — Không có nghiệp vụ đảo promotion đã confirm khi refund

Promotion Service không cho release reservation đã confirm:
`PromotionReservationServiceImpl.java:289-309`.

Booking chỉ release promotion khi `CANCELLED` hoặc `EXPIRED`; nhánh `REFUNDED` chỉ refund ticket:
`BookingLifecycleService.java:235-260`.

**Tác động**

Sau refund:

- voucher vẫn bị tính đã dùng;
- usage count vẫn bị tiêu thụ;
- campaign redemption count và budget vẫn bị trừ;
- kể cả khi rạp/hệ thống hủy suất chiếu ngoài lỗi của khách hàng.

**Khắc phục**

Thêm nghiệp vụ append-only `reverseConfirmed`/`compensateConfirmed` với reason code:

- `CUSTOMER_REFUND`: có thể không hoàn voucher tùy chính sách;
- `CINEMA_CANCELLED`, `SYSTEM_ERROR`, `PAYMENT_REVERSED`: thường phải hoàn;
- ghi ledger bù, không sửa/xóa lịch sử cũ.

---

## HIGH-08 — Ledger số tiền sai ngữ nghĩa khi stacking

Mỗi `PromotionRedemption` lưu:

- cùng `originalAmount` của cả reservation;
- `finalAmount = originalAmount - discount của riêng promotion đó`.

Bằng chứng:
`PromotionReservationServiceImpl.java:200-217`.

Với hai promotion, không redemption nào chứa final amount thật của đơn; nếu cộng dữ liệu sẽ double-count gross/final.

**Khắc phục**

Thiết kế lại theo một trong hai mô hình:

- Reservation lưu original/final; redemption chỉ lưu delta discount; hoặc
- Mỗi redemption lưu `sequenceNo`, `amountBefore`, `discountAmount`, `amountAfter`.

---

## HIGH-09 — Promotion template không phải snapshot bất biến

`PromotionMapper.apply()` cho phép thay đổi campaign, action, condition, limits, validity:
`PromotionMapper.java:30-45`.

Redemption chỉ giữ `promotionPublicId`, không giữ immutable `campaignPublicId` hoặc snapshot tên/code/action. Khi confirm, rollback hoặc trả response, service lại tra Promotion hiện tại:
`PromotionReservationServiceImpl.java:547-552, 610-668`.

`PromotionCatalogService.update()` chỉ chặn status ACTIVE, nên promotion đã hết hạn/có lịch sử có thể được sửa và chuyển campaign:
`PromotionCatalogService.java:91-108`.

**Tác động**

- Lịch sử redemption có thể bị “viết lại” theo campaign mới.
- Báo cáo usage của campaign cũ/mới sai.
- Rollback/response phụ thuộc template hiện tại thay vì quyết định lúc mua.

**Khắc phục**

- Snapshot tối thiểu trong redemption: campaign ID, type, code, name, action, priority, discount basis/version.
- Promotion đã có redemption phải immutable; sửa bằng cách tạo version mới.

---

## HIGH-10 — Soft delete có thể phá ví, lịch sử và reservation

Promotion delete chỉ chặn trạng thái ACTIVE:
`PromotionCatalogService.java:185-197`.

Campaign delete chỉ kiểm tra active reservation, không kiểm tra promotion/wallet/history:
`CampaignServiceImpl.java:160-182`.

Trong khi wallet/reservation response vẫn yêu cầu template chưa bị xóa:
`UserPromotionRepository.java:36-67`
`PromotionReservationServiceImpl.java:622-668`.

**Tác động**

- Wallet ALL có thể join promotion đã deleted nhưng mapper không tìm được template.
- Reservation detail/history có thể lỗi “missing promotion template”.
- Audit trail mất tính ổn định.

**Khắc phục**

Không delete thực thể thương mại đã được phát hành/sử dụng. Dùng `ARCHIVED`/`DISABLED`; chỉ xóa draft chưa có bất kỳ tham chiếu nào.

---

## HIGH-11 — Auto-pause có thể bị giữ vĩnh viễn bởi các hold tạm thời

Khi tổng `budgetUsed + budgetReserved` đạt 98%, reserve đặt campaign thành `PAUSED`:
`PromotionReservationServiceImpl.java:725-735`.

Khi reservation hết hạn/release, `budgetReserved` được trả lại nhưng không có auto-resume.

**Kịch bản vận hành/DoS**

Nhiều checkout giữ voucher đến ngưỡng 98% khiến campaign bị pause. Sau khi hold hết hạn, ngân sách đã rảnh nhưng campaign vẫn dừng cho đến khi người vận hành can thiệp.

**Khắc phục**

- Không biến trạng thái business thành PAUSED chỉ do exposure tạm thời; chỉ từ chối reserve mới ở threshold.
- Hoặc auto-resume có hysteresis, ví dụ pause 98%, resume dưới 90%, có reason/audit/metric.

---

## HIGH-12 — “Giới hạn pháp lý 50%” có thể bị vượt qua trong chính mô hình hiện tại

`PromotionPolicyValidator` chỉ giới hạn từng action `PERCENTAGE <= 50`:
`PromotionPolicyValidator.java:14, 44-60`.

Nhưng:

- `FIXED_AMOUNT` có thể tương đương >50%;
- `FULL_DISCOUNT` cho phép 100%;
- hai promotion 50% stackable có thể tạo tổng giảm 100%;
- engine tính phần trăm trên original amount cho từng candidate.

Không kết luận quy định pháp luật hiện hành ở đây; vấn đề là **logic nội bộ tự gọi đây là legal limit nhưng không kiểm tra effective aggregate discount**.

**Khắc phục**

- Theo quyết định nghiệp vụ cuối, bỏ trần giảm 50% khỏi create/update và checkout runtime. `minimumOrderAmount` là ngưỡng giá trị đơn duy nhất khi promotion có cấu hình điều kiện này.
- Mức giảm thực tế luôn là `min(configuredDiscount, originalAmount)`; đơn có giá trị thấp hơn voucher được giảm về 0đ, không âm. Percentage chỉ giới hạn ở 100% như validation dữ liệu.

---

## HIGH-13 — Xác nhận promotion và booking không nằm trong một giao dịch phân tán an toàn

Khi payment success, Booking Service gọi Promotion Service confirm trước rồi mới cập nhật/confirm booking local:
`InternalBookingPaymentServiceImpl.java:448-486`.

Tương tự với free checkout:
`BookingServiceImpl.java:1470-1485`.

Nếu remote confirm thành công nhưng transaction Booking rollback hoặc lỗi vĩnh viễn, promotion đã bị tiêu thụ nhưng booking chưa confirm. Confirm idempotent giúp retry, nhưng không xử lý trường hợp retry không thành công lâu dài.

**Khắc phục**

- Dùng saga/outbox + reconciliation state rõ ràng.
- Có job đối chiếu `promotion CONFIRMED` nhưng booking chưa CONFIRMED.
- Có compensation/reversal hợp lệ thay vì dựa vào retry vô hạn.

---

## MEDIUM-01 — Điều kiện `format` và `roomType` không nhận được dữ liệu từ Booking

Engine hỗ trợ:

- `formats` / `format`
- `excludeRoomTypes` / `excludeRoomType`

Tại `PromotionConditionEvaluator.java:122-123, 135-137, 177-185`.

Nhưng context Booking tại `BookingServiceImpl.java:1384-1446` không đặt `format` hoặc `roomType`.

**Tác động**

Promotion nhắm theo định dạng/phòng có thể luôn không match hoặc exclude không hoạt động như mong đợi.

**Khắc phục**

Bổ sung dữ liệu authoritative từ showtime/auditorium snapshot; thêm contract test giữa Booking và Promotion.

---

## MEDIUM-02 — `channel` bị hardcode là WEB

`BookingServiceImpl.java:1446` luôn đặt `channel=WEB`.

Nếu cùng API được dùng cho nhân viên quầy, kiosk, mobile hoặc cash flow, promotion theo channel sẽ áp sai.

**Khắc phục**

Lưu booking origin/channel từ lúc tạo booking và dùng snapshot server-side.

---

## MEDIUM-03 — `businessDate` đang là ngày chiếu, không phải ngày mua

`BookingServiceImpl.java:1423-1429` lấy ngày từ `showtimeStart`.

Đây có thể đúng hoặc sai tùy ý nghĩa marketing. Hiện tên condition không nói rõ, dễ cấu hình nhầm “Thứ Hai” theo ngày mua nhưng engine lại xét ngày xem phim.

**Khắc phục**

Tách rõ `purchaseDate`, `showtimeDate`, `purchaseDayOfWeek`, `showtimeDayOfWeek`.

---

## MEDIUM-04 — Public voucher catalog thiếu legal-status filter

`findPublicPromotions()` kiểm tra active/time/kill switch nhưng không kiểm tra `LegalStatus.PASSED`:
`PromotionRepository.java:125-145`.

`findSystemPromotions()` có kiểm tra legal status:
`PromotionRepository.java:147-168`.

Claim sau đó mới gọi runtime validation nên customer có thể thấy voucher nhưng claim thất bại.

**Khắc phục**

Dùng chung một runtime predicate/specification cho discovery, claim, preview và reserve.

---

## MEDIUM-05 — Ví “usable” không xét trạng thái runtime đầy đủ

`findUsableWallet()` chỉ xét wallet status/time/usage và promotion chưa deleted; không xét:

- promotion status;
- campaign status;
- legal status;
- kill switch;
- budget/capacity.

Bằng chứng: `UserPromotionRepository.java:50-67`.

Frontend cũng chỉ xét wallet status/count/time:
`promotionPresentation.js:375-395`.

**Tác động**

Voucher được hiển thị là khả dụng nhưng checkout từ chối.

**Khắc phục**

Trả `runtimeAvailability` và `unavailableReason` từ backend, dựa trên cùng engine predicate.

---

## MEDIUM-06 — Issue promotion không xác thực đầy đủ recipient và lifecycle

Luồng issue chấp nhận user ID dạng chuỗi và không thấy xác minh authoritative người dùng tồn tại; đồng thời có thể phát promotion chưa runtime-active.

**Tác động**

- Tạo wallet mồ côi.
- Gửi thông báo voucher nhưng customer không dùng được.
- Khó phân biệt pre-issue có chủ đích với issue sai trạng thái.

**Khắc phục**

- Validate recipient qua User Service/batch endpoint.
- Tách `preIssue` với issue thông thường.
- Quy định rõ trạng thái promotion/campaign được phép cấp.

---

## MEDIUM-07 — Không thể cấp lại cùng promotion cho cùng user sau khi đã dùng

Database unique theo `(user_public_id, promotion_public_id)` và issue trả lại record cũ thay vì tạo grant instance mới.

**Tác động**

Không thể tái cấp đúng cùng voucher template cho customer sau một lần USED/REVOKED, kể cả chương trình muốn cấp bù.

**Khắc phục**

Dùng `grantPublicId`/`issueBatchId` làm instance; unique không nên nằm chỉ trên user + promotion template.

---

## MEDIUM-08 — Claim công khai không có claim inventory/cap riêng

`claim()` kiểm tra runtime-active nhưng không reserve redemption inventory và không có `maxClaims`.

**Tác động**

Có thể rất nhiều người nhận voucher vào ví dù chỉ vài người đầu dùng được. Nếu UX ngầm hứa “đã lưu trong ví = đã được đảm bảo”, đây là sai thực tế.

**Khắc phục**

- Thêm `maxClaims` nếu claim mang ý nghĩa giữ quyền dùng.
- Hoặc hiển thị rõ “first come, first served at checkout” và remaining không được bảo đảm.

---

## MEDIUM-09 — Campaign redemption count tăng theo từng promotion, không theo đơn

Confirm gom từng redemption rồi tăng campaign count theo số redemption. Nếu hai promotion của cùng campaign stack trên một booking, campaign có thể tăng count hai lần.

**Tác động**

`redemptionCount` không rõ là số đơn, số customer hay số promotion application; dashboard dễ diễn giải sai.

**Khắc phục**

Định nghĩa metric rõ ràng:

- `redeemedOrderCount`: distinct reservation/booking;
- `appliedPromotionCount`: số redemption row;
- `redeemedUserCount`: distinct user.

---

## MEDIUM-10 — Phép tính stacking phần trăm dùng original amount cho từng promotion

Mỗi candidate discount được tính trên original amount:
`PromotionEngineService.java:333-351`
`PromotionDiscountCalculator.java:29-40`.

Ví dụ 10% + 20% = 30% original, không phải tuần tự 10% rồi 20% trên số còn lại (=28%).

Đây là policy decision nhưng hiện chưa được thể hiện rõ, dễ over-discount so với kỳ vọng nghiệp vụ.

**Khắc phục**

Quy định rõ stacking mode: `ADDITIVE_ORIGINAL`, `SEQUENTIAL_REMAINING`, hoặc theo component; lưu sequence và base amount.

---

## MEDIUM-11 — Action model backend và frontend không đồng nhất

Backend hỗ trợ `CASHBACK`, `FREE_TICKET`, `FREE_COMBO`; frontend admin chỉ cho:

`PERCENTAGE`, `FIXED_AMOUNT`, `FULL_DISCOUNT`
`promotionPresentation.js:39`.

Ngoài ra backend xử lý `CASHBACK` như giảm trực tiếp lúc checkout:
`PromotionDiscountCalculator.java:47-53`, không phải hoàn tiền/credit sau thanh toán theo nghĩa thông thường.

**Khắc phục**

- Xóa action chưa hỗ trợ khỏi backend, hoặc xây đủ UI và settlement flow.
- Không gọi instant discount là cashback.

---

## MEDIUM-12 — Role naming không nhất quán

Promotion controller dùng role `OPERATIONS`, trong khi campaign/reservation controller dùng `OPERATIONS_MANAGER`:

- `AdminPromotionController.java:72,79,103`
- `AdminCampaignController.java:97,108`
- `AdminReservationController.java:31`

**Tác động**

Operations manager có thể truy cập campaign nhưng bị chặn ở promotion detail/search/pause, tùy role thực tế trong JWT.

**Khắc phục**

Dùng role constants/canonical matrix và integration test từng endpoint/role.

---

## MEDIUM-13 — Scheduler expiration có nguy cơ backlog

Reservation expiration xử lý batch 100 mỗi lần:
`PromotionReservationServiceImpl.java:79, 404`
Scheduler chạy mỗi 30 giây.

Nếu số reservation hết hạn phát sinh nhanh hơn tốc độ xử lý, budgetReserved bị giữ lâu và làm trầm trọng auto-pause giả.

**Khắc phục**

- Loop theo cursor cho đến rỗng hoặc đến time budget.
- Metric backlog/oldest-expired-age.
- Alert khi queue không giảm.

---

## LOW-01 — Endpoint tên “public” nhưng yêu cầu authentication

Security config yêu cầu auth cho toàn bộ `/api/**`:
`promotion-service/.../SecurityConfig.java:56-66`.

`/api/promotions/public` vì vậy không public theo nghĩa anonymous. Có thể đây là “public trong phạm vi customer”, nhưng tên endpoint và tài liệu cần thống nhất.

---

## LOW-02 — Budget bằng 0 có semantics không rõ

Một số validation chấp nhận budget không âm, nhưng activation/publish lại yêu cầu budget > 0. Nếu 0 nghĩa “unlimited” thì runtime sai; nếu 0 nghĩa không có ngân sách thì create/update nên từ chối sớm.

**Khắc phục:** chọn một semantics duy nhất và validate ngay khi lưu.

---

# 4. Các phần đang làm tốt

1. Internal token filter fail-closed, phân tách token nội bộ và so sánh constant-time.
2. Reserve chạy authoritative preview lại dưới lock thay vì tin quote từ frontend.
3. Có pessimistic locking cho reservation/campaign/promotion/wallet.
4. Idempotency dùng request hash/canonicalization và xử lý replay.
5. Wallet ownership được kiểm tra ở engine và reservation layer.
6. Payment callback kiểm tra amount/currency khớp final amount đã khóa.
7. Campaign lifecycle có approval/legal/kill switch/time checks.
8. Non-admin approval có nguyên tắc four-eyes ở luồng duyệt.

Những phần này nên được giữ lại khi refactor.

---

# 5. Thứ tự sửa đề xuất

## P0 — Chặn thất thoát ngay

1. Khóa và enforce payment provider xuyên Booking → Payment → callback.
2. Bỏ hardcode `identityVerified=true`.
3. Aggregate capacity check cho tổ hợp và dùng global campaign counts.
4. Enforce campaign exclusivity/stackability.
5. Chặn aggregate discount vượt policy compliance.

## P1 — Sửa tính đúng của vòng đời và dữ liệu

1. Thêm confirmed-promotion reversal cho refund/cancellation policy.
2. Snapshot immutable redemption/campaign/action.
3. Cấm delete/sửa template đã có lịch sử; dùng version/archive.
4. Sửa ledger amount và định nghĩa campaign counters.
5. Thêm reconciliation cho distributed confirm.

## P2 — Đồng bộ sản phẩm và UX

1. Giữ single-select cho lựa chọn thủ công nhưng luôn so sánh với AUTO tốt nhất; không consume manual benefit nếu AUTO có lợi hơn.
2. Cho phép tối đa 1 manual + 1 AUTO khi cả promotion và campaign đều bật stacking; đồng bộ rule giữa frontend, Booking và Promotion.
3. Backend trả availability/reason thống nhất cho wallet/catalog.
4. Bổ sung format/roomType/channel/date semantics.
5. Đồng bộ action types và role names.

## P3 — Vận hành

1. Sửa auto-pause/hysteresis.
2. Xử lý scheduler backlog.
3. Dashboard cho budget reserved, expired backlog, reversal, reconciliation mismatch.

---

# 6. Luồng mục tiêu nên hướng tới

1. Customer chọn provider hoặc Booking tạo server-owned Payment Intent.
2. Booking dựng toàn bộ promotion context từ dữ liệu server authoritative.
3. Promotion preview trả quote kèm decision/version; không tin giá/tier/verification từ client.
4. Reserve khóa snapshot bất biến: campaign, promotion version, provider, action, conditions, amount basis.
5. Booking lưu locked provider + final amount + reservation ID.
6. Payment Service chỉ tạo đúng provider đã khóa.
7. Callback kiểm tra amount, currency, provider và payment intent.
8. Confirm dùng saga/outbox; reconciliation theo dõi trạng thái lệch.
9. Refund/cancel tạo compensation ledger theo policy, không sửa lịch sử.

---

# 7. Test bắt buộc cần bổ sung

1. Finalize bằng MOMO, create payment bằng VNPAY phải bị từ chối.
2. Promotion yêu cầu verification với user chưa verified phải fail.
3. Exclusive campaign không được stack với campaign khác.
4. Campaign max=1, hai promotion cùng campaign trong một request không được tạo 2 redemption.
5. Campaign max áp dụng chung AUTO/VOUCHER/COUPON.
6. Manual voucher vẫn giữ AUTO promotion tương thích.
7. Refund do cinema cancellation hoàn wallet/budget đúng policy.
8. Sửa/xóa promotion có redemption phải bị chặn hoặc lịch sử vẫn đọc được từ snapshot.
9. Public voucher legal status FAILED/PENDING không được hiển thị.
10. Wallet của campaign paused/kill-switched trả unavailable reason đúng.
11. Format/roomType/channel condition contract test từ Booking đến Promotion.
12. Hold làm campaign chạm 98%, sau expiry không được pause vĩnh viễn.
13. Voucher có giá trị lớn hơn đơn phải giảm đơn về đúng 0đ; `minimumOrderAmount` chỉ có hiệu lực khi được cấu hình tường minh.
14. Remote promotion confirm thành công nhưng local booking commit fail phải được reconciliation phát hiện.

---

# 8. Kết luận cuối

Promotion module không chỉ có lỗi UI nhỏ; có một số lỗi ở mức **business integrity và revenue leakage**. Lỗi cần chặn đầu tiên là payment-method bypass, sau đó là authoritative identity/context, aggregate capacity và campaign stacking rules. Sau khi xử lý P0, nên refactor reservation/redemption thành ledger bất biến có reversal, thay vì tiếp tục dựa vào template promotion có thể thay đổi.

---

# 9. Kết quả khắc phục

**Ngày hoàn tất:** 2026-08-01
**Trạng thái:** đã xử lý toàn bộ 29 phát hiện trong phạm vi audit, hoàn thiện HIGH-13 distributed reconciliation, P3 monitoring/alert và migration tương thích dữ liệu cũ.

## 9.1. Quyết định nghiệp vụ đã chốt

1. `identityVerified` nghĩa là account đã hoàn tất bước xác thực để chuyển sang `ACTIVE`; giá trị được Auth Service ký trong access token. Claim thiếu hoặc sai kiểu được Booking xử lý fail-closed thành `false`.
2. Customer chọn tối đa một voucher/system promotion hoặc nhập một coupon. Engine luôn so sánh với AUTO tốt nhất, không consume lựa chọn thủ công nếu AUTO có lợi hơn, và chỉ cộng tối đa 1 manual + 1 AUTO khi cả hai promotion/campaign cho phép.
3. `campaign.maxRedemptions` được tính theo số reservation/order distinct trên toàn chiến dịch. `campaign.maxRedemptionsPerUser` là trần áp cho từng promotion, kết hợp với `promotion.maxRedemptionsPerUser` theo giá trị nhỏ hơn; lượt của khách được đếm theo `promotionPublicId` để các promotion clone trong cùng campaign không khóa lẫn nhau.
4. Engine chỉ cộng dồn tối đa 1 manual + 1 AUTO theo thứ tự phase/priority và tính percentage trên số tiền còn lại tại phase đó. Percentage tối đa 100%; fixed/full discount được giới hạn bằng giá trị còn lại để final amount không âm. Không còn trần giảm runtime 50% hoặc ngưỡng tối thiểu suy diễn từ giá trị voucher.
5. Promotion có điều kiện payment provider không được tạo checkout 0 đồng. Đơn còn phải thanh toán bắt buộc khóa provider trước khi amount lock.
6. Full refund đảo promotion đã confirm, hoàn wallet usage, promotion/campaign counters và campaign budget bằng adjustment ledger append-only.
7. Public claim dùng `promotion.maxRedemptions` làm claim inventory để voucher đã claim có quyền sử dụng được bảo đảm trong giới hạn. Cấp lại từ admin tạo grant instance mới sau khi grant cũ `USED`, `REVOKED` hoặc `EXPIRED`.
8. Budget `0` không mang nghĩa unlimited; create/update yêu cầu ít nhất `0.01`. Các action chưa có settlement flow (`CASHBACK`, `FREE_TICKET`, `FREE_COMBO`) bị từ chối.

## 9.2. Ma trận phát hiện và thay đổi

| Phát hiện | Trạng thái | Thay đổi chính |
|---|---|---|
| CRITICAL-01 | Đã sửa | Booking lưu provider trong amount lock; Payment từ chối provider thiếu/khác; callback kiểm tra provider thực tế; chặn provider-conditioned free checkout. |
| HIGH-01 | Đã sửa | Auth ký claim `identityVerified`; Booking đọc claim và fail closed; bỏ hardcode `true`. |
| HIGH-02 | Đã sửa | Engine chỉ ghép 1 manual VOUCHER/COUPON với 1 AUTO khi cả hai promotion và cả hai campaign bật `stackable`; `exclusiveCampaign` chặn ghép khác campaign. Campaign priority tiếp tục tham gia tie-breaker. |
| HIGH-03 | Đã sửa | Tổng capacity dùng global campaign count, không chia theo promotion type. Quota mỗi khách được tính độc lập theo promotion để clone cùng campaign có lượt riêng. |
| HIGH-04 | Đã sửa | Kiểm tra delta theo promotion, wallet, tổng campaign và budget sau khi chọn; runtime một booking có tối đa 1 manual benefit và 1 AUTO benefit hợp lệ. |
| HIGH-05 | Đã sửa | AUTO luôn nằm trong tập so sánh. Engine giữ mức giảm tốt nhất, không consume manual benefit nếu AUTO tốt hơn, và chỉ ghép manual + AUTO khi stacking policy cho phép. |
| HIGH-06 | Đã sửa | Checkout dùng state chọn đơn; DTO Booking/Promotion giới hạn request thủ công tối đa một phần tử. Engine có thể trả tối đa hai applied items theo BR-VOU-03. |
| HIGH-07 | Đã sửa | Thêm API idempotent `reverse`, trạng thái `REVERSED`, hoàn counters/wallet/budget và ghi adjustment reason code. |
| HIGH-08 | Đã sửa | Redemption lưu `sequenceNo`, `amountBefore`, `discountAmount`, `amountAfter`; V11 backfill ledger cũ. |
| HIGH-09 | Đã sửa | Snapshot campaign/type/code/name/priority/stackable/conditions/actions tại reserve; history/reversal đọc snapshot. |
| HIGH-10 | Đã sửa | Chỉ xóa draft chưa có promotion, wallet, reservation hoặc redemption reference; template đã issue/redeem là immutable. |
| HIGH-11 | Đã sửa | Ngưỡng exposure 98% chỉ từ chối reserve mới, không đổi business status sang `PAUSED`. |
| HIGH-12 | Đã thay thế chính sách | Bỏ trần runtime 50% theo quyết định nghiệp vụ; percentage tối đa 100%, fixed/full discount tối đa bằng original amount và `minimumOrderAmount` là điều kiện tường minh duy nhất. |
| HIGH-13 | Đã sửa | Promotion confirm/reverse phát durable outbox event; Booking consume qua inbox idempotent và scheduler đối chiếu sau grace period. Event chỉ có `orderPublicId` được bỏ qua; reservation ID lệch luôn là `MISMATCH`, không thể thành `MATCHED`. |
| MEDIUM-01 | Đã sửa | Movie/Showtime context đưa `format` và `roomType` vào immutable Booking price snapshot rồi sang Promotion. |
| MEDIUM-02 | Đã sửa | `channel` được snapshot theo actor (`WEB`/`BOX_OFFICE`), không còn hardcode ở promotion context. |
| MEDIUM-03 | Đã sửa | Tách `purchaseDate`, `showtimeDate`, `purchaseDayOfWeek`, `showtimeDayOfWeek`; giữ alias cũ để tương thích. |
| MEDIUM-04 | Đã sửa | Public catalog bắt buộc campaign `LegalStatus.PASSED`. |
| MEDIUM-05 | Đã sửa | Wallet API trả `runtimeAvailable`, `unavailableReasonCode`, `unavailableReason` sau khi xét lifecycle, legal, kill switch, budget và capacity; frontend dùng kết quả này. |
| MEDIUM-06 | Đã sửa | Issue chỉ cho promotion runtime-active và batch-validate toàn bộ recipient ACTIVE qua User Service, fail closed. |
| MEDIUM-07 | Đã sửa | V11 bỏ unique user+template; admin issue tạo grant mới khi grant gần nhất không còn AVAILABLE. |
| MEDIUM-08 | Đã sửa | Public claim khóa promotion và enforce claim inventory; không cấp trùng cho owner hiện có. |
| MEDIUM-09 | Đã sửa | Campaign counter tăng/giảm một lần cho mỗi distinct reservation/order. |
| MEDIUM-10 | Không còn áp dụng | Runtime chỉ chọn một promotion nên không còn percentage stacking; ledger vẫn lưu `sequenceNo`, amount before/discount/after để bảo toàn lịch sử. |
| MEDIUM-11 | Đã sửa | Runtime chỉ chấp nhận ba action đã có UI và settlement đúng nghĩa: percentage, fixed amount, full discount. |
| MEDIUM-12 | Đã sửa | Promotion admin endpoints dùng canonical role `OPERATIONS_MANAGER`, đồng nhất campaign/reservation. |
| MEDIUM-13 | Đã sửa | Expiration worker lặp theo batch đến rỗng hoặc time budget 5.000 records mỗi lượt. |
| LOW-01 | Đã sửa | `GET /api/promotions/public` cho phép anonymous đúng tên contract. |
| LOW-02 | Đã sửa | Budget create/update validate dương ngay tại DTO boundary. |

## 9.3. Clone voucher trong cùng campaign

- Mỗi clone tiếp tục là một Promotion độc lập, có `publicId`, code, capacity, wallet grant và redemption ledger riêng; public catalog không group/deduplicate theo campaign hoặc `clonedFromPublicId`.
- Lượt dùng của khách được đếm theo `promotionPublicId`. Dùng bản gốc hoặc một clone không làm các clone khác báo hết lượt; trần campaign theo khách chỉ đóng vai trò giới hạn tối đa cho từng promotion.
- Clone draft kế thừa `publicVisible` của voucher nguồn thay vì luôn ép `false`.
- Migration `V10__restore_public_clone_visibility.sql` nhận diện clone do endpoint legacy tạo, backfill `cloned_from_public_id` và khôi phục visibility khi nguồn là public voucher.
- Vì vậy ba voucher gồm bản gốc và hai clone trong cùng campaign đều xuất hiện ở trang customer nếu cùng thỏa lifecycle/legal/kill-switch/time predicate.

## 9.4. Migration dữ liệu

- `V10__restore_public_clone_visibility.sql`: sửa lineage/visibility cho voucher clone legacy.
- `V11__harden_promotion_redemption_integrity.sql`: snapshot redemption, chuyển ledger sang sequential amounts, thêm `REVERSED`, tạo `promotion_redemption_adjustments`, bỏ unique wallet owner+template và thêm index phục vụ reissue.
- Flyway migration test chạy tuần tự từ V9 lên V10/V11; Hibernate `ddl-auto=validate` trên MySQL container đã pass với schema mới.

## 9.5. Monitoring và cảnh báo vận hành P3

- Promotion cung cấp `GET /api/admin/promotion-monitoring/summary`; Promotion Center có view **Vận hành** hiển thị expiration backlog, oldest-expired age, reversal tổng/1 giờ, budget reserved/exposure và reconciliation mismatch từ Booking.
- Các gauge Prometheus gồm `promotion_reservation_expiration_backlog`, `promotion_reservation_oldest_expired_age_seconds`, `promotion_reversal_count`, `promotion_reversals_last_hour`, `promotion_active_budget_reserved`, `promotion_active_budget_exposure`, `promotion_campaigns_at_exposure_threshold`, `promotion_operations_active_alerts` và `booking_promotion_reconciliation_mismatch`.
- Alert được kích hoạt/khôi phục theo transition để không lặp log mỗi chu kỳ. Ngưỡng backlog, oldest age, reversal/hour, budget exposure và reconciliation mismatch đều cấu hình được bằng environment variable trong `application.example.properties`.
- Reversal đếm theo reservation distinct để một reservation stack nhiều promotion không làm sai dashboard. Budget dashboard chỉ tổng hợp campaign `ACTIVE`; exposure được định nghĩa là `budgetUsed + budgetReserved`.
- Reconciliation task dùng inbox event idempotent. Scheduler recheck theo `coalesce(checkedAt, createdAt)` và cập nhật `checkedAt` sau mỗi lần kiểm tra để mismatch cũ không chiếm batch làm đói task mới.

## 9.6. Kết quả kiểm thử

| Thành phần | Lệnh xác minh | Kết quả |
|---|---|---|
| Auth Service | `mvn -q test` | 34 test, 0 failure/error |
| Promotion Service | `mvn test` + focused contract tests | 75 test, 0 failure/error |
| Booking Service | `mvn -q test` | 164 test, 0 failure/error |
| Payment Service | `mvn -q test` | 96 test, 0 failure/error |
| User Service | `mvn -q test` | 28 test, 0 failure/error |
| Frontend | `npm test -- --run` | 391 test, 0 failure/error |
| Frontend build | `npm run build` | Thành công |

Các regression test trọng yếu bao phủ provider mismatch, verification fail-closed, global campaign capacity, quota độc lập của promotion clone, giữ lựa chọn voucher thủ công, manual + AUTO stacking, exclusive campaign, chọn một AUTO tốt nhất, từ chối nhiều manual voucher, voucher lớn hơn đơn trả final amount 0đ, minimum order tường minh, ẩn promotion đã dùng khỏi chooser, free-checkout provider rule, confirmed reversal/adjustment ledger, Flyway/JPA validation, authoritative Booking promotion context, lifecycle event redelivery/order-only, trạng thái reconciliation và reservation ID mismatch.

## 9.7. Single-select voucher và UX checkout

- `BookingCheckoutPage` lưu đúng một voucher khách chọn và chỉ gửi tối đa một `selectedUserPromotionPublicIds` hoặc một `couponCode` khi preview/finalize. `selectedPromotionPublicIds` luôn rỗng vì AUTO do Promotion Engine quyết định.
- Chọn thành công đóng modal; mở lại để đổi voucher, hoặc dùng nút xóa tại khối tóm tắt để bỏ lựa chọn hiện tại. Coupon và voucher loại trừ lẫn nhau.
- Khối ưu đãi vẫn dùng container bo góc, item co giãn, text/code tự xuống dòng và hàng nhập coupon dùng grid `minmax(0,1fr)` để không tràn sidebar.
- Form tạo/sửa campaign và promotion hiển thị riêng quyền cộng dồn. Backend lưu đúng `stackable` do admin chọn; mặc định vẫn là `false`.
- Booking DTO, Promotion DTO và Reservation DTO tiếp tục từ chối hơn một lựa chọn thủ công. Khi khách không chọn voucher/coupon, runtime chọn một AUTO tốt nhất. Khi đã chọn, voucher/coupon đó được giữ lại và chỉ cộng thêm tối đa một AUTO tương thích.
- Stacking yêu cầu cả hai promotion và cả hai campaign đều bật `stackable`; nếu khác campaign thì không bên nào được `exclusiveCampaign`. Nếu không đủ điều kiện, checkout chỉ áp dụng voucher/coupon khách đã chọn. Nếu cộng dồn, checkout liệt kê từng benefit và tổng giảm authoritative từ Engine.
- Không còn trần giảm mặc định 50%. Voucher cố định 15.000đ trên đơn 10.000đ giảm thực tế 10.000đ và final amount bằng 0đ; chỉ `minimumOrderAmount` được cấu hình tường minh mới khóa voucher theo giá trị đơn. Checkout vẫn chỉ lưu lựa chọn khi voucher thực sự xuất hiện trong `appliedPromotions`.
- Modal chỉ hiển thị promotion đang eligible hoặc đang hoạt động nhưng chưa thỏa điều kiện đơn/phim/rạp/tài khoản. Wallet `USED`/hết lượt và evaluation terminal như `USAGE_LIMIT_REACHED`, hết ngân sách, hết hiệu lực hoặc cấu hình lỗi đều bị loại khỏi danh sách và khỏi số đếm tab.
