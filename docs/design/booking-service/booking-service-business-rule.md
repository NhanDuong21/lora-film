# BOOKING_SERVICE_BUSINESS_QA.md


> **Scope:** Chỉ áp dụng cho `booking-service`.

# Chapter 1. Booking Lifecycle

## Booking Creation

- **Q001.** Khi nào hệ thống được phép tạo một booking mới?
- **Q002.** Những điều kiện nào bắt buộc phải thỏa trước khi tạo booking?
- **Q003.** User chưa đăng nhập có được phép tạo booking không?
- **Q004.** Booking có bắt buộc phải gắn với một user không?
- **Q005.** Một booking có bắt buộc phải thuộc một showtime không?
- **Q006.** Một booking có bắt buộc phải thuộc một auditorium không?
- **Q007.** Một booking có bắt buộc phải thuộc một cinema không?
- **Q008.** Một booking có bắt buộc phải chứa ít nhất một ghế không?
- **Q009.** Một booking có được phép không có seat reservation không?
- **Q010.** Booking được tạo trước hay seat reservation được tạo trước?
- **Q011.** Khi tạo booking có cần transaction không?
- **Q012.** Nếu tạo booking thất bại thì có rollback toàn bộ không?
- **Q013.** Booking Code được sinh ở thời điểm nào?
- **Q014.** Booking Code có được phép thay đổi không?
- **Q015.** Booking Code có cần duy nhất trên toàn hệ thống không?
- **Q016.** Booking có được phép sửa sau khi tạo không?
- **Q017.** Booking có được phép đổi ghế sau khi tạo không?
- **Q018.** Booking có được phép đổi showtime sau khi tạo không?
- **Q019.** Booking có được phép đổi user không?
- **Q020.** Booking có được phép xóa vật lý không?
- **Q021.** Booking có hỗ trợ soft delete không?
- **Q022.** Booking có lưu thời gian tạo không?
- **Q023.** Booking có lưu người tạo không?
- **Q024.** Booking có lưu thời gian cập nhật cuối không?
- **Q025.** Booking có lưu người cập nhật cuối không?

---

## Booking Ownership

- **Q026.** Một user có thể có bao nhiêu booking đang hoạt động?
- **Q027.** Một user có thể có nhiều booking PENDING cùng lúc không?
- **Q028.** Một user có thể tạo nhiều booking cho cùng một showtime không?
- **Q029.** Một user có thể tạo nhiều booking cho cùng một phim không?
- **Q030.** Một user có thể tạo nhiều booking cho cùng một ghế không?
- **Q031.** Một booking có thể thuộc nhiều user không?
- **Q032.** Booking có thể chuyển quyền sở hữu sang user khác không?
- **Q033.** Booking có được tạo thay cho người khác không?
- **Q034.** Admin có được phép tạo booking thay user không?
- **Q035.** Booking có cần lưu nguồn tạo booking không?

---

# Chapter 2. Booking Validation

- **Q036.** Hệ thống kiểm tra booking trước hay seat trước?
- **Q037.** Hệ thống kiểm tra showtime trước hay seat trước?
- **Q038.** Hệ thống kiểm tra seat trước hay reservation trước?
- **Q039.** Hệ thống kiểm tra reservation trước hay booking trước?
- **Q040.** Có cho phép request không có seat không?
- **Q041.** Có cho phép request chứa seat bị trùng không?
- **Q042.** Có cho phép request có danh sách seat rỗng không?
- **Q043.** Có cho phép request vượt quá số ghế tối đa không?
- **Q044.** Có giới hạn số ghế tối đa trong một booking không?
- **Q045.** Có giới hạn số ghế tối thiểu trong một booking không?
- **Q046.** Có kiểm tra seat tồn tại không?
- **Q047.** Có kiểm tra seat thuộc đúng auditorium không?
- **Q048.** Có kiểm tra seat thuộc đúng showtime không?
- **Q049.** Có kiểm tra seat đang hoạt động không?
- **Q050.** Có kiểm tra seat đang bảo trì không?
- **Q051.** Có kiểm tra seat đã bán chưa không?
- **Q052.** Có kiểm tra seat đang được giữ không?
- **Q053.** Có kiểm tra trạng thái showtime không?
- **Q054.** Có kiểm tra showtime đã mở bán chưa?
- **Q055.** Có kiểm tra showtime đã bắt đầu chưa?
- **Q056.** Có kiểm tra showtime đã kết thúc chưa?
- **Q057.** Có kiểm tra showtime đã hủy chưa?
- **Q058.** Có kiểm tra showtime còn tồn tại không?
- **Q059.** Có kiểm tra user còn hoạt động không?
- **Q060.** Có kiểm tra user có quyền đặt vé không?

