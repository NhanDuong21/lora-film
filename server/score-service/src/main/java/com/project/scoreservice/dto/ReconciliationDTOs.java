package com.project.scoreservice.dto;

import com.project.scoreservice.entity.ReconciliationDetail;
import com.project.scoreservice.entity.ReconciliationRun;
import com.project.scoreservice.enumtype.ReconciliationDetailStatus;
import com.project.scoreservice.enumtype.ReconciliationRunStatus;

import java.time.LocalDateTime;

public class ReconciliationDTOs {

    public record ReconciliationRunRequest(
            String batchCode,
            String remark
    ) {}

    public record ReconciliationRunResponse(
            Long id,
            String batchCode,
            ReconciliationRunStatus status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            Integer totalUsers,
            Integer matchedUsers,
            Integer mismatchedUsers,
            Integer totalAdjustments,
            Long executedBy,
            String remark,
            LocalDateTime createdAt
    ) {
        public static ReconciliationRunResponse fromEntity(ReconciliationRun entity) {
            return new ReconciliationRunResponse(
                    entity.getId(),
                    entity.getBatchCode(),
                    entity.getStatus(),
                    entity.getStartedAt(),
                    entity.getFinishedAt(),
                    entity.getTotalUsers(),
                    entity.getMatchedUsers(),
                    entity.getMismatchedUsers(),
                    entity.getTotalAdjustments(),
                    entity.getExecutedBy(),
                    entity.getRemark(),
                    entity.getCreatedAt()
            );
        }
    }

    public record ReconciliationDetailResponse(
            Long id,
            Long runId,
            Long userId,
            Integer currentBalance,
            Integer ledgerBalance,
            Integer balanceDifference,
            Integer currentHeldPoints,
            Integer ledgerHeldPoints,
            Integer heldDifference,
            Integer currentAccumulated,
            Integer ledgerAccumulated,
            Integer accumulatedDifference,
            ReconciliationDetailStatus status,
            Long adjustmentHistoryId,
            String remark,
            LocalDateTime createdAt
    ) {
        public static ReconciliationDetailResponse fromEntity(ReconciliationDetail entity) {
            return new ReconciliationDetailResponse(
                    entity.getId(),
                    entity.getRun() != null ? entity.getRun().getId() : null,
                    entity.getUserId(),
                    entity.getCurrentBalance(),
                    entity.getLedgerBalance(),
                    entity.getBalanceDifference(),
                    entity.getCurrentHeldPoints(),
                    entity.getLedgerHeldPoints(),
                    entity.getHeldDifference(),
                    entity.getCurrentAccumulated(),
                    entity.getLedgerAccumulated(),
                    entity.getAccumulatedDifference(),
                    entity.getStatus(),
                    entity.getAdjustmentHistoryId(),
                    entity.getRemark(),
                    entity.getCreatedAt()
            );
        }
    }
}
