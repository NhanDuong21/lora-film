package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

public class VerifyRequest {
    @NotNull(message = "Account ID is required")
    @Schema(example = "15")
    private Long accountId;

    @Schema(example = "123456")
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(REGISTRATION|FORGOTTEN PASSWORD|CHANGE EMAIL|CHANGE PASSWORD)$", message = "Invalid OTP purpose")
    @Schema(example = "REGISTRATION")
    private String purpose;

    public VerifyRequest() {}

    public VerifyRequest(Long accountId, String otp, String purpose) {
        this.accountId = accountId;
        this.otp = otp;
        this.purpose = purpose;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
