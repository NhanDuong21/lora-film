package com.project.scoreservice.service;
 
import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import org.springframework.data.domain.Page;
 
import java.time.LocalDateTime;
 
public interface ScoreService {
    UserScoreResponse getUserScore(Long userId);
    UserTierResponse getUserTier(Long userId);
    InternalUserScoreResponse getInternalUserScore(Long userId);
    RedeemPreviewResponse previewRedeem(Long userId, RedeemPreviewRequest request);
    Page<ScoreHistoryResponse> getUserHistory(Long userId, int page, int size, ScoreTransactionType transactionType, Long bookingId, LocalDateTime from, LocalDateTime to, String sort);
    ScoreEarnResponse earnScore(ScoreEarnRequest request);
}
