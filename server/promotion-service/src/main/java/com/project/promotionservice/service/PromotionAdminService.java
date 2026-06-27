package com.project.promotionservice.service;

import com.project.promotionservice.dto.*;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface PromotionAdminService {
    PromotionResponse createPromotion(CreatePromotionRequest request);
    PromotionPageResponse getPromotions(Boolean isActive, String availabilityStatus, String discountType,
                                        Long campaignId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    PromotionResponse getPromotionById(Long id);
    PromotionResponse updatePromotion(Long id, UpdatePromotionRequest request);
    PromotionResponse updatePromotionStatus(Long id, UpdatePromotionStatusRequest request);
}
