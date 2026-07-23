# SCORE SERVICE — BỘ CÂU HỎI KHẢO SÁT YÊU CẦU & BUSINESS RULES

### Dựa trên mô hình Loyalty Program của CGV, Galaxy Cinema, Cinépolis, AMC Stubs, Vista Loyalty và các hệ thống Membership thực tế

> Tài liệu này dành riêng cho **Score Service** trong hệ thống đặt vé rạp chiếu phim.
>
> Service này chịu trách nhiệm:
>
> - Loyalty Point
> - Membership Tier
> - Point Ledger
> - Point History
> - Earn / Redeem
> - Hold / Commit / Release
> - Revoke
> - Expiration
> - Reconciliation
> - Admin Adjustment
> - Audit
>
> Promotion Service KHÔNG quản lý điểm.
>
> Booking Service KHÔNG tự cộng điểm.
>
> Payment Service KHÔNG tự trừ điểm.
>
> Mọi thay đổi số dư điểm đều phải đi qua Score Service.

---

# PHẦN A — DISCOVERY QUESTIONS

---

# A1. Membership Program

## 1.

Membership được tạo khi nào?

- đăng ký tài khoản
- xác minh email
- xác minh OTP
- giao dịch đầu tiên

---

## 2.

Một user có thể có nhiều membership không?

Ví dụ:

- Personal
- Corporate
- Employee

hay chỉ được đúng một membership duy nhất?

---

## 3.

Nếu user bị khóa tài khoản thì Membership có bị khóa theo không?

---

## 4.

Nếu user xóa tài khoản thì điểm sẽ:

- bị xóa
- giữ lại X ngày
- archive
- chuyển trạng thái INACTIVE

---

## 5.

Membership ID có được phép thay đổi không?

Hay phải immutable suốt đời?

---

## 6.

Guest checkout có được tích điểm không?

Nếu có

Booking trước

Đăng ký sau

có merge điểm được không?

---

## 7.

Một user đổi số điện thoại thì Membership có giữ nguyên không?

---

## 8.

Nếu merge hai account

Điểm xử lý như thế nào?

- cộng
- lấy lớn hơn
- giữ account chính
- merge history

---

## 9.

Có cho phép chuyển điểm sang tài khoản khác không?

Nếu có

điều kiện là gì?

---

## 10.

Có giới hạn số lượng Membership trên một CCCD không?

để chống tạo nhiều tài khoản nhận khuyến mãi.

---

# A2. Loyalty Point

---

## 11.

Điểm được tính theo:

- tiền thanh toán
- tiền trước giảm giá
- tiền sau giảm giá

---

## 12.

VAT có được tính điểm không?

---

## 13.

Phí tiện ích

Convenience Fee

có được tính điểm không?

---

## 14.

Bắp nước

Combo

Food

Merchandise

có được tích điểm không?

Hay chỉ vé phim?

---

## 15.

Nếu đơn hàng gồm

- vé
- combo

thì mỗi loại có earn rate khác nhau không?

---

## 16.

Một vé miễn phí

0đ

có được tích điểm không?

---

## 17.

Voucher Birthday

Voucher Compensation

Voucher Gift

có được tính điểm không?

---

## 18.

Điểm được làm tròn như thế nào?

Ví dụ

48.9

=> 48

49

=>49

hay

50

---

## 19.

Có giới hạn số điểm tối đa một giao dịch không?

Ví dụ

Max

500 point

1000 point

---

## 20.

Có giới hạn số điểm tối đa một ngày không?

để chống gian lận.

---

## 21.

Nếu Booking được chia thành nhiều payment

điểm tính lúc nào?

---

## 22.

Nếu Payment thành công

Booking FAIL

điểm có cộng không?

---

## 23.

Nếu Payment Retry nhiều lần

điểm có cộng nhiều lần không?

---

## 24.

Điểm cộng khi

Payment Success

hay

Booking Completed

hay

Movie Watched

---

## 25.

Nếu khách không đến xem phim

(No Show)

điểm xử lý thế nào?

---

# A3. Redeem Point

---

## 26.

1 điểm đổi được bao nhiêu tiền?

Có cố định không?

Hay Marketing thay đổi được?

---

## 27.

Một đơn hàng tối đa được dùng bao nhiêu điểm?

---

## 28.

Có yêu cầu thanh toán tối thiểu sau khi dùng điểm không?

Ví dụ

ít nhất còn lại

10.000đ

---

## 29.

Có được dùng điểm thanh toán 100% không?

Hay luôn phải trả tiền thật một phần?

---

## 30.

Có được dùng điểm mua combo không?

---

## 31.

Có được dùng điểm mua Gift Card không?

---

## 32.

Có được dùng điểm mua Voucher không?

---

## 33.

Có được đổi điểm lấy Merchandise không?

---

## 34.

Có được vừa dùng Voucher

vừa dùng Point

không?

---

## 35.

Nếu Promotion Service timeout

Point Hold xử lý thế nào?

---

## 36.

Point Hold tối đa giữ bao lâu?

---

## 37.

Booking hết hạn

Hold có tự release không?

---

## 38.

Release xảy ra khi nào?

---

## 39.

Nếu Hold thành công

Commit thất bại

thì xử lý thế nào?

---

## 40.

Nếu Commit timeout

Promotion retry bao nhiêu lần?


---

# A4. Membership Tier

---

## 41.

Có bao nhiêu hạng thành viên?

Ví dụ

- SILVER
- GOLD
- PLATINUM
- DIAMOND

Hay Marketing có thể tự tạo thêm hạng mới?

---

## 42.

Tier được tính dựa trên:

- Current Point
- Lifetime Point
- Annual Spending
- Annual Ticket Count
- Combination Rule

---

## 43.

Tier được nâng ngay lập tức sau mỗi giao dịch hay chạy batch cuối ngày?

---

## 44.

Tier downgrade xảy ra khi nào?

- đầu năm
- cuối năm
- rolling 12 tháng
- batch hàng tháng

---

## 45.

Khi bị downgrade

Voucher đã phát có bị thu hồi không?

---

## 46.

Tier Benefit lấy snapshot khi nào?

Ví dụ

Booking tạo lúc Gold

Thanh toán lúc Diamond

Áp dụng quyền lợi nào?

---

## 47.

Tier có ngày hết hạn không?

---

## 48.

Nếu Marketing thay đổi điều kiện Tier

User cũ xử lý như thế nào?

---

## 49.

Tier Upgrade có gửi Notification không?

---

## 50.

Tier Downgrade có gửi Notification không?

---

# A5. Point Expiration

---

## 51.

Điểm có hết hạn không?

---

## 52.

Điểm hết hạn theo

- Calendar Year
- Rolling 12 Months
- FIFO
- Theo từng giao dịch

---

## 53.

Điểm cũ hết hạn trước hay điểm mới?

(FIFO/LIFO)

---

## 54.

Nếu User vừa Earn vừa Expire cùng ngày

thì xử lý thứ tự thế nào?

---

## 55.

Nếu điểm hết hạn trong lúc đang Hold

được phép hay không?

---

## 56.

Point Expiration chạy

- realtime
- batch
- scheduler

---

## 57.

Có gửi cảnh báo

30 ngày

15 ngày

7 ngày

trước khi hết hạn không?

