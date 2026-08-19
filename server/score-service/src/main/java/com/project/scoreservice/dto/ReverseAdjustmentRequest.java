package com.project.scoreservice.dto;

public record ReverseAdjustmentRequest(
        Long historyId,
        String reason,
        String requestId,
        String caseId
) {
    public ReverseAdjustmentRequest(Long historyId, String reason, String requestId) {
        this(historyId, reason, requestId, null);
    }
}
