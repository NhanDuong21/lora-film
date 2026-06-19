package com.project.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SendOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Purpose is required")
    @Pattern(regexp = "^(REGISTRATION|LOGIN|FORGOTTEN PASSWORD|CHANGE EMAIL|CHANGE PASSWORD)$", message = "Invalid OTP purpose")
    private String purpose;

    public SendOtpRequest() {}

    public SendOtpRequest(String email, String purpose) {
        this.email = email;
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
