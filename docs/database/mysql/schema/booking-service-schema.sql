-- ============================================================
-- DỊCH VỤ ĐẶT VÉ (BOOKING SERVICE) - CƠ SỞ DỮ LIỆU ĐÃ TỐI ƯU (PRODUCTION READY)
-- Phiên bản MySQL 8+
-- ============================================================

CREATE DATABASE IF NOT EXISTS booking_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE booking_db;

SET NAMES utf8mb4;

-- =====================================================
-- 1. ĐƠN ĐẶT VÉ (BOOKINGS - AGGREGATE ROOT)
-- =====================================================

CREATE TABLE bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Mã định danh nội bộ (Khóa chính tự tăng, dùng cho JOIN nội bộ DB)',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'Mã định danh UUID công khai dạng chuỗi VARCHAR(36) (Ví dụ: 550e8400-e29b-41d4-a716-446655440000)',

    booking_code VARCHAR(50) NOT NULL
        COMMENT 'Mã đơn hàng hiển thị cho khách (Ví dụ: BK20260721-8899)',

    user_id BIGINT NOT NULL
        COMMENT 'ID người dùng (Liên kết tới User Service)',

    counter_customer_account_id BIGINT NULL
        COMMENT 'ID tài khoản thành viên đang được phục vụ tại quầy; không thay thế chủ sở hữu đơn là nhân viên',

    counter_customer_name VARCHAR(150) NULL
        COMMENT 'Tên khách được nhân viên ghi nhận tại quầy',

    counter_customer_phone VARCHAR(30) NULL
        COMMENT 'Số điện thoại liên hệ của khách tại quầy',

    counter_customer_email VARCHAR(254) NULL
        COMMENT 'Email liên hệ của khách tại quầy; không tự động dùng email nhân viên',

    showtime_id BIGINT NOT NULL
        COMMENT 'ID suất chiếu (Liên kết tới Movie/Showtime Service)',

    showtime_public_id VARCHAR(36),

    movie_id BIGINT NOT NULL
        COMMENT 'ID phim tại thời điểm đặt vé (Snapshot)',

    cinema_id BIGINT NOT NULL
        COMMENT 'ID rạp chiếu tại thời điểm đặt vé (Snapshot)',

    cinema_public_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL
        COMMENT 'UUID rạp dùng để khóa phạm vi vận hành của Manager',

    auditorium_id BIGINT NOT NULL
        COMMENT 'ID phòng chiếu tại thời điểm đặt vé (Snapshot)',

    ticket_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Tổng tiền vé xem phim (chưa áp mã giảm giá)',

    food_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Tổng tiền đồ ăn/thức uống đi kèm',

    service_fee DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Phí dịch vụ/tiện ích',

    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Tổng tiền thuế (VAT)',

    promotion_discount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Số tiền giảm giá từ chương trình khuyến mãi',

    voucher_discount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Số tiền giảm từ Promotion Engine (AUTO, VOUCHER, COUPON)',

    promotion_reservation_public_id VARCHAR(36) NULL
        COMMENT 'Phiên giữ ưu đãi tại Promotion Service',

    promotion_selection_fingerprint VARCHAR(64) NULL
        COMMENT 'Dấu vân tay lựa chọn ưu đãi đã khóa cùng checkout',

    applied_promotions_json JSON NULL
        COMMENT 'Snapshot các ưu đãi được Engine áp dụng',

    score_points_used INT NOT NULL DEFAULT 0,

    score_discount DECIMAL(12,2) NOT NULL DEFAULT 0,

    score_hold_code VARCHAR(80) NULL,

    final_amount DECIMAL(12,2) NOT NULL
        COMMENT 'Số tiền thực tế khách phải trả = (Vé + Đồ ăn + Phí + Thuế) - Giảm giá',

    currency VARCHAR(10) NOT NULL DEFAULT 'VND'
        COMMENT 'Đơn vị tiền tệ (VND, USD, THB...)',

    booking_status ENUM(
        'PENDING_PAYMENT',
        'CONFIRMED',
        'COMPLETED',
        'CANCELLED',
        'EXPIRED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT'
        COMMENT 'Trạng thái đơn hàng: Chờ thanh toán, Đã xác nhận, Hoàn thành, Đã hủy, Hết hạn, Đã hoàn tiền',

    payment_status ENUM(
        'PENDING',
        'SUCCESS',
        'FAILED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái giao dịch thanh toán: Chờ xử lý, Thành công, Thất bại, Đã hoàn tiền',

    payment_method_snapshot VARCHAR(50)
        COMMENT 'Phương thức thanh toán (Ví dụ: CREDIT_CARD, MOMO, VNPAY, ZALOPAY)',

    payment_provider VARCHAR(50)
        COMMENT 'Đơn vị cung cấp cổng thanh toán (Ví dụ: Stripe, MoMo, VNPay)',

    payment_reference VARCHAR(100)
        COMMENT 'Mã giao dịch tham chiếu từ phía Cổng thanh toán',

    expires_at DATETIME NOT NULL
        COMMENT 'Thời điểm đơn hàng hết hạn giữ chỗ nếu không thanh toán',

    amount_locked_at DATETIME,

    confirmed_at DATETIME
        COMMENT 'Thời điểm đơn hàng được xác nhận thanh toán thành công',

    completed_at DATETIME
        COMMENT 'Thời điểm đơn hàng hoàn tất (Khách đã xem phim/soát vé xong)',

    cancelled_at DATETIME
        COMMENT 'Thời điểm đơn hàng bị hủy',

    expired_at DATETIME
        COMMENT 'Thời điểm đơn hàng bị hệ thống đánh dấu hết hạn do quá giờ thanh toán',

    refunded_at DATETIME
        COMMENT 'Thời điểm hoàn tiền thành công cho khách',

    cancel_reason_code VARCHAR(50)
        COMMENT 'Mã lý do hủy đơn (Ví dụ: USER_CANCEL, PAYMENT_TIMEOUT, SYSTEM_ERROR)',

    cancel_reason_detail TEXT
        COMMENT 'Mô tả chi tiết lý do hủy đơn',

    note TEXT
        COMMENT 'Ghi chú thêm về đơn hàng',

    version INT NOT NULL DEFAULT 0
        COMMENT 'Phiên bản bản ghi (Dùng cho Optimistic Locking / Khóa lạc quan)',

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Cờ đánh dấu xóa mềm (Soft delete)',

    active_customer_showtime_key VARCHAR(100) GENERATED ALWAYS AS (
        IF(
            booking_status = 'PENDING_PAYMENT' AND is_deleted = FALSE,
            CONCAT(user_id, '_', showtime_id),
            NULL
        )
    ) STORED
        COMMENT 'Database guard: one active pending Booking per customer and Showtime',

    created_by VARCHAR(100)
        COMMENT 'Người/Hệ thống tạo đơn',

    updated_by VARCHAR(100)
        COMMENT 'Người/Hệ thống cập nhật đơn gần nhất',

    deleted_by VARCHAR(100)
        COMMENT 'Người/Hệ thống thực hiện xóa',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo đơn',

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật gần nhất',

    deleted_at DATETIME
        COMMENT 'Thời điểm xóa mềm',

    -- Ràng buộc duy nhất
    CONSTRAINT uk_booking_public UNIQUE(public_id),
    CONSTRAINT uk_booking_code UNIQUE(booking_code),
    CONSTRAINT uk_booking_promotion_reservation UNIQUE(promotion_reservation_public_id),
    CONSTRAINT uk_active_customer_showtime_booking UNIQUE(active_customer_showtime_key),

    -- Ràng buộc kiểm tra số tiền hợp lệ (không âm)
    CONSTRAINT chk_booking_ticket_amount CHECK (ticket_amount >= 0),
    CONSTRAINT chk_booking_food_amount CHECK (food_amount >= 0),
    CONSTRAINT chk_booking_service_fee CHECK (service_fee >= 0),
    CONSTRAINT chk_booking_tax CHECK (tax_amount >= 0),
    CONSTRAINT chk_booking_discount CHECK (promotion_discount >= 0),
    CONSTRAINT chk_booking_final_amount CHECK (final_amount >= 0),

    -- Các Chỉ mục (Indexes) hỗ trợ tìm kiếm nhanh
    INDEX idx_booking_user(user_id),
    INDEX idx_booking_showtime(showtime_id),
    INDEX idx_booking_showtime_public(showtime_public_id),
    INDEX idx_booking_status(booking_status),
    INDEX idx_booking_payment(payment_status),
    INDEX idx_booking_created(created_at),
    INDEX idx_booking_status_expires(booking_status, expires_at),
    INDEX idx_booking_payment_ready(booking_status, amount_locked_at, expires_at),
    
    -- Composite Indexes tối ưu truy vấn phức hợp thường gặp
    INDEX idx_booking_user_status(user_id, booking_status),
    INDEX idx_booking_user_created(user_id, created_at),
    INDEX idx_booking_status_created(booking_status, created_at),
    INDEX idx_booking_payment_status(payment_status, booking_status)
)
ENGINE=InnoDB
COMMENT='Bảng chính lưu thông tin Đơn đặt vé (Aggregate Root)';


-- =====================================================
-- 2. CHI TIẾT VÉ XEM PHIM (BOOKING TICKETS)
-- =====================================================

CREATE TABLE booking_tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID công khai của vé dạng VARCHAR(36)',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    ticket_code VARCHAR(50) NOT NULL
        COMMENT 'Mã vé xem phim (Ví dụ: TK-8899-01)',

    seat_id BIGINT NOT NULL
        COMMENT 'ID ghế trong hệ thống rạp',

    seat_public_id VARCHAR(36),

    seat_label VARCHAR(20) NOT NULL
        COMMENT 'Tên hiển thị của ghế (Ví dụ: A12, VIP-F05)',

    seat_row VARCHAR(5)
        COMMENT 'Hàng ghế (Ví dụ: A, B, C)',

    seat_column INT
        COMMENT 'Số cột/vị trí ghế (Ví dụ: 1, 2, 12)',

    seat_type VARCHAR(30)
        COMMENT 'Loại ghế (Ví dụ: STANDARD, VIP, COUPLE)',

    ticket_price DECIMAL(12,2) NOT NULL
        COMMENT 'Giá vé niêm yết tại thời điểm mua',

    movie_title VARCHAR(255)
        COMMENT 'Tên phim (Snapshot tại thời điểm in vé)',

    cinema_name VARCHAR(255)
        COMMENT 'Tên rạp (Snapshot)',

    auditorium_name VARCHAR(255)
        COMMENT 'Tên phòng chiếu (Snapshot)',

    showtime_start DATETIME
        COMMENT 'Giờ chiếu phim',

    showtime_end DATETIME
        COMMENT 'Giờ kết thúc phim',

    movie_format VARCHAR(30)
        COMMENT 'Định dạng phim (Ví dụ: 2D, 3D, IMAX, 4DX)',

    audio_language VARCHAR(30)
        COMMENT 'Ngôn ngữ lồng tiếng/thuyết minh (Ví dụ: VIETNAMESE, ENGLISH)',

    subtitle_language VARCHAR(30)
        COMMENT 'Ngôn ngữ phụ đề (Ví dụ: VIETNAMESE)',

    qr_code VARCHAR(255)
        COMMENT 'Chuỗi dữ liệu / Đường dẫn ảnh QR Code để soát vé',

    barcode VARCHAR(255)
        COMMENT 'Chuỗi dữ liệu Barcode',

    status ENUM(
        'ACTIVE',
        'USED',
        'CANCELLED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'Trạng thái vé: Hoạt động (Chờ soát vé), Đã sử dụng, Đã hủy, Đã hoàn tiền',

    used_at DATETIME
        COMMENT 'Thời điểm vé được quét/soát vào phòng chiếu',

    used_by_account_id BIGINT
        COMMENT 'Tài khoản nhân viên đã soát vé thành công',

    used_cinema_public_id VARCHAR(36)
        COMMENT 'Rạp ghi nhận lượt vào',

    used_gate_label VARCHAR(80)
        COMMENT 'Cửa hoặc khu vực soát vé',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo vé',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật vé gần nhất',

    CONSTRAINT uk_ticket_public UNIQUE(public_id),
    CONSTRAINT uk_ticket_code UNIQUE(ticket_code),
    CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT chk_ticket_price CHECK (ticket_price >= 0),

    INDEX idx_ticket_booking(booking_id),
    INDEX idx_ticket_status(status),
    INDEX idx_ticket_seat(seat_id),
    INDEX idx_ticket_booking_status(booking_id, status)
)
ENGINE=InnoDB
COMMENT='Bảng lưu thông tin chi tiết từng vé xem phim';

CREATE TABLE ticket_scan_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    ticket_id BIGINT NULL,
    entered_code VARCHAR(255) NOT NULL,
    employee_account_id BIGINT NOT NULL,
    cinema_public_id VARCHAR(36) NOT NULL,
    gate_label VARCHAR(80) NULL,
    result ENUM(
        'ADMITTED', 'ALREADY_USED', 'NOT_FOUND', 'WRONG_CINEMA',
        'TOO_EARLY', 'TOO_LATE', 'REFUNDED', 'CANCELLED',
        'NOT_PAID', 'INVALID_STATUS'
    ) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_message VARCHAR(500) NOT NULL,
    scanned_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ticket_scan_event_public UNIQUE(public_id),
    CONSTRAINT fk_ticket_scan_event_ticket FOREIGN KEY(ticket_id) REFERENCES booking_tickets(id),
    INDEX idx_ticket_scan_employee_time(employee_account_id, scanned_at),
    INDEX idx_ticket_scan_cinema_time(cinema_public_id, scanned_at),
    INDEX idx_ticket_scan_result_time(result, scanned_at),
    INDEX idx_ticket_scan_ticket(ticket_id)
) ENGINE=InnoDB
COMMENT='Nhật ký bất biến của tất cả lượt quét vé thành công và bị từ chối';

