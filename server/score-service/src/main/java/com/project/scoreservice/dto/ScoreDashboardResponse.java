package com.project.scoreservice.dto;

import java.time.LocalDateTime;
import com.project.scoreservice.enumtype.ReconciliationRunStatus;

public record ScoreDashboardResponse(
        long totalMembers,
        long activeMembers,
        long lockedMembers,
        long totalAvailablePoints,
        long totalAccumulatedPoints,
        long totalOutstandingPoints,
        long totalPointsEarned,
        long totalPointsRedeemed,
        long totalPointsHeld,
        long totalPointsExpired,
        long silverMembers,
        long goldMembers,
        long diamondMembers,
        long pendingReconciliations,
        String lastReconciliationBatch,
        LocalDateTime lastReconciliationDate,
        LocalDateTime lastReconciliationFinishedAt,
        ReconciliationRunStatus lastReconciliationStatus,
        long lastReconciliationTotalUsers,
        long lastReconciliationMatchedUsers,
        long lastReconciliationMismatchedUsers
) {}