---

## 58.

Điểm hết hạn có rollback được không?

---

## 59.

Admin có được restore điểm hết hạn không?

---

## 60.

Có lưu lịch sử expire riêng không?

---

# A6. Refund & Revoke

---

## 61.

Nếu Booking bị hủy trước khi thanh toán

Point Hold xử lý thế nào?

---

## 62.

Nếu Payment Fail

Point Hold xử lý thế nào?

---

## 63.

Nếu Payment Success

Booking Cancel

do lỗi hệ thống

điểm xử lý thế nào?

---

## 64.

Nếu Cinema hủy suất chiếu

điểm redeem có trả lại không?

---

## 65.

Nếu Cinema hoàn tiền

điểm Earn có bị thu hồi không?

---

## 66.

Nếu chỉ hoàn một phần đơn hàng

điểm thu hồi tính thế nào?

---

## 67.

Nếu User đã tiêu hết điểm Earn

sau đó Refund

thì có cho Current Point âm không?

---

## 68.

Hay tạo Outstanding Balance?

---

## 69.

Nếu Outstanding Point tồn tại

User có được tiếp tục Redeem không?

---

## 70.

Nếu Revoke Point làm Tier bị giảm

Voucher Tier đã phát xử lý thế nào?

---

## 71.

Nếu Revoke nhiều lần cùng Booking

Idempotency xử lý thế nào?

---

## 72.

Refund sau 3 tháng

điểm đã Expire

xử lý thế nào?

---

## 73.

Refund sau khi Tier thay đổi

tính theo Tier hiện tại

hay Tier lúc Earn?

---

## 74.

Có cho phép Manual Refund Point không?

---

## 75.

Customer Support được phép hoàn tối đa bao nhiêu điểm?

---

# A7. Admin Operation

---

## 76.

Admin có được cộng điểm thủ công không?

---

## 77.

Admin có được trừ điểm thủ công không?

---

## 78.

Có cần Approval 2 cấp khi cộng nhiều điểm không?

---

## 79.

Có giới hạn số điểm được cộng trong ngày?

---

## 80.

Reason bắt buộc khi Adjustment?

---

## 81.

Adjustment có ảnh hưởng Lifetime Point không?

---

## 82.

Adjustment có thay đổi Tier không?

---

## 83.

Admin có được sửa History không?

---

## 84.

Admin có được xóa Ledger không?

---

## 85.

Admin Adjustment có gửi Notification không?

---

## 86.

Có cần Upload CSV để cộng điểm hàng loạt không?

---

## 87.

Có Undo Adjustment không?

---

## 88.

Undo tạo transaction mới

hay sửa transaction cũ?

---

## 89.

Audit giữ bao lâu?

---

## 90.

Có Export Audit cho Finance không?


---

# A8. Accounting & Financial Ledger


## 91.

Điểm được xem là:

- Loyalty Reward
- Virtual Currency
- Discount Liability

theo chuẩn kế toán của doanh nghiệp?

---

## 92.

Có cần theo dõi tổng "Point Liability" (nghĩa vụ điểm chưa sử dụng) không?

---

## 93.

Finance cần báo cáo:

- Current Outstanding Point
- Expired Point
- Redeemed Point
- Earned Point

theo ngày/tháng/quý?

---

## 94.

Có cần quy đổi Point Liability sang VNĐ theo tỷ lệ hiện hành không?

---

## 95.

Nếu Marketing thay đổi tỷ lệ quy đổi điểm

Point cũ xử lý như thế nào?

---

## 96.

Có cần snapshot tỷ lệ Earn Rate theo từng giao dịch không?

---

## 97.

Có cần snapshot tỷ lệ Redeem Rate theo từng giao dịch không?

---

## 98.

Finance có được sửa Ledger không?

---

## 99.

Ledger có được phép UPDATE không?

Hay chỉ INSERT (Append-only)?

---

## 100.

Có cần Journal/Reconciliation Report cuối ngày không?

---

# A9. Fraud Prevention

---

## 101.

Một tài khoản Earn quá nhiều điểm trong thời gian ngắn có cần cảnh báo không?

---

## 102.

Một thiết bị đăng nhập nhiều tài khoản để Earn Point có cần đánh dấu Fraud không?

---

## 103.

Có giới hạn Earn Point theo IP không?

---

## 104.

Có giới hạn Redeem Point theo thiết bị không?

---

## 105.

Một Booking bị Refund nhiều lần có cần cảnh báo không?

---

## 106.

Một Admin cộng điểm liên tục có cần Audit Alert không?

---

## 107.

Có Machine Learning Fraud Detection không?

Hay Rule-based?

---

## 108.

OTP có cần khi Redeem Point giá trị lớn không?

---

## 109.

Có yêu cầu xác thực lại mật khẩu khi Redeem nhiều điểm không?

---

## 110.

Có Blacklist User không?

---

# A10. Integration

---

## 111.

Booking Service gọi API nào?

---

## 112.

Promotion Service được phép Hold Point hay Commit trực tiếp?

---

## 113.

Payment Service có được gọi Score Service không?

---

## 114.

Nếu Kafka Down

Event xử lý thế nào?

---

## 115.

Nếu RabbitMQ Down

Retry bao lâu?

---

## 116.

Event phải Exactly Once

hay At Least Once?

---

## 117.

Idempotency Key được sinh ở đâu?

---

## 118.

BookingID có được dùng làm EventID không?

---

## 119.

Nếu Event đến trễ

Score xử lý thế nào?

---

## 120.

Nếu Event bị Duplicate

Ledger xử lý thế nào?

---

## 121.

Nếu Booking Service rollback

Score rollback bằng Event hay API?

---

## 122.

Có Saga Pattern không?

---

## 123.

Có Outbox Pattern không?

---

## 124.

Có CDC không?

---

## 125.

Có Dead Letter Queue không?

---

# A11. Reporting

---

## 126.

Dashboard cần hiển thị:

- Tổng điểm đã phát
- Tổng điểm đã sử dụng
- Tổng điểm hết hạn
- Tổng liability

---

## 127.

Top User nhiều điểm nhất?

---

## 128.

Top User Redeem nhiều nhất?

---

## 129.

Top Tier Distribution?

---

## 130.

Tỷ lệ Earn/Redeem?

---

## 131.

Điểm trung bình mỗi giao dịch?

---

## 132.

Tỷ lệ Point Expire?

---

## 133.

Tỷ lệ Refund Earn?

---

## 134.

Tỷ lệ Revoke?

---

## 135.

Tier Upgrade mỗi tháng?

---

## 136.

Tier Downgrade mỗi tháng?

---

## 137.

Average Point Balance?

---

## 138.

Lifetime Point Distribution?

---

## 139.

Dormant Member (>12 tháng không hoạt động)?

---

## 140.

Export CSV/Excel/PDF?

---

# A12. Legal & Compliance

---

## 141.

Điểm có phải tài sản của khách hàng không?

---

## 142.

Có được quy đổi thành tiền mặt không?

---

## 143.

Có được chuyển nhượng không?

---

## 144.

Có được thừa kế khi tài khoản mất không?

---

## 145.

Có cần đồng ý điều khoản Loyalty trước khi Earn Point không?

---

## 146.

