# Promotion Service API Specification (v2 - Consolidated)

## 1. Thông Tin Chung

| Mục | Nội dung |
| :--- | :--- |
| Service | `promotion-service` |
| Feature | Campaign, Coupon, Voucher, Rule Configuration, Reservation and Partner Settlements |
| API liên quan | Campaign Admin, Coupon/Voucher Admin, Customer Wallet, Promotion Checkout, Partner & Settlement |
| Contract Owner | Dương Thiện Nhân |
| Backend Owner | Trần Lương Thiện Hoàng |
| Reviewer | Trần Lương Thiện Hoàng |
| Trạng thái | Promotion core implemented/verified; end-to-end Booking/Payment integration pending |
| Milestone | Sprint 2 - Core Service API Foundation & Consolidated Benefit Domain |
| Ngày cập nhật | 29/07/2026 |

---

## 2. Mục Tiêu Tài Liệu & Phạm Vi

Tài liệu này đặc tả toàn bộ API Contract cho `promotion-service` của hệ thống **LoraFilm**. 

Tài liệu này **hợp nhất** nội dung từ đặc tả cũ của `promotion-service-api.md` và `benefit-domain-api.md`, đồng thời loại bỏ tài liệu `benefit-domain-api.md` riêng biệt. Thiết kế ưu tiên tính đúng khi checkout, tránh vượt ngân sách, vượt quota, dùng quyền lợi hai lần hoặc mất quyền lợi khi thanh toán thất bại.

### Phạm Vi Của Promotion Service
- **Quản lý Campaign**: Vòng đời chiến dịch khuyến mại (Draft -> Pending -> Approved -> Active -> Paused -> Completed).
- **Quản lý Coupon**: Phát hành mã dùng chung, mã cá nhân hóa hoặc mã một lần.
- **Quản lý Voucher**: Phát hành và theo dõi voucher nằm trong ví khách hàng (Customer Wallet).
- **Promotion Rules**: Cấu hình tùy chọn để preview và chuẩn bị cho automatic rule discovery. Rule không còn là điều kiện submit vì Reservation Runtime hiện áp dụng `conditions_json` và `actions_json` của coupon/voucher.
- **Promotion Reservation (Giữ chỗ khuyến mại)**: Lock atomically coupon/voucher, quota, và budget trong quá trình checkout.
- **Redemption & Reservation lifecycle**: Confirm khi thanh toán thành công; release khi thanh toán thất bại; cancel khi booking bị hủy; job tự expire phiên quá hạn. Giao dịch đã confirm phải dùng refund/compensation, không rollback ledger.
- **Compensation (Bồi thường)**: Phát hành voucher đền bù chăm sóc khách hàng.
- **Partner & Settlement**: Quản lý đối tác tài trợ và đối soát quyết toán tài chính.

### Nằm Ngoài Phạm Vi (Out of Scope)
- Lưu trữ điểm tích lũy hoặc xếp hạng thành viên (thuộc về `score-service`). Promotion Service chỉ tích hợp qua API/Event để đọc hạng thành viên hoặc yêu cầu hold/commit/release điểm.
- Độc lập hoàn toàn với `movie-service` (phim/suất chiếu) và `user-service` (thông tin đăng ký).

---

## 3. Physical Database Schema

Hệ thống sử dụng MySQL làm Database chính, bao gồm 15 bảng nghiệp vụ và hạ tầng đã được align:

### 3.1. Bảng `promotion_campaigns`
```sql
CREATE TABLE promotion_campaigns (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    code VARCHAR(100) NOT NULL COMMENT 'Mã chiến dịch',
    name VARCHAR(255) NOT NULL COMMENT 'Tên chiến dịch',
    slug VARCHAR(255) NOT NULL COMMENT 'Slug duy nhất',
    description TEXT NULL COMMENT 'Mô tả chiến dịch',
    campaign_type VARCHAR(50) NOT NULL COMMENT 'Loại chiến dịch',
    funding_source VARCHAR(50) NOT NULL COMMENT 'Nguồn tài trợ',
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
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_campaign_public UNIQUE (public_id),
    CONSTRAINT uk_campaign_code UNIQUE (code)
) COMMENT = 'Thông tin chiến dịch khuyến mãi';
```

### 3.2. Bảng `coupons`
```sql
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
```

### 3.3. Bảng `coupon_redemptions`
```sql
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
```

### 3.4. Bảng `vouchers`
```sql
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
```

### 3.5. Bảng `voucher_redemptions`
```sql
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
```

### 3.6. Bảng `promotion_reservations`
```sql
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
```

Schema được quản lý bằng Flyway:
[`V1__promotion_schema.sql`](../../server/promotion-service/src/main/resources/db/migration/V1__promotion_schema.sql)
cho database mới và
[`V2__promotion_runtime_hardening.sql`](../../server/promotion-service/src/main/resources/db/migration/V2__promotion_runtime_hardening.sql)
cho cả database mới/lịch sử. Database cũ được baseline ở V1 rồi tự chạy V2; file
đối chiếu vận hành nằm tại
[`20260728_promotion_reservation_runtime.sql`](../database/mysql/migrations/20260728_promotion_reservation_runtime.sql).

### 3.7. Bảng `compensation_vouchers`
```sql
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
```

### 3.8. Bảng `partners`
```sql
CREATE TABLE partners (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    code VARCHAR(100) NOT NULL COMMENT 'Mã đối tác',
    name VARCHAR(255) NOT NULL COMMENT 'Tên đối tác',
    partner_type VARCHAR(50) NOT NULL COMMENT 'Loại đối tác',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái',
    tax_code VARCHAR(50) NULL COMMENT 'Mã số thuế',
    email VARCHAR(255) NULL COMMENT 'Email liên hệ',
    phone VARCHAR(30) NULL COMMENT 'Số điện thoại',
    contact_person VARCHAR(255) NULL COMMENT 'Người liên hệ',
    address VARCHAR(500) NULL COMMENT 'Địa chỉ',
    website VARCHAR(255) NULL COMMENT 'Website',
    contract_number VARCHAR(100) NULL COMMENT 'Số hợp đồng',
    contract_start_at DATETIME(6) NULL COMMENT 'Ngày bắt đầu hợp đồng',
    contract_end_at DATETIME(6) NULL COMMENT 'Ngày kết thúc hợp đồng',
    settlement_cycle VARCHAR(30) NOT NULL COMMENT 'Chu kỳ đối soát',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_partner_public UNIQUE (public_id),
    CONSTRAINT uk_partner_code UNIQUE (code)
) COMMENT = 'Thông tin đối tác';
```

