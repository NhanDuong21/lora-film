package com.project.userservice.mapper;

import com.project.userservice.dto.response.UserAuditResponse;
import com.project.userservice.entity.UserAuditLog;
import org.springframework.stereotype.Component;

@Component
public class UserAuditMapper {

    public UserAuditResponse toResponse(UserAuditLog entry) {
        return new UserAuditResponse(
                entry.getId(),
                entry.getActorAccountId(),
                entry.getAction(),
                entry.getTargetType(),
                entry.getTargetId(),
                entry.getDetails(),
                entry.getResult(),
                entry.getSeverity(),
                entry.getReviewStatus(),
                entry.getReviewedBy(),
                entry.getReviewNote(),
                entry.getReviewedAt(),
                entry.getCreatedAt());
    }
}
