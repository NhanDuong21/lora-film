package com.project.authservice.dto.response;

public class ResendOtpResponse {
    private Long accountId;
    private Long expiresIn;
    private Long resendAvailableIn;

    public ResendOtpResponse() {}

    public ResendOtpResponse(Long accountId, Long expiresIn, Long resendAvailableIn) {
        this.accountId = accountId;
        this.expiresIn = expiresIn;
        this.resendAvailableIn = resendAvailableIn;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getResendAvailableIn() {
        return resendAvailableIn;
    }

    public void setResendAvailableIn(Long resendAvailableIn) {
        this.resendAvailableIn = resendAvailableIn;
    }
}
