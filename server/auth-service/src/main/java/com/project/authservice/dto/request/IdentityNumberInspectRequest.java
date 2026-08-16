package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class IdentityNumberInspectRequest {
    @NotBlank(message = "identity number is required")
    @Pattern(regexp = "^\\d{12}$", message = "identity number must contain 12 digits")
    private String cccd;

    public IdentityNumberInspectRequest() {
    }

    public IdentityNumberInspectRequest(String cccd) {
        this.cccd = cccd;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }
}
