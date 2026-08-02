CREATE TABLE promotion_campaigns (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    code VARCHAR(100) NOT NULL COMMENT 'Mã chiến dịch',
    name VARCHAR(255) NOT NULL COMMENT 'Tên chiến dịch',
    slug VARCHAR(255) NOT NULL COMMENT 'Slug duy nhất',
    description TEXT NULL COMMENT 'Mô tả chiến dịch',
    campaign_type VARCHAR(50) NOT NULL COMMENT 'Loại chiến dịch',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái chiến dịch',
    approval_status VARCHAR(30) NOT NULL COMMENT 'Trạng thái phê duyệt',
    legal_status VARCHAR(30) NOT NULL COMMENT 'Trạng thái pháp lý',
    priority INT NOT NULL DEFAULT 100 COMMENT 'Độ ưu tiên',
    stackable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép cộng dồn',
    exclusive_campaign BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Chiến dịch độc quyền',
    auto_activate BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Tự động kích hoạt',
    auto_complete BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Tự động kết thúc',
    auto_pause_when_budget_exceeded BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Tự động tạm dừng khi hết ngân sách',
    kill_switch BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Dừng khẩn cấp',
    timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh' COMMENT 'Múi giờ',
    start_at DATETIME(6) NOT NULL COMMENT 'Thời gian bắt đầu',
    end_at DATETIME(6) NOT NULL COMMENT 'Thời gian kết thúc',
    published_at DATETIME(6) NULL COMMENT 'Thời gian công bố',
    approved_at DATETIME(6) NULL COMMENT 'Thời gian phê duyệt',
    approved_by CHAR(36) NULL COMMENT 'Người phê duyệt',
    budget_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Tổng ngân sách',
    budget_used DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Ngân sách đã sử dụng',
    budget_reserved DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Ngân sách đang giữ',
    budget_remaining DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Ngân sách còn lại',
    max_redemptions INT NULL COMMENT 'Số lượt sử dụng tối đa',
    redemption_count INT NOT NULL DEFAULT 0 COMMENT 'Số lượt đã sử dụng',
    max_redemptions_per_user INT NOT NULL DEFAULT 1 COMMENT 'Số lượt tối đa mỗi người dùng',
    legal_notification_ref VARCHAR(150) NULL COMMENT 'Mã hồ sơ thông báo khuyến mại',
    remarks TEXT NULL COMMENT 'Ghi chú nội bộ',
    version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản dữ liệu',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm'
) COMMENT = 'Thông tin chiến dịch khuyến mãi';
-- ============================================================
-- PHASE 2 - COUPON
-- ============================================================

CREATE TABLE coupons (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    campaign_public_id CHAR(36) NOT NULL COMMENT 'Public ID chiến dịch',
    code VARCHAR(100) NOT NULL COMMENT 'Mã coupon',
    name VARCHAR(255) NOT NULL COMMENT 'Tên coupon',
    description TEXT NULL COMMENT 'Mô tả coupon',
    coupon_type VARCHAR(50) NOT NULL COMMENT 'Loại coupon',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái',
    distribution_type VARCHAR(30) NOT NULL COMMENT 'Hình thức phát hành',
    stackable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép cộng dồn',
    transferable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép chuyển nhượng',
    reusable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép sử dụng lại',
    auto_apply BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Tự động áp dụng',
    priority INT NOT NULL DEFAULT 100 COMMENT 'Độ ưu tiên',
    max_redemptions INT NULL COMMENT 'Số lượt tối đa toàn hệ thống',
    redemption_count INT NOT NULL DEFAULT 0 COMMENT 'Số lượt đã sử dụng',
    max_redemptions_per_user INT NOT NULL DEFAULT 1 COMMENT 'Số lượt tối đa mỗi người',
    valid_from DATETIME(6) NOT NULL COMMENT 'Ngày bắt đầu hiệu lực',
    valid_to DATETIME(6) NOT NULL COMMENT 'Ngày kết thúc hiệu lực',
    conditions_json JSON NOT NULL COMMENT 'Điều kiện áp dụng',
    actions_json JSON NOT NULL COMMENT 'Hành động giảm giá',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_coupon_public UNIQUE (public_id),
    CONSTRAINT uk_coupon_code UNIQUE (code),
    CONSTRAINT chk_coupon_period CHECK (valid_to > valid_from)
) COMMENT = 'Danh sách coupon';

