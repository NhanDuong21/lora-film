package com.project.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SendOtpRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    public SendOtpRequest() {}

    public SendOtpRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