---

# Chapter 3. Seat Reservation

- **Q061.** Seat Reservation được tạo ở thời điểm nào?
- **Q062.** Seat Reservation được xóa ở thời điểm nào?
- **Q063.** Seat Reservation tồn tại trong bao lâu?
- **Q064.** Thời gian giữ ghế có cấu hình được không?
- **Q065.** Một seat có thể có nhiều reservation không?
- **Q066.** Một reservation có thể chứa nhiều seat không?
- **Q067.** Một booking có thể có nhiều reservation không?
- **Q068.** Reservation có bắt buộc phải thuộc booking không?
- **Q069.** Reservation có bắt buộc phải thuộc user không?
- **Q070.** Reservation có được gia hạn không?
- **Q071.** Reservation có tự động hết hạn không?
- **Q072.** Reservation có bị hủy khi booking bị hủy không?
- **Q073.** Reservation có bị hủy khi booking hết hạn không?
- **Q074.** Reservation có bị hủy khi thanh toán thành công không?
- **Q075.** Reservation có bị hủy khi thanh toán thất bại không?
- **Q076.** Reservation có bị hủy khi user logout không?
- **Q077.** Reservation có bị hủy khi mất kết nối không?
- **Q078.** Reservation có lưu thời gian hết hạn không?
- **Q079.** Reservation có lưu thời gian tạo không?
- **Q080.** Reservation có cần audit không?

---

# Chapter 4. Booking Status

- **Q081.** Booking có bao nhiêu trạng thái?
- **Q082.** Trạng thái mặc định của booking là gì?
- **Q083.** Booking chuyển sang `PENDING_PAYMENT` khi nào?
- **Q084.** Booking chuyển sang `PAID` khi nào?
- **Q085.** Booking chuyển sang `CANCELLED` khi nào?
- **Q086.** Booking chuyển sang `EXPIRED` khi nào?
- **Q087.** Booking có thể chuyển trực tiếp từ `PENDING_PAYMENT` sang `CANCELLED` không?
- **Q088.** Booking có thể chuyển trực tiếp từ `PENDING_PAYMENT` sang `PAID` không?
- **Q089.** Booking có thể chuyển trực tiếp từ `PAID` sang `CANCELLED` không?
- **Q090.** Booking có thể chuyển trực tiếp từ `EXPIRED` sang `PAID` không?
- **Q091.** Booking có thể chuyển trực tiếp từ `CANCELLED` sang `PAID` không?
- **Q092.** Booking có thể quay lại trạng thái trước đó không?
- **Q093.** Booking có sử dụng State Machine để quản lý trạng thái không?
- **Q094.** Có kiểm tra State Transition hợp lệ trước khi cập nhật trạng thái không?
- **Q095.** Có lưu lịch sử thay đổi trạng thái booking không?
- **Q096.** Có phát sinh Domain Event khi trạng thái booking thay đổi không?
- **Q097.** Trạng thái booking có được cập nhật thủ công không?
- **Q098.** Những vai trò nào được phép thay đổi trạng thái booking?
- **Q099.** Booking đạt trạng thái nào thì được xem là kết thúc vòng đời (Terminal State)?

# Part 2 (Q101–Q200)

---

# Chapter 5. Booking Cancellation

## User Cancellation

