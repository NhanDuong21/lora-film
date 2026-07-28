package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    
    @NotBlank
    @Size(min = 8, max = 50, message = "password length must be between 8 and 50")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).+$",
            message = "Password must contain uppercase, lowercase, digit, and special character")
    private String newPassword;
    public String getOldPassword() {
        return this.oldPassword;
    }
    public String getNewPassword() {
        return this.newPassword;
    }
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    public ChangePasswordRequest() {
    }
    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