Có cần lưu lịch sử chấp thuận Terms & Conditions không?

---

## 147.

Audit Log giữ bao nhiêu năm?

---

## 148.

Có cần mã hóa Ledger không?

---

## 149.

Có cần lưu IP khi Redeem Point không?

---

## 150.

Có cần lưu DeviceID khi Redeem Point không?

---

# KẾT THÚC PHẦN A

> Sau khi Product Owner, Marketing, Finance, CSKH và Legal trả lời toàn bộ 150 câu hỏi trên, Score Service mới có thể chốt Business Rules chính thức để triển khai Production.
# SCORE SERVICE — BỘ BUSINESS RULES

> Các Business Rule dưới đây được tổng hợp từ mô hình Loyalty của CGV, Galaxy Cinema, AMC Stubs, Cinepolis Club, Vista Loyalty, Oracle Simphony Loyalty và các best practices của hệ thống Loyalty/Event Sourcing.
>
> Những rule đánh dấu ** ASSUMPTION** là giả định hợp lý, cần Product Owner/Marketing/Finance xác nhận trước Production.

---

# B1. Membership Rules

| Mã | Business Rule |
|-----|---------------|
| BR-MEMBER-001 | Mỗi User chỉ được tồn tại **duy nhất một Loyalty Account** trong toàn hệ thống. |
| BR-MEMBER-002 | Loyalty Account được tạo tự động ngay sau khi User hoàn thành đăng ký tài khoản và xác minh OTP thành công. |
| BR-MEMBER-003 | Membership ID là Immutable, không được phép thay đổi trong suốt vòng đời tài khoản. |
| BR-MEMBER-004 | Nếu User đổi Email hoặc Số điện thoại thì Membership vẫn giữ nguyên. |
| BR-MEMBER-005 | Nếu User bị khóa tài khoản thì Loyalty Account chuyển sang trạng thái `LOCKED`, không được Earn hoặc Redeem. |
| BR-MEMBER-006 | Nếu User bị Soft Delete thì Loyalty Account chuyển sang `INACTIVE`, không xóa dữ liệu lịch sử. |
| BR-MEMBER-007 | Score Service không được phép Hard Delete Loyalty Account. |
| BR-MEMBER-008 | Merge Account chỉ được thực hiện bởi Admin và phải lưu đầy đủ Audit Log. |
| BR-MEMBER-009 | Sau khi Merge Account, Loyalty Account phụ chuyển trạng thái `MERGED`, không được sử dụng lại. |
| BR-MEMBER-010 | Mọi Membership phải luôn thuộc đúng một UserID duy nhất. |
| BR-MEMBER-011 | Loyalty Account không được phép chuyển Owner. |
| BR-MEMBER-012 | Guest Checkout không tạo Loyalty Account. |
| BR-MEMBER-013 | Guest Booking không được Earn Point.  ASSUMPTION |
| BR-MEMBER-014 | Nếu doanh nghiệp hỗ trợ Claim Booking sau khi đăng ký thì việc cộng điểm phải được xử lý bằng quy trình riêng, không cộng tự động. |
| BR-MEMBER-015 | Membership Status gồm: ACTIVE, LOCKED, INACTIVE, MERGED. |
| BR-MEMBER-016 | Loyalty Account phải được khởi tạo với Tier thấp nhất của hệ thống. |
| BR-MEMBER-017 | Loyalty Account luôn có Current Point = 0 khi khởi tạo. |
| BR-MEMBER-018 | Lifetime Point = 0 khi tạo mới. |
| BR-MEMBER-019 | Available Point = Current Point − Held Point. |
| BR-MEMBER-020 | Score Service là nguồn dữ liệu duy nhất (Single Source of Truth) cho Membership. |

---

# B2. Tier Rules

| Mã | Business Rule |
|-----|---------------|
| BR-TIER-001 | Tier được xác định hoàn toàn bởi Score Service, không Service nào khác được phép tự tính. |
| BR-TIER-002 | Mỗi Loyalty Account chỉ có đúng một Tier tại một thời điểm. |
| BR-TIER-003 | Tier được xác định dựa trên Lifetime Point hoặc Annual Spending tùy cấu hình doanh nghiệp. |
| BR-TIER-004 | Tier thấp nhất luôn là SILVER hoặc STANDARD. |
| BR-TIER-005 | Tier Upgrade xảy ra ngay sau khi Earn Point thành công. |
| BR-TIER-006 | Tier Downgrade chỉ được phép chạy bởi Scheduled Job hoặc Batch Job. |
| BR-TIER-007 | Tier không được phép downgrade ngay giữa giao dịch đang xử lý. |
| BR-TIER-008 | Khi Tier thay đổi phải tạo History Record mới. |
| BR-TIER-009 | Tier History không được Update. |
| BR-TIER-010 | Tier History chỉ Append. |
| BR-TIER-011 | Nếu Marketing thay đổi ngưỡng Tier thì không ảnh hưởng Tier History cũ. |
| BR-TIER-012 | Tier Benefit chỉ áp dụng từ thời điểm Upgrade thành công trở đi. |
| BR-TIER-013 | Tier Upgrade không áp dụng hồi tố cho Booking cũ. |
| BR-TIER-014 | Tier Downgrade không làm thay đổi Point History. |
| BR-TIER-015 | Tier phải được Snapshot vào Earn Transaction. |
| BR-TIER-016 | Tier cũng phải được Snapshot vào Redeem Transaction. |
| BR-TIER-017 | Tier hiện tại luôn được đọc từ User Score Table, không đọc từ History. |
| BR-TIER-018 | Tier Calculation phải Deterministic. |
| BR-TIER-019 | Một Loyalty Account không bao giờ có nhiều Tier đồng thời. |
| BR-TIER-020 | Mọi thay đổi Tier đều phải ghi Audit Log. |

---

# B3. Point Balance Rules

| Mã | Business Rule |
|-----|---------------|
| BR-POINT-001 | Current Point không bao giờ được nhỏ hơn 0. |
| BR-POINT-002 | Available Point = Current Point − Held Point. |
| BR-POINT-003 | Held Point luôn nhỏ hơn hoặc bằng Current Point. |
| BR-POINT-004 | Lifetime Point không bao giờ giảm, ngoại trừ chính sách Revoke được doanh nghiệp cho phép. ASSUMPTION |
| BR-POINT-005 | Current Point được cập nhật bằng Atomic Transaction. |
| BR-POINT-006 | Không được phép Update Point bằng phép gán trực tiếp (SET current_point = x). |
| BR-POINT-007 | Chỉ được Increment/Decrement thông qua Transaction Service. |
| BR-POINT-008 | Point Balance phải được Lock khi Commit Redeem. |
| BR-POINT-009 | Concurrent Update phải dùng Optimistic Lock hoặc Row Lock. |
| BR-POINT-010 | Mọi thay đổi Balance phải sinh Ledger Record. |
| BR-POINT-011 | Balance Snapshot phải được lưu vào History. |
| BR-POINT-012 | Balance Before và Balance After luôn phải được ghi nhận. |
| BR-POINT-013 | Nếu Transaction thất bại thì Balance phải Rollback toàn bộ. |
| BR-POINT-014 | Không cho phép Current Point âm do Race Condition. |
| BR-POINT-015 | Không cho phép Manual SQL Update Point ngoài Score Service. |
| BR-POINT-016 | Balance chỉ được thay đổi bởi Earn, Redeem, Expire, Revoke hoặc Admin Adjustment. |
| BR-POINT-017 | Point Balance phải nhất quán với Ledger. |
| BR-POINT-018 | Nếu Balance khác Ledger thì chuyển trạng thái RECONCILIATION_REQUIRED. |
| BR-POINT-019 | Hệ thống phải hỗ trợ Job Reconciliation tự động. |
| BR-POINT-020 | Ledger là nguồn kiểm chứng cuối cùng khi đối soát Balance. |