- **Q100.** User được phép hủy booking trong những trường hợp nào?
- **Q101.** User không được phép hủy booking trong những trường hợp nào?
- **Q102.** Booking đã thanh toán có được phép hủy không?
- **Q103.** Booking đã hết hạn có được phép hủy không?
- **Q104.** Booking đã bị hủy có được phép hủy lại không?
- **Q105.** Booking đã hoàn tất có được phép hủy không?
- **Q106.** Admin có được phép hủy booking của user không?
- **Q107.** Hệ thống có ghi nhận người thực hiện hủy booking không?
- **Q108.** Hệ thống có ghi nhận lý do hủy booking không?

## Cancellation Processing

- **Q109.** Khi booking bị hủy thì trạng thái booking được cập nhật như thế nào?
- **Q110.** Khi booking bị hủy thì reservation có được giải phóng ngay không?
- **Q111.** Khi booking bị hủy thì toàn bộ ghế có được mở lại không?
- **Q112.** Khi booking bị hủy có phát sinh domain event không?
- **Q113.** Booking đã hủy có thể khôi phục lại không?
- **Q114.** Booking đang xử lý thanh toán có được phép hủy không?
- **Q115.** Booking đang hết hạn xử lý có được phép hủy không?
- **Q116.** Nếu hủy booking thất bại thì transaction có rollback không?
- **Q117.** Hủy booking có cần idempotent không?

---

# Chapter 6. Booking Expiration

## Expiration Rules

- **Q118.** Booking hết hạn được xác định như thế nào?
- **Q119.** Booking hết hạn sau bao nhiêu phút?
- **Q120.** Thời gian hết hạn có cấu hình được không?
- **Q121.** Booking đã thanh toán có bị hết hạn không?
- **Q122.** Booking đã hủy có bị kiểm tra hết hạn không?
- **Q123.** Booking đã hết hạn có được kiểm tra lại không?
- **Q124.** Booking hết hạn có được gia hạn không?
- **Q125.** Booking hết hạn có thể thanh toán tiếp không?
- **Q126.** Booking hết hạn có thể chuyển sang trạng thái khác không?

## Expiration Processing

- **Q127.** Ai chịu trách nhiệm kiểm tra booking hết hạn?
- **Q128.** Scheduler có chạy trên nhiều instance không?
- **Q129.** Làm thế nào để tránh nhiều scheduler xử lý cùng một booking?
- **Q130.** Khi booking hết hạn thì reservation được xử lý như thế nào?
- **Q131.** Khi booking hết hạn thì ghế được xử lý như thế nào?
- **Q132.** Khi booking hết hạn có publish event không?
- **Q133.** Khi booking hết hạn có gửi thông báo không?
- **Q134.** Nếu scheduler bị dừng thì booking hết hạn được xử lý như thế nào?

---

# Chapter 7. Payment Integration (Booking Perspective)

## Payment Events

- **Q135.** Booking Service nhận những sự kiện nào từ Payment Service?
- **Q136.** Khi nhận Payment Success thì booking xử lý như thế nào?
- **Q137.** Khi nhận Payment Failed thì booking xử lý như thế nào?
- **Q138.** Khi nhận Payment Cancelled thì booking xử lý như thế nào?
- **Q139.** Khi nhận Payment Refunded thì booking xử lý như thế nào?
- **Q140.** Payment Success có bắt buộc phải cập nhật booking không?
- **Q141.** Payment Failed có giải phóng ghế ngay không?
- **Q142.** Payment Failed có giữ nguyên booking không?
- **Q143.** Payment Timeout có làm booking hết hạn ngay không?
- **Q144.** Payment Event có cần idempotent không?

## Payment Consistency

- **Q145.** Nếu Payment Success được gửi hai lần thì booking xử lý như thế nào?
- **Q146.** Nếu Payment Failed đến sau Payment Success thì xử lý như thế nào?
- **Q147.** Nếu Payment Success đến sau khi booking đã hết hạn thì xử lý như thế nào?
- **Q148.** Nếu Payment Success đến sau khi booking đã bị hủy thì xử lý như thế nào?
- **Q149.** Booking có kiểm tra trạng thái hiện tại trước khi cập nhật không?
- **Q150.** Booking có kiểm tra transaction id không?
- **Q151.** Booking có kiểm tra booking code từ payment event không?
- **Q152.** Booking có bỏ qua payment event không hợp lệ không?
- **Q153.** Booking có ghi audit khi xử lý payment event không?

