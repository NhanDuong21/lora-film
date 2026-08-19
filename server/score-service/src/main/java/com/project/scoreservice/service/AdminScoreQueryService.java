package com.project.scoreservice.service;

import com.project.scoreservice.dto.AdminScoreHistoryItemResponse;
import com.project.scoreservice.dto.AdminUserScoreResponse;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface AdminScoreQueryService {
    Page<AdminUserScoreResponse> getUserScores(int page, int size);

    AdminUserScoreResponse getUserScoreDetail(Long userId);

    Page<AdminScoreHistoryItemResponse> getUserHistory(
            Long userId,
            int page,
            int size,
            ScoreTransactionType transactionType,
            Long bookingId,
            ReconciliationStatus reconciliationStatus,
            LocalDateTime from,
            LocalDateTime to,
            String sort
    );
}