---
# B4. Earn Point Rules

| Mã | Business Rule |
|-----|---------------|
| BR-EARN-001 | Điểm chỉ được cộng sau khi Booking được xác nhận thanh toán thành công (`PAID` hoặc `COMPLETED` theo nghiệp vụ doanh nghiệp), không cộng tại thời điểm tạo Booking. |
| BR-EARN-002 | Earn Point phải dựa trên **số tiền đủ điều kiện tích điểm (Eligible Amount)**, không phải tổng giá trị đơn hàng. |
| BR-EARN-003 | Eligible Amount không bao gồm các khoản bị loại trừ (Voucher, Gift Ticket, các khoản Marketing cấu hình không tích điểm). |
| BR-EARN-004 | Công thức Earn Point phải được cấu hình, không hard-code trong source code. |
| BR-EARN-005 | Mọi Earn Transaction đều phải có `eventId` và `idempotencyKey`. |
| BR-EARN-006 | Một Booking chỉ được Earn Point đúng **một lần**. |
| BR-EARN-007 | Nếu nhận lại cùng `eventId`, Score Service phải trả về kết quả cũ (Idempotent). |
| BR-EARN-008 | Earn Point luôn sinh Ledger Transaction mới, không Update Transaction cũ. |
| BR-EARN-009 | Lifetime Point được tăng cùng lúc với Current Point khi Earn thành công. |
| BR-EARN-010 | Earn Point phải lưu Balance Before và Balance After. |
| BR-EARN-011 | Earn Point phải Snapshot Tier tại thời điểm Earn. |
| BR-EARN-012 | Earn Point không được thực hiện nếu Membership ở trạng thái LOCKED. |
| BR-EARN-013 | Earn Point không được thực hiện nếu Booking bị Cancel trước khi hoàn tất. |
| BR-EARN-014 | Earn Point không được phụ thuộc Promotion Service. |
| BR-EARN-015 | Nếu Booking có nhiều Ticket thì Earn tính theo tổng Eligible Amount của toàn Booking. |
| BR-EARN-016 | Earn Point không được tính hai lần khi Payment Retry. |
| BR-EARN-017 | Earn Point phải ghi Audit Log đầy đủ. |
| BR-EARN-018 | Nếu Transaction DB thất bại thì không publish Event. |
| BR-EARN-019 | Nếu Publish Event thất bại sau Commit DB thì Outbox Pattern phải đảm bảo Event được gửi lại. |
| BR-EARN-020 | Score Service là Service duy nhất được phép ghi Earn Ledger. |

---

# B5. Hold Point Rules

| Mã | Business Rule |
|-----|---------------|
| BR-HOLD-001 | Hold Point chỉ là thao tác **tạm giữ**, chưa làm giảm Current Point. |
| BR-HOLD-002 | Hold Point chỉ làm tăng `Held Point`. |
| BR-HOLD-003 | Available Point = Current Point − Held Point luôn phải ≥ 0. |
| BR-HOLD-004 | Không được Hold vượt quá Available Point. |
| BR-HOLD-005 | Hold Point phải có TTL. |
| BR-HOLD-006 | TTL Hold phải nhỏ hơn hoặc bằng TTL giữ ghế của Booking Service. |
| BR-HOLD-007 | Hết TTL phải tự động Release nếu chưa Commit. |
| BR-HOLD-008 | Hold phải gắn với BookingID duy nhất. |
| BR-HOLD-009 | Một Booking chỉ có một Hold đang ACTIVE. |
| BR-HOLD-010 | Hold Request phải Idempotent. |
| BR-HOLD-011 | Hold không làm thay đổi Lifetime Point. |
| BR-HOLD-012 | Hold không tạo Earn Transaction. |
| BR-HOLD-013 | Hold không làm thay đổi Tier. |
| BR-HOLD-014 | Hold phải sinh Ledger loại HOLD. |
| BR-HOLD-015 | Hold phải lưu ExpiredAt. |
| BR-HOLD-016 | Hold quá hạn phải chuyển trạng thái EXPIRED. |
| BR-HOLD-017 | Hold EXPIRED phải tự Release Point. |
| BR-HOLD-018 | Hold không được Update thành Booking khác. |
| BR-HOLD-019 | Hold chỉ được Commit hoặc Release đúng một lần. |
| BR-HOLD-020 | Hold History không được xóa. |

---

# B6. Commit Redeem Rules

| Mã | Business Rule |
|-----|---------------|
| BR-REDEEM-001 | Commit chỉ được thực hiện trên Hold đang ACTIVE. |
| BR-REDEEM-002 | Commit làm giảm Current Point và Held Point cùng lúc trong một Transaction. |
| BR-REDEEM-003 | Commit không được thực hiện hai lần với cùng Booking. |
| BR-REDEEM-004 | Commit phải Idempotent. |
| BR-REDEEM-005 | Commit phải ghi Ledger loại REDEEM. |
| BR-REDEEM-006 | Commit phải lưu Balance Before và Balance After. |
| BR-REDEEM-007 | Commit không làm giảm Lifetime Point. |
| BR-REDEEM-008 | Commit không được thực hiện nếu Hold đã EXPIRED. |
| BR-REDEEM-009 | Commit phải được Lock để tránh Double Spend. |
| BR-REDEEM-010 | Hai Commit đồng thời chỉ được phép thành công một Transaction. |
| BR-REDEEM-011 | Nếu Commit thất bại thì Held Point phải giữ nguyên. |
| BR-REDEEM-012 | Commit chỉ publish Event sau khi DB Commit thành công. |
| BR-REDEEM-013 | Promotion Service không được tự trừ Point trong DB. |
| BR-REDEEM-014 | Mọi thay đổi Point đều đi qua API của Score Service. |
| BR-REDEEM-015 | Commit phải sinh Audit Log. |
| BR-REDEEM-016 | Commit phải lưu Request Payload để phục vụ Reconciliation. |
| BR-REDEEM-017 | Commit phải lưu Response Payload. |
| BR-REDEEM-018 | Commit phải lưu Correlation ID. |
| BR-REDEEM-019 | Commit phải lưu Source Service. |
| BR-REDEEM-020 | Commit hoàn tất thì Hold chuyển trạng thái COMMITTED và không được tái sử dụng. |

---
# B7. Release Point Rules

