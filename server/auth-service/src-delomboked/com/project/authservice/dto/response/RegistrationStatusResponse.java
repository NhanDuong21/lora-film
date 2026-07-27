package com.project.authservice.dto.response;

public record RegistrationStatusResponse(
        String requestId,
        String status,
        String errorCode
) {
}
