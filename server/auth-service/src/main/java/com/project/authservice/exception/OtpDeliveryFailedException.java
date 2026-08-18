package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class OtpDeliveryFailedException extends BaseAuthException {
    private final String providerFailureCode;

    public OtpDeliveryFailedException(String providerFailureCode) {
        super(
                "Không thể gửi mã xác minh vì máy chủ email đã từ chối thư. Bạn có thể thử gửi lại ngay.",
                "AUTH_OTP_DELIVERY_FAILED",
                HttpStatus.SERVICE_UNAVAILABLE);
        this.providerFailureCode = providerFailureCode;
    }

    public String getProviderFailureCode() {
        return providerFailureCode;
    }
}