CREATE INDEX idx_coupon_campaign ON coupons (campaign_public_id);

CREATE INDEX idx_coupon_status ON coupons (status);

CREATE INDEX idx_coupon_valid ON coupons (valid_from, valid_to);

CREATE INDEX idx_coupon_deleted ON coupons (deleted_at);

CREATE INDEX idx_coupon_priority ON coupons (priority);

-- ============================================================
-- COUPON REDEMPTIONS
-- ============================================================

CREATE TABLE coupon_redemptions (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai',
    coupon_public_id CHAR(36) NOT NULL COMMENT 'Public ID coupon',
    campaign_public_id CHAR(36) NOT NULL COMMENT 'Public ID chiến dịch',
    reservation_public_id CHAR(36) NULL COMMENT 'Public ID phiên giữ khuyến mãi',
    booking_public_id CHAR(36) NULL COMMENT 'Public ID booking',
    order_public_id CHAR(36) NULL COMMENT 'Public ID đơn hàng',
    payment_public_id CHAR(36) NULL COMMENT 'Public ID thanh toán',
    user_public_id CHAR(36) NOT NULL COMMENT 'Public ID người dùng',
    customer_phone VARCHAR(20) NULL COMMENT 'Số điện thoại xác thực',
    redeemed_code VARCHAR(100) NOT NULL COMMENT 'Mã coupon đã sử dụng',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái sử dụng',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Số tiền giảm',
    original_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị đơn trước giảm',
    final_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị sau giảm',
    rollback_reason VARCHAR(255) NULL COMMENT 'Lý do rollback',
    rollback_at DATETIME(6) NULL COMMENT 'Thời điểm rollback',
    confirmed_at DATETIME(6) NULL COMMENT 'Thời điểm xác nhận',
    expired_at DATETIME(6) NULL COMMENT 'Thời điểm hết hạn giữ',
    metadata_json JSON NULL COMMENT 'Thông tin bổ sung',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_coupon_redemption_public UNIQUE (public_id)
) COMMENT = 'Lịch sử sử dụng coupon';

CREATE INDEX idx_coupon_redemption_coupon ON coupon_redemptions (coupon_public_id);

CREATE INDEX idx_coupon_redemption_campaign ON coupon_redemptions (campaign_public_id);

CREATE INDEX idx_coupon_redemption_user ON coupon_redemptions (user_public_id);

CREATE INDEX idx_coupon_redemption_order ON coupon_redemptions (order_public_id);

CREATE INDEX idx_coupon_redemption_booking ON coupon_redemptions (booking_public_id);

CREATE INDEX idx_coupon_redemption_status ON coupon_redemptions (status);

CREATE INDEX idx_coupon_redemption_deleted ON coupon_redemptions (deleted_at);

CREATE INDEX idx_coupon_redemption_created ON coupon_redemptions (created_at);

CREATE UNIQUE INDEX uk_coupon_redemption_reservation
    ON coupon_redemptions (reservation_public_id);

-- ============================================================
-- PHASE 3 - VOUCHER
-- ============================================================

CREATE TABLE vouchers (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    campaign_public_id CHAR(36) NULL COMMENT 'Public ID chiến dịch',
    owner_public_id CHAR(36) NOT NULL COMMENT 'Public ID người sở hữu',
    code VARCHAR(100) NOT NULL COMMENT 'Mã voucher',
    name VARCHAR(255) NOT NULL COMMENT 'Tên voucher',
    description TEXT NULL COMMENT 'Mô tả voucher',
    voucher_type VARCHAR(50) NOT NULL COMMENT 'Loại voucher',
    source VARCHAR(50) NOT NULL COMMENT 'Nguồn phát hành',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái',
    issue_reason VARCHAR(255) NULL COMMENT 'Lý do phát hành',
    issued_by CHAR(36) NULL COMMENT 'Người phát hành',
    issued_at DATETIME(6) NOT NULL COMMENT 'Ngày phát hành',
    valid_from DATETIME(6) NOT NULL COMMENT 'Ngày bắt đầu hiệu lực',
    valid_to DATETIME(6) NOT NULL COMMENT 'Ngày hết hiệu lực',
    transferable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép chuyển nhượng',
    stackable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép cộng dồn',
    reusable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép sử dụng lại',
    max_usage INT NOT NULL DEFAULT 1 COMMENT 'Số lượt sử dụng tối đa',
    usage_count INT NOT NULL DEFAULT 0 COMMENT 'Số lượt đã sử dụng',
    face_value DECIMAL(18, 2) NULL COMMENT 'Giá trị voucher',
    minimum_order_amount DECIMAL(18, 2) NULL COMMENT 'Đơn hàng tối thiểu',
    conditions_json JSON NOT NULL COMMENT 'Điều kiện sử dụng',
    actions_json JSON NOT NULL COMMENT 'Nội dung ưu đãi',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_voucher_public UNIQUE (public_id),
    CONSTRAINT uk_voucher_code UNIQUE (code),
    CONSTRAINT chk_voucher_period CHECK (valid_to > valid_from)
) COMMENT = 'Danh sách voucher';