---

# Chapter 8. Idempotency

## Duplicate Request

- **Q154.** Booking API có hỗ trợ Idempotency-Key không?
- **Q155.** Idempotency-Key có bắt buộc không?
- **Q156.** Idempotency-Key được lưu trong bao lâu?
- **Q157.** Hai request có cùng Idempotency-Key được xử lý như thế nào?
- **Q158.** Idempotency có áp dụng cho Create Booking không?
- **Q159.** Idempotency có áp dụng cho Cancel Booking không?
- **Q160.** Idempotency có áp dụng cho Payment Event không?
- **Q161.** Idempotency có áp dụng cho Scheduler không?
- **Q162.** Idempotency có áp dụng cho Domain Event không?
- **Q163.** Khi phát hiện duplicate request thì hệ thống trả về dữ liệu gì?

---

# Chapter 9. Event Publishing

## Domain Events

- **Q164.** Booking Service phát sinh những domain event nào?
- **Q165.** BookingCreated Event được phát sinh khi nào?
- **Q166.** BookingPaid Event được phát sinh khi nào?
- **Q167.** BookingCancelled Event được phát sinh khi nào?
- **Q168.** BookingExpired Event được phát sinh khi nào?
- **Q169.** BookingUpdated Event có cần thiết không?
- **Q170.** BookingDeleted Event có cần thiết không?
- **Q171.** Domain Event được publish trước hay sau khi commit transaction?
- **Q172.** Domain Event có cần Outbox Pattern không?
- **Q173.** Nếu publish event thất bại thì booking xử lý như thế nào?

## Event Reliability

- **Q174.** Event có được retry khi publish thất bại không?
- **Q175.** Retry tối đa bao nhiêu lần?
- **Q176.** Event có cần Dead Letter Queue không?
- **Q177.** Event có cần ordering không?
- **Q178.** Event có cần unique id không?
- **Q179.** Event có cần timestamp không?
- **Q180.** Event có cần version không?
- **Q181.** Event có cần correlation id không?
- **Q182.** Event có cần trace id không?
- **Q183.** Booking Service có ghi log khi publish event không?

---

# Chapter 10. Booking Integrity

## Data Integrity

- **Q184.** Booking có sử dụng optimistic locking không?
- **Q185.** Booking có sử dụng version field không?
- **Q186.** Booking có kiểm tra lost update không?
- **Q187.** Booking có kiểm tra concurrent update không?
- **Q188.** Booking có rollback khi update trạng thái thất bại không?
- **Q189.** Booking có đảm bảo tính nhất quán dữ liệu không?
- **Q190.** Booking có đảm bảo một ghế chỉ thuộc một booking hợp lệ tại một thời điểm không?
- **Q191.** Booking có ghi audit khi rollback transaction không?
- **Q192.** Booking Service làm gì để đảm bảo tính toàn vẹn dữ liệu trong môi trường nhiều instance? 

# Part 3 (Q201–Q300)

---

# Chapter 11. Seat Locking & Concurrency

## Concurrent Booking

- **Q193.** Hai user cùng chọn một ghế tại cùng một thời điểm thì hệ thống xử lý như thế nào?
- **Q194.** Hai request từ cùng một user đến đồng thời có được xử lý không?
- **Q195.** Hệ thống ngăn chặn double booking bằng cách nào?
- **Q196.** Hệ thống khóa ghế trước hay tạo booking trước?
- **Q197.** Ghế được khóa trong giai đoạn nào của transaction?
- **Q198.** Khi khóa một ghế thất bại thì toàn bộ booking có bị hủy không?
- **Q199.** Hệ thống có khóa từng ghế hay khóa toàn bộ booking?
- **Q200.** Có thể khóa nhiều ghế trong cùng một transaction không?
- **Q201.** Hệ thống xử lý khi chỉ khóa thành công một phần số ghế như thế nào?
- **Q202.** Khi rollback transaction thì lock ghế được giải phóng như thế nào?

