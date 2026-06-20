package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import io.swagger.v3.oas.annotations.media.Schema;

public class ResendOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Schema(example = "user@example.com")
    private String email;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(REGISTRATION|LOGIN|FORGOTTEN PASSWORD|CHANGE EMAIL|CHANGE PASSWORD)$", message = "Invalid OTP purpose")
    @Schema(example = "REGISTRATION")
    private String purpose;

    public ResendOtpRequest() {}

    public ResendOtpRequest(String email, String purpose) {
        this.email = email;
        this.purpose = purpose;
    }


    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
