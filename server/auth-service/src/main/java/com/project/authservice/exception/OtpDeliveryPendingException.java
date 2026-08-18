package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class OtpDeliveryPendingException extends BaseAuthException {
    public OtpDeliveryPendingException() {
        super(
                "Yêu cầu gửi mã xác minh vẫn đang được xử lý. Vui lòng kiểm tra email trong ít phút trước khi yêu cầu mã mới.",
                "AUTH_OTP_DELIVERY_PENDING",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