### 3.9. Bảng `partner_settlements`
```sql
CREATE TABLE partner_settlements (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    partner_public_id CHAR(36) NOT NULL COMMENT 'Public ID đối tác',
    campaign_public_id CHAR(36) NULL COMMENT 'Public ID chiến dịch',
    settlement_code VARCHAR(100) NOT NULL COMMENT 'Mã đối soát',
    settlement_period_from DATETIME(6) NOT NULL COMMENT 'Kỳ đối soát từ',
    settlement_period_to DATETIME(6) NOT NULL COMMENT 'Kỳ đối soát đến',
    total_orders INT NOT NULL DEFAULT 0 COMMENT 'Tổng số đơn',
    total_discount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Tổng tiền giảm',
    partner_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Số tiền đối tác thanh toán',
    platform_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Số tiền hệ thống chịu',
    adjustment_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Khoản điều chỉnh',
    final_amount DECIMAL(18, 2) NOT NULL DEFAULT 0 COMMENT 'Số tiền quyết toán',
    currency VARCHAR(10) NOT NULL DEFAULT 'VND' COMMENT 'Đơn vị tiền tệ',
    settlement_rule VARCHAR(50) NOT NULL DEFAULT 'PERCENTAGE_OF_DISCOUNT' COMMENT 'Quy tắc quyết toán',
    partner_percentage DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT 'Tỷ lệ đối tác tài trợ',
    fixed_amount_per_redemption DECIMAL(18,2) NULL COMMENT 'Mức cố định mỗi redemption',
    status VARCHAR(30) NOT NULL COMMENT 'Trạng thái đối soát',
    approved_at DATETIME(6) NULL COMMENT 'Ngày phê duyệt',
    paid_at DATETIME(6) NULL COMMENT 'Ngày thanh toán',
    note TEXT NULL COMMENT 'Ghi chú',
    metadata_json JSON NULL COMMENT 'Thông tin mở rộng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_settlement_public UNIQUE (public_id),
    CONSTRAINT uk_settlement_code UNIQUE (settlement_code),
    CONSTRAINT chk_settlement_period CHECK (
        settlement_period_to > settlement_period_from
    )
) COMMENT = 'Đối soát với đối tác';
```

### 3.10. Bảng `promotion_configurations`
```sql
CREATE TABLE promotion_configurations (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT 'Khóa chính nội bộ' PRIMARY KEY,
    public_id CHAR(36) NOT NULL COMMENT 'Định danh công khai (UUID)',
    config_key VARCHAR(150) NOT NULL COMMENT 'Khóa cấu hình',
    config_value TEXT NOT NULL COMMENT 'Giá trị cấu hình',
    value_type VARCHAR(30) NOT NULL COMMENT 'Kiểu dữ liệu',
    category VARCHAR(100) NOT NULL COMMENT 'Nhóm cấu hình',
    description VARCHAR(500) NULL COMMENT 'Mô tả',
    editable BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Cho phép chỉnh sửa',
    version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    requires_restart BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Chỉ có hiệu lực sau restart',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/DEPRECATED',
    metadata_json JSON NULL COMMENT 'Giới hạn và metadata cấu hình',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Ngày tạo',
    created_by CHAR(36) NULL COMMENT 'Người tạo',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Ngày cập nhật',
    updated_by CHAR(36) NULL COMMENT 'Người cập nhật',
    deleted_at DATETIME(6) NULL COMMENT 'Ngày xóa mềm',
    deleted_by CHAR(36) NULL COMMENT 'Người xóa mềm',
    CONSTRAINT uk_configuration_public UNIQUE (public_id),
    CONSTRAINT uk_configuration_key UNIQUE (config_key)
) COMMENT = 'Cấu hình Promotion Service';
```

### 3.11. Bảng `approval_histories`
```sql
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
```

### 3.12. Bảng `audit_logs`
```sql
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
```

### 3.13. Bảng `outbox_events`
```sql
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
```

### 3.14. Bảng `promotion_idempotency_keys`
```sql
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
```

### 3.15. Bảng `promotion_rules`
```sql
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
```

---

## 4. Quy Ước Chung & Tích Hợp Hệ Thống

### 4.1. Headers Truy Cập
- **Protected Customer API**:
  ```http
  Authorization: Bearer <accessToken>
  Content-Type: application/json
  ```
- **Internal Microservice API**:
  ```http
  X-Service-Name: BOOKING_SERVICE | PAYMENT_SERVICE
  X-Internal-Token: <service-token>
  X-Idempotency-Key: <unique-uuid-key>
  Content-Type: application/json
  ```
  `X-Service-Name` phải khớp token riêng của service. Booking và Payment không dùng
  chung secret. Header trùng, thiếu hoặc khai service sai đều bị từ chối trước khi
  vào controller. `X-Idempotency-Key` chỉ bắt buộc với năm lệnh thay đổi reservation.

### 4.2. Định Dạng Ngày Tháng & Tiền Tệ
- **Datetime**: API dùng ISO-8601 có offset, ví dụ `2026-07-28T14:10:00.000000Z`; database lưu với độ chính xác microsecond.
- **Tiền Tệ**: Hệ thống lưu số tiền kiểu decimal. Quy đổi về `BigDecimal` với scale = 2 và làm tròn `HALF_UP`; đơn vị mặc định là `VND`.
- **Mã Ưu Đãi (Coupon Code)**: Trim khoảng trắng, uppercase trước khi lưu và so sánh không phân biệt chữ hoa/thường ở tầng API.

### 4.3. Định Dạng Phản Hồi Chung (Common Response Contract)
- **Thành Công (2xx)**:
  ```json
  {
    "success": true,
    "message": "Operation completed successfully",
    "data": {}
  }
  ```