CREATE INDEX idx_voucher_owner ON vouchers (owner_public_id);

CREATE INDEX idx_voucher_campaign ON vouchers (campaign_public_id);

CREATE INDEX idx_voucher_status ON vouchers (status);

CREATE INDEX idx_voucher_source ON vouchers (source);

CREATE INDEX idx_voucher_valid ON vouchers (valid_from, valid_to);

CREATE INDEX idx_voucher_deleted ON vouchers (deleted_at);

-- ============================================================
-- VOUCHER REDEMPTIONS
-- ============================================================

CREATE TABLE voucher_redemptions (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai',
    voucher_public_id CHAR(36) NOT NULL COMMENT 'Public ID voucher',
    campaign_public_id CHAR(36) NULL COMMENT 'Public ID chiến dịch',
    reservation_public_id CHAR(36) NULL COMMENT 'Public ID phiên giữ',
    booking_public_id CHAR(36) NULL COMMENT 'Public ID booking',
    order_public_id CHAR(36) NULL COMMENT 'Public ID đơn hàng',
    payment_public_id CHAR(36) NULL COMMENT 'Public ID thanh toán',
    owner_public_id CHAR(36) NOT NULL COMMENT 'Public ID người sở hữu',
    redeemed_by CHAR(36) NOT NULL COMMENT 'Public ID người sử dụng',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái sử dụng',
    original_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị trước giảm',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị giảm',
    final_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị sau giảm',
    confirmed_at DATETIME(6) NULL COMMENT 'Ngày xác nhận',
    rollback_at DATETIME(6) NULL COMMENT 'Ngày rollback',
    rollback_reason VARCHAR(255) NULL COMMENT 'Lý do rollback',
    expired_at DATETIME(6) NULL COMMENT 'Ngày hết giữ',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_voucher_redemption_public UNIQUE (public_id)
) COMMENT = 'Lịch sử sử dụng voucher';

CREATE INDEX idx_voucher_redemption_voucher ON voucher_redemptions (voucher_public_id);

CREATE INDEX idx_voucher_redemption_owner ON voucher_redemptions (owner_public_id);

CREATE INDEX idx_voucher_redemption_user ON voucher_redemptions (redeemed_by);

CREATE INDEX idx_voucher_redemption_booking ON voucher_redemptions (booking_public_id);

CREATE INDEX idx_voucher_redemption_order ON voucher_redemptions (order_public_id);

CREATE INDEX idx_voucher_redemption_status ON voucher_redemptions (status);

CREATE INDEX idx_voucher_redemption_deleted ON voucher_redemptions (deleted_at);

CREATE INDEX idx_voucher_redemption_created ON voucher_redemptions (created_at);

CREATE UNIQUE INDEX uk_voucher_redemption_reservation
    ON voucher_redemptions (reservation_public_id);

-- ============================================================
-- PHASE 4 - PROMOTION RESERVATION
-- ============================================================

