# Golden demo flows

| Flow | Trạng thái sau sửa | Bằng chứng / precondition |
|---|---|---|
| Role login | READY | ADMIN, STAFF và CUSTOMER vào đúng surface; CUSTOMER không quay lại route STAFF/ADMIN không tương thích |
| Guest discovery | READY | Home, movie list/detail, cinema/showtime và auth guard hoạt động |
| Movie admin | READY_WITH_OPTIONAL_TMDB_OFF | Saved movie detail chạy, không phát sinh review 502 khi helper vắng mặt |
| Facilities | READY_WITH_PRECONDITIONS | Cinema/room/seat layout đọc dữ liệu hiện hữu ổn định |
| Showtime/pricing | READY | Showtime kiểm tra hiển thị `complete=true`; DISABLED dùng giá STANDARD 80.000đ |
| Food/promotion | NOT_TESTED_END_TO_END | Cần booking disposable để chứng minh recalculation/mutation |
| Customer fresh booking | PARTIAL | Funnel đến seat map và các state seed đã pass; chưa tạo mutation disposable hai lần |
| Payment result/ticket | READY_WITH_SEED | Success hiển thị 4 vé, 305.000đ và payment confirmed; cancelled ẩn QR/admission instruction |
| Admin read operations | READY | Score dashboard và CASH payment detail mở đúng route, không redirect/Console error |
| STAFF cash | READY_WITH_DISPOSABLE_BOOKING_REQUIRED | Role 403 và legacy snapshot 500 đã hết; seed hiện tại trả 409 business eligibility vì không có booking pending đang trong thời hạn thu tiền |

Không dùng seed quá hạn/tương lai để trình diễn thu tiền. Trước demo cần tạo một booking disposable hợp lệ qua application flow, sau đó chạy lookup/collect và DB verification hai lần trên hai record riêng.