## Distributed Lock

- **Q203.** Booking Service có sử dụng Distributed Lock không?
- **Q204.** Distributed Lock được áp dụng cho những nghiệp vụ nào?
- **Q205.** Lock có thời gian hết hạn không?
- **Q206.** Lock timeout được xác định như thế nào?
- **Q207.** Lock có tự động giải phóng khi transaction hoàn thành không?
- **Q208.** Lock có được giải phóng khi service bị crash không?
- **Q209.** Lock có được giải phóng khi Redis restart không?
- **Q210.** Nếu lock bị mất giữa transaction thì xử lý như thế nào?
- **Q211.** Nếu không lấy được lock thì request có retry không?
- **Q212.** Retry lock tối đa bao nhiêu lần?

---

# Chapter 12. Scheduler

## Expiration Scheduler

- **Q213.** Booking Service có scheduler kiểm tra booking hết hạn không?
- **Q214.** Scheduler chạy theo fixed delay hay fixed rate?
- **Q215.** Chu kỳ chạy scheduler là bao lâu?
- **Q216.** Scheduler có xử lý theo batch không?
- **Q217.** Mỗi batch xử lý tối đa bao nhiêu booking?
- **Q218.** Scheduler có phân trang dữ liệu không?
- **Q219.** Scheduler có chạy song song không?
- **Q220.** Scheduler có giới hạn số thread không?
- **Q221.** Scheduler có khóa dữ liệu trước khi xử lý không?
- **Q222.** Scheduler có retry khi thất bại không?

## Scheduler Reliability

- **Q223.** Nếu scheduler dừng giữa chừng thì dữ liệu được xử lý như thế nào?
- **Q224.** Nếu service restart thì scheduler có tiếp tục xử lý không?
- **Q225.** Scheduler có ghi log khi bắt đầu xử lý không?
- **Q226.** Scheduler có ghi log khi kết thúc xử lý không?
- **Q227.** Scheduler có ghi log khi xảy ra lỗi không?
- **Q228.** Scheduler có lưu số booking đã xử lý không?
- **Q229.** Scheduler có bỏ qua booking đã xử lý không?
- **Q230.** Scheduler có xử lý lại booking thất bại không?
- **Q231.** Scheduler có phát sinh event khi booking hết hạn không?
- **Q232.** Scheduler có đảm bảo không xử lý trùng booking không?

---

# Chapter 13. Audit & Logging

## Audit

- **Q233.** Booking Service có ghi Audit Log khi tạo booking không?
- **Q234.** Booking Service có ghi Audit Log khi cập nhật booking không?
- **Q235.** Booking Service có ghi Audit Log khi hủy booking không?
- **Q236.** Booking Service có ghi Audit Log khi booking hết hạn không?
- **Q237.** Booking Service có ghi Audit Log khi trạng thái booking thay đổi không?
- **Q238.** Booking Service có ghi người thực hiện thao tác không?
- **Q239.** Booking Service có ghi thời gian thao tác không?
- **Q240.** Booking Service có ghi IP của request không?
- **Q241.** Booking Service có ghi User Agent không?
- **Q242.** Booking Service có ghi Correlation ID không?

## Logging

- **Q243.** Booking Service có log request tạo booking không?
- **Q244.** Booking Service có log request hủy booking không?
- **Q245.** Booking Service có log scheduler không?
- **Q246.** Booking Service có log payment event không?
- **Q247.** Booking Service có log booking expired không?
- **Q248.** Booking Service có log booking paid không?
- **Q249.** Booking Service có log exception không?
- **Q250.** Booking Service có log retry không?
- **Q251.** Booking Service có log rollback transaction không?
- **Q252.** Booking Service có log duplicate request không?

---

# Chapter 14. Booking Query

## User Booking