CREATE TABLE ticket_gate_handoffs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    employee_account_id BIGINT NOT NULL,
    cinema_public_id VARCHAR(36) NOT NULL,
    shift_date DATE NOT NULL,
    gate_label VARCHAR(80) NULL,
    total_scans INT NOT NULL DEFAULT 0,
    successful_scans INT NOT NULL DEFAULT 0,
    rejected_scans INT NOT NULL DEFAULT 0,
    unresolved_incidents INT NOT NULL DEFAULT 0,
    note VARCHAR(1000) NULL,
    handed_off_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ticket_gate_handoff_public UNIQUE(public_id),
    CONSTRAINT uk_ticket_gate_handoff_employee_day UNIQUE(employee_account_id, shift_date),
    INDEX idx_ticket_gate_handoff_cinema_day(cinema_public_id, shift_date)
) ENGINE=InnoDB
COMMENT='Biên bản bàn giao cửa soát vé theo nhân viên và ngày';


-- =====================================================
-- 3. LỊCH SỬ THAY ĐỔI TRẠNG THÁI ĐƠN HÀNG
-- =====================================================

CREATE TABLE booking_status_histories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    from_status VARCHAR(50)
        COMMENT 'Trạng thái trước khi chuyển',

    to_status VARCHAR(50) NOT NULL
        COMMENT 'Trạng thái mới sau khi chuyển',

    reason VARCHAR(255)
        COMMENT 'Lý do thay đổi trạng thái',

    source VARCHAR(50)
        COMMENT 'Nguồn phát sinh chuyển trạng thái (Ví dụ: USER_WEB, PAYMENT_WEBHOOK, CRON_JOB)',

    changed_by VARCHAR(100)
        COMMENT 'Người hoặc Hệ thống thực hiện thay đổi',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm ghi nhận lịch sử',

    CONSTRAINT fk_history_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_history_booking(booking_id),
    INDEX idx_history_status(to_status),
    INDEX idx_history_booking_created(booking_id, created_at)
)
ENGINE=InnoDB
COMMENT='Lịch sử chuyển đổi trạng thái của đơn hàng';