- **Thất bại / Lỗi nghiệp vụ (4xx/5xx)**:
  ```json
  {
    "success": false,
    "message": "Error details message",
    "errorCode": "ERROR_CODE_NAME",
    "data": null,
    "errors": null
  }
  ```

---

## 5. Các Quy Tắc Nghiệp Vụ Chính (Business Rules)

Hệ thống áp dụng các quy tắc nghiệp vụ nghiêm ngặt nhằm tránh thất thoát tài chính và gian lận:

- **BR-CAMP-00 (Khóa cấu hình)**: Campaign, rule và benefit gắn campaign chỉ được thay đổi khi campaign là `DRAFT` và approval là `DRAFT/REJECTED`. Submit chuyển approval sang `PENDING` và khóa cấu hình. Mọi thay đổi sau reject reset approval về `DRAFT`, legal về `PENDING` và xóa quyết định phê duyệt cũ.
- **BR-CAMP-01 (Kích hoạt chiến dịch)**: Campaign chuyển từ `SCHEDULED` sang `ACTIVE` khi đồng thời thỏa mãn: `current_time >= start_at` VÀ `approval_status = APPROVED` VÀ `legal_status = PASSED` VÀ `budget_amount > 0`.
- **BR-CAMP-02 (Kiểm soát ngân sách)**: Tự động chuyển chiến dịch sang `PAUSED` khi `budget_used + budget_reserved >= budget_amount * 0.98`. Campaign chỉ được publish/activate sau khi business approval đã `APPROVED`, legal review đã `PASSED` và budget dương.
- **BR-CAMP-03 (Benefit thực thi)**: Campaign `COUPON/VOUCHER` chỉ được submit và publish khi có ít nhất một benefit đúng loại, chưa ở trạng thái terminal và có thời hạn giao với thời hạn campaign. `promotion_rules` là tùy chọn; không cho tạo `AUTOMATIC_DISCOUNT` vì checkout runtime hiện chưa hỗ trợ.
- **BR-CAMP-04 (Coupon draft)**: Coupon mới luôn là `DRAFT`. Coupon đủ thời hạn được kích hoạt cùng lúc campaign chuyển sang `ACTIVE`; scheduler tiếp tục kích hoạt coupon có `valid_from` muộn hơn.
- **BR-COUP-01 (Giới hạn mỗi khách hàng)**: Một `user_public_id` hoặc số điện thoại chỉ được áp dụng thành công mã coupon tối đa `max_redemptions_per_user` lần.
- **BR-VOU-01 (Hạn chế vé tặng)**: Vé tặng 0đ (phát do sinh nhật hoặc nâng hạng thành viên) không áp dụng cho suất chiếu sớm (sneak show), các phòng chiếu VIP/IMAX/4DX/Gold Class và ngày Lễ Tết.
- **BR-STACK-01 (Phạm vi runtime hiện tại)**: Reservation Runtime nhận đúng một mã coupon hoặc voucher và mức giá sau giảm không âm. Automatic rule discovery, stacking nhiều benefit và loyalty-point saga là pipeline riêng; không được giả lập stacking bằng cách gọi reserve nhiều lần cho cùng checkout.
- **BR-ELIG-01 (Kiểm tra điều kiện)**: Thực hiện cơ chế fail-fast: Trạng thái -> Hạng thành viên (lấy từ `score-service` qua cache ACL) -> Điều kiện giỏ vé -> Trần pháp luật (Nghị định 81/2018/NĐ-CP - tối đa giảm 50% giá trị gốc).
- **BR-REDEEM-01 (Vòng đời Reservation)**: State machine thực tế là `ACTIVE -> COMPLETED | RELEASED | CANCELLED | EXPIRED`; mọi trạng thái đích đều là terminal. `POST /internal/reservations` vừa validate vừa giữ atomically nên không tồn tại trạng thái `CREATED/RESERVED` trung gian. TTL từ 60 đến 1800 giây, mặc định 900 giây; job nội bộ tự chuyển phiên quá hạn sang `EXPIRED` và hoàn `budget_reserved`.
- **BR-REDEEM-02 (Pricing snapshot)**: `originalAmount`, `discountAmount`, `finalAmount` và `currency` được chốt khi reserve. Confirm trong TTL phải ghi redemption đúng snapshot đã giữ, không tính lại theo cấu hình có thể vừa thay đổi. Refresh thì phải validate lại snapshot hiện vẫn hợp lệ.
- **BR-REDEEM-03 (Idempotency và cạnh tranh)**: Mọi lệnh thay đổi trạng thái yêu cầu `X-Idempotency-Key`. Reserve ràng buộc key với payload; confirm/release/cancel/refresh idempotent theo reservation và dữ liệu đích. Redis lock chỉ giảm cạnh tranh; transaction, pessimistic lock và unique constraint của MySQL là nguồn bảo đảm cuối cùng.
- **BR-REDEEM-04 (Quota của active hold)**: Active reservation được tính chung với redemption đã consume khi kiểm tra giới hạn campaign, coupon, voucher, user và số điện thoại xác thực. Coupon public nhiều lượt vẫn cho phép các khách độc lập giữ song song đến đúng quota; không khóa độc quyền cả mã.
- **BR-REDEEM-05 (Một benefit trên checkout)**: Một `orderPublicId` (ưu tiên) hoặc `bookingPublicId` chỉ có một reservation `ACTIVE/COMPLETED` hiệu lực. Release/cancel/expire giải phóng scope; completed giữ scope để không thể áp mã mới sau thanh toán. Runtime này không hỗ trợ lách stacking bằng nhiều request.
- **BR-REDEEM-06 (Active hold khi campaign bị dừng)**: pause/cancel/kill-switch/revoke chặn reserve và refresh mới. Hold đã tạo hợp lệ vẫn được Payment confirm trong TTL theo pricing snapshot; hết TTL thì expire. Không được xóa campaign khi còn active hold.
- **BR-ELIG-02 (Condition schema fail-closed)**: Runtime chỉ chấp nhận các condition đã được engine hỗ trợ; field lạ, sai kiểu, alias trùng, ngày/thứ không hợp lệ hoặc mảng rỗng bị từ chối khi cấu hình. Coupon/voucher không được silently bỏ qua condition.
- **BR-VOU-02 (Free item pricing)**: `FREE_TICKET` yêu cầu `contextJson.ticketAmount`, `FREE_COMBO` yêu cầu `contextJson.comboAmount`; số tiền phải dương và không vượt `originalAmount`. Không được giảm toàn bộ đơn hàng khi thiếu subtotal của item đủ điều kiện.
- **BR-EVENT-01 (Transactional outbox)**: Domain và outbox được ghi cùng transaction. Worker claim từng event bằng DB lease; chỉ chuyển `PUBLISHED` sau Kafka broker ACK. Event lỗi retry exponential backoff, event `PROCESSING` quá lease được worker khác reclaim; payload dùng envelope version `1.0` và đã redact dữ liệu nhạy cảm.
- **BR-POINT-01 (Điểm tích lũy)**: Promotion Service không sở hữu số dư và không tự cộng/trừ điểm. Saga hold/commit/release điểm với `score-service` là integration riêng, chưa nằm trong Reservation Runtime hiện tại.

