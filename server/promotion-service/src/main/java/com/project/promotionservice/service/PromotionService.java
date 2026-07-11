package com.project.promotionservice.service;

import com.project.promotionservice.dto.PromotionResponse;
import com.project.promotionservice.dto.PromotionValidationRequest;
import com.project.promotionservice.dto.PromotionValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface PromotionService {
    Page<PromotionResponse> getActivePromotions(String discountType, BigDecimal minOrderAmount, Pageable pageable);
    PromotionResponse getPromotionDetail(Long id);
    PromotionValidationResponse validatePromotion(PromotionValidationRequest request, String authHeader);
    PromotionValidationResponse previewDiscount(PromotionValidationRequest request, String authHeader);
}
