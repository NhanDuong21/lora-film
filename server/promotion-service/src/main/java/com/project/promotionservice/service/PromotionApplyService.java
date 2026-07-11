package com.project.promotionservice.service;

import com.project.promotionservice.dto.ApplyPromotionRequest;
import com.project.promotionservice.dto.ApplyPromotionResponse;

public interface PromotionApplyService {
    ApplyPromotionResponse applyPromotion(ApplyPromotionRequest request);
}