---

## 6. Danh Sách API Endpoints

| Nhóm | Method | Endpoint | Caller / Quyền | Tác dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Campaign Admin** | POST | `/api/admin/promotion-campaigns` | Admin/Marketing | Tạo chiến dịch khuyến mại |
| | GET | `/api/admin/promotion-campaigns` | Admin/Marketing | Danh sách chiến dịch (Phân trang) |
| | GET | `/api/admin/promotion-campaigns/{id}` | Admin/Marketing | Xem chi tiết chiến dịch |
| | PUT | `/api/admin/promotion-campaigns/{id}` | Admin/Marketing | Cập nhật thông tin chiến dịch |
| | PATCH | `/api/admin/promotion-campaigns/{id}/status` | Admin/Marketing | Thay đổi trạng thái hoạt động |
| | POST | `/api/admin/promotion-campaigns/{id}/legal-review` | Admin/Legal Compliance | Ghi quyết định `PASSED/FAILED` và mã hồ sơ pháp lý trước publish |
| **Coupon Admin** | POST | `/api/admin/coupons` | Admin/Marketing | Tạo coupon mới |
| | POST | `/api/admin/coupons/generate` | Admin/Marketing | Phát sinh coupon hàng loạt theo lô |
| | POST | `/api/admin/coupons/import` | Admin/Marketing | Nhập mã coupon từ file CSV/Excel |
| | GET | `/api/admin/coupons/export` | Admin/Marketing | Xuất file danh sách mã coupon |
| | PUT | `/api/admin/coupons/{id}` | Admin/Marketing | Cập nhật cấu hình coupon |
| | DELETE | `/api/admin/coupons/{id}` | Admin/Marketing | Vô hiệu hóa coupon (Xóa mềm) |
| | GET | `/api/admin/coupons` | Admin/Marketing | Tìm kiếm và lọc danh sách coupon |
| | GET | `/api/admin/coupons/{id}` | Admin/Marketing | Xem chi tiết coupon quản trị |
| **Voucher Admin** | POST | `/api/admin/vouchers` | Admin/CSKH | Phát voucher cho một người dùng |
| | POST | `/api/admin/vouchers/batch` | Admin/CSKH | Phát voucher hàng loạt cho nhóm user |
| | PUT | `/api/admin/vouchers/{id}` | Admin/CSKH | Cập nhật thời hạn hoặc điều kiện |
| | POST | `/api/admin/vouchers/{id}/revoke` | Admin/CSKH | Thu hồi voucher chưa sử dụng |
| | POST | `/api/admin/vouchers/{id}/extend` | Admin/CSKH | Gia hạn ngày hết hạn voucher |
| | GET | `/api/admin/vouchers` | Admin/CSKH | Xem danh sách voucher hệ thống |
| | GET | `/api/admin/vouchers/{id}` | Admin/CSKH | Chi tiết thông tin voucher |
| **Customer Wallet** | GET | `/api/customers/me/vouchers` | Customer | Lấy danh sách ví voucher của tôi |
| **Validation & History**| POST | `/internal/coupons/validate` | Booking / Gate | Preview điều kiện & tính tiền giảm coupon |
| | POST | `/internal/vouchers/validate` | Booking / Gate | Preview điều kiện & tính tiền giảm voucher |
| | GET | `/api/admin/redemptions` | Admin/Finance | Lấy lịch sử sử dụng (redemption ledger) |
| **Compensation** | POST | `/api/admin/compensation-vouchers` | Admin/CSKH | Phát voucher bồi thường sự cố |
| | PUT | `/api/admin/compensation-vouchers/{id}` | Admin/CSKH | Cập nhật voucher bồi thường |
| | GET | `/api/admin/compensation-vouchers` | Admin/CSKH | Danh sách đền bù bồi thường |
| | GET | `/api/admin/compensation-vouchers/{id}` | Admin/CSKH | Chi tiết đền bù bồi thường |
| **Runtime & Reservation** | POST | `/internal/runtime/validate` | Booking Service | Preview tính hợp lệ, quota, budget và giá; không giữ tài nguyên |
| | POST | `/internal/reservations` | Booking Service | Validate và giữ atomically benefit, quota, budget |
| | GET | `/internal/reservations/{reservationId}` | Booking/Payment | Đọc chi tiết; tự sửa trạng thái nếu phiên đã quá hạn |
| | POST | `/internal/reservations/{reservationId}/confirm` | Payment Service | Confirm thanh toán và tạo đúng một redemption |
| | POST | `/internal/reservations/{reservationId}/release` | Payment Service | Thanh toán thất bại: giải phóng phiên giữ |
| | POST | `/internal/reservations/{reservationId}/cancel` | Booking Service | Booking bị hủy: hủy phiên giữ |
| | POST | `/internal/reservations/{reservationId}/refresh` | Booking Service | Gia hạn bằng deadline tuyệt đối |
| | GET | `/api/admin/reservations` | Admin/Operations | Tra cứu lịch sử reservation có lọc và phân trang |
| **Partner & Settlement**| POST | `/api/admin/partners` | Admin/Finance | Thêm đối tác tài trợ mới |
| | GET | `/api/admin/partners` | Admin/Finance | Danh sách đối tác hệ thống |
| | GET | `/api/admin/partners/{id}` | Admin/Finance | Xem chi tiết thông tác |
| | PUT | `/api/admin/partners/{id}` | Admin/Finance | Cập nhật thông tin đối tác |
| | DELETE | `/api/admin/partners/{id}` | Admin/Finance | Xóa mềm đối tác |
| | POST | `/api/admin/partner-settlements` | Admin/Finance | Tạo phiên đối soát quyết toán kỳ hạn |
| | GET | `/api/admin/partner-settlements` | Admin/Finance | Danh sách phiên đối soát tài chính |
| | GET | `/api/admin/partner-settlements/{id}` | Admin/Finance | Chi tiết quyết toán với đối tác |
| | PUT | `/api/admin/partner-settlements/{id}/status` | Admin/Finance | Phê duyệt/Thay đổi trạng thái đối soát |
| **Configuration** | POST | `/api/admin/configurations` | Admin/Configuration | Tạo cấu hình động |
| | PUT | `/api/admin/configurations/{id}` | Admin/Configuration | Cập nhật và refresh cache |
| | DELETE | `/api/admin/configurations/{id}` | Admin/Configuration | Deprecate cấu hình (xóa mềm) |
| | GET | `/api/admin/configurations` | Admin/Configuration | Tìm kiếm cấu hình |
| | GET | `/api/admin/configurations/{id}` | Admin/Configuration | Chi tiết cấu hình |
| **Integration Operations** | POST | `/internal/events/publish` | Operations Service | Ghi outbound event vào transactional outbox |
| | POST | `/internal/events/retry/{id}` | Operations Service | Retry event lỗi (có alias body `/retry`) |
| | POST | `/internal/events/dlq/reprocess` | Operations Service | Reprocess inbound/outbound DLQ |
| | GET | `/internal/events/status` | Operations Service | Thống kê trạng thái event |
| **Scheduler Hooks** | POST | `/internal/schedulers/campaigns/expire` | Operations Service | Hết hạn campaign |
| | POST | `/internal/schedulers/coupons/activate` | Operations Service | Kích hoạt coupon draft khi campaign và thời hạn đã active |
| | POST | `/internal/schedulers/coupons/expire` | Operations Service | Hết hạn coupon |
| | POST | `/internal/schedulers/vouchers/expire` | Operations Service | Hết hạn voucher |
| | POST | `/internal/schedulers/outbox/publish` | Operations Service | Publish outbox |
| | POST | `/internal/schedulers/outbox/retry` | Operations Service | Retry outbox |
| | POST | `/internal/schedulers/cache/refresh` | Operations Service | Refresh runtime configuration cache |
| **Monitoring** | GET | `/api/admin/events` | Admin/Operations | Event history (OUTBOUND/INBOUND) |
| | GET | `/api/admin/events/{id}` | Admin/Operations | Event detail |
| | GET | `/api/admin/events/jobs` | Admin/Operations | Scheduler execution history |
| | POST | `/api/admin/events/jobs/{jobName}/run` | Operations Manager | Chạy job thủ công |

