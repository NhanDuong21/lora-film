package com.project.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {
    @NotBlank
    private String token;
    
    @NotBlank
    private String newPassword;
    public String getToken() {
        return this.token;
    }
    public String getNewPassword() {
        return this.newPassword;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    public ResetPasswordRequest() {
    }
    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }
}