-- =====================================================
-- 4. SNAPSHOT THÔNG TIN ĐƠN HÀNG
-- =====================================================

CREATE TABLE booking_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID snapshot công khai dạng VARCHAR(36)',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    movie_id BIGINT
        COMMENT 'Mã phim',

    movie_title VARCHAR(255)
        COMMENT 'Tên phim',

    original_title VARCHAR(255)
        COMMENT 'Tên gốc của phim (Tiếng Anh/Tiếng gốc)',

    movie_poster VARCHAR(500)
        COMMENT 'Đường dẫn ảnh Poster phim',

    duration INT
        COMMENT 'Thời lượng phim (Phút)',

    age_rating VARCHAR(20)
        COMMENT 'Nhãn phân loại độ tuổi (Ví dụ: P, K, T13, T16, T18)',

    showtime_id BIGINT
        COMMENT 'Mã suất chiếu',

    showtime_start DATETIME
        COMMENT 'Giờ chiếu bắt đầu',

    showtime_end DATETIME
        COMMENT 'Giờ chiếu kết thúc',

    cinema_id BIGINT
        COMMENT 'Mã rạp',

    cinema_name VARCHAR(255)
        COMMENT 'Tên rạp chiếu',

    auditorium_id BIGINT
        COMMENT 'Mã phòng chiếu',

    auditorium_name VARCHAR(255)
        COMMENT 'Tên phòng chiếu',

    seat_count INT
        COMMENT 'Số lượng ghế đã đặt',

    promotion_code VARCHAR(100)
        COMMENT 'Mã khuyến mãi đã áp dụng',

    promotion_name VARCHAR(255)
        COMMENT 'Tên chương trình khuyến mãi',

    snapshot_json JSON 
        COMMENT 'Dữ liệu JSON đóng gói toàn bộ thông tin đơn hàng để render UI nhanh mà không cần JOIN nhiều dịch vụ',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm chụp snapshot',

    CONSTRAINT uk_snapshot_public UNIQUE(public_id),
    CONSTRAINT fk_snapshot_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_snapshot_booking(booking_id)
)
ENGINE=InnoDB
COMMENT='Bảng lưu Snapshot thông tin cố định của phim, rạp, suất chiếu tại thời điểm đặt';


-- =====================================================
-- 5. ĐẶT GIỮ GHẾ TẠM THỜI (SEAT RESERVATIONS)
-- =====================================================

