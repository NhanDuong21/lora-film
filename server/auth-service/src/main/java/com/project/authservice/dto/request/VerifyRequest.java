package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

public class VerifyRequest {
    @Schema(example = "test@example.com", description = "Required for all verifications")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(example = "123456")
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(REGISTRATION|FORGOTTEN PASSWORD|CHANGE EMAIL|CHANGE PASSWORD)$", message = "Invalid OTP purpose")
    @Schema(example = "REGISTRATION")
    private String purpose;

    public VerifyRequest() {
    }

    public VerifyRequest(String email, String otp, String purpose) {
        this.email = email;
        this.otp = otp;
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
