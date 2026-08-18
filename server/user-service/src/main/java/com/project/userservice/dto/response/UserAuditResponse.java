package com.project.userservice.dto.response;

import java.time.LocalDateTime;

public record UserAuditResponse(
        Long id,
        Long actorAccountId,
        String action,
        String targetType,
        String targetId,
        String details,
        String result,
        String severity,
        String reviewStatus,
        Long reviewedBy,
        String reviewNote,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