---

### 6.1. Runtime contract alignment (2026-07-29)

The controller inventory is the source of truth for the current implementation.
The rows above are retained for the original product contract; the following
items make the recently added runtime APIs explicit:

| Group | Method | Endpoint | Authorization |
| :--- | :--- | :--- | :--- |
| Promotion Rule Admin | POST/PUT/DELETE | `/api/admin/promotion-rules[/{id}]` | `ADMIN`, `MARKETING_MANAGER` |
| Promotion Rule Admin | GET | `/api/admin/promotion-rules[/{id}]` | `ADMIN`, `MARKETING_MANAGER`, `FINANCE_DIRECTOR` |
| Promotion Rule Admin | POST | `/api/admin/promotion-rules/{id}/clone` | `ADMIN`, `MARKETING_MANAGER` |
| Promotion Rule Admin | POST | `/api/admin/promotion-rules/preview` | `ADMIN`, `MARKETING_MANAGER` |
| Campaign lifecycle | POST | `/api/admin/promotion-campaigns/{id}/approve` | `ADMIN`, `MARKETING_MANAGER`, `FINANCE_DIRECTOR` |
| Campaign lifecycle | POST | `/api/admin/promotion-campaigns/{id}/reject` | `ADMIN`, `MARKETING_MANAGER`, `FINANCE_DIRECTOR` |
| Campaign lifecycle | PATCH | `/api/admin/promotion-campaigns/{id}/status` with `KILL_SWITCH` | `ADMIN`, `MARKETING_MANAGER` |
| Scheduler hook | POST | `/internal/schedulers/campaigns/activate` | trusted operations caller |
| Internal configuration | GET | `/internal/configurations/{key}` | trusted internal caller |
| Customer wallet | GET | `/api/customers/me/vouchers` | authenticated customer; identity comes from JWT |

Admin authorization is method-level and role-specific. The broad `/api/admin/**`
matcher only requires authentication so that `MARKETING_*`, `FINANCE_*`,
`CSKH_AGENT`, `LEGAL_COMPLIANCE`, and `OPERATIONS_MANAGER` can reach the
endpoints explicitly assigned to them; an endpoint without a method/class
`@PreAuthorize` is a security defect.
Auth bootstrap currently creates only `ADMIN`, `EMPLOYEE`, and `CUSTOMER`, so
the Promotion-specific business roles must be created and assigned before
these non-admin APIs are deployed.

`userPublicId` and `ownerPublicId` accept either a positive numeric account ID
(the current Auth/User/Score contract) or a legacy UUID. Promotion stores these
references as `VARCHAR(36)` after Flyway V4. `publicId` values generated by
Promotion itself remain UUIDs.

The internal scheduler hooks are operational service-to-service hooks and must
not be exposed through the public gateway. Reservation expiration remains an
in-process/DB-claimed job; the hook list covers campaign activation/expiry,
benefit expiry, outbox publish/retry, and cache refresh.

An inbox/Kafka consumer is present for a future versioned event contract, but
`promotion.integration.consumers-enabled` defaults to `false`; it must remain
disabled until Booking/Payment publish the envelope and ownership rules agreed
with Promotion.

