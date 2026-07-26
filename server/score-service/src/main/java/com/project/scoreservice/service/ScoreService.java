package com.project.scoreservice.service;

import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface ScoreService {
    UserScoreResponse getUserScore(Long userId);
    UserScoreResponse getUserTier(Long userId);
    InternalUserScoreResponse getInternalUserScore(Long userId);
    Page<ScoreHistoryResponse> getUserHistory(Long userId, int page, int size, ScoreTransactionType transactionType, Long bookingId, LocalDateTime from, LocalDateTime to, String sort);

    ScoreEarnResponse earnPoints(ScoreEarnRequest request);
    RedeemPreviewResponse previewRedeem(Long userId, RedeemPreviewRequest request);
    ScoreHoldResponse holdPoints(ScoreHoldRequest request);
    ScoreCommitResponse commitPoints(ScoreCommitRequest request);
    ScoreReleaseResponse releasePoints(ScoreReleaseRequest request);
}
