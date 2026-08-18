package com.project.authservice.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        Long accountId,
        Long actorAccountId,
        String action,
        String resource,
        String resourceId,
        String description,
        String result,
        String severity,
        String reviewStatus,
        Long reviewedBy,
        String reviewNote,
        LocalDateTime reviewedAt,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
