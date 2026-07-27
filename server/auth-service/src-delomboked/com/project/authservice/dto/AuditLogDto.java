package com.project.authservice.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        Long accountId,
        String action,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
