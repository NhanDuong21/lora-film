package com.project.scoreservice.service;

import com.project.scoreservice.dto.ScoreAdjustmentRequest;
import com.project.scoreservice.dto.ScoreAdjustmentResponse;

public interface ScoreAdjustmentService {
    ScoreAdjustmentResponse adjustScore(Long userId, ScoreAdjustmentRequest request, Long adminId);
}