## 7. Đặc Tả Chi Tiết Các API Chính

### 7.1. Nhóm Promotion Checkout (Internal APIs)

Tất cả endpoint `/internal/**` yêu cầu cặp `X-Service-Name` + `X-Internal-Token`.
Các lệnh thay đổi reservation còn yêu cầu `X-Idempotency-Key` dài tối đa 255
ký tự. Idempotency được lưu DB theo `(service, API, key)`, ràng buộc SHA-256
của payload và replay nguyên response đã hoàn thành; record `PROCESSING` có lease
để tự phục hồi khi instance chết. `benefitType` chỉ nhận `COUPON` hoặc `VOUCHER`;
ít nhất một trong `bookingPublicId` và `orderPublicId` phải có giá trị.

#### 7.1.1. Runtime Validate

Preview điều kiện benefit, campaign, quota, active hold và ngân sách tại thời điểm gọi. API này **không giữ tài nguyên** nên kết quả chỉ mang tính advisory; checkout luôn phải dùng API reserve làm bước quyết định.

- **Endpoint**: `POST /internal/runtime/validate`
- **Request Body**:

  ```json
  {
    "benefitType": "COUPON",
    "code": "LORAFILM50K",
    "userPublicId": "8f395bb2-33cc-4b77-88f5-9372f7cbe801",
    "customerPhone": "0987654321",
    "originalAmount": 250000.00,
    "bookingPublicId": "c86e0c03-5182-4bf3-a3d8-a99f1fa43ab5",
    "contextJson": {}
  }
  ```

- **Response `data`**: `valid`, `benefitType`, `benefitPublicId`, `code`, `originalAmount`, `discountAmount`, `finalAmount`, `currency`, `reasonCode`, `message`.
- `customerPhone` được canonicalize về E.164 (`0901234567` -> `+84901234567`)
  trước khi tính quota; giá trị không hợp lệ trả `400`.
- `contextJson` tối đa 16 KiB và phải là object. Engine hiện hỗ trợ:
  `movieId(s)`, `cinemaId(s)`, `paymentMethod(s)`, `channel(s)`, `format(s)`,
  `orderType(s)`, `seatType(s)`, `allowedUserIds`, `dayOfWeek`,
  `excludeRoomType(s)`, `excludeDates`, `requiredTierCode`,
  `requiresVerification`, `minimumOrderAmount/minOrderAmount`.
- `allowMultipleVoucherPerOrder`, `stackableWith` và `notStackableWith` là metadata
  policy được validate kiểu dữ liệu; chúng không mở khóa stacking trong Reservation
  Runtime một-benefit-per-checkout.

#### 7.1.2. Create Reservation

Validate và giữ atomically coupon/voucher, campaign quota và `budget_reserved`. Cùng idempotency key và cùng payload trả lại cùng reservation; cùng key nhưng payload khác trả `409`.

Mỗi checkout chỉ có một benefit hiệu lực. `orderPublicId` là scope chính; nếu chưa
có order thì dùng `bookingPublicId`. Unique constraint tại DB là lớp bảo vệ cuối
cùng khi nhiều pod cùng reserve.

- **Endpoint**: `POST /internal/reservations`
- **Request Body**:

  ```json
  {
    "benefitType": "COUPON",
    "code": "LORAFILM50K",
    "userPublicId": "8f395bb2-33cc-4b77-88f5-9372f7cbe801",
    "customerPhone": "0987654321",
    "originalAmount": 250000.00,
    "bookingPublicId": "c86e0c03-5182-4bf3-a3d8-a99f1fa43ab5",
    "orderPublicId": "e58dfc02-e2bc-4402-a4f7-e7d6ab6277b0",
    "contextJson": {},
    "holdDurationSeconds": 900
  }
  ```

- **Response Success**: `201 Created`, `data.publicId` là reservation ID; trạng thái ban đầu là `ACTIVE`. Response chứa pricing snapshot, benefit/campaign/transaction references, thời gian bắt đầu và deadline.

#### 7.1.3. Confirm Reservation

Chuyển `ACTIVE -> COMPLETED`, chuyển `budget_reserved` thành `budget_used` và tạo đúng một coupon/voucher redemption gắn unique với reservation. Retry cùng `paymentPublicId` trả cùng redemption; payment khác trả `409`.

- **Endpoint**: `POST /internal/reservations/{reservationId}/confirm`
- **Request Body**:

  ```json
  {
    "paymentPublicId": "a09f2203-d2bc-4402-a4f7-e7d6ab6299d0"
  }
  ```

- Reservation hết hạn trả `409 RESERVATION_EXPIRED`.
- Reservation đã release/cancel không thể confirm.
- Reservation đã `COMPLETED` không được release/cancel; hoàn tiền hoặc bồi thường phải đi qua nghiệp vụ refund/compensation.
- Campaign bị pause/cancel/kill-switch sau lúc reserve không làm mất hold đã cấp:
  Payment vẫn được confirm trong TTL theo snapshot. Client không được refresh hold đó.

#### 7.1.4. Release Reservation

Dùng khi payment thất bại hoặc timeout trước confirm. Chuyển `ACTIVE -> RELEASED`, ghi `rollbackAt/rollbackReason` và hoàn phần ngân sách đang giữ.

- **Endpoint**: `POST /internal/reservations/{reservationId}/release`
- **Request Body**:

  ```json
  {
    "reason": "Payment failed"
  }
  ```

#### 7.1.5. Cancel Reservation

Dùng khi booking bị khách hàng hoặc hệ thống hủy trước confirm. Chuyển `ACTIVE -> CANCELLED`, ghi `cancelledAt/cancelledReason` và hoàn phần ngân sách đang giữ.

- **Endpoint**: `POST /internal/reservations/{reservationId}/cancel`
- **Request Body**:

  ```json
  {
    "reason": "Customer cancelled booking"
  }
  ```

#### 7.1.6. Refresh Reservation

Gia hạn một reservation `ACTIVE`. Deadline tuyệt đối giúp retry không vô tình cộng TTL nhiều lần. Deadline mới phải lớn hơn deadline hiện tại, không vượt quá 1800 giây tính từ lúc reserve, `validTo` của benefit hoặc `endAt` của campaign. Pricing snapshot phải còn validate được.