CREATE TABLE seat_reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID giữ chỗ dạng VARCHAR(36)',

    reservation_code VARCHAR(50) NOT NULL
        COMMENT 'Mã giữ chỗ tạm thời',

    user_id BIGINT NOT NULL
        COMMENT 'ID người dùng đang giữ chỗ',

    showtime_id BIGINT NOT NULL
        COMMENT 'ID suất chiếu',

    seat_id BIGINT NOT NULL,

    showtime_public_id VARCHAR(36),

    seat_public_id VARCHAR(36)
        COMMENT 'ID ghế đang chọn',

    seat_label VARCHAR(20) NOT NULL
        COMMENT 'Tên ghế',

    seat_type VARCHAR(30)
        COMMENT 'Loại ghế',

    reservation_source ENUM('WEB', 'MOBILE', 'ADMIN', 'KIOSK') NOT NULL DEFAULT 'WEB'
        COMMENT 'Kênh đặt giữ chỗ',

    redis_lock_key VARCHAR(255)
        COMMENT 'Khóa tham chiếu Redis Distributed Lock',

    expires_at DATETIME NOT NULL
        COMMENT 'Thời điểm hết hạn giữ ghế (thường sau 5-10 phút)',

    reserved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm bắt đầu giữ ghế',

    status ENUM('HELD', 'BOOKED', 'EXPIRED', 'RELEASED') NOT NULL DEFAULT 'HELD'
        COMMENT 'Trạng thái giữ chỗ: Đang giữ (HELD), Đã thanh toán (BOOKED), Hết hạn (EXPIRED), Chủ động nhả ghế (RELEASED)',

    expired_reason VARCHAR(255)
        COMMENT 'Lý do giải phóng ghế',

    booking_id BIGINT
        COMMENT 'Mã đơn hàng liên kết (sau khi khách tạo booking thành công)',

    version INT NOT NULL DEFAULT 0
        COMMENT 'Phiên bản bản ghi (Khóa lạc quan)',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo bản ghi',

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật bản ghi',
        
    active_unique_key VARCHAR(255) GENERATED ALWAYS AS (IF(status IN ('HELD', 'BOOKED'), CONCAT(showtime_id, '_', seat_id), NULL)) STORED
        COMMENT 'Cột ảo để đảm bảo 1 ghế chỉ có tối đa 1 active reservation',

    CONSTRAINT uk_reservation_public UNIQUE(public_id),
    CONSTRAINT uk_reservation_code UNIQUE(reservation_code),
    CONSTRAINT fk_reservation_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT uk_active_seat_reservation UNIQUE(active_unique_key),

    INDEX idx_reservation_user(user_id),
    INDEX idx_reservation_showtime(showtime_id),
    INDEX idx_reservation_booking(booking_id),
    INDEX idx_reservation_showtime_public_status(showtime_public_id, status, expires_at),
    INDEX idx_reservation_seat_public(seat_public_id),
    INDEX idx_reservation_seat(seat_id),
    INDEX idx_reservation_status(status),
    INDEX idx_reservation_expire(expires_at),
    INDEX idx_reservation_showtime_status(showtime_id, status),
    INDEX idx_reservation_expire_status(expires_at, status)
)
ENGINE=InnoDB
COMMENT='Quản lý việc giữ ghế tạm thời trong lúc khách hàng thao tác thanh toán';


-- =====================================================
-- 6. ĐƠN ĐẶT ĐỒ ĂN / NƯỚC UỐNG (FOOD ORDERS & ITEMS)
-- =====================================================

CREATE TABLE booking_food_catalog_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Khóa chính tự tăng',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã sản phẩm',
    name VARCHAR(255) NOT NULL COMMENT 'Tên sản phẩm',
    product_type ENUM('FOOD', 'DRINK', 'COMBO') NOT NULL COMMENT 'Loại sản phẩm: Đồ ăn, Nước uống, Combo',
    image_url VARCHAR(500) COMMENT 'Đường dẫn ảnh sản phẩm',
    price DECIMAL(12,2) NOT NULL COMMENT 'Giá bán',
    active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cờ kích hoạt',
    sellable BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cho phép bán',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Đã xóa mềm',
    disabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Bị vô hiệu hóa',
    currency VARCHAR(10) NOT NULL DEFAULT 'VND' COMMENT 'Loại tiền tệ',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật'
) ENGINE=InnoDB COMMENT='Bảng danh mục đồ ăn, thức uống và combo thực tế (Catalog)';

CREATE TABLE booking_food_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID đơn đồ ăn dạng VARCHAR(36)',

    booking_id BIGINT NULL
        COMMENT 'Mã đơn đặt vé liên kết (FK) (NULL nếu là đơn rời)',

    user_id BIGINT NULL
        COMMENT 'Mã khách hàng (Dùng khi mua rời không qua booking)',

    total_quantity INT NOT NULL DEFAULT 0
        COMMENT 'Tổng số lượng các món đồ ăn/nước uống',

    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Tạm tính tiền đồ ăn',

    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Tiền giảm giá riêng cho đồ ăn',

    final_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Tổng tiền đồ ăn sau giảm giá',

    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'REFUNDED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái đơn đồ ăn',

    payment_status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái giao dịch thanh toán của đơn đồ ăn',

    payment_method_snapshot VARCHAR(50)
        COMMENT 'Phương thức thanh toán (Ví dụ: CREDIT_CARD, MOMO)',

    payment_provider VARCHAR(50)
        COMMENT 'Đơn vị cung cấp cổng thanh toán (Ví dụ: Stripe, MoMo)',

    payment_reference VARCHAR(100)
        COMMENT 'Mã giao dịch tham chiếu từ phía Cổng thanh toán',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật',

    version INT NOT NULL DEFAULT 0
        COMMENT 'Phiên bản bản ghi (Dùng cho Optimistic Locking / Khóa lạc quan)',

    CONSTRAINT uk_food_order_public UNIQUE(public_id),
    CONSTRAINT fk_food_order_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),

    INDEX idx_food_booking(booking_id),
    INDEX idx_food_status(status),
    INDEX idx_food_order_booking_status(booking_id, status)
)
ENGINE=InnoDB
COMMENT='Đơn hàng bắp nước/đồ ăn đi kèm đơn đặt vé';


CREATE TABLE booking_food_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    food_order_id BIGINT NOT NULL
        COMMENT 'Mã đơn đồ ăn liên kết (FK)',

    product_id BIGINT NOT NULL
        COMMENT 'ID sản phẩm bắp/nước',

    product_code VARCHAR(50)
        COMMENT 'Mã sản phẩm',

    product_name VARCHAR(255) NOT NULL
        COMMENT 'Tên sản phẩm (Ví dụ: Combo Popcorn L + PepSi 22oz)',

    product_type ENUM('FOOD', 'DRINK', 'COMBO') NOT NULL
        COMMENT 'Loại sản phẩm: Đồ ăn, Nước uống, Combo',

    product_image VARCHAR(500)
        COMMENT 'Đường dẫn ảnh sản phẩm',

    quantity INT NOT NULL
        COMMENT 'Số lượng mua',

    unit_price DECIMAL(12,2) NOT NULL
        COMMENT 'Đơn giá tại thời điểm mua',

    subtotal DECIMAL(12,2) NOT NULL
        COMMENT 'Thành tiền = Số lượng * Đơn giá',

    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT 'Giảm giá riêng cho item này',

    final_amount DECIMAL(12,2) NOT NULL
        COMMENT 'Thành tiền cuối cùng của item',

    snapshot_json JSON
        COMMENT 'Dữ liệu Snapshot chi tiết món ăn (thành phần trong combo...)',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo',

    CONSTRAINT fk_food_item_order FOREIGN KEY (food_order_id) REFERENCES booking_food_orders(id),
    CONSTRAINT chk_food_quantity CHECK (quantity > 0),
    CONSTRAINT chk_food_price CHECK (unit_price >= 0),
    CONSTRAINT chk_food_final CHECK (final_amount >= 0),

    INDEX idx_food_item_order(food_order_id),
    INDEX idx_food_item_product(product_id)
)
ENGINE=InnoDB
COMMENT='Chi tiết từng món bắp/nước trong đơn hàng';