| Mã | Business Rule |
|-----|---------------|
| BR-RELEASE-001 | Release chỉ áp dụng cho Hold đang ở trạng thái `ACTIVE`. |
| BR-RELEASE-002 | Release không làm thay đổi Current Point. |
| BR-RELEASE-003 | Release chỉ giảm Held Point. |
| BR-RELEASE-004 | Sau Release, Available Point phải được khôi phục đúng bằng Current Point. |
| BR-RELEASE-005 | Release phải Idempotent. |
| BR-RELEASE-006 | Một Hold chỉ được Release đúng một lần. |
| BR-RELEASE-007 | Release không làm thay đổi Lifetime Point. |
| BR-RELEASE-008 | Release không ảnh hưởng Tier. |
| BR-RELEASE-009 | Release phải sinh Ledger loại `RELEASE`. |
| BR-RELEASE-010 | Release phải lưu đầy đủ Balance Before/After. |
| BR-RELEASE-011 | Release tự động xảy ra khi Hold hết TTL. |
| BR-RELEASE-012 | Booking bị Cancel trước Payment phải Release Point. |
| BR-RELEASE-013 | Payment Timeout phải Release Point. |
| BR-RELEASE-014 | Promotion Service được phép gọi Release nhiều lần nhưng chỉ một lần thành công. |
| BR-RELEASE-015 | Release phải ghi Audit Log. |
| BR-RELEASE-016 | Release không được phép Update Ledger cũ. |
| BR-RELEASE-017 | Release luôn tạo Transaction mới. |
| BR-RELEASE-018 | Release phải Publish Event sau Commit DB. |
| BR-RELEASE-019 | Release Event phải Idempotent. |
| BR-RELEASE-020 | Hold sau Release chuyển trạng thái `RELEASED`. |

---

# B8. Refund & Revoke Rules

| Mã | Business Rule |
|-----|---------------|
| BR-REVOKE-001 | Revoke chỉ áp dụng với Earn Transaction đã CONFIRMED. |
| BR-REVOKE-002 | Một Earn Transaction chỉ được Revoke đúng một lần. |
| BR-REVOKE-003 | Revoke phải Idempotent. |
| BR-REVOKE-004 | Revoke luôn tham chiếu đến Original Earn Transaction (`reference_history_id`). |
| BR-REVOKE-005 | Revoke không được Update Earn Ledger cũ. |
| BR-REVOKE-006 | Revoke luôn sinh Ledger mới loại `REVOKE_EARN`. |
| BR-REVOKE-007 | Revoke làm giảm Current Point. |
| BR-REVOKE-008 | Revoke có thể làm giảm Lifetime Point nếu doanh nghiệp quy định. 🟡 ASSUMPTION |
| BR-REVOKE-009 | Nếu Current Point không đủ để thu hồi thì chỉ trừ phần còn lại và ghi Outstanding Point. |
| BR-REVOKE-010 | Outstanding Point phải được lưu riêng để đối soát. |
| BR-REVOKE-011 | Outstanding Point không được tự động xóa. |
| BR-REVOKE-012 | User có Outstanding Point có thể bị khóa Redeem theo cấu hình doanh nghiệp. |
| BR-REVOKE-013 | Revoke phải Trigger Tier Recalculation. |
| BR-REVOKE-014 | Tier có thể Downgrade sau Revoke. |
| BR-REVOKE-015 | Revoke phải lưu Balance Before/After. |
| BR-REVOKE-016 | Revoke phải lưu Outstanding Before/After. |
| BR-REVOKE-017 | Revoke phải ghi Audit Log đầy đủ. |
| BR-REVOKE-018 | Revoke chỉ Publish Event sau DB Commit. |
| BR-REVOKE-019 | Revoke phải hỗ trợ Retry mà không gây Double Deduction. |
| BR-REVOKE-020 | Không cho phép Revoke bằng thao tác Update trực tiếp Database. |

---

# B9. Point Expiration Rules

| Mã | Business Rule |
|-----|---------------|
| BR-EXP-001 | Điểm hết hạn phải được xử lý bởi Scheduled Job hoặc Batch Job. |
| BR-EXP-002 | Không Expire theo Request từ Client. |
| BR-EXP-003 | Điểm hết hạn luôn sinh Ledger loại `EXPIRED`. |
| BR-EXP-004 | Điểm hết hạn không làm giảm Lifetime Point. |
| BR-EXP-005 | Điểm hết hạn làm giảm Current Point. |
| BR-EXP-006 | Điểm hết hạn không được làm Current Point âm. |
| BR-EXP-007 | Expire theo nguyên tắc FIFO (điểm cũ hết hạn trước). |
| BR-EXP-008 | Mỗi Earn Transaction phải lưu Expiration Date riêng. |
| BR-EXP-009 | Điểm đã Redeem không được Expire lần nữa. |
| BR-EXP-010 | Điểm đã Revoke không được Expire lần nữa. |
| BR-EXP-011 | Expire Job phải Idempotent. |
| BR-EXP-012 | Chạy lại Job không được Expire trùng. |
| BR-EXP-013 | Expire phải lưu Balance Before/After. |
| BR-EXP-014 | Expire phải lưu Expiration Batch ID. |
| BR-EXP-015 | Expire phải Publish Event sau Commit DB. |
| BR-EXP-016 | Expire phải sinh Notification Event cho Notification Service. |
| BR-EXP-017 | Hệ thống nên gửi cảnh báo trước khi Expire theo cấu hình (30/15/7 ngày). 🟡 ASSUMPTION |
| BR-EXP-018 | Expired Point không được Restore tự động. |
| BR-EXP-019 | Restore chỉ được thực hiện bởi Admin Adjustment nếu doanh nghiệp cho phép. |
| BR-EXP-020 | Mọi Expiration phải lưu Audit Log. |


---

# B10. Admin Adjustment Rules

> Đây là nhóm nghiệp vụ thường xuất hiện trong các hệ thống Loyalty của CGV, AMC, Cinépolis, Oracle Simphony Loyalty, Salesforce Loyalty Management... dùng để xử lý khiếu nại, đền bù, migration dữ liệu và vận hành.

| Mã | Business Rule |
|-----|---------------|
| BR-ADMIN-001 | Chỉ người có quyền `SCORE_ADMIN` hoặc `LOYALTY_ADMIN` mới được phép Adjustment Point. |
| BR-ADMIN-002 | Mọi Adjustment đều bắt buộc nhập Reason Code. |
| BR-ADMIN-003 | Không cho phép Adjustment nếu Reason rỗng. |
| BR-ADMIN-004 | Adjustment phải sinh Ledger Transaction mới. |
| BR-ADMIN-005 | Không được phép Update Ledger cũ. |
| BR-ADMIN-006 | Manual Add Point làm tăng Current Point. |
| BR-ADMIN-007 | Manual Add Point có thể tăng Lifetime Point theo cấu hình doanh nghiệp.  ASSUMPTION |
| BR-ADMIN-008 | Manual Deduct Point làm giảm Current Point. |
| BR-ADMIN-009 | Không cho phép Manual Deduct làm Current Point âm. |
| BR-ADMIN-010 | Nếu không đủ Point thì ghi Outstanding Point hoặc từ chối Adjustment theo cấu hình. |
| BR-ADMIN-011 | Adjustment phải lưu Balance Before và Balance After. |
| BR-ADMIN-012 | Adjustment phải lưu Operator ID. |
| BR-ADMIN-013 | Adjustment phải lưu IP Address của Operator. |
| BR-ADMIN-014 | Adjustment phải lưu Device Information nếu có. |
| BR-ADMIN-015 | Adjustment phải lưu Ticket/Case ID của CSKH nếu phát sinh từ khiếu nại. |
| BR-ADMIN-016 | Adjustment phải lưu Correlation ID. |
| BR-ADMIN-017 | Không cho phép Delete Adjustment. |
| BR-ADMIN-018 | Không cho phép Edit Adjustment. |
| BR-ADMIN-019 | Nếu nhập sai Adjustment thì phải tạo Transaction đảo (Reverse Adjustment), không sửa dữ liệu cũ. |
| BR-ADMIN-020 | Reverse Adjustment phải tham chiếu Adjustment gốc (`reference_history_id`). |
| BR-ADMIN-021 | Adjustment phải Publish Event sau Commit DB. |
| BR-ADMIN-022 | Adjustment phải xuất hiện trong Statement của khách hàng. |
| BR-ADMIN-023 | Adjustment phải ghi Audit Log đầy đủ. |
| BR-ADMIN-024 | Adjustment có thể yêu cầu Approval nếu vượt ngưỡng cấu hình.  ASSUMPTION |
| BR-ADMIN-025 | Approval và Operator không được là cùng một người (Four-Eyes Principle). |

