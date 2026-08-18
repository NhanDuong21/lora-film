const notificationNames = {
    AUTH_REGISTRATION_OTP: 'OTP đăng ký tài khoản',
    REGISTER_OTP: 'OTP đăng ký tài khoản',
    AUTH_CHANGE_EMAIL_OTP: 'OTP thay đổi email',
    CHANGE_EMAIL_OTP: 'OTP thay đổi email',
    AUTH_FORGOT_PASSWORD_OTP: 'OTP đặt lại mật khẩu',
    FORGOT_PASSWORD_OTP: 'OTP đặt lại mật khẩu',
    AUTH_PASSWORD_RESET_OTP: 'OTP đặt lại mật khẩu',
    BOOKING_CONFIRMED: 'Xác nhận đặt vé',
    TICKET_ISSUED: 'Xác nhận đặt vé',
    BOOKING_CREATED: 'Tiếp nhận đặt vé',
    BOOKING_CANCELLED: 'Hủy đặt vé',
    BOOKING_EXPIRED: 'Đặt vé đã hết hạn',
    VOUCHER_GRANTED: 'Thông báo cấp voucher',
    VOUCHER_EXPIRING: 'Voucher sắp hết hạn',
    VOUCHER_USED: 'Xác nhận sử dụng voucher',
    ACCOUNT_LOCKED: 'Cảnh báo khóa tài khoản',
    ACCOUNT_UNLOCKED: 'Thông báo mở khóa tài khoản',
    DISCOUNT_ANNOUNCEMENT: 'Thông báo ưu đãi',
    LOGIN_ALERT: 'Cảnh báo đăng nhập',
    MOVIE_REMINDER: 'Nhắc lịch xem phim',
    MOVIE_STARTING_SOON: 'Phim sắp bắt đầu',
    NEW_FEATURE: 'Giới thiệu tính năng mới',
    NEW_MOVIE: 'Thông báo phim mới',
    PASSWORD_CHANGED: 'Xác nhận đổi mật khẩu',
    PAYMENT_FAILED: 'Thanh toán không thành công',
    PAYMENT_SUCCESS: 'Xác nhận thanh toán thành công',
    PROMOTION_EVENT: 'Sự kiện khuyến mãi',
    SHOWTIME_CANCELLED: 'Hủy suất chiếu',
    SHOWTIME_CHANGED: 'Thay đổi suất chiếu',
    SYSTEM_MAINTENANCE: 'Thông báo bảo trì hệ thống',
    TEST: 'Mẫu kiểm thử',
    VERIFY_EMAIL: 'Xác minh địa chỉ email',
};

const serviceNames = {
    'auth-service': 'Tài khoản và đăng nhập',
    'booking-service': 'Đặt vé',
    'payment-service': 'Thanh toán',
    'promotion-service': 'Khuyến mãi',
    'notification-service': 'Thông báo',
    'score-service': 'Tích điểm',
    'user-service': 'Khách hàng',
};

const channelNames = {
    EMAIL: 'Email',
    IN_APP: 'Trong ứng dụng',
    SMS: 'Tin nhắn SMS',
    WEB_PUSH: 'Thông báo trình duyệt',
};

const categoryNames = {
    TRANSACTIONAL: 'Giao dịch',
    SECURITY: 'Bảo mật',
    MARKETING: 'Tiếp thị',
    OPERATIONAL: 'Vận hành',
    SYSTEM: 'Hệ thống',
};

export const notificationBusinessName = (eventType, templateKey) => {
    const keys = [eventType, templateKey].filter(Boolean).map(value => String(value).toUpperCase());
    for (const key of keys) {
        if (notificationNames[key]) return notificationNames[key];
    }
    const fallback = keys[0] || 'THÔNG BÁO';
    return fallback.toLowerCase().replaceAll('_', ' ').replace(/^./, character => character.toUpperCase());
};

export const serviceBusinessName = value => serviceNames[value] || value || 'Hệ thống nội bộ';
export const channelBusinessName = value => channelNames[String(value || '').toUpperCase()] || value || 'Chưa xác định';
export const categoryBusinessName = value => categoryNames[String(value || '').toUpperCase()] || value || 'Chưa phân loại';
export const localeBusinessName = value => ({ 'vi-VN': 'Tiếng Việt', 'en-US': 'Tiếng Anh' }[value] || value || 'Mặc định');

export const recipientDisplay = recipient => recipient?.maskedEmail
    || recipient?.maskedPhone
    || (recipient?.userPublicId ? `Khách hàng ${String(recipient.userPublicId).slice(0, 8)}…` : 'Chưa có thông tin');

export const deliveryOutcome = delivery => {
    const status = String(delivery?.status || '').toUpperCase();
    if (status === 'DELIVERED') return 'Đã có xác nhận giao';
    if (status === 'SENT') return 'Nhà cung cấp đã nhận yêu cầu';
    if (status === 'RETRY_SCHEDULED') return 'Hệ thống đang tự thử lại';
    if (status === 'PENDING' || status === 'PROCESSING') return 'Đang chờ xử lý';
    if (status === 'FAILED' || status === 'DEAD_LETTERED') return 'Gửi không thành công';
    if (status === 'CANCELLED') return 'Đã hủy';
    if (status === 'SUPPRESSED') return 'Không gửi theo chính sách';
    return 'Đang cập nhật kết quả';
};

export const notificationVariantSummary = variants => (variants || [])
    .map(variant => `${channelBusinessName(variant.channel)} ${localeBusinessName(variant.locale)}`)
    .join(', ');
