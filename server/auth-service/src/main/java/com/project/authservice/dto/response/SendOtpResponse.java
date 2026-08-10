package com.project.authservice.dto.response;

public class SendOtpResponse {
    private Long accountId;
    private Long expiresIn;

    public SendOtpResponse() {}

    public SendOtpResponse(Long accountId, Long expiresIn) {
        this.accountId = accountId;
        this.expiresIn = expiresIn;
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
}