- **Q253.** User có được xem danh sách booking của chính mình không?
- **Q254.** User có được xem booking của người khác không?
- **Q255.** Booking có hỗ trợ tìm kiếm theo Booking Code không?
- **Q256.** Booking có hỗ trợ tìm kiếm theo User không?
- **Q257.** Booking có hỗ trợ tìm kiếm theo Showtime không?
- **Q258.** Booking có hỗ trợ tìm kiếm theo Status không?
- **Q259.** Booking có hỗ trợ phân trang không?
- **Q260.** Booking có hỗ trợ sắp xếp không?
- **Q261.** Booking có hỗ trợ lọc theo khoảng thời gian không?
- **Q262.** Booking có hỗ trợ tìm kiếm theo Public ID không?

## Admin Query

- **Q263.** Admin có được xem toàn bộ booking không?
- **Q264.** Admin có được tìm kiếm booking theo user không?
- **Q265.** Admin có được tìm kiếm booking theo trạng thái không?
- **Q266.** Admin có được xem booking đã hủy không?
- **Q267.** Admin có được xem booking hết hạn không?
- **Q268.** Admin có được xem booking đã thanh toán không?
- **Q269.** Admin có được xuất danh sách booking không?
- **Q270.** Admin có được xem lịch sử thay đổi booking không?
- **Q271.** Admin có được xem audit log của booking không?
- **Q272.** Admin có được xem reservation của booking không?

---

# Chapter 15. Security

## Authorization

- **Q273.** Booking API có yêu cầu JWT không?
- **Q274.** Booking API có kiểm tra quyền truy cập không?
- **Q275.** User chỉ được truy cập booking của chính mình phải không?
- **Q276.** Admin có được truy cập mọi booking không?
- **Q277.** Booking Service có kiểm tra ownership trước khi hủy booking không?
- **Q278.** Booking Service có kiểm tra ownership trước khi xem chi tiết booking không?
- **Q279.** Booking Service có từ chối request không có token không?
- **Q280.** Booking Service có từ chối token hết hạn không?
- **Q281.** Booking Service có từ chối token không hợp lệ không?
- **Q282.** Booking Service có ghi log các request bị từ chối không?

## Abuse Protection

- **Q283.** Booking Service có giới hạn số request tạo booking không?
- **Q284.** Booking Service có giới hạn số request hủy booking không?
- **Q285.** Booking Service có chống spam booking không?
- **Q286.** Booking Service có chống brute-force Booking Code không?
- **Q287.** Booking Service có phát hiện duplicate request bất thường không?
- **Q288.** Booking Service có giới hạn số booking PENDING của một user không?
- **Q289.** Booking Service có phát hiện hành vi giữ ghế nhưng không thanh toán liên tục không?
- **Q290.** Booking Service có hỗ trợ blacklist user không?
- **Q291.** Booking Service có ghi nhận các hành vi bất thường không?
- **Q292.** Booking Service có phát sinh security event khi phát hiện hành vi bất thường không?
# Part 4 (Q301–Q400)

---

# Chapter 16. Failure & Recovery

## Database Failure

- **Q293.** Nếu Database không kết nối được khi tạo booking thì hệ thống xử lý như thế nào?
- **Q294.** Nếu Database bị lỗi sau khi tạo booking nhưng trước khi commit transaction thì xử lý như thế nào?
- **Q295.** Nếu transaction rollback thì booking có còn tồn tại không?
- **Q296.** Nếu transaction rollback thì reservation được xử lý như thế nào?
- **Q297.** Nếu transaction rollback thì seat lock được xử lý như thế nào?
- **Q298.** Nếu commit thất bại thì booking được xem là thành công hay thất bại?
- **Q299.** Nếu mất kết nối Database trong khi cập nhật booking thì xử lý như thế nào?
- **Q300.** Booking Service có retry khi Database tạm thời không khả dụng không?
- **Q301.** Retry Database có giới hạn số lần không?
- **Q302.** Khi Database phục hồi thì các booking thất bại có được xử lý lại không?

---

## Redis Failure