CREATE TABLE promotion_reservations (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    reservation_code VARCHAR(100) NOT NULL COMMENT 'Mã giữ khuyến mãi',
    booking_public_id CHAR(36) NULL COMMENT 'Public ID booking',
    order_public_id CHAR(36) NULL COMMENT 'Public ID đơn hàng',
    payment_public_id CHAR(36) NULL COMMENT 'Public ID thanh toán',
    user_public_id CHAR(36) NOT NULL COMMENT 'Public ID người dùng',
    customer_phone VARCHAR(20) NULL COMMENT 'Số điện thoại đã xác thực dùng để giữ quota coupon',
    reservation_scope_key VARCHAR(80) NULL COMMENT 'Một benefit hiệu lực trên mỗi order/booking',
    campaign_public_id CHAR(36) NULL COMMENT 'Public ID chiến dịch',
    coupon_public_id CHAR(36) NULL COMMENT 'Public ID coupon',
    voucher_public_id CHAR(36) NULL COMMENT 'Public ID voucher',
    reservation_type VARCHAR(30) NOT NULL COMMENT 'Loại giữ',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái giữ',
    original_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị trước giảm',
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị giảm',
    final_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị sau giảm',
    currency VARCHAR(10) NOT NULL DEFAULT 'VND' COMMENT 'Đơn vị tiền tệ',
    reservation_started_at DATETIME(6) NOT NULL COMMENT 'Thời điểm bắt đầu giữ',
    reservation_expired_at DATETIME(6) NOT NULL COMMENT 'Thời điểm hết hạn giữ',
    confirmed_at DATETIME(6) NULL COMMENT 'Thời điểm xác nhận',
    cancelled_at DATETIME(6) NULL COMMENT 'Thời điểm hủy',
    cancelled_reason VARCHAR(255) NULL COMMENT 'Lý do hủy',
    rollback_at DATETIME(6) NULL COMMENT 'Thời điểm rollback',
    rollback_reason VARCHAR(255) NULL COMMENT 'Lý do rollback',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    expiration_attempts INT NOT NULL DEFAULT 0 COMMENT 'Số lần scheduler thử hết hạn',
    expiration_last_attempt_at DATETIME(6) NULL COMMENT 'Lần thử hết hạn gần nhất',
    expiration_next_attempt_at DATETIME(6) NULL COMMENT 'Lần retry hết hạn tiếp theo',
    expiration_error VARCHAR(1000) NULL COMMENT 'Lỗi hết hạn gần nhất',
    version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản dữ liệu',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_promotion_reservation_public UNIQUE (public_id),
    CONSTRAINT uk_promotion_reservation_code UNIQUE (reservation_code),
    CONSTRAINT uk_promotion_reservation_scope UNIQUE (reservation_scope_key),
    CONSTRAINT chk_reservation_amount CHECK (
        original_amount >= 0
        AND discount_amount >= 0
        AND final_amount >= 0
        AND final_amount = original_amount - discount_amount
    ),
    CONSTRAINT chk_reservation_single_benefit CHECK (
        (
            reservation_type = 'COUPON'
            AND coupon_public_id IS NOT NULL
            AND voucher_public_id IS NULL
        )
        OR (
            reservation_type = 'VOUCHER'
            AND coupon_public_id IS NULL
            AND voucher_public_id IS NOT NULL
        )
    ),
    CONSTRAINT chk_reservation_status CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'RELEASED', 'CANCELLED', 'EXPIRED')
    ),
    CONSTRAINT chk_reservation_lifecycle CHECK (
        (
            status = 'ACTIVE'
            AND confirmed_at IS NULL
            AND cancelled_at IS NULL
            AND rollback_at IS NULL
        )
        OR (
            status = 'COMPLETED'
            AND confirmed_at IS NOT NULL
            AND payment_public_id IS NOT NULL
            AND cancelled_at IS NULL
            AND rollback_at IS NULL
        )
        OR (
            status = 'RELEASED'
            AND confirmed_at IS NULL
            AND cancelled_at IS NULL
            AND rollback_at IS NOT NULL
            AND rollback_reason IS NOT NULL
        )
        OR (
            status = 'CANCELLED'
            AND confirmed_at IS NULL
            AND cancelled_at IS NOT NULL
            AND cancelled_reason IS NOT NULL
            AND rollback_at IS NULL
        )
        OR (
            status = 'EXPIRED'
            AND confirmed_at IS NULL
            AND cancelled_at IS NULL
            AND rollback_at IS NULL
        )
    ),
    CONSTRAINT chk_reservation_period CHECK (
        reservation_expired_at > reservation_started_at
    )
) COMMENT = 'Phiên giữ khuyến mãi';