- **Endpoint**: `POST /internal/reservations/{reservationId}/refresh`
- **Request Body**:

  ```json
  {
    "requestedExpiredAt": "2026-07-28T14:10:00.000000Z"
  }
  ```

#### 7.1.7. Get Reservation Detail

- **Endpoint**: `GET /internal/reservations/{reservationId}`
- Trả toàn bộ pricing snapshot, lifecycle timestamps và redemption nếu đã `COMPLETED`.
- Nếu dữ liệu vẫn là `ACTIVE` nhưng deadline đã qua, request này atomically tự chuyển sang `EXPIRED`, hoàn budget và ghi lifecycle event.

#### 7.1.8. Reservation History

- **Endpoint**: `GET /api/admin/reservations`
- **Auth**: Bearer token.
- **Query tùy chọn**: `type`, `status`, `userPublicId`, `bookingPublicId`, `orderPublicId`, `from`, `to`, `page` (mặc định 0), `size` (1-100, mặc định 20).
- Kết quả phân trang theo `createdAt DESC`.

#### Các API không tạo

- Không có `/internal/runtime/check`: trùng mục đích với `validate`.
- Không có `/internal/runtime/force-expire`: client không được ép một active hold sang expired; dùng release/cancel đúng nguyên nhân.
- Reservation expiration itself is not exposed as a public HTTP operation:
  `ReservationExpirationScheduler` runs in-process, claims rows in the database,
  and processes each reservation in its own `REQUIRES_NEW` transaction. The
  operational `/internal/schedulers/**` hooks listed in section 6.1 are
  restricted service-to-service hooks for campaign/benefit lifecycle, outbox,
  and cache jobs; they must not be routed through the public gateway.

#### Nguồn sự thật tích hợp

- The intended caller contract is Booking →
  `validate/reserve/refresh/cancel` and Payment →
  `detail/confirm/release`. In the current repository snapshot those clients
  are not wired yet; the integration checklist in
  `promotion-service-production-readiness-report.md` is a release blocker.
- Các API đồng bộ trên là nguồn sự thật cho thay đổi trạng thái. Promotion chỉ
  **publish** lifecycle event qua transactional outbox.
- Không đồng thời consume `PaymentCompleted`, `PaymentFailed` hoặc
  `BookingCancelled` trong issue này: chưa có event contract/idempotency ownership
  được hai service thống nhất, và thêm consumer sẽ tạo hai writer cạnh tranh cho
  cùng state machine. Khi bổ sung event-driven fallback phải có inbox table,
  schema version và quy tắc ownership riêng.

---

### 7.2. Nhóm Validation (Internal APIs)

#### 7.2.1. Validate Coupon
Kiểm tra điều kiện của coupon trước khi áp dụng thực tế, không lock ngân sách.

- **Endpoint**: `POST /internal/coupons/validate`
- **Request Body**:
  ```json
  {
    "code": "LORAFILM50K",
    "userPublicId": "8f395bb2-33cc-4b77-88f5-9372f7cbe801",
    "customerPhone": "0987654321",
    "originalAmount": 250000.00,
    "bookingPublicId": "c86e0c03-5182-4bf3-a3d8-a99f1fa43ab5",
    "contextJson": {
      "cinemaId": "cin-1234",
      "showtimeId": "st-5678",
      "movieId": "mov-9999"
    }
  }
  ```
- **Response Success (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Operation successful",
    "data": {
      "valid": true,
      "benefitType": "COUPON",
      "benefitPublicId": "cop-8888-8888",
      "code": "LORAFILM50K",
      "originalAmount": 250000.00,
      "discountAmount": 50000.00,
      "finalAmount": 200000.00,
      "currency": "VND",
      "reasonCode": "ELIGIBLE",
      "message": "Benefit is eligible"
    },
    "timestamp": "2026-07-28T14:00:00Z"
  }
  ```

#### 7.2.2. Validate Voucher
Kiểm tra tính hợp lệ của voucher cá nhân trong ví của user.

- **Endpoint**: `POST /internal/vouchers/validate`
- **Request Body**:
  ```json
  {
    "code": "VCH-XYZ-123",
    "userPublicId": "8f395bb2-33cc-4b77-88f5-9372f7cbe801",
    "originalAmount": 250000.00,
    "bookingPublicId": "c86e0c03-5182-4bf3-a3d8-a99f1fa43ab5",
    "contextJson": {}
  }
  ```
- **Response Success (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Operation successful",
    "data": {
      "valid": true,
      "benefitType": "VOUCHER",
      "benefitPublicId": "vch-7777-7777",
      "code": "VCH-XYZ-123",
      "originalAmount": 250000.00,
      "discountAmount": 100000.00,
      "finalAmount": 150000.00,
      "currency": "VND",
      "reasonCode": "ELIGIBLE",
      "message": "Benefit is eligible"
    },
    "timestamp": "2026-07-28T14:00:00Z"
  }
  ```

---

### 7.3. Ví Voucher Khách Hàng (Customer APIs)

#### 7.3.1. Get My Vouchers
Lấy danh sách các voucher thuộc quyền sở hữu của khách hàng đang đăng nhập.

- **Endpoint**: `GET /api/customers/me/vouchers`
- **Headers**:
  - `Authorization: Bearer <accessToken>`
- **Query Parameters**:
  - `status`: Lọc theo trạng thái (`ACTIVE`, `USED`, `EXPIRED`, `REVOKED`)
  - `page`: Số trang (mặc định: 0)
  - `size`: Kích thước trang (mặc định: 10)