---

# B11. Point Ledger Rules

> Đây là phần quan trọng nhất của Score Service. Trong các hệ thống Loyalty chuyên nghiệp, **Ledger mới là nguồn dữ liệu gốc**, Balance chỉ là dữ liệu tổng hợp (Projection).

| Mã | Business Rule |
|-----|---------------|
| BR-LEDGER-001 | Ledger là Append-only. |
| BR-LEDGER-002 | Không được UPDATE Ledger Record. |
| BR-LEDGER-003 | Không được DELETE Ledger Record. |
| BR-LEDGER-004 | Mỗi Ledger Record có UUID riêng. |
| BR-LEDGER-005 | Ledger phải lưu Event ID. |
| BR-LEDGER-006 | Ledger phải lưu Idempotency Key. |
| BR-LEDGER-007 | Ledger phải lưu Source Service. |
| BR-LEDGER-008 | Ledger phải lưu Transaction Type. |
| BR-LEDGER-009 | Ledger phải lưu Requested Point. |
| BR-LEDGER-010 | Ledger phải lưu Actual Point Change. |
| BR-LEDGER-011 | Ledger phải lưu Balance Before. |
| BR-LEDGER-012 | Ledger phải lưu Balance After. |
| BR-LEDGER-013 | Ledger phải lưu Lifetime Before. |
| BR-LEDGER-014 | Ledger phải lưu Lifetime After. |
| BR-LEDGER-015 | Ledger phải lưu Held Point Before. |
| BR-LEDGER-016 | Ledger phải lưu Held Point After. |
| BR-LEDGER-017 | Ledger phải lưu Tier Snapshot. |
| BR-LEDGER-018 | Ledger phải lưu Booking ID nếu có. |
| BR-LEDGER-019 | Ledger phải lưu Payment ID nếu có. |
| BR-LEDGER-020 | Ledger phải lưu Promotion ID nếu giao dịch liên quan Promotion. |
| BR-LEDGER-021 | Ledger phải lưu Created Time UTC. |
| BR-LEDGER-022 | Ledger phải lưu Timezone phục vụ hiển thị. |
| BR-LEDGER-023 | Ledger phải lưu Metadata dạng JSON để mở rộng. |
| BR-LEDGER-024 | Ledger phải có Hash hoặc Checksum nếu doanh nghiệp yêu cầu chống chỉnh sửa dữ liệu.  ASSUMPTION |
| BR-LEDGER-025 | Ledger là nguồn duy nhất phục vụ Reconciliation và Audit. |

---

# B12. History Rules

| Mã | Business Rule |
|-----|---------------|
| BR-HISTORY-001 | User có quyền xem toàn bộ lịch sử Point của mình. |
| BR-HISTORY-002 | History chỉ đọc (Read Only). |
| BR-HISTORY-003 | History được phân trang. |
| BR-HISTORY-004 | Mặc định sắp xếp theo thời gian giảm dần. |
| BR-HISTORY-005 | Có thể lọc theo Transaction Type. |
| BR-HISTORY-006 | Có thể lọc theo khoảng thời gian. |
| BR-HISTORY-007 | Có thể tìm theo Booking ID. |
| BR-HISTORY-008 | Có thể tìm theo Event ID. |
| BR-HISTORY-009 | Có thể tìm theo Promotion ID. |
| BR-HISTORY-010 | Có thể tìm theo Source Service. |
| BR-HISTORY-011 | History phải hiển thị Balance sau mỗi Transaction. |
| BR-HISTORY-012 | History không hiển thị dữ liệu Internal như Idempotency Key cho khách hàng. |
| BR-HISTORY-013 | Admin có quyền xem Metadata đầy đủ. |
| BR-HISTORY-014 | Customer chỉ xem dữ liệu đã được Public View Mapping. |
| BR-HISTORY-015 | History API không được trả dữ liệu của User khác. |
| BR-HISTORY-016 | Internal API phải xác thực bằng Internal Token hoặc Service Authentication. |
| BR-HISTORY-017 | History phải hỗ trợ Export cho Admin. |
| BR-HISTORY-018 | Export phải ghi Audit Log. |
| BR-HISTORY-019 | Export dữ liệu lớn phải chạy bất đồng bộ. |
| BR-HISTORY-020 | History không được phép sửa hoặc xóa từ API. |

---

> **Nhận xét:** Đây là nhóm Business Rule mà nhiều đội dự án thường bỏ sót, nhưng lại là phần được QA và Auditor kiểm tra nhiều nhất vì liên quan trực tiếp đến tính toàn vẹn dữ liệu, đối soát và khả năng truy vết.


---

# B13. Idempotency Rules

> Đây là nhóm Rule quan trọng nhất trong hệ thống Loyalty theo kiến trúc Event-Driven. Nếu không có Idempotency, việc Retry từ Booking Service, Payment Service hoặc Message Broker sẽ làm cộng/trừ điểm nhiều lần.

| Mã | Business Rule |
|-----|---------------|
| BR-IDEMP-001 | Mọi API làm thay đổi Point bắt buộc phải có `Idempotency Key`. |
| BR-IDEMP-002 | `Idempotency Key` phải duy nhất trong phạm vi Transaction Type. |
| BR-IDEMP-003 | Hai Request cùng `Idempotency Key` phải trả về cùng một kết quả. |
| BR-IDEMP-004 | Không thực hiện Transaction lần hai nếu Idempotency Key đã tồn tại. |
| BR-IDEMP-005 | Earn Point phải Idempotent. |
| BR-IDEMP-006 | Hold Point phải Idempotent. |
| BR-IDEMP-007 | Commit Point phải Idempotent. |
| BR-IDEMP-008 | Release Point phải Idempotent. |
| BR-IDEMP-009 | Revoke Point phải Idempotent. |
| BR-IDEMP-010 | Expire Point phải Idempotent. |
| BR-IDEMP-011 | Admin Adjustment phải Idempotent nếu gọi qua API. |
| BR-IDEMP-012 | Idempotency Record không được xóa trong thời gian TTL cấu hình. |
| BR-IDEMP-013 | Idempotency phải được kiểm tra trước Business Validation. |
| BR-IDEMP-014 | Response của Request đầu tiên phải được cache để trả lại cho các Request trùng. |
| BR-IDEMP-015 | Nếu Request đầu tiên thất bại trước DB Commit thì Request tiếp theo được phép thực hiện lại. |
| BR-IDEMP-016 | Nếu DB Commit thành công nhưng Publish Event thất bại thì không được cộng/trừ Point lần nữa. |
| BR-IDEMP-017 | Outbox Pattern phải xử lý việc Publish lại Event. |
| BR-IDEMP-018 | Idempotency Key phải được ghi vào Ledger. |
| BR-IDEMP-019 | Event Consumer cũng phải kiểm tra Idempotency. |
| BR-IDEMP-020 | Không Service nào được bỏ qua kiểm tra Idempotency đối với Transaction thay đổi số dư. |

