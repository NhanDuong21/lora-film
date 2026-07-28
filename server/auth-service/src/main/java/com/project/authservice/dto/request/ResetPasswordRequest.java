package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8, max = 50)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).+$",
            message = "Password must contain uppercase, lowercase, number, and special character")
    private String newPassword;
    public String getToken() {
        return this.token;
    }
    public String getNewPassword() {
        return this.newPassword;
    }
    public String getEmail() {
        return email;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public ResetPasswordRequest() {
    }
    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }
}
