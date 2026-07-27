package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    
    @NotBlank
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
