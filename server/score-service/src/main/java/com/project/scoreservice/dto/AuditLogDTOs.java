package com.project.scoreservice.dto;

import com.project.scoreservice.entity.AuditLog;

import java.time.LocalDateTime;

public class AuditLogDTOs {

    public record AuditLogResponse(
            Long id,
            String transactionUuid,
            Long historyId,
            Long userId,
            Long operatorId,
            String action,
            String resource,
            String httpMethod,
            String requestUri,
            Integer httpStatus,
            String clientIp,
            String userAgent,
            String deviceId,
            String correlationId,
            String requestPayload,
            String responsePayload,
            String metadata,
            Long durationMs,
            LocalDateTime createdAt
    ) {
        public static AuditLogResponse fromEntity(AuditLog entity) {
            return new AuditLogResponse(
                    entity.getId(),
                    entity.getTransactionUuid(),
                    entity.getHistoryId(),
                    entity.getUserId(),
                    entity.getOperatorId(),
                    entity.getAction(),
                    entity.getResource(),
                    entity.getHttpMethod(),
                    entity.getRequestUri(),
                    entity.getHttpStatus(),
                    entity.getClientIp(),
                    entity.getUserAgent(),
                    entity.getDeviceId(),
                    entity.getCorrelationId(),
                    entity.getRequestPayload(),
                    entity.getResponsePayload(),
                    entity.getMetadata(),
                    entity.getDurationMs(),
                    entity.getCreatedAt()
            );
        }
    }
}
