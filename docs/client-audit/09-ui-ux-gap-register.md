# UI/UX gap register

## Đã xử lý

- Guard các booking/payment route của CUSTOMER và giữ return-to URL hợp lệ sau login.
- Không đưa CUSTOMER trở lại `/employee`, `/admin` hoặc error route sau login.
- Booking cancelled/expired/refunded giữ ticket code để audit nhưng ẩn QR và hướng dẫn vào rạp.
- STAFF landing/menu chỉ còn cash workflow; POS/mock/check-in/schedule/dashboard/payroll không còn visible trong scope STAFF v1.
- Admin avatar dùng đúng API base; dashboard dẫn tới analytics đang hoạt động.
- Movie admin mặc định `ALL`; score operation có entry point; notification wording được Việt hóa.
- Footer/contact/loyalty và trang chủ đã loại dead link/claim capability chưa được triển khai.
- Client chuyển sang same-origin proxy, bỏ duplicate development render từ `StrictMode`.
- QR ticket có local visual fallback nếu dịch vụ ảnh bên ngoài không khả dụng.
- Score/payment error state không còn crash hoặc redirect im lặng.

## Còn mở (P1/P2)

- Chuẩn hóa health contract giữa các service; hiện custom health có thể là 404/401/500 tùy service.
- Hoàn tất room-list empty-state copy và rà soát technical IDs trên các bảng chi tiết.
- Chạy responsive sweep bổ sung cho desktop nhỏ và các bảng admin nhiều cột.
- Golden regression cho fresh booking, food/voucher, payment failure/success, refund/reconciliation và STAFF cash mutation cần disposable data.
- Chỉ công bố capability TMDB/notification/loyalty nâng cao sau khi dependency và end-to-end flow tương ứng vượt regression.