-- =====================================================
-- 7. SNAPSHOT CHI TIẾT TÍNH GIÁ (PRICING SNAPSHOT)
-- =====================================================

CREATE TABLE booking_price_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    currency VARCHAR(10) NOT NULL DEFAULT 'VND'
        COMMENT 'Loại tiền tệ',

    pricing_engine_version VARCHAR(20) DEFAULT 'v1.0'
        COMMENT 'Phiên bản thuật toán/quy tắc tính giá',
        
    pricing_breakdown_json JSON 
        COMMENT 'Chi tiết từng dòng tính tiền, công thức VAT, danh sách quy tắc giá đã áp dụng',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm lưu công thức tính giá',

    CONSTRAINT fk_price_snapshot_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT uk_price_snapshot_booking UNIQUE (booking_id),
    INDEX idx_price_booking(booking_id)
)
ENGINE=InnoDB
COMMENT='Lưu vết chi tiết quy tắc tính giá của Pricing Engine';


-- =====================================================
-- 8. SỰ KIỆN THANH TOÁN VÀ HOÀN TIỀN (PAYMENTS & REFUNDS)
-- =====================================================

CREATE TABLE booking_payment_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID sự kiện thanh toán dạng VARCHAR(36)',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    payment_id BIGINT
        COMMENT 'ID giao dịch ở Payment Service',

    payment_public_id VARCHAR(36)
        COMMENT 'UUID công khai của giao dịch ở Payment Service; authoritative cho contract mới',

    schema_version VARCHAR(20) NOT NULL DEFAULT '1.0'
        COMMENT 'Phiên bản normalized Payment result contract',

    transaction_id VARCHAR(100)
        COMMENT 'Mã giao dịch nội bộ hệ thống thanh toán',

    gateway_transaction_id VARCHAR(100)
        COMMENT 'Mã giao dịch trả về từ ngân hàng/cổng thanh toán',

    payment_provider VARCHAR(50)
        COMMENT 'Nhà cung cấp thanh toán (MoMo, VNPay, ZaloPay, Visa)',

    payment_method VARCHAR(50)
        COMMENT 'Phương thức (QR_CODE, E_WALLET, ATM_CARD, CREDIT_CARD)',

    event_type ENUM(
        'PAYMENT_CREATED',
        'PAYMENT_PENDING',
        'PAYMENT_SUCCESS',
        'PAYMENT_FAILED',
        'PAYMENT_TIMEOUT',
        'PAYMENT_CANCELLED',
        'REFUND_CREATED',
        'REFUND_SUCCESS',
        'REFUND_FAILED'
    ) NOT NULL
        COMMENT 'Loại sự kiện thanh toán phát sinh',

    amount DECIMAL(12,2) NOT NULL
        COMMENT 'Số tiền của giao dịch thanh toán này',

    currency VARCHAR(10) NOT NULL DEFAULT 'VND'
        COMMENT 'Đơn vị tiền tệ',

    request_payload JSON
        COMMENT 'Dữ liệu gửi đi cho Cổng thanh toán (Webhook/Callback Input)',

    response_payload JSON
        COMMENT 'Dữ liệu phản hồi nhận từ Cổng thanh toán',

    payload_hash VARCHAR(64)
        COMMENT 'SHA-256 của normalized Payment result để phát hiện eventId reuse',

    processing_outcome VARCHAR(40) NOT NULL DEFAULT 'ACCEPTED'
        COMMENT 'Kết quả Booking xử lý receipt: ACCEPTED hoặc RECONCILIATION_REQUIRED',

    processing_error_code VARCHAR(100)
        COMMENT 'Mã lỗi nghiệp vụ ổn định nếu receipt bị từ chối',

    reconciliation_task_public_id VARCHAR(36)
        COMMENT 'UUID task đối soát được tạo cho receipt bất thường',

    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái xử lý sự kiện',

    occurred_at DATETIME NOT NULL
        COMMENT 'Thời điểm thực tế xảy ra giao dịch tại Cổng thanh toán',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm hệ thống ghi nhận sự kiện',

    CONSTRAINT uk_payment_event_public UNIQUE(public_id),
    CONSTRAINT fk_payment_event_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),

    INDEX idx_payment_booking(booking_id),
    INDEX idx_payment_public(payment_public_id),
    INDEX idx_payment_transaction(transaction_id),
    INDEX idx_payment_gateway(gateway_transaction_id),
    INDEX idx_payment_event(event_type),
    INDEX idx_payment_booking_event(booking_id, event_type),
    INDEX idx_payment_processing(processing_outcome, created_at)
)
ENGINE=InnoDB
COMMENT='Lịch sử nhật ký các sự kiện tương tác với Cổng thanh toán';


