package com.project.promotionservice.service;

import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import java.time.LocalDateTime;

public interface PromotionAvailabilityService {
    String getCampaignStatus(PromotionCampaign campaign, LocalDateTime now);
    String getPromotionStatus(Promotion promotion, LocalDateTime now);
}