CREATE INDEX idx_reservation_user ON promotion_reservations (user_public_id);

CREATE INDEX idx_reservation_booking ON promotion_reservations (booking_public_id);

CREATE INDEX idx_reservation_order ON promotion_reservations (order_public_id);

CREATE INDEX idx_reservation_campaign ON promotion_reservations (campaign_public_id);

CREATE INDEX idx_reservation_coupon ON promotion_reservations (coupon_public_id);

CREATE INDEX idx_reservation_voucher ON promotion_reservations (voucher_public_id);

CREATE INDEX idx_reservation_status ON promotion_reservations (status);

CREATE INDEX idx_reservation_expired ON promotion_reservations (reservation_expired_at);

CREATE INDEX idx_reservation_deleted ON promotion_reservations (deleted_at);

CREATE INDEX idx_reservation_expiration_due
    ON promotion_reservations (status, reservation_expired_at, expiration_next_attempt_at);

CREATE INDEX idx_reservation_coupon_active
    ON promotion_reservations (coupon_public_id, status, reservation_expired_at);

CREATE INDEX idx_reservation_coupon_user_active
    ON promotion_reservations (
        coupon_public_id, user_public_id, status, reservation_expired_at
    );

CREATE INDEX idx_reservation_coupon_phone_active
    ON promotion_reservations (
        coupon_public_id, customer_phone, status, reservation_expired_at
    );

CREATE INDEX idx_reservation_voucher_active
    ON promotion_reservations (voucher_public_id, status, reservation_expired_at);

CREATE INDEX idx_reservation_campaign_active
    ON promotion_reservations (campaign_public_id, status, reservation_expired_at);

CREATE INDEX idx_reservation_history
    ON promotion_reservations (status, reservation_type, created_at);

CREATE INDEX idx_reservation_created
    ON promotion_reservations (created_at);

-- ============================================================
-- COMPENSATION VOUCHERS
-- ============================================================

CREATE TABLE compensation_vouchers (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    voucher_public_id CHAR(36) NOT NULL COMMENT 'Public ID voucher bồi thường',
    reservation_public_id CHAR(36) NULL COMMENT 'Public ID phiên giữ',
    booking_public_id CHAR(36) NULL COMMENT 'Public ID booking',
    order_public_id CHAR(36) NULL COMMENT 'Public ID đơn hàng',
    user_public_id CHAR(36) NOT NULL COMMENT 'Public ID người nhận',
    compensation_type VARCHAR(50) NOT NULL COMMENT 'Loại bồi thường',
    reason VARCHAR(255) NOT NULL COMMENT 'Lý do bồi thường',
    amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Giá trị bồi thường',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái',
    issued_at DATETIME(6) NOT NULL COMMENT 'Ngày phát hành',
    expired_at DATETIME(6) NULL COMMENT 'Ngày hết hạn',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_compensation_public UNIQUE (public_id)
) COMMENT = 'Voucher bồi thường';

CREATE INDEX idx_compensation_voucher ON compensation_vouchers (voucher_public_id);

CREATE INDEX idx_compensation_user ON compensation_vouchers (user_public_id);

CREATE INDEX idx_compensation_booking ON compensation_vouchers (booking_public_id);

CREATE INDEX idx_compensation_order ON compensation_vouchers (order_public_id);

CREATE INDEX idx_compensation_status ON compensation_vouchers (status);

CREATE INDEX idx_compensation_deleted ON compensation_vouchers (deleted_at);

-- ============================================================
-- PROMOTION CONFIGURATIONS
-- ============================================================

CREATE TABLE promotion_configurations (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    config_key VARCHAR(150) NOT NULL COMMENT 'Khóa cấu hình',
    config_value TEXT NOT NULL COMMENT 'Giá trị cấu hình',
    value_type VARCHAR(30) NOT NULL COMMENT 'Kiểu dữ liệu',
    category VARCHAR(100) NOT NULL COMMENT 'Nhóm cấu hình',
    description VARCHAR(500) NULL COMMENT 'Mô tả',
    editable BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cho phép chỉnh sửa',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_configuration_public UNIQUE (public_id),
    CONSTRAINT uk_configuration_key UNIQUE (config_key)
) COMMENT = 'Cấu hình Promotion Service';

