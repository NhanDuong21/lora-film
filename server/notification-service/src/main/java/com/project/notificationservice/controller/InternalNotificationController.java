package com.project.notificationservice.controller;

import com.project.notificationservice.common.ApiResponse;
import com.project.notificationservice.dto.request.SendNotificationRequest;
import com.project.notificationservice.dto.response.SendNotificationResponse;
import com.project.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationService service;

    public InternalNotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendNotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        SendNotificationResponse response = service.sendNotification(request);

        if (Boolean.TRUE.equals(response.getIdempotent())) {
            ApiResponse<SendNotificationResponse> apiResponse = ApiResponse.success(
                    "Notification request was already processed", response);
            return ResponseEntity.ok(apiResponse);
        }

        ApiResponse<SendNotificationResponse> apiResponse = ApiResponse.success(
                "Notification sent successfully", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
}
