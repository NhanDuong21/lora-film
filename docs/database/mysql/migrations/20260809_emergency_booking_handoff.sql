-- Lưu kết quả xử lý giữ ghế, thanh toán và danh sách khách cần bàn giao
-- khi một phòng chiếu phải đóng khẩn cấp.
-- Chạy trên movie_db trước khi khởi động phiên bản movie-service mới.

ALTER TABLE auditorium_maintenance_windows
    ADD COLUMN emergency_summary_json TEXT NULL
        COMMENT 'Kết quả đồng bộ Booking/Payment và danh sách khách đã thanh toán'
        AFTER extension_note;
