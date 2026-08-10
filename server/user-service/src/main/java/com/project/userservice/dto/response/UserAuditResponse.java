package com.project.userservice.dto.response;

import java.time.LocalDateTime;

public record UserAuditResponse(
        Long id,
        Long actorAccountId,
        String action,
        String targetType,
        String targetId,
        String details,
        LocalDateTime createdAt
) {
}