CREATE INDEX idx_configuration_category ON promotion_configurations (category);

CREATE INDEX idx_configuration_deleted ON promotion_configurations (deleted_at);

-- ============================================================
-- PHASE 6 - AUDIT & OUTBOX
-- ============================================================

-- ============================================================
-- APPROVAL HISTORIES
-- ============================================================

CREATE TABLE approval_histories (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    target_type VARCHAR(50) NOT NULL COMMENT 'Loại đối tượng',
    target_public_id CHAR(36) NOT NULL COMMENT 'Public ID đối tượng',
    action VARCHAR(50) NOT NULL COMMENT 'Hành động',
    old_status VARCHAR(30) NULL COMMENT 'Trạng thái trước',
    new_status VARCHAR(30) NOT NULL COMMENT 'Trạng thái sau',
    approver_public_id CHAR(36) NOT NULL COMMENT 'Public ID người phê duyệt',
    comment TEXT NULL COMMENT 'Ghi chú phê duyệt',
    approved_at DATETIME(6) NOT NULL COMMENT 'Ngày phê duyệt',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_approval_public UNIQUE (public_id)
) COMMENT = 'Lịch sử phê duyệt';

CREATE INDEX idx_approval_target ON approval_histories (target_type, target_public_id);

CREATE INDEX idx_approval_approver ON approval_histories (approver_public_id);

CREATE INDEX idx_approval_action ON approval_histories (action);

CREATE INDEX idx_approval_deleted ON approval_histories (deleted_at);

-- ============================================================
-- AUDIT LOGS
-- ============================================================

CREATE TABLE audit_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    entity_type VARCHAR(50) NOT NULL COMMENT 'Loại dữ liệu',
    entity_public_id CHAR(36) NOT NULL COMMENT 'Public ID dữ liệu',
    action VARCHAR(50) NOT NULL COMMENT 'Hành động',
    actor_public_id CHAR(36) NULL COMMENT 'Người thực hiện',
    actor_type VARCHAR(30) NULL COMMENT 'Loại người thực hiện',
    ip_address VARCHAR(100) NULL COMMENT 'Địa chỉ IP',
    user_agent TEXT NULL COMMENT 'Thông tin trình duyệt',
    request_id VARCHAR(100) NULL COMMENT 'Mã request',
    trace_id VARCHAR(100) NULL COMMENT 'Trace ID',
    before_data JSON NULL COMMENT 'Dữ liệu trước thay đổi',
    after_data JSON NULL COMMENT 'Dữ liệu sau thay đổi',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_audit_public UNIQUE (public_id)
) COMMENT = 'Nhật ký thao tác';

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_public_id);

CREATE INDEX idx_audit_actor ON audit_logs (actor_public_id);

CREATE INDEX idx_audit_action ON audit_logs (action);

CREATE INDEX idx_audit_request ON audit_logs (request_id);

CREATE INDEX idx_audit_trace ON audit_logs (trace_id);

CREATE INDEX idx_audit_created ON audit_logs (created_at);

-- ============================================================
-- OUTBOX EVENTS
-- ============================================================

CREATE TABLE outbox_events (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    aggregate_type VARCHAR(50) NOT NULL COMMENT 'Loại aggregate',
    aggregate_public_id CHAR(36) NOT NULL COMMENT 'Public ID aggregate',
    event_type VARCHAR(100) NOT NULL COMMENT 'Loại sự kiện',
    event_key VARCHAR(100) NULL COMMENT 'Khóa phân vùng Kafka',
    payload JSON NOT NULL COMMENT 'Dữ liệu sự kiện',
    headers_json JSON NULL COMMENT 'Header sự kiện',
    topic_name VARCHAR(150) NOT NULL COMMENT 'Tên Kafka topic',
    publish_status VARCHAR(30) NOT NULL COMMENT 'Trạng thái publish',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'Số lần gửi lại',
    next_retry_at DATETIME(6) NULL COMMENT 'Lần gửi tiếp theo',
    processing_started_at DATETIME(6) NULL COMMENT 'Thời điểm bắt đầu lease publish',
    processing_owner VARCHAR(100) NULL COMMENT 'Instance đang giữ lease publish',
    published_at DATETIME(6) NULL COMMENT 'Ngày publish',
    error_message TEXT NULL COMMENT 'Lỗi publish',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_outbox_public UNIQUE (public_id)
) COMMENT = 'Outbox Event';

