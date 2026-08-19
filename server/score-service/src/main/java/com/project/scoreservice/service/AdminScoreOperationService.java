package com.project.scoreservice.service;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ReconciliationDetailStatus;
import com.project.scoreservice.enumtype.ReconciliationRunStatus;

import java.time.LocalDateTime;

public interface AdminScoreOperationService {
    AdminAdjustmentResponse adjustScore(Long userId, ScoreAdjustmentRequest request, String operatorId, String clientIp);

    AdminAdjustmentResponse reverseAdjustment(Long userId, ReverseAdjustmentRequest request, String operatorId, String clientIp);

    AdminAdjustmentResponse recalculateTier(Long userId, String operatorId, String clientIp);

    AdminUserScoreResponse updateAccountStatus(Long userId, ScoreAccountStatusRequest request, String operatorId, String clientIp);

    ReconciliationDTOs.ReconciliationRunResponse runReconciliation(ReconciliationDTOs.ReconciliationRunRequest request, String operatorId);

    PageResponse<ReconciliationDTOs.ReconciliationRunResponse> getReconciliationRuns(int page, int size, ReconciliationRunStatus status, LocalDateTime from, LocalDateTime to);

    PageResponse<ReconciliationDTOs.ReconciliationDetailResponse> getReconciliationDetails(Long runId, int page, int size, ReconciliationDetailStatus status);

    PageResponse<AuditLogDTOs.AuditLogResponse> getAuditLogs(int page, int size, Long userId, Long operatorId, String action, LocalDateTime from, LocalDateTime to);

    byte[] exportScoreData(String type, String format, Long userId, LocalDateTime from, LocalDateTime to);

    byte[] exportScoreData(String type, String format, Long userId, LocalDateTime from, LocalDateTime to,
                           String operatorId, String clientIp);

    ScoreDashboardResponse getDashboardStats();
}
