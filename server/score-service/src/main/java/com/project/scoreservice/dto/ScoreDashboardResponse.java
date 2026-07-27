package com.project.scoreservice.dto;

import java.time.LocalDateTime;

public record ScoreDashboardResponse(
        long totalMembers,
        long totalPointsEarned,
        long totalPointsRedeemed,
        long totalPointsHeld,
        long totalPointsExpired,
        long silverMembers,
        long goldMembers,
        long diamondMembers,
        long pendingReconciliations,
        String lastReconciliationBatch,
        LocalDateTime lastReconciliationDate
) {}
