package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.NotificationRecipientResponse;
import com.project.userservice.service.NotificationRecipientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final NotificationRecipientService recipientService;

    public InternalUserController(NotificationRecipientService recipientService) {
        this.recipientService = recipientService;
    }

    @GetMapping("/{accountId}/notification-recipient")
    public ResponseEntity<ApiResponse<NotificationRecipientResponse>> notificationRecipient(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Notification recipient retrieved",
                recipientService.findByAccountId(accountId)));
    }
}