CREATE TABLE booking_refunds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID yêu cầu hoàn tiền dạng VARCHAR(36)',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    refund_code VARCHAR(50) NOT NULL
        COMMENT 'Mã nghiệp vụ hoàn tiền (Ví dụ: RF20260721-001)',

    payment_event_id BIGINT
        COMMENT 'Sự kiện thanh toán gốc được hoàn tiền (FK)',

    refund_reason_code VARCHAR(50)
        COMMENT 'Mã lý do hoàn tiền (Ví dụ: SHOWTIME_CANCELLED, USER_REQUEST)',

    refund_reason_detail TEXT
        COMMENT 'Chi tiết lý do hoàn tiền',

    refund_amount DECIMAL(12,2) NOT NULL
        COMMENT 'Số tiền thực hiện hoàn lại cho khách',

    refunded_by VARCHAR(100)
        COMMENT 'Tài khoản nhân viên/Hệ thống thực hiện hoàn tiền',

    refund_method VARCHAR(50)
        COMMENT 'Kênh nhận tiền hoàn',

    refund_reference VARCHAR(100)
        COMMENT 'Mã giao dịch hoàn tiền trả về từ Cổng thanh toán',

    status ENUM('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái tiến trình hoàn tiền',

    requested_at DATETIME NOT NULL
        COMMENT 'Thời điểm gửi yêu cầu hoàn tiền',

    completed_at DATETIME
        COMMENT 'Thời điểm tiền đã hoàn thành công',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo bản ghi',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật bản ghi',

    CONSTRAINT uk_refund_public UNIQUE(public_id),
    CONSTRAINT uk_refund_code UNIQUE(refund_code),
    CONSTRAINT fk_refund_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_refund_payment_event FOREIGN KEY (payment_event_id) REFERENCES booking_payment_events(id),

    INDEX idx_refund_booking(booking_id),
    INDEX idx_refund_status(status)
)
ENGINE=InnoDB
COMMENT='Quản lý thông tin và lịch sử xử lý hoàn tiền';


-- =====================================================
-- 9. HẠ TẦNG MICROSERVICES (OUTBOX, INBOX, IDEMPOTENCY, DLQ)
-- =====================================================

CREATE TABLE booking_outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    aggregate_type VARCHAR(100) NOT NULL
        COMMENT 'Tên Aggregate (Ví dụ: BOOKING, TICKET)',

    aggregate_id BIGINT NOT NULL,

    aggregate_public_id VARCHAR(36)
        COMMENT 'ID của Aggregate liên quan',

    event_id VARCHAR(36) NOT NULL
        COMMENT 'UUID duy nhất của Event (Chống gửi trùng)',

    event_type VARCHAR(100) NOT NULL
        COMMENT 'Tên loại Sự kiện (Ví dụ: BookingConfirmedEvent, BookingCancelledEvent)',

    event_version INT NOT NULL DEFAULT 1
        COMMENT 'Phiên bản cấu trúc của Event',

    payload JSON NOT NULL
        COMMENT 'Nội dung chi tiết của Event phát đi (Sẽ đẩy qua Kafka/RabbitMQ)',

    headers JSON
        COMMENT 'Thông tin Header bổ sung (Trace ID, Authentication...)',

    status ENUM('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái phát sự kiện: Chờ phát, Đang xử lý, Đã phát thành công, Thất bại',

    retry_count INT NOT NULL DEFAULT 0
        COMMENT 'Số lần đã thử phát lại Event',

    next_retry_at DATETIME
        COMMENT 'Thời điểm cho lần thử lại tiếp theo',

    published_at DATETIME
        COMMENT 'Thời điểm phát thành công lên Message Broker',

    error_message TEXT
        COMMENT 'Thông tin lỗi nếu phát thất bại',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo Event',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật',

    CONSTRAINT uk_outbox_event UNIQUE(event_id),
    INDEX idx_outbox_status(status),
    INDEX idx_outbox_retry(next_retry_at),
    INDEX idx_outbox_type(event_type),
    INDEX idx_outbox_aggregate(aggregate_type, aggregate_id),
    INDEX idx_outbox_aggregate_public(aggregate_type, aggregate_public_id),
    INDEX idx_outbox_publish(status, next_retry_at)
)
ENGINE=InnoDB
COMMENT='Transactional Outbox Pattern - Bảo đảm phát Event đáng tin cậy giữa các Service';


CREATE TABLE booking_inbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    event_id VARCHAR(36) NOT NULL
        COMMENT 'UUID duy nhất của Event nhận được từ Service khác',

    source_service VARCHAR(100) NOT NULL
        COMMENT 'Tên Service phát ra Event này (Ví dụ: PaymentService, UserService)',

    aggregate_type VARCHAR(100)
        COMMENT 'Tên Aggregate',

    aggregate_id BIGINT
        COMMENT 'ID Aggregate',

    event_type VARCHAR(100)
        COMMENT 'Tên loại Event nhận được',

    payload JSON
        COMMENT 'Nội dung Event nhận được',

    processed BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Cờ xác nhận đã xử lý Event này chưa (Tránh xử lý lặp lại)',

    processed_at DATETIME
        COMMENT 'Thời điểm xử lý xong',

    error_message TEXT
        COMMENT 'Thông báo lỗi nếu xử lý thất bại',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm nhận Event',

    CONSTRAINT uk_inbox_event UNIQUE(event_id),
    INDEX idx_inbox_processed(processed),
    INDEX idx_inbox_source(source_service),
    INDEX idx_inbox_type(event_type)
)
ENGINE=InnoDB
COMMENT='Inbox Pattern - Đảm bảo xử lý idempotency (không trùng lặp) cho Message Consumer';


CREATE TABLE booking_idempotency_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    idempotency_key VARCHAR(255) NOT NULL
        COMMENT 'Mã Idempotency Key truyền từ Header Client',

    request_hash VARCHAR(255) NOT NULL
        COMMENT 'Mã Hash (SHA256) của Request Body để chống gửi trùng dữ liệu khác nhau',

    user_id BIGINT NOT NULL
        COMMENT 'ID người dùng thực hiện Request',

    endpoint VARCHAR(255) NOT NULL
        COMMENT 'Đường dẫn API Endpoint',

    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING'
        COMMENT 'Trạng thái xử lý (PROCESSING, COMPLETED, FAILED)',

    response_status INT
        COMMENT 'Mã trạng thái HTTP Response trả về lần đầu (200, 201...)',

    response_body JSON,

    locked_until DATETIME,

    resource_public_id VARCHAR(36)
        COMMENT 'Nội dung Response đã trả về trước đó để cache lại trả về cho Client',

    expires_at DATETIME NOT NULL
        COMMENT 'Thời điểm Key hết hiệu lực',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm ghi nhận',

    CONSTRAINT uk_idempotency_scope UNIQUE(user_id, endpoint, idempotency_key),
    INDEX idx_idempotency_user(user_id),
    INDEX idx_idempotency_expire(expires_at),
    INDEX idx_idempotency_status_expire(status, expires_at),
    INDEX idx_idempotency_resource(resource_public_id)
)
ENGINE=InnoDB
COMMENT='Lưu trữ Idempotency Key để chặn trùng lặp API Request trùng lặp';


