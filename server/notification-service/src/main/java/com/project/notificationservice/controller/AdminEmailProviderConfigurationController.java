package com.project.notificationservice.controller;

import com.project.notificationservice.api.ApiResponse;
import com.project.notificationservice.service.EmailProviderConfigurationService;
import com.project.notificationservice.service.EmailProviderConfigurationService.EmailProviderStatus;
import com.project.notificationservice.service.EmailProviderConfigurationService.SmtpConnectionTestResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notification-settings/email-provider")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmailProviderConfigurationController {

    private final EmailProviderConfigurationService service;

    public AdminEmailProviderConfigurationController(
            EmailProviderConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<EmailProviderStatus> status() {
        return ApiResponse.success(service.status());
    }

    @PostMapping("/test")
    public ApiResponse<SmtpConnectionTestResult> test(
            @Valid @RequestBody UpdateEmailProviderRequest request) {
        return ApiResponse.success(service.testConnection(
                request.getSenderEmail(),
                request.getAppPassword(),
                request.getFromName()));
    }

    @PutMapping
    public ApiResponse<EmailProviderStatus> update(
            @Valid @RequestBody UpdateEmailProviderRequest request,
            Authentication authentication) {
        return ApiResponse.success(service.update(
                request.getSenderEmail(),
                request.getAppPassword(),
                request.getFromName(),
                authentication == null ? "system" : authentication.getName()));
    }

    public static class UpdateEmailProviderRequest {
        @NotBlank(message = "Email gửi là bắt buộc")
        @Email(message = "Email gửi không đúng định dạng")
        @Size(max = 320, message = "Email gửi quá dài")
        private String senderEmail;

        @NotBlank(message = "App Password là bắt buộc")
        @Size(min = 8, max = 200, message = "App Password phải từ 8 đến 200 ký tự")
        private String appPassword;

        @NotBlank(message = "Tên người gửi là bắt buộc")
        @Size(max = 120, message = "Tên người gửi tối đa 120 ký tự")
        private String fromName;

        public String getSenderEmail() { return senderEmail; }
        public void setSenderEmail(String value) { this.senderEmail = value; }
        public String getAppPassword() { return appPassword; }
        public void setAppPassword(String value) { this.appPassword = value; }
        public String getFromName() { return fromName; }
        public void setFromName(String value) { this.fromName = value; }

        @Override
        public String toString() {
            return "UpdateEmailProviderRequest{senderEmail='" + senderEmail
                    + "', appPassword='[REDACTED]', fromName='" + fromName + "'}";
        }
    }
}
