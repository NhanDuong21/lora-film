const failureMessages = {
    SMTP_AUTHENTICATION_FAILED: {
        title: 'SMTP từ chối thông tin đăng nhập',
        description: 'Tài khoản gửi hoặc app password không hợp lệ. Kiểm tra secret MAIL_USERNAME và MAIL_PASSWORD của notification-service.',
    },
    SMTP_CONNECTION_FAILED: {
        title: 'Không thể kết nối máy chủ SMTP',
        description: 'Notification-service không kết nối được máy chủ gửi thư. Kiểm tra MAIL_HOST, MAIL_PORT, DNS, mạng và cấu hình TLS.',
    },
    SMTP_TEMPORARILY_UNAVAILABLE: {
        title: 'Máy chủ SMTP đang tạm thời từ chối yêu cầu',
        description: 'Nhà cung cấp trả về lỗi tạm thời. Hệ thống sẽ tự thử lại theo thời gian hiển thị bên dưới.',
    },
    SMTP_RECIPIENT_REJECTED: {
        title: 'Địa chỉ email người nhận bị từ chối',
        description: 'Máy chủ SMTP không chấp nhận địa chỉ người nhận. Kiểm tra lại email trước khi tạo yêu cầu mới.',
    },
    SMTP_MESSAGE_REJECTED: {
        title: 'Máy chủ SMTP từ chối nội dung thư',
        description: 'Nhà cung cấp từ chối thư vĩnh viễn. Kiểm tra chính sách người gửi, nội dung và phản hồi SMTP trong log dịch vụ.',
    },
    SMTP_SEND_FAILED: {
        title: 'Không thể gửi email qua SMTP',
        description: 'Nhà cung cấp không nhận thư trong lần gửi này nhưng hệ thống cũ chưa lưu được nguyên nhân cụ thể. Kiểm tra log SMTP; hệ thống sẽ tự thử lại nếu yêu cầu còn hiệu lực.',
    },
    SMTP_REJECTED: {
        title: 'Nhà cung cấp email từ chối thư',
        description: 'Kiểm tra chính sách người gửi, địa chỉ người nhận và nội dung thư trong log notification-service.',
    },
    INVALID_EMAIL: {
        title: 'Địa chỉ email không hợp lệ',
        description: 'Email người nhận sai định dạng hoặc đang để trống. Sửa dữ liệu người nhận rồi tạo yêu cầu mới.',
    },
    DELIVERY_PREPARATION_FAILED: {
        title: 'Không thể chuẩn bị nội dung gửi',
        description: 'Payload hoặc dữ liệu người nhận không thể xử lý. Kiểm tra dữ liệu đầu vào và schema của template.',
    },
    TEMPLATE_NOT_FOUND: {
        title: 'Không tìm thấy template đã phát hành',
        description: 'Template hoặc locale của yêu cầu chưa có bản phát hành đang hoạt động.',
    },
    TEMPLATE_RENDER_FAILED: {
        title: 'Không thể render template',
        description: 'Dữ liệu truyền vào không đáp ứng biến bắt buộc hoặc nội dung template không hợp lệ.',
    },
};

export function getNotificationFailurePresentation(code) {
    const normalizedCode = String(code || 'UNKNOWN_FAILURE').toUpperCase();
    return {
        code: normalizedCode,
        ...(failureMessages[normalizedCode] || {
            title: 'Gửi thông báo thất bại',
            description: 'Hệ thống chưa phân loại được lỗi này. Dùng mã kỹ thuật bên dưới để tra log notification-service.',
        }),
    };
}