---

# B14. Reconciliation Rules

> Trong các hệ thống Loyalty thực tế (Vista, Oracle Loyalty, Salesforce Loyalty), luôn tồn tại Job đối soát để phát hiện sai lệch giữa Balance và Ledger.

| Mã | Business Rule |
|-----|---------------|
| BR-RECON-001 | Reconciliation Job chạy theo lịch (ví dụ mỗi đêm). |
| BR-RECON-002 | Balance được tính lại hoàn toàn từ Ledger. |
| BR-RECON-003 | Nếu Balance tính lại khác Balance hiện tại thì đánh dấu `RECONCILIATION_REQUIRED`. |
| BR-RECON-004 | Không tự động sửa dữ liệu nếu chưa xác minh nguyên nhân. |
| BR-RECON-005 | Reconciliation phải tạo báo cáo sai lệch. |
| BR-RECON-006 | Báo cáo phải bao gồm User ID, Balance hiện tại, Balance tính lại và chênh lệch. |
| BR-RECON-007 | Reconciliation không được làm thay đổi Ledger. |
| BR-RECON-008 | Reconciliation phải hỗ trợ chạy lại nhiều lần. |
| BR-RECON-009 | Reconciliation Job phải Idempotent. |
| BR-RECON-010 | Chỉ Admin có quyền xác nhận hoàn tất đối soát. |
| BR-RECON-011 | Sau khi xác minh, việc điều chỉnh phải thông qua Adjustment Transaction, không sửa trực tiếp Balance. |
| BR-RECON-012 | Mọi Adjustment phát sinh từ Reconciliation phải tham chiếu Report ID. |
| BR-RECON-013 | Reconciliation phải kiểm tra Outstanding Point. |
| BR-RECON-014 | Reconciliation phải kiểm tra Held Point. |
| BR-RECON-015 | Reconciliation phải kiểm tra Transaction bị thiếu Event. |
| BR-RECON-016 | Reconciliation phải kiểm tra Duplicate Transaction. |
| BR-RECON-017 | Reconciliation phải kiểm tra Missing Ledger Record. |
| BR-RECON-018 | Reconciliation phải lưu lịch sử các lần chạy. |
| BR-RECON-019 | Kết quả Reconciliation phải Export được cho Finance. |
| BR-RECON-020 | Không được xóa lịch sử Reconciliation. |

---

# B15. Integration Rules

| Mã | Business Rule |
|-----|---------------|
| BR-INT-001 | Booking Service không được phép ghi trực tiếp vào Database của Score Service. |
| BR-INT-002 | Promotion Service không được phép tự trừ Point. |
| BR-INT-003 | Payment Service không được phép tự cộng Point. |
| BR-INT-004 | Mọi thay đổi Point phải đi qua API hoặc Event của Score Service. |
| BR-INT-005 | Internal API phải xác thực bằng Internal Token hoặc Service Authentication. |
| BR-INT-006 | Public API bắt buộc xác thực JWT của người dùng. |
| BR-INT-007 | Booking Service là nguồn phát sinh Earn Event. |
| BR-INT-008 | Promotion Service là nguồn phát sinh Hold/Commit/Release Event. |
| BR-INT-009 | Notification Service chỉ nhận Event, không gọi sửa Point. |
| BR-INT-010 | Analytics Service chỉ đọc Event, không thay đổi dữ liệu. |
| BR-INT-011 | Score Service không phụ thuộc trực tiếp vào UI. |
| BR-INT-012 | Nếu Kafka/RabbitMQ tạm thời không khả dụng, Outbox Pattern đảm bảo Event được gửi lại. |
| BR-INT-013 | Không được Publish Event trước khi DB Commit. |
| BR-INT-014 | Consumer phải xử lý Duplicate Event. |
| BR-INT-015 | Event phải có Version để hỗ trợ thay đổi Schema. |
| BR-INT-016 | Event phải chứa Correlation ID. |
| BR-INT-017 | Event phải chứa Event ID duy nhất. |
| BR-INT-018 | Event phải chứa Timestamp UTC. |
| BR-INT-019 | Không Consumer nào được giả định Event luôn đến đúng thứ tự. |
| BR-INT-020 | Score Service phải hoạt động đúng ngay cả khi Event đến trễ (Out-of-Order) nếu có Idempotency và Version hợp lệ. |

---

# B16. Audit Rules

| Mã | Business Rule |
|-----|---------------|
| BR-AUDIT-001 | Mọi Transaction thay đổi Point đều phải ghi Audit Log. |
| BR-AUDIT-002 | Audit Log phải lưu User ID. |
| BR-AUDIT-003 | Audit Log phải lưu Operator ID (nếu là Admin). |
| BR-AUDIT-004 | Audit Log phải lưu IP Address. |
| BR-AUDIT-005 | Audit Log phải lưu User Agent hoặc Device Information nếu có. |
| BR-AUDIT-006 | Audit Log phải lưu Timestamp UTC. |
| BR-AUDIT-007 | Audit Log phải lưu Correlation ID. |
| BR-AUDIT-008 | Audit Log phải lưu Event ID. |
| BR-AUDIT-009 | Audit Log không được chỉnh sửa. |
| BR-AUDIT-010 | Audit Log không được xóa bằng API nghiệp vụ. |
| BR-AUDIT-011 | Audit Log phải được lưu riêng với Business Data nếu doanh nghiệp yêu cầu. |
| BR-AUDIT-012 | Export Audit phải được ghi nhận bằng một Audit Record khác. |
| BR-AUDIT-013 | Chỉ người có quyền phù hợp mới được xem Audit. |
| BR-AUDIT-014 | Audit phải hỗ trợ truy vết toàn bộ vòng đời của một Booking. |
| BR-AUDIT-015 | Audit phải hỗ trợ tìm kiếm theo Event ID, Booking ID, User ID và Transaction ID. |

---

> Đây là phần cuối cùng và cũng là phần mà các hệ thống Loyalty thương mại (Vista Loyalty, Oracle Simphony, Salesforce Loyalty, Capillary Loyalty, Annex Cloud...) luôn có để đảm bảo khả năng vận hành lâu dài.

---

# B17. Fraud Detection Rules