- **Q303.** Nếu Redis không khả dụng thì có cho phép tạo booking không?
- **Q304.** Nếu Redis mất kết nối trong khi giữ ghế thì xử lý như thế nào?
- **Q305.** Nếu Redis restart thì các reservation đang tồn tại được xử lý như thế nào?
- **Q306.** Nếu Redis bị mất dữ liệu thì Booking Service phát hiện như thế nào?
- **Q307.** Booking Service có cơ chế đồng bộ lại reservation sau khi Redis phục hồi không?
- **Q308.** Booking Service có fallback khi Redis lỗi không?
- **Q309.** Booking Service có từ chối booking nếu không lấy được distributed lock không?
- **Q310.** Nếu Redis timeout thì request được xử lý như thế nào?
- **Q311.** Booking Service có retry khi Redis timeout không?
- **Q312.** Redis failure có được ghi audit hoặc monitoring không?

---

# Chapter 17. Service Communication

## Movie Service

- **Q313.** Nếu Movie Service không phản hồi thì Booking Service xử lý như thế nào?
- **Q314.** Nếu Movie Service timeout thì booking có tiếp tục không?
- **Q315.** Nếu Movie Service trả về dữ liệu không hợp lệ thì booking xử lý như thế nào?
- **Q316.** Nếu Showtime bị xóa sau khi đã validate thì Booking Service xử lý như thế nào?
- **Q317.** Nếu Showtime chuyển sang trạng thái không cho phép đặt trong lúc tạo booking thì xử lý như thế nào?

---

## Payment Service

- **Q318.** Nếu Payment Service không phản hồi thì booking xử lý như thế nào?
- **Q319.** Nếu Payment Event bị mất thì Booking Service xử lý như thế nào?
- **Q320.** Nếu Payment Event đến nhiều lần thì xử lý như thế nào?
- **Q321.** Nếu Payment Event đến sai thứ tự thì xử lý như thế nào?
- **Q322.** Nếu Payment Service bị downtime trong thời gian dài thì booking được xử lý như thế nào?

---

# Chapter 18. Data Consistency

## Consistency

- **Q323.** Booking Service làm gì để đảm bảo một ghế chỉ thuộc một booking hợp lệ?
- **Q324.** Booking Service có kiểm tra dữ liệu trước khi commit không?
- **Q325.** Booking Service có kiểm tra dữ liệu sau khi commit không?
- **Q326.** Booking Service có cơ chế phát hiện dữ liệu không nhất quán không?
- **Q327.** Nếu trạng thái booking và reservation không đồng nhất thì xử lý như thế nào?
- **Q328.** Nếu booking tồn tại nhưng reservation không tồn tại thì xử lý như thế nào?
- **Q329.** Nếu reservation tồn tại nhưng booking không tồn tại thì xử lý như thế nào?
- **Q330.** Nếu seat đã được bán nhưng booking vẫn ở trạng thái PENDING thì xử lý như thế nào?
- **Q331.** Booking Service có cơ chế reconciliation dữ liệu không?
- **Q332.** Booking Service có định kỳ kiểm tra dữ liệu bất thường không?

---

# Chapter 19. Performance

## Performance

- **Q333.** Booking Service hỗ trợ bao nhiêu request tạo booking đồng thời?
- **Q334.** Booking Service có giới hạn số lượng booking xử lý cùng lúc không?
- **Q335.** Booking Service có sử dụng connection pool không?
- **Q336.** Booking Service có sử dụng batch update không?
- **Q337.** Booking Service có phân trang khi đọc dữ liệu không?
- **Q338.** Booking Service có tối ưu truy vấn theo Booking Code không?
- **Q339.** Booking Service có tối ưu truy vấn theo User ID không?
- **Q340.** Booking Service có tối ưu truy vấn theo Showtime ID không?
- **Q341.** Booking Service có index cho các trường tìm kiếm chính không?
- **Q342.** Booking Service có theo dõi thời gian xử lý request không?

---

## Scalability

