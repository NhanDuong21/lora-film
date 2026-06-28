package com.project.notificationservice.controller;

import com.project.notificationservice.common.ApiResponse;
import com.project.notificationservice.dto.request.CreateNotificationTemplateRequest;
import com.project.notificationservice.dto.request.UpdateNotificationTemplateRequest;
import com.project.notificationservice.dto.request.UpdateNotificationTemplateStatusRequest;
import com.project.notificationservice.dto.response.NotificationTemplateResponse;
import com.project.notificationservice.dto.response.NotificationTemplateSummaryResponse;
import com.project.notificationservice.dto.response.PageResponse;
import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notification-templates")
public class NotificationTemplateAdminController {

    private final NotificationTemplateService service;

    public NotificationTemplateAdminController(NotificationTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_CREATE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateNotificationTemplateRequest request) {
        NotificationTemplateResponse response = service.createTemplate(request);
        ApiResponse<NotificationTemplateResponse> apiResponse = ApiResponse.success(
                "Notification template created successfully", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_READ') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<NotificationTemplateSummaryResponse>>> getTemplateList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) NotificationChannel channelType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String sort) {
        Page<NotificationTemplateSummaryResponse> servicePage = service.getTemplateList(
                page, size, code, channelType, isActive, sort);
        PageResponse<NotificationTemplateSummaryResponse> pageResponse = new PageResponse<>(servicePage);
        ApiResponse<PageResponse<NotificationTemplateSummaryResponse>> apiResponse = ApiResponse.success(
                "Notification templates retrieved successfully", pageResponse);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_READ') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getTemplateDetail(
            @PathVariable Integer templateId) {
        NotificationTemplateResponse response = service.getTemplateDetail(templateId);
        ApiResponse<NotificationTemplateResponse> apiResponse = ApiResponse.success(
                "Notification template retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplate(
            @PathVariable Integer templateId,
            @Valid @RequestBody UpdateNotificationTemplateRequest request) {
        NotificationTemplateResponse response = service.updateTemplate(templateId, request);
        ApiResponse<NotificationTemplateResponse> apiResponse = ApiResponse.success(
                "Notification template updated successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{templateId}/status")
    @PreAuthorize("hasAuthority('NOTIFICATION_TEMPLATE_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplateStatus(
            @PathVariable Integer templateId,
            @Valid @RequestBody UpdateNotificationTemplateStatusRequest request) {
        NotificationTemplateResponse response = service.updateTemplateStatus(templateId, request);
        ApiResponse<NotificationTemplateResponse> apiResponse = ApiResponse.success(
                "Notification template status updated successfully", response);
        return ResponseEntity.ok(apiResponse);
    }
}