CREATE TABLE booking_retry_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID task retry dạng VARCHAR(36)',

    task_type ENUM(
        'OUTBOX_PUBLISH',
        'PAYMENT_CALLBACK',
        'REFUND',
        'RECONCILIATION',
        'INBOX_PROCESS'
    ) NOT NULL
        COMMENT 'Phân loại công việc cần Retry lại',

    reference_type VARCHAR(100) NOT NULL
        COMMENT 'Tên loại đối tượng tham chiếu',

    reference_id BIGINT NOT NULL
        COMMENT 'ID đối tượng tham chiếu',

    payload JSON
        COMMENT 'Dữ liệu phục vụ việc chạy lại Task',

    retry_count INT NOT NULL DEFAULT 0
        COMMENT 'Số lần đã thực hiện chạy lại',

    max_retry INT NOT NULL DEFAULT 10
        COMMENT 'Số lần thử lại tối đa cho phép',

    status ENUM('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'DEAD_LETTER') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái tiến trình Retry',

    next_retry_at DATETIME
        COMMENT 'Thời điểm thực hiện Retry tiếp theo',

    last_retry_at DATETIME
        COMMENT 'Thời điểm vừa thực hiện Retry gần nhất',

    error_code VARCHAR(100)
        COMMENT 'Mã lỗi gặp phải',

    error_message TEXT
        COMMENT 'Nội dung chi tiết lỗi',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo Task',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật',

    CONSTRAINT uk_retry_public UNIQUE(public_id),
    INDEX idx_retry_status(status),
    INDEX idx_retry_next(next_retry_at),
    INDEX idx_retry_reference(reference_type, reference_id),
    INDEX idx_retry_scheduler(status, next_retry_at)
)
ENGINE=InnoDB
COMMENT='Lập lịch thử lại (Retry Scheduler) cho các tác vụ bị lỗi tạm thời';


CREATE TABLE booking_dead_letter_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID dead letter dạng VARCHAR(36)',

    event_id VARCHAR(36) NOT NULL
        COMMENT 'UUID sự kiện bị lỗi',

    source_table VARCHAR(100) NOT NULL
        COMMENT 'Bảng nguồn nơi sự kiện bị lỗi (Outbox/Inbox)',

    aggregate_type VARCHAR(100)
        COMMENT 'Tên Aggregate',

    aggregate_id BIGINT
        COMMENT 'ID Aggregate',

    event_type VARCHAR(100)
        COMMENT 'Tên loại Event',

    payload JSON
        COMMENT 'Nội dung sự kiện bị hỏng',

    retry_count INT
        COMMENT 'Số lần đã thử lại trước khi từ bỏ',

    error_code VARCHAR(100)
        COMMENT 'Mã lỗi cuối cùng',

    error_message TEXT
        COMMENT 'Nội dung lỗi chi tiết',

    moved_at DATETIME NOT NULL
        COMMENT 'Thời điểm bị đẩy vào DLQ (Chờ Admin xử lý thủ công)',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo bản ghi',

    CONSTRAINT uk_dead_public UNIQUE(public_id),
    INDEX idx_dead_event(event_id),
    INDEX idx_dead_type(event_type),
    INDEX idx_dead_aggregate(aggregate_type, aggregate_id),
    INDEX idx_dead_event_type(event_type, moved_at)
)
ENGINE=InnoDB
COMMENT='Dead Letter Queue (DLQ) - Lưu trữ các sự kiện bị lỗi nặng không thể tự phục hồi';


CREATE TABLE booking_reconciliation_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID task đối soát dạng VARCHAR(36)',

    booking_id BIGINT NOT NULL
        COMMENT 'Mã đơn hàng liên kết (FK)',

    payment_event_id BIGINT
        COMMENT 'Payment receipt gắn với task; nullable chỉ để tương thích dữ liệu lịch sử',

    payment_reference VARCHAR(100)
        COMMENT 'Mã đối soát từ cổng thanh toán',

    expected_amount DECIMAL(12,2)
        COMMENT 'Số tiền hệ thống kỳ vọng nhận được',

    actual_amount DECIMAL(12,2)
        COMMENT 'Số tiền thực tế ngân hàng/cổng thanh toán báo về',

    expected_currency VARCHAR(10)
        COMMENT 'Currency được khóa tại Booking',

    actual_currency VARCHAR(10)
        COMMENT 'Currency Payment Service gửi về',

    reconciliation_status ENUM('PENDING', 'MATCHED', 'MISMATCH', 'FAILED') NOT NULL DEFAULT 'PENDING'
        COMMENT 'Trạng thái đối soát: Chờ đối soát, Khớp tiền, Lệch tiền, Thất bại',

    reason VARCHAR(255)
        COMMENT 'Mô tả lý do nếu bị sai lệch tiền',

    checked_at DATETIME
        COMMENT 'Thời điểm thực hiện kiểm tra đối soát',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm tạo bản ghi',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật',

    CONSTRAINT uk_reconciliation_public UNIQUE(public_id),
    CONSTRAINT uk_reconciliation_payment_event UNIQUE(payment_event_id),
    CONSTRAINT fk_reconciliation_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_reconciliation_payment_event FOREIGN KEY (payment_event_id) REFERENCES booking_payment_events(id),
    INDEX idx_reconciliation_status(reconciliation_status),
    INDEX idx_reconciliation_booking(booking_id),
    INDEX idx_reconciliation_pending(reconciliation_status, created_at)
)
ENGINE=InnoDB
COMMENT='Quản lý công việc đối soát dữ liệu thanh toán giữa hệ thống và đối tác';


-- =====================================================
-- 10. LOGS VÀ DISTRIBUTED LOCKS
-- =====================================================

CREATE TABLE booking_operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID log vận hành dạng VARCHAR(36)',

    booking_id BIGINT
        COMMENT 'Mã đơn hàng tương ứng (nếu có)',

    operation_type VARCHAR(100) NOT NULL
        COMMENT 'Tên thao tác/hành động (Ví dụ: CREATE_BOOKING, CANCEL_BOOKING)',

    request_id VARCHAR(100)
        COMMENT 'Mã Request ID từ Client',

    trace_id VARCHAR(100)
        COMMENT 'Mã Trace ID phục vụ Distributed Tracing (Zipkin/Jaeger)',

    actor VARCHAR(100)
        COMMENT 'Tài khoản/Dịch vụ thực hiện',

    execution_time_ms BIGINT
        COMMENT 'Thời gian xử lý của tác vụ (tính bằng mili giây)',

    success BOOLEAN NOT NULL
        COMMENT 'Thao tác thành công hay thất bại',

    error_code VARCHAR(100)
        COMMENT 'Mã lỗi phát sinh (nếu có)',

    error_message TEXT
        COMMENT 'Chi tiết thông báo lỗi',

    metadata JSON
        COMMENT 'Dữ liệu ngữ cảnh đi kèm',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm ghi log',

    CONSTRAINT uk_operation_public UNIQUE(public_id),
    CONSTRAINT fk_operation_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_operation_booking(booking_id),
    INDEX idx_operation_type(operation_type),
    INDEX idx_operation_request(request_id),
    INDEX idx_operation_created(created_at)
)
ENGINE=InnoDB
COMMENT='Nhật ký vận hành ứng dụng (Theo dõi hiệu năng và lỗi hệ thống)';


