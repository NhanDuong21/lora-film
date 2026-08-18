package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class RegistrationExpiredException extends BaseAuthException {
    public RegistrationExpiredException() {
        super(
                "Phiên đăng ký đã hết hạn nên tài khoản chưa thể được kích hoạt. Vui lòng quay lại trang đăng ký và gửi lại thông tin.",
                "AUTH_REGISTRATION_EXPIRED",
                HttpStatus.CONFLICT);
    }
}
