package com.project.userservice.dto.response;

public record NotificationRecipientResponse(
        Long accountId,
        String email,
        String fullName) {
}
