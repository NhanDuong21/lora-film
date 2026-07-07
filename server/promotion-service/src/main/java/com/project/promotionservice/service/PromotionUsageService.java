package com.project.promotionservice.service;

import com.project.promotionservice.dto.ConfirmUsageRequest;
import com.project.promotionservice.dto.PromotionUsageResponse;
import com.project.promotionservice.dto.RevertUsageRequest;

public interface PromotionUsageService {
    PromotionUsageResponse confirmUsage(Long usageId, ConfirmUsageRequest request);
    PromotionUsageResponse revertUsage(Long usageId, RevertUsageRequest request);
    PromotionUsageResponse getUsageByBookingId(Long bookingId);
}
