package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

public class ResendOtpRequest {
    @NotNull(message = "Account ID is required")
    @Schema(example = "15")
    private Long accountId;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(REGISTRATION|LOGIN|FORGOTTEN PASSWORD|CHANGE EMAIL|CHANGE PASSWORD)$", message = "Invalid OTP purpose")
    @Schema(example = "REGISTRATION")
    private String purpose;

    public ResendOtpRequest() {}

    public ResendOtpRequest(Long accountId, String purpose) {
        this.accountId = accountId;
        this.purpose = purpose;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