- **Response Success (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Vouchers retrieved successfully",
    "data": {
      "content": [
        {
          "voucherPublicId": "vch-7777-7777",
          "code": "VCH-XYZ-123",
          "name": "Voucher Nâng Hạng VIP",
          "description": "Giảm ngay 100K cho mọi loại vé phim",
          "status": "ACTIVE",
          "issuedAt": "2026-07-20T10:00:00.000",
          "validFrom": "2026-07-20T00:00:00.000",
          "validTo": "2026-10-20T23:59:59.000",
          "faceValue": 100000.00,
          "minimumOrderAmount": 150000.00
        }
      ],
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

---

### 7.4. Nhóm Campaign Admin (Admin APIs)

#### 7.4.1. Create Campaign
- **Endpoint**: `POST /api/admin/promotion-campaigns`
- **Request Body**:
  ```json
  {
    "code": "LORAFILM_SUMMER_2026",
    "name": "LoraFilm Summer Campaign 2026",
    "description": "Chiến dịch ưu đãi vé hè 2026",
    "campaignType": "COUPON",
    "fundingSource": "PLATFORM",
    "priority": 100,
    "startAt": "2026-08-01T00:00:00.000",
    "endAt": "2026-08-31T23:59:59.000",
    "budgetAmount": 50000000.00,
    "maxRedemptions": 1000,
    "maxRedemptionsPerUser": 2,
    "legalNotificationRef": "CV-81/2026/SCT"
  }
  ```
- **Response Success (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Campaign created successfully",
    "data": {
      "campaignPublicId": "cam-1010-1010",
      "code": "LORAFILM_SUMMER_2026",
      "status": "DRAFT",
      "approvalStatus": "PENDING",
      "legalStatus": "PENDING"
    }
  }
  ```

---

### 7.5. Nhóm Partner & Settlement (Admin APIs)

#### 7.5.1. Create Partner Settlement
Tạo đợt đối soát và quyết toán chi phí khuyến mãi với các đối tác ví điện tử / ngân hàng.

- **Endpoint**: `POST /api/admin/partner-settlements`
- **Request Body**:
  ```json
  {
    "partnerPublicId": "part-9999-9999",
    "campaignPublicId": "cam-1010-1010",
    "settlementPeriodFrom": "2026-07-01T00:00:00.000",
    "settlementPeriodTo": "2026-07-31T23:59:59.000"
  }
  ```
- **Response Success (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Partner settlement session initialized successfully",
    "data": {
      "settlementPublicId": "set-1111-2222",
      "settlementCode": "SET-MOMO-JULY26",
      "totalOrders": 120,
      "totalDiscount": 6000000.00,
      "partnerAmount": 3000000.00,
      "platformAmount": 3000000.00,
      "finalAmount": 3000000.00,
      "status": "PENDING_APPROVAL"
    }
  }
  ```

---

## 8. Danh Mục Mã Lỗi (Error Catalog)

| Error Code | HTTP Status | Ý nghĩa |
| :--- | :---: | :--- |
| `CAMPAIGN_NOT_FOUND` | 404 | Chiến dịch không tồn tại |
| `CAMPAIGN_INACTIVE` | 409 | Chiến dịch đang không hoạt động (Disabled hoặc Paused) |
| `CAMPAIGN_EXPIRED` | 409 | Chiến dịch đã hết hạn hiệu lực |
| `PROMOTION_NOT_FOUND` | 404 | Coupon / Voucher không tồn tại |
| `PROMOTION_DISABLED` | 409 | Ưu đãi đã bị vô hiệu hóa |
| `PROMOTION_EXPIRED` | 409 | Thời gian sử dụng ưu đãi đã hết hạn |
| `PROMOTION_USAGE_LIMIT_REACHED` | 409 | Vượt quá số lượt sử dụng tối đa toàn hệ thống |
| `PROMOTION_USER_LIMIT_REACHED` | 409 | Vượt quá lượt sử dụng tối đa của người dùng này |
| `PROMOTION_MINIMUM_AMOUNT_NOT_MET`| 400 | Không đạt giá trị đơn hàng tối thiểu |
| `LEGAL_DISCOUNT_CEILING_EXCEEDED` | 400 | Mức giảm vượt quá trần quy định của pháp luật (50%) |
| `RESERVATION_NOT_FOUND` | 404 | Không tìm thấy phiên giữ khuyến mại |
| `RESERVATION_EXPIRED` | 409 | Phiên giữ khuyến mại đã hết hạn TTL |
| `RESERVATION_CONFLICT` | 409 | Benefit/quota/budget đang được giữ hoặc dữ liệu reservation không nhất quán |
| `RESERVATION_COMPLETED` | 409 | Không thể release/cancel reservation đã tạo redemption |
| `RESERVATION_IDEMPOTENCY_CONFLICT` | 409 | Replay có payload, payment hoặc transition data khác request ban đầu |
| `RESERVATION_INVALID_STATE` | 409 hoặc 400 | Chuyển trạng thái hoặc deadline không hợp lệ |
| `PARTNER_NOT_FOUND` | 404 | Không tìm thấy đối tác |
| `IDEMPOTENCY_CONFLICT` | 409 | Trùng Idempotency Key với dữ liệu yêu cầu khác biệt |
| `SCORE_SERVICE_UNAVAILABLE` | 503 | Dịch vụ Loyalty Point bị lỗi (Áp dụng Circuit Breaker fallback) |

---

## 9. Lịch Sử Chỉnh Sửa

| Ngày | Nội dung chỉnh sửa | Người thực hiện |
| :--- | :--- | :--- |
| 21/06/2026 | Khởi tạo Promotion Service API Contract dựa trên schema Sprint 0 | Dương Thiện Nhân |
| 22/06/2026 | Cập nhật theo review của Owner: lifecycle, snapshot, revert audit. | Dương Thiện Nhân |
| 24/06/2026 | Đồng bộ timestamps với DB schema & approve | Dương Thiện Nhân |
| 28/07/2026 | **Hợp nhất hoàn toàn đặc tả Benefit Domain (API + Business Rules + Thực tế MySQL DDL)**. Thay đổi các endpoint apply/confirm sang `/internal/promotions/...`. Xóa bỏ tệp `benefit-domain-api.md`. | Antigravity |
| 28/07/2026 | Đồng bộ Reservation Runtime với implementation thực tế: 8 API canonical, state machine terminal, pricing snapshot, idempotency, lock, auto-expiration và lịch sử vận hành. | Codex |
| 28/07/2026 | Production hardening: service-scoped auth/idempotency, one-benefit checkout scope, legal/budget gate, strict condition engine, E.164 phone, Kafka ACK + DB lease outbox, per-row scheduler retry và Flyway migration. | Codex |