CREATE INDEX idx_outbox_status ON outbox_events (publish_status);

CREATE INDEX idx_outbox_topic ON outbox_events (topic_name);

CREATE INDEX idx_outbox_retry ON outbox_events (next_retry_at);

CREATE INDEX idx_outbox_claim
    ON outbox_events (publish_status, next_retry_at, processing_started_at, created_at);

CREATE INDEX idx_outbox_aggregate ON outbox_events (
    aggregate_type,
    aggregate_public_id
);

CREATE INDEX idx_outbox_created ON outbox_events (created_at);

CREATE INDEX idx_outbox_deleted ON outbox_events (deleted_at);

-- ============================================================
-- PHASE 7 - IDEMPOTENCY
-- ============================================================

CREATE TABLE promotion_idempotency_keys (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    idempotency_key VARCHAR(255) NOT NULL COMMENT 'Khóa chống gửi trùng',
    request_hash CHAR(64) NOT NULL COMMENT 'Hash nội dung request',
    api_name VARCHAR(150) NOT NULL COMMENT 'Tên API',
    http_method VARCHAR(10) NOT NULL COMMENT 'Phương thức HTTP',
    user_public_id CHAR(36) NULL COMMENT 'Public ID người dùng',
    client_id VARCHAR(100) NOT NULL COMMENT 'Service gọi đã được xác thực',
    device_id VARCHAR(150) NULL COMMENT 'Mã thiết bị',
    session_id VARCHAR(150) NULL COMMENT 'Mã phiên đăng nhập',
    request_uri VARCHAR(255) NOT NULL COMMENT 'Đường dẫn API',
    request_body JSON NULL COMMENT 'Nội dung request',
    response_body JSON NULL COMMENT 'Nội dung response',
    response_status INT NULL COMMENT 'Mã trạng thái HTTP',
    reservation_public_id CHAR(36) NULL COMMENT 'Public ID phiên giữ',
    booking_public_id CHAR(36) NULL COMMENT 'Public ID booking',
    payment_public_id CHAR(36) NULL COMMENT 'Public ID thanh toán',
    processing_status VARCHAR(30) NOT NULL COMMENT 'Trạng thái xử lý',
    first_request_at DATETIME(6) NOT NULL COMMENT 'Lần gọi đầu tiên',
    completed_at DATETIME(6) NULL COMMENT 'Thời điểm hoàn thành',
    expired_at DATETIME(6) NOT NULL COMMENT 'Thời điểm hết hiệu lực',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_idempotency_public UNIQUE (public_id),
    CONSTRAINT uk_idempotency_scope UNIQUE (client_id, api_name, idempotency_key)
) COMMENT = 'Lưu khóa chống xử lý trùng request';

CREATE INDEX idx_idempotency_user ON promotion_idempotency_keys (user_public_id);

CREATE INDEX idx_idempotency_status ON promotion_idempotency_keys (processing_status);

CREATE INDEX idx_idempotency_api ON promotion_idempotency_keys (api_name);

CREATE INDEX idx_idempotency_request_hash ON promotion_idempotency_keys (request_hash);

CREATE INDEX idx_idempotency_booking ON promotion_idempotency_keys (booking_public_id);

CREATE INDEX idx_idempotency_payment ON promotion_idempotency_keys (payment_public_id);

CREATE INDEX idx_idempotency_expired ON promotion_idempotency_keys (expired_at);

CREATE INDEX idx_idempotency_deleted ON promotion_idempotency_keys (deleted_at);

-- ============================================================
-- PROMOTION RULES
-- ============================================================