CREATE TABLE booking_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    public_id VARCHAR(36) NOT NULL
        COMMENT 'UUID log audit dạng VARCHAR(36)',

    booking_id BIGINT
        COMMENT 'Mã đơn hàng liên kết',

    actor VARCHAR(100)
        COMMENT 'Người tác động (Admin / User ID / System)',

    action VARCHAR(100)
        COMMENT 'Hành động (Ví dụ: UPDATE_STATUS, FORCE_REFUND)',

    field_name VARCHAR(100)
        COMMENT 'Tên trường bị thay đổi dữ liệu',

    old_value TEXT
        COMMENT 'Giá trị cũ trước khi thay đổi',

    new_value TEXT
        COMMENT 'Giá trị mới sau khi thay đổi',

    request_id VARCHAR(100)
        COMMENT 'Mã Request ID',

    trace_id VARCHAR(100)
        COMMENT 'Mã Trace ID',

    ip_address VARCHAR(50)
        COMMENT 'Địa chỉ IP của người dùng thực hiện',

    user_agent TEXT
        COMMENT 'Thẻ thông tin trình duyệt/thiết bị của Client',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        COMMENT 'Thời điểm ghi nhật ký kiểm toán',

    CONSTRAINT uk_audit_public UNIQUE(public_id),
    CONSTRAINT fk_audit_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_audit_booking(booking_id),
    INDEX idx_audit_actor(actor),
    INDEX idx_audit_created(created_at),
    INDEX idx_audit_booking_created(booking_id, created_at)
)
ENGINE=InnoDB
COMMENT='Nhật ký kiểm toán (Audit Trail) - Truy vết lịch sử chỉnh sửa dữ liệu quan trọng';


CREATE TABLE booking_scheduler_locks (
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    scheduler_name VARCHAR(100) NOT NULL
        COMMENT 'Tên tiến trình chạy ngầm (Ví dụ: BOOKING_EXPIRE, RECONCILIATION)',

    lock_owner VARCHAR(100)
        COMMENT 'Tên Server / Pod đang giữ khóa',

    locked_at DATETIME
        COMMENT 'Thời điểm khóa',

    expires_at DATETIME
        COMMENT 'Thời điểm khóa tự hết hạn (Chống Deadlock nếu Pod bị crash)',

    status ENUM('LOCKED', 'RELEASED') NOT NULL DEFAULT 'LOCKED'
        COMMENT 'Trạng thái khóa: Đang khóa hay Đã giải phóng',

    CONSTRAINT uk_scheduler_name UNIQUE(scheduler_name)
)
ENGINE=InnoDB
COMMENT='Distributed Lock dùng DB cho các Scheduler / Cronjob đa node (Multi-pod)';


CREATE TABLE booking_sequence_numbers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
        COMMENT 'Khóa chính tự tăng',

    sequence_name VARCHAR(100) NOT NULL
        COMMENT 'Tên chuỗi sinh mã (Ví dụ: BOOKING, TICKET, REFUND)',

    sequence_date DATE NOT NULL
        COMMENT 'Ngày áp dụng sinh chuỗi',

    current_value BIGINT NOT NULL DEFAULT 0
        COMMENT 'Giá trị số đếm hiện tại',

    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT 'Thời điểm cập nhật số đếm',

    CONSTRAINT uk_sequence UNIQUE(sequence_name, sequence_date)
)
ENGINE=InnoDB
COMMENT='Bộ sinh chuỗi số tự tăng theo ngày dùng tạo mã nghiệp vụ (Booking Code, Ticket Code...)';


-- =====================================================
-- 11. VIEWS TRUY VẤN
-- =====================================================

-- View tổng quan đơn hàng (Dùng trực tiếp public_id dạng VARCHAR, không cần BIN_TO_UUID)
CREATE OR REPLACE VIEW vw_booking_summary AS
SELECT
    b.id,
    b.public_id,
    b.booking_code,
    b.user_id,
    b.showtime_id,
    b.showtime_public_id,
    b.final_amount,
    b.currency,
    b.booking_status,
    b.payment_status,
    b.amount_locked_at,
    b.expires_at,
    b.created_at,
    (SELECT COUNT(*) FROM booking_tickets bt WHERE bt.booking_id = b.id) AS total_ticket,
    COALESCE((SELECT total_quantity FROM booking_food_orders fo WHERE fo.booking_id = b.id LIMIT 1), 0) AS total_food
FROM bookings b;


-- View cho Dashboard Quản trị (Admin)
CREATE OR REPLACE VIEW vw_booking_admin AS
SELECT
    booking_code,
    user_id,
    booking_status,
    payment_status,
    ticket_amount,
    food_amount,
    promotion_discount,
    final_amount,
    currency,
    expires_at,
    showtime_public_id,
    amount_locked_at,
    created_at
FROM bookings;


-- =====================================================
-- 12. KHỞI TẠO DỮ LIỆU BAN ĐẦU (SEED DATA)
-- =====================================================

-- Khởi tạo giá trị ban đầu cho các chuỗi sinh mã
INSERT INTO booking_sequence_numbers (sequence_name, sequence_date, current_value)
VALUES
    ('BOOKING', CURRENT_DATE, 0),
    ('TICKET', CURRENT_DATE, 0),
    ('REFUND', CURRENT_DATE, 0);

-- Khởi tạo danh sách các Distributed Lock dùng cho tiến trình ngầm
INSERT INTO booking_scheduler_locks (scheduler_name, status)
VALUES
    ('BookingExpirationScheduler', 'RELEASED'),
    ('OutboxEventPublisherScheduler', 'RELEASED'),
    ('RetryTaskScheduler', 'RELEASED'),
    ('ReservationExpirationScheduler', 'RELEASED'),
    ('RECONCILIATION', 'RELEASED');
