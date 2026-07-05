package com.project.scoreservice.service;

import com.project.scoreservice.dto.RecalculateTierResponse;

public interface AdminTierRecalculationService {
    RecalculateTierResponse recalculateUserTier(Long userId);
}