CREATE TABLE promotion_rules (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    campaign_public_id CHAR(36) NOT NULL COMMENT 'Public ID chiến dịch',
    code VARCHAR(100) NOT NULL COMMENT 'Mã rule',
    name VARCHAR(255) NOT NULL COMMENT 'Tên rule',
    description TEXT NULL COMMENT 'Mô tả rule',
    rule_type VARCHAR(50) NOT NULL COMMENT 'Loại rule',
    priority INT NOT NULL DEFAULT 100 COMMENT 'Độ ưu tiên thực thi',
    execution_order INT NOT NULL DEFAULT 1 COMMENT 'Thứ tự thực thi',
    stackable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép cộng dồn',
    stop_further_rules BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Dừng các rule tiếp theo nếu thỏa',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cho phép áp dụng',
    conditions_json JSON NOT NULL COMMENT 'Điều kiện áp dụng',
    actions_json JSON NOT NULL COMMENT 'Hành động ưu đãi',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    effective_from DATETIME(6) NOT NULL COMMENT 'Thời gian bắt đầu hiệu lực',
    effective_to DATETIME(6) NULL COMMENT 'Thời gian kết thúc hiệu lực',
    version INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản rule',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_promotion_rule_public UNIQUE (public_id),
    CONSTRAINT chk_rule_priority CHECK (priority >= 0),
    CONSTRAINT chk_rule_execution_order CHECK (execution_order > 0),
    CONSTRAINT chk_rule_period CHECK (
        effective_to IS NULL
        OR effective_to > effective_from
    )
) COMMENT = 'Danh sách rule của chiến dịch khuyến mãi';

CREATE INDEX idx_rule_campaign ON promotion_rules (campaign_public_id);

CREATE INDEX idx_rule_type ON promotion_rules (rule_type);

CREATE INDEX idx_rule_enabled ON promotion_rules (enabled);

CREATE INDEX idx_rule_priority ON promotion_rules (priority);

CREATE INDEX idx_rule_execution ON promotion_rules (execution_order);

CREATE INDEX idx_rule_effective ON promotion_rules (effective_from, effective_to);

CREATE INDEX idx_rule_deleted ON promotion_rules (deleted_at);

-- ============================================================
-- RUNTIME ALIGNMENT (CONFIGURATION / INTEGRATION)
-- ============================================================
-- The Flyway V3 migration is the authoritative upgrade path for existing
-- installations. These statements keep this canonical bootstrap schema in
-- sync with the runtime entities and can be applied after the tables above.

ALTER TABLE promotion_configurations
    ADD COLUMN version INT NOT NULL DEFAULT 1 AFTER editable,
    ADD COLUMN requires_restart BOOLEAN NOT NULL DEFAULT FALSE AFTER editable,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER requires_restart,
    ADD COLUMN metadata_json JSON NULL AFTER status;
CREATE INDEX idx_configuration_status ON promotion_configurations (status);

CREATE TABLE promotion_integration_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    source_service VARCHAR(60) NOT NULL,
    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    schema_version VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(100) NULL,
    trace_id VARCHAR(100) NULL,
    payload JSON NOT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    last_error VARCHAR(4000) NULL,
    processed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_integration_event_public UNIQUE (public_id),
    CONSTRAINT uk_integration_event_source_id UNIQUE (source_service, event_id)
) COMMENT = 'Inbound integration event inbox and deduplication ledger';
CREATE INDEX idx_integration_event_status
    ON promotion_integration_events (processing_status, next_retry_at, created_at);
CREATE INDEX idx_integration_event_type
    ON promotion_integration_events (event_type, created_at);

CREATE TABLE promotion_scheduler_job_executions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    job_name VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    instance_id VARCHAR(100) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    processed_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(4000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_scheduler_execution_public UNIQUE (public_id)
);
CREATE INDEX idx_scheduler_execution_job
    ON promotion_scheduler_job_executions (job_name, started_at);
CREATE INDEX idx_scheduler_execution_status
    ON promotion_scheduler_job_executions (status, started_at);

CREATE TABLE promotion_scheduler_locks (
    job_name VARCHAR(100) PRIMARY KEY,
    owner VARCHAR(100) NOT NULL,
    locked_until DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6)
);

-- ============================================================
-- RUNTIME ALIGNMENT (V4 - PUBLIC REFERENCE TYPES)
-- ============================================================
-- Flyway V4 converts every legacy CHAR(n) column to VARCHAR(n), preserving
-- nullability/defaults/comments. This is required because Auth/User/Score
-- currently exchange numeric account IDs while Promotion-generated public IDs
-- remain UUID strings. The migration is data-preserving and keeps existing
-- unique indexes intact.