| Mã | Business Rule |
|-----|---------------|
| BR-FRAUD-001 | Một User Earn Point bất thường vượt ngưỡng cấu hình trong khoảng thời gian ngắn phải được đánh dấu Suspicious. |
| BR-FRAUD-002 | Một Booking không được phép Earn Point nhiều hơn một lần. |
| BR-FRAUD-003 | Một Payment ID không được liên kết với nhiều Earn Transaction. |
| BR-FRAUD-004 | Một User Redeem Point liên tục trong thời gian ngắn vượt ngưỡng phải sinh Fraud Alert. |
| BR-FRAUD-005 | Một Device đăng nhập nhiều User để Redeem Point phải được ghi nhận Risk Score. |
| BR-FRAUD-006 | Một IP thực hiện nhiều giao dịch Redeem bất thường phải được cảnh báo. |
| BR-FRAUD-007 | Nếu Outstanding Point vượt ngưỡng cấu hình thì User có thể bị tạm khóa Redeem. |
| BR-FRAUD-008 | Nhiều lần Revoke liên tiếp trên cùng User phải được đưa vào báo cáo Fraud. |
| BR-FRAUD-009 | Manual Adjustment giá trị lớn phải yêu cầu Approval hai cấp. |
| BR-FRAUD-010 | Một Admin cộng điểm cho cùng User nhiều lần trong thời gian ngắn phải sinh Audit Alert. |
| BR-FRAUD-011 | Fraud Rule chỉ đánh dấu cảnh báo, không tự động xóa dữ liệu Loyalty. |
| BR-FRAUD-012 | Fraud Investigation phải tham chiếu Ledger thay vì Balance hiện tại. |
| BR-FRAUD-013 | Fraud Investigation phải truy vết được toàn bộ Event liên quan. |
| BR-FRAUD-014 | Fraud Report phải Export được cho đội Vận hành. |
| BR-FRAUD-015 | Fraud Rule phải cấu hình được, không hard-code trong source code. |

---

# B18. Performance & Scalability Rules

| Mã | Business Rule |
|-----|---------------|
| BR-PERF-001 | Balance Query phải tối ưu để đọc trực tiếp từ bảng tổng hợp (`user_scores`), không tính lại từ Ledger trong mỗi request. |
| BR-PERF-002 | Ledger chỉ được dùng cho Audit, History và Reconciliation. |
| BR-PERF-003 | History API phải phân trang (Pagination). |
| BR-PERF-004 | History API không được trả toàn bộ Transaction trong một lần gọi. |
| BR-PERF-005 | Các Job Expiration và Reconciliation phải chạy theo Batch. |
| BR-PERF-006 | Không Lock toàn bộ bảng khi Expire Point. |
| BR-PERF-007 | Không Lock toàn bộ bảng khi Reconciliation. |
| BR-PERF-008 | Các Transaction thay đổi Balance phải sử dụng Transaction ngắn để giảm Lock Time. |
| BR-PERF-009 | Chỉ Cache dữ liệu đọc (Tier, Membership...), không Cache Balance có thể ghi đồng thời nếu chưa có chiến lược Invalidation phù hợp. |
| BR-PERF-010 | Tất cả truy vấn History phải có Index phù hợp trên UserID, EventID, BookingID và CreatedAt. |

---

# B19. Security Rules

| Mã | Business Rule |
|-----|---------------|
| BR-SEC-001 | Public API bắt buộc xác thực JWT hợp lệ. |
| BR-SEC-002 | Internal API bắt buộc xác thực Service-to-Service Authentication hoặc Internal Token. |
| BR-SEC-003 | Không Client nào được phép truyền Current Point để cập nhật trực tiếp. |
| BR-SEC-004 | Score Service luôn tự tính Balance mới ở phía Server. |
| BR-SEC-005 | Không tin tưởng bất kỳ giá trị Point nào gửi từ Client. |
| BR-SEC-006 | Request Adjustment phải kiểm tra Role và Permission. |
| BR-SEC-007 | Mọi Endpoint Admin phải ghi Audit Log. |
| BR-SEC-008 | Không trả về Internal Metadata trong Public API. |
| BR-SEC-009 | Không trả về Idempotency Key cho Client. |
| BR-SEC-010 | Không trả về Internal Event ID trong API dành cho khách hàng. |
| BR-SEC-011 | Ledger và Audit Log chỉ được truy cập bởi người có quyền. |
| BR-SEC-012 | Không cho phép SQL trực tiếp từ Service khác truy cập Database Score Service. |
| BR-SEC-013 | Toàn bộ giao tiếp Service phải dùng HTTPS/TLS trong Production. |
| BR-SEC-014 | Secret, Internal Token và Credential phải quản lý qua Secret Manager hoặc Environment Variables, không hard-code. |
| BR-SEC-015 | Tất cả API thay đổi dữ liệu phải có Rate Limit phù hợp để giảm nguy cơ Abuse. |

---

# B20. Data Integrity Rules

| Mã | Business Rule |
|-----|---------------|
| BR-DATA-001 | Current Point phải luôn bằng tổng Earn − Redeem − Revoke − Expire + Adjustment (không tính Hold). |
| BR-DATA-002 | Available Point phải luôn bằng Current Point − Held Point. |
| BR-DATA-003 | Không cho phép Held Point lớn hơn Current Point. |
| BR-DATA-004 | Balance không được âm. |
| BR-DATA-005 | Ledger luôn là nguồn dữ liệu gốc (Source of Truth). |
| BR-DATA-006 | Balance là dữ liệu tổng hợp (Projection) có thể tái tạo từ Ledger. |
| BR-DATA-007 | Không được cập nhật Balance nếu Ledger ghi thất bại. |
| BR-DATA-008 | Không được ghi Ledger nếu Transaction DB thất bại. |
| BR-DATA-009 | Mọi Transaction đều phải có Timestamp UTC. |
| BR-DATA-010 | Mọi Transaction đều phải có UUID. |
| BR-DATA-011 | Mọi Transaction đều phải có Transaction Type. |
| BR-DATA-012 | Mọi Transaction đều phải có User ID. |
| BR-DATA-013 | Không được phép tồn tại Ledger "mồ côi" (không tham chiếu User hợp lệ). |
| BR-DATA-014 | Không được phép tồn tại History không có Ledger tương ứng. |
| BR-DATA-015 | Không được phép sửa trực tiếp Balance ngoài Score Service. |

---

# Tổng kết

## Discovery Questions

- 12 nhóm nghiệp vụ
- **150 câu hỏi**

---

## Business Rules

| Nhóm | Số lượng |
|--------|----------:|
| Membership | 20 |
| Tier | 20 |
| Point Balance | 20 |
| Earn | 20 |
| Hold | 20 |
| Redeem | 20 |
| Release | 20 |
| Revoke / Refund | 20 |
| Expiration | 20 |
| Admin Adjustment | 25 |
| Ledger | 25 |
| History | 20 |
| Idempotency | 20 |
| Reconciliation | 20 |
| Integration | 20 |
| Audit | 15 |
| Fraud | 15 |
| Performance | 10 |
| Security | 15 |
| Data Integrity | 15 |

### Tổng cộng

- **150 Discovery Questions**
- **400 Business Rules**
- Bao phủ toàn bộ nghiệp vụ của một **Score Service (Loyalty & Membership)** trong hệ thống quản lý rạp chiếu phim theo hướng microservices, đủ làm cơ sở cho BA, PO, Backend, QA và DevOps thống nhất yêu cầu trước khi triển khai.