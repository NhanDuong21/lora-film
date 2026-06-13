package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VerifyRequest {
    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotBlank(message = "otp is required")
    private String otp;

    public VerifyRequest() {}

    public VerifyRequest(Long accountId, String otp) {
        this.accountId = accountId;
        this.otp = otp;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