- **Q343.** Booking Service có hỗ trợ chạy nhiều instance không?
- **Q344.** Booking Service có hỗ trợ horizontal scaling không?
- **Q345.** Booking Service có đảm bảo dữ liệu nhất quán giữa các instance không?
- **Q346.** Scheduler có hoạt động đúng khi chạy nhiều instance không?
- **Q347.** Distributed Lock có hoạt động đúng trên nhiều instance không?
- **Q348.** Có nguy cơ duplicate booking khi scale nhiều instance không?
- **Q349.** Booking Service có stateless không?
- **Q350.** Booking Service có lưu session cục bộ không?
- **Q351.** Booking Service có phụ thuộc vào local memory không?
- **Q352.** Booking Service có hỗ trợ rolling deployment không?

---

# Chapter 20. Monitoring

## Metrics

- **Q353.** Booking Service có theo dõi tổng số booking được tạo không?
- **Q354.** Booking Service có theo dõi số booking thành công không?
- **Q355.** Booking Service có theo dõi số booking thất bại không?
- **Q356.** Booking Service có theo dõi số booking bị hủy không?
- **Q357.** Booking Service có theo dõi số booking hết hạn không?
- **Q358.** Booking Service có theo dõi số reservation đang hoạt động không?
- **Q359.** Booking Service có theo dõi số request bị từ chối không?
- **Q360.** Booking Service có theo dõi số duplicate request không?
- **Q361.** Booking Service có theo dõi số payment event nhận được không?
- **Q362.** Booking Service có theo dõi số event publish thất bại không?

---

## Alerting

- **Q363.** Khi tỷ lệ booking thất bại tăng cao thì có cảnh báo không?
- **Q364.** Khi scheduler ngừng hoạt động thì có cảnh báo không?
- **Q365.** Khi Redis không khả dụng thì có cảnh báo không?
- **Q366.** Khi Database không khả dụng thì có cảnh báo không?
- **Q367.** Khi event publish thất bại liên tục thì có cảnh báo không?
- **Q368.** Khi số booking hết hạn tăng bất thường thì có cảnh báo không?
- **Q369.** Khi duplicate booking tăng bất thường thì có cảnh báo không?
- **Q370.** Khi response time tăng bất thường thì có cảnh báo không?
- **Q371.** Khi lock timeout tăng bất thường thì có cảnh báo không?
- **Q372.** Khi transaction rollback tăng bất thường thì có cảnh báo không?

---

# Chapter 21. Edge Cases

## Edge Cases

- **Q373.** User refresh trình duyệt nhiều lần khi tạo booking thì xử lý như thế nào?
- **Q374.** User gửi nhiều request giống nhau trong vài mili giây thì xử lý như thế nào?
- **Q375.** User đóng trình duyệt ngay sau khi tạo booking thì xử lý như thế nào?
- **Q376.** User mất mạng trong khi tạo booking thì xử lý như thế nào?
- **Q377.** User chuyển sang thiết bị khác trong khi booking đang PENDING thì xử lý như thế nào?
- **Q378.** Booking đang PENDING thì service restart sẽ xử lý như thế nào?
- **Q379.** Booking đang PENDING thì Redis restart sẽ xử lý như thế nào?
- **Q380.** Booking đang PENDING thì Database failover sẽ xử lý như thế nào?
- **Q381.** Booking đang EXPIRED nhưng Payment Success đến ngay sau đó thì xử lý như thế nào?
- **Q382.** Hai Payment Success đến đồng thời thì xử lý như thế nào?

- **Q383.** Hai scheduler cùng xử lý một booking hết hạn thì xử lý như thế nào?
- **Q384.** Hai request hủy booking đến cùng lúc thì xử lý như thế nào?
- **Q385.** Booking đang cập nhật trạng thái thì có cho phép request khác cập nhật không?
- **Q386.** Booking đã ở terminal state có được cập nhật nữa không?
- **Q387.** Booking đã bị soft delete có được truy vấn không?
- **Q388.** Booking bị rollback sau khi đã publish event thì xử lý như thế nào?
- **Q389.** Booking commit thành công nhưng publish event thất bại thì xử lý như thế nào?
- **Q390.** Booking Service có cơ chế khôi phục sau lỗi hệ thống không?
- **Q391.** Booking Service có đảm bảo tính nhất quán sau khi phục hồi không?
- **Q392.** Những tình huống nào được xem là lỗi nghiêm trọng (Critical Error) của Booking Service?